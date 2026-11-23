package com.poollines.mod;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    public static final String ACTION_ORIGIN = "com.poollines.mod.SET_ORIGIN";
    public static final String ACTION_DIRECTION = "com.poollines.mod.SET_DIRECTION";
    public static final String EXTRA_X = "origin_x";
    public static final String EXTRA_Y = "origin_y";
    public static final String EXTRA_RESULT_CODE = "projection_result";
    public static final String EXTRA_RESULT_DATA = "projection_data";

    private MediaProjection projection;
    private ImageReader reader;
    private VirtualDisplay display;
    private HandlerThread thread;
    private Handler handler;
    private float originX = -1f, originY = -1f;
    private int screenWidth, screenHeight, density;
    private long lastUpdate;
    private float stableAngle = Float.NaN;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("pool_capture",
                    "Detección de guía", NotificationManager.IMPORTANCE_LOW));
        }
        Notification n = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "pool_capture").setSmallIcon(android.R.drawable.ic_menu_view)
                    .setContentTitle("Pool Lines").setContentText("Siguiendo la guía visible").build()
                : new Notification.Builder(this).setSmallIcon(android.R.drawable.ic_menu_view)
                    .setContentTitle("Pool Lines").setContentText("Siguiendo la guía visible").build();
        startForeground(8, n);
        thread = new HandlerThread("PoolGuideDetector");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        if (ACTION_ORIGIN.equals(intent.getAction())) {
            originX = intent.getFloatExtra(EXTRA_X, -1f);
            originY = intent.getFloatExtra(EXTRA_Y, -1f);
            stableAngle = Float.NaN;
            return START_STICKY;
        }
        int result = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent data = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        if (data != null && projection == null) startCapture(result, data);
        return START_STICKY;
    }

    private void startCapture(int result, Intent data) {
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        density = metrics.densityDpi;
        MediaProjectionManager manager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        projection = manager.getMediaProjection(result, data);
        if (projection == null) {
            stopSelf();
            return;
        }
        reader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        reader.setOnImageAvailableListener(r -> {
            Image image = null;
            try {
                image = r.acquireLatestImage();
                if (image != null) analyze(image);
            } catch (Exception ignored) {
            } finally {
                if (image != null) image.close();
            }
        }, handler);
        display = projection.createVirtualDisplay("PoolLinesCapture", screenWidth, screenHeight,
                density, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.getSurface(), null, handler);
    }

    private void analyze(Image image) {
        if (originX < 0 || originY < 0) return;
        long now = System.currentTimeMillis();
        if (now - lastUpdate < 150) return;
        lastUpdate = now;

        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int base = buffer.position();

        int bestAngle = -1;
        int bestScore = 0;
        int maxDistance = Math.min(600, Math.min(screenWidth, screenHeight) * 3 / 4);

        // La guía debe formar un trazo continuo desde la bola, no solo reflejos aislados.
        for (int angle = 0; angle < 360; angle++) {
            double rad = Math.toRadians(angle);
            double dx = Math.cos(rad), dy = Math.sin(rad);
            int hits = 0;
            int run = 0;
            int longestRun = 0;

            for (int d = 22; d < maxDistance; d += 3) {
                int x = Math.round(originX + (float)(dx * d));
                int y = Math.round(originY + (float)(dy * d));
                if (x < 3 || y < 3 || x >= screenWidth - 3 || y >= screenHeight - 3) break;

                boolean hit = false;
                for (int side = -2; side <= 2; side++) {
                    int sx = Math.round(x - (float)(dy * side));
                    int sy = Math.round(y + (float)(dx * side));
                    if (sx < 0 || sy < 0 || sx >= screenWidth || sy >= screenHeight) continue;
                    int index = base + sy * rowStride + sx * pixelStride;
                    if (index < base || index + 2 >= buffer.limit()) continue;

                    int red = buffer.get(index) & 255;
                    int green = buffer.get(index + 1) & 255;
                    int blue = buffer.get(index + 2) & 255;
                    if (red > 225 && green > 225 && blue > 225
                            && Math.abs(red - green) < 38 && Math.abs(green - blue) < 38) {
                        hit = true;
                        break;
                    }
                }

                if (hit) {
                    hits++;
                    run++;
                    if (run > longestRun) longestRun = run;
                } else {
                    run = 0;
                }
            }

            // Penaliza puntos blancos dispersos y premia una línea larga y seguida.
            int score = hits + longestRun * 2;
            if (hits >= 10 && longestRun >= 6 && score > bestScore) {
                bestScore = score;
                bestAngle = angle;
            }
        }

        if (bestAngle < 0) return;

        // Suavizado angular circular para evitar que la línea tiemble entre fotogramas.
        if (Float.isNaN(stableAngle)) {
            stableAngle = bestAngle;
        } else {
            float delta = ((bestAngle - stableAngle + 540f) % 360f) - 180f;
            // Si el detector salta a una dirección opuesta por un reflejo aislado, no seguirlo.
            if (Math.abs(delta) > 70f) return;
            stableAngle = (stableAngle + delta * 0.38f + 360f) % 360f;
        }

        Intent update = new Intent(ACTION_DIRECTION);
        update.setPackage(getPackageName());
        update.putExtra("angle", Math.round(stableAngle) % 360);
        sendBroadcast(update);
    }

    @Override public void onDestroy() {
        if (display != null) display.release();
        if (reader != null) reader.close();
        if (projection != null) projection.stop();
        if (thread != null) thread.quitSafely();
        stopForeground(true);
        super.onDestroy();
    }
}
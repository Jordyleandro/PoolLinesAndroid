package com.poollines.mod;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class PoolOverlayService extends Service {
    private static final String CHANNEL_ID = "pool_lines_overlay";
    private WindowManager windowManager;
    private LineView lineView;
    private View panel;
    private WindowManager.LayoutParams lineParams;
    private WindowManager.LayoutParams panelParams;
    private boolean lineVisible = true;
    private boolean choosingOrigin = false;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startAsForegroundService();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        addLineLayer();
        addControlPanel();
    }

    private void startAsForegroundService() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "Pool Lines", NotificationManager.IMPORTANCE_LOW));
        }
        Notification notification = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Pool Lines activo")
                    .setContentText("Guía visual de práctica")
                    .setSmallIcon(android.R.drawable.ic_menu_edit).setOngoing(true).build()
                : new Notification.Builder(this)
                    .setContentTitle("Pool Lines activo")
                    .setContentText("Guía visual de práctica")
                    .setSmallIcon(android.R.drawable.ic_menu_edit).setOngoing(true).build();
        startForeground(7, notification);
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void addLineLayer() {
        lineView = new LineView();
        lineParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lineParams.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(lineView, lineParams);
    }

    private void addControlPanel() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(14, 10, 14, 10);
        root.setBackgroundColor(0xEE171C22);

        TextView title = new TextView(this);
        title.setText("POOL LINES  •  LÍNEA CON REBOTE");
        title.setTextColor(Color.WHITE);
        title.setTextSize(13);
        title.setPadding(4, 4, 4, 10);
        root.addView(title);
        title.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;
            @Override public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = panelParams.x; startY = panelParams.y;
                    downX = event.getRawX(); downY = event.getRawY();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    panelParams.x = startX + (int)(event.getRawX() - downX);
                    panelParams.y = startY + (int)(event.getRawY() - downY);
                    windowManager.updateViewLayout(panel, panelParams);
                    return true;
                }
                return true;
            }
        });

        Button markButton = new Button(this);
        markButton.setText("MARCAR BOLA BLANCA");
        root.addView(markButton);
        markButton.setOnClickListener(v -> {
            choosingOrigin = true;
            lineParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            windowManager.updateViewLayout(lineView, lineParams);
            Toast.makeText(this, "Toca el centro de la bola blanca", Toast.LENGTH_SHORT).show();
        });

        TextView angleLabel = new TextView(this);
        angleLabel.setTextColor(Color.WHITE);
        angleLabel.setText("Dirección de tiro");
        root.addView(angleLabel);

        SeekBar angleBar = new SeekBar(this);
        angleBar.setMax(359);
        angleBar.setProgress(0);
        root.addView(angleBar);
        angleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lineView.setAngle(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        Button visibilityButton = new Button(this);
        visibilityButton.setText("OCULTAR LÍNEA");
        root.addView(visibilityButton);
        visibilityButton.setOnClickListener(v -> {
            lineVisible = !lineVisible;
            lineView.setLineVisible(lineVisible);
            visibilityButton.setText(lineVisible ? "OCULTAR LÍNEA" : "MOSTRAR LÍNEA");
        });

        Button closeButton = new Button(this);
        closeButton.setText("CERRAR");
        root.addView(closeButton);
        closeButton.setOnClickListener(v -> stopSelf());

        panel = root;
        panelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = 20; panelParams.y = 100;
        windowManager.addView(panel, panelParams);
    }

    private void finishOriginSelection(float x, float y) {
        lineView.setOrigin(x, y);
        choosingOrigin = false;
        lineParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        windowManager.updateViewLayout(lineView, lineParams);
        Toast.makeText(this, "Origen fijado", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        if (windowManager != null) {
            if (lineView != null) windowManager.removeView(lineView);
            if (panel != null) windowManager.removeView(panel);
        }
        stopForeground(true);
        super.onDestroy();
    }

    private class LineView extends View {
        // Límites iniciales aproximados del paño, expresados como proporción de pantalla.
        private static final float TABLE_LEFT = 0.195f;
        private static final float TABLE_RIGHT = 0.805f;
        private static final float TABLE_TOP = 0.236f;
        private static final float TABLE_BOTTOM = 0.916f;
        private static final int MAX_BOUNCES = 12;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float originX = -1, originY = -1, angle = 0;
        private boolean visible = true;

        LineView() {
            super(PoolOverlayService.this);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(5f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);
        }

        void setOrigin(float x, float y) {
            originX = x; originY = y; invalidate();
        }
        void setAngle(int degrees) {
            angle = (float)Math.toRadians(degrees); invalidate();
        }
        void setLineVisible(boolean show) {
            visible = show; invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!visible || originX < 0 || originY < 0) return;

            float left = getWidth() * TABLE_LEFT;
            float right = getWidth() * TABLE_RIGHT;
            float top = getHeight() * TABLE_TOP;
            float bottom = getHeight() * TABLE_BOTTOM;

            // No dibujar fuera del paño de la mesa.
            if (originX < left || originX > right || originY < top || originY > bottom) return;

            float x = originX, y = originY;
            float dx = (float)Math.cos(angle);
            float dy = (float)Math.sin(angle);
            float remaining = (float)Math.hypot(right - left, bottom - top) * 5.0f;

            for (int bounce = 0; bounce < MAX_BOUNCES && remaining > 1f; bounce++) {
                float tx = Float.POSITIVE_INFINITY;
                float ty = Float.POSITIVE_INFINITY;

                if (dx > 0.00001f) tx = (right - x) / dx;
                else if (dx < -0.00001f) tx = (left - x) / dx;

                if (dy > 0.00001f) ty = (bottom - y) / dy;
                else if (dy < -0.00001f) ty = (top - y) / dy;

                float distance = Math.min(tx, ty);
                if (!Float.isFinite(distance) || distance <= 0f) break;
                float segment = Math.min(distance, remaining);
                float endX = x + dx * segment;
                float endY = y + dy * segment;
                canvas.drawLine(x, y, endX, endY, paint);
                remaining -= segment;

                if (segment < distance) break;

                x = endX;
                y = endY;
                if (Math.abs(tx - ty) < 0.5f) {
                    dx = -dx; dy = -dy;
                } else if (tx < ty) {
                    dx = -dx;
                } else {
                    dy = -dy;
                }
                // Separación mínima para evitar repetir el mismo borde por redondeo.
                x += dx * 0.5f;
                y += dy * 0.5f;
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (choosingOrigin && event.getAction() == MotionEvent.ACTION_UP) {
                finishOriginSelection(event.getX(), event.getY());
                return true;
            }
            return choosingOrigin;
        }
    }
}

package com.poollines.android;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PoolOverlayService extends Service {

    private WindowManager windowManager;
    private LinearLayout panel;
    private LinearLayout content;
    private WindowManager.LayoutParams params;

    private FrameOverlay frameOverlay;
    private WindowManager.LayoutParams frameParams;

    private HandleView[] handles = new HandleView[4];
    private WindowManager.LayoutParams[] handleParams =
            new WindowManager.LayoutParams[4];

    private float left, top, right, bottom;
    private boolean frameInitialized = false;

    private boolean expanded = true;
    private boolean frameVisible = false;

    private final int GREEN = Color.rgb(0, 220, 125);
    private final int DARK = Color.rgb(19, 23, 29);
    private final int CARD = Color.rgb(30, 36, 44);

    private TextView frameButton;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 23 &&
                !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager)
                getSystemService(WINDOW_SERVICE);

        createNotification();
        createPanel();
    }

    private void createNotification() {
        String channel = "poollines_overlay";

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    channel,
                    "PoolLines Panel",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            manager.createNotificationChannel(ch);
        }

        Notification notification;

        if (Build.VERSION.SDK_INT >= 26) {
            notification = new Notification.Builder(this, channel)
                    .setContentTitle("PoolLines")
                    .setContentText("Panel flotante activo")
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("PoolLines")
                    .setContentText("Panel flotante activo")
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .build();
        }

        startForeground(1, notification);
    }

    private void createPanel() {
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setBackground(background(DARK, 12));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(10), dp(8), dp(5), dp(8));
        header.setBackground(background(CARD, 8));

        TextView title = text("POOL LINES", 17, GREEN);
        title.setTypeface(null, Typeface.BOLD);

        TextView minimize = text("  -  ", 22, GREEN);
        minimize.setGravity(Gravity.CENTER);

        TextView close = text("  X  ", 17, Color.WHITE);
        close.setGravity(Gravity.CENTER);

        header.addView(title, new LinearLayout.LayoutParams(
                0, -2, 1
        ));
        header.addView(minimize);
        header.addView(close);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(5), dp(12), dp(5), dp(5));

        TextView status = text(
                "● PANEL FLOTANTE ACTIVO", 12, GREEN
        );
        status.setPadding(dp(5), dp(8), dp(5), dp(12));

        TextView info = text(
                "PoolLines Visual Assistant", 13, Color.LTGRAY
        );

        frameButton = text(
                "AJUSTAR MARCO DE MESA",
                12,
                Color.WHITE
        );
        frameButton.setGravity(Gravity.CENTER);
        frameButton.setPadding(dp(8), dp(12), dp(8), dp(12));
        frameButton.setBackground(background(CARD, 8));

        content.addView(status);
        content.addView(info);
        content.addView(frameButton);

        panel.addView(header);
        panel.addView(content);

        minimize.setOnClickListener(v -> {
            expanded = !expanded;
            content.setVisibility(
                    expanded ? View.VISIBLE : View.GONE
            );
            minimize.setText(expanded ? "  -  " : "  +  ");
            updatePanel();
        });

        close.setOnClickListener(v -> stopSelf());

        frameButton.setOnClickListener(v -> {
            frameVisible = !frameVisible;

            if (frameVisible) {
                showFrame();
                frameButton.setText("MARCO ACTIVO - ARRASTRA LAS ESQUINAS");
                frameButton.setTextColor(GREEN);
            } else {
                hideFrame();
                frameButton.setText("AJUSTAR MARCO DE MESA");
                frameButton.setTextColor(Color.WHITE);
            }
        });

        title.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x;
                    startY = params.y;
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = startX +
                            (int)(event.getRawX() - touchX);
                    params.y = startY +
                            (int)(event.getRawY() - touchY);
                    updatePanel();
                    return true;
                }

                return true;
            }
        });

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                dp(280),
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(15);
        params.y = dp(100);

        windowManager.addView(panel, params);
    }

    private void initializeFrame() {
        if (frameInitialized) return;

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;

        left = width * 0.16f;
        right = width * 0.84f;
        top = height * 0.15f;
        bottom = height * 0.99f;

        frameInitialized = true;
    }

    private void showFrame() {
        if (frameOverlay != null) return;

        initializeFrame();

        frameOverlay = new FrameOverlay();

        frameParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= 26
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        frameParams.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(frameOverlay, frameParams);

        for (int i = 0; i < 4; i++) {
            final int corner = i;

            handles[i] = new HandleView(corner);

            handleParams[i] = new WindowManager.LayoutParams(
                    dp(36),
                    dp(36),
                    Build.VERSION.SDK_INT >= 26
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );

            handleParams[i].gravity = Gravity.TOP | Gravity.START;

            handles[i].setOnTouchListener(
                    new View.OnTouchListener() {
                        private float downX, downY;
                        private float originalX, originalY;

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                downX = event.getRawX();
                                downY = event.getRawY();

                                originalX = getCornerX(corner);
                                originalY = getCornerY(corner);
                                return true;
                            }

                            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                                float newX = originalX +
                                        event.getRawX() - downX;
                                float newY = originalY +
                                        event.getRawY() - downY;

                                moveCorner(corner, newX, newY);
                                return true;
                            }

                            return true;
                        }
                    }
            );

            windowManager.addView(handles[i], handleParams[i]);
        }

        updateFrame();
    }

    private float getCornerX(int corner) {
        return (corner == 0 || corner == 2) ? left : right;
    }

    private float getCornerY(int corner) {
        return (corner == 0 || corner == 1) ? top : bottom;
    }

    private void moveCorner(int corner, float x, float y) {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;

        float min = dp(80);

        if (corner == 0 || corner == 2) {
            left = Math.max(0, Math.min(x, right - min));
        } else {
            right = Math.min(width, Math.max(x, left + min));
        }

        if (corner == 0 || corner == 1) {
            top = Math.max(0, Math.min(y, bottom - min));
        } else {
            bottom = Math.min(height, Math.max(y, top + min));
        }

        updateFrame();
    }

    private void updateFrame() {
        if (frameOverlay != null) {
            frameOverlay.invalidate();
        }

        for (int i = 0; i < 4; i++) {
            if (handles[i] != null && handleParams[i] != null) {
                handleParams[i].x =
                        (int)getCornerX(i) - dp(18);
                handleParams[i].y =
                        (int)getCornerY(i) - dp(18);

                windowManager.updateViewLayout(
                        handles[i], handleParams[i]
                );
            }
        }
    }

    private void hideFrame() {
        for (int i = 0; i < 4; i++) {
            if (handles[i] != null) {
                windowManager.removeView(handles[i]);
                handles[i] = null;
                handleParams[i] = null;
            }
        }

        if (frameOverlay != null) {
            windowManager.removeView(frameOverlay);
            frameOverlay = null;
            frameParams = null;
        }
    }

    private class HandleView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        HandleView(int corner) {
            super(PoolOverlayService.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;

            paint.setColor(GREEN);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, dp(11), paint);

            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            canvas.drawCircle(cx, cy, dp(11), paint);
        }
    }

    private class FrameOverlay extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        FrameOverlay() {
            super(PoolOverlayService.this);
            paint.setColor(GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawRect(left, top, right, bottom, paint);
        }
    }

    private void updatePanel() {
        if (panel != null && panel.isAttachedToWindow()) {
            windowManager.updateViewLayout(panel, params);
        }
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.rgb(48, 59, 68));
        return d;
    }

    private int dp(int value) {
        return (int)(value *
                getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        hideFrame();

        if (panel != null && windowManager != null) {
            windowManager.removeView(panel);
            panel = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

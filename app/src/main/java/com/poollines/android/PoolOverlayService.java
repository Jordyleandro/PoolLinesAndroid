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
                "MARCO DE REFERENCIA: DESACTIVADO",
                12,
                Color.WHITE
        );
        frameButton.setGravity(Gravity.CENTER);
        frameButton.setPadding(dp(8), dp(12), dp(8), dp(12));
        frameButton.setBackground(background(CARD, 8));

        content.addView(status);
        content.addView(info);

        content.addView(frameButton,
                new LinearLayout.LayoutParams(
                        -1, -2
                ));

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
                frameButton.setText(
                        "MARCO DE REFERENCIA: ACTIVADO"
                );
                frameButton.setTextColor(GREEN);
            } else {
                hideFrame();
                frameButton.setText(
                        "MARCO DE REFERENCIA: DESACTIVADO"
                );
                frameButton.setTextColor(Color.WHITE);
            }
        });

        title.setOnTouchListener(new View.OnTouchListener() {
            private int startX;
            private int startY;
            private float touchX;
            private float touchY;

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
                            (int) (event.getRawX() - touchX);

                    params.y = startY +
                            (int) (event.getRawY() - touchY);

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

    private void showFrame() {
        if (frameOverlay != null) return;

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
    }

    private void hideFrame() {
        if (frameOverlay != null) {
            windowManager.removeView(frameOverlay);
            frameOverlay = null;
            frameParams = null;
        }
    }

    private class FrameOverlay extends View {

        private final Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG
        );

        FrameOverlay() {
            super(PoolOverlayService.this);

            paint.setColor(GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float marginX = getWidth() * 0.06f;
            float marginY = getHeight() * 0.20f;

            if (getWidth() > getHeight()) {
                marginY = getHeight() * 0.15f;
            }

            canvas.drawRect(
                    marginX,
                    marginY,
                    getWidth() - marginX,
                    getHeight() - marginY,
                    paint
            );
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
        return (int) (value *
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

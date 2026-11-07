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

    private WindowManager wm;
    private LinearLayout panel;
    private LinearLayout content;
    private WindowManager.LayoutParams panelParams;

    private final int GREEN = Color.rgb(0, 220, 125);
    private final int DARK = Color.rgb(19, 23, 29);
    private final int CARD = Color.rgb(30, 36, 44);

    private boolean expanded = true;
    private boolean frameVisible = false;
    private boolean lineVisible = false;
    private boolean frameInitialized = false;

    private FrameOverlay frameOverlay;
    private WindowManager.LayoutParams frameParams;

    private HandleView[] frameHandles = new HandleView[4];
    private WindowManager.LayoutParams[] frameHandleParams =
            new WindowManager.LayoutParams[4];

    private float left, top, right, bottom;

    private LineOverlay lineOverlay;
    private WindowManager.LayoutParams lineParams;

    private HandleView[] lineHandles = new HandleView[3];
    private WindowManager.LayoutParams[] lineHandleParams =
            new WindowManager.LayoutParams[3];

    private float x1, y1, x2, y2;
    private boolean lineInitialized = false;

    private TextView frameButton;
    private TextView lineButton;
    private TextView clearLineButton;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 23 &&
                !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

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

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
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
        TextView close = text("  X  ", 17, Color.WHITE);

        minimize.setGravity(Gravity.CENTER);
        close.setGravity(Gravity.CENTER);

        header.addView(title,
                new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(minimize);
        header.addView(close);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(5), dp(12), dp(5), dp(5));

        TextView status = text(
                "● PANEL FLOTANTE ACTIVO", 12, GREEN);
        status.setPadding(dp(5), dp(8), dp(5), dp(12));

        TextView info = text(
                "PoolLines Visual Assistant", 13, Color.LTGRAY);

        frameButton = menuButton("MARCO DE REFERENCIA: DESACTIVADO");
        lineButton = menuButton("LÍNEA: DESACTIVADA");
        clearLineButton = menuButton("BORRAR LÍNEA");

        content.addView(status);
        content.addView(info);
        content.addView(frameButton);
        content.addView(lineButton);
        content.addView(clearLineButton);

        panel.addView(header);
        panel.addView(content);

        minimize.setOnClickListener(v -> {
            expanded = !expanded;
            content.setVisibility(
                    expanded ? View.VISIBLE : View.GONE);
            minimize.setText(expanded ? "  -  " : "  +  ");
            updatePanel();
        });

        close.setOnClickListener(v -> stopSelf());

        frameButton.setOnClickListener(v -> {
            frameVisible = !frameVisible;

            if (frameVisible) {
                showFrame();
                frameButton.setText("MARCO: ACTIVADO");
                frameButton.setTextColor(GREEN);
            } else {
                hideFrame();
                frameButton.setText("MARCO DE REFERENCIA: DESACTIVADO");
                frameButton.setTextColor(Color.WHITE);
            }
        });

        lineButton.setOnClickListener(v -> {
            lineVisible = !lineVisible;

            if (lineVisible) {
                showLine();
                lineButton.setText("LÍNEA: ACTIVADA");
                lineButton.setTextColor(GREEN);
            } else {
                hideLine();
                lineButton.setText("LÍNEA: DESACTIVADA");
                lineButton.setTextColor(Color.WHITE);
            }
        });

        clearLineButton.setOnClickListener(v -> {
            hideLine();
            lineInitialized = false;
            lineVisible = false;
            lineButton.setText("LÍNEA: DESACTIVADA");
            lineButton.setTextColor(Color.WHITE);
        });

        title.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = panelParams.x;
                    startY = panelParams.y;
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    panelParams.x = startX +
                            (int)(event.getRawX() - touchX);
                    panelParams.y = startY +
                            (int)(event.getRawY() - touchY);
                    updatePanel();
                    return true;
                }

                return true;
            }
        });

        panelParams = new WindowManager.LayoutParams(
                dp(280),
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = dp(15);
        panelParams.y = dp(100);

        wm.addView(panel, panelParams);
    }

    private TextView menuButton(String value) {
        TextView t = text(value, 12, Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(12), dp(8), dp(12));
        t.setBackground(background(CARD, 8));

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(7);
        t.setLayoutParams(lp);

        return t;
    }

    // MARCO AJUSTABLE

    private void initializeFrame() {
        if (frameInitialized) return;

        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;

        left = w * 0.16f;
        right = w * 0.84f;
        top = h * 0.15f;
        bottom = h * 0.99f;

        frameInitialized = true;
    }

    private void showFrame() {
        if (frameOverlay != null) return;

        initializeFrame();

        frameOverlay = new FrameOverlay();

        frameParams = new WindowManager.LayoutParams(
                -1, -1, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        frameParams.gravity = Gravity.TOP | Gravity.START;
        wm.addView(frameOverlay, frameParams);

        for (int i = 0; i < 4; i++) {
            final int corner = i;
            frameHandles[i] = new HandleView(GREEN);

            frameHandleParams[i] = handleParams();

            frameHandles[i].setOnTouchListener(
                    new View.OnTouchListener() {
                private float downX, downY, originalX, originalY;

                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    if (e.getAction() == MotionEvent.ACTION_DOWN) {
                        downX = e.getRawX();
                        downY = e.getRawY();
                        originalX = cornerX(corner);
                        originalY = cornerY(corner);
                        return true;
                    }

                    if (e.getAction() == MotionEvent.ACTION_MOVE) {
                        moveCorner(corner,
                                originalX + e.getRawX() - downX,
                                originalY + e.getRawY() - downY);
                        return true;
                    }

                    return true;
                }
            });

            wm.addView(frameHandles[i], frameHandleParams[i]);
        }

        updateFrame();
    }

    private float cornerX(int i) {
        return (i == 0 || i == 2) ? left : right;
    }

    private float cornerY(int i) {
        return (i == 0 || i == 1) ? top : bottom;
    }

    private void moveCorner(int i, float x, float y) {
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        float min = dp(80);

        if (i == 0 || i == 2) {
            left = Math.max(0, Math.min(x, right - min));
        } else {
            right = Math.min(w, Math.max(x, left + min));
        }

        if (i == 0 || i == 1) {
            top = Math.max(0, Math.min(y, bottom - min));
        } else {
            bottom = Math.min(h, Math.max(y, top + min));
        }

        updateFrame();
    }

    private void updateFrame() {
        if (frameOverlay != null) frameOverlay.invalidate();

        for (int i = 0; i < 4; i++) {
            if (frameHandles[i] != null) {
                frameHandleParams[i].x =
                        (int)cornerX(i) - dp(18);
                frameHandleParams[i].y =
                        (int)cornerY(i) - dp(18);

                wm.updateViewLayout(
                        frameHandles[i], frameHandleParams[i]);
            }
        }
    }

    private void hideFrame() {
        for (int i = 0; i < 4; i++) {
            if (frameHandles[i] != null) {
                wm.removeView(frameHandles[i]);
                frameHandles[i] = null;
                frameHandleParams[i] = null;
            }
        }

        if (frameOverlay != null) {
            wm.removeView(frameOverlay);
            frameOverlay = null;
            frameParams = null;
        }
    }

    private class FrameOverlay extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        FrameOverlay() {
            super(PoolOverlayService.this);
            p.setColor(GREEN);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(2));
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.drawRect(left, top, right, bottom, p);
        }
    }

    // LÍNEA MANUAL PARA ANOTACIONES VISUALES

    private void initializeLine() {
        if (lineInitialized) return;

        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;

        x1 = w * 0.30f;
        y1 = h * 0.50f;
        x2 = w * 0.70f;
        y2 = h * 0.50f;

        lineInitialized = true;
    }

    private void showLine() {
        if (lineOverlay != null) return;

        initializeLine();

        lineOverlay = new LineOverlay();

        lineParams = new WindowManager.LayoutParams(
                -1, -1, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        lineParams.gravity = Gravity.TOP | Gravity.START;
        wm.addView(lineOverlay, lineParams);

        for (int i = 0; i < 3; i++) {
            final int point = i;

            lineHandles[i] = new HandleView(
                    point == 1 ? Color.WHITE : GREEN);

            lineHandleParams[i] = handleParams();

            lineHandles[i].setOnTouchListener(
                    new View.OnTouchListener() {
                private float downX, downY;
                private float oldX1, oldY1, oldX2, oldY2;

                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    if (e.getAction() == MotionEvent.ACTION_DOWN) {
                        downX = e.getRawX();
                        downY = e.getRawY();

                        oldX1 = x1;
                        oldY1 = y1;
                        oldX2 = x2;
                        oldY2 = y2;
                        return true;
                    }

                    if (e.getAction() == MotionEvent.ACTION_MOVE) {
                        float dx = e.getRawX() - downX;
                        float dy = e.getRawY() - downY;

                        if (point == 0) {
                            x1 = oldX1 + dx;
                            y1 = oldY1 + dy;
                        } else if (point == 2) {
                            x2 = oldX2 + dx;
                            y2 = oldY2 + dy;
                        } else {
                            x1 = oldX1 + dx;
                            y1 = oldY1 + dy;
                            x2 = oldX2 + dx;
                            y2 = oldY2 + dy;
                        }

                        updateLine();
                        return true;
                    }

                    return true;
                }
            });

            wm.addView(lineHandles[i], lineHandleParams[i]);
        }

        updateLine();
    }

    private WindowManager.LayoutParams handleParams() {
        WindowManager.LayoutParams p =
                new WindowManager.LayoutParams(
                dp(36), dp(36), overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        p.gravity = Gravity.TOP | Gravity.START;
        return p;
    }

    private void updateLine() {
        if (lineOverlay != null) lineOverlay.invalidate();

        float centerX = (x1 + x2) / 2f;
        float centerY = (y1 + y2) / 2f;

        float[] px = {x1, centerX, x2};
        float[] py = {y1, centerY, y2};

        for (int i = 0; i < 3; i++) {
            if (lineHandles[i] != null) {
                lineHandleParams[i].x =
                        (int)px[i] - dp(18);
                lineHandleParams[i].y =
                        (int)py[i] - dp(18);

                wm.updateViewLayout(
                        lineHandles[i], lineHandleParams[i]);
            }
        }
    }

    private void hideLine() {
        for (int i = 0; i < 3; i++) {
            if (lineHandles[i] != null) {
                wm.removeView(lineHandles[i]);
                lineHandles[i] = null;
                lineHandleParams[i] = null;
            }
        }

        if (lineOverlay != null) {
            wm.removeView(lineOverlay);
            lineOverlay = null;
            lineParams = null;
        }
    }

    private class LineOverlay extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        LineOverlay() {
            super(PoolOverlayService.this);
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(2));
            p.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.drawLine(x1, y1, x2, y2, p);
        }
    }

    private class HandleView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int color;

        HandleView(int color) {
            super(PoolOverlayService.this);
            this.color = color;
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;

            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            c.drawCircle(cx, cy, dp(11), p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(2));
            p.setColor(Color.WHITE);
            c.drawCircle(cx, cy, dp(11), p);
        }
    }

    private void updatePanel() {
        if (panel != null && panel.isAttachedToWindow()) {
            wm.updateViewLayout(panel, panelParams);
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
        hideLine();
        hideFrame();

        if (panel != null && wm != null) {
            wm.removeView(panel);
            panel = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

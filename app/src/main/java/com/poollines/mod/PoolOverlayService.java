package com.poollines.mod;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PoolOverlayService extends Service {
    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        overlayView = createMenu();
        windowManager.addView(overlayView, params);
    }

    private View createMenu() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xEE111111);
        layout.setPadding(35, 25, 35, 25);

        TextView title = new TextView(this);
        title.setText("👑 8 POOL MOD MENU");
        title.setTextColor(0xFFFFCC00);
        title.setTextSize(14);
        title.setPadding(0, 0, 0, 15);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        CheckBox chkLong = new CheckBox(this);
        chkLong.setText("Líneas Largas");
        chkLong.setTextColor(0xFFFFFFFF);
        chkLong.setChecked(true);
        layout.addView(chkLong);

        CheckBox chkTwo = new CheckBox(this);
        chkTwo.setText("Guía de Rebote");
        chkTwo.setTextColor(0xFFFFFFFF);
        chkTwo.setChecked(true);
        layout.addView(chkTwo);

        layout.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - touchX);
                        params.y = initialY + (int) (event.getRawY() - touchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });

        return layout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }
}

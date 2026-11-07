package com.poollines.android;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
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

    // Variables de control de opciones
    private boolean isLongLineEnabled = true;
    private boolean isTwoLineEnabled = true;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Definir tipo de ventana según la versión de Android
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // Parámetros de la ventana flotante
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

        // Construcción dinámica de la interfaz del menú flotante
        overlayView = createMenuView();

        // Agregar la vista al WindowManager
        windowManager.addView(overlayView, params);
    }

    private View createMenuView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xDD111111); // Fondo oscuro semi-transparente
        layout.setPadding(30, 20, 30, 20);

        // Título del menú
        TextView title = new TextView(this);
        title.setText("👑 MOD MENU POOL");
        title.setTextColor(0xFFFFCC00); // Color dorado
        title.setTextSize(14);
        title.setPadding(0, 0, 0, 15);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        // Opción 1: Línea Larga
        CheckBox chkLongLine = new CheckBox(this);
        chkLongLine.setText("Líneas Largas");
        chkLongLine.setTextColor(0xFFFFFFFF);
        chkLongLine.setChecked(isLongLineEnabled);
        chkLongLine.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isLongLineEnabled = isChecked;
            // Aquí se actualiza el estado de la línea de mira
        });
        layout.addView(chkLongLine);

        // Opción 2: Línea de Rebote (Two Line)
        CheckBox chkTwoLine = new CheckBox(this);
        chkTwoLine.setText("Guía de Rebote");
        chkTwoLine.setTextColor(0xFFFFFFFF);
        chkTwoLine.setChecked(isTwoLineEnabled);
        chkTwoLine.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isTwoLineEnabled = isChecked;
            // Aquí se actualiza el estado de la segunda línea
        });
        layout.addView(chkTwoLine);

        // Lógica para arrastrar el menú con el dedo
        layout.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
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

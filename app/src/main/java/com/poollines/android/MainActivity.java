package com.poollines.android;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private final int GREEN = Color.rgb(0, 220, 125);
    private LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showMenu();
    }

    private void showMenu() {
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(25, 25, 25, 25);
        layout.setBackgroundColor(Color.rgb(13, 16, 21));

        TextView title = new TextView(this);
        title.setText("POOL LINES");
        title.setTextColor(GREEN);
        title.setTextSize(30);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("MOD MENU");
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(13);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 10, 0, 30);

        Button start = new Button(this);
        start.setText("ACTIVAR PANEL FLOTANTE");
        start.setOnClickListener(v -> activarPanel());

        Button practice = new Button(this);
        practice.setText("ABRIR MESA DE PRÁCTICA");
        practice.setOnClickListener(v -> showPractice());

        layout.addView(title);
        layout.addView(info);
        layout.addView(start);
        layout.addView(practice);

        setContentView(layout);
    }

    private void activarPanel() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
            Toast.makeText(this,
                "Activa Permitir sobre otras aplicaciones",
                Toast.LENGTH_LONG).show();
            return;
        }

        Intent service = new Intent(this, PoolOverlayService.class);
        startService(service);

        Toast.makeText(this,
            "Panel activado",
            Toast.LENGTH_SHORT).show();
    }

    private void showPractice() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(12, 12, 12, 12);
        screen.setBackgroundColor(Color.rgb(13, 16, 21));

        TextView heading = new TextView(this);
        heading.setText("MESA DE PRÁCTICA");
        heading.setTextColor(GREEN);
        heading.setTextSize(20);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        heading.setPadding(5, 10, 5, 15);

        PracticeView board = new PracticeView();

        LinearLayout.LayoutParams boardParams =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
            );

        Button undo = new Button(this);
        undo.setText("DESHACER ÚLTIMA LÍNEA");
        undo.setOnClickListener(v -> board.undo());

        Button clear = new Button(this);
        clear.setText("BORRAR TODAS");
        clear.setOnClickListener(v -> board.clearLines());

        Button back = new Button(this);
        back.setText("VOLVER");
        back.setOnClickListener(v -> showMenu());

        screen.addView(heading);
        screen.addView(board, boardParams);
        screen.addView(undo);
        screen.addView(clear);
        screen.addView(back);

        setContentView(screen);
    }

    private class PracticeView extends View {

        private class Line {
            float x1, y1, x2, y2;

            Line(float a, float b, float c, float d) {
                x1 = a;
                y1 = b;
                x2 = c;
                y2 = d;
            }
        }

        private final ArrayList<Line> lines = new ArrayList<>();

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private float startX, startY;
        private float currentX, currentY;
        private boolean drawing = false;

        PracticeView() {
            super(MainActivity.this);
            setBackgroundColor(Color.rgb(13, 16, 21));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float w = getWidth();
            float h = getHeight();

            float margin = 18;
            float tableTop = h * 0.12f;
            float tableBottom = h * 0.88f;

            RectF table = new RectF(
                margin,
                tableTop,
                w - margin,
                tableBottom
            );

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(35, 45, 55));
            canvas.drawRoundRect(table, 24, 24, paint);

            RectF cloth = new RectF(
                margin + 22,
                tableTop + 22,
                w - margin - 22,
                tableBottom - 22
            );

            paint.setColor(Color.rgb(15, 105, 145));
            canvas.drawRoundRect(cloth, 12, 12, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(GREEN);
            canvas.drawRoundRect(table, 24, 24, paint);

            paint.setStyle(Paint.Style.FILL);

            float pocket = 15;
            float[][] pockets = {
                {cloth.left, cloth.top},
                {w / 2, cloth.top},
                {cloth.right, cloth.top},
                {cloth.left, cloth.bottom},
                {w / 2, cloth.bottom},
                {cloth.right, cloth.bottom}
            };

            paint.setColor(Color.BLACK);

            for (float[] p : pockets) {
                canvas.drawCircle(p[0], p[1], pocket, paint);
            }

            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);

            for (Line line : lines) {
                canvas.drawLine(
                    line.x1, line.y1,
                    line.x2, line.y2,
                    paint
                );
            }

            if (drawing) {
                paint.setColor(GREEN);
                canvas.drawLine(
                    startX, startY,
                    currentX, currentY,
                    paint
                );
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextSize(13);
            canvas.drawText(
                "Arrastra el dedo para dibujar segmentos",
                margin + 5,
                h - 10,
                paint
            );
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = x;
                    startY = y;
                    currentX = x;
                    currentY = y;
                    drawing = true;
                    invalidate();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    currentX = x;
                    currentY = y;
                    invalidate();
                    return true;

                case MotionEvent.ACTION_UP:
                    currentX = x;
                    currentY = y;

                    if (Math.abs(currentX - startX) > 5 ||
                        Math.abs(currentY - startY) > 5) {
                        lines.add(new Line(
                            startX, startY,
                            currentX, currentY
                        ));
                    }

                    drawing = false;
                    invalidate();
                    return true;
            }

            return true;
        }

        void undo() {
            if (!lines.isEmpty()) {
                lines.remove(lines.size() - 1);
                invalidate();
            }
        }

        void clearLines() {
            lines.clear();
            invalidate();
        }
    }
}

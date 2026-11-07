package com.poollines.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.ScrollView;

public class MainActivity extends Activity {

    private final int BG = Color.rgb(13, 16, 21);
    private final int PANEL = Color.rgb(24, 29, 36);
    private final int GREEN = Color.rgb(0, 220, 125);
    private final int WHITE = Color.WHITE;
    private final int GRAY = Color.rgb(155, 165, 175);

    private LinearLayout menu;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(20, 25, 20, 25);

        scroll.addView(menu);

        TextView title = text("POOL LINES", 27, GREEN);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        menu.addView(title);

        TextView subtitle = text("VISUAL ASSISTANT  •  VERSION 1.0", 10, GRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 5, 0, 22);
        menu.addView(subtitle);

        LinearLayout header = card();
        header.setOrientation(LinearLayout.VERTICAL);

        TextView heading = text("CONTROL PANEL", 15, WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        header.addView(heading);

        status = text("●  SISTEMA INACTIVO", 12, GREEN);
        status.setPadding(0, 12, 0, 0);
        header.addView(status);

        menu.addView(header);

        section("GENERAL");

        addSwitch("Mostrar líneas", false);
        addSwitch("Asistencia visual", false);
        addSwitch("Detección visual", false);

        section("CALIBRACIÓN");

        addButton("CALIBRAR MESA");
        addButton("AJUSTAR ESQUINAS");

        section("PRESENTACIÓN");

        addSwitch("Mostrar panel", true);
        addSwitch("Líneas visibles", true);

        TextView footer = text("POOL LINES  •  MOD MENU STYLE", 10, GRAY);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 25, 0, 5);
        menu.addView(footer);

        setContentView(scroll);
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(18, 16, 18, 16);
        box.setBackground(shape(PANEL, 12));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            -1, -2
        );
        p.bottomMargin = 12;
        box.setLayoutParams(p);
        return box;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius * getResources().getDisplayMetrics().density);
        d.setStroke(1, Color.rgb(43, 53, 62));
        return d;
    }

    private void section(String name) {
        TextView t = text(name, 12, GREEN);
        t.setTypeface(null, Typeface.BOLD);
        t.setPadding(5, 18, 5, 9);
        menu.addView(t);
    }

    private void addSwitch(String name, boolean checked) {
        LinearLayout box = card();

        Switch control = new Switch(this);
        control.setText(name);
        control.setTextColor(WHITE);
        control.setTextSize(14);
        control.setChecked(checked);
        control.setPadding(2, 3, 2, 3);

        box.addView(control);
        menu.addView(box);
    }

    private void addButton(String name) {
        TextView button = text(name, 13, GREEN);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(15, 17, 15, 17);
        button.setBackground(shape(PANEL, 10));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            -1, -2
        );
        p.bottomMargin = 10;
        button.setLayoutParams(p);

        menu.addView(button);
    }
}

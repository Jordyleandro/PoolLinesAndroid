package com.poollines.android;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private int GREEN = Color.rgb(0, 220, 125);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
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

        layout.addView(title);
        layout.addView(info);
        layout.addView(start);

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
}

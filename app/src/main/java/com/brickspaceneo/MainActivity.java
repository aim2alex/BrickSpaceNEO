package com.brickspaceneo;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.view.Window window = getWindow();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        } else {
            window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);

            window.getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        setContentView(new GameView(this));
    }
}

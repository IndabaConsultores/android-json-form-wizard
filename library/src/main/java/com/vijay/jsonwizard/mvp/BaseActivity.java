package com.vijay.jsonwizard.mvp;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Created by vijay on 4/21/15.
 */
public abstract class BaseActivity<VS extends ViewState> extends AppCompatActivity {
    // @Icicle
    protected VS mViewState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Icepick.restoreInstanceState(this, savedInstanceState);
        if (savedInstanceState == null) {
            mViewState = createViewState();
            mViewState.setSavedInstance(false);
        } else {
            mViewState.setSavedInstance(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Icepick.saveInstanceState(this, outState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setEdgeToEdge();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        setEdgeToEdge();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        setEdgeToEdge();
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        super.addContentView(view, params);
    }

    private void setEdgeToEdge() {
        View rootView = findViewById(android.R.id.content);
        Window window = getWindow();

        EdgeToEdge.enable(this);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false);

            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

                v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));

                return insets;
            });

        } else {
            WindowInsetsControllerCompat controller =
                    new WindowInsetsControllerCompat(window, rootView);
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );

            WindowCompat.setDecorFitsSystemWindows(window, true);
        }
    }

    protected abstract VS createViewState();

    public VS getViewState() {
        return mViewState;
    }
}

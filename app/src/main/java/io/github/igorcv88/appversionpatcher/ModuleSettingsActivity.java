package io.github.igorcv88.appversionpatcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class ModuleSettingsActivity extends Activity {
    private boolean updatingLauncherToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureEdgeToEdge();
        View content = buildContentView();
        setContentView(content);
        content.requestApplyInsets();
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color(R.color.background));
        root.setClipToPadding(false);
        applySystemBarInsets(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setContentDescription(getString(R.string.app_icon_content_description));
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(56), dp(56));
        logoParams.setMarginEnd(dp(14));
        header.addView(logo, logoParams);

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(22);
        title.setTextColor(color(R.color.text_primary));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headerText.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.module_settings_tagline);
        subtitle.setTextSize(13);
        subtitle.setTextColor(color(R.color.text_secondary));
        subtitle.setPadding(0, dp(2), 0, 0);
        headerText.addView(subtitle);

        header.addView(headerText, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));
        root.addView(header);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.bg_panel);
        panel.setPadding(dp(16), dp(14), dp(16), dp(16));

        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        panelParams.topMargin = dp(18);
        root.addView(panel, panelParams);

        TextView sectionTitle = new TextView(this);
        sectionTitle.setText(R.string.launcher_visibility_title);
        sectionTitle.setTextSize(16);
        sectionTitle.setTextColor(color(R.color.text_primary));
        sectionTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        panel.addView(sectionTitle);

        CheckBox hideLauncher = new CheckBox(this);
        styleCheckBox(hideLauncher);
        hideLauncher.setText(R.string.hide_launcher_icon);
        hideLauncher.setChecked(isLauncherHidden());

        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        toggleParams.topMargin = dp(6);
        panel.addView(hideLauncher, toggleParams);

        TextView explanation = new TextView(this);
        explanation.setText(R.string.hide_launcher_description);
        explanation.setTextSize(12);
        explanation.setTextColor(color(R.color.text_secondary));
        explanation.setLineSpacing(0, 1.08f);
        panel.addView(explanation);

        hideLauncher.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingLauncherToggle) {
                return;
            }
            try {
                setLauncherHidden(isChecked);
                Toast.makeText(
                        this,
                        isChecked
                                ? R.string.launcher_icon_hidden
                                : R.string.launcher_icon_shown,
                        Toast.LENGTH_LONG
                ).show();
            } catch (Throwable throwable) {
                updatingLauncherToggle = true;
                buttonView.setChecked(!isChecked);
                updatingLauncherToggle = false;
                Toast.makeText(
                        this,
                        getString(
                                R.string.launcher_icon_change_failed,
                                throwableMessage(throwable)
                        ),
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        Button openConfiguration = new Button(this);
        openConfiguration.setText(R.string.open_configuration);
        openConfiguration.setAllCaps(false);
        openConfiguration.setTextSize(14);
        openConfiguration.setTextColor(Color.WHITE);
        openConfiguration.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        openConfiguration.setBackgroundTintList(
                ColorStateList.valueOf(color(R.color.accent))
        );
        openConfiguration.setMinHeight(dp(50));
        openConfiguration.setOnClickListener(view -> {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = dp(18);
        root.addView(openConfiguration, buttonParams);

        return root;
    }

    private boolean isLauncherHidden() {
        int state = getPackageManager().getComponentEnabledSetting(launcherAlias());
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
    }

    private void setLauncherHidden(boolean hidden) {
        getPackageManager().setComponentEnabledSetting(
                launcherAlias(),
                hidden
                        ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
        );
    }

    private ComponentName launcherAlias() {
        return new ComponentName(
                getPackageName(),
                getPackageName() + ".LauncherAlias"
        );
    }

    private void configureEdgeToEdge() {
        boolean night = isNightMode();
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int appearanceMask =
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(
                        night ? 0 : appearanceMask,
                        appearanceMask
                );
            }
        } else {
            int flags =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (!night) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    @SuppressWarnings("deprecation")
    private void applySystemBarInsets(View root) {
        int horizontal = dp(18);
        int top = dp(16);
        int bottom = dp(16);

        root.setPadding(horizontal, top, horizontal, bottom);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets systemBars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
                );
                view.setPadding(
                        horizontal + systemBars.left,
                        top + systemBars.top,
                        horizontal + systemBars.right,
                        bottom + systemBars.bottom
                );
            } else {
                view.setPadding(
                        horizontal + insets.getSystemWindowInsetLeft(),
                        top + insets.getSystemWindowInsetTop(),
                        horizontal + insets.getSystemWindowInsetRight(),
                        bottom + insets.getSystemWindowInsetBottom()
                );
            }
            return insets;
        });
    }

    private void styleCheckBox(CheckBox checkBox) {
        checkBox.setTextSize(14);
        checkBox.setTextColor(color(R.color.text_primary));
        checkBox.setButtonTintList(ColorStateList.valueOf(color(R.color.accent)));
        checkBox.setMinHeight(dp(44));
    }

    private boolean isNightMode() {
        int nightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private int color(int resourceId) {
        return getColor(resourceId);
    }

    private String throwableMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.trim().isEmpty()
                ? throwable.getClass().getSimpleName()
                : message;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

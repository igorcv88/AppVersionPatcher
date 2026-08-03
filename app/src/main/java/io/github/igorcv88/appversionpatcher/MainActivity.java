package io.github.igorcv88.appversionpatcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.github.libxposed.service.XposedService;

public final class MainActivity extends Activity implements XposedServiceClient.Listener {
    private final List<AppEntry> allApps = new ArrayList<>();
    private final Set<String> scope = new HashSet<>();
    private final Collator collator = Collator.getInstance();

    private AppListAdapter adapter;
    private SharedPreferences preferences;
    private XposedService xposedService;
    private EditText searchInput;
    private CheckBox showSystemApps;
    private CheckBox showOnlyScope;
    private ProgressBar progressBar;
    private TextView emptyView;
    private TextView serviceStatus;
    private boolean appsLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureEdgeToEdge();
        View content = buildContentView();
        setContentView(content);
        content.requestApplyInsets();
        loadApps();
    }

    @Override
    protected void onStart() {
        super.onStart();
        XposedServiceClient.addListener(this);
    }

    @Override
    protected void onStop() {
        XposedServiceClient.removeListener(this);
        super.onStop();
    }

    @Override
    public void onServiceAvailable(XposedService service) {
        runOnUiThread(() -> bindService(service));
    }

    @Override
    public void onServiceUnavailable() {
        runOnUiThread(() -> {
            xposedService = null;
            preferences = null;
            scope.clear();
            adapter.setState(null, Collections.emptySet());
            serviceStatus.setText(R.string.service_disconnected);
            applyFilter();
        });
    }

    private void bindService(XposedService service) {
        try {
            int api = service.getApiVersion();
            if (api < 101) {
                serviceStatus.setText(getString(R.string.service_api_unsupported, api));
                return;
            }

            SharedPreferences remote = service.getRemotePreferences(ConfigStore.PREFS_NAME);
            ConfigStore.initializeRemote(this, remote);

            xposedService = service;
            preferences = remote;
            scope.clear();
            scope.addAll(service.getScope());
            if (scope.isEmpty() && ConfigStore.configuredPackages(preferences).isEmpty()) {
                showOnlyScope.setChecked(false);
            }
            adapter.setState(preferences, scope);
            serviceStatus.setText(getResources().getQuantityString(
                    R.plurals.service_connected,
                    scope.size(),
                    service.getFrameworkName(),
                    service.getFrameworkVersion(),
                    api,
                    scope.size()
            ));
            applyFilter();
        } catch (Throwable throwable) {
            xposedService = null;
            preferences = null;
            scope.clear();
            adapter.setState(null, Collections.emptySet());
            serviceStatus.setText(getString(
                    R.string.service_connection_failed,
                    throwableMessage(throwable)
            ));
            applyFilter();
        }
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
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(52), dp(52));
        logoParams.setMarginEnd(dp(12));
        header.addView(logo, logoParams);

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(22);
        title.setTextColor(color(R.color.text_primary));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headerText.addView(title);

        TextView description = new TextView(this);
        description.setText(R.string.app_tagline);
        description.setTextSize(13);
        description.setTextColor(color(R.color.text_secondary));
        description.setPadding(0, dp(2), 0, 0);
        headerText.addView(description);

        header.addView(headerText, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        headerParams.bottomMargin = dp(12);
        root.addView(header, headerParams);

        serviceStatus = new TextView(this);
        serviceStatus.setText(R.string.service_waiting);
        serviceStatus.setTextSize(12);
        serviceStatus.setTextColor(color(R.color.accent));
        serviceStatus.setBackgroundResource(R.drawable.bg_status);
        serviceStatus.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.addView(serviceStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        searchInput = new EditText(this);
        searchInput.setHint(R.string.search_hint);
        searchInput.setSingleLine(true);
        searchInput.setInputType(InputType.TYPE_CLASS_TEXT);
        searchInput.setTextSize(15);
        searchInput.setTextColor(color(R.color.text_primary));
        searchInput.setHintTextColor(color(R.color.text_secondary));
        searchInput.setBackgroundResource(R.drawable.bg_input);
        searchInput.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_search,
                0,
                0,
                0
        );
        searchInput.setCompoundDrawablePadding(dp(10));
        searchInput.setCompoundDrawableTintList(
                ColorStateList.valueOf(color(R.color.text_secondary))
        );
        searchInput.setPadding(dp(14), 0, dp(14), 0);
        searchInput.setMinHeight(dp(52));

        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        searchParams.topMargin = dp(12);
        root.addView(searchInput, searchParams);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.VERTICAL);
        filters.setPadding(0, dp(4), 0, dp(4));

        showOnlyScope = new CheckBox(this);
        styleCheckBox(showOnlyScope);
        showOnlyScope.setText(R.string.filter_scope_only);
        showOnlyScope.setChecked(true);
        filters.addView(showOnlyScope);

        showSystemApps = new CheckBox(this);
        styleCheckBox(showSystemApps);
        showSystemApps.setText(R.string.filter_system_apps);
        filters.addView(showSystemApps);

        root.addView(filters, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ListView listView = new ListView(this);
        adapter = new AppListAdapter(this);
        listView.setAdapter(adapter);
        listView.setBackgroundColor(Color.TRANSPARENT);
        listView.setDivider(new ColorDrawable(color(R.color.outline)));
        listView.setDividerHeight(1);
        listView.setClipToPadding(false);
        listView.setPadding(0, dp(4), 0, dp(8));
        listView.setScrollbarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (preferences == null || xposedService == null) {
                Toast.makeText(this, R.string.wait_for_lsposed, Toast.LENGTH_SHORT).show();
                return;
            }
            showEditor(adapter.getItem(position));
        });

        emptyView = new TextView(this);
        emptyView.setText(R.string.empty_apps);
        emptyView.setTextSize(14);
        emptyView.setTextColor(color(R.color.text_secondary));
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(24), dp(24), dp(24), dp(24));
        emptyView.setVisibility(View.GONE);

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(color(R.color.accent)));

        FrameLayout listContainer = new FrameLayout(this);
        listContainer.setBackgroundResource(R.drawable.bg_panel);
        listContainer.setClipToOutline(true);
        listContainer.addView(listView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        FrameLayout.LayoutParams emptyParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        listContainer.addView(emptyView, emptyParams);

        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        listContainer.addView(progressBar, progressParams);

        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        );
        listParams.topMargin = dp(6);
        root.addView(listContainer, listParams);

        searchInput.addTextChangedListener(new SimpleTextWatcher(this::applyFilter));
        showSystemApps.setOnCheckedChangeListener((buttonView, isChecked) -> applyFilter());
        showOnlyScope.setOnCheckedChangeListener((buttonView, isChecked) -> applyFilter());
        return root;
    }

    private void loadApps() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            PackageManager packageManager = getPackageManager();
            List<ApplicationInfo> applications;
            if (Build.VERSION.SDK_INT >= 33) {
                applications = packageManager.getInstalledApplications(
                        PackageManager.ApplicationInfoFlags.of(0)
                );
            } else {
                applications = packageManager.getInstalledApplications(0);
            }

            List<AppEntry> loaded = new ArrayList<>();
            for (ApplicationInfo info : applications) {
                if (!info.enabled || getPackageName().equals(info.packageName)) {
                    continue;
                }
                try {
                    PackageInfo packageInfo;
                    if (Build.VERSION.SDK_INT >= 33) {
                        packageInfo = packageManager.getPackageInfo(
                                info.packageName,
                                PackageManager.PackageInfoFlags.of(0)
                        );
                    } else {
                        packageInfo = packageManager.getPackageInfo(info.packageName, 0);
                    }
                    boolean system = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0 &&
                            (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
                    String label = String.valueOf(packageManager.getApplicationLabel(info));
                    String versionName = packageInfo.versionName == null ? "?" : packageInfo.versionName;
                    long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                            ? packageInfo.getLongVersionCode()
                            : packageInfo.versionCode;
                    loaded.add(new AppEntry(
                            label,
                            info.packageName,
                            packageManager.getApplicationIcon(info),
                            system,
                            versionName,
                            versionCode
                    ));
                } catch (PackageManager.NameNotFoundException ignored) {
                    // The package changed while the list was loading.
                }
            }

            runOnUiThread(() -> {
                allApps.clear();
                allApps.addAll(loaded);
                appsLoaded = true;
                progressBar.setVisibility(View.GONE);
                applyFilter();
            });
        }, "app-list-loader").start();
    }

    private void applyFilter() {
        if (adapter == null) {
            return;
        }

        String query = searchInput == null
                ? ""
                : searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        boolean includeSystem = showSystemApps != null && showSystemApps.isChecked();
        boolean onlyScope = showOnlyScope != null && showOnlyScope.isChecked();
        Set<String> configured = ConfigStore.configuredPackages(preferences);

        List<AppEntry> filtered = new ArrayList<>();
        for (AppEntry entry : allApps) {
            boolean isConfigured = configured.contains(entry.packageName);
            boolean isInScope = scope.contains(entry.packageName);
            if (!includeSystem && entry.systemApp && !isConfigured) {
                continue;
            }
            if (onlyScope && !isInScope && !isConfigured) {
                continue;
            }
            if (!query.isEmpty() &&
                    !entry.label.toLowerCase(Locale.ROOT).contains(query) &&
                    !entry.packageName.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            filtered.add(entry);
        }

        filtered.sort((left, right) -> {
            int leftRank = listRank(left, configured);
            int rightRank = listRank(right, configured);
            if (leftRank != rightRank) {
                return Integer.compare(leftRank, rightRank);
            }
            int labelComparison = collator.compare(left.label, right.label);
            return labelComparison != 0
                    ? labelComparison
                    : left.packageName.compareTo(right.packageName);
        });

        adapter.setState(preferences, scope);
        adapter.replace(filtered);
        emptyView.setVisibility(
                appsLoaded && filtered.isEmpty() ? View.VISIBLE : View.GONE
        );
    }

    private int listRank(AppEntry entry, Set<String> configured) {
        if (configured.contains(entry.packageName)) {
            return 0;
        }
        return scope.contains(entry.packageName) ? 1 : 2;
    }

    private void showEditor(AppEntry entry) {
        VersionConfig existing = ConfigStore.read(preferences, entry.packageName);
        boolean currentlyInScope = scope.contains(entry.packageName);

        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(20), dp(8), dp(20), dp(8));

        TextView packageView = new TextView(this);
        packageView.setText(getString(
                R.string.installed_info,
                entry.packageName,
                entry.installedVersionName,
                entry.installedVersionCode,
                getString(currentlyInScope
                        ? R.string.scope_included
                        : R.string.scope_not_included)
        ));
        packageView.setTextSize(13);
        packageView.setTextColor(color(R.color.text_secondary));
        LinearLayout.LayoutParams packageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        packageParams.bottomMargin = dp(12);
        fields.addView(packageView, packageParams);

        EditText versionName = new EditText(this);
        styleInput(versionName);
        versionName.setHint(R.string.version_name_hint);
        versionName.setInputType(InputType.TYPE_CLASS_TEXT);
        versionName.setText(existing == null ? entry.installedVersionName : existing.versionName);
        LinearLayout.LayoutParams versionNameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        versionNameParams.bottomMargin = dp(10);
        fields.addView(versionName, versionNameParams);

        EditText versionCode = new EditText(this);
        styleInput(versionCode);
        versionCode.setHint(R.string.version_code_hint);
        versionCode.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (existing != null && existing.versionCode != null) {
            versionCode.setText(Long.toString(existing.versionCode));
        }
        LinearLayout.LayoutParams versionCodeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        versionCodeParams.bottomMargin = dp(8);
        fields.addView(versionCode, versionCodeParams);

        CheckBox packageManagerHook = new CheckBox(this);
        styleCheckBox(packageManagerHook);
        packageManagerHook.setText(R.string.hook_package_manager);
        packageManagerHook.setChecked(existing == null || existing.hookPackageManager);
        fields.addView(packageManagerHook);

        CheckBox reactNativeHook = new CheckBox(this);
        styleCheckBox(reactNativeHook);
        reactNativeHook.setText(R.string.hook_rn_device_info);
        reactNativeHook.setChecked(existing == null || existing.hookReactNativeDeviceInfo);
        fields.addView(reactNativeHook);

        TextView note = new TextView(this);
        note.setText(currentlyInScope
                ? R.string.scope_already_included
                : R.string.scope_will_request);
        note.setTextSize(12);
        note.setTextColor(color(R.color.text_secondary));
        note.setPadding(0, dp(8), 0, 0);
        fields.addView(note);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(fields);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(entry.label)
                .setView(scrollView)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null)
                .setNeutralButton(
                        existing == null
                                ? R.string.no_configuration
                                : R.string.action_remove,
                        null
                )
                .create();

        dialog.setOnShowListener(ignored -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(view -> {
                String name = versionName.getText().toString().trim();
                if (name.isEmpty()) {
                    versionName.setError(getString(R.string.error_version_name_required));
                    return;
                }

                Long code = null;
                String codeText = versionCode.getText().toString().trim();
                if (!codeText.isEmpty()) {
                    try {
                        code = Long.parseLong(codeText);
                        if (!ConfigStore.isSupportedVersionCode(code)) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException exception) {
                        versionCode.setError(getString(
                                R.string.error_version_code_range,
                                ConfigStore.MAX_VERSION_CODE
                        ));
                        return;
                    }
                }

                if (!packageManagerHook.isChecked() && !reactNativeHook.isChecked()) {
                    Toast.makeText(
                            this,
                            R.string.error_enable_hook,
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                boolean saved = ConfigStore.save(preferences, new VersionConfig(
                        entry.packageName,
                        name,
                        code,
                        packageManagerHook.isChecked(),
                        reactNativeHook.isChecked()
                ));
                if (!saved) {
                    Toast.makeText(
                            this,
                            R.string.save_not_confirmed,
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                applyFilter();
                dialog.dismiss();
                if (scope.contains(entry.packageName)) {
                    Toast.makeText(
                            this,
                            R.string.save_success_restart,
                            Toast.LENGTH_LONG
                    ).show();
                } else {
                    requestScope(entry.packageName);
                }
            });

            Button removeButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            removeButton.setEnabled(existing != null);
            removeButton.setOnClickListener(view -> {
                if (!ConfigStore.remove(preferences, entry.packageName)) {
                    Toast.makeText(
                            this,
                            R.string.remove_not_confirmed,
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }
                applyFilter();
                Toast.makeText(
                        this,
                        R.string.configuration_removed,
                        Toast.LENGTH_LONG
                ).show();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void requestScope(String packageName) {
        XposedService service = xposedService;
        if (service == null) {
            Toast.makeText(
                    this,
                    R.string.scope_service_disconnected,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        try {
            service.requestScope(
                    Collections.singletonList(packageName),
                    new XposedService.OnScopeEventListener() {
                        @Override
                        public void onScopeRequestApproved(List<String> approved) {
                            runOnUiThread(() -> {
                                scope.addAll(approved);
                                adapter.setState(preferences, scope);
                                applyFilter();
                                Toast.makeText(
                                        MainActivity.this,
                                        approved.contains(packageName)
                                                ? R.string.scope_approved
                                                : R.string.scope_not_added,
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                        }

                        @Override
                        public void onScopeRequestFailed(String message) {
                            runOnUiThread(() -> Toast.makeText(
                                    MainActivity.this,
                                    getString(R.string.scope_request_failed, message),
                                    Toast.LENGTH_LONG
                            ).show());
                        }
                    }
            );
        } catch (Throwable throwable) {
            Toast.makeText(
                    this,
                    getString(
                            R.string.scope_request_failed,
                            throwableMessage(throwable)
                    ),
                    Toast.LENGTH_LONG
            ).show();
        }
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
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(
                        night ? 0 : appearanceMask,
                        appearanceMask
                );
            }
        } else {
            int flags =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (!night) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    @SuppressWarnings("deprecation")
    private void applySystemBarInsets(View root) {
        int horizontal = dp(16);
        int top = dp(12);
        int bottom = dp(10);

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

    private void styleInput(EditText input) {
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setTextColor(color(R.color.text_primary));
        input.setHintTextColor(color(R.color.text_secondary));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setMinHeight(dp(52));
    }

    private void styleCheckBox(CheckBox checkBox) {
        checkBox.setTextSize(13);
        checkBox.setTextColor(color(R.color.text_primary));
        checkBox.setButtonTintList(ColorStateList.valueOf(color(R.color.accent)));
        checkBox.setMinHeight(dp(40));
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

    private static final class SimpleTextWatcher implements TextWatcher {
        private final Runnable action;

        private SimpleTextWatcher(Runnable action) {
            this.action = action;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            action.run();
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}

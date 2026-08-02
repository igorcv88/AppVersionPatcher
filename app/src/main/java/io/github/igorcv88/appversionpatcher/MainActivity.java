package io.github.igorcv88.appversionpatcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
    private final Collator collator = Collator.getInstance(new Locale("pt", "BR"));

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
        setContentView(buildContentView());
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
            serviceStatus.setText("Serviço do LSPosed desconectado.");
            applyFilter();
        });
    }

    private void bindService(XposedService service) {
        try {
            int api = service.getApiVersion();
            if (api < 101) {
                serviceStatus.setText("LSPosed API " + api + " detectada; este módulo requer API 101.");
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
            serviceStatus.setText(
                    service.getFrameworkName() + " " + service.getFrameworkVersion() +
                            " · API " + api + " · " + scope.size() + " app(s) no escopo"
            );
            applyFilter();
        } catch (Throwable throwable) {
            xposedService = null;
            preferences = null;
            scope.clear();
            adapter.setState(null, Collections.emptySet());
            serviceStatus.setText("Falha ao conectar ao serviço do LSPosed: " + throwable.getMessage());
            applyFilter();
        }
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(8));

        TextView title = new TextView(this);
        title.setText("App Version Patcher");
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView description = new TextView(this);
        description.setText(
                "API moderna do LSPosed. Apps configurados ficam no topo; ao salvar um app fora " +
                        "do escopo, o módulo solicita sua inclusão. Reinicie o processo-alvo após salvar."
        );
        description.setTextSize(14);
        description.setPadding(0, dp(6), 0, dp(8));
        root.addView(description);

        serviceStatus = new TextView(this);
        serviceStatus.setText("Aguardando serviço do LSPosed…");
        serviceStatus.setTextSize(13);
        serviceStatus.setPadding(0, 0, 0, dp(10));
        root.addView(serviceStatus);

        searchInput = new EditText(this);
        searchInput.setHint("Buscar aplicativo ou pacote");
        searchInput.setSingleLine(true);
        searchInput.setInputType(InputType.TYPE_CLASS_TEXT);
        root.addView(searchInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        showOnlyScope = new CheckBox(this);
        showOnlyScope.setText("Mostrar somente aplicativos no escopo");
        showOnlyScope.setChecked(true);
        root.addView(showOnlyScope);

        showSystemApps = new CheckBox(this);
        showSystemApps.setText("Mostrar aplicativos do sistema");
        root.addView(showSystemApps);

        progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = dp(24);
        root.addView(progressBar, progressParams);

        ListView listView = new ListView(this);
        adapter = new AppListAdapter(this);
        listView.setAdapter(adapter);
        listView.setDividerHeight(1);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (preferences == null || xposedService == null) {
                Toast.makeText(this, "Aguarde a conexão com o LSPosed.", Toast.LENGTH_SHORT).show();
                return;
            }
            showEditor(adapter.getItem(position));
        });

        emptyView = new TextView(this);
        emptyView.setText("Nenhum aplicativo encontrado.");
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));
        listContainer.addView(emptyView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(listContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

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
                    // O pacote foi alterado durante o carregamento.
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
        fields.setPadding(dp(20), dp(4), dp(20), dp(4));

        TextView packageView = new TextView(this);
        packageView.setText(
                entry.packageName + "\nInstalada: " + entry.installedVersionName +
                        " (code " + entry.installedVersionCode + ")\nEscopo: " +
                        (currentlyInScope ? "incluído" : "não incluído")
        );
        packageView.setTextSize(13);
        packageView.setPadding(0, 0, 0, dp(10));
        fields.addView(packageView);

        EditText versionName = new EditText(this);
        versionName.setHint("versionName, por exemplo 1.2.3");
        versionName.setSingleLine(true);
        versionName.setInputType(InputType.TYPE_CLASS_TEXT);
        versionName.setText(existing == null ? entry.installedVersionName : existing.versionName);
        fields.addView(versionName);

        EditText versionCode = new EditText(this);
        versionCode.setHint("versionCode opcional");
        versionCode.setSingleLine(true);
        versionCode.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (existing != null && existing.versionCode != null) {
            versionCode.setText(Long.toString(existing.versionCode));
        }
        fields.addView(versionCode);

        CheckBox packageManagerHook = new CheckBox(this);
        packageManagerHook.setText("Falsificar PackageManager (Android geral)");
        packageManagerHook.setChecked(existing == null || existing.hookPackageManager);
        fields.addView(packageManagerHook);

        CheckBox reactNativeHook = new CheckBox(this);
        reactNativeHook.setText("Falsificar RNDeviceInfo (React Native)");
        reactNativeHook.setChecked(existing == null || existing.hookReactNativeDeviceInfo);
        fields.addView(reactNativeHook);

        TextView note = new TextView(this);
        note.setText(
                currentlyInScope
                        ? "O aplicativo já está no escopo."
                        : "Ao salvar, o LSPosed solicitará a inclusão deste aplicativo no escopo."
        );
        note.setTextSize(12);
        note.setPadding(0, dp(8), 0, 0);
        fields.addView(note);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(fields);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(entry.label)
                .setView(scrollView)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .setNeutralButton(existing == null ? "Sem configuração" : "Remover", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(view -> {
                String name = versionName.getText().toString().trim();
                if (name.isEmpty()) {
                    versionName.setError("Informe a versão.");
                    return;
                }

                Long code = null;
                String codeText = versionCode.getText().toString().trim();
                if (!codeText.isEmpty()) {
                    try {
                        code = Long.parseLong(codeText);
                        if (code < 0) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException exception) {
                        versionCode.setError("Use um número inteiro não negativo.");
                        return;
                    }
                }

                if (!packageManagerHook.isChecked() && !reactNativeHook.isChecked()) {
                    Toast.makeText(this, "Ative pelo menos um método de hook.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "O LSPosed não confirmou a gravação.", Toast.LENGTH_LONG).show();
                    return;
                }

                applyFilter();
                dialog.dismiss();
                if (scope.contains(entry.packageName)) {
                    Toast.makeText(
                            this,
                            "Salvo. Force a parada e reabra o aplicativo-alvo.",
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
                    Toast.makeText(this, "O LSPosed não confirmou a remoção.", Toast.LENGTH_LONG).show();
                    return;
                }
                applyFilter();
                Toast.makeText(
                        this,
                        "Configuração removida. O aplicativo permanece no escopo.",
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
                    "Configuração salva, mas o serviço desconectou antes de solicitar o escopo.",
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
                                                ? "Escopo aprovado. Force a parada e reabra o aplicativo-alvo."
                                                : "Configuração salva, mas o pacote não foi adicionado ao escopo.",
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                        }

                        @Override
                        public void onScopeRequestFailed(String message) {
                            runOnUiThread(() -> Toast.makeText(
                                    MainActivity.this,
                                    "Configuração salva; falha ao solicitar escopo: " + message,
                                    Toast.LENGTH_LONG
                            ).show());
                        }
                    }
            );
        } catch (Throwable throwable) {
            Toast.makeText(
                    this,
                    "Configuração salva; falha ao solicitar escopo: " + throwable.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
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

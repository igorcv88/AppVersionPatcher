package io.github.igorcv88.appversionpatcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ConfigStore {
    public static final String MODULE_PACKAGE = "io.github.igorcv88.appversionpatcher";
    public static final String PREFS_NAME = "version_spoofs";

    private static final String LEGACY_PREFS_NAME = PREFS_NAME;
    private static final String KEY_PACKAGES = "configured_packages";
    private static final String KEY_MIGRATED = "migrated_to_remote_v2";
    private static final String PREFIX_NAME = "version_name::";
    private static final String PREFIX_CODE = "version_code::";
    private static final String PREFIX_PM = "hook_pm::";
    private static final String PREFIX_RN = "hook_rn::";

    private ConfigStore() {}

    public static SharedPreferences legacyPreferences(Context context) {
        return context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void initializeRemote(Context context, SharedPreferences remotePreferences) {
        migrateLegacy(context, remotePreferences);
    }

    private static void migrateLegacy(Context context, SharedPreferences remotePreferences) {
        if (remotePreferences.getBoolean(KEY_MIGRATED, false)) {
            return;
        }

        SharedPreferences legacy = legacyPreferences(context);
        Set<String> packages = legacy.getStringSet(KEY_PACKAGES, Collections.emptySet());
        if (packages != null) {
            for (String packageName : new HashSet<>(packages)) {
                VersionConfig config = read(legacy, packageName);
                if (config != null) {
                    save(remotePreferences, config);
                }
            }
        }
        remotePreferences.edit().putBoolean(KEY_MIGRATED, true).commit();
    }

    public static VersionConfig read(SharedPreferences preferences, String packageName) {
        if (preferences == null || packageName == null) {
            return null;
        }

        Set<String> packages = preferences.getStringSet(KEY_PACKAGES, Collections.emptySet());
        if (packages == null || !packages.contains(packageName)) {
            return null;
        }

        String versionName = preferences.getString(PREFIX_NAME + packageName, null);
        if (versionName == null || versionName.trim().isEmpty()) {
            return null;
        }

        Long versionCode = parseLongOrNull(preferences.getString(PREFIX_CODE + packageName, null));
        return new VersionConfig(
                packageName,
                versionName.trim(),
                versionCode,
                preferences.getBoolean(PREFIX_PM + packageName, true),
                preferences.getBoolean(PREFIX_RN + packageName, true)
        );
    }

    public static Set<String> configuredPackages(SharedPreferences preferences) {
        if (preferences == null) {
            return Collections.emptySet();
        }
        Set<String> packages = preferences.getStringSet(KEY_PACKAGES, Collections.emptySet());
        return packages == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(packages));
    }

    public static boolean save(SharedPreferences preferences, VersionConfig config) {
        Set<String> current = preferences.getStringSet(KEY_PACKAGES, Collections.emptySet());
        Set<String> packages = current == null ? new HashSet<>() : new HashSet<>(current);
        packages.add(config.packageName);

        SharedPreferences.Editor editor = preferences.edit()
                .putStringSet(KEY_PACKAGES, packages)
                .putString(PREFIX_NAME + config.packageName, config.versionName)
                .putBoolean(PREFIX_PM + config.packageName, config.hookPackageManager)
                .putBoolean(PREFIX_RN + config.packageName, config.hookReactNativeDeviceInfo);

        if (config.versionCode == null) {
            editor.remove(PREFIX_CODE + config.packageName);
        } else {
            editor.putString(PREFIX_CODE + config.packageName, Long.toString(config.versionCode));
        }
        return editor.commit();
    }

    public static boolean remove(SharedPreferences preferences, String packageName) {
        Set<String> current = preferences.getStringSet(KEY_PACKAGES, Collections.emptySet());
        Set<String> packages = current == null ? new HashSet<>() : new HashSet<>(current);
        packages.remove(packageName);

        return preferences.edit()
                .putStringSet(KEY_PACKAGES, packages)
                .remove(PREFIX_NAME + packageName)
                .remove(PREFIX_CODE + packageName)
                .remove(PREFIX_PM + packageName)
                .remove(PREFIX_RN + packageName)
                .commit();
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

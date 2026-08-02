package io.github.igorcv88.appversionpatcher;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class XposedInit extends XposedModule {
    private static final String TAG = "AppVersionPatcher";
    private String processName = "unknown";

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        processName = param.getProcessName();
        logInfo(
                "Module loaded in " + processName +
                        " via " + getFrameworkName() + " " + getFrameworkVersion() +
                        " (API " + getApiVersion() + ")"
        );
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!param.isFirstPackage()) {
            return;
        }

        String packageName = param.getPackageName();
        SharedPreferences preferences;
        try {
            preferences = getRemotePreferences(ConfigStore.PREFS_NAME);
        } catch (Throwable throwable) {
            logError("Could not open remote preferences for " + packageName, throwable);
            return;
        }

        VersionConfig config = ConfigStore.read(preferences, packageName);
        if (config == null) {
            logInfo("No active configuration for " + packageName + " in " + processName);
            return;
        }

        int packageManagerHooks = 0;
        int reactNativeHooks = 0;
        try {
            if (config.hookPackageManager) {
                packageManagerHooks = hookPackageManager(config);
            }
            if (config.hookReactNativeDeviceInfo) {
                reactNativeHooks = hookReactNativeDeviceInfo(param.getClassLoader(), config);
            }
            logInfo(
                    "Hooks ready for " + packageName +
                            " -> versionName=" + config.versionName +
                            (config.versionCode == null ? "" : ", versionCode=" + config.versionCode) +
                            ", PackageManager hooks=" + packageManagerHooks +
                            ", RNDeviceInfo hooks=" + reactNativeHooks
            );
        } catch (Throwable throwable) {
            logError("Hook installation failed for " + packageName, throwable);
        }
    }

    private int hookPackageManager(VersionConfig config) throws ClassNotFoundException {
        Class<?> packageManagerClass = Class.forName(
                "android.app.ApplicationPackageManager",
                false,
                null
        );
        AtomicBoolean patchLogged = new AtomicBoolean(false);
        int count = 0;

        for (Method method : packageManagerClass.getDeclaredMethods()) {
            String name = method.getName();
            boolean singleResult = name.equals("getPackageInfo") || name.equals("getPackageInfoAsUser");
            boolean listResult = name.equals("getInstalledPackages") || name.equals("getInstalledPackagesAsUser");
            if (!singleResult && !listResult) {
                continue;
            }

            hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        boolean patched = false;
                        if (result instanceof PackageInfo packageInfo) {
                            patched = patchIfTarget(packageInfo, config);
                        } else if (result instanceof List<?> list) {
                            for (Object item : list) {
                                if (item instanceof PackageInfo packageInfo) {
                                    patched |= patchIfTarget(packageInfo, config);
                                }
                            }
                        }
                        if (patched && patchLogged.compareAndSet(false, true)) {
                            logInfo("PackageManager result patched for " + config.packageName);
                        }
                        return result;
                    });
            count++;
        }
        return count;
    }

    private int hookReactNativeDeviceInfo(ClassLoader classLoader, VersionConfig config) {
        Class<?> moduleClass;
        try {
            moduleClass = Class.forName(
                    "com.learnium.RNDeviceInfo.RNDeviceModule",
                    false,
                    classLoader
            );
        } catch (ClassNotFoundException exception) {
            logInfo("RNDeviceModule not present in " + config.packageName);
            return 0;
        }

        AtomicBoolean constantsLogged = new AtomicBoolean(false);
        AtomicBoolean packageInfoLogged = new AtomicBoolean(false);
        int count = 0;
        for (Method method : moduleClass.getDeclaredMethods()) {
            if (method.getName().equals("getConstants") && method.getParameterCount() == 0) {
                hook(method)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            if (!(result instanceof Map<?, ?> source)) {
                                logInfo(
                                        "RNDeviceModule#getConstants returned " +
                                                (result == null ? "null" : result.getClass().getName())
                                );
                                return result;
                            }

                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> direct = (Map<String, Object>) source;
                                patchReactNativeConstants(direct, config);
                                if (constantsLogged.compareAndSet(false, true)) {
                                    logInfo("RNDeviceInfo constants patched for " + config.packageName);
                                }
                                return direct;
                            } catch (UnsupportedOperationException | ClassCastException ignored) {
                                Map<String, Object> copy = new HashMap<>();
                                for (Map.Entry<?, ?> entry : source.entrySet()) {
                                    if (entry.getKey() instanceof String key) {
                                        copy.put(key, entry.getValue());
                                    }
                                }
                                patchReactNativeConstants(copy, config);
                                if (constantsLogged.compareAndSet(false, true)) {
                                    logInfo("RNDeviceInfo constants copied and patched for " + config.packageName);
                                }
                                return copy;
                            }
                        });
                count++;
            } else if (method.getName().equals("getPackageInfo") &&
                    PackageInfo.class.isAssignableFrom(method.getReturnType())) {
                hook(method)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            if (result instanceof PackageInfo packageInfo &&
                                    patchIfTarget(packageInfo, config) &&
                                    packageInfoLogged.compareAndSet(false, true)) {
                                logInfo("RNDeviceInfo PackageInfo patched for " + config.packageName);
                            }
                            return result;
                        });
                count++;
            }
        }
        return count;
    }

    private static boolean patchIfTarget(PackageInfo packageInfo, VersionConfig config) {
        if (packageInfo.packageName == null || !packageInfo.packageName.equals(config.packageName)) {
            return false;
        }
        packageInfo.versionName = config.versionName;
        if (config.versionCode != null) {
            int code = config.versionCode.intValue();
            packageInfo.versionCode = code;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.setLongVersionCode(code);
            }
        }
        return true;
    }

    private static void patchReactNativeConstants(
            Map<String, Object> constants,
            VersionConfig config
    ) {
        constants.put("appVersion", config.versionName);
        constants.put("appVersionName", config.versionName);
        constants.put("versionName", config.versionName);

        if (config.versionCode != null) {
            String code = Long.toString(config.versionCode);
            constants.put("buildNumber", code);
            constants.put("appVersionCode", code);
            constants.put("versionCode", code);
        }
    }

    private void logInfo(String message) {
        log(Log.INFO, TAG, message);
    }

    private void logError(String message, Throwable throwable) {
        log(Log.ERROR, TAG, message, throwable);
    }
}

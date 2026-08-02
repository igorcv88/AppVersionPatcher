package io.github.igorcv88.appversionpatcher;

import java.util.Objects;

public final class VersionConfig {
    public final String packageName;
    public final String versionName;
    public final Long versionCode;
    public final boolean hookPackageManager;
    public final boolean hookReactNativeDeviceInfo;

    public VersionConfig(
            String packageName,
            String versionName,
            Long versionCode,
            boolean hookPackageManager,
            boolean hookReactNativeDeviceInfo
    ) {
        this.packageName = Objects.requireNonNull(packageName);
        this.versionName = Objects.requireNonNull(versionName);
        this.versionCode = versionCode;
        this.hookPackageManager = hookPackageManager;
        this.hookReactNativeDeviceInfo = hookReactNativeDeviceInfo;
    }
}

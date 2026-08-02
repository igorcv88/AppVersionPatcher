package io.github.igorcv88.appversionpatcher;

import android.graphics.drawable.Drawable;

public final class AppEntry {
    public final String label;
    public final String packageName;
    public final Drawable icon;
    public final boolean systemApp;
    public final String installedVersionName;
    public final long installedVersionCode;

    public AppEntry(
            String label,
            String packageName,
            Drawable icon,
            boolean systemApp,
            String installedVersionName,
            long installedVersionCode
    ) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
        this.systemApp = systemApp;
        this.installedVersionName = installedVersionName;
        this.installedVersionCode = installedVersionCode;
    }
}

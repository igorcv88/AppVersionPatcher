package io.github.igorcv88.appversionpatcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AppListAdapter extends BaseAdapter {
    private final Context context;
    private final List<AppEntry> visibleEntries = new ArrayList<>();
    private SharedPreferences preferences;
    private Set<String> scope = Collections.emptySet();

    public AppListAdapter(Context context) {
        this.context = context;
    }

    public void setState(SharedPreferences preferences, Set<String> scope) {
        this.preferences = preferences;
        this.scope = scope == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(scope));
        notifyDataSetChanged();
    }

    public void replace(List<AppEntry> entries) {
        visibleEntries.clear();
        visibleEntries.addAll(entries);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return visibleEntries.size();
    }

    @Override
    public AppEntry getItem(int position) {
        return visibleEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            holder = new Holder();
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int padding = dp(14);
            row.setPadding(padding, dp(10), padding, dp(10));

            holder.icon = new ImageView(context);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
            iconParams.setMarginEnd(dp(14));
            row.addView(holder.icon, iconParams);

            LinearLayout texts = new LinearLayout(context);
            texts.setOrientation(LinearLayout.VERTICAL);
            holder.title = new TextView(context);
            holder.title.setTextSize(16);
            holder.title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            holder.packageName = new TextView(context);
            holder.packageName.setTextSize(12);
            holder.summary = new TextView(context);
            holder.summary.setTextSize(13);
            texts.addView(holder.title);
            texts.addView(holder.packageName);
            texts.addView(holder.summary);
            row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            convertView = row;
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        AppEntry entry = getItem(position);
        holder.icon.setImageDrawable(entry.icon);
        holder.title.setText(entry.label);
        holder.packageName.setText(entry.packageName);

        VersionConfig config = ConfigStore.read(preferences, entry.packageName);
        boolean inScope = scope.contains(entry.packageName);
        if (config == null) {
            holder.summary.setText(
                    "Instalada: " + entry.installedVersionName +
                            " · " + (inScope ? "no escopo" : "fora do escopo")
            );
            holder.summary.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        } else {
            String code = config.versionCode == null ? "" : " (code " + config.versionCode + ")";
            holder.summary.setText(
                    "Aplicada: " + config.versionName + code +
                            " · " + (inScope ? "no escopo" : "fora do escopo")
            );
            holder.summary.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return convertView;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class Holder {
        ImageView icon;
        TextView title;
        TextView packageName;
        TextView summary;
    }
}

package io.github.igorcv88.appversionpatcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
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
            row.setMinimumHeight(dp(72));
            row.setPadding(dp(14), dp(10), dp(14), dp(10));

            TypedValue selectableBackground = new TypedValue();
            if (context.getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    selectableBackground,
                    true
            )) {
                row.setBackgroundResource(selectableBackground.resourceId);
            }

            holder.icon = new ImageView(context);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(42), dp(42));
            iconParams.setMarginEnd(dp(14));
            row.addView(holder.icon, iconParams);

            LinearLayout texts = new LinearLayout(context);
            texts.setOrientation(LinearLayout.VERTICAL);

            holder.title = new TextView(context);
            holder.title.setTextSize(15);
            holder.title.setTextColor(color(R.color.text_primary));
            holder.title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            holder.title.setSingleLine(true);
            holder.title.setEllipsize(TextUtils.TruncateAt.END);

            holder.packageName = new TextView(context);
            holder.packageName.setTextSize(11);
            holder.packageName.setTextColor(color(R.color.text_secondary));
            holder.packageName.setSingleLine(true);
            holder.packageName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            holder.packageName.setPadding(0, dp(1), 0, 0);

            holder.summary = new TextView(context);
            holder.summary.setTextSize(12);
            holder.summary.setTextColor(color(R.color.text_secondary));
            holder.summary.setPadding(0, dp(2), 0, 0);

            texts.addView(holder.title);
            texts.addView(holder.packageName);
            texts.addView(holder.summary);
            row.addView(texts, new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            ));

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
        String scopeState = context.getString(
                inScope ? R.string.scope_state_in : R.string.scope_state_out
        );

        if (config == null) {
            holder.summary.setText(context.getString(
                    R.string.installed_summary,
                    entry.installedVersionName,
                    scopeState
            ));
            holder.summary.setTextColor(color(R.color.text_secondary));
            holder.summary.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        } else {
            String code = config.versionCode == null
                    ? ""
                    : context.getString(R.string.version_code_suffix, config.versionCode);
            holder.summary.setText(context.getString(
                    R.string.applied_summary,
                    config.versionName,
                    code,
                    scopeState
            ));
            holder.summary.setTextColor(color(R.color.accent));
            holder.summary.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return convertView;
    }

    private int color(int resourceId) {
        return context.getColor(resourceId);
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

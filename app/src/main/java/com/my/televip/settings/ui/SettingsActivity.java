package com.my.televip.settings.ui;


import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Drawable.ArrowDrawable;
import com.my.televip.audio;
import com.my.televip.base.AbstractMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.AutomationResolver;
import com.my.televip.settings.controller.SettingsController;
import com.my.televip.ui.toolBar.MainToolBar;
import com.my.televip.virtuals.Theme;

public class SettingsActivity {

    public static boolean isSettings;

    private final Context context;

    /**
     * The settings: a plain scrolling column of the client's own cells. The list is short, so it
     * needs no recycling, and so nothing of RecyclerView: forks rename, strip or change the
     * client's, and the list built on it came up empty on Nagram X 12.9.2.
     */
    public View createView(SettingsController settingsController) {

        LinearLayout layout = new LinearLayout(context);

        try {
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setBackgroundColor(Theme.getBackgroundGrayColor());

            MainToolBar toolbar = new MainToolBar(context);

            toolbar.setColorTitle(Theme.getTextToolBarColor());
            toolbar.setRippleColor(Theme.getToolBarRippleColor());
            toolbar.setTextTitle(Translator.get(Keys.SettingsName));

            ArrowDrawable arrow = new ArrowDrawable();
            toolbar.setImageDrawable(arrow);
            toolbar.getImage().setOnClickListener(v -> settingsController.hide());

            layout.addView(toolbar);

            ScrollView scroll = new ScrollView(context);
            scroll.setBackgroundColor(Theme.getBackgroundWhiteOrBlueColor());
            scroll.setVerticalScrollBarEnabled(false);

            LinearLayout rows = new LinearLayout(context);
            rows.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(rows, new ScrollView.LayoutParams(
                    ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

            SettingsCells.prepare(context);
            for (int i = 0; i < SettingsAdapter.getRowCount(); i++) {
                // Row by row, so a cell this client cannot build costs that row, not the list.
                try {
                    SettingsAdapter.Row row = SettingsAdapter.createRow(context, i);
                    SettingsAdapter.bind(row, settingsController, i);
                    rows.addView(row.itemView, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                } catch (Throwable t) {
                    Logger.e(t);
                }
            }

            LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
            );
            listParams.setMargins(10, 10, 10, 0);

            layout.addView(scroll, listParams);

        } catch (Throwable e) {
            Logger.e(e);
        }

        return layout;
    }

    public SettingsActivity(Context context) {
        this.context = context;
        isSettings = true;
    }

    public static void init(SettingsController settingsController) {
        audio.init();
        try {
            HMethod.hookMethod(ClassLoad.getClass(ClassNames.LAUNCH_ACTIVITY), "onBackPressed", new AbstractMethodHook() {
                @Override
                protected void beforeMethod(MethodHookParam param) {
                    if (isSettings) {
                        settingsController.hide();
                        settingsController.settingsView = null;
                        param.setResult(null);
                    }
                }
            });

            HMethod.hookMethod(ClassLoad.getClass(ClassNames.ANDROID_UTILITIES), AutomationResolver.resolve("AndroidUtilities", "isTabletInternal", AutomationResolver.ResolverType.Method), new AbstractMethodHook() {
                @Override
                protected void beforeMethod(MethodHookParam param) {
                    if (isSettings) {
                        param.setResult(true);
                    }
                }
            });
        } catch (Throwable e) {
            Logger.e(e);
        }
    }
}

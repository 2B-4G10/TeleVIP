package com.my.televip.settings.controller;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.my.televip.Configs.ConfigPreferences;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.settings.ui.SettingsActivity;
import com.my.televip.logging.Logger;
import com.my.televip.virtuals.ActionBar.AlertDialog;
import com.my.televip.virtuals.messenger.browser.Browser;
import com.my.televip.virtuals.ui.LaunchActivity;

public class SettingsController {

    public FrameLayout settingsView;
    private final Context context;

    public SettingsActivity settingsActivity;

    public SettingsController(Context context) {
        this.context = context;
    }

    public void openView(){
        try {
            if (settingsView == null) {
                settingsView = new FrameLayout(context);
            }

            settingsView.removeAllViews();
            settingsActivity = new SettingsActivity(context);
            showJoinTeleVip();

            settingsView.addView(settingsActivity.createView(this));

            show(settingsView);
        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    private void showJoinTeleVip() {
        try {
            if (!ConfigPreferences.getBoolean("JTV")) {
                AlertDialog alertDialog = new AlertDialog(context);

                alertDialog.setTitle(Translator.get(Keys.SettingsName));
                alertDialog.setMessage(Translator.get(Keys.JoinTeleVip));

                alertDialog.setPositiveButton(Translator.get(Keys.Join), AlertDialog.click(() -> {
                    try {
                        Browser.openUrl(context, "https://t.me/t_l0_e");
                    } catch (Throwable t) {
                        Logger.e(t);
                    }
                    hide();
                }));

                alertDialog.setNegativeButton(Translator.get(Keys.Cancel), null);
                alertDialog.setNeutralButton(Translator.get(Keys.DontShowAgain), AlertDialog.click(() -> ConfigPreferences.putBoolean("JTV", true)));
                alertDialog.show();
            }
        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    public void show(View target) {
        try {
            FrameLayout root = new LaunchActivity(context).frameLayout;
            if (root == null) {
                Logger.e(new IllegalStateException("LaunchActivity content view not found"));
                return;
            }
            if (target.getParent() == null) {
                root.addView(target);
            }

            for (int i = 0; i < root.getChildCount(); i++) {
                View child = root.getChildAt(i);
                child.setVisibility(child == target ? View.VISIBLE : View.GONE);
            }

            target.bringToFront();
        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    public void hide() {
        SettingsActivity.isSettings = false;
        if (settingsView == null) return;
        try {
            // Where the view actually is beats where we think it went.
            ViewGroup parent = settingsView.getParent() instanceof ViewGroup ? (ViewGroup) settingsView.getParent() : null;
            if (parent == null) parent = new LaunchActivity(settingsView.getContext()).frameLayout;
            if (parent == null) return;

            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                child.setVisibility(child == settingsView ? View.GONE : View.VISIBLE);
            }
            if (settingsView.getParent() == parent) {
                parent.removeView(settingsView);
            }
        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    public Context getContext() {
        return context;
    }

}

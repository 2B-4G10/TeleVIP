package com.my.televip;

import static com.my.televip.obfuscate.AutomationResolver.resolverRegistry;

import android.content.Context;

import com.my.televip.Configs.ConfigManager;
import com.my.televip.application.AndroidUtilities;
import com.my.televip.diagnostics.HookHealth;
import com.my.televip.dex.DexInjector;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.RuntimeMappings;
import com.my.televip.settings.SettingsManager;
import com.my.televip.settings.controller.SettingsController;
import com.my.televip.utils.Utils;
import com.my.televip.virtuals.TeleVip.Bridge.Bridge;

public class TeleVip {
    
    public static void startHook(Context context) {
        try {
            // Before any feature looks a name up: the names this build really uses, from its APK.
            RuntimeMappings.activate(context, Utils.pkgName);
            resolverRegistry.loadParameter();
            Translator.init();
            AndroidUtilities.init(context);
            DexInjector.injectDex(Utils.classLoader);

            SettingsController settingsController = new SettingsController(context);

            Bridge.init(settingsController);
            ConfigManager.loadAndRead(context);
            SettingsManager.init(settingsController);

        } catch (Throwable e){
            Logger.e(e);
        } finally {
            // Reported even when setup threw part way through - that is when it matters most.
            HookHealth.logReport();
        }

    }

}

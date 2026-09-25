package com.my.televip.virtuals.ActionBar;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.AutomationResolver;
import com.my.televip.reflect.XReflect;
import com.my.televip.utils.Utils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * TeleVip's dialogs, built with the client's own AlertDialog.Builder when that can be driven
 * safely, and with the platform's otherwise.
 *
 * <p>"Safely" matters for the buttons. In an obfuscated build the positive, negative and neutral
 * setters are three identically shaped methods with meaningless names; if they cannot each be
 * pinned down, guessing would attach an action to the wrong button. So the client's builder is
 * only used when every method this class calls actually exists on it - which is always true for
 * clients that keep their names, and true for obfuscated ones only when resolution pinned them
 * all. The platform dialog looks less native but is never wrong.</p>
 */
public class AlertDialog {

    @FunctionalInterface
    public interface OnClick {
        void onClick();
    }

    /** A button action. Opaque to callers; turned into whichever listener type the backend wants. */
    public static final class Click {
        final OnClick action;

        Click(OnClick action) {
            this.action = action;
        }
    }

    public static Object click(OnClick lambda) {
        return new Click(lambda);
    }

    private static final String BUILDER = "AlertDialog$Builder";
    private static final String[] NEEDED = {
            "setTitle", "setMessage", "setView", "setPositiveButton", "setNegativeButton", "setNeutralButton", "show"
    };

    private final Context context;
    private Object telegramBuilder;
    private android.app.AlertDialog.Builder platformBuilder;
    private Dialog created;

    public AlertDialog(Context context) {
        this.context = context;
        Class<?> builderClass = ClassLoad.getClass(ClassNames.ALERT_DIALOG_BUILDER);
        if (builderClass != null && hasAll(builderClass)) {
            try {
                telegramBuilder = XReflect.newInstance(builderClass, context);
            } catch (Throwable t) {
                Logger.e(t);
            }
        }
        if (telegramBuilder == null) {
            platformBuilder = new android.app.AlertDialog.Builder(context);
        }
    }

    private static boolean hasAll(Class<?> builderClass) {
        for (String name : NEEDED) {
            String resolved = AutomationResolver.resolve(BUILDER, name, AutomationResolver.ResolverType.Method);
            if (!declares(builderClass, resolved)) return false;
        }
        return true;
    }

    private static boolean declares(Class<?> cls, String name) {
        for (Method m : cls.getDeclaredMethods()) if (m.getName().equals(name)) return true;
        return false;
    }

    private void call(String name, Object... args) {
        XReflect.callMethod(telegramBuilder, AutomationResolver.resolve(BUILDER, name, AutomationResolver.ResolverType.Method), args);
    }

    public void setTitle(CharSequence title) {
        if (telegramBuilder != null) call("setTitle", title);
        else platformBuilder.setTitle(title);
    }

    public void setView(View view) {
        if (telegramBuilder != null) call("setView", view);
        else platformBuilder.setView(view);
    }

    public void setMessage(CharSequence message) {
        if (telegramBuilder != null) call("setMessage", message);
        else platformBuilder.setMessage(message);
    }

    public void setPositiveButton(CharSequence text, Object click) {
        if (telegramBuilder != null) call("setPositiveButton", text, telegramListener(click));
        else platformBuilder.setPositiveButton(text, platformListener(click));
    }

    public void setNegativeButton(CharSequence text, Object click) {
        if (telegramBuilder != null) call("setNegativeButton", text, telegramListener(click));
        else platformBuilder.setNegativeButton(text, platformListener(click));
    }

    public void setNeutralButton(CharSequence text, Object click) {
        if (telegramBuilder != null) call("setNeutralButton", text, telegramListener(click));
        else platformBuilder.setNeutralButton(text, platformListener(click));
    }

    public void show() {
        if (telegramBuilder != null) {
            call("show");
        } else {
            created = platformBuilder.show();
        }
    }

    public Dialog create() {
        if (telegramBuilder != null) {
            created = (Dialog) XReflect.callMethod(telegramBuilder,
                    AutomationResolver.resolve(BUILDER, "create", AutomationResolver.ResolverType.Method));
        } else {
            created = platformBuilder.create();
        }
        return created;
    }

    public Runnable getDismissRunnable() {
        if (telegramBuilder != null) {
            try {
                return (Runnable) XReflect.callMethod(telegramBuilder,
                        AutomationResolver.resolve(BUILDER, "getDismissRunnable", AutomationResolver.ResolverType.Method));
            } catch (Throwable ignored) {
                // Inlined away in some builds; fall through to dismissing what we created.
            }
        }
        return () -> {
            if (created != null) created.dismiss();
        };
    }

    // -------------------------------------------------------------- listeners

    private static Object telegramListener(Object click) {
        if (!(click instanceof Click)) return click;
        final OnClick action = ((Click) click).action;
        Class<?> listenerClass = ClassLoad.getClass(ClassNames.ALERT_DIALOG_BUTTON_CLICK);
        if (listenerClass == null) return null;
        final String onClick = AutomationResolver.resolve("AlertDialog$OnButtonClickListener", "onClick",
                AutomationResolver.ResolverType.Method);
        return Proxy.newProxyInstance(Utils.classLoader, new Class[]{listenerClass}, (proxy, method, args) -> {
            if (method.getName().equals(onClick)) action.onClick();
            return null;
        });
    }

    private static DialogInterface.OnClickListener platformListener(Object click) {
        if (!(click instanceof Click)) return null;
        final OnClick action = ((Click) click).action;
        return (dialog, which) -> action.onClick();
    }
}

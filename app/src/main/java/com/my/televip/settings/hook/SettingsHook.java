package com.my.televip.settings.hook;


import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.ClientChecker;
import com.my.televip.Drawable.AppIconDrawable;
import com.my.televip.base.AbstractMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.AutomationResolver;
import com.my.televip.settings.controller.SettingsController;
import com.my.televip.utils.Utils;
import com.my.televip.virtuals.Adapters.DrawerLayoutAdapter;
import com.my.televip.virtuals.SettingsIconResolver;
import com.my.televip.virtuals.ui.Components.UItem;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import com.my.televip.reflect.XReflect;

public class SettingsHook {

    private Constructor<?> itemConstructor;

    public void newSettings(Class<?> SettingsActivityClass, Class<?> SettingsActivity$SettingCell$FactoryClass, SettingsController settingsController){
        try {

            hookRowIcon(SettingsActivity$SettingCell$FactoryClass, new AppIconDrawable());

            String fillItems = AutomationResolver.resolve("SettingsActivity", "fillItems", AutomationResolver.ResolverType.Method);
            String onClick = AutomationResolver.resolve("SettingsActivity", "onClick", AutomationResolver.ResolverType.Method);
            if (!declares(SettingsActivityClass, fillItems) || !declares(SettingsActivityClass, onClick)) {
                // R8 folded them into merged lambda classes (Nekogram 12.10.5+): wrap the
                // callbacks the settings list is built with instead.
                hookListCallbacks(SettingsActivityClass, SettingsActivity$SettingCell$FactoryClass, settingsController);
                return;
            }

            HMethod.hookMethod(SettingsActivityClass, fillItems,
                    AutomationResolver.merge(AutomationResolver.resolveObject("fillItems", new Class<?>[]{java.util.ArrayList.class, ClassLoad.getClass(ClassNames.UNIVERSAL_ADAPTER)}), new AbstractMethodHook() {
                        @Override
                        protected void afterMethod(final MethodHookParam param) {
                            ArrayList<Object> arrayList = argOfType(param, ArrayList.class);
                            if (arrayList != null) addRow(arrayList, SettingsActivity$SettingCell$FactoryClass);
                        }
                    }));


            Class<?> UItemClass = ClassLoad.getClass(ClassNames.UITEM);

            HMethod.hookMethod(
                    SettingsActivityClass,
                    onClick, AutomationResolver.merge(AutomationResolver.resolveObject("onClick", new Class<?>[]{UItemClass, View.class, int.class, float.class, float.class}), new AbstractMethodHook() {
                        @Override
                        protected void afterMethod(final MethodHookParam param) {
                            UItem uItem = new UItem(argOfType(param, UItemClass));
                            if (uItem.getUItem() != null) {
                                if (uItem.getID() == 8353847) {
                                    settingsController.openView();
                                }
                            }
                        }
                    }));
        } catch (Throwable t){
            Logger.e(t);
        }
    }

    private static final int ROW_ID = 8353847;

    /** TeleVip's row, before the first row that has a subtitle (the account section). */
    private static void addRow(ArrayList<Object> items, Class<?> factory) {
        // The tile in the app icon's yellow; the plane is drawn on it by hookRowIcon.
        Object row = newSettingItem(factory, ROW_ID, AppIconDrawable.TILE_COLOR, AppIconDrawable.TILE_COLOR, ROW_ID,
                Translator.get(Keys.GhostMode), Translator.get(Keys.ByMustafa));
        for (int i = 0; i < items.size(); i++) {
            UItem item = new UItem(items.get(i));
            if (item.getText() != null && item.getSubtext() != null) {
                items.add(i, row);
                return;
            }
        }
    }

    private static boolean declares(Class<?> cls, String name) {
        for (Method m : cls.getDeclaredMethods()) if (m.getName().equals(name)) return true;
        return false;
    }

    /**
     * The settings screen builds its list with new UniversalRecyclerView(this, fillItems, onClick,
     * onLongClick). For the settings fragment, the fill callback is wrapped to add TeleVip's row and
     * the click callback to open TeleVip on it - whatever R8 did to the methods behind them.
     */
    private void hookListCallbacks(final Class<?> settingsActivity, final Class<?> factory, final SettingsController controller) {
        Class<?> listView = ClassLoad.getClass(ClassNames.UNIVERSAL_RECYCLER_VIEW);
        final Class<?> itemClass = ClassLoad.getClass(ClassNames.UITEM);
        if (listView == null || itemClass == null) return;
        for (Constructor<?> constructor : listView.getDeclaredConstructors()) {
            final Class<?>[] params = constructor.getParameterTypes();
            if (params.length != 4 || !params[0].isAssignableFrom(settingsActivity)
                    || !params[1].isInterface() || !params[2].isInterface()) continue;
            HMethod.hookMember(constructor, new AbstractMethodHook() {
                @Override
                @SuppressWarnings("unchecked")   // the client's own lists, changed in place
                protected void beforeMethod(MethodHookParam param) {
                    if (!settingsActivity.isInstance(param.args[0])) return;
                    final Object fill = param.args[1], click = param.args[2];
                    if (fill != null) {
                        param.args[1] = wrap(params[1], fill, (method, args) -> {
                            Object result = method.invoke(fill, args);
                            if (args != null && args.length > 0 && args[0] instanceof ArrayList) {
                                addRow((ArrayList<Object>) args[0], factory);
                            }
                            return result;
                        });
                    }
                    if (click != null) {
                        param.args[2] = wrap(params[2], click, (method, args) -> {
                            if (args != null && args.length > 0 && itemClass.isInstance(args[0])
                                    && new UItem(args[0]).getID() == ROW_ID) {
                                controller.openView();
                                return null;
                            }
                            return method.invoke(click, args);
                        });
                    }
                }
            });
        }
    }

    private interface Call {
        Object run(Method method, Object[] args) throws Throwable;
    }

    /** A proxy of a callback interface: Object's methods go to the original, the callback to {@code call}. */
    private static Object wrap(Class<?> iface, final Object original, final Call call) {
        return java.lang.reflect.Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("equals")) return args != null && args.length == 1 && proxy == args[0];
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                return method.invoke(original, args);
            }
            try {
                return call.run(method, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    /**
     * The first argument of this type. R8 may make the hooked method static (the fragment comes
     * first) and drop parameters it never reads, so positions are not fixed.
     */
    @SuppressWarnings("unchecked")
    private static <T> T argOfType(AbstractMethodHook.MethodHookParam param, Class<?> type) {
        if (type == null) return null;
        for (Object arg : param.args) {
            if (type.isInstance(arg)) return (T) arg;
        }
        return null;
    }

    /**
     * A settings row from SettingCell.Factory.of. The seven-argument overload is tried first: the
     * shorter ones only forward to it with a null value, and a build that never calls them has
     * them inlined away (Nekogram 12.10.3 does), while the seven-argument one is always there.
     */
    private static Object newSettingItem(Class<?> factory, int id, int colorTop, int colorBottom, int icon,
                                         CharSequence title, CharSequence subtitle) {
        String of = AutomationResolver.resolve("SettingsActivity$SettingCell$Factory", "of", AutomationResolver.ResolverType.Method);
        String of7 = AutomationResolver.resolve("SettingsActivity$SettingCell$Factory", "ofIIIICCC", AutomationResolver.ResolverType.Method);
        // An unmapped key comes back unchanged; only a mapped one is a real method name.
        String sevenArgs = "ofIIIICCC".equals(of7) ? of : of7;
        try {
            return XReflect.callStaticMethod(factory, sevenArgs, id, colorTop, colorBottom, icon, title, subtitle, null);
        } catch (Throwable sevenFailed) {
            return XReflect.callStaticMethod(factory, of, id, colorTop, colorBottom, icon, title, subtitle);
        }
    }

    /**
     * Puts TeleVip's app icon on TeleVip's row. Hooks SettingCell.set where the build still has it;
     * where R8 inlined set into the factory's bindView (Nekogram 12.10.3), hooks bindView, which
     * cannot be inlined because it overrides UItemFactory's.
     */
    private void hookRowIcon(Class<?> factory, final AppIconDrawable icon) {
        final Class<?> cellClass = ClassLoad.getClass(ClassNames.SETTINGS_ACTIVITY_SETTING_CELL);
        final String iconField = AutomationResolver.resolve("SettingsActivity$SettingCell", "iconView", AutomationResolver.ResolverType.Field);
        String setName = AutomationResolver.resolve("SettingsActivity$SettingCell", "set", AutomationResolver.ResolverType.Method);
        Class<?>[] setParams = AutomationResolver.resolveObject("set", new Class<?>[]{int.class, int.class, int.class, CharSequence.class, CharSequence.class, CharSequence.class});

        if (cellClass != null && XReflect.findMethodExactIfExists(cellClass, setName, setParams) != null) {
            HMethod.hookMethod(cellClass, setName, AutomationResolver.merge(setParams, new AbstractMethodHook() {
                @Override
                protected void afterMethod(MethodHookParam param) {
                    if ((int) param.args[2] == 8353847) setRowIcon(param.thisObject, iconField, icon);
                }
            }));
            return;
        }

        if (factory == null) return;
        String bindName = AutomationResolver.resolve("SettingsActivity$SettingCell$Factory", "bindView", AutomationResolver.ResolverType.Method);
        for (Method method : factory.getDeclaredMethods()) {
            Class<?>[] params = method.getParameterTypes();
            if (!method.getName().equals(bindName) || params.length < 2 || !View.class.isAssignableFrom(params[0])) continue;
            HMethod.hookMethod(method, new AbstractMethodHook() {
                @Override
                protected void afterMethod(MethodHookParam param) {
                    if (param.args[1] != null && new UItem(param.args[1]).getID() == 8353847) {
                        setRowIcon(param.args[0], iconField, icon);
                    }
                }
            });
            return;
        }
        Logger.w("settings: neither SettingCell.set nor Factory.bindView found, TeleVip's row keeps the default icon");
    }

    private static void setRowIcon(Object cell, String iconField, AppIconDrawable icon) {
        ImageView iconView = null;
        try {
            iconView = (ImageView) XReflect.getObjectField(cell, iconField);
        } catch (Throwable renamed) {
            // iconView renamed and not resolved (Nekogram 12.10.6): the row's only image is its icon.
        }
        if (iconView == null && cell instanceof ViewGroup) iconView = onlyImage((ViewGroup) cell);
        if (iconView != null) {
            iconView.setImageDrawable(icon);
        } else {
            Logger.w("settings: no icon view on TeleVip's row, it keeps the default icon");
        }
    }

    /** The one ImageView inside {@code group}, or null if there is none or more than one. */
    private static ImageView onlyImage(ViewGroup group) {
        ImageView found = null;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            ImageView image = child instanceof ImageView ? (ImageView) child
                    : child instanceof ViewGroup ? onlyImage((ViewGroup) child) : null;
            if (image == null && child instanceof ViewGroup && hasImage((ViewGroup) child)) return null;
            if (image == null) continue;
            if (found != null) return null;
            found = image;
        }
        return found;
    }

    private static boolean hasImage(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ImageView || child instanceof ViewGroup && hasImage((ViewGroup) child)) return true;
        }
        return false;
    }

    public void oldSettings(SettingsController settingsController){
        final Class<?> itemClass = XReflect.findClassIfExists(AutomationResolver.resolve("org.telegram.ui.Adapters.DrawerLayoutAdapter$Item"), Utils.classLoader);

        if (itemClass != null) {
            HMethod.hookMethod(
                    ClassLoad.getClass(ClassNames.DRAWER_LAYOUT_ADAPTER),
                    AutomationResolver.resolve("DrawerLayoutAdapter", "resetItems", AutomationResolver.ResolverType.Method),
                    new AbstractMethodHook() {
                        @Override
                        @SuppressWarnings("unchecked")   // the client's own lists, changed in place
                        protected void afterMethod(MethodHookParam param) throws Throwable {

                            DrawerLayoutAdapter drawerLayoutAdapter = new DrawerLayoutAdapter(param.thisObject);

                            ArrayList<?> items = drawerLayoutAdapter.getItems();

                            if (itemConstructor == null) {
                                itemConstructor = itemClass.getDeclaredConstructor(AutomationResolver.resolveObject("item", new Class<?>[]{int.class, CharSequence.class, int.class}));
                                itemConstructor.setAccessible(true);
                            }

                            Object newItem = itemConstructor.newInstance(8353847, Translator.get(Keys.GhostMode), SettingsIconResolver.getIconSettings());

                            if (items instanceof ArrayList<?>) {
                                ArrayList<Object> typedItems = (ArrayList<Object>) items;
                                typedItems.add(newItem);
                            }
                        }
                    }
            );

            AbstractMethodHook onCreateHook = new AbstractMethodHook() {
                @Override
                protected void afterMethod(final MethodHookParam param) {

                    Object Launch = param.thisObject;

                    Object drawerLayoutAdapter = XReflect.getObjectField(Launch, AutomationResolver.resolve("LaunchActivity", "drawerLayoutAdapter", AutomationResolver.ResolverType.Field));
                    if (drawerLayoutAdapter != null) {
                        Object args = param.args[1];

                        int id = (int) XReflect.callMethod(drawerLayoutAdapter, AutomationResolver.resolve("DrawerLayoutAdapter", "getId", AutomationResolver.ResolverType.Method), args);
                        if (id == 8353847) {

                            Object drawerLayoutContainer = XReflect.getObjectField(Launch, AutomationResolver.resolve("LaunchActivity", "drawerLayoutContainer", AutomationResolver.ResolverType.Field));
                            if (drawerLayoutContainer != null) {
                                if (!ClientChecker.check(ClientChecker.ClientType.ForkgramClassic)) {
                                    XReflect.callMethod(drawerLayoutContainer, AutomationResolver.resolve("DrawerLayoutContainer", "closeDrawer", AutomationResolver.ResolverType.Method));
                                } else {
                                    XReflect.callMethod(drawerLayoutContainer, AutomationResolver.resolve("DrawerLayoutContainer", "closeDrawer", AutomationResolver.ResolverType.Method), true);
                                }
                            }

                            settingsController.openView();
                        }

                    }
                }
            };

            if (ClassLoad.getClass(ClassNames.LAUNCH_ACTIVITY) != null) {

                Method onCreateMethod = null;
                for (Method method : ClassLoad.getClass(ClassNames.LAUNCH_ACTIVITY).getDeclaredMethods()) {
                    if (Arrays.equals(method.getParameterTypes(), AutomationResolver.resolveObject("onCreateMethod", new Class<?>[]{android.view.View.class, int.class, float.class, float.class}))) {
                        onCreateMethod = method;
                        break;
                    }
                }

                if (onCreateMethod == null) {
                    Logger.w("Failed to hook onCreateMethod! Reason: No method found, " + Utils.issue);
                    return;
                }

                HMethod.hookMember(onCreateMethod, onCreateHook);
            }
        }

    }
}


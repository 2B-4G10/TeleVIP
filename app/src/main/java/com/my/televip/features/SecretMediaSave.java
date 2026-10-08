package com.my.televip.features;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import com.my.televip.Class.ClassNames;
import com.my.televip.Class.ClassLoad;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.AbstractMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.AutomationResolver;
import com.my.televip.reflect.XReflect;
import com.my.televip.virtuals.messenger.MessageObject;
import com.my.televip.virtuals.tgnet.TLRPC;
import com.my.televip.virtuals.ui.Cells.ChatMessageCell;
import com.my.televip.virtuals.ui.PhotoViewer;
import com.my.televip.virtuals.ui.SecretMediaViewer;

import java.io.File;
import java.lang.reflect.Method;

public class SecretMediaSave {

    public static boolean isEnable = false;

    public static long id;
    public static File pathImage;

    public static void init() {
        try {
            if (!isEnable) {
                isEnable = true;
                if (ClassLoad.getClass(ClassNames.MESSAGE_OBJECT) != null) {
                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.MESSAGE_OBJECT), AutomationResolver.resolve("MessageObject", "isSecret", AutomationResolver.ResolverType.Method), new AbstractMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            if (ConfigManager.secretMediaSave.isEnable()) param.setResult(false);
                        }
                    });
                }
                if (ClassLoad.getClass(ClassNames.CHAT_MESSAGE_CELL_DELEGATE) != null) {
                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.CHAT_MESSAGE_CELL_DELEGATE), AutomationResolver.resolve("ChatActivity$ChatMessageCellDelegate", "didPressImage", AutomationResolver.ResolverType.Method), AutomationResolver.merge(AutomationResolver.resolveObject("didPressImage", new Class<?>[]{ClassLoad.getClass(ClassNames.CHAT_MESSAGE_CELL), float.class, float.class, boolean.class}), new AbstractMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            try {
                                if (ConfigManager.secretMediaSave.isEnable() && param.args[0] != null) {
                                    ChatMessageCell messageCell = new ChatMessageCell(param.args[0]);
                                    if (messageCell.getChatMessageCell() != null) {
                                        MessageObject messageObject = messageCell.getMessageObject();
                                        if (messageObject.getMessageObject() != null) {
                                            TLRPC.Message message = messageObject.getMessageOwner();
                                            if (message.getTtl() > 0) message.setTtl(0);
                                        }
                                        bindPhotoViewerToActivity(messageCell);
                                    }
                                }
                            } catch (Throwable e) {
                                Logger.e(e);
                            }
                        }
                    }));
                }

                Class<?> fileLoader = ClassLoad.getClass(ClassNames.FILE_LOADER);
                Class<?> tlMessage = ClassLoad.getClass(ClassNames.TL_MESSAGE);
                if (fileLoader != null && tlMessage != null) {
                    // Every overload the build has: R8 inlines or duplicates the short ones
                    // (Momogram has two look-alike one-argument copies), and all of them end in
                    // the three-argument one.
                    AbstractMethodHook savedPath = new AbstractMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            try {
                                if (ConfigManager.secretMediaSave.isEnable() && param.args[0] != null && pathImage != null) {
                                    TLRPC.Message message = new TLRPC.Message(param.args[0]);
                                    if (message.getID() == id) {
                                        param.setResult(pathImage);
                                    }
                                }
                            } catch (Throwable e) {
                                Logger.e(e);
                            }
                        }
                    };
                    hookIfPresent(fileLoader, AutomationResolver.resolve("FileLoader", "getPathToMessage", AutomationResolver.ResolverType.Method),
                            AutomationResolver.resolveObject("getPathToMessage", new Class<?>[]{tlMessage}), savedPath);
                    hookIfPresent(fileLoader, AutomationResolver.resolveOverload("FileLoader", "getPathToMessageOZ", "getPathToMessage"),
                            new Class<?>[]{tlMessage, boolean.class}, savedPath);
                    hookIfPresent(fileLoader, AutomationResolver.resolveOverload("FileLoader", "getPathToMessageOZZ", "getPathToMessage"),
                            new Class<?>[]{tlMessage, boolean.class, boolean.class}, savedPath);
                }


                if (ConfigManager.secretMediaSave.isEnable()) SecretMediaViewer.openMedia();
            }
        } catch (Throwable e){
            Logger.e(e);
        }
    }

    private static void hookIfPresent(Class<?> owner, String name, Class<?>[] params, AbstractMethodHook hook) {
        Method method = params == null ? null : XReflect.findMethodExactIfExists(owner, name, params);
        if (method != null) HMethod.hookMethod(method, hook);
    }

    private static void bindPhotoViewerToActivity(ChatMessageCell cell) {
        try {
            if (!(cell.getChatMessageCell() instanceof View)) return;
            View view = (View) cell.getChatMessageCell();
            Context context = view.getContext();
            Activity activity = extractActivityFromContext(context);
            if (activity == null) return;

            PhotoViewer.getInstance().setParentActivity(activity);
        } catch (Throwable ignored) {}
    }

    private static Activity extractActivityFromContext(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}

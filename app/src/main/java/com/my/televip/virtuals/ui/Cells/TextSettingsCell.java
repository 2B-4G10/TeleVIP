package com.my.televip.virtuals.ui.Cells;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.my.televip.Class.ClassNames;
import com.my.televip.Class.ClassLoad;
import com.my.televip.obfuscate.AutomationResolver;

import com.my.televip.reflect.XReflect;

public class TextSettingsCell {

    public Object textSettingsCell;

    public TextSettingsCell(Context context){
        textSettingsCell = XReflect.newInstance(ClassLoad.getClass(ClassNames.TEXT_SETTINGS_CELL), context);
    }

    public TextSettingsCell(Object obj){
        textSettingsCell = obj;
    }

    public View getView(){
        return (View) textSettingsCell;
    }

    public void setText(CharSequence text, boolean divider){
        XReflect.callMethod(textSettingsCell, AutomationResolver.resolve("TextSettingsCell","setText", AutomationResolver.ResolverType.Method), text, divider);
    }

    public void setTextAndValue(CharSequence text, String value, boolean animated, boolean divider){
        XReflect.callMethod(textSettingsCell, AutomationResolver.resolve("TextSettingsCell","setTextAndValue", AutomationResolver.ResolverType.Method), text, value, animated, divider);
    }

    public TextView getTextView(){
        try {
            return (TextView) XReflect.getObjectField(textSettingsCell,AutomationResolver.resolve("TextSettingsCell","textView", AutomationResolver.ResolverType.Field));
        } catch (Throwable unresolved) {
            // The field name did not resolve for this build; the title is the first TextView.
            return firstTextView((View) textSettingsCell);
        }
    }


    private static TextView firstTextView(View view) {
        if (view instanceof TextView) return (TextView) view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = firstTextView(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }
}

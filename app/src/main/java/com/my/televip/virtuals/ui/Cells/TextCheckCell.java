package com.my.televip.virtuals.ui.Cells;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.obfuscate.AutomationResolver;

import com.my.televip.reflect.XReflect;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class TextCheckCell {

    /**
     * The last state set on each cell. isChecked() is a one-line getter that R8 inlines away in
     * some builds (Nekogram 12.10.3), and wrappers are created afresh on every bind, so the state
     * is remembered per cell - weakly, so recycled cells are not kept alive by it.
     */
    private static final Map<Object, Boolean> LAST_SET = Collections.synchronizedMap(new WeakHashMap<Object, Boolean>());

    public Object textCell;

    public TextCheckCell(Context context){
        textCell = XReflect.newInstance(ClassLoad.getClass(ClassNames.TEXT_CHECK_CELL), context);
    }

    public TextCheckCell(Object obj){
        textCell = obj;
    }

    public void setTextAndValueAndCheck(CharSequence text, String value, boolean checked, boolean multiline, boolean divider){
        LAST_SET.put(textCell, checked);
        XReflect.callMethod(textCell, AutomationResolver.resolve("TextCheckCell","setTextAndValueAndCheck", AutomationResolver.ResolverType.Method), text, value, checked, multiline,  divider);
    }

    public void setTextAndCheck(CharSequence text, boolean checked, boolean divider){
        LAST_SET.put(textCell, checked);
        XReflect.callMethod(textCell, AutomationResolver.resolve("TextCheckCell","setTextAndCheck", AutomationResolver.ResolverType.Method), text, checked,  divider);
    }

    public void setChecked(boolean checked){
        LAST_SET.put(textCell, checked);
        XReflect.callMethod(textCell, AutomationResolver.resolve("TextCheckCell","setChecked", AutomationResolver.ResolverType.Method), checked);
    }

    public boolean isChecked(){
        try {
            return (boolean) XReflect.callMethod(textCell, AutomationResolver.resolve("TextCheckCell","isChecked", AutomationResolver.ResolverType.Method));
        } catch (Throwable inlinedAway) {
            Boolean last = LAST_SET.get(textCell);
            return last != null && last;
        }
    }

    public TextView getTextView(){
        try {
            return (TextView) XReflect.getObjectField(textCell,AutomationResolver.resolve("TextCheckCell","textView", AutomationResolver.ResolverType.Field));
        } catch (Throwable unresolved) {
            // The field name did not resolve for this build; the title is the first TextView.
            return firstTextView((View) textCell);
        }
    }

    public View getView(){
        return (View) textCell;
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

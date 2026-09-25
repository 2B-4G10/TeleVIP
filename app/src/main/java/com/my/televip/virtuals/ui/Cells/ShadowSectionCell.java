package com.my.televip.virtuals.ui.Cells;

import android.content.Context;
import android.view.View;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.reflect.XReflect;

public class ShadowSectionCell {

    Object shadowSectionCell;

    public ShadowSectionCell(Context context){
        Class<?> cls = ClassLoad.getClass(ClassNames.SHADOW_SECTION_CELL);
        if (cls != null) {
            try {
                shadowSectionCell = XReflect.newInstance(cls, context);
            } catch (Throwable ignored) {
                // Falls through to the plain spacer below.
            }
        }
        if (shadowSectionCell == null) {
            // R8 can merge this small cell into a shared class, leaving nothing to construct
            // (Nekogram 12.10.3 does). A plain gap keeps the sections apart all the same.
            View spacer = new View(context);
            spacer.setMinimumHeight((int) (12 * context.getResources().getDisplayMetrics().density));
            spacer.setBackgroundColor(0x0D000000);
            shadowSectionCell = spacer;
        }
    }

    public View getView(){
        return (View) shadowSectionCell;
    }


}

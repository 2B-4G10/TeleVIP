package com.my.televip.settings.ui;

import android.content.Context;
import android.util.TypedValue;

import com.my.televip.virtuals.Theme;
import com.my.televip.virtuals.ui.Cells.ExpandableTextCheckCell;
import com.my.televip.virtuals.ui.Cells.HeaderCell;
import com.my.televip.virtuals.ui.Cells.TextCheckCell;
import com.my.televip.virtuals.ui.Cells.TextInfoCell;
import com.my.televip.virtuals.ui.Cells.TextSettingsCell;

/** The client's own cells, set up as rows of TeleVip's settings list. */
public class SettingsCells {

    /** The client theme's selectableItemBackground, for the rows' touch feedback. */
    private static final TypedValue rowBackground = new TypedValue();

    /** Looks the row background up in {@code context}'s theme; call before creating rows. */
    public static void prepare(Context context) {
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, rowBackground, true);
    }

    public static TextCheckCell createTextCheckCell(Context context) {
        TextCheckCell textCheckCell = new TextCheckCell(context);
        textCheckCell.getView().setBackgroundColor(Theme.getBackgroundWhiteOrBlueColor());
        textCheckCell.getView().setBackgroundResource(rowBackground.resourceId);
        textCheckCell.getView().setClickable(true);
        textCheckCell.getView().setFocusable(true);
        return textCheckCell;
    }

    public static ExpandableTextCheckCell createExpandableTextCheckCell(Context context) {
        ExpandableTextCheckCell expandableTextCheckCell = new ExpandableTextCheckCell(context);
        expandableTextCheckCell.setBackgroundColor(Theme.getBackgroundWhiteOrBlueColor());
        expandableTextCheckCell.setBackgroundResource(rowBackground.resourceId);
        expandableTextCheckCell.setBChildResource(rowBackground.resourceId);
        expandableTextCheckCell.setClickable(true);
        expandableTextCheckCell.setFocusable(true);
        return expandableTextCheckCell;
    }

    public static TextSettingsCell createTextSettingsCell(Context context) {
        TextSettingsCell textSettingsCell = new TextSettingsCell(context);
        textSettingsCell.getView().setBackgroundColor(Theme.getBackgroundWhiteOrBlueColor());
        textSettingsCell.getView().setBackgroundResource(rowBackground.resourceId);
        textSettingsCell.getView().setClickable(true);
        textSettingsCell.getView().setFocusable(true);
        return textSettingsCell;
    }

    public static HeaderCell createHeaderCell(Context context) {
        HeaderCell header = new HeaderCell(context);
        header.getView().setBackgroundColor(Theme.getBackgroundWhiteOrBlueColor());
        return header;
    }

    public static TextInfoCell createTextInfoCell(Context context) {
        TextInfoCell textInfoCell = new TextInfoCell(context);
        textInfoCell.setBackgroundColor(Theme.getBackgroundGrayColor());
        return textInfoCell;
    }
}

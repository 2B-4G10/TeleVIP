package com.my.televip.Drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * TeleVip's app icon - the paper plane from res/drawable/ic_launcher_foreground.xml - for TeleVip's
 * row in the client's settings, whose rounded tile is painted in the icon's yellow
 * ({@link #TILE_COLOR}). Drawn in code because the module's own resources are not available
 * inside the client.
 *
 * <p>Uses the launcher drawable's coordinates (a 108-unit square); keep the two in step. The drop
 * shadow is left out: at 24dp it would be cut off by the icon view's square edge.</p>
 */
public class AppIconDrawable extends Drawable {

    /** The launcher icon's background. */
    public static final int TILE_COLOR = 0xFFFFD500;

    /** The plane with its white keyline: x 19.5..85.5, y 24.5..83.5 in launcher units. */
    private static final float LEFT = 19.5f, TOP = 24.5f, WIDTH = 66f, HEIGHT = 59f;
    /** Room left around the plane, as a share of the icon's size. */
    private static final float INSET = 0.04f;

    private final Path outline = path(80, 30, 69, 78, 51, 66, 42, 76, 35, 57, 25, 48);
    private final Path farWing = path(25, 48, 80, 30, 35, 57);
    private final Path underside = path(35, 57, 80, 30, 42, 76);
    private final Path nearWing = path(80, 30, 69, 78, 51, 66, 42, 76);
    private final Path folds = new Path();

    private final Paint keyline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int alpha = 255;

    public AppIconDrawable() {
        folds.moveTo(80, 30);
        folds.lineTo(35, 57);
        folds.moveTo(80, 30);
        folds.lineTo(42, 76);

        keyline.setStyle(Paint.Style.FILL_AND_STROKE);
        keyline.setColor(0xFFFFFFFF);
        keyline.setStrokeWidth(11);
        keyline.setStrokeJoin(Paint.Join.ROUND);

        fill.setStyle(Paint.Style.FILL);

        ink.setStyle(Paint.Style.STROKE);
        ink.setColor(0xFF231F20);
        ink.setStrokeWidth(4.5f);
        ink.setStrokeJoin(Paint.Join.ROUND);
        ink.setStrokeCap(Paint.Cap.ROUND);
    }

    private static Path path(float... xy) {
        Path p = new Path();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.close();
        return p;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float side = Math.min(b.width(), b.height());
        if (side <= 0) return;
        float scale = side * (1 - 2 * INSET) / WIDTH;

        canvas.save();
        // A logo, so not mirrored in right-to-left layouts.
        canvas.translate(b.exactCenterX(), b.exactCenterY());
        canvas.scale(scale, scale);
        canvas.translate(-(LEFT + WIDTH / 2), -(TOP + HEIGHT / 2));

        canvas.drawPath(outline, keyline);
        drawFace(canvas, farWing, 0xFF565A63);
        drawFace(canvas, underside, 0xFFF99B1C);
        drawFace(canvas, nearWing, 0xFF4A4D54);
        canvas.drawPath(outline, ink);
        canvas.drawPath(folds, ink);
        canvas.restore();
    }

    private void drawFace(Canvas canvas, Path face, int color) {
        fill.setColor(color);
        fill.setAlpha(alpha);
        canvas.drawPath(face, fill);
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        keyline.setAlpha(alpha);
        ink.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        keyline.setColorFilter(colorFilter);
        fill.setColorFilter(colorFilter);
        ink.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @SuppressWarnings("deprecation")   // still abstract in Drawable
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

package com.amazon.apl.android.image.filters.blender;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.amazon.apl.enums.BlendMode;

import java.util.HashMap;
import java.util.Map;

// Android is capable of blending using all the blend modes, but it's only supported
// in API 29 or higher.
@RequiresApi(api = Build.VERSION_CODES.Q)
public class CanvasBlender extends Blender {

    // As per official android guidelines, Paints should be reused.
    private final Paint mPaint;

    static Map<BlendMode, android.graphics.BlendMode> blendModeMap;

    static {
        blendModeMap = new HashMap<>();
        blendModeMap.put(BlendMode.kBlendModeScreen, android.graphics.BlendMode.SCREEN);
        blendModeMap.put(BlendMode.kBlendModeOverlay, android.graphics.BlendMode.OVERLAY);
        blendModeMap.put(BlendMode.kBlendModeDarken, android.graphics.BlendMode.DARKEN);
        blendModeMap.put(BlendMode.kBlendModeLighten, android.graphics.BlendMode.LIGHTEN);
        blendModeMap.put(BlendMode.kBlendModeColorDodge, android.graphics.BlendMode.COLOR_DODGE);
        blendModeMap.put(BlendMode.kBlendModeColorBurn, android.graphics.BlendMode.COLOR_BURN);
        blendModeMap.put(BlendMode.kBlendModeHardLight, android.graphics.BlendMode.HARD_LIGHT);
        blendModeMap.put(BlendMode.kBlendModeSoftLight, android.graphics.BlendMode.SOFT_LIGHT);
        blendModeMap.put(BlendMode.kBlendModeDifference, android.graphics.BlendMode.DIFFERENCE);
        blendModeMap.put(BlendMode.kBlendModeExclusion, android.graphics.BlendMode.EXCLUSION);

        blendModeMap.put(BlendMode.kBlendModeHue, android.graphics.BlendMode.HUE);
        blendModeMap.put(BlendMode.kBlendModeSaturation, android.graphics.BlendMode.SATURATION);
        blendModeMap.put(BlendMode.kBlendModeColor, android.graphics.BlendMode.COLOR);;
        blendModeMap.put(BlendMode.kBlendModeLuminosity, android.graphics.BlendMode.LUMINOSITY);
    }

    public CanvasBlender(BlendMode blendMode) {
        super(blendMode);

        mPaint = new Paint();
        mPaint.setBlendMode(blendModeMap.get(mBlendMode));
    }

    @Override
    public Bitmap performBlending(Bitmap source, Bitmap destination, Bitmap result) {
        Canvas canvas = new Canvas(result);

        canvas.drawBitmap(destination, 0, 0, null);
        canvas.drawBitmap(source, 0, 0, mPaint);

        return result;
    }

    // Unused in this implementation.
    @Override
    int blendPixels(int sourceColor, int destinationColor) {
        return 0;
    }
}

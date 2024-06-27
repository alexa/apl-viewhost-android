package com.amazon.apl.android.sgcontent.filters;

import com.amazon.apl.android.media.MediaObject;
import com.amazon.apl.android.sgcontent.Paint;
import com.amazon.apl.enums.BlendMode;
import com.amazon.apl.enums.NoiseFilterKind;

public abstract class Filter {
    protected long mNativeHandle;

    public String getType() {
        switch(nGetType(mNativeHandle)) {
            case 0: return "Blend";
            case 1: return "Blur";
            case 2: return "Grayscale";
            case 3: return "MediaObject";
            case 4: return "Noise";
            case 5: return "Saturate";
            case 6: return "Solid";
        }
        return "Unknown";
    }

    public static Filter create(long filterHandle) {
        if (filterHandle == 0) {
            return null;
        }
        switch(nGetType(filterHandle)) {
            case 0: return BlendFilter.create(filterHandle, BlendMode.valueOf(nBlendGetBlendMode(filterHandle)), Filter.create(nBlendGetFront(filterHandle)), Filter.create(nBlendGetBack(filterHandle)));
            case 1: return BlurFilter.create(filterHandle, Filter.create(nBlurGetFilter(filterHandle)), nBlurGetRadius(filterHandle));
            case 2: return GrayscaleFilter.create(filterHandle, Filter.create(nGrayscaleGetFilter(filterHandle)), nGrayscaleGetAmount(filterHandle));
            case 3: return MediaObjectFilter.create(filterHandle, MediaObject.ensure(nMediaObjectGetMediaObject(filterHandle)));
            case 4: return NoiseFilter.create(filterHandle, Filter.create(nNoiseGetFilter(filterHandle)), NoiseFilterKind.valueOf(nNoiseGetKind(filterHandle)), nNoiseGetSigma(filterHandle), nNoiseUseColor(filterHandle));
            case 5: return SaturateFilter.create(filterHandle, Filter.create(nSaturateGetFilter(filterHandle)), nSaturateGetAmount(filterHandle));
            case 6: return SolidFilter.create(filterHandle, new Paint(nSolidGetPaint(filterHandle)));
            default: return null;
        }
    }

    private static native int nGetType(long nativeHandle);
    private static native long nMediaObjectGetMediaObject(long nativeHandle);
    private static native float nBlurGetRadius(long nativeHandle);
    private static native long nBlurGetFilter(long nativeHandle);
    private static native float nGrayscaleGetAmount(long nativeHandle);
    private static native long nGrayscaleGetFilter(long nativeHandle);
    private static native long nNoiseGetFilter(long nativeHandle);
    private static native int nNoiseGetKind(long nativeHandle);
    private static native float nNoiseGetSigma(long nativeHandle);
    private static native boolean nNoiseUseColor(long nativeHandle);
    private static native float nSaturateGetAmount(long nativeHandle);
    private static native long nSaturateGetFilter(long nativeHandle);
    private static native long nSolidGetPaint(long nativeHandle);
    private static native long nBlendGetFront(long nativeHandle);
    private static native long nBlendGetBack(long nativeHandle);
    private static native int nBlendGetBlendMode(long nativeHandle);
}

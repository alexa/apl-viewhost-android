package com.amazon.apl.android.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.util.Size;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.graphic.ImageNodeBitmapKey;
import com.amazon.apl.android.image.filters.BlendFilterOperation;
import com.amazon.apl.android.image.filters.BlurFilterOperation;
import com.amazon.apl.android.image.filters.ColorMatrixFilterOperation;
import com.amazon.apl.android.image.filters.NoiseFilterOperation;
import com.amazon.apl.android.image.filters.RenderScriptWrapper;
import com.amazon.apl.android.image.filters.SolidFilterOperation;
import com.amazon.apl.android.image.filters.bitmap.BitmapRegionFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.scenegraph.rendering.APLRender;
import com.amazon.apl.android.sgcontent.filters.BlendFilter;
import com.amazon.apl.android.sgcontent.filters.BlurFilter;
import com.amazon.apl.android.sgcontent.filters.Filter;
import com.amazon.apl.android.sgcontent.filters.GrayscaleFilter;
import com.amazon.apl.android.sgcontent.filters.MediaObjectFilter;
import com.amazon.apl.android.sgcontent.filters.NoiseFilter;
import com.amazon.apl.android.sgcontent.filters.SaturateFilter;
import com.amazon.apl.android.sgcontent.filters.SolidFilter;
import com.amazon.apl.enums.FilterType;
import com.google.common.util.concurrent.Futures;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * Handles queueing, batching, and callbacks for filters.
 */
// TODO: define this as as an interface with no-op default implementation in the RenderingContext
public class ImageFilterProcessor {
    private static final String TAG = "ImageFilterProcessor";
    private final IBitmapCache mBitmapCache;
    private final ExecutorService mExecutorService;
    private Map<DecodedImageBitmapKey, Future<FilterResult>> mPendingDecodeRequests = new HashMap<>();
    private Map<String, List<WeakReference<DecodedImageBitmapKey>>> mDecodedItems = new HashMap<>();
    private RenderScriptWrapper mRenderScriptWrapper;

    private Map<Filter, List<WeakReference<ImageNodeBitmapKey>>> mFiltersProcessed = new HashMap<>();

    private ImageFilterProcessor(IBitmapCache bitmapCache, ExecutorService executorService, RenderScriptWrapper renderScriptWrapper) {
        mBitmapCache = bitmapCache;
        mExecutorService = executorService;
        mRenderScriptWrapper = renderScriptWrapper;
    }

    public static ImageFilterProcessor create(IBitmapCache bitmapCache, ExecutorService executorService, RenderScriptWrapper renderScriptWrapper) {
        return new ImageFilterProcessor(bitmapCache, executorService, renderScriptWrapper);
    }

    public Future<FilterResult> processFilter(RenderingContext renderingContext, Filter filter, float canvasScale, Rect source, com.amazon.apl.android.image.filters.bitmap.Size targetSize) {
        //check cache
        ImageNodeBitmapKey cacheKey = ImageNodeBitmapKey.create(filter, source, targetSize);

        Future<FilterResult> result = checkCacheForFilterResult(renderingContext, cacheKey, source, targetSize);
        if (result == null) {
            result = processFilterInternal(renderingContext, filter, canvasScale, source, targetSize);
            recordFilterProcessed(cacheKey);
        }
        return result;
    }

    private synchronized void recordFilterProcessed(ImageNodeBitmapKey cacheKey) {
        List<WeakReference<ImageNodeBitmapKey>> filterResults = mFiltersProcessed.get(cacheKey.filter());
        if (filterResults == null) {
            filterResults = new LinkedList<>();
            mFiltersProcessed.put(cacheKey.filter(), filterResults);
        }
        filterResults.add(new WeakReference<>(cacheKey));
    }

    private synchronized Future<FilterResult> checkCacheForFilterResult(RenderingContext renderingContext, ImageNodeBitmapKey cacheKey, Rect sourceRegion, com.amazon.apl.android.image.filters.bitmap.Size targetSize) {
        List<WeakReference<ImageNodeBitmapKey>> filterResults = mFiltersProcessed.get(cacheKey.filter());
        if (filterResults != null) {
            for (WeakReference<ImageNodeBitmapKey> cacheEntryRef : filterResults) {
                ImageNodeBitmapKey cacheEntry = cacheEntryRef.get();
                if (cacheEntry != null) {
                    if (isWithin(sourceRegion, cacheEntry.sourceRegion())) {
                        Bitmap cachedBitmap = mBitmapCache.getBitmap(cacheEntry);
                        if (cachedBitmap != null) {
                            float widthRatio = sourceRegion.width() / cacheEntry.sourceRegion().width();
                            float heightRatio = sourceRegion.height() / cacheEntry.sourceRegion().height();

                            int achievableWidth = Math.round(cachedBitmap.getWidth() * widthRatio);
                            int achievableHeight = Math.round(cachedBitmap.getHeight() * heightRatio);

                            if (achievableHeight >= targetSize.height() && achievableWidth >= targetSize.width()) {
                                BitmapRegionFilterResult cachedResult = new BitmapRegionFilterResult(cachedBitmap, renderingContext.getBitmapFactory(), sourceRegion, cacheEntry.sourceRegion());
                                return Futures.immediateFuture(cachedResult);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private Future<FilterResult> processFilterInternal(RenderingContext renderingContext, Filter filter, float canvasScale, Rect source, com.amazon.apl.android.image.filters.bitmap.Size targetSize) {
        if (filter != null) {
            if (filter instanceof MediaObjectFilter) {
                MediaObject mediaObject = ((MediaObjectFilter)filter).mediaObject();
                Size size = mediaObject.getSize();
                if (size != null && size.getHeight() > 0 && size.getHeight() > 0) {
                    return requestBitmap(renderingContext, mediaObject, source, targetSize);
                }
            } else if (filter instanceof SolidFilter) {
                com.amazon.apl.android.sgcontent.Paint paint = ((SolidFilter) filter).paint();
                if (paint != null) {
                    SolidFilterOperation solidFilterOperation = new SolidFilterOperation((Rect bounds) -> {Paint aPaint = new Paint(); APLRender.applyPaintProps(renderingContext, paint, bounds, canvasScale, 1.0f, aPaint); return aPaint;}, renderingContext.getBitmapFactory());
                    return mExecutorService.submit(solidFilterOperation);
                }
            } else if (filter instanceof BlurFilter) {
                BlurFilter blurFilter = ((BlurFilter) filter);
                Future<FilterResult> sourceFilter = processFilterInternal(renderingContext, blurFilter.filter(), canvasScale, source, targetSize);
                if (sourceFilter != null) {
                    List<Future<FilterResult>> sources = new LinkedList<>();
                    sources.add(sourceFilter);
                    Filters.Filter model = Filters.Filter.builder().filterType(FilterType.kFilterTypeBlur)
                            .radius(blurFilter.radius())
                            .build();
                    BlurFilterOperation blurFilterOperation = new BlurFilterOperation(sources, model, renderingContext.getBitmapFactory(), mRenderScriptWrapper, blurFilter.radius() * canvasScale, targetSize);
                    return mExecutorService.submit(blurFilterOperation);
                }
            } else if (filter instanceof GrayscaleFilter) {
                GrayscaleFilter grayscaleFilter = ((GrayscaleFilter) filter);
                Future<FilterResult> sourceFilter = processFilterInternal(renderingContext, grayscaleFilter.filter(), canvasScale, source, targetSize);
                if (sourceFilter != null) {
                    List<Future<FilterResult>> sources = new LinkedList<>();
                    sources.add(sourceFilter);
                    Filters.Filter model = Filters.Filter.builder().filterType(FilterType.kFilterTypeGrayscale)
                            .amount(grayscaleFilter.amount())
                            .build();
                    ColorMatrixFilterOperation colorMatrixFilterOperation = new ColorMatrixFilterOperation(sources, model, renderingContext.getBitmapFactory(), mRenderScriptWrapper, targetSize);
                    return mExecutorService.submit(colorMatrixFilterOperation);
                }
            } else if (filter instanceof SaturateFilter) {
                SaturateFilter saturateFilter = ((SaturateFilter) filter);
                Future<FilterResult> sourceFilter = processFilterInternal(renderingContext, saturateFilter.filter(), canvasScale, source, targetSize);
                if (sourceFilter != null) {
                    List<Future<FilterResult>> sources = new LinkedList<>();
                    sources.add(sourceFilter);
                    Filters.Filter model = Filters.Filter.builder().filterType(FilterType.kFilterTypeSaturate)
                            .amount(saturateFilter.amount())
                            .build();
                    ColorMatrixFilterOperation colorMatrixFilterOperation = new ColorMatrixFilterOperation(sources, model, renderingContext.getBitmapFactory(), mRenderScriptWrapper, targetSize);
                    return mExecutorService.submit(colorMatrixFilterOperation);
                }
            } else if (filter instanceof NoiseFilter) {
                NoiseFilter noiseFilter = ((NoiseFilter) filter);
                Future<FilterResult> sourceFilter = processFilterInternal(renderingContext, noiseFilter.filter(), canvasScale, source, targetSize);
                if (sourceFilter != null) {
                    List<Future<FilterResult>> sources = new LinkedList<>();
                    sources.add(sourceFilter);
                    Filters.Filter model = Filters.Filter.builder().filterType(FilterType.kFilterTypeNoise)
                            .noiseSigma(noiseFilter.sigma())
                            .noiseKind(noiseFilter.kind())
                            .noiseUseColor(noiseFilter.useColor())
                            .build();

                    // TODO: Get clarification if noise should be applied to pre/post scaling image
                    // Currently vh is doing it pre-scaling which results in large patches of noise
                    // if the image is scaled up
                    NoiseFilterOperation noiseFilterOperation = new NoiseFilterOperation(sources, model, renderingContext.getBitmapFactory(), targetSize);
                    return mExecutorService.submit(noiseFilterOperation);
                }
            } else if (filter instanceof BlendFilter) {
                BlendFilter blendFilter = ((BlendFilter) filter);
                // TODO: Get clarification of how scaling should happen for source/dest
                Future<FilterResult> backImg = processFilterInternal(renderingContext, blendFilter.backFilter(), canvasScale, source, targetSize);

                // back calculate the resolution we previously would have downloaded this image at
//                    float widthPercentage = targetSize.width() / (float) source.getSize().width();
//                    float heightPercentage = targetSize.height() / (float) source.getSize().height();
//                    float scale = Math.min(1f, Math.max(widthPercentage, heightPercentage));
//                    Rect sourceRegion = new Rect(0, 0, Math.round((float) targetSize.width() / scale), Math.round((float) targetSize.height() / scale));
                Future<FilterResult> frontImg = processFilterInternal(renderingContext, blendFilter.frontFilter(), canvasScale, source, targetSize);
                if (backImg != null && frontImg != null) {
                    List<Future<FilterResult>> sources = new LinkedList<>();
                    sources.add(frontImg);
                    sources.add(backImg);
                    BlendFilterOperation blendFilterOperation = new BlendFilterOperation(sources, blendFilter.blendMode(), renderingContext.getBitmapFactory(), mRenderScriptWrapper, targetSize);
                    return mExecutorService.submit(blendFilterOperation);
                }
            }
        }

        return null;
    }

    /**
     * Requests a particular region of a source image to decode and the desired resolution for the
     * resulting bitmap. If a bitmap satisfying the request is not immediately available,
     * the specified layer will be redrawn when the request is ready.
     *
     * @param mediaObject identifies the source image
     * @param decodeRegionRequested the region of the source image to populate in the bitmap
     * @param targetSize the desired resolution for the resulting Bitmap. We will not upsample the source image region,
     *                   so resulting bitmap size may be smaller than the specified targetSize.
     * @return a bitmap containing the request region of the source image if available, else null
     */
    private synchronized Future<FilterResult> requestBitmap(RenderingContext renderingContext, MediaObject mediaObject, Rect decodeRegionRequested, com.amazon.apl.android.image.filters.bitmap.Size targetSize) {
        Size mediaSize = mediaObject.getSize();
        if (decodeRegionRequested == null || decodeRegionRequested.right > mediaSize.getWidth() || decodeRegionRequested.bottom > mediaSize.getHeight()) {
            decodeRegionRequested = new Rect(0,0, mediaSize.getWidth(), mediaSize.getHeight());
        }

        final Rect decodeRegionRequestedNonNull = decodeRegionRequested;

        int neededSampleSize = calculateSampleSize(decodeRegionRequested, targetSize);

        // check if we already have the requested region at the needed sample size
        DecodeResult decodeResult = checkCacheForSourceRegion(mediaObject, decodeRegionRequested, neededSampleSize);
        if (decodeResult != null) {
            return Futures.immediateFuture(new BitmapRegionFilterResult(decodeResult.getBitmap(), renderingContext.getBitmapFactory(), decodeRegionRequested, decodeResult.mDecodedRegion));
        }

        Rect calculatedDecodeRegion = calculateDecodeRegion(decodeRegionRequested, mediaObject.getSize());
        DecodedImageBitmapKey cacheKey = DecodedImageBitmapKey.create(mediaObject.getUrl(), calculatedDecodeRegion, neededSampleSize);

        // A resizing image can easily fill up the thread pool with duplicate requests
        Future<FilterResult> futureResult = mPendingDecodeRequests.get(cacheKey);
        if (futureResult == null) {
            try {
                futureResult = mExecutorService.submit(() -> decode(renderingContext, new DecodeRequest(mediaObject, decodeRegionRequestedNonNull, calculatedDecodeRegion, targetSize, cacheKey)));
                mPendingDecodeRequests.put(cacheKey, futureResult);
            } catch (RejectedExecutionException ex) {
                Log.e(TAG, "Unable to submit image decode request", ex);
            }
        }

        return futureResult;
    }

    /**
     * Check for previously completed decoding results to see whether there is already a matching
     * image in the request.
     * @param mediaObject identifies the source for the image
     * @param decodeRegionRequested the region from the source image needed
     * @param sampleSize the maximum sampleSize that is acceptable for the image
     * @return the cached DecodeResult if available, otherwise null
     */
    private DecodeResult checkCacheForSourceRegion(MediaObject mediaObject, Rect decodeRegionRequested, int sampleSize) {
        List<WeakReference<DecodedImageBitmapKey>> previousDecodingsOfMediaObject = mDecodedItems.get(mediaObject.getUrl());
        if (previousDecodingsOfMediaObject != null) {
            for (WeakReference<DecodedImageBitmapKey> decodedItemRef : previousDecodingsOfMediaObject) {
                DecodedImageBitmapKey decodedItem = decodedItemRef.get();
                if (decodedItem != null
                        && decodedItem.sampleSize() <= sampleSize
                        && isWithin(decodeRegionRequested, decodedItem.decodeRegion())) {
                    Bitmap bitmap  = mBitmapCache.getBitmap(decodedItem);
                    if (bitmap != null) {
                        DecodeResult decodeResult = new DecodeResult();
                        decodeResult.mBitmap = bitmap;
                        decodeResult.mDecodedRegion = decodedItem.decodeRegion();
                        decodeResult.mSampleSize = decodedItem.sampleSize();
                        return decodeResult;
                    }
                }
            }
        }
        return null;
    }

    private Rect calculateDecodeRegion(Rect decodeRegionRequested, Size sourceSize) {
        // this is a simple heuristic which selects the region area to be decoded based on the next
        // smallest/largest power of two for each coordinate. Some nice properties of this simple
        // heuristic are that it is stateless and supports growing the decoded area just in the
        // direction required.
        // TODO: Get feedback and suggestions for alternatives
        int powerOfTwoWidth = Integer.highestOneBit(decodeRegionRequested.right) << 1;
        int powerOfTwoHeight = Integer.highestOneBit(decodeRegionRequested.bottom) << 1;
        int maxWidth = Math.min(sourceSize.getWidth(), powerOfTwoWidth);
        int maxHeight = Math.min(sourceSize.getHeight(), powerOfTwoHeight);

        int powerOfTwoLeft = Integer.highestOneBit(decodeRegionRequested.left) >> 1;
        int powerOfTwoTop = Integer.highestOneBit(decodeRegionRequested.top) >> 1;
        int left = Math.max(0, powerOfTwoLeft);
        int top = Math.max(0, powerOfTwoTop);
        return new Rect(left,top, maxWidth, maxHeight);
    }

    private boolean isWithin(Rect regionRequested, Rect decodedRegion) {
        return decodedRegion.left <= regionRequested.left
                && decodedRegion.right >= regionRequested.right
                && decodedRegion.top <= regionRequested.top
                && decodedRegion.bottom >= regionRequested.bottom;
    }

    private synchronized BitmapRegionFilterResult decode(RenderingContext renderingContext, DecodeRequest decodeRequest) {
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = decodeRequest.mCacheKey.sampleSize();

        // decoding took ~20ms for 1000x1000 image on a crown
        try (FileInputStream fis = new FileInputStream(decodeRequest.mMediaObject.getFile())) {
            BitmapRegionDecoder bitmapRegionDecoder = BitmapRegionDecoder.newInstance(fis, false);
            Bitmap bm = bitmapRegionDecoder.decodeRegion(decodeRequest.mDecodeRegion, decodeOptions);
            mBitmapCache.putBitmap(decodeRequest.mCacheKey, bm);
            List<WeakReference<DecodedImageBitmapKey>> decoded = mDecodedItems.get(decodeRequest.mCacheKey.sourceUrl());
            if (decoded == null) {
                decoded = new LinkedList<>();
                mDecodedItems.put(decodeRequest.mCacheKey.sourceUrl(), decoded);
            }

            decoded.add(new WeakReference<>(decodeRequest.mCacheKey));
            return new BitmapRegionFilterResult(bm, renderingContext.getBitmapFactory(), decodeRequest.mDecodeRegionRequested,  decodeRequest.mCacheKey.decodeRegion());
        } catch (IOException ex) {
            Log.e(TAG, "There was a problem decoding a region of the requested image");
            // do we need a fallback decoder, e.g. BitmapFactory, here?
            return null;
        } finally {
            mPendingDecodeRequests.remove(decodeRequest.mCacheKey);
        }
    }

    /**
     * Determine what sampleSize will be sufficient given the image region to decode and the
     * target screen area.
     * @param sourceRegion the area of the source image to decode
     * @param targetSize the area which we are targeting the content to (in pixels)
     * @return the sampleSize to use when decoding the specified sourceRegion for the needed
     * targetSize
     */
    private int calculateSampleSize(Rect sourceRegion, com.amazon.apl.android.image.filters.bitmap.Size targetSize) {
        int targetWidth = targetSize.width();
        int targetHeight = targetSize.height();
        int decodeWidth = sourceRegion.width();
        int decodeHeight = sourceRegion.height();
        if (targetWidth < decodeWidth && targetHeight < decodeHeight) {
            int sampleSize = Math.min(decodeWidth/targetWidth, decodeHeight/targetHeight);
            // round down to power of two
            return Math.max(1, Integer.highestOneBit(sampleSize) >> 1);
        } else {
            return 1;
        }
    }

    /**
     * BitmapRegionDecoder has some known issues so keeping BitmapFactory as the fallack decoder:
     * https://issuetracker.google.com/issues/37006509
     */
    private synchronized void decodeUsingBitmapFactory(DecodeRequest decodeRequest, BitmapFactory.Options decodeOptions) {
        try (FileInputStream fis = new FileInputStream(decodeRequest.mMediaObject.getFile())) {
            Bitmap bm = BitmapFactory.decodeStream(fis, null, decodeOptions);
            // Create new key to reflect that we have decoded the entire image
            DecodedImageBitmapKey bitmapKey = DecodedImageBitmapKey.create(decodeRequest.mCacheKey.sourceUrl(),
                    new Rect(0, 0, bm.getWidth(), bm.getHeight()) ,decodeRequest.mCacheKey.sampleSize());
            mBitmapCache.putBitmap(bitmapKey, bm);
        } catch (IOException ex) {
            Log.e(TAG, "There was a problem decoding the requested image");
        }
    }

    private static class DecodeRequest {
        MediaObject mMediaObject;
        Rect mDecodeRegionRequested;
        Rect mDecodeRegion;
        com.amazon.apl.android.image.filters.bitmap.Size mTargetSize;
        DecodedImageBitmapKey mCacheKey;

        /**
         * Captures all of the necessary details to decode a MediaObject.
         *
         * @param mediaObject the MediaObject containing image data references to decode
         * @param decodeRegion the region of the image to decode
         * @param targetSize the requested bitmap size in pixels, this should account for any
         *                   dp/canvas scaling required
         */
        public DecodeRequest(MediaObject mediaObject, Rect decodeRegionRequested, Rect decodeRegion, com.amazon.apl.android.image.filters.bitmap.Size targetSize, DecodedImageBitmapKey cacheKey) {
            mMediaObject = mediaObject;
            mDecodeRegionRequested = decodeRegionRequested;
            mDecodeRegion = decodeRegion;
            mTargetSize = targetSize;
            mCacheKey = cacheKey;
        }
    }

    private static class DecodeResult {
        Bitmap mBitmap;
        Rect mDecodedRegion;
        int mSampleSize;

        public Bitmap getBitmap() {
            return mBitmap;
        }

        /**
         * Translate the region from the source into coordinates relative to the actual decoded region
         * taking into account partial decoding and down sampling.
         * @param sourceRegion the requested area from the source image
         * @return a Rect which maps the decoded image content corresponding to the image area in the sourceRegion
         */
        public Rect getDecodedRegion(Rect sourceRegion) {
            if (mDecodedRegion.equals(sourceRegion)) {
                return null;
            }

            int translatedLeft = (sourceRegion.left / mSampleSize) - mDecodedRegion.left;
            int translatedTop = (sourceRegion.top / mSampleSize) - mDecodedRegion.top;
            return new Rect(translatedLeft, translatedTop, translatedLeft+(sourceRegion.width() / mSampleSize), translatedTop+(sourceRegion.height() / mSampleSize));
        }
    }
}

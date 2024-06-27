package com.amazon.apl.android.scenegraph.rendering;

import static android.graphics.Paint.FILTER_BITMAP_FLAG;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.graphics.RectF;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.APLVersionCodes;
import com.amazon.apl.android.ITextProxy;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.StaticLayoutBuilder;
import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.font.FontConstant;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.media.MediaObject;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.bitmap.BitmapKey;
import com.amazon.apl.android.graphic.ImageNodeBitmapKey;
import com.amazon.apl.android.image.filters.bitmap.BitmapFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.image.filters.bitmap.Size;
import com.amazon.apl.android.primitive.SGRRect;
import com.amazon.apl.android.primitive.SGRect;
import com.amazon.apl.android.scenegraph.APLLayer;
import com.amazon.apl.android.scenegraph.text.APLTextLayout;
import com.amazon.apl.android.scenegraph.text.APLTextProperties;
import com.amazon.apl.android.sgcontent.filters.Filter;
import com.amazon.apl.android.sgcontent.Node;
import com.amazon.apl.android.sgcontent.PathOp;
import com.amazon.apl.android.thread.Threading;
import com.amazon.apl.enums.GradientSpreadMethod;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public class APLRender {
    private static final String TAG = "APLRender";
    private static final Paint sBitmapPaint = new Paint(FILTER_BITMAP_FLAG);
    /**
     * A bitmask that defines the bits android uses for alpha.
     */
    private static final int ALPHA_BITMASK = 0xFF000000;

    public static void drawNode(APLLayer aplLayer, RenderingContext renderingContext, Node node, float opacity, Canvas canvas) {

        if (!node.isVisible()) {
            return;
        }
        switch (node.getType()) {
            case "Draw": {
                PathOp pathOp = node.getOp();
                do {
                    drawPath(renderingContext, canvas, pathOp, opacity, node);
                } while ((pathOp = pathOp.getNextSibbling()) != null);

                drawNodeChildren(aplLayer, renderingContext, node, opacity, canvas);
                break;
            }
            case "Opacity": {
                float nodeOpacity = node.getOpacity();
                // Opacity can be specified in the document, hence it should be checked for valid range
                if (nodeOpacity > 0.0f) {
                    if (nodeOpacity < 1.0f) {
                        drawNodeChildren(aplLayer, renderingContext, node, opacity * node.getOpacity(), canvas);
                    } else {
                        drawNodeChildren(aplLayer, renderingContext, node, opacity, canvas);
                    }
                }
                break;
            }
            case "Transform": {
                canvas.save();
                canvas.concat(node.getTransform());
                drawNodeChildren(aplLayer, renderingContext, node, opacity, canvas);
                canvas.restore();
                break;
            }
            case "Clip": {
                canvas.save();
                Path path = convertPath(node.getClipPath());
                canvas.clipPath(path);
                drawNodeChildren(aplLayer, renderingContext, node, opacity, canvas);
                canvas.restore();
                break;
            }
            case "Shadow": {
                Canvas shadowCanvas = canvas;
                Bitmap shadowChildrenBitmap = null;
                float scale = calculateCanvasScale(canvas);
                try {
                    // TODO: get actual bounds
                    Rect bounds = canvas.getClipBounds();
                    shadowChildrenBitmap = renderingContext.getBitmapFactory().createBitmap(Math.round(bounds.width()*scale),Math.round(bounds.height()*scale));
                    shadowCanvas = new Canvas(shadowChildrenBitmap);
                    shadowCanvas.scale(scale,scale);
                } catch (BitmapCreationException ex) {
                    Log.e(TAG, "Unable to allocate bitmap for Shadows");
                }

                drawNodeChildren(aplLayer, renderingContext, node, opacity, shadowCanvas);

                if (shadowChildrenBitmap != null) {
                    drawShadow(shadowCanvas, shadowChildrenBitmap, node, scale);
                    Matrix m = new Matrix();
                    m.preScale(1/scale, 1/scale);
                    canvas.drawBitmap(shadowChildrenBitmap, m, null);
                }
                break;
            }
            case "Text": {
                APLTextLayout textLayout = node.getAplTextLayout();
                if (textLayout != null) {
                    APLTextProperties textProperties = textLayout.getTextProperties();
                    try {
                        Layout androidTextLayout = textLayout.getLayout();
                        PathOp pathOp = node.getOp();
                        do {
                            /**
                             * Since {@link Layout#getPaint()} asks the Paint to be accessed for reading only,
                             * we need to create the paint and copy the layout here again.
                             */
                            TextPaint paint = new TextPaint(androidTextLayout.getPaint());
                            float scaleForPattern = calculateCanvasScale(canvas);
                            if (shouldComputeBounds(pathOp)) {
                                applyPaintProps(renderingContext, pathOp.getPaint(), computeBounds(androidTextLayout), scaleForPattern, opacity, paint);
                            } else {
                                applyPaintProps(renderingContext, pathOp.getPaint(), null, scaleForPattern, opacity, paint);
                            }
                            updatePaintFromPathOp(pathOp, null,  paint);
                            StaticLayout layout = createStaticLayout(renderingContext.getDocVersion(),
                                    androidTextLayout.getText(),
                                    paint,
                                    textProperties,
                                    androidTextLayout.getWidth(),
                                    androidTextLayout.getHeight(),
                                    androidTextLayout.getEllipsizedWidth());
                            if (layout != null) {
                                layout.draw(canvas);
                            }
                        } while ((pathOp = pathOp.getNextSibbling()) != null);
                    } catch (StaticLayoutBuilder.LayoutBuilderException e) {
                        Log.e(TAG, "Could not copy the text layout ", e);
                    }
                } else {
                    Log.e(TAG, "Text layout is null, cannot draw text content");
                }

                drawNodeChildren(aplLayer, renderingContext, node, opacity, canvas);
                break;
            }
            case "Image": {
                Filter filter = node.getFilter();
                if (filter != null) {
                    SGRect source = node.getSourceRect();
                    SGRect target = node.getTargetRect();
                    float scale = calculateCanvasScale(canvas);
                    com.amazon.apl.android.image.filters.bitmap.Size targetSize = com.amazon.apl.android.image.filters.bitmap.Size.create(Math.round(target.intWidth() * scale), Math.round(target.intHeight() * scale));
                    Rect sRect = source == null ? null : new Rect(source.intLeft(), source.intTop(), source.intRight(), source.intBottom());

                    BitmapKey filterKey = buildFilterKey(filter, sRect, targetSize);
                    if (target.getHeight() > 0 && target.getWidth() > 0 && (node.mFilterKey == null || !node.mFilterKey.equals(filterKey))) {

                            Bitmap filteredBitmap = renderingContext.getBitmapCache().getBitmap(filterKey);
                            if (filteredBitmap != null) {
                               drawBitmap(canvas, filteredBitmap, target);
                            } else {
                                final Future<FilterResult> filterResultFuture = renderingContext.getImageFilterProcessor().processFilter(renderingContext, filter, scale, sRect, targetSize);
                                // tracks that we have already submitted a request to process the filter/s for this node
                                if (filterResultFuture.isDone()) {
                                    try {
                                        FilterResult filterResult = filterResultFuture.get();
                                        drawBitmap(canvas, filterResult.getBitmap(targetSize), target);
                                    } catch (InterruptedException | ExecutionException ex) {
                                        // These checked exceptions should not happen for cached results
                                        Log.wtf(TAG, "Unexpected exception with cached filter processing result", ex);
                                    }
                                } else {
                                    waitForFilterResult(aplLayer, renderingContext, filterResultFuture, filterKey, targetSize, node);
                                }
                            }
                    }
                }
                drawNodeChildren(aplLayer, renderingContext, node, opacity, canvas);
                break;
            }
            default: {
                drawNodeChildren(aplLayer, renderingContext, node, opacity, canvas);
            }
        }
    }

    private static boolean shouldComputeBounds(PathOp pathOp) {
        // Bounds are needed to calculate the Paint Shader, which is needed only for Gradient.
        return !"Color".equals(pathOp.getPaint().getType());
    }

    /**
     * Creates a new static layout for a Text component
     */
    private static StaticLayout createStaticLayout(int versionCode, CharSequence text,
                                            TextPaint textPaint, ITextProxy proxy,
                                            final int layoutWidth, final int layoutHeight,
                                            int ellipsizedWidth) throws StaticLayoutBuilder.LayoutBuilderException {
        final boolean limitLines = proxy.limitLines();
        final int maxLines = proxy.getMaxLines();

        StaticLayoutBuilder.Builder builder = StaticLayoutBuilder.create().
                text(text).
                textPaint(textPaint).
                lineSpacing(proxy.getLineHeight()).
                innerWidth(layoutWidth).
                alignment(proxy.getTextAlignment()).
                limitLines(limitLines).
                maxLines(maxLines).
                ellipsizedWidth(ellipsizedWidth).
                textDirection(proxy.getDirectionHeuristic()).
                aplVersionCode(versionCode);

        StaticLayout textLayout = builder.build();

        // In case the text does not fit into the view, we need to indicate the user that there is more
        // text remaining now being shown. We'd proceed truncating the text until the last fully
        // visible line with an ellipsis.
        //
        // After creating the layout, we are able to know the final height of
        // the entire spannable text. Truncates text if layout exceeds the view
        // box dimensions.
        final int staticLayoutHeight = textLayout.getHeight();

        if (!limitLines && staticLayoutHeight > layoutHeight) {
            final int linesNeeded = textLayout.getLineCount();
            final int lineHeight = staticLayoutHeight / linesNeeded;
            final int linesFullyVisible = layoutHeight / lineHeight;

            // Create a new and similar layout but setting maxLines param.
            builder.limitLines(true).maxLines(linesFullyVisible);
            textLayout = builder.build();
        }

        return textLayout;
    }

    private static void drawBitmap(Canvas canvas, Bitmap filteredBitmap, SGRect target) {
        RectF tRect = new RectF(target.getLeft(), target.getTop(), target.getRight(), target.getBottom());
        canvas.drawBitmap(filteredBitmap, null, tRect, sBitmapPaint);
    }

    private static BitmapKey buildFilterKey(Filter filter, Rect sourceRegion, Size targetSize) {
        return ImageNodeBitmapKey.create(filter, sourceRegion, targetSize);
    }

    private static void waitForFilterResult(APLLayer aplLayer, RenderingContext renderingContext, Future<FilterResult> filterResultFuture, BitmapKey filterKey, Size targetSize, Node node) {
        node.mFilterKey = filterKey;
        try {
            Threading.THREAD_POOL_EXECUTOR.submit(() -> {
                try {
                    FilterResult filterResult = filterResultFuture.get();
                    renderingContext.getBitmapCache().putBitmap(filterKey, filterResult.getBitmap(targetSize));
                } catch (InterruptedException |
                         ExecutionException ex) {
                    Log.e(TAG, "Exception in filter processing task", ex);
                } finally {
                    node.mFilterKey = null;
                    // trigger a redraw for the layer when the bitmap is ready
                    aplLayer.forceUpdate();
                }
            });
        } catch (RejectedExecutionException ex) {
            Log.e(TAG, "Exception submitting filter processing task", ex);
        }
    }

    private static void drawNodeChildren(APLLayer aplLayer, RenderingContext renderingContext, Node node, float opacity, Canvas canvas) {
        for (Node n : node.getChildren()) {
            drawNode(aplLayer, renderingContext, n, opacity, canvas);
        }
    }

    public static Path convertPath(com.amazon.apl.android.sgcontent.Path path) {
        Path path2 = new Path();
        switch(path.getType()) {
            case "General": {
                float[] points = path.getPoints();
                String value = path.getValue();
                path2.moveTo(0,0);
                int i = 0;
                for (char c : value.toCharArray()) {
                    switch (c) {
                        case 'C':   // Cubic bezier
                            path2.cubicTo(points[i++], points[i++], points[i++], points[i++], points[i++], points[i++]);
                            break;
                        case 'L': // LineTo
                            path2.lineTo(points[i++], points[i++]);
                            break;
                        case 'M': // Move To
                            path2.moveTo(points[i++], points[i++]);
                            break;
                        case 'Q': // Quadratic bezier curve
                            path2.quadTo(points[i++], points[i++], points[i++], points[i++]);
                            break;
                        case 'Z': // Close path
                            path2.close();
                            break;
                        default:
                            Log.e("APLRender", "Unrecognized path character " + c );
                            break;
                    }
                }
                break;
            }
            case "Frame": {
                // outer
                SGRRect rrect = path.getFramePathRRect();
                float[] radii = rrect.getRadii();
                path2.addRoundRect(rrect.getLeft(), rrect.getTop(), rrect.getRight(), rrect.getBottom(), new float[]{radii[0], radii[0],radii[1], radii[1],radii[2], radii[2],radii[3], radii[3]}, Path.Direction.CW);

                // inner
                rrect = path.getFramePathInset();
                radii = rrect.getRadii();
                path2.addRoundRect(rrect.getLeft(), rrect.getTop(), rrect.getRight(), rrect.getBottom(), new float[]{radii[0], radii[0], radii[1], radii[1], radii[2], radii[2], radii[3], radii[3]}, Path.Direction.CW);
                break;
            }
            case "Rect": {
                RectF rect = path.getRectPathRect();
                path2.addRect(rect, Path.Direction.CW);
                break;
            }
            case "RRect": {
                SGRRect rrect = path.getRRectPathRRect();
                float[] radii = rrect.getRadii();
                path2.addRoundRect(rrect.getLeft(), rrect.getTop(), rrect.getRight(), rrect.getBottom(), new float[]{radii[0], radii[0],radii[1], radii[1],radii[2], radii[2],radii[3], radii[3]}, Path.Direction.CW);
                break;
            }
        }

        return path2;
    }

    private static void drawShadow(Canvas shadowCanvas, Bitmap shadowChildrenBitmap, Node shadowNode, float scale) {
        float scaledRadius = shadowNode.getRadius() * scale;
        if(scaledRadius < 0) return;

        //final Canvas shadowCanvas = new Canvas(shadowChildrenBitmap);
        final Paint shadowPaint = new Paint();
        if (scaledRadius != 0) {
            shadowPaint.setMaskFilter(new BlurMaskFilter(scaledRadius, BlurMaskFilter.Blur.NORMAL));
        }
        int[] bitmapOffsetXY = new int[2]; // populated by extractAlpha to align returned bitmap with the original
        final Bitmap alphaBlurredBitmap = shadowChildrenBitmap.extractAlpha(shadowPaint, bitmapOffsetXY);

        final Paint transferPaint = new Paint();
        // Fix the alpha component, which will be calculated in the PorterDuffColorFilter
        int color = shadowNode.getShadowColor();
        transferPaint.setColor(color | ALPHA_BITMASK);
        // Fix the color of src, but take the alpha component from the src color
        transferPaint.setColorFilter(new PorterDuffColorFilter(~ALPHA_BITMASK | color, PorterDuff.Mode.DST_IN));
        transferPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));

        float[] shadowOffset = shadowNode.getShadowOffset();
        shadowCanvas.translate(shadowOffset[0], shadowOffset[1]);

        Matrix m = new Matrix();
        m.preScale(1/scale,1/scale);
        m.preTranslate(bitmapOffsetXY[0], bitmapOffsetXY[1]);
        shadowCanvas.drawBitmap(alphaBlurredBitmap, m, transferPaint);
        alphaBlurredBitmap.recycle();
    }

    private static void drawPath(RenderingContext renderingContext, Canvas canvas, PathOp pathOp, float opacity, Node node) {
        Path path = convertPath(node.getPath());
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float scaleForPattern = calculateCanvasScale(canvas);
        applyPaintProps(renderingContext, pathOp.getPaint(), computeBounds(path), scaleForPattern, opacity, paint);
        updatePaintFromPathOp(pathOp, path, paint);
        canvas.drawPath(path, paint);
        // TODO: Uncomment the below lines if there are visual differences due to scaling problems with Hardware Acceleration.
        /*if ("General".equals(node.getPath().getType())) {
            updatePaintFromPathOp(pathOp, path, paint);
            canvas.drawPath(path, paint);
        } else {
            paint.setStyle(Paint.Style.STROKE);
            drawSimplePath(node.getPath(), canvas, paint);
        }*/
    }

    /**
     * Draws rectangular and arc shapes using simple drawShapes calls as documented in
     * https://developer.android.com/topic/performance/hardware-accel#scaling
     * TODO: Right now this method is unused
     * @param path
     * @param canvas
     * @param paint
     */
    private static void drawSimplePath(com.amazon.apl.android.sgcontent.Path path, Canvas canvas, Paint paint) {
        switch(path.getType()) {
            case "Frame": {
                // outer
                SGRRect outerRect = path.getFramePathRRect();
                float[] outerRadii = outerRect.getRadii();
                float outerX = outerRect.getLeft();
                drawRoundedRect(canvas, outerRect, paint);
                //path2.addRoundRect(rrect.getLeft(), rrect.getTop(), rrect.getRight(), rrect.getBottom(), new float[]{radii[0], radii[0],radii[1], radii[1],radii[2], radii[2],radii[3], radii[3]}, Path.Direction.CW);

                // inner
                SGRRect innerRect = path.getFramePathInset();
                float innerX = innerRect.getLeft();
                drawRoundedRect(canvas, innerRect, paint);
                int count = 0;
                for (float i = outerX; i <= innerX; i++) {
                    count++;
                }
                float left = outerRect.getLeft() + 1.0f;
                float top = outerRect.getTop() + 1.0f;
                float right = outerRect.getRight() - 1.0f;
                float bottom = outerRect.getBottom() - 1.0f;

                float radiusLeftTop = outerRadii[0] - 1.0f;
                float radiusRightTop = outerRadii[1] - 1.0f;
                float radiusRightBottom = outerRadii[2] - 1.0f;
                float radiusLeftBottom = outerRadii[3] - 1.0f;
                for (int i = 1; i < count; i++) {
                    SGRRect rrect = SGRRect.create(new float[] {left, top, right, bottom}, new float[] {radiusLeftTop, radiusRightTop, radiusRightBottom, radiusLeftBottom});
                    drawRoundedRect(canvas, rrect, paint);
                    left++;
                    top++;
                    right--;
                    bottom--;

                    radiusLeftTop--;
                    radiusRightTop--;
                    radiusRightBottom--;
                    radiusLeftBottom--;
                }
                break;
            }
            case "Rect": {
                RectF rect = path.getRectPathRect();
                canvas.drawRect(rect, paint);
                break;
            }
            case "RRect": {
                SGRRect rrect = path.getRRectPathRRect();
                drawRoundedRect(canvas, rrect, paint);
                break;
            }
        }
    }

    private static void drawRoundedRect(Canvas canvas, SGRRect rrect, Paint paint) {
        float[] radii = rrect.getRadii();
        canvas.drawLine(rrect.getLeft() + radii[0], rrect.getTop(), rrect.getRight() - radii[1], rrect.getTop(), paint);
        canvas.drawArc(rrect.getRight() - 2 * radii[1], rrect.getTop(), rrect.getRight(), rrect.getTop() + 2 * radii[1], 270, 90, false, paint);
        canvas.drawLine(rrect.getRight(), rrect.getTop() + radii[1], rrect.getRight(), rrect.getBottom() - radii[2], paint);
        canvas.drawArc(rrect.getRight() - 2 * radii[2], rrect.getBottom() - 2 * radii[2], rrect.getRight(), rrect.getBottom(), 0, 90, false, paint);
        canvas.drawLine(rrect.getRight() - radii[2], rrect.getBottom(), rrect.getLeft() + radii[3], rrect.getBottom(), paint);
        canvas.drawArc(rrect.getLeft(), rrect.getBottom() - 2 * radii[3], rrect.getLeft() + 2 * radii[3], rrect.getBottom(), 90, 90, false, paint);
        canvas.drawLine(rrect.getLeft(), rrect.getBottom() - radii[3], rrect.getLeft(), rrect.getTop() + radii[0], paint);
        canvas.drawArc(rrect.getLeft(), rrect.getTop(), rrect.getLeft() + 2 * radii[0], rrect.getTop() + 2 * radii[0], 180, 90, false, paint);
    }

    private static Paint updatePaintFromPathOp(PathOp pathOp, Path optionalPath, Paint paint) {
        switch(pathOp.getType()) {
            case "Stroke": {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(pathOp.getStrokeWidth());
                paint.setStrokeMiter(pathOp.getMiterLimit());
                applyStrokeCap(pathOp, paint);
                applyStrokeJoin(pathOp, paint);

                if (pathOp.getStrokeDashArray().length > 0) {
                    if (optionalPath != null) {
                        paint.setPathEffect(createDashPathEffect(optionalPath, pathOp));
                    } else {
                        Log.w(TAG, "StrokeDashArray is only supported for Paths");
                    }
                }
                break;
            }
            case "Fill": {
                paint.setStyle(Paint.Style.FILL);
                if (optionalPath != null) {
                    if (pathOp.getFillType() == 0) {
                        optionalPath.setFillType(Path.FillType.EVEN_ODD);
                    } else {
                        optionalPath.setFillType(Path.FillType.WINDING);
                    }
                }
                break;
            }
        }

        return paint;
    }

    public static void applyPaintProps(RenderingContext renderingContext, com.amazon.apl.android.sgcontent.Paint sgPaint, @Nullable Rect bounds, float canvasScale, float opacity, Paint paint) {
        int alpha = (int)(255f * sgPaint.getOpacity() * opacity);
        paint.setAlpha(alpha);
        switch(sgPaint.getType()) {
            case "Color": {
                paint.setColor(sgPaint.getColor());
                break;
            }
            case "Pattern": {
                APLLayer aplLayer = null; // not needed?
                applyPattern(aplLayer, renderingContext, paint, sgPaint, opacity, canvasScale);
                break;
            }
            case "LinearGradient": {
                applyLinearGradient(paint, sgPaint, bounds);
                break;
            }
            case "RadialGradient": {
                applyRadialGradient(paint, sgPaint, bounds);
                break;
            }
        }
    }

    private static void applyStrokeCap(PathOp pathOp, Paint paint) {
        switch(pathOp.getLineCap()) {
            case kGraphicLineCapButt: paint.setStrokeCap(Paint.Cap.BUTT); break;
            case kGraphicLineCapSquare: paint.setStrokeCap(Paint.Cap.SQUARE); break;
            case kGraphicLineCapRound: paint.setStrokeCap(Paint.Cap.ROUND); break;
            default: {
                Log.w(TAG, "Unknown StrokeLineCap value " + pathOp.getLineCap());
                paint.setStrokeCap(Paint.Cap.BUTT);
            }
        }
    }

    private static void applyStrokeJoin(PathOp pathOp, Paint paint) {
        switch (pathOp.getLineJoin()) {
            case kGraphicLineJoinBevel: paint.setStrokeJoin(Paint.Join.BEVEL); break;
            case kGraphicLineJoinMiter: paint.setStrokeJoin(Paint.Join.MITER); break;
            case kGraphicLineJoinRound: paint.setStrokeJoin(Paint.Join.ROUND); break;
            default: {
                Log.w(TAG, "Unknown StrokeLineJoin value " + pathOp.getLineJoin());
                paint.setStrokeJoin(Paint.Join.BEVEL);
            }
        }
    }

    private static void applyPattern(APLLayer aplLayer, RenderingContext renderingContext, Paint paint, com.amazon.apl.android.sgcontent.Paint sgPaint, float opacity, float scale) {
        try {
            float[] size = sgPaint.getSize();
            Bitmap patternBitmap = renderingContext.getBitmapFactory().createBitmap(Math.round(size[0]*scale), Math.round(size[1]*scale));
            Canvas canvas = new Canvas(patternBitmap);
            canvas.scale(scale, scale);
            Node n = sgPaint.getNode();
            do {
                drawNode(aplLayer, renderingContext, n, opacity, canvas);
                n = n.next();
            } while (n != null);
            BitmapShader bitmapShader =
                    new BitmapShader(patternBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            Matrix m =  new Matrix(sgPaint.getTransform());
            m.preScale(1f/scale, 1f/scale);
            bitmapShader.setLocalMatrix(m);
            paint.setShader(bitmapShader);
        } catch (BitmapCreationException ex) {
            Log.e(TAG, "Unable to allocate bitmap for pattern paint", ex);
        }
    }

    private static Rect computeBounds(Path path) {
        // These bounds include control points of bezier curves, so we can't use
        // them directly. Instead we use Region to get the actual shape bounds and
        // use these for the required clip region.
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        Rect roundedBounds = new Rect();
        bounds.roundOut(roundedBounds); // round towards outside to be safe

        Region clip = new Region(roundedBounds);

        Region region = new Region();
        region.setPath(path, clip);

        return region.getBounds();
    }

    private static Rect computeBounds(Layout layout) {
        int lineCount = layout.getLineCount();
        if (lineCount == 0) {
            // Probably an error scenario
            return new Rect(0, 0, 0, 0);
        } else if (lineCount == 1) {
            Rect bounds = new Rect();
            layout.getLineBounds(0, bounds);
            return bounds;
        } else {
            // Bounds are calculated as follows
            // |   ---Line1---     |
            // |------Line2-----   |
            // |      Line3--------|
            // | -----Line4-       |
            int minLeft = Integer.MAX_VALUE, minTop = Integer.MAX_VALUE, maxRight = Integer.MIN_VALUE, maxBottom = Integer.MIN_VALUE;
            for (int i = 0; i < lineCount; i++) {
                Rect bounds = new Rect();
                layout.getLineBounds(i, bounds);
                if (minLeft > bounds.left) {
                    minLeft = bounds.left;
                }
                if (minTop > bounds.top) {
                    minTop = bounds.top;
                }
                if (maxRight < bounds.right) {
                    maxRight = bounds.right;
                }
                if (maxBottom < bounds.bottom) {
                    maxBottom = bounds.bottom;
                }
            }
            return new Rect(minLeft, minTop, maxRight, maxBottom);
        }
    }

    private static void applyLinearGradient(Paint paint, com.amazon.apl.android.sgcontent.Paint sgPaint, @Nullable Rect bounds) {
        Shader.TileMode tileMode = getTileMode(sgPaint.getSpreadMethod());
        PointF start = sgPaint.getLinearGradientStart();
        PointF end = sgPaint.getLinearGradientEnd();
        float x1 = start.x;
        float y1 = start.y;
        float x2 = end.x;
        float y2 = end.y;
        if (bounds != null && sgPaint.getUseBoundingBox()) {
            x1 = bounds.left + start.x * bounds.width();
            y1 = bounds.top + start.y * bounds.height();
            x2 = bounds.left + end.x * bounds.width();
            y2 = bounds.top + end.y * bounds.height();
        }

        LinearGradient shader = new LinearGradient(x1, y1, x2, y2, sgPaint.getColors(), sgPaint.getPoints(), tileMode);
        shader.setLocalMatrix(sgPaint.getTransform());
        paint.setShader(shader);
    }

    private static void applyRadialGradient(Paint paint, com.amazon.apl.android.sgcontent.Paint sgPaint, @Nullable Rect bounds) {
        Shader.TileMode tileMode = getTileMode(sgPaint.getSpreadMethod());
        PointF center = sgPaint.getRadialGradientCenter();
        float x1 = center.x;
        float y1 = center.y;
        float radius = sgPaint.getRadialGradientRadius();
        if (bounds != null && sgPaint.getUseBoundingBox()) {
            x1 = bounds.left + center.x * bounds.width();
            y1 = bounds.top + center.y * bounds.height();
            radius *= Math.max(bounds.width(), bounds.height());
        }

        RadialGradient shader = new RadialGradient(x1, y1, radius, sgPaint.getColors(), sgPaint.getPoints(), tileMode);
        shader.setLocalMatrix(sgPaint.getTransform());
        paint.setShader(shader);
    }

    private static Shader.TileMode getTileMode(final GradientSpreadMethod gradientSpreadMethod) {
        Shader.TileMode tileMode = Shader.TileMode.CLAMP;
        switch(gradientSpreadMethod) {
            case PAD:
                tileMode = Shader.TileMode.CLAMP;
                break;
            case REFLECT:
                tileMode = Shader.TileMode.MIRROR;
                break;
            case REPEAT:
                tileMode = Shader.TileMode.REPEAT;
                break;
        }
        return tileMode;
    }

    private static float calculateCanvasScale(Canvas canvas) {
        // mapping a unit square to determine the cumulative scaling applied. This is
        // used to determine the needed upsampling of the vector graphic pattern
        // so it is rasterized at the resolution in will be drawn at.
        Matrix m = canvas.getMatrix();
        RectF unitSquare = new RectF(0,0, 1f, 1f);
        m.mapRect(unitSquare);
        // TODO: get actual canvas scale - this isn't working - hw layer problem?
        return Math.max(unitSquare.width(), unitSquare.height());
    }

    @NonNull
    private static DashPathEffect createDashPathEffect(@NonNull final Path path,
                                                       @NonNull final PathOp pathOp) {
        float[] strokeDashArray = pathOp.getStrokeDashArray();
        float strokeDashOffset = pathOp.getStrokeDashOffset();
        if (pathOp.getPathLength() > 0) {
            PathMeasure pathMeasure = new PathMeasure();
            pathMeasure.setPath(path, false);
            float truePathLength = 0;
            do {
                truePathLength += pathMeasure.getLength();
            } while (pathMeasure.nextContour());

            float userDefinedScale = truePathLength / pathOp.getPathLength();

            float[] userDefinedScaleStrokeDashArray = new float[strokeDashArray.length];
            for (int i = 0; i < strokeDashArray.length; i++) {
                userDefinedScaleStrokeDashArray[i] = strokeDashArray[i] * userDefinedScale;
            }
            strokeDashArray = userDefinedScaleStrokeDashArray;
            strokeDashOffset *= userDefinedScale;
        }

        return new DashPathEffect(strokeDashArray, strokeDashOffset);
    }
}

/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of The Linux Foundation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.Log;

import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.FilterEnvironment;
import com.android.gallery3d.filtershow.tools.DualCameraNativeEngine;

public class ImageFilterDualCamFusion extends ImageFilter {
    private static final String TAG = ImageFilterDualCamFusion.class.getSimpleName();

    private FilterDualCamFusionRepresentation mParameters;
    private Paint mPaint = new Paint();
    private Bitmap mFilteredBitmap = null;
    private Point mPoint = null;

    public ImageFilterDualCamFusion() {
        mName = "Fusion";
    }

    public void useRepresentation(FilterRepresentation representation) {
        FilterDualCamFusionRepresentation parameters = (FilterDualCamFusionRepresentation) representation;
        mParameters = parameters;
    }

    public FilterDualCamFusionRepresentation getParameters() {
        return mParameters;
    }

    public FilterRepresentation getDefaultRepresentation() {
        return new FilterDualCamFusionRepresentation();
    }

    @Override
    public void freeResources() {
        if (mFilteredBitmap != null)
            mFilteredBitmap.recycle();
        mFilteredBitmap = null;
        mPoint = null;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() == null) {
            return bitmap;
        }

        Point point = getParameters().getPoint();

        if(!point.equals(-1,-1)) {
            long startTime = System.currentTimeMillis();
            Log.e(TAG, "dual cam fusion - start processing: " + startTime);

            if(!point.equals(mPoint)) {
                mPoint = point;

                if(mFilteredBitmap == null) {
                    Rect originalBounds = MasterImage.getImage().getOriginalBounds();
                    int origW = originalBounds.width();
                    int origH = originalBounds.height();

                    mFilteredBitmap = Bitmap.createBitmap(origW, origH, Bitmap.Config.ARGB_8888);
                    mFilteredBitmap.setHasAlpha(true);
                }


                boolean result = DualCameraNativeEngine.getInstance().getForegroundImg(mPoint.x, mPoint.y,
                        mFilteredBitmap);

                if(result == false) {
                    Log.e(TAG, "Imagelib API failed");
                    return bitmap;
                }
            }

            mPaint.reset();
            if(quality == FilterEnvironment.QUALITY_FINAL) {
                mPaint.setAntiAlias(true);
                mPaint.setFilterBitmap(true);
                mPaint.setDither(true);
            }

            bitmap.setHasAlpha(true);

            Canvas canvas = new Canvas(bitmap);
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            if(getEnvironment().getImagePreset().getDoApplyGeometry()) {
                Matrix originalToScreen = getOriginalToScreenMatrix(w, h);
                canvas.drawBitmap(mFilteredBitmap, originalToScreen, null);
            } else {
                canvas.drawBitmap(mFilteredBitmap, null, new Rect(0,0,w,h), null);
            }

            Log.e(TAG, "dual cam fusion - finish processing: " + (System.currentTimeMillis()-startTime));
        }

        return bitmap;
    }
}

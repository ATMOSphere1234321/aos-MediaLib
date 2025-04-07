// Copyright 2017 Archos SA
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.archos.mediacenter.utils;

import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.view.ViewPropertyAnimator;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;

import com.archos.medialib.R;


public class GlobalResumeView extends RelativeLayout {

    private static final String TAG = "GlobalResumeView";

    Bitmap mImage;

    public GlobalResumeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onSizeChanged (int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != 0 && h != 0 && mImage != null) {
            resizeImage(mImage, w, h);
            mImage = null;
        }
    }

    public void setImage(Bitmap bm) {
        if (bm == null) {
            setBackgroundResource(android.R.color.black);
        } else {
            int dstWidth = getWidth();
            int dstHeight = getHeight();
            if (dstWidth != 0 && dstHeight != 0) {
                resizeImage(bm, dstWidth, dstHeight);
            } else {
                mImage = bm;
            }
        }
    }

    private void resizeImage(Bitmap bm, int dstWidth, int dstHeight) {
        int xOffset, yOffset;
        int rescaleWidth, rescaleHeight;
        int srcWidth = bm.getWidth();
        int srcHeight = bm.getHeight();

        if (srcWidth * dstHeight > dstWidth * srcHeight) {
            // Scale based on width
            float scaleFactor = (float) dstHeight / (float) srcHeight;
            rescaleWidth = (int) (scaleFactor * (float) srcWidth);
            rescaleHeight = dstHeight;
            xOffset = (rescaleWidth - dstWidth) / 2;  // Center crop horizontally
            yOffset = 0;
        } else {
            // Scale based on height
            float scaleFactor = (float) dstWidth / (float) srcWidth;
            rescaleWidth = dstWidth;
            rescaleHeight = (int) (scaleFactor * (float) srcHeight);
            xOffset = 0;
            yOffset = (rescaleHeight - dstHeight) / 2;  // Center crop vertically
        }

        Bitmap sbm = Bitmap.createScaledBitmap(bm, rescaleWidth, rescaleHeight, true);
        if (sbm != bm) {
            bm.recycle();
        }

        // Create a cropped bitmap from the scaled bitmap
        Bitmap cbm = Bitmap.createBitmap(sbm, xOffset, yOffset, dstWidth, dstHeight);
        if (cbm != sbm) {
            sbm.recycle();
        }

        // Create a BitmapShader to apply the image texture
        BitmapShader shader = new BitmapShader(cbm, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        // Create a ShapeDrawable with rounded corners
        float px = 8;
        float density = getResources().getDisplayMetrics().density;
        float radius = px * density;

        //float radius = 100f;  // Adjust corner radius as needed
        RoundRectShape roundRectShape = new RoundRectShape(new float[] { radius, radius, radius, radius, radius, radius, radius, radius }, null, null);
        ShapeDrawable shapeDrawable = new ShapeDrawable(roundRectShape);
        shapeDrawable.getPaint().setShader(shader);
        shapeDrawable.getPaint().setAntiAlias(true);

        // Create a Bitmap from the shapeDrawable
        Bitmap roundedBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(roundedBitmap);
        shapeDrawable.setBounds(0, 0, dstWidth, dstHeight);
        shapeDrawable.draw(canvas);

        // Create BitmapDrawable from the generated rounded bitmap
        BitmapDrawable imageDrawable = new BitmapDrawable(getResources(), roundedBitmap);

        // Load stroke drawable
        Drawable strokeDrawable = ContextCompat.getDrawable(getContext(), R.drawable.browser_resume_stroke);

        // Combine both using LayerDrawable
        Drawable[] layers = new Drawable[2];
        layers[0] = imageDrawable;      // Bottom layer: the image
        layers[1] = strokeDrawable;     // Top layer: the stroke

        LayerDrawable layerDrawable = new LayerDrawable(layers);
        setBackground(layerDrawable);
    }

    public void launchOpenAnimation(AnimatorListener listener) {
        ViewPropertyAnimator a = animate();
        a.scaleX(5f).scaleY(5f).alpha(0f);
        a.setDuration(300);
        a.setListener(listener);
    }

    public void resetOpenAnimation() {
        setScaleX(1f);
        setScaleY(1f);
        setAlpha(1f);
    }

    public void clearImage() {
        setBackground(null);
        if (mImage != null) {
            mImage.recycle();
            mImage = null;
        }
    }

    @Override
    public boolean performClick() {
        // Call super to handle accessibility events
        super.performClick();
        // Add any custom logic here if needed
        return true;
    }
}

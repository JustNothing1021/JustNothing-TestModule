package com.justnothing.testmodule.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.caverock.androidsvg.SVG;

import java.io.InputStream;

public class SVGView extends View {
    private SVG svg;
    private Picture picture;
    private Handler animationHandler;
    private Runnable animationRunnable;
    private boolean isAnimating = false;

    public SVGView(Context context) {
        super(context);
        initAnimation();
    }

    public SVGView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initAnimation();
    }

    private void initAnimation() {
        animationHandler = new Handler(Looper.getMainLooper());
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAnimating) {
                    invalidate();
                    animationHandler.postDelayed(this, 16);
                }
            }
        };
    }

    public void setSVG(String fileName) {
        try {
            InputStream inputStream = getContext().getAssets().open(fileName);
            svg = SVG.getFromInputStream(inputStream);
            svg.setRenderDPI(96);
            picture = svg.renderToPicture();
            inputStream.close();
            invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPicture(Picture picture) {
        this.picture = picture;
        invalidate();
    }

    public void setSVG(SVG svg) {
        this.svg = svg;
        svg.setRenderDPI(96);
        this.picture = svg.renderToPicture();
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAnimating = true;
        if (animationHandler != null && animationRunnable != null) {
            animationHandler.post(animationRunnable);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAnimating = false;
        if (animationHandler != null && animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        
        if (svg != null) {
            RectF viewBox = svg.getDocumentViewBox();
            if (viewBox != null) {
                float svgWidth = viewBox.width();
                float svgHeight = viewBox.height();
                float canvasWidth = getWidth();
                float canvasHeight = getHeight();
                
                float scaleX = canvasWidth / svgWidth;
                float scaleY = canvasHeight / svgHeight;
                float scale = Math.max(scaleX, scaleY);
                
                float scaledWidth = svgWidth * scale;
                float scaledHeight = svgHeight * scale;
                
                float offsetX = (canvasWidth - scaledWidth) / 2;
                float offsetY = (canvasHeight - scaledHeight) / 2;
                
                canvas.save();
                canvas.translate(offsetX, offsetY);
                canvas.scale(scale, scale);
                svg.renderToCanvas(canvas);
                canvas.restore();
            } else {
                svg.renderToCanvas(canvas);
            }
        } else if (picture != null) {
            float canvasWidth = getWidth();
            float canvasHeight = getHeight();
            float pictureWidth = picture.getWidth();
            float pictureHeight = picture.getHeight();
            
            float scaleX = canvasWidth / pictureWidth;
            float scaleY = canvasHeight / pictureHeight;
            float scale = Math.max(scaleX, scaleY);
            
            float scaledWidth = pictureWidth * scale;
            float scaledHeight = pictureHeight * scale;
            
            float offsetX = (canvasWidth - scaledWidth) / 2;
            float offsetY = (canvasHeight - scaledHeight) / 2;
            
            canvas.save();
            canvas.translate(offsetX, offsetY);
            canvas.scale(scale, scale);
            canvas.drawPicture(picture);
            canvas.restore();
        }
    }
}

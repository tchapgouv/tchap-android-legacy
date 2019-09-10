/*
 * Copyright 2018 New Vector Ltd
 * Copyright 2018 DINSIC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.gouv.tchap.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

public class HexagonMaskView extends androidx.appcompat.widget.AppCompatImageView {
    private Path hexagonPath;
    private float width, height;
    private Paint borderPaint;
    private int borderRatio;

    public HexagonMaskView(Context context) {
        super(context);
        init();
    }

    public HexagonMaskView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HexagonMaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Define the border settings
     *
     * @param color the border color (Color.LTGRAY by default).
     * @param ratio the ratio of the border width to the radius of the hexagon (value between 0 and 100, default value: 3)
     */
    public void setBorderSettings(int color, int ratio) {
        this.borderPaint.setColor(color);

        if (ratio < 0) {
            ratio = 0;
        } else if (ratio > 100) {
            ratio = 100;
        }

        if (this.borderRatio != ratio) {
            this.borderRatio = ratio;
            // The hexagon path must be updated
            calculatePath();
        } else {
            invalidate();
        }
    }

    private void init() {
        hexagonPath = new Path();
        borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.LTGRAY);
        borderRatio = 3;
    }

    private void calculatePath() {
        // Compute the radius of the hexagon, and the border width
        float radius = height/2;
        float borderWidth = (radius * borderRatio)/100;
        borderPaint.setStrokeWidth(borderWidth);

        // Define the hexagon path by placing it in the middle of the border.
        float pathRadius = radius - borderWidth/2;
        float triangleHeight = (float) (Math.sqrt(3) * pathRadius / 2);
        float centerX = width/2;
        float centerY = height/2;

        hexagonPath.reset();
        hexagonPath.moveTo(centerX, centerY + pathRadius);
        hexagonPath.lineTo(centerX - triangleHeight, centerY + pathRadius/2);
        hexagonPath.lineTo(centerX - triangleHeight, centerY - pathRadius/2);
        hexagonPath.lineTo(centerX, centerY - pathRadius);
        hexagonPath.lineTo(centerX + triangleHeight, centerY - pathRadius/2);
        hexagonPath.lineTo(centerX + triangleHeight, centerY + pathRadius/2);
        hexagonPath.lineTo(centerX, centerY + pathRadius);
        // Add again the first segment to get the right display of the border.
        hexagonPath.lineTo(centerX - triangleHeight, centerY + pathRadius/2);
        invalidate();
    }

    @Override
    public void onDraw(Canvas c){
        // Apply a clip to draw the bitmap inside an hexagon shape
        c.save();
        c.clipPath(hexagonPath);
        super.onDraw(c);
        // Restore the canvas context
        c.restore();
        // Draw the border
        c.drawPath(hexagonPath, borderPaint);
    }

    // getting the view size
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = this.getMeasuredWidth();
        height = this.getMeasuredHeight();
        calculatePath();
    }
}

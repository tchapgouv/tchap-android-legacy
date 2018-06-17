/*
 * Copyright 2018 New Vector Ltd
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
import android.graphics.Path;
import android.graphics.Region;
import android.util.AttributeSet;

public class HexagonMaskView extends android.support.v7.widget.AppCompatImageView {
    private Path hexagonPath;
    private Path hexagonBorderPath;
    private float radius;
    private float width, height;
    private int maskColor;

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

    private void init() {
        hexagonPath = new Path();
        hexagonBorderPath = new Path();
        maskColor = Color.WHITE;
    }

    public void setRadius(float r) {
        this.radius = r;
        calculatePath();
    }

    public void setMaskColor(int color) {
        this.maskColor = color;
        invalidate();
    }

    private void calculatePath() {
        float triangleHeight = (float) (Math.sqrt(3) * radius / 2);
        float centerX = width/2;
        float centerY = height/2;
        hexagonPath.moveTo(centerX, centerY + radius);
        hexagonPath.lineTo(centerX - triangleHeight, centerY + radius/2);
        hexagonPath.lineTo(centerX - triangleHeight, centerY - radius/2);
        hexagonPath.lineTo(centerX, centerY - radius);
        hexagonPath.lineTo(centerX + triangleHeight, centerY - radius/2);
        hexagonPath.lineTo(centerX + triangleHeight, centerY + radius/2);
        hexagonPath.moveTo(centerX, centerY + radius);

        float radiusBorder = radius - 5;
        float triangleBorderHeight = (float) (Math.sqrt(3) * radiusBorder / 2);
        hexagonBorderPath.moveTo(centerX, centerY + radiusBorder);
        hexagonBorderPath.lineTo(centerX - triangleBorderHeight, centerY + radiusBorder/2);
        hexagonBorderPath.lineTo(centerX - triangleBorderHeight, centerY - radiusBorder/2);
        hexagonBorderPath.lineTo(centerX, centerY - radiusBorder);
        hexagonBorderPath.lineTo(centerX + triangleBorderHeight, centerY - radiusBorder/2);
        hexagonBorderPath.lineTo(centerX + triangleBorderHeight, centerY + radiusBorder/2);
        hexagonBorderPath.moveTo(centerX, centerY + radiusBorder);
        invalidate();
    }

    @Override
    public void onDraw(Canvas c){
        super.onDraw(c);
        c.clipPath(hexagonBorderPath, Region.Op.DIFFERENCE);
        c.drawColor(Color.LTGRAY);
        c.save();
        c.clipPath(hexagonPath, Region.Op.DIFFERENCE);
        c.drawColor(maskColor);
        c.save();
    }

    // getting the view size and default radius
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //with Measurespec.getSize it doesn't work for little sizes.
        //works fine with getMeasuredWidth
        width = this.getMeasuredWidth();//MeasureSpec.getSize(widthMeasureSpec);
        height = this.getMeasuredHeight();//MeasureSpec.getSize(heightMeasureSpec);
        radius = height / 2;// - 10;
        calculatePath();
    }
}

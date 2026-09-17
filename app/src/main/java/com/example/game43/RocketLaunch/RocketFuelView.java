package com.example.game43.RocketLaunch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class RocketFuelView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF outerRect = new RectF();
    private final RectF innerRect = new RectF();
    private final RectF fuelRect = new RectF();
    private float fuelFraction = 1f;

    public RocketFuelView(Context context) {
        this(context, null);
    }

    public RocketFuelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setFuelFraction(float fuelFraction) {
        float clamped = Math.max(0f, Math.min(1f, fuelFraction));
        if (Math.abs(this.fuelFraction - clamped) < 0.002f) {
            return;
        }
        this.fuelFraction = clamped;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float outline = 3f * density;
        float radius = 6f * density;

        outerRect.set(0f, 0f, getWidth(), getHeight());
        paint.setColor(Color.rgb(25, 28, 34));
        canvas.drawRoundRect(outerRect, radius, radius, paint);

        innerRect.set(outline, outline, getWidth() - outline, getHeight() - outline);
        paint.setColor(Color.rgb(82, 85, 91));
        canvas.drawRoundRect(innerRect, radius * 0.65f, radius * 0.65f, paint);

        float inset = outline + 3f * density;
        float availableHeight = getHeight() - inset * 2f;
        fuelRect.set(inset,
                getHeight() - inset - availableHeight * fuelFraction,
                getWidth() - inset,
                getHeight() - inset);
        paint.setColor(fuelFraction < 0.2f
                ? Color.rgb(238, 73, 54)
                : Color.rgb(255, 139, 58));
        canvas.drawRoundRect(fuelRect, 3f * density, 3f * density, paint);

        if (fuelRect.height() > 8f * density) {
            fuelRect.right = fuelRect.left + 3f * density;
            paint.setColor(Color.argb(130, 255, 230, 153));
            canvas.drawRoundRect(fuelRect, 2f * density, 2f * density, paint);
        }
    }
}

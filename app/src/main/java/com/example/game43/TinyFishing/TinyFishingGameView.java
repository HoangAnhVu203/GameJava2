package com.example.game43.TinyFishing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.game43.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class TinyFishingGameView extends View {
    private static final int FISH_COUNT = 14;
    private static final float HOOK_DROP_SPEED = 0.62f;
    private static final float HOOK_PULL_SPEED = 0.92f;
    private static final float MIN_CAST_DEPTH_FRACTION = 0.62f;
    private static final float MAX_CAST_DEPTH_FRACTION = 2.12f;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawRect = new RectF();
    private final List<Fish> fishes = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<Bubble> bubbles = new ArrayList<>();
    private final Random random = new Random();

    private Bitmap backgroundBitmap;
    private Bitmap waterTopBitmap;
    private Bitmap waterMiddleBitmap;
    private Bitmap fishermanBitmap;
    private Bitmap hookBitmap;
    private Bitmap spinnerBitmap;
    private Bitmap spinnerPointerBitmap;
    private Bitmap sideLandBitmap;
    private Bitmap[] fishBitmaps;
    private Bitmap[] jellyBitmaps;
    private MoneyStateListener moneyStateListener;

    private float viewWidth;
    private float viewHeight;
    private float minSide;
    private float worldHeight;
    private float cameraY;
    private float waterTopY;
    private float hookStartX;
    private float hookStartY;
    private float hookX;
    private float hookY;
    private float hookTargetX;
    private float hookMaxY;
    private float sideLandWidth;
    private float leftWaterBound;
    private float rightWaterBound;
    private float spinnerCenterX;
    private float spinnerCenterY;
    private float spinnerSize;
    private float spinnerPhase;
    private float selectedDepthPower = 1f;
    private float waveTime;
    private long lastFrameNanos;
    private int money;
    private Fish caughtFish;
    private HookState hookState = HookState.IDLE;

    public TinyFishingGameView(Context context) {
        super(context);
        init();
    }

    public TinyFishingGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TinyFishingGameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setMoneyStateListener(MoneyStateListener moneyStateListener) {
        this.moneyStateListener = moneyStateListener;
        notifyMoneyChanged();
    }

    private void init() {
        setFocusable(true);
        bitmapPaint.setDither(true);
        bitmapPaint.setFilterBitmap(true);

        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fishing_bg);
        waterTopBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.top);
        waterMiddleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.middle);
        fishermanBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fisherman);
        hookBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cancau);
        spinnerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.oquay);
        spinnerPointerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.muiten);
        sideLandBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dat);
        fishBitmaps = new Bitmap[] {
                BitmapFactory.decodeResource(getResources(), R.drawable.ca1),
                BitmapFactory.decodeResource(getResources(), R.drawable.ca2),
                BitmapFactory.decodeResource(getResources(), R.drawable.ca3),
                BitmapFactory.decodeResource(getResources(), R.drawable.ca4),
                BitmapFactory.decodeResource(getResources(), R.drawable.ca5),
                BitmapFactory.decodeResource(getResources(), R.drawable.ca6),
                BitmapFactory.decodeResource(getResources(), R.drawable.ca7),
                BitmapFactory.decodeResource(getResources(), R.drawable.ca8),
                BitmapFactory.decodeResource(getResources(), R.drawable.ca9),
                BitmapFactory.decodeResource(getResources(), R.drawable.ca10),
                BitmapFactory.decodeResource(getResources(), R.drawable.ca11)
        };
        jellyBitmaps = new Bitmap[] {
                BitmapFactory.decodeResource(getResources(), R.drawable.sua1),
                BitmapFactory.decodeResource(getResources(), R.drawable.sua2)
        };

        linePaint.setColor(Color.argb(210, 238, 248, 255));
        linePaint.setStrokeWidth(dp(2.2f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        bubblePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        minSide = Math.min(w, h);
        worldHeight = viewHeight + minSide * 1.55f;
        waterTopY = viewHeight * 0.24f;
        sideLandWidth = clamp(viewWidth * 0.16f, dp(42f), minSide * 0.22f);
        leftWaterBound = sideLandWidth * 0.82f;
        rightWaterBound = viewWidth - sideLandWidth * 0.82f;
        hookStartX = viewWidth * 0.38f;
        hookStartY = waterTopY + minSide * 0.02f;
        spinnerSize = minSide * 0.24f;
        spinnerCenterX = viewWidth - spinnerSize * 0.66f;
        spinnerCenterY = waterTopY - spinnerSize * 0.15f;
        resetGame();
    }

    private void resetGame() {
        fishes.clear();
        floatingTexts.clear();
        bubbles.clear();
        money = 0;
        hookX = hookStartX;
        hookY = hookStartY;
        hookTargetX = hookStartX;
        hookMaxY = worldHeight - minSide * 0.12f;
        cameraY = 0f;
        spinnerPhase = 0f;
        selectedDepthPower = 1f;
        caughtFish = null;
        hookState = HookState.IDLE;
        lastFrameNanos = 0L;
        for (int i = 0; i < FISH_COUNT; i++) {
            spawnFish(true);
        }
        notifyMoneyChanged();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
        }
        float dt = Math.min(0.033f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;

        updateGame(dt);
        drawGame(canvas);
        postInvalidateOnAnimation();
    }

    private void updateGame(float dt) {
        waveTime += dt;
        updateSpinner(dt);
        updateFish(dt);
        updateHook(dt);
        updateCamera(dt);
        updateFloatingTexts(dt);
        updateBubbles(dt);
        if (random.nextFloat() < dt * 2.4f) {
            spawnBubble();
        }
    }

    private void updateSpinner(float dt) {
        if (hookState == HookState.IDLE) {
            spinnerPhase = positiveModulo(spinnerPhase + dt * 0.72f, 1f);
        }
    }

    private void updateFish(float dt) {
        for (int i = fishes.size() - 1; i >= 0; i--) {
            Fish fish = fishes.get(i);
            if (fish == caughtFish) {
                continue;
            }
            fish.x += fish.vx * dt;
            fish.y += (float) Math.sin(waveTime * fish.waveSpeed + fish.waveOffset) * minSide * 0.012f * dt;
            fish.wiggle += dt * fish.waveSpeed;

            float minX = leftWaterBound + fish.size * 0.5f;
            float maxX = rightWaterBound - fish.size * 0.5f;
            if (fish.x < minX) {
                fish.x = minX;
                fish.vx = Math.abs(fish.vx);
                updateFishFacing(fish);
            } else if (fish.x > maxX) {
                fish.x = maxX;
                fish.vx = -Math.abs(fish.vx);
                updateFishFacing(fish);
            }
        }
    }

    private void updateHook(float dt) {
        if (hookState == HookState.IDLE) {
            hookX = hookStartX;
            hookY = hookStartY;
            return;
        }

        if (hookState == HookState.DROPPING) {
            hookX += (hookTargetX - hookX) * Math.min(1f, dt * 3.2f);
            hookY += minSide * HOOK_DROP_SPEED * dt;
            if (hookY >= hookMaxY) {
                hookState = HookState.PULLING;
            }
            return;
        }

        float dx = hookStartX - hookX;
        float dy = hookStartY - hookY;
        float distance = Math.max(1f, (float) Math.hypot(dx, dy));
        float move = minSide * HOOK_PULL_SPEED * dt;
        hookX += dx / distance * move;
        hookY += dy / distance * move;
        if (caughtFish != null) {
            caughtFish.x = hookX;
            caughtFish.y = hookY + caughtFish.size * 0.36f;
        } else {
            checkHookCatch();
        }
        if (distance <= move + minSide * 0.02f) {
            finishPull();
        }
    }

    private void updateCamera(float dt) {
        float followY = hookState == HookState.IDLE ? 0f : hookY - viewHeight * 0.58f;
        float targetCameraY = clamp(followY, 0f, maxCameraY());
        cameraY += (targetCameraY - cameraY) * Math.min(1f, dt * 5.4f);
        if (Math.abs(targetCameraY - cameraY) < 0.5f) {
            cameraY = targetCameraY;
        }
    }

    private void checkHookCatch() {
        if (caughtFish != null) {
            return;
        }
        float hookRadius = minSide * 0.045f;
        for (Fish fish : fishes) {
            float dx = fish.x - hookX;
            float dy = fish.y - hookY;
            if (dx * dx + dy * dy <= (hookRadius + fish.size * 0.38f) * (hookRadius + fish.size * 0.38f)) {
                caughtFish = fish;
                hookState = HookState.PULLING;
                spawnCatchSplash(fish.x, fish.y);
                return;
            }
        }
    }

    private void finishPull() {
        hookState = HookState.IDLE;
        hookX = hookStartX;
        hookY = hookStartY;
        if (caughtFish == null) {
            return;
        }

        money += caughtFish.value;
        spawnMoneyText(hookStartX, waterTopY + minSide * 0.06f, caughtFish.value);
        fishes.remove(caughtFish);
        caughtFish = null;
        spawnFish(false);
        notifyMoneyChanged();
    }

    private void spawnFish(boolean anywhere) {
        Fish fish = new Fish();
        boolean jelly = random.nextFloat() < 0.18f;
        fish.bitmap = jelly
                ? jellyBitmaps[random.nextInt(jellyBitmaps.length)]
                : fishBitmaps[random.nextInt(fishBitmaps.length)];
        fish.size = minSide * (jelly ? randomBetween(0.105f, 0.14f) : randomBetween(0.12f, 0.18f));
        fish.value = jelly ? randomBetweenInt(6000, 18000) : randomBetweenInt(12000, 42000);
        fish.y = randomBetween(waterTopY + minSide * 0.16f, worldHeight - minSide * 0.11f);
        boolean fromLeft = random.nextBoolean();
        float minX = leftWaterBound + fish.size * 0.5f;
        float maxX = rightWaterBound - fish.size * 0.5f;
        if (anywhere) {
            fish.x = randomBetween(minX, maxX);
        } else {
            fish.x = fromLeft ? minX : maxX;
        }
        fish.vx = (fromLeft ? 1f : -1f) * minSide * randomBetween(0.12f, 0.24f);
        updateFishFacing(fish);
        fish.waveOffset = randomBetween(0f, (float) Math.PI * 2f);
        fish.waveSpeed = randomBetween(1.4f, 2.8f);
        fishes.add(fish);
    }

    private void updateFishFacing(Fish fish) {
        fish.flip = fish.vx > 0f;
    }

    private void spawnMoneyText(float x, float y, int value) {
        FloatingText text = new FloatingText();
        text.x = x;
        text.y = y;
        text.vy = -minSide * 0.24f;
        text.value = value;
        text.life = 0.85f;
        floatingTexts.add(text);
    }

    private void updateFloatingTexts(float dt) {
        Iterator<FloatingText> iterator = floatingTexts.iterator();
        while (iterator.hasNext()) {
            FloatingText text = iterator.next();
            text.age += dt;
            text.y += text.vy * dt;
            if (text.age >= text.life) {
                iterator.remove();
            }
        }
    }

    private void spawnBubble() {
        Bubble bubble = new Bubble();
        bubble.x = randomBetween(leftWaterBound + minSide * 0.03f, rightWaterBound - minSide * 0.03f);
        bubble.y = clamp(cameraY + viewHeight + minSide * 0.04f,
                waterTopY + minSide * 0.24f, worldHeight - minSide * 0.05f);
        bubble.vy = -minSide * randomBetween(0.12f, 0.28f);
        bubble.radius = minSide * randomBetween(0.005f, 0.011f);
        bubble.life = randomBetween(1.8f, 3.5f);
        bubbles.add(bubble);
    }

    private void spawnCatchSplash(float x, float y) {
        for (int i = 0; i < 10; i++) {
            Bubble bubble = new Bubble();
            bubble.x = x + randomBetween(-minSide * 0.025f, minSide * 0.025f);
            bubble.y = y + randomBetween(-minSide * 0.02f, minSide * 0.02f);
            bubble.vy = -minSide * randomBetween(0.2f, 0.42f);
            bubble.vx = randomBetween(-minSide * 0.12f, minSide * 0.12f);
            bubble.radius = minSide * randomBetween(0.006f, 0.016f);
            bubble.life = randomBetween(0.45f, 0.9f);
            bubbles.add(bubble);
        }
    }

    private void updateBubbles(float dt) {
        Iterator<Bubble> iterator = bubbles.iterator();
        while (iterator.hasNext()) {
            Bubble bubble = iterator.next();
            bubble.age += dt;
            bubble.x += bubble.vx * dt;
            bubble.y += bubble.vy * dt;
            if (bubble.age >= bubble.life || bubble.y < waterTopY) {
                iterator.remove();
            }
        }
    }

    private void drawGame(Canvas canvas) {
        drawBackground(canvas);
        canvas.save();
        canvas.translate(0f, -cameraY);
        drawWater(canvas);
        drawSideLands(canvas);
        drawBubbles(canvas);
        drawFishes(canvas);
        drawHook(canvas);
        drawFisherman(canvas);
        drawFloatingTexts(canvas);
        canvas.restore();
        drawSpinner(canvas);
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundBitmap != null) {
            float scale = Math.max(viewWidth / backgroundBitmap.getWidth(), viewHeight / backgroundBitmap.getHeight());
            float width = backgroundBitmap.getWidth() * scale;
            float height = backgroundBitmap.getHeight() * scale;
            drawRect.set((viewWidth - width) * 0.5f, (viewHeight - height) * 0.5f,
                    (viewWidth + width) * 0.5f, (viewHeight + height) * 0.5f);
            canvas.drawBitmap(backgroundBitmap, null, drawRect, bitmapPaint);
        } else {
            canvas.drawColor(Color.rgb(36, 152, 223));
        }

    }

    private void drawWater(Canvas canvas) {
        drawWaterLayer(canvas, waterMiddleBitmap, waterTopY + minSide * 0.025f,
                worldHeight - waterTopY + minSide * 0.08f);
        drawWaterLayer(canvas, waterTopBitmap, waterTopY - minSide * 0.018f, viewHeight * 0.52f);
    }

    private void drawWaterLayer(Canvas canvas, Bitmap bitmap, float top, float height) {
        if (bitmap == null) {
            return;
        }
        float width = height * bitmap.getWidth() / bitmap.getHeight();
        float offset = positiveModulo(waveTime * minSide * 0.03f, width);
        for (float left = -offset - width; left < viewWidth + width; left += width) {
            drawRect.set(left, top, left + width, top + height);
            canvas.drawBitmap(bitmap, null, drawRect, bitmapPaint);
        }
    }

    private void drawSideLands(Canvas canvas) {
        if (sideLandBitmap == null || sideLandWidth <= 0f) {
            return;
        }
        float top = waterTopY + minSide * 0.015f;
        drawRect.set(0f, top, sideLandWidth, worldHeight);
        canvas.save();
        canvas.scale(-1f, 1f, drawRect.centerX(), drawRect.centerY());
        canvas.drawBitmap(sideLandBitmap, null, drawRect, bitmapPaint);
        canvas.restore();

        drawRect.set(viewWidth - sideLandWidth, top, viewWidth, worldHeight);
        canvas.drawBitmap(sideLandBitmap, null, drawRect, bitmapPaint);
    }

    private void drawFishes(Canvas canvas) {
        for (Fish fish : fishes) {
            float bob = (float) Math.sin(fish.wiggle + fish.waveOffset) * minSide * 0.006f;
            drawBitmapCentered(canvas, fish.bitmap, fish.x, fish.y + bob, fish.size, fish.flip, 0f, 255);
        }
    }

    private void drawHook(Canvas canvas) {
        canvas.drawLine(hookStartX, hookStartY, hookX, hookY - minSide * 0.025f, linePaint);
        drawBitmapCentered(canvas, hookBitmap, hookX, hookY, minSide * 0.12f, false, 0f, 255);
    }

    private void drawSpinner(Canvas canvas) {
        float wheelAlpha = hookState == HookState.IDLE ? 255 : 150;
        drawBitmapCentered(canvas, spinnerBitmap, spinnerCenterX, spinnerCenterY, spinnerSize, false, 0f, (int) wheelAlpha);

        float trackWidth = spinnerSize * 0.72f;
        float trackY = spinnerCenterY + spinnerSize * 0.24f;
        float left = spinnerCenterX - trackWidth * 0.5f;
        float right = spinnerCenterX + trackWidth * 0.5f;

        Paint.Style previousStyle = bubblePaint.getStyle();
        bubblePaint.setStyle(Paint.Style.FILL);
        bubblePaint.setColor(Color.argb(135, 255, 255, 255));
        drawRect.set(left, trackY - dp(6f), right, trackY + dp(6f));
        canvas.drawRoundRect(drawRect, dp(6f), dp(6f), bubblePaint);

        bubblePaint.setColor(Color.argb(210, 255, 178, 42));
        drawRect.set(spinnerCenterX - trackWidth * 0.14f, trackY - dp(7f),
                spinnerCenterX + trackWidth * 0.14f, trackY + dp(7f));
        canvas.drawRoundRect(drawRect, dp(7f), dp(7f), bubblePaint);

        float pointerX = left + trackWidth * spinnerPointerProgress();
        float pointerSize = spinnerSize * 0.16f;
        drawBitmapCentered(canvas, spinnerPointerBitmap, pointerX, trackY - pointerSize * 0.72f,
                pointerSize, false, 0f, 255);

        textPaint.setTextSize(spinnerSize * 0.085f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("MIN", left, trackY + spinnerSize * 0.17f, textPaint);
        canvas.drawText("MAX", spinnerCenterX, trackY + spinnerSize * 0.17f, textPaint);
        canvas.drawText("MIN", right, trackY + spinnerSize * 0.17f, textPaint);

        if (hookState != HookState.IDLE) {
            textPaint.setTextSize(spinnerSize * 0.075f);
            textPaint.setColor(Color.argb(225, 255, 255, 255));
            int meters = 8 + Math.round(selectedDepthPower * 25f);
            canvas.drawText(meters + "m", spinnerCenterX, spinnerCenterY + spinnerSize * 0.48f, textPaint);
        }
        bubblePaint.setStyle(previousStyle);
    }

    private void drawFisherman(Canvas canvas) {
        if (fishermanBitmap == null) {
            return;
        }
        float width = minSide * 0.64f;
        float height = width * fishermanBitmap.getHeight() / fishermanBitmap.getWidth();
        float left = viewWidth * 0.5f - width * 0.5f;
        float bottom = waterTopY + minSide * 0.07f;
        drawRect.set(left, bottom - height, left + width, bottom);
        canvas.drawBitmap(fishermanBitmap, null, drawRect, bitmapPaint);
    }

    private void drawBubbles(Canvas canvas) {
        bubblePaint.setStyle(Paint.Style.FILL);
        for (Bubble bubble : bubbles) {
            float progress = Math.min(1f, bubble.age / bubble.life);
            bubblePaint.setColor(Color.argb((int) (120f * (1f - progress)), 232, 250, 255));
            canvas.drawCircle(bubble.x, bubble.y, bubble.radius, bubblePaint);
        }
    }

    private void drawFloatingTexts(Canvas canvas) {
        for (FloatingText text : floatingTexts) {
            float progress = Math.min(1f, text.age / text.life);
            int alpha = (int) (255f * (1f - progress));
            textPaint.setColor(Color.argb(alpha, 255, 255, 255));
            textPaint.setTextSize(minSide * 0.09f);
            canvas.drawText("$" + text.value, text.x, text.y, textPaint);
        }
    }

    private void drawBitmapCentered(Canvas canvas, Bitmap bitmap, float centerX, float centerY,
                                    float size, boolean flip, float rotation, int alpha) {
        if (bitmap == null) {
            return;
        }
        float width;
        float height;
        if (bitmap.getWidth() >= bitmap.getHeight()) {
            width = size;
            height = size * bitmap.getHeight() / bitmap.getWidth();
        } else {
            height = size;
            width = size * bitmap.getWidth() / bitmap.getHeight();
        }
        drawRect.set(centerX - width * 0.5f, centerY - height * 0.5f,
                centerX + width * 0.5f, centerY + height * 0.5f);
        int previousAlpha = bitmapPaint.getAlpha();
        bitmapPaint.setAlpha(alpha);
        canvas.save();
        if (flip) {
            canvas.scale(-1f, 1f, centerX, centerY);
        }
        if (Math.abs(rotation) > 0.01f) {
            canvas.rotate(rotation, centerX, centerY);
        }
        canvas.drawBitmap(bitmap, null, drawRect, bitmapPaint);
        canvas.restore();
        bitmapPaint.setAlpha(previousAlpha);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && hookState == HookState.IDLE) {
            selectedDepthPower = currentDepthPower();
            hookTargetX = hookStartX;
            hookMaxY = waterTopY + minSide * (MIN_CAST_DEPTH_FRACTION
                    + (MAX_CAST_DEPTH_FRACTION - MIN_CAST_DEPTH_FRACTION) * selectedDepthPower);
            hookMaxY = clamp(hookMaxY, waterTopY + minSide * 0.34f, worldHeight - minSide * 0.12f);
            hookState = HookState.DROPPING;
            caughtFish = null;
            return true;
        }
        return true;
    }

    private float currentDepthPower() {
        float pointerX = spinnerPointerProgress();
        float distanceFromCenter = Math.abs(pointerX - 0.5f) * 2f;
        return clamp(1f - distanceFromCenter, 0f, 1f);
    }

    private float spinnerPointerProgress() {
        return 0.5f + 0.5f * (float) Math.sin(spinnerPhase * Math.PI * 2f);
    }

    private void notifyMoneyChanged() {
        if (moneyStateListener != null) {
            moneyStateListener.onMoneyChanged(money);
        }
    }

    private float randomBetween(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private int randomBetweenInt(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float maxCameraY() {
        return Math.max(0f, worldHeight - viewHeight);
    }

    private float positiveModulo(float value, float modulo) {
        float result = value % modulo;
        return result < 0f ? result + modulo : result;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    public interface MoneyStateListener {
        void onMoneyChanged(int money);
    }

    private enum HookState {
        IDLE,
        DROPPING,
        PULLING
    }

    private static final class Fish {
        Bitmap bitmap;
        float x;
        float y;
        float vx;
        float size;
        float waveOffset;
        float waveSpeed;
        float wiggle;
        int value;
        boolean flip;
    }

    private static final class FloatingText {
        float x;
        float y;
        float vy;
        float age;
        float life;
        int value;
    }

    private static final class Bubble {
        float x;
        float y;
        float vx;
        float vy;
        float radius;
        float age;
        float life;
    }
}

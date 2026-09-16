package com.example.game43.TinyFishing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.game43.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class TinyFishingGameView extends View {
    private static final int FISH_COUNT = 27;
    private static final int MAX_CATCH_COUNT = 5;
    private static final int INITIAL_CAST_TURNS = 1;
    private static final float HOOK_DROP_SPEED = 0.62f;
    private static final float HOOK_PULL_SPEED = 0.92f;
    private static final float FULL_CATCH_PULL_MULTIPLIER = 2.25f;
    private static final float BOTTOM_WAIT_TIME = 0.25f;
    private static final float MIN_CAST_DEPTH_FRACTION = 0.82f;
    private static final float MAX_CAST_DEPTH_FRACTION = 3.35f;
    private static final float CAST_FRAME_TIME = 0.075f;
    private static final float BACKGROUND_WATERLINE_FRACTION = 0.42f;
    private static final int[] DEPTH_ZONE_FISH = {0, 1, 3, 8, 4, 5, 9};
    private static final int[] DEPTH_ZONE_MIN_VALUE = {6000, 12000, 22000, 38000, 62000, 88000, 125000};
    private static final int[] DEPTH_ZONE_MAX_VALUE = {12000, 24000, 42000, 70000, 105000, 150000, 220000};
    private static final boolean[] FISH_FACES_RIGHT = {
            false, false, true, false, true, true, true, false, true, false, true
    };

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sceneryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawRect = new RectF();
    private final List<Fish> fishes = new ArrayList<>();
    private final List<Fish> caughtFishes = new ArrayList<>();
    private final List<FlyingFish> flyingFishes = new ArrayList<>();
    private final List<Coral> corals = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<Bubble> bubbles = new ArrayList<>();
    private final Random random = new Random();

    private Bitmap backgroundBitmap;
    private Bitmap waterTopBitmap;
    private Bitmap waterMiddleBitmap;
    private Bitmap fishermanBitmap;
    private Bitmap[] fishermanCastFrames;
    private Bitmap hookBitmap;
    private Bitmap spinnerBitmap;
    private Bitmap spinnerPointerBitmap;
    private Bitmap sideLandBitmap;
    private Bitmap cloudBitmap;
    private Bitmap[] fishBitmaps;
    private Bitmap[] jellyBitmaps;
    private Bitmap[] coralBitmaps;
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
    private float hookControlX;
    private float hookDesiredControlX;
    private float hookMaxY;
    private float sideLandWidth;
    private float leftWaterBound;
    private float rightWaterBound;
    private float spinnerCenterX;
    private float spinnerCenterY;
    private float spinnerSize;
    private float spinnerPhase;
    private float castAnimationTime;
    private float bottomWaitTime;
    private float selectedDepthPower = 1f;
    private float waveTime;
    private long lastFrameNanos;
    private int money;
    private int castTurns = INITIAL_CAST_TURNS;
    private CastTurnRequestListener castTurnRequestListener;
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

    public void setCastTurnRequestListener(CastTurnRequestListener castTurnRequestListener) {
        this.castTurnRequestListener = castTurnRequestListener;
    }

    public void grantCastTurn() {
        castTurns += 1;
        invalidate();
    }

    private void init() {
        setFocusable(true);
        bitmapPaint.setDither(true);
        bitmapPaint.setFilterBitmap(true);

        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fishing_bg);
        waterTopBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.top);
        waterMiddleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.middle);
        fishermanBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fisherman);
        fishermanCastFrames = new Bitmap[] {
                BitmapFactory.decodeResource(getResources(), R.drawable.fishing_cast_1),
                BitmapFactory.decodeResource(getResources(), R.drawable.fishing_cast_2),
                BitmapFactory.decodeResource(getResources(), R.drawable.fishing_cast_3),
                BitmapFactory.decodeResource(getResources(), R.drawable.fishing_cast_4),
                BitmapFactory.decodeResource(getResources(), R.drawable.fishing_cast_5),
                BitmapFactory.decodeResource(getResources(), R.drawable.fishing_cast_6),
                BitmapFactory.decodeResource(getResources(), R.drawable.fishing_cast_7)
        };
        hookBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cancau);
        spinnerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.oquay);
        spinnerPointerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.muiten);
        sideLandBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dat);
        cloudBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cloud);
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
        coralBitmaps = new Bitmap[] {
                BitmapFactory.decodeResource(getResources(), R.drawable.sanho1),
                BitmapFactory.decodeResource(getResources(), R.drawable.sanho2),
                BitmapFactory.decodeResource(getResources(), R.drawable.sanho3)
        };

        linePaint.setColor(Color.argb(210, 238, 248, 255));
        linePaint.setStrokeWidth(dp(2.2f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        bubblePaint.setStyle(Paint.Style.FILL);
        sceneryPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        minSide = Math.min(w, h);
        worldHeight = viewHeight + minSide * 2.75f;
        waterTopY = Math.max(viewHeight * 0.28f, viewHeight * 0.45f - dp(80f));
        sideLandWidth = clamp(viewWidth * 0.16f, dp(42f), minSide * 0.22f);
        leftWaterBound = sideLandWidth * 0.82f;
        rightWaterBound = viewWidth - sideLandWidth * 0.82f;
        hookStartX = rodTipX();
        hookStartY = waterTopY + minSide * 0.015f;
        spinnerSize = minSide * 0.24f;
        spinnerCenterX = viewWidth * 0.5f;
        spinnerCenterY = waterTopY + spinnerSize * 0.12f;
        resetGame();
    }

    private void resetGame() {
        fishes.clear();
        caughtFishes.clear();
        flyingFishes.clear();
        corals.clear();
        floatingTexts.clear();
        bubbles.clear();
        money = 0;
        castTurns = INITIAL_CAST_TURNS;
        hookX = hookStartX;
        hookY = hookStartY;
        hookTargetX = hookStartX;
        hookControlX = hookStartX;
        hookDesiredControlX = hookStartX;
        hookMaxY = worldHeight - minSide * 0.12f;
        cameraY = 0f;
        spinnerPhase = 0f;
        castAnimationTime = 0f;
        bottomWaitTime = 0f;
        selectedDepthPower = 1f;
        hookState = HookState.IDLE;
        lastFrameNanos = 0L;
        for (int i = 0; i < FISH_COUNT; i++) {
            spawnFish(true, randomDepthInZone(i % DEPTH_ZONE_FISH.length));
        }
        spawnCorals();
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
        updateCastAnimation(dt);
        updateHook(dt);
        updateCamera(dt);
        updateFlyingFishes(dt);
        updateFloatingTexts(dt);
        updateBubbles(dt);
        if (random.nextFloat() < dt * 2.4f) {
            spawnBubble();
        }
    }

    private void updateSpinner(float dt) {
        if (hookState == HookState.IDLE) {
            float centerBoost = 1f - Math.abs(spinnerPointerProgress() - 0.5f) * 2f;
            float speedMultiplier = 1f + (float) Math.pow(clamp(centerBoost, 0f, 1f), 3f) * 3.8f;
            spinnerPhase = positiveModulo(spinnerPhase + dt * 0.72f * speedMultiplier, 1f);
        }
    }

    private void updateFish(float dt) {
        for (int i = fishes.size() - 1; i >= 0; i--) {
            Fish fish = fishes.get(i);
            if (caughtFishes.contains(fish)) {
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
        hookStartX = rodTipX();
        hookStartY = waterTopY + minSide * 0.015f;
        if (hookState == HookState.IDLE) {
            hookX = hookStartX;
            hookY = hookStartY;
            hookControlX = hookStartX;
            hookDesiredControlX = hookStartX;
            return;
        }

        if (hookState == HookState.CASTING) {
            hookX = hookStartX;
            hookY = hookStartY;
            hookControlX = hookStartX;
            hookDesiredControlX = hookStartX;
            return;
        }

        if (hookState == HookState.DROPPING) {
            hookX += (hookTargetX - hookX) * Math.min(1f, dt * 3.2f);
            hookY += minSide * HOOK_DROP_SPEED * dt;
            if (hookY >= hookMaxY) {
                hookY = hookMaxY;
                hookState = HookState.BOTTOM_WAITING;
                bottomWaitTime = 0f;
                hookControlX = clamp(hookX, minHookControlX(), maxHookControlX());
                hookDesiredControlX = hookControlX;
            }
            return;
        }

        if (hookState == HookState.BOTTOM_WAITING) {
            updateHookHorizontal(dt);
            bottomWaitTime += dt;
            if (bottomWaitTime >= BOTTOM_WAIT_TIME) {
                hookState = HookState.PULLING;
            }
            return;
        }

        float pullMultiplier = caughtFishes.size() >= MAX_CATCH_COUNT ? FULL_CATCH_PULL_MULTIPLIER : 1f;
        float move = minSide * HOOK_PULL_SPEED * pullMultiplier * dt;
        updateHookHorizontal(dt);
        hookY -= move;
        if (caughtFishes.size() < MAX_CATCH_COUNT) {
            checkHookCatch();
        }
        updateCaughtFishPositions();
        if (hookY <= hookStartY + minSide * 0.02f) {
            finishPull();
        }
    }

    private void updateHookHorizontal(float dt) {
        hookControlX += (hookDesiredControlX - hookControlX) * Math.min(1f, dt * 15f);
        hookX += (hookControlX - hookX) * Math.min(1f, dt * 24f);
    }

    private void updateCastAnimation(float dt) {
        if (hookState != HookState.CASTING) {
            return;
        }
        castAnimationTime += dt;
        if (castAnimationTime >= castAnimationDuration()) {
            hookX = hookStartX;
            hookY = hookStartY;
            hookState = HookState.DROPPING;
        }
    }

    private void updateCamera(float dt) {
        float followY = (hookState == HookState.IDLE || hookState == HookState.CASTING)
                ? 0f : hookY - viewHeight * 0.58f;
        float targetCameraY = clamp(followY, 0f, maxCameraY());
        cameraY += (targetCameraY - cameraY) * Math.min(1f, dt * 5.4f);
        if (Math.abs(targetCameraY - cameraY) < 0.5f) {
            cameraY = targetCameraY;
        }
    }

    private void checkHookCatch() {
        if (caughtFishes.size() >= MAX_CATCH_COUNT) {
            return;
        }
        float hookRadius = minSide * 0.045f;
        for (Fish fish : fishes) {
            if (caughtFishes.contains(fish)) {
                continue;
            }
            float dx = fish.x - hookCatchPointX();
            float dy = fish.y - hookCatchPointY();
            if (dx * dx + dy * dy <= (hookRadius + fish.size * 0.38f) * (hookRadius + fish.size * 0.38f)) {
                caughtFishes.add(fish);
                fish.caughtPhase = randomBetween(0f, (float) Math.PI * 2f);
                hookState = HookState.PULLING;
                spawnCatchSplash(fish.x, fish.y);
                if (caughtFishes.size() >= MAX_CATCH_COUNT) {
                    spawnFullCatchSplash();
                    return;
                }
            }
        }
    }

    private void finishPull() {
        hookState = HookState.IDLE;
        hookX = hookStartX;
        hookY = hookStartY;
        if (caughtFishes.isEmpty()) {
            return;
        }

        int caughtCount = caughtFishes.size();
        float textStartX = hookStartX - (caughtCount - 1) * minSide * 0.035f;
        for (int i = 0; i < caughtFishes.size(); i++) {
            Fish fish = caughtFishes.get(i);
            money += fish.value;
            spawnMoneyText(textStartX + i * minSide * 0.07f, waterTopY + minSide * 0.03f, fish.value);
            spawnFlyingFish(fish, i, caughtCount);
            fishes.remove(fish);
        }
        caughtFishes.clear();
        for (int i = 0; i < caughtCount; i++) {
            spawnFish(false);
        }
        notifyMoneyChanged();
    }

    private void updateCaughtFishPositions() {
        if (caughtFishes.isEmpty()) {
            return;
        }
        boolean fullCatch = caughtFishes.size() >= MAX_CATCH_COUNT;
        for (int i = 0; i < caughtFishes.size(); i++) {
            Fish fish = caughtFishes.get(i);
            float airLift = fullCatch ? minSide * (0.06f + i * 0.012f) : 0f;
            float baseRotation = -90f;
            float wobble = (float) Math.sin(waveTime * 10.5f + fish.caughtPhase + i * 0.8f) * 11f;
            fish.caughtRotation = fish.directional ? baseRotation + wobble : wobble;
            placeFishHeadAt(fish, hookCatchPointX(), hookCatchPointY() - airLift);
        }
    }

    private void placeFishHeadAt(Fish fish, float headX, float headY) {
        if (!fish.directional) {
            fish.x = headX;
            fish.y = headY + fish.size * 0.34f;
            return;
        }
        float localHeadX = fishDrawWidth(fish) * 0.44f;
        float radians = (float) Math.toRadians(fish.caughtRotation);
        float rotatedHeadX = (float) Math.cos(radians) * localHeadX;
        float rotatedHeadY = (float) Math.sin(radians) * localHeadX;
        fish.x = headX - rotatedHeadX;
        fish.y = headY - rotatedHeadY;
    }

    private void spawnFlyingFish(Fish fish, int index, int count) {
        FlyingFish flyingFish = new FlyingFish();
        flyingFish.bitmap = fish.bitmap;
        flyingFish.x = fish.x;
        flyingFish.y = fish.y;
        flyingFish.size = fish.size;
        flyingFish.flip = caughtFishFlip(fish);
        flyingFish.rotation = fish.directional ? fish.caughtRotation : randomBetween(-16f, 16f);
        float centerOffset = index - (count - 1) * 0.5f;
        flyingFish.vx = centerOffset * minSide * 0.22f + randomBetween(-minSide * 0.12f, minSide * 0.12f);
        flyingFish.vy = -minSide * randomBetween(1.25f, 1.65f);
        flyingFish.rotationSpeed = randomBetween(-150f, 150f);
        flyingFish.life = 0.95f;
        flyingFishes.add(flyingFish);
    }

    private void updateFlyingFishes(float dt) {
        Iterator<FlyingFish> iterator = flyingFishes.iterator();
        while (iterator.hasNext()) {
            FlyingFish fish = iterator.next();
            fish.age += dt;
            fish.x += fish.vx * dt;
            fish.y += fish.vy * dt;
            fish.vy += minSide * 2.2f * dt;
            fish.rotation += fish.rotationSpeed * dt;
            if (fish.age >= fish.life) {
                iterator.remove();
            }
        }
    }

    private void spawnFish(boolean anywhere) {
        spawnFish(anywhere, randomDepthInSparseZone());
    }

    private void spawnFish(boolean anywhere, float depthHint) {
        Fish fish = new Fish();
        float minY = waterTopY + minSide * 0.16f;
        float maxY = worldHeight - minSide * 0.13f;
        depthHint = clamp(depthHint, 0f, 1f);
        float depthSpread = 0.08f;
        float depth = clamp(depthHint + randomBetween(-depthSpread, depthSpread), 0f, 1f);
        fish.y = minY + (maxY - minY) * depth;
        setupFishForDepth(fish, depth);
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

    private void setupFishForDepth(Fish fish, float depth) {
        int zone = depthZoneForDepth(depth);
        fish.zoneIndex = zone;
        setupFishBitmap(fish, DEPTH_ZONE_FISH[zone]);
        fish.directional = true;
        float zoneProgress = zone / (float) Math.max(1, DEPTH_ZONE_FISH.length - 1);
        fish.size = minSide * randomBetween(0.108f + zoneProgress * 0.045f,
                0.145f + zoneProgress * 0.085f);
        fish.value = randomBetweenInt(DEPTH_ZONE_MIN_VALUE[zone], DEPTH_ZONE_MAX_VALUE[zone]);
    }

    private int depthZoneForDepth(float depth) {
        return Math.min(DEPTH_ZONE_FISH.length - 1,
                Math.max(0, (int) (clamp(depth, 0f, 0.999f) * DEPTH_ZONE_FISH.length)));
    }

    private float randomDepthInZone(int zone) {
        float zoneSize = 1f / DEPTH_ZONE_FISH.length;
        return clamp((zone + randomBetween(0.02f, 0.98f)) * zoneSize, 0f, 1f);
    }

    private float randomDepthInSparseZone() {
        int[] counts = new int[DEPTH_ZONE_FISH.length];
        for (Fish fish : fishes) {
            if (!caughtFishes.contains(fish) && fish.zoneIndex >= 0 && fish.zoneIndex < counts.length) {
                counts[fish.zoneIndex]++;
            }
        }
        int minCount = Integer.MAX_VALUE;
        int candidates = 0;
        for (int count : counts) {
            if (count < minCount) {
                minCount = count;
                candidates = 1;
            } else if (count == minCount) {
                candidates++;
            }
        }
        int pick = random.nextInt(Math.max(1, candidates));
        for (int zone = 0; zone < counts.length; zone++) {
            if (counts[zone] == minCount) {
                if (pick == 0) {
                    return randomDepthInZone(zone);
                }
                pick--;
            }
        }
        return random.nextFloat();
    }

    private void setupFishBitmap(Fish fish, int index) {
        fish.bitmap = fishBitmaps[index];
        fish.facesRight = FISH_FACES_RIGHT[index];
    }

    private void setupJellyBitmap(Fish fish) {
        fish.bitmap = jellyBitmaps[random.nextInt(jellyBitmaps.length)];
        fish.directional = false;
        fish.facesRight = true;
    }

    private void updateFishFacing(Fish fish) {
        fish.flip = fish.directional && ((fish.vx > 0f) != fish.facesRight);
    }

    private void spawnCorals() {
        if (coralBitmaps == null || coralBitmaps.length == 0) {
            return;
        }
        float startY = waterTopY + minSide * 0.35f;
        float endY = worldHeight - minSide * 0.18f;
        float step = minSide * 0.42f;
        for (float y = startY; y < endY; y += step) {
            if (random.nextFloat() < 0.42f) {
                spawnCoral(false, y + randomBetween(-step * 0.28f, step * 0.28f));
            }
            if (random.nextFloat() < 0.42f) {
                spawnCoral(true, y + randomBetween(-step * 0.28f, step * 0.28f));
            }
        }
    }

    private void spawnCoral(boolean rightSide, float y) {
        Coral coral = new Coral();
        coral.bitmap = coralBitmaps[random.nextInt(coralBitmaps.length)];
        coral.size = minSide * randomBetween(0.07f, 0.12f);
        coral.y = clamp(y, waterTopY + minSide * 0.18f, worldHeight - minSide * 0.12f);
        coral.x = rightSide
                ? rightWaterBound + sideLandWidth * randomBetween(0.08f, 0.27f)
                : leftWaterBound - sideLandWidth * randomBetween(0.08f, 0.27f);
        coral.flip = rightSide;
        coral.rotation = randomBetween(-7f, 7f);
        corals.add(coral);
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

    private void spawnFullCatchSplash() {
        for (Fish fish : caughtFishes) {
            spawnCatchSplash(fish.x, fish.y);
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
        drawCorals(canvas);
        drawWaterWaves(canvas);
        drawBubbles(canvas);
        drawFishes(canvas);
        drawFlyingFishes(canvas);
        if (hookState != HookState.IDLE && hookState != HookState.CASTING) {
            drawHook(canvas);
        }
        drawFisherman(canvas);
        if (hookState == HookState.IDLE) {
            drawIdleHook(canvas);
        }
        drawFloatingTexts(canvas);
        canvas.restore();
        drawSpinner(canvas);
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundBitmap != null) {
            float waterlineInBitmap = backgroundBitmap.getHeight() * BACKGROUND_WATERLINE_FRACTION;
            float scale = Math.max(viewWidth / backgroundBitmap.getWidth(), waterTopY / waterlineInBitmap);
            float width = backgroundBitmap.getWidth() * scale;
            float height = backgroundBitmap.getHeight() * scale;
            float left = (viewWidth - width) * 0.5f;
            float top = waterTopY - waterlineInBitmap * scale;
            drawRect.set(left, top, left + width, top + height);
            canvas.drawBitmap(backgroundBitmap, null, drawRect, bitmapPaint);
        } else {
            drawSkyFallback(canvas);
        }
        drawClouds(canvas);
    }

    private void drawWater(Canvas canvas) {
        Shader previousShader = waterPaint.getShader();
        waterPaint.setShader(new LinearGradient(0f, waterTopY, 0f, worldHeight,
                new int[] {
                        Color.rgb(0, 93, 244),
                        Color.rgb(11, 54, 218),
                        Color.rgb(13, 30, 155)
                },
                new float[] {0f, 0.42f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0f, waterTopY, viewWidth, worldHeight, waterPaint);
        waterPaint.setShader(previousShader);
        drawDepthShadows(canvas);
    }

    private void drawWaterWaves(Canvas canvas) {
        drawWaterLayer(canvas, waterMiddleBitmap, waterTopY + minSide * 0.015f,
                minSide * 0.28f, minSide * 0.014f, 0.9f, 0f);
        drawWaterLayer(canvas, waterTopBitmap, waterTopY - minSide * 0.018f,
                minSide * 0.24f, minSide * 0.02f, 0.9f, (float) Math.PI);
    }

    private void drawWaterLayer(Canvas canvas, Bitmap bitmap, float top, float height,
                                float waveAmplitude, float waveSpeed, float phase) {
        if (bitmap == null) {
            return;
        }
        float width = height * bitmap.getWidth() / bitmap.getHeight();
        int tile = 0;
        for (float left = -width * 0.5f; left < viewWidth + width; left += width) {
            float waveY = (float) Math.sin(waveTime * waveSpeed + phase + tile * 0.45f) * waveAmplitude;
            drawRect.set(left, top + waveY, left + width, top + height + waveY);
            canvas.drawBitmap(bitmap, null, drawRect, bitmapPaint);
            tile++;
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

    private void drawCorals(Canvas canvas) {
        for (Coral coral : corals) {
            drawBitmapCentered(canvas, coral.bitmap, coral.x, coral.y, coral.size,
                    coral.flip, coral.rotation, 235);
        }
    }

    private void drawFishes(Canvas canvas) {
        for (Fish fish : fishes) {
            if (caughtFishes.contains(fish)) {
                drawBitmapCentered(canvas, fish.bitmap, fish.x, fish.y, fish.size, caughtFishFlip(fish), fish.caughtRotation, 255);
                continue;
            }
            float bob = (float) Math.sin(fish.wiggle + fish.waveOffset) * minSide * 0.006f;
            drawBitmapCentered(canvas, fish.bitmap, fish.x, fish.y + bob, fish.size, fish.flip, 0f, 255);
        }
    }

    private void drawFlyingFishes(Canvas canvas) {
        for (FlyingFish fish : flyingFishes) {
            float progress = Math.min(1f, fish.age / fish.life);
            int alpha = (int) (255f * (1f - progress * 0.35f));
            drawBitmapCentered(canvas, fish.bitmap, fish.x, fish.y, fish.size, fish.flip, fish.rotation, alpha);
        }
    }

    private void drawHook(Canvas canvas) {
        canvas.drawLine(rodTipX(), rodTipY(), hookLineAttachX(), hookLineAttachY(), linePaint);
        drawBitmapCentered(canvas, hookBitmap, hookDrawCenterX(), hookY, hookDrawSize(), false, 0f, 255);
    }

    private void drawIdleHook(Canvas canvas) {
        float idleHookSize = hookDrawSize() * 0.62f;
        float idleHookX = rodTipX() - idleHookSize * 0.12f - dp(3f);
        float idleHookY = rodTipY() + minSide * 0.075f;
        canvas.drawLine(rodTipX(), rodTipY(), rodTipX(), idleHookY - idleHookSize * 0.45f, linePaint);
        drawBitmapCentered(canvas, hookBitmap, idleHookX, idleHookY, idleHookSize, false, 0f, 220);
    }

    private void drawSpinner(Canvas canvas) {
        float wheelAlpha = hookState == HookState.IDLE ? 255 : 150;
        drawBitmapCentered(canvas, spinnerBitmap, spinnerCenterX, spinnerCenterY, spinnerSize, false, 0f, (int) wheelAlpha);

        Paint.Style previousStyle = bubblePaint.getStyle();
        float previousStrokeWidth = bubblePaint.getStrokeWidth();
        float arcRadius = spinnerSize * 0.58f;
        drawRect.set(spinnerCenterX - arcRadius, spinnerCenterY - arcRadius,
                spinnerCenterX + arcRadius, spinnerCenterY + arcRadius);
        bubblePaint.setStyle(Paint.Style.STROKE);
        bubblePaint.setStrokeWidth(dp(4f));
        bubblePaint.setStrokeCap(Paint.Cap.ROUND);
        bubblePaint.setColor(Color.argb(170, 255, 255, 255));
        canvas.drawArc(drawRect, 180f, 180f, false, bubblePaint);

        float pointerProgress = spinnerPointerProgress();
        float pointerAngle = spinnerPointerAngleRadians(pointerProgress);
        float pointerX = spinnerCenterX + (float) Math.cos(pointerAngle) * arcRadius;
        float pointerY = spinnerCenterY + (float) Math.sin(pointerAngle) * arcRadius;
        float pointerSize = spinnerSize * 0.2f;
        float pointerRotation = (float) Math.toDegrees(pointerAngle) + 90f;
        drawBitmapCentered(canvas, spinnerPointerBitmap, pointerX, pointerY,
                pointerSize, false, pointerRotation, 255);

        textPaint.setTextSize(spinnerSize * 0.085f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("MIN", spinnerCenterX - arcRadius * 0.9f, spinnerCenterY - spinnerSize * 0.05f, textPaint);
        canvas.drawText("MAX", spinnerCenterX, spinnerCenterY - arcRadius - spinnerSize * 0.06f, textPaint);
        canvas.drawText("MIN", spinnerCenterX + arcRadius * 0.9f, spinnerCenterY - spinnerSize * 0.05f, textPaint);

        textPaint.setTextSize(spinnerSize * 0.15f);
        textPaint.setColor(hookState == HookState.IDLE ? Color.WHITE : Color.argb(145, 255, 255, 255));
        canvas.drawText("PLAY", spinnerCenterX, spinnerCenterY + spinnerSize * 0.12f, textPaint);

        textPaint.setTextSize(spinnerSize * 0.07f);
        textPaint.setColor(Color.argb(hookState == HookState.IDLE ? 230 : 145, 255, 255, 255));
        canvas.drawText("Luot: " + castTurns, spinnerCenterX, spinnerCenterY + spinnerSize * 0.28f, textPaint);

        if (hookState != HookState.IDLE) {
            textPaint.setTextSize(spinnerSize * 0.075f);
            textPaint.setColor(Color.argb(225, 255, 255, 255));
            int meters = 8 + Math.round(selectedDepthPower * 25f);
            canvas.drawText(meters + "m", spinnerCenterX, spinnerCenterY + spinnerSize * 0.48f, textPaint);
        }
        bubblePaint.setStyle(previousStyle);
        bubblePaint.setStrokeWidth(previousStrokeWidth);
    }

    private void drawFisherman(Canvas canvas) {
        if (fishermanBitmap == null) {
            return;
        }
        Bitmap activeFisherman = currentFishermanBitmap();
        float width = fishermanDrawWidth();
        float height = fishermanDrawHeight(activeFisherman);
        float left = fishermanLeft();
        float bottom = fishermanBottom();
        drawRect.set(left, bottom - height, left + width, bottom);
        canvas.drawBitmap(activeFisherman, null, drawRect, bitmapPaint);
    }

    private Bitmap currentFishermanBitmap() {
        if (hookState != HookState.CASTING || fishermanCastFrames == null || fishermanCastFrames.length == 0) {
            return fishermanBitmap;
        }
        int index = Math.min(fishermanCastFrames.length - 1,
                (int) (castAnimationTime / CAST_FRAME_TIME));
        Bitmap frame = fishermanCastFrames[index];
        return frame != null ? frame : fishermanBitmap;
    }

    private float castAnimationDuration() {
        return fishermanCastFrames == null ? 0f : fishermanCastFrames.length * CAST_FRAME_TIME;
    }

    private float fishermanDrawWidth() {
        return minSide * 0.56f;
    }

    private float fishermanDrawHeight() {
        return fishermanDrawHeight(fishermanBitmap);
    }

    private float fishermanDrawHeight(Bitmap bitmap) {
        if (bitmap == null) {
            return 0f;
        }
        return fishermanDrawWidth() * bitmap.getHeight() / bitmap.getWidth();
    }

    private float fishermanLeft() {
        return viewWidth - fishermanDrawWidth() * 0.96f + fishermanSwayX();
    }

    private float fishermanBottom() {
        return waterTopY + minSide * 0.055f + fishermanBobY();
    }

    private float fishermanSwayX() {
        return (float) Math.sin(waveTime * 0.95f) * minSide * 0.006f;
    }

    private float fishermanBobY() {
        return (float) Math.sin(waveTime * 1.25f + 0.7f) * minSide * 0.01f;
    }

    private float rodTipX() {
        return fishermanLeft() + fishermanDrawWidth() * 0.145f;
    }

    private float rodTipY() {
        return fishermanBottom() - fishermanDrawHeight() + fishermanDrawHeight() * 0.015f;
    }

    private float hookDrawSize() {
        return minSide * 0.12f;
    }

    private float hookDrawWidth() {
        if (hookBitmap == null) {
            return hookDrawSize();
        }
        return hookDrawSize() * hookBitmap.getWidth() / hookBitmap.getHeight();
    }

    private float hookLineAttachX() {
        return hookX + hookDrawWidth() * 0.24f;
    }

    private float hookLineAttachY() {
        return hookY - hookDrawSize() * 0.49f;
    }

    private float hookDrawCenterX() {
        return hookX - dp(3f);
    }

    private float hookCatchPointX() {
        return hookDrawCenterX() - hookDrawWidth() * 0.2f;
    }

    private float hookCatchPointY() {
        return hookY + hookDrawSize() * 0.08f;
    }

    private float fishDrawWidth(Fish fish) {
        if (fish.bitmap == null) {
            return fish.size;
        }
        if (fish.bitmap.getWidth() >= fish.bitmap.getHeight()) {
            return fish.size;
        }
        return fish.size * fish.bitmap.getWidth() / fish.bitmap.getHeight();
    }

    private boolean displayedFishFacesRight(Fish fish) {
        return fish.facesRight != fish.flip;
    }

    private boolean caughtFishFlip(Fish fish) {
        return fish.directional && !fish.facesRight;
    }

    private void drawSkyFallback(Canvas canvas) {
        Shader previousShader = sceneryPaint.getShader();
        sceneryPaint.setShader(new LinearGradient(0f, 0f, 0f, waterTopY,
                Color.rgb(94, 87, 248), Color.rgb(89, 224, 227), Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, viewWidth, waterTopY, sceneryPaint);
        sceneryPaint.setShader(previousShader);

        sceneryPaint.setColor(Color.rgb(94, 170, 67));
        drawRect.set(0f, waterTopY - minSide * 0.12f, viewWidth, waterTopY + minSide * 0.03f);
        canvas.drawRect(drawRect, sceneryPaint);
    }

    private void drawClouds(Canvas canvas) {
        if (cloudBitmap == null) {
            return;
        }
        drawBitmapCentered(canvas, cloudBitmap, viewWidth * 0.24f, waterTopY * 0.22f,
                minSide * 0.16f, false, 0f, 220);
        drawBitmapCentered(canvas, cloudBitmap, viewWidth * 0.68f, waterTopY * 0.31f,
                minSide * 0.18f, false, 0f, 210);
        drawBitmapCentered(canvas, cloudBitmap, viewWidth * 0.88f, waterTopY * 0.18f,
                minSide * 0.15f, false, 0f, 225);
    }

    private void drawDepthShadows(Canvas canvas) {
        bubblePaint.setStyle(Paint.Style.FILL);
        bubblePaint.setColor(Color.argb(34, 0, 23, 105));
        for (int i = 0; i < 7; i++) {
            float depth = (i + 0.5f) / 7f;
            float centerY = waterTopY + (worldHeight - waterTopY) * depth;
            float centerX = i % 2 == 0 ? leftWaterBound + sideLandWidth * 0.42f : rightWaterBound - sideLandWidth * 0.42f;
            drawRect.set(centerX - minSide * 0.34f, centerY - minSide * 0.16f,
                    centerX + minSide * 0.34f, centerY + minSide * 0.16f);
            canvas.drawOval(drawRect, bubblePaint);
        }
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
            textPaint.setTextSize(minSide * 0.058f);
            String label = "+$" + formatCompactMoney(text.value);
            textPaint.setStyle(Paint.Style.STROKE);
            textPaint.setStrokeWidth(dp(3f));
            textPaint.setColor(Color.argb(alpha, 93, 55, 0));
            canvas.drawText(label, text.x, text.y, textPaint);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setStrokeWidth(0f);
            textPaint.setColor(Color.argb(alpha, 255, 226, 72));
            textPaint.setShadowLayer(dp(2f), 0f, dp(1f), Color.argb(alpha, 120, 67, 0));
            canvas.drawText(label, text.x, text.y, textPaint);
            textPaint.clearShadowLayer();
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
        if (hookState == HookState.PULLING || hookState == HookState.BOTTOM_WAITING) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                    || event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                hookDesiredControlX = clamp(event.getX(), minHookControlX(), maxHookControlX());
                return true;
            }
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && hookState == HookState.IDLE) {
            if (castTurns <= 0) {
                showReceiveCastTurnPopup();
                return true;
            }
            castTurns--;
            selectedDepthPower = currentDepthPower();
            hookTargetX = hookStartX;
            hookControlX = hookStartX;
            hookDesiredControlX = hookStartX;
            hookMaxY = waterTopY + minSide * (MIN_CAST_DEPTH_FRACTION
                    + (MAX_CAST_DEPTH_FRACTION - MIN_CAST_DEPTH_FRACTION) * selectedDepthPower);
            hookMaxY = clamp(hookMaxY, waterTopY + minSide * 0.34f, worldHeight - minSide * 0.12f);
            hookState = HookState.CASTING;
            castAnimationTime = 0f;
            caughtFishes.clear();
            return true;
        }
        return true;
    }

    private float currentDepthPower() {
        float pointerX = spinnerPointerProgress();
        float distanceFromCenter = Math.abs(pointerX - 0.5f) * 2f;
        float rawPower = clamp(1f - distanceFromCenter, 0f, 1f);
        return (float) Math.pow(rawPower, 2.6f);
    }

    private void showReceiveCastTurnPopup() {
        if (castTurnRequestListener != null) {
            castTurnRequestListener.onCastTurnRequested();
        }
    }

    private float spinnerPointerProgress() {
        return 0.5f + 0.5f * (float) Math.sin(spinnerPhase * Math.PI * 2f);
    }

    private float spinnerPointerAngleRadians(float progress) {
        return (float) (Math.PI + Math.PI * progress);
    }

    private void notifyMoneyChanged() {
        if (moneyStateListener != null) {
            moneyStateListener.onMoneyChanged(money);
        }
    }

    private String formatCompactMoney(int value) {
        if (value >= 1_000_000_000) {
            return trimCompact(value / 1_000_000_000f) + "b";
        }
        if (value >= 1_000_000) {
            return trimCompact(value / 1_000_000f) + "m";
        }
        if (value >= 1_000) {
            return trimCompact(value / 1_000f) + "k";
        }
        return String.valueOf(value);
    }

    private String trimCompact(float value) {
        if (value >= 100 || value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format(java.util.Locale.US, "%.1f", value);
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

    private float minHookControlX() {
        return leftWaterBound + hookDrawSize() * 0.45f;
    }

    private float maxHookControlX() {
        return rightWaterBound - hookDrawSize() * 0.45f;
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

    public interface CastTurnRequestListener {
        void onCastTurnRequested();
    }

    private enum HookState {
        IDLE,
        CASTING,
        DROPPING,
        BOTTOM_WAITING,
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
        float caughtPhase;
        float caughtRotation;
        int value;
        boolean flip;
        boolean directional;
        boolean facesRight;
        int zoneIndex;
    }

    private static final class FloatingText {
        float x;
        float y;
        float vy;
        float age;
        float life;
        int value;
    }

    private static final class FlyingFish {
        Bitmap bitmap;
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float rotation;
        float rotationSpeed;
        float age;
        float life;
        boolean flip;
    }

    private static final class Coral {
        Bitmap bitmap;
        float x;
        float y;
        float size;
        float rotation;
        boolean flip;
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

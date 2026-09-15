package com.example.game43.SeaCatcher;

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
import java.util.List;
import java.util.Random;

public class SeaCatcherGameView extends View {
    private static final int FLOATING_OBJECT_TARGET = 18;
    private static final int MAX_CAUGHT_PER_THROW = 5;
    private static final float NET_THROW_SECONDS = 0.18f;
    private static final float TIMER_SECONDS = 30f;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waterParticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawRect = new RectF();
    private final List<FloatingObject> objects = new ArrayList<>();
    private final List<FloatingObject> caughtObjects = new ArrayList<>();
    private final List<CoinBurst> coinBursts = new ArrayList<>();
    private final List<WaterParticle> waterParticles = new ArrayList<>();
    private final Random random = new Random();

    private Bitmap backgroundBitmap;
    private Bitmap raftBitmap;
    private Bitmap characterActionOneBitmap;
    private Bitmap characterActionTwoBitmap;
    private Bitmap barrelBitmap;
    private Bitmap netBitmap;
    private Bitmap ropeBitmap;
    private Bitmap woodBitmap;
    private Bitmap leafBitmap;
    private Bitmap cloverBitmap;
    private Bitmap plankBitmap;
    private Bitmap coinBitmap;

    private float viewWidth;
    private float viewHeight;
    private float minSide;
    private float centerX;
    private float centerY;
    private float waterScrollX;
    private float waterScrollY;
    private float spawnTimer;
    private float netStartX;
    private float netStartY;
    private float netX;
    private float netY;
    private float netTargetX;
    private float netTargetY;
    private float netThrowTime;
    private float timerRemainingSeconds = TIMER_SECONDS;
    private long lastFrameNanos;
    private int score;
    private int money;
    private boolean netActive;
    private boolean pulling;
    private boolean waitForReleaseAfterCollect;
    private boolean characterFacesRight;
    private boolean throwSplashPlayed;
    private TimerStateListener timerStateListener;
    private MoneyStateListener moneyStateListener;

    public SeaCatcherGameView(Context context) {
        super(context);
        init();
    }

    public SeaCatcherGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SeaCatcherGameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(true);
        bitmapPaint.setDither(true);
        bitmapPaint.setFilterBitmap(true);

        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg3);
        raftBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.manhgo);
        characterActionOneBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.action1);
        characterActionTwoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.action2);
        barrelBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.thung);
        netBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.luoidanh);
        ropeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sea_rope);
        woodBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.go);
        leafBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.la);
        cloverBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rau);
        plankBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.manhgo);
        coinBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_coin_icon);

        linePaint.setColor(Color.argb(190, 245, 252, 255));
        linePaint.setStrokeWidth(dp(2f));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        waterParticlePaint.setStyle(Paint.Style.FILL);
    }

    public void setTimerStateListener(TimerStateListener timerStateListener) {
        this.timerStateListener = timerStateListener;
        notifyTimerChanged();
    }

    public void setMoneyStateListener(MoneyStateListener moneyStateListener) {
        this.moneyStateListener = moneyStateListener;
        notifyMoneyChanged();
    }

    public void refillTimer() {
        timerRemainingSeconds = TIMER_SECONDS;
        notifyTimerChanged();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        minSide = Math.min(viewWidth, viewHeight);
        centerX = viewWidth * 0.5f;
        centerY = viewHeight * 0.53f;
        resetGame();
    }

    private void resetGame() {
        objects.clear();
        score = 0;
        money = 0;
        spawnTimer = 0f;
        timerRemainingSeconds = TIMER_SECONDS;
        waterScrollX = 0f;
        waterScrollY = 0f;
        netActive = false;
        pulling = false;
        waitForReleaseAfterCollect = false;
        characterFacesRight = false;
        throwSplashPlayed = false;
        caughtObjects.clear();
        coinBursts.clear();
        waterParticles.clear();
        lastFrameNanos = 0L;
        for (int i = 0; i < FLOATING_OBJECT_TARGET; i++) {
            spawnObject(-random.nextFloat() * viewHeight);
        }
        notifyTimerChanged();
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
        waterScrollX = (waterScrollX + minSide * 0.018f * dt) % Math.max(1f, viewWidth);
        waterScrollY = (waterScrollY + minSide * 0.04f * dt) % Math.max(1f, viewHeight);
        updateTimer(dt);
        updateCoinBursts(dt);
        updateWaterParticles(dt);

        spawnTimer -= dt;
        if (spawnTimer <= 0f && objects.size() < FLOATING_OBJECT_TARGET + 4) {
            spawnObject(0f);
            spawnTimer = 0.35f + random.nextFloat() * 0.55f;
        }

        for (int i = objects.size() - 1; i >= 0; i--) {
            FloatingObject object = objects.get(i);
            if (caughtObjects.contains(object)) {
                continue;
            }
            object.x += object.vx * dt;
            object.y += object.vy * dt;
            object.rotation += object.angularVelocity * dt;
            if (object.y > viewHeight + object.size || object.x < -object.size || object.x > viewWidth + object.size) {
                objects.remove(i);
            }
        }

        while (objects.size() < FLOATING_OBJECT_TARGET) {
            spawnObject(-random.nextFloat() * viewHeight * 0.6f);
        }

        if (netActive) {
            netThrowTime = Math.min(NET_THROW_SECONDS, netThrowTime + dt);
            if (netThrowTime < NET_THROW_SECONDS) {
                float t = netThrowTime / NET_THROW_SECONDS;
                float eased = 1f - (1f - t) * (1f - t);
                netX = netStartX + (netTargetX - netStartX) * eased;
                netY = netStartY + (netTargetY - netStartY) * eased;
            } else {
                if (!throwSplashPlayed) {
                    spawnWaterSplash(netX, netY, false);
                    throwSplashPlayed = true;
                }
                catchObjectsInNet();
                syncCaughtObjectsToNet();
                if (pulling) {
                    pullNet(dt);
                    catchObjectsInNet();
                    syncCaughtObjectsToNet();
                }
            }
        }
    }

    private void updateTimer(float dt) {
        if (timerRemainingSeconds <= 0f) {
            return;
        }
        timerRemainingSeconds = Math.max(0f, timerRemainingSeconds - dt);
        notifyTimerChanged();
    }

    private void spawnObject(float extraTopOffset) {
        FloatingObject object = createFloatingObject();
        object.size = minSide * (0.07f + random.nextFloat() * 0.035f);
        object.x = viewWidth * (0.58f + random.nextFloat() * 0.55f);
        object.y = -object.size + extraTopOffset;
        object.vx = -minSide * (0.04f + random.nextFloat() * 0.08f);
        object.vy = minSide * (0.16f + random.nextFloat() * 0.1f);
        object.rotation = random.nextFloat() * 360f;
        object.angularVelocity = (random.nextFloat() - 0.5f) * 55f;
        objects.add(object);
    }
//Price 
    private FloatingObject createFloatingObject() {
        float roll = random.nextFloat();
        if (roll < 0.28f) {
            return new FloatingObject(woodBitmap, 5);
        }
        if (roll < 0.5f) {
            return new FloatingObject(leafBitmap, 2);
        }
        if (roll < 0.72f) {
            return new FloatingObject(cloverBitmap, 3);
        }
        if (roll < 0.88f) {
            return new FloatingObject(barrelBitmap, 10);
        }
        return new FloatingObject(plankBitmap, 7);
    }

    private void updateCoinBursts(float dt) {
        for (int i = coinBursts.size() - 1; i >= 0; i--) {
            CoinBurst coinBurst = coinBursts.get(i);
            coinBurst.age += dt;
            coinBurst.x += coinBurst.vx * dt;
            coinBurst.y += coinBurst.vy * dt;
            coinBurst.vy -= minSide * 0.35f * dt;
            coinBurst.rotation += coinBurst.angularVelocity * dt;
            if (coinBurst.age >= coinBurst.lifetime) {
                coinBursts.remove(i);
            }
        }
    }

    private void updateWaterParticles(float dt) {
        for (int i = waterParticles.size() - 1; i >= 0; i--) {
            WaterParticle particle = waterParticles.get(i);
            particle.age += dt;
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vy += minSide * 1.15f * dt;
            particle.radius *= 0.985f;
            if (particle.age >= particle.lifetime) {
                waterParticles.remove(i);
            }
        }
    }

    private void catchObjectsInNet() {
        float netRadius = getNetCatchRadius();
        for (FloatingObject object : objects) {
            if (caughtObjects.size() >= MAX_CAUGHT_PER_THROW) {
                return;
            }
            if (caughtObjects.contains(object)) {
                continue;
            }
            float dx = object.x - netX;
            float dy = object.y - netY;
            float distance = (float) Math.hypot(dx, dy);
            if (distance < netRadius + object.size * 0.45f) {
                caughtObjects.add(object);
            }
        }
    }

    private void syncCaughtObjectsToNet() {
        for (int i = 0; i < caughtObjects.size(); i++) {
            FloatingObject object = caughtObjects.get(i);
            float angle = caughtObjects.size() == 1 ? 0f : (float) (Math.PI * 2f * i / caughtObjects.size());
            float offset = caughtObjects.size() == 1 ? 0f : minSide * 0.018f;
            object.x = netX + (float) Math.cos(angle) * offset;
            object.y = netY + (float) Math.sin(angle) * offset;
            object.rotation *= 0.92f;
        }
    }

    private void pullNet(float dt) {
        float pullSpeed = minSide * 2.15f * dt;
        movePointToward(centerX, centerY, pullSpeed);
        syncCaughtObjectsToNet();
        if (distance(netX, netY, centerX, centerY) < minSide * 0.055f) {
            if (!caughtObjects.isEmpty()) {
                int collectedMoney = 0;
                for (FloatingObject object : caughtObjects) {
                    collectedMoney += object.value;
                }
                score += caughtObjects.size();
                money += collectedMoney;
                spawnCoinBursts(caughtObjects.size());
                objects.removeAll(caughtObjects);
                caughtObjects.clear();
                notifyMoneyChanged();
            }
            spawnWaterSplash(centerX, centerY, true);
            netActive = false;
            pulling = false;
            waitForReleaseAfterCollect = true;
        }
    }

    private void movePointToward(float targetX, float targetY, float amount) {
        float dx = targetX - netX;
        float dy = targetY - netY;
        float distance = (float) Math.hypot(dx, dy);
        if (distance <= amount || distance <= 0.001f) {
            netX = targetX;
            netY = targetY;
            return;
        }
        netX += dx / distance * amount;
        netY += dy / distance * amount;
    }

    private void drawGame(Canvas canvas) {
        drawWater(canvas);
        drawFloatingObjects(canvas);
        drawNet(canvas);
        drawWaterParticles(canvas);
        drawRaft(canvas);
        drawCoinBursts(canvas);
    }

    private void drawWater(Canvas canvas) {
        if (backgroundBitmap == null) {
            canvas.drawColor(Color.rgb(27, 168, 227));
            return;
        }

        float tileSize = Math.max(viewWidth, viewHeight);
        float scale = tileSize / backgroundBitmap.getWidth();
        float width = backgroundBitmap.getWidth() * scale;
        float height = backgroundBitmap.getHeight() * scale;
        float firstLeft = -positiveModulo(waterScrollX, width);
        float firstTop = -positiveModulo(waterScrollY, height);
        for (float left = firstLeft - width; left < viewWidth + width; left += width) {
            for (float top = firstTop - height; top < viewHeight + height; top += height) {
                drawRect.set(left, top, left + width, top + height);
                canvas.drawBitmap(backgroundBitmap, null, drawRect, bitmapPaint);
            }
        }
    }

    private void drawFloatingObjects(Canvas canvas) {
        for (FloatingObject object : objects) {
            drawBitmapCentered(canvas, object.bitmap, object.x, object.y, object.size, object.rotation);
        }
    }

    private void drawNet(Canvas canvas) {
        if (!netActive) {
            return;
        }

        drawRope(canvas);

        float size = pulling ? minSide * 0.17f : minSide * 0.13f;
        drawBitmapCentered(canvas, netBitmap, netX, netY, size, 0f);
    }

    private void drawRope(Canvas canvas) {
        if (ropeBitmap == null) {
            canvas.drawLine(centerX, centerY, netX, netY, linePaint);
            return;
        }

        float dx = netX - centerX;
        float dy = netY - centerY;
        float length = (float) Math.hypot(dx, dy);
        if (length <= 1f) {
            return;
        }

        float ropeHeight = minSide * 0.0175f;
        float segmentLength = ropeHeight * ropeBitmap.getWidth() / ropeBitmap.getHeight();
        int segmentCount = Math.max(1, (int) Math.ceil(length / segmentLength));
        float fittedSegmentLength = length / segmentCount;
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate((float) Math.toDegrees(Math.atan2(dy, dx)));
        for (int i = 0; i < segmentCount; i++) {
            float left = i * fittedSegmentLength;
            float overlap = i < segmentCount - 1 ? ropeHeight * 0.35f : 0f;
            drawRect.set(left, -ropeHeight * 0.5f, left + fittedSegmentLength + overlap, ropeHeight * 0.5f);
            canvas.drawBitmap(ropeBitmap, null, drawRect, bitmapPaint);
        }
        canvas.restore();
    }

    private void drawRaft(Canvas canvas) {
        float raftWidth = minSide * 0.25f;
        float raftHeight = raftWidth * raftBitmap.getHeight() / raftBitmap.getWidth();
        drawRect.set(centerX - raftWidth * 0.5f, centerY - raftHeight * 0.5f,
                centerX + raftWidth * 0.5f, centerY + raftHeight * 0.5f);
        canvas.drawBitmap(raftBitmap, null, drawRect, bitmapPaint);

        drawCharacter(canvas, raftHeight);
    }

    private void drawCharacter(Canvas canvas, float raftHeight) {
        Bitmap characterBitmap = netActive ? characterActionTwoBitmap : characterActionOneBitmap;
        if (characterBitmap == null) {
            return;
        }

        float width = minSide * (netActive ? 0.2175f : 0.1575f);
        float height = width * characterBitmap.getHeight() / characterBitmap.getWidth();
        float bottom = centerY + raftHeight * (netActive ? 0.32f : 0.24f);
        drawRect.set(centerX - width * 0.5f, bottom - height,
                centerX + width * 0.5f, bottom);
        if (characterFacesRight) {
            canvas.save();
            canvas.scale(-1f, 1f, drawRect.centerX(), drawRect.centerY());
            canvas.drawBitmap(characterBitmap, null, drawRect, bitmapPaint);
            canvas.restore();
        } else {
            canvas.drawBitmap(characterBitmap, null, drawRect, bitmapPaint);
        }
    }

    private void spawnCoinBursts(int count) {
        int burstCount = Math.min(8, Math.max(3, count));
        for (int i = 0; i < burstCount; i++) {
            CoinBurst coinBurst = new CoinBurst();
            coinBurst.x = centerX + (random.nextFloat() - 0.5f) * minSide * 0.08f;
            coinBurst.y = centerY - minSide * 0.04f;
            coinBurst.vx = (random.nextFloat() - 0.5f) * minSide * 0.22f;
            coinBurst.vy = -minSide * (0.55f + random.nextFloat() * 0.25f);
            coinBurst.size = minSide * (0.045f + random.nextFloat() * 0.015f);
            coinBurst.rotation = random.nextFloat() * 360f;
            coinBurst.angularVelocity = (random.nextFloat() - 0.5f) * 180f;
            coinBurst.lifetime = 0.8f + random.nextFloat() * 0.25f;
            coinBursts.add(coinBurst);
        }
    }

    private void drawCoinBursts(Canvas canvas) {
        if (coinBitmap == null) {
            return;
        }

        for (CoinBurst coinBurst : coinBursts) {
            float progress = Math.min(1f, coinBurst.age / coinBurst.lifetime);
            int alpha = (int) (255f * (1f - progress));
            int previousAlpha = bitmapPaint.getAlpha();
            bitmapPaint.setAlpha(alpha);
            drawBitmapCentered(canvas, coinBitmap, coinBurst.x, coinBurst.y, coinBurst.size, coinBurst.rotation);
            bitmapPaint.setAlpha(previousAlpha);
        }
    }

    private void spawnWaterSplash(float x, float y, boolean strongSplash) {
        int count = strongSplash ? 22 : 15;
        float spread = strongSplash ? 0.5f : 0.35f;
        for (int i = 0; i < count; i++) {
            float angle = (float) (-Math.PI + random.nextFloat() * Math.PI);
            WaterParticle particle = new WaterParticle();
            particle.x = x + (random.nextFloat() - 0.5f) * minSide * 0.05f;
            particle.y = y + (random.nextFloat() - 0.5f) * minSide * 0.025f;
            float speed = minSide * (0.22f + random.nextFloat() * spread);
            particle.vx = (float) Math.cos(angle) * speed;
            particle.vy = -Math.abs((float) Math.sin(angle) * speed) - minSide * (0.08f + random.nextFloat() * 0.16f);
            particle.radius = minSide * (0.006f + random.nextFloat() * 0.008f);
            particle.lifetime = 0.38f + random.nextFloat() * 0.28f;
            particle.alpha = 145 + random.nextInt(90);
            waterParticles.add(particle);
        }
    }

    private void drawWaterParticles(Canvas canvas) {
        for (WaterParticle particle : waterParticles) {
            float progress = Math.min(1f, particle.age / particle.lifetime);
            int alpha = (int) (particle.alpha * (1f - progress));
            waterParticlePaint.setColor(Color.argb(alpha, 226, 248, 255));
            canvas.drawCircle(particle.x, particle.y, particle.radius, waterParticlePaint);
        }
    }

    private void drawBitmapCentered(Canvas canvas, Bitmap bitmap, float centerX, float centerY, float size, float rotation) {
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
        if (Math.abs(rotation) > 0.01f) {
            canvas.save();
            canvas.rotate(rotation, centerX, centerY);
            canvas.drawBitmap(bitmap, null, drawRect, bitmapPaint);
            canvas.restore();
            return;
        }
        canvas.drawBitmap(bitmap, null, drawRect, bitmapPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                startOrMoveNet(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                releaseNet();
                return true;
            default:
                return true;
        }
    }

    private void startOrMoveNet(float x, float y) {
        if (waitForReleaseAfterCollect) {
            return;
        }
        if (!netActive) {
            if (timerRemainingSeconds <= 0f) {
                return;
            }
            netStartX = centerX;
            netStartY = centerY;
            netTargetX = clamp(x, minSide * 0.06f, viewWidth - minSide * 0.06f);
            netTargetY = clamp(y, minSide * 0.12f, viewHeight - minSide * 0.16f);
            characterFacesRight = netTargetX > centerX;
            netThrowTime = 0f;
            netActive = true;
            pulling = false;
            caughtObjects.clear();
            netX = netStartX;
            netY = netStartY;
            throwSplashPlayed = false;
        } else if (netThrowTime >= NET_THROW_SECONDS) {
            pulling = true;
        }
    }

    private void releaseNet() {
        if (waitForReleaseAfterCollect) {
            waitForReleaseAfterCollect = false;
        }
        pulling = false;
    }

    private float getNetCatchRadius() {
        return minSide * 0.085f;
    }

    private float distance(float ax, float ay, float bx, float by) {
        return (float) Math.hypot(ax - bx, ay - by);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float positiveModulo(float value, float modulo) {
        float result = value % modulo;
        return result < 0f ? result + modulo : result;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void notifyTimerChanged() {
        if (timerStateListener != null) {
            timerStateListener.onTimerChanged(timerRemainingSeconds / TIMER_SECONDS);
        }
    }

    private void notifyMoneyChanged() {
        if (moneyStateListener != null) {
            moneyStateListener.onMoneyChanged(money);
        }
    }

    public interface TimerStateListener {
        void onTimerChanged(float fillFraction);
    }

    public interface MoneyStateListener {
        void onMoneyChanged(int money);
    }

    private static final class FloatingObject {
        final Bitmap bitmap;
        final int value;
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float rotation;
        float angularVelocity;

        FloatingObject(Bitmap bitmap, int value) {
            this.bitmap = bitmap;
            this.value = value;
        }
    }

    private static final class CoinBurst {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float rotation;
        float angularVelocity;
        float age;
        float lifetime;
    }

    private static final class WaterParticle {
        float x;
        float y;
        float vx;
        float vy;
        float radius;
        float age;
        float lifetime;
        int alpha;
    }

}

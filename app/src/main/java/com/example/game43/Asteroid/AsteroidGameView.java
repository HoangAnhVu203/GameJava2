package com.example.game43.Asteroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.game43.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AsteroidGameView extends View {
    private static final int MAX_ASTEROIDS = 10;
    private static final int MAX_BULLETS = 36;
    private static final int MAX_COIN_DROPS = 24;
    private static final int MAX_PARTICLES = 48;
    private static final float SHOT_INTERVAL = 0.16f;
    private static final float ROUND_SECONDS = 30f;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flashPaint = new Paint();
    private final Rect sourceRect = new Rect();
    private final RectF drawRect = new RectF();
    private final RectF barRect = new RectF();
    private final Random random = new Random();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<AsteroidBody> asteroids = new ArrayList<>();
    private final List<CoinDrop> coinDrops = new ArrayList<>();
    private final List<Spark> particles = new ArrayList<>();

    private Bitmap backgroundBitmap;
    private Bitmap planeBitmap;
    private Bitmap bulletBitmap;
    private Bitmap coinBitmap;
    private Bitmap healthBackgroundBitmap;
    private Bitmap healthFillBitmap;
    private Bitmap skullBitmap;
    private Bitmap[] asteroidBitmaps;
    private Bitmap[] ringBitmaps;

    private float viewWidth;
    private float viewHeight;
    private float minSide;
    private float hudBottom;
    private float planeX;
    private float planeY;
    private float targetPlaneX;
    private float targetPlaneY;
    private float planeWidth;
    private float planeHeight;
    private float touchOffsetX;
    private float touchOffsetY;
    private float shootTimer;
    private float spawnTimer;
    private float nextSpawnDelay;
    private float hitFlash;
    private float invincibleTimer;
    private float timeRemaining = ROUND_SECONDS;
    private long lastFrameNanos;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private int health = 100;
    private int destroyedCount;
    private int coins;
    private boolean started;
    private boolean draggingPlane;
    private boolean gameOver;
    private boolean paused;
    private boolean awaitingExtraTime;
    private ExtraTimeRequestListener extraTimeRequestListener;

    private final SharedPreferences preferences;

    public AsteroidGameView(Context context) {
        this(context, null);
    }

    public AsteroidGameView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AsteroidGameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        preferences = context.getSharedPreferences("asteroid_game", Context.MODE_PRIVATE);
        init();
    }

    private void init() {
        setFocusable(true);
        setClickable(true);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        BitmapFactory.Options backgroundOptions = new BitmapFactory.Options();
        backgroundOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        backgroundOptions.inSampleSize = 2;
        backgroundBitmap = BitmapFactory.decodeResource(
                getResources(), R.drawable.backgroundasteroid, backgroundOptions);
        planeBitmap = decode(R.drawable.plane);
        bulletBitmap = decode(R.drawable.bullet);
        coinBitmap = decode(R.drawable.coinnew);
        healthBackgroundBitmap = decode(R.drawable.filledbg);
        healthFillBitmap = decode(R.drawable.filled);
        skullBitmap = decode(R.drawable.skull);
        asteroidBitmaps = new Bitmap[] {
                decode(R.drawable.bluemitiorite),
                decode(R.drawable.greenmitiorite),
                decode(R.drawable.purplemitiorite)
        };
        ringBitmaps = new Bitmap[] {
                decode(R.drawable.itemcircle),
                decode(R.drawable.itemcircle1),
                decode(R.drawable.itemcircle2)
        };

        bitmapPaint.setDither(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(2f));
        borderPaint.setColor(Color.argb(235, 255, 255, 255));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        shadowTextPaint.setColor(Color.argb(185, 0, 0, 20));
        shadowTextPaint.setTextAlign(Paint.Align.CENTER);
        shadowTextPaint.setFakeBoldText(true);
        flashPaint.setColor(Color.WHITE);
        coins = preferences.getInt("coins", 0);
    }

    private Bitmap decode(int resourceId) {
        return BitmapFactory.decodeResource(getResources(), resourceId);
    }

    public void setExtraTimeRequestListener(ExtraTimeRequestListener listener) {
        extraTimeRequestListener = listener;
    }

    public void grantExtraTime() {
        timeRemaining = ROUND_SECONDS;
        awaitingExtraTime = false;
        lastFrameNanos = 0L;
        postInvalidateOnAnimation();
    }

    public void declineExtraTime() {
        awaitingExtraTime = false;
        gameOver = true;
        lastFrameNanos = 0L;
        postInvalidateOnAnimation();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        minSide = Math.min(viewWidth, viewHeight);
        hudBottom = Math.max(dp(72f), viewHeight * 0.115f);
        planeWidth = minSide * 0.145f;
        planeHeight = planeWidth * planeBitmap.getHeight() / planeBitmap.getWidth();
        resetRound();
    }

    private void resetRound() {
        bullets.clear();
        asteroids.clear();
        coinDrops.clear();
        particles.clear();
        planeX = viewWidth * 0.5f;
        planeY = viewHeight - planeHeight * 0.78f - dp(16f);
        targetPlaneX = planeX;
        targetPlaneY = planeY;
        shootTimer = 0f;
        spawnTimer = 0f;
        nextSpawnDelay = 0.35f;
        hitFlash = 0f;
        invincibleTimer = 0f;
        timeRemaining = ROUND_SECONDS;
        health = 100;
        destroyedCount = 0;
        started = false;
        draggingPlane = false;
        gameOver = false;
        awaitingExtraTime = false;
        activePointerId = MotionEvent.INVALID_POINTER_ID;
        lastFrameNanos = 0L;
        invalidate();
    }

    public void pauseGame() {
        paused = true;
        draggingPlane = false;
        activePointerId = MotionEvent.INVALID_POINTER_ID;
    }

    public void resumeGame() {
        paused = false;
        lastFrameNanos = 0L;
        postInvalidateOnAnimation();
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

        if (!paused) {
            update(dt);
        }
        drawGame(canvas);
        if (!paused) {
            postInvalidateOnAnimation();
        }
    }

    private void update(float dt) {
        hitFlash = Math.max(0f, hitFlash - dt * 2.8f);
        invincibleTimer = Math.max(0f, invincibleTimer - dt);
        updateParticles(dt);

        float movementBlend = Math.min(1f, dt * 20f);
        planeX += (targetPlaneX - planeX) * movementBlend;
        planeY += (targetPlaneY - planeY) * movementBlend;

        if (!started || gameOver || awaitingExtraTime) {
            return;
        }

        timeRemaining = Math.max(0f, timeRemaining - dt);
        if (timeRemaining <= 0f) {
            awaitingExtraTime = true;
            draggingPlane = false;
            activePointerId = MotionEvent.INVALID_POINTER_ID;
            if (extraTimeRequestListener != null) {
                post(extraTimeRequestListener::onExtraTimeRequested);
            }
            return;
        }

        shootTimer += dt;
        while (shootTimer >= SHOT_INTERVAL) {
            shootTimer -= SHOT_INTERVAL;
            fireBullet();
        }

        spawnTimer += dt;
        if (spawnTimer >= nextSpawnDelay && asteroids.size() < MAX_ASTEROIDS) {
            spawnTimer = 0f;
            spawnAsteroid();
            float difficulty = Math.min(0.24f, destroyedCount * 0.004f);
            nextSpawnDelay = 0.68f + random.nextFloat() * 0.42f - difficulty;
        }

        updateBullets(dt);
        updateAsteroids(dt);
        resolveBulletHits();
        updateCoinDrops(dt);
        resolvePlaneHits();
    }

    private void fireBullet() {
        if (bullets.size() >= MAX_BULLETS) {
            bullets.remove(0);
        }
        float bulletWidth = Math.max(dp(9f), planeWidth * 0.15f);
        float bulletHeight = bulletWidth * bulletBitmap.getHeight() / bulletBitmap.getWidth();
        bullets.add(new Bullet(
                planeX,
                planeY - planeHeight * 0.48f,
                bulletWidth,
                bulletHeight,
                -viewHeight * 0.88f));
    }

    private void spawnAsteroid() {
        int type = random.nextInt(asteroidBitmaps.length);
        float radius = minSide * (0.065f + random.nextFloat() * 0.055f);
        float margin = radius * 1.25f + dp(5f);
        float x = margin + random.nextFloat() * Math.max(1f, viewWidth - margin * 2f);
        float baseSpeed = viewHeight * (0.105f + random.nextFloat() * 0.075f);
        float vx = viewWidth * (-0.04f + random.nextFloat() * 0.08f);
        boolean ringed = random.nextFloat() < 0.28f;
        int hp = Math.max(1, Math.round(radius / (minSide * 0.027f)));
        if (ringed) {
            hp += 2;
            radius *= 1.08f;
        }
        asteroids.add(new AsteroidBody(
                x,
                -radius * 1.8f,
                radius,
                vx,
                baseSpeed,
                random.nextFloat() * 360f,
                -48f + random.nextFloat() * 96f,
                type,
                hp,
                ringed ? random.nextInt(ringBitmaps.length) : -1));
    }

    private void updateBullets(float dt) {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.y += bullet.velocityY * dt;
            if (bullet.y + bullet.height < -dp(8f)) {
                bullets.remove(i);
            }
        }
    }

    private void updateAsteroids(float dt) {
        for (int i = asteroids.size() - 1; i >= 0; i--) {
            AsteroidBody asteroid = asteroids.get(i);
            asteroid.x += asteroid.velocityX * dt;
            asteroid.y += asteroid.velocityY * dt;
            asteroid.angle += asteroid.spin * dt;
            asteroid.ringAngle -= asteroid.spin * 1.35f * dt;

            float left = dp(7f) + asteroid.radius;
            float right = viewWidth - dp(7f) - asteroid.radius;
            if (asteroid.x < left) {
                asteroid.x = left;
                asteroid.velocityX = Math.abs(asteroid.velocityX);
            } else if (asteroid.x > right) {
                asteroid.x = right;
                asteroid.velocityX = -Math.abs(asteroid.velocityX);
            }

            if (asteroid.y - asteroid.radius > viewHeight) {
                asteroids.remove(i);
            }
        }
    }

    private void resolveBulletHits() {
        for (int bulletIndex = bullets.size() - 1; bulletIndex >= 0; bulletIndex--) {
            Bullet bullet = bullets.get(bulletIndex);
            boolean consumed = false;
            for (int asteroidIndex = asteroids.size() - 1; asteroidIndex >= 0; asteroidIndex--) {
                AsteroidBody asteroid = asteroids.get(asteroidIndex);
                float dx = bullet.x - asteroid.x;
                float dy = bullet.y - asteroid.y;
                float hitRadius = asteroid.radius * 0.78f + bullet.width * 0.5f;
                if (dx * dx + dy * dy > hitRadius * hitRadius) {
                    continue;
                }

                consumed = true;
                asteroid.hp--;
                asteroid.velocityY *= 0.96f;
                addParticles(bullet.x, bullet.y, asteroid.type, 7);
                if (asteroid.hp <= 0) {
                    addParticles(asteroid.x, asteroid.y, asteroid.type, 14);
                    spawnCoinDrops(asteroid.x, asteroid.y);
                    destroyedCount++;
                    asteroids.remove(asteroidIndex);
                }
                break;
            }
            if (consumed) {
                bullets.remove(bulletIndex);
            }
        }
    }

    private void spawnCoinDrops(float x, float y) {
        int count = 1 + random.nextInt(3);
        float radius = minSide * 0.027f;
        float spacing = radius * 1.55f;
        float firstX = x - spacing * (count - 1) * 0.5f;
        for (int i = 0; i < count; i++) {
            if (coinDrops.size() >= MAX_COIN_DROPS) {
                coinDrops.remove(0);
            }
            float coinX = clamp(firstX + spacing * i, radius, viewWidth - radius);
            coinDrops.add(new CoinDrop(
                    coinX,
                    y,
                    radius,
                    viewHeight * (0.29f + random.nextFloat() * 0.05f),
                    random.nextFloat() * 360f,
                    -90f + random.nextFloat() * 180f));
        }
    }

    private void updateCoinDrops(float dt) {
        float planeRadius = planeWidth * 0.36f;
        for (int i = coinDrops.size() - 1; i >= 0; i--) {
            CoinDrop coin = coinDrops.get(i);
            coin.y += coin.velocityY * dt;
            coin.angle += coin.spin * dt;

            if (coin.y - coin.radius > viewHeight) {
                coinDrops.remove(i);
                continue;
            }

            float dx = coin.x - planeX;
            float dy = coin.y - planeY;
            float collectRadius = planeRadius + coin.radius * 0.75f;
            if (dx * dx + dy * dy <= collectRadius * collectRadius) {
                coinDrops.remove(i);
                coins++;
                preferences.edit().putInt("coins", coins).apply();
            }
        }
    }

    private void resolvePlaneHits() {
        if (invincibleTimer > 0f) {
            return;
        }
        float planeRadius = planeWidth * 0.31f;
        for (int i = asteroids.size() - 1; i >= 0; i--) {
            AsteroidBody asteroid = asteroids.get(i);
            float dx = asteroid.x - planeX;
            float dy = asteroid.y - planeY;
            float hitRadius = planeRadius + asteroid.radius * 0.68f;
            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                asteroids.remove(i);
                addParticles(asteroid.x, asteroid.y, asteroid.type, 14);
                damagePlayer(28);
                invincibleTimer = 1.05f;
                return;
            }
        }
    }

    private void damagePlayer(int amount) {
        if (gameOver) {
            return;
        }
        health = Math.max(0, health - amount);
        hitFlash = 0.42f;
        if (health == 0) {
            gameOver = true;
            draggingPlane = false;
            activePointerId = MotionEvent.INVALID_POINTER_ID;
        }
    }

    private void addParticles(float x, float y, int colorIndex, int count) {
        int color;
        if (colorIndex == 0) {
            color = Color.rgb(69, 211, 247);
        } else if (colorIndex == 1) {
            color = Color.rgb(81, 220, 187);
        } else {
            color = Color.rgb(172, 143, 255);
        }
        for (int i = 0; i < count; i++) {
            if (particles.size() >= MAX_PARTICLES) {
                particles.remove(0);
            }
            float angle = random.nextFloat() * (float) Math.PI * 2f;
            float speed = minSide * (0.08f + random.nextFloat() * 0.18f);
            particles.add(new Spark(
                    x,
                    y,
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed,
                    dp(2.25f) + random.nextFloat() * dp(3.75f),
                    0.32f + random.nextFloat() * 0.25f,
                    color));
        }
    }

    private void updateParticles(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Spark particle = particles.get(i);
            particle.life -= dt;
            if (particle.life <= 0f) {
                particles.remove(i);
                continue;
            }
            particle.x += particle.velocityX * dt;
            particle.y += particle.velocityY * dt;
            particle.velocityX *= 0.97f;
            particle.velocityY *= 0.97f;
        }
    }

    private void drawGame(Canvas canvas) {
        drawBackground(canvas);
        drawBullets(canvas);
        drawAsteroids(canvas);
        drawCoinDrops(canvas);
        drawParticles(canvas);
        drawPlane(canvas);
        drawHud(canvas);

        if (!started && !gameOver) {
            drawReadyIndicator(canvas);
        }
        if (gameOver) {
            drawGameOver(canvas);
        }
        if (hitFlash > 0f) {
            flashPaint.setAlpha(Math.round(70f * Math.min(1f, hitFlash / 0.42f)));
            canvas.drawRect(0f, 0f, viewWidth, viewHeight, flashPaint);
        }
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundBitmap == null) {
            canvas.drawColor(Color.rgb(2, 8, 36));
            return;
        }
        float viewRatio = viewWidth / viewHeight;
        float bitmapRatio = (float) backgroundBitmap.getWidth() / backgroundBitmap.getHeight();
        if (bitmapRatio > viewRatio) {
            int sourceWidth = Math.round(backgroundBitmap.getHeight() * viewRatio);
            int left = (backgroundBitmap.getWidth() - sourceWidth) / 2;
            sourceRect.set(left, 0, left + sourceWidth, backgroundBitmap.getHeight());
        } else {
            int sourceHeight = Math.round(backgroundBitmap.getWidth() / viewRatio);
            int top = (backgroundBitmap.getHeight() - sourceHeight) / 2;
            sourceRect.set(0, top, backgroundBitmap.getWidth(), top + sourceHeight);
        }
        drawRect.set(0f, 0f, viewWidth, viewHeight);
        canvas.drawBitmap(backgroundBitmap, sourceRect, drawRect, bitmapPaint);
        drawRect.set(dp(3f), dp(3f), viewWidth - dp(3f), viewHeight - dp(3f));
        canvas.drawRect(drawRect, borderPaint);
    }

    private void drawBullets(Canvas canvas) {
        for (Bullet bullet : bullets) {
            drawRect.set(
                    bullet.x - bullet.width * 0.5f,
                    bullet.y - bullet.height * 0.5f,
                    bullet.x + bullet.width * 0.5f,
                    bullet.y + bullet.height * 0.5f);
            canvas.drawBitmap(bulletBitmap, null, drawRect, bitmapPaint);
        }
    }

    private void drawAsteroids(Canvas canvas) {
        for (AsteroidBody asteroid : asteroids) {
            if (asteroid.ringIndex >= 0) {
                Bitmap ring = ringBitmaps[asteroid.ringIndex];
                float ringRadius = asteroid.radius * 1.43f;
                drawRotatedBitmap(canvas, ring, asteroid.x, asteroid.y, ringRadius, asteroid.ringAngle);
            }
            drawRotatedBitmap(
                    canvas,
                    asteroidBitmaps[asteroid.type],
                    asteroid.x,
                    asteroid.y,
                    asteroid.radius,
                    asteroid.angle);

            if (asteroid.hp > 1) {
                float textSize = Math.max(dp(11f), asteroid.radius * 0.43f);
                textPaint.setTextSize(textSize);
                shadowTextPaint.setTextSize(textSize);
                float textY = asteroid.y - (textPaint.ascent() + textPaint.descent()) * 0.5f;
                canvas.drawText(String.valueOf(asteroid.hp), asteroid.x + dp(1f), textY + dp(1f), shadowTextPaint);
                canvas.drawText(String.valueOf(asteroid.hp), asteroid.x, textY, textPaint);
            }
        }
    }

    private void drawRotatedBitmap(
            Canvas canvas, Bitmap bitmap, float centerX, float centerY, float radius, float angle) {
        canvas.save();
        canvas.rotate(angle, centerX, centerY);
        drawRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        canvas.drawBitmap(bitmap, null, drawRect, bitmapPaint);
        canvas.restore();
    }

    private void drawParticles(Canvas canvas) {
        for (Spark particle : particles) {
            particlePaint.setColor(particle.color);
            particlePaint.setAlpha(Math.max(0, Math.min(255, Math.round(255f * particle.life / particle.maxLife))));
            canvas.drawCircle(particle.x, particle.y, particle.radius, particlePaint);
        }
    }

    private void drawCoinDrops(Canvas canvas) {
        for (CoinDrop coin : coinDrops) {
            float halfWidth = coin.radius;
            float halfHeight = halfWidth * coinBitmap.getHeight() / coinBitmap.getWidth();
            canvas.save();
            canvas.rotate(coin.angle, coin.x, coin.y);
            drawRect.set(
                    coin.x - halfWidth,
                    coin.y - halfHeight,
                    coin.x + halfWidth,
                    coin.y + halfHeight);
            canvas.drawBitmap(coinBitmap, null, drawRect, bitmapPaint);
            canvas.restore();
        }
    }

    private void drawPlane(Canvas canvas) {
        int alpha = 255;
        if (invincibleTimer > 0f && ((int) (invincibleTimer * 12f) & 1) == 0) {
            alpha = 105;
        }
        bitmapPaint.setAlpha(alpha);
        drawRect.set(
                planeX - planeWidth * 0.5f,
                planeY - planeHeight * 0.5f,
                planeX + planeWidth * 0.5f,
                planeY + planeHeight * 0.5f);
        canvas.drawBitmap(planeBitmap, null, drawRect, bitmapPaint);
        bitmapPaint.setAlpha(255);
    }

    private void drawHud(Canvas canvas) {
        float coinSize = minSide * 0.07f;
        float coinLeft = dp(15f);
        float coinTop = dp(18f);
        drawRect.set(coinLeft, coinTop, coinLeft + coinSize, coinTop + coinSize);
        canvas.drawBitmap(coinBitmap, null, drawRect, bitmapPaint);

        float coinTextSize = Math.max(dp(14f), minSide * 0.038f);
        textPaint.setTextAlign(Paint.Align.LEFT);
        shadowTextPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(coinTextSize);
        shadowTextPaint.setTextSize(coinTextSize);
        float coinTextY = coinTop + coinSize * 0.68f;
        canvas.drawText(String.valueOf(coins), coinLeft + coinSize + dp(7f), coinTextY + dp(1f), shadowTextPaint);
        canvas.drawText(String.valueOf(coins), coinLeft + coinSize + dp(6f), coinTextY, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        shadowTextPaint.setTextAlign(Paint.Align.CENTER);

        float barWidth = viewWidth * 0.49f;
        float barHeight = Math.max(dp(8f), barWidth * 43f / 620f);
        float barLeft = viewWidth * 0.5f - barWidth * 0.5f;
        float barTop = dp(25f);
        barRect.set(barLeft, barTop, barLeft + barWidth, barTop + barHeight);
        canvas.drawBitmap(healthBackgroundBitmap, null, barRect, bitmapPaint);

        float fillInsetX = barWidth * 0.014f;
        float fillInsetY = barHeight * 0.21f;
        float timeProgress = Math.max(0f, Math.min(1f, timeRemaining / ROUND_SECONDS));
        float fillRight = barLeft + fillInsetX + (barWidth - fillInsetX * 2f) * timeProgress;
        if (fillRight > barLeft + fillInsetX) {
            canvas.save();
            canvas.clipRect(barLeft + fillInsetX, barTop + fillInsetY, fillRight, barTop + barHeight - fillInsetY);
            drawRect.set(
                    barLeft + fillInsetX,
                    barTop + fillInsetY,
                    barLeft + barWidth - fillInsetX,
                    barTop + barHeight - fillInsetY);
            canvas.drawBitmap(healthFillBitmap, null, drawRect, bitmapPaint);
            canvas.restore();
        }

        float skullSize = minSide * 0.055f;
        float skullLeft = barLeft + barWidth + dp(9f);
        drawRect.set(
                skullLeft,
                barTop + barHeight * 0.5f - skullSize * 0.5f,
                skullLeft + skullSize,
                barTop + barHeight * 0.5f + skullSize * 0.5f);
        canvas.drawBitmap(skullBitmap, null, drawRect, bitmapPaint);
    }

    private void drawReadyIndicator(Canvas canvas) {
        float pulse = 1f + 0.08f * (float) Math.sin(System.nanoTime() / 230_000_000d);
        particlePaint.setStyle(Paint.Style.STROKE);
        particlePaint.setStrokeWidth(dp(2f));
        particlePaint.setColor(Color.argb(170, 126, 228, 255));
        canvas.drawCircle(planeX, planeY, planeWidth * 0.72f * pulse, particlePaint);
        particlePaint.setStyle(Paint.Style.FILL);
    }

    private void drawGameOver(Canvas canvas) {
        particlePaint.setColor(Color.argb(175, 0, 3, 22));
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, particlePaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(minSide * 0.11f);
        canvas.drawText("GAME OVER", viewWidth * 0.5f, viewHeight * 0.47f, textPaint);
        textPaint.setTextSize(minSide * 0.045f);
        canvas.drawText("Chạm để chơi lại", viewWidth * 0.5f, viewHeight * 0.53f, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            if (gameOver) {
                resetRound();
                performClick();
                return true;
            }
            if (awaitingExtraTime) {
                return true;
            }
            float x = event.getX();
            float y = event.getY();
            if (isInsidePlane(x, y)) {
                activePointerId = event.getPointerId(0);
                draggingPlane = true;
                started = true;
                touchOffsetX = planeX - x;
                touchOffsetY = planeY - y;
                setPlaneTarget(x + touchOffsetX, y + touchOffsetY);
            }
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE && draggingPlane) {
            int pointerIndex = event.findPointerIndex(activePointerId);
            if (pointerIndex >= 0) {
                setPlaneTarget(
                        event.getX(pointerIndex) + touchOffsetX,
                        event.getY(pointerIndex) + touchOffsetY);
            }
            return true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            draggingPlane = false;
            activePointerId = MotionEvent.INVALID_POINTER_ID;
            performClick();
            return true;
        }

        if (action == MotionEvent.ACTION_POINTER_UP
                && event.getPointerId(event.getActionIndex()) == activePointerId) {
            draggingPlane = false;
            activePointerId = MotionEvent.INVALID_POINTER_ID;
            return true;
        }
        return true;
    }

    private boolean isInsidePlane(float x, float y) {
        return Math.abs(x - planeX) <= planeWidth * 0.78f
                && Math.abs(y - planeY) <= planeHeight * 0.66f;
    }

    private void setPlaneTarget(float x, float y) {
        float halfWidth = planeWidth * 0.5f;
        float halfHeight = planeHeight * 0.5f;
        targetPlaneX = clamp(x, dp(6f) + halfWidth, viewWidth - dp(6f) - halfWidth);
        targetPlaneY = clamp(y, hudBottom + halfHeight, viewHeight - dp(7f) - halfHeight);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private static final class Bullet {
        float x;
        float y;
        final float width;
        final float height;
        final float velocityY;

        Bullet(float x, float y, float width, float height, float velocityY) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.velocityY = velocityY;
        }
    }

    private static final class AsteroidBody {
        float x;
        float y;
        final float radius;
        float velocityX;
        float velocityY;
        float angle;
        final float spin;
        final int type;
        int hp;
        final int ringIndex;
        float ringAngle;

        AsteroidBody(
                float x,
                float y,
                float radius,
                float velocityX,
                float velocityY,
                float angle,
                float spin,
                int type,
                int hp,
                int ringIndex) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.angle = angle;
            this.spin = spin;
            this.type = type;
            this.hp = hp;
            this.ringIndex = ringIndex;
            ringAngle = -angle;
        }
    }

    private static final class CoinDrop {
        final float x;
        float y;
        final float radius;
        final float velocityY;
        float angle;
        final float spin;

        CoinDrop(float x, float y, float radius, float velocityY, float angle, float spin) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.velocityY = velocityY;
            this.angle = angle;
            this.spin = spin;
        }
    }

    private static final class Spark {
        float x;
        float y;
        float velocityX;
        float velocityY;
        final float radius;
        float life;
        final float maxLife;
        final int color;

        Spark(
                float x,
                float y,
                float velocityX,
                float velocityY,
                float radius,
                float life,
                int color) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.radius = radius;
            this.life = life;
            this.maxLife = life;
            this.color = color;
        }
    }

    public interface ExtraTimeRequestListener {
        void onExtraTimeRequested();
    }
}

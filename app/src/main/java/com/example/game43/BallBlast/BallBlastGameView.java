package com.example.game43.BallBlast;

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

public class BallBlastGameView extends View {
    private static final int MAX_ACTIVE_ROCKS = 3;
    private static final int STARTING_ROCKS_PER_WAVE = 2;
    private static final int POWERED_ROCKS_PER_WAVE = 3;
    private static final int SPECIAL_WAVE_INTERVAL = 10;
    private static final int SPECIAL_ROCK_HP = 30;
    private static final float COIN_REWARD_MULTIPLIER = 0.25f;
    private static final int MAX_ROCK_HP = 100;
    private static final int HP_INCREASE_PER_POWERED_WAVE = 5;
    private static final float SHOT_INTERVAL_SECONDS = 0.115f;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawRect = new RectF();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Rock> rocks = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final List<CollectibleCoin> collectibleCoins = new ArrayList<>();
    private final List<HitParticle> hitParticles = new ArrayList<>();
    private final List<MuzzleFlash> muzzleFlashes = new ArrayList<>();
    private final Random random = new Random();

    private Bitmap backgroundBitmap;
    private Bitmap cloudBitmap;
    private Bitmap cannonBitmap;
    private Bitmap wheelBitmap;
    private Bitmap bulletBitmap;
    private Bitmap coinBitmap;
    private Bitmap powerUpBitmap;
    private Bitmap[] rockBitmaps;
    private Bitmap specialRockBitmap;
    private Bitmap[] specialRockDamageBitmaps;
    private Bitmap tutorialHandBitmap;
    private GameOverListener gameOverListener;

    private float viewWidth;
    private float viewHeight;
    private float minSide;
    private float groundY;
    private float cannonX;
    private float targetCannonX;
    private float cannonY;
    private float cannonWidth;
    private float cannonHeight;
    private float wheelRadius;
    private float wheelRotation;
    private float shootTimer;
    private float spawnTimer;
    private float cloudOffset;
    private float cameraShake;
    private float tutorialHandTime;
    private long lastFrameNanos;
    private int score;
    private int coins;
    private int waveNumber;
    private int poweredStartWave;
    private int bulletStreams = 1;
    private boolean draggingCannon;
    private boolean poweredUp;
    private boolean gameOver;
    private boolean waitingForPlayerStart = true;
    private boolean showFirstRunHand = true;

    public BallBlastGameView(Context context) {
        super(context);
        init();
    }

    public BallBlastGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BallBlastGameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setGameOverListener(GameOverListener gameOverListener) {
        this.gameOverListener = gameOverListener;
    }

    private void init() {
        setFocusable(true);
        bitmapPaint.setDither(true);
        bitmapPaint.setFilterBitmap(true);

        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg1_);
        cloudBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cloud);
        cannonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.daibac);
        wheelBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.banhxe);
        bulletBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dan);
        coinBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_coin_icon);
        powerUpBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ngoc);
        specialRockBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.nau);
        tutorialHandBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hand);
        specialRockDamageBitmaps = new Bitmap[] {
                BitmapFactory.decodeResource(getResources(), R.drawable.brokenrock1),
                BitmapFactory.decodeResource(getResources(), R.drawable.brokenrock2),
                BitmapFactory.decodeResource(getResources(), R.drawable.brockenrock3)
        };
        rockBitmaps = new Bitmap[] {
                BitmapFactory.decodeResource(getResources(), R.drawable.vang),
                BitmapFactory.decodeResource(getResources(), R.drawable.cam),
                BitmapFactory.decodeResource(getResources(), R.drawable.rock_do),
                BitmapFactory.decodeResource(getResources(), R.drawable.rockhong),
                BitmapFactory.decodeResource(getResources(), R.drawable.rocktim),
                BitmapFactory.decodeResource(getResources(), R.drawable.xanhlo),
                BitmapFactory.decodeResource(getResources(), R.drawable.ngoc),
                BitmapFactory.decodeResource(getResources(), R.drawable.nau),
                BitmapFactory.decodeResource(getResources(), R.drawable.xanh_bien)
        };

        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        textShadowPaint.setColor(Color.argb(150, 18, 25, 33));
        textShadowPaint.setTextAlign(Paint.Align.CENTER);
        textShadowPaint.setFakeBoldText(true);

        shadowPaint.setColor(Color.argb(75, 39, 55, 42));
        smokePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        minSide = Math.min(viewWidth, viewHeight);
        groundY = viewHeight - minSide * 0.15f - dp(60f);
        cannonWidth = minSide * 0.085f;
        cannonHeight = cannonWidth * 199f / 166f;
        wheelRadius = minSide * 0.02f;
        cannonX = viewWidth * 0.5f;
        targetCannonX = cannonX;
        cannonY = groundY - cannonHeight * 0.44f;
        resetGame();
    }

    public void resetGame() {
        bullets.clear();
        rocks.clear();
        powerUps.clear();
        collectibleCoins.clear();
        hitParticles.clear();
        muzzleFlashes.clear();
        shootTimer = 0f;
        spawnTimer = 0f;
        wheelRotation = 0f;
        cameraShake = 0f;
        tutorialHandTime = 0f;
        score = 0;
        coins = 0;
        waveNumber = 0;
        poweredStartWave = 0;
        bulletStreams = 1;
        poweredUp = false;
        gameOver = false;
        waitingForPlayerStart = true;
        draggingCannon = false;
        cannonX = viewWidth * 0.5f;
        targetCannonX = cannonX;
        lastFrameNanos = 0L;
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
        if (gameOver) {
            return;
        }
        updateCannon(dt);
        cloudOffset = (cloudOffset + minSide * 0.018f * dt) % Math.max(1f, viewWidth);
        if (waitingForPlayerStart) {
            tutorialHandTime += dt;
            updateHitParticles(dt);
            updateMuzzleFlashes(dt);
            cameraShake = Math.max(0f, cameraShake - dt * 5f);
            return;
        }
        updateBullets(dt);
        updateRocks(dt);
        updatePowerUps(dt);
        updateCollectibleCoins(dt);
        updateHitParticles(dt);
        updateMuzzleFlashes(dt);
        updateSpawning(dt);
        if (draggingCannon) {
            shootTimer -= dt;
            if (shootTimer <= 0f) {
                spawnBullet();
                shootTimer = SHOT_INTERVAL_SECONDS;
            }
        }
        cameraShake = Math.max(0f, cameraShake - dt * 5f);
    }

    private void updateCannon(float dt) {
        float previousX = cannonX;
        float moveSpeed = minSide * 6.8f * dt;
        cannonX = moveToward(cannonX, targetCannonX, moveSpeed);
        cannonX = clamp(cannonX, cannonWidth * 0.6f, viewWidth - cannonWidth * 0.6f);
        float dx = cannonX - previousX;
        if (Math.abs(dx) > 0.01f) {
            wheelRotation += dx / Math.max(1f, wheelRadius) * 57.29578f;
        }
    }

    private void updateBullets(float dt) {
        Iterator<Bullet> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();
            bullet.y -= minSide * 1.55f * dt;
            bullet.age += dt;
            if (bullet.y < -bullet.radius || bullet.age > 2.4f) {
                iterator.remove();
            }
        }
    }

    private void updateRocks(float dt) {
        for (int i = rocks.size() - 1; i >= 0; i--) {
            Rock rock = rocks.get(i);
            rock.vy += gravity() * dt;
            rock.x += rock.vx * dt;
            rock.y += rock.vy * dt;
            rock.rotation += rock.angularVelocity * dt;

            if (rock.x - rock.radius >= 0f && rock.x + rock.radius <= viewWidth) {
                rock.enteredScreen = true;
            }

            if (rock.enteredScreen) {
                if (rock.x - rock.radius < 0f) {
                    rock.x = rock.radius;
                    rock.vx = Math.abs(rock.vx) * 0.88f;
                } else if (rock.x + rock.radius > viewWidth) {
                    rock.x = viewWidth - rock.radius;
                    rock.vx = -Math.abs(rock.vx) * 0.88f;
                }
            }

            if (rock.y + rock.radius > groundY) {
                rock.y = groundY - rock.radius;
                spawnGroundSmoke(rock.x, groundY, rock.radius);
                rock.vy = -bounceSpeedForLevel(rock.level);
            }

            if (rock.y > viewHeight + rock.radius * 2f) {
                rocks.remove(i);
                continue;
            }

            if (isRockCollidingWithCannon(rock)) {
                triggerGameOver();
                return;
            }

            if (resolveBulletHits(rock)) {
                rocks.remove(i);
            }
        }
    }

    private boolean isRockCollidingWithCannon(Rock rock) {
        float left = cannonX - cannonWidth * 0.58f;
        float right = cannonX + cannonWidth * 0.58f;
        float top = cannonY - cannonHeight * 0.52f;
        float bottom = groundY + wheelRadius;
        float closestX = clamp(rock.x, left, right);
        float closestY = clamp(rock.y, top, bottom);
        float dx = rock.x - closestX;
        float dy = rock.y - closestY;
        float collisionRadius = rock.radius * 0.84f;
        return dx * dx + dy * dy <= collisionRadius * collisionRadius;
    }

    private void triggerGameOver() {
        if (gameOver) {
            return;
        }
        gameOver = true;
        draggingCannon = false;
        if (gameOverListener != null) {
            gameOverListener.onGameOver();
        }
    }

    private boolean resolveBulletHits(Rock rock) {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            float dx = bullet.x - rock.x;
            float dy = bullet.y - rock.y;
            float hitDistance = rock.radius + bullet.radius * 0.8f;
            if (dx * dx + dy * dy > hitDistance * hitDistance) {
                continue;
            }

            bullets.remove(i);
            rock.hp--;
            score++;
            cameraShake = Math.min(1f, cameraShake + 0.08f);
            spawnHitParticles(bullet.x, bullet.y, rock.color);
            updateSpecialRockDamageBitmap(rock);
            if (rock.hp <= 0) {
                spawnHitParticles(rock.x, rock.y, rock.color);
                if (rock.special) {
                    spawnPowerUp(rock.x, rock.y);
                } else {
                    spawnCollectibleCoins(rock.x, rock.y, rock.reward);
                }
                if (!rock.special && rock.level > 1) {
                    splitRock(rock);
                }
                return true;
            }
        }
        return false;
    }

    private void updateSpecialRockDamageBitmap(Rock rock) {
        if (!rock.special || specialRockDamageBitmaps == null) {
            return;
        }

        int hitCount = rock.hpMax - rock.hp;
        if (hitCount >= 25 && specialRockDamageBitmaps.length > 2 && specialRockDamageBitmaps[2] != null) {
            rock.bitmap = specialRockDamageBitmaps[2];
        } else if (hitCount >= 15 && specialRockDamageBitmaps.length > 1 && specialRockDamageBitmaps[1] != null) {
            rock.bitmap = specialRockDamageBitmaps[1];
        } else if (hitCount >= 5 && specialRockDamageBitmaps.length > 0 && specialRockDamageBitmaps[0] != null) {
            rock.bitmap = specialRockDamageBitmaps[0];
        }
    }

    private void updateHitParticles(float dt) {
        Iterator<HitParticle> iterator = hitParticles.iterator();
        while (iterator.hasNext()) {
            HitParticle particle = iterator.next();
            particle.age += dt;
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vy += minSide * 0.9f * dt;
            particle.radius *= 0.988f;
            if (particle.age >= particle.lifetime) {
                iterator.remove();
            }
        }
    }

    private void updateMuzzleFlashes(float dt) {
        Iterator<MuzzleFlash> iterator = muzzleFlashes.iterator();
        while (iterator.hasNext()) {
            MuzzleFlash flash = iterator.next();
            flash.age += dt;
            if (flash.age >= flash.lifetime) {
                iterator.remove();
            }
        }
    }

    private void updatePowerUps(float dt) {
        Iterator<PowerUp> iterator = powerUps.iterator();
        while (iterator.hasNext()) {
            PowerUp powerUp = iterator.next();
            powerUp.vy += gravity() * 0.42f * dt;
            powerUp.y += powerUp.vy * dt;
            powerUp.rotation += powerUp.angularVelocity * dt;

            if (isPowerUpCollected(powerUp)) {
                bulletStreams = Math.max(2, bulletStreams * 2);
                if (!poweredUp) {
                    poweredUp = true;
                    poweredStartWave = waveNumber;
                }
                spawnHitParticles(powerUp.x, powerUp.y, Color.rgb(64, 225, 190));
                iterator.remove();
                continue;
            }

            if (powerUp.y - powerUp.radius > viewHeight) {
                iterator.remove();
            }
        }
    }

    private boolean isPowerUpCollected(PowerUp powerUp) {
        float closestX = clamp(powerUp.x, cannonX - cannonWidth * 0.55f, cannonX + cannonWidth * 0.55f);
        float closestY = clamp(powerUp.y, cannonY - cannonHeight * 0.55f, groundY + wheelRadius);
        float dx = powerUp.x - closestX;
        float dy = powerUp.y - closestY;
        return dx * dx + dy * dy <= powerUp.radius * powerUp.radius;
    }

    private void updateCollectibleCoins(float dt) {
        Iterator<CollectibleCoin> iterator = collectibleCoins.iterator();
        while (iterator.hasNext()) {
            CollectibleCoin coin = iterator.next();
            if (coin.flyingToHud) {
                moveCoinToHud(coin, dt);
                if (distance(coin.x, coin.y, getHudCoinX(), getHudCoinY()) < minSide * 0.025f) {
                    coins += coin.value;
                    iterator.remove();
                }
                continue;
            }

            coin.vy += gravity() * 0.72f * dt;
            coin.x += coin.vx * dt;
            coin.y += coin.vy * dt;
            coin.rotation += coin.angularVelocity * dt;

            if (coin.x - coin.radius < 0f) {
                coin.x = coin.radius;
                coin.vx = Math.abs(coin.vx) * 0.55f;
            } else if (coin.x + coin.radius > viewWidth) {
                coin.x = viewWidth - coin.radius;
                coin.vx = -Math.abs(coin.vx) * 0.55f;
            }

            if (coin.y + coin.radius > groundY) {
                coin.y = groundY - coin.radius;
                if (!coin.onGround && Math.abs(coin.vy) > minSide * 0.22f) {
                    coin.vy = -Math.abs(coin.vy) * 0.18f;
                } else {
                    coin.vy = 0f;
                    coin.onGround = true;
                }
                coin.vx *= 0.95f;
            }

            if (coin.onGround) {
                coin.vy = 0f;
                coin.vx *= 0.985f;
                coin.angularVelocity = coin.vx / Math.max(1f, coin.radius) * 57.29578f;
                if (Math.abs(coin.vx) < minSide * 0.006f) {
                    coin.vx = 0f;
                    coin.angularVelocity = 0f;
                }
            }

            if (isCoinCollected(coin)) {
                coin.flyingToHud = true;
                coin.onGround = false;
            }
        }
    }

    private void moveCoinToHud(CollectibleCoin coin, float dt) {
        float targetX = getHudCoinX();
        float targetY = getHudCoinY();
        float dx = targetX - coin.x;
        float dy = targetY - coin.y;
        float t = Math.min(1f, dt * 8.5f);
        coin.x += dx * t;
        coin.y += dy * t;
        coin.rotation += 720f * dt;
        coin.radius = Math.max(minSide * 0.014f, coin.radius * 0.985f);
    }

    private boolean isCoinCollected(CollectibleCoin coin) {
        float closestX = clamp(coin.x, cannonX - cannonWidth * 0.65f, cannonX + cannonWidth * 0.65f);
        float closestY = clamp(coin.y, cannonY - cannonHeight * 0.55f, groundY + wheelRadius);
        float dx = coin.x - closestX;
        float dy = coin.y - closestY;
        return dx * dx + dy * dy <= coin.radius * coin.radius;
    }

    private void updateSpawning(float dt) {
        spawnTimer -= dt;
        if (spawnTimer > 0f || !rocks.isEmpty()) {
            return;
        }
        waveNumber++;
        boolean specialWave = waveNumber > SPECIAL_WAVE_INTERVAL
                && (waveNumber - SPECIAL_WAVE_INTERVAL - 1) % SPECIAL_WAVE_INTERVAL == 0;
        if (specialWave && rocks.size() < MAX_ACTIVE_ROCKS) {
            spawnMainRock(random.nextBoolean(), true);
        }
        int rocksPerWave = poweredUp ? POWERED_ROCKS_PER_WAVE : STARTING_ROCKS_PER_WAVE;
        for (int i = specialWave ? 1 : 0; i < rocksPerWave && rocks.size() < MAX_ACTIVE_ROCKS; i++) {
            spawnMainRock(i % 2 == 0, false);
        }
        spawnTimer = randomBetween(0.85f, 1.45f);
    }

    private void spawnMainRock(boolean fromLeft, boolean specialRock) {
        int level = specialRock ? 4 : random.nextFloat() < 0.45f ? 3 : random.nextFloat() < 0.75f ? 2 : 1;
        float radius = specialRock ? minSide * 0.12f : rockRadiusForLevel(level);
        Rock rock = new Rock();
        rock.level = level;
        rock.mainRock = true;
        rock.special = specialRock;
        rock.bitmap = specialRock ? specialRockBitmap : rockBitmaps[random.nextInt(rockBitmaps.length)];
        rock.radius = radius;
        rock.x = fromLeft ? -radius * 1.15f : viewWidth + radius * 1.15f;
        rock.y = randomBetween(-radius * 0.35f, viewHeight * 0.12f);
        rock.vx = (fromLeft ? 1f : -1f) * randomBetween(minSide * 0.34f, minSide * 0.52f);
        rock.vy = randomBetween(-minSide * 0.12f, minSide * 0.04f);
        rock.hp = specialRock ? SPECIAL_ROCK_HP : hpForLevel(level);
        rock.hpMax = rock.hp;
        rock.reward = specialRock ? 0 : level * 6;
        rock.color = specialRock ? Color.rgb(128, 91, 56) : colorForBitmap(rock.bitmap);
        rock.angularVelocity = randomBetween(-95f, 95f);
        rocks.add(rock);
    }

    private void splitRock(Rock source) {
        int availableSlots = Math.max(0, MAX_ACTIVE_ROCKS - (rocks.size() - 1));
        int splitCount = Math.min(2, availableSlots);
        for (int i = 0; i < splitCount; i++) {
            Rock child = new Rock();
            child.level = source.level - 1;
            child.mainRock = false;
            child.bitmap = source.bitmap;
            child.radius = rockRadiusForLevel(child.level);
            child.x = source.x + (i == 0 ? -child.radius * 0.25f : child.radius * 0.25f);
            child.y = source.y;
            child.vx = (i == 0 ? -1f : 1f) * randomBetween(minSide * 0.24f, minSide * 0.42f);
            child.vy = -randomBetween(minSide * 0.34f, minSide * 0.52f);
            child.hp = Math.max(1, source.hpMax / 2);
            child.hpMax = child.hp;
            child.reward = Math.max(2, source.reward / 2);
            child.color = source.color;
            child.angularVelocity = randomBetween(-150f, 150f);
            child.enteredScreen = true;
            rocks.add(child);
        }
    }

    private void spawnPowerUp(float x, float y) {
        PowerUp powerUp = new PowerUp();
        powerUp.x = x;
        powerUp.y = y;
        powerUp.vy = minSide * 0.12f;
        powerUp.radius = minSide * 0.038f;
        powerUp.angularVelocity = randomBetween(-120f, 120f);
        powerUps.add(powerUp);
    }

    private void spawnCollectibleCoins(float x, float y, int totalValue) {
        totalValue = Math.max(1, Math.round(totalValue * COIN_REWARD_MULTIPLIER));
        int coinCount = Math.min(8, Math.max(1, Math.min(totalValue, Math.max(3, totalValue / 2))));
        int remainingValue = totalValue;
        for (int i = 0; i < coinCount; i++) {
            CollectibleCoin coin = new CollectibleCoin();
            coin.value = Math.max(1, remainingValue / (coinCount - i));
            remainingValue -= coin.value;
            coin.x = x + randomBetween(-minSide * 0.018f, minSide * 0.018f);
            coin.y = y + randomBetween(-minSide * 0.012f, minSide * 0.012f);
            coin.vx = randomBetween(-minSide * 0.42f, minSide * 0.42f);
            coin.vy = -randomBetween(minSide * 0.36f, minSide * 0.72f);
            coin.radius = minSide * 0.022f;
            coin.rotation = randomBetween(0f, 360f);
            coin.angularVelocity = randomBetween(-420f, 420f);
            collectibleCoins.add(coin);
        }
    }

    private float rockRadiusForLevel(int level) {
        if (level >= 3) {
            return minSide * 0.08625f;
        }
        if (level == 2) {
            return minSide * 0.06375f;
        }
        return minSide * 0.0465f;
    }

    private float bounceSpeedForLevel(int level) {
        if (level >= 3) {
            return minSide * 1.62f;
        }
        if (level == 2) {
            return minSide * 1.45f;
        }
        return minSide * 1.29f;
    }

    private int hpForLevel(int level) {
        int baseHp = level == 3 ? randomBetweenInt(9, 16) : level == 2 ? randomBetweenInt(4, 8) : randomBetweenInt(1, 3);
        if (!poweredUp) {
            return baseHp;
        }

        int poweredWaveCount = Math.max(1, waveNumber - poweredStartWave);
        int scaledHp = baseHp + poweredWaveCount * HP_INCREASE_PER_POWERED_WAVE;
        return Math.min(MAX_ROCK_HP, scaledHp);
    }

    private int colorForBitmap(Bitmap bitmap) {
        if (bitmap == rockBitmaps[0]) {
            return Color.rgb(245, 205, 38);
        }
        if (bitmap == rockBitmaps[1]) {
            return Color.rgb(255, 146, 40);
        }
        if (bitmap == rockBitmaps[2]) {
            return Color.rgb(232, 80, 68);
        }
        if (bitmap == rockBitmaps[3]) {
            return Color.rgb(226, 75, 178);
        }
        if (bitmap == rockBitmaps[4]) {
            return Color.rgb(119, 75, 208);
        }
        if (bitmap == rockBitmaps[5]) {
            return Color.rgb(68, 214, 82);
        }
        if (bitmap == rockBitmaps[6]) {
            return Color.rgb(42, 212, 204);
        }
        return Color.rgb(128, 91, 56);
    }

    private void spawnBullet() {
        float spread = minSide * 0.025f;
        float startOffset = -(bulletStreams - 1) * spread * 0.5f;
        float muzzleX = cannonX;
        float muzzleY = cannonY - cannonHeight * 0.48f;
        for (int i = 0; i < bulletStreams; i++) {
            Bullet bullet = new Bullet();
            bullet.x = cannonX + startOffset + i * spread;
            bullet.y = muzzleY;
            bullet.radius = minSide * 0.012f;
            bullets.add(bullet);
        }
        spawnMuzzleFlash(muzzleX, muzzleY);
    }

    private void spawnHitParticles(float x, float y, int color) {
        for (int i = 0; i < 8; i++) {
            HitParticle particle = new HitParticle();
            particle.x = x;
            particle.y = y;
            float angle = randomBetween(0f, (float) Math.PI * 2f);
            float speed = randomBetween(minSide * 0.08f, minSide * 0.36f);
            particle.vx = (float) Math.cos(angle) * speed;
            particle.vy = (float) Math.sin(angle) * speed;
            particle.radius = randomBetween(minSide * 0.006f, minSide * 0.014f);
            particle.color = color;
            particle.lifetime = randomBetween(0.22f, 0.45f);
            hitParticles.add(particle);
        }
    }

    private void spawnMuzzleFlash(float x, float y) {
        MuzzleFlash flash = new MuzzleFlash();
        flash.x = x;
        flash.y = y;
        flash.radius = minSide * 0.034f;
        flash.lifetime = 0.075f;
        muzzleFlashes.add(flash);

        for (int i = 0; i < 5; i++) {
            HitParticle particle = new HitParticle();
            particle.x = x + randomBetween(-minSide * 0.009f, minSide * 0.009f);
            particle.y = y + randomBetween(-minSide * 0.012f, minSide * 0.006f);
            particle.vx = randomBetween(-minSide * 0.08f, minSide * 0.08f);
            particle.vy = -randomBetween(minSide * 0.12f, minSide * 0.32f);
            particle.radius = randomBetween(minSide * 0.005f, minSide * 0.01f);
            particle.color = Color.rgb(255, 214, 82);
            particle.lifetime = randomBetween(0.12f, 0.22f);
            hitParticles.add(particle);
        }
    }

    private void spawnGroundSmoke(float x, float y, float rockRadius) {
        int count = rockRadius > minSide * 0.08f ? 14 : 9;
        for (int i = 0; i < count; i++) {
            HitParticle particle = new HitParticle();
            particle.x = x + randomBetween(-rockRadius * 0.45f, rockRadius * 0.45f);
            particle.y = y - randomBetween(0f, rockRadius * 0.18f);
            particle.vx = randomBetween(-minSide * 0.26f, minSide * 0.26f);
            particle.vy = -randomBetween(minSide * 0.08f, minSide * 0.22f);
            particle.radius = randomBetween(minSide * 0.012f, minSide * 0.03f);
            particle.color = Color.rgb(138, 130, 112);
            particle.lifetime = randomBetween(0.32f, 0.58f);
            hitParticles.add(particle);
        }
    }

    private void drawGame(Canvas canvas) {
        float shakeX = cameraShake <= 0f ? 0f : randomBetween(-minSide * 0.005f, minSide * 0.005f);
        float shakeY = cameraShake <= 0f ? 0f : randomBetween(-minSide * 0.005f, minSide * 0.005f);
        canvas.save();
        canvas.translate(shakeX, shakeY);
        drawBackground(canvas);
        drawBullets(canvas);
        drawRocks(canvas);
        drawPowerUps(canvas);
        drawHitParticles(canvas);
        drawCollectibleCoins(canvas);
        drawCannon(canvas);
        drawTutorialHand(canvas);
        drawMuzzleFlashes(canvas);
        drawHud(canvas);
        canvas.restore();
    }

    private void drawTutorialHand(Canvas canvas) {
        if (!waitingForPlayerStart || !showFirstRunHand || tutorialHandBitmap == null) {
            return;
        }

        float travel = minSide * 0.16f;
        float x = cannonX + (float) Math.sin(tutorialHandTime * 2.6f) * travel;
        float size = minSide * 0.11f;
        float y = groundY + wheelRadius * 2.35f;
        drawRect.set(x - size * 0.5f, y - size * 0.5f, x + size * 0.5f, y + size * 0.5f);
        canvas.drawBitmap(tutorialHandBitmap, null, drawRect, bitmapPaint);
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundBitmap == null) {
            canvas.drawColor(Color.rgb(200, 222, 248));
            return;
        }
        float scale = Math.max(viewWidth / backgroundBitmap.getWidth(), viewHeight / backgroundBitmap.getHeight());
        float width = backgroundBitmap.getWidth() * scale;
        float height = backgroundBitmap.getHeight() * scale;
        drawRect.set((viewWidth - width) * 0.5f, (viewHeight - height) * 0.5f,
                (viewWidth + width) * 0.5f, (viewHeight + height) * 0.5f);
        canvas.drawBitmap(backgroundBitmap, null, drawRect, bitmapPaint);
        drawClouds(canvas);
    }

    private void drawClouds(Canvas canvas) {
        if (cloudBitmap == null) {
            return;
        }

        float cloudWidth = minSide * 0.32f;
        float cloudHeight = cloudWidth * cloudBitmap.getHeight() / cloudBitmap.getWidth();
        for (int i = 0; i < 4; i++) {
            float x = positiveModulo(i * viewWidth * 0.36f - cloudOffset, viewWidth + cloudWidth * 2f) - cloudWidth;
            float y = viewHeight * (0.015f + i * 0.035f);
            drawRect.set(x, y, x + cloudWidth, y + cloudHeight);
            canvas.drawBitmap(cloudBitmap, null, drawRect, bitmapPaint);
        }
    }

    private void drawBullets(Canvas canvas) {
        for (Bullet bullet : bullets) {
            float width = bullet.radius * 2f;
            float height = width * 42f / 34f;
            drawRect.set(bullet.x - width * 0.5f, bullet.y - height * 0.5f,
                    bullet.x + width * 0.5f, bullet.y + height * 0.5f);
            if (bulletBitmap != null) {
                canvas.drawBitmap(bulletBitmap, null, drawRect, bitmapPaint);
            } else {
                canvas.drawOval(drawRect, bitmapPaint);
            }
        }
    }

    private void drawRocks(Canvas canvas) {
        for (Rock rock : rocks) {
            drawRect.set(rock.x - rock.radius, rock.y - rock.radius, rock.x + rock.radius, rock.y + rock.radius);
            canvas.save();
            canvas.rotate(rock.rotation, rock.x, rock.y);
            canvas.drawBitmap(rock.bitmap, null, drawRect, bitmapPaint);
            canvas.restore();

            float textSize = Math.max(dp(18f), rock.radius * 0.62f);
            textPaint.setTextSize(textSize);
            textShadowPaint.setTextSize(textSize);
            String hpText = rock.special ? "?" : String.valueOf(rock.hp);
            float textY = rock.y - (textPaint.ascent() + textPaint.descent()) * 0.5f;
            canvas.drawText(hpText, rock.x + dp(2f), textY + dp(2f), textShadowPaint);
            canvas.drawText(hpText, rock.x, textY, textPaint);
        }
    }

    private void drawPowerUps(Canvas canvas) {
        for (PowerUp powerUp : powerUps) {
            drawRect.set(powerUp.x - powerUp.radius, powerUp.y - powerUp.radius,
                    powerUp.x + powerUp.radius, powerUp.y + powerUp.radius);
            canvas.save();
            canvas.rotate(powerUp.rotation, powerUp.x, powerUp.y);
            if (powerUpBitmap != null) {
                canvas.drawBitmap(powerUpBitmap, null, drawRect, bitmapPaint);
            } else {
                smokePaint.setColor(Color.rgb(64, 225, 190));
                canvas.drawOval(drawRect, smokePaint);
            }
            canvas.restore();

            textPaint.setTextSize(powerUp.radius * 0.72f);
            textShadowPaint.setTextSize(powerUp.radius * 0.72f);
            float textY = powerUp.y - (textPaint.ascent() + textPaint.descent()) * 0.5f;
            canvas.drawText("x2", powerUp.x + dp(1.2f), textY + dp(1.2f), textShadowPaint);
            canvas.drawText("x2", powerUp.x, textY, textPaint);
        }
    }

    private void drawHitParticles(Canvas canvas) {
        for (HitParticle particle : hitParticles) {
            float progress = Math.min(1f, particle.age / particle.lifetime);
            smokePaint.setColor(applyAlpha(particle.color, (int) (220f * (1f - progress))));
            canvas.drawCircle(particle.x, particle.y, particle.radius, smokePaint);
        }
    }

    private void drawCollectibleCoins(Canvas canvas) {
        for (CollectibleCoin coin : collectibleCoins) {
            drawRect.set(coin.x - coin.radius, coin.y - coin.radius,
                    coin.x + coin.radius, coin.y + coin.radius);
            canvas.save();
            canvas.rotate(coin.rotation, coin.x, coin.y);
            if (coinBitmap != null) {
                canvas.drawBitmap(coinBitmap, null, drawRect, bitmapPaint);
            } else {
                smokePaint.setColor(Color.rgb(245, 190, 38));
                canvas.drawOval(drawRect, smokePaint);
            }
            canvas.restore();
        }
    }

    private void drawMuzzleFlashes(Canvas canvas) {
        for (MuzzleFlash flash : muzzleFlashes) {
            float progress = Math.min(1f, flash.age / flash.lifetime);
            float radius = flash.radius * (1f + progress * 0.55f);

            smokePaint.setColor(Color.argb((int) (220f * (1f - progress)), 255, 224, 92));
            drawRect.set(flash.x - radius * 0.55f, flash.y - radius,
                    flash.x + radius * 0.55f, flash.y + radius * 0.18f);
            canvas.drawOval(drawRect, smokePaint);

            smokePaint.setColor(Color.argb((int) (185f * (1f - progress)), 255, 122, 42));
            drawRect.set(flash.x - radius * 0.32f, flash.y - radius * 0.64f,
                    flash.x + radius * 0.32f, flash.y + radius * 0.08f);
            canvas.drawOval(drawRect, smokePaint);
        }
    }

    private void drawCannon(Canvas canvas) {
        drawRect.set(cannonX - cannonWidth * 0.42f, groundY - wheelRadius * 0.34f,
                cannonX + cannonWidth * 0.42f, groundY + wheelRadius * 0.34f);
        canvas.drawOval(drawRect, shadowPaint);

        float bodyLeft = cannonX - cannonWidth * 0.5f;
        float bodyTop = cannonY - cannonHeight * 0.5f;
        float bodyRight = cannonX + cannonWidth * 0.5f;
        float bodyBottom = cannonY + cannonHeight * 0.5f;
        drawRect.set(bodyLeft, bodyTop, bodyRight, bodyBottom);
        if (cannonBitmap != null) {
            canvas.drawBitmap(cannonBitmap, null, drawRect, bitmapPaint);
        }

        drawWheel(canvas, cannonX - cannonWidth * 0.34f, groundY - wheelRadius * 0.45f);
        drawWheel(canvas, cannonX + cannonWidth * 0.34f, groundY - wheelRadius * 0.45f);
    }

    private void drawWheel(Canvas canvas, float x, float y) {
        drawRect.set(x - wheelRadius, y - wheelRadius, x + wheelRadius, y + wheelRadius);
        canvas.save();
        canvas.rotate(wheelRotation, x, y);
        if (wheelBitmap != null) {
            canvas.drawBitmap(wheelBitmap, null, drawRect, bitmapPaint);
        } else {
            canvas.drawOval(drawRect, shadowPaint);
        }
        canvas.restore();
    }

    private void drawHud(Canvas canvas) {
        float panelHeight = minSide * 0.078f;
        float panelWidth = minSide * 0.25f;
        float top = dp(18f);
        float right = viewWidth - dp(14f);
        drawRect.set(right - panelWidth, top, right, top + panelHeight);
        smokePaint.setColor(Color.argb(145, 142, 150, 159));
        canvas.drawRoundRect(drawRect, panelHeight * 0.22f, panelHeight * 0.22f, smokePaint);

        float coinSize = panelHeight * 0.72f;
        RectF coinRect = new RectF(drawRect.left + panelHeight * 0.16f, drawRect.centerY() - coinSize * 0.5f,
                drawRect.left + panelHeight * 0.16f + coinSize, drawRect.centerY() + coinSize * 0.5f);
        if (coinBitmap != null) {
            canvas.drawBitmap(coinBitmap, null, coinRect, bitmapPaint);
        }

        textPaint.setTextSize(panelHeight * 0.43f);
        textShadowPaint.setTextSize(panelHeight * 0.43f);
        String coinText = String.valueOf(coins);
        float textX = drawRect.right - panelHeight * 0.25f;
        float textY = drawRect.centerY() - (textPaint.ascent() + textPaint.descent()) * 0.5f;
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textShadowPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(coinText, textX + dp(1.3f), textY + dp(1.3f), textShadowPaint);
        canvas.drawText(coinText, textX, textY, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textShadowPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) {
            draggingCannon = false;
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isTouchOnCannon(event.getX(), event.getY())) {
                    draggingCannon = true;
                    targetCannonX = event.getX();
                    shootTimer = 0f;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (draggingCannon) {
                    if (waitingForPlayerStart && Math.abs(event.getX() - cannonX) > dp(4f)) {
                        startGameFromPlayerMove();
                    }
                    targetCannonX = event.getX();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                draggingCannon = false;
                return true;
            default:
                return true;
        }
    }

    private void startGameFromPlayerMove() {
        waitingForPlayerStart = false;
        showFirstRunHand = false;
        shootTimer = 0f;
        spawnTimer = 0.18f;
    }

    private boolean isTouchOnCannon(float x, float y) {
        return Math.abs(x - cannonX) <= cannonWidth * 0.85f
                && y >= cannonY - cannonHeight * 0.6f
                && y <= groundY + wheelRadius * 1.4f;
    }

    private float gravity() {
        return minSide * 1.45f;
    }

    private float moveToward(float current, float target, float amount) {
        if (Math.abs(target - current) <= amount) {
            return target;
        }
        return current + Math.signum(target - current) * amount;
    }

    private int applyAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float randomBetween(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private float getHudCoinX() {
        float panelHeight = minSide * 0.078f;
        float panelWidth = minSide * 0.25f;
        float right = viewWidth - dp(14f);
        float left = right - panelWidth;
        float coinSize = panelHeight * 0.72f;
        return left + panelHeight * 0.16f + coinSize * 0.5f;
    }

    private float getHudCoinY() {
        float panelHeight = minSide * 0.078f;
        return dp(18f) + panelHeight * 0.5f;
    }

    private int randomBetweenInt(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float positiveModulo(float value, float modulo) {
        float result = value % modulo;
        return result < 0f ? result + modulo : result;
    }

    private float distance(float ax, float ay, float bx, float by) {
        return (float) Math.hypot(ax - bx, ay - by);
    }

    public interface GameOverListener {
        void onGameOver();
    }

    private static final class Bullet {
        float x;
        float y;
        float radius;
        float age;
    }

    private static final class Rock {
        Bitmap bitmap;
        float x;
        float y;
        float vx;
        float vy;
        float radius;
        float rotation;
        float angularVelocity;
        int hp;
        int hpMax;
        int level;
        int reward;
        int color;
        boolean mainRock;
        boolean special;
        boolean enteredScreen;
    }

    private static final class HitParticle {
        float x;
        float y;
        float vx;
        float vy;
        float radius;
        float age;
        float lifetime;
        int color;
    }

    private static final class MuzzleFlash {
        float x;
        float y;
        float radius;
        float age;
        float lifetime;
    }

    private static final class CollectibleCoin {
        float x;
        float y;
        float vx;
        float vy;
        float radius;
        float rotation;
        float angularVelocity;
        int value;
        boolean onGround;
        boolean flyingToHud;
    }

    private static final class PowerUp {
        float x;
        float y;
        float vy;
        float radius;
        float rotation;
        float angularVelocity;
    }
}

package com.example.game43.SupertBallFall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
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

public class SuperBallFallGameView extends View {
    private static final float BALL_SPEED = 590f;
    private static final float BALL_GRAVITY = 360f;
    private static final float WALL_BOUNCE_DAMPING = 0.82f;
    private static final float TARGET_BOUNCE_DAMPING = 0.78f;
    private static final float MIN_BALL_SPEED = 230f;
    private static final int MAX_TARGETS_PER_WAVE = 3;
    private static final float TRAIL_SPAWN_INTERVAL_SECONDS = 0.018f;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF drawRect = new RectF();
    private final List<TargetCircle> targets = new ArrayList<>();
    private final List<SparkParticle> particles = new ArrayList<>();
    private final List<Shockwave> shockwaves = new ArrayList<>();
    private final List<BallTrail> ballTrails = new ArrayList<>();
    private final Random random = new Random();

    private Bitmap[] targetBitmaps;
    private Bitmap coinBitmap;
    private GameOverListener gameOverListener;

    private float viewWidth;
    private float viewHeight;
    private float minSide;
    private float shooterX;
    private float shooterY;
    private float ballX;
    private float ballY;
    private float ballVx;
    private float ballVy;
    private float ballRadius;
    private float targetRadius;
    private float rowStep;
    private float aimDx;
    private float aimDy = 1f;
    private float trailTimer;
    private long lastFrameNanos;
    private int wave;
    private int totalDamage;
    private int coins;
    private boolean aiming;
    private boolean ballMoving;
    private boolean gameOver;

    public SuperBallFallGameView(Context context) {
        super(context);
        init();
    }

    public SuperBallFallGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SuperBallFallGameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setGameOverListener(GameOverListener gameOverListener) {
        this.gameOverListener = gameOverListener;
    }

    public void resetGame() {
        targets.clear();
        particles.clear();
        shockwaves.clear();
        ballTrails.clear();
        wave = 0;
        totalDamage = 0;
        coins = 0;
        aiming = false;
        ballMoving = false;
        gameOver = false;
        ballX = shooterX;
        ballY = shooterY;
        ballVx = 0f;
        ballVy = 0f;
        trailTimer = 0f;
        aimDx = 0f;
        aimDy = 1f;
        lastFrameNanos = 0L;
        spawnNextWave();
    }

    private void init() {
        setFocusable(true);
        targetBitmaps = new Bitmap[] {
                BitmapFactory.decodeResource(getResources(), R.drawable.super_ball_1),
                BitmapFactory.decodeResource(getResources(), R.drawable.super_ball_2),
                BitmapFactory.decodeResource(getResources(), R.drawable.super_ball_3),
                BitmapFactory.decodeResource(getResources(), R.drawable.super_ball_4),
                BitmapFactory.decodeResource(getResources(), R.drawable.super_ball_5),
                BitmapFactory.decodeResource(getResources(), R.drawable.super_ball_6),
                BitmapFactory.decodeResource(getResources(), R.drawable.super_ball_7)
        };
        coinBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_coin_icon);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.2f));
        strokePaint.setColor(Color.rgb(216, 216, 216));
        strokePaint.setPathEffect(new DashPathEffect(new float[] {dp(7f), dp(7f)}, 0f));

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        minSide = Math.min(w, h);
        shooterX = viewWidth * 0.5f;
        shooterY = viewHeight * 0.115f;
        ballRadius = minSide * 0.047f;
        targetRadius = minSide * 0.056f;
        rowStep = targetRadius * 2.55f;
        resetGame();
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
        updateBall(dt);
        updateParticles(dt);
        updateShockwaves(dt);
        updateBallTrails(dt);
    }

    private void updateBall(float dt) {
        if (!ballMoving) {
            ballX = shooterX;
            ballY = shooterY;
            return;
        }

        ballVy += scaledBallGravity() * dt;
        ballX += ballVx * dt;
        ballY += ballVy * dt;
        spawnBallTrail(dt);

        if (ballX - ballRadius < 0f) {
            ballX = ballRadius;
            ballVx = Math.abs(ballVx) * WALL_BOUNCE_DAMPING;
            ballVy *= WALL_BOUNCE_DAMPING;
        } else if (ballX + ballRadius > viewWidth) {
            ballX = viewWidth - ballRadius;
            ballVx = -Math.abs(ballVx) * WALL_BOUNCE_DAMPING;
            ballVy *= WALL_BOUNCE_DAMPING;
        }

        resolveTargetHits();
        if (ballY - ballRadius > viewHeight) {
            finishShot();
        }
    }

    private void resolveTargetHits() {
        for (int i = targets.size() - 1; i >= 0; i--) {
            TargetCircle target = targets.get(i);
            float dx = ballX - target.x;
            float dy = ballY - target.y;
            float hitDistance = ballRadius + target.radius;
            if (dx * dx + dy * dy > hitDistance * hitDistance) {
                continue;
            }

            float length = Math.max(1f, (float) Math.hypot(dx, dy));
            float nx = dx / length;
            float ny = dy / length;
            float dot = ballVx * nx + ballVy * ny;
            ballVx -= 2f * dot * nx;
            ballVy -= 2f * dot * ny;
            dampBallVelocity(TARGET_BOUNCE_DAMPING);

            ballX = target.x + nx * (hitDistance + dp(1f));
            ballY = target.y + ny * (hitDistance + dp(1f));
            target.hp--;
            totalDamage++;

            if (target.hp <= 0) {
                coins++;
                spawnExplosionEffect(target.x, target.y, target.radius, target.color);
                targets.remove(i);
            } else {
                spawnImpactEffect(target.x, target.y, target.radius, target.color);
            }
            return;
        }
    }

    private void dampBallVelocity(float damping) {
        float length = Math.max(1f, (float) Math.hypot(ballVx, ballVy));
        float minSpeed = scaledMinBallSpeed();
        if (length < minSpeed) {
            ballVx = ballVx / length * minSpeed;
            ballVy = ballVy / length * minSpeed;
            return;
        }
        ballVx *= damping;
        ballVy *= damping;
    }

    private void finishShot() {
        ballMoving = false;
        ballTrails.clear();
        ballX = shooterX;
        ballY = shooterY;
        moveTargetsUp();
        if (isDangerReached()) {
            triggerGameOver();
            return;
        }
        spawnNextWave();
        if (isDangerReached()) {
            triggerGameOver();
        }
    }

    private void moveTargetsUp() {
        for (TargetCircle target : targets) {
            target.y -= rowStep;
        }
    }

    private boolean isDangerReached() {
        float dangerY = shooterY + ballRadius * 1.55f;
        for (TargetCircle target : targets) {
            if (target.y - target.radius <= dangerY) {
                return true;
            }
        }
        return false;
    }

    private void triggerGameOver() {
        if (gameOver) {
            return;
        }
        gameOver = true;
        aiming = false;
        ballMoving = false;
        if (gameOverListener != null) {
            gameOverListener.onGameOver();
        }
    }

    private void spawnNextWave() {
        wave++;
        int count = Math.min(MAX_TARGETS_PER_WAVE, 1 + wave / 2 + random.nextInt(2));
        List<Float> rowXs = new ArrayList<>();
        float y = viewHeight - targetRadius * 1.55f;

        for (int i = 0; i < count; i++) {
            TargetCircle target = new TargetCircle();
            target.x = randomTargetX(rowXs);
            rowXs.add(target.x);
            target.y = y;
            target.radius = targetRadius;
            target.hp = Math.max(1, wave + randomBetweenInt(0, 3));
            target.bitmap = targetBitmaps[random.nextInt(targetBitmaps.length)];
            target.color = colorForBitmap(target.bitmap);
            targets.add(target);
        }
    }

    private float randomTargetX(List<Float> rowXs) {
        float minX = targetRadius * 1.35f;
        float maxX = viewWidth - targetRadius * 1.35f;
        float minSpacing = targetRadius * 2.8f;
        for (int attempt = 0; attempt < 40; attempt++) {
            float x = randomBetween(minX, maxX);
            boolean farEnough = true;
            for (float usedX : rowXs) {
                if (Math.abs(x - usedX) < minSpacing) {
                    farEnough = false;
                    break;
                }
            }
            if (farEnough) {
                return x;
            }
        }

        float fallbackGap = (maxX - minX) / MAX_TARGETS_PER_WAVE;
        return minX + fallbackGap * (rowXs.size() + 0.5f);
    }

    private int colorForBitmap(Bitmap bitmap) {
        if (bitmap == targetBitmaps[0] || bitmap == targetBitmaps[1]) {
            return Color.rgb(250, 210, 35);
        }
        if (bitmap == targetBitmaps[2]) {
            return Color.rgb(255, 165, 38);
        }
        if (bitmap == targetBitmaps[3]) {
            return Color.rgb(82, 210, 88);
        }
        if (bitmap == targetBitmaps[4]) {
            return Color.rgb(45, 180, 225);
        }
        if (bitmap == targetBitmaps[5]) {
            return Color.rgb(155, 88, 225);
        }
        return Color.rgb(235, 40, 40);
    }

    private void updateParticles(float dt) {
        Iterator<SparkParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            SparkParticle particle = iterator.next();
            particle.age += dt;
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vy += minSide * 0.24f * dt;
            if (particle.age >= particle.life) {
                iterator.remove();
            }
        }
    }

    private void updateShockwaves(float dt) {
        Iterator<Shockwave> iterator = shockwaves.iterator();
        while (iterator.hasNext()) {
            Shockwave shockwave = iterator.next();
            shockwave.age += dt;
            if (shockwave.age >= shockwave.life) {
                iterator.remove();
            }
        }
    }

    private void updateBallTrails(float dt) {
        Iterator<BallTrail> iterator = ballTrails.iterator();
        while (iterator.hasNext()) {
            BallTrail trail = iterator.next();
            trail.age += dt;
            if (trail.age >= trail.life) {
                iterator.remove();
            }
        }
    }

    private void spawnBallTrail(float dt) {
        trailTimer -= dt;
        if (trailTimer > 0f) {
            return;
        }

        BallTrail trail = new BallTrail();
        trail.x = ballX;
        trail.y = ballY;
        trail.radius = ballRadius;
        trail.life = 0.18f;
        ballTrails.add(trail);
        if (ballTrails.size() > 16) {
            ballTrails.remove(0);
        }
        trailTimer = TRAIL_SPAWN_INTERVAL_SECONDS;
    }

    private void spawnImpactEffect(float x, float y, float radius, int color) {
        spawnBurstParticles(x, y, radius, color, 8, 0.28f, 0.7f);
        spawnShockwave(x, y, radius * 0.45f, radius * 1.25f, color, 0.18f);
    }

    private void spawnExplosionEffect(float x, float y, float radius, int color) {
        spawnBurstParticles(x, y, radius, Color.rgb(255, 169, 38), 14, 0.42f, 1.15f);
        spawnBurstParticles(x, y, radius, color, 16, 0.34f, 1.0f);
        spawnShockwave(x, y, radius * 0.55f, radius * 2.05f, Color.rgb(255, 178, 44), 0.28f);
        spawnShockwave(x, y, radius * 0.25f, radius * 1.45f, Color.WHITE, 0.16f);
    }

    private void spawnBurstParticles(float x, float y, float radius, int color, int count,
                                     float lifeScale, float speedScale) {
        for (int i = 0; i < count; i++) {
            SparkParticle particle = new SparkParticle();
            particle.x = x + randomBetween(-radius * 0.22f, radius * 0.22f);
            particle.y = y + randomBetween(-radius * 0.22f, radius * 0.22f);
            float angle = randomBetween(0f, (float) Math.PI * 2f);
            float speed = randomBetween(minSide * 0.18f, minSide * 0.7f) * speedScale;
            particle.vx = (float) Math.cos(angle) * speed;
            particle.vy = (float) Math.sin(angle) * speed;
            particle.radius = randomBetween(radius * 0.08f, radius * 0.18f);
            particle.color = color;
            particle.life = randomBetween(0.2f, 0.42f) * lifeScale / 0.34f;
            particles.add(particle);
        }
    }

    private void spawnShockwave(float x, float y, float startRadius, float endRadius, int color, float life) {
        Shockwave shockwave = new Shockwave();
        shockwave.x = x;
        shockwave.y = y;
        shockwave.startRadius = startRadius;
        shockwave.endRadius = endRadius;
        shockwave.color = color;
        shockwave.life = life;
        shockwaves.add(shockwave);
    }

    private void drawGame(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        drawBounds(canvas);
        drawHud(canvas);
        drawAim(canvas);
        drawTargets(canvas);
        drawShockwaves(canvas);
        drawParticles(canvas);
        drawBallTrails(canvas);
        drawBlackBall(canvas);
    }

    private void drawBounds(Canvas canvas) {
        canvas.drawLine(dp(6f), 0f, dp(6f), viewHeight, strokePaint);
        canvas.drawLine(viewWidth - dp(6f), 0f, viewWidth - dp(6f), viewHeight, strokePaint);

        fillPaint.setColor(Color.argb(28, 20, 20, 20));
        canvas.drawCircle(shooterX, shooterY, ballRadius * 1.9f, fillPaint);
    }

    private void drawHud(Canvas canvas) {
        textPaint.setColor(Color.rgb(50, 50, 50));
        textPaint.setTextSize(minSide * 0.055f);
        canvas.drawText(String.valueOf(totalDamage), shooterX - minSide * 0.16f, shooterY - ballRadius * 0.72f, textPaint);

        textPaint.setColor(Color.rgb(152, 152, 152));
        textPaint.setTextSize(minSide * 0.021f);
        canvas.drawText("TOTAL DAMAGE", shooterX, shooterY + ballRadius * 1.42f, textPaint);

        textPaint.setColor(Color.rgb(255, 166, 34));
        textPaint.setTextSize(minSide * 0.048f);
        canvas.drawText(String.valueOf(wave), shooterX, shooterY + ballRadius * 2.18f, textPaint);

        drawCoinHolder(canvas);
    }

    private void drawCoinHolder(Canvas canvas) {
        float panelHeight = minSide * 0.066f;
        float panelWidth = minSide * 0.23f;
        float right = viewWidth - dp(14f);
        float top = dp(16f);
        drawRect.set(right - panelWidth, top, right, top + panelHeight);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.argb(150, 142, 150, 159));
        canvas.drawRoundRect(drawRect, panelHeight * 0.22f, panelHeight * 0.22f, fillPaint);

        float coinSize = panelHeight * 0.72f;
        RectF coinRect = new RectF(drawRect.left + panelHeight * 0.16f,
                drawRect.centerY() - coinSize * 0.5f,
                drawRect.left + panelHeight * 0.16f + coinSize,
                drawRect.centerY() + coinSize * 0.5f);
        if (coinBitmap != null) {
            canvas.drawBitmap(coinBitmap, null, coinRect, bitmapPaint);
        } else {
            fillPaint.setColor(Color.rgb(245, 190, 38));
            canvas.drawOval(coinRect, fillPaint);
        }

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(panelHeight * 0.45f);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        float textX = drawRect.right - panelHeight * 0.22f;
        float textY = drawRect.centerY() - (textPaint.ascent() + textPaint.descent()) * 0.5f;
        canvas.drawText(String.valueOf(coins), textX, textY, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void drawAim(Canvas canvas) {
        if (!aiming || ballMoving) {
            return;
        }

        float length = viewHeight * 0.34f;
        fillPaint.setColor(Color.rgb(82, 82, 82));
        float spacing = ballRadius * 1.18f;
        int dotCount = Math.max(5, (int) (length / spacing));
        for (int i = 1; i <= dotCount; i++) {
            float dotX = shooterX + aimDx * spacing * i;
            float dotY = shooterY + aimDy * spacing * i;
            if (dotY > viewHeight - dp(12f)) {
                break;
            }
            canvas.drawCircle(dotX, dotY, dp(3.3f), fillPaint);
        }
    }

    private void drawTargets(Canvas canvas) {
        for (TargetCircle target : targets) {
            drawRect.set(target.x - target.radius, target.y - target.radius,
                    target.x + target.radius, target.y + target.radius);
            if (target.bitmap != null) {
                canvas.drawBitmap(target.bitmap, null, drawRect, bitmapPaint);
            } else {
                fillPaint.setColor(target.color);
                canvas.drawCircle(target.x, target.y, target.radius, fillPaint);
            }

            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(target.radius * 0.72f);
            String hpText = String.valueOf(target.hp);
            float textY = target.y - (textPaint.ascent() + textPaint.descent()) * 0.5f;
            canvas.drawText(hpText, target.x, textY, textPaint);
        }
    }

    private void drawParticles(Canvas canvas) {
        for (SparkParticle particle : particles) {
            float progress = Math.min(1f, particle.age / particle.life);
            fillPaint.setColor(applyAlpha(particle.color, (int) (210f * (1f - progress))));
            canvas.drawCircle(particle.x, particle.y, particle.radius * (1f - progress * 0.35f), fillPaint);
        }
    }

    private void drawShockwaves(Canvas canvas) {
        Paint.Style previousStyle = fillPaint.getStyle();
        fillPaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStrokeWidth(dp(2.2f));
        for (Shockwave shockwave : shockwaves) {
            float progress = Math.min(1f, shockwave.age / shockwave.life);
            float radius = shockwave.startRadius + (shockwave.endRadius - shockwave.startRadius) * progress;
            fillPaint.setColor(applyAlpha(shockwave.color, (int) (190f * (1f - progress))));
            canvas.drawCircle(shockwave.x, shockwave.y, radius, fillPaint);
        }
        fillPaint.setStyle(previousStyle);
    }

    private void drawBallTrails(Canvas canvas) {
        for (BallTrail trail : ballTrails) {
            float progress = Math.min(1f, trail.age / trail.life);
            int alpha = (int) (42f * (1f - progress));
            fillPaint.setColor(Color.argb(alpha, 20, 20, 20));
            canvas.drawCircle(trail.x, trail.y, trail.radius * (1f - progress * 0.18f), fillPaint);
        }
    }

    private void drawBlackBall(Canvas canvas) {
        fillPaint.setColor(Color.rgb(15, 15, 15));
        canvas.drawCircle(ballX, ballY, ballRadius, fillPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver || ballMoving) {
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                updateAim(event.getX(), event.getY());
                aiming = true;
                return true;
            case MotionEvent.ACTION_UP:
                updateAim(event.getX(), event.getY());
                shootBall();
                aiming = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                aiming = false;
                return true;
            default:
                return true;
        }
    }

    private void updateAim(float touchX, float touchY) {
        float dx = touchX - shooterX;
        float dy = touchY - shooterY;
        dy = Math.max(ballRadius * 1.4f, dy);
        float length = Math.max(1f, (float) Math.hypot(dx, dy));
        aimDx = clamp(dx / length, -0.86f, 0.86f);
        aimDy = Math.max(0.28f, dy / length);
        float normalized = Math.max(1f, (float) Math.hypot(aimDx, aimDy));
        aimDx /= normalized;
        aimDy /= normalized;
    }

    private void shootBall() {
        float speed = scaledBallSpeed();
        ballX = shooterX;
        ballY = shooterY;
        ballVx = aimDx * speed;
        ballVy = aimDy * speed;
        ballMoving = true;
    }

    private float scaledBallSpeed() {
        return minSide * BALL_SPEED / 360f;
    }

    private float scaledMinBallSpeed() {
        return minSide * MIN_BALL_SPEED / 360f;
    }

    private float scaledBallGravity() {
        return minSide * BALL_GRAVITY / 360f;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
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

    private int applyAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public interface GameOverListener {
        void onGameOver();
    }

    private static final class TargetCircle {
        float x;
        float y;
        float radius;
        Bitmap bitmap;
        int hp;
        int color;
    }

    private static final class SparkParticle {
        float x;
        float y;
        float vx;
        float vy;
        float radius;
        float age;
        float life;
        int color;
    }

    private static final class Shockwave {
        float x;
        float y;
        float startRadius;
        float endRadius;
        float age;
        float life;
        int color;
    }

    private static final class BallTrail {
        float x;
        float y;
        float radius;
        float age;
        float life;
    }
}

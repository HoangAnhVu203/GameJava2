package com.example.game43;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DunkALotGameView extends View {
    private static final int STATE_READY = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_GAME_OVER = 2;
    private static final float FIXED_STEP = 1f / 120f;
    private static final int MAX_STEPS_PER_FRAME = 6;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawRect = new RectF();
    private final List<Hoop> hoops = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random(43L);
    private final SharedPreferences preferences;

    private final Bitmap ballBitmap;
    private final Bitmap blueBackBitmap;
    private final Bitmap blueFrontBitmap;
    private final Bitmap orangeBackBitmap;
    private final Bitmap orangeFrontBitmap;
    private final Bitmap redVerticalBitmap;
    private final Bitmap redHorizontalBitmap;
    private final Bitmap redSquareBitmap;
    private final Bitmap purpleBarBitmap;

    private float density;
    private float ballX;
    private float ballY;
    private float ballVx;
    private float ballVy;
    private float ballRotation;
    private float ballRadius;
    private float cameraBottom;
    private float cameraTarget;
    private float accumulator;
    private float scorePulse;
    private float bonusTimer;
    private long lastFrameNanos;
    private long lastTapNanos;
    private int state = STATE_READY;
    private int score;
    private int bestScore;
    private int targetHoop;
    private int nextDirection = -1;
    private int lastBonus = 1;
    private boolean frameLoopRunning;

    public DunkALotGameView(Context context) {
        this(context, null);
    }

    public DunkALotGameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        preferences = context.getSharedPreferences("dunk_a_lot", Context.MODE_PRIVATE);
        bestScore = preferences.getInt("best_score", 0);

        ballBitmap = decode(R.drawable.dunk_ball);
        blueBackBitmap = decode(R.drawable.dunk_hoop_blue_back);
        blueFrontBitmap = decode(R.drawable.dunk_hoop_blue_front);
        orangeBackBitmap = decode(R.drawable.dunk_hoop_orange_back);
        orangeFrontBitmap = decode(R.drawable.dunk_hoop_orange_front);
        redVerticalBitmap = decode(R.drawable.docdo);
        redHorizontalBitmap = decode(R.drawable.nganghong1);
        redSquareBitmap = decode(R.drawable.khoi_vuong);
        purpleBarBitmap = decode(R.drawable.khoinagngtim);

        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        setFocusable(true);
        setClickable(true);
    }

    private Bitmap decode(int drawableId) {
        return BitmapFactory.decodeResource(getResources(), drawableId);
    }

    private float dp(float value) {
        return value * density;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        ballRadius = Math.min(dp(18f), width * 0.047f);
        buildLevel();
        resetGame();
    }

    private void buildLevel() {
        hoops.clear();
        obstacles.clear();
        if (getWidth() == 0) {
            return;
        }

        float interiorLeft = dp(28f);
        float interiorRight = getWidth() - dp(28f);
        float spacing = Math.max(dp(390f), getHeight() * 0.70f);
        float firstHoopY = dp(74f);
        float hoopWidth = Math.min(dp(108f), getWidth() * 0.28f);
        float[] hoopPositions = {0.24f, 0.72f, 0.34f, 0.68f, 0.27f, 0.57f, 0.76f, 0.39f};

        for (int i = 0; i < 48; i++) {
            float centerX = interiorLeft + (interiorRight - interiorLeft) * hoopPositions[i % hoopPositions.length];
            centerX = clamp(centerX, interiorLeft + hoopWidth * 0.5f, interiorRight - hoopWidth * 0.5f);
            float hoopY = firstHoopY + i * spacing;
            hoops.add(new Hoop(centerX, hoopY, hoopWidth, i % 3 == 2));
            addSectionObstacles(i, hoopY, spacing, interiorLeft, interiorRight);
        }
    }

    private void addSectionObstacles(int section, float baseY, float spacing,
                                     float left, float right) {
        float areaWidth = right - left;
        float y = baseY + spacing * 0.48f;
        switch (section % 6) {
            case 0:
                obstacles.add(Obstacle.hazard(right - dp(48f), y, dp(48f), dp(92f),
                        Obstacle.DRAW_RED_VERTICAL, false));
                break;
            case 1:
                obstacles.add(Obstacle.solid(left, y - dp(18f), areaWidth * 0.34f, dp(25f)));
                obstacles.add(Obstacle.hazard(right - dp(40f), y + dp(70f), dp(40f), dp(78f),
                        Obstacle.DRAW_RED_VERTICAL, false));
                break;
            case 2:
                obstacles.add(Obstacle.hazard(left, y - dp(36f), dp(72f), dp(126f),
                        Obstacle.DRAW_RED_SQUARE, false));
                obstacles.add(Obstacle.solid(right - areaWidth * 0.32f, y + dp(96f),
                        areaWidth * 0.32f, dp(24f)));
                break;
            case 3:
                obstacles.add(Obstacle.hazard(right - dp(82f), y - dp(28f), dp(82f), dp(106f),
                        Obstacle.DRAW_RED_SQUARE, true));
                obstacles.add(Obstacle.solid(left, y + dp(116f), areaWidth * 0.30f, dp(24f)));
                break;
            case 4:
                obstacles.add(Obstacle.hazard(left, y, dp(38f), dp(76f),
                        Obstacle.DRAW_RED_VERTICAL, true));
                obstacles.add(Obstacle.hazard(right - dp(38f), y + dp(112f), dp(38f), dp(76f),
                        Obstacle.DRAW_RED_VERTICAL, false));
                obstacles.add(Obstacle.solid(left + areaWidth * 0.36f, y + dp(48f),
                        areaWidth * 0.29f, dp(22f)));
                break;
            default:
                float gap = dp(118f);
                float sideWidth = (areaWidth - gap) * 0.5f;
                obstacles.add(Obstacle.hazard(left, y + dp(48f), sideWidth, dp(13f),
                        Obstacle.DRAW_RED_HORIZONTAL, false));
                obstacles.add(Obstacle.hazard(right - sideWidth, y + dp(48f), sideWidth, dp(13f),
                        Obstacle.DRAW_RED_HORIZONTAL, false));
                break;
        }
    }

    private void resetGame() {
        if (getWidth() == 0 || getHeight() == 0 || hoops.isEmpty()) {
            return;
        }
        state = STATE_READY;
        score = 0;
        targetHoop = 0;
        nextDirection = -1;
        ballX = getWidth() * 0.5f;
        ballY = dp(285f);
        ballVx = 0f;
        ballVy = 0f;
        ballRotation = 0f;
        cameraBottom = 0f;
        cameraTarget = 0f;
        accumulator = 0f;
        scorePulse = 0f;
        bonusTimer = 0f;
        particles.clear();
        for (Hoop hoop : hoops) {
            hoop.scored = false;
        }
        lastFrameNanos = 0L;
        startFrameLoop();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startFrameLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        frameLoopRunning = false;
        super.onDetachedFromWindow();
    }

    public void resumeGame() {
        lastFrameNanos = 0L;
        startFrameLoop();
    }

    public void pauseGame() {
        frameLoopRunning = false;
    }

    private void startFrameLoop() {
        if (!frameLoopRunning) {
            frameLoopRunning = true;
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
        }
        float frameSeconds = Math.min((now - lastFrameNanos) / 1_000_000_000f, 0.05f);
        lastFrameNanos = now;

        if (state == STATE_PLAYING) {
            accumulator += frameSeconds;
            int steps = 0;
            while (accumulator >= FIXED_STEP && steps < MAX_STEPS_PER_FRAME) {
                updatePhysics(FIXED_STEP);
                accumulator -= FIXED_STEP;
                steps++;
            }
            if (steps == MAX_STEPS_PER_FRAME) {
                accumulator = 0f;
            }
        }
        updateVisualEffects(frameSeconds);
        drawWorld(canvas);

        boolean hasAnimation = state == STATE_PLAYING || scorePulse > 0f
                || bonusTimer > 0f || !particles.isEmpty();
        if (frameLoopRunning && hasAnimation) {
            postInvalidateOnAnimation();
        } else {
            frameLoopRunning = false;
        }
    }

    private void updatePhysics(float dt) {
        float previousY = ballY;
        ballVy -= dp(900f) * dt;
        ballX += ballVx * dt;
        ballY += ballVy * dt;
        ballRotation += (ballVx / Math.max(ballRadius, 1f)) * dt * 30f;

        float leftWall = dp(28f) + ballRadius;
        float rightWall = getWidth() - dp(28f) - ballRadius;
        if (ballX < leftWall) {
            ballX = leftWall;
            ballVx = Math.abs(ballVx) * 0.82f;
        } else if (ballX > rightWall) {
            ballX = rightWall;
            ballVx = -Math.abs(ballVx) * 0.82f;
        }

        for (Obstacle obstacle : obstacles) {
            if (!isNearViewport(obstacle.bottom, obstacle.bottom + obstacle.height, dp(160f))) {
                continue;
            }
            if (circleIntersectsRect(ballX, ballY, ballRadius, obstacle)) {
                if (obstacle.lethal) {
                    endGame();
                    return;
                }
                resolveSolidRect(obstacle);
            }
        }

        for (Hoop hoop : hoops) {
            if (!isNearViewport(hoop.y - dp(30f), hoop.y + dp(30f), dp(120f))) {
                continue;
            }
            resolveRimCollision(hoop, hoop.x - hoop.width * 0.39f, hoop.y + dp(1f));
            resolveRimCollision(hoop, hoop.x + hoop.width * 0.39f, hoop.y + dp(1f));
        }

        if (targetHoop < hoops.size()) {
            Hoop target = hoops.get(targetHoop);
            boolean crossedDown = previousY > target.y + dp(2f)
                    && ballY <= target.y + dp(2f) && ballVy < 0f;
            float opening = target.width * 0.34f;
            if (crossedDown && Math.abs(ballX - target.x) < opening) {
                scoreHoop(target);
            }
        }

        cameraTarget = Math.max(cameraTarget, ballY - getHeight() * 0.62f);
        cameraBottom += (cameraTarget - cameraBottom) * Math.min(1f, dt * 4.2f);

        if (ballY < cameraBottom - dp(85f)) {
            endGame();
        }
    }

    private boolean isNearViewport(float bottom, float top, float margin) {
        float viewBottom = cameraBottom - margin;
        float viewTop = cameraBottom + getHeight() + margin;
        return top >= viewBottom && bottom <= viewTop;
    }

    private boolean circleIntersectsRect(float cx, float cy, float radius, Obstacle obstacle) {
        float nearestX = clamp(cx, obstacle.left, obstacle.left + obstacle.width);
        float nearestY = clamp(cy, obstacle.bottom, obstacle.bottom + obstacle.height);
        float dx = cx - nearestX;
        float dy = cy - nearestY;
        return dx * dx + dy * dy < radius * radius;
    }

    private void resolveSolidRect(Obstacle obstacle) {
        float nearestX = clamp(ballX, obstacle.left, obstacle.left + obstacle.width);
        float nearestY = clamp(ballY, obstacle.bottom, obstacle.bottom + obstacle.height);
        float dx = ballX - nearestX;
        float dy = ballY - nearestY;
        float distanceSquared = dx * dx + dy * dy;

        if (distanceSquared < 0.0001f) {
            float toLeft = Math.abs(ballX - obstacle.left);
            float toRight = Math.abs(obstacle.left + obstacle.width - ballX);
            float toBottom = Math.abs(ballY - obstacle.bottom);
            float toTop = Math.abs(obstacle.bottom + obstacle.height - ballY);
            float minimum = Math.min(Math.min(toLeft, toRight), Math.min(toBottom, toTop));
            if (minimum == toLeft) {
                dx = -1f;
                dy = 0f;
            } else if (minimum == toRight) {
                dx = 1f;
                dy = 0f;
            } else if (minimum == toBottom) {
                dx = 0f;
                dy = -1f;
            } else {
                dx = 0f;
                dy = 1f;
            }
            distanceSquared = 1f;
        }

        float distance = (float) Math.sqrt(distanceSquared);
        float nx = dx / distance;
        float ny = dy / distance;
        float penetration = ballRadius - distance;
        ballX += nx * (penetration + dp(0.5f));
        ballY += ny * (penetration + dp(0.5f));
        float normalVelocity = ballVx * nx + ballVy * ny;
        if (normalVelocity < 0f) {
            float impulse = -(1f + 0.72f) * normalVelocity;
            ballVx = (ballVx + impulse * nx) * 0.96f;
            ballVy = (ballVy + impulse * ny) * 0.96f;
        }
    }

    private void resolveRimCollision(Hoop hoop, float rimX, float rimY) {
        float rimRadius = dp(5.5f);
        float dx = ballX - rimX;
        float dy = ballY - rimY;
        float minimumDistance = ballRadius + rimRadius;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared >= minimumDistance * minimumDistance || distanceSquared < 0.0001f) {
            return;
        }
        float distance = (float) Math.sqrt(distanceSquared);
        float nx = dx / distance;
        float ny = dy / distance;
        float penetration = minimumDistance - distance;
        ballX += nx * penetration;
        ballY += ny * penetration;
        float normalVelocity = ballVx * nx + ballVy * ny;
        if (normalVelocity < 0f) {
            float impulse = -(1f + 0.78f) * normalVelocity;
            ballVx += impulse * nx;
            ballVy += impulse * ny;
        }
    }

    private void scoreHoop(Hoop hoop) {
        hoop.scored = true;
        float centerError = Math.abs(ballX - hoop.x);
        lastBonus = centerError < dp(9f) ? 2 : 1;
        score += lastBonus;
        targetHoop++;
        scorePulse = 1f;
        bonusTimer = 0.85f;
        ballVy = Math.max(ballVy, dp(480f));
        spawnScoreParticles(hoop.x, hoop.y);
        if (score > bestScore) {
            bestScore = score;
            preferences.edit().putInt("best_score", bestScore).apply();
        }
    }

    private void spawnScoreParticles(float x, float y) {
        int cyan = Color.rgb(32, 198, 232);
        int pink = Color.rgb(255, 44, 119);
        for (int i = 0; i < 18; i++) {
            float angle = (float) (Math.PI * 2.0 * i / 18.0);
            float speed = dp(95f + random.nextFloat() * 115f);
            particles.add(new Particle(x, y, (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed, i % 3 == 0 ? pink : cyan));
        }
    }

    private void updateVisualEffects(float dt) {
        scorePulse = Math.max(0f, scorePulse - dt * 3.5f);
        bonusTimer = Math.max(0f, bonusTimer - dt);
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            particle.life -= dt;
            if (particle.life <= 0f) {
                particles.remove(i);
                continue;
            }
            particle.vy -= dp(230f) * dt;
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
        }
    }

    private void endGame() {
        if (state == STATE_GAME_OVER) {
            return;
        }
        state = STATE_GAME_OVER;
        ballVx = 0f;
        ballVy = 0f;
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
    }

    private void drawWorld(Canvas canvas) {
        canvas.drawColor(Color.rgb(103, 52, 126));
        drawBackground(canvas);
        drawObstacles(canvas);
        drawHoopBackLayers(canvas);
        drawParticles(canvas);
        drawBall(canvas);
        drawHoopFrontLayers(canvas);
        drawHud(canvas);
    }

    private void drawBackground(Canvas canvas) {
        float wallWidth = dp(21f);
        fillPaint.setColor(Color.rgb(55, 30, 77));
        canvas.drawRect(dp(7f), 0f, dp(7f) + wallWidth, getHeight(), fillPaint);
        canvas.drawRect(getWidth() - dp(7f) - wallWidth, 0f,
                getWidth() - dp(7f), getHeight(), fillPaint);

        fillPaint.setColor(Color.rgb(87, 42, 109));
        float brickW = dp(54f);
        float brickH = dp(14f);
        float worldStart = cameraBottom - dp(80f);
        int firstRow = (int) Math.floor(worldStart / dp(135f));
        for (int row = firstRow; row < firstRow + 10; row++) {
            float worldY = row * dp(135f) + dp(72f);
            float screenY = worldToScreenY(worldY);
            float x = ((row * 83) % Math.max(1, (int) (getWidth() - brickW - dp(80f)))) + dp(40f);
            canvas.drawRect(x, screenY, x + brickW, screenY + brickH, fillPaint);
            canvas.drawRect(x + brickW * 0.42f, screenY + brickH + dp(2f),
                    x + brickW * 1.25f, screenY + brickH * 2f + dp(2f), fillPaint);
        }
    }

    private void drawObstacles(Canvas canvas) {
        for (Obstacle obstacle : obstacles) {
            if (!isNearViewport(obstacle.bottom, obstacle.bottom + obstacle.height, dp(40f))) {
                continue;
            }
            float screenBottom = worldToScreenY(obstacle.bottom);
            float screenTop = worldToScreenY(obstacle.bottom + obstacle.height);
            drawRect.set(obstacle.left, screenTop, obstacle.left + obstacle.width, screenBottom);
            Bitmap bitmap;
            if (!obstacle.lethal) {
                bitmap = purpleBarBitmap;
            } else if (obstacle.drawType == Obstacle.DRAW_RED_VERTICAL) {
                bitmap = redVerticalBitmap;
            } else if (obstacle.drawType == Obstacle.DRAW_RED_HORIZONTAL) {
                bitmap = redHorizontalBitmap;
            } else {
                bitmap = redSquareBitmap;
            }
            drawBitmap(canvas, bitmap, drawRect, obstacle.mirror);
        }
    }

    private void drawHoopBackLayers(Canvas canvas) {
        for (Hoop hoop : hoops) {
            if (!isNearViewport(hoop.y - dp(30f), hoop.y + dp(30f), dp(50f))) {
                continue;
            }
            Bitmap bitmap = hoop.orange ? orangeBackBitmap : blueBackBitmap;
            float centerY = worldToScreenY(hoop.y);
            drawRect.set(hoop.x - hoop.width * 0.5f, centerY - hoop.width * 0.17f,
                    hoop.x + hoop.width * 0.5f, centerY + hoop.width * 0.24f);
            bitmapPaint.setAlpha(hoop.scored ? 150 : 255);
            canvas.drawBitmap(bitmap, null, drawRect, bitmapPaint);
        }
        bitmapPaint.setAlpha(255);
    }

    private void drawHoopFrontLayers(Canvas canvas) {
        for (Hoop hoop : hoops) {
            if (!isNearViewport(hoop.y - dp(30f), hoop.y + dp(30f), dp(50f))) {
                continue;
            }
            Bitmap bitmap = hoop.orange ? orangeFrontBitmap : blueFrontBitmap;
            float centerY = worldToScreenY(hoop.y);
            drawRect.set(hoop.x - hoop.width * 0.5f, centerY + hoop.width * 0.015f,
                    hoop.x + hoop.width * 0.5f, centerY + hoop.width * 0.27f);
            bitmapPaint.setAlpha(hoop.scored ? 150 : 255);
            canvas.drawBitmap(bitmap, null, drawRect, bitmapPaint);
        }
        bitmapPaint.setAlpha(255);
    }

    private void drawBall(Canvas canvas) {
        float screenY = worldToScreenY(ballY);
        canvas.save();
        canvas.rotate(ballRotation, ballX, screenY);
        drawRect.set(ballX - ballRadius, screenY - ballRadius,
                ballX + ballRadius, screenY + ballRadius);
        canvas.drawBitmap(ballBitmap, null, drawRect, bitmapPaint);
        canvas.restore();
    }

    private void drawParticles(Canvas canvas) {
        for (Particle particle : particles) {
            int alpha = (int) (255f * Math.min(1f, particle.life / 0.7f));
            fillPaint.setColor(particle.color);
            fillPaint.setAlpha(alpha);
            canvas.drawCircle(particle.x, worldToScreenY(particle.y), dp(3.2f), fillPaint);
        }
        fillPaint.setAlpha(255);
    }

    private void drawHud(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        float scoreSize = dp(48f) * (1f + scorePulse * 0.13f);
        textPaint.setTextSize(scoreSize);
        canvas.drawText(Integer.toString(score), getWidth() * 0.5f,
                dp(70f), textPaint);

        if (bonusTimer > 0f) {
            textPaint.setTextSize(dp(24f));
            canvas.drawText("+" + lastBonus, getWidth() * 0.5f,
                    dp(104f) - (0.85f - bonusTimer) * dp(18f), textPaint);
        }

        if (state == STATE_READY) {
            shadePaint.setColor(0x44000000);
            canvas.drawRect(0f, 0f, getWidth(), getHeight(), shadePaint);
            textPaint.setTextSize(dp(28f));
            canvas.drawText("DUNK A LOT", getWidth() * 0.5f, getHeight() * 0.36f, textPaint);
            textPaint.setTextSize(dp(17f));
            textPaint.setColor(0xFFEDE4F3);
            canvas.drawText("Chạm để nảy sang trái", getWidth() * 0.5f,
                    getHeight() * 0.43f, textPaint);
            textPaint.setTextSize(dp(14f));
            canvas.drawText("Lần chạm tiếp theo sẽ đổi sang phải", getWidth() * 0.5f,
                    getHeight() * 0.47f, textPaint);
            if (bestScore > 0) {
                canvas.drawText("Kỷ lục: " + bestScore, getWidth() * 0.5f,
                        getHeight() * 0.53f, textPaint);
            }
        } else if (state == STATE_GAME_OVER) {
            shadePaint.setColor(0x990F0818);
            canvas.drawRect(0f, 0f, getWidth(), getHeight(), shadePaint);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(dp(31f));
            canvas.drawText("THUA RỒI", getWidth() * 0.5f, getHeight() * 0.40f, textPaint);
            textPaint.setTextSize(dp(18f));
            canvas.drawText("Điểm: " + score + "   Kỷ lục: " + bestScore,
                    getWidth() * 0.5f, getHeight() * 0.46f, textPaint);
            fillPaint.setColor(Color.rgb(255, 45, 118));
            float buttonHalfWidth = Math.min(dp(105f), getWidth() * 0.32f);
            drawRect.set(getWidth() * 0.5f - buttonHalfWidth, getHeight() * 0.52f,
                    getWidth() * 0.5f + buttonHalfWidth, getHeight() * 0.60f);
            canvas.drawRoundRect(drawRect, dp(7f), dp(7f), fillPaint);
            textPaint.setTextSize(dp(18f));
            canvas.drawText("CHƠI LẠI", getWidth() * 0.5f,
                    getHeight() * 0.575f, textPaint);
        }
    }

    private void drawBitmap(Canvas canvas, Bitmap bitmap, RectF destination, boolean mirror) {
        if (!mirror) {
            canvas.drawBitmap(bitmap, null, destination, bitmapPaint);
            return;
        }
        canvas.save();
        canvas.scale(-1f, 1f, destination.centerX(), destination.centerY());
        canvas.drawBitmap(bitmap, null, destination, bitmapPaint);
        canvas.restore();
    }

    private float worldToScreenY(float worldY) {
        return getHeight() - dp(34f) - (worldY - cameraBottom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        long now = System.nanoTime();
        if (now - lastTapNanos < 55_000_000L) {
            return true;
        }
        lastTapNanos = now;
        performClick();

        if (state == STATE_GAME_OVER) {
            resetGame();
            return true;
        }
        if (state == STATE_READY) {
            state = STATE_PLAYING;
            lastFrameNanos = now;
            startFrameLoop();
        }
        ballVx = nextDirection * dp(185f);
        ballVy = dp(540f);
        nextDirection *= -1;
        performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class Hoop {
        final float x;
        final float y;
        final float width;
        final boolean orange;
        boolean scored;

        Hoop(float x, float y, float width, boolean orange) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.orange = orange;
        }
    }

    private static final class Obstacle {
        static final int DRAW_RED_VERTICAL = 1;
        static final int DRAW_RED_HORIZONTAL = 2;
        static final int DRAW_RED_SQUARE = 3;

        final float left;
        final float bottom;
        final float width;
        final float height;
        final boolean lethal;
        final int drawType;
        final boolean mirror;

        private Obstacle(float left, float bottom, float width, float height,
                         boolean lethal, int drawType, boolean mirror) {
            this.left = left;
            this.bottom = bottom;
            this.width = width;
            this.height = height;
            this.lethal = lethal;
            this.drawType = drawType;
            this.mirror = mirror;
        }

        static Obstacle hazard(float left, float bottom, float width, float height,
                               int drawType, boolean mirror) {
            return new Obstacle(left, bottom, width, height, true, drawType, mirror);
        }

        static Obstacle solid(float left, float bottom, float width, float height) {
            return new Obstacle(left, bottom, width, height, false, 0, false);
        }
    }

    private static final class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float life = 0.72f;
        final int color;

        Particle(float x, float y, float vx, float vy, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
        }
    }
}

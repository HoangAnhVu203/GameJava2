package com.example.game43.DunkALot;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.game43.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DunkALotGameView extends View {
    private static final int STATE_READY = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_GAME_OVER = 2;
    private static final float FIXED_STEP = 1f / 120f;
    private static final int MAX_STEPS_PER_FRAME = 6;
    private static final float GRAVITY_DP = 780f;
    private static final float TERMINAL_VELOCITY_DP = 700f;
    private static final float TAP_VELOCITY_X_DP = 188f;
    private static final float TAP_VELOCITY_Y_DP = -475f;
    private static final float HORIZONTAL_DRAG_PER_SECOND = 0.12f;
    private static final float MAX_HORIZONTAL_SPEED_DP = 330f;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawRect = new RectF();
    private final RectF exitButtonRect = new RectF();
    private final RectF coinBoxRect = new RectF();
    private final Path drawPath = new Path();
    private final List<Hoop> hoops = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<SpinnerObstacle> spinners = new ArrayList<>();
    private final List<TriangleHazard> triangles = new ArrayList<>();
    private final List<DotDecor> dots = new ArrayList<>();
    private final List<ChainBead> chainBeads = new ArrayList<>();
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
    private final Bitmap coinBitmap;

    private float density;
    private float ballX;
    private float ballY;
    private float ballVx;
    private float ballVy;
    private float ballRotation;
    private float ballRadius;
    private float playLeft;
    private float playRight;
    private float cameraTop;
    private float cameraTarget;
    private float accumulator;
    private float worldTime;
    private float scorePulse;
    private float bonusTimer;
    private long lastFrameNanos;
    private long lastTapNanos;
    private int state = STATE_READY;
    private int score;
    private int bestScore;
    private int coins;
    private int targetHoop;
    private int nextDirection = 1;
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
        coins = preferences.getInt("coins", 0);

        ballBitmap = decode(R.drawable.dunk_ball);
        blueBackBitmap = decode(R.drawable.dunk_hoop_blue_back);
        blueFrontBitmap = decode(R.drawable.dunk_hoop_blue_front);
        orangeBackBitmap = decode(R.drawable.dunk_hoop_orange_back);
        orangeFrontBitmap = decode(R.drawable.dunk_hoop_orange_front);
        redVerticalBitmap = decode(R.drawable.docdo);
        redHorizontalBitmap = decode(R.drawable.nganghong1);
        redSquareBitmap = decode(R.drawable.khoi_vuong);
        purpleBarBitmap = decode(R.drawable.khoinagngtim);
        coinBitmap = decode(R.drawable.tape_coin_icon);

        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        textShadowPaint.setTypeface(textPaint.getTypeface());
        textShadowPaint.setColor(0x884B3600);
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
        spinners.clear();
        triangles.clear();
        dots.clear();
        chainBeads.clear();
        if (getWidth() == 0) {
            return;
        }

        float maxPlayWidth = dp(390f);
        float minimumSide = dp(12f);
        float playWidth = Math.min(getWidth() - minimumSide * 2f, maxPlayWidth);
        playLeft = (getWidth() - playWidth) * 0.5f;
        playRight = playLeft + playWidth;

        float interiorLeft = playLeft + dp(21f);
        float interiorRight = playRight - dp(21f);
        float spacing = Math.max(dp(360f), getHeight() * 0.62f);
        float firstHoopY = Math.max(dp(300f), getHeight() * 0.58f);
        float hoopWidth = Math.min(dp(108f), playWidth * 0.29f);
        float[] hoopPositions = {0.24f, 0.72f, 0.34f, 0.68f, 0.27f, 0.57f, 0.76f, 0.39f};

        for (int i = 0; i < 72; i++) {
            float centerX = interiorLeft + (interiorRight - interiorLeft) * hoopPositions[i % hoopPositions.length];
            centerX = clamp(centerX, interiorLeft + hoopWidth * 0.5f, interiorRight - hoopWidth * 0.5f);
            float hoopY = firstHoopY + i * spacing;
            hoops.add(new Hoop(centerX, hoopY, hoopWidth, i % 3 == 2));
            obstacles.add(Obstacle.gate(interiorLeft, hoopY + dp(150f),
                    interiorRight - interiorLeft, dp(13f), i));
            addSectionObstacles(i, hoopY, spacing, interiorLeft, interiorRight);
            addHoopDecor(i, centerX, hoopY, hoopWidth);
        }
    }

    private void addSectionObstacles(int section, float baseY, float spacing,
                                     float left, float right) {
        float areaWidth = right - left;
        float y = baseY + spacing * 0.48f;
        switch (section % 12) {
            case 0:
                obstacles.add(Obstacle.hazard(right - dp(48f), y, dp(48f), dp(92f),
                        Obstacle.DRAW_RED_VERTICAL, false));
                addDots(left, right, y - dp(70f), Color.rgb(28, 183, 236), 5, 0.22f);
                break;
            case 1:
                obstacles.add(Obstacle.solid(left, y - dp(18f), areaWidth * 0.34f, dp(25f)));
                obstacles.add(Obstacle.hazard(right - dp(40f), y + dp(70f), dp(40f), dp(78f),
                        Obstacle.DRAW_RED_VERTICAL, false));
                addDots(left, right, y - dp(30f), Color.rgb(255, 126, 29), 7, 0.10f);
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
                spinners.add(new SpinnerObstacle(left + areaWidth * 0.55f, y + dp(52f),
                        dp(66f), 0.8f, section % 2 == 0));
                addDots(left, right, y - dp(70f), Color.rgb(255, 126, 29), 8, 0.35f);
                break;
            case 4:
                obstacles.add(Obstacle.hazard(left, y, dp(38f), dp(76f),
                        Obstacle.DRAW_RED_VERTICAL, true));
                obstacles.add(Obstacle.hazard(right - dp(38f), y + dp(112f), dp(38f), dp(76f),
                        Obstacle.DRAW_RED_VERTICAL, false));
                obstacles.add(Obstacle.solid(left + areaWidth * 0.36f, y + dp(48f),
                        areaWidth * 0.29f, dp(22f)));
                addDots(left, right, y - dp(56f), Color.rgb(28, 183, 236), 9, 0.48f);
                break;
            case 5:
                float gap = dp(118f);
                float sideWidth = (areaWidth - gap) * 0.5f;
                obstacles.add(Obstacle.hazard(left, y + dp(48f), sideWidth, dp(13f),
                        Obstacle.DRAW_RED_HORIZONTAL, false));
                obstacles.add(Obstacle.hazard(right - sideWidth, y + dp(48f), sideWidth, dp(13f),
                        Obstacle.DRAW_RED_HORIZONTAL, false));
                break;
            case 6:
                triangles.add(TriangleHazard.left(left, y - dp(34f), dp(112f), dp(122f)));
                triangles.add(TriangleHazard.right(right, y + dp(78f), dp(118f), dp(135f)));
                obstacles.add(Obstacle.hazard(left, y + dp(198f), areaWidth, dp(13f),
                        Obstacle.DRAW_RED_HORIZONTAL, false));
                break;
            case 7:
                spinners.add(new SpinnerObstacle(left + areaWidth * 0.48f, y + dp(26f),
                        dp(76f), 1.15f, true));
                obstacles.add(Obstacle.hazard(right - dp(58f), y - dp(126f), dp(58f), dp(74f),
                        Obstacle.DRAW_RED_VERTICAL, false));
                break;
            case 8:
                obstacles.add(Obstacle.hazard(left, y - dp(10f), dp(42f), dp(16f),
                        Obstacle.DRAW_RED_HORIZONTAL, false));
                obstacles.add(Obstacle.hazard(right - dp(80f), y - dp(10f), dp(80f), dp(16f),
                        Obstacle.DRAW_RED_HORIZONTAL, false));
                addDots(left, right, y - dp(100f), Color.rgb(28, 183, 236), 6, 0.28f);
                break;
            case 9:
                triangles.add(TriangleHazard.right(right, y - dp(74f), dp(120f), dp(170f)));
                obstacles.add(Obstacle.hazard(left, y + dp(112f), areaWidth * 0.36f, dp(13f),
                        Obstacle.DRAW_RED_HORIZONTAL, false));
                obstacles.add(Obstacle.solid(left + areaWidth * 0.42f, y + dp(42f),
                        areaWidth * 0.25f, dp(24f)));
                break;
            case 10:
                spinners.add(new SpinnerObstacle(left + areaWidth * 0.57f, y,
                        dp(62f), -0.95f, false));
                obstacles.add(Obstacle.hazard(left, y + dp(170f), areaWidth, dp(13f),
                        Obstacle.DRAW_RED_HORIZONTAL, false));
                addDots(left, right, y - dp(86f), Color.rgb(255, 126, 29), 8, 0.18f);
                break;
            default:
                obstacles.add(Obstacle.hazard(left, y - dp(12f), dp(56f), dp(96f),
                        Obstacle.DRAW_RED_VERTICAL, true));
                obstacles.add(Obstacle.hazard(right - dp(56f), y + dp(120f), dp(56f), dp(96f),
                        Obstacle.DRAW_RED_VERTICAL, false));
                triangles.add(TriangleHazard.left(left, y + dp(185f), dp(96f), dp(130f)));
                break;
        }
    }

    private void addHoopDecor(int section, float centerX, float hoopY, float hoopWidth) {
        if (section % 4 != 0 && section % 4 != 1) {
            return;
        }
        int firstColor = section % 8 == 0 ? Color.rgb(245, 245, 245) : Color.rgb(255, 48, 119);
        int secondColor = Color.rgb(245, 245, 245);
        float leftAnchorX = clamp(centerX - hoopWidth * 1.55f, playLeft + dp(20f), playRight - dp(20f));
        float rightAnchorX = clamp(centerX + hoopWidth * 1.55f, playLeft + dp(20f), playRight - dp(20f));
        float leftRimX = centerX - hoopWidth * 0.43f;
        float rightRimX = centerX + hoopWidth * 0.43f;
        float anchorY = hoopY - dp(38f);
        float rimY = hoopY + dp(8f);
        addChain(section * 2, leftAnchorX, anchorY, leftRimX, rimY,
                secondColor, secondColor);
        addChain(section * 2 + 1, rightRimX, rimY, rightAnchorX, anchorY,
                firstColor, firstColor);
    }

    private void addChain(int chainId, float startX, float startY, float endX, float endY,
                          int startColor, int endColor) {
        int beadCount = 11;
        for (int i = 0; i < beadCount; i++) {
            float t = i / (float) (beadCount - 1);
            float x = startX + (endX - startX) * t;
            float y = startY + (endY - startY) * t
                    + (float) Math.sin(Math.PI * t) * dp(12f);
            int color = t < 0.5f ? startColor : endColor;
            chainBeads.add(new ChainBead(x, y, dp(5.2f), color, chainId));
        }
    }

    private void addDots(float left, float right, float y, int color, int count, float seed) {
        float areaWidth = right - left;
        for (int i = 0; i < count; i++) {
            float t = ((i * 0.37f + seed) % 1f);
            float x = left + areaWidth * (0.10f + 0.80f * t);
            float yy = y + ((i % 3) - 1) * dp(42f);
            dots.add(new DotDecor(x, yy, dp(4.6f), color));
        }
    }

    private void resetGame() {
        if (getWidth() == 0 || getHeight() == 0 || hoops.isEmpty()) {
            return;
        }
        state = STATE_READY;
        score = 0;
        targetHoop = 0;
        nextDirection = 1;
        ballX = getWidth() * 0.5f;
        ballY = Math.max(dp(120f), getHeight() * 0.24f);
        ballVx = 0f;
        ballVy = 0f;
        ballRotation = 0f;
        cameraTop = 0f;
        cameraTarget = 0f;
        accumulator = 0f;
        worldTime = 0f;
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
        ballVy = Math.min(ballVy + dp(GRAVITY_DP) * dt, dp(TERMINAL_VELOCITY_DP));
        ballVx *= Math.max(0f, 1f - HORIZONTAL_DRAG_PER_SECOND * dt);
        ballVx = clamp(ballVx, -dp(MAX_HORIZONTAL_SPEED_DP), dp(MAX_HORIZONTAL_SPEED_DP));
        ballX += ballVx * dt;
        ballY += ballVy * dt;
        ballRotation += (ballVx / Math.max(ballRadius, 1f)) * dt * 22f;

        float leftWall = playLeft + dp(21f) + ballRadius;
        float rightWall = playRight - dp(21f) - ballRadius;
        if (ballX < leftWall) {
            ballX = leftWall;
            ballVx = Math.abs(ballVx) * 0.76f;
            ballVy *= 0.985f;
        } else if (ballX > rightWall) {
            ballX = rightWall;
            ballVx = -Math.abs(ballVx) * 0.76f;
            ballVy *= 0.985f;
        }

        for (Obstacle obstacle : obstacles) {
            if (!isObstacleActive(obstacle)) {
                continue;
            }
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

        for (TriangleHazard triangle : triangles) {
            if (!isNearViewport(triangle.minY(), triangle.maxY(), dp(120f))) {
                continue;
            }
            if (triangle.contains(ballX, ballY, ballRadius * 0.35f)) {
                endGame();
                return;
            }
        }

        for (SpinnerObstacle spinner : spinners) {
            if (!isNearViewport(spinner.cy - spinner.armLength, spinner.cy + spinner.armLength, dp(120f))) {
                continue;
            }
            int touchedArm = spinner.touchedArm(ballX, ballY, ballRadius, worldTime, density);
            if (touchedArm == spinner.pinkArm) {
                endGame();
                return;
            } else if (touchedArm >= 0) {
                resolveSpinnerArm(spinner, touchedArm);
            }
        }

        resolveChainCollisions();

        for (Hoop hoop : hoops) {
            if (!isNearViewport(hoop.y - dp(30f), hoop.y + dp(30f), dp(120f))) {
                continue;
            }
            resolveRimCollision(hoop, hoop.x - hoop.width * 0.39f, hoop.y + dp(1f));
            resolveRimCollision(hoop, hoop.x + hoop.width * 0.39f, hoop.y + dp(1f));
        }

        if (targetHoop < hoops.size()) {
            Hoop target = hoops.get(targetHoop);
            boolean crossedDown = previousY < target.y - dp(2f)
                    && ballY >= target.y - dp(2f) && ballVy > 0f;
            float opening = target.width * 0.39f;
            float assistedOpening = target.width * 0.47f;
            if (crossedDown) {
                float offset = ballX - target.x;
                if (Math.abs(offset) < assistedOpening) {
                    if (Math.abs(offset) > opening) {
                        ballX -= offset * 0.28f;
                    }
                    scoreHoop(target);
                }
            }
        }

        cameraTarget = Math.max(cameraTarget, ballY - getHeight() * 0.36f);
        cameraTop += (cameraTarget - cameraTop) * Math.min(1f, dt * 4.2f);

        if (ballY > cameraTop + getHeight() + dp(120f)) {
            endGame();
        }
    }

    private boolean isNearViewport(float bottom, float top, float margin) {
        float viewBottom = cameraTop - margin;
        float viewTop = cameraTop + getHeight() + margin;
        return top >= viewBottom && bottom <= viewTop;
    }

    private boolean isObstacleActive(Obstacle obstacle) {
        return obstacle.linkedHoopIndex < 0 || !hoops.get(obstacle.linkedHoopIndex).scored;
    }

    private boolean circleIntersectsRect(float cx, float cy, float radius, Obstacle obstacle) {
        float nearestX = clamp(cx, obstacle.left, obstacle.left + obstacle.width);
        float nearestY = clamp(cy, obstacle.bottom, obstacle.bottom + obstacle.height);
        float dx = cx - nearestX;
        float dy = cy - nearestY;
        return dx * dx + dy * dy < radius * radius;
    }

    private void resolveChainCollisions() {
        for (int i = 0; i < chainBeads.size(); i++) {
            ChainBead bead = chainBeads.get(i);
            if (!isNearViewport(bead.y - bead.radius, bead.y + bead.radius, dp(90f))) {
                continue;
            }
            resolveChainCircle(bead.x, bead.y, bead.radius);
            if (i > 0) {
                ChainBead previous = chainBeads.get(i - 1);
                if (previous.chainId == bead.chainId
                        && Math.abs(previous.y - bead.y) < dp(95f)
                        && Math.abs(previous.x - bead.x) < dp(42f)) {
                    resolveChainSegment(previous.x, previous.y, bead.x, bead.y,
                            Math.max(previous.radius, bead.radius) * 0.55f);
                }
            }
        }
    }

    private void resolveChainCircle(float cx, float cy, float chainRadius) {
        float dx = ballX - cx;
        float dy = ballY - cy;
        float minimumDistance = ballRadius + chainRadius;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared >= minimumDistance * minimumDistance || distanceSquared < 0.0001f) {
            return;
        }
        float distance = (float) Math.sqrt(distanceSquared);
        resolveChainNormal(dx / distance, dy / distance, minimumDistance - distance);
    }

    private void resolveChainSegment(float ax, float ay, float bx, float by, float chainRadius) {
        float vx = bx - ax;
        float vy = by - ay;
        float lengthSquared = vx * vx + vy * vy;
        if (lengthSquared <= 0.0001f) {
            return;
        }
        float t = ((ballX - ax) * vx + (ballY - ay) * vy) / lengthSquared;
        t = clamp(t, 0f, 1f);
        float nearestX = ax + vx * t;
        float nearestY = ay + vy * t;
        float dx = ballX - nearestX;
        float dy = ballY - nearestY;
        float minimumDistance = ballRadius + chainRadius;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared >= minimumDistance * minimumDistance || distanceSquared < 0.0001f) {
            return;
        }
        float distance = (float) Math.sqrt(distanceSquared);
        resolveChainNormal(dx / distance, dy / distance, minimumDistance - distance);
    }

    private void resolveChainNormal(float nx, float ny, float penetration) {
        ballX += nx * (penetration + dp(0.35f));
        ballY += ny * (penetration + dp(0.35f));
        float normalVelocity = ballVx * nx + ballVy * ny;
        if (normalVelocity < 0f) {
            float tangentX = -ny;
            float tangentY = nx;
            float tangentVelocity = ballVx * tangentX + ballVy * tangentY;
            ballVx = tangentX * tangentVelocity * 0.975f - nx * normalVelocity * 0.28f;
            ballVy = tangentY * tangentVelocity * 0.975f - ny * normalVelocity * 0.28f;
        }
    }

    private void resolveSpinnerArm(SpinnerObstacle spinner, int armIndex) {
        float angle = spinner.armAngle(worldTime, armIndex);
        float endX = spinner.endX(angle);
        float endY = spinner.endY(angle);
        float vx = endX - spinner.cx;
        float vy = endY - spinner.cy;
        float lengthSquared = vx * vx + vy * vy;
        if (lengthSquared <= 0.0001f) {
            return;
        }
        float t = ((ballX - spinner.cx) * vx + (ballY - spinner.cy) * vy) / lengthSquared;
        t = clamp(t, 0f, 1f);
        float nearestX = spinner.cx + vx * t;
        float nearestY = spinner.cy + vy * t;
        float dx = ballX - nearestX;
        float dy = ballY - nearestY;
        float armRadius = dp(7f);
        float minimumDistance = ballRadius + armRadius;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared >= minimumDistance * minimumDistance) {
            return;
        }
        if (distanceSquared < 0.0001f) {
            float armNormalX = -vy / (float) Math.sqrt(lengthSquared);
            float armNormalY = vx / (float) Math.sqrt(lengthSquared);
            dx = armNormalX;
            dy = armNormalY;
            distanceSquared = 1f;
        }
        float distance = (float) Math.sqrt(distanceSquared);
        float nx = dx / distance;
        float ny = dy / distance;
        ballX += nx * (minimumDistance - distance + dp(0.45f));
        ballY += ny * (minimumDistance - distance + dp(0.45f));

        float normalVelocity = ballVx * nx + ballVy * ny;
        if (normalVelocity < 0f) {
            float tangentX = -ny;
            float tangentY = nx;
            float tangentVelocity = ballVx * tangentX + ballVy * tangentY;
            ballVx = tangentX * tangentVelocity * 0.93f - nx * normalVelocity * 0.52f;
            ballVy = tangentY * tangentVelocity * 0.93f - ny * normalVelocity * 0.52f;
        }
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
            float impulse = -(1f + 0.58f) * normalVelocity;
            ballVx = (ballVx + impulse * nx) * 0.97f;
            ballVy = (ballVy + impulse * ny) * 0.97f;
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
            float impulse = -(1f + 0.62f) * normalVelocity;
            ballVx = (ballVx + impulse * nx) * 0.965f;
            ballVy = (ballVy + impulse * ny) * 0.965f;
        }
    }

    private void scoreHoop(Hoop hoop) {
        hoop.scored = true;
        float centerError = Math.abs(ballX - hoop.x);
        lastBonus = centerError < dp(9f) ? 2 : 1;
        score += lastBonus;
        coins += 10;
        targetHoop++;
        scorePulse = 1f;
        bonusTimer = 0.85f;
        ballVy = Math.max(ballVy, dp(150f));
        spawnScoreParticles(hoop.x, hoop.y);
        preferences.edit().putInt("coins", coins).apply();
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
        if (state == STATE_PLAYING) {
            worldTime += dt;
        }
        scorePulse = Math.max(0f, scorePulse - dt * 3.5f);
        bonusTimer = Math.max(0f, bonusTimer - dt);
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            particle.life -= dt;
            if (particle.life <= 0f) {
                particles.remove(i);
                continue;
            }
            particle.vy += dp(230f) * dt;
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
        canvas.drawColor(Color.rgb(10, 7, 14));
        drawBackground(canvas);
        drawDots(canvas);
        drawChains(canvas);
        drawObstacles(canvas);
        drawTriangles(canvas);
        drawSpinners(canvas);
        drawHoopBackLayers(canvas);
        drawParticles(canvas);
        drawBall(canvas);
        drawHoopFrontLayers(canvas);
        drawHud(canvas);
    }

    private void drawBackground(Canvas canvas) {
        fillPaint.setColor(Color.rgb(103, 52, 126));
        canvas.drawRect(playLeft, 0f, playRight, getHeight(), fillPaint);

        float wallWidth = dp(21f);
        fillPaint.setColor(Color.rgb(55, 30, 77));
        canvas.drawRect(playLeft, 0f, playLeft + wallWidth, getHeight(), fillPaint);
        canvas.drawRect(playRight - wallWidth, 0f, playRight, getHeight(), fillPaint);

        fillPaint.setColor(Color.rgb(87, 42, 109));
        float brickW = dp(54f);
        float brickH = dp(14f);
        float worldStart = cameraTop - dp(80f);
        int firstRow = (int) Math.floor(worldStart / dp(135f));
        for (int row = firstRow; row < firstRow + 10; row++) {
            float worldY = row * dp(135f) + dp(72f);
            float screenY = worldToScreenY(worldY);
            float available = Math.max(1f, playRight - playLeft - brickW - dp(80f));
            float x = playLeft + dp(40f) + ((row * 83) % Math.max(1, (int) available));
            canvas.drawRect(x, screenY, x + brickW, screenY + brickH, fillPaint);
            canvas.drawRect(x + brickW * 0.42f, screenY + brickH + dp(2f),
                    x + brickW * 1.25f, screenY + brickH * 2f + dp(2f), fillPaint);
        }
    }

    private void drawDots(Canvas canvas) {
        for (DotDecor dot : dots) {
            if (!isNearViewport(dot.y - dot.radius, dot.y + dot.radius, dp(40f))) {
                continue;
            }
            fillPaint.setColor(dot.color);
            canvas.drawCircle(dot.x, worldToScreenY(dot.y), dot.radius, fillPaint);
        }
    }

    private void drawChains(Canvas canvas) {
        float oldStrokeWidth = fillPaint.getStrokeWidth();
        Paint.Style oldStyle = fillPaint.getStyle();
        Paint.Cap oldCap = fillPaint.getStrokeCap();

        fillPaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStrokeCap(Paint.Cap.ROUND);
        fillPaint.setStrokeWidth(dp(3.2f));
        for (int i = 1; i < chainBeads.size(); i++) {
            ChainBead previous = chainBeads.get(i - 1);
            ChainBead bead = chainBeads.get(i);
            if (previous.chainId != bead.chainId
                    || Math.abs(previous.y - bead.y) >= dp(95f)
                    || Math.abs(previous.x - bead.x) >= dp(42f)
                    || !isNearViewport(bead.y - bead.radius, bead.y + bead.radius, dp(50f))) {
                continue;
            }
            fillPaint.setColor(Color.rgb(58, 39, 84));
            canvas.drawLine(previous.x, worldToScreenY(previous.y),
                    bead.x, worldToScreenY(bead.y), fillPaint);
        }

        fillPaint.setStyle(Paint.Style.FILL);
        for (ChainBead bead : chainBeads) {
            if (!isNearViewport(bead.y - bead.radius, bead.y + bead.radius, dp(50f))) {
                continue;
            }
            fillPaint.setColor(bead.color);
            canvas.drawCircle(bead.x, worldToScreenY(bead.y), bead.radius, fillPaint);
        }

        fillPaint.setStrokeWidth(oldStrokeWidth);
        fillPaint.setStrokeCap(oldCap);
        fillPaint.setStyle(oldStyle);
    }

    private void drawObstacles(Canvas canvas) {
        for (Obstacle obstacle : obstacles) {
            if (!isObstacleActive(obstacle)) {
                continue;
            }
            if (!isNearViewport(obstacle.bottom, obstacle.bottom + obstacle.height, dp(40f))) {
                continue;
            }
            float screenTop = worldToScreenY(obstacle.bottom);
            float screenBottom = worldToScreenY(obstacle.bottom + obstacle.height);
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

    private void drawTriangles(Canvas canvas) {
        fillPaint.setColor(Color.rgb(255, 45, 118));
        for (TriangleHazard triangle : triangles) {
            if (!isNearViewport(triangle.minY(), triangle.maxY(), dp(60f))) {
                continue;
            }
            drawPath.reset();
            drawPath.moveTo(triangle.x1, worldToScreenY(triangle.y1));
            drawPath.lineTo(triangle.x2, worldToScreenY(triangle.y2));
            drawPath.lineTo(triangle.x3, worldToScreenY(triangle.y3));
            drawPath.close();
            canvas.drawPath(drawPath, fillPaint);
        }
    }

    private void drawSpinners(Canvas canvas) {
        float oldStrokeWidth = fillPaint.getStrokeWidth();
        Paint.Style oldStyle = fillPaint.getStyle();
        Paint.Cap oldCap = fillPaint.getStrokeCap();
        fillPaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStrokeCap(Paint.Cap.ROUND);
        for (SpinnerObstacle spinner : spinners) {
            if (!isNearViewport(spinner.cy - spinner.armLength, spinner.cy + spinner.armLength, dp(80f))) {
                continue;
            }
            float centerY = worldToScreenY(spinner.cy);
            float baseAngle = spinner.angle(worldTime);
            for (int i = 0; i < 3; i++) {
                float angle = baseAngle + (float) (Math.PI * 2f * i / 3f);
                float endX = spinner.cx + (float) Math.cos(angle) * spinner.armLength;
                float endY = worldToScreenY(spinner.cy + (float) Math.sin(angle) * spinner.armLength);
                fillPaint.setStrokeWidth(i == spinner.pinkArm ? dp(13f) : dp(13.5f));
                fillPaint.setColor(i == spinner.pinkArm ? Color.rgb(255, 45, 118)
                        : Color.rgb(61, 37, 91));
                canvas.drawLine(spinner.cx, centerY, endX, endY, fillPaint);
            }
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(Color.rgb(255, 174, 34));
            canvas.drawCircle(spinner.cx, centerY, dp(12f), fillPaint);
            fillPaint.setStyle(Paint.Style.STROKE);
        }
        fillPaint.setStrokeWidth(oldStrokeWidth);
        fillPaint.setStrokeCap(oldCap);
        fillPaint.setStyle(oldStyle);
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
            canvas.drawText("Chạm để nảy lên bên phải", getWidth() * 0.5f,
                    getHeight() * 0.43f, textPaint);
            textPaint.setTextSize(dp(14f));
            canvas.drawText("Lần chạm sau nảy lên bên trái", getWidth() * 0.5f,
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
        drawTopControls(canvas);
    }

    private void drawTopControls(Canvas canvas) {
        updateExitButtonRect();
        fillPaint.setColor(0xAA1F1430);
        canvas.drawRoundRect(exitButtonRect, exitButtonRect.height() * 0.22f,
                exitButtonRect.height() * 0.22f, fillPaint);
        drawExitIcon(canvas);

        updateCoinBoxRect();
        float panelHeight = coinBoxRect.height();
        fillPaint.setColor(0xAA1F1430);
        canvas.drawRoundRect(coinBoxRect, panelHeight * 0.22f, panelHeight * 0.22f, fillPaint);

        float coinSize = panelHeight * 0.74f;
        drawRect.set(coinBoxRect.left + panelHeight * 0.16f,
                coinBoxRect.centerY() - coinSize * 0.5f,
                coinBoxRect.left + panelHeight * 0.16f + coinSize,
                coinBoxRect.centerY() + coinSize * 0.5f);
        if (coinBitmap != null) {
            canvas.drawBitmap(coinBitmap, null, drawRect, bitmapPaint);
        }

        String coinText = Integer.toString(coins);
        float textSize = panelHeight * 0.43f;
        textPaint.setTextSize(textSize);
        textShadowPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textShadowPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setColor(Color.WHITE);
        float textX = coinBoxRect.right - panelHeight * 0.25f;
        float textY = coinBoxRect.centerY() - (textPaint.ascent() + textPaint.descent()) * 0.5f;
        canvas.drawText(coinText, textX + dp(1.3f), textY + dp(1.3f), textShadowPaint);
        canvas.drawText(coinText, textX, textY, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void updateExitButtonRect() {
        float size = dp(46f);
        exitButtonRect.set(dp(14f), dp(18f), dp(14f) + size, dp(18f) + size);
    }

    private void updateCoinBoxRect() {
        float height = dp(46f);
        float width = Math.min(dp(132f), getWidth() * 0.34f);
        coinBoxRect.set(getWidth() - dp(14f) - width, dp(18f),
                getWidth() - dp(14f), dp(18f) + height);
    }

    private void drawExitIcon(Canvas canvas) {
        float oldStrokeWidth = fillPaint.getStrokeWidth();
        Paint.Style oldStyle = fillPaint.getStyle();
        Paint.Cap oldCap = fillPaint.getStrokeCap();
        Paint.Join oldJoin = fillPaint.getStrokeJoin();

        fillPaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStrokeCap(Paint.Cap.ROUND);
        fillPaint.setStrokeJoin(Paint.Join.ROUND);
        fillPaint.setStrokeWidth(dp(4.2f));
        fillPaint.setColor(Color.WHITE);
        float centerX = exitButtonRect.centerX();
        float centerY = exitButtonRect.centerY();
        float size = exitButtonRect.width();
        drawPath.reset();
        drawPath.moveTo(centerX + size * 0.16f, centerY - size * 0.22f);
        drawPath.lineTo(centerX - size * 0.14f, centerY);
        drawPath.lineTo(centerX + size * 0.16f, centerY + size * 0.22f);
        canvas.drawPath(drawPath, fillPaint);
        canvas.drawLine(centerX - size * 0.10f, centerY, centerX + size * 0.22f, centerY, fillPaint);

        fillPaint.setStrokeWidth(oldStrokeWidth);
        fillPaint.setStrokeCap(oldCap);
        fillPaint.setStrokeJoin(oldJoin);
        fillPaint.setStyle(oldStyle);
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
        return dp(34f) + (worldY - cameraTop);
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

        if (isExitTap(event.getX(), event.getY())) {
            Context context = getContext();
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
            return true;
        }
        if (state == STATE_GAME_OVER) {
            resetGame();
            return true;
        }
        if (state == STATE_READY) {
            state = STATE_PLAYING;
            lastFrameNanos = now;
            startFrameLoop();
        }
        float targetVx = nextDirection * dp(TAP_VELOCITY_X_DP);
        ballVx = ballVx * 0.22f + targetVx;
        ballVx = clamp(ballVx, -dp(MAX_HORIZONTAL_SPEED_DP), dp(MAX_HORIZONTAL_SPEED_DP));
        if (ballVy > dp(90f)) {
            ballVy = dp(90f);
        }
        ballVy = ballVy * 0.12f + dp(TAP_VELOCITY_Y_DP);
        nextDirection *= -1;
        performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        return true;
    }

    private boolean isExitTap(float x, float y) {
        updateExitButtonRect();
        return exitButtonRect.contains(x, y);
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
        final int linkedHoopIndex;

        private Obstacle(float left, float bottom, float width, float height,
                         boolean lethal, int drawType, boolean mirror, int linkedHoopIndex) {
            this.left = left;
            this.bottom = bottom;
            this.width = width;
            this.height = height;
            this.lethal = lethal;
            this.drawType = drawType;
            this.mirror = mirror;
            this.linkedHoopIndex = linkedHoopIndex;
        }

        static Obstacle hazard(float left, float bottom, float width, float height,
                               int drawType, boolean mirror) {
            return new Obstacle(left, bottom, width, height, true, drawType, mirror, -1);
        }

        static Obstacle gate(float left, float bottom, float width, float height,
                             int linkedHoopIndex) {
            return new Obstacle(left, bottom, width, height, true,
                    DRAW_RED_HORIZONTAL, false, linkedHoopIndex);
        }

        static Obstacle solid(float left, float bottom, float width, float height) {
            return new Obstacle(left, bottom, width, height, false, 0, false, -1);
        }
    }

    private static final class SpinnerObstacle {
        final float cx;
        final float cy;
        final float armLength;
        final float speed;
        final int pinkArm;
        final float phase;

        SpinnerObstacle(float cx, float cy, float armLength, float speed, boolean reverse) {
            this.cx = cx;
            this.cy = cy;
            this.armLength = armLength;
            this.speed = reverse ? speed : -speed;
            this.phase = reverse ? 0.9f : 0.25f;
            this.pinkArm = reverse ? 1 : 0;
        }

        float angle(float worldTime) {
            return phase + worldTime * speed;
        }

        float armAngle(float worldTime, int armIndex) {
            return angle(worldTime) + (float) (Math.PI * 2f * armIndex / 3f);
        }

        float endX(float angle) {
            return cx + (float) Math.cos(angle) * armLength;
        }

        float endY(float angle) {
            return cy + (float) Math.sin(angle) * armLength;
        }

        int touchedArm(float ballX, float ballY, float ballRadius,
                       float worldTime, float density) {
            int touched = -1;
            float closest = Float.MAX_VALUE;
            float touchDistance = ballRadius + 7f * density;
            for (int i = 0; i < 3; i++) {
                float armAngle = armAngle(worldTime, i);
                float distance = distanceToSegment(ballX, ballY, cx, cy,
                        endX(armAngle), endY(armAngle));
                if (distance < touchDistance && distance < closest) {
                    closest = distance;
                    touched = i;
                }
            }
            return touched;
        }
    }

    private static final class TriangleHazard {
        final float x1;
        final float y1;
        final float x2;
        final float y2;
        final float x3;
        final float y3;

        private TriangleHazard(float x1, float y1, float x2, float y2,
                               float x3, float y3) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.x3 = x3;
            this.y3 = y3;
        }

        static TriangleHazard left(float left, float y, float width, float height) {
            return new TriangleHazard(left, y, left, y + height, left + width, y + height * 0.5f);
        }

        static TriangleHazard right(float right, float y, float width, float height) {
            return new TriangleHazard(right, y, right, y + height, right - width, y + height * 0.5f);
        }

        float minY() {
            return Math.min(y1, Math.min(y2, y3));
        }

        float maxY() {
            return Math.max(y1, Math.max(y2, y3));
        }

        boolean contains(float x, float y, float radius) {
            if (pointInTriangle(x, y)) {
                return true;
            }
            return distanceToSegment(x, y, x1, y1, x2, y2) < radius
                    || distanceToSegment(x, y, x2, y2, x3, y3) < radius
                    || distanceToSegment(x, y, x3, y3, x1, y1) < radius;
        }

        private boolean pointInTriangle(float x, float y) {
            float d1 = sign(x, y, x1, y1, x2, y2);
            float d2 = sign(x, y, x2, y2, x3, y3);
            float d3 = sign(x, y, x3, y3, x1, y1);
            boolean hasNegative = d1 < 0f || d2 < 0f || d3 < 0f;
            boolean hasPositive = d1 > 0f || d2 > 0f || d3 > 0f;
            return !(hasNegative && hasPositive);
        }

        private float sign(float px, float py, float ax, float ay, float bx, float by) {
            return (px - bx) * (ay - by) - (ax - bx) * (py - by);
        }
    }

    private static final class DotDecor {
        final float x;
        final float y;
        final float radius;
        final int color;

        DotDecor(float x, float y, float radius, int color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
        }
    }

    private static final class ChainBead {
        final float x;
        final float y;
        final float radius;
        final int color;
        final int chainId;

        ChainBead(float x, float y, float radius, int color, int chainId) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            this.chainId = chainId;
        }
    }

    private static float distanceToSegment(float px, float py, float ax, float ay,
                                           float bx, float by) {
        float vx = bx - ax;
        float vy = by - ay;
        float lengthSquared = vx * vx + vy * vy;
        if (lengthSquared <= 0.0001f) {
            float dx = px - ax;
            float dy = py - ay;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
        float t = ((px - ax) * vx + (py - ay) * vy) / lengthSquared;
        t = Math.max(0f, Math.min(1f, t));
        float nearestX = ax + vx * t;
        float nearestY = ay + vy * t;
        float dx = px - nearestX;
        float dy = py - nearestY;
        return (float) Math.sqrt(dx * dx + dy * dy);
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

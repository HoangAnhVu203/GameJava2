package com.example.game43.FlipperDrunk;

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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.game43.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlipperDrunkGameView extends FrameLayout {
    private static final boolean SHOW_COLLIDER_DEBUG = false;
    private static final int HOOPS_PER_TURN = 3;
    private static final float FLIP_SECONDS = 0.18f;
    private static final float FLIP_RETURN_SECONDS = 0.22f;
    private static final float DESIGN_WIDTH = 309f;
    private static final float DESIGN_HEIGHT = 669f;
    private static final float RIGHT_WALL_ASPECT = 648f / 2727f;
    private static final float LEFT_WALL_ASPECT = 88f / 2243f;
    private static final float FLIPPER_ASPECT = 584f / 339f;
    private static final float FLIPPER_SCALE = 0.75f;
    private static final float FLIPPER_MIN_LAUNCH_ANGLE = 42f;
    private static final float FLIPPER_MAX_LAUNCH_ANGLE = 72f;
    private static final float FLIPPER_LAUNCH_SPEED = 1.3f;
    private static final float FLIPPER_SURFACE_HEIGHT_RATIO = 0.35f;
    private static final float FLIPPER_PIVOT_DOWN = 5f;
    private static final float RIGHT_HOOP_OFFSET = 7f;
    private static final float RIGHT_WALL_RAMP_START_Y = 0.685f;
    private static final float RIGHT_WALL_RAMP_SMOOTH_EXIT = 0.24f;
    private static final float RIGHT_WALL_RAMP_ENTRY_SLOPE_DEGREES = 10f;
    private static final float RIGHT_WALL_RAMP_LEAD_IN_WIDTH = 0.01f;
    private static final float HOOP_RIM_COLLIDER_OFFSET = 25f;
    private static final float WALL_RESTITUTION = 0.28f;
    private static final float WALL_BOUNCE_DAMPING = 0.86f;
    private static final float RAMP_RESTITUTION = 0.12f;
    private static final float RAMP_BOUNCE_DAMPING = 0.92f;
    private static final int WALL_ALPHA_THRESHOLD = 32;

    private final List<Hoop> hoops = new ArrayList<>();
    private final Random random = new Random();
    private final Paint colliderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private FrameLayout stageView;
    private ImageView backgroundView;
    private ImageView rightWallView;
    private ImageView leftWallView;
    private ImageView hoopBackView;
    private ImageView hoopFrontView;
    private ImageView ballView;
    private ImageView flipperView;
    private TextView scoreView;
    private Bitmap rightWallBitmap;

    private float actualWidth;
    private float actualHeight;
    private float stageOffsetX;
    private float stageOffsetY;
    private float viewWidth;
    private float viewHeight;
    private float ballRadius;
    private float ballX;
    private float ballY;
    private float previousBallY;
    private float velocityX;
    private float velocityY;
    private float ballRotation;
    private float flipperSide = 1f;
    private float hoopSide = 1f;
    private float flipperAngle;
    private float flipperTargetAngle;
    private float flipperTimer;
    private long lastFrameNanos;
    private int score;
    private int scoredHoopsThisTurn;
    private boolean waitingForFirstTap = true;
    private boolean gameOver;
    private boolean flipperKickUsed;
    private GameOverListener gameOverListener;

    public FlipperDrunkGameView(Context context) {
        super(context);
        init();
    }

    public FlipperDrunkGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FlipperDrunkGameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(true);
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        rightWallBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ui_04);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        stageView = findViewById(R.id.flipperStage);
        backgroundView = findViewById(R.id.flipperBackground);
        rightWallView = findViewById(R.id.flipperRightWall);
        leftWallView = findViewById(R.id.flipperLeftWall);
        hoopBackView = findViewById(R.id.flipperHoopBack);
        hoopFrontView = findViewById(R.id.flipperHoopFront);
        ballView = findViewById(R.id.flipperBall);
        flipperView = findViewById(R.id.flipperPaddle);
        scoreView = findViewById(R.id.flipperScore);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        configureSize(w, h);
    }

    private void configureSize(int width, int height) {
        actualWidth = width;
        actualHeight = height;
        float designAspect = DESIGN_WIDTH / DESIGN_HEIGHT;
        float currentAspect = actualWidth / Math.max(1f, actualHeight);
        if (currentAspect > designAspect) {
            viewHeight = actualHeight;
            viewWidth = viewHeight * designAspect;
        } else {
            viewWidth = actualWidth;
            viewHeight = viewWidth / designAspect;
        }
        stageOffsetX = (actualWidth - viewWidth) * 0.5f;
        stageOffsetY = (actualHeight - viewHeight) * 0.5f;
        ballRadius = gs(DESIGN_WIDTH * 0.056f);
        resetGame();
    }

    public void resetGame() {
        if (viewWidth <= 0f || viewHeight <= 0f) {
            return;
        }

        score = 0;
        scoredHoopsThisTurn = 0;
        gameOver = false;
        flipperSide = 1f;
        hoopSide = 1f;
        flipperKickUsed = false;
        flipperAngle = getRestAngle();
        flipperTargetAngle = flipperAngle;
        spawnActiveHoop();
        placeBallOnFlipper();
        waitingForFirstTap = true;
        updateScoreView();
    }

    private void placeBallOnFlipper() {
        float[] pivot = getFlipperPivot();
        float[] end = getFlipperEndPoint();
        float segmentX = end[0] - pivot[0];
        float segmentY = end[1] - pivot[1];
        float length = Math.max(1f, (float) Math.hypot(segmentX, segmentY));
        float normalX = -segmentY / length;
        float normalY = segmentX / length;
        if (normalY > 0f) {
            normalX = -normalX;
            normalY = -normalY;
        }
        float t = 0.68f;
        float surfaceX = pivot[0] + segmentX * t;
        float surfaceY = pivot[1] + segmentY * t;
        float lift = getFlipperSurfaceLift();
        ballX = surfaceX + normalX * lift;
        ballY = surfaceY + normalY * lift;
        previousBallY = ballY;
        velocityX = 0f;
        velocityY = 0f;
        ballRotation = 0f;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        ensureSize();

        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
        }
        float dt = Math.min(0.033f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;

        updateGame(dt);
        layoutGameViews();
        super.dispatchDraw(canvas);
        drawColliderDebug(canvas);
        postInvalidateOnAnimation();
    }

    private void ensureSize() {
        int width = getWidth();
        int height = getHeight();
        if ((viewWidth <= 0f || viewHeight <= 0f) && width > 0 && height > 0) {
            configureSize(width, height);
        }
    }

    private void updateGame(float dt) {
        if (viewWidth <= 0f || viewHeight <= 0f) {
            return;
        }

        updateFlipper(dt);
        if (gameOver) {
            return;
        }
        if (waitingForFirstTap) {
            placeBallOnFlipper();
            return;
        }
        previousBallY = ballY;

        velocityY += gravity() * dt;
        ballX += velocityX * dt;
        ballY += velocityY * dt;
        ballRotation += velocityX * dt * 0.24f;

        resolveWallCollisions();
        resolveFlipperCollision();
        for (Hoop hoop : hoops) {
            if (!hoop.scored) {
                resolveHoopCollision(hoop);
            }
        }
        checkScores();

        if (ballY - ballRadius > viewHeight + gs(8f)) {
            triggerGameOver();
        }
    }

    private void updateFlipper(float dt) {
        if (flipperTimer > 0f) {
            flipperTimer = Math.max(0f, flipperTimer - dt);
            if (flipperTimer <= 0f) {
                flipperTargetAngle = getRestAngle();
                flipperKickUsed = false;
            }
        }

        float speed = flipperTargetAngle == getActiveAngle()
                ? 1f / FLIP_SECONDS
                : 1f / FLIP_RETURN_SECONDS;
        flipperAngle += (flipperTargetAngle - flipperAngle) * Math.min(1f, dt * speed * 10f);
    }

    private void resolveWallCollisions() {
        resolveSideWallCollision(-1f);
        resolveSideWallCollision(1f);
        resolveRightWallRampCollision();
    }

    private void resolveSideWallCollision(float side) {
        if (ballY > viewHeight - getWallLift() + ballRadius * 0.4f) {
            return;
        }
        if (side > 0f && isRightWallRampZone(ballY + ballRadius * 0.35f)) {
            return;
        }

        float innerX = getSideWallInnerX(ballY, side);
        float slope = getSideWallSlope(ballY, side);
        boolean hitRight = side > 0f && ballX + ballRadius > innerX;
        boolean hitLeft = side < 0f && ballX - ballRadius < innerX;
        if (!hitRight && !hitLeft) {
            return;
        }

        float penetration = hitRight
                ? ballX + ballRadius - innerX
                : innerX - (ballX - ballRadius);
        float normalX = -side;
        float normalY = side > 0f ? slope : -slope;
        float length = (float) Math.hypot(normalX, normalY);
        if (length > 0.001f) {
            normalX /= length;
            normalY /= length;
        }

        ballX += normalX * (penetration + 1f);
        ballY += normalY * (penetration + 1f);

        float velocityAlongNormal = velocityX * normalX + velocityY * normalY;
        if (velocityAlongNormal < 0f) {
            velocityX -= (1f + WALL_RESTITUTION) * velocityAlongNormal * normalX;
            velocityY -= (1f + WALL_RESTITUTION) * velocityAlongNormal * normalY;
            velocityX *= WALL_BOUNCE_DAMPING;
            velocityY *= WALL_BOUNCE_DAMPING;
        }
    }

    private void resolveRightWallRampCollision() {
        float surfaceY = getRightWallRampYAt(ballX);
        if (surfaceY < 0f || ballY + ballRadius < surfaceY) {
            return;
        }

        float sample = Math.max(gs(2f), ballRadius * 0.2f);
        float leftY = getRightWallRampYAt(ballX - sample);
        float rightY = getRightWallRampYAt(ballX + sample);
        if (leftY < 0f) {
            leftY = surfaceY;
        }
        if (rightY < 0f) {
            rightY = surfaceY;
        }

        float tangentX = sample * 2f;
        float tangentY = rightY - leftY;
        float normalX = -tangentY;
        float normalY = tangentX;
        float normalLength = (float) Math.hypot(normalX, normalY);
        if (normalLength <= 0.001f) {
            normalX = 0f;
            normalY = -1f;
        } else {
            normalX /= normalLength;
            normalY /= normalLength;
            if (normalY > 0f) {
                normalX = -normalX;
                normalY = -normalY;
            }
        }

        ballY = surfaceY - ballRadius;
        float velocityAlongNormal = velocityX * normalX + velocityY * normalY;
        if (velocityAlongNormal < 0f) {
            velocityX -= (1f + RAMP_RESTITUTION) * velocityAlongNormal * normalX;
            velocityY -= (1f + RAMP_RESTITUTION) * velocityAlongNormal * normalY;
            velocityX *= RAMP_BOUNCE_DAMPING;
            velocityY *= RAMP_BOUNCE_DAMPING;
        }
        velocityX *= 0.985f;
    }

    private void resolveFlipperCollision() {
        float[] pivot = getFlipperPivot();
        float[] end = getFlipperEndPoint();
        float segmentX = end[0] - pivot[0];
        float segmentY = end[1] - pivot[1];
        float lengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (lengthSquared <= 0.001f) {
            return;
        }

        float t = ((ballX - pivot[0]) * segmentX + (ballY - pivot[1]) * segmentY) / lengthSquared;
        if (t < 0.05f || t > 0.96f) {
            return;
        }

        float nearestX = pivot[0] + segmentX * t;
        float nearestY = pivot[1] + segmentY * t;
        float length = (float) Math.sqrt(lengthSquared);
        float normalX = -segmentY / length;
        float normalY = segmentX / length;
        if (normalY > 0f) {
            normalX = -normalX;
            normalY = -normalY;
        }

        float aboveSurface = (ballX - nearestX) * normalX + (ballY - nearestY) * normalY;
        float surfaceLift = getFlipperSurfaceLift();
        float velocityAlongNormal = velocityX * normalX + velocityY * normalY;
        if (aboveSurface <= 0f || aboveSurface > surfaceLift || velocityAlongNormal >= 0f) {
            return;
        }

        float penetration = surfaceLift - aboveSurface;
        ballX += normalX * (penetration + 0.5f);
        ballY += normalY * (penetration + 0.5f);

        if (flipperTargetAngle == getActiveAngle() && !flipperKickUsed) {
            kickBallFromFlipper(t);
        } else {
            velocityX -= 1.12f * velocityAlongNormal * normalX;
            velocityY -= 1.12f * velocityAlongNormal * normalY;
        }
    }

    private void resolveHoopCollision(Hoop hoop) {
        resolveSegmentCollision(hoop.frontRimX, hoop.frontRimTop,
                hoop.frontRimX, hoop.frontRimBottom, ballRadius, 0.55f);
    }

    private boolean resolveSegmentCollision(float startX, float startY, float endX, float endY,
                                            float radius, float bounce) {
        float segmentX = endX - startX;
        float segmentY = endY - startY;
        float lengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (lengthSquared <= 0.001f) {
            return false;
        }

        float t = ((ballX - startX) * segmentX + (ballY - startY) * segmentY) / lengthSquared;
        t = clamp(t, 0f, 1f);
        float nearestX = startX + segmentX * t;
        float nearestY = startY + segmentY * t;
        float dx = ballX - nearestX;
        float dy = ballY - nearestY;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared >= radius * radius) {
            return false;
        }

        float distance = distanceSquared > 0.001f ? (float) Math.sqrt(distanceSquared) : 0f;
        float normalX = distance > 0f ? dx / distance : -flipperSide;
        float normalY = distance > 0f ? dy / distance : -1f;
        float overlap = radius - distance;
        ballX += normalX * (overlap + 1f);
        ballY += normalY * (overlap + 1f);

        float velocityAlongNormal = velocityX * normalX + velocityY * normalY;
        if (velocityAlongNormal < 0f) {
            velocityX -= (1f + bounce) * velocityAlongNormal * normalX;
            velocityY -= (1f + bounce) * velocityAlongNormal * normalY;
        }
        return true;
    }

    private void checkScores() {
        boolean scoredAnyHoop = false;
        for (Hoop hoop : hoops) {
            if (hoop.scored) {
                continue;
            }

            boolean crossedDown = previousBallY < hoop.openY && ballY >= hoop.openY;
            boolean falling = velocityY > 0f;
            boolean inside = Math.abs(ballX - hoop.openX) <= hoop.openingHalfWidth;
            if (crossedDown && falling && inside) {
                hoop.scored = true;
                score++;
                scoredHoopsThisTurn++;
                scoredAnyHoop = true;
                velocityY = Math.min(velocityY, -gy(DESIGN_HEIGHT * 0.24f));
            }
        }

        if (scoredAnyHoop) {
            if (scoredHoopsThisTurn >= HOOPS_PER_TURN) {
                scoredHoopsThisTurn = 0;
            }
            updateScoreView();
            switchSideAndSpawnNextHoop();
            placeBallOnFlipper();
        }
    }

    private void switchSideAndSpawnNextHoop() {
        hoopSide *= -1f;
        spawnActiveHoop();
    }

    private void spawnActiveHoop() {
        hoops.clear();
        Hoop hoop = new Hoop();
        hoop.side = hoopSide;
        float wallNudge = gx(5f);
        hoop.openX = hoopSide > 0f
                ? getRightWallInnerX(gy(DESIGN_HEIGHT * 0.32f)) - gs(DESIGN_WIDTH * 0.085f) + wallNudge + gx(RIGHT_HOOP_OFFSET)
                : getLeftWallInnerX(gy(DESIGN_HEIGHT * 0.32f)) + gs(DESIGN_WIDTH * 0.085f) - wallNudge;
        hoop.openY = chooseRandomHoopHeight();
        layoutHoop(hoop);
        hoops.add(hoop);
    }

    private float chooseRandomHoopHeight() {
        float top = gy(DESIGN_HEIGHT * 0.23f);
        float bottom = gy(DESIGN_HEIGHT * 0.56f);
        return randomBetween(top, bottom);
    }

    private void layoutHoop(Hoop hoop) {
        float frontWidth = gs(DESIGN_WIDTH * 0.225f);
        float frontHeight = frontWidth * 80f / 96f;
        float backWidth = frontWidth * 112f / 96f;
        float backHeight = frontWidth * 30f / 96f;
        float hoopWidth = Math.max(frontWidth, backWidth);
        float backTop = hoop.openY - backHeight * 0.55f;
        float backLeft = hoop.openX - backWidth * 0.5f;
        float frontTop = backTop + backHeight - frontWidth * 16f / 96f;
        float frontOffsetX = -hoop.side * frontWidth * 9f / 96f + hoop.side * gx(3f);

        hoop.openingHalfWidth = Math.max(ballRadius * 1.35f, hoopWidth * 0.25f);
        hoop.backLayerRect.set(backLeft, backTop, backLeft + backWidth, backTop + backHeight);
        hoop.frontLayerRect.set(hoop.openX - frontWidth * 0.5f + frontOffsetX,
                frontTop,
                hoop.openX + frontWidth * 0.5f + frontOffsetX,
                frontTop + frontHeight);
        float rimSegmentHeight = Math.max(ballRadius * 0.5f, frontWidth * 12f / 96f);
        hoop.frontRimX = hoop.openX - hoop.side * (hoop.openingHalfWidth + gx(HOOP_RIM_COLLIDER_OFFSET));
        hoop.frontRimTop = hoop.openY - rimSegmentHeight * 0.5f;
        hoop.frontRimBottom = hoop.openY + rimSegmentHeight * 0.5f;
    }

    private void layoutGameViews() {
        layoutRect(stageView, stageOffsetX, stageOffsetY, stageOffsetX + viewWidth, stageOffsetY + viewHeight);
        layoutRect(backgroundView, 0f, 0f, viewWidth, viewHeight);
        layoutRect(rightWallView, viewWidth - getRightWallWidth(), -getWallLift(),
                viewWidth, viewHeight - getWallLift());
        layoutRect(leftWallView, 0f, -getWallLift(), getLeftWallWidth(), viewHeight - getWallLift());

        Hoop hoop = hoops.isEmpty() ? null : hoops.get(0);
        if (hoop != null && !hoop.scored) {
            layoutImage(hoopBackView, hoop.backLayerRect, hoop.side < 0f, 0f, 0.5f, 0.5f);
            layoutImage(hoopFrontView, hoop.frontLayerRect, hoop.side < 0f, 0f, 0.5f, 0.5f);
        } else {
            hideView(hoopBackView);
            hideView(hoopFrontView);
        }

        layoutFlipper();
        layoutImage(ballView,
                ballX - ballRadius, ballY - ballRadius,
                ballX + ballRadius, ballY + ballRadius,
                false, ballRotation, 0.5f, 0.5f);
        if (ballView != null) {
            ballView.bringToFront();
        }
        if (hoopFrontView != null) {
            hoopFrontView.bringToFront();
        }
        if (scoreView != null) {
            scoreView.bringToFront();
        }
    }

    private void layoutFlipper() {
        float[] pivot = getFlipperPivot();
        float[] end = getFlipperEndPoint();
        float length = getFlipperVisualLength();
        float height = getFlipperVisualHeight();
        float left;
        float right;
        float rotation;
        float pivotRatioX;
        boolean flip;
        if (flipperSide > 0f) {
            left = pivot[0] - length * 0.92f;
            right = pivot[0] + length * 0.08f;
            rotation = flipperAngle - 180f;
            pivotRatioX = 0.92f;
            flip = false;
        } else {
            left = pivot[0] - length * 0.08f;
            right = pivot[0] + length * 0.92f;
            rotation = flipperAngle;
            pivotRatioX = 0.08f;
            flip = true;
        }

        layoutImage(flipperView, left, pivot[1] - height * 0.5f,
                right, pivot[1] + height * 0.5f, flip, rotation, pivotRatioX, 0.5f);
    }

    private void layoutImage(View view, RectF rect, boolean flipHorizontal,
                             float rotation, float pivotRatioX, float pivotRatioY) {
        layoutImage(view, rect.left, rect.top, rect.right, rect.bottom,
                flipHorizontal, rotation, pivotRatioX, pivotRatioY);
    }

    private void layoutImage(View view, float left, float top, float right, float bottom,
                             boolean flipHorizontal, float rotation,
                             float pivotRatioX, float pivotRatioY) {
        layoutRect(view, left, top, right, bottom);
        if (view == null) {
            return;
        }
        view.setPivotX(view.getWidth() * pivotRatioX);
        view.setPivotY(view.getHeight() * pivotRatioY);
        view.setRotation(rotation);
        view.setScaleX(flipHorizontal ? -1f : 1f);
    }

    private void layoutRect(View view, float left, float top, float right, float bottom) {
        if (view == null) {
            return;
        }

        int l = Math.round(left);
        int t = Math.round(top);
        int r = Math.round(right);
        int b = Math.round(bottom);
        int width = Math.max(1, r - l);
        int height = Math.max(1, b - t);
        view.setVisibility(VISIBLE);
        view.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        view.layout(l, t, l + width, t + height);
    }

    private void hideView(View view) {
        if (view != null) {
            view.setVisibility(INVISIBLE);
        }
    }

    private void drawColliderDebug(Canvas canvas) {
        if (!SHOW_COLLIDER_DEBUG || viewWidth <= 0f || viewHeight <= 0f) {
            return;
        }

        int save = canvas.save();
        canvas.translate(stageOffsetX, stageOffsetY);

        colliderPaint.setStyle(Paint.Style.STROKE);
        colliderPaint.setStrokeCap(Paint.Cap.ROUND);
        colliderPaint.setStrokeJoin(Paint.Join.ROUND);
        colliderPaint.setStrokeWidth(Math.max(2f, gs(2.2f)));

        colliderPaint.setColor(Color.argb(235, 0, 255, 120));
        drawSideWallCollider(canvas, -1f);
        drawSideWallCollider(canvas, 1f);

        colliderPaint.setColor(Color.argb(245, 255, 185, 0));
        drawRightRampCollider(canvas);

        colliderPaint.setColor(Color.argb(245, 255, 40, 210));
        drawFlipperCollider(canvas);

        colliderPaint.setColor(Color.argb(245, 255, 60, 60));
        drawHoopCollider(canvas);

        colliderPaint.setColor(Color.argb(235, 255, 255, 0));
        canvas.drawCircle(ballX, ballY, ballRadius, colliderPaint);

        colliderPaint.setStyle(Paint.Style.FILL);
        colliderPaint.setTextAlign(Paint.Align.LEFT);
        colliderPaint.setTextSize(Math.max(12f, gs(10f)));
        colliderPaint.setColor(Color.argb(245, 255, 255, 255));
        canvas.drawText("COLLIDER DEBUG", gs(8f), gy(18f), colliderPaint);

        canvas.restoreToCount(save);
    }

    private void drawSideWallCollider(Canvas canvas, float side) {
        float topY = 0f;
        float bottomY = side > 0f
                ? Math.min(viewHeight, gy(DESIGN_HEIGHT * RIGHT_WALL_RAMP_START_Y) - getWallLift())
                : viewHeight - getWallLift();
        float step = Math.max(gs(4f), viewHeight / 140f);
        float lastX = getSideWallInnerX(topY, side);
        float lastY = topY;
        for (float y = topY + step; y <= bottomY; y += step) {
            float x = getSideWallInnerX(y, side);
            canvas.drawLine(lastX, lastY, x, y, colliderPaint);
            lastX = x;
            lastY = y;
        }
        if (lastY < bottomY) {
            float x = getSideWallInnerX(bottomY, side);
            canvas.drawLine(lastX, lastY, x, bottomY, colliderPaint);
        }
    }

    private void drawRightRampCollider(Canvas canvas) {
        float leftX = viewWidth - getRightWallWidth();
        float rightX = viewWidth;
        float step = Math.max(gs(3f), getRightWallWidth() / 64f);
        boolean hasLast = false;
        float lastX = 0f;
        float lastY = 0f;
        for (float x = leftX; x <= rightX; x += step) {
            float y = getRightWallRampYAt(x);
            if (y >= 0f) {
                if (hasLast) {
                    canvas.drawLine(lastX, lastY, x, y, colliderPaint);
                }
                lastX = x;
                lastY = y;
                hasLast = true;
            } else {
                hasLast = false;
            }
        }
    }

    private void drawFlipperCollider(Canvas canvas) {
        float[] pivot = getFlipperPivot();
        float[] end = getFlipperEndPoint();
        canvas.drawLine(pivot[0], pivot[1], end[0], end[1], colliderPaint);

        float segmentX = end[0] - pivot[0];
        float segmentY = end[1] - pivot[1];
        float length = Math.max(1f, (float) Math.hypot(segmentX, segmentY));
        float normalX = -segmentY / length;
        float normalY = segmentX / length;
        if (normalY > 0f) {
            normalX = -normalX;
            normalY = -normalY;
        }
        float lift = getFlipperSurfaceLift();
        canvas.drawLine(pivot[0] + normalX * lift, pivot[1] + normalY * lift,
                end[0] + normalX * lift, end[1] + normalY * lift, colliderPaint);
    }

    private void drawHoopCollider(Canvas canvas) {
        for (Hoop hoop : hoops) {
            if (hoop.scored) {
                continue;
            }
            canvas.drawLine(hoop.frontRimX, hoop.frontRimTop,
                    hoop.frontRimX, hoop.frontRimBottom, colliderPaint);
            canvas.drawLine(hoop.openX - hoop.openingHalfWidth, hoop.openY,
                    hoop.openX + hoop.openingHalfWidth, hoop.openY, colliderPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            activateFlipper();
            return true;
        }
        return true;
    }

    private void activateFlipper() {
        if (gameOver) {
            return;
        }
        waitingForFirstTap = false;
        flipperKickUsed = false;
        flipperTimer = FLIP_SECONDS;
        flipperTargetAngle = getActiveAngle();
        float contactT = getBallFlipperContactT();
        if (contactT >= 0f) {
            kickBallFromFlipper(contactT);
        }
    }

    private void kickBallFromFlipper(float contactT) {
        flipperKickUsed = true;
        float tipAmount = clamp(contactT, 0f, 1f);
        float angleDegrees = FLIPPER_MAX_LAUNCH_ANGLE
                - (FLIPPER_MAX_LAUNCH_ANGLE - FLIPPER_MIN_LAUNCH_ANGLE) * tipAmount;
        float incomingInfluence = clamp(velocityX / Math.max(1f, gx(DESIGN_WIDTH * 1.4f)), -0.12f, 0.12f);
        angleDegrees += incomingInfluence * 18f;

        float radians = (float) Math.toRadians(angleDegrees);
        float speed = gy(DESIGN_HEIGHT * (FLIPPER_LAUNCH_SPEED + tipAmount * 0.12f));
        float previousX = velocityX;
        float previousY = velocityY;
        velocityX = flipperSide * (float) Math.cos(radians) * speed + previousX * 0.16f;
        velocityY = -(float) Math.sin(radians) * speed + Math.min(0f, previousY) * 0.08f;
        ballRotation += flipperSide * (0.4f + tipAmount * 0.35f);
    }

    private void triggerGameOver() {
        if (gameOver) {
            return;
        }

        gameOver = true;
        velocityX = 0f;
        velocityY = 0f;
        if (gameOverListener != null) {
            gameOverListener.onGameOver();
        }
    }

    public void setGameOverListener(GameOverListener gameOverListener) {
        this.gameOverListener = gameOverListener;
    }

    private boolean isBallOnFlipperSurface() {
        return getBallFlipperContactT() >= 0f;
    }

    private float getBallFlipperContactT() {
        float[] pivot = getFlipperPivot();
        float[] end = getFlipperEndPoint();
        float segmentX = end[0] - pivot[0];
        float segmentY = end[1] - pivot[1];
        float lengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (lengthSquared <= 0.001f) {
            return -1f;
        }
        float t = ((ballX - pivot[0]) * segmentX + (ballY - pivot[1]) * segmentY) / lengthSquared;
        if (t < 0.05f || t > 0.96f) {
            return -1f;
        }
        t = clamp(t, 0f, 1f);
        float nearestX = pivot[0] + segmentX * t;
        float nearestY = pivot[1] + segmentY * t;
        float length = (float) Math.sqrt(lengthSquared);
        float normalX = -segmentY / length;
        float normalY = segmentX / length;
        if (normalY > 0f) {
            normalX = -normalX;
            normalY = -normalY;
        }
        float offsetX = ballX - nearestX;
        float offsetY = ballY - nearestY;
        float aboveSurface = offsetX * normalX + offsetY * normalY;
        float tangentDistance = Math.abs(offsetX * (segmentX / length) + offsetY * (segmentY / length));
        float maxSurfaceGap = getFlipperSurfaceLift() + ballRadius * 0.35f;
        return aboveSurface > 0f
                && aboveSurface <= maxSurfaceGap
                && tangentDistance <= ballRadius * 0.5f
                ? t
                : -1f;
    }

    private float[] getFlipperPivot() {
        float x = flipperSide > 0f ? gx(DESIGN_WIDTH * 0.55f) : gx(DESIGN_WIDTH * 0.45f);
        float y = gy(DESIGN_HEIGHT * 0.82f) + gs(FLIPPER_PIVOT_DOWN);
        return new float[] { x, y };
    }

    private float[] getFlipperEndPoint() {
        float[] pivot = getFlipperPivot();
        float length = gs(DESIGN_WIDTH * 0.42f * FLIPPER_SCALE);
        float radians = (float) Math.toRadians(flipperAngle);
        return new float[] {
                pivot[0] + (float) Math.cos(radians) * length,
                pivot[1] + (float) Math.sin(radians) * length
        };
    }

    private float getRestAngle() {
        return flipperSide > 0f ? 160f : 20f;
    }

    private float getActiveAngle() {
        return flipperSide > 0f ? 200f : -20f;
    }

    private float getFlipperVisualLength() {
        float[] pivot = getFlipperPivot();
        float[] end = getFlipperEndPoint();
        return distance(pivot[0], pivot[1], end[0], end[1]) * 1.18f;
    }

    private float getFlipperVisualHeight() {
        return getFlipperVisualLength() / FLIPPER_ASPECT;
    }

    private float getFlipperSurfaceLift() {
        return ballRadius + getFlipperVisualHeight() * FLIPPER_SURFACE_HEIGHT_RATIO;
    }

    private float getSideWallInnerX(float y, float side) {
        if (side > 0f) {
            return getRightWallInnerX(y);
        }
        return getLeftWallInnerX(y);
    }

    private float getSideWallSlope(float y, float side) {
        float sample = Math.max(gs(2f), gy(DESIGN_HEIGHT * 0.004f));
        if (side > 0f && isRightWallRampZone(y + sample)) {
            return 0f;
        }
        float top = getSideWallInnerX(y - sample, side);
        float bottom = getSideWallInnerX(y + sample, side);
        return (bottom - top) / (sample * 2f);
    }

    private float getRightWallInnerX(float y) {
        float sampledX = getRightWallAlphaInnerX(y);
        if (sampledX >= 0f) {
            return sampledX;
        }

        float wallWidth = getRightWallWidth();
        float straightX = viewWidth - wallWidth * 0.27f;
        float localY = y + getWallLift();
        float curveStart = gy(DESIGN_HEIGHT * 0.61f);
        float curveEnd = gy(DESIGN_HEIGHT * 0.89f);
        if (localY <= curveStart) {
            return straightX;
        }

        float progress = clamp((localY - curveStart) / Math.max(1f, curveEnd - curveStart), 0f, 1f);
        float smooth = progress * progress * progress * (progress * (progress * 6f - 15f) + 10f);
        float deepestInset = wallWidth * 0.82f;
        float curvedX = straightX - deepestInset * smooth;
        if (progress >= 1f) {
            float lowerProgress = clamp((localY - curveEnd) / Math.max(1f, gy(DESIGN_HEIGHT) - curveEnd), 0f, 1f);
            curvedX += wallWidth * 0.08f * lowerProgress;
        }
        return curvedX;
    }

    private boolean isRightWallRampZone(float y) {
        return y + getWallLift() >= gy(DESIGN_HEIGHT * RIGHT_WALL_RAMP_START_Y);
    }

    private float getRightWallRampYAt(float x) {
        if (rightWallBitmap == null || rightWallBitmap.isRecycled()) {
            return -1f;
        }

        float wallWidth = getRightWallWidth();
        float localX = x - (viewWidth - wallWidth);
        float leadInWidth = wallWidth * RIGHT_WALL_RAMP_LEAD_IN_WIDTH;
        if (localX < -leadInWidth || localX > wallWidth) {
            return -1f;
        }

        if (localX < 0f) {
            float entryYProgress = getRightWallRampEntryYProgress(wallWidth);
            float leadInDrop = (float) Math.tan(Math.toRadians(RIGHT_WALL_RAMP_ENTRY_SLOPE_DEGREES))
                    * -localX;
            return entryYProgress * gy(DESIGN_HEIGHT) - getWallLift() - gs(1f) + leadInDrop;
        }

        float localProgress = clamp(localX / Math.max(1f, wallWidth), 0f, 1f);
        int bitmapX = Math.round(localProgress * (rightWallBitmap.getWidth() - 1));
        int topMostOpaque = -1;
        for (int y = 0; y < rightWallBitmap.getHeight(); y++) {
            int alpha = (rightWallBitmap.getPixel(bitmapX, y) >>> 24) & 0xff;
            if (alpha > WALL_ALPHA_THRESHOLD) {
                topMostOpaque = y;
                break;
            }
        }
        if (topMostOpaque < 0) {
            return -1f;
        }

        float bitmapYProgress = topMostOpaque / (float) Math.max(1, rightWallBitmap.getHeight() - 1);
        if (localProgress < RIGHT_WALL_RAMP_SMOOTH_EXIT) {
            bitmapYProgress = getRightWallRampEntryYProgress(wallWidth)
                    - localProgress * wallWidth
                    * (float) Math.tan(Math.toRadians(RIGHT_WALL_RAMP_ENTRY_SLOPE_DEGREES))
                    / Math.max(1f, gy(DESIGN_HEIGHT));
        }
        if (bitmapYProgress < RIGHT_WALL_RAMP_START_Y) {
            return -1f;
        }
        return bitmapYProgress * gy(DESIGN_HEIGHT) - getWallLift() - gs(1f);
    }

    private float getRightWallRampEntryYProgress(float wallWidth) {
        float targetProgress = getRightWallRampAlphaYProgress(RIGHT_WALL_RAMP_SMOOTH_EXIT);
        if (targetProgress < RIGHT_WALL_RAMP_START_Y) {
            targetProgress = 0.835f;
        }
        float distanceToExit = RIGHT_WALL_RAMP_SMOOTH_EXIT * wallWidth;
        float slopeDrop = (float) Math.tan(Math.toRadians(RIGHT_WALL_RAMP_ENTRY_SLOPE_DEGREES))
                * distanceToExit;
        return targetProgress + slopeDrop / Math.max(1f, gy(DESIGN_HEIGHT));
    }

    private float getRightWallRampAlphaYProgress(float localProgress) {
        if (rightWallBitmap == null || rightWallBitmap.isRecycled()) {
            return -1f;
        }

        int bitmapX = Math.round(clamp(localProgress, 0f, 1f)
                * (rightWallBitmap.getWidth() - 1));
        for (int y = 0; y < rightWallBitmap.getHeight(); y++) {
            int alpha = (rightWallBitmap.getPixel(bitmapX, y) >>> 24) & 0xff;
            if (alpha > WALL_ALPHA_THRESHOLD) {
                return y / (float) Math.max(1, rightWallBitmap.getHeight() - 1);
            }
        }
        return -1f;
    }

    private float getRightWallAlphaInnerX(float y) {
        if (rightWallBitmap == null || rightWallBitmap.isRecycled()) {
            return -1f;
        }

        float wallWidth = getRightWallWidth();
        float localY = y + getWallLift();
        if (localY < 0f || localY > gy(DESIGN_HEIGHT)) {
            return -1f;
        }

        int bitmapY = Math.round(clamp(localY / Math.max(1f, gy(DESIGN_HEIGHT)), 0f, 1f)
                * (rightWallBitmap.getHeight() - 1));
        int leftMostOpaque = -1;
        for (int x = 0; x < rightWallBitmap.getWidth(); x++) {
            int alpha = (rightWallBitmap.getPixel(x, bitmapY) >>> 24) & 0xff;
            if (alpha > WALL_ALPHA_THRESHOLD) {
                leftMostOpaque = x;
                break;
            }
        }
        if (leftMostOpaque < 0 || leftMostOpaque == 0) {
            return -1f;
        }

        float bitmapProgress = leftMostOpaque / (float) Math.max(1, rightWallBitmap.getWidth() - 1);
        return viewWidth - wallWidth + bitmapProgress * wallWidth + gs(1.5f);
    }

    private float getLeftWallInnerX(float y) {
        float width = getLeftWallWidth();
        float bottom = gy(DESIGN_HEIGHT) - getWallLift();
        float bendHeight = gs(DESIGN_WIDTH * 0.16f);
        if (y > bottom - bendHeight) {
            float progress = clamp((y - (bottom - bendHeight)) / Math.max(1f, bendHeight), 0f, 1f);
            return width * (0.92f - progress * 0.22f);
        }
        return width * 0.92f;
    }

    private float getRightWallWidth() {
        return gy(DESIGN_HEIGHT) * RIGHT_WALL_ASPECT;
    }

    private float getLeftWallWidth() {
        return gy(DESIGN_HEIGHT) * LEFT_WALL_ASPECT;
    }

    private float getWallLift() {
        return gs(DESIGN_WIDTH * 0.11f);
    }

    private void updateScoreView() {
        if (scoreView != null) {
            scoreView.setText(String.valueOf(score));
        }
    }

    private float gravity() {
        return gy(DESIGN_HEIGHT * 1.72f);
    }

    private float randomBetween(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private float distance(float ax, float ay, float bx, float by) {
        return (float) Math.hypot(ax - bx, ay - by);
    }

    private float gx(float designX) {
        return designX * frameScale();
    }

    private float gy(float designY) {
        return designY * frameScale();
    }

    private float gs(float designValue) {
        return designValue * frameScale();
    }

    private float frameScale() {
        return viewWidth / DESIGN_WIDTH;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Hoop {
        final RectF backLayerRect = new RectF();
        final RectF frontLayerRect = new RectF();
        float openX;
        float openY;
        float side = 1f;
        float openingHalfWidth;
        float frontRimX;
        float frontRimTop;
        float frontRimBottom;
        boolean scored;
    }

    public interface GameOverListener {
        void onGameOver();
    }
}

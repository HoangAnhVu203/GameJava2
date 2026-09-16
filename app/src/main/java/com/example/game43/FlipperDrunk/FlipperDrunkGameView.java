package com.example.game43.FlipperDrunk;

import android.content.Context;
import android.graphics.Canvas;
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
    private static final int HOOPS_PER_TURN = 3;
    private static final float FLIP_SECONDS = 0.18f;
    private static final float FLIP_RETURN_SECONDS = 0.22f;
    private static final float DESIGN_WIDTH = 309f;
    private static final float DESIGN_HEIGHT = 669f;
    private static final float RIGHT_WALL_ASPECT = 648f / 2727f;
    private static final float LEFT_WALL_ASPECT = 88f / 2243f;
    private static final float FLIPPER_ASPECT = 584f / 339f;
    private static final float FLIPPER_SCALE = 0.75f;

    private final List<Hoop> hoops = new ArrayList<>();
    private final Random random = new Random();

    private FrameLayout stageView;
    private ImageView backgroundView;
    private ImageView rightWallView;
    private ImageView leftWallView;
    private ImageView hoopBackView;
    private ImageView hoopFrontView;
    private ImageView ballView;
    private ImageView flipperView;
    private TextView scoreView;

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

    private void resetGame() {
        if (viewWidth <= 0f || viewHeight <= 0f) {
            return;
        }

        score = 0;
        scoredHoopsThisTurn = 0;
        flipperSide = 1f;
        hoopSide = 1f;
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
        float lift = ballRadius + getFlipperThickness() * 0.22f;
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

        if (ballY - ballRadius > gy(DESIGN_HEIGHT + DESIGN_WIDTH * 0.08f)
                || ballX + ballRadius < -gx(DESIGN_WIDTH * 0.25f)
                || ballX - ballRadius > viewWidth + gx(DESIGN_WIDTH * 0.25f)) {
            placeBallOnFlipper();
        }
    }

    private void updateFlipper(float dt) {
        if (flipperTimer > 0f) {
            flipperTimer = Math.max(0f, flipperTimer - dt);
            if (flipperTimer <= 0f) {
                flipperTargetAngle = getRestAngle();
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

        float floorY = getFloorYAt(ballX);
        if (ballY + ballRadius > floorY) {
            ballY = floorY - ballRadius;
            if (velocityY > gs(DESIGN_WIDTH * 0.18f)) {
                velocityY = -velocityY * 0.34f;
            } else {
                velocityY = 0f;
            }
            velocityX *= 0.86f;
        }
    }

    private void resolveSideWallCollision(float side) {
        if (ballY > viewHeight - getWallLift() + ballRadius * 0.4f) {
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
            velocityX -= 1.72f * velocityAlongNormal * normalX;
            velocityY -= 1.72f * velocityAlongNormal * normalY;
        }
    }

    private void resolveFlipperCollision() {
        float[] pivot = getFlipperPivot();
        float[] end = getFlipperEndPoint();
        float radius = ballRadius + getFlipperThickness() * 0.45f;
        if (!resolveSegmentCollision(pivot[0], pivot[1], end[0], end[1], radius, 0.28f)) {
            return;
        }

        if (flipperTargetAngle == getActiveAngle()) {
            velocityX = flipperSide * gx(DESIGN_WIDTH * 0.78f);
            velocityY = -gy(DESIGN_HEIGHT * 1.02f);
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
                ? getRightWallInnerX(gy(DESIGN_HEIGHT * 0.32f)) - gs(DESIGN_WIDTH * 0.085f) + wallNudge
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
        hoop.frontRimX = hoop.openX - hoop.side * (hoop.openingHalfWidth + gx(35f));
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
        float length = distance(pivot[0], pivot[1], end[0], end[1]) * 1.18f;
        float height = length / FLIPPER_ASPECT;
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            activateFlipper();
            return true;
        }
        return true;
    }

    private void activateFlipper() {
        waitingForFirstTap = false;
        flipperTimer = FLIP_SECONDS;
        flipperTargetAngle = getActiveAngle();
        if (isBallNearFlipper()) {
            velocityX = flipperSide * gx(DESIGN_WIDTH * 0.78f);
            velocityY = -gy(DESIGN_HEIGHT * 1.02f);
        }
    }

    private boolean isBallNearFlipper() {
        float[] pivot = getFlipperPivot();
        float[] end = getFlipperEndPoint();
        float segmentX = end[0] - pivot[0];
        float segmentY = end[1] - pivot[1];
        float lengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (lengthSquared <= 0.001f) {
            return false;
        }
        float t = ((ballX - pivot[0]) * segmentX + (ballY - pivot[1]) * segmentY) / lengthSquared;
        t = clamp(t, 0f, 1f);
        float nearestX = pivot[0] + segmentX * t;
        float nearestY = pivot[1] + segmentY * t;
        float dx = ballX - nearestX;
        float dy = ballY - nearestY;
        float radius = ballRadius + getFlipperThickness() * 1.2f;
        return dx * dx + dy * dy <= radius * radius;
    }

    private float[] getFlipperPivot() {
        float x = flipperSide > 0f ? gx(DESIGN_WIDTH * 0.55f) : gx(DESIGN_WIDTH * 0.45f);
        float y = gy(DESIGN_HEIGHT * 0.82f);
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

    private float getFlipperThickness() {
        return gs(DESIGN_WIDTH * 0.085f * FLIPPER_SCALE);
    }

    private float getSideWallInnerX(float y, float side) {
        if (side > 0f) {
            return getRightWallInnerX(y);
        }
        return getLeftWallInnerX(y);
    }

    private float getSideWallSlope(float y, float side) {
        float sample = Math.max(gs(2f), gy(DESIGN_HEIGHT * 0.004f));
        float top = getSideWallInnerX(y - sample, side);
        float bottom = getSideWallInnerX(y + sample, side);
        return (bottom - top) / (sample * 2f);
    }

    private float getRightWallInnerX(float y) {
        float wallWidth = getRightWallWidth();
        float straightX = viewWidth - wallWidth * 0.34f;
        float localY = y + getWallLift();
        float curveStart = gy(DESIGN_HEIGHT * 0.66f);
        float curveEnd = gy(DESIGN_HEIGHT * 0.91f);
        if (localY <= curveStart) {
            return straightX;
        }

        float progress = clamp((localY - curveStart) / Math.max(1f, curveEnd - curveStart), 0f, 1f);
        float smooth = progress * progress * (3f - 2f * progress);
        float deepestInset = wallWidth * 0.74f;
        float curvedX = straightX - deepestInset * smooth;
        if (progress >= 1f) {
            float lowerProgress = clamp((localY - curveEnd) / Math.max(1f, gy(DESIGN_HEIGHT) - curveEnd), 0f, 1f);
            curvedX += wallWidth * 0.12f * lowerProgress;
        }
        return curvedX;
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

    private float getFloorYAt(float x) {
        float base = gy(DESIGN_HEIGHT * 0.92f);
        float curve = (float) Math.cos((x / Math.max(1f, gx(DESIGN_WIDTH))) * Math.PI) * gs(DESIGN_WIDTH * 0.028f);
        return base + curve;
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
}

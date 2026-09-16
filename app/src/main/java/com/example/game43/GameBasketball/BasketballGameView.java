package com.example.game43.GameBasketball;

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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Random;

public class BasketballGameView extends View {
    private static final float BALL_START_X = 0.22f;
    private static final float HOOP_DISTANCE = 0.78f;
    private static final float TRAIL_LIFETIME_SECONDS = 0.22f;
    private static final float TRAIL_SAMPLE_SECONDS = 0.028f;
    private static final float ROUND_SECONDS = 30f;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scoreShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint resetTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint resetTextShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint skyPaint = new Paint();
    private final RectF drawRect = new RectF();
    private final RectF timerFrameRect = new RectF();
    private final RectF timerFillRect = new RectF();
    private final RectF timerFillClipRect = new RectF();
    private final RectF resetButtonRect = new RectF();
    private final ArrayDeque<TrailPoint> ballTrail = new ArrayDeque<>();
    private final Random random = new Random();
    private final Hoop hoop = new Hoop();

    private Bitmap backgroundBitmap;
    private Bitmap ballBitmap;
    private Bitmap backboardBitmap;
    private Bitmap hoopBackBitmap;
    private Bitmap hoopFrontBitmap;
    private Bitmap timerFrameBitmap;
    private Bitmap timerFillBitmap;
    private Bitmap resetButtonBitmap;

    private float viewWidth;
    private float viewHeight;
    private float minSide;
    private float floorY;
    private float ballRadius;
    private float ballX;
    private float ballY;
    private float previousBallY;
    private float velocityX;
    private float velocityY;
    private float direction = 1f;
    private float cameraX;
    private float targetCameraX;
    private float ballRotation;
    private float trailSampleTimer;
    private float timeRemaining = ROUND_SECONDS;
    private long lastFrameNanos;
    private ExtraTimeRequestListener extraTimeRequestListener;
    private int score;
    private boolean hasStarted;
    private boolean scoredCurrentHoop;

    public BasketballGameView(Context context) {
        super(context);
        init();
    }

    public BasketballGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BasketballGameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(true);
        bitmapPaint.setDither(true);
        bitmapPaint.setFilterBitmap(true);

        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.basketball_court_background);
        ballBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.basketball_ball);
        backboardBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.basketball_backboard);
        hoopBackBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.basketball_hoop_back_layer);
        hoopFrontBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.basketball_hoop_front_layer);
        timerFrameBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.duoii);
        timerFillBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.trong);
        resetButtonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.buttn);

        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setFakeBoldText(true);

        scoreShadowPaint.setColor(Color.argb(120, 0, 0, 0));
        scoreShadowPaint.setTextAlign(Paint.Align.CENTER);
        scoreShadowPaint.setFakeBoldText(true);

        shadowPaint.setColor(Color.argb(70, 0, 0, 0));

        resetTextPaint.setColor(Color.WHITE);
        resetTextPaint.setTextAlign(Paint.Align.CENTER);
        resetTextPaint.setFakeBoldText(true);

        resetTextShadowPaint.setColor(Color.argb(120, 95, 45, 0));
        resetTextShadowPaint.setTextAlign(Paint.Align.CENTER);
        resetTextShadowPaint.setFakeBoldText(true);
    }

    public void setExtraTimeRequestListener(ExtraTimeRequestListener extraTimeRequestListener) {
        this.extraTimeRequestListener = extraTimeRequestListener;
    }

    public void grantExtraTime() {
        resetPlayTimer();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        configureSize(w, h);
    }

    private void configureSize(int w, int h) {
        viewWidth = w;
        viewHeight = h;
        minSide = Math.min(viewWidth, viewHeight);
        skyPaint.setShader(new LinearGradient(0f, 0f, 0f, viewHeight,
                Color.rgb(139, 210, 255),
                Color.rgb(247, 251, 255),
                Shader.TileMode.CLAMP));
        resetGame();
    }

    private void resetGame() {
        if (viewWidth <= 0f || viewHeight <= 0f) {
            return;
        }

        score = 0;
        hasStarted = false;
        direction = 1f;
        velocityX = 0f;
        velocityY = 0f;
        cameraX = 0f;
        targetCameraX = 0f;
        ballRotation = 0f;
        trailSampleTimer = 0f;
        resetPlayTimer();
        ballTrail.clear();
        floorY = viewHeight - minSide * 0.12f;
        ballRadius = minSide * 0.055f;
        ballX = viewWidth * BALL_START_X;
        ballY = floorY - ballRadius;
        previousBallY = ballY;
        placeHoop(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ensureSizeForPreview();

        if (isInEditMode()) {
            drawScene(canvas);
            return;
        }

        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
        }
        float dt = Math.min(0.033f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;

        updateGame(dt);
        drawScene(canvas);
        postInvalidateOnAnimation();
    }

    private void ensureSizeForPreview() {
        int currentWidth = getWidth();
        int currentHeight = getHeight();
        if ((viewWidth <= 0f || viewHeight <= 0f) && currentWidth > 0 && currentHeight > 0) {
            configureSize(currentWidth, currentHeight);
        }
    }

    private void updateGame(float dt) {
        if (viewWidth <= 0f || viewHeight <= 0f) {
            return;
        }

        previousBallY = ballY;
        if (timeRemaining > 0f) {
            timeRemaining = Math.max(0f, timeRemaining - dt);
        }

        if (hasStarted || Math.abs(velocityY) > 0.01f || Math.abs(velocityX) > 0.01f) {
            velocityY += gravity() * dt;
            ballX += velocityX * dt;
            ballY += velocityY * dt;
            ballRotation += velocityX * dt * 0.35f;

            if (ballY + ballRadius >= floorY) {
                float impactVelocityY = velocityY;
                ballY = floorY - ballRadius;
                velocityX *= 0.82f;
                if (impactVelocityY > viewHeight * 0.24f) {
                    velocityY = -impactVelocityY * 0.34f;
                    hasStarted = true;
                } else {
                    velocityY = 0f;
                }

                if (velocityY == 0f && Math.abs(velocityX) < travelSpeed() * 0.18f) {
                    velocityX = 0f;
                    hasStarted = false;
                }
            }

            resolveBackboardCollision();
            resolveRimCollision();
            if (wrapBallAtScreenEdges()) {
                ballTrail.clear();
            } else {
                updateBallTrail(dt);
            }
        } else {
            ballTrail.clear();
        }

        checkScore();
        updateCamera(dt);
    }

    private void checkScore() {
        if (scoredCurrentHoop) {
            return;
        }

        boolean crossedRim = previousBallY < hoop.openY && ballY >= hoop.openY;
        boolean falling = velocityY > 0f;
        boolean insideHoop = Math.abs(ballX - hoop.openX) <= hoop.openingHalfWidth;
        if (crossedRim && falling && insideHoop) {
            score++;
            scoredCurrentHoop = true;
            direction *= -1f;
            velocityX = direction * travelSpeed();
            velocityY = Math.min(velocityY, -viewHeight * 0.34f);
            placeHoop(false);
        }
    }

    private void updateCamera(float dt) {
        cameraX += (targetCameraX - cameraX) * Math.min(1f, dt * 4.8f);
    }

    private void drawScene(Canvas canvas) {
        drawBackground(canvas);

        canvas.save();
        canvas.translate(-cameraX, 0f);
        drawHoopBack(canvas);
        drawBallShadow(canvas);
        drawBallTrail(canvas);
        drawBall(canvas);
        drawHoopFront(canvas);
        canvas.restore();

        drawScore(canvas);
        drawTimerHud(canvas);
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, skyPaint);
        if (backgroundBitmap == null) {
            return;
        }

        float scale = Math.max(viewHeight / backgroundBitmap.getHeight(), viewWidth / backgroundBitmap.getWidth());
        float bgWidth = backgroundBitmap.getWidth() * scale;
        float bgHeight = backgroundBitmap.getHeight() * scale;
        float offsetX = -positiveModulo(cameraX * 0.22f, bgWidth);
        for (float x = offsetX - bgWidth; x < viewWidth + bgWidth; x += bgWidth) {
            drawRect.set(x, viewHeight - bgHeight, x + bgWidth, viewHeight);
            canvas.drawBitmap(backgroundBitmap, null, drawRect, bitmapPaint);
        }
    }

    private void drawHoopBack(Canvas canvas) {
        drawBitmapMaybeFlipped(canvas, backboardBitmap, hoop.backboardRect, hoop.side < 0f);
        drawBitmapMaybeFlipped(canvas, hoopBackBitmap, hoop.backLayerRect, hoop.side < 0f);
    }

    private void drawHoopFront(Canvas canvas) {
        drawBitmapMaybeFlipped(canvas, hoopFrontBitmap, hoop.frontLayerRect, hoop.side < 0f);
    }

    private void drawBallShadow(Canvas canvas) {
        float heightAboveFloor = Math.max(0f, floorY - (ballY + ballRadius));
        float shadowScale = clamp(1f - heightAboveFloor / (viewHeight * 0.45f), 0.35f, 1f);
        float shadowWidth = ballRadius * 2.1f * shadowScale;
        float shadowHeight = ballRadius * 0.42f * shadowScale;
        drawRect.set(ballX - shadowWidth * 0.5f, floorY - shadowHeight * 0.5f,
                ballX + shadowWidth * 0.5f, floorY + shadowHeight * 0.5f);
        canvas.drawOval(drawRect, shadowPaint);
    }

    private void drawBall(Canvas canvas) {
        drawRect.set(ballX - ballRadius, ballY - ballRadius, ballX + ballRadius, ballY + ballRadius);
        canvas.save();
        canvas.rotate(ballRotation, ballX, ballY);
        canvas.drawBitmap(ballBitmap, null, drawRect, bitmapPaint);
        canvas.restore();
    }

    private void drawBallTrail(Canvas canvas) {
        if (ballTrail.isEmpty() || ballBitmap == null) {
            return;
        }

        int originalAlpha = bitmapPaint.getAlpha();
        Iterator<TrailPoint> iterator = ballTrail.descendingIterator();
        while (iterator.hasNext()) {
            TrailPoint trailPoint = iterator.next();
            float lifePercent = 1f - trailPoint.age / TRAIL_LIFETIME_SECONDS;
            if (lifePercent <= 0f) {
                continue;
            }

            float scale = 0.55f + lifePercent * 0.35f;
            float radius = ballRadius * scale;
            bitmapPaint.setAlpha((int) (95f * lifePercent));
            drawRect.set(trailPoint.x - radius, trailPoint.y - radius,
                    trailPoint.x + radius, trailPoint.y + radius);
            canvas.drawBitmap(ballBitmap, null, drawRect, bitmapPaint);
        }
        bitmapPaint.setAlpha(originalAlpha);
    }

    private void drawScore(Canvas canvas) {
        float textSize = minSide * 0.12f;
        float y = Math.max(textSize * 1.45f, viewHeight * 0.17f);
        scorePaint.setTextSize(textSize);
        scoreShadowPaint.setTextSize(textSize);
        String scoreText = String.valueOf(score);
        canvas.drawText(scoreText, viewWidth * 0.5f + minSide * 0.008f, y + minSide * 0.008f, scoreShadowPaint);
        canvas.drawText(scoreText, viewWidth * 0.5f, y, scorePaint);
    }

    private void drawTimerHud(Canvas canvas) {
        float top = dp(12f);
        float resetHeight = dp(42f);
        float resetWidth = resetHeight * 765f / 222f;
        float sidePadding = dp(16f);
        float frameHeight = dp(38f);
        float frameWidth = Math.min(viewWidth * 0.58f, dp(240f));
        float frameLeft = (viewWidth - frameWidth) * 0.5f;

        timerFrameRect.set(frameLeft, top, frameLeft + frameWidth, top + frameHeight);
        timerFillRect.set(
                timerFrameRect.left + frameWidth * 0.055f,
                timerFrameRect.top + frameHeight * 0.26f,
                timerFrameRect.right - frameWidth * 0.055f,
                timerFrameRect.bottom - frameHeight * 0.26f
        );
        resetButtonRect.set(
                viewWidth - sidePadding - resetWidth,
                top - dp(2f),
                viewWidth - sidePadding,
                top - dp(2f) + resetHeight
        );

        if (timerFrameBitmap != null) {
            canvas.drawBitmap(timerFrameBitmap, null, timerFrameRect, bitmapPaint);
        }
        drawTimerFill(canvas);
        drawResetButton(canvas);
    }

    private void drawTimerFill(Canvas canvas) {
        if (timerFillBitmap == null) {
            return;
        }

        float progress = timeRemaining / ROUND_SECONDS;
        timerFillClipRect.set(timerFillRect);
        timerFillClipRect.right = timerFillRect.left + timerFillRect.width() * progress;

        canvas.save();
        canvas.clipRect(timerFillClipRect);
        canvas.drawBitmap(timerFillBitmap, null, timerFillRect, bitmapPaint);
        canvas.restore();
    }

    private void drawResetButton(Canvas canvas) {
        if (resetButtonBitmap != null) {
            canvas.drawBitmap(resetButtonBitmap, null, resetButtonRect, bitmapPaint);
        }

        float textSize = resetButtonRect.height() * 0.36f;
        resetTextPaint.setTextSize(textSize);
        resetTextShadowPaint.setTextSize(textSize);
        float textX = resetButtonRect.left + resetButtonRect.width() * 0.58f;
        float textY = resetButtonRect.centerY() - (resetTextPaint.descent() + resetTextPaint.ascent()) * 0.5f;
        canvas.drawText("+30s", textX + dp(1.5f), textY + dp(1.5f), resetTextShadowPaint);
        canvas.drawText("+30s", textX, textY, resetTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return true;
        }

        if (resetButtonRect.contains(event.getX(), event.getY())) {
            requestExtraTime();
            return true;
        }

        if (timeRemaining <= 0f) {
            return true;
        }

        jumpBall();
        return true;
    }

    private void resetPlayTimer() {
        timeRemaining = ROUND_SECONDS;
    }

    private void requestExtraTime() {
        if (extraTimeRequestListener != null) {
            extraTimeRequestListener.onExtraTimeRequested();
            return;
        }
        grantExtraTime();
    }

    private void jumpBall() {
        if (isBallOnFloor()) {
            velocityY = -viewHeight * 0.86f;
            velocityX = direction * travelSpeed();
            hasStarted = true;
        } else {
            velocityY = Math.min(velocityY, -viewHeight * 0.98f);
            velocityX = direction * travelSpeed();
        }
    }

    private void updateBallTrail(float dt) {
        Iterator<TrailPoint> iterator = ballTrail.iterator();
        while (iterator.hasNext()) {
            TrailPoint trailPoint = iterator.next();
            trailPoint.age += dt;
            if (trailPoint.age >= TRAIL_LIFETIME_SECONDS) {
                iterator.remove();
            }
        }

        if (isBallOnFloor()) {
            ballTrail.clear();
            trailSampleTimer = 0f;
            return;
        }

        trailSampleTimer += dt;
        if (trailSampleTimer < TRAIL_SAMPLE_SECONDS) {
            return;
        }

        trailSampleTimer = 0f;
        ballTrail.addFirst(new TrailPoint(ballX, ballY));
        while (ballTrail.size() > 10) {
            ballTrail.removeLast();
        }
    }

    private boolean wrapBallAtScreenEdges() {
        float leftEdge = cameraX;
        float rightEdge = cameraX + viewWidth;

        if (velocityX > 0f && ballX - ballRadius > rightEdge) {
            ballX = leftEdge - ballRadius;
            return true;
        } else if (velocityX < 0f && ballX + ballRadius < leftEdge) {
            ballX = rightEdge + ballRadius;
            return true;
        }
        return false;
    }

    private void resolveBackboardCollision() {
        RectF board = hoop.backboardRect;
        float nearestX = clamp(ballX, board.left, board.right);
        float nearestY = clamp(ballY, board.top, board.bottom);
        float dx = ballX - nearestX;
        float dy = ballY - nearestY;
        float distanceSquared = dx * dx + dy * dy;
        float radiusSquared = ballRadius * ballRadius;

        if (distanceSquared >= radiusSquared) {
            return;
        }

        float normalX;
        float normalY;
        float distance;
        if (distanceSquared > 0.001f) {
            distance = (float) Math.sqrt(distanceSquared);
            normalX = dx / distance;
            normalY = dy / distance;
        } else {
            distance = 0f;
            normalX = hoop.side > 0f ? -1f : 1f;
            normalY = 0f;
        }

        float overlap = ballRadius - distance;
        ballX += normalX * (overlap + 1f);
        ballY += normalY * (overlap + 1f);

        float velocityAlongNormal = velocityX * normalX + velocityY * normalY;
        if (velocityAlongNormal < 0f) {
            float bounce = 0.65f;
            velocityX -= (1f + bounce) * velocityAlongNormal * normalX;
            velocityY -= (1f + bounce) * velocityAlongNormal * normalY;
        }
    }

    private void resolveRimCollision() {
        resolveBallSegmentCollision(
                hoop.frontRimX,
                hoop.frontRimTop,
                hoop.frontRimX,
                hoop.frontRimBottom,
                -hoop.side,
                0f,
                0.55f
        );
    }

    private void resolveBallSegmentCollision(float startX, float startY, float endX, float endY,
                                             float fallbackNormalX, float fallbackNormalY, float bounce) {
        float segmentX = endX - startX;
        float segmentY = endY - startY;
        float lengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (lengthSquared <= 0.001f) {
            return;
        }

        float t = ((ballX - startX) * segmentX + (ballY - startY) * segmentY) / lengthSquared;
        t = clamp(t, 0f, 1f);

        float nearestX = startX + segmentX * t;
        float nearestY = startY + segmentY * t;
        float dx = ballX - nearestX;
        float dy = ballY - nearestY;
        float distanceSquared = dx * dx + dy * dy;
        float radiusSquared = ballRadius * ballRadius;

        if (distanceSquared >= radiusSquared) {
            return;
        }

        float normalX;
        float normalY;
        float distance;
        if (distanceSquared > 0.001f) {
            distance = (float) Math.sqrt(distanceSquared);
            normalX = dx / distance;
            normalY = dy / distance;
        } else {
            distance = 0f;
            normalX = fallbackNormalX;
            normalY = fallbackNormalY;
        }

        float overlap = ballRadius - distance;
        ballX += normalX * (overlap + 1f);
        ballY += normalY * (overlap + 1f);

        float velocityAlongNormal = velocityX * normalX + velocityY * normalY;
        if (velocityAlongNormal < 0f) {
            velocityX -= (1f + bounce) * velocityAlongNormal * normalX;
            velocityY -= (1f + bounce) * velocityAlongNormal * normalY;
        }
    }

    private boolean isBallOnFloor() {
        return ballY + ballRadius >= floorY - minSide * 0.006f;
    }

    private void placeHoop(boolean firstHoop) {
        float distance = viewWidth * HOOP_DISTANCE;
        hoop.side = direction;
        hoop.openX = firstHoop ? viewWidth * 0.84f : ballX + direction * distance;
        hoop.openY = chooseHoopHeight();
        layoutHoop();
        if (!firstHoop) {
            targetCameraX = (ballX + hoop.openX) * 0.5f - viewWidth * 0.5f;
        }
        scoredCurrentHoop = false;
    }

    private float chooseHoopHeight() {
        float high = viewHeight * 0.30f;
        float middle = viewHeight * 0.42f;
        float low = Math.min(viewHeight * 0.55f, floorY - ballRadius * 4.5f);
        float[] lanes = { high, middle, low };
        float selected = lanes[random.nextInt(lanes.length)];
        return clamp(selected, viewHeight * 0.24f, floorY - ballRadius * 4f);
    }

    private void layoutHoop() {
        float frontWidth = minSide * 0.225f;
        float frontHeight = frontWidth * 80f / 96f;
        float backWidth = frontWidth * 112f / 96f;
        float backHeight = frontWidth * 30f / 96f;
        float hoopWidth = Math.max(frontWidth, backWidth);
        float boardWidth = frontWidth * 62f / 96f;
        float boardHeight = frontWidth * 216f / 96f;
        float backTop = hoop.openY - backHeight * 0.55f;
        float backLeft = hoop.openX - backWidth * 0.5f;
        float frontTop = backTop + backHeight - frontWidth * 16f / 96f;
        float frontOffsetX = -hoop.side * frontWidth * 9f / 96f + hoop.side * dp(3f);
        float boardTop = hoop.openY - boardHeight * 0.5f;
        float boardOverlap = frontWidth * 42f / 96f;
        float boardNearEdgeOffset = backWidth * 0.5f - boardOverlap;

        hoop.openingHalfWidth = Math.max(ballRadius * 1.35f, hoopWidth * 0.25f);
        hoop.backLayerRect.set(backLeft,
                backTop,
                backLeft + backWidth,
                backTop + backHeight);
        hoop.frontLayerRect.set(hoop.openX - frontWidth * 0.5f + frontOffsetX,
                frontTop,
                hoop.openX + frontWidth * 0.5f + frontOffsetX,
                frontTop + frontHeight);
        float rimSegmentHeight = Math.max(ballRadius * 0.5f, frontWidth * 12f / 96f);
        hoop.frontRimX = hoop.openX - hoop.side * (hoop.openingHalfWidth + dp(35f));
        hoop.frontRimTop = hoop.openY - rimSegmentHeight * 0.5f;
        hoop.frontRimBottom = hoop.openY + rimSegmentHeight * 0.5f;

        if (hoop.side > 0f) {
            float boardLeft = hoop.openX + boardNearEdgeOffset;
            hoop.backboardRect.set(boardLeft, boardTop,
                    boardLeft + boardWidth, boardTop + boardHeight);
        } else {
            float boardRight = hoop.openX - boardNearEdgeOffset;
            hoop.backboardRect.set(boardRight - boardWidth, boardTop,
                    boardRight, boardTop + boardHeight);
        }
    }

    private void drawBitmapMaybeFlipped(Canvas canvas, Bitmap bitmap, RectF dst, boolean flipHorizontal) {
        if (bitmap == null) {
            return;
        }

        if (!flipHorizontal) {
            canvas.drawBitmap(bitmap, null, dst, bitmapPaint);
            return;
        }

        canvas.save();
        canvas.scale(-1f, 1f, dst.centerX(), dst.centerY());
        canvas.drawBitmap(bitmap, null, dst, bitmapPaint);
        canvas.restore();
    }

    private float gravity() {
        return viewHeight * 1.68f;
    }

    private float travelSpeed() {
        return viewWidth * 0.42f;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float positiveModulo(float value, float modulo) {
        float result = value % modulo;
        return result < 0f ? result + modulo : result;
    }

    private static final class Hoop {
        final RectF backboardRect = new RectF();
        final RectF backLayerRect = new RectF();
        final RectF frontLayerRect = new RectF();
        float openX;
        float openY;
        float side = 1f;
        float openingHalfWidth;
        float frontRimX;
        float frontRimTop;
        float frontRimBottom;
    }

    private static final class TrailPoint {
        final float x;
        final float y;
        float age;

        TrailPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public interface ExtraTimeRequestListener {
        void onExtraTimeRequested();
    }
}

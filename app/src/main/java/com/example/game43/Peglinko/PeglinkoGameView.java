package com.example.game43.Peglinko;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.util.Random;

public class PeglinkoGameView extends View {
    private static final int PEG_COUNT = 20;
    private static final float MAX_FRAME_STEP = 1f / 30f;
    private static final float PHYSICS_STEP = 1f / 120f;

    private final Paint pegPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pegTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint meterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint layerPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Random random = new Random();

    private final float[] pegX = new float[PEG_COUNT];
    private final float[] pegY = new float[PEG_COUNT];
    private final int[] pegValue = new int[PEG_COUNT];
    private final String[] pegLabel = new String[PEG_COUNT];
    private final boolean[] pegHit = new boolean[PEG_COUNT];
    private final RectF frameRect = new RectF();
    private final RectF launcherRect = new RectF();
    private final RectF drawRect = new RectF();
    private Bitmap pegLayerBitmap;
    private Canvas pegLayerCanvas;
    private ImageView ballImageView;

    private float density;
    private float frameCornerRadius;
    private float pegRadius;
    private float ballRadius;
    private float launchX;
    private float launchY;
    private float ballX;
    private float ballY;
    private float velocityX;
    private float velocityY;
    private float stallAnchorX;
    private float stallAnchorY;
    private float stallTime;
    private float pullStartY;
    private float power;
    private int coins;
    private long lastFrameNanos;
    private boolean geometryReady;
    private boolean aiming;
    private boolean ballMoving;
    private boolean ballReady;
    private boolean paused;
    private boolean launcherGateClosed;
    private boolean physicsFramePosted;
    private boolean pegLayerDirty;
    private boolean coinsDirty;
    private OnCoinsChangedListener coinsChangedListener;
    private BallLifecycleListener ballLifecycleListener;
    private final Runnable physicsFrame = new Runnable() {
        @Override
        public void run() {
            physicsFramePosted = false;
            if (!ballMoving || paused) {
                return;
            }
            updatePhysics();
            updateBallImagePosition();
            schedulePhysicsFrame();
        }
    };

    public PeglinkoGameView(Context context) {
        this(context, null);
    }

    public PeglinkoGameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        pegPaint.setColor(Color.rgb(38, 55, 58));
        pegTextPaint.setColor(Color.WHITE);
        pegTextPaint.setTextAlign(Paint.Align.CENTER);
        pegTextPaint.setFakeBoldText(true);
        aimPaint.setColor(Color.argb(150, 255, 255, 255));
        aimPaint.setStrokeWidth(2f * density);
        aimPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        float frameHeight = height;
        frameRect.set(0f, 0f, width, height);
        frameCornerRadius = width * 0.255f;
        pegRadius = width * 0.043f;
        ballRadius = width * 0.041f;

        float launcherWidth = width * 0.115f;
        launcherRect.set(width - launcherWidth - width * 0.012f,
                frameRect.top + frameHeight * 0.47f,
                width - width * 0.012f,
                frameRect.bottom);
        launchX = launcherRect.centerX();
        launchY = frameRect.bottom - ballRadius - width * 0.018f;
        ballX = launchX;
        ballY = launchY;
        resizeBallImage();
        updateBallImagePosition();
        if (ballImageView != null) {
            ballImageView.post(() -> {
                resizeBallImage();
                updateBallImagePosition();
                ballImageView.requestLayout();
            });
        }

        int index = 0;
        for (int row = 0; row < 5; row++) {
            float rowOffset = row % 2 == 0 ? 0f : width * 0.04f;
            for (int column = 0; column < 4; column++) {
                pegX[index] = width * (0.14f + column * 0.18f) + rowOffset;
                pegY[index] = frameRect.top + frameHeight * (0.22f + row * 0.135f);
                pegValue[index] = random.nextInt(30) + 1;
                pegLabel[index] = String.valueOf(pegValue[index]);
                index++;
            }
        }
        pegTextPaint.setTextSize(Math.max(10f * density, pegRadius * 0.9f));
        if (pegLayerBitmap != null) {
            pegLayerBitmap.recycle();
        }
        pegLayerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        pegLayerCanvas = new Canvas(pegLayerBitmap);
        rebuildPegLayer();
        geometryReady = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!geometryReady) {
            return;
        }

        drawPegs(canvas);

        if (aiming) {
            drawAimAndPower(canvas);
        }
    }

    private void drawPegs(Canvas canvas) {
        canvas.drawBitmap(pegLayerBitmap, 0f, 0f, layerPaint);
    }

    private void rebuildPegLayer() {
        if (pegLayerCanvas == null) {
            return;
        }
        pegLayerDirty = false;
        pegLayerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        Paint.FontMetrics metrics = pegTextPaint.getFontMetrics();
        float baselineOffset = -(metrics.ascent + metrics.descent) * 0.5f;
        for (int i = 0; i < PEG_COUNT; i++) {
            pegPaint.setColor(pegHit[i] ? Color.rgb(63, 103, 107) : Color.rgb(38, 55, 58));
            pegLayerCanvas.drawCircle(pegX[i], pegY[i], pegRadius, pegPaint);
            pegLayerCanvas.drawText(pegLabel[i], pegX[i], pegY[i] + baselineOffset, pegTextPaint);
        }
    }

    private void drawAimAndPower(Canvas canvas) {
        float previewLength = getWidth() * (0.18f + 0.2f * power);
        canvas.drawLine(launchX, launchY,
                launchX,
                launchY - previewLength, aimPaint);

        int segments = 10;
        float gap = 2f * density;
        float segmentHeight = (launcherRect.height() - gap * (segments + 1)) / segments;
        float left = launcherRect.left - 13f * density;
        float right = launcherRect.left - 4f * density;
        int active = Math.round(power * segments);
        for (int i = 0; i < segments; i++) {
            float bottom = launcherRect.bottom - gap - i * (segmentHeight + gap);
            drawRect.set(left, bottom - segmentHeight, right, bottom);
            if (i >= active) {
                meterPaint.setColor(Color.argb(80, 255, 255, 255));
            } else if (i < 4) {
                meterPaint.setColor(Color.rgb(65, 215, 105));
            } else if (i < 7) {
                meterPaint.setColor(Color.rgb(255, 215, 55));
            } else {
                meterPaint.setColor(Color.rgb(244, 75, 67));
            }
            canvas.drawRoundRect(drawRect, 2f * density, 2f * density, meterPaint);
        }
    }

    private void updatePhysics() {
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return;
        }
        float elapsed = Math.min(MAX_FRAME_STEP, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;
        int steps = Math.max(1, (int) Math.ceil(elapsed / PHYSICS_STEP));
        float dt = elapsed / steps;
        for (int i = 0; i < steps && ballMoving; i++) {
            stepPhysics(dt);
        }
        flushFrameChanges();
    }

    private void flushFrameChanges() {
        if (pegLayerDirty) {
            rebuildPegLayer();
            invalidate();
        }
        if (coinsDirty) {
            coinsDirty = false;
            if (coinsChangedListener != null) {
                coinsChangedListener.onCoinsChanged(coins);
            }
        }
    }

    private void stepPhysics(float dt) {
        float gravity = getHeight() * 1.9f;
        velocityY += gravity * dt;
        float airDrag = Math.max(0f, 1f - 0.15f * dt);
        velocityX *= airDrag;
        velocityY *= airDrag;
        float previousX = ballX;
        float previousY = ballY;
        ballX += velocityX * dt;
        ballY += velocityY * dt;

        if (!launcherGateClosed
                && ballY + ballRadius < launcherRect.top - 2f * density) {
            launcherGateClosed = true;
        }
        collideWithWalls(previousX, previousY);
        collideWithPegs();
        resolveStall(dt);

        if (ballY + ballRadius >= frameRect.bottom) {
            finishShot();
        }
    }

    private void collideWithWalls(float previousX, float previousY) {
        float restitution = 0.79f;
        float inset = ballRadius + 3f * density;
        float left = frameRect.left + inset;
        float right = frameRect.right - inset;
        float top = frameRect.top + inset;
        float leftCornerX = frameRect.left + frameCornerRadius;
        float rightCornerX = frameRect.right - frameCornerRadius;
        float cornerY = frameRect.top + frameCornerRadius;
        float innerCornerRadius = frameCornerRadius - inset;

        if (ballX < leftCornerX && ballY < cornerY) {
            collideWithRoundedCorner(leftCornerX, cornerY, innerCornerRadius, restitution);
        } else if (ballX > rightCornerX && ballY < cornerY) {
            collideWithRoundedCorner(rightCornerX, cornerY, innerCornerRadius, restitution);
        } else if (ballY < top) {
            ballY = top;
            velocityY = Math.abs(velocityY) * restitution;
        }

        if (ballX < left) {
            ballX = left;
            velocityX = Math.abs(velocityX) * restitution;
        } else if (ballX > right) {
            ballX = right;
            velocityX = -Math.abs(velocityX) * restitution;
        }

        collideWithLauncherGate(previousY, restitution);
        collideWithLauncherDivider(previousX, restitution);
    }

    private void collideWithLauncherGate(float previousY, float restitution) {
        if (!launcherGateClosed || ballX + ballRadius < launcherRect.left) {
            return;
        }

        float gateY = launcherRect.top - ballRadius - 2f * density;
        if (previousY <= gateY && ballY > gateY && velocityY > 0f) {
            ballY = gateY;
            velocityY = -Math.abs(velocityY) * restitution;
            velocityX = Math.min(velocityX, -getWidth() * 0.32f);
        }
    }

    private void collideWithRoundedCorner(float centerX, float centerY,
                                          float radius, float restitution) {
        float dx = ballX - centerX;
        float dy = ballY - centerY;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared <= radius * radius) {
            return;
        }

        float distance = (float) Math.sqrt(distanceSquared);
        float outwardX = dx / distance;
        float outwardY = dy / distance;
        ballX = centerX + outwardX * radius;
        ballY = centerY + outwardY * radius;
        reflectVelocity(-outwardX, -outwardY, restitution);
    }

    private void collideWithLauncherDivider(float previousX, float restitution) {
        float dividerX = launcherRect.left;
        float dividerTop = launcherRect.top;
        float collisionPadding = 2f * density;
        if (ballY + ballRadius < dividerTop
                || ballY - ballRadius > launcherRect.bottom) {
            return;
        }

        float minimumDistance = ballRadius + collisionPadding;
        if (previousX >= dividerX && ballX < dividerX + minimumDistance) {
            ballX = dividerX + minimumDistance;
            velocityX = Math.abs(velocityX) * restitution;
        } else if (previousX < dividerX && ballX > dividerX - minimumDistance) {
            ballX = dividerX - minimumDistance;
            velocityX = -Math.abs(velocityX) * restitution;
        }
    }

    private void collideWithPegs() {
        float minimumDistance = ballRadius + pegRadius;
        float minimumDistanceSquared = minimumDistance * minimumDistance;
        for (int i = 0; i < PEG_COUNT; i++) {
            float dx = ballX - pegX[i];
            float dy = ballY - pegY[i];
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared >= minimumDistanceSquared) {
                continue;
            }

            float distance = (float) Math.sqrt(Math.max(1f, distanceSquared));
            float normalX = dx / distance;
            float normalY = dy / distance;
            ballX = pegX[i] + normalX * minimumDistance;
            ballY = pegY[i] + normalY * minimumDistance;
            reflectVelocity(normalX, normalY, 0.66f);

            if (!pegHit[i]) {
                pegHit[i] = true;
                pegLayerDirty = true;
                coins += pegValue[i];
                coinsDirty = true;
            }
        }
    }

    private void resolveStall(float dt) {
        float dx = ballX - stallAnchorX;
        float dy = ballY - stallAnchorY;
        float movementThreshold = ballRadius * 0.45f;
        if (dx * dx + dy * dy > movementThreshold * movementThreshold) {
            stallAnchorX = ballX;
            stallAnchorY = ballY;
            stallTime = 0f;
            return;
        }

        stallTime += dt;
        if (stallTime < 0.55f) {
            return;
        }

        float escapeDirection = ballX > getWidth() * 0.5f ? -1f : 1f;
        velocityX = escapeDirection * getWidth() * 0.38f;
        velocityY = -getWidth() * 0.2f;
        ballX += escapeDirection * 2f * density;
        stallAnchorX = ballX;
        stallAnchorY = ballY;
        stallTime = 0f;
    }

    private void reflectVelocity(float normalX, float normalY, float restitution) {
        float normalSpeed = velocityX * normalX + velocityY * normalY;
        if (normalSpeed >= 0f) {
            return;
        }
        float impulse = (1f + restitution) * normalSpeed;
        velocityX -= impulse * normalX;
        velocityY -= impulse * normalY;
        velocityX *= 0.992f;
        velocityY *= 0.992f;
    }

    private void finishShot() {
        ballMoving = false;
        ballReady = false;
        lastFrameNanos = 0L;
        if (ballImageView != null) {
            ballImageView.setVisibility(INVISIBLE);
        }
        postDelayed(() -> {
            if (!ballMoving && !aiming) {
                for (int i = 0; i < PEG_COUNT; i++) {
                    pegHit[i] = false;
                }
                rebuildPegLayer();
                ballX = launchX;
                ballY = launchY;
                if (ballLifecycleListener != null) {
                    ballLifecycleListener.onShotFinished();
                }
                invalidate();
            }
        }, 450L);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!geometryReady || paused) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!ballReady || ballMoving || !isLauncherTouch(event.getX(), event.getY())) {
                    return false;
                }
                aiming = true;
                pullStartY = event.getY();
                updatePull(event.getY());
                getParent().requestDisallowInterceptTouchEvent(true);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (aiming) {
                    updatePull(event.getY());
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (aiming) {
                    updatePull(event.getY());
                    launchBall();
                    performClick();
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                aiming = false;
                power = 0f;
                invalidate();
                return true;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private boolean isLauncherTouch(float x, float y) {
        float padding = 36f * density;
        return x >= launcherRect.left - padding
                && x <= launcherRect.right + padding
                && y >= launcherRect.top - padding
                && y <= launcherRect.bottom;
    }

    private void updatePull(float y) {
        float pullDistance = Math.max(0f, y - pullStartY);
        power = Math.min(1f, pullDistance / (launcherRect.height() * 0.62f));
    }

    private void launchBall() {
        aiming = false;
        if (power < 0.08f) {
            power = 0f;
            invalidate();
            return;
        }
        float speed = getWidth() * (1.0f + 2.15f * power);
        velocityX = 0f;
        velocityY = -speed;
        stallAnchorX = ballX;
        stallAnchorY = ballY;
        stallTime = 0f;
        ballReady = false;
        ballMoving = true;
        launcherGateClosed = false;
        lastFrameNanos = 0L;
        power = 0f;
        if (ballLifecycleListener != null) {
            ballLifecycleListener.onBallLaunched();
        }
        invalidate();
        schedulePhysicsFrame();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    public void setOnCoinsChangedListener(OnCoinsChangedListener listener) {
        coinsChangedListener = listener;
        if (listener != null) {
            listener.onCoinsChanged(coins);
        }
    }

    public void setBallLifecycleListener(BallLifecycleListener listener) {
        ballLifecycleListener = listener;
    }

    public void prepareBall(int drawableResource) {
        if (ballImageView != null) {
            ballImageView.setImageResource(drawableResource);
            ballImageView.setVisibility(VISIBLE);
        }
        ballX = launchX;
        ballY = launchY;
        ballReady = true;
        updateBallImagePosition();
    }

    public boolean isWaitingForBall() {
        return !ballMoving && !aiming && !ballReady;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        lastFrameNanos = 0L;
        if (paused) {
            removeCallbacks(physicsFrame);
            physicsFramePosted = false;
        } else {
            schedulePhysicsFrame();
        }
    }

    public void bindBallImage(ImageView ballImageView) {
        this.ballImageView = ballImageView;
        resizeBallImage();
        updateBallImagePosition();
    }

    private void schedulePhysicsFrame() {
        if (!physicsFramePosted && ballMoving && !paused) {
            physicsFramePosted = true;
            postOnAnimation(physicsFrame);
        }
    }

    private void resizeBallImage() {
        if (ballImageView == null || ballRadius <= 0f) {
            return;
        }
        ViewGroup.LayoutParams params = ballImageView.getLayoutParams();
        int size = Math.round(ballRadius * 2f);
        if (params.width != size || params.height != size) {
            params.width = size;
            params.height = size;
            ballImageView.setLayoutParams(params);
        }
    }

    private void updateBallImagePosition() {
        if (ballImageView == null) {
            return;
        }
        ballImageView.setTranslationX(ballX - ballRadius);
        ballImageView.setTranslationY(ballY - ballRadius);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(physicsFrame);
        physicsFramePosted = false;
        super.onDetachedFromWindow();
    }

    public interface OnCoinsChangedListener {
        void onCoinsChanged(int coins);
    }

    public interface BallLifecycleListener {
        void onBallLaunched();

        void onShotFinished();
    }
}

package com.example.game43.GameBall;

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

public class BallGameView extends View {
    private static final int[] BUCKET_SCORES = {100, 200, 500, 200, 100};

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballCounterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pegHitRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawRect = new RectF();
    private final RectF addBallButtonRect = new RectF();
    private final List<Peg> pegs = new ArrayList<>();
    private final List<DropBall> balls = new ArrayList<>();
    private final List<PegHitRing> pegHitRings = new ArrayList<>();
    private final RectF[] bucketRects = new RectF[5];
    private final Random random = new Random();

    private Bitmap backgroundBitmap;
    private Bitmap addBallButtonBitmap;
    private Bitmap[] bucketBackBitmaps;
    private Bitmap[] bucketFrontBitmaps;
    private Bitmap[] pegBitmaps;
    private Bitmap ballBitmap;

    private float viewWidth;
    private float viewHeight;
    private float minSide;
    private float ballRadius;
    private long lastFrameNanos;
    private int totalDroppedBalls;
    private AddBallRequestListener addBallRequestListener;

    public BallGameView(Context context) {
        super(context);
        init();
    }

    public BallGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BallGameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(true);
        bitmapPaint.setDither(true);
        bitmapPaint.setFilterBitmap(true);

        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        addBallButtonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.button1);
        ballBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.peg_yellow);
        pegBitmaps = new Bitmap[] {
                BitmapFactory.decodeResource(getResources(), R.drawable.xanhla),
                BitmapFactory.decodeResource(getResources(), R.drawable.xanh),
                BitmapFactory.decodeResource(getResources(), R.drawable.tim),
                BitmapFactory.decodeResource(getResources(), R.drawable.hong),
                BitmapFactory.decodeResource(getResources(), R.drawable.peg_yellow)
        };
        bucketBackBitmaps = new Bitmap[] {
                BitmapFactory.decodeResource(getResources(), R.drawable.vangduoi),
                BitmapFactory.decodeResource(getResources(), R.drawable.xanhduoi),
                BitmapFactory.decodeResource(getResources(), R.drawable.duoi),
                BitmapFactory.decodeResource(getResources(), R.drawable.xanhhduoi),
                BitmapFactory.decodeResource(getResources(), R.drawable.vangduoi)
        };
        bucketFrontBitmaps = new Bitmap[] {
                BitmapFactory.decodeResource(getResources(), R.drawable.vangtren),
                BitmapFactory.decodeResource(getResources(), R.drawable.xanhtren),
                BitmapFactory.decodeResource(getResources(), R.drawable.tren),
                BitmapFactory.decodeResource(getResources(), R.drawable.xanh_tren),
                BitmapFactory.decodeResource(getResources(), R.drawable.vangtren)
        };

        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        textShadowPaint.setColor(Color.argb(150, 20, 12, 46));
        textShadowPaint.setTextAlign(Paint.Align.CENTER);
        textShadowPaint.setFakeBoldText(true);

        ballCounterPaint.setColor(Color.argb(180, 11, 13, 33));

        pegHitRingPaint.setStyle(Paint.Style.STROKE);
        pegHitRingPaint.setStrokeCap(Paint.Cap.ROUND);
        pegHitRingPaint.setColor(Color.WHITE);
    }

    public void setAddBallRequestListener(AddBallRequestListener addBallRequestListener) {
        this.addBallRequestListener = addBallRequestListener;
    }

    public void grantExtraBall() {
        addWaitingBall();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        configureBoard(w, h);
    }

    private void configureBoard(int width, int height) {
        viewWidth = width;
        viewHeight = height;
        minSide = Math.min(viewWidth, viewHeight);
        ballRadius = minSide * 0.023f;
        balls.clear();
        pegs.clear();
        pegHitRings.clear();
        totalDroppedBalls = 0;

        configureButton();
        configureBuckets();
        configurePegs();
    }

    private void configureButton() {
        float buttonWidth = minSide * 0.28f;
        float buttonHeight = buttonWidth * 135f / 456f;
        float margin = minSide * 0.025f;
        addBallButtonRect.set(margin, margin, margin + buttonWidth, margin + buttonHeight);
    }

    private void configureBuckets() {
        float gap = minSide * 0.003f;
        float bucketWidth = (viewWidth - gap * 6f) / 5f;
        float bucketHeight = bucketWidth * 174f / 187f;
        float bottom = viewHeight - minSide * 0.02f - dp(50f);
        for (int i = 0; i < bucketRects.length; i++) {
            float left = gap + i * (bucketWidth + gap);
            bucketRects[i] = new RectF(left, bottom - bucketHeight, left + bucketWidth, bottom);
        }
    }

    private void configurePegs() {
        int rowCount = 9;
        float bottom = bucketRects[0].top - ballRadius * 3.4f;
        float spacing = Math.min(viewWidth * 0.105f, minSide * 0.105f);
        float rowGap = spacing * 0.72f;
        float top = bottom - rowGap * (rowCount - 1);

        for (int row = 0; row < rowCount; row++) {
            int count = row + 2;
            float rowWidth = (count - 1) * spacing;
            float startX = viewWidth * 0.5f - rowWidth * 0.5f;
            float y = top + row * rowGap;
            int bitmapIndex = row % pegBitmaps.length;
            for (int col = 0; col < count; col++) {
                float x = startX + col * spacing;
                if (x < ballRadius * 2f || x > viewWidth - ballRadius * 2f) {
                    continue;
                }
                pegs.add(new Peg(x, y, ballRadius * 0.62f, pegBitmaps[bitmapIndex]));
            }

            if (row == rowCount - 1) {
                float sideInset = ballRadius * 2.3f;
                addPeg(Math.max(sideInset, startX - spacing), y, pegBitmaps[3]);
                addPeg(Math.min(viewWidth - sideInset, startX + count * spacing), y, pegBitmaps[3]);
            }
        }
    }

    private void addPeg(float x, float y, Bitmap bitmap) {
        if (x < ballRadius * 2f || x > viewWidth - ballRadius * 2f) {
            return;
        }
        pegs.add(new Peg(x, y, ballRadius * 0.62f, bitmap));
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
        updatePegHitRings(dt);

        Iterator<DropBall> iterator = balls.iterator();
        while (iterator.hasNext()) {
            DropBall ball = iterator.next();
            if (!ball.falling) {
                continue;
            }

            ball.vy += gravity() * dt;
            ball.x += ball.vx * dt;
            ball.y += ball.vy * dt;

            resolvePegCollisions(ball);
            if (resolveBucket(ball)) {
                iterator.remove();
            }
        }
    }

    private void resolvePegCollisions(DropBall ball) {
        for (Peg peg : pegs) {
            float dx = ball.x - peg.x;
            float dy = ball.y - peg.y;
            float minDistance = ball.radius + peg.radius;
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared <= 0.001f || distanceSquared >= minDistance * minDistance) {
                continue;
            }

            float distance = (float) Math.sqrt(distanceSquared);
            float normalX = dx / distance;
            float normalY = dy / distance;
            float overlap = minDistance - distance;
            ball.x += normalX * (overlap + 0.5f);
            ball.y += normalY * (overlap + 0.5f);

            float velocityAlongNormal = ball.vx * normalX + ball.vy * normalY;
            if (velocityAlongNormal < 0f) {
                float bounce = 0.18f;
                ball.vx -= (1f + bounce) * velocityAlongNormal * normalX;
                ball.vy -= (1f + bounce) * velocityAlongNormal * normalY;
            }

            float slideDirection;
            if (Math.abs(normalX) < 0.18f) {
                slideDirection = random.nextBoolean() ? 1f : -1f;
            } else {
                slideDirection = Math.signum(normalX);
            }
            ball.vx += slideDirection * randomBetween(minSide * 0.025f, minSide * 0.07f);
            ball.vx += randomBetween(-minSide * 0.025f, minSide * 0.025f);
            ball.vx = clamp(ball.vx, -minSide * 0.32f, minSide * 0.32f);
            ball.vy = Math.min(ball.vy, -minSide * 0.045f);
            addPegHitRing(peg);
        }
    }

    private void updatePegHitRings(float dt) {
        Iterator<PegHitRing> iterator = pegHitRings.iterator();
        while (iterator.hasNext()) {
            PegHitRing ring = iterator.next();
            ring.age += dt;
            if (ring.age >= ring.duration) {
                iterator.remove();
            }
        }
    }

    private void addPegHitRing(Peg peg) {
        pegHitRings.add(new PegHitRing(peg.x, peg.y, peg.radius * 1.45f));
    }

    private boolean resolveBucket(DropBall ball) {
        if (ball.x + ball.radius < 0f || ball.x - ball.radius > viewWidth) {
            return true;
        }

        if (ball.y - ball.radius < bucketRects[0].top) {
            return false;
        }

        for (RectF bucketRect : bucketRects) {
            if (ball.x >= bucketRect.left && ball.x <= bucketRect.right
                    && ball.y >= bucketRect.top + bucketRect.height() * 0.52f) {
                return true;
            }
        }
        return ball.y - ball.radius > viewHeight;
    }

    private void drawGame(Canvas canvas) {
        drawBackground(canvas);
        drawButton(canvas);
        drawPegs(canvas);
        drawPegHitRings(canvas);
        drawBucketBacks(canvas);
        drawBalls(canvas);
        drawBucketFronts(canvas);
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundBitmap == null) {
            canvas.drawColor(Color.rgb(27, 27, 73));
            return;
        }
        float scale = Math.max(viewWidth / backgroundBitmap.getWidth(), viewHeight / backgroundBitmap.getHeight());
        float width = backgroundBitmap.getWidth() * scale;
        float height = backgroundBitmap.getHeight() * scale;
        drawRect.set((viewWidth - width) * 0.5f, (viewHeight - height) * 0.5f,
                (viewWidth + width) * 0.5f, (viewHeight + height) * 0.5f);
        canvas.drawBitmap(backgroundBitmap, null, drawRect, bitmapPaint);
    }

    private void drawButton(Canvas canvas) {
        if (addBallButtonBitmap != null) {
            canvas.drawBitmap(addBallButtonBitmap, null, addBallButtonRect, bitmapPaint);
        }

        float textSize = addBallButtonRect.height() * 0.38f;
        textPaint.setTextSize(textSize);
        textShadowPaint.setTextSize(textSize);
        float x = addBallButtonRect.centerX();
        float y = addBallButtonRect.centerY() - (textPaint.ascent() + textPaint.descent()) * 0.5f;
        canvas.drawText("+1 Ball", x + dp(1.2f), y + dp(1.2f), textShadowPaint);
        canvas.drawText("+1 Ball", x, y, textPaint);

        float counterHeight = addBallButtonRect.height() * 0.62f;
        float counterWidth = counterHeight * 2.2f;
        drawRect.set(addBallButtonRect.right + dp(6f), addBallButtonRect.top,
                addBallButtonRect.right + dp(6f) + counterWidth, addBallButtonRect.top + counterHeight);
        canvas.drawRoundRect(drawRect, counterHeight * 0.5f, counterHeight * 0.5f, ballCounterPaint);
        textPaint.setTextSize(counterHeight * 0.45f);
        String countText = balls.size() + " Ball";
        float countY = drawRect.centerY() - (textPaint.ascent() + textPaint.descent()) * 0.5f;
        canvas.drawText(countText, drawRect.centerX(), countY, textPaint);
    }

    private void drawPegs(Canvas canvas) {
        for (Peg peg : pegs) {
            drawRect.set(peg.x - peg.radius, peg.y - peg.radius, peg.x + peg.radius, peg.y + peg.radius);
            canvas.drawBitmap(peg.bitmap, null, drawRect, bitmapPaint);
        }
    }

    private void drawPegHitRings(Canvas canvas) {
        pegHitRingPaint.setStrokeWidth(Math.max(dp(1f), ballRadius * 0.12f));
        for (PegHitRing ring : pegHitRings) {
            float progress = ring.age / ring.duration;
            int alpha = (int) (220f * (1f - progress));
            pegHitRingPaint.setAlpha(Math.max(0, alpha));
            canvas.drawCircle(ring.x, ring.y, ring.radius + ballRadius * 0.25f * progress, pegHitRingPaint);
        }
        pegHitRingPaint.setAlpha(255);
    }

    private void drawBalls(Canvas canvas) {
        for (DropBall ball : balls) {
            drawRect.set(ball.x - ball.radius, ball.y - ball.radius, ball.x + ball.radius, ball.y + ball.radius);
            canvas.drawBitmap(ballBitmap, null, drawRect, bitmapPaint);
        }
    }

    private void drawBucketBacks(Canvas canvas) {
        for (int i = 0; i < bucketRects.length; i++) {
            Bitmap bucketBitmap = bucketBackBitmaps[i];
            if (bucketBitmap != null) {
                canvas.drawBitmap(bucketBitmap, null, bucketRects[i], bitmapPaint);
            }
        }
    }

    private void drawBucketFronts(Canvas canvas) {
        for (int i = 0; i < bucketRects.length; i++) {
            Bitmap bucketBitmap = bucketFrontBitmaps[i];
            if (bucketBitmap != null) {
                float frontHeight = bucketRects[i].width() * bucketBitmap.getHeight() / bucketBitmap.getWidth();
                drawRect.set(bucketRects[i].left, bucketRects[i].bottom - frontHeight,
                        bucketRects[i].right, bucketRects[i].bottom);
                canvas.drawBitmap(bucketBitmap, null, drawRect, bitmapPaint);
            }

            float scoreSize = bucketRects[i].width() * 0.27f;
            textPaint.setTextSize(scoreSize);
            textShadowPaint.setTextSize(scoreSize);
            String scoreText = String.valueOf(BUCKET_SCORES[i]);
            float y = bucketRects[i].top + bucketRects[i].height() * 0.7f;
            canvas.drawText(scoreText, bucketRects[i].centerX() + dp(1.2f), y + dp(1.2f), textShadowPaint);
            canvas.drawText(scoreText, bucketRects[i].centerX(), y, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return true;
        }

        float touchX = event.getX();
        float touchY = event.getY();
        if (addBallButtonRect.contains(touchX, touchY)) {
            requestExtraBall();
            return true;
        }

        for (DropBall ball : balls) {
            if (ball.falling) {
                continue;
            }

            float dx = touchX - ball.x;
            float dy = touchY - ball.y;
            if (dx * dx + dy * dy <= ball.radius * ball.radius * 3f) {
                ball.falling = true;
                ball.vx = randomBetween(-minSide * 0.035f, minSide * 0.035f);
                ball.vy = minSide * 0.08f;
                totalDroppedBalls++;
                return true;
            }
        }

        return true;
    }

    private void addWaitingBall() {
        if (!balls.isEmpty()) {
            return;
        }

        float x = viewWidth * 0.5f + randomBetween(-ballRadius * 0.35f, ballRadius * 0.35f);
        float y = addBallButtonRect.bottom + ballRadius * 2.2f;
        balls.add(new DropBall(x, y, ballRadius, ballBitmap));
    }

    private void requestExtraBall() {
        if (addBallRequestListener != null) {
            addBallRequestListener.onAddBallRequested();
            return;
        }
        grantExtraBall();
    }

    private float gravity() {
        return viewHeight * 1.18f;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float randomBetween(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Peg {
        final float x;
        final float y;
        final float radius;
        final Bitmap bitmap;

        Peg(float x, float y, float radius, Bitmap bitmap) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.bitmap = bitmap;
        }
    }

    private static final class DropBall {
        float x;
        float y;
        float vx;
        float vy;
        final float radius;
        final Bitmap bitmap;
        boolean falling;

        DropBall(float x, float y, float radius, Bitmap bitmap) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.bitmap = bitmap;
        }
    }

    private static final class PegHitRing {
        final float x;
        final float y;
        final float radius;
        final float duration = 0.12f;
        float age;

        PegHitRing(float x, float y, float radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }

    public interface AddBallRequestListener {
        void onAddBallRequested();
    }
}

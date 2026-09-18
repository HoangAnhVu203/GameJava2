package com.example.game43.FlappyDunk;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.game43.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlappyDunkGameView extends View {
    private static final String PREFS_NAME = "flappy_dunk";
    private static final String COINS_KEY = "coins";

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawRect = new RectF();
    private final RectF backRect = new RectF();
    private final RectF coinHolderRect = new RectF();
    private final List<Hoop> hoops = new ArrayList<>();
    private final Random random = new Random();
    private final SharedPreferences preferences;

    private final Bitmap background;
    private final Bitmap ballBitmap;
    private final Bitmap wingBitmap;
    private final Bitmap hoopBackBitmap;
    private final Bitmap hoopFrontBitmap;
    private final Bitmap coinBitmap;

    private float ballX;
    private float ballY;
    private float ballVx;
    private float ballVy;
    private float ballRadius;
    private float ballRotation;
    private float hoopWidth;
    private float previousBallY;
    private float wingFlap;
    private long lastFrameNanos;
    private int coins;
    private int runScore;
    private boolean layoutReady;
    private boolean started;
    private boolean gameOver;

    public FlappyDunkGameView(Context context) {
        this(context, null);
    }

    public FlappyDunkGameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        setFocusable(true);
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        coins = preferences.getInt(COINS_KEY, 0);
        background = decode(R.drawable.flappy_dunk_background);
        ballBitmap = decode(R.drawable.flappy_dunk_ball);
        wingBitmap = decode(R.drawable.flappy_dunk_wing);
        hoopBackBitmap = decode(R.drawable.flappy_dunk_hoop_front);
        hoopFrontBitmap = decode(R.drawable.flappy_dunk_hoop_back);
        coinBitmap = decode(R.drawable.tape_coin_icon);
        textPaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        shadowPaint.setTypeface(textPaint.getTypeface());
    }

    private Bitmap decode(int resourceId) {
        return BitmapFactory.decodeResource(getResources(), resourceId);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        ballRadius = width * 0.052f;
        hoopWidth = width * 0.24f;
        float hudTop = dp(16f);
        float backSize = Math.max(dp(32f), width * 0.09f);
        backRect.set(dp(14f), hudTop, dp(14f) + backSize, hudTop + backSize);

        float coinHeight = Math.max(dp(34f), width * 0.105f);
        float coinWidth = coinHeight * 2.65f;
        coinHolderRect.set(width - dp(14f) - coinWidth, hudTop,
                width - dp(14f), hudTop + coinHeight);
        layoutReady = true;
        resetGame();
    }

    private void resetGame() {
        if (!layoutReady) {
            return;
        }
        ballX = getWidth() * 0.15f;
        ballY = getHeight() * 0.56f;
        previousBallY = ballY;
        ballVx = 0f;
        ballVy = 0f;
        ballRotation = 0f;
        wingFlap = 0f;
        runScore = 0;
        started = false;
        gameOver = false;
        hoops.clear();

        addHoop(getWidth() * 0.35f, getHeight() * 0.61f);
        addHoop(getWidth() * 1.0625f, getHeight() * 0.535f);
        lastFrameNanos = System.nanoTime();
        invalidate();
    }

    private void addHoop(float x, float y) {
        hoops.add(new Hoop(x, y));
    }

    private void addNextHoop() {
        float lastX = hoops.isEmpty() ? getWidth() : hoops.get(hoops.size() - 1).x;
        float minY = Math.max(dp(120f), getHeight() * 0.28f);
        float maxY = getHeight() * 0.74f;
        float y = minY + random.nextFloat() * Math.max(dp(20f), maxY - minY);
        addHoop(lastX + getWidth() * (0.825f + random.nextFloat() * 0.15f), y);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        if (!layoutReady) {
            return;
        }

        long now = System.nanoTime();
        float dt = Math.min(0.032f, Math.max(0f, (now - lastFrameNanos) / 1_000_000_000f));
        lastFrameNanos = now;
        if (started && !gameOver) {
            update(dt);
        }

        drawHoopBackLayers(canvas);
        drawWing(canvas, false);
        drawBall(canvas);
        drawWing(canvas, true);
        drawHoopFrontLayers(canvas);
        drawHud(canvas);
        if (!started && !gameOver) {
            drawReadyPrompt(canvas);
        }
        if (gameOver) {
            drawCenteredOverlay(canvas, "GAME OVER", "Tap to play again");
        }
        postInvalidateOnAnimation();
    }

    private void update(float dt) {
        previousBallY = ballY;
        float gravity = getHeight() * 1.42f;
        ballVy += gravity * dt;
        ballVy = Math.min(ballVy, getHeight() * 0.92f);
        wingFlap = Math.max(0f, wingFlap - dt * 4.8f);
        ballX += ballVx * dt;
        ballY += ballVy * dt;
        ballRotation += ballVx * dt * 0.24f;

        float minimumForwardSpeed = getWidth() * 0.29f;
        ballVx += (minimumForwardSpeed - ballVx) * Math.min(1f, dt * 3.5f);
        float followX = getWidth() * 0.28f;
        if (ballX > followX) {
            float scroll = ballX - followX;
            ballX = followX;
            for (Hoop hoop : hoops) {
                hoop.x -= scroll;
            }
        }

        int physicsSteps = Math.max(1, Math.min(3,
                (int) Math.ceil(Math.abs(ballVy) * dt / Math.max(1f, ballRadius * 0.65f))));
        for (int step = 0; step < physicsSteps; step++) {
            for (Hoop hoop : hoops) {
                resolveRimCollision(hoop, hoop.x - hoopWidth * 0.39f, hoop.y);
                resolveRimCollision(hoop, hoop.x + hoopWidth * 0.39f, hoop.y);
            }
        }

        checkScores();
        checkMissedHoops();
        if (gameOver) {
            return;
        }
        recycleHoops();
        if (ballY - ballRadius <= 0f || ballY + ballRadius >= getHeight()) {
            endGame();
        }
    }

    private void resolveRimCollision(Hoop hoop, float rimX, float rimY) {
        float rimRadius = hoopWidth * 0.058f;
        float dx = ballX - rimX;
        float dy = ballY - rimY;
        float minimumDistance = ballRadius + rimRadius;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared >= minimumDistance * minimumDistance) {
            return;
        }
        float distance = (float) Math.sqrt(Math.max(0.0001f, distanceSquared));
        float nx = dx / distance;
        float ny = dy / distance;
        ballX = rimX + nx * minimumDistance;
        ballY = rimY + ny * minimumDistance;
        float normalSpeed = ballVx * nx + ballVy * ny;
        if (normalSpeed < 0f) {
            float impulse = -(1f + 0.58f) * normalSpeed;
            ballVx += impulse * nx;
            ballVy += impulse * ny;
            ballVx *= 0.96f;
            ballVy *= 0.96f;
        }
    }

    private void checkScores() {
        for (Hoop hoop : hoops) {
            if (hoop.scored || ballVy <= 0f) {
                continue;
            }
            float openingHalfWidth = hoopWidth * 0.31f;
            boolean crossedDown = previousBallY < hoop.y && ballY >= hoop.y;
            boolean insideOpening = Math.abs(ballX - hoop.x) <= openingHalfWidth;
            if (crossedDown && insideOpening) {
                hoop.scored = true;
                coins++;
                runScore++;
                preferences.edit().putInt(COINS_KEY, coins).apply();
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
        }
    }

    private void checkMissedHoops() {
        float openingHalfWidth = hoopWidth * 0.31f;
        for (Hoop hoop : hoops) {
            if (hoop.scored) {
                continue;
            }
            if (ballX > hoop.x + openingHalfWidth) {
                endGame();
                return;
            }
        }
    }

    private void endGame() {
        if (gameOver) {
            return;
        }
        gameOver = true;
        ballVx = 0f;
        ballVy = 0f;
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    private void recycleHoops() {
        while (!hoops.isEmpty() && hoops.get(0).x + hoopWidth < -dp(8f)) {
            hoops.remove(0);
        }
        while (hoops.isEmpty() || hoops.get(hoops.size() - 1).x < getWidth() * 1.15f) {
            addNextHoop();
        }
    }

    private void flap() {
        started = true;
        ballVy = -getHeight() * 0.49f;
        ballVx = Math.max(ballVx, getWidth() * 0.35f);
        wingFlap = 1f;
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private void drawBackground(Canvas canvas) {
        float scale = Math.max(getWidth() / (float) background.getWidth(),
                getHeight() / (float) background.getHeight());
        float width = background.getWidth() * scale;
        float height = background.getHeight() * scale;
        drawRect.set((getWidth() - width) * 0.5f, (getHeight() - height) * 0.5f,
                (getWidth() + width) * 0.5f, (getHeight() + height) * 0.5f);
        canvas.drawBitmap(background, null, drawRect, bitmapPaint);
    }

    private void drawHoopBackLayers(Canvas canvas) {
        for (Hoop hoop : hoops) {
            float height = hoopWidth * hoopBackBitmap.getHeight() / hoopBackBitmap.getWidth();
            drawRect.set(hoop.x - hoopWidth * 0.5f, hoop.y - height * 0.52f,
                    hoop.x + hoopWidth * 0.5f, hoop.y + height * 0.48f);
            canvas.drawBitmap(hoopBackBitmap, null, drawRect, bitmapPaint);
        }
    }

    private void drawHoopFrontLayers(Canvas canvas) {
        for (Hoop hoop : hoops) {
            float height = hoopWidth * hoopFrontBitmap.getHeight() / hoopFrontBitmap.getWidth();
            float frontOffsetY = dp(3f);
            drawRect.set(hoop.x - hoopWidth * 0.5f,
                    hoop.y - height * 0.48f + frontOffsetY,
                    hoop.x + hoopWidth * 0.5f,
                    hoop.y + height * 0.52f + frontOffsetY);
            canvas.drawBitmap(hoopFrontBitmap, null, drawRect, bitmapPaint);
        }
    }

    private void drawWing(Canvas canvas, boolean frontWing) {
        float flapAmount = (float) Math.sin((1f - wingFlap) * Math.PI);
        if (wingFlap <= 0f) {
            flapAmount = 0f;
        }
        float wingWidth = ballRadius * (frontWing ? 1.18f : 0.96f);
        float wingHeight = wingWidth * wingBitmap.getHeight() / wingBitmap.getWidth();
        float centerX = ballX + ballRadius * (frontWing ? -0.54f : 0.30f);
        float centerY = ballY - ballRadius * (frontWing ? 0.27f : 0.47f)
                - flapAmount * ballRadius * (frontWing ? 0.08f : 0.06f);
        float angle = frontWing ? 18f + flapAmount * 12f : -18f - flapAmount * 12f;
        drawRect.set(centerX - wingWidth * 0.5f, centerY - wingHeight * 0.5f,
                centerX + wingWidth * 0.5f, centerY + wingHeight * 0.5f);
        canvas.save();
        canvas.rotate(angle, centerX, centerY);
        if (!frontWing) {
            canvas.scale(-1f, 1f, centerX, centerY);
        }
        bitmapPaint.setAlpha(frontWing ? 255 : 230);
        canvas.drawBitmap(wingBitmap, null, drawRect, bitmapPaint);
        bitmapPaint.setAlpha(255);
        canvas.restore();
    }

    private void drawBall(Canvas canvas) {
        canvas.save();
        canvas.rotate(ballRotation, ballX, ballY);
        drawRect.set(ballX - ballRadius, ballY - ballRadius,
                ballX + ballRadius, ballY + ballRadius);
        canvas.drawBitmap(ballBitmap, null, drawRect, bitmapPaint);
        canvas.restore();
    }

    private void drawHud(Canvas canvas) {
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0xD936332E);
        canvas.drawOval(backRect, fillPaint);
        fillPaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStrokeWidth(Math.max(dp(2.5f), backRect.width() * 0.09f));
        fillPaint.setStrokeCap(Paint.Cap.ROUND);
        fillPaint.setStrokeJoin(Paint.Join.ROUND);
        fillPaint.setColor(Color.WHITE);
        float arrowLeft = backRect.left + backRect.width() * 0.28f;
        float arrowRight = backRect.right - backRect.width() * 0.25f;
        float arrowTop = backRect.top + backRect.height() * 0.28f;
        float arrowBottom = backRect.bottom - backRect.height() * 0.28f;
        canvas.drawLine(arrowLeft, backRect.centerY(), arrowRight, backRect.centerY(), fillPaint);
        canvas.drawLine(arrowLeft, backRect.centerY(),
                backRect.centerX(), arrowTop, fillPaint);
        canvas.drawLine(arrowLeft, backRect.centerY(),
                backRect.centerX(), arrowBottom, fillPaint);
        fillPaint.setStrokeCap(Paint.Cap.BUTT);
        fillPaint.setStyle(Paint.Style.FILL);

        fillPaint.setColor(0xD938342E);
        canvas.drawRoundRect(coinHolderRect, coinHolderRect.height() * 0.45f,
                coinHolderRect.height() * 0.45f, fillPaint);
        float coinSize = coinHolderRect.height() * 0.76f;
        drawRect.set(coinHolderRect.left + coinHolderRect.height() * 0.12f,
                coinHolderRect.centerY() - coinSize * 0.5f,
                coinHolderRect.left + coinHolderRect.height() * 0.12f + coinSize,
                coinHolderRect.centerY() + coinSize * 0.5f);
        canvas.drawBitmap(coinBitmap, null, drawRect, bitmapPaint);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(coinHolderRect.height() * 0.45f);
        float coinTextY = coinHolderRect.centerY() - (textPaint.ascent() + textPaint.descent()) * 0.5f;
        canvas.drawText(Integer.toString(coins), coinHolderRect.right - coinHolderRect.height() * 0.23f,
                coinTextY, textPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Math.min(getWidth() * 0.18f, sp(58f)));
        textPaint.setShadowLayer(dp(2f), 0f, dp(2f), 0x55000000);
        canvas.drawText(Integer.toString(runScore), getWidth() * 0.5f,
                getHeight() * 0.17f, textPaint);
        textPaint.clearShadowLayer();
    }

    private void drawReadyPrompt(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(Math.min(getWidth() * 0.055f, sp(18f)));
        textPaint.setColor(0xCC514D39);
        canvas.drawText("TAP TO JUMP", getWidth() * 0.5f, getHeight() * 0.25f, textPaint);
    }

    private void drawCenteredOverlay(Canvas canvas, String title, String subtitle) {
        fillPaint.setColor(0x88000000);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), fillPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Math.min(getWidth() * 0.12f, sp(38f)));
        canvas.drawText(title, getWidth() * 0.5f, getHeight() * 0.46f, textPaint);
        textPaint.setTextSize(Math.min(getWidth() * 0.052f, sp(17f)));
        canvas.drawText(subtitle, getWidth() * 0.5f, getHeight() * 0.53f, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN || !layoutReady) {
            return true;
        }
        float x = event.getX();
        float y = event.getY();
        if (backRect.contains(x, y)) {
            Context context = getContext();
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
            performClick();
            return true;
        }
        if (gameOver) {
            resetGame();
        }
        flap();
        performClick();
        invalidate();
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        lastFrameNanos = 0L;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private static final class Hoop {
        float x;
        final float y;
        boolean scored;

        Hoop(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}

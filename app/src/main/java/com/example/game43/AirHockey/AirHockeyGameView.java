package com.example.game43.AirHockey;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.game43.R;

import java.util.Locale;
public class AirHockeyGameView extends View {
    private static final int WIN_SCORE = 10;
    private static final float PUCK_FRICTION = 0.22f;
    private static final float WALL_RESTITUTION = 0.91f;
    private static final float MALLET_RESTITUTION = 0.82f;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF boardRect = new RectF();
    private final RectF playRect = new RectF();
    private final RectF scoreRect = new RectF();
    private final RectF topGoalRect = new RectF();
    private final RectF bottomGoalRect = new RectF();
    private final RectF scoreIconRect = new RectF();
    private final Path scoreIconClip = new Path();

    private final Bitmap background;
    private final Bitmap table;
    private final Bitmap pusher;
    private final Bitmap puckImage;
    private final Bitmap scoreboard;
    private final Bitmap goal;
    private final Bitmap scoreIcon;

    private float puckX;
    private float puckY;
    private float puckVx;
    private float puckVy;
    private float puckRadius;
    private float malletRadius;

    private float playerX;
    private float playerY;
    private float playerVx;
    private float playerVy;
    private float cpuX;
    private float cpuY;
    private float cpuVx;
    private float cpuVy;

    private int playerScore;
    private int cpuScore;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private float touchOffsetX;
    private float touchOffsetY;
    private long lastTouchNanos;
    private long lastFrameNanos;
    private long roundResumeAtMs;
    private boolean layoutReady;
    private boolean gameOver;
    private boolean cpuReadyToStrike;

    public AirHockeyGameView(Context context) {
        this(context, null);
    }

    public AirHockeyGameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setClickable(true);
        background = loadBitmap(R.drawable.bg_hockey);
        table = loadBitmap(R.drawable.air_hockey_table);
        pusher = loadBitmap(R.drawable.hockey_pusher);
        puckImage = loadBitmap(R.drawable.puck);
        scoreboard = loadBitmap(R.drawable.hockey_scoreboard);
        goal = loadBitmap(R.drawable.hockey_goal);
        scoreIcon = loadBitmap(R.drawable.hockey_score_icon);
        overlayPaint.setColor(0xB8202840);
        textPaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
    }

    private Bitmap loadBitmap(int resourceId) {
        return BitmapFactory.decodeResource(getResources(), resourceId);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        float outerPadding = Math.max(dp(8f), width * 0.025f);
        float availableHeight = height - outerPadding * 2f;
        float boardAspect = table.getWidth() / (float) table.getHeight();
        float boardWidth = Math.min(width - outerPadding * 2f, availableHeight * boardAspect);
        float boardHeight = boardWidth / boardAspect;
        if (boardHeight > availableHeight) {
            boardHeight = availableHeight;
            boardWidth = boardHeight * boardAspect;
        }

        float left = (width - boardWidth) * 0.5f;
        float top = (height - boardHeight) * 0.5f;
        boardRect.set(left, top, left + boardWidth, top + boardHeight);

        // These insets match the transparent rail area in air_hockey_table.png.
        playRect.set(
                boardRect.left + boardWidth * 0.071f,
                boardRect.top + boardHeight * 0.071f,
                boardRect.right - boardWidth * 0.071f,
                boardRect.bottom - boardHeight * 0.071f);

        malletRadius = boardWidth * 0.082f;
        puckRadius = boardWidth * 0.056f;
        float scoreWidth = boardWidth * 0.105f;
        float scoreHeight = scoreWidth * scoreboard.getHeight() / scoreboard.getWidth();
        float scoreCenterX = boardRect.right - scoreWidth * 1.05f;
        scoreRect.set(
                scoreCenterX - scoreWidth * 0.5f,
                boardRect.centerY() - scoreHeight * 0.5f,
                scoreCenterX + scoreWidth * 0.5f,
                boardRect.centerY() + scoreHeight * 0.5f);

        float goalWidth = boardWidth * 0.48f;
        float goalHeight = goalWidth * goal.getHeight() / goal.getWidth();
        float topGoalCenterY = boardRect.top + boardHeight * 0.061f - dp(25f);
        float bottomGoalCenterY = boardRect.bottom - boardHeight * 0.061f + dp(65f);
        topGoalRect.set(
                boardRect.centerX() - goalWidth * 0.5f,
                topGoalCenterY - goalHeight * 0.5f,
                boardRect.centerX() + goalWidth * 0.5f,
                topGoalCenterY + goalHeight * 0.5f);
        bottomGoalRect.set(
                boardRect.centerX() - goalWidth * 0.5f,
                bottomGoalCenterY - goalHeight * 0.5f,
                boardRect.centerX() + goalWidth * 0.5f,
                bottomGoalCenterY + goalHeight * 0.5f);

        layoutReady = true;
        resetMatch();
    }

    private void resetMatch() {
        playerScore = 0;
        cpuScore = 0;
        gameOver = false;
        resetRound(450L);
    }

    private void resetRound(long delayMs) {
        puckX = playRect.centerX();
        puckY = boardRect.top + boardRect.height() * 0.607f;
        puckVx = 0f;
        puckVy = 0f;

        playerX = playRect.centerX();
        playerY = boardRect.top + boardRect.height() * 0.727f;
        playerVx = 0f;
        playerVy = 0f;
        cpuX = playRect.centerX();
        cpuY = boardRect.top + boardRect.height() * 0.273f;
        cpuVx = 0f;
        cpuVy = 0f;
        cpuReadyToStrike = false;

        roundResumeAtMs = SystemClock.uptimeMillis() + delayMs;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBitmap(canvas, background, new RectF(0f, 0f, getWidth(), getHeight()));
        if (!layoutReady) {
            return;
        }

        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
        }
        float frameSeconds = Math.min(0.032f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;
        if (!gameOver && SystemClock.uptimeMillis() >= roundResumeAtMs) {
            update(frameSeconds);
        }

        drawBitmap(canvas, table, boardRect);
        drawBitmap(canvas, goal, topGoalRect);
        canvas.save();
        canvas.rotate(180f, bottomGoalRect.centerX(), bottomGoalRect.centerY());
        drawBitmap(canvas, goal, bottomGoalRect);
        canvas.restore();
        drawBitmapCentered(canvas, pusher, cpuX, cpuY, malletRadius);
        drawBitmapCentered(canvas, pusher, playerX, playerY, malletRadius);
        drawBitmapCentered(canvas, puckImage, puckX, puckY, puckRadius);
        drawScoreboard(canvas);

        if (gameOver) {
            drawGameOver(canvas);
        }
        postInvalidateOnAnimation();
    }

    private void update(float dt) {
        if (dt <= 0f) {
            return;
        }
        updateCpu(dt);

        int steps = Math.max(1, Math.min(4,
                (int) Math.ceil(Math.max(Math.abs(puckVx), Math.abs(puckVy)) * dt
                        / Math.max(1f, puckRadius * 0.65f))));
        float step = dt / steps;
        for (int i = 0; i < steps && !gameOver; i++) {
            puckX += puckVx * step;
            puckY += puckVy * step;
            resolveMalletCollision(playerX, playerY, playerVx, playerVy);
            resolveMalletCollision(cpuX, cpuY, cpuVx, cpuVy);
            resolveWallsAndGoals();
        }

        float drag = Math.max(0f, 1f - PUCK_FRICTION * dt);
        puckVx *= drag;
        puckVy *= drag;
        float maxSpeed = boardRect.width() * 4.1f;
        float speed = length(puckVx, puckVy);
        if (speed > maxSpeed) {
            puckVx *= maxSpeed / speed;
            puckVy *= maxSpeed / speed;
        }
        playerVx *= Math.max(0f, 1f - 7.5f * dt);
        playerVy *= Math.max(0f, 1f - 7.5f * dt);
    }

    private void updateCpu(float dt) {
        float oldX = cpuX;
        float oldY = cpuY;
        float defendY = playRect.top + (playRect.height() * 0.23f);
        float targetX = playRect.centerX();
        float targetY = defendY;

        if (puckY < playRect.centerY() + puckRadius) {
            if (puckVy > boardRect.width() * 0.20f) {
                cpuReadyToStrike = false;
            } else {
                float contactDistance = puckRadius + malletRadius * 0.87f;
                float predictedPuckX = puckX + puckVx * 0.055f;
                float behindX = clamp(predictedPuckX,
                        playRect.left + malletRadius, playRect.right - malletRadius);
                float behindY = puckY - contactDistance * 1.08f;

                if (!cpuReadyToStrike) {
                    targetX = behindX;
                    targetY = behindY;
                    if (length(targetX - cpuX, targetY - cpuY) < malletRadius * 0.22f) {
                        cpuReadyToStrike = true;
                    }
                }
                if (cpuReadyToStrike) {
                    targetX = predictedPuckX;
                    targetY = puckY + contactDistance * 0.55f;
                }
            }
        } else {
            cpuReadyToStrike = false;
        }

        targetX = clamp(targetX, playRect.left + malletRadius, playRect.right - malletRadius);
        targetY = clamp(targetY, playRect.top + malletRadius,
                playRect.centerY() - malletRadius - dp(2f));
        float dx = targetX - cpuX;
        float dy = targetY - cpuY;
        float distance = length(dx, dy);
        float maxMove = boardRect.width() * 1.18f * dt;
        if (distance > maxMove && distance > 0f) {
            dx *= maxMove / distance;
            dy *= maxMove / distance;
        }
        cpuX += dx;
        cpuY += dy;
        cpuVx = (cpuX - oldX) / Math.max(dt, 0.001f);
        cpuVy = (cpuY - oldY) / Math.max(dt, 0.001f);
    }

    private void resolveMalletCollision(float malletX, float malletY, float malletVx, float malletVy) {
        float dx = puckX - malletX;
        float dy = puckY - malletY;
        float minimumDistance = puckRadius + malletRadius * 0.87f;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared >= minimumDistance * minimumDistance) {
            return;
        }

        float distance = (float) Math.sqrt(Math.max(0.0001f, distanceSquared));
        float nx = dx / distance;
        float ny = dy / distance;
        puckX = malletX + nx * minimumDistance;
        puckY = malletY + ny * minimumDistance;

        float relativeNormalSpeed = (puckVx - malletVx) * nx + (puckVy - malletVy) * ny;
        if (relativeNormalSpeed < 0f) {
            float impulse = -(1f + MALLET_RESTITUTION) * relativeNormalSpeed;
            puckVx += impulse * nx;
            puckVy += impulse * ny;

            // Transfer only on approach so a mallet cannot continuously pin the puck.
            float malletNormalSpeed = Math.max(0f, malletVx * nx + malletVy * ny);
            puckVx += nx * malletNormalSpeed * 0.28f;
            puckVy += ny * malletNormalSpeed * 0.28f;
        }
    }

    private void resolveWallsAndGoals() {
        if (puckX - puckRadius < playRect.left) {
            puckX = playRect.left + puckRadius;
            puckVx = Math.abs(puckVx) * WALL_RESTITUTION;
        } else if (puckX + puckRadius > playRect.right) {
            puckX = playRect.right - puckRadius;
            puckVx = -Math.abs(puckVx) * WALL_RESTITUTION;
        }

        float goalHalfWidth = playRect.width() * 0.245f;
        boolean insideGoal = Math.abs(puckX - playRect.centerX()) < goalHalfWidth - puckRadius * 0.15f;
        if (puckY - puckRadius < playRect.top) {
            if (insideGoal && puckY < playRect.top - puckRadius * 0.55f) {
                scorePoint(true);
            } else if (!insideGoal) {
                puckY = playRect.top + puckRadius;
                puckVy = Math.abs(puckVy) * WALL_RESTITUTION;
            }
        } else if (puckY + puckRadius > playRect.bottom) {
            if (insideGoal && puckY > playRect.bottom + puckRadius * 0.55f) {
                scorePoint(false);
            } else if (!insideGoal) {
                puckY = playRect.bottom - puckRadius;
                puckVy = -Math.abs(puckVy) * WALL_RESTITUTION;
            }
        }
    }

    private void scorePoint(boolean playerScored) {
        if (playerScored) {
            playerScore++;
        } else {
            cpuScore++;
        }
        puckVx = 0f;
        puckVy = 0f;
        if (playerScore >= WIN_SCORE || cpuScore >= WIN_SCORE) {
            gameOver = true;
            activePointerId = MotionEvent.INVALID_POINTER_ID;
            performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
        } else {
            performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            resetRound(700L);
        }
    }

    private void drawScoreboard(Canvas canvas) {
        drawBitmap(canvas, scoreboard, scoreRect);
        float centerX = scoreRect.centerX();
        float iconSize = scoreRect.width() * 0.82f;
        scoreIconRect.set(centerX - iconSize * 0.5f, scoreRect.centerY() - iconSize * 0.5f,
                centerX + iconSize * 0.5f, scoreRect.centerY() + iconSize * 0.5f);
        scoreIconClip.reset();
        scoreIconClip.addCircle(centerX, scoreRect.centerY(), iconSize * 0.5f,
                Path.Direction.CW);
        canvas.save();
        canvas.clipPath(scoreIconClip);
        drawBitmap(canvas, scoreIcon, scoreIconRect);
        canvas.restore();

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setShadowLayer(dp(1.5f), 0f, dp(1f), 0x88000000);

        textPaint.setTextSize(scoreRect.width() * 0.34f);
        if (cpuScore > 0) {
            canvas.drawText(String.format(Locale.US, "%d", cpuScore), centerX,
                    scoreRect.top + scoreRect.height() * 0.23f, textPaint);
        }
        if (playerScore > 0) {
            canvas.drawText(String.format(Locale.US, "%d", playerScore), centerX,
                    scoreRect.top + scoreRect.height() * 0.89f, textPaint);
        }
        textPaint.clearShadowLayer();
    }

    private void drawGameOver(Canvas canvas) {
        canvas.drawRoundRect(boardRect, dp(18f), dp(18f), overlayPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setShadowLayer(dp(2f), 0f, dp(2f), 0xAA000000);
        textPaint.setTextSize(Math.min(boardRect.width() * 0.12f, sp(32f)));
        canvas.drawText(playerScore > cpuScore ? "VICTORY!" : "GAME OVER",
                boardRect.centerX(), boardRect.centerY() - dp(8f), textPaint);
        textPaint.setTextSize(Math.min(boardRect.width() * 0.055f, sp(17f)));
        canvas.drawText("Tap to play again", boardRect.centerX(),
                boardRect.centerY() + dp(27f), textPaint);
        textPaint.clearShadowLayer();
    }

    private void drawBitmap(Canvas canvas, Bitmap bitmap, RectF destination) {
        canvas.drawBitmap(bitmap, null, destination, bitmapPaint);
    }

    private void drawBitmapCentered(Canvas canvas, Bitmap bitmap, float centerX, float centerY,
                                    float radius) {
        RectF destination = new RectF(centerX - radius, centerY - radius,
                centerX + radius, centerY + radius);
        drawBitmap(canvas, bitmap, destination);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!layoutReady) {
            return false;
        }
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            if (gameOver) {
                resetMatch();
                invalidate();
                return true;
            }
            float dx = event.getX() - playerX;
            float dy = event.getY() - playerY;
            if (dx * dx + dy * dy > malletRadius * malletRadius * 1.55f) {
                return false;
            }
            activePointerId = event.getPointerId(0);
            touchOffsetX = playerX - event.getX();
            touchOffsetY = playerY - event.getY();
            lastTouchNanos = event.getEventTime() * 1_000_000L;
            getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }

        int pointerIndex = event.findPointerIndex(activePointerId);
        if (action == MotionEvent.ACTION_MOVE && pointerIndex >= 0) {
            movePlayer(event.getX(pointerIndex) + touchOffsetX,
                    event.getY(pointerIndex) + touchOffsetY,
                    event.getEventTime() * 1_000_000L);
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                || (action == MotionEvent.ACTION_POINTER_UP
                && event.getPointerId(event.getActionIndex()) == activePointerId)) {
            activePointerId = MotionEvent.INVALID_POINTER_ID;
            playerVx = 0f;
            playerVy = 0f;
            performClick();
            return true;
        }
        return activePointerId != MotionEvent.INVALID_POINTER_ID;
    }

    private void movePlayer(float desiredX, float desiredY, long eventNanos) {
        float nextX = clamp(desiredX, playRect.left + malletRadius, playRect.right - malletRadius);
        float nextY = clamp(desiredY, playRect.centerY() + malletRadius + dp(2f),
                playRect.bottom - malletRadius);
        float dt = Math.max(0.004f, Math.min(0.05f,
                (eventNanos - lastTouchNanos) / 1_000_000_000f));
        float instantVx = (nextX - playerX) / dt;
        float instantVy = (nextY - playerY) / dt;
        float maxMalletSpeed = boardRect.width() * 5f;
        float speed = length(instantVx, instantVy);
        if (speed > maxMalletSpeed) {
            instantVx *= maxMalletSpeed / speed;
            instantVy *= maxMalletSpeed / speed;
        }
        playerVx = playerVx * 0.28f + instantVx * 0.72f;
        playerVy = playerVy * 0.28f + instantVy * 0.72f;
        playerX = nextX;
        playerY = nextY;
        lastTouchNanos = eventNanos;

        resolveMalletCollision(playerX, playerY, playerVx, playerVy);
        invalidate();
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

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float length(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }
}

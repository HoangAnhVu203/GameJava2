package com.example.game43.TapeItUp;

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
import java.util.List;
import java.util.Random;

public class TapeItUpGameView extends View {
    private static final int LANE_COUNT = 3;
    private static final int START_SAFE_ROWS = 4;
    private static final float CONTACT_TOLERANCE_RATIO = 0.08f;
    private static final float LANDING_TOLERANCE_RATIO = 0.56f;
    private static final float LANDING_GRACE_SECONDS = 0.34f;
    private static final float SAW_BLADE_POP_SECONDS = 0.28f;
    private static final float FLYING_COIN_SECONDS = 0.62f;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawRect = new RectF();
    private final RectF playRect = new RectF();
    private final List<BoxRow> rows = new ArrayList<>();
    private final List<TapeStrip> tapeStrips = new ArrayList<>();
    private final List<FlyingCoin> flyingCoins = new ArrayList<>();
    private final Random random = new Random();

    private Bitmap backgroundBitmap;
    private Bitmap brickWallBitmap;
    private Bitmap conveyorBitmap;
    private Bitmap steelBarBitmap;
    private Bitmap powerOutletBitmap;
    private Bitmap wireBitmap;
    private Bitmap tapeBitmap;
    private Bitmap machineBaseBitmap;
    private Bitmap sawBladeBitmap;
    private Bitmap tapeStripBitmap;
    private Bitmap coinBitmap;
    private Bitmap coinIconBitmap;
    private BoxType[] boxTypes;

    private float viewWidth;
    private float viewHeight;
    private float minSide;
    private float laneWidth;
    private float rowHeight;
    private float playerY;
    private float playerLaneX;
    private float playerDrawX;
    private float targetPlayerX;
    private float playerJump;
    private float playerFallOffset;
    private float playerFallVelocity;
    private float playerRotation;
    private float conveyorOffset;
    private float speed;
    private float downStep;
    private float touchStartX;
    private float touchStartY;
    private float landingGrace;
    private final float[] sawBladePop = new float[LANE_COUNT];
    private long lastFrameNanos;
    private int currentLane = 1;
    private int plannedLane = 1;
    private int plannedLaneRun;
    private int lastSideLane = -1;
    private int score;
    private boolean gameOver;
    private boolean swipeHandled;
    private boolean ignoreTouchUntilUp;

    public TapeItUpGameView(Context context) {
        super(context);
        init();
    }

    public TapeItUpGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TapeItUpGameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(true);
        bitmapPaint.setDither(true);
        bitmapPaint.setFilterBitmap(true);

        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_background);
        brickWallBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_brick_wall);
        conveyorBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_conveyor);
        steelBarBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_steel_bar);
        powerOutletBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_power_outlet);
        wireBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_wire);
        tapeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_machine);
        machineBaseBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_machine_base);
        sawBladeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_saw_blade);
        tapeStripBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_strip);
        coinBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_coin);
        coinIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tape_coin_icon);
        boxTypes = new BoxType[] {
                new BoxType(
                        BitmapFactory.decodeResource(getResources(), R.drawable.tape_box_short_open),
                        BitmapFactory.decodeResource(getResources(), R.drawable.tape_box_short_closed),
                        true,
                        false,
                        0.61f),
                new BoxType(
                        BitmapFactory.decodeResource(getResources(), R.drawable.tape_box_medium),
                        BitmapFactory.decodeResource(getResources(), R.drawable.tape_box_medium_sticker),
                        true,
                        false,
                        0.75f),
                new BoxType(
                        BitmapFactory.decodeResource(getResources(), R.drawable.tape_box_short_orange),
                        BitmapFactory.decodeResource(getResources(), R.drawable.tape_box_short_orange),
                        false,
                        true,
                        0f),
                new BoxType(
                        BitmapFactory.decodeResource(getResources(), R.drawable.tape_box_short_blue),
                        BitmapFactory.decodeResource(getResources(), R.drawable.tape_box_short_blue),
                        false,
                        true,
                        0f),
                new BoxType(
                        BitmapFactory.decodeResource(getResources(), R.drawable.tape_box_short_purple),
                        BitmapFactory.decodeResource(getResources(), R.drawable.tape_box_short_purple),
                        false,
                        true,
                        0f),
                new BoxType(
                        BitmapFactory.decodeResource(getResources(), R.drawable.tape_box_round),
                        BitmapFactory.decodeResource(getResources(), R.drawable.tape_box_round),
                        false,
                        true,
                        0f)
        };

        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        shadowPaint.setColor(Color.argb(170, 35, 20, 18));
        shadowPaint.setTextAlign(Paint.Align.CENTER);
        shadowPaint.setFakeBoldText(true);

        overlayPaint.setColor(Color.argb(145, 0, 0, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        configureGame(w, h);
    }

    private void configureGame(int width, int height) {
        viewWidth = width;
        viewHeight = height;
        minSide = Math.min(viewWidth, viewHeight);

        float horizontalMargin = minSide * 0.095f;
        playRect.set(horizontalMargin, 0f, viewWidth - horizontalMargin, viewHeight);
        laneWidth = playRect.width() / LANE_COUNT;
        rowHeight = laneWidth * 0.84f;
        playerY = viewHeight * 0.68f;
        speed = rowHeight * 1.9f;
        downStep = rowHeight;

        resetGame();
    }

    private void resetGame() {
        rows.clear();
        tapeStrips.clear();
        flyingCoins.clear();
        currentLane = 1;
        score = 0;
        gameOver = false;
        conveyorOffset = 0f;
        playerJump = 0f;
        playerFallOffset = 0f;
        playerFallVelocity = 0f;
        playerRotation = 0f;
        landingGrace = 0f;
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            sawBladePop[lane] = 0f;
        }
        ignoreTouchUntilUp = false;
        playerLaneX = laneCenter(currentLane);
        playerDrawX = playerLaneX;
        targetPlayerX = playerLaneX;
        lastFrameNanos = 0L;
        plannedLane = 1;
        lastSideLane = -1;

        float startTop = playerY + rowHeight * 0.42f;
        boolean[] startPattern = {false, true, false};
        float y = startTop;
        for (int i = 0; i < START_SAFE_ROWS; i++) {
            int[] startBoxTypes = createRandomBoxTypes();
            startBoxTypes[1] = 1;
            BoxRow row = createBoxRow(0f, startPattern, startBoxTypes);
            row.top = i == 0 ? startTop : y - getRowSurfaceHeight(row);
            rows.add(row);
            y = row.top;
        }
        plannedLaneRun = START_SAFE_ROWS;

        boolean[] previousPattern = startPattern;
        while (y > -rowHeight * 3f) {
            BoxRow row = createRandomRow(0f, previousPattern);
            row.top = y - getRowSurfaceHeight(row);
            rows.add(row);
            previousPattern = row.filled;
            y = row.top;
        }
    }

    private BoxRow createRandomRow(float top, boolean[] previousPattern) {
        boolean[] filled = new boolean[LANE_COUNT];
        int previousLane = plannedLane;
        int nextLane = chooseNextPlannedLane();
        filled[nextLane] = true;

        if (nextLane != previousLane) {
            filled[previousLane] = true;
        }

        return createBoxRow(top, filled, createRandomBoxTypes());
    }

    private int chooseNextPlannedLane() {
        int nextLane = plannedLane;
        if (plannedLane == 1) {
            if (lastSideLane == 0) {
                nextLane = LANE_COUNT - 1;
            } else if (lastSideLane == LANE_COUNT - 1) {
                nextLane = 0;
            } else {
                nextLane = random.nextBoolean() ? 0 : LANE_COUNT - 1;
            }
        } else {
            if (plannedLaneRun >= 2 || random.nextFloat() < 0.58f) {
                nextLane = chooseAdjacentLane(plannedLane);
            }
        }

        if (nextLane == plannedLane) {
            plannedLaneRun++;
        } else {
            if (plannedLane != 1) {
                lastSideLane = plannedLane;
            }
            if (nextLane != 1) {
                lastSideLane = nextLane;
            }
            plannedLane = nextLane;
            plannedLaneRun = 1;
        }
        return plannedLane;
    }

    private int chooseAdjacentLane(int lane) {
        if (lane == 0) {
            return 1;
        }
        if (lane == LANE_COUNT - 1) {
            return LANE_COUNT - 2;
        }
        return random.nextBoolean() ? lane - 1 : lane + 1;
    }

    private int chooseFilledLane(boolean[] filled) {
        int[] candidates = new int[LANE_COUNT];
        int count = 0;
        for (int i = 0; i < LANE_COUNT; i++) {
            if (filled[i]) {
                candidates[count] = i;
                count++;
            }
        }
        return count == 0 ? 1 : candidates[random.nextInt(count)];
    }

    private int[] createRandomBoxTypes() {
        int[] types = new int[LANE_COUNT];
        for (int i = 0; i < types.length; i++) {
            types[i] = chooseRandomBoxType();
        }
        return types;
    }

    private int chooseRandomBoxType() {
        float roll = random.nextFloat();
        if (roll < 0.3f) {
            return 0;
        }
        if (roll < 0.55f) {
            return 1;
        }
        if (roll < 0.7f) {
            return 2;
        }
        if (roll < 0.73f) {
            return 3;
        }
        if (roll < 0.9f) {
            return 4;
        }
        return 5;
    }

    private BoxRow createBoxRow(float top, boolean[] filled, int[] boxTypes) {
        BoxRow row = new BoxRow(top, filled, boxTypes);
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            if (row.filled[lane] && this.boxTypes[row.boxTypes[lane]].startsClosed) {
                row.closed[lane] = true;
            }
        }
        maybeAddCoin(row);
        return row;
    }

    private void maybeAddCoin(BoxRow row) {
        if (random.nextFloat() > 0.55f) {
            return;
        }

        int[] candidates = new int[LANE_COUNT];
        int count = 0;
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            if (row.filled[lane]) {
                candidates[count] = lane;
                count++;
            }
        }
        if (count > 0) {
            row.hasCoin[candidates[random.nextInt(count)]] = true;
        }
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
            playerJump = Math.max(0f, playerJump - dt * 2.8f);
            playerFallVelocity += gravity() * dt;
            playerFallOffset += playerFallVelocity * dt;
            playerRotation += dt * 360f;
            return;
        }

        float move = speed * dt;
        float machineOutputY = getMachineOutputY();
        conveyorOffset = (conveyorOffset + move) % viewHeight;
        for (BoxRow row : rows) {
            float oldTop = row.top;
            row.top += move;
            triggerSawBladesIfBoxLeavesMachine(row, oldTop, machineOutputY);
        }
        for (TapeStrip strip : tapeStrips) {
            strip.top += move;
        }
        for (int i = flyingCoins.size() - 1; i >= 0; i--) {
            FlyingCoin coin = flyingCoins.get(i);
            coin.elapsed += dt;
            if (coin.elapsed >= FLYING_COIN_SECONDS) {
                flyingCoins.remove(i);
            }
        }
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            sawBladePop[lane] = Math.max(0f, sawBladePop[lane] - dt);
        }

        playerDrawX += (targetPlayerX - playerDrawX) * Math.min(1f, dt * 14f);
        playerJump = Math.max(0f, playerJump - dt * 3.8f);
        landingGrace = Math.max(0f, landingGrace - dt);

        recycleRows();
        BoxRow supportRow = findSupportRow(currentLane);
        if (supportRow == null) {
            if (landingGrace > 0f || playerJump > 0.28f) {
                return;
            }
            startGameOver();
        } else {
            supportRow.closed[currentLane] = true;
            collectCoinIfNeeded(supportRow, currentLane);
            if (playerJump <= 0.35f) {
                addTapeStrip(currentLane, move + dp(3f));
            }
        }
    }

    private void recycleRows() {
        while (!rows.isEmpty()
                && rows.get(0).top > viewHeight) {
            rows.remove(0);
        }

        for (int i = tapeStrips.size() - 1; i >= 0; i--) {
            TapeStrip strip = tapeStrips.get(i);
            if (strip.top > viewHeight) {
                tapeStrips.remove(i);
            }
        }

        while (!rows.isEmpty() && topMostRow().top > -rowHeight) {
            BoxRow anchor = topMostRow();
            BoxRow newRow = createRandomRow(0f, anchor.filled);
            newRow.top = anchor.top - getRowSurfaceHeight(newRow);
            rows.add(newRow);
        }
    }

    private BoxRow topMostRow() {
        BoxRow topRow = rows.get(0);
        for (BoxRow row : rows) {
            if (row.top < topRow.top) {
                topRow = row;
            }
        }
        return topRow;
    }

    private BoxRow findSupportRow(int lane) {
        BoxRow bestRow = null;
        float bestDistance = Float.MAX_VALUE;
        float contactY = getPlayerContactY();
        for (BoxRow row : rows) {
            if (!row.filled[lane]) {
                continue;
            }

            float topSurfaceHeight = getTopSurfaceHeight(row, lane);
            float surfaceBottom = row.top + topSurfaceHeight;
            float tolerance = rowHeight * getCurrentContactToleranceRatio();
            if (contactY < row.top - tolerance || contactY > surfaceBottom + tolerance) {
                continue;
            }

            float distance = Math.abs(row.top - contactY);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestRow = row;
            }
        }
        return bestRow;
    }

    private void addTapeStrip(int lane, float height) {
        if (tapeStripBitmap == null || height <= 0f) {
            return;
        }

        float top = getPlayerBottomY() - dp(2f);
        if (!tapeStrips.isEmpty()) {
            TapeStrip lastStrip = tapeStrips.get(tapeStrips.size() - 1);
            if (lastStrip.lane == lane && lastStrip.top <= top + height + dp(3f)) {
                float bottom = Math.max(lastStrip.top + lastStrip.height, top + height);
                lastStrip.top = Math.min(lastStrip.top, top);
                lastStrip.height = bottom - lastStrip.top;
                return;
            }
        }

        tapeStrips.add(new TapeStrip(lane, top, height));
    }

    private void collectCoinIfNeeded(BoxRow row, int lane) {
        if (!row.hasCoin[lane]) {
            return;
        }

        row.hasCoin[lane] = false;
        score++;
        float startX = laneCenter(lane);
        float startY = getCoinCenterY(row, lane);
        flyingCoins.add(new FlyingCoin(startX, startY));
    }

    private void startGameOver() {
        gameOver = true;
        playerJump = 0f;
        playerFallOffset = 0f;
        playerFallVelocity = rowHeight * 0.35f;
        playerRotation = 0f;
    }

    private void drawGame(Canvas canvas) {
        drawBackground(canvas);
        drawBackWallDecorations(canvas);
        drawConveyor(canvas);
        drawRows(canvas);
        drawCoins(canvas);
        drawTapeStrips(canvas);
        drawSawBlades(canvas);
        drawMachineBase(canvas);
        drawPlayer(canvas);
        drawFlyingCoins(canvas);
        drawHud(canvas);
        if (gameOver) {
            drawGameOver(canvas);
        }
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundBitmap == null) {
            canvas.drawColor(Color.rgb(39, 44, 47));
            return;
        }

        float scale = Math.max(viewWidth / backgroundBitmap.getWidth(), viewHeight / backgroundBitmap.getHeight());
        float width = backgroundBitmap.getWidth() * scale;
        float height = backgroundBitmap.getHeight() * scale;
        drawRect.set((viewWidth - width) * 0.5f, (viewHeight - height) * 0.5f,
                (viewWidth + width) * 0.5f, (viewHeight + height) * 0.5f);
        canvas.drawBitmap(backgroundBitmap, null, drawRect, bitmapPaint);
    }

    private void drawBackWallDecorations(Canvas canvas) {
        drawBrickWall(canvas);
        drawSteelBar(canvas);
        drawWire(canvas);
        drawPowerOutlet(canvas);
    }

    private void drawBrickWall(Canvas canvas) {
        if (brickWallBitmap == null) {
            return;
        }

        float wallHeight = Math.max(getMachineOutputY(), rowHeight * 1.2f);
        float scale = Math.max(viewWidth / brickWallBitmap.getWidth(), wallHeight / brickWallBitmap.getHeight());
        float width = brickWallBitmap.getWidth() * scale;
        float height = brickWallBitmap.getHeight() * scale;
        drawRect.set((viewWidth - width) * 0.5f, 0f,
                (viewWidth + width) * 0.5f, height);
        canvas.save();
        canvas.clipRect(0f, 0f, viewWidth, wallHeight);
        canvas.drawBitmap(brickWallBitmap, null, drawRect, bitmapPaint);
        canvas.restore();
    }

    private void drawSteelBar(Canvas canvas) {
        if (steelBarBitmap == null) {
            return;
        }

        float height = viewHeight;
        float width = Math.max(dp(18f), height * steelBarBitmap.getWidth() / steelBarBitmap.getHeight());
        drawRect.set(0f, 0f, width, height);
        canvas.drawBitmap(steelBarBitmap, null, drawRect, bitmapPaint);
    }

    private void drawWire(Canvas canvas) {
        if (wireBitmap == null) {
            return;
        }

        float height = viewHeight;
        float width = Math.max(dp(18f), height * wireBitmap.getWidth() / wireBitmap.getHeight());
        drawRect.set(viewWidth - width, 0f, viewWidth, height);
        canvas.drawBitmap(wireBitmap, null, drawRect, bitmapPaint);
    }

    private void drawPowerOutlet(Canvas canvas) {
        if (powerOutletBitmap == null) {
            return;
        }

        float height = minSide * 0.3f;
        float width = height * powerOutletBitmap.getWidth() / powerOutletBitmap.getHeight();
        float right = viewWidth - dp(6f);
        float top = Math.max(dp(58f), getMachineOutputY() + rowHeight * 0.12f);
        drawRect.set(right - width, top, right, top + height);
        canvas.drawBitmap(powerOutletBitmap, null, drawRect, bitmapPaint);
    }

    private void drawConveyor(Canvas canvas) {
        if (conveyorBitmap == null) {
            return;
        }

        float conveyorHeight = playRect.width() * conveyorBitmap.getHeight() / conveyorBitmap.getWidth();
        float firstTop = -conveyorHeight + conveyorOffset;
        for (float top = firstTop; top < viewHeight + conveyorHeight; top += conveyorHeight) {
            drawRect.set(playRect.left, top, playRect.right, top + conveyorHeight);
            canvas.drawBitmap(conveyorBitmap, null, drawRect, bitmapPaint);
        }
    }

    private void drawRows(Canvas canvas) {
        for (int rowIndex = rows.size() - 1; rowIndex >= 0; rowIndex--) {
            BoxRow row = rows.get(rowIndex);
            for (int lane = 0; lane < LANE_COUNT; lane++) {
                if (!row.filled[lane]) {
                    continue;
                }

                Bitmap boxBitmap = boxTypes[row.boxTypes[lane]].bitmapFor(row.closed[lane]);
                float centerX = laneCenter(lane);
                float boxWidth = laneWidth * 0.98f;
                float boxHeight = boxWidth * boxBitmap.getHeight() / boxBitmap.getWidth();
                float visualTop = row.top;
                drawRect.set(centerX - boxWidth * 0.5f, visualTop,
                        centerX + boxWidth * 0.5f, visualTop + boxHeight);
                canvas.drawBitmap(boxBitmap, null, drawRect, bitmapPaint);
            }
        }
    }

    private void drawCoins(Canvas canvas) {
        if (coinBitmap == null) {
            return;
        }

        float width = laneWidth * 0.28f;
        float height = width * coinBitmap.getHeight() / coinBitmap.getWidth();
        for (int rowIndex = rows.size() - 1; rowIndex >= 0; rowIndex--) {
            BoxRow row = rows.get(rowIndex);
            for (int lane = 0; lane < LANE_COUNT; lane++) {
                if (!row.filled[lane] || !row.hasCoin[lane]) {
                    continue;
                }

                float centerX = laneCenter(lane);
                float centerY = getCoinCenterY(row, lane);
                drawRect.set(centerX - width * 0.5f, centerY - height * 0.5f,
                        centerX + width * 0.5f, centerY + height * 0.5f);
                canvas.drawBitmap(coinBitmap, null, drawRect, bitmapPaint);
            }
        }
    }

    private void drawTapeStrips(Canvas canvas) {
        if (tapeStripBitmap == null) {
            return;
        }

        float stripWidth = laneWidth * 0.28f;
        for (TapeStrip strip : tapeStrips) {
            float centerX = laneCenter(strip.lane);
            drawRect.set(centerX - stripWidth * 0.5f, strip.top,
                    centerX + stripWidth * 0.5f, strip.top + strip.height);
            canvas.drawBitmap(tapeStripBitmap, null, drawRect, bitmapPaint);
        }
    }

    private void drawMachineBase(Canvas canvas) {
        if (machineBaseBitmap == null) {
            return;
        }

        float width = getMachineBaseWidth();
        float height = getMachineBaseHeight();
        float left = getMachineBaseLeft();
        float top = getMachineBaseTop();
        drawRect.set(left, top, left + width, top + height);
        canvas.drawBitmap(machineBaseBitmap, null, drawRect, bitmapPaint);
    }

    private void drawSawBlades(Canvas canvas) {
        if (sawBladeBitmap == null) {
            return;
        }

        float outputY = getMachineOutputY();
        float size = laneWidth * 0.42f;
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            float progress = sawBladePop[lane] / SAW_BLADE_POP_SECONDS;
            float easedProgress = (float) Math.sin(progress * Math.PI);
            float rise = size * (0.06f + easedProgress * 0.42f);
            float baseBottom = outputY + size * 0.55f;
            float[] offsets = {-laneWidth * 0.22f, laneWidth * 0.22f};
            for (float offset : offsets) {
                float centerX = laneCenter(lane) + offset;
                drawRect.set(centerX - size * 0.5f, baseBottom - rise - size,
                        centerX + size * 0.5f, baseBottom - rise);
                canvas.drawBitmap(sawBladeBitmap, null, drawRect, bitmapPaint);
            }
        }
    }

    private void triggerSawBladesIfBoxLeavesMachine(BoxRow row, float oldTop, float machineOutputY) {
        if (oldTop < machineOutputY && row.top >= machineOutputY) {
            for (int lane = 0; lane < LANE_COUNT; lane++) {
                if (row.filled[lane]) {
                    sawBladePop[lane] = SAW_BLADE_POP_SECONDS;
                }
            }
        }
    }

    private float getMachineBaseWidth() {
        return playRect.width() * 1.18f;
    }

    private float getMachineBaseHeight() {
        if (machineBaseBitmap == null) {
            return rowHeight * 1.8f;
        }
        return getMachineBaseWidth() * machineBaseBitmap.getHeight() / machineBaseBitmap.getWidth();
    }

    private float getMachineBaseLeft() {
        return playRect.centerX() - getMachineBaseWidth() * 0.5f;
    }

    private float getMachineBaseTop() {
        return -getMachineBaseHeight() * 0.52f;
    }

    private float getMachineOutputY() {
        return getMachineBaseTop() + getMachineBaseHeight() * 0.88f;
    }

    private float getTopSurfaceHeight(BoxRow row, int lane) {
        BoxType type = boxTypes[row.boxTypes[lane]];
        if (type.surfaceRatio <= 0f) {
            return rowHeight;
        }

        float boxWidth = laneWidth * 0.98f;
        float boxHeight = boxWidth * type.openBitmap.getHeight() / type.openBitmap.getWidth();
        return boxHeight * type.surfaceRatio;
    }

    private float getRowSurfaceHeight(BoxRow row) {
        float surfaceHeight = 0f;
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            if (!row.filled[lane]) {
                continue;
            }

            surfaceHeight = Math.max(surfaceHeight, getTopSurfaceHeight(row, lane));
        }
        return surfaceHeight > 0f ? surfaceHeight : rowHeight;
    }

    private BoxRow findConnectedBoxBehind(BoxRow currentRow, int lane) {
        BoxRow behindRow = null;
        float closestDistance = Float.MAX_VALUE;
        for (BoxRow row : rows) {
            if (row == currentRow || !row.filled[lane] || row.top >= currentRow.top) {
                continue;
            }

            float distance = currentRow.top - row.top;
            if (distance < closestDistance) {
                closestDistance = distance;
                behindRow = row;
            }
        }

        return closestDistance <= rowHeight * 1.08f ? behindRow : null;
    }

    private void drawPlayer(Canvas canvas) {
        if (tapeBitmap == null) {
            return;
        }

        float width = laneWidth * 0.62f;
        float height = width * tapeBitmap.getHeight() / tapeBitmap.getWidth();
        float bottom = getPlayerBottomY();
        drawRect.set(playerDrawX - width * 0.5f, bottom - height,
                playerDrawX + width * 0.5f, bottom);
        if (gameOver) {
            canvas.save();
            canvas.rotate(playerRotation, drawRect.centerX(), drawRect.centerY());
            canvas.drawBitmap(tapeBitmap, null, drawRect, bitmapPaint);
            canvas.restore();
            return;
        }
        canvas.drawBitmap(tapeBitmap, null, drawRect, bitmapPaint);
    }

    private void drawFlyingCoins(Canvas canvas) {
        if (coinBitmap == null) {
            return;
        }

        float targetX = getHudCoinCenterX();
        float targetY = getHudCoinCenterY();
        for (FlyingCoin coin : flyingCoins) {
            float t = Math.min(1f, coin.elapsed / FLYING_COIN_SECONDS);
            float eased = 1f - (1f - t) * (1f - t);
            float x = coin.startX + (targetX - coin.startX) * eased;
            float y = coin.startY + (targetY - coin.startY) * eased - (float) Math.sin(t * Math.PI) * rowHeight * 0.45f;
            float width = laneWidth * (0.27f - 0.09f * eased);
            float height = width * coinBitmap.getHeight() / coinBitmap.getWidth();
            drawRect.set(x - width * 0.5f, y - height * 0.5f,
                    x + width * 0.5f, y + height * 0.5f);
            canvas.drawBitmap(coinBitmap, null, drawRect, bitmapPaint);
        }
    }

    private void drawHud(Canvas canvas) {
        if (coinIconBitmap != null) {
            float size = getHudCoinIconSize();
            drawRect.set(getHudCoinLeft(), getHudCoinTop(), getHudCoinLeft() + size, getHudCoinTop() + size);
            canvas.drawBitmap(coinIconBitmap, null, drawRect, bitmapPaint);
        }

        textPaint.setTextSize(minSide * 0.06f);
        shadowPaint.setTextSize(minSide * 0.06f);
        String scoreText = String.valueOf(score);
        float x = dp(24f) + minSide * 0.09f;
        float y = dp(16f) + minSide * 0.058f;
        canvas.drawText(scoreText, x + dp(1.5f), y + dp(1.5f), shadowPaint);
        canvas.drawText(scoreText, x, y, textPaint);
    }

    private void drawGameOver(Canvas canvas) {
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, overlayPaint);
        textPaint.setTextSize(minSide * 0.09f);
        shadowPaint.setTextSize(minSide * 0.09f);
        float centerY = viewHeight * 0.44f;
        canvas.drawText("Game Over", viewWidth * 0.5f + dp(2f), centerY + dp(2f), shadowPaint);
        canvas.drawText("Game Over", viewWidth * 0.5f, centerY, textPaint);

        textPaint.setTextSize(minSide * 0.045f);
        shadowPaint.setTextSize(minSide * 0.045f);
        float restartY = centerY + minSide * 0.09f;
        canvas.drawText("Tap to restart", viewWidth * 0.5f + dp(1f), restartY + dp(1f), shadowPaint);
        canvas.drawText("Tap to restart", viewWidth * 0.5f, restartY, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                swipeHandled = false;
                if (gameOver) {
                    resetGame();
                    ignoreTouchUntilUp = true;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (ignoreTouchUntilUp) {
                    return true;
                }
                if (!gameOver && !swipeHandled) {
                    swipeHandled = handleSwipe(event.getX() - touchStartX, event.getY() - touchStartY);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (ignoreTouchUntilUp) {
                    ignoreTouchUntilUp = false;
                    return true;
                }
                if (!gameOver && !swipeHandled) {
                    swipeHandled = handleSwipe(event.getX() - touchStartX, event.getY() - touchStartY);
                }
                return true;
            default:
                return true;
        }
    }

    private boolean handleSwipe(float dx, float dy) {
        if (Math.abs(dx) < dp(22f) || Math.abs(dx) < Math.abs(dy)) {
            return false;
        }

        int direction = dx > 0f ? 1 : -1;
        int targetLane = clampLane(currentLane + direction);
        if (targetLane == currentLane) {
            return false;
        }

        currentLane = targetLane;
        targetPlayerX = laneCenter(currentLane);
        playerJump = 1f;
        landingGrace = LANDING_GRACE_SECONDS;
        return true;
    }

    private float laneCenter(int lane) {
        return playRect.left + laneWidth * (lane + 0.5f);
    }

    private int clampLane(int lane) {
        return Math.max(0, Math.min(LANE_COUNT - 1, lane));
    }

    private float getPlayerContactY() {
        return playerY + rowHeight * 0.42f;
    }

    private float getCoinCenterY(BoxRow row, int lane) {
        return row.top + getTopSurfaceHeight(row, lane) * 0.36f;
    }

    private float getHudCoinIconSize() {
        return minSide * 0.08f;
    }

    private float getHudCoinLeft() {
        return dp(14f);
    }

    private float getHudCoinTop() {
        return dp(16f);
    }

    private float getHudCoinCenterX() {
        return getHudCoinLeft() + getHudCoinIconSize() * 0.5f;
    }

    private float getHudCoinCenterY() {
        return getHudCoinTop() + getHudCoinIconSize() * 0.5f;
    }

    private float getCurrentContactToleranceRatio() {
        if (landingGrace > 0f || playerJump > 0.18f) {
            return LANDING_TOLERANCE_RATIO;
        }
        return CONTACT_TOLERANCE_RATIO;
    }

    private float getPlayerBottomY() {
        float jumpOffset = (float) Math.sin(playerJump * Math.PI) * rowHeight * 0.38f;
        return playerY + rowHeight * 0.05f - jumpOffset + playerFallOffset;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float gravity() {
        return viewHeight * 2.4f;
    }

    private static final class BoxType {
        final Bitmap openBitmap;
        final Bitmap closedBitmap;
        final boolean closesWhenTouched;
        final boolean startsClosed;
        final float surfaceRatio;

        BoxType(Bitmap openBitmap, Bitmap closedBitmap, boolean closesWhenTouched, boolean startsClosed,
                float surfaceRatio) {
            this.openBitmap = openBitmap;
            this.closedBitmap = closedBitmap;
            this.closesWhenTouched = closesWhenTouched;
            this.startsClosed = startsClosed;
            this.surfaceRatio = surfaceRatio;
        }

        Bitmap bitmapFor(boolean closed) {
            if (startsClosed || !closesWhenTouched) {
                return closedBitmap;
            }
            return closed ? closedBitmap : openBitmap;
        }
    }

    private static final class BoxRow {
        float top;
        final boolean[] filled;
        final int[] boxTypes;
        final boolean[] closed;
        final boolean[] hasCoin;

        BoxRow(float top, boolean[] filled, int[] boxTypes) {
            this.top = top;
            this.filled = filled;
            this.boxTypes = boxTypes;
            this.closed = new boolean[LANE_COUNT];
            this.hasCoin = new boolean[LANE_COUNT];
        }
    }

    private static final class TapeStrip {
        final int lane;
        float top;
        float height;

        TapeStrip(int lane, float top, float height) {
            this.lane = lane;
            this.top = top;
            this.height = height;
        }
    }

    private static final class FlyingCoin {
        final float startX;
        final float startY;
        float elapsed;

        FlyingCoin(float startX, float startY) {
            this.startX = startX;
            this.startY = startY;
        }
    }
}

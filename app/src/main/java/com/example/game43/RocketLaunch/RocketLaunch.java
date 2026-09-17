package com.example.game43.RocketLaunch;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.game43.R;

import java.util.Locale;

public class RocketLaunch extends AppCompatActivity {
    private static final String PREFS_NAME = "rocket_launch_progress";
    private static final float MAX_FRAME_SECONDS = 1f / 30f;

    private enum FlightState {
        READY,
        FLYING,
        FALLING
    }

    private View flightArea;
    private View rocketActor;
    private View rocketFlame;
    private RocketExhaustView rocketExhaust;
    private View rocketPlatform;
    private View rocketSupport;
    private View planet;
    private ImageView groundBackground;
    private ImageView spaceBackgroundOne;
    private ImageView spaceBackgroundTwo;
    private RocketFuelView fuelView;
    private TextView fuelValueText;
    private TextView moneyText;
    private TextView altitudeText;
    private TextView speedText;
    private TextView launchButton;
    private TextView holdLabel;
    private TextView incomePopup;
    private TextView speedLevelText;
    private TextView fuelLevelText;
    private TextView incomeLevelText;
    private TextView powerLevelText;
    private TextView speedUpgradeButton;
    private TextView fuelUpgradeButton;
    private TextView incomeUpgradeButton;
    private TextView powerUpgradeButton;

    private FlightState flightState = FlightState.READY;
    private SharedPreferences preferences;
    private long lastFrameNanos;
    private boolean framePosted;
    private boolean holdingRocket;
    private float altitude;
    private float verticalSpeed;
    private float fuel;
    private float autoBoostRemaining;
    private float incomeTimer;
    private float hudTimer;
    private int lastFuelPercent = -1;
    private int money;
    private int speedLevel = 1;
    private int fuelLevel = 1;
    private int incomeLevel = 1;
    private int powerLevel = 1;

    private final Choreographer.FrameCallback frameCallback = frameTimeNanos -> {
        framePosted = false;
        if (flightState == FlightState.READY) {
            return;
        }
        if (lastFrameNanos == 0L) {
            lastFrameNanos = frameTimeNanos;
            scheduleFrame();
            return;
        }
        float dt = Math.min(MAX_FRAME_SECONDS,
                (frameTimeNanos - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = frameTimeNanos;
        updateFlight(dt, frameTimeNanos);
        scheduleFrame();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rocket_launch);
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(
                getWindow(), findViewById(R.id.main));
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);
        bindViews();
        loadProgress();
        resetRocketVisuals();
        updateUpgradeUi();

        launchButton.setOnClickListener(v -> startLaunch());
        bindRocketHoldInput();
        speedUpgradeButton.setOnClickListener(v -> buyUpgrade(Upgrade.SPEED, v));
        fuelUpgradeButton.setOnClickListener(v -> buyUpgrade(Upgrade.FUEL, v));
        incomeUpgradeButton.setOnClickListener(v -> buyUpgrade(Upgrade.INCOME, v));
        powerUpgradeButton.setOnClickListener(v -> buyUpgrade(Upgrade.POWER, v));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void bindViews() {
        flightArea = findViewById(R.id.flightArea);
        rocketActor = findViewById(R.id.rocketActor);
        rocketFlame = findViewById(R.id.rocketFlame);
        rocketExhaust = findViewById(R.id.rocketExhaust);
        rocketPlatform = findViewById(R.id.rocketPlatform);
        rocketSupport = findViewById(R.id.rocketSupport);
        planet = findViewById(R.id.planet);
        groundBackground = findViewById(R.id.groundBackground);
        spaceBackgroundOne = findViewById(R.id.spaceBackgroundOne);
        spaceBackgroundTwo = findViewById(R.id.spaceBackgroundTwo);
        fuelView = findViewById(R.id.fuelView);
        fuelValueText = findViewById(R.id.fuelValueText);
        moneyText = findViewById(R.id.moneyText);
        altitudeText = findViewById(R.id.altitudeText);
        speedText = findViewById(R.id.speedText);
        launchButton = findViewById(R.id.launchButton);
        holdLabel = findViewById(R.id.holdLabel);
        incomePopup = findViewById(R.id.incomePopup);
        speedLevelText = findViewById(R.id.speedLevelText);
        fuelLevelText = findViewById(R.id.fuelLevelText);
        incomeLevelText = findViewById(R.id.incomeLevelText);
        powerLevelText = findViewById(R.id.powerLevelText);
        speedUpgradeButton = findViewById(R.id.speedUpgradeButton);
        fuelUpgradeButton = findViewById(R.id.fuelUpgradeButton);
        incomeUpgradeButton = findViewById(R.id.incomeUpgradeButton);
        powerUpgradeButton = findViewById(R.id.powerUpgradeButton);
    }

    private void bindRocketHoldInput() {
        rocketActor.setOnTouchListener((view, event) -> {
            if (flightState == FlightState.READY) {
                return false;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    holdingRocket = fuel > 0f;
                    updateHoldUi();
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    return true;
                case MotionEvent.ACTION_UP:
                    holdingRocket = false;
                    updateHoldUi();
                    view.performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    holdingRocket = false;
                    updateHoldUi();
                    return true;
                default:
                    return false;
            }
        });
    }

    private void startLaunch() {
        if (flightState != FlightState.READY) {
            return;
        }
        flightState = FlightState.FLYING;
        fuel = fuelCapacity();
        verticalSpeed = dp(12f + powerLevel * 4f);
        altitude = 0f;
        autoBoostRemaining = 0.9f;
        incomeTimer = 0f;
        holdingRocket = false;
        launchButton.setEnabled(false);
        launchButton.setAlpha(0.62f);
        launchButton.setText("FLYING");
        holdLabel.setVisibility(View.VISIBLE);
        updateHoldUi();
        lastFrameNanos = 0L;
        scheduleFrame();
    }

    private void updateFlight(float dt, long frameTimeNanos) {
        if (autoBoostRemaining > 0f) {
            autoBoostRemaining = Math.max(0f, autoBoostRemaining - dt);
        }
        boolean thrusting = flightState == FlightState.FLYING
                && fuel > 0f
                && (holdingRocket || autoBoostRemaining > 0f);

        float maxUpwardSpeed = dp(76f + speedLevel * 14f);
        float acceleration = dp(58f + powerLevel * 11f);
        float gravity = dp(52f);
        if (thrusting) {
            verticalSpeed = Math.min(maxUpwardSpeed, verticalSpeed + acceleration * dt);
            fuel = Math.max(0f, fuel - dt);
            if (fuel == 0f) {
                flightState = FlightState.FALLING;
                holdingRocket = false;
                updateHoldUi();
            }
        } else {
            verticalSpeed -= gravity * dt;
        }

        altitude = Math.max(0f, altitude + verticalSpeed * dt);
        if (altitude <= 0f && verticalSpeed < 0f && autoBoostRemaining <= 0f) {
            finishFlight();
            return;
        }

        if (verticalSpeed > 0f && altitude > dp(8f)) {
            incomeTimer += dt;
            while (incomeTimer >= 1f) {
                incomeTimer -= 1f;
                awardIncome();
            }
        }

        updateWorldVisuals(thrusting, frameTimeNanos);
        hudTimer += dt;
        if (hudTimer >= 0.08f) {
            hudTimer = 0f;
            updateFlightHud();
        }
    }

    private void updateWorldVisuals(boolean thrusting, long frameTimeNanos) {
        float maxRocketTravel = Math.max(dp(80f), rocketActor.getTop() - dp(78f));
        float rocketTravel = Math.min(altitude, maxRocketTravel);
        rocketActor.setTranslationY(-rocketTravel);

        float spaceProgress = clamp(altitude / dp(260f));
        spaceBackgroundOne.setAlpha(spaceProgress);
        spaceBackgroundTwo.setAlpha(spaceProgress);
        groundBackground.setAlpha(1f - spaceProgress * 0.92f);
        planet.setAlpha(1f - spaceProgress);

        float groundFade = 1f - clamp(altitude / dp(105f));
        rocketPlatform.setAlpha(groundFade);
        rocketSupport.setAlpha(groundFade);

        int backgroundHeight = Math.max(1, spaceBackgroundOne.getHeight());
        float worldScroll = (Math.max(0f, altitude - maxRocketTravel) * 0.22f)
                % backgroundHeight;
        spaceBackgroundOne.setTranslationY(worldScroll);
        spaceBackgroundTwo.setTranslationY(worldScroll - backgroundHeight);

        rocketFlame.setVisibility(thrusting ? View.VISIBLE : View.INVISIBLE);
        rocketExhaust.setThrusting(thrusting);
        rocketExhaust.update(frameTimeNanos,
                clamp(Math.abs(verticalSpeed) / dp(105f)));
        if (thrusting) {
            float pulse = 0.88f + 0.16f
                    * (float) Math.sin(frameTimeNanos / 75_000_000d);
            rocketFlame.setScaleX(pulse);
            rocketFlame.setScaleY(0.92f + pulse * 0.26f);
            rocketActor.setRotation(0.35f
                    * (float) Math.sin(frameTimeNanos / 130_000_000d));
        } else {
            rocketActor.setRotation(0f);
        }
    }

    private void updateFlightHud() {
        int percent = Math.round(100f * fuel / fuelCapacity());
        if (percent != lastFuelPercent) {
            lastFuelPercent = percent;
            fuelView.setFuelFraction(percent / 100f);
            fuelValueText.setText(String.format(Locale.US, "%d%%", percent));
        }
        float altitudeMeters = altitude / dp(4.5f);
        float speedMeters = verticalSpeed / dp(8f);
        altitudeText.setText(String.format(Locale.US, "%.0f m", altitudeMeters));
        speedText.setText(String.format(Locale.US, "%.1f m/s", speedMeters));
    }

    private void awardIncome() {
        int earned = 1 + incomeLevel;
        money += earned;
        moneyText.setText(String.valueOf(money));
        updateUpgradeAffordability();

        incomePopup.animate().cancel();
        incomePopup.setText(String.format(Locale.US, "+$%d", earned));
        incomePopup.setAlpha(1f);
        incomePopup.setTranslationY(rocketActor.getTranslationY() - dp(265f));
        incomePopup.animate()
                .translationY(rocketActor.getTranslationY() - dp(300f))
                .alpha(0f)
                .setDuration(700L)
                .start();
    }

    private void finishFlight() {
        flightState = FlightState.READY;
        holdingRocket = false;
        altitude = 0f;
        verticalSpeed = 0f;
        fuel = fuelCapacity();
        lastFrameNanos = 0L;
        resetRocketVisuals();
        saveProgress();
    }

    private void resetRocketVisuals() {
        rocketActor.setTranslationY(0f);
        rocketActor.setRotation(0f);
        rocketFlame.setVisibility(View.INVISIBLE);
        rocketExhaust.reset();
        rocketPlatform.setAlpha(1f);
        rocketSupport.setAlpha(1f);
        planet.setAlpha(1f);
        groundBackground.setAlpha(1f);
        spaceBackgroundOne.setAlpha(0f);
        spaceBackgroundTwo.setAlpha(0f);
        holdLabel.setVisibility(View.INVISIBLE);
        launchButton.setEnabled(true);
        launchButton.setAlpha(1f);
        launchButton.setText("LAUNCH");
        fuel = fuelCapacity();
        lastFuelPercent = -1;
        updateFlightHud();
    }

    private void updateHoldUi() {
        if (flightState == FlightState.READY) {
            holdLabel.setVisibility(View.INVISIBLE);
            return;
        }
        holdLabel.setVisibility(View.VISIBLE);
        holdLabel.setText(holdingRocket ? "THRUST" : "HOLD");
        holdLabel.setTextColor(holdingRocket ? 0xFF6FE74B : 0xFFFBC84A);
    }

    private enum Upgrade {
        SPEED,
        FUEL,
        INCOME,
        POWER
    }

    private void buyUpgrade(Upgrade upgrade, View button) {
        int cost = upgradeCost(upgrade);
        if (money < cost) {
            button.animate().cancel();
            button.animate().alpha(0.35f).setDuration(90L)
                    .withEndAction(() -> button.animate().alpha(1f)
                            .setDuration(150L).start())
                    .start();
            return;
        }

        money -= cost;
        switch (upgrade) {
            case SPEED:
                speedLevel++;
                break;
            case FUEL:
                fuelLevel++;
                if (flightState == FlightState.READY) {
                    fuel = fuelCapacity();
                }
                break;
            case INCOME:
                incomeLevel++;
                break;
            case POWER:
                powerLevel++;
                break;
        }
        updateUpgradeUi();
        if (flightState == FlightState.READY) {
            resetRocketVisuals();
        }
        saveProgress();
    }

    private int upgradeCost(Upgrade upgrade) {
        int base;
        int level;
        switch (upgrade) {
            case SPEED:
                base = 5;
                level = speedLevel;
                break;
            case FUEL:
                base = 8;
                level = fuelLevel;
                break;
            case INCOME:
                base = 8;
                level = incomeLevel;
                break;
            case POWER:
            default:
                base = 12;
                level = powerLevel;
                break;
        }
        return Math.max(base, Math.round((float) (base * Math.pow(1.55d, level - 1))));
    }

    private void updateUpgradeUi() {
        moneyText.setText(String.valueOf(money));
        speedLevelText.setText(String.format(Locale.US, "Lv. %d", speedLevel));
        fuelLevelText.setText(String.format(Locale.US, "Lv. %d", fuelLevel));
        incomeLevelText.setText(String.format(Locale.US, "Lv. %d", incomeLevel));
        powerLevelText.setText(String.format(Locale.US, "Lv. %d", powerLevel));
        speedUpgradeButton.setText(String.format(Locale.US, "$%d", upgradeCost(Upgrade.SPEED)));
        fuelUpgradeButton.setText(String.format(Locale.US, "$%d", upgradeCost(Upgrade.FUEL)));
        incomeUpgradeButton.setText(String.format(Locale.US, "$%d", upgradeCost(Upgrade.INCOME)));
        powerUpgradeButton.setText(String.format(Locale.US, "$%d", upgradeCost(Upgrade.POWER)));
        updateUpgradeAffordability();
    }

    private void updateUpgradeAffordability() {
        setAffordable(speedUpgradeButton, money >= upgradeCost(Upgrade.SPEED));
        setAffordable(fuelUpgradeButton, money >= upgradeCost(Upgrade.FUEL));
        setAffordable(incomeUpgradeButton, money >= upgradeCost(Upgrade.INCOME));
        setAffordable(powerUpgradeButton, money >= upgradeCost(Upgrade.POWER));
    }

    private void setAffordable(View button, boolean affordable) {
        button.setAlpha(affordable ? 1f : 0.58f);
    }

    private float fuelCapacity() {
        return 5.5f + fuelLevel * 1.5f;
    }

    private void scheduleFrame() {
        if (!framePosted && flightState != FlightState.READY) {
            framePosted = true;
            Choreographer.getInstance().postFrameCallback(frameCallback);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private void loadProgress() {
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        money = preferences.getInt("money", 0);
        speedLevel = preferences.getInt("speed_level", 1);
        fuelLevel = preferences.getInt("fuel_level", 1);
        incomeLevel = preferences.getInt("income_level", 1);
        powerLevel = preferences.getInt("power_level", 1);
    }

    private void saveProgress() {
        if (preferences == null) {
            return;
        }
        preferences.edit()
                .putInt("money", money)
                .putInt("speed_level", speedLevel)
                .putInt("fuel_level", fuelLevel)
                .putInt("income_level", incomeLevel)
                .putInt("power_level", powerLevel)
                .apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        lastFrameNanos = 0L;
        scheduleFrame();
    }

    @Override
    protected void onPause() {
        holdingRocket = false;
        if (framePosted) {
            Choreographer.getInstance().removeFrameCallback(frameCallback);
            framePosted = false;
        }
        saveProgress();
        super.onPause();
    }
}

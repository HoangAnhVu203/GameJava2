package com.example.game43.TinyFishing;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.game43.R;

public class TinyFishing extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tiny_fishing);

        TinyFishingGameView gameView = findViewById(R.id.tinyFishingGame);
        TextView moneyText = findViewById(R.id.tinyFishingMoneyText);
        View rewardOverlay = findViewById(R.id.tinyFishingRewardOverlay);
        FrameLayout rewardYesButton = findViewById(R.id.tinyFishingRewardYesButton);
        FrameLayout rewardNoButton = findViewById(R.id.tinyFishingRewardNoButton);
        gameView.setMoneyStateListener(money -> moneyText.setText(formatCompactMoney(money)));
        gameView.setCastTurnRequestListener(() -> rewardOverlay.setVisibility(View.VISIBLE));
        rewardYesButton.setOnClickListener(v -> {
            rewardOverlay.setVisibility(View.GONE);
            gameView.grantCastTurn();
        });
        rewardNoButton.setOnClickListener(v -> rewardOverlay.setVisibility(View.GONE));
    }

    private String formatCompactMoney(int value) {
        if (value >= 1_000_000_000) {
            return trimCompact(value / 1_000_000_000f) + "b";
        }
        if (value >= 1_000_000) {
            return trimCompact(value / 1_000_000f) + "m";
        }
        if (value >= 1_000) {
            return trimCompact(value / 1_000f) + "k";
        }
        return String.valueOf(value);
    }

    private String trimCompact(float value) {
        if (value >= 100 || value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format(java.util.Locale.US, "%.1f", value);
    }
}

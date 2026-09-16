package com.example.game43.GameBasketball;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.game43.R;

public class Basketball extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_basketball);

        BasketballGameView gameView = findViewById(R.id.basketballGame);
        View rewardOverlay = findViewById(R.id.basketballRewardOverlay);
        gameView.setExtraTimeRequestListener(() -> rewardOverlay.setVisibility(View.VISIBLE));
        findViewById(R.id.basketballRewardYesButton).setOnClickListener(v -> {
            rewardOverlay.setVisibility(View.GONE);
            gameView.grantExtraTime();
        });
        findViewById(R.id.basketballRewardNoButton).setOnClickListener(v -> rewardOverlay.setVisibility(View.GONE));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}

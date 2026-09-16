package com.example.game43.GameBall;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.game43.R;

public class Ball extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ball);

        BallGameView gameView = findViewById(R.id.ballGame);
        View rewardOverlay = findViewById(R.id.ballRewardOverlay);

        gameView.setAddBallRequestListener(() -> rewardOverlay.setVisibility(View.VISIBLE));
        findViewById(R.id.ballRewardYesButton).setOnClickListener(v -> {
            rewardOverlay.setVisibility(View.GONE);
            gameView.grantExtraBall();
        });
        findViewById(R.id.ballRewardNoButton).setOnClickListener(v -> rewardOverlay.setVisibility(View.GONE));
    }
}

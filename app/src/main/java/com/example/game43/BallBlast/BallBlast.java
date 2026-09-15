package com.example.game43.BallBlast;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.game43.R;

public class BallBlast extends AppCompatActivity {
    private BallBlastGameView gameView;
    private View gameOverOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ball_blast);

        gameView = findViewById(R.id.ballBlastGame);
        gameOverOverlay = findViewById(R.id.ballBlastGameOverOverlay);

        gameView.setGameOverListener(() -> gameOverOverlay.setVisibility(View.VISIBLE));
        findViewById(R.id.ballBlastPlayAgainButton).setOnClickListener(v -> {
            gameOverOverlay.setVisibility(View.GONE);
            gameView.resetGame();
        });
        findViewById(R.id.ballBlastQuitButton).setOnClickListener(v -> finish());
    }
}

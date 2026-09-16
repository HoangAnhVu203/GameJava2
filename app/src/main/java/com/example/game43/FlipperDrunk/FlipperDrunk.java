package com.example.game43.FlipperDrunk;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.game43.R;

public class FlipperDrunk extends AppCompatActivity {
    private FlipperDrunkGameView gameView;
    private View gameOverOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_flipper_drunk);

        gameView = findViewById(R.id.flipperDrunkGame);
        gameOverOverlay = findViewById(R.id.flipperDrunkGameOverOverlay);
        gameView.setGameOverListener(() -> gameOverOverlay.setVisibility(View.VISIBLE));
        findViewById(R.id.flipperDrunkPlayAgainButton).setOnClickListener(v -> {
            gameOverOverlay.setVisibility(View.GONE);
            gameView.resetGame();
        });
        findViewById(R.id.flipperDrunkQuitButton).setOnClickListener(v -> finish());
    }
}

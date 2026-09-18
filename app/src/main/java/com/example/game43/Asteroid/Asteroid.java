package com.example.game43.Asteroid;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.game43.R;

public class Asteroid extends AppCompatActivity {
    private AsteroidGameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(R.layout.activity_asteroid);
        gameView = findViewById(R.id.asteroidGameView);
        View timeOverlay = findViewById(R.id.asteroidTimeOverlay);

        gameView.setExtraTimeRequestListener(() -> timeOverlay.setVisibility(View.VISIBLE));
        findViewById(R.id.asteroidTimeYesButton).setOnClickListener(view -> {
            timeOverlay.setVisibility(View.GONE);
            gameView.grantExtraTime();
        });
        findViewById(R.id.asteroidTimeNoButton).setOnClickListener(view -> {
            timeOverlay.setVisibility(View.GONE);
            gameView.declineExtraTime();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pauseGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.resumeGame();
        }
    }
}

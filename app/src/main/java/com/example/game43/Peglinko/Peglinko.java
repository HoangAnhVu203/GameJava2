package com.example.game43.Peglinko;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.game43.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Peglinko extends AppCompatActivity {
    private static final int MAX_BALLS = 9;
    private static final int INITIAL_BALLS = 6;
    private static final int[] BALL_DRAWABLES = {
            R.drawable.ball, R.drawable.ball1, R.drawable.ball2,
            R.drawable.ball3, R.drawable.ball4, R.drawable.ball5,
            R.drawable.ball6, R.drawable.ball7, R.drawable.ball8
    };

    private final List<Integer> balls = new ArrayList<>();
    private final Random random = new Random();
    private final ImageView[] ballSlots = new ImageView[MAX_BALLS];
    private PeglinkoGameView gameView;
    private TextView ballCountText;
    private Button addBallButton;
    private View addBallOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_peglinko);
        TextView coinText = findViewById(R.id.coinText);
        gameView = findViewById(R.id.gameView);
        gameView.bindBallImage(findViewById(R.id.physicsBallImage));
        ballCountText = findViewById(R.id.ballCountText);
        addBallButton = findViewById(R.id.addBallButton);
        addBallOverlay = findViewById(R.id.addBallOverlay);
        bindBallSlots();

        for (int i = 0; i < INITIAL_BALLS; i++) {
            balls.add(randomBallDrawable());
        }
        updateBallTray();
        gameView.prepareBall(balls.get(0));

        gameView.setOnCoinsChangedListener(coins -> coinText.setText(String.valueOf(coins)));
        gameView.setBallLifecycleListener(new PeglinkoGameView.BallLifecycleListener() {
            @Override
            public void onBallLaunched() {
                if (!balls.isEmpty()) {
                    balls.remove(0);
                    updateBallTray();
                }
            }

            @Override
            public void onShotFinished() {
                prepareNextBallIfAvailable();
            }
        });

        addBallButton.setOnClickListener(v -> {
            if (balls.size() < MAX_BALLS) {
                gameView.setPaused(true);
                addBallOverlay.setVisibility(View.VISIBLE);
            }
        });
        findViewById(R.id.confirmAddBallButton).setOnClickListener(v -> {
            addBallOverlay.setVisibility(View.GONE);
            addRandomBall();
            gameView.setPaused(false);
        });
        findViewById(R.id.cancelAddBallButton).setOnClickListener(v -> {
            addBallOverlay.setVisibility(View.GONE);
            gameView.setPaused(false);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void bindBallSlots() {
        int[] slotIds = {
                R.id.ballSlot0, R.id.ballSlot1, R.id.ballSlot2,
                R.id.ballSlot3, R.id.ballSlot4, R.id.ballSlot5,
                R.id.ballSlot6, R.id.ballSlot7, R.id.ballSlot8
        };
        for (int i = 0; i < slotIds.length; i++) {
            ballSlots[i] = findViewById(slotIds[i]);
        }
    }

    private void addRandomBall() {
        if (balls.size() >= MAX_BALLS) {
            return;
        }
        balls.add(randomBallDrawable());
        updateBallTray();
        if (gameView.isWaitingForBall()) {
            gameView.prepareBall(balls.get(0));
        }
    }

    private void prepareNextBallIfAvailable() {
        if (!balls.isEmpty()) {
            gameView.prepareBall(balls.get(0));
        }
    }

    private int randomBallDrawable() {
        return BALL_DRAWABLES[random.nextInt(BALL_DRAWABLES.length)];
    }

    private void updateBallTray() {
        for (int i = 0; i < ballSlots.length; i++) {
            ballSlots[i].setImageResource(i < balls.size() ? balls.get(i) : 0);
        }
        ballCountText.setText(getString(R.string.peglinko_ball_count, balls.size(), MAX_BALLS));
        boolean canAdd = balls.size() < MAX_BALLS;
        addBallButton.setEnabled(canAdd);
        addBallButton.setAlpha(canAdd ? 1f : 0.55f);
    }
}

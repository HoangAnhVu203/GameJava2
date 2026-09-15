package com.example.game43.SeaCatcher;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.game43.R;

public class SeaCatcher extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sea_catcher);

        SeaCatcherGameView gameView = findViewById(R.id.seaCatcherGame);
        View timerFill = findViewById(R.id.seaTimerFill);
        View addTimeButton = findViewById(R.id.seaAddTimeButton);
        TextView moneyText = findViewById(R.id.seaMoneyText);

        timerFill.setPivotX(0f);
        gameView.setTimerStateListener(fillFraction -> timerFill.setScaleX(fillFraction));
        gameView.setMoneyStateListener(money -> moneyText.setText(String.valueOf(money)));
        addTimeButton.setOnClickListener(view -> gameView.refillTimer());
    }
}

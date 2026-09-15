package com.example.game43.TinyFishing;

import android.os.Bundle;
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
        gameView.setMoneyStateListener(money -> moneyText.setText(String.valueOf(money)));
    }
}

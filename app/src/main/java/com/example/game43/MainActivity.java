package com.example.game43;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.game43.BallBlast.BallBlast;
import com.example.game43.FlipperDrunk.FlipperDrunk;
import com.example.game43.GameBall.Ball;
import com.example.game43.GameBasketball.Basketball;
import com.example.game43.SeaCatcher.SeaCatcher;
import com.example.game43.SupertBallFall.SuperBallFall;
import com.example.game43.TapeItUp.TapeItUp;
import com.example.game43.TinyFishing.TinyFishing;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.basketballButton).setOnClickListener(v ->
                startActivity(new Intent(this, Basketball.class)));
        findViewById(R.id.ballButton).setOnClickListener(v ->
                startActivity(new Intent(this, Ball.class)));
        findViewById(R.id.tapeItUpButton).setOnClickListener(v ->
                startActivity(new Intent(this, TapeItUp.class)));
        findViewById(R.id.seaCatcherButton).setOnClickListener(v ->
                startActivity(new Intent(this, SeaCatcher.class)));
        findViewById(R.id.ballBlastButton).setOnClickListener(v ->
                startActivity(new Intent(this, BallBlast.class)));
        findViewById(R.id.superBallFallButton).setOnClickListener(v ->
                startActivity(new Intent(this, SuperBallFall.class)));
        findViewById(R.id.tinyFishingButton).setOnClickListener(v ->
                startActivity(new Intent(this, TinyFishing.class)));
        findViewById(R.id.flipperDrunkButton).setOnClickListener(v ->
                startActivity(new Intent(this, FlipperDrunk.class)));
    }
}

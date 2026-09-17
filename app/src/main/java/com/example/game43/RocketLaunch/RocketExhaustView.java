package com.example.game43.RocketLaunch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Random;

public class RocketExhaustView extends View {
    private static final int PARTICLE_COUNT = 64;
    private static final float MAX_FRAME_SECONDS = 1f / 30f;
    private static final int[] PARTICLE_COLORS = {
            Color.rgb(255, 250, 224),
            Color.rgb(255, 235, 112),
            Color.rgb(255, 190, 55),
            Color.rgb(255, 133, 39),
            Color.rgb(244, 92, 31)
    };

    private final Particle[] particles = new Particle[PARTICLE_COUNT];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF particleRect = new RectF();
    private final Random random = new Random();
    private final float density;
    private boolean thrusting;
    private long lastFrameNanos;
    private float emissionRemainder;
    private int spawnCursor;

    public RocketExhaustView(Context context) {
        this(context, null);
    }

    public RocketExhaustView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle();
        }
        setVisibility(INVISIBLE);
    }

    public void setThrusting(boolean thrusting) {
        if (this.thrusting == thrusting) {
            return;
        }
        this.thrusting = thrusting;
        if (thrusting) {
            setVisibility(VISIBLE);
        }
    }

    public void update(long frameTimeNanos, float intensity) {
        float dt = 1f / 60f;
        if (lastFrameNanos != 0L) {
            dt = Math.min(MAX_FRAME_SECONDS,
                    (frameTimeNanos - lastFrameNanos) / 1_000_000_000f);
        }
        lastFrameNanos = frameTimeNanos;

        if (thrusting) {
            emissionRemainder += dt * (78f + 52f * intensity);
            while (emissionRemainder >= 1f) {
                emissionRemainder -= 1f;
                spawnParticle(intensity);
            }
        }

        boolean hasLiveParticles = false;
        for (Particle particle : particles) {
            if (particle.life <= 0f) {
                continue;
            }
            particle.life -= dt;
            if (particle.life <= 0f) {
                continue;
            }
            particle.x += particle.velocityX * dt;
            particle.y += particle.velocityY * dt;
            particle.velocityX *= Math.max(0f, 1f - 1.4f * dt);
            particle.velocityY += 20f * density * dt;
            hasLiveParticles = true;
        }

        if (!thrusting && !hasLiveParticles) {
            setVisibility(INVISIBLE);
            lastFrameNanos = 0L;
        } else {
            setVisibility(VISIBLE);
            invalidate();
        }
    }

    public void reset() {
        thrusting = false;
        emissionRemainder = 0f;
        lastFrameNanos = 0L;
        for (Particle particle : particles) {
            particle.life = 0f;
        }
        setVisibility(INVISIBLE);
    }

    private void spawnParticle(float intensity) {
        Particle particle = particles[spawnCursor];
        spawnCursor = (spawnCursor + 1) % particles.length;

        float size = (5f + random.nextFloat() * 8f) * density;
        particle.x = getWidth() * 0.5f + randomRange(-5f, 5f) * density;
        particle.y = 32f * density + randomRange(-2f, 3f) * density;
        particle.velocityX = randomRange(-38f, 38f) * density;
        particle.velocityY = (48f + random.nextFloat() * (55f + 28f * intensity))
                * density;
        particle.width = size * randomRange(0.8f, 1.45f);
        particle.height = size * randomRange(0.8f, 1.8f);
        particle.maxLife = randomRange(0.42f, 0.85f);
        particle.life = particle.maxLife;

        float colorPick = random.nextFloat();
        if (colorPick < 0.2f) {
            particle.color = PARTICLE_COLORS[0];
        } else if (colorPick < 0.47f) {
            particle.color = PARTICLE_COLORS[1];
        } else if (colorPick < 0.72f) {
            particle.color = PARTICLE_COLORS[2];
        } else if (colorPick < 0.91f) {
            particle.color = PARTICLE_COLORS[3];
        } else {
            particle.color = PARTICLE_COLORS[4];
        }
    }

    private float randomRange(float minimum, float maximum) {
        return minimum + random.nextFloat() * (maximum - minimum);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (thrusting) {
            paint.setColor(Color.argb(72, 255, 171, 49));
            canvas.drawCircle(getWidth() * 0.5f, 37f * density,
                    18f * density, paint);
            paint.setColor(Color.argb(110, 255, 247, 196));
            canvas.drawCircle(getWidth() * 0.5f, 34f * density,
                    6f * density, paint);
        }

        for (Particle particle : particles) {
            if (particle.life <= 0f) {
                continue;
            }
            float remaining = particle.life / particle.maxLife;
            float scale = 0.8f + (1f - remaining) * 0.65f;
            float halfWidth = particle.width * scale * 0.5f;
            float halfHeight = particle.height * scale * 0.5f;
            particleRect.set(particle.x - halfWidth, particle.y - halfHeight,
                    particle.x + halfWidth, particle.y + halfHeight);
            paint.setColor(particle.color);
            paint.setAlpha(Math.max(0, Math.min(255, Math.round(255f * remaining))));
            canvas.drawOval(particleRect, paint);
        }
        paint.setAlpha(255);
    }

    private static final class Particle {
        float x;
        float y;
        float velocityX;
        float velocityY;
        float width;
        float height;
        float life;
        float maxLife;
        int color;
    }
}

package com.plattysoft.leonids;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

class ParticleField extends View {
    private View mMimicDimensionFrom;
	private List<Particle> mParticles;

	public ParticleField(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
        init();
	}
	
	public ParticleField(Context context, AttributeSet attrs) {
		super(context, attrs);
        init();
	}
	
	public ParticleField(Context context) {
		super(context);
        init();
	}

    public void setMimiDimensionView(View mimicDimensionFrom) {
        mMimicDimensionFrom = mimicDimensionFrom;
    }

    private void init() {
        setWillNotDraw(false);
    }

	public void setParticles(List<Particle> particles) {
		mParticles = particles;
	}

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mMimicDimensionFrom != null) {
            setMeasuredDimension(mMimicDimensionFrom.getWidth(), mMimicDimensionFrom.getHeight());
        }
    }
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
        final List<Particle> particles = mParticles;
		// Draw all the particles
		if (particles != null) {
			synchronized (particles) {
				for (int i = 0; i < particles.size(); i++) {
					particles.get(i).draw(canvas);
				}
			}
		}
	}
}

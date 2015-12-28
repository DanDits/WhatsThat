package com.plattysoft.leonids;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

class ParticleFieldView extends View implements ParticleField {
    private View mMimicDimensionFrom;
	private volatile List<Particle> mParticles;
    private Controller mController;

    private class Controller implements ParticleFieldController {

        private ViewGroup mParentView;
        private int[] mParentLocation;

        public Controller(Activity a, int parentResId) {
            mParentLocation = new int[2];
            mParentView = (ViewGroup) a.findViewById(parentResId);
            setParentViewGroup(mParentView);
        }

        public Controller(ViewGroup parentView) {
            mParentLocation = new int[2];
            setParentViewGroup(parentView);
        }

        /**
         * Initializes the parent view group.
         * Drawing will be done to a child that is added to this view. So this view
         * needs to allow displaying an arbitrary sized view on top of its other content.
         * @param viewGroup The view group to use.
         */
        public void setParentViewGroup(ViewGroup viewGroup) {
            mParentView = viewGroup;
            mParentView.getLocationInWindow(mParentLocation);
        }

        @Override
        public int getPositionInParentX() {
            return mParentLocation[0];
        }

        @Override
        public int getPositionInParentY() {
            return mParentLocation[1];
        }

        @Override
        public void prepareEmitting(List<Particle> particles) {
            // Add a full size view to the parent view
            setParticles(particles);
            setMimicDimensionView(mParentView);
            mParentView.addView(ParticleFieldView.this);
        }

        @Override
        public void onUpdate() {
            ParticleFieldView.this.postInvalidate();
        }

        @Override
        public void onCleanup() {
            mParentView.removeView(ParticleFieldView.this);
            mParentView.postInvalidate();
        }
    }
	public ParticleFieldView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
        init();
	}
	
	public ParticleFieldView(Context context, AttributeSet attrs) {
		super(context, attrs);
        init();
	}
	
	public ParticleFieldView(Context context) {
		super(context);
        init();
	}

    @Override
    public ParticleFieldController getController() {
        return mController;
    }

    public void setMimicDimensionView(View mimicDimensionFrom) {
        mMimicDimensionFrom = mimicDimensionFrom;
    }

    private void init() {
        setWillNotDraw(false);
    }

    public ParticleFieldView initController(Activity a, int parentResId) {
        mController = new Controller(a, parentResId);
        return this;
    }

    public ParticleFieldView initController(ViewGroup parentView) {
        mController = new Controller(parentView);
        return this;
    }

	@Override
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
	public void onDraw(Canvas canvas) {
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

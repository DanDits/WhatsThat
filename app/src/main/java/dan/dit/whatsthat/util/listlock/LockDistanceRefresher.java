package dan.dit.whatsthat.util.listlock;

import android.view.MotionEvent;

/**
 * Created by daniel on 10.06.15.
 */
public class LockDistanceRefresher {
    private ListLockMaxIndex mLock;
    private float mMinDistanceToRefreshLock;
    private float mRefreshedAtX;
    private float mRefreshedAtY;

    public LockDistanceRefresher(ListLockMaxIndex lock, float minDistanceToRefreshLock) {
        mLock = lock;
        mMinDistanceToRefreshLock = minDistanceToRefreshLock;
        if (mLock == null) {
            throw new IllegalArgumentException("No lock to refresh.");
        }
    }

    public void update(MotionEvent event) {
        boolean refresh = false;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            refresh = true;
        }
        float x= event.getX();
        float y = event.getY();
        if ((x - mRefreshedAtX) * (x - mRefreshedAtX) + (y - mRefreshedAtY) * (y - mRefreshedAtY) > mMinDistanceToRefreshLock * mMinDistanceToRefreshLock) {
            refresh = true;
        }
        if (refresh) {
            mRefreshedAtX = x;
            mRefreshedAtY = y;
            mLock.refresh();
        }
    }
}

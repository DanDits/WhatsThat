package dan.dit.whatsthat.system.store;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import org.sufficientlysecure.donations.DonationsFragment;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 12.06.15.
 */
public class DonationsView extends FrameLayout implements StoreContainer {

    /**
     * Google
     */
    private static final String GOOGLE_PUBKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn6hUO5Q7mp16xluE7lRKlXJeFAvNktJjEHaDeD624XNcVcfRs7F5+LooYu2fCgHPnX74MyYG6p3ZDXDsHfaubenWVodBYHUo9ZfLWk3bo/kfd3vdFDiRVErQflZgQ88MUwcHl0caexKVYAP/LOCkniJvZKQ7+B/bj93us1nyqbaJOV+iBNveZwoODAgYembc0pOCJ4YgjY+J112o1+cgKZkuDunVLSapokhIWriVDp3XHU6b/1WyegzAA3cSiJV6y6rxy6kMFwGIc1e15oZPV8S7ae6GbZMc/GkeYGzxY1Qi1i4edk0K44rNjm0gfGd1Tz5WYD19cdECY9PXn4WD6QIDAQAB";
    private static final String[] GOOGLE_CATALOG = new String[]{"whatsthat.donation.1",
            "whatsthat.donation.2", "whatsthat.donation.3", "whatsthat.donation.5", "whatsthat.donation.8",
            "whatsthat.donation.13"};

    /**
     * Flattr
     */
    private static final String FLATTR_PROJECT_URL = "https://github.com/DanDits/WhatsThat/";
    // FLATTR_URL without http:// !
    private static final String FLATTR_URL = "flattr.com";//TODO add own as soon as thing created (after first flattr)

    /**
     * Bitcoin
     */
    private static final String BITCOIN_ADDRESS = "1NWKJnR8wYPmVcbBJ4SZx3Ph53cccH3WBU";

    public static final String FRAGMENT_TAG = "donationsFragment";

    private DonationsFragment mFragment;
    public DonationsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public void refresh(FragmentActivity activity, Button titleBackButton) {
        titleBackButton.setText(R.string.store_category_donate);
        if (mFragment == null) {
            mFragment = DonationsFragment.newInstance(false, true, GOOGLE_PUBKEY, GOOGLE_CATALOG,
                    getResources().getStringArray(R.array.donation_google_catalog_values), false, null, null,
                    null, true, FLATTR_PROJECT_URL, FLATTR_URL, true, BITCOIN_ADDRESS);
        }
        FragmentTransaction t = activity.getSupportFragmentManager().beginTransaction();
        t.replace(R.id.store_container, mFragment, FRAGMENT_TAG);
        t.commit();
        requestLayout();
        invalidate();
    }

    @Override
    public void stop(FragmentActivity activity, boolean pausedOnly) {
        FragmentTransaction t = activity.getSupportFragmentManager().beginTransaction();
        t.remove(mFragment);
        t.commit();
    }

    @Override
    public View getView() {
        return this;
    }
}

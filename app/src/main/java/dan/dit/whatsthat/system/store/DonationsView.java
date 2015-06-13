package dan.dit.whatsthat.system.store;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import org.sufficientlysecure.donations.DonationsFragment;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 12.06.15.
 */
public class DonationsView extends FrameLayout implements StoreContainer {

    /**
     * Google //TODO add own
     */
    private static final String GOOGLE_PUBKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAg8bTVFK5zIg4FGYkHKKQ/j/iGZQlXU0qkAv2BA6epOX1ihbMz78iD4SmViJlECHN8bKMHxouRNd9pkmQKxwEBHg5/xDC/PHmSCXFx/gcY/xa4etA1CSfXjcsS9i94n+j0gGYUg69rNkp+p/09nO9sgfRTAQppTxtgKaXwpfKe1A8oqmDUfOnPzsEAG6ogQL6Svo6ynYLVKIvRPPhXkq+fp6sJ5YVT5Hr356yCXlM++G56Pk8Z+tPzNjjvGSSs/MsYtgFaqhPCsnKhb55xHkc8GJ9haq8k3PSqwMSeJHnGiDq5lzdmsjdmGkWdQq2jIhKlhMZMm5VQWn0T59+xjjIIwIDAQAB";
    private static final String[] GOOGLE_CATALOG = new String[]{"ntpsync.donation.1",
            "ntpsync.donation.2", "ntpsync.donation.3", "ntpsync.donation.5", "ntpsync.donation.8",
            "ntpsync.donation.13"};

    /**
     * Flattr
     */
    private static final String FLATTR_PROJECT_URL = "https://github.com/DanDits/WhatsThat/";
    // FLATTR_URL without http:// !
    private static final String FLATTR_URL = null;//"flattr.com/thing/712895/dschuermannandroid-donations-lib-on-GitHub";//TODO add own?! cant make a thing

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
    public void refresh(FragmentActivity activity) {
        if (mFragment == null) {
            mFragment = DonationsFragment.newInstance(false, true, GOOGLE_PUBKEY, GOOGLE_CATALOG,
                    getResources().getStringArray(R.array.donation_google_catalog_values), false, null, null,
                    null, true, FLATTR_PROJECT_URL, FLATTR_URL, true, BITCOIN_ADDRESS);
        }
        FragmentTransaction t = activity.getSupportFragmentManager().beginTransaction();
        t.replace(R.id.store_container, mFragment, FRAGMENT_TAG);
        t.commit();
    }

    @Override
    public View getView() {
        return this;
    }
}

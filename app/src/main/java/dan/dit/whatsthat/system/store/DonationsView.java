/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.system.store;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import org.sufficientlysecure.donations.DonationsFragment;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.achievement.holders.MiscAchievementHolder;
import dan.dit.whatsthat.testsubject.TestSubject;

/**
 * Created by daniel on 12.06.15.
 */
public class DonationsView extends FrameLayout implements StoreContainer {

    /**
     * Google
     */
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
    private long mStartedTime;

    public DonationsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public void refresh(StoreActivity activity, FrameLayout titleBackContainer) {
        if (mFragment == null) {
            mFragment = DonationsFragment.newInstance(false, true, StoreActivity.GOOGLE_PUBKEY,
                    GOOGLE_CATALOG,
                    getResources().getStringArray(R.array.donation_google_catalog_values), false, null, null,
                    null, true, FLATTR_PROJECT_URL, FLATTR_URL, true, BITCOIN_ADDRESS);
        }
        FragmentTransaction t = activity.getSupportFragmentManager().beginTransaction();
        t.replace(R.id.store_container, mFragment, FRAGMENT_TAG);
        t.commit();
        requestLayout();
        invalidate();
        mStartedTime = System.currentTimeMillis();
    }

    @Override
    public void stop(FragmentActivity activity, boolean pausedOnly) {
        FragmentTransaction t = activity.getSupportFragmentManager().beginTransaction();
        t.remove(mFragment);
        t.commit();
        if (TestSubject.isInitialized()) {
            AchievementProperties data = TestSubject.getInstance().getAchievementHolder().getMiscData();
            if (data != null) {
                data.putValue(MiscAchievementHolder.KEY_LEFT_DONATION_SITE_STAY_TIME,
                        (System.currentTimeMillis() - mStartedTime),
                        AchievementProperties.UPDATE_POLICY_ALWAYS);
            }
        }
    }

    @Override
    public View getView() {
        return this;
    }
}

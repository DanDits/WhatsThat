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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TabHost;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.BundleCreator;
import dan.dit.whatsthat.image.BundleManager;
import dan.dit.whatsthat.image.LogoView;
import dan.dit.whatsthat.preferences.User;
import dan.dit.whatsthat.util.mosaic.MosaicGeneratorUi;
import dan.dit.whatsthat.util.ui.UiStyleUtil;

/**
 * Created by daniel on 02.09.15.
 */
public class WorkshopView extends FrameLayout implements StoreContainer {
    private static final String TAB_BUNDLE_MANAGER = "bundle_manager";
    private static final String TAB_BUNDLE_CREATOR = "bundle_creator";
    private static final String TAB_MOSAIC = "mosaic";
    private static final String TAB_LOGO = "logo";
    public static final int PICK_IMAGES_FOR_BUNDLE = 1338; // intent to pick images for bundle
    public static final int PICK_IMAGE_FOR_MOSAIC = 1339; // intent to pick image to generate a mosaic of
    private TabHost mTabHost;
    private BundleCreator mBundleCreator;
    private BundleManager mBundleManager;
    private MosaicGeneratorUi mMosaicGenerator;
    private View mLogoView;

    public WorkshopView(Context context) {
        super(context);
    }

    public WorkshopView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WorkshopView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void refresh(FragmentActivity activity, FrameLayout titleBackContainer) {
        if (mTabHost == null) {
            mTabHost = (TabHost) getRootView().findViewById(android.R.id.tabhost);
            initializeTabHost(activity);
        }
        if (mBundleManager != null) {
            mBundleManager.refresh();
        }
        syncOrigin();
    }

    private void initializeTabHost(FragmentActivity activity) {
        mTabHost.setup();
        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (mBundleManager != null && tabId != null && tabId.equalsIgnoreCase(TAB_BUNDLE_MANAGER)) {
                    mBundleManager.refresh();
                }
            }
        });
        TabFactory factory = new TabFactory(activity);
        addTab(mTabHost, factory, mTabHost.newTabSpec(TAB_BUNDLE_MANAGER).setIndicator(getResources().getString(R.string.workshop_tab_bundle_manager)));
        addTab(mTabHost, factory, mTabHost.newTabSpec(TAB_LOGO).setIndicator(getResources().getString(R.string.workshop_tab_logo)));
        addTab(mTabHost, factory, mTabHost.newTabSpec(TAB_MOSAIC).setIndicator(getResources().getString(R.string.workshop_tab_mosaic)));
        addTab(mTabHost, factory, mTabHost.newTabSpec(TAB_BUNDLE_CREATOR).setIndicator(getResources().getString(R.string.workshop_tab_bundle_creator)));

        UiStyleUtil.setTabHostSelector(mTabHost, R.drawable.tab_widget_selector_alien);
    }

    private void addTab(TabHost tabHost, TabFactory factory, TabHost.TabSpec tabSpec) {
        // Attach a Tab view factory to the spec
        tabSpec.setContent(factory);
        tabHost.addTab(tabSpec);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data == null) {
            //Display an error
            return;
        }
        if (mBundleCreator != null) {
            mBundleCreator.onActivityResult(requestCode, resultCode, data);
        }
        if (mMosaicGenerator != null) {
            mMosaicGenerator.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class TabFactory implements TabHost.TabContentFactory {

        private final LayoutInflater mInflater;
        private final Activity mActivity;

        public TabFactory(Activity activity) {
            mActivity = activity;
            mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View createTabContent(String tag) {
            if (tag.equals(TAB_BUNDLE_CREATOR)) {
                mBundleCreator = new BundleCreator(mActivity);
                return mBundleCreator.getView();
            } else if (tag.equals(TAB_BUNDLE_MANAGER)) {
                mBundleManager = new BundleManager(mActivity);
                return mBundleManager.getView();
            } else if (tag.equals(TAB_MOSAIC)) {
                mMosaicGenerator = new MosaicGeneratorUi(mActivity);
                return mMosaicGenerator.getView();
            } else if (tag.equalsIgnoreCase(TAB_LOGO)) {
                mLogoView = mActivity.getLayoutInflater().inflate(R.layout.workshop_logo, null);
                initLogoUiControl();
                return mLogoView;
            }
            return null;
        }

    }

    private void syncOrigin() {
        if (mLogoView == null) {
            return;
        }
        ((EditText) mLogoView.findViewById(R.id.origin)).setText(User.getInstance().getOriginName());
    }

    private void initLogoUiControl() {
        if (mLogoView == null) {
            return;
        }
        syncOrigin();
        ((EditText) mLogoView.findViewById(R.id.origin)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                User.getInstance().saveOriginName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mLogoView.findViewById(R.id.logo_redo).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((LogoView) mLogoView.findViewById(R.id.logo)).onRedo();
            }
        });

        mLogoView.findViewById(R.id.logo_save).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((LogoView) mLogoView.findViewById(R.id.logo)).onSave();
            }
        });
    }

    @Override
    public void stop(FragmentActivity activity, boolean pausedOnly) {
        if (!pausedOnly && mMosaicGenerator != null) {
            mMosaicGenerator.clear();
        }
    }

    @Override
    public View getView() {
        return this;
    }
}

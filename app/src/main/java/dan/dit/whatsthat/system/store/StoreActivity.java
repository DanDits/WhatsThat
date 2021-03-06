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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.Constants;
import com.anjlab.android.iab.v3.TransactionDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.preferences.User;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.system.InitActivity;
import dan.dit.whatsthat.testsubject.TestSubject;

/**
 * For billing library see: https://github.com/anjlab/android-inapp-billing-v3
 * Created by daniel on 10.06.15.
 */
public class StoreActivity extends FragmentActivity implements BillingProcessor.IBillingHandler, BillingCallback {
    public static final String GOOGLE_PUBKEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn6hUO5Q7mp16xluE7lRKlXJeFAvNktJjEHaDeD624XNcVcfRs7F5+LooYu2fCgHPnX74MyYG6p3ZDXDsHfaubenWVodBYHUo9ZfLWk3bo/kfd3vdFDiRVErQflZgQ88MUwcHl0caexKVYAP/LOCkniJvZKQ7+B/bj93us1nyqbaJOV+iBNveZwoODAgYembc0pOCJ4YgjY+J112o1+cgKZkuDunVLSapokhIWriVDp3XHU6b/1WyegzAA3cSiJV6y6rxy6kMFwGIc1e15oZPV8S7ae6GbZMc/GkeYGzxY1Qi1i4edk0K44rNjm0gfGd1Tz5WYD19cdECY9PXn4WD6QIDAQAB";

    private static final int CATEGORY_MENU = 0;
    private static final int CATEGORY_SHOP = 1; // start categories' index at 1
    private static final int CATEGORY_ACHIEVEMENTS = 2;
    private static final int CATEGORY_WORKSHOP = 3;
    private static final int CATEGORY_ABOUT = 4;
    private static final int CATEGORY_DONATE = 5;
    private static final int CATEGORY_CREDITS = 6;
    private static final int CATEGORIES_COUNT = 7;
    private static final int[] mCategoryLayoutId = new int[] {0, R.layout.shop_base, R.layout.achievements_base, R.layout.workshop_base, R.layout.about_base, R.layout.donations_base, R.layout.credits_base};
    private static final String KEY_CURR_CATEGORY = "dan.dit.whatsthat.STORE_MENU_CURR_CATEGORY";

    private boolean mStateOpening;
    private StoreContainer mVisibleCategory;
    private int mCurrCategory;
    private StoreContainer[] mCategory;
    private ViewGroup mCategoriesContainer;
    private Button mCategoryTitleBackButton;

    private ViewGroup mMenuContainer;
    private List<Button> mMenuButtons;
    private FrameLayout mCategoryTitleContainer;
    private BillingProcessor mBP;
    private boolean mBillingAvailable;
    private Map<String, ProductPurchasedCallback> mProductPurchaseCallbacks = new HashMap<>();
    private boolean mBillingInitialized;
    private String mLastStartedPurchaseProductId;

    private void showMenu() {
        mCurrCategory = CATEGORY_MENU;
        closeVisibleCategory(false);
        mCategoriesContainer.setVisibility(View.GONE);
        for (Button view : mMenuButtons) {
            view.clearAnimation();
        }
        mMenuContainer.setVisibility(View.VISIBLE);
    }

    private void closeVisibleCategory(boolean pausedOnly) {
        if (mVisibleCategory != null) {
            mVisibleCategory.stop(this, pausedOnly);
            mVisibleCategory = null;
        }
    }

    private void prepareCategory() {
        if (mCurrCategory == CATEGORY_MENU) {
            Log.e("HomeStuff", "Trying to prepare menu category!");
            return;
        }
        closeVisibleCategory(false);
        mMenuContainer.setVisibility(View.GONE);
        mCategoriesContainer.removeAllViews();
        mCategoriesContainer.setVisibility(View.VISIBLE);
        Animation categoryFadeIn = AnimationUtils.loadAnimation(this, R.anim.store_category_fadein);
        mCategoriesContainer.startAnimation(categoryFadeIn);

        StoreContainer container = mCategory[mCurrCategory];
        if (container == null && mCategoryLayoutId[mCurrCategory] != 0) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            container = (StoreContainer) inflater.inflate(mCategoryLayoutId[mCurrCategory], null).findViewById(R.id.store_container);
            mCategory[mCurrCategory] = container;
        }
        if (container != null) {
            mCategoriesContainer.addView(container.getView());
            mCategoryTitleBackButton.setText(mMenuButtons.get(mCurrCategory - 1).getText());
            container.refresh(this, mCategoryTitleContainer);
            mVisibleCategory = container;
        }
    }

    private void showCurrCategory() {
        if (mCurrCategory == CATEGORY_MENU) {
            mCategoryTitleBackButton.setText(R.string.store_category_menu);
            mCategoryTitleBackButton.setCompoundDrawablesWithIntrinsicBounds(TestSubject.getInstance().getImageResId(), 0, R.drawable.shop_title_back, 0);
            showMenu();
        } else {
            mCategoryTitleBackButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.alien_menu_title_back, 0, 0, R.drawable.shop_title_back_bottom);
            prepareCategory();
        }
        mStateOpening = false;
    }

    private boolean onMenuButtonClicked(View view) {
        if (!mStateOpening) {
            mStateOpening = true;
            List<Button> otherMenuButtons = new ArrayList<>(mMenuButtons);
            //noinspection SuspiciousMethodCalls
            otherMenuButtons.remove(view);

            Animation animFadeOut = AnimationUtils.loadAnimation(this, R.anim.store_button_fadeout);
            Animation animScale = new ScaleAnimation(1.f, mCategoryTitleBackButton.getWidth() / (float) view.getWidth(), 1.f, mCategoryTitleBackButton.getHeight() / (float) view.getHeight(),
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            Animation animMoveTop = new TranslateAnimation(Animation.ABSOLUTE, 0.f, Animation.ABSOLUTE, 0.f, Animation.ABSOLUTE, 0.f, Animation.ABSOLUTE, -view.getY() - mCategoryTitleBackButton.getHeight());
            AnimationSet onPress = new AnimationSet(true);
            onPress.setDuration(400L);
            onPress.addAnimation(animScale);
            onPress.addAnimation(animMoveTop);
            onPress.setFillAfter(true);
            for (Button btn : otherMenuButtons) {
                btn.startAnimation(animFadeOut);
            }
            view.startAnimation(onPress);
            onPress.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    showCurrCategory();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (mCurrCategory != CATEGORY_MENU) {
            mCurrCategory = CATEGORY_MENU;
            showCurrCategory();
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.riddles_enter, R.anim.store_exit);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_CURR_CATEGORY, mCurrCategory);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCategoriesContainer != null) {
            closeVisibleCategory(true);
            mCategoriesContainer.removeAllViews();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVisibleCategory == null) {
            showCurrCategory();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        AchievementManager.commit();
    }

    @Override
    public void onDestroy() {
        if (mBP != null) {
            mBP.release();
        }
        if (mProductPurchaseCallbacks != null) {
            mProductPurchaseCallbacks.clear();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (RiddleInitializer.INSTANCE.isNotInitialized() || !TestSubject.isInitialized()) {
            // app got killed by android and is trying to reconstruct this activity when not initialized
            Log.d("HomeStuff", "App killed and trying to reconstruct non initialized into StoreActivity.");
            Intent reInit = new Intent(getApplicationContext(), InitActivity.class);
            reInit.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(reInit);
            finish();
            return;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.store_activity);

        mBP = new BillingProcessor(this, GOOGLE_PUBKEY, this);
        mBillingAvailable = BillingProcessor.isIabServiceAvailable(this);

        mCategoriesContainer = (ViewGroup) findViewById(R.id.category_container);
        mCategoryTitleContainer = (FrameLayout) findViewById(R.id.category_title_container);
        mCategoryTitleBackButton = (Button) findViewById(R.id.btn_category_title_back);
        mCategoryTitleBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        mMenuContainer = (ViewGroup) findViewById(R.id.menu_buttons);
        mMenuButtons = new ArrayList<>(CATEGORIES_COUNT);
        Button shopButton = (Button) findViewById(R.id.btn_shop);
        shopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onMenuButtonClicked(view)) {
                    mCurrCategory = CATEGORY_SHOP;
                }
            }
        });
        Button achievementsButton = (Button) findViewById(R.id.btn_achievements);
        achievementsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onMenuButtonClicked(view)) {
                    mCurrCategory = CATEGORY_ACHIEVEMENTS;
                }
            }
        });
        Button workshopButton = (Button) findViewById(R.id.btn_workshop);
        if (User.getInstance().hasPermission(User.PERMISSION_WORKSHOP)) {
            workshopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onMenuButtonClicked(view)) {
                        mCurrCategory = CATEGORY_WORKSHOP;
                    }
                }
            });
        } else {
            workshopButton.setVisibility(View.GONE);
        }
        Button aboutButton = (Button) findViewById(R.id.btn_about);
        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onMenuButtonClicked(view)) {
                    mCurrCategory = CATEGORY_ABOUT;
                }
            }
        });
        Button donateButton = (Button) findViewById(R.id.btn_donate);
        donateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onMenuButtonClicked(view)) {
                    mCurrCategory = CATEGORY_DONATE;
                }
            }
        });
        Button creditsButton = (Button) findViewById(R.id.btn_credits);
        creditsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onMenuButtonClicked(view)) {
                    mCurrCategory = CATEGORY_CREDITS;
                }
            }
        });
        mMenuButtons.add(shopButton);
        mMenuButtons.add(achievementsButton);
        mMenuButtons.add(workshopButton);
        mMenuButtons.add(aboutButton);
        mMenuButtons.add(donateButton);
        mMenuButtons.add(creditsButton);
        mCategory = new StoreContainer[CATEGORIES_COUNT];
        mCurrCategory = CATEGORY_MENU;
        if (savedInstanceState != null) {
            mCurrCategory = savedInstanceState.getInt(KEY_CURR_CATEGORY, CATEGORY_MENU);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mBP.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        Log.d("HomeStuff", "Store activity got activity result!" + resultCode);
        /**
         * Needed for Google Play In-app Billing. It uses startIntentSenderForResult(). The result is not propagated to
         * the Fragment like in startActivityForResult(). Thus we need to propagate manually to our Fragment.
         */
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(DonationsView.FRAGMENT_TAG);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }

        if (resultCode == Activity.RESULT_OK && mCurrCategory == CATEGORY_WORKSHOP) {
            // this downcast is not nice but as long as only workshop needs activity results we are fine
            ((WorkshopView) mCategory[CATEGORY_WORKSHOP]).onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        /*
         * Called when requested PRODUCT ID was successfully purchased
         */
        ProductPurchasedCallback callback = mProductPurchaseCallbacks.get(productId);
        if (callback != null) {
            Log.d("Billing", "Product purchased " + productId + " details: " + details);
            int result = callback.onProductPurchased(productId, details);
            mProductPurchaseCallbacks.remove(productId);
            if (result == ProductPurchasedCallback.CONSUME_PRODUCT) {
                if (!mBP.consumePurchase(productId)) {
                    Log.e("Billing", "Product consuming failed!");
                }
            }
        }
    }

    @Override
    public void onPurchaseHistoryRestored() {
        Log.d("Billing", "Purchase history restored.");
        /*
         * Called when purchase history was restored and the list of all owned PRODUCT ID's
         * was loaded from Google Play,
         * see: mBP.loadOwnedPurchasesFromGoogle();
         */
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        if (errorCode == Constants.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
            Log.e("Billing", "Billing error, already owned item: " + error + ", is it " +
                    mLastStartedPurchaseProductId + "?");
        } else {
            Log.e("Billing", "Billing error: " + errorCode + " error: " + error);
        }
    }

    @Override
    public void onBillingInitialized() {
        Log.d("Billing", "Billing initialized.");
        /*
         * Called when BillingProcessor was initialized and it's ready to purchase
         */
        mBillingInitialized = true;
    }

    @Override
    public boolean isAvailable() {
        return mBP != null && mBillingAvailable && mBillingInitialized;
    }

    @Override
    public void purchase(String productId, ProductPurchasedCallback callback) {
        if (isAvailable() && callback != null) {
            if (mBP.purchase(this, productId)) {
                Log.d("Billing", "Purchased successfully started " + productId);
                mLastStartedPurchaseProductId = productId;
                mProductPurchaseCallbacks.put(productId, callback);
            }
        }
    }

    public interface ProductPurchasedCallback {
        int DO_NOTHING = 0;
        int CONSUME_PRODUCT = 1;
        int onProductPurchased(String productId, TransactionDetails details);
    }
}

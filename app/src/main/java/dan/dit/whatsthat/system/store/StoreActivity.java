package dan.dit.whatsthat.system.store;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 10.06.15.
 */
public class StoreActivity extends Activity {
    private static final int CATEGORY_MENU = 0;
    private static final int CATEGORY_SHOP = 1;
    private static final int CATEGORY_ACHIEVEMENTS = 2;
    private static final int CATEGORY_ABOUT = 3;
    private static final int CATEGORY_DONATE = 4;
    private static final int CATEGORY_CREDITS = 5;
    private static final int[] CATEGORY_NAME_RES_ID = new int[] {R.string.store_category_menu, R.string.store_category_shop, R.string.store_category_achievement, R.string.store_category_about, R.string.store_category_donate, R.string.store_category_credit};
    private static final int[] mCategoryLayoutId = new int[] {0, 0, 0, 0, 0, R.layout.credits_base};
    private static final String KEY_CURR_CATEGORY = "dan.dit.whatsthat.STORE_MENU_CURR_CATEGORY";

    private boolean mStateOpening;
    private int mCurrCategory;
    private StoreContainer[] mCategory;
    private ViewGroup mCategoriesContainer;
    private Button mCategoryTitleBackButton;

    private ViewGroup mMenuContainer;
    private List<Button> mMenuButtons;

    private void showMenu() {
        mCurrCategory = CATEGORY_MENU;
        mCategoriesContainer.setVisibility(View.GONE);
        for (Button view : mMenuButtons) {
            view.clearAnimation();
        }
        mMenuContainer.setVisibility(View.VISIBLE);
    }

    private void prepareCategory() {
        if (mCurrCategory == CATEGORY_MENU) {
            Log.e("HomeStuff", "Trying to prepare menu category!");
            return;
        }
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
            container.refresh();
        }
    }

    private void showCurrCategory() {
        mCategoryTitleBackButton.setText(CATEGORY_NAME_RES_ID[mCurrCategory]);
        if (mCurrCategory == CATEGORY_MENU) {
            showMenu();
        } else {
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
                    Animation.RELATIVE_TO_PARENT, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
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
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURR_CATEGORY, mCurrCategory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.store_activity);
        mCategory = new StoreContainer[CATEGORY_NAME_RES_ID.length];
        mCategoriesContainer = (ViewGroup) findViewById(R.id.category_container);
        mCategoryTitleBackButton = (Button) findViewById(R.id.btn_category_title_back);
        mCategoryTitleBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        mMenuContainer = (ViewGroup) findViewById(R.id.menu_buttons);
        mMenuButtons = new ArrayList<>(5);
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
        mMenuButtons.add(aboutButton);
        mMenuButtons.add(donateButton);
        mMenuButtons.add(creditsButton);
        mCurrCategory = CATEGORY_MENU;
        if (savedInstanceState != null) {
            mCurrCategory = savedInstanceState.getInt(KEY_CURR_CATEGORY, CATEGORY_MENU);
        }
        showCurrCategory();
    }
}

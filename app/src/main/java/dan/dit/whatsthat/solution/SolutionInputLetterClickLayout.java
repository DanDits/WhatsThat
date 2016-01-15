package dan.dit.whatsthat.solution;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * Created by daniel on 09.01.16.
 */
class SolutionInputLetterClickLayout extends SolutionInputLayout {

    private static final int USER_LETTER_COLOR_COMPLETED = Color.GREEN;
    private static final int USER_LETTER_COLOR_INCOMPLETE = 0xFFFF4400;
    private static final int USER_LETTER_COLOR_MISSING = Color.RED;

    /*  values stored are in pixel units, constants are in density independent */
    private static final float PADDING_TB = 5.f; //dp , padding top + bottom
    private static final float PADDING_LR = 30.f; //dp, padding left + right
    private static final float PADDING_USER_ALL = 5.f; //dp, space between user letters and all letters
    private static final float LETTER_MAX_RADIUS = 35.f; //dp, maximum radius for letters
    private static final float LETTER_MAX_GAP = 10.f; //dp, maximum gap between letters
    private static final float ALL_LETTER_BASE_SIZE = 20f; //dp, base size of letters
    private static final float USER_LETTER_BASE_SIZE = 25f; //dp, base size of letters
    private static final float CIRCLE_BORDER_WIDTH = 1f; //dp, width of the circle border
    private static final int ALL_LETTERS_MAX_ROWS = 3;
    private static final float ALL_LETTERS_ROW_PADDING = 2.f; //dp, padding between rows
    private static final float USER_LETTER_FRACTION = 1.f/3.f;
    private static final float CLICK_DISTANCE_MAX_DELTA = 25.f; // dp, maximum additional distance so that a click counts
    private final SolutionInputLetterClick mLetterClick;

    private float mUserLetterCircleRadius;
    private List<Float> mUserLetterX=new LinkedList<>();
    private float mUserLetterY;
    private List<Float> mAllLettersX=new LinkedList<>();
    private List<Float> mAllLettersY=new LinkedList<>();
    private float mAllLettersCircleRadius;
    private Paint mUserLetterCirclePaint = new Paint();
    private Paint mUserLetterPaint = new Paint();
    private final Rect mTextBounds = new Rect();
    private Paint mUserLetterCircleBorderPaint = new Paint();
    private Paint mAllLetterPaint = new Paint();
    private Paint mAllLetterCirclePaint = new Paint();
    private Paint mAllLetterCircleBorderPaint = new Paint();
    private float mWidth;
    private float mHeight;
    private DisplayMetrics mMetrics;

    public SolutionInputLetterClickLayout(SolutionInputLetterClick letterClick) {
        mLetterClick = letterClick;
        initPaints();
    }

    private void initPaints() {
        mUserLetterCirclePaint.setAntiAlias(true);
        mUserLetterPaint.setAntiAlias(true);
        mAllLetterPaint.setAntiAlias(true);
        mAllLetterCirclePaint.setAntiAlias(true);
        mAllLetterCircleBorderPaint.setAntiAlias(true);
        mUserLetterCircleBorderPaint.setAntiAlias(true);
    }

    @Override
    // this holy mother of all stupid functions was written by looking at a hand drawn rectangle and thinking about stuff, had fun, never again
    public void calculateLayout(float width, float height, DisplayMetrics metrics) {
        // basic initializiation
        mWidth = width;
        mHeight = height;
        mMetrics = metrics;
        mUserLetterCirclePaint.setColor(Color.WHITE);
        mUserLetterCirclePaint.setAntiAlias(true);
        mAllLetterCirclePaint.setColor(Color.WHITE);
        mAllLetterCirclePaint.setAntiAlias(true);
        mUserLetterCircleBorderPaint.setStyle(Paint.Style.STROKE);
        mUserLetterCircleBorderPaint.setStrokeWidth(ImageUtil.convertDpToPixel(CIRCLE_BORDER_WIDTH, metrics));
        mUserLetterCircleBorderPaint.setAntiAlias(true);
        mAllLetterCircleBorderPaint.setColor(Color.BLACK);
        mAllLetterCircleBorderPaint.setStyle(Paint.Style.STROKE);
        mAllLetterCircleBorderPaint.setStrokeWidth(ImageUtil.convertDpToPixel(CIRCLE_BORDER_WIDTH, metrics));
        mAllLetterCircleBorderPaint.setAntiAlias(true);
        mAllLetterPaint.setColor(Color.BLACK);
        calculateUserLetterLayout();
        calculateAllLetterLayout();
    }

    private void calculateAllLetterLayout() {
        if (mMetrics == null) {
            return;
        }
        final float letter_base_size = ImageUtil.convertDpToPixel(ALL_LETTER_BASE_SIZE, mMetrics);
        final float padding_lr = ImageUtil.convertDpToPixel(PADDING_LR, mMetrics);
        final float padding_tb = ImageUtil.convertDpToPixel(PADDING_TB, mMetrics);
        final float padding_user_all = ImageUtil.convertDpToPixel(PADDING_USER_ALL, mMetrics);
        final float gapBetweenRows = ImageUtil.convertDpToPixel(ALL_LETTERS_ROW_PADDING, mMetrics);

        // -------------- all letters ---------------
        mAllLettersX.clear();
        mAllLettersY.clear();

        // calculate available height for all letters and the global y offset
        float heightForUserLetters = (mHeight - padding_tb - padding_user_all) * USER_LETTER_FRACTION;
        float heightForAllLetters = (mHeight - padding_tb - padding_user_all) * (1.f - USER_LETTER_FRACTION);
        float offsetAllLettersY = padding_tb / 2.f + heightForUserLetters + + padding_user_all;

        // calculate radius for all letters and number of requires rows to attempt to stay bigger than minimum radius
        int allLetterCount = mLetterClick.getAllLettersCount();
        int rowCount = 1;
        int lettersPerRow = 0;
        mAllLettersCircleRadius = 0.f;
        if (allLetterCount > 0) {
            // find maximum radius that fits all letters into the area, test all rows counts
            float maxRadius = 0.f;
            int rowCountForMaxRadius = 1;
            int lettersPerRowForMaxRadius = allLetterCount;
            for (rowCount = 1; rowCount <= ALL_LETTERS_MAX_ROWS; rowCount++) {
                lettersPerRow = (int) Math.ceil(allLetterCount / ((float) rowCount));
                mAllLettersCircleRadius = Math.min((heightForAllLetters - (rowCount - 1) * gapBetweenRows) / rowCount, (mWidth - padding_lr) / lettersPerRow ) / 2.f;
                // maximize the radius
                if (mAllLettersCircleRadius > maxRadius) {
                    maxRadius = mAllLettersCircleRadius;
                    rowCountForMaxRadius = rowCount;
                    lettersPerRowForMaxRadius = lettersPerRow;
                }
            }
            // set the maximum radius values
            rowCount = rowCountForMaxRadius;
            mAllLettersCircleRadius = maxRadius;
            lettersPerRow = lettersPerRowForMaxRadius;
            mAllLetterPaint.setTextSize(letter_base_size);
        }

        // calculate if there is a gap between letters
        float widthSpaceAvailable = mWidth - padding_lr - lettersPerRow * 2.f * mAllLettersCircleRadius;
        float gapBetweenAllLetters = 0.f;
        if (widthSpaceAvailable > 0 && lettersPerRow > 1) {
            gapBetweenAllLetters = widthSpaceAvailable / (lettersPerRow - 1);
        }

        // add the values of all letters
        if (rowCount > 0) {
            float currX;
            for (int i = 0; i < rowCount; i++) {
                currX = padding_lr / 2.f + mAllLettersCircleRadius;
                float currY = offsetAllLettersY + mAllLettersCircleRadius + i * 2.f * mAllLettersCircleRadius + i * gapBetweenRows;
                for (int j = 0; j < Math.min(lettersPerRow, allLetterCount - lettersPerRow * i); j++) {
                    mAllLettersX.add(currX);
                    mAllLettersY.add(currY);
                    currX += 2.f * mAllLettersCircleRadius + gapBetweenAllLetters;
                }
            }
        }
    }

    void calculateUserLetterLayout() {
        if (mMetrics == null) {
            return;
        }
        final boolean showLength = mLetterClick.showMainSolutionWordLength();
        final int minLengthToShow = showLength ? mLetterClick.getMainSolutionWordLength() : 0;

        mUserLetterX.clear();
        float letter_base_size = ImageUtil.convertDpToPixel(USER_LETTER_BASE_SIZE, mMetrics);
        float padding_lr = ImageUtil.convertDpToPixel(PADDING_LR, mMetrics);
        float padding_tb = ImageUtil.convertDpToPixel(PADDING_TB, mMetrics);
        float gap_lr = ImageUtil.convertDpToPixel(LETTER_MAX_GAP, mMetrics);
        float padding_user_all = ImageUtil.convertDpToPixel(PADDING_USER_ALL, mMetrics);
        float letter_max_radius = ImageUtil.convertDpToPixel(LETTER_MAX_RADIUS, mMetrics);

        // -------user letters ---------------------------------

        // calculate available height for user letters
        float heightForUserLetters = (mHeight - padding_tb - padding_user_all) * USER_LETTER_FRACTION;

        // calculate radius for user letters
        int userLetterCount = Math.max(mLetterClick.getUserLettersCount(), minLengthToShow);
        mUserLetterCircleRadius = 0.f;
        if (userLetterCount > 0) {
            userLetterCount = (showLength
                        || mLetterClick.isStateCompleted()
                        || userLetterCount == mLetterClick.getAllLettersCount()) ?
                    userLetterCount :
                    userLetterCount + 1;
            // simulate one more user letter so that we show that there is room for more letters in the solution word
            mUserLetterCircleRadius = Math.min(heightForUserLetters, (mWidth - padding_lr) / userLetterCount) / 2.f;
            mUserLetterCircleRadius = Math.min(mUserLetterCircleRadius, letter_max_radius);
            if (mUserLetterCircleRadius > 0.f) {
                mUserLetterPaint.setTextSize(letter_base_size * mUserLetterCircleRadius * 2 / heightForUserLetters);
            }
        }

        // calculate if there is a gap between letters
        float widthSpaceAvailable = mWidth - padding_lr - userLetterCount * 2f * mUserLetterCircleRadius;
        float gapBetweenUserLetters = 0.f;
        if (widthSpaceAvailable > 0 && userLetterCount > 1) {
            gapBetweenUserLetters = Math.min(gap_lr, widthSpaceAvailable / (userLetterCount - 1));
        }

        // add the values of the user letters
        float currX = padding_lr / 2.f + mUserLetterCircleRadius;
        mUserLetterY = padding_tb / 2.f  + (heightForUserLetters - 2.f*mUserLetterCircleRadius) / 2.f + mUserLetterCircleRadius;
        for (int i = 0; i < userLetterCount; i++) {
            mUserLetterX.add(currX);
            currX += 2f*mUserLetterCircleRadius + gapBetweenUserLetters;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        drawUserLetters(canvas);
        drawAllLetters(canvas);
    }

    private void drawAllLetters(Canvas canvas) {
        int index = 0;
        float allTextOffsetY = -((mAllLetterPaint.descent() + mAllLetterPaint.ascent()) / 2);
        Iterator<Float> xIt = mAllLettersX.iterator();
        Iterator<Float> yIt = mAllLettersY.iterator();
        while (xIt.hasNext()) {
            float x = xIt.next();
            float y = yIt.next();
            if (mLetterClick.isAllLetterNotSelected(index)) {
                canvas.drawCircle(x, y, mAllLettersCircleRadius, mAllLetterCirclePaint);
                canvas.drawCircle(x, y, mAllLettersCircleRadius, mAllLetterCircleBorderPaint);
                String text = String.valueOf(mLetterClick.getAllLetter(index));
                mAllLetterPaint.getTextBounds(text, 0, text.length(), mTextBounds);
                canvas.drawText(text, x - mTextBounds.exactCenterX(), y + allTextOffsetY, mAllLetterPaint);
            }
            index++;
        }
    }

    private void drawUserLetters(Canvas canvas) {
        int validUserLettersCount = mLetterClick.getUserLettersCount();
        int userLetterCountToShow = mLetterClick.showMainSolutionWordLength() ?
                mLetterClick.getMainSolutionWordLength() : 0;
        userLetterCountToShow = Math.max(userLetterCountToShow, validUserLettersCount);
        userLetterCountToShow = Math.max(userLetterCountToShow, mUserLetterX.size());
        if (userLetterCountToShow > 0) {
            if (mLetterClick.showCompleted() && mLetterClick.isStateCompleted()) {
                mUserLetterPaint.setColor(USER_LETTER_COLOR_COMPLETED);
                mUserLetterCircleBorderPaint.setColor(USER_LETTER_COLOR_COMPLETED);
            } else {
                mUserLetterPaint.setColor(USER_LETTER_COLOR_INCOMPLETE);
                mUserLetterCircleBorderPaint.setColor(USER_LETTER_COLOR_INCOMPLETE);
            }
            float userTextOffsetY = -((mUserLetterPaint.descent() + mUserLetterPaint.ascent()) / 2);
            for (int i = 0; i < userLetterCountToShow; i++) {
                if (i < validUserLettersCount) {
                    float x = mUserLetterX.get(i);
                    canvas.drawCircle(x, mUserLetterY, mUserLetterCircleRadius, mUserLetterCirclePaint);
                    canvas.drawCircle(x, mUserLetterY, mUserLetterCircleRadius, mUserLetterCircleBorderPaint);
                    String text = String.valueOf(mLetterClick.getUserLetter(i));
                    mUserLetterPaint.getTextBounds(text, 0, text.length(), mTextBounds);
                    canvas.drawText(text, x - mTextBounds.exactCenterX(), mUserLetterY + userTextOffsetY, mUserLetterPaint);
                } else if (!mLetterClick.showMainSolutionWordLength()
                        && i < mUserLetterX.size()) {
                    float x = mUserLetterX.get(i);
                    // show that there is room for more
                    final int COUNT = Math.min(5, mLetterClick.getAllLettersCount() -
                            mLetterClick.getUserLettersCount());
                    float availableWidth = 2 * mUserLetterCircleRadius;
                    // we got space in interval [x-mUserLetterCircleRadius, x+mUserLetterCircleRadius] for COUNT circles
                    float currX = x - mUserLetterCircleRadius;
                    mUserLetterCircleBorderPaint.setColor(USER_LETTER_COLOR_MISSING);
                    for (int j = 0; j < COUNT; j++) {
                        float radius = availableWidth / 4;
                        availableWidth -= 2 * radius;
                        currX += radius;
                        canvas.drawCircle(currX, mUserLetterY, radius, mUserLetterCirclePaint);
                        canvas.drawCircle(currX, mUserLetterY, radius, mUserLetterCircleBorderPaint);
                        currX += radius;
                    }
                } else if (mLetterClick.showMainSolutionWordLength()
                        && i < mUserLetterX.size()) {
                    float x = mUserLetterX.get(i);
                    canvas.drawCircle(x, mUserLetterY, mUserLetterCircleRadius, mUserLetterCirclePaint);
                    canvas.drawCircle(x, mUserLetterY, mUserLetterCircleRadius, mUserLetterCircleBorderPaint);
                }
            }
        }
    }

    public List<Integer> getTouchedUserIndicies(float startEventX, float startEventY, float
            endEventX, float endEventY) {
        float userLetterHeight = mHeight * USER_LETTER_FRACTION;
        if (startEventY  <= userLetterHeight && endEventY <= userLetterHeight) {
            float leftX = Math.min(startEventX, endEventX);
            float rightX = Math.max(startEventX, endEventX);
            List<Integer> indicesToRemove = new ArrayList<>(mLetterClick.getUserLettersCount());
            for (int i = 0; i < mLetterClick.getUserLettersCount(); i++) {
                float currX = mUserLetterX.get(i);
                if (currX >= leftX && currX < rightX) {
                    indicesToRemove.add(i);
                }
            }
            return indicesToRemove;
        }
        return null;
    }


    private double getDistance(float x1, float y1, float x2, float y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1- y2));
    }

    public boolean executeClick(float x, float y) {
        // find out if any of the all letters was clicked
        double allLetterMinDistance = Double.MAX_VALUE;
        int allLetterMinDistanceIndex = -1;
        Iterator<Float> xIt = mAllLettersX.iterator();
        Iterator<Float> yIt = mAllLettersY.iterator();
        for (int index = 0; index < mLetterClick.getAllLettersCount(); index++) {
            // if letter is not yet selected check the distance to the actual click
            float xCurr = xIt.next();
            float yCurr = yIt.next();
            if (mLetterClick.isAllLetterNotSelected(index)) {
                double currDistance = getDistance(x, y, xCurr, yCurr);
                if (currDistance <= mAllLettersCircleRadius
                        && mLetterClick.performAllLettersClicked(index)) {
                    return true;
                }
                if (currDistance < allLetterMinDistance) {
                    allLetterMinDistance = currDistance;
                    allLetterMinDistanceIndex = index;
                }
            }
        }

        // find out if any of the user letters was clicked
        double userLetterMinDistance = Double.MAX_VALUE;
        int userLetterMinDistanceIndex = -1;
        for (int i = 0; i < mUserLetterX.size(); i++) {
            float xCurr = mUserLetterX.get(i);
            float yCurr = mUserLetterY;
            double currDistance = getDistance(x, y, xCurr, yCurr);
            if (currDistance <= mUserLetterCircleRadius && i < mLetterClick.getUserLettersCount()
                    && mLetterClick.performUserLetterClick(i)) {
                return true;
            }
            if (currDistance < userLetterMinDistance) {
                userLetterMinDistance = currDistance;
                userLetterMinDistanceIndex = i;
            }
        }

        // apply click tolerance
        if (allLetterMinDistance < userLetterMinDistance) {
            if (allLetterMinDistanceIndex >= 0 && allLetterMinDistanceIndex < mLetterClick.getAllLettersCount()
                    && allLetterMinDistance < mAllLettersCircleRadius + ImageUtil.convertDpToPixel(CLICK_DISTANCE_MAX_DELTA, mMetrics)) {
                if (mLetterClick.performAllLettersClicked(allLetterMinDistanceIndex)) {
                    return true;
                }
            }
        } else {
            if (userLetterMinDistanceIndex >= 0 && userLetterMinDistanceIndex < mLetterClick.getUserLettersCount()
                    && userLetterMinDistance < mUserLetterCircleRadius + ImageUtil.convertDpToPixel(CLICK_DISTANCE_MAX_DELTA, mMetrics)) {
                return mLetterClick.performUserLetterClick(userLetterMinDistanceIndex);
            }
        }
        return false;
    }
}

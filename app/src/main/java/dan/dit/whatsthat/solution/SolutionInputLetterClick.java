package dan.dit.whatsthat.solution;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * Created by daniel on 12.04.15.
 */
public class SolutionInputLetterClick extends SolutionInput {
    private static final char NO_LETTER = 0; // this letter is not expected to be in any alphabet


    public static final int LETTER_POOL_MIN_SIZE = 14; // minimum pool size of all displayed letters
    public static final int LETTER_POOL_MIN_WRONG_LETTERS = 2; // minimum amount of wrong letters
    public static final int LETTER_POOL_MAX_WRONG_LETTERS = 7; // maximum amount of wrong letters
    public static final String IDENTIFIER = "LETTERCLICK";
    private static final int USER_LETTER_COLOR_COMPLETED = Color.GREEN;
    private static final int USER_LETTER_COLOR_INCOMPLETE = Color.RED;
    private char[] mSolutionLetters; // in order
    private char[] mAllLetters; // permuted randomly including solution letters
    private int[] mAllLettersSelected; // index of letter in user letters if the letter is selected, invisible and one of the user letters

    private ArrayList<Character> mUserLetters;
    private boolean mStateCompleted;
    private boolean mShowCompleted;

    /* LAYOUT RELATED, values stored are in pixel units, constants are in density independent */
    private static final float PADDING_TB = 5.f; //dp , padding top + bottom
    private static final float PADDING_LR = 30.f; //dp, padding left + right
    private static final float PADDING_USER_ALL = 5.f; //dp, space between user letters and all letters
    private static final float LETTER_MAX_RADIUS = 25.f; //dp, maximum radius for letters
    private static final float LETTER_MAX_GAP = 10.f; //dp, maximum gap between letters
    private static final float LETTER_BASE_SIZE = 0.75f; //dp, base size of letters, will scale on radius
    private static final float CIRCLE_BORDER_WIDTH = 1f; //dp, width of the circle border
    private static final int ALL_LETTERS_MAX_ROWS = 5;
    private static final float ALL_LETTERS_ROW_PADDING = 2.f; //dp, padding between rows
    private static final float USER_LETTER_FRACTION = 1.f/3.f;
    private static final float CLICK_DISTANCE_MAX_DELTA = 25.f; // dp, maximum additional distance so that a click counts

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


    public SolutionInputLetterClick(Solution sol) {
        super(sol);
    }

    public SolutionInputLetterClick(Compacter data) throws CompactedDataCorruptException {
        super(data);
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
        final float letter_base_size = ImageUtil.convertDpToPixel(LETTER_BASE_SIZE, mMetrics);
        final float padding_lr = ImageUtil.convertDpToPixel(PADDING_LR, mMetrics);
        final float padding_tb = ImageUtil.convertDpToPixel(PADDING_TB, mMetrics);
        final float gap_lr = ImageUtil.convertDpToPixel(LETTER_MAX_GAP, mMetrics);
        final float padding_user_all = ImageUtil.convertDpToPixel(PADDING_USER_ALL, mMetrics);
        final float letter_max_radius = ImageUtil.convertDpToPixel(LETTER_MAX_RADIUS, mMetrics);
        final float gapBetweenRows = ImageUtil.convertDpToPixel(ALL_LETTERS_ROW_PADDING, mMetrics);

        // -------------- all letters ---------------
        mAllLettersX.clear();
        mAllLettersY.clear();

        // calculate available height for all letters and the global y offset
        float heightForUserLetters = (mHeight - padding_tb - padding_user_all) * USER_LETTER_FRACTION;
        float heightForAllLetters = (mHeight - padding_tb - padding_user_all) * (1.f - USER_LETTER_FRACTION);
        float offsetAllLettersY = padding_tb / 2.f + heightForUserLetters + + padding_user_all;

        // calculate radius for all letters and number of requires rows to attempt to stay bigger than minimum radius
        int allLetterCount = mAllLetters.length;
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
                mAllLettersCircleRadius = Math.min(mAllLettersCircleRadius, letter_max_radius);
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
            mAllLetterPaint.setTextSize(letter_base_size * mAllLettersCircleRadius);
        }

        // calculate if there is a gap between letters
        float widthSpaceAvailable = mWidth - padding_lr - lettersPerRow * 2.f * mAllLettersCircleRadius;
        float gapBetweenAllLetters = 0.f;
        if (widthSpaceAvailable > 0 && lettersPerRow > 1) {
            gapBetweenAllLetters = Math.min(gap_lr, widthSpaceAvailable / (lettersPerRow - 1));
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

    private void calculateUserLetterLayout() {

        mUserLetterX.clear();
        float letter_base_size = ImageUtil.convertDpToPixel(LETTER_BASE_SIZE, mMetrics);
        float padding_lr = ImageUtil.convertDpToPixel(PADDING_LR, mMetrics);
        float padding_tb = ImageUtil.convertDpToPixel(PADDING_TB, mMetrics);
        float gap_lr = ImageUtil.convertDpToPixel(LETTER_MAX_GAP, mMetrics);
        float padding_user_all = ImageUtil.convertDpToPixel(PADDING_USER_ALL, mMetrics);
        float letter_max_radius = ImageUtil.convertDpToPixel(LETTER_MAX_RADIUS, mMetrics);

        // -------user letters ---------------------------------

        // calculate available height for user letters
        float heightForUserLetters = (mHeight - padding_tb - padding_user_all) * USER_LETTER_FRACTION;

        // calculate radius for user letters
        int userLetterCount = mUserLetters.size();
        mUserLetterCircleRadius = 0.f;
        if (userLetterCount > 0) {
            mUserLetterCircleRadius = Math.min(heightForUserLetters, (mWidth - padding_lr) / userLetterCount) / 2.f;
            mUserLetterCircleRadius = Math.min(mUserLetterCircleRadius, letter_max_radius);
            if (mUserLetterCircleRadius > 0.f) {
                mUserLetterPaint.setTextSize(letter_base_size * mUserLetterCircleRadius);
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
    public int estimateSolvedValue() {
        return mSolution.estimateSolvedValue(userLettersToWord());
    }


    @Override
    protected void initSolution(@NonNull Solution solution) {
        mSolution = solution;
        String mainWord = mSolution.getMainWord();
        mSolutionLetters = new char[mainWord.length()];
        mAllLetters = new char[Math.max(mSolutionLetters.length + LETTER_POOL_MIN_WRONG_LETTERS
                + new Random().nextInt(LETTER_POOL_MAX_WRONG_LETTERS - LETTER_POOL_MIN_WRONG_LETTERS), LETTER_POOL_MIN_SIZE)];
        mAllLettersSelected = new int[mAllLetters.length];
        Arrays.fill(mAllLettersSelected, -1);
        mUserLetters = new ArrayList<>(mAllLetters.length);

        List<Character> allLetters = new ArrayList<>(mAllLetters.length);
        for (int i = 0; i < mainWord.length(); i++) {
            mSolutionLetters[i] = mainWord.charAt(i);
            allLetters.add(mSolutionLetters[i]);
        }
        for (int i = allLetters.size(); i < mAllLetters.length; i++) {
            allLetters.add(mSolution.getTongue().getRandomLetter());
        }
        Collections.shuffle(allLetters);
        for (int i = 0; i < allLetters.size(); i++) {
            mAllLetters[i] = allLetters.get(i);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int index = 0;
        if (mUserLetters.size() > 0) {
            if (mShowCompleted && mStateCompleted) {
                mUserLetterPaint.setColor(USER_LETTER_COLOR_COMPLETED);
                mUserLetterCircleBorderPaint.setColor(USER_LETTER_COLOR_COMPLETED);
            } else {
                mUserLetterPaint.setColor(USER_LETTER_COLOR_INCOMPLETE);
                mUserLetterCircleBorderPaint.setColor(USER_LETTER_COLOR_INCOMPLETE);
            }
            float userTextOffsetY = -((mUserLetterPaint.descent() + mUserLetterPaint.ascent()) / 2);
            for (Float x : mUserLetterX) {
                canvas.drawCircle(x, mUserLetterY, mUserLetterCircleRadius, mUserLetterCirclePaint);
                canvas.drawCircle(x, mUserLetterY, mUserLetterCircleRadius, mUserLetterCircleBorderPaint);
                String text = String.valueOf(mUserLetters.get(index));
                mUserLetterPaint.getTextBounds(text, 0, text.length(), mTextBounds);
                canvas.drawText(text, x - mTextBounds.exactCenterX(), mUserLetterY + userTextOffsetY, mUserLetterPaint);
                index++;
            }
        }
        float allTextOffsetY = -((mAllLetterPaint.descent() + mAllLetterPaint.ascent()) / 2);
        Iterator<Float> xIt = mAllLettersX.iterator();
        Iterator<Float> yIt = mAllLettersY.iterator();
        index = 0;
        while (xIt.hasNext()) {
            float x = xIt.next();
            float y = yIt.next();
            if (mAllLettersSelected[index] == -1) {
                canvas.drawCircle(x, y, mAllLettersCircleRadius, mAllLetterCirclePaint);
                canvas.drawCircle(x, y, mAllLettersCircleRadius, mAllLetterCircleBorderPaint);
                String text = String.valueOf(mAllLetters[index]);
                mAllLetterPaint.getTextBounds(text, 0, text.length(), mTextBounds);
                canvas.drawText(text, x - mTextBounds.exactCenterX(), y + allTextOffsetY, mAllLetterPaint);
            }
            index++;
        }
    }

    private boolean isSolved(String userWord) {
        return mSolution.getMainWord().equals(userWord);
    }

    private void checkCompleted() {
        String userWord = userLettersToWord();
        if (mStateCompleted) {
            if (!isSolved(userWord)) {
                mStateCompleted = false;
                if (mListener != null) {
                    mListener.onSolutionIncomplete();
                }
            }
        } else {
            // is state incomplete
            if (isSolved(userWord)) {
                mStateCompleted = true;
                if (mListener != null) {
                    mShowCompleted = mListener.onSolutionComplete(userWord);
                }
            }
        }
    }

    private int fillLetterInUserLetters(char letter) {
        for (int i = 0; i < mUserLetters.size(); i++) {
            if (mUserLetters.get(i).equals(NO_LETTER)) {
                mUserLetters.set(i, letter);
                return i;
            }
        }
        mUserLetters.add(letter);
        checkCompleted();
        return mUserLetters.size() - 1;
    }

    private int findAllLetterIndex(int userIndex) {
        for (int index = 0; index < mAllLetters.length; index++) {
            if (mAllLettersSelected[index] == userIndex) {
                return index;
            }
        }
        return -1;
    }

    private void removeAppendedNoLetters() {
        int removedCount = 0;
        for (int i = mUserLetters.size() - 1; i >= 0; i--) {
            if (mUserLetters.get(i).equals(NO_LETTER)) {
                removedCount++;
                mUserLetters.remove(i);
            } else {
                break; // stop at the first other letter
            }
        }
        if (removedCount > 0) {
            checkCompleted();
        }
    }

    private boolean performUserLetterClick(int userLetterIndex) {
        char clickedChar = mUserLetters.get(userLetterIndex);
        if (clickedChar != NO_LETTER) {
            int allIndex = findAllLetterIndex(userLetterIndex);
            if (allIndex != -1) {
                // remove from user selection and make available again
                mAllLettersSelected[allIndex] = -1;
                mUserLetters.set(userLetterIndex, NO_LETTER);
                // remove NO_LETTERs at the end
                removeAppendedNoLetters();
                calculateUserLetterLayout();
                return true;
            }
        }
        return false;
    }

    private boolean performAllLettersClicked(int allLetterIndex) {
        if (mAllLettersSelected[allLetterIndex] == -1) {
            mAllLettersSelected[allLetterIndex] = fillLetterInUserLetters(mAllLetters[allLetterIndex]);
            calculateUserLetterLayout();
            return true;
        }
        return false;
    }

    @Override
    public boolean onUserTouchDown(float x, float y) {

        // find out if any of the all letters was clicked
        double allLetterMinDistance = Double.MAX_VALUE;
        int allLetterMinDistanceIndex = -1;
        Iterator<Float> xIt = mAllLettersX.iterator();
        Iterator<Float> yIt = mAllLettersY.iterator();
        for (int index = 0; index < mAllLetters.length; index++) {
            // if letter is not yet selected check the distance to the actual click
            float xCurr = xIt.next();
            float yCurr = yIt.next();
            if (mAllLettersSelected[index] == -1) {
                double currDistance = getDistance(x, y, xCurr, yCurr);
                if (currDistance <= mAllLettersCircleRadius && performAllLettersClicked(index)) {
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
            if (currDistance <= mUserLetterCircleRadius && performUserLetterClick(i)) {
                return true;
            }
            if (currDistance < userLetterMinDistance) {
                userLetterMinDistance = currDistance;
                userLetterMinDistanceIndex = i;
            }
        }

        // apply click tolerance
        if (allLetterMinDistance < userLetterMinDistance) {
            if (allLetterMinDistanceIndex >= 0 && allLetterMinDistanceIndex < mAllLetters.length
                    && allLetterMinDistance < mAllLettersCircleRadius + ImageUtil.convertDpToPixel(CLICK_DISTANCE_MAX_DELTA, mMetrics)) {
                if (performAllLettersClicked(allLetterMinDistanceIndex)) {
                    return true;
                }
            }
        } else {
            if (userLetterMinDistanceIndex >= 0 && userLetterMinDistanceIndex < mUserLetters.size()
                    && userLetterMinDistance < mUserLetterCircleRadius + ImageUtil.convertDpToPixel(CLICK_DISTANCE_MAX_DELTA, mMetrics)) {
                return performUserLetterClick(userLetterMinDistanceIndex);
            }
        }
        return false;
    }

    @Override
    public boolean onFling(MotionEvent startEvent, MotionEvent endEvent, float velocityX, float velocityY) {
        float userLetterHeight = mHeight * USER_LETTER_FRACTION;
        if (startEvent.getY() <= userLetterHeight && endEvent.getY() <= userLetterHeight) {
            float leftX = Math.min(startEvent.getX(), endEvent.getX());
            float rightX = Math.max(startEvent.getX(), endEvent.getX());
            List<Integer> indicesToRemove = new ArrayList<>(mUserLetters.size());
            for (int i = 0; i < mUserLetters.size(); i++) {
                float currX = mUserLetterX.get(i);
                if (currX >= leftX && currX < rightX) {
                    indicesToRemove.add(i);
                }
            }
            for (Integer index : indicesToRemove) {
                char clickedChar = mUserLetters.get(index);
                if (clickedChar != NO_LETTER) {
                    int allIndex = findAllLetterIndex(index);
                    if (allIndex != -1) {
                        // remove from user selection and make available again
                        mAllLettersSelected[allIndex] = -1;
                        mUserLetters.set(index, NO_LETTER);
                    }
                }
            }
            if (!indicesToRemove.isEmpty()) {
                // remove NO_LETTERs at the end
                removeAppendedNoLetters();
                calculateUserLetterLayout();
                return true;
            }
        }
        return false;
    }

    private double getDistance(float x1, float y1, float x2, float y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1- y2));
    }

    private String userLettersToWord() {
        StringBuilder builder = new StringBuilder(mUserLetters.size());
        for (Character c : mUserLetters) {
            builder.append(c);
        }
        return builder.toString();
    }

    private void wordToUserLetters(String word) {
        mUserLetters = new ArrayList<>(word.length());
        for (int i = 0; i < word.length(); i++) {
            mUserLetters.add(word.charAt(i));
        }
    }

    @NonNull
    @Override
    public Solution getCurrentUserSolution() {
        String word = userLettersToWord();
        if (TextUtils.isEmpty(word)) {
            return new Solution(mSolution.getTongue()); // empty solution, should not be the case
        }
        return new Solution(mSolution.getTongue(), userLettersToWord());
    }

    @Override
    public String compact() {
        Compacter cmp = new Compacter();
        cmp.appendData(IDENTIFIER);
        cmp.appendData(mSolution.compact());
        cmp.appendData(userLettersToWord());
        cmp.appendData(String.valueOf(mAllLetters));
        cmp.appendData(String.valueOf(mSolutionLetters));
        Compacter cmp2 = new Compacter(mAllLettersSelected.length);
        for (int i = 0; i < mAllLettersSelected.length; i++) {
            cmp2.appendData(mAllLettersSelected[i]);
        }
        cmp.appendData(cmp2.compact());
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData == null || compactedData.getSize() < 6) {
            throw new CompactedDataCorruptException("Too little data given to build letter click.");
        }
        mSolution = new Solution(new Compacter(compactedData.getData(1)));
        wordToUserLetters(compactedData.getData(2));
        String word = compactedData.getData(3);
        mAllLetters = word.toCharArray();
        word = compactedData.getData(4);
        mSolutionLetters = word.toCharArray();
        mAllLettersSelected = new int[mAllLetters.length];
        Compacter inner = new Compacter(compactedData.getData(5));
        for (int i = 0; i < inner.getSize(); i++) {
            mAllLettersSelected[i] = inner.getInt(i);
        }
    }
}

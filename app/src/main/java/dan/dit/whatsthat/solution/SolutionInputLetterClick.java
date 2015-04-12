package dan.dit.whatsthat.solution;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 12.04.15.
 */
public class SolutionInputLetterClick extends SolutionInput {
    private static final char NO_LETTER = 0; // this letter is not expected to be in any alphabet


    public static final int LETTER_POOL_MIN_SIZE = 14; // minimum pool size of all displayed letters
    public static final int LETTER_POOL_WRONG_LETTERS = 5; // minimum amount of wrong letters
    public static final String IDENTIFIER = "LETTERCLICK";
    private char[] mSolutionLetters; // in order
    private char[] mAllLetters; // permuted randomly including solution letters
    private int[] mAllLettersSelected; // index of letter in user letters if the letter is selected, invisible and one of the user letters

    private ArrayList<Character> mUserLetters;

    /* LAYOUT RELATED, values stored are in pixel units, constants are in density independent */
    private static final float PADDING_TB = 5.f; //dp , padding top + bottom
    private static final float PADDING_LR = 30.f; //dp, padding left + right
    private static final float PADDING_USER_ALL = 5.f; //dp, space between user letters and all letters
    private static final float LETTER_MAX_RADIUS = 25.f; //dp, maximum radius for letters
    private static final float LETTER_MAX_GAP = 10.f; //dp, maximum gap between letters
    private static final float LETTER_BASE_SIZE = 0.75f; //dp, base size of letters, will scale on radius
    private static final float CIRCLE_BORDER_WIDTH = 1f; //dp, width of the circle border
    private static final float ALL_LETTERS_MIN_RADIUS = 24.f; //dp, minimum radius, will try to push next letters in next row unless max rows exceeded
    private static final int ALL_LETTERS_MAX_ROWS = 2;
    private static final float ALL_LETTERS_ROW_PADDING = 2.f; //dp, padding between rows
    private static final float USER_LETTER_FRACTION = 1.f/3.f;
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

    // convertDpToPixel(25f, metrics) -> (25dp converted to pixels)
    public static float convertDpToPixel(float dp, DisplayMetrics metrics){
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    public static float convertPixelsToDp(float px, DisplayMetrics metrics){
        float dp = px / (metrics.densityDpi / 160.f);
        return dp;
    }

    private void calculateLayout() {
        calculateLayout(mWidth, mHeight, mMetrics);
    }

    @Override
    // this holy mother of all stupid functions was written by looking at a hand drawn rectangle and thinking about stuff, had fun, never again
    public void calculateLayout(float width, float height, DisplayMetrics metrics) {
        mWidth = width;
        mHeight = height;
        mMetrics = metrics;
        float letter_base_size = convertDpToPixel(LETTER_BASE_SIZE, metrics);
        mUserLetterCirclePaint.setColor(Color.WHITE);
        mUserLetterCirclePaint.setAntiAlias(true);
        mAllLetterCirclePaint.setColor(Color.WHITE);
        mAllLetterCirclePaint.setAntiAlias(true);
        mUserLetterPaint.setColor(Color.RED);
        mUserLetterCircleBorderPaint.setColor(Color.RED);
        mUserLetterCircleBorderPaint.setStyle(Paint.Style.STROKE);
        mUserLetterCircleBorderPaint.setStrokeWidth(convertDpToPixel(CIRCLE_BORDER_WIDTH, metrics));
        mUserLetterCircleBorderPaint.setAntiAlias(true);
        mAllLetterCircleBorderPaint.setColor(Color.BLACK);
        mAllLetterCircleBorderPaint.setStyle(Paint.Style.STROKE);
        mAllLetterCircleBorderPaint.setStrokeWidth(convertDpToPixel(CIRCLE_BORDER_WIDTH, metrics));
        mAllLetterCircleBorderPaint.setAntiAlias(true);
        mAllLetterPaint.setColor(Color.BLACK);
        mUserLetterX.clear();
        mAllLettersX.clear();
        mAllLettersY.clear();
        float padding_lr = convertDpToPixel(PADDING_LR, metrics);
        float padding_tb = convertDpToPixel(PADDING_TB, metrics);
        float gap_lr = convertDpToPixel(LETTER_MAX_GAP, metrics);
        float padding_user_all = convertDpToPixel(PADDING_USER_ALL, metrics);
        float letter_max_radius = convertDpToPixel(LETTER_MAX_RADIUS, metrics);
        float letter_min_radius = convertDpToPixel(ALL_LETTERS_MIN_RADIUS, metrics);
        float gapBetweenRows = convertDpToPixel(ALL_LETTERS_ROW_PADDING, metrics);

        // -------user letters ---------------------------------

        // calculate available height for user letters
        float heightForUserLetters = (height - padding_tb - padding_user_all) * USER_LETTER_FRACTION;

        // calculate radius for user letters
        int userLetterCount = mUserLetters.size();
        if (userLetterCount > 0) {
            mUserLetterCircleRadius = Math.min(heightForUserLetters, (width - padding_lr) / userLetterCount) / 2.f;
            mUserLetterCircleRadius = Math.min(mUserLetterCircleRadius, letter_max_radius);
            if (mUserLetterCircleRadius > 0.f) {
                mUserLetterPaint.setTextSize(letter_base_size * mUserLetterCircleRadius);
            }
        } else {
            mUserLetterCircleRadius = 0.f;
        }

        // calculate if there is a gap between letters
        float widthSpaceAvailable = width - padding_lr - userLetterCount * 2f * mUserLetterCircleRadius;
        float gapBetweenUserLetters;
        if (widthSpaceAvailable > 0 && userLetterCount > 1) {
            gapBetweenUserLetters = Math.min(gap_lr, widthSpaceAvailable / (userLetterCount - 1));
        } else {
            gapBetweenUserLetters = 0.f;
        }

        // add the values of the user letters
        float currX = padding_lr / 2.f + mUserLetterCircleRadius;
        mUserLetterY = padding_tb / 2.f  + (heightForUserLetters - 2.f*mUserLetterCircleRadius) / 2.f + mUserLetterCircleRadius;
        for (int i = 0; i < userLetterCount; i++) {
            mUserLetterX.add(currX);
            currX += 2f*mUserLetterCircleRadius + gapBetweenUserLetters;
        }

        // -------------- all letters ---------------

        // calculate available height for all letters and the global y offset
        float heightForAllLetters = (height - padding_tb - padding_user_all) * (1.f - USER_LETTER_FRACTION);
        float offsetAllLettersY = padding_tb / 2.f + heightForUserLetters + + padding_user_all;

        // calculate radius for all letters and number of requires rows to attempt to stay bigger than minimum radius
        int allLetterCount = mAllLetters.length;
        int rowCount = 1;
        if (allLetterCount > 0) {
            for (rowCount = 1; rowCount <= ALL_LETTERS_MAX_ROWS; rowCount++) {
                mAllLettersCircleRadius = Math.min((heightForAllLetters - (rowCount - 1) * gapBetweenRows) / rowCount, (width - padding_lr) * rowCount / ((float) allLetterCount) ) / 2.f;
                mAllLettersCircleRadius = Math.min(mAllLettersCircleRadius, letter_max_radius);
                if (mAllLettersCircleRadius > letter_min_radius) {
                    mAllLetterPaint.setTextSize(letter_base_size * mAllLettersCircleRadius);
                    break;
                }
            }
            if (rowCount > ALL_LETTERS_MAX_ROWS) {
                mAllLetterPaint.setTextSize(letter_base_size * mAllLettersCircleRadius);
                rowCount--;
            }
        } else {
            mAllLettersCircleRadius = 0.f;
        }

        // calculate if there is a gap between letters
        widthSpaceAvailable = width - padding_lr - allLetterCount / ((float) rowCount) * 2.f * mAllLettersCircleRadius;
        float gapBetweenAllLetters;
        if (widthSpaceAvailable > 0 && allLetterCount / ((float) rowCount) > 1) {
            gapBetweenAllLetters = Math.min(gap_lr, widthSpaceAvailable / (allLetterCount / ((float) rowCount) - 1));
        } else {
            gapBetweenAllLetters = 0.f;
        }

        // add the values of all letters
        if (rowCount > 0) {
            int circlesInRow = allLetterCount / rowCount;
            for (int i = 0; i < rowCount; i++) {
                currX = padding_lr / 2.f + mAllLettersCircleRadius;
                float currY = offsetAllLettersY + mAllLettersCircleRadius + i * 2.f * mAllLettersCircleRadius + i * gapBetweenRows;
                for (int j = 0; j < Math.min(circlesInRow, allLetterCount - circlesInRow * i); j++) {
                    mAllLettersX.add(currX);
                    mAllLettersY.add(currY);
                    currX += 2.f * mAllLettersCircleRadius + gapBetweenAllLetters;
                }
            }
        }
    }

    @Override
    public int estimateSolvedValue() {
        return 0;
    }


    @Override
    protected void initSolution(Solution solution) {
        mSolution = solution;
        String mainWord = mSolution.getMainWord();
        mSolutionLetters = new char[mainWord.length()];
        mAllLetters = new char[Math.max(mSolutionLetters.length + LETTER_POOL_WRONG_LETTERS, LETTER_POOL_MIN_SIZE)];
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
        Log.d("Image", "For solution " + solution + " made all letters: " + Arrays.toString(mAllLetters));
    }

    /**
     * Sets the text size for a Paint object so a given string of text will be a
     * given width.
     * Source: http://stackoverflow.com/questions/12166476/android-canvas-drawtext-set-font-size-from-width
     * @param paint
     *            the Paint to set the text size for
     * @param desiredWidth
     *            the desired width
     * @param text
     *            the text that should be that width
     */
    private static void setTextSizeForWidth(Paint paint, float desiredWidth,
                                            String text) {

        // Pick a reasonably large value for the test. Larger values produce
        // more accurate results, but may cause problems with hardware
        // acceleration. But there are workarounds for that, too; refer to
        // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
        final float testTextSize = 42f;

        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSize = testTextSize * desiredWidth / bounds.width();

        // Set the paint for that size.
        paint.setTextSize(desiredTextSize);
    }

    @Override
    public void draw(Canvas canvas) {
        int index = 0;
        if (mUserLetters.size() > 0) {
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

    private int fillLetterInUserLetters(char letter) {
        for (int i = 0; i < mUserLetters.size(); i++) {
            if (mUserLetters.get(i).equals(NO_LETTER)) {
                mUserLetters.set(i, letter);
                return i;
            }
        }
        mUserLetters.add(letter);
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
        for (int i = mUserLetters.size() - 1; i >= 0; i--) {
            if (mUserLetters.get(i).equals(NO_LETTER)) {
                mUserLetters.remove(i);
            } else {
                break; // stop at the first other letter
            }
        }
    }

    @Override
    public boolean performClick(float x, float y) {
        // find out if any of the all letters was clicked
        Iterator<Float> xIt = mAllLettersX.iterator();
        Iterator<Float> yIt = mAllLettersY.iterator();
        int index = 0;
        while (xIt.hasNext()) {
            float xCurr = xIt.next();
            float yCurr = yIt.next();
            if (getDistance(x, y, xCurr, yCurr) <= mAllLettersCircleRadius) {
                if (mAllLettersSelected[index] == -1) {
                    mAllLettersSelected[index] = fillLetterInUserLetters(mAllLetters[index]);
                    calculateLayout();
                    return true;
                }
            }
            index++;
        }

        // find out if any of the user letters was clicked
        for (int i = 0; i < mUserLetterX.size(); i++) {
            float xCurr = mUserLetterX.get(i);
            float yCurr = mUserLetterY;
            if (getDistance(x, y, xCurr, yCurr) < mUserLetterCircleRadius) {
                char clickedChar = mUserLetters.get(i);
                if (clickedChar != NO_LETTER) {
                    int allIndex = findAllLetterIndex(i);
                    if (allIndex != -1) {
                        // remove from user selection and make available again
                        mAllLettersSelected[allIndex] = -1;
                        mUserLetters.set(i, NO_LETTER);
                        // remove NO_LETTERs at the end
                        removeAppendedNoLetters();
                        calculateLayout();
                        return true;
                    }
                }
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

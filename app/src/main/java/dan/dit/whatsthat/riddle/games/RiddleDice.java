package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dan.dit.whatsthat.BuildConfig;
import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementDice;
import dan.dit.whatsthat.util.BuildException;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.field.Field2D;
import dan.dit.whatsthat.util.field.FieldElement;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * A RiddleGame implementation that uses a Field2D and dices of various states.
 * Created by daniel on 22.04.15.
 */
public class RiddleDice extends RiddleGame {
    public static final int FIELD_X = 6;
    public static final int FIELD_Y = 6;

    private static final float DICE_DOT_BASE_RADIUS = 5.f;
    private static final int NUMBERS_PER_STATE = 9;
    private static final int[] STATE_COLOR = new int[]{0xFFFF0000, 0xFFE9F700, 0xFF0dd70d, 0xFF740A8B};
    public static final int STATE_ALIEN = 3;
    public static final int STATE_GREEN = 2;
    public static final int STATE_YELLOW = 1;
    public static final int STATE_RED = 0;
    private static final int[] STATE_CURRENT_POSITION_TEMP_ALPHA = new int[] {5, 10, 30, 50};
    private static final int[] STATE_NEIGHBOR_PERM_ALPHA_INCREASE = new int[] {1, 2, 3, 0};
    private static final int STATE_NEIGHBOR_TEMP_ALPHA_DIVIDE_BY_NUMBER = 250;
    private static final float ALPHA_RESET_PENALTY_FACTOR = 0.25f;
    private static final FieldElement.Neighbor[][] NUMBER_NEIGBORS = new FieldElement.Neighbor[][] {
            new FieldElement.Neighbor[] {FieldElement.Neighbor.SELF}, //1
            new FieldElement.Neighbor[] {FieldElement.Neighbor.TOP_LEFT, FieldElement.Neighbor.BOTTOM_RIGHT}, //2
            new FieldElement.Neighbor[] {FieldElement.Neighbor.TOP_LEFT, FieldElement.Neighbor.BOTTOM_RIGHT, FieldElement.Neighbor.SELF}, //3
            new FieldElement.Neighbor[] {FieldElement.Neighbor.TOP_LEFT, FieldElement.Neighbor.BOTTOM_RIGHT, FieldElement.Neighbor.TOP_RIGHT, FieldElement.Neighbor.BOTTOM_LEFT}, //4
            new FieldElement.Neighbor[] {FieldElement.Neighbor.TOP_LEFT, FieldElement.Neighbor.BOTTOM_RIGHT, FieldElement.Neighbor.TOP_RIGHT, FieldElement.Neighbor.BOTTOM_LEFT, FieldElement.Neighbor.SELF}, //5
            new FieldElement.Neighbor[] {FieldElement.Neighbor.TOP_LEFT, FieldElement.Neighbor.BOTTOM_RIGHT, FieldElement.Neighbor.TOP_RIGHT, FieldElement.Neighbor.BOTTOM_LEFT, FieldElement.Neighbor.LEFT, FieldElement.Neighbor.RIGHT}, //6
            new FieldElement.Neighbor[] {FieldElement.Neighbor.TOP_LEFT, FieldElement.Neighbor.BOTTOM_RIGHT, FieldElement.Neighbor.TOP_RIGHT, FieldElement.Neighbor.BOTTOM_LEFT, FieldElement.Neighbor.LEFT, FieldElement.Neighbor.RIGHT, FieldElement.Neighbor.SELF}, //7
            new FieldElement.Neighbor[] {FieldElement.Neighbor.TOP_LEFT, FieldElement.Neighbor.BOTTOM_RIGHT, FieldElement.Neighbor.TOP_RIGHT, FieldElement.Neighbor.BOTTOM_LEFT, FieldElement.Neighbor.LEFT, FieldElement.Neighbor.RIGHT, FieldElement.Neighbor.TOP, FieldElement.Neighbor.BOTTOM}, //8
            new FieldElement.Neighbor[] {FieldElement.Neighbor.TOP_LEFT, FieldElement.Neighbor.BOTTOM_RIGHT, FieldElement.Neighbor.TOP_RIGHT, FieldElement.Neighbor.BOTTOM_LEFT, FieldElement.Neighbor.LEFT, FieldElement.Neighbor.RIGHT, FieldElement.Neighbor.TOP, FieldElement.Neighbor.BOTTOM, FieldElement.Neighbor.SELF}, //9

    };
    private Random mRand;
    private Field2D<DicePosition> mField;
    private Paint mBorderPaint;
    private Paint mTransparentPaint;
    private Bitmap mFieldArea;
    private Canvas mFieldAreaCanvas;
    private float mDiceDotRadius;
    private Paint mDiceDotPaint;
    private Paint mFieldPaint;
    private boolean mDiceMoveForbidden;
    private DicePosition mDiceToMove;
    private List<DicePosition> mMarkedFields;
    private int mMarkedColor;
    private Bitmap mDiceAlien;
    private int mMovedDistance;

    public RiddleDice(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(mFieldArea, 0, 0, null);
        mBorderPaint.setColor(Color.BLACK);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBorderPaint);
    }

    private void drawFieldArea() {
        mField.drawField(mFieldAreaCanvas);
    }

    @Override
    public void onClose() {
        super.onClose();
        mBorderPaint = null;
        mRand = null;
        mFieldAreaCanvas = null;
        mFieldArea = null;
        mFieldPaint = null;
        mField = null;
        mMarkedFields = null;
        mDiceToMove = null;
        mDiceDotPaint = null;
        mTransparentPaint = null;
        mDiceDotPaint = null;
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        mMarkedFields = new ArrayList<>();
        mFieldPaint = new Paint();
        mFieldPaint.setAntiAlias(true);
        mFieldPaint.setStyle(Paint.Style.FILL);
        mBitmap.setHasAlpha(true);
        mDiceDotRadius = ImageUtil.convertDpToPixel(DICE_DOT_BASE_RADIUS, mConfig.mScreenDensity);
        mDiceDotPaint = new Paint();
        mDiceDotPaint.setStyle(Paint.Style.FILL);
        mDiceDotPaint.setColor(Color.BLACK);
        mDiceDotPaint.setAntiAlias(true);
        mFieldArea = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), mBitmap.getConfig());
        mFieldAreaCanvas = new Canvas(mFieldArea);
        listener.onProgressUpdate(30);
        mTransparentPaint = new Paint();
        mTransparentPaint.setColor(Color.TRANSPARENT);
        mTransparentPaint.setAntiAlias(true);
        mRand = new Random();
        mBorderPaint = new Paint();
        mBorderPaint.setColor(Color.BLACK);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(2.f);
        listener.onProgressUpdate(50);
        initFields(res, getCurrentState());
        listener.onProgressUpdate(80);
        drawFieldArea();
        listener.onProgressUpdate(100);
    }

    private void initFields(Resources res, Compacter loadedData) {
        try {
            DiceFieldBuilder builder = new DiceFieldBuilder(FIELD_X, FIELD_Y);
            mField = builder.build(mBitmap.getWidth() / FIELD_X, mBitmap.getHeight() / FIELD_Y);
        } catch (BuildException build) {
            throw new IllegalStateException("Could not build dice field! " + build);
        }
        mDiceAlien = ImageUtil.loadBitmap(res, R.drawable.dice_alien, (int) (mField.getFieldWidth() * 0.7f), (int) (mField.getFieldHeight() * 0.7f), false);
        mDiceToMove = null;
        if (loadedData != null) {
            final int dataOffset = 1;
            for (int i = 0; dataOffset + 3 * i + 2 < loadedData.getSize(); i++) {
                int x = i % FIELD_X;
                int y = i / FIELD_X;
                if (mField.isValidPosition(x, y)) {
                    try {
                        DicePosition element = mField.getField(x, y);
                        element.mDiceNumber = loadedData.getInt(dataOffset + 3 * i);
                        element.mDiceState = loadedData.getInt(dataOffset + 3 * i + 1);
                        element.mAlpha = loadedData.getInt(dataOffset + 3 * i + 2);
                    } catch (CompactedDataCorruptException e) {
                        Log.e("Riddle", "Error reading compacted field number: " + e);
                        break;
                    }
                }
            }
        } else {
            DicePosition pos = mField.getRandomField(mRand);
            if (pos != null) {
                pos.checkCreateDice();
            }
        }
    }

    private int rollDice() {
        return 1 + mRand.nextInt(NUMBERS_PER_STATE);
    }

    private void putAchievementGameData(String key, long value) {
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.putValue(key, value, AchievementProperties.UPDATE_POLICY_ALWAYS);
        }
    }

    private void incrementAchievementGameData(String key) {
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.increment(key, 1L, 0L);
        }
    }

    private void resetFields() {
        mDiceToMove = null;
        for (DicePosition pos : mField) {
            pos.onReset();
        }
        incrementAchievementGameData(AchievementDice.KEY_GAME_RESET_COUNT);
        updateAchievementDataForFields();
    }

    private void updateAchievementDataForFields() {
        int completelyVisible = 0;
        int redCount = 0;
        int yellowCount = 0;
        int greenCount = 0;
        int purpleCount = 0;
        int alienCount = 0;
        long redNumbersAvailable = 0;
        long yellowNumbersAvailable = 0;
        long greenNumbersAvailable = 0;
        long purpleNumbersAvailable = 0;
        for (DicePosition pos: mField) {
            if (pos.mAlpha >= 255) {
                completelyVisible++;
            }
            if (pos.isOccupied()) {
                switch (pos.mDiceState) {
                    case STATE_GREEN:
                        greenNumbersAvailable |= 1L << pos.mDiceNumber; // this will only work if maximum dice number is below 64
                        greenCount++;
                        break;
                    case STATE_YELLOW:
                        yellowNumbersAvailable |= 1L << pos.mDiceNumber;
                        yellowCount++;
                        break;
                    case STATE_RED:
                        redNumbersAvailable |= 1L << pos.mDiceNumber;
                        redCount++;
                        break;
                    case STATE_ALIEN:
                        if (pos.isNumberedAlien()) {
                            purpleNumbersAvailable |= 1L << pos.mDiceNumber;
                            purpleCount++;
                        } else {
                            alienCount++;
                        }
                }
            }
        }
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.enableSilentChanges(AchievementDataEvent.EVENT_TYPE_DATA_UPDATE);
        }
        putAchievementGameData(AchievementDice.KEY_GAME_FIELDS_COMPLETELY_VISIBLE_COUNT, completelyVisible);
        putAchievementGameData(AchievementDice.KEY_GAME_RED_COUNT, redCount);
        putAchievementGameData(AchievementDice.KEY_GAME_YELLOW_COUNT, yellowCount);
        putAchievementGameData(AchievementDice.KEY_GAME_GREEN_COUNT, greenCount);
        putAchievementGameData(AchievementDice.KEY_GAME_PURPLE_COUNT, purpleCount);
        putAchievementGameData(AchievementDice.KEY_GAME_ALIEN_COUNT, alienCount);
        putAchievementGameData(AchievementDice.KEY_GAME_RED_NUMBERS_AVAILABLE, redNumbersAvailable);
        putAchievementGameData(AchievementDice.KEY_GAME_YELLOW_NUMBERS_AVAILABLE, yellowNumbersAvailable);
        putAchievementGameData(AchievementDice.KEY_GAME_GREEN_NUMBERS_AVAILABLE, greenNumbersAvailable);
        putAchievementGameData(AchievementDice.KEY_GAME_PURPLE_NUMBERS_AVAILABLE, purpleNumbersAvailable);
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.disableSilentChanges();
        }
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mDiceMoveForbidden = false;
            DicePosition field = mField.getFieldByCoordinates(event.getX(), event.getY());
            if (field != null) {
                mDiceToMove = field;
                return field.checkCreateDice();
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && mDiceMoveForbidden && mDiceToMove != null) {
            DicePosition currField = mField.getFieldByCoordinates(event.getX(), event.getY());
            if (currField != null && currField.equals(mDiceToMove)) {
                mDiceMoveForbidden = false;
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && mDiceToMove != null & !mDiceMoveForbidden) {
            DicePosition currField = mField.getFieldByCoordinates(event.getX(), event.getY());
            boolean moveAntStyle = mDiceToMove.moveAntStyle();
            long movedState = (long) mDiceToMove.mDiceState;
            long movedNumber = (long) mDiceToMove.mDiceNumber;
            mMovedDistance = 0;
            if (executeDiceMove(currField, !moveAntStyle)) {
                if (mConfig.mAchievementGameData != null) {
                    mConfig.mAchievementGameData.putValues(AchievementDice.KEY_GAME_LAST_DICE_MOVED_DISTANCE, (long) mMovedDistance, AchievementProperties.UPDATE_POLICY_ALWAYS,
                            AchievementDice.KEY_GAME_LAST_DICE_MOVED_STATE, movedState, AchievementProperties.UPDATE_POLICY_ALWAYS,
                            AchievementDice.KEY_GAME_LAST_DICE_MOVED_NUMBER, movedNumber, AchievementProperties.UPDATE_POLICY_ALWAYS);
                }
                return true;
            }
            return false;
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            mDiceMoveForbidden = false;
            mDiceToMove = null;
        }
        return false;
    }

    private int checkDiceMove(DicePosition moveToField) {
        if (mDiceToMove != null && !mDiceToMove.equals(moveToField)  && mDiceToMove.isMoveable() && moveToField != null) {
            if (mDiceToMove.moveAntStyle()) {
                return mField.isReachable(mDiceToMove, moveToField, FieldElement.DIRECT_NEIGHBORS);
            } else {
                return FieldElement.areNeighbors(mDiceToMove, moveToField, FieldElement.DIRECT_NEIGHBORS) ? 1 : -1;
            }
        }
        return -1;
    }

    private boolean executeDiceMove(DicePosition moveToField, boolean travelAsFarAsPossible) {
        int distanceToMove = checkDiceMove(moveToField);
        if (distanceToMove < 0) {
            return false;
        }
        mMovedDistance += distanceToMove;
        if (!moveToField.isBlocked()) {
            // target field is empty
            moveToField.mDiceNumber = mDiceToMove.mDiceNumber;
            mDiceToMove.mDiceNumber = 0;
            moveToField.mDiceState = mDiceToMove.mDiceState;
            mDiceToMove.mDiceState = STATE_RED;
            DicePosition movedDice = mDiceToMove;
            mDiceToMove = moveToField;
            drawFieldArea();
            if (!mDiceMoveForbidden && travelAsFarAsPossible && !executeDiceMove(mField.travelDirection(movedDice, moveToField), true)) {
                mDiceMoveForbidden = true;
            }
            return true;
        } else if (mDiceToMove.isCombinableInto(moveToField)) {
            mDiceToMove.combineInto(moveToField);
            mDiceToMove = moveToField;
            drawFieldArea();
            mDiceMoveForbidden = true;
            return true;
        }
        return false;
    }

    @NonNull
    @Override
    protected String compactCurrentState() {
        Compacter cmp = new Compacter();
        cmp.appendData(""); // in case we need it
        for (DicePosition pos : mField) {
            cmp.appendData(pos.mDiceNumber);
            cmp.appendData(pos.mDiceState);
            cmp.appendData(pos.mAlpha);
        }
        return cmp.compact();
    }


    @Override
    protected int calculateGainedScore() {
        return RiddleGame.DEFAULT_SCORE;
    }

    @Override
    public Bitmap makeSnapshot() {
        return BitmapUtil.resize(mFieldArea, SNAPSHOT_WIDTH, SNAPSHOT_HEIGHT);
    }

    @Override
    protected void initAchievementData() {

    }

    private class DicePosition extends FieldElement {
        int mDiceNumber;
        int mDiceState;
        int mAlpha;

        @Override
        public boolean isBlocked() {
            return isOccupied();
        }

        public boolean isOccupied() {
            return mDiceState == STATE_ALIEN || mDiceNumber > 0;
        }

        @Override
        public void draw(Canvas canvas, Rect fieldRect) {
            int alpha = mAlpha + (isOccupied() ? STATE_CURRENT_POSITION_TEMP_ALPHA[mDiceState] : 0);
            for (FieldElement.Neighbor neighbor : FieldElement.DIRECT_AND_DIAGONAL_NEIGHBORS) {
                if (mField.hasNeighbor(this, neighbor)) {
                    DicePosition pos = mField.getNeighbor(this, neighbor);
                    if (pos.isNumberedAlien() && FieldElement.areNeighbors(pos, this, NUMBER_NEIGBORS[pos.mDiceNumber - 1])) {
                        alpha += STATE_NEIGHBOR_TEMP_ALPHA_DIVIDE_BY_NUMBER / pos.mDiceNumber;
                    }
                }
            }
            alpha = Math.min(255, alpha);
            mTransparentPaint.setAlpha(alpha);
            canvas.drawBitmap(mBitmap, fieldRect, fieldRect, mTransparentPaint);
            if (mMarkedFields.contains(this)) {
                mBorderPaint.setColor(mMarkedColor);
            } else {
                mBorderPaint.setColor(Color.BLACK);
            }
            canvas.drawRect(fieldRect, mBorderPaint);
            if (isOccupied()) {
                RectF diceRect = new RectF();
                final float padding = ImageUtil.convertDpToPixel(2.f, mConfig.mScreenDensity);
                diceRect.set(fieldRect.left + padding, fieldRect.top + padding, fieldRect.right - padding, fieldRect.bottom - padding);
                drawDice(canvas, diceRect);
            }
        }


        private void drawDice(Canvas canvas, RectF fieldRect) {
            mFieldPaint.setColor(STATE_COLOR[mDiceState]);
            canvas.drawRoundRect(fieldRect, fieldRect.width() / 3.f, fieldRect.height() / 3.f, mFieldPaint);
            if (mDiceNumber > 0) {
                for (FieldElement.Neighbor n : NUMBER_NEIGBORS[mDiceNumber - 1]) {
                    canvas.drawCircle(fieldRect.centerX() + n.getXDelta() * fieldRect.width() / 4,
                            fieldRect.centerY() + n.getYDelta() * fieldRect.height() / 4,
                            mDiceDotRadius, mDiceDotPaint);
                }
            } else if (mDiceState == STATE_ALIEN) {
                canvas.drawBitmap(mDiceAlien, fieldRect.left + (mField.getFieldWidth() - mDiceAlien.getWidth()) / 2.f,
                        fieldRect.top + (mField.getFieldHeight() - mDiceAlien.getHeight()) / 2.f, null);
            }
        }

        private void increaseNeighborAlphaPermanently() {
            int number = mDiceNumber;
            if (number <= 0) {
                return;
            }
            mMarkedFields.clear();
            mMarkedColor = STATE_COLOR[mDiceState];
            int alphaIncrease = STATE_NEIGHBOR_PERM_ALPHA_INCREASE[mDiceState];
            if (alphaIncrease > 0) {
                FieldElement.Neighbor[] neighbors = NUMBER_NEIGBORS[Math.max(mDiceNumber - 1, 0)];
                for (FieldElement.Neighbor n : neighbors) {
                    if (mField.hasNeighbor(this, n)) {
                        DicePosition neighbor = mField.getNeighbor(this, n);
                        mMarkedFields.add(neighbor);
                        neighbor.mAlpha += alphaIncrease;
                    }
                }
            }
        }

        private boolean checkCreateDice() {
            if (!isOccupied()) {
                mDiceNumber = rollDice();
                mDiceState = STATE_RED;
                int dicesCount = 0;
                for (DicePosition pos : mField) {
                    if (pos.isOccupied()) {
                        dicesCount++;
                    }
                }
                if (dicesCount == mField.getFieldCount()) {
                    resetFields();
                } else {
                    updateAchievementDataForFields();
                }
                drawFieldArea();
                return true;
            }
            return false;
        }

        public void onReset() {
            if (mDiceNumber <= 0) {
                return;
            }
            switch (mDiceState) {
                case STATE_RED: // no break
                case STATE_YELLOW:
                    mDiceNumber = 0;
                    mDiceState = STATE_RED;
                    break;
                case STATE_GREEN: // no break
                    mDiceNumber--;
                    break;
                case STATE_ALIEN:
                    if (mDiceNumber > 0) {
                        mDiceNumber = 0;
                        mDiceState = STATE_RED;
                    }
            }
            mAlpha = (int) (ALPHA_RESET_PENALTY_FACTOR * mAlpha);
        }

        public boolean isMoveable() {
            return isOccupied();
        }

        public boolean isCombinableInto(DicePosition target) {
            return isOccupied() && target.isOccupied() &&
                    ((mDiceState == STATE_ALIEN && mDiceNumber == 0 && target.mDiceState != STATE_ALIEN)
                        || (mDiceState != STATE_ALIEN && mDiceState == target.mDiceState && mDiceNumber == target.mDiceNumber));
        }

        public void combineInto(DicePosition target) {
            if (BuildConfig.DEBUG && !isCombinableInto(target)) {
                Log.e("Riddle", "Trying to combine into not combinable.");
                return;
            }
            if (mConfig.mAchievementGameData != null) {
                mConfig.mAchievementGameData.putValues(AchievementDice.KEY_GAME_LAST_DICE_COMBINED_STATE, (long) mDiceState, AchievementProperties.UPDATE_POLICY_ALWAYS,
                        AchievementDice.KEY_GAME_LAST_DICE_COMBINED_POSITION, (long) (target.mX + target.mY * FIELD_X), AchievementProperties.UPDATE_POLICY_ALWAYS,
                        null, 0L, 0L);
            }
            if (mDiceState == STATE_ALIEN) {
                if (target.mDiceState == STATE_RED) {
                    // skip yellow and get the same number with green
                    target.mDiceState = STATE_GREEN;
                    incrementAchievementGameData(AchievementDice.KEY_GAME_ALIEN_FUSED_WITH_RED);
                } else if (target.mDiceState == STATE_YELLOW) {
                    // make target field completely visible and make it disappear
                    target.mAlpha += 255;
                    target.mDiceState = STATE_RED;
                    target.mDiceNumber = 0;
                    incrementAchievementGameData(AchievementDice.KEY_GAME_ALIEN_FUSED_WITH_YELLOW);
                } else if (target.mDiceState == STATE_GREEN) {
                    // make target a numbered alien dice
                    target.mDiceState = STATE_ALIEN;
                    incrementAchievementGameData(AchievementDice.KEY_GAME_ALIEN_FUSED_WITH_GREEN);
                }
            } else {
                target.mDiceState++;
                if (target.mDiceState == STATE_ALIEN) {
                    target.mDiceNumber = 0;
                }
            }
            target.increaseNeighborAlphaPermanently();
            mDiceNumber = 0;
            mDiceState = STATE_RED;
            updateAchievementDataForFields();
        }

        /**
         * Returns if this dice (if any) is supposed to move ant style, that is field by field discovering
         * any possible path. If false then the dice is just like a chess tower and moves in a straight line.
         * @return If the dice is supposed to search any possible path to the target.
         */
        public boolean moveAntStyle() {
            return mDiceState == STATE_RED || mDiceState == STATE_YELLOW;
        }

        /**
         * If this is an alien dice with a number.
         * @return Is this a dotted alien?
         */
        public boolean isNumberedAlien() {
            return mDiceState == STATE_ALIEN && mDiceNumber > 0;
        }
    }

    /**
     * Factory class mainly because we cannot instantiate generic arrays.
     */
    private class DiceFieldBuilder extends Field2D.Builder<DicePosition> {

        public DiceFieldBuilder(int xCount, int yCount) throws BuildException {
            for (int y = 0; y < yCount; y++) {
                nextRow();
                for (int x = 0; x < xCount; x++) {
                    nextElement(new DicePosition());
                }
            }
        }

        @Override
        protected DicePosition[][] makeArray(int rows, int columns) {
            return new DicePosition[rows][columns];
        }
    }
}

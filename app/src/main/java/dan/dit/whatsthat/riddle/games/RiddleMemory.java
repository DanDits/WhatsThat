package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.system.RiddleFragment;
import dan.dit.whatsthat.util.BuildException;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.field.Field2D;
import dan.dit.whatsthat.util.field.FieldElement;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.Dimension;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * Created by daniel on 01.08.15.
 */
public class RiddleMemory extends RiddleGame {
    private static final int DEFAULT_FIELD_X = 8;
    private static final int DEFAULT_FIELD_Y = 7; // one dimension must be a multiple of 2!
    private static final int CONTENT_IN_PATH_ALPHA = 155;
    private static final int TILE_IN_PATH_COLOR = Color.GREEN;

    private Field2D<MemoryCard> mField;
    private Dimension mFieldDimension;
    private Paint mCardBorderPaint;
    private int mPeakedCards;
    private int mFieldX;
    private int mFieldY;
    private Map<Integer, Bitmap> mCoveredCardBitmap;
    private Bitmap mFieldBitmap;
    private Canvas mFieldCanvas;
    private Bitmap mBlackUncoveredCardBitmap;
    private List<MemoryCard> mPath; // not saved, so must be only relevant for visual aspects
    private List<MemoryCard> mExplicitlySelected; // not saved, so must be only relevant for visual aspects
    private int mBlackCardsToDraw;
    private Paint mCardContentPaint;
    private Paint mCardShapePaint;
    private Paint mTileInPathPaint;

    public RiddleMemory(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    protected void initAchievementData() {
    }

    @Override
    protected int calculateGainedScore() {
        return RiddleGame.DEFAULT_SCORE;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(mFieldBitmap, 0, 0, null);
    }

    private void drawField() {
        mBlackCardsToDraw = 1;
        mField.drawField(mFieldCanvas);
    }

    @Override
    public Bitmap makeSnapshot() {
        return BitmapUtil.resize(mFieldBitmap, SNAPSHOT_WIDTH, SNAPSHOT_HEIGHT);
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        mFieldBitmap = Bitmap.createBitmap(mConfig.mWidth, mConfig.mHeight, Bitmap.Config.ARGB_8888);
        mFieldCanvas = new Canvas(mFieldBitmap);
        mExplicitlySelected = new ArrayList<>(5);

        Compacter cmp = getCurrentState();
        mFieldX = DEFAULT_FIELD_X;
        mFieldY = DEFAULT_FIELD_Y;
        if (cmp != null && cmp.getSize() > 1) {
            try {
                mFieldX = cmp.getInt(0);
                mFieldY = cmp.getInt(1);
            } catch (CompactedDataCorruptException e) {
                cmp = null;
            }
        }
        float fieldWidth = mConfig.mWidth / mFieldX;
        float fieldHeight = mConfig.mHeight / mFieldY;
        try {
            mField = new MemoryBuilder(mFieldX, mFieldY).build(fieldWidth, fieldHeight);
        } catch (BuildException be) {
            Log.e("Riddle", "Failed building field for RiddleMemory.");
            throw new RuntimeException(be);
        }
        mFieldDimension = new Dimension((int) mField.getFieldWidth(), (int) mField.getFieldHeight());
        mCardBorderPaint = new Paint();
        mCardBorderPaint.setColor(Color.BLACK);
        mCardBorderPaint.setStyle(Paint.Style.STROKE);
        mCardContentPaint = new Paint();
        mCardShapePaint = new Paint();
        mCardShapePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        mTileInPathPaint = new Paint();
        mTileInPathPaint.setColor(TILE_IN_PATH_COLOR);
        mTileInPathPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

        mCoveredCardBitmap = new HashMap<>(5);
        mCoveredCardBitmap.put(MemoryCard.STATE_COVERED_GREEN, ImageUtil.loadBitmap(res, R.drawable.memory_card_covered_green, mFieldDimension.getWidth(), mFieldDimension.getHeight(), true));
        mCoveredCardBitmap.put(MemoryCard.STATE_COVERED_YELLOW, ImageUtil.loadBitmap(res, R.drawable.memory_card_covered_yellow, mFieldDimension.getWidth(), mFieldDimension.getHeight(), true));
        mCoveredCardBitmap.put(MemoryCard.STATE_COVERED_RED, ImageUtil.loadBitmap(res, R.drawable.memory_card_covered_red, mFieldDimension.getWidth(), mFieldDimension.getHeight(), true));
        mCoveredCardBitmap.put(MemoryCard.STATE_COVERED_BLACK, ImageUtil.loadBitmap(res, R.drawable.memory_card_covered_black, mFieldDimension.getWidth(), mFieldDimension.getHeight(), true));
        mBlackUncoveredCardBitmap = ImageUtil.loadBitmap(res, R.drawable.memory_card_uncovered_black, mFieldDimension.getWidth(), mFieldDimension.getHeight(), true);

        initMemoryImages(res, cmp, listener);
        for (MemoryCard card : mField) {
            if (!card.isPairUncovered() && card.mUncovered) {
                mPeakedCards++;
            }
        }
        drawField();
    }

    private void initMemoryImages(Resources res, Compacter cmp, PercentProgressListener listener) {
        final int requiredImages = mFieldX * mFieldY / 2;
        List<Image> memoryImages = new ArrayList<>(requiredImages);
        Map<String, Image> allImages = new HashMap<>(RiddleFragment.ALL_IMAGES);
        // first use the previously used images when reloading
        if (cmp != null && cmp.getSize() > 2) {
            String imageKeys = cmp.getData(2);
            if (!TextUtils.isEmpty(imageKeys)) {
                Compacter keys = new Compacter(imageKeys);
                for (int i = 0; i < keys.getSize() && i < requiredImages; i++) {
                    Image curr = allImages.get(keys.getData(i));
                    if (curr != null && !curr.equals(mImage)) {
                        memoryImages.add(curr);
                    }
                }
            }
        }
        listener.onProgressUpdate(25);
        // if some or all are missing, fill with new random images
        boolean missedImages = false;
        if (memoryImages.size() < requiredImages) {
            missedImages = true;
            List<Image> shuffled = new ArrayList<>(allImages.values());
            Collections.shuffle(shuffled);
            for (int i = 0; i < shuffled.size() && memoryImages.size() < requiredImages; i++) {
                Image curr = shuffled.get(i);
                if (curr != null && !curr.equals(mImage) && !memoryImages.contains(curr)) {
                    memoryImages.add(curr);
                }
            }
            if (memoryImages.size() < requiredImages) {
                Log.e("Riddle", "Too little images found to build a memory riddle of " + mFieldX + "x" + mFieldY + " and all images: " + allImages);
                throw new RuntimeException("Too little images.");
            }
        }
        listener.onProgressUpdate(40);
        // now duplicate images and assign to MemoryCards and restoring order or shuffling newly
        List<Image> memoryImagesFinal = new ArrayList<>(memoryImages.size() * 2);
        boolean[] uncoveredStates = null;
        int[] coverStates = null;
        boolean shuffled = false;
        if (!missedImages && cmp != null && cmp.getSize() > 5) {
            Compacter imagePosition = new Compacter(cmp.getData(3));
            Compacter imageDoppelgangerPosition = new Compacter(cmp.getData(4));
            Compacter coverStatesRaw = new Compacter(cmp.getData(5));
            Image[] images = new Image[requiredImages * 2];
            uncoveredStates = new boolean[requiredImages * 2];
            shuffled = true;
            coverStates = new int[requiredImages * 2];
            try {
                for (int i = 0; i < coverStates.length; i++) {
                    if (i < coverStatesRaw.getSize()) {
                        coverStates[i] = coverStatesRaw.getInt(i);
                    } else {
                        coverStates[i] = MemoryCard.STATE_COVERED_GREEN;
                    }
                }
            } catch (CompactedDataCorruptException e) {
                Log.e("Riddle", "Cover state data corrupt: " + e);
                coverStates = null;
            }
            for (int i = 0; i < requiredImages; i++) {
                try {
                    int pos1 = imagePosition.getInt(i);
                    int pos2 = imageDoppelgangerPosition.getInt(i);
                    if (pos1 < 0 && -(pos1+1) < uncoveredStates.length) {
                        pos1 = -(pos1+1);
                        uncoveredStates[pos1] = true;
                    }
                    if (pos2 < 0 && -(pos2+1) < uncoveredStates.length) {
                        pos2 = -(pos2+1);
                        uncoveredStates[pos2] = true;
                    }
                    if (pos1 < images.length && pos2 < images.length) {
                        images[pos1] = memoryImages.get(i);
                        images[pos2] = images[pos1];
                    } else {
                        Log.e("Riddle", "Wrong position for memory image: " + pos1 + " or " + pos2 + " for required images: " + requiredImages);
                        shuffled = false;
                        break;
                    }
                } catch (CompactedDataCorruptException cde) {
                    Log.e("Riddle", "Error extracting positions of memory riddle." + cde);
                    shuffled = false;
                    break;
                }

            }
            if (shuffled) {
                memoryImagesFinal.addAll(Arrays.asList(images));
            }
        }
        listener.onProgressUpdate(45);
        if (!shuffled) {
            uncoveredStates = null;
            memoryImagesFinal.clear();
            memoryImagesFinal.addAll(memoryImages);
            memoryImagesFinal.addAll(memoryImages);
            Collections.shuffle(memoryImagesFinal);
        }

        // now init the MemoryCard pairs, restoring cover state and uncovered attribute if available
        listener.onProgressUpdate(50);
        int bitmapDeltaX = -(mFieldBitmap.getWidth() - mBitmap.getWidth()) / 2;
        int bitmapDeltaY = -(mFieldBitmap.getHeight() - mBitmap.getHeight()) / 2;
        for (int index = 0; index < requiredImages * 2; index++) {
            MemoryCard card = mField.getField(index % mFieldX, index / mFieldX);
            Image image = memoryImagesFinal.get(index);
            int doppelgangerIndex = memoryImagesFinal.lastIndexOf(image);
            if (index == doppelgangerIndex) {
                continue;
            }
            Log.d("Riddle", "Card index " + index + " and doppelganger " + doppelgangerIndex + " with image " + image + " in final list: " + memoryImagesFinal);
            MemoryCard doppelganger = mField.getField(doppelgangerIndex % mFieldX, doppelgangerIndex / mFieldX);
            boolean uncovered1 = false, uncovered2 = false;

            if (uncoveredStates != null) {
                uncovered1 = uncoveredStates[index];
                uncovered2 = uncoveredStates[doppelgangerIndex];
            }
            int coverState1 = MemoryCard.STATE_COVERED_GREEN, coverState2 = MemoryCard.STATE_COVERED_GREEN;
            if (coverStates != null) {
                coverState1 = coverStates[index];
                coverState2 = coverStates[doppelgangerIndex];
            }
            Rect source = mField.setFieldRect(new Rect(), card);
            source.offset(bitmapDeltaX, bitmapDeltaY);
            Rect doppelgangerSource = mField.setFieldRect(new Rect(), doppelganger);
            doppelgangerSource.offset(bitmapDeltaX, bitmapDeltaY);
            card.initPair(res, image, source, doppelgangerSource, doppelganger, uncovered1, uncovered2, coverState1, coverState2);

            listener.onProgressUpdate(50 + (int) (index * 50 / ((double) (requiredImages * 2))));
        }
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            MemoryCard card = mField.getFieldByCoordinates(event.getX(), event.getY());
            if (card != null) {
                if (mPeakedCards >= 2) {
                    coverNotFoundPairs();
                }
                mExplicitlySelected.add(card);
                card.onClick();
                drawField();
                return true;
            }
        }
        return false;
    }

    private void coverNotFoundPairs() {
        for (MemoryCard allCard : mField) {
            allCard.cover();
        }
        mPath = null;
        mExplicitlySelected.clear();
        mPeakedCards = 0;
    }

    @NonNull
    @Override
    protected String compactCurrentState() {
        Compacter cmp = new Compacter();
        cmp.appendData(mFieldX);
        cmp.appendData(mFieldY);
        // memory images
        List<String> keySet = new LinkedList<>();
        for (MemoryCard card : mField) {
            String hash = card.mMemoryImage.getHash();
            if (!keySet.contains(hash)) {
                keySet.add(hash);
            }
        }
        Compacter keys = new Compacter(keySet.size());
        for (String key : keySet) {
            keys.appendData(key);
        }
        // permutation and cover states of images
        Compacter cardPositions = new Compacter(keySet.size());
        Compacter cardDoppelgangerPositions = new Compacter(keySet.size());
        List<Image> imageList = new ArrayList<>(mField.getFieldCount());
        for (MemoryCard card : mField) {
            imageList.add(card.mMemoryImage);
        }
        for (MemoryCard card : mField) {
            int index = imageList.indexOf(card.mMemoryImage);
            int doppelgangerIndex = imageList.lastIndexOf(card.mMemoryImage);
            if (index == -1 || doppelgangerIndex == -1) {
                continue;
            }
            imageList.set(index, null);
            imageList.set(doppelgangerIndex, null);
            cardPositions.appendData(card.mUncovered ? -(index+1): index);// we encode covered state in index, since 0=-0 increase by 1
            cardDoppelgangerPositions.appendData(card.mDoppelganger.mUncovered ? -(doppelgangerIndex+1): doppelgangerIndex);
        }

        cmp.appendData(keys.compact());
        cmp.appendData(cardPositions.compact());
        cmp.appendData(cardDoppelgangerPositions.compact());

        Compacter coverStates = new Compacter(mField.getFieldCount());
        for (MemoryCard card : mField) {
            coverStates.appendData(card.mCoverState);
        }
        cmp.appendData(coverStates.compact());
        return cmp.compact();
    }

    private class MemoryCard extends FieldElement {
        public static final int STATE_COVERED_GREEN = 0;
        public static final int STATE_COVERED_YELLOW = 1;
        public static final int STATE_COVERED_RED = 2;
        public static final int STATE_COVERED_BLACK = 3;

        private boolean mUncovered;
        private MemoryCard mDoppelganger;
        private Rect mBitmapSource;
        private Image mMemoryImage;
        private Bitmap mMemoryBitmap;
        private int mCoverState;

        @Override
        public boolean isBlocked() {
            return mUncovered;
        }

        public void initPair(Resources res, Image memoryImage, Rect bitmapSource, Rect doppelgangerbitmapSource, MemoryCard doppelganger, boolean uncovered, boolean doppelgangerUncovered, int coverState, int doppelgangerCoverState) {
            mDoppelganger = doppelganger;
            mDoppelganger.mDoppelganger = this;
            mMemoryImage = memoryImage;
            mDoppelganger.mMemoryImage = memoryImage;
            mMemoryBitmap = mMemoryImage.loadBitmap(res, mFieldDimension, true);
            mDoppelganger.mMemoryBitmap = mMemoryBitmap;
            mBitmapSource = bitmapSource;
            mDoppelganger.mBitmapSource = doppelgangerbitmapSource;
            mUncovered = uncovered;
            mDoppelganger.mUncovered = doppelgangerUncovered;
            mCoverState = coverState;
            mDoppelganger.mCoverState = doppelgangerCoverState;
            Log.d("Riddle", "Init pair: " + this + " and " + mDoppelganger + " with " + mMemoryBitmap + " rect " + bitmapSource + " rect d" + doppelgangerbitmapSource + " image " + memoryImage);
        }

        public boolean isPairUncovered() {
            return mUncovered && mDoppelganger.mUncovered;
        }

        @Override
        public void draw(Canvas canvas, Rect fieldRect) {
            boolean isInPath = mPath != null && mPath.contains(this);
            int oldColor;
            Bitmap cardCover = mCoveredCardBitmap.get(mCoverState);
            cardCover = cardCover == null ? mCoveredCardBitmap.get(STATE_COVERED_GREEN) : cardCover;

            if (isPairUncovered()) {
                canvas.drawBitmap(mBitmap, mBitmapSource, fieldRect, null);
                canvas.drawBitmap(cardCover, fieldRect.left, fieldRect.top, mCardShapePaint);
            } else if (mUncovered) {
                if (mCoverState == STATE_COVERED_BLACK && mBlackCardsToDraw <= 0) {
                    canvas.drawBitmap(mBlackUncoveredCardBitmap, fieldRect.left, fieldRect.top, null);
                } else {
                    if (mCoverState == STATE_COVERED_BLACK) {
                        mBlackCardsToDraw--;
                    }
                    if (mMemoryBitmap != null) {
                        canvas.drawBitmap(mMemoryBitmap, fieldRect.left, fieldRect.top, null);
                        canvas.drawBitmap(cardCover, fieldRect.left, fieldRect.top, mCardShapePaint);
                    } else {
                        // emergency case in case bitmap loading failed since we can't fetch a new one easily
                        oldColor = mCardBorderPaint.getColor();
                        mCardContentPaint.setColor(mMemoryImage.getAverageARGB());
                        canvas.drawRect(fieldRect, mCardContentPaint);
                        canvas.drawBitmap(cardCover, fieldRect.left, fieldRect.top, mCardShapePaint);
                        mCardContentPaint.setColor(oldColor);
                    }
                }
            } else {
                canvas.drawBitmap(cardCover, fieldRect.left, fieldRect.top, null);
            }
            if (isInPath && !mExplicitlySelected.contains(this)) {
                canvas.drawRect(fieldRect, mTileInPathPaint);
            }
            /*
            // draw border on top
            oldColor = mCardBorderPaint.getColor();
            if (isInPath) {
                mCardBorderPaint.setColor(Color.YELLOW);
            }
            fieldRect.set(fieldRect.left-1, fieldRect.top-1, fieldRect.right+1, fieldRect.bottom+1);
            canvas.drawRect(fieldRect, mCardBorderPaint);
            fieldRect.set(fieldRect.left+1, fieldRect.top+1, fieldRect.right-1, fieldRect.bottom-1);
            mCardBorderPaint.setColor(oldColor);*/
        }

        public boolean uncover() {
            if (!mUncovered) {
                mUncovered = true;
                mPeakedCards++;
                if (isPairUncovered()) {
                    mPeakedCards -= 2;
                }
                return true;
            }
            return false;
        }

        public void onClick() {
            if (uncover() && isPairUncovered()) {
                mPath = mField.findPath(this, mDoppelganger, FieldElement.DIRECT_AND_DIAGONAL_NEIGHBORS);
                if (mPath != null) {
                    for (MemoryCard card : mPath) {
                        if (card.mCoverState == STATE_COVERED_GREEN) {
                            if (card.uncover()) {
                                card.mCoverState--;
                            }
                        } else {
                            card.mCoverState--;
                        }
                    }
                }
            }
        }

        public void cover() {
            if (!isPairUncovered() && mUncovered) {
                mPeakedCards--;
                mUncovered = false;
                if (mCoverState < MemoryCard.STATE_COVERED_BLACK) {
                    mCoverState++;
                }
                mCoverState = Math.max(mCoverState, STATE_COVERED_GREEN); //just to make sure we are never in an illegal state, works without though
            }
        }
    }

    private class MemoryBuilder extends Field2D.Builder<MemoryCard> {
        public MemoryBuilder(int xCount, int yCount) throws BuildException {
            for (int y = 0; y < yCount; y++) {
                nextRow();
                for (int x = 0; x < xCount; x++) {
                    nextElement(new MemoryCard());
                }
            }
        }
        @Override
        protected MemoryCard[][] makeArray(int rows, int columns) {
            return new MemoryCard[rows][columns];
        }
    }
}

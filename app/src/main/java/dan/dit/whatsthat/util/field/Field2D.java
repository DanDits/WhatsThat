package dan.dit.whatsthat.util.field;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import dan.dit.whatsthat.util.BuildException;

/**
 * Field with basic methods that is useful for RiddleGames
 * that require a 2D field layout with not many fields. If there size
 * is arbitrary, depending on the screen size or performance critical,
 * you should consider using own arrays as this class involves creation of more wrapper objects
 * and maybe data that is not used or required.<br>
 *     The elements are iterated row wise. There is always at least one row and one column for the rectangle field.
 * Created by daniel on 29.05.15.
 */
public class Field2D<FE extends FieldElement> implements Iterable<FE> {
    private int mXCount;
    private int mYCount;
    private float mFieldWidth;
    private float mFieldHeight;
    private FE[][] mFieldElements; //[0][0] top left corner, [3][1] fourth row, second column
    private Queue<FE> mPathfindingNodes;
    private Paint mClearPaint;
    private Rect mFieldRect;
    private int mFieldRectPadding = 1;

    private Field2D(FE[][] fieldElements, float fieldWidth, float fieldHeight) throws BuildException {
        int xCount = fieldElements[0].length;
        int yCount = fieldElements.length;
        if (xCount <= 0 || yCount <= 0 || fieldWidth <= 0 || fieldHeight <= 0) {
            throw new BuildException().setMissingData("Field2D", "Empty field: " + xCount + "x" + yCount + " a " + fieldWidth + "x" + fieldHeight);
        }
        for (FE[] fieldElement : fieldElements) {
            if (fieldElement.length != xCount) {
                throw new BuildException().setMissingData("Field2D", "Field not square: " + fieldElement.length);
            }
        }
        mXCount = xCount;
        mYCount = yCount;
        mFieldWidth= fieldWidth;
        mFieldHeight = fieldHeight;
        mFieldElements = fieldElements;
        mPathfindingNodes = new LinkedList<>();
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mFieldRect = new Rect();
        for (FE field : this) {
            if (field == null) {
                throw new BuildException().setMissingData("Field2D", "Null field given");
            }
        }
    }

    public void drawField(Canvas canvas) {
        canvas.drawPaint(mClearPaint);
        for (FE e : this) {
            setFieldRect(mFieldRect, e);
            e.draw(canvas, mFieldRect);
        }
    }

    public void setFieldRectPadding(int padding) {
        mFieldRectPadding = padding;
    }

    public int getFieldRectPadding() {
        return mFieldRectPadding;
    }

    public Rect setFieldRect(Rect rect, FE fieldElement) {
        rect.set((int) (mFieldWidth * fieldElement.mX) + mFieldRectPadding, (int) (mFieldHeight * fieldElement.mY) + mFieldRectPadding, (int) (mFieldWidth * fieldElement.mX + mFieldWidth) - mFieldRectPadding, (int) (mFieldHeight * fieldElement.mY + mFieldHeight) - mFieldRectPadding);
        return rect;
    }

    public FE travelDirection(FieldElement field1, FieldElement field2) {
        int x = 2 * field2.mX - field1.mX;
        int y = 2 * field2.mY - field1.mY;
        if (isValidPosition(x, y)) {
            return getField(x, y);
        }
        return null;
    }

    public float getFieldWidth() {
        return mFieldWidth;
    }

    public float getFieldHeight() {
        return mFieldHeight;
    }

    public FE getFieldByCoordinates(float xCoord, float yCoord) {
        int x = (int) (xCoord / mFieldWidth);
        int y = (int) (yCoord / mFieldHeight);
        if (isValidPosition(x, y)) {
            return mFieldElements[y][x];
        }
        return null;
    }

    public FE getField(int x, int y) {
        return mFieldElements[y][x];
    }

    public int isReachable(FE fromField, FieldElement target, FieldElement.Neighbor[] neighborTypes) {
        if (fromField.equals(target)) {
            return 0;
        }
        if (neighborTypes == null || neighborTypes.length == 0) {
            return -1;
        }
        //clear pathfinding data
        for (FE e : this) {
            e.mPathfindingValue = 0;
        }
        mPathfindingNodes.clear();

        // ant algorithm, start with fromField
        mPathfindingNodes.add(fromField);
        fromField.mPathfindingValue = 1;
        do {
            FieldElement currField = mPathfindingNodes.poll();
            for (FieldElement.Neighbor n : neighborTypes) {

                if (hasNeighbor(currField, n)) {
                    FE nextField = getNeighbor(currField, n);
                    if (nextField.equals(target)) {
                        return currField.mPathfindingValue;
                    }
                    if (nextField.mPathfindingValue == 0 && !nextField.isBlocked()) {
                        // field not yet reached and
                        // the next field is not blocked, go on
                        mPathfindingNodes.add(nextField);
                        nextField.mPathfindingValue = currField.mPathfindingValue + 1;
                    }
                }
            }
        } while (!mPathfindingNodes.isEmpty());
        return -1;
    }

    public List<FE> findPath(FE fromField, FE target, FieldElement.Neighbor[] neighborTypes) {
        int pathLength = isReachable(fromField, target, neighborTypes);
        if (pathLength == -1) {
            return null;
        }
        List<FE> path = new ArrayList<>(pathLength);
        FE curr = target;
        path.add(curr);
        for (int i = 0; i < pathLength; i++) {
            for (FieldElement.Neighbor neighbor : neighborTypes) {
                if (hasNeighbor(curr, neighbor)) {
                    FE currNeighbor = getNeighbor(curr, neighbor);
                    if (currNeighbor.mPathfindingValue == pathLength - i) {
                        curr = currNeighbor;
                        path.add(curr);
                        break;
                    }
                }
            }
        }
        Collections.reverse(path);
        return path;
    }

    public FE getNeighbor(FieldElement field, FieldElement.Neighbor neighbor) {
        return mFieldElements[field.mY + neighbor.mYDelta][field.mX + neighbor.mXDelta];
    }

    public boolean hasNeighbor(FieldElement field, FieldElement.Neighbor neighbor) {
        return field.mX + neighbor.mXDelta >= 0 && field.mX + neighbor.mXDelta < mXCount
                && field.mY + neighbor.mYDelta >= 0 && field.mY + neighbor.mYDelta < mYCount;
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < mXCount && y >= 0 && y < mYCount;
    }

    public int getFieldCount() {
        return mXCount * mYCount;
    }

    public FE getRandomField(Random rand) {
        return mFieldElements[rand.nextInt(mYCount)][rand.nextInt(mXCount)];
    }

    public static abstract class Builder<FE extends FieldElement> {
        private List<List<FE>> mRows = new ArrayList<>();
        private int mCurrRow = -1;

        public final void nextElement(FE element) throws BuildException {
            if (element == null || mCurrRow < 0 || mCurrRow >= mRows.size()) {
                throw new BuildException().setMissingData("Field2D", "NextElement null or no row yet");
            }
            List<FE> row = mRows.get(mCurrRow);
            element.mY = mCurrRow;
            element.mX = row.size();
            row.add(element);
        }

        public final void nextRow() {
            mCurrRow++;
            mRows.add(new ArrayList<FE>());
        }

        protected abstract FE[][] makeArray(int rows, int columns);

        public final Field2D<FE> build(float fieldWidth, float fieldHeight) throws BuildException {
            if (mRows.isEmpty()) {
                throw new BuildException().setMissingData("Field2D", "No rows.");
            }
            FE[][] elements = makeArray(mRows.size(), mRows.get(0).size());
            int y = 0;
            for (List<FE> row : mRows) {
                int x = 0;
                for (FE element : row) {
                    if (elements[y].length > x) {
                        elements[y][x] = element;
                        x++;
                    } else {
                        throw new BuildException().setMissingData("Field2D", "Too many elements in row " + y + ": " + elements[y].length);
                    }
                }
                y++;
            }
            return new Field2D<>(elements, fieldWidth, fieldHeight);
        }
    }

    @Override
    public Iterator<FE> iterator() {
        return new FieldIterator();
    }

    private class FieldIterator implements Iterator<FE> {
        private int mCurrX;
        private int mCurrY;

        @Override
        public boolean hasNext() {
            return mCurrX < mXCount && mCurrY < mYCount;
        }

        @Override
        public FE next() {
            FE next = mFieldElements[mCurrY][mCurrX];
            mCurrX++;
            if (mCurrX % mXCount == 0) {
                mCurrX = 0;
                mCurrY++;
            }
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Cannot remove field element.");
        }
    }
}

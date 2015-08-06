package dan.dit.whatsthat.testsubject;

import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.util.compaction.Compactable;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 06.05.15.
 */
public class TestSubjectRiddleType implements Compactable {

    private PracticalRiddleType mType;
    private boolean mSelected = true;

    TestSubjectRiddleType(PracticalRiddleType toDecorate) {
        if (toDecorate == null) {
            throw new NullPointerException();
        }
        mType = toDecorate;
    }

    TestSubjectRiddleType(Compacter compactedData) throws CompactedDataCorruptException {
        unloadData(compactedData);
    }


    @Override
    public boolean equals(Object other) {
        if (other instanceof TestSubjectRiddleType) {
            return mType.equals(((TestSubjectRiddleType) other).mType);
        } else {
            return super.equals(other);
        }
    }

    @Override
    public int hashCode() {
        return mType.hashCode();
    }

    @Override
    public String compact() {
        Compacter cmp = new Compacter();
        cmp.appendData(mType.getFullName());
        cmp.appendData(mSelected);
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData == null || compactedData.getSize() < 2) {
            throw new CompactedDataCorruptException("Data missing for TestSubjectRiddleType.");
        }
        mType = PracticalRiddleType.getInstance(compactedData.getData(0));
        mSelected = compactedData.getBoolean(1);
        if (mType == null) {
            throw new CompactedDataCorruptException("No type!").setCorruptData(compactedData);
        }
    }

    public int getIconResId() {
        return mType.getIconResId();
    }

    public int getNameResId() {
        return mType.getNameResId();
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        if (selected != mSelected) {
            mSelected = selected;
        }
        TestSubject.getInstance().saveTypes();
    }

    public PracticalRiddleType getType() {
        return mType;
    }
}

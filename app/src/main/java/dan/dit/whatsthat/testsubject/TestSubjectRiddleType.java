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
    private boolean mAvailable = false;

    protected TestSubjectRiddleType(PracticalRiddleType toDecorate) {
        if (toDecorate == null) {
            throw new NullPointerException();
        }
        mType = toDecorate;
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


    //TODO implement, save and restore, init and hold by TestSubject INSTANCE
    @Override
    public String compact() {
        return null;
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {

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
        this.mSelected = selected;
    }

    public PracticalRiddleType getType() {
        return mType;
    }
}

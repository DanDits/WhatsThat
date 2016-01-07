package dan.dit.whatsthat.testsubject;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 07.01.16.
 */
public class TestSubjectRiddleTypeController implements TestSubjectRiddleType.OnChangedListener {
    private static final String TEST_SUBJECT_PREF_RIDDLE_TYPES = "key_testsubject_riddletypes";

    public static final Comparator<PracticalRiddleType> TYPE_COMPARATOR = new Comparator<PracticalRiddleType>() {
        @Override
        public int compare(PracticalRiddleType t1, PracticalRiddleType t2) {
            if (t1.equals(t2)) {
                return 0;
            } else if (TestSubject.isInitialized()) {
                List<TestSubjectRiddleType> sortedTypes = TestSubject.getInstance()
                        .getTypesController().getAll();
                int index = 0;
                int pos1 = 0;
                int pos2 = 0;
                for (TestSubjectRiddleType type : sortedTypes) {
                    if (type.getType().equals(t1)) {
                        pos1 = index;
                    } else if (type.getType().equals(t2)) {
                        pos2 = index;
                    }
                    index++;
                }
                return pos1 - pos2;
            } else {
                return t1.getFullName().compareTo(t2.getFullName());
            }
        }
    };

    private final SharedPreferences mPrefs;
    private List<TestSubjectRiddleType> mTypes = new ArrayList<>(PracticalRiddleType
            .ALL_PLAYABLE_TYPES.size());
    private final Random mRand = new Random();

    public TestSubjectRiddleTypeController(SharedPreferences prefs) {
        mPrefs = prefs;
        init(mPrefs.getString(TEST_SUBJECT_PREF_RIDDLE_TYPES, null));
    }

    private void init(String typesDataRaw) {
        Compacter typesData = TextUtils.isEmpty(typesDataRaw) ? null : new Compacter(typesDataRaw);
        if (typesData != null) {
            for (String typeRaw : typesData) {
                try {
                    TestSubjectRiddleType type = new TestSubjectRiddleType(new Compacter(typeRaw)
                            , this);
                    if (!mTypes.contains(type)) {
                        mTypes.add(type);
                    }
                } catch (CompactedDataCorruptException e) {
                    Log.e("HomeStuff", "Could not load testsubject riddle type: " + e);
                }
            }
        }
    }


    public @Nullable
    PracticalRiddleType findNextRiddleType(boolean selectedTypesOnly,
                                           PracticalRiddleType exclude) {
        List<TestSubjectRiddleType> types = new ArrayList<>(mTypes);
        Iterator<TestSubjectRiddleType> it = types.iterator();
        while (it.hasNext()) {
            TestSubjectRiddleType next = it.next();
            if ((selectedTypesOnly && !next.isSelected())
                    || (exclude != null && next.getType().equals(exclude))) {
                it.remove();
            }
        }
        if (types.size() > 0) {
            return types.get(mRand.nextInt(types.size())).getType();
        } else {
            return null;
        }
    }

    public @NonNull
    PracticalRiddleType findNextRiddleType() {
        PracticalRiddleType type = findNextRiddleType(true, null);
        if (type == null) {
            // nothing was selected, ignore selection
            type = findNextRiddleType(false, null);
        }
        // if still null there were no riddle types, but we want to always return a valid one
        return type == null ? PracticalRiddleType.CIRCLE_INSTANCE : type;
    }

    public boolean isTypeAvailable(PracticalRiddleType type) {
        if (type == null) {
            return false;
        }
        for (TestSubjectRiddleType testType : mTypes) {
            if (testType.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    public void saveTypes() {
        if (mTypes != null) {
            Compacter typesData = new Compacter(mTypes.size());
            for (TestSubjectRiddleType type : mTypes) {
                typesData.appendData(type.compact());
            }
            mPrefs.edit().putString(TEST_SUBJECT_PREF_RIDDLE_TYPES, typesData.compact()).apply();
        }
    }

    List<TestSubjectRiddleType> getAll() {
        return mTypes;
    }

    boolean addNewType(PracticalRiddleType type) {
        for (TestSubjectRiddleType currType : mTypes) {
            if (currType.getType().equals(type)) {
                return false;
            }
        }
        mTypes.add(new TestSubjectRiddleType(type, this));
        return true;
    }

    public int getCount() {
        return mTypes.size();
    }

    @Override
    public void onSelectedChanged(TestSubjectRiddleType changed) {
        saveTypes();
    }
}

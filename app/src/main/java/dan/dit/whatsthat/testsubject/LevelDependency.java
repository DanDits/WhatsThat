package dan.dit.whatsthat.testsubject;

import android.content.res.Resources;

import java.util.HashMap;
import java.util.Map;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.util.dependencies.MinValueDependency;

/**
 * Created by Fabian on 27.09.2015.
 */
public class LevelDependency extends MinValueDependency {
    private static Map<Integer, LevelDependency> LEVEL_DEPENDENCIES = new HashMap<>();
    private LevelDependency(int level) {
        super(TestSubject.getInstance().getLevelDependency(), level);
    }

    public static LevelDependency getInstance(int level) {
        LevelDependency dep = LEVEL_DEPENDENCIES.get(level);
        if (dep == null) {
            dep = new LevelDependency(level);
            LEVEL_DEPENDENCIES.put(level, dep);
        }
        return dep;
    }

    @Override
    public CharSequence getName(Resources res) {
        return res.getString(R.string.level_dependency, getMinValue() + 1);
    }
}

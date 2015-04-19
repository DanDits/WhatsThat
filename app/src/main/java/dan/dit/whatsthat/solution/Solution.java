package dan.dit.whatsthat.solution;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.preferences.Tongue;
import dan.dit.whatsthat.util.compaction.Compactable;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 24.03.15.
 */
public class Solution implements Compactable {
    public static final int SOLVED_NOTHING = 0;
    public static final int SOLVED_COMPLETELY = 100;
    private Tongue mTongue;
    private List<String> mSolutionWords = new LinkedList<>();

    public Solution(Compacter compacter) throws CompactedDataCorruptException {
        unloadData(compacter);
    }

    /**
     * Constructs a solution of a language with an empty word. This will not
     * be an expected solution for an image, don't use it there!
     * @param tongue The tongue of the solution.
     */
    protected Solution(Tongue tongue) {
        if (tongue == null) {
            throw new IllegalArgumentException("Null tongue given.");
        }
        mTongue = tongue;
        addWord("");
    }

    public Solution(Tongue tongue, String word) {
        if (tongue == null || TextUtils.isEmpty(word)) {
            throw new IllegalArgumentException("Null tongue or word given.");
        }
        mTongue = tongue;
        addWord(word);
    }

    public Solution(Tongue tongue, String word1, String word2) {
        if (tongue == null || (TextUtils.isEmpty(word1) && TextUtils.isEmpty(word2))) {
            throw new IllegalArgumentException("Null tongue or word given.");
        }
        mTongue = tongue;
        if (!TextUtils.isEmpty(word1)) {
            addWord(word1);
        }
        if (!TextUtils.isEmpty(word2)) {
            addWord(word2);
        }
    }

    public Solution(Tongue tongue, List<String> words) {
        mTongue = tongue;
        for (String word : words) {
            if (!TextUtils.isEmpty(word)) {
                addWord(word);
            }
        }
        if (tongue == null || mSolutionWords.isEmpty()) {
            throw new IllegalArgumentException("Null tongue or no valid words given.");
        }
    }

    public Solution(Tongue tongue, String[] words) {
        mTongue = tongue;
        for (String word : words) {
            if (!TextUtils.isEmpty(word)) {
                addWord(word);
            }
        }
        if (tongue == null || mSolutionWords.isEmpty()) {
            throw new IllegalArgumentException("Null tongue or no valid words given.");
        }
    }

    private void addWord(@NonNull String word) {
        mSolutionWords.add(word.toUpperCase());
    }

    @Override
    public String toString() {
        return mTongue.getShortcut() + ": " + mSolutionWords;
    }

    @Override
    public String compact() {
        Compacter cmp = new Compacter();
        cmp.appendData(mTongue.getShortcut());
        for (String solution : mSolutionWords) {
            cmp.appendData(solution);
        }
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData.getSize() <= 1) {
            throw new CompactedDataCorruptException("Too little data to unload solution.");
        }
        mTongue = Tongue.getByShortcut(compactedData.getData(0));
        for (int i=1;i<compactedData.getSize();i++) {
            mSolutionWords.add(compactedData.getData(i));
        }
    }

    public String getMainWord() {
        return mSolutionWords.get(0);
    }

    public Tongue getTongue() {
        return mTongue;
    }

    public int estimateSolvedValue(String userWord) {
        if (userWord == null) {
            return SOLVED_NOTHING;
        }
        int maxSolved = SOLVED_NOTHING;
        for (String word : mSolutionWords) {
            // the empty word will never count as solved
            int length = Math.min(word.length(), userWord.length());
            int solvedLettersCount = 0;
            for (int i = 0; i < length; i++) {
                if (word.charAt(i) == userWord.charAt(i)) {
                    solvedLettersCount++;
                }
            }
            int currSolved = length == 0 ? SOLVED_NOTHING : (SOLVED_COMPLETELY * (solvedLettersCount / word.length()));
            if (currSolved > maxSolved) {
                maxSolved = currSolved;
            }
        }
        return maxSolved;
    }

    public List<String> getWords() {
        return mSolutionWords;
    }
}

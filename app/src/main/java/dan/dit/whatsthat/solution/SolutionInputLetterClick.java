/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.solution;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dan.dit.whatsthat.riddle.achievement.AchievementPropertiesMapped;
import dan.dit.whatsthat.riddle.achievement.holders.MiscAchievementHolder;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 12.04.15.
 */
public class SolutionInputLetterClick extends SolutionInput {
    private static final char NO_LETTER = '\0'; // this letter is not expected to be in any alphabet
    private static final int ALL_LETTERS_AMOUNT_DIVISOR = 6; // must be divisible by this number, related to ALL_LETTERS_MAX_ROWS


    private static final int LETTER_POOL_MIN_SIZE = 18; // minimum pool size of all displayed letters
    private static final int LETTER_POOL_MIN_WRONG_LETTERS = 2; // minimum amount of wrong letters
    public static final String IDENTIFIER = "LETTERCLICK";
    private final SolutionInputLetterClickLayout mLayout;
    private char[] mAllLetters; // permuted randomly including solution letters
    private int[] mAllLettersSelected; // index of letter in user letters if the letter is selected, invisible and one of the user letters

    private ArrayList<Character> mUserLetters;
    private boolean mStateCompleted;
    private boolean mShowCompleted;
    private boolean mHintShowMainSolutionWordLength;


    public SolutionInputLetterClick(Solution sol) {
        super(sol);
        mLayout = new SolutionInputLetterClickLayout(this);
    }

    public SolutionInputLetterClick(Compacter data) throws CompactedDataCorruptException {
        super(data);
        mLayout = new SolutionInputLetterClickLayout(this);
    }

    @Override
    public void reset() {
        clearAllLetters();
    }

    @Override
    public boolean provideHint(int hintLevel) {
        switch (hintLevel) {
            case HINT_LEVEL_MINIMAL:
                return provideSolutionWordLengthHint();
            default:
                return false;
        }
    }

    @Override
    public int getProvidedHintLevel() {
        return mHintShowMainSolutionWordLength ? SolutionInput.HINT_LEVEL_MINIMAL :
                SolutionInput.HINT_LEVEL_NONE;
    }

    private boolean provideSolutionWordLengthHint() {
        if (!mHintShowMainSolutionWordLength) {
            mHintShowMainSolutionWordLength = true;
            mLayout.calculateUserLetterLayout();
            return true;
        }
        return false;
    }

    @Override
    public int estimateSolvedValue() {
        return mSolution.estimateSolvedValue(userLettersToWord());
    }

    private int calculateAllLetterAmount(int minLength) {
        // ensure a minimum size and a minimum amount of extra letters added to the solution
        int amount = Math.max(LETTER_POOL_MIN_SIZE, minLength + LETTER_POOL_MIN_WRONG_LETTERS);
        // ensure amount is divisible by 2 and 3
        if (amount % ALL_LETTERS_AMOUNT_DIVISOR != 0) {
            amount += ALL_LETTERS_AMOUNT_DIVISOR - (amount % ALL_LETTERS_AMOUNT_DIVISOR);
        }
        return amount;
    }

    @Override
    protected void initSolution(@NonNull Solution solution) {
        mSolution = solution;
        List<String> solutionWords = mSolution.getWords();
        String mainWord = solutionWords.get(0);
        String alternativeWord = solutionWords.size() > 1 ? solutionWords.get(1) : null;
        int minLetterCount = mainWord.length();
        mAllLetters = new char[calculateAllLetterAmount(minLetterCount)];
        mAllLettersSelected = new int[mAllLetters.length];
        Arrays.fill(mAllLettersSelected, -1);
        mUserLetters = new ArrayList<>(mAllLetters.length);

        // first init the main word
        List<Character> allLetters = new ArrayList<>(mAllLetters.length);
        for (int i = 0; i < minLetterCount; i++) {
            allLetters.add(mainWord.charAt(i));
        }

        boolean[] usedMarker = new boolean[minLetterCount + (TextUtils.isEmpty(alternativeWord) ? 0 : alternativeWord.length())];
        // then init the alternative word as far as possible, but try to use letters already present to make the alternative word
        // only if too little or the wrong letters in main word we add the letters of the alternative word
        if (!TextUtils.isEmpty(alternativeWord)) {
            for (int i = 0; i < alternativeWord.length() && allLetters.size() < mAllLetters.length; i++) {
                char requiredChar = alternativeWord.charAt(i);
                boolean foundUnused = false;
                for (int j = 0; j < mainWord.length() && !foundUnused; j++) {
                    if (!usedMarker[j] && allLetters.get(j) == requiredChar) {
                        foundUnused = true;
                        usedMarker[j] = true;
                    }
                }
                if (!foundUnused) {
                    minLetterCount++;
                    allLetters.add(requiredChar);
                }
            }
        }

        // fill allLetters with remaining random letters, approximating the
        // distribution of letters in the used tongue
        Arrays.fill(usedMarker, false);
        while (allLetters.size() < mAllLetters.length) {
            char nextRandom = mSolution.getTongue().getRandomLetter();
            boolean nextRandomMatchedSolutionLetter = false;
            for (int j = 0; j < minLetterCount; j++) {
                if (allLetters.get(j) == nextRandom && !usedMarker[j]) {
                    usedMarker[j] = true;
                    nextRandomMatchedSolutionLetter = true;
                    break;
                }
            }
            if (!nextRandomMatchedSolutionLetter) {
                allLetters.add(nextRandom);
            }
        }
        Collections.shuffle(allLetters);
        for (int i = 0; i < allLetters.size(); i++) {
            mAllLetters[i] = allLetters.get(i);
        }
    }

    @NonNull
    @Override
    SolutionInputLayout getLayout() {
        return mLayout;
    }


    private boolean isSolved(String userWord) {
        return mSolution.estimateSolvedValue(userWord) == Solution.SOLVED_COMPLETELY;
    }

    private synchronized void checkCompleted() {
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
                checkCompleted();
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

    boolean performUserLetterClick(int userLetterIndex) {
        if (userLetterIndex < 0 || userLetterIndex >= mUserLetters.size()) {
            return false;
        }
        char clickedChar = mUserLetters.get(userLetterIndex);
        if (clickedChar != NO_LETTER) {
            int allIndex = findAllLetterIndex(userLetterIndex);
            if (allIndex != -1) {
                // remove from user selection and make available again
                mAllLettersSelected[allIndex] = -1;
                mUserLetters.set(userLetterIndex, NO_LETTER);
                // remove NO_LETTERs at the end
                removeAppendedNoLetters();
                mLayout.calculateUserLetterLayout();
                return true;
            }
        } else {
            // already not a letter, cut this one out and let cycle following letters left by one
            for (int i = userLetterIndex + 1; i < mUserLetters.size(); i++) {
                int allIndex = findAllLetterIndex(i);
                if (allIndex >= 0) {
                    mAllLettersSelected[allIndex] = i - 1;
                }
            }
            mUserLetters.remove(userLetterIndex);

            removeAppendedNoLetters();
            checkCompleted();
            mLayout.calculateUserLetterLayout();
            return true;
        }
        return false;
    }

    boolean performAllLettersClicked(int allLetterIndex) {
        if (mAllLettersSelected[allLetterIndex] == -1) {
            mAllLettersSelected[allLetterIndex] = fillLetterInUserLetters(mAllLetters[allLetterIndex]);
            mLayout.calculateUserLetterLayout();
            return true;
        }
        return false;
    }

    @Override
    public boolean onUserTouchDown(float x, float y) {
        return mLayout.executeClick(x, y);
    }

    private void clearAllLetters() {
        List<Integer> all = new ArrayList<>(mUserLetters.size());
        for (int i = 0; i < mUserLetters.size(); i++) {
            all.add(i);
        }
        clearLetters(all);
    }

    private boolean clearLetters(List<Integer> indicesToRemove) {
        for (Integer index : indicesToRemove) {
            if (index >= mUserLetters.size()) {
                continue;
            }
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
            mLayout.calculateUserLetterLayout();
            return true;
        }
        return false;
    }

    boolean showMainSolutionWordLength() {
        return mHintShowMainSolutionWordLength;
    }

    int getMainSolutionWordLength() {
        return mSolution.getWords().get(0).length();
    }

    int getUserLettersCount() {
        return mUserLetters.size();
    }

    int getAllLettersCount() {
        return mAllLetters.length;
    }

    char getUserLetter(int index) {
        return mUserLetters.get(index);
    }

    char getAllLetter(int index) {
        return mAllLetters[index];
    }

    boolean isAllLetterNotSelected(int index) {
        return mAllLettersSelected[index] == -1;
    }

    boolean showCompleted() {
        return mShowCompleted;
    }

    boolean isStateCompleted() {
        return mStateCompleted;
    }

    @Override
    public boolean onFling(MotionEvent startEvent, MotionEvent endEvent, float velocityX, float velocityY) {
        List<Integer> affectedIndicies = mLayout.getTouchedUserIndicies(startEvent.getX(), startEvent
                .getY(), endEvent.getX(), endEvent.getY());
        return affectedIndicies != null && clearLetters(affectedIndicies);
    }

    private String userLettersToWord() {
        StringBuilder builder = new StringBuilder(mUserLetters.size());
        for (Character c : mUserLetters) {
            builder.append(c);
        }
        String userWord = builder.toString();
        if (TestSubject.isInitialized()) {
            AchievementPropertiesMapped<String> data = TestSubject.getInstance().getAchievementHolder().getMiscData();
            if (data != null) {
                data.updateMappedValue(MiscAchievementHolder.KEY_SOLUTION_INPUT_CURRENT_TEXT, userWord);
            }
        }
        return userWord;
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
        return new Solution(mSolution.getTongue(), word);
    }

    @Override
    public String compact() {
        Compacter cmp = new Compacter();
        cmp.appendData(IDENTIFIER);
        cmp.appendData(mSolution.compact());
        cmp.appendData(userLettersToWord());
        cmp.appendData(String.valueOf(mAllLetters));
        Compacter cmp2 = new Compacter(mAllLettersSelected.length);
        for (int allLetterSelected : mAllLettersSelected) {
            cmp2.appendData(allLetterSelected);
        }
        cmp.appendData(cmp2.compact());
        Compacter hintsCompacter = new Compacter(1);
        hintsCompacter.appendData(mHintShowMainSolutionWordLength);
        cmp.appendData(hintsCompacter.compact());
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData == null || compactedData.getSize() < 5) {
            throw new CompactedDataCorruptException("Too little data given to build letter click.");
        }
        mSolution = new Solution(new Compacter(compactedData.getData(1)));
        wordToUserLetters(compactedData.getData(2));
        String word = compactedData.getData(3);
        mAllLetters = word.toCharArray();
        mAllLettersSelected = new int[mAllLetters.length];
        Compacter inner = new Compacter(compactedData.getData(4));
        for (int i = 0; i < inner.getSize(); i++) {
            mAllLettersSelected[i] = inner.getInt(i);
        }
        if (compactedData.getSize() >= 6) {
            inner = new Compacter(compactedData.getData(5));
            mHintShowMainSolutionWordLength = inner.getBoolean(0);
        }
    }
}

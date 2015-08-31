package dan.dit.whatsthat.testsubject.shopping.sortiment;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.shopping.SubProduct;

/**
 * Created by daniel on 30.07.15.
 */
public class HintProduct extends SubProduct {
    private static final int STATE_SHORT_LENGTH = 15;
    private View mView;
    private boolean mStateFullyVisible;
    private PracticalRiddleType mType;
    private int mHintNumber;
    private boolean mAlreadyRead;
    private int mDefaultTextColor;

    public HintProduct(PracticalRiddleType type, int hintNumber, boolean alreadyRead) {
        mType = type;
        mHintNumber = hintNumber;
        mAlreadyRead = alreadyRead;
    }

    public void setAlreadyRead(boolean alreadyRead) {
        mAlreadyRead = alreadyRead;
        setText();
    }

    @Override
    public View getView() {
        return mView;
    }

    @Override
    public void inflateView(LayoutInflater inflater) {
        mView = inflater.inflate(R.layout.hint_product, null);
        mDefaultTextColor = ((TextView) mView.findViewById(R.id.hint_text)).getCurrentTextColor();
        setText();
    }

    private void setText() {
        TextView view = ((TextView) mView.findViewById(R.id.hint_text));
        CharSequence text = mType.getRiddleHint(mView.getResources(), mHintNumber);
        if (!mAlreadyRead) {
            view.setTextColor(view.getResources().getColor(R.color.important_on_main_background));
            view.setText(R.string.article_hint_not_yet_read);
            return;
        } else if (TextUtils.isEmpty(text)) {
            view.setTextColor(Color.YELLOW);
            view.setText(R.string.article_hint_no_translation);
            return;
        }
        view.setTextColor(mDefaultTextColor);
        if (mStateFullyVisible || text.length() <= STATE_SHORT_LENGTH) {
            view.setText(text);
        } else {
            view.setText(text.subSequence(0, STATE_SHORT_LENGTH) + "...");
        }
    }

    @Override
    public boolean hasNoView() {
        return mView == null;
    }

    @Override
    public void onClick() {
        if (hasNoView()) {
            return;
        }
        mStateFullyVisible = !mStateFullyVisible;
        setText();
    }
}

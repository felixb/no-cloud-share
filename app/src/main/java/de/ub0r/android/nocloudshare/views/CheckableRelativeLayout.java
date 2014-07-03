package de.ub0r.android.nocloudshare.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.RelativeLayout;

/**
 * A checkable RelativeLayout.
 *
 * @author flx
 */
public class CheckableRelativeLayout extends RelativeLayout implements Checkable {

    private static final int[] STATE_CHECKABLE = {android.R.attr.state_checked};

    private boolean mIsChecked;

    @SuppressWarnings("UnusedDeclaration")
    public CheckableRelativeLayout(final Context context) {
        super(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public CheckableRelativeLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void setChecked(final boolean checked) {
        mIsChecked = checked;
        setChecked(this, checked);
        refreshDrawableState();
    }

    private void setChecked(final View child, final boolean checked) {
        if (child != this) {
            if (child instanceof Checkable) {
                ((Checkable) child).setChecked(checked);
            }
            child.setSelected(checked);
        }
        if (child instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) child;
            int l = vg.getChildCount();
            for (int i = 0; i < l; i++) {
                setChecked(vg.getChildAt(i), checked);
            }
        }
    }

    @Override
    public void toggle() {
        setChecked(!mIsChecked);
    }

    @Override
    protected int[] onCreateDrawableState(final int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (mIsChecked) {
            mergeDrawableStates(drawableState, STATE_CHECKABLE);
        }
        return drawableState;
    }
}

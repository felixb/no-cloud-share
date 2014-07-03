package de.ub0r.android.nocloudshare.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * @author flx
 */
public class MeasuringRelativeLayout extends RelativeLayout {

    private Runnable mRunnable;

    @SuppressWarnings("UnusedDeclaration")
    public MeasuringRelativeLayout(final Context context) {
        super(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public MeasuringRelativeLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("UnusedDeclaration")
    public MeasuringRelativeLayout(final Context context, final AttributeSet attrs,
            final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mRunnable != null) {
            mRunnable.run();
            mRunnable = null;
        }
    }

    public void registerOnMeasureListener(final Runnable r) {
        mRunnable = r;
    }
}

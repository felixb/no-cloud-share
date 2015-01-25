package de.ub0r.android.nocloudshare.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author flx
 */
public class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {


    public interface OnItemClickListener {

        public void onItemClick(View view, int position);

        public void onItemLongClick(View view, int position);
    }

    private final OnItemClickListener mListener;

    final GestureDetector mGestureDetector;

    public RecyclerItemClickListener(final Context context, final RecyclerView view,
            final OnItemClickListener listener) {
        mListener = listener;
        mGestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    public boolean onSingleTapUp(final MotionEvent e) {
                        View childView = view.findChildViewUnder(e.getX(), e.getY());
                        if (childView == null) {
                            return false;
                        }
                        mListener.onItemClick(childView, view.getChildPosition(childView));
                        return true;
                    }

                    @Override
                    public void onLongPress(final MotionEvent e) {
                        View childView = view.findChildViewUnder(e.getX(), e.getY());
                        if (childView == null) {
                            return;
                        }
                        mListener.onItemLongClick(childView, view.getChildPosition(childView));
                    }
                });
    }

    @Override
    public boolean onInterceptTouchEvent(final RecyclerView view, final MotionEvent e) {
        return mListener != null && mGestureDetector.onTouchEvent(e);
    }

    @Override
    public void onTouchEvent(final RecyclerView view, final MotionEvent motionEvent) {
    }
}
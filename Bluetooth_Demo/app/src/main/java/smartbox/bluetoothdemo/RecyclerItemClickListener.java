package smartbox.bluetoothdemo;

/**
 * Created by Yuan Zhang on 2/22/2019.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * listener for RecyclerView that's used to list shelters
 */
public class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
    private final OnItemClickListener mListener;

    public interface OnItemClickListener {
        /**
         * mando thing we don't need
         * @param view view
         * @param position position
         */
        void onItemClick(View view, int position);

        void onLongItemClick(View view, int position);
    }

    GestureDetector mGestureDetector;

    /**
     *
     * @param context
     * @param recyclerView
     * @param listener
     */
    public RecyclerItemClickListener(Context context, final RecyclerView recyclerView,
                                     OnItemClickListener listener) {
        mListener = listener;
        mGestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                        if ((child != null) && (mListener != null)) {
                            mListener.onLongItemClick(child, recyclerView.getChildAdapterPosition(child));
                        }
                    }
                });
    }

    @Override public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        View childView = view.findChildViewUnder(e.getX(), e.getY());
        if ((childView != null) && (mListener != null) && mGestureDetector.onTouchEvent(e)) {
            mListener.onItemClick(childView, view.getChildAdapterPosition(childView));
            return true;
        }
        return false;
    }

    @Override public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) { }

    @Override
    public void onRequestDisallowInterceptTouchEvent (boolean disallowIntercept){}

}


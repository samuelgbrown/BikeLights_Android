package to.us.suncloud.bikelights.common;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.ExecutionException;

public class ObservedRecyclerView extends RecyclerView {
    protected View mEmptyView;

    public ObservedRecyclerView(Context context) {
        super(context);
    }

    public ObservedRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ObservedRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private final AdapterDataObserver observer = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            checkIfEmpty();
        }

    };

    @Override
    public void setAdapter(Adapter newAdapter) {
        Adapter oldAdapter = getAdapter();

        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(observer);
        }

        super.setAdapter(newAdapter);

        if (newAdapter != null) {
            newAdapter.registerAdapterDataObserver(observer);
        }

        checkIfEmpty();
    }

    // TODO: Remove these two methods?
    // Method for checking if the adapter is empty, and to decide what view to display
    private void checkIfEmpty() {
        if (mEmptyView != null && getAdapter() != null) {
            // Determine if the adapter is empty
            boolean isEmpty = getAdapter().getItemCount() == 0;

            // Set the empty view as visible, and this view as invisible (if empty...otherwise, vise versa)
            int emptyVis;
            int thisVis;
            if (isEmpty) {
                emptyVis = VISIBLE;
                thisVis = GONE;
            } else {
                emptyVis = GONE;
                thisVis = VISIBLE;
            }
//            mEmptyView.setVisibility(isEmpty ? VISIBLE : GONE);
//            setVisibility(isEmpty ? GONE : VISIBLE);
            mEmptyView.setVisibility(emptyVis);
            setVisibility(thisVis);
        }
    }

    public void setEmptyView(View mEmptyView) { //, ViewGroup.LayoutParams params) {
        this.mEmptyView = mEmptyView;
//        this.addView(mEmptyView, 0, params); // Add the empty view to this RecyclerView
        mEmptyView.setVisibility(GONE);
//        checkIfEmpty();
    }
}

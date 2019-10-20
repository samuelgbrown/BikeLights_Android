package to.us.suncloud.bikelights.common.WheelView;

import android.animation.AnimatorSet;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Color.Bike_Wheel_Animation;
import to.us.suncloud.bikelights.common.Color.Color_;

public class ImagePatternRecyclerAdapter extends RecyclerView.Adapter<ImagePatternRecyclerAdapter.ImagePatternViewHolder> {
    private ArrayList<Boolean> isSelectedList;
    private ArrayList<Boolean> memory_IsSelectedList; // Used to calculated differences in data

    private ArrayList<Color_> colorList; // TO_DO: Change this to a Bike_Wheel_Animation?
    private ArrayList<Color_> memory_ColorList;

    private List<AnimatorSet> colorAnimatorSets; // The AnimatorSets used to animate each of the preview Color_'s in palette

    public ImagePatternRecyclerAdapter(Bike_Wheel_Animation colorList) {
        // Initialize all of the Lists using a list of Color_'s, which corresponds to the slice
        setColorList(colorList);
    }

    class ImagePatternViewHolder extends RecyclerView.ViewHolder {
        int position;
        boolean isSelected = false;

        TextView numText;
        ImageView selectedView;
        ImageView mainView;

        public ImagePatternViewHolder(View layout) {
            super(layout);

            // Extract all of the needed Views
            numText = layout.findViewById(R.id.image_pattern_num_text);
            selectedView = layout.findViewById(R.id.image_pattern_selected);
            mainView = layout.findViewById(R.id.image_pattern_main_view);

            // Create an OnClickListener to toggle the selected value of this view, and notify the isSelectedList
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Update the View and Color_ViewHolder object
                    // Change the value of isSelected
                    isSelected = !isSelected;

                    // Update the selected state of the view
                    updateSelected();

                    // Update the structure that the adapter controls
                    // Update the data structure
                    isSelectedList.set(position, isSelected);

                    // Notify the adapter that the data structure has changed
                    updateData();
                }
            };

            // Set the listener all views
            numText.setOnClickListener(listener);
            selectedView.setOnClickListener(listener);
            mainView.setOnClickListener(listener);

        }

        private void updateSelected() {
            // Use the current value of isSelected to update the visibility of the selectedView border in the View
            int vis;
            if (isSelected) {
                vis = View.VISIBLE;
            } else {
                vis = View.INVISIBLE;
            }
            selectedView.setVisibility(vis);
        }

        public void bind(int position) {
            // Get the required information from data
            // Set the position
            this.position = position;

            // Get the selected value from the isSelectedList
            isSelected = isSelectedList.get(position);


            // Update the View
            // Update the selected state of the view
            updateSelected();

            // Add the background color from the colorsList
            colorAnimatorSets.set(position, colorList.get(position).modColor_Animator(mainView, colorAnimatorSets.get(position), "BackgroundColor"));

            // Display this value in the numText

            numText.setText(Integer.toString(position + 1)); // Display the position plus one, because normal people don't think of indexing as starting at 0 :P
        }

        public void unbind() {
            // This BWA_ViewHolder is being unbound.  Finish the animation associated with this one
            colorAnimatorSets.get(position).removeAllListeners();
            colorAnimatorSets.get(position).cancel();

        }
    }

    public void setSize(int newSize) {
        setSize(newSize, false);
    }

    public void setSize(int newSize, boolean initialVal) {
        // Create a new selected List according to the new size of the data
        isSelectedList = new ArrayList<>(Collections.nCopies(newSize, initialVal));

        // Preserve as many of the Color_'s as possible (if the old and new sizes are equal, do nothing)
        if (newSize > colorList.size()) {
            colorList.addAll(new ArrayList<>(Collections.nCopies(newSize - colorList.size(), colorList.get(colorList.size() - 1)))); // Repeat the final Color_ until palette matches the new size
        } else if (newSize < colorList.size()) {
            colorList.subList(newSize + 1, colorList.size() - 1).clear(); // Remove the extra elements
        }

        // Notify the adapter that the data structure has changed
        updateData();
    }

//    public void setColorObj(Color_ newColor) {
//        // This function will modify the GUI so that all *selected* viewHolders will be of the given color (to represent the pattern)
//        for (int viewHolderInd = 0;viewHolderInd < isSelectedList.size();viewHolderInd++) {
//            // Go through each Color_ViewHolder
//            if (isSelectedList.get(viewHolderInd)) {
//                // If this Color_ViewHolder is selected, then update the palette
//                palette.set(viewHolderInd, newColor);
//            }
//        }
//
//        // After this operation has been performed, clear the selected Views
//        clearAllSelected();
//
//        // Update the GUI
//        updateData();
//    }


    public void setColorList(Bike_Wheel_Animation newColorList) {
        // Update the palette
        colorList = new ArrayList<>(newColorList.sizeImage());
//        colorAnimatorSets = new ArrayList<>(Collections.nCopies(newColorList.sizeImage(), new AnimatorSet())); // TO_DO: Should this just be changed to be newColorList.numColors() (with no other modification)?
        colorAnimatorSets = new ArrayList<>(Collections.nCopies(newColorList.numColors(), new AnimatorSet()));

        for (int i = 0;i < newColorList.sizeImage();i++) {
            colorList.add(newColorList.getP(newColorList.getIMain(i)));
        }

        // Synchronize the size of isSelected to the palette, and clear the selection
        setSize(colorList.size());
    }

    public void clearAllSelected() {
        // Create a new, blank selected data set of the same size as before (yay for code efficiency!)
        setSize(isSelectedList.size(), false);
    }

    public void setAllSelected() {
        // Create a new, blank selected data set of the same size as before (yay for code efficiency!)
        setSize(isSelectedList.size(), true);
    }

    private void updateData() {
        if (memory_ColorList != null && memory_IsSelectedList != null) {
            // Find the differences between the old and new arrays, and dispatch them
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new ImagePatternDiffUtil(memory_IsSelectedList, memory_ColorList, isSelectedList, colorList));
            result.dispatchUpdatesTo(this);
        }

        // Update the memory arrays, to be used later
        memory_IsSelectedList = isSelectedList;
        memory_ColorList = colorList;
    }

    @NonNull
    @Override
    public ImagePatternRecyclerAdapter.ImagePatternViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View layout = inflater.inflate(R.layout.image_pattern_specific_view, parent, false);

        return new ImagePatternViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull ImagePatternRecyclerAdapter.ImagePatternViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ImagePatternViewHolder holder) {
//        holder.unbind();
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public int getItemCount() {
        return isSelectedList.size();
    }

    public ArrayList<Boolean> getIsSelectedList() {
        return new ArrayList<> (isSelectedList);
    }

    private class ImagePatternDiffUtil extends DiffUtil.Callback {
        private ArrayList<Boolean> mOldSelectedList;
        private ArrayList<Boolean> mNewSelectedList;

        private ArrayList<Color_> mOldColorsList;
        private ArrayList<Color_> mNewColorsList;

        public ImagePatternDiffUtil(ArrayList<Boolean> oldSelectedList, ArrayList<Color_> oldColorsList, ArrayList<Boolean> newSelectedList, ArrayList<Color_> newColorsList) {
            mOldSelectedList = oldSelectedList;
            mNewSelectedList = newSelectedList;

            mOldColorsList = oldColorsList;
            mNewColorsList = newColorsList;
        }

        @Override
        public int getOldListSize() { return mOldSelectedList.size(); }

        @Override
        public int getNewListSize() {
            return mNewSelectedList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // The item position is as much identity as the Views have
            return oldItemPosition == newItemPosition;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // Same contents if the contents (selected status and Color_) are equal (XNOR, I guess...)
            return (mOldSelectedList.get(oldItemPosition) == mNewSelectedList.get(newItemPosition)) & (mOldColorsList.get(oldItemPosition).equals(mNewColorsList.get(newItemPosition)));
        }
    }
}

package to.us.suncloud.bikelights.common.WheelView;

import android.animation.AnimatorSet;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Color.Color_;

public class ColorSpinnerAdapter implements SpinnerAdapter {
    protected LayoutInflater inflater;
    private Context context;
    private ArrayList<Color_> colorList;
    private List<AnimatorSet> colorAnimatorSets; // The AnimatorSets that describe the animations for each of the Color_'s in palette
    private AnimatorSet mainViewAnimatorSet; // The AnimatorSets that describe the animations for each of the Color_'s in palette

    public ColorSpinnerAdapter(Context context, ArrayList<Color_> colorList) {
        this.context = context;
        this.colorList = colorList;
        colorAnimatorSets = new ArrayList<>(Collections.nCopies(colorList.size(), new AnimatorSet()));
        mainViewAnimatorSet = new AnimatorSet();

        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View returnView;

        if (convertView != null) {
            returnView = convertView;
        } else {
            returnView = inflater.inflate(R.layout.image_mod_spinner_item_view, parent, false);
        }

        // Get all of the important parts of this view
        TextView itemText = returnView.findViewById(R.id.spinner_item_text);
        ImageView itemIcon = returnView.findViewById(R.id.spinner_item_icon);

        // Put in all of the required content
        itemText.setText(context.getResources().getQuantityString(R.plurals.color_number_text, position, position));
        AnimatorSet newAnimSet = colorList.get(position).modColor_Animator(itemIcon, colorAnimatorSets.get(position), "BackgroundColor");
        colorAnimatorSets.set(position, newAnimSet);

        return returnView;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return colorList.size();
    }

    @Override
    public Object getItem(int position) {
        return colorList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View returnView;

        if (convertView != null) {
            returnView = convertView;
        } else {
            returnView = inflater.inflate(R.layout.image_mod_spinner_item_view, parent, false);
        }

        // Get all of the important parts of this view
        TextView itemText = returnView.findViewById(R.id.spinner_item_text);
        ImageView itemIcon = returnView.findViewById(R.id.spinner_item_icon);

        // Put in all of the required content
        itemText.setText(context.getResources().getQuantityString(R.plurals.color_number_text, position, position));
        mainViewAnimatorSet = colorList.get(position).modColor_Animator(itemIcon, mainViewAnimatorSet, "BackgroundColor");

        return returnView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}

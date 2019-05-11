package to.us.suncloud.bikelights.common.WheelView;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import to.us.suncloud.bikelights.R;

public class ImageModSpinnerAdapter implements SpinnerAdapter {
    //    private ArrayAdapter baseAdapter;
    private LayoutInflater inflater;
    private String[] contentStrings;
    private List<Integer> contentDrawables;

    public ImageModSpinnerAdapter(Context context) {
//        this.baseAdapter = baseAdapter;
        inflater = LayoutInflater.from(context);

        String[] allContentStrings = context.getResources().getStringArray(R.array.image_mod_start_types); // Get the content strings from resources (assume that the first string is simply the "select one" string to be displayed on the Spinner, but not selectable)
        contentStrings = Arrays.copyOfRange(allContentStrings, 1, allContentStrings.length); // Store all but the first string
        contentDrawables = new ArrayList<>();
        contentDrawables.add(R.drawable.slice);
        contentDrawables.add(R.drawable.repeat_slice);
        contentDrawables.add(R.drawable.repeat_pattern);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
//        baseAdapter.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
//        baseAdapter.unregisterDataSetObserver(observer);
    }

    @Override
    public int getCount() {
        return contentStrings.length;
    }

    @Override
    public Object getItem(int position) {
        return contentStrings[position];
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getDropDownView(final int position, View convertView, ViewGroup parent) {
        View returnView;

        // Inflate the view if needed
        if (convertView != null && (convertView.findViewById(R.id.spinner_item_text) != null)) {
            returnView = convertView;
        } else {
            returnView = inflater.inflate(R.layout.image_mod_spinner_item_view, parent, false);
        }

        // Get all of the important parts of this view
        TextView itemText = returnView.findViewById(R.id.spinner_item_text);
        ImageView itemIcon = returnView.findViewById(R.id.spinner_item_icon);

        // Put in all of the required content
        itemText.setText(contentStrings[position]);
        itemIcon.setImageResource(contentDrawables.get(position));

        return returnView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if there exists a text box in this convertView
        TextView text = null;
        if (convertView != null) {
            text = convertView.findViewById(R.id.simpleTextView);
        }

        // Create the view that will be returned
        View returnView;
        if (text == null) {
            // If the convert view has NOT be initialized before, inflate one now
            returnView = inflater.inflate(R.layout.simple_text_view, parent, false);
            text = returnView.findViewById(R.id.simpleTextView);
        } else {
            // If it has been initialized before, just pass it on
            returnView = convertView;
        }

        // Make sure that the string is set properly
        text.setText(R.string.Image_Mod_Select_One);

        return returnView;
    }

    @Override
    public int getItemViewType(int position) {
//        if (position == 0) {
//            return 0;
//        } else {
//            return 1;
//        }
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

package to.us.suncloud.bikelights.common.WheelView;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Color.Bike_Wheel_Animation;
import to.us.suncloud.bikelights.common.Color.Color_;
import to.us.suncloud.bikelights.common.Constants;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ImageModFragment.ImageModListener} interface
 * to handle interaction events.
 * Use the {@link ImageModFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageModFragment extends DialogFragment {
    // The fragment initialization parameters
    private static final String ARG_PALETTE = "ARG_PALETTE";
    private static final String ARG_IMAGE = "ARG_IMAGE";
    private static final String ARG_INITIALIZATION = "ARG_INITIALIZATION";
    private static final String ARG_ISIDLE = "ARG_ISIDLE";

    private static final int ROTATION_OFFSET = 0; // Rotation offset to make the right side of the wheel index 0

    private static final int SLICE_STARTING_SIZE = 10; // The starting width of slice

    // Parameters that the GUI keeps track of

    // Parameters related to the pattern
    ArrayList<Integer> originalImage; // A copy of the original image
    ArrayList<Integer> slice; // The "slice" of the image that will be applied to the originalImage
    ArrayList<Color_> colorList; // The list of Color_'s that the "originalImage" and "slice" indexes into

    // Values to keep track of
    int currentRotation = 0;
    boolean doRepeat = false;
    int numRepeats = 1;
    boolean doEqualSpacedRepeats = true;

    // The drawable of the patternView
    LEDViewDrawable ledViewDrawable;

    // Views in the GUI
    ImageView patternView;

    TextView sliceWidthText;

    SeekBar sliceRotateSlider;
    ImageView sliceRotateLeft;
    ImageView sliceRotateRight;
    TextView sliceRotateView;

    Switch sliceRepeatSwitch;
    ConstraintLayout repeatNumOptions;
    TextView sliceRepeatNum;
    CheckBox sliceSpaceEqually;

    Switch specifyPatternSwitch;
    ConstraintLayout patternBasicsLayout;
    ConstraintLayout patternSpecificsLayout;
    Spinner patternBasicAssign;
    RecyclerView patternRecyclerView;
    SpinnerSelectListener patternSpecificAssign;
    ImageView patternSelectAll;
    ImageView patternClearAll;
    Button cancelButton;
    Button applyButton;


    ImagePatternRecyclerAdapter patternAdapter;

    // Parameters to keep track of the pattern settings between toggles of the switch
    int patternBasic = 0;
    ArrayList<Integer> patternSpecific = new ArrayList<>(Collections.nCopies(SLICE_STARTING_SIZE, 0)); // Keeps track of the pattern that may be repeated around the wheel


    private ImageModListener mListener;

//    public interface ImageSliceInterface {
//        boolean getDataVal(int ind);
//        void setDataVal(int ind, boolean val);
//        int getDataSize();
//        void setDataSize(int size);
//    }


    public ImageModFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param palette              List of colors that image indexes into.
     * @param initializationChoice Method to use to initialize the dialog.
     * @return A new instance of fragment ImageModFragment.
     */
    public static ImageModFragment newInstance(ArrayList<Color_> palette, ArrayList<Integer> image, int initializationChoice) {
        ImageModFragment fragment = new ImageModFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PALETTE, palette);
        args.putSerializable(ARG_IMAGE, image);
        args.putInt(ARG_INITIALIZATION, initializationChoice);
        fragment.setArguments(args);

        return fragment;
    }

    public static ImageModFragment newInstance(ArrayList<Color_> palette, ArrayList<Integer> image, int initializationChoice, boolean isIdle) {
        ImageModFragment fragment = new ImageModFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PALETTE, palette);
        args.putSerializable(ARG_IMAGE, image);
        args.putInt(ARG_INITIALIZATION, initializationChoice);
        args.putBoolean(ARG_ISIDLE, isIdle);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the calling fragment, as a listener for events
        Fragment parentFragment = getParentFragment();
        try {
            mListener = (ImageModListener) parentFragment;
        } catch (ClassCastException e) {
            throw new RuntimeException(parentFragment.toString()
                    + " must implement ImageModListener");
        }

        // Initialize originalImage
        if (getArguments() != null) {
            ArrayList<Color_> palette = (ArrayList<Color_>) getArguments().getSerializable(ARG_PALETTE);
            ArrayList<Integer> image = (ArrayList<Integer>) getArguments().getSerializable(ARG_IMAGE);
            originalImage = new ArrayList<>(image);
            colorList = new ArrayList<>(palette);
        } else {
            // Uh oh...
            originalImage = new ArrayList<>(Collections.nCopies(getResources().getInteger(R.integer.num_leds), 0)); // Create a blank image
            colorList = (new Bike_Wheel_Animation(getResources().getInteger(R.integer.num_leds))).getPalette(); // Create a new, "blank" ArrayList of Color_'s
        }

    }

    public void setSliceSize(int newSize) {
        // Create a new array to represent the slice, and add/remove elements
        ArrayList<Integer> newSlice = new ArrayList<>(slice);
        if (newSize > slice.size()) {
            if (slice.size() == 0) {
                newSlice = new ArrayList<>(Collections.nCopies(newSize, 0));
                patternSpecific = new ArrayList<>(Collections.nCopies(newSize, 0));
            } else {
                // If the new size is larger, then add a few extra values
                newSlice.addAll(new ArrayList<>(Collections.nCopies(newSize - newSlice.size(), newSlice.get(newSlice.size() - 1))));
                patternSpecific.addAll(new ArrayList<>(Collections.nCopies(newSize - patternSpecific.size(), patternSpecific.get(patternSpecific.size() - 1)))); // Also modify the patternSpecific array
            }
        } else if (newSize < newSlice.size()) {
            // If the new size is smaller, then remove the extra values
            newSlice.subList(newSize, newSlice.size()).clear(); // Remove the extra elements
            patternSpecific.subList(newSize, patternSpecific.size()).clear(); // Remove the extra elements (Also modify the patternSpecific array)
        } else {
            return;
        }

        // Assign the new slice value
        setSlice(newSlice);
    }

    public void setSlice(ArrayList<Integer> newSlice) {
        slice = newSlice;

        // Adjust the specific pattern RecyclerView (whether or not it is being used)
        if (patternAdapter != null) {
            patternAdapter.setColorList(renderPatternColorList());
        }

        // Update the slice width text-box (I think the slice width can only be changed via that text box, but...whatever)
        sliceWidthText.setText(Integer.toString(newSlice.size()));

        // Redraw the pattern
        updatePattern();
    }

    public void setRotation(int newRotation) {
        // Calculate the progress along the slider that this rotation represents
        int progressVal = newRotation + Math.round(getContext().getResources().getInteger(R.integer.num_leds) / 2);

        // If the new rotation would put the rotation value out of range, then ignore it
        if (progressVal < 0 | getResources().getInteger(R.integer.num_leds) < progressVal) {
            return;
        }

        // Update the rotation to the new value
        currentRotation = newRotation;

        // Update all required widgets
        sliceRotateView.setText(Integer.toString(currentRotation));
        sliceRotateSlider.setProgress(progressVal);


        // Redraw the pattern
        updatePattern();
    }

    public int getRotation() {
        return currentRotation;
    }

    public void updatePattern() {
//        ledViewDrawable.setPalette(getColorList());
        ledViewDrawable.setImage(getNewImage());
        ledViewDrawable.setSelection(getSelected());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mod_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Prepare the GUI
        // Find all of the views in the GUI
        patternView = view.findViewById(R.id.patternView);

        sliceWidthText = view.findViewById(R.id.slice_width);

        sliceRotateSlider = view.findViewById(R.id.slice_rotate_slider);
        sliceRotateLeft = view.findViewById(R.id.slice_rotate_left);
        sliceRotateRight = view.findViewById(R.id.slice_rotate_right);
        sliceRotateView = view.findViewById(R.id.slice_Rotate_View);

        sliceRepeatSwitch = view.findViewById(R.id.slice_repeat);
        repeatNumOptions = view.findViewById(R.id.repeat_num_opts_layout);
        sliceRepeatNum = view.findViewById(R.id.slice_repeat_num);
        sliceSpaceEqually = view.findViewById(R.id.slice_space_equally);

        specifyPatternSwitch = view.findViewById(R.id.specify_pattern);
        patternBasicsLayout = view.findViewById(R.id.pattern_basics_layout);
        patternSpecificsLayout = view.findViewById(R.id.pattern_specifics_layout);
        patternBasicAssign = view.findViewById(R.id.pattern_basic_assign);
        patternRecyclerView = view.findViewById(R.id.pattern_color_list);
        patternSpecificAssign = view.findViewById(R.id.pattern_specific_assign);
        patternSelectAll = view.findViewById(R.id.image_pattern_select_all);
        patternClearAll = view.findViewById(R.id.image_pattern_clear_selected);
        cancelButton = view.findViewById(R.id.cancel_button);
        applyButton = view.findViewById(R.id.apply_button);

        // Prepare interactions for GUI

        // Set up the pattern view (to show the current image, as it stands)
        ledViewDrawable = new LEDViewDrawable(patternView, getResources().getInteger(R.integer.num_leds));
        ledViewDrawable.setPalette(getColorList());
        patternView.setImageDrawable(ledViewDrawable);

        // Set up the slice_width text box
        sliceWidthText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    return processSliceWidthInput(v);
                } else {
                    return false;
                }
            }
        });
        sliceWidthText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    processSliceWidthInput((TextView) v);
                }
            }
        });

        // Set up the rotation SeekBar
        sliceRotateSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
//                    // Convert the progress into a rotation
                    int rotation = progress - Math.round(getContext().getResources().getInteger(R.integer.num_leds) / 2);
//

                    // Notify the rest of the GUI and underlying data of the new rotation value
                    setRotation(rotation);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Set up the left rotation listener
        sliceRotateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRotation(getRotation() - 1);
            }
        });

        // Set up the right rotation listener
        sliceRotateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRotation(getRotation() + 1);
            }
        });

        // Set up the repeat slice switch
        sliceRepeatSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doRepeat = isChecked;

                int vis;
                if (doRepeat) {
                    // If repeating is enabled, reveal the settings for it
                    vis = View.VISIBLE;
                } else {
                    // If repeating is disabled, hide the settings for it
                    vis = View.GONE;
                }
                repeatNumOptions.setVisibility(vis);

                // Update the displayed pattern
                updatePattern();
            }
        });

        numRepeats = Integer.parseInt(sliceRepeatNum.getText().toString());
        sliceRepeatNum.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    return processRepeatNumInput(v);
                } else {
                    return false;
                }
            }
        });
        sliceRepeatNum.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    processRepeatNumInput((TextView) v);
                }
            }
        });

//        sliceRepeatNum.addTextChangedListener(new TextWatcher() {
//            int prevValue;
//
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                try {
//                    // Before the text is changed, record the current value of the text
//                    prevValue = Integer.parseInt(s.toString());
//                } catch (NumberFormatException e) {
//                    // Do nothing
//                }
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                if (!UPDATING_TEXT) {
//                    if (s != null) {
//                        // Get the value from the input
//                        int newVal = prevValue;
//                        if (!s.toString().isEmpty()) {
//                            try {
//                                newVal = Integer.parseInt(s.toString()); // Try to getP the value set in the text field
//                                if (newVal < 0 || getResources().getInteger(R.integer.num_leds) < newVal) {
//                                    newVal = prevValue; // If the value is out of range, then reset it
//                                }
//                            } catch (NumberFormatException e) {
//                                // Do nothing
//                            }
//                        }
//
//                        // Set the number of repeats
//                        numRepeats = newVal;
//
//                        // Redraw the pattern
//                        updatePattern();
//                    }
//                }
//            }
//        });

        sliceSpaceEqually.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Record the new value
                doEqualSpacedRepeats = isChecked;

                // Redraw the pattern
                updatePattern();
            }
        });

        // Set up the pattern portion of the GUI
        specifyPatternSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    patternBasicsLayout.setVisibility(View.GONE);
                    patternSpecificsLayout.setVisibility(View.VISIBLE);

                    // Set the current specified color pattern as the slice pattern
                    setSlice(patternSpecific);
                } else {
                    patternBasicsLayout.setVisibility(View.VISIBLE);
                    patternSpecificsLayout.setVisibility(View.GONE);

                    // Set the current color as the slice pattern
                    sliceSetColor(patternBasic);
                }
            }
        });

        // Set up the patternBasicAssign
        patternBasicAssign.setAdapter(new ColorSpinnerAdapter(getContext(), colorList));
        patternBasicAssign.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sliceSetColor(position); // Set slice to represent only the selected color
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Set an adapter that is a slightly modified version of ColorSpinnerAdapter
        patternSpecificAssign.setAdapter(new ColorSpinnerAdapter(getContext(), colorList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View layout = inflater.inflate(R.layout.simple_text_view, parent, false);
                TextView textView = layout.findViewById(R.id.simpleTextView);
                textView.setText(R.string.spinner_assign_color);

                return layout;
            }
        });
        patternSpecificAssign.setListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sliceSetPattern(position); // Set slice by changing all selected colors
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Set up the select all and clear all buttons
        patternSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patternAdapter.setAllSelected();
            }
        });

        patternClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patternAdapter.clearAllSelected();
            }
        });

        // Set up buttons at bottom
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.setImage(getNewImage());
                dismiss();
            }
        });

        // Initialize GUI according to input options
        // Get the initialization option from the input arguments
        int initialization = Constants.SINGLE_SLICE;
        boolean doIdle = false;
        Bundle args = getArguments();

        if (args != null) {
            initialization = getArguments().getInt(ARG_INITIALIZATION);
            if (args.containsKey(ARG_ISIDLE)) {
                doIdle = args.getBoolean(ARG_ISIDLE);
            }
        }

        // Determine whether or not pattern should be specified and/or slice be repeated
        boolean specifyPattern = false;
        boolean repeatSlice = false;
        switch (initialization) {
            case Constants.REPEAT_SLICE:
                repeatSlice = true;
                break;
            case Constants.REPEAT_PATTERN:
                repeatSlice = true;
                specifyPattern = true;
                break;
            default:
                // Uh oh
        }

        if (doIdle) {
            // If this is an idle image, force repeating the slice
            repeatSlice = true;
        }

        // Set the switches (toggle them because Android is kind of stupid)
        specifyPatternSwitch.setChecked(!specifyPattern);
        specifyPatternSwitch.setChecked(specifyPattern);
        sliceRepeatSwitch.setChecked(!repeatSlice);
        sliceRepeatSwitch.setChecked(repeatSlice);

        // If this is for an Idle animation, do a few specific things
        if (doIdle) {
            // Force the pattern to repeat around the wheel, equally spaced
            sliceRepeatSwitch.setVisibility(View.GONE);

            sliceSpaceEqually.setChecked(false);
            sliceSpaceEqually.setChecked(true);
            sliceSpaceEqually.setVisibility(View.GONE);
        }

        // Initialize a new slice
        ArrayList<Integer> initialSlice = new ArrayList<>(Collections.nCopies(SLICE_STARTING_SIZE, 0)); // Initialize slice with a size of 10 (doesn't really matter all that much what the size is, just needs to be set and the information will be propagated to the rest of the GUI)

        // Prepare the Pattern RecyclerView for the specific pattern
        LinearLayoutManager layout = new LinearLayoutManager(getContext(), LinearLayout.HORIZONTAL, false);
        patternRecyclerView.setLayoutManager(layout); // Make the layout horizontal
        patternAdapter = new ImagePatternRecyclerAdapter(new Bike_Wheel_Animation(colorList, initialSlice));
        patternRecyclerView.setAdapter(patternAdapter);

        // Initialize slice
        setSlice(initialSlice);

        // Initialize the GUI to have 0 rotation
        setRotation(0);
    }

    private boolean processRepeatNumInput(TextView v) {
        CharSequence s = v.getText();
        if (s != null) {
            // Get the value from the input
            int newVal = numRepeats;
            if (!s.toString().isEmpty()) {
                try {
                    newVal = Integer.parseInt(s.toString()); // Try to getP the value set in the text field
                    if (newVal < 0 || getResources().getInteger(R.integer.num_leds) < newVal) {
                        newVal = numRepeats; // If the value is out of range, then reset it
                    }
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            }

            // Set the number of repeats
            numRepeats = newVal;

            // Redraw the pattern
            updatePattern();
        }
        return true;
    }

    private boolean processSliceWidthInput(TextView v) {
        CharSequence s = v.getText();
        if (s != null) {
            // Get the value from the input
            int newVal = slice.size();
            if (!s.toString().isEmpty()) {
                try {
                    newVal = Integer.parseInt(s.toString()); // Try to getP the value set in the text field
                    if (newVal < 0 || getResources().getInteger(R.integer.num_leds) < newVal) {
                        newVal = slice.size(); // If the value is out of range, then reset it
                    }
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            }

            // Update the display of the TextView (in case it needs to be reverted)
            sliceWidthText.setText(String.valueOf(newVal));

            // Set the slice size
            setSliceSize(newVal);
        }
        return true;
    }

    private void sliceSetColor(int colorPosition) {
        // This function sets the integer array slice to represent the color in palette located at colorPosition
        ArrayList<Integer> newPattern = new ArrayList<>(Collections.nCopies(slice.size(), colorPosition)); // Build a new pattern based on this color

        // Save the new color
        patternBasic = colorPosition;

        // Set the new pattern to slice
        setSlice(newPattern);
    }

    private void sliceSetPattern(int newColorChange) {
        // This function updates the slice pattern by changing each of the selected colors in the adapter to the color represented by newColorChange

        // Build the new pattern by copying slice and setting all selected colors to the new color
        ArrayList<Integer> newPattern = new ArrayList<>(slice);
        ArrayList<Boolean> isSelected = patternAdapter.getIsSelectedList();
        for (int ind = 0; ind < slice.size(); ind++) {
            if (isSelected.get(ind)) {
                // If this color is selected, then set this color
                newPattern.set(ind, newColorChange);
            }
        }

        // Save the new pattern
        patternSpecific = newPattern;

        // Set the new pattern to slice
        setSlice(newPattern);
    }

    private Bike_Wheel_Animation renderPatternColorList() {
        // Create a Bike_Wheel_Animation that represents just the slice
        return new Bike_Wheel_Animation(colorList, slice);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

//        if (context instanceof ImageModListener) {
//            mListener = (ImageModListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement ImageModListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

//        // Send the complete new image back to the calling activity
//        mListener.setImage(getNewImage());

        // Remove listener (Why?  Because all-mighty Google says so)
        mListener = null;
    }

    public ArrayList<Integer> getNewImage() {
        // Render a Bike_Wheel_Animation
        // Set up required parameters
        int numLEDs = getResources().getInteger(R.integer.num_leds); // Number of LEDs in the wheel
        int centerPatternOffset = -Math.round(slice.size() / 2); // The offset needed to make sure that the center of the slice is located at the first index
        float repeatSliceOffset;
        int thisNumRepeats;
        if (doRepeat) {
            if (doEqualSpacedRepeats) {
                repeatSliceOffset = (float)numLEDs / (float)numRepeats; // The offset between the centers of each repeat of the pattern
            } else {
                repeatSliceOffset = slice.size();
            }
            thisNumRepeats = numRepeats;
        } else {
            repeatSliceOffset = 0;
            thisNumRepeats = 1;
        }

        // Create a numLEDs size array out of the slice, depending on how it should be repeated.  Simultaneously generate the selected array, which should be selected for all colors in the pattern/slice
        // TO_DO: If/when pattern is not properly rotated, fix it here. Probably need to add a round(numLEDs/4) offset?
        ArrayList<Integer> newImage = new ArrayList<>(originalImage); // Initialize the newImage as the original image sent to this fragment in the beginning.  Overwrite it with the "processed" slice to produce the new image

        for (int repeatNum = 0; repeatNum < thisNumRepeats; repeatNum++) {
            // Go through each repeat of the slice
            // TO_DO: Add error-checking for overlap?  Meh...
            int thisStartInd = ROTATION_OFFSET + centerPatternOffset + (int)((float)repeatNum * repeatSliceOffset) + getRotation(); // Where to start this pattern
            for (int sliceInd = 0; sliceInd < slice.size(); sliceInd++) {
                // "Inlay" this repeat of the slice into the newImage

                // Modulus thisStartInd + sliceInd into newImage.size (requires 2 lines because Java is stupid)
                int newInd = (thisStartInd + sliceInd) % newImage.size();
                if (newInd < 0) newInd += newImage.size();

                newImage.set(newInd, slice.get(sliceInd));
            }
        }

        return newImage;
    }

//    public Bike_Wheel_Animation getColorList() {
//        return new Bike_Wheel_Animation(colorList, getNewImage());
//    }


    public ArrayList<Color_> getColorList() {
        return colorList;
    }

    private ArrayList<Boolean> getSelected() {
        // Produce an isSelected list to update the LEDViewDrawable with the selected LEDs in the image
        int numLEDs = getResources().getInteger(R.integer.num_leds); // Number of LEDs in the wheel
        int centerPatternOffset = -Math.round(slice.size() / 2); // The offset needed to make sure that the center of the slice is located at the first index
        int repeatSliceOffset;
        int thisNumRepeats;
        if (doRepeat) {
            if (doEqualSpacedRepeats) {
                repeatSliceOffset = Math.round(numLEDs / numRepeats); // The offset between the centers of each repeat of the pattern
            } else {
                repeatSliceOffset = slice.size();
            }
            thisNumRepeats = numRepeats;
        } else {
            repeatSliceOffset = 0;
            thisNumRepeats = 1;
        }
        ArrayList<Boolean> isSelected = new ArrayList<>(Collections.nCopies(numLEDs, false));

        for (int repeatNum = 0; repeatNum < thisNumRepeats; repeatNum++) {
            // Go through each repeat of the slice
            // TO_DO: Add error-checking for overlap?  Meh...
            int thisStartInd = ROTATION_OFFSET + centerPatternOffset + repeatNum * repeatSliceOffset + getRotation(); // Where to start this pattern
            for (int sliceInd = 0; sliceInd < slice.size(); sliceInd++) {
                // "Inlay" this repeat of the slice into the isSelected list
                int thisInd = thisStartInd + sliceInd;

                // Modulus thisInd into newImage.size (requires 2 lines because Java is stupid)
                int newInd = thisInd % isSelected.size();
                if (newInd < 0) newInd += isSelected.size();

                isSelected.set(newInd, true);
            }
        }

        return isSelected;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface ImageModListener {
        void setImage(ArrayList<Integer> image);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (ledViewDrawable != null) {
            ledViewDrawable.setAnimationRunning(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (ledViewDrawable != null) {
            ledViewDrawable.setAnimationRunning(false);
        }
    }
}

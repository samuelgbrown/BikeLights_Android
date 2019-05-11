package to.us.suncloud.bikelights.common.WheelView;

import android.animation.AnimatorSet;
import android.content.DialogInterface;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;


import java.io.Serializable;
import java.util.Arrays;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Color.Bike_Wheel_Animation;
import to.us.suncloud.bikelights.common.Color.Color_;
import to.us.suncloud.bikelights.common.Color.Color_Static;
import to.us.suncloud.bikelights.common.Color.Color_d;
import to.us.suncloud.bikelights.common.Color.Color_dTime;
import to.us.suncloud.bikelights.common.Color.Color_dVel;
import to.us.suncloud.bikelights.common.Color.colorObj;
import to.us.suncloud.bikelights.common.Constants;
import to.us.suncloud.bikelights.common.colorpickerview.BikeLightsFlagView;
import to.us.suncloud.bikelights.common.colorpickerview.ColorEnvelope;
import to.us.suncloud.bikelights.common.colorpickerview.ColorPickerDialog;
import to.us.suncloud.bikelights.common.colorpickerview.ColorPickerPreferenceManager;
import to.us.suncloud.bikelights.common.colorpickerview.ColorPickerView;
import to.us.suncloud.bikelights.common.colorpickerview.listeners.ColorEnvelopeListener;

public class ColorDefRecyclerAdapter extends ColorDefRecyclerView.Adapter<ColorDefRecyclerAdapter.bindViewHolder> {
    private ColorDefRecyclerAdapter thisAdapter;
    private final String TAG = "ColorDefRecyclerAdapter";
    private static final int VIEWTYPE_FOOTER = 0; // Used to refer to the Footer at the bottom of the ColorDefRecyclerAdapter view
    private static final int VIEWTYPE_COLOROBJ_S = 1; // Used to refer to a view that describes a static colorObj
    private static final int VIEWTYPE_COLOROBJ_D = 2; // Used to refer to a view that describes a dynamic colorObj

    private static final float[] LEDWhiteRatio = {1f, 1f, 1.2f};

    private static final int PAYLOAD_UPDATE_POSITION = 0; // Define payload: update ONLY the position of a viewholder (to update the colornumber text view)

    // Who cares about being memory efficient???
    // Keep track of a color object of each type, so that the user can switch back and forth between them (using the main spinner) without losing much work
//    private int curColor_Type = Constants.COLOR_STATIC; // The exact type of Color_ being modified
    //    private int curColor_ViewType = Constants.COLOR_STATIC; // The type of view that should be implemented for each object of the current Color_'s type
    private Color_ color_Choice_Static = new Color_Static();
    private Color_ color_Choice_Time = new Color_dTime();
    private Color_ color_Choice_Vel = new Color_dVel();
    private Color_ color_Choice_Current = new Color_Static(); // The Color_ that the View is currently editing
    private Color_ color_Memory; // Keep track of the last recorded version of the Color_ used with DiffUtil to find what has updated during recent events

    private ImageView colorViewMain = null; // The ImageView that represents the main view of the color as it currently is recorded
    private AnimatorSet colorViewAnimSet = new AnimatorSet();  // The AnimatorSet that controls the repeated color animation of the colorViewMain object.  Gets passed back to the current Color_ object to be cleared and loaded with a new animation

    private final int colorObjDView_r = R.layout.color_obj_view_d_alt;
    private final int colorObjSView_r = R.layout.color_obj_view_alt;
    private final int colorObjFooterView_r = R.layout.color_footer_view;

    public ColorDefRecyclerAdapter(Color_ initialColor) {
        color_Memory = initialColor;
        setColor_(initialColor);

        thisAdapter = this;
    }

    public void setColorViewMain(ImageView colorViewMain) {
        this.colorViewMain = colorViewMain;
    }

    public abstract class bindViewHolder extends ColorDefRecyclerView.ViewHolder implements Serializable {
        public bindViewHolder(final View layoutView) {
            super(layoutView);
        }

        public abstract void bind(int position);

        public abstract void bind_updateColorNum(int position);
    }

    public class Color_ViewHolder extends bindViewHolder {
        private static final int UNSET = -1;
        //        private boolean UPDATING_COLOR = false; // Is the updateColor() method currently running?  If so, do not do anything in the TextWatcher for the color objects' R, G, B, W, or T TextViews (otherwise an infinite loop will occur)
        int position = UNSET;
        View layoutView;

        // Color_Static Views (also used for Color_Dynamic)
        private TextView color_r;
        private TextView color_g;
        private TextView color_b;
        private TextView color_w;
        private ImageView color_preview;

        // Color Picker information
        ColorPickerView colorPickerView; // The instance of the colorPickerView unique to this color/
//        ColorPickerPreferenceManager manager; // The preference (save state) manager for the colorPickerView;


        // Color_Dynamic Views
        TextView tName; // The name of the t value in this context (time, speed, etc.)
        EditText tView; // The input field for the user to adjust the t value
        Spinner bSpinner; // The spinner (menu) that allows the user to choose the blend type for this colorObj
        ArrayAdapter<CharSequence> bSpinnerAdapter; // A helper object for the bSpinner, which stores the possible choices
        ImageView removeColor; // The View that corresponds to the delete function for this color
        ImageView copyColor; // The View that corresponds to the copy function for this color
        ImageView moveColorUp; // The View that corresponds to moving this color up
        ImageView moveColorDown; // The View that corresponds to moving this color down
        TextView colorNumText; // The number in the series of this colorObj

        public Color_ViewHolder(final View layoutView, int viewType) {
            super(layoutView);

            // Store the layout View (the parent view)
            this.layoutView = layoutView;

            // Store all of the Views in the Static portion of this Color_ViewHolder
            color_r = layoutView.findViewById(R.id.colorObj_R); // RGBW TextViews
            color_g = layoutView.findViewById(R.id.colorObj_G);
            color_b = layoutView.findViewById(R.id.colorObj_B);
            color_w = layoutView.findViewById(R.id.colorObj_W);
            color_preview = layoutView.findViewById(R.id.ColorObj_Preview); // Preview ImageView

            // Create a text watcher to keep track of when the user inputs new information
            TextView.OnEditorActionListener colorPortionWatcher = new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        return processColorPortionInput(v);
                    } else {
                        return false;
                    }
                }
            };

            View.OnFocusChangeListener colorPortionFocusWatch = new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
//                    if (!hasFocus) {
//                        processColorPortionInput((TextView) v);
//                    }
                }
            };

            // Add the text changed watcher
            color_r.setOnEditorActionListener(colorPortionWatcher);
            color_r.setOnFocusChangeListener(colorPortionFocusWatch);
            color_g.setOnEditorActionListener(colorPortionWatcher);
            color_g.setOnFocusChangeListener(colorPortionFocusWatch);
            color_b.setOnEditorActionListener(colorPortionWatcher);
            color_b.setOnFocusChangeListener(colorPortionFocusWatch);
            color_w.setOnEditorActionListener(colorPortionWatcher);
            color_w.setOnFocusChangeListener(colorPortionFocusWatch);

            // Set up the color_preview View
            // Set an onClickListener for the color_preview, to start a color picker and graphically select a color
            View.OnClickListener createColorPickerListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ColorPickerDialog.Builder builder = new ColorPickerDialog.Builder(layoutView.getContext());
                    builder.setTitle("ColorPicker Dialog");
//                    builder.setFlagView(new ColorFlag(layoutView.getContext(), R.layout.color_flag));
                    builder.setPositiveButton(layoutView.getContext().getString(R.string.confirm_color), new ColorEnvelopeListener() {
                        @Override
                        public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                            // Set the new color to the current Color_
                            saveColorEnvelopeToCurrentColor_(envelope, false);

                            // Save the settings from this colorPickerView
                            saveColorPickerSettings();
                        }
                    });
                    builder.setNegativeButton(layoutView.getContext().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    builder.setNeutralButton(layoutView.getContext().getString(R.string.confirm_color_toRGBW), new ColorEnvelopeListener() {
                        @Override
                        public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                            // Convert this ColorEnvelope so that it uses RGBW instead of just RGB

                            // Set the new color to the current Color_
                            saveColorEnvelopeToCurrentColor_(envelope, true);

                            // Save the settings from this colorPickerView
                            saveColorPickerSettings();
                        }
                    });
//                builder.attachAlphaSlideBar(); // attach AlphaSlideBar
                    builder.attachBrightnessSlideBar(); // attach BrightnessSlideBar

                    colorPickerView = builder.getColorPickerView();
                    colorPickerView.setFlagView(new BikeLightsFlagView(layoutView.getContext(), R.layout.color_flag));
                    colorPickerView.setPreferenceName(getPrefName()); // Connect this picker to the colorObj being manipulated

                    // Do some customization of the view
                    // Make it appear the same as it did before.
//                    manager.restoreColorPickerData(colorPickerView);

                    builder.show(); // show dialog
                }

            };

            color_preview.setOnClickListener(createColorPickerListener);

            // See if this view is describing a colorObj for a Dynamic Color_.  If so, do a bit more processing
            if (viewType == VIEWTYPE_COLOROBJ_D) {
                // Prepare the T-value
                tName = layoutView.findViewById(R.id.text_T); // Store the t name view
                tView = layoutView.findViewById(R.id.colorObj_T);
                tView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            return processTViewInput(v);
                        } else {
                            return false;
                        }
                    }
                });
                tView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
//                        if (!hasFocus) {
//                            processTViewInput((TextView) v);
//                        }
                    }
                });

                // Prepare the blend-type spinner
                bSpinner = layoutView.findViewById(R.id.blendTypeSpinner);
                // Get an "adapter" that will keep track of the possible blend type strings
                bSpinnerAdapter = ArrayAdapter.createFromResource(bSpinner.getContext(), R.array.blend_types, android.R.layout.simple_spinner_item);
                bSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                bSpinner.setAdapter(bSpinnerAdapter);
                bSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {
                        if (position != UNSET) {
                            String choice = (String) parent.getItemAtPosition(spinnerPosition); // Get the choice as a string

                            Color_d.BLEND_TYPE newBlend;
                            // Set the position-th blend type
                            if (bSpinner.getResources().getString(R.string.Blend_Constant).equals(choice)) {
                                // Assign constant blend-type to the position-th colorObj in color_Choice.
                                newBlend = Color_d.BLEND_TYPE.CONSTANT;
                            } else if (bSpinner.getResources().getString(R.string.Blend_Linear).equals(choice)) {
                                // Assign linear blend-type to the position-th colorObj in color_Choice.
                                newBlend = Color_d.BLEND_TYPE.LINEAR;
                            } else {
                                // Uh-oh...
                                newBlend = Color_d.BLEND_TYPE.CONSTANT;
                            }

                            // Update the blend type
                            ((Color_d) color_Choice_Current).setB(newBlend, position);

                            // Let the adapter know something has changed
                            notifyChangeColor_();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // TODO: SAM: Do nothing, I think?  Test.
                    }
                });

                // Prepare the up/down arrows, as well as the delete button
                removeColor = layoutView.findViewById(R.id.removeColor);
                copyColor = layoutView.findViewById(R.id.copyColor);
                moveColorDown = layoutView.findViewById(R.id.moveColorDown);
                moveColorUp = layoutView.findViewById(R.id.moveColorUp);

                removeColor.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Delete this preference from the ColorPickerManager (for memory purposes)
                        clearFromColorPickerPrefManager();

                        // Remove this colorObj from the Color_
                        if (position != UNSET) {
                            ((Color_d) color_Choice_Current).removeIndex(position);
                        }

                        // Let the adapter know something has changed
//                        notifyItemRangeChanged(position, color_Choice_Current.getNumColors() - position, PAYLOAD_UPDATE_POSITION); // This and all above it have been changed, but only their position numbers
//                        notifyItemChanged(position + 1, PAYLOAD_UPDATE_POSITION);
                        notifyDataSetChanged();
                        notifyChangeColor_();

                    }
                });

                copyColor.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Create a new copy of this colorObjMeta
                        Color_d thisCol = (Color_d) color_Choice_Current;
                        thisCol.addNewColorObj(thisCol.getColorObjMeta(position));

                        // Copy any color picker preferences over
                        ColorPickerPreferenceManager manager = ColorPickerPreferenceManager.getInstance(layoutView.getContext());
                        if (manager.getColor(getPrefName(), 0) != 0) {
                            // If a colorPicker preference exists for this colorObj, then add a new preference for this newly created color
                            int newPosition = thisCol.getNumColors() - 1; // The position in thisCol of the new color
                            String newPrefName = "T" + Integer.toString(thisCol.getColorType()) + "P" + Integer.toString(thisCol.getColorObjMeta(newPosition).getID()); // The unique preferences name of this new color
                            String oldPrefName = getPrefName();

                            // Set all saved values
                            manager.setColor(newPrefName, manager.getColor(oldPrefName, 0));
                            manager.setAlphaSliderPosition(newPrefName, manager.getAlphaSliderPosition(oldPrefName, 0));
                            manager.setBrightnessSliderPosition(newPrefName, manager.getBrightnessSliderPosition(oldPrefName, 0));
                            manager.setSelectorPosition(newPrefName, manager.getSelectorPosition(oldPrefName, new Point(120, 120)));
                        }

                        notifyItemRangeChanged(color_Choice_Current.getNumColors() - 2, 1); // Notify the color that was (until just a moment ago) the last (lowest in the GUI) color, to let it know that it is no longer the last color
                        notifyChangeColor_();
                    }
                });

                moveColorDown.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Move this colorObj down in the Color_
                        if (position != UNSET) {
                            ((Color_d) color_Choice_Current).moveIndexDown(position);
                        }

                        // Let the adapter know something has changed
                        notifyItemRangeChanged(position, 2); // This and the one above is have been changed
                        notifyChangeColor_();
                    }
                });

                moveColorUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Move this colorObj up in the Color_
                        if (position != UNSET) {
                            ((Color_d) color_Choice_Current).moveIndexUp(position);
                        }

                        // Let the adapter know something has changed
                        notifyItemRangeChanged(position - 1, 2); // This and the one above is have been changed
                        notifyChangeColor_();
                    }
                });

                // Find the colorNum text box
                colorNumText = layoutView.findViewById(R.id.color_num_text);
            }
        }

        private void saveColorEnvelopeToCurrentColor_(ColorEnvelope envelope, boolean convertToRGBW) {
            // TODO: Clean this up, use a white value?
            int[] colorRGBW = new int[4];
//                        int[] colorRGB = new int[3];
            System.arraycopy(envelope.getArgb(), 1, colorRGBW, 0, 3);

            if (convertToRGBW) {
                // If converting to RGBW, then take any common power from RGB (given by the ratio in LEDWhiteRatio), and convert it to white
                float[] whiteColorPowerRatio = new float[3];
                for (int i = 0; i < 3; i++) {
                    whiteColorPowerRatio[i] = (((float) colorRGBW[i]) / LEDWhiteRatio[i])/255f;
                }

                // Find the minimum white power in the RGB triple, and which LED it corresponds to
                float[] whiteColorPowerRatioSorted = Arrays.copyOf(whiteColorPowerRatio, whiteColorPowerRatio.length);
                Arrays.sort(whiteColorPowerRatioSorted);
                float whiteColorPowerMinRatio = whiteColorPowerRatioSorted[0];
                int whiteColorPowerMinIndex = findMinIndex(whiteColorPowerRatio);
                int originalMinPower = colorRGBW[whiteColorPowerMinIndex];

                // Remove the corresponding amount of white power from each RGB
                for (int i = 0; i < 3; i++) {
                    colorRGBW[i] = Math.round((float) colorRGBW[i] * (1 - (whiteColorPowerMinRatio * (LEDWhiteRatio[i] / LEDWhiteRatio[whiteColorPowerMinIndex]))));
                }

                // Add the white power to colorRGBW
                colorRGBW[3] = Math.round((float) originalMinPower * whiteColorPowerMinRatio*(1/LEDWhiteRatio[whiteColorPowerMinIndex]));
            }

            // Set the colorObj at this position in the RecyclerView using the color selected from the colorPickerView
            color_Choice_Current.setColorObj(new colorObj(colorRGBW), position);

            // Let the adapter know something has changed
            notifyChangeColor_();
        }

        private int findMinIndex(float[] array) {
            int minIndex = 0;
            float minVal = array[0];
            for (int i = 1; i < array.length; i++) {
                if (array[i] < minVal) {
                    minIndex = i;
                }
            }

            return minIndex;
        }

        private void saveColorPickerSettings() {
            colorPickerView.saveCurrentColorToFile(); // Save the color that was chosen to file, so it can be referenced later
            ColorPickerPreferenceManager.getInstance(layoutView.getContext()).saveColorPickerData(colorPickerView);
        }

        private String getPrefName() {
            return ColorDefRecyclerAdapter.this.getPrefName(color_Choice_Current, position);
        }

        private void clearFromColorPickerPrefManager() {
            ColorPickerPreferenceManager manager = ColorPickerPreferenceManager.getInstance(layoutView.getContext());
            manager.clearSavedAlphaSliderPosition(getPrefName());
            manager.clearSavedBrightnessSlider(getPrefName());
            manager.clearSavedColor(getPrefName());
            manager.clearSavedSelectorPosition(getPrefName());
        }

        private boolean processTViewInput(TextView v) {
            CharSequence s = v.getText(); // Get the string to be checked
            int prevValue = ((Color_d) color_Choice_Current).getT(position); // Get current colorObj

            if (s != null) {
                // Get the value from the input
                int newVal;
                if (s.toString().isEmpty()) {
                    newVal = prevValue;
                } else {
                    try {
                        newVal = Integer.parseInt(s.toString());
                        if (newVal < 0) {
                            // If the input value is less than 0, then just set it back to the previous value
                            newVal = prevValue;
                        }
                    } catch (NumberFormatException e) {
                        newVal = prevValue;
                    }
                }

                // Update the Color_ data
                boolean listChanged = ((Color_d) color_Choice_Current).setT(newVal, position);
                if (listChanged) {
                    // Setting T means that the color_Choice_Current colorObjMeta list MAY have completely changed by adding a new colorObj to the beginning
                    // If the list did NOT stay the same, then let the RecyclerAdapter know that ALL values have changed (but only their positions)
                    notifyItemRangeChanged(0, color_Choice_Current.getNumColors(), PAYLOAD_UPDATE_POSITION);
                }

                // Let the adapter know something has changed
                notifyChangeColor_();
            }
            return true;
        }

        private boolean processColorPortionInput(TextView v) {
            CharSequence s = v.getText(); // Get the string to be checked
            colorObj c = color_Choice_Current.getColorObj(position); // Get current colorObj

            // Find the previous value by checking each color input
            int prevValue = 0;
            if (v.hashCode() == color_r.hashCode()) {
                // R
                prevValue = c.getR();
            } else if ((v.hashCode() == color_g.hashCode())) {
                // G
                prevValue = c.getG();
            } else if ((v.hashCode() == color_b.hashCode())) {
                // B
                prevValue = c.getB();
            } else if ((v.hashCode() == color_w.hashCode())) {
                // W
                prevValue = c.getW();
            }

            if (s != null && position != UNSET) {
                // Get the value from the input
                int newVal = prevValue;
                if (!s.toString().isEmpty()) {
                    try {
                        newVal = Integer.parseInt(s.toString()); // Try to getP the value set in the text field
                        if (newVal < 0 || layoutView.getContext().getResources().getInteger(R.integer.max_color_val) < newVal) {
                            newVal = prevValue; // If the value is out of range, then reset it
                        }
                    } catch (NumberFormatException e) {
                        // Do nothing
                    }
                }

                // Generate a CharSequence based on the new value
                CharSequence newValS = String.valueOf(newVal);

                // Ensure that the correct color is being updated (allows using this same TextWatcher on all TextViews
                if (v.hashCode() == color_r.hashCode()) {
                    // R
                    c.setR(newVal);
                    color_r.setText(newValS); // Update the display of the TextView (in case it needs to be reverted)
                } else if ((v.hashCode() == color_g.hashCode())) {
                    // G
                    c.setG(newVal);
                    color_g.setText(newValS);
                } else if ((v.hashCode() == color_b.hashCode())) {
                    // B
                    c.setB(newVal);
                    color_b.setText(newValS);
                } else if ((v.hashCode() == color_w.hashCode())) {
                    // W
                    c.setW(newVal);
                    color_w.setText(newValS);
                }

                // Set the colorObj back into color_Choice_Current
                color_Choice_Current.setColorObj(c, position);

                // Let the adapter know something has changed
                notifyChangeColor_();
            }
            return true;
        }

        public void bind_updateColorNum(int position) {
            // This bind procedure is for the special case that the list gets reordered but not otherwise modified, and the colorNum on the viewholder needs to be updated
            this.position = position;
            colorNumText.setText(Integer.toString(position + 1));
        }

        public void bind(int position) {
            // Store the position of the newly bound data
            this.position = position;
            int viewType = getItemViewType();
            colorObj c = color_Choice_Current.getColorObj(position); // Get the current color

            // 1. Update the RGBW text boxes
//            UPDATING_COLOR = true;
            color_r.setText(Integer.toString(c.getR()));
            color_g.setText(Integer.toString(c.getG()));
            color_b.setText(Integer.toString(c.getB()));
            color_w.setText(Integer.toString(c.getW()));
//            UPDATING_COLOR = false;

            // 2. Update the color preview
//            color_preview.setColorFilter(Color.rgb(c_r, c_g, c_b), PorterDuff.Mode.DST_ATOP);
            color_preview.setBackgroundColor(c.getColorInt());

            // See if this view is describing a colorObj for a Dynamic Color_.  If so, do a bit more processing
            if (viewType == VIEWTYPE_COLOROBJ_D) {
                int t = ((Color_d) color_Choice_Current).getT(position);
                Color_d.BLEND_TYPE b = ((Color_d) color_Choice_Current).getB(position);

                // 3. Update the T value
                tName.setText(getTTypeString(color_Choice_Current.getColorType())); // Set the title for the T type
//                UPDATING_COLOR = true;
                tView.setText(Integer.toString(t));
//                UPDATING_COLOR = false;

                // 4. Set up the Blend Spinner
                bSpinner.setSelection(bSpinnerAdapter.getPosition(getBlendTypeString(b))); // Get the index in the adapter of the current Blend-type choice

                // 5. Set visibility of the up/down arrows and remove button (getItemCount() acts a bit weird because getItemCount() also counts the footer)
                if (position == 0 && getItemCount() == 2) {
                    // If this is the only colorObj...
                    setIsOnly();
                } else if (position == 0) {
                    // If this colorObj is at the top...
                    setIsTop();
                } else if (position == (getItemCount() - 2)) {
                    // If this colorObj is at the bottom...
                    setIsBottom();
                } else {
                    // If this colorObj is in the middle...
                    setIsMid();
                }

                int itemCount = getItemCount(); // Number of items in the recycler
                if (itemCount == 2) {
                    // If this is the only colorObj, you cannot remove it (just this colorObj and the footer)
                    removeColor.setVisibility(View.INVISIBLE);
                } else {
                    // If there are other objects, it is possible to remove this one
                    removeColor.setVisibility(View.VISIBLE);
                }

                // 6. Set the number of this color
                colorNumText.setText(Integer.toString(position + 1));
            }
        }

        private void setIsOnly() {
            // Make the both arrows invisible
            moveColorUp.setVisibility(View.GONE);
            moveColorDown.setVisibility(View.GONE);
        }

        private void setIsBottom() {
            // Make the down arrow invisible
            moveColorUp.setVisibility(View.VISIBLE);
            moveColorDown.setVisibility(View.GONE);
        }

        private void setIsTop() {
            // Make the down arrow invisible
            moveColorUp.setVisibility(View.GONE);
            moveColorDown.setVisibility(View.VISIBLE);
        }

        private void setIsMid() {
            // Make both arrows visible
            moveColorUp.setVisibility(View.VISIBLE);
            moveColorDown.setVisibility(View.VISIBLE);
        }

        private String getTTypeString(int tTypeConstantVal) {
            // Converts Color_ type to the style of T that it accepts
            switch (tTypeConstantVal) {
                case Constants.COLOR_DTIME:
                    return layoutView.getContext().getResources().getString(R.string.color_quality_time);
                case Constants.COLOR_DSPEED:
                    return layoutView.getContext().getResources().getString(R.string.color_quality_speed);
                default:
                    // Uh oh.
                    return layoutView.getContext().getResources().getString(R.string.color_quality_time);
            }
        }

        private String getBlendTypeString(Color_d.BLEND_TYPE blendTypeConstantVal) {
            switch (blendTypeConstantVal) {
                case CONSTANT:
                    return layoutView.getContext().getResources().getString(R.string.Blend_Constant);
                case LINEAR:
                    return layoutView.getContext().getResources().getString(R.string.Blend_Linear);
                default:
                    return layoutView.getContext().getResources().getString(R.string.Blend_Constant);
            }
        }
    }

    public class Color_FooterViewHolder extends bindViewHolder {
        private ImageView addColorObjView;

        @Override
        public void bind_updateColorNum(int position) {
        } // Do nothing

        public Color_FooterViewHolder(View layoutView) {
            super(layoutView);

            // Find the ImageView that will respond to user clicks
            addColorObjView = layoutView.findViewById(R.id.addColorObj);

            // Assign a click listener so the user can add a new colorObj
            addColorObjView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Add a new colorObj to the Color_D
                    ((Color_d) color_Choice_Current).addNewColorObj();

                    // Notify the adapter that the underlying data set has changed
                    // Let the adapter know something has changed
                    notifyItemRangeChanged(color_Choice_Current.getNumColors() - 2, 1); // Notify the color that was (until just a moment ago) the last (lowest in the GUI) color, to let it know that it is no longer the last color
                    notifyChangeColor_();
                }
            });
        }

        @Override
        public void bind(int position) {
            // Do nothing
        }
    }

    private void notifyChangeColor_() {
        // Notify the adapter via DiffUtil that a change has occurred
//        Color_ a = color_Memory;
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new Color_DiffUtil(color_Memory, color_Choice_Current));
        result.dispatchUpdatesTo(thisAdapter);

        // Update the ColorViewMain animation of the current color
        if (colorViewMain != null) {
            colorViewAnimSet = color_Choice_Current.modColor_Animator(colorViewMain, colorViewAnimSet, "BackgroundColor");
        }

        // Finally, record the most recent change to color_Choice_Current in the memory variable
        color_Memory = color_Choice_Current.clone();
    }

    private class Color_DiffUtil extends DiffUtil.Callback {
        private Color_ thisMemoryColor_;
        private Color_ thisColor_Choice_Current;

        public Color_DiffUtil(Color_ newMemoryColor_, Color_ newcolor_Choice_Current) {
            thisMemoryColor_ = newMemoryColor_;
            thisColor_Choice_Current = newcolor_Choice_Current;
        }

        @Override
        public int getNewListSize() {
            return getColorSize(thisColor_Choice_Current);
        }

        @Override
        public int getOldListSize() {
            return getColorSize(thisMemoryColor_);
        }

        private int getColorSize(Color_ color) {
            return color.getNumColors() + ((color instanceof Color_Static) ? 0 : 1);
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            if (thisColor_Choice_Current instanceof Color_Static) {
                if (thisMemoryColor_ instanceof Color_Static) {
                    // If both Color_'s are Color_Statics, then there is only one item!
                    return true;
                } else {
                    // They do not match
                    return false;
                }
            } else {
                if (thisMemoryColor_ instanceof Color_Static) {
                    // They do not match
                    return false;
                } else {
                    // If both Color_'s are dynamic, then there may be multiple colors
                    // First, check if either of the items is the footer
                    if (oldItemPosition == thisMemoryColor_.getNumColors() || newItemPosition == thisColor_Choice_Current.getNumColors()) {
                        // If both are footers, then they are equivalent.  Otherwise, only one is a footer and they are not equivalent
                        return (oldItemPosition == thisMemoryColor_.getNumColors() && newItemPosition == thisColor_Choice_Current.getNumColors());
                    } else {
                        // Now, check if the colors are equivalent
                        if (thisColor_Choice_Current.getClass() != thisMemoryColor_.getClass()) {
                            // If the two Color_'s are of different types, then they must be different
                            return false;
                        } else {
                            // Lastly, check if the ID's are the same
                            return ((Color_d) thisMemoryColor_).getColorObjMeta(oldItemPosition).getID() == ((Color_d) thisColor_Choice_Current).getColorObjMeta(newItemPosition).getID();
                        }
                    }
                }
            }
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            if (thisColor_Choice_Current instanceof Color_Static) {
                if (thisMemoryColor_ instanceof Color_Static) {
                    // If the Color_ is a Color_Static, then compare only the actual colorObj's
                    return thisMemoryColor_.getColorObj(oldItemPosition).equals(thisColor_Choice_Current.getColorObj(newItemPosition));
                } else {
                    // They do not match
                    return false;
                }
            } else {
                if (thisMemoryColor_ instanceof Color_Static) {
                    // They do not match
                    return false;
                } else {
                    // If both Color_'s are dynamic, then compare only the actual colorObj's
                    // First, check if either of the items is the footer
                    if (oldItemPosition == thisMemoryColor_.getNumColors() || newItemPosition == thisColor_Choice_Current.getNumColors()) {
                        // If both are footers, then they are equivalent.  Otherwise, only one is a footer and they are not equivalent
                        return (oldItemPosition == thisMemoryColor_.getNumColors() && newItemPosition == thisColor_Choice_Current.getNumColors());
                    } else {
                        // Now, check if the colors are equivalent
                        if (thisColor_Choice_Current.getClass() != thisMemoryColor_.getClass()) {
                            // If the two Color_'s are of different types, then they must be different
                            return false;
                        } else {
                            // Lastly, check if the ID's are the same
                            return ((Color_d) thisMemoryColor_).getColorObjMeta(oldItemPosition).equals(((Color_d) thisColor_Choice_Current).getColorObjMeta(newItemPosition));
                        }
                    }
                }
            }
        }
    }


    @NonNull
    @Override
    public bindViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Determine which layout type will be used as this Color_ViewHolder, depending on whether or not Static or Dynamic colors are being described
        int color_Layout = colorObjSView_r;
        switch (viewType) {
            case VIEWTYPE_COLOROBJ_S:
                // If this is a static color colorObj
                color_Layout = colorObjSView_r;
                break;
            case VIEWTYPE_COLOROBJ_D:
                // If this is a dynamic color colorObj
                color_Layout = colorObjDView_r;
                break;
            case VIEWTYPE_FOOTER:
                // If this view refers to the footer at the bottom of the list (of a dynamic Color_)
                color_Layout = colorObjFooterView_r;
                break;
        }

        View layoutView = inflater.inflate(color_Layout, parent, false);

        if (viewType == VIEWTYPE_FOOTER) {
            return new Color_FooterViewHolder(layoutView);
        } else {
            return new Color_ViewHolder(layoutView, viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull bindViewHolder holder, int position) {
        holder.bind(position); // If this is the footer, it will simply be ignored
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull bindViewHolder holder) {
        // TODO: Implement this to remove any onClickListeners
        super.onViewDetachedFromWindow(holder);
    }

//    @Override
//    public void onBindViewHolder(@NonNull bindViewHolder holder, int position, @NonNull List<Object> payloads) {
//        if (!payloads.isEmpty()) {
//            for (Object payload : payloads) {
//                if (payload instanceof Integer) {
//                    if (((Integer) payload).intValue() == PAYLOAD_UPDATE_POSITION) {
//                        // Update only the position
//                        holder.bind_updateColorNum(position);
//                    }
//                }
//            }
//        } else {
//            super.onBindViewHolder(holder, position, payloads);
//        }
//
//    }

    @Override
    public int getItemViewType(int position) {
        if (position == color_Choice_Current.getNumColors())
            return VIEWTYPE_FOOTER; // If the position refers to the last in the RecyclerView (position equal to the number of colors, where position starts at 0), then this is a footer view
        else {
            if (color_Choice_Current.getColorType() == Constants.COLOR_STATIC) {
                // If this is a static color colorObj
                return VIEWTYPE_COLOROBJ_S;
            } else {
                // If this is a dynamic color colorObj
                return VIEWTYPE_COLOROBJ_D;
            }
        }
    }

    @Override
    public int getItemCount() {
        // If using Static, then no footer is needed
        // If not using Static, add an additional item because of the footer (add colorObj view)
        return color_Choice_Current.getNumColors() + ((color_Choice_Current.getColorType() != Constants.COLOR_STATIC) ? 1 : 0);
        // If adding extra Color_ types later, some of which are not multi-colorObj:
        // Add a new member boolean, "isMulti", and set this individually for each color in setColorType)
    }

    public void setColor_(Color_ newColor) {
        // Find out what kind of Color_ this is
        int newColorType = newColor.getColorType();

        // Save the new Color_ to the corresponding color_Choice_X according to its type
        switch (newColorType) {
            case Constants.COLOR_STATIC:
                color_Choice_Static = newColor;
                break;
            case Constants.COLOR_DTIME:
                color_Choice_Time = newColor;
                break;
            case Constants.COLOR_DSPEED:
                color_Choice_Vel = newColor;
                break;
            default:
                // Uh oh
                Log.e(TAG, "Attempting to store unrecognized Color_");
        }

        // Set the color type of the adapter to this new color's type
        setColorType(newColorType, false);
    }

    public void setColorType(int newColorType) {
        setColorType(newColorType, true);
    }

    public void setColorType(int newColorType, boolean saveColor) {
        // Switch over the color_Choice_* Color_'s based on the old and new choices for Color_ type (system in place to prevent data loss when switching between color types)

        // Save the old color choice to its respective variable (if it should be saved)
        if (saveColor) {
            switch (color_Choice_Current.getColorType()) {
                case Constants.COLOR_STATIC:
                    color_Choice_Static = color_Choice_Current;
                    break;
                case Constants.COLOR_DTIME:
                    color_Choice_Time = color_Choice_Current;
                    break;
                case Constants.COLOR_DSPEED:
                    color_Choice_Vel = color_Choice_Current;
                    break;
                default:
                    // Uh oh
                    Log.e(TAG, "Attempting to store unrecognized Color_");
            }
        }

        // Recall one of the older Color_'s to use now
        switch (newColorType) {
            case Constants.COLOR_STATIC:
                color_Choice_Current = color_Choice_Static;
                break;
            case Constants.COLOR_DTIME:
                color_Choice_Current = color_Choice_Time;
                break;
            case Constants.COLOR_DSPEED:
                color_Choice_Current = color_Choice_Vel;
                break;
            default:
                // Uh oh
                Log.e(TAG, "Attempting to retrieve unrecognized Color_");
        }

//         Save the new color_ choice, so that the RecyclerView will reflect the new Color_ type's view formats
//        curColor_Type = newColorType;

        // Notify the adapter that the underlying data (may have) changed, so the view may need to be updated
        notifyChangeColor_();
    }

    public Color_ getColor_() {
        // Get the current most up-to-date version of the Color_ being actively worked on.
        return color_Choice_Current;
    }

//    public void clearUnusedColorPickerSettings(Bike_Wheel_Animation bikeWheelAnimation) {
//        // TODO: This function will accept a bikeWheelAnimation and clear all ColorPickerSettings from this wheel that are not associated with it (Abandoned, because it's just a mess of trying to get the wheel location down to this level, and it's not really a big deal...)
//
//    }

    private String getPrefName(Color_ thisColor, int thisPosition) {
        if (thisColor.getColorType() == Constants.COLOR_STATIC) {
            return "T" + Integer.toString(thisColor.getColorType());
        } else {
            return "T" + Integer.toString(thisColor.getColorType()) + "P" + Integer.toString(((Color_d) thisColor).getColorObjMeta(thisPosition).getID());
        }
    }

    public void cancelAnim() {
        colorViewAnimSet.cancel();
    }
}

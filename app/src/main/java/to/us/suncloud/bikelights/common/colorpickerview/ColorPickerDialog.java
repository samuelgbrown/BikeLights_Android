
/*
 * Copyright (C) 2018 skydoves
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package to.us.suncloud.bikelights.common.colorpickerview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableRow;

import java.util.ArrayList;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.LocalPersistence;
import to.us.suncloud.bikelights.common.colorpickerview.flag.FlagView;
import to.us.suncloud.bikelights.common.colorpickerview.listeners.ColorEnvelopeListener;
import to.us.suncloud.bikelights.common.colorpickerview.listeners.ColorListener;
import to.us.suncloud.bikelights.common.colorpickerview.listeners.ColorPickerViewListener;
import to.us.suncloud.bikelights.common.colorpickerview.sliders.AlphaSlideBar;
import to.us.suncloud.bikelights.common.colorpickerview.sliders.BrightnessSlideBar;

@SuppressWarnings({"WeakerAccess", "unchecked", "unused"})
public class ColorPickerDialog extends AlertDialog {
    public final static int numColorBoxesPerRow = 8;
    public final static int numColorBoxRows = 2;
    public final static String SAVED_COLORS = "SAVED_COLORS";

    private ColorPickerView colorPickerView;

    public ColorPickerDialog(Context context) {
        super(context);
        initColorPickerView();
    }

    protected ColorPickerDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        initColorPickerView();
    }

    protected ColorPickerDialog(Context context, int themeResId) {
        super(context, themeResId);
        initColorPickerView();
    }

    private void initColorPickerView() {
        LayoutInflater layoutInflater = this.getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.layout_dialog_colorpicker, null);
        this.colorPickerView = view.findViewById(R.id.ColorPickerView);
        super.setView(view);
    }

    public void setFlagView(FlagView flagView) {
        this.colorPickerView.setFlagView(flagView);
    }

    public void setOnColorListener(ColorListener colorListener) {
        this.colorPickerView.setColorListener(colorListener);
    }

    public static class Builder extends AlertDialog.Builder {
        private ColorPickerView colorPickerView;
        private ArrayList<View> colorBoxViews;
        //        private TableRow tableRow1;
//        private TableRow tableRow2;
        private View view;

        private Context context;

        public Builder(Context context) {
            super(context);
            this.context = context;
            initColorPickerView();
        }

        public Builder(Context context, int themeResId) {
            super(context, themeResId);
            this.context = context;
            initColorPickerView();
        }

        private void initColorPickerView() {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (view == null) {
                this.view = layoutInflater.inflate(R.layout.layout_dialog_colorpicker, null);
            }
            this.colorPickerView = view.findViewById(R.id.ColorPickerView);
            this.colorPickerView.setColorListener(new ColorListener() {
                @Override
                public void onColorSelected(int color, boolean fromUser) {
                    // nothing
                }
            });

            // Added by Sam Brown
            // Get both table rows
            TableRow tableRow1 = view.findViewById(R.id.table_row_1);

            TableRow tableRow2 = view.findViewById(R.id.table_row_2);

            // Load the savedColorsList array from file
//            SharedPreferences prefs = view.getContext().getSharedPreferences(SHARED_PREF_FILE, 0);
            ArrayList<SavedColor> savedColorsList = (ArrayList<SavedColor>) LocalPersistence.readObjectFromFile(view.getContext(), SAVED_COLORS);

            boolean hasSavedColors = false;
            if (savedColorsList != null) {
                hasSavedColors = true;
            }

            // Create an onClickListener to attach to all colorBoxSamples
            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setColorFromSample((SavedColor) v.getTag());
                }
            };

            // For each table row, inflate some number of color boxes
            colorBoxViews = new ArrayList<>();
            for (int i = 0; i < numColorBoxesPerRow * 2; i++) {
                TableRow thisTableRow;
                if (i < numColorBoxesPerRow) {
                    thisTableRow = tableRow1;
                } else {
                    thisTableRow = tableRow2;
                }

                // Create the color box
                View thisColorBox = layoutInflater.inflate(R.layout.color_sample_box, thisTableRow, false);
                thisTableRow.addView(thisColorBox);

                // See if there is a preview color to populate the box with, and add a listener iff it exists
                if (hasSavedColors && (savedColorsList.size() > i)) {
                    SavedColor thisColor = savedColorsList.get(i);

                    // Set the color of the box to this color
                    ImageView colorBoxPreview = thisColorBox.findViewById(R.id.color_box);
                    colorBoxPreview.setBackgroundColor(thisColor.getColor());
//                    colorBoxPreview.setBackgroundColor(colorIntFromSavedColor(thisColor));

                    // Add the saved color object as a tag
                    thisColorBox.setTag(thisColor);

                    // If there is a saved color, add the onClickListener
                    thisColorBox.setOnClickListener(clickListener);
                }

                // Add this colorBox to the array that stores all of them
                colorBoxViews.add(thisColorBox);
            }


            super.setView(view);
        }

//        private int colorIntFromSavedColor(SavedColor savedColor) {
//            // Get the actual color by looking it up on the ColorPickerView bitmap and applying the saved brightness value
//            float[] hsv = new float[3]; // HSV array to hold the color
////            Color.colorToHSV(colorPickerView.getColorFromBitmap(savedColor.getX(), savedColor.getY()), hsv); // Assign the saved color to the hsv array for easy brightness modification
//            int thisColorInt = savedColor.getColor();
//            Color.colorToHSV(thisColorInt, hsv); // Assign the saved color to the hsv array for easy brightness modification
//            hsv[2] = savedColor.getBrightness(); // Set the brightness of the color
//            return Color.HSVToColor(hsv);
//        }

        private void setColorFromSample(SavedColor savedColor) {
            // Set the colorPickerView to the color given by savedColor
            getColorPickerView().setPureColor(savedColor.getColor());
            Point thisPoint = new Point(savedColor.getX(), savedColor.getY());
            getColorPickerView().setCoordinate(thisPoint.x, thisPoint.y);
            getColorPickerView().setSelectorPoint(thisPoint.x, thisPoint.y);
            getColorPickerView().getBrightnessSlider().updateSelectorX(savedColor.getBrightness());
//            getColorPickerView().fireColorListener(savedColor.getColor(), true);
//            getColorPickerView().notifyToSlideBars();
        }

        public void setFlagView(FlagView flagView) {
            this.colorPickerView.setFlagView(flagView);
        }

        public void setOnColorListener(ColorListener colorListener) {
            this.colorPickerView.setColorListener(colorListener);
        }

        public ColorPickerView getColorPickerView() {
            return this.colorPickerView;
        }

        public void attachAlphaSlideBar() {
            AlphaSlideBar alphaSlideBar = view.findViewById(R.id.AlphaSlideBar);
            colorPickerView.attachAlphaSlider(alphaSlideBar);
            alphaSlideBar.setVisibility(View.VISIBLE);
        }

        public void attachBrightnessSlideBar() {
            BrightnessSlideBar brightnessSlideBar = view.findViewById(R.id.BrightnessSlideBar);
            colorPickerView.attachBrightnessSlider(brightnessSlideBar);
            brightnessSlideBar.setVisibility(View.VISIBLE);
        }

        @Override
        public AlertDialog.Builder setPositiveButton(int textId, OnClickListener listener) {
            return super.setPositiveButton(textId, listener);
        }

        @SuppressWarnings("UnusedReturnValue")
        public AlertDialog.Builder setPositiveButton(CharSequence text, final ColorPickerViewListener colorListener) {
            OnClickListener onClickListener = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (colorListener instanceof ColorListener) {
                        ((ColorListener) colorListener).onColorSelected(colorPickerView.getColor(), true);
                    } else if (colorListener instanceof ColorEnvelopeListener) {
                        ((ColorEnvelopeListener) colorListener).onColorSelected(colorPickerView.getColorEnvelope(), true);
                    }
                }
            };

            return super.setPositiveButton(text, onClickListener);
        }

        public AlertDialog.Builder setNeutralButton(CharSequence text, final ColorPickerViewListener colorListener) {
            OnClickListener onClickListener = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (colorListener instanceof ColorListener) {
                        ((ColorListener) colorListener).onColorSelected(colorPickerView.getColor(), true);
                    } else if (colorListener instanceof ColorEnvelopeListener) {
                        ((ColorEnvelopeListener) colorListener).onColorSelected(colorPickerView.getColorEnvelope(), true);
                    }
                }
            };

            return super.setNeutralButton(text, onClickListener);
        }
    }

    /**
     * disable set overrides
     */
    @Override
    public void setContentView(int layoutResID) {
    }

    @Override
    public void setContentView(@NonNull View view) {
    }

    @Override
    public void setContentView(@NonNull View view, ViewGroup.LayoutParams params) {
    }

    @Override
    public void addContentView(@NonNull View view, ViewGroup.LayoutParams params) {
    }

    @Override
    public void setView(View view) {
    }

    @Override
    public void setView(View view, int viewSpacingLeft, int viewSpacingTop, int viewSpacingRight, int viewSpacingBottom) {
    }
}

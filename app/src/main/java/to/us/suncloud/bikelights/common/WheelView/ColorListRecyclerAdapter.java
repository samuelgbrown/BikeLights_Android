package to.us.suncloud.bikelights.common.WheelView;

import android.animation.AnimatorSet;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Color.Color_;
import to.us.suncloud.bikelights.common.ObservedRecyclerView;

public class ColorListRecyclerAdapter extends ObservedRecyclerView.Adapter<ColorListRecyclerAdapter.bindViewHolder> {
    private static String SEL = "SEL";

    private static final int FOOTER = 0;
    private static final int MAIN = 1;

//    private Bike_Wheel_Animation colorList;
    private ArrayList<Color_> colorList;
    private int selectedItem = -1; // The index of the item that is currently selected

    private ColorListInterface listener;
//    private LEDViewDrawable ledViewDrawable; // Drawable that is assigned to the WheelActivity main listener

    public ColorListRecyclerAdapter(ArrayList<Color_> colorList, ColorListInterface listener) { //, LEDViewDrawable ledViewDrawable) {
        this.colorList = new ArrayList<>(colorList);
        this.listener = listener;
//        this.ledViewDrawable = ledViewDrawable;
//
//        // Initialize the LEDViewDrawable with the Bike_Wheel_Animation
//        ledViewDrawable.setPalette(palette);
    }

    abstract class bindViewHolder extends ObservedRecyclerView.ViewHolder {
        bindViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class Color_ViewHolder extends bindViewHolder {
        Color_ mColor_; // The Color_ that is associated with this Color_ViewHolder
        int mColorInd; // The index in palette that this Color_ViewHolder currently represents

        // Views associated with this Color_ViewHolder
        ImageView mSettingsView;
        ImageView mPreviewView;
        ImageView mRemoveView;
        TextView mNameView;
        TextView mDescriptionView;
        ConstraintLayout mBackground;

        AnimatorSet previewAnimSet = new AnimatorSet();

        Color_ViewHolder(View itemView) {
            super(itemView);

            // Extract Views from the layout
            mSettingsView = itemView.findViewById(R.id.Color_Settings);
            mPreviewView = itemView.findViewById(R.id.Color_Preview);
            mRemoveView = itemView.findViewById(R.id.Color_Remove);
            mNameView = itemView.findViewById(R.id.Color_Name);
            mDescriptionView = itemView.findViewById(R.id.Color_Description);
            mBackground = itemView.findViewById(R.id.Color_Background);

            // Create an OnClick listener for the name, description, and background to highlight this Color_
            View.OnClickListener highlightListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Indicate a highlight in this Color_ViewHolder
                    // Store the previous VH that was selected, and getP this VH's layout position
                    int oldSelection = selectedItem;
                    int thisItem = getLayoutPosition();

                    if (oldSelection == thisItem) {
                        // If this item is currently selected, the user wants to deselect this item
                        selectedItem = -1; // Select nothing
                    } else {
                        // If something else (or nothing) is currently selected, the user wants to select this item
                        selectedItem = thisItem; // Select this item
                        if (oldSelection != -1) {
                            // If another item is currently selected
                            notifyItemChanged(oldSelection, SEL); // Change the old item's background
                        }
                    }

                    // Update this item's background
                    notifyItemChanged(thisItem, SEL);

                    // Indicate the correct LED's in the wheel
                    // TODO: Send this selection to the Activity, who will send it to the ImageDefineFragment
//                    ledViewDrawable.setSelection(selectedItem);
                    listener.setSelected(selectedItem);
                }
            };

            // Assign highlight listener to the name, description, and background
            mNameView.setOnClickListener(highlightListener);
            mDescriptionView.setOnClickListener(highlightListener);
            mBackground.setOnClickListener(highlightListener);

            // Add an OnClickListener to the Settings View
            View.OnClickListener modColorListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ModColorFragment dialog = ModColorFragment.newInstance(mColor_, getLayoutPosition());
                    dialog.show(((AppCompatActivity) mBackground.getContext()).getSupportFragmentManager(), "Modify Color");
                }
            };

            mSettingsView.setOnClickListener(modColorListener);

            mRemoveView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (colorList.size() > 0) {
                        // If this is not the only Color_ left (this should not be visible if it is the only one left), then ask the user if they're sure that they want to delete this color
                        new AlertDialog.Builder(mRemoveView.getContext())
                                .setMessage(mRemoveView.getResources().getString(R.string.dialog_deleted_Color_))
                                .setPositiveButton(mRemoveView.getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeColor(getLayoutPosition()); // Remove this color from the colorList
                                    }
                                })
                                .setNegativeButton(mRemoveView.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Do nothing
                                    }
                                })
                                .show();
                    }
                }
            });
        }

        void bind(int position) {
            // Save the position
            mColorInd = position;

            // Set the color from the palette
            setColor_(colorList.get(mColorInd).clone());

            // Determine whether or not this Color_ViewHolder should be highlighted
            itemView.setSelected(getLayoutPosition() == selectedItem); // Set whether or not this is selected
        }

//        @Override
//        public void receiveModifiedColor_(Color_ newColor) {
//            // When the ModColorFragment has been dismissed, it will send the final Color_ to this Color_ViewHolder through this method.
//
//            // Update this Color_ViewHolder with the new color information
//            setColor_(newColor);
//
//            // Update the palette, and let the adapter know something changed
//            palette.set(mColorInd, newColor);
//
//            updateData();
////            notifyItemChanged(mColorInd); // TODO: Maybe this isn't needed?
//        }

        public void setColor_(Color_ color_New) {
            // Use this new color to fill in the viewholder
            mColor_ = color_New;

            String colorName = mColor_.getName();

            if (colorName.isEmpty()) {
                mNameView.setText(String.format(Locale.US, "Color %d",getLayoutPosition()));
            } else {
                mNameView.setText(colorName);
            }

            mDescriptionView.setText(mColor_.getDescription());

            if (colorList.size() > 1) {
                mRemoveView.setVisibility(View.VISIBLE);
            } else {
                mRemoveView.setVisibility(View.GONE);
            }

            // Animate mPreview
            previewAnimSet = mColor_.modColor_Animator(mPreviewView, previewAnimSet, "BackgroundColor");
        }
    }

    public class Footer_ViewHolder extends bindViewHolder {
        Footer_ViewHolder(final View itemView) {
            super(itemView);

            // Find the ImageView that will respond to user clicks
            ImageView addColorObjView = itemView.findViewById(R.id.addColorObj);

            // Assign a click listener so the user can add a new colorObj
            addColorObjView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Call the ModColorFragment fragment, without supplying a color, to generate a brand new color
                    ModColorFragment dialog = ModColorFragment.newInstance();
                    dialog.show(((AppCompatActivity) itemView.getContext()).getSupportFragmentManager(), "Modify Color");
                }
            });
        }

//        @Override
//        public void receiveModifiedColor_(Color_ newColor) {
//            // Update the palette, and let the adapter know something changed
//            palette.add(newColor);
//
//            updateData();
//        }
    }

    private void removeColor(int position) {
        if (colorList.size() > 0 && colorList.size() > position) {
            // If this is not the last color in the list, and the position is not beyond the list

            // Remove the Color_ from the colorList, and update the GUI
            int originalSize = colorList.size();
            colorList.remove(position);
            notifyItemRemoved(position);
            if (colorList.size() == 1 || originalSize == 1) {
                notifyItemChanged(0); // If there was or now is only one item, then the visibility of the remove button must change on the first color
            }

            // Let the Images Fragment(s) know that a color was removed, as well
            listener.removeColorFromPalette(position);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull bindViewHolder holder, int position) {
        try {
            if (holder instanceof Color_ViewHolder) {
                Color_ViewHolder vh = (Color_ViewHolder) holder;

                vh.bind(position);
            }
            // TODO: Probably don't need to do anything if it's a footer...right?
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull bindViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);

        try {
            if (holder instanceof Color_ViewHolder) {
                if (!payloads.isEmpty()) {
                    // If the payload is not empty
                    for (int i = 0; i < payloads.size(); i++) {
                        // Loop through it
                        if (payloads.get(i).equals(SEL)) {
                            // If any of the payloads is the string SEL, then it indicates that this holder's selection status has changed
                            holder.itemView.setSelected(position == selectedItem); // Set whether or not this is selected (See?  No need to invoke holder.setColor_ !)
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateData() {
        // The palette has been updated, so update the drawable
//        ledViewDrawable.setPalette(colorList);
        listener.setPalette(new ArrayList<>(colorList));

        notifyDataSetChanged(); // TODO: A very broad stroke, but maybe needed?
    }

    @Override
    public int getItemCount() {
        return colorList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == colorList.size()) {
            // Indicate the footer
            return FOOTER;
        } else {
            return MAIN;
        }
    }

    @NonNull
    @Override
    public bindViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == MAIN) {
            // Inflate the Color_ViewHolder from xml
            View layoutView = inflater.inflate(R.layout.wheelview_viewholder, parent, false);

            // Set the background drawable of the itemView
            layoutView.setBackground(parent.getContext().getDrawable(R.drawable.highlight_drawable));

            return new Color_ViewHolder(layoutView);
        } else {
            // Create the footer
            View footerView = inflater.inflate(R.layout.color_footer_view, parent, false);

            return new Footer_ViewHolder(footerView);
        }
    }

    public ArrayList<Color_> getColorList() {
        return colorList;
    }

    public void setColorList(ArrayList<Color_> colorList) {
        this.colorList = colorList;
        updateData();
    }

    public void setModColorResults(Color_ newColor, int colorPosition) {
        // A Color_ has been either modified or created by a call to ModColorFragment, and WheelActivity has passed the result on to this function.  Place the result into the Bike_Wheel_Animation
        if (colorPosition == -1) {
            // If this is a new color, then simply add it onto the Bike_Wheel_Animation
            colorList.add(newColor);
        } else {
            // If this is a modification to an existing color, replace the old color
            colorList.set(colorPosition, newColor);
        }

        // After the Bike_Wheel_Animation has been modified, update the data
        updateData();
    }

    public interface ColorListInterface {
        void setSelected(int selected);
        void setPalette(ArrayList<Color_> newPalette);
        void removeColorFromPalette(int colorIndToRemove);
    }
}

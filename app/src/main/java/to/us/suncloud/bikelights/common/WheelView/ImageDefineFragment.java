package to.us.suncloud.bikelights.common.WheelView;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Color.Color_;
import to.us.suncloud.bikelights.common.Color.Bike_Wheel_Animation;
import to.us.suncloud.bikelights.common.Constants;
import to.us.suncloud.bikelights.common.Image.ImageMeta_;
import to.us.suncloud.bikelights.common.Image.Image_Meta_ConstRot_;
import to.us.suncloud.bikelights.common.Image.Image_Meta_ConstRot_GRel;
import to.us.suncloud.bikelights.common.Image.Image_Meta_ConstRot_WRel;
import to.us.suncloud.bikelights.common.Image.Image_Meta_Spinner;

public class ImageDefineFragment extends Fragment implements ImageModFragment.ImageModListener {
    private final static String TAG = "ImageDefineFragment";

    public static final String IS_IDLE = "IS_IDLE";
    public static final String BIKE_WHEEL_ANIMATION = "BIKE_WHEEL_ANIMATION";
    public static final String IMAGE_STACK = "IMAGE_STACK";
    public static final String IMAGE_LOC = "IMAGE_LOC";
//    public static final String LED_DRAWABLE = "LED_DRAWABLE";
//    public static final String TEST_ID = "TEST_ID";
//    public static final String ROOT_VIEW = "ROOT_VIEW";

    ImageMetaChangeInterface parentActivity;

    private View rootView; // The root view of this Fragment
    private ImageStack imageStack; // The stack that represents all images that have been set up until now (allows undoing and redoing)
    ArrayList<Color_> palette; // A list of colors that the ledViewDrawable can use
    boolean isIdle; // Is this an idle image?
    int thisNumLEDs; // The number of LEDs on the wheel

    private SpinnerSelectListener imageModSpinner; // The spinner that allows a user to paint colors onto the wheel
    private ImageView imageModUndo; // The undo button for modifying the wheel
    private ImageView imageModRedo; // The undo button for modifying the wheel

    private class ImageStack {
        private ArrayList<ArrayList<Integer>> imagesList = new ArrayList<>(); // This stack represents the current state of the Image undo-redo buffer for h
        private int curStackLocation = 0; // The location of the current Image

//        private LEDViewDrawable ledViewDrawable;
//        private View imageModUndo;
//        private View imageModRedo;

        //        public ImageStack() {}
        ImageStack(ArrayList<Integer> image) {
            save(image);
        }

        ImageStack() {
        }

        ;

        ImageStack(ImageStack old) {
            this.imagesList = old.getImagesList();
            this.curStackLocation = old.getCurStackLocation();
        }

//        public void setViews(LEDViewDrawable thisDrawable, View imageModUndo, View imageModRedo) {
//            this.ledViewDrawable = thisDrawable;
//            this.imageModUndo = imageModUndo;
//            this.imageModRedo = imageModRedo;
//        }

        void undo() {
            // Undo the previous action, and return the last Image
//            ArrayList<Integer> imageToReturn;
            if (canUndo()) {
                curStackLocation--;
            }

//            imageToReturn = new ArrayList<>(imagesList.get(curStackLocation));

            // Update the GUI
            updateImageGUI();

            // Return the image
//            return currentImage();
        }

        void redo() {
            // Redo the last action, and return the new Image
//            ArrayList<Integer> imageToReturn;
            if (canRedo()) {
                curStackLocation++;
            }

//            imageToReturn = new ArrayList<>(imagesList.get(curStackLocation));

            // Update the GUI
            updateImageGUI();

            // Return the image
//            return currentImage();
        }

        public void save(ArrayList<Integer> newImage) {
            // Add an Image to the stack

            // First, check if we are up to date
            if (canRedo()) {
                // If we can redo, that means that there are some actions that were previously undone, which will now be deleted
//                List<ArrayList<Integer>> imagesToRemove = imagesList.subList(curStackLocation + 1, imagesList.size());
//                imagesList.removeAll(imagesToRemove);
                int indToRemove = curStackLocation + 1;
                int numRemovals = imagesList.size() - indToRemove;
                for (int i = 0; i < numRemovals; i++) {
                    imagesList.remove(indToRemove); // Remove the indToRemove'th image, numRemovals number of times (the image stack will "move backwards" from the end towards indToRemove, until it's depleted)
                }
            }

            // Now, add the newest Image and iterate curStackLocation
            imagesList.add(newImage);
            curStackLocation = imagesList.size() - 1;

            // Update the GUI
            updateImageGUI();
        }

        ArrayList<Integer> currentImage() {
            return new ArrayList<>(imagesList.get(curStackLocation));
        }

        private boolean canUndo() {
            // Is the ImageStack able to undo an action?
            return curStackLocation > 0;
        }

        private boolean canRedo() {
            // Is the ImageStack able to redo an action?
            return curStackLocation < (imagesList.size() - 1);
        }

        private void updateImageGUI() {
            if (imageModRedo != null) {
                if (canRedo()) {
                    imageModRedo.setVisibility(View.VISIBLE);
                } else {
                    imageModRedo.setVisibility(View.INVISIBLE);
                }
            }

            if (imageModUndo != null) {
                if (canUndo()) {
                    imageModUndo.setVisibility(View.VISIBLE);
                } else {
                    imageModUndo.setVisibility(View.INVISIBLE);
                }
            }

            if (ledViewDrawable != null) {
                ledViewDrawable.setImage(currentImage());
            }
        }

        void eraseBuffer(ArrayList<Integer> replacementImage) {
            // If an action has been done that can't be undone (such as modifying the palette), erase the ImageStack buffer.
            imagesList.clear();
            imagesList.add(replacementImage);
            curStackLocation = 0;

            updateImageGUI();
        }

        public ArrayList<ArrayList<Integer>> getImagesList() {
            return imagesList;
        }

        public int getCurStackLocation() {
            return curStackLocation;
        }

        public void setImagesList(ArrayList<ArrayList<Integer>> imagesList) {
            this.imagesList = imagesList;
        }

        public void setCurStackLocation(int curStackLocation) {
            this.curStackLocation = curStackLocation;
        }
    }

    Spinner imageMetaSpinner; // Spinner to choose the type of Image meta data to store (rotation or inertia [for spinner]?
    ImageView wheelView; // The wheel view of this image
    TextView attrText; // The textview that shows the name of the current attribute
    SeekBar attrSeekbar; // The seekbar that modifies the attribute
    ImageView attrLeft; // Button to modify the attribute down
    ImageView attrRight; // Button to modify the attribute up
    TextView attrVal; // The textview that shows the value of the current attribute
    TextView idTest; // The textview that shows the value of the current attribute
    View attrContainer; // The container for the attribute modification widgets
    int curAttrVal;  // The current value of the attribute

    int curAttrType; // The type of the current attribute
    int curAttrMax; // The maximum value of the attribute
    int curAttrMin; // The minimum value of the attribute
    int curSeekbarOffset; // The offset of the seekbar widget to convert its value to the current attribute

    ArrayList<Integer> storedAttrVals = new ArrayList<>(Arrays.asList(0, 10));

    private LEDViewDrawable ledViewDrawable; // Drawable that draws the main wheel view
//    ImageDefineInterface listener; // A listener registered with the Fragment that can provide a Bike_Wheel_Animation palette

    ObjectAnimator rotationAnimator; // Animator for the LEDViewDrawable's rotation speed

//    @Override
//    public void setArguments(Bundle args) {
//        super.setArguments(args);
//    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public static ImageDefineFragment newInstance(PagerAdapter parentAdapter, Bike_Wheel_Animation bikeWheelAnimation, boolean isIdle) {

        Bundle args = new Bundle();
        args.putSerializable(BIKE_WHEEL_ANIMATION, bikeWheelAnimation);
        args.putBoolean(IS_IDLE, isIdle);

        ImageDefineFragment fragment = new ImageDefineFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(IMAGE_STACK, imageStack.getImagesList());
        outState.putInt(IMAGE_LOC, imageStack.getCurStackLocation());
//        outState.putInt(TEST_ID, thisTestID);
//        outState.putSerializable(LED_DRAWABLE, ledViewDrawable);
//        outState.putParcelable(ROOT_VIEW, rootView);
        super.onSaveInstanceState(outState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the view
//        if (savedInstanceState != null) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_image_define, container, false);
        }
//        } else {
//            rootView = inflater.inflate(R.layout.fragment_image_define, container, false);
//        }

        // Extract the view elements to be referred to later
        wheelView = rootView.findViewById(R.id.wheelView);
        attrSeekbar = rootView.findViewById(R.id.attribute_seekbar);
        attrText = rootView.findViewById(R.id.attribute_text);
        attrLeft = rootView.findViewById(R.id.attribute_left);
        attrRight = rootView.findViewById(R.id.attribute_right);
        attrVal = rootView.findViewById(R.id.attribute_val);
        attrContainer = rootView.findViewById(R.id.imageTypeContainer);
        imageMetaSpinner = rootView.findViewById(R.id.image_meta_spinner);

        // Extract information from the arguments bundle
        Bundle args = getArguments();

        Bike_Wheel_Animation initializeBikeWheelAnimation;
        thisNumLEDs = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("num_leds", Integer.toString(getResources().getInteger(R.integer.num_leds))));

        if (args.containsKey(BIKE_WHEEL_ANIMATION)) {
            initializeBikeWheelAnimation = (Bike_Wheel_Animation) args.getSerializable(BIKE_WHEEL_ANIMATION);
        } else {
            initializeBikeWheelAnimation = new Bike_Wheel_Animation(thisNumLEDs);
        }

        if (args.containsKey(IS_IDLE)) {
            isIdle = args.getBoolean(IS_IDLE);
        } else {
            isIdle = false;
        }

        // Set up the spinner for the Image Meta type (either main or idle)
        int imageMetaSpinnerResourceID;
        AdapterView.OnItemSelectedListener imageMetaItemSelectedListener;

        if (isIdle) {
            // If this is an idle image, then only wheel-relative motion can be used
            imageMetaSpinnerResourceID = R.array.image_idle_meta_types;

            imageMetaItemSelectedListener = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String choice = (String) parent.getItemAtPosition(position);

                    if (getResources().getString(R.string.WRel_Const).equals(choice)) {
//                    imageDefineAdapter.setImageMainMeta(Constants.IMAGE_CONSTROT_WREL);
                        setAttributeType(Constants.IMAGE_CONSTROT_WREL);
                    } else {
                        // Uh oh...
                        Log.e(TAG, "Got illegal Idle ImageMetaSpinner choice.");
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            };
        } else {
            // If this is a main image, then any type of image can be used
            imageMetaSpinnerResourceID = R.array.image_main_meta_types;

            imageMetaItemSelectedListener = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String choice = (String) parent.getItemAtPosition(position);

                    if (getResources().getString(R.string.WRel_Const).equals(choice)) {
//                    imageDefineAdapter.setImageMainMeta(Constants.IMAGE_CONSTROT_WREL);
                        setAttributeType(Constants.IMAGE_CONSTROT_WREL);
                    } else if (getResources().getString(R.string.GRel_Const).equals(choice)) {
//                    imageDefineAdapter.setImageMainMeta(Constants.IMAGE_CONSTROT_GREL);
                        setAttributeType(Constants.IMAGE_CONSTROT_GREL);
                    } else if (getResources().getString(R.string.Spinner_Wheel).equals(choice)) {
//                    imageDefineAdapter.setImageMainMeta(Constants.IMAGE_SPINNER);
                        setAttributeType(Constants.IMAGE_SPINNER);
                    } else {
                        // Uh oh...
                        Log.e(TAG, "Got illegal Main ImageMetaSpinner choice.");
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            };
        }

        // Populate the spinner according to the above selections
        ArrayAdapter imageMetaSpinnerAdapter = ArrayAdapter.createFromResource(getContext(), imageMetaSpinnerResourceID, android.R.layout.simple_spinner_item);
        imageMetaSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        imageMetaSpinner.setAdapter(imageMetaSpinnerAdapter);
        imageMetaSpinner.setOnItemSelectedListener(imageMetaItemSelectedListener);

        // Initialize the spinner choice (take into account whether this is an idle or main fragment)
        int imageMetaSpinnerChoice;
        if (isIdle) {
            imageMetaSpinnerChoice = imageMetaTypeToSpinnerPos(initializeBikeWheelAnimation.getImageIdleMeta().getImageType());
        } else {
            imageMetaSpinnerChoice = imageMetaTypeToSpinnerPos(initializeBikeWheelAnimation.getImageMainMeta().getImageType());
        }

        imageMetaSpinner.setSelection(imageMetaSpinnerChoice, false); // Set the original image meta type in the GUI

        // Set up the drawable for the wheelView image view
        ledViewDrawable = new LEDViewDrawable(wheelView, thisNumLEDs);
        wheelView.setBackground(ledViewDrawable);

        // Create an ImageStack and save the input/new image as the first in the stack

        // Set up the seekbar
        attrSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setAttrValue(progress + curSeekbarOffset);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Set left and right button callbacks
        attrLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (curAttrVal > curAttrMin) {
                    setAttrValue(curAttrVal - 1);
                }
            }
        });

        attrRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (curAttrVal < curAttrMax) {
                    setAttrValue(curAttrVal + 1);
                }
            }
        });

        // Set up the Image Modification Spinner
        imageModSpinner = rootView.findViewById(R.id.imageModSpinner);
        imageModSpinner.setAdapter(new ImageModSpinnerAdapter(getContext(), isIdle));
        imageModSpinner.setSelection(0, false);
        imageModSpinner.setListener(new AdapterView.OnItemSelectedListener() {
//            boolean check = false;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                if (check) {
                // Launch a dialog window, chosen according to the selection, that will allow the user to paint onto the image.
                FragmentManager fm = getChildFragmentManager(); //getFragmentManager();
                ImageModSpinnerAdapter.ImageModSpinnerItem itemInfo = (ImageModSpinnerAdapter.ImageModSpinnerItem) parent.getItemAtPosition(position); // Get the information of the current object

                ImageModFragment fragment = ImageModFragment.newInstance(palette, imageStack.currentImage(), itemInfo.itemType, isIdle);
                fragment.show(fm, "Image Mod Dialog");
//                }

//                check = true;
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Set up the imagestack undo/redo buttons
        imageModUndo = rootView.findViewById(R.id.image_undo);
        imageModUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageStack.undo();
            }
        });

        imageModRedo = rootView.findViewById(R.id.image_redo);
        imageModRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageStack.redo();
            }
        });

        // Initialize the view
        setPalette(initializeBikeWheelAnimation.getPalette()); // Set the palette

        // Initialize the images and meta data
        ArrayList<Integer> image;
        ImageMeta_ imageMeta;
        if (isIdle) {
            image = new ArrayList<>(initializeBikeWheelAnimation.getImageIdle());
            imageMeta = initializeBikeWheelAnimation.getImageIdleMeta();
        } else {
            image = new ArrayList<>(initializeBikeWheelAnimation.getImageMain());
            imageMeta = initializeBikeWheelAnimation.getImageMainMeta();
        }

        setAttribute(imageMeta); // Set the image meta type and its current value
        imageStack = new ImageStack(image);


        // Finally, if there was a saved ImageStack, put that in instead
        if (savedInstanceState != null && savedInstanceState.containsKey(IMAGE_STACK)) {
            ArrayList<ArrayList<Integer>> newImageList = (ArrayList<ArrayList<Integer>>) savedInstanceState.getSerializable(IMAGE_STACK);
            int curLoc = savedInstanceState.getInt(IMAGE_LOC);
//            ledViewDrawable = (LEDViewDrawable) savedInstanceState.getSerializable(LED_DRAWABLE);
//            thisTestID = savedInstanceState.getInt(TEST_ID);
            if (newImageList != null) {
                imageStack = new ImageStack();
                imageStack.setCurStackLocation(curLoc);
                imageStack.setImagesList(newImageList);
//                wheelView.setBackground(ledViewDrawable);
            }
        }

        // Ensure that the Image is seen on the LEDViewDrawable
//        imageStack.setViews(ledViewDrawable, imageModUndo, imageModRedo);
        imageStack.updateImageGUI();

        return rootView;
    }

    private void setAttribute(ImageMeta_ newImageMeta) {
        int newAttrType = newImageMeta.getImageType();
        curAttrType = newAttrType; // Tell the Fragment that the current attribute type is the incoming ImageMeta's type, so that it saves the new curAttrVal

        // Save the attribute value
        switch (newAttrType) {
            case Constants.IMAGE_CONSTROT_WREL:
                curAttrVal = ((Image_Meta_ConstRot_WRel) newImageMeta).getRotationSpeed();
                break;
            case Constants.IMAGE_CONSTROT_GREL:
                curAttrVal = ((Image_Meta_ConstRot_GRel) newImageMeta).getRotationSpeed();
                break;
            case Constants.IMAGE_SPINNER:
                curAttrVal = ((Image_Meta_Spinner) newImageMeta).getInertia();
                break;
        }

        // Set the attribute type
        setAttributeType(newAttrType);
    }

    public void setAttributeType(int newAttrType) {
        // Save the old attribute type
        switch (curAttrType) {
            case Constants.IMAGE_CONSTROT_WREL:
                storedAttrVals.set(0, curAttrVal);
                break;
            case Constants.IMAGE_CONSTROT_GREL:
                storedAttrVals.set(0, curAttrVal);
                break;
            case Constants.IMAGE_SPINNER:
                storedAttrVals.set(1, curAttrVal);
                break;
            default:
                // If the fragment was just initialized, don't save any old values, because they don't exist yet!
        }

        curAttrType = newAttrType;

        // Set all of the values that are unique to each type of image
        boolean isAdjustable; // Default to an idle state
        String attrString = "";
        int attrVal = 0;
        int maxVal = 0;
        int minVal = 0;

        if (newAttrType == Constants.IMAGE_CONSTROT_WREL || newAttrType == Constants.IMAGE_CONSTROT_GREL) {
            isAdjustable = true;
            attrString = getResources().getString(R.string.Rotation_Rate);
            attrVal = storedAttrVals.get(0);
            maxVal = getResources().getInteger(R.integer.max_speed);
            minVal = -1 * getResources().getInteger(R.integer.max_speed);
        } else if (newAttrType == Constants.IMAGE_SPINNER) {
            isAdjustable = true;
            attrString = getResources().getString(R.string.Inertia);
            attrVal = storedAttrVals.get(1);
            maxVal = getResources().getInteger(R.integer.max_inertia);
            minVal = 0;
        } else {
            isAdjustable = false;
        }

        // Adjust the GUI
        setVisibility(isAdjustable);

        if (isAdjustable) {
            // If this Fragment is not being used to adjust an Idle animation, then attribute adjustments can be made
            attrText.setText(attrString);
            attrSeekbar.setMax(maxVal - minVal);
            curSeekbarOffset = minVal;

            // Set the attribute value from the stored value
            curAttrMax = maxVal;
            curAttrMin = minVal;
            setAttrValue(attrVal);
        }

        // Finally, attempt to notify the pager adapter (through the WheelViewActivity), so that it can display/remove the Idle image tab
        if (parentActivity != null) {
            parentActivity.updatePagerAdapter();
        }
    }

    private void setAttrValue(int newVal) {
        curAttrVal = newVal; // Keep track of the value as an int
        attrSeekbar.setProgress(newVal - curSeekbarOffset); // Set the seekbar progress
        attrVal.setText(Integer.toString(newVal)); // Set the value in text

        // Delete the old animator set
        if (rotationAnimator != null) {
            rotationAnimator.end();
            rotationAnimator.cancel();
        }

        if (curAttrType == Constants.IMAGE_CONSTROT_WREL || curAttrType == Constants.IMAGE_CONSTROT_GREL) {
            int rotEnd = thisNumLEDs;
            if (curAttrVal == 0) {
                // Do not animate the speed if it is 0
                return;
            } else if (curAttrVal < 0) {
                rotEnd *= -1; // If the rotation speed is negative, make the goal rotation (achieved by the end of the animation) negative
            }

            // Set up the new animation
            int rotStart = 0;
            long duration = (long) (((float) rotEnd / (float) curAttrVal) * 1000); // Get the duration in ms
            rotationAnimator = ObjectAnimator.ofInt(ledViewDrawable, "rotationOffset", rotStart, rotEnd)
                    .setDuration(duration);
            rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            rotationAnimator.setRepeatMode(ValueAnimator.RESTART);
            rotationAnimator.setInterpolator(new LinearInterpolator());
            rotationAnimator.start();
        }
    }

//    public ImageMeta_ getCurImageMeta() {
//        switch (curAttrType) {
//            case Constants.IMAGE_CONSTROT_:
//
//            case Constants.IMAGE_SPINNER:
//        }
//    }

    private void setVisibility(boolean newVisibility) {
        // Set the same visibility for each widget
        int newVisVal;
        if (newVisibility) {
            newVisVal = View.VISIBLE;
        } else {
            newVisVal = View.GONE;

        }

//        attrText.setVisibility(newVisVal);
//        attrSeekbar.setVisibility(newVisVal);
//        attrLeft.setVisibility(newVisVal);
//        attrRight.setVisibility(newVisVal);
//        attrVal.setVisibility(newVisVal);
        attrContainer.setVisibility(newVisVal);
    }

//    public void setAttrUpdatedListener(AttrUpdatedListener listener) {
//        this.listener = listener;
//    }

//    public interface AttrUpdatedListener {
//        void imageAttrUpdate(int newAttrVal);
//    }

    @Override
    public void setImage(ArrayList<Integer> image) {
        // First, save the new image to the stack
        imageStack.save(image);

//        // Update the Bike_Wheel_Animation that wheelListAdapter uses
//        Bike_Wheel_Animation list =  listener.getColorList();
//        list.setImage(imageStack.currentImage());
//        wheelListAdapter.setPalette(list);
    }

//    // Methods for interacting with the palette, through the Activity
//    public interface ImageDefineInterface {
//        Bike_Wheel_Animation setPalette(ArrayList<Color_> colorList); // Will be called by the ColorListRecyclerAdapter to set the palette for this fragment
//
//        void setSelectedItem(int selectedItem); // Will be called by the ColorListRecyclerAdapter to update THIS fragment
//    }

    public void setPalette(ArrayList<Color_> newColorList) {
        // Set the palette that the image displays with
        palette = new ArrayList<>(newColorList);
        ledViewDrawable.setPalette(palette);
    }

    public void setSelectedItem(int selectedItem) {
        // Set which item is highlighted in the GUI
        ledViewDrawable.setSelection(selectedItem);
    }

    public void removeColorFromPalette(int colorIndToRemove) {
        // Modify the palette and update the GUI
        palette.remove(colorIndToRemove);
        ledViewDrawable.setPalette(palette);

        // Modify the Image, if needed
        boolean didMod = false;
        ArrayList<Integer> modifiedImage = imageStack.currentImage();
        for (int i = 0; i < modifiedImage.size(); i++) {
            if (modifiedImage.get(i) == colorIndToRemove) {
                modifiedImage.set(i, 0); // If the current value of the image is equal to the color being removed, then just set it to represent the first Color_
                didMod = true;
            } else if (modifiedImage.get(i) > colorIndToRemove) {
                modifiedImage.set(i, modifiedImage.get(i) - 1); // If the value of the image is higher than the index of the deleted color, then shift its value down
                didMod = true;

            }
        }

        // If any modifications occurred, erase the imageStack buffer
        if (didMod) {
            // Erase the buffer of the image stack and add the modified image that was just created (removing a color means we can't go back...we can set up a more robust undo/redo implementation for this when we become rich)
            imageStack.eraseBuffer(modifiedImage);
        }
    }

    private int imageMetaTypeToSpinnerPos(int imageMetaType) {
        switch (imageMetaType) {
            case Constants.IMAGE_CONSTROT_WREL:
                return 0;
            case Constants.IMAGE_CONSTROT_GREL:
                return 1;
            case Constants.IMAGE_SPINNER:
                return 2;
            default:
                // Uh-oh...
                return 0;
        }
    }

    public ArrayList<Integer> getImage() {
        return imageStack.currentImage();
    }

    public ImageMeta_ getImageMeta() {
        ImageMeta_ newImageMeta_;
        switch (curAttrType) {
            case Constants.IMAGE_CONSTROT_WREL:
                newImageMeta_ = new Image_Meta_ConstRot_WRel(curAttrVal);
                break;
            case Constants.IMAGE_CONSTROT_GREL:
                newImageMeta_ = new Image_Meta_ConstRot_GRel(curAttrVal);
                break;
            case Constants.IMAGE_SPINNER:
                newImageMeta_ = new Image_Meta_Spinner(curAttrVal);
                break;
            default:
                // Uh oh...
                newImageMeta_ = new Image_Meta_ConstRot_WRel(0);
        }

        return newImageMeta_;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (ledViewDrawable != null) {
            ledViewDrawable.setAnimationRunning(true);
        }
        if (rotationAnimator != null) {
            rotationAnimator.start();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (ledViewDrawable != null) {
            ledViewDrawable.setAnimationRunning(false);
        }

        if (rotationAnimator != null) {
            rotationAnimator.pause();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            parentActivity = (ImageMetaChangeInterface) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement the ImageMetaChangeInterface (updatePagerAdapter() must notify the pager adapter of a data set change)");
        }
    }

    public interface ImageMetaChangeInterface {
        // A very stupid and clunky interface to let the ImageDefinePagerAdapter that created this fragment know when the main ImageMeta changes (so it can display the idle image tab only when applicable)
        // (Unfortunately, there does not seem to be a way to elegantly pass a reference to the ImageDefinePagerAdapter to this Fragment, otherwise that would be the simplest solution to updating ImageDefinePagerAdapter when needed
        void updatePagerAdapter();
    }
}


package to.us.suncloud.bikelights.common.WheelView;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Color.Color_;
import to.us.suncloud.bikelights.common.Color.Bike_Wheel_Animation;
import to.us.suncloud.bikelights.common.Constants;
import to.us.suncloud.bikelights.common.Image.ImageMeta_;
import to.us.suncloud.bikelights.common.Image.Image_Meta_ConstRot;
import to.us.suncloud.bikelights.common.Image.Image_Meta_Spinner;

public class ImageDefineFragment extends Fragment implements ImageModFragment.ImageModListener {
    public static final String IS_IDLE = "IS_IDLE";
    public static final String BIKE_WHEEL_ANIMATION = "BIKE_WHEEL_ANIMATION";
    public static final String IMAGE_STACK = "IMAGE_STACK";
    public static final String IMAGE_LOC = "IMAGE_LOC";
//    public static final String LED_DRAWABLE = "LED_DRAWABLE";
//    public static final String TEST_ID = "TEST_ID";
//    public static final String ROOT_VIEW = "ROOT_VIEW";

    private View rootView; // The root view of this Fragment
    private ImageStack imageStack; // The stack that represents all images that have been set up until now (allows undoing and redoing)
    ArrayList<Color_> palette; // A list of colors that the ledViewDrawable can use
    boolean isIdle; // Is this an idle image?

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

        ImageStack() {};

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

    public static ImageDefineFragment newInstance(Bike_Wheel_Animation bikeWheelAnimation, boolean isIdle) {

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

        // Extract information from the arguments bundle
        Bundle args = getArguments();

        Bike_Wheel_Animation initializeBikeWheelAnimation;

        if (args.containsKey(BIKE_WHEEL_ANIMATION)) {
            initializeBikeWheelAnimation = (Bike_Wheel_Animation) args.getSerializable(BIKE_WHEEL_ANIMATION);
        } else {
            initializeBikeWheelAnimation = new Bike_Wheel_Animation(getResources().getInteger(R.integer.num_leds));
        }

        if (args.containsKey(IS_IDLE)) {
            isIdle = args.getBoolean(IS_IDLE);
        } else {
            isIdle = false;
        }

        // Set up the drawable for the wheelView image view
        wheelView = rootView.findViewById(R.id.wheelView);
        ledViewDrawable = new LEDViewDrawable(wheelView, getResources().getInteger(R.integer.num_leds));
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
        imageModSpinner.setAdapter(new ImageModSpinnerAdapter(getContext()));
        imageModSpinner.setSelection(0, false);
        imageModSpinner.setListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Launch a dialog window, chosen according to the selection, that will allow the user to paint onto the image.
                FragmentManager fm = getChildFragmentManager(); //getFragmentManager();
                ImageModFragment fragment = ImageModFragment.newInstance(palette, imageStack.currentImage(), position, isIdle);
                fragment.show(fm, "Image Mod Dialog");
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

        if (isIdle) {
            setAttributeType(Constants.IMAGE_IDLE); // Initialize how this fragment should display
            imageStack = new ImageStack(new ArrayList<>(initializeBikeWheelAnimation.getImageIdle())); // Initialize the Image for this fragment
        } else {
            setAttribute(initializeBikeWheelAnimation.getImageMeta()); // Set the image meta type and its current value
            imageStack = new ImageStack(new ArrayList<>(initializeBikeWheelAnimation.getImageMain()));
        }

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
            case Constants.IMAGE_CONSTROT:
                curAttrVal = ((Image_Meta_ConstRot) newImageMeta).getRotationSpeed();
                break;
            case Constants.IMAGE_SPINNER:
                curAttrVal = ((Image_Meta_Spinner) newImageMeta).getInertia();
                break;
        }

        // Set the attribute type
        setAttributeType(newAttrType);
    }

    public void setAttributeType(int newAttrType) {
        // Initialize the textbox, set the bounds and position of the seekbar, and call setAttrVal

        // Save the old attribute type
        switch (curAttrType) {
            case Constants.IMAGE_CONSTROT:
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
        switch (newAttrType) {
            case Constants.IMAGE_CONSTROT:
                isAdjustable = true;
                attrString = getResources().getString(R.string.Rotation_Rate);
                attrVal = storedAttrVals.get(0);
                maxVal = getResources().getInteger(R.integer.max_speed);
                minVal = -1 * getResources().getInteger(R.integer.max_speed);
                break;
            case Constants.IMAGE_SPINNER:
                isAdjustable = true;
                attrString = getResources().getString(R.string.Inertia);
                attrVal = storedAttrVals.get(1);
                maxVal = getResources().getInteger(R.integer.max_inertia);
                minVal = 0;
                break;
            default:
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

        if (curAttrType == Constants.IMAGE_CONSTROT) {
            int rotEnd = getResources().getInteger(R.integer.num_leds);
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
//            case Constants.IMAGE_CONSTROT:
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

    public ArrayList<Integer> getImage() {
        return imageStack.currentImage();
    }

    public ImageMeta_ getImageMeta() {
        ImageMeta_ newImageMeta_;
        switch (curAttrType) {
            case Constants.IMAGE_CONSTROT:
                newImageMeta_ = new Image_Meta_ConstRot(curAttrVal);
                break;
            case Constants.IMAGE_SPINNER:
                newImageMeta_ = new Image_Meta_Spinner(curAttrVal);
                break;
            default:
                // Uh oh...
                newImageMeta_ = new Image_Meta_ConstRot(0);
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
}


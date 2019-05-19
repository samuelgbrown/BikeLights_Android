package to.us.suncloud.bikelights.common.Color;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import to.us.suncloud.bikelights.common.Constants;
import to.us.suncloud.bikelights.common.Image.ImageMeta_;
import to.us.suncloud.bikelights.common.Image.Image_Meta_ConstRot;
import to.us.suncloud.bikelights.common.Image.Image_Meta_Spinner;
import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim;

public class Bike_Wheel_Animation implements Serializable {
    // Main data
    private ArrayList<Color_> mPalette; // Initialize the list, in case it is not overwritten
    private ArrayList<Integer> mImageMain; // Initialize the mImageMain
    private ArrayList<Integer> mImageIdle; // Initialize the mImageMain

    // Meta data
    private ImageMeta_ mImageMeta = new Image_Meta_ConstRot(0); // The meta data for this Bike_Wheel_Animation
//    private float brightnessScale = 1; // TODO: Use colorObj#scale to generate new colorObj's with a brightness scale GIVEN BY THE MAIN ACTIVITY.  Value between (0 1] that scales the brightness of all LED values (allows user to quickly and easily scale the brightness of the wheel as a whole)

    public Bike_Wheel_Animation(Bike_Wheel_Animation otherList) {
        this.mPalette = otherList.getPalette();
        this.mImageMain = otherList.getImageMain();
        this.mImageIdle = otherList.getImageIdle();
        this.mImageMeta = otherList.getImageMeta();
    }

    public Bike_Wheel_Animation(int numLEDs) {
        mPalette = new ArrayList<>(Collections.nCopies(1, (Color_) new Color_Static()));
        mImageMain = new ArrayList<>(Collections.nCopies(numLEDs, 0));

        checkIdle();
        checkImage();
        checkEmpty();
    }

    public Bike_Wheel_Animation(ArrayList<Color_> colors, int numLEDs) {
        mPalette = colors;
        mImageMain = new ArrayList<>(Collections.nCopies(numLEDs, 0));
        checkIdle();
        checkImage();
        checkEmpty();
    }

    public Bike_Wheel_Animation(ArrayList<Color_> colors, ArrayList<Integer> image) {
        mPalette = colors;
        mImageMain = image;
        checkIdle();
        checkImage();
        checkEmpty();
    }

    public Bike_Wheel_Animation(ArrayList<Color_> colors, ArrayList<Integer> imageMain, ArrayList<Integer> imageIdle, ImageMeta_ imageMeta) {
        mPalette = colors;
        mImageMain = imageMain;
        mImageIdle = imageIdle;
        mImageMeta = imageMeta;

        checkIdle();
        checkImage();
        checkEmpty();
    }

    public void add(Color_ c) {
        mPalette.add(c);
        checkEmpty();
    }

    public void set(int index, Color_ c) {
        mPalette.set(index, c);
        checkEmpty();
    }

    public Color_ getP(int index) {
        return mPalette.get(index);
    }

    public int getIMain(int index) {
        return mImageMain.get(index);
    }

    public int getIIdle(int index) {
        return mImageIdle.get(index);
    }

    public int numColors() {
        return mPalette.size();
    }

    public int sizeImage() {
        return mImageMain.size();
    }

    public void remove(int index) {
        mPalette.remove(index);
        checkEmpty();
        checkImage();
    }

    public void remove(Color_ obj) {
        mPalette.remove(obj);
        checkEmpty();
        checkImage();
    }

    public void clear() {
        mPalette.clear();
        checkEmpty();
        checkImage();
    }

    public ArrayList<Color_> getPalette() {
        return new ArrayList<>(mPalette);
    }

    public ArrayList<Integer> getImageMain() {
        return new ArrayList<>(mImageMain);
    }

    public ArrayList<Integer> getImageIdle() {
        if (mImageMeta.supportsIdle()) {
            return new ArrayList<>(mImageIdle);
        } else {
            // This shouldn't really happen...
            return new ArrayList<>(Collections.nCopies(mImageMain.size(), 0));
        }
    }

    public ImageMeta_ getImageMeta() {
        return mImageMeta.clone();
    }

//    public void setImageMain(ArrayList<Integer> indsToSet, int valToSet) {
//        // This function allows modification of the mImageMain.  The List represents indices in mImageMain to set to the int valToSet.
//
////        // First, error-check valToSet
////        if (valToSet >= getNumColors()) {
////            valToSet = getNumColors() - 1; // Ensure that valToSet can never reference a color that does not exist
////        }
//
//        // Set the values in indsToSet
//        for (int i = 0; i < indsToSet.size(); i++) {
//            mImageMain.set(indsToSet.get(i), valToSet);
//        }
//
//        // Ensure that there are enough colors to be referenced
//        checkImage();
//    }


    public void setPalette(ArrayList<Color_> mPalette) {
        this.mPalette = mPalette;
    }

    public void setImageMain(ArrayList<Integer> image) {
        this.mImageMain = image;

        checkImage();
    }

    public void setImageIdle(ArrayList<Integer> image) {
        this.mImageIdle = image;

        checkIdle();
        checkImage();
    }

    public void setImageMeta(ImageMeta_ imageMeta) {
        this.mImageMeta = imageMeta;

        checkIdle();
    }

    public void setImageMetaType(int imageMetaType) {
        ImageMeta_ newImageMeta_;
        switch (imageMetaType) {
            case Constants.IMAGE_CONSTROT:
                newImageMeta_ = new Image_Meta_ConstRot(0);
                break;
            case Constants.IMAGE_SPINNER:
                newImageMeta_ = new Image_Meta_Spinner(0);
                break;
            default:
                newImageMeta_ = new Image_Meta_ConstRot(0);
        }
        this.mImageMeta = newImageMeta_;

        checkIdle();
    }

    private void checkImage() {
        // Check that each index in the new image refers to a color that is defined in mPalette
        int maxNumColors = getPalette().size();
        for (int i = 0; i < mImageMain.size(); i++) {
            maxNumColors = Math.max(maxNumColors, mImageMain.get(i)); // Find the maximum index in colors that is referred to in image
        }

        if (mImageMeta.supportsIdle()) {
            for (int i = 0; i < mImageIdle.size(); i++) {
                maxNumColors = Math.max(maxNumColors, mImageIdle.get(i)); // Find the maximum index in colors that is referred to in image
            }
        }

        // Ensure that there are enough colors to be referenced
        for (int i = getPalette().size(); i < maxNumColors; i++) {
            mPalette.add(new Color_Static());
        }

//        // Check that the color list is not empty
//        checkEmpty();
    }

    private void checkEmpty() {
//        // Ensure that mPalette and mImageMain are the same size.  Remove elements from the larger
//        if (mPalette.size() != mImageMain.size()) {
//            int newSize = Math.max(mPalette.size(), mImageMain.size());
//            mPalette.subList(0, newSize).clear();
//            mImageMain.subList(0, newSize).clear();
//        }

        // Ensure that mPalette is never completely empty, and contains at least a black static color
        if (mPalette.size() == 0) {
            mPalette.add(new Color_Static());
//            mImageMain.add(0);
        }
    }

    private void checkIdle() {
        // Ensure that, iff the current ImageMeta_ requires an idle animation, one exists
        if (mImageMeta.supportsIdle() && mImageIdle == null) {
            // There should be an idle
            mImageIdle = new ArrayList<>(Collections.nCopies(mImageMain.size(), 0)); // Make a blank idle image
        } else if (!mImageMeta.supportsIdle() && mImageIdle != null) {
            // There should not be an idle
            mImageIdle = null;
        }
    }

    public Bike_Wheel_Animation clone() {
        return new Bike_Wheel_Animation(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bike_Wheel_Animation) {
            // If the Palette, Idle Image, Main Image, and Image Meta are all equivalent, then the two objects are equivalent
            boolean pEqual = getPalette().equals(((Bike_Wheel_Animation) obj).getPalette());
            boolean idleEqual = getImageIdle().equals(((Bike_Wheel_Animation) obj).getImageIdle());
            boolean mainEqual = getImageMain().equals(((Bike_Wheel_Animation) obj).getImageMain());
            boolean metaEqual = getImageMeta().equals(((Bike_Wheel_Animation) obj).getImageMeta());
            return pEqual && idleEqual && mainEqual && metaEqual;
        } else {
            return false;
        }
    }

    private ArrayList<BikeWheelAnim.Color_> getPaletteBuf() {
        ArrayList<BikeWheelAnim.Color_> paletteBuf = new ArrayList<>();
        for (int colorInd = 0; colorInd < numColors(); colorInd++) {
            paletteBuf.add(mPalette.get(colorInd).getColorBuf());
        }

        return paletteBuf;
    }

    public BikeWheelAnim toProtoBuf() {
        BikeWheelAnim.Builder BWAProto = BikeWheelAnim.newBuilder()
                // Add number of LEDs
                .setNumLeds(mImageMain.size())

                // Add the palette
                .addAllPalette(getPaletteBuf())

                // Add the main Image
                .addAllImageMain(mImageMain)

                // Add ImageMeta
                .setImageMeta(BikeWheelAnim.ImageMeta.newBuilder()
                        .setType(mImageMeta.getImageTypeBuf())
                        .setParameterSet(mImageMeta.getImageParameterBuf())
                );

        // If there is an idle image, add that too
        if (mImageMeta.supportsIdle()) {
            BWAProto.addAllImageIdle(mImageIdle);
        }

        return BWAProto.build();
    }

    static public Bike_Wheel_Animation fromProtoBuf(BikeWheelAnim messageBWA) {
        // Extract all of the information from the message
        ArrayList<Color_> palette = Color_.fromProtoBufColor_(messageBWA.getPaletteList()); // Get the palette from the message
        ArrayList<Integer> imageMain = new ArrayList<>(messageBWA.getImageMainList()); // Get the main image
        ImageMeta_ imageMeta = ImageMeta_.fromProtoBuf(messageBWA.getImageMeta()); // Get the image meta

        ArrayList<Integer> imageIdle;
        if (imageMeta.supportsIdle()) {
            imageIdle = new ArrayList<>(messageBWA.getImageIdleList()); // If this pattern supports an idle animation, get it from the message
        } else {
            // If there is no idle supported for this pattern, then image idle should be null
            imageIdle = null;
        }

        // Compile the new information into a new BWA object, and return it
        return new Bike_Wheel_Animation(palette, imageMain, imageIdle, imageMeta);
    }
}

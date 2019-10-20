package to.us.suncloud.bikelights.common.Color;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import to.us.suncloud.bikelights.common.Bluetooth.BluetoothByteList;
import to.us.suncloud.bikelights.common.Bluetooth.BluetoothByteList.ContentType;
import to.us.suncloud.bikelights.common.ByteMath;
import to.us.suncloud.bikelights.common.Constants;
import to.us.suncloud.bikelights.common.Image.ImageMeta_;
import to.us.suncloud.bikelights.common.Image.Image_Meta_ConstRot_;
import to.us.suncloud.bikelights.common.Image.Image_Meta_ConstRot_GRel;
import to.us.suncloud.bikelights.common.Image.Image_Meta_ConstRot_WRel;
import to.us.suncloud.bikelights.common.Image.Image_Meta_Spinner;
import to.us.suncloud.bikelights.common.Color.Color_d.BLEND_TYPE;
//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim;

public class Bike_Wheel_Animation implements Serializable {
    // Main data
    private ArrayList<Color_> mPalette; // Initialize the list, in case it is not overwritten
    private ArrayList<Integer> mImageMain; // Initialize the mImageMain
    private ArrayList<Integer> mImageIdle; // Initialize the mImageMain

    // Meta data
    private ImageMeta_ mImageMainMeta = new Image_Meta_ConstRot_WRel(0); // The meta data for this Bike_Wheel_Animation
    private ImageMeta_ mImageIdleMeta = new Image_Meta_ConstRot_WRel(0); // The meta data for this Bike_Wheel_Animation
//    private float brightnessScale = 1; // TO_DO: (Still doing?) Use colorObj#scale to generate new colorObj's with a brightness scale GIVEN BY THE MAIN ACTIVITY.  Value between (0 1] that scales the brightness of all LED values (allows user to quickly and easily scale the brightness of the wheel as a whole)

    public Bike_Wheel_Animation(Bike_Wheel_Animation otherList) {
        this.mPalette = otherList.getPalette();
        this.mImageMain = otherList.getImageMain();
        this.mImageIdle = otherList.getImageIdle();
        this.mImageMainMeta = otherList.getImageMainMeta();
        this.mImageIdleMeta = otherList.getImageIdleMeta();
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

    public Bike_Wheel_Animation(ArrayList<Color_> colors, ArrayList<Integer> imageMain, ArrayList<Integer> imageIdle, ImageMeta_ imageMainMeta, ImageMeta_ imageIdleMeta) {
        mPalette = colors;
        mImageMain = imageMain;
        mImageIdle = imageIdle;
        mImageMainMeta = imageMainMeta;
        mImageIdleMeta = imageIdleMeta;

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
        if (mImageMainMeta.supportsIdle()) {
            return new ArrayList<>(mImageIdle);
        } else {
            // This shouldn't really happen...
            return new ArrayList<>(Collections.nCopies(mImageMain.size(), 0));
        }
    }

    public ImageMeta_ getImageMainMeta() {
        return mImageMainMeta.clone();
    }

    public ImageMeta_ getImageIdleMeta() {
        return mImageIdleMeta.clone();
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

    public void setImageMainMeta(ImageMeta_ imageMeta) {
        this.mImageMainMeta = imageMeta;

        // Make sure that this new ImageMeta is compatibile with the existing idle image
        checkIdle();
    }

    public void setImageIdleMeta(ImageMeta_ imageMeta) {
        // If the main image supports an idle, then add the new one
        if (mImageMainMeta.supportsIdle()) {
            this.mImageIdleMeta = imageMeta;
        }

    }

    public void setImageMainMetaType(int imageMetaType) {
        // This method is only used in the ImageDefinePagerAdapter, where a Bike_Wheel_Animation is used specifically to hold onto image data and metadata; it is not meant to represent a full BWA. (I know that's not exactly proper, but whatever...)
        // Therefore, this method is used ONLY in that context, and is a bit shortcut-y

        ImageMeta_ newImageMeta_;
        switch (imageMetaType) {
            case Constants.IMAGE_CONSTROT_WREL:
                newImageMeta_ = new Image_Meta_ConstRot_WRel(0);
                break;
            case Constants.IMAGE_CONSTROT_GREL:
                newImageMeta_ = new Image_Meta_ConstRot_GRel(0);
                break;
            case Constants.IMAGE_SPINNER:
                newImageMeta_ = new Image_Meta_Spinner(0);
                break;
            default:
                newImageMeta_ = new Image_Meta_ConstRot_WRel(0);
        }

        // Set this new ImageMeta
        setImageMainMeta(newImageMeta_);
    }

    public void setImageIdleMetaType(int imageMetaType) {
        // This method is only used in the ImageDefinePagerAdapter, where a Bike_Wheel_Animation is used specifically to hold onto image data and metadata; it is not meant to represent a full BWA. (I know that's not exactly proper, but whatever...)
        // Therefore, this method is used ONLY in that context, and is a bit shortcut-y

        if (mImageMainMeta.supportsIdle()) {
            ImageMeta_ newImageMeta_;
            switch (imageMetaType) {
                // Left as a switch statement for future compatibility, in case of other idle image types being added
                case Constants.IMAGE_CONSTROT_WREL:
                    newImageMeta_ = new Image_Meta_ConstRot_WRel(0);
                    break;
                default:
                    newImageMeta_ = new Image_Meta_ConstRot_WRel(0);
            }

            setImageIdleMeta(newImageMeta_);
        }
    }

    private void checkImage() {
        // Check that each index in the new image refers to a color that is defined in mPalette
        int maxNumColors = getPalette().size();
        for (int i = 0; i < mImageMain.size(); i++) {
            maxNumColors = Math.max(maxNumColors, mImageMain.get(i)); // Find the maximum index in colors that is referred to in image
        }

        if (mImageMainMeta.supportsIdle()) {
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
        if (mImageMainMeta.supportsIdle()) {
            // There should be an idle
            if (mImageIdle == null) {
                mImageIdle = new ArrayList<>(Collections.nCopies(mImageMain.size(), 0)); // Make a blank idle image
            }

            if (mImageIdleMeta == null) {
                mImageIdleMeta = new Image_Meta_ConstRot_WRel(0);
            }
        } else {
            // There should not be an idle
            if (mImageIdle != null) {
                mImageIdle = null;
            }

            if (mImageIdleMeta != null) {
                mImageIdleMeta = null;
            }
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
            boolean metaEqual = getImageMainMeta().equals(((Bike_Wheel_Animation) obj).getImageMainMeta());
            return pEqual && idleEqual && mainEqual && metaEqual;
        } else {
            return false;
        }
    }

    public boolean isEmpty() {
        return this.equals(new Bike_Wheel_Animation(sizeImage()));
    }

    // New byte-level manipulation code
    public BluetoothByteList toByteList() {
        // Create a byte-list (ready to be sent to the Arduino, minus headers) from this Bike_Wheel_Anim
        BluetoothByteList byteList = new BluetoothByteList(ContentType.BWA, false);

        // First, create a content header that includes information about the number of LEDs and the size of the palette
        byteList.addByte((byte) mImageMain.size());
        byteList.addByte((byte) mPalette.size());

        // For each Color_ in the Palette, encode the information to the byteList
        for (int color_Num = 0; color_Num < mPalette.size(); color_Num++) {
            byteList.addBytes(mPalette.get(color_Num).toByteList());
        }

        // Finally, encode the images information
        // Start with the header for the image
        byte imageHeaderByte = (byte) 0; // Initialize it to zero
        imageHeaderByte = ByteMath.putBoolToByte(imageHeaderByte, mImageMainMeta.supportsIdle(), 4); // Record whether or not the idle is supported
        imageHeaderByte = ByteMath.putDataToByte(imageHeaderByte, mImageMainMeta.getImageTypeByte(), 4, 0);
        byteList.addByte(imageHeaderByte);

        // Add in the parameter for the main image
        switch (mImageMainMeta.getImageType()) {
            case Constants.IMAGE_CONSTROT_GREL:
                byteList.addByte((byte) ((Image_Meta_ConstRot_GRel) mImageMainMeta).getRotationSpeed());
                break;
            case Constants.IMAGE_CONSTROT_WREL:
                byteList.addByte((byte) ((Image_Meta_ConstRot_WRel) mImageMainMeta).getRotationSpeed());
                break;
            case Constants.IMAGE_SPINNER:
                byteList.addByte((byte) ((Image_Meta_Spinner) mImageMainMeta).getInertia());
                break;
        }

        // Add the main image
        addImageToByteList(byteList, mImageMain); // Add the main image to the byteList

        // If there exists an idle image, add it to the byteList
        if (mImageMainMeta.supportsIdle()) {
            // Put in the type for the idle image (at the moment, only a wheel-relative idle image is supported, but who knows what the future will bring?)
            byteList.addByte(mImageIdleMeta.getImageTypeByte());

            // Add the parameter for the idle image
            switch (mImageIdleMeta.getImageType()) {
                // Left as a switch for future backwards compatibility in case other idle image types are added
                case Constants.IMAGE_CONSTROT_WREL:
                    byteList.addByte((byte) ((Image_Meta_ConstRot_) mImageIdleMeta).getRotationSpeed());
                    break;
                default:
                    byteList.addByte((byte) ((Image_Meta_ConstRot_) mImageIdleMeta).getRotationSpeed());
                    break;
            }

            // Add the idle image itself
            addImageToByteList(byteList, mImageIdle);
        }

        // Return the completed byteList!!!
        return byteList;
    }

    private void addImageToByteList(BluetoothByteList byteList, List<Integer> image) {
        for (int byteNum = 0; byteNum < (image.size() / 2); byteNum++) {
            // Create a zero'd byte to add the image data to
            byte thisByte = 0x00;

            // Add the image data
            thisByte = ByteMath.putDataToByte(thisByte, (byte) image.get(byteNum * 2).intValue(), 4, 0);
            thisByte = ByteMath.putDataToByte(thisByte, (byte) image.get(byteNum * 2 + 1).intValue(), 4, 4);

            // Add the newly constructed byte to the list
            byteList.addByte(thisByte);
        }
    }

    static public Bike_Wheel_Animation fromByteList(BluetoothByteList rawByteList) {
        rawByteList.startReading(); // Set the pointer to the beginning of the byte list
//        int curListLoc = 0; // A counter to keep track of how far into the rawByteList we are
        // First, extract the number of LEDs and the size of the Color_ palette
        int numLEDs = rawByteList.getByte();
        int numColors = rawByteList.getByte();

        // Initialize a new Bike_Wheel_Animation
        Bike_Wheel_Animation bwa = new Bike_Wheel_Animation(numLEDs);

        // Create and populate the palette
        ArrayList<Color_> palette = new ArrayList<>(numColors);
        for (int colorNum = 0; colorNum < numColors; colorNum++) {
            Color_ newColor_; // Make a variable to store the new Color_

            // First, get the Color_ type of the next Color_ in the incoming palette
            int colorType = ByteMath.getNIntFromByte(rawByteList.getByte(), 4, 4);

            if (colorType == Constants.COLOR_STATIC) {
                // Create a new Color_Static from the incoming bytes
                newColor_ = new Color_Static(); // Make the new Color_ a static Color_
                colorObj c = new colorObj(rawByteList.getNextBytes(4));
                palette.add(new Color_Static(c));
            } else {
                // Create a new Color_d from the incoming bytes
                switch (colorType) {
                    case Constants.COLOR_DTIME:
                        newColor_ = new Color_dTime();
                        break;
                    case Constants.COLOR_DSPEED:
                        newColor_ = new Color_dVel();
                        break;
                    default:
                        newColor_ = new Color_dTime();
                }

                // First, get the number of incoming colorObj's
                int numColorObjs = ByteMath.getNIntFromByte(rawByteList.getByte(), 8, 0);
                for (int colorObjNum = 0; colorObjNum < numColorObjs; colorObjNum++) {
                    // For each incoming colorObj in the list

                    // First, get the colorObj
                    colorObj c = new colorObj((rawByteList.getNextBytes(4)));

                    // Next, get the blend type
                    int blendTypeInt = ByteMath.getNIntFromByte(rawByteList.getByte(), 8, 0);
                    BLEND_TYPE blendType = BLEND_TYPE.CONSTANT;
                    switch (blendTypeInt) {
                        case 0:
                            blendType = BLEND_TYPE.CONSTANT;
                            break;
                        case 1:
                            blendType = BLEND_TYPE.LINEAR;
                            break;
                    }

                    // Lastly, get T
                    long T = ByteMath.getLongIntFromByteArray(rawByteList.getNextBytes(4));
                    ; // Get the T value (always a long) that will be assigned to the new Color_d

                    // Finally, create a new colorObjMeta object (to hold the RGB values, the blend value, and the T value), and add it to newColor_
                    ((Color_d) newColor_).addColorObj(c, blendType, T);
                }
            }

            // Assign the newly created color to the palette
            palette.add(newColor_);
        }

        // Assign the palette to the new BWA
        bwa.setPalette(palette);

        // Create and populate the Images and their meta data
        // Read the data from the byte stream
        byte mainImageMetaDataByte = rawByteList.getByte();
        byte mainImageParamByte = rawByteList.getByte();

        // Extract the required information
        boolean imageSupportsIdle = ByteMath.getBoolFromByte(mainImageMetaDataByte, 4); // Does the main image support an idle image?
        int imageTypeMain = ByteMath.getNIntFromByte(mainImageMetaDataByte, 4, 0); // Of what type is the main image?
        int imageParamMain = ByteMath.getNIntFromByte(mainImageParamByte, 8, 0); // What is the parameter for the main image?

        ImageMeta_ mainImageMeta;
        switch (imageTypeMain) {
            case 0:
                // Create a wheel-relative rotation
                mainImageMeta = new Image_Meta_ConstRot_WRel(imageParamMain);
                break;
            case 1:
                // Create a ground-relative rotation
                mainImageMeta = new Image_Meta_ConstRot_GRel(imageParamMain);
                break;
            case 2:
                // Create a spinner-type image
                mainImageMeta = new Image_Meta_Spinner(imageParamMain);
                break;
            default:
                // Create a wheel-relative rotation
                mainImageMeta = new Image_Meta_ConstRot_WRel(imageParamMain);
        }
        bwa.setImageMainMeta(mainImageMeta);


        // Extract the main image
        int numBytesInIncomingImage = (int) Math.ceil(((double) numLEDs) / 2.0);
        ArrayList<Integer> mainImage = new ArrayList<>(numLEDs);
        for (int byteNum = 0; byteNum < numBytesInIncomingImage; byteNum++) {
            // Get a new byte from the incoming list
            byte thisByte = rawByteList.getByte();

            // Split the byte into nibbles and add them to the image
            mainImage.add(ByteMath.getNIntFromByte(thisByte, 4, 0)); // Add the first (lower) nibble
            mainImage.add(ByteMath.getNIntFromByte(thisByte, 4, 4)); // Add the second (upper) nibble
        }

        // Add the main image information to the Bike_Wheel_Animation
        bwa.setImageMain(mainImage);

        // If the image support an idle animation, load it as well
        if (imageSupportsIdle) {
            // Read the data from the byte stream
            byte idleImageMetaDataByte = rawByteList.getByte();
            byte idleImageParamByte = rawByteList.getByte();

            // Extract the required information
            int imageTypeIdle = ByteMath.getNIntFromByte(idleImageMetaDataByte, 8, 0); // Of what type is the idle image?
            int imageParamIdle = ByteMath.getNIntFromByte(idleImageParamByte, 8, 0); // What is the parameter for the idle image?

            ImageMeta_ idleImageMeta;
            switch (imageTypeIdle) {
                // Left as a switch statement for future compatibility, in case other kinds of images are to be added
                case 0:
                    // Create a wheel-relative rotation
                    idleImageMeta = new Image_Meta_ConstRot_WRel(imageParamIdle);
                    break;
                default:
                    // Create a wheel-relative rotation
                    idleImageMeta = new Image_Meta_ConstRot_WRel(imageParamIdle);
            }
            bwa.setImageIdleMeta(idleImageMeta);

            // Extract the idle image
            ArrayList<Integer> idleImage = new ArrayList<>(numLEDs);
            for (int byteNum = 0; byteNum < numBytesInIncomingImage; byteNum++) {
                // Get a new byte from the incoming list
                byte thisByte = rawByteList.getByte();

                // Split the byte into nibbles and add them to the image
                idleImage.add(ByteMath.getNIntFromByte(thisByte, 4, 0)); // Add the first (lower) nibble
                idleImage.add(ByteMath.getNIntFromByte(thisByte, 4, 4)); // Add the second (upper) nibble
            }

            // Add the main image information to the Bike_Wheel_Animation
            bwa.setImageIdle(idleImage);
        }

        // Finally, return the object that we just constructed
        return bwa;
    }

    // Old Protocol Buffer functions
//    private ArrayList<BikeWheelAnim.Color_> getPaletteBuf() {
//        ArrayList<BikeWheelAnim.Color_> paletteBuf = new ArrayList<>();
//        for (int colorInd = 0; colorInd < numColors(); colorInd++) {
//            paletteBuf.add(mPalette.get(colorInd).getColorBuf());
//        }
//
//        return paletteBuf;
//    }
//
//    public BikeWheelAnim toProtoBuf() {
//        BikeWheelAnim.Builder BWAProto = BikeWheelAnim.newBuilder()
//                // Add number of LEDs
//                .setNumLeds(mImageMain.size())
//
//                // Add the palette
//                .addAllPalette(getPaletteBuf())
//
//                // Add the main Image
//                .addAllImageMain(mImageMain)
//
//                // Add ImageMeta
//                .setImageMainMetaType(BikeWheelAnim.ImageMeta.newBuilder()
//                        .setType(mImageMainMeta.getImageTypeBuf())
//                        .setParameterSet(mImageMainMeta.getImageParameterBuf())
//                );
//
//        // If there is an idle image, add that too
//        if (mImageMainMeta.supportsIdle()) {
//            BWAProto.addAllImageIdle(mImageIdle);
//        }
//
////        BikeWheelAnim bwa_message = BWAProto.build();
////        bwa_message.getSerializedSize();
//
//        return BWAProto.build();
//    }
//
//    static public Bike_Wheel_Animation fromProtoBuf(BikeWheelAnim messageBWA) {
//        // Extract all of the information from the message
//        ArrayList<Color_> palette = Color_.fromProtoBufColor_(messageBWA.getPaletteList()); // Get the palette from the message
//        ArrayList<Integer> imageMain = new ArrayList<>(messageBWA.getImageMainList()); // Get the main image
//        ImageMeta_ imageMeta = ImageMeta_.fromProtoBuf(messageBWA.getImageMainMeta()); // Get the image meta
//
//        ArrayList<Integer> imageIdle;
//        if (imageMeta.supportsIdle()) {
//            imageIdle = new ArrayList<>(messageBWA.getImageIdleList()); // If this pattern supports an idle animation, get it from the message
//        } else {
//            // If there is no idle supported for this pattern, then image idle should be null
//            imageIdle = null;
//        }
//
//        // Compile the new information into a new BWA object, and return it
//        return new Bike_Wheel_Animation(palette, imageMain, imageIdle, imageMeta);
//    }
}

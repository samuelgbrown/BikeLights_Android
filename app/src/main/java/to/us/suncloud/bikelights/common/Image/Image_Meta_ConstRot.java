package to.us.suncloud.bikelights.common.Image;

import to.us.suncloud.bikelights.common.Constants;
//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim.ImageMeta.ImageType;
//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim.ImageMeta.ImageMetaParameter;

// TODO: BIG: Change the way that Images are interpretted!!  There should be wheel-relative rotation, ground-relative rotation, and the spinner (the idle image can only have wheel-relative motion)
public class Image_Meta_ConstRot extends ImageMeta_ {
    private int rotationSpeed;

    @Override
    public boolean supportsIdle() {
        return true; // Needs an idle animation
    }

    public Image_Meta_ConstRot(int rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    public Image_Meta_ConstRot(Image_Meta_ConstRot otherImageMeta) {
        this.rotationSpeed = otherImageMeta.getRotationSpeed();
    }

    @Override
    public ImageMeta_ clone() {
        return new Image_Meta_ConstRot(this);
    }

    public int getRotationSpeed() {
        return rotationSpeed;
    }

    public void setRotationSpeed(int rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

//    public void incrementRotationSpeed() {
//        rotationSpeed++;
//    }
//
//    public void decrementRotationSpeed() {
//        rotationSpeed--;
//    }

    @Override
    public int getImageType() {
        return Constants.IMAGE_CONSTROT;
    }

    @Override
    public byte getImageTypeByte() {
        // TODO: Change this
        return 0;
    }

    //    @Override
//    public ImageType getImageTypeBuf() {
//        return ImageType.CONST_ROT;
//    }
//
//    @Override
//    public ImageMetaParameter getImageParameterBuf() {
//        return ImageMetaParameter.newBuilder()
//                .setP1(getRotationSpeed())
//                .build();
//    }

    @Override
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if (obj instanceof Image_Meta_ConstRot) {
            isEqual = getRotationSpeed() == ((Image_Meta_ConstRot) obj).getRotationSpeed();
        }

        return isEqual;
    }
}

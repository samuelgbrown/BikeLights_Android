package to.us.suncloud.bikelights.common.Image;

import to.us.suncloud.bikelights.common.Constants;
//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim.ImageMeta.ImageType;
//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim.ImageMeta.ImageMetaParameter;

public class Image_Meta_Spinner extends ImageMeta_ {
    private int inertia;

    @Override
    public boolean supportsIdle() {
        return false; // Doesn't need an idle animation
    }

    public Image_Meta_Spinner (int inertia) {
        this.inertia = inertia;
    }

    public Image_Meta_Spinner(Image_Meta_Spinner otherImageMeta) {
        this.inertia = otherImageMeta.getInertia();
    }

    @Override
    public ImageMeta_ clone() {
        return new Image_Meta_Spinner(this);
    }

    public int getInertia() {
        return inertia;
    }

    public void setInertia(int inertia) {
        if (inertia > 0) {
            this.inertia = inertia;
        }
    }

    @Override
    public String getImageDescription() {
        return "Image moves like a \"Spinner\" Hubcap";
    }

    //    public void incrementInertia() {
//        inertia++;
//    }
//
//    public void decrementInertia() {
//        inertia--;
//    }

    @Override
    public int getImageType() {
        return Constants.IMAGE_SPINNER;
    }

    @Override
    public byte getImageTypeByte() {
        return 2;
    }

    //    @Override
//    public ImageType getImageTypeBuf() {
//        return ImageType.SPINNER;
//    }
//
//    @Override
//    public ImageMetaParameter getImageParameterBuf() {
//        return ImageMetaParameter.newBuilder()
//                .setP1(getInertia())
//                .build();
//    }

    @Override
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if (obj instanceof Image_Meta_Spinner) {
            isEqual = getInertia() == ((Image_Meta_Spinner) obj).getInertia();
        }

        return isEqual;
    }
}

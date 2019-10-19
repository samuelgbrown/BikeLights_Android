package to.us.suncloud.bikelights.common.Image;

import to.us.suncloud.bikelights.common.Constants;
//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim.ImageMeta.ImageType;
//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim.ImageMeta.ImageMetaParameter;

// An abstract ImageMeta_ class that defines some Image type that has a constant rotation speed, whether that movement speed is relative to the ground or to the wheel.
public abstract class Image_Meta_ConstRot_ extends ImageMeta_ {
    protected int rotationSpeed;

    @Override
    public boolean supportsIdle() {
        return true; // Needs an idle animation
    }

    public Image_Meta_ConstRot_(int rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    public int getRotationSpeed() {
        return rotationSpeed;
    }

    public void setRotationSpeed(int rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
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
}

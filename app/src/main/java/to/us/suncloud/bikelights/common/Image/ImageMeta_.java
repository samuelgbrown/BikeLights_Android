package to.us.suncloud.bikelights.common.Image;

import java.io.Serializable;

//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim;
//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim.ImageMeta.ImageType;
//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim.ImageMeta.ImageMetaParameter;

public abstract class ImageMeta_ implements Serializable {
    abstract public int getImageType();
//    abstract public ImageType getImageTypeBuf();
//    abstract public ImageMetaParameter getImageParameterBuf();
    abstract public ImageMeta_ clone();
    abstract public boolean supportsIdle(); // Is an idle animation required for slow speeds?
    abstract public boolean equals(Object obj);
    abstract public byte getImageTypeByte(); // Get the image type as it should be interpreted in the byte code to be sent to the Arduino
    abstract public String getImageDescription(); // Get a description of how this image moves

//    static public ImageMeta_ fromProtoBuf(BikeWheelAnim.ImageMeta messageIM) {
//        ImageType type = messageIM.getType();
//
//        ImageMeta_ imageMeta_Out;
//        switch (type) {
//            case SPINNER:
//                imageMeta_Out = new Image_Meta_Spinner(messageIM.getParameterSet().getP1());
//                break;
//            case CONST_ROT:
//                imageMeta_Out = new Image_Meta_ConstRot_(messageIM.getParameterSet().getP1());
//                break;
//            default:
//                imageMeta_Out = new Image_Meta_ConstRot_(messageIM.getParameterSet().getP1());
//        }
//
//        return imageMeta_Out;
//    }
}

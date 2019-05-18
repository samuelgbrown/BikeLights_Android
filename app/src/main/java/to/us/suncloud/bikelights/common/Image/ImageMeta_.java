package to.us.suncloud.bikelights.common.Image;

import java.io.Serializable;

import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim.ImageMeta.ImageType;
import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim.ImageMeta.ImageMetaParameter;

public abstract class ImageMeta_ implements Serializable {
    abstract public int getImageType();
    abstract public ImageType getImageTypeBuf();
    abstract public ImageMetaParameter getImageParameterBuf();
    abstract public ImageMeta_ clone();
    abstract public boolean supportsIdle(); // Is an idle animation required for slow speeds? TODO: Implement a way to modify the idle animation!!!!
    abstract public boolean equals(Object obj);
}

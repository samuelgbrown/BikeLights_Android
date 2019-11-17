package to.us.suncloud.bikelights.common.Image;

import to.us.suncloud.bikelights.common.Constants;

public class Image_Meta_ConstRot_WRel extends Image_Meta_ConstRot_ {
    public Image_Meta_ConstRot_WRel(int rotationSpeed) {
        super(rotationSpeed);
    }

    public Image_Meta_ConstRot_WRel(Image_Meta_ConstRot_WRel otherImageMeta) {
        super(otherImageMeta.getRotationSpeed());
    }

    @Override
    public int getImageType() {
        return Constants.IMAGE_CONSTROT_WREL;
    }

    @Override
    public ImageMeta_ clone() {
        return new Image_Meta_ConstRot_WRel(this);
    }

    @Override
    public String getImageDescription() {
        return "Image moves with a constant speed relative to the wheel";
    }

    @Override
    public byte getImageTypeByte() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if (obj instanceof Image_Meta_ConstRot_WRel) {
            isEqual = getRotationSpeed() == ((Image_Meta_ConstRot_WRel) obj).getRotationSpeed();
        }

        return isEqual;
    }
}
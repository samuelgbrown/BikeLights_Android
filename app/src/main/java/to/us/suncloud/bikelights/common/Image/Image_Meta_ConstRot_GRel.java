package to.us.suncloud.bikelights.common.Image;

import to.us.suncloud.bikelights.common.Constants;

public class Image_Meta_ConstRot_GRel extends Image_Meta_ConstRot_ {

    public Image_Meta_ConstRot_GRel(int rotationSpeed) {
        super(rotationSpeed);
    }

    public Image_Meta_ConstRot_GRel(Image_Meta_ConstRot_GRel otherImageMeta) {
        super(otherImageMeta.getRotationSpeed());
    }

    @Override
    public int getImageType() {
        return Constants.IMAGE_CONSTROT_GREL;
    }

    @Override
    public ImageMeta_ clone() {
        return new Image_Meta_ConstRot_GRel(this);
    }

    @Override
    public String getImageDescription() {
        return "Image moves with a constant speed relative to the ground";
    }

    @Override
    public byte getImageTypeByte() {
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if (obj instanceof Image_Meta_ConstRot_GRel) {
            isEqual = getRotationSpeed() == ((Image_Meta_ConstRot_GRel) obj).getRotationSpeed();
        }

        return isEqual;
    }
}

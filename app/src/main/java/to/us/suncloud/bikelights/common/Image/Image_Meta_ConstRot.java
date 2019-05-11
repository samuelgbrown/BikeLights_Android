package to.us.suncloud.bikelights.common.Image;

import to.us.suncloud.bikelights.common.Constants;

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
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if (obj instanceof Image_Meta_ConstRot) {
            isEqual = getRotationSpeed() == ((Image_Meta_ConstRot) obj).getRotationSpeed();
        }

        return isEqual;
    }
}

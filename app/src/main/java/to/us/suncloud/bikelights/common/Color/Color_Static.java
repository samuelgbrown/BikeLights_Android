package to.us.suncloud.bikelights.common.Color;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;

import to.us.suncloud.bikelights.common.Constants;
import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim;

public class Color_Static extends Color_ {
    private colorObj c = new colorObj();

    public Color_Static() {
    }

    public Color_Static(Color_Static otherColor) {
        setColorObj(otherColor.getColorObj());
    }

    @Override
    public String getDescription() {
        return "Static color";
    }

    @Override
    public int getColorType() {
        return Constants.COLOR_STATIC;
    }

    @Override
    public AnimatorSet modColor_Animator(Object obj, AnimatorSet oldAnimSet, String param) {
        // Prepare the animator set to be filled again
        oldAnimSet.removeAllListeners();
        oldAnimSet.cancel();
        AnimatorSet animSet = new AnimatorSet();

        int thisColor = c.getColorInt();
        ObjectAnimator thisAnim = ObjectAnimator.ofArgb(obj, param, thisColor, thisColor);
        thisAnim.setDuration(1); // Make the "animation" 1 ms long...no idea if this actually changes anything...

        animSet.playSequentially(thisAnim); // Play the "animation"
        animSet.start();

        return animSet;
    }

    @Override
    public int getNumColors() {
        return 1;
    }

    @Override
    public boolean setColorObj(colorObj newColorObj, int index) {
        c = newColorObj; // Change the colorObj
        return true;
    }

    public void setColorObj(colorObj c) {
        setColorObj(c, 0);
    }

    @Override
    public colorObj getColorObj(int index) {
        return new colorObj(c);
    }

    public colorObj getColorObj() {
        return getColorObj(0);
    }

    @Override
    public Color_ clone() {
        return new Color_Static(this);
    }

    @Override
    public BikeWheelAnim.Color_ getColorBuf() {
        return BikeWheelAnim.Color_.newBuilder()
                .addColorObjs(c.getColorObjBuf())
                .setType(BikeWheelAnim.Color_.ColorType.STATIC)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (o instanceof Color_Static) {
            isEqual = getColorObj().equals(((Color_Static) o).getColorObj());
        }

        return isEqual;
    }
}

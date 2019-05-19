package to.us.suncloud.bikelights.common.Color;

import android.animation.AnimatorSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim;

public abstract class Color_ implements Serializable {
    private String name = "";

    Color_() {

    }

    public String getName() {
        return name;
    }

    public abstract String getDescription();

    public abstract int getColorType();

    public abstract BikeWheelAnim.Color_ getColorBuf();

    public abstract int getNumColors();

    public abstract AnimatorSet modColor_Animator(Object obj, AnimatorSet animSet, String param); // Create an animator object using the ImageView passed to this function.

    //    public abstract colorObj getStaticColor(int index);  // Make this Color_ change the color of the ImageView to that of the colorObj at the index-th position of the array (or 0th position if Static)
    public abstract boolean setColorObj(colorObj newColorObj, int index); // Change the colorObj at a certain index in this Color_

    public abstract colorObj getColorObj(int index); // Change the colorObj at a certain index in this Color_

    public abstract Color_ clone(); // Require the Colors to implement a clone function

    public abstract boolean equals(Object o); // Children must implement equality function

    static public Color_ fromProtoBufColor_(BikeWheelAnim.Color_ messageColor_) {
        BikeWheelAnim.Color_.ColorType type = messageColor_.getType();

        // Depending on what type of Color_ is stored, create a different Color_ object
        Color_ newColor;
        switch (type) {
            case STATIC:
                newColor = new Color_Static(colorObj.fromProtoBuf(messageColor_.getColorObjs(0)));
                break;
            case D_TIME:
                newColor = new Color_dTime(Color_d.fromProtoBufColorObjMeta(messageColor_.getColorObjsList()));
                break;
            case D_VEL:
                newColor = new Color_dVel(Color_d.fromProtoBufColorObjMeta(messageColor_.getColorObjsList()));
                break;
            default:
                newColor = new Color_Static(colorObj.fromProtoBuf(messageColor_.getColorObjs(0)));
                break;
        }

        return newColor;
    }

    static public ArrayList<Color_> fromProtoBufColor_(List<BikeWheelAnim.Color_> messageColor_Array) {
        ArrayList<Color_> color_ArrayOut = new ArrayList<>();

        for (int colorInd = 0; colorInd < messageColor_Array.size(); colorInd++) {
            color_ArrayOut.add(Color_.fromProtoBufColor_(messageColor_Array.get(colorInd)));
        }

        return color_ArrayOut;
    }
}
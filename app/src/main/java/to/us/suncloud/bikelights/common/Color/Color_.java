package to.us.suncloud.bikelights.common.Color;

import android.animation.AnimatorSet;

import java.io.Serializable;

import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim;

public abstract class Color_ implements Serializable {
    private String name = "";

    Color_() {

    }

    public String getName() {return name;}
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
}
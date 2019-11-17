package to.us.suncloud.bikelights.common.Color;

import android.graphics.Color;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim;

public class colorObj implements Serializable {
    private static final float MAX_VAL = 255;
    private static final float MAX_WHITE_RATIO = .8f; // The maximum ratio between a current color and the maximum value a color can be (MAX_VAL) that the white value can increase the color to (i.e. a [0 0 0 255] will be increased to [128 128 128 0] if this value is .5)
    private byte r;
    private byte g;
    private byte b;
    private byte w;

    public colorObj() {
        r = 0;
        g = 0;
        b = 0;
        w = 0;
    }

    public colorObj(int[] list) {
        r = (byte) list[0];
        g = (byte) list[1];
        b = (byte) list[2];
        w = (byte) list[3];
    }

    public colorObj(colorObj c) {
        r = (byte) c.getR();
        g = (byte) c.getG();
        b = (byte) c.getB();
        w = (byte) c.getW();
    }

//    public colorObj (List<Byte> list) {
//        r = list.get(0);
//        g = list.get(1);
//        b = list.get(2);
//        w = list.get(3);
//    }

    public colorObj (List<Byte> list) {
        r = list.get(0);
        g = list.get(1);
        b = list.get(2);
        w = list.get(3);
    }

    public colorObj scale(float brightnessScale) {
        // This function will return a copy of this colorObj whose values are scaled by a brightness value (0 1]
        if (brightnessScale >= 1) {
            return this;
        } else if (brightnessScale <= 0) {
            return new colorObj();
        }

        colorObj output = new colorObj();
        output.setR(Math.round(brightnessScale * (float) getR()));
        output.setG(Math.round(brightnessScale * (float) getG()));
        output.setB(Math.round(brightnessScale * (float) getB()));
        output.setW(Math.round(brightnessScale * (float) getW()));

        return output;
    }

    public int getR() {
        return ((int) r) & 0xff;
    }

    public void setR(int r) {
        this.r = (byte) r;
    }

    public int getG() {
        return ((int) g) & 0xff;
    }

    public void setG(int g) {
        this.g = (byte) g;
    }

    public int getB() {
        return ((int) b) & 0xff;
    }

    public void setB(int b) {
        this.b = (byte) b;
    }

    public int getW() {
        return ((int) w) & 0xff;
    }

    public void setW(int w) {
        this.w = (byte) w;
    }

    public int getColorInt() {
        return getRGBEquivalent().getRGBColorInt();
    }

    private int getRGBColorInt() {
        return Color.rgb(getR(), getG(), getB());
    }

// New byte-level manipulation code
    public List<Byte> toByteList() {
        // Return a byte list that represents the color as unsigned 8-bit integers
        List<Byte> returnList = new ArrayList<Byte>(4); // Preallocate 4 bytes for the ArrayList
        returnList.add(r);
        returnList.add(g);
        returnList.add(b);
        returnList.add(w);

        return returnList;
    }

    // Old Protocol Buffer functions
//    public BikeWheelAnim.Color_.ColorObj getColorObjBuf() {
//        return BikeWheelAnim.Color_.ColorObj.newBuilder()
//                .setR(getR())
//                .setG(getG())
//                .setB(getB())
//                .setW(getW())
//                .build();
//    }
//
//    static public colorObj fromProtoBuf(BikeWheelAnim.Color_.ColorObj messageCO) {
//        int[] RGBW = {messageCO.getR(), messageCO.getG(), messageCO.getB(), messageCO.getW()};
//        return new colorObj(RGBW);
//    }
//
//    static public ArrayList<colorObj> fromProtoBuf(List<BikeWheelAnim.Color_.ColorObj> messageCOArray) {
//        ArrayList<colorObj> colorObjArrayOut = new ArrayList<>();
//
//        for (int colorInd = 0; colorInd < messageCOArray.size(); colorInd++) {
//            colorObjArrayOut.add(fromProtoBuf(messageCOArray.get(colorInd)));
//        }
//
//        return colorObjArrayOut;
//    }

    private colorObj getRGBEquivalent() {
        // Get the equivalent to this colorObj using only RGB value
        // This is SUPER hacky, but it's really just for display, not really meant to look exactly right, just be a representation
        colorObj outputC = new colorObj();

        float ratio = MAX_WHITE_RATIO * ((float) getW() / MAX_VAL); // Use the white value to scale the remaining colors
        outputC.setR(getR() + Math.round(ratio * (MAX_VAL - (float) getR())));
        outputC.setG(getG() + Math.round(ratio * (MAX_VAL - (float) getG())));
        outputC.setB(getB() + Math.round(ratio * (MAX_VAL - (float) getB())));

        return outputC;
    }

//    // From user Kaikz on Stack Overflow
//    public int getColorHex() {
//        String as = pad(Integer.toHexString(0));
//        String rs = pad(Integer.toHexString(getR()));
//        String gs = pad(Integer.toHexString(getG()));
//        String bs = pad(Integer.toHexString(getB()));
//        String hex = as + rs + gs + bs;
//        return Integer.parseInt(hex, 16);
//    }

//    // From user Kaikz on Stack Overflow
//    private static String pad(String s) {
//        return (s.length() == 1) ? "0" + s : s;
//    }

    @Override
    public boolean equals(Object obj) {
        try {
            colorObj otherC = (colorObj) obj;
            int otherR = otherC.getR();
            int otherG = otherC.getG();
            int otherB = otherC.getB();
            int otherW = otherC.getW();

            // Check if all values are equal
            return (otherR == getR() && otherG == getG() && otherB == getB() && otherW == getW());

        } catch (Error e) {
            return false;
        }
    }
}

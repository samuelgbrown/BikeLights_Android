package to.us.suncloud.bikelights.common.Color;

import java.util.List;

import to.us.suncloud.bikelights.common.ByteMath;
import to.us.suncloud.bikelights.common.Constants;
//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim;

public class Color_dTime extends Color_d {
    public Color_dTime() {
        super();
    }

    public Color_dTime(Color_dTime otherColor) {
        setC(otherColor.getC());
        setNextID(otherColor.getNextID());
    }

    public Color_dTime(List<colorObjMeta> colorObjList) {
        setC(colorObjList);
    }

    @Override
    public String getDescription() {
        return "Time Dependent Color";
    }

    @Override
    public int getColorType() {
        return Constants.COLOR_DTIME;
    }

    @Override
    public Color_ clone() {
        return new Color_dTime(this);
    }

    @Override
    int getTStandardDistance() {
        return 1000; // The standard difference in T between new shades of this color
    }

    // New byte level manipulation functions
    @Override
    byte getColor_Header() {
        return 0x01;
    }

    @Override
    List<Byte> getTByteCode(int i) {
        return ByteMath.putIntToByteArray(getColorObjMeta(i).getT());
    }

    // Old Protocol Buffer functions
//    @Override
//    public BikeWheelAnim.Color_ getColorBuf() {
//        return BikeWheelAnim.Color_.newBuilder()
//                .addAllColorObjs(getAllColorObjBufs())
//                .setType(BikeWheelAnim.Color_.ColorType.D_TIME)
//                .build();
//    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (o instanceof Color_dTime) {
            isEqual = super.equals(o);
        }

        return isEqual;
    }

    //    @Override
//    int getIncrementT() {
//        return 1000; // Add 1000ms (1 second) to each new colorObjMeta
//    }
}

package to.us.suncloud.bikelights.common.Color;

import java.util.List;

import to.us.suncloud.bikelights.common.ByteMath;
import to.us.suncloud.bikelights.common.Constants;
//import to.us.suncloud.bluetoothproto.BluetoothProto.BluetoothMessage.BikeWheelAnim;

public class Color_dVel extends Color_d {
    public Color_dVel() {
        super();
    }

    public Color_dVel(Color_dVel otherColor) {
        setC(otherColor.getC());
        setNextID(otherColor.getNextID());
    }

    public Color_dVel(List<colorObjMeta> colorObjList) {
        setC(colorObjList);
    }

    @Override
    public String getDescription() {
        return "Speed Dependent Color";
    }

    @Override
    public int getColorType() {
        return Constants.COLOR_DSPEED;
    }

    @Override
    public Color_ clone() {
        return new Color_dVel(this);
    }

    @Override
    int getTStandardDistance() {
        return 120; // The standard distance in T between new shades of this color
    }

    // New byte level manipulation functions
    @Override
    byte getColor_Header() {
        return 0x02;
    }

    @Override
    List<Byte> getTByteCode(int i) {
        return ByteMath.putFloatToByteArray(getColorObjMeta(i).getT());
    }

    // Old Protocol Buffer functions
//    @Override
//    public BikeWheelAnim.Color_ getColorBuf() {
//        return BikeWheelAnim.Color_.newBuilder()
//                .addAllColorObjs(getAllColorObjBufs())
//                .setType(BikeWheelAnim.Color_.ColorType.D_VEL)
//                .build();
//    }
}


package to.us.suncloud.bikelights.common.Color;

import to.us.suncloud.bikelights.common.Constants;

public class Color_dVel extends Color_d {
    public Color_dVel() {
        super();
    }

    public Color_dVel(Color_dVel otherColor) {
        setC(otherColor.getC());
        setNextID(otherColor.getNextID());
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
    int getTScale() {
        return 100; // One unit of T for the Color_dVel represents an LED/sec, which can be represented 1:100 in animation time
    }

//    @Override
//    int getIncrementT() {
//        return 10; // Add 10 units to each new colorObjMeta
//    }
}

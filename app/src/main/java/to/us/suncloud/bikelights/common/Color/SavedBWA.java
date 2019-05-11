package to.us.suncloud.bikelights.common.Color;

import java.io.Serializable;

// This is a simple wrapper class for a Bike_Wheel_Animation (BWA), which also contains a name for the saved BWA
public class SavedBWA implements Serializable {
    private String saveName;
    private Bike_Wheel_Animation BWA;

    public SavedBWA(String saveName, Bike_Wheel_Animation BWA) {
        this.saveName = saveName;
        this.BWA = BWA;
    }

    public String getSaveName() {
        return saveName;
    }

    public void setSaveName(String saveName) {
        this.saveName = saveName;
    }

    public Bike_Wheel_Animation getBWA() {
        return BWA;
    }

    public void setBWA(Bike_Wheel_Animation BWA) {
        this.BWA = BWA;
    }

    @Override
    public boolean equals(Object obj) {
        // When comparing two SavedBWA, only compare the underlying BWA, not the string

        boolean isEqual = false;
        if (obj instanceof SavedBWA) {
            isEqual = BWA.equals(((SavedBWA) obj).getBWA());
        }

        return isEqual;
    }
}

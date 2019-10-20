package to.us.suncloud.bikelights.common.Bluetooth;

import to.us.suncloud.bikelights.common.ByteMath;

public class BTC_PowerState {
    boolean powerState = false;

    public BTC_PowerState(boolean powerState) {
        this.powerState = powerState;
    }

    public boolean getPowerState() {
        return powerState;
    }

    public BluetoothByteList toByteList() {
        // Convert this BTC_PowerState object to a byte list
        // Create the byte list that will eventually be sent
        BluetoothByteList rawByteList = new BluetoothByteList(BluetoothByteList.ContentType.Power, false);

        // Send the power state as a boolean
        byte powerByte = 0;
        powerByte = ByteMath.putBoolToByte(powerByte, powerState, 7);
        rawByteList.addByte(powerByte);

        // Return the byte list so that it can be set
        return rawByteList;
    }

    public static BTC_PowerState fromByteList(BluetoothByteList rawByteList) {
        // Extract a BTC_Battery object from the incoming byte list

        rawByteList.startReading(); // Set the pointer to the beginning of the byte list

        // Get the battery amount as an int (0-255)
        byte powerByte = rawByteList.getByte();
        boolean powerState = ByteMath.getBoolFromByte(powerByte, 7);

        // Create a new BTC_Battery object from the extracted information, and return it
        return new BTC_PowerState(powerState);
    }
}

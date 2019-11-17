package to.us.suncloud.bikelights.common.Bluetooth;

import to.us.suncloud.bikelights.common.ByteMath;

// A class for receiving information about the battery in the Arduino.  Input only
public class BTC_Battery {
    private int battery = 0; // Will be 0-255

    public BTC_Battery(int battery) {
        this.battery = battery;
    }

    public int getBattery() {
        return battery;
    }


    public static BTC_Battery fromByteList(BluetoothByteList rawByteList) {
        // Extract a BTC_Battery object from the incoming byte list

        rawByteList.startReading(); // Set the pointer to the beginning of the byte list

        // Get the battery amount as an int (0-255)
        byte batteryByte = rawByteList.getByteAndIter();
        int battery = ByteMath.getNIntFromByte(batteryByte, 8, 0);

        // Create a new BTC_Battery object from the extracted information, and return it
        return new BTC_Battery(battery);
    }
}

package to.us.suncloud.bikelights.common.Bluetooth;

import java.util.List;

import to.us.suncloud.bikelights.common.ByteMath;

// A class for receiving information about the storage in the Arduino.  Input only
public class BTC_Storage {
    private int remaining = 0;
    private int total = 0;

    private BTC_Storage(int remaining, int total) {
        this.remaining = remaining;
        this.total = total;
    }

    public int getTotal() {
        return total;
    }

    public int getRemaining() {
        return remaining;
    }

    public static BTC_Storage fromByteList(BluetoothByteList rawByteList) {
        // Extract a BTC_Storage object from the incoming byte list

        rawByteList.startReading(); // Set the pointer to the beginning of the byte list

        // Get the remaining and total storage capacity, both as longs
        List<Byte> remainingBytes = rawByteList.getNextBytes(4);
        int remaining = (int) ByteMath.getLongIntFromByteArray(remainingBytes);

        List<Byte> totalBytes = rawByteList.getNextBytes(4);
        int total = (int) ByteMath.getLongIntFromByteArray(totalBytes);

        // Create a new BTC_Storage object from the extracted information, and return it
        return new BTC_Storage(remaining, total);
    }
}

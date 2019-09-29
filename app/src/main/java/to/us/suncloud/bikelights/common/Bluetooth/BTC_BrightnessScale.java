package to.us.suncloud.bikelights.common.Bluetooth;

import java.util.List;

import to.us.suncloud.bikelights.common.ByteMath;

// A class for receiving information about the LED brightness scale in the Arduino.  Input and output
public class BTC_BrightnessScale {
    private float brightnessScale = 1f; //  Will be (0 - 1]

    private BTC_BrightnessScale(float brightnessScale) {
        this.brightnessScale = brightnessScale;
    }

    public float getBrightnessScale() {
        return brightnessScale;
    }

    public static BTC_BrightnessScale fromByteList(BluetoothByteList rawByteList) {
        // Extract a BTC_BrightnessScale from the incoming byte list

        rawByteList.startReading(); // Set the pointer to the beginning of the byte list

        // Get the brightness scale, as a float
        List<Byte> brightnessScaleBytes = rawByteList.getNextBytes(4);
        float brightnessScale = ByteMath.getFloatFromByteArray(brightnessScaleBytes);

        // Create a new BTC_BrightnessScale from the extracted information, and return it
        return new BTC_BrightnessScale(brightnessScale);
    }

    public BluetoothByteList toByteList() {
        // Convert this BTC_BrightnessScale object to a byte list
        // Create the byte list that will eventually be sent
        BluetoothByteList rawByteList = new BluetoothByteList(BluetoothByteList.ContentType.Brightness, false);

        // Send the brightness scale as a float
        rawByteList.addBytes(ByteMath.putFloatToByteArray(brightnessScale));

        // Return the byte list so that it can be set
        return rawByteList;
    }
}

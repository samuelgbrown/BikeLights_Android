package to.us.suncloud.bikelights.common.Bluetooth;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

// A class that will hold a raw byte list that will be automatically processed to be ready to send to the Arduino
public class BluetoothByteList{
    private List<Byte> rawByteList; // The byte list as it is created by the Android code.  Must be processed before being sent to the Arduino
    private ContentType content; // The type of content that is being held in the rawByteList (used to create the header at the beginning of each message




    public enum ContentType {
        BWA,
        Kalman,
        Brightness,
        Storage,
        Battery
    }

    public BluetoothByteList(ContentType content) {
        this.content = content; // Create a new byte list that will be interpreted as the content
    }

    public void addByte(Byte newByte) {
        rawByteList.add(newByte);
    }

    public void addBytes(List<Byte> newBytes) {
        rawByteList.addAll(newBytes);
    }

    public Byte getByte(int i) {
        return rawByteList.get(i);
    }

    public List<Byte> getBytes(int i, int length) {
        return rawByteList.subList(i, length); // TODO: Check that this is the right call to the function...
    }

    public List<Byte> getAllBytes() {
        return rawByteList;
    }

    public List<Byte> getProcessedByteList() {
        // TODO: Do the processing on this byte list, adding in headers every 64 bytes as needed.  Possible change to only return one 64-byte segment at a time?
        return new ArrayList<Byte>();
    }
}

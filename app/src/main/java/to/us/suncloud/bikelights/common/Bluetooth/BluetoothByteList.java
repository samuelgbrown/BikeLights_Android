package to.us.suncloud.bikelights.common.Bluetooth;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import to.us.suncloud.bikelights.common.ByteMath;

// A class that will hold a raw byte list that will be automatically processed to be ready to send to the Arduino.  Pretty redundant with the ByteBuffer java.nio class, but...well, I already wrote most of this by the time I found that, and also, since C++ is on one side and Java is on the other, I like being pretty close to the byte-level mechanics of the stream, and I don't really trust ByteBuffer...
public class BluetoothByteList {
    private static final String TAG = "BluetoothByteList";

    // Set some parameters for the message and header sizes
    public static final int NUM_TOTAL_BYTES_PER_MESSAGE = 32; //64;
    public static final int NUM_BYTES_PER_HEADER = 2;
    public static final int NUM_RAW_BYTES_PER_MESSAGE = NUM_TOTAL_BYTES_PER_MESSAGE - NUM_BYTES_PER_HEADER;
    public static final int MAX_NUM_MESSAGES = 2^4; // The current maximum number of messages that can be incorporated as a single communication; allows for 992 raw bytes of data

    private List<Byte> rawByteList = new ArrayList<>(); // The byte list as it is created by the Android code.  Must be processed before being sent to the Arduino
    private ContentType content; // The type of content that is being held in the rawByteList (used to create the header at the beginning of each message
    private boolean request; // Is the message that is being held in this byte list a request for more information, or does it contain information to send to the Arduino?
    private boolean confirmation; // Is the message currently being held in this byte list a confirmation message sent from Arduino?
    private int readingBytePointerLoc = 0; // For reading, where is the location of the pointer to the next byte to be read.
    private int writingMessagePointerLoc = 0; //  For writing, what is the number of the next message.
    private int totalRequiredMessages = 0; // For writing, how many 64-byte messages will be needed to send this?  Calculated in startWriting()
    private boolean isWriting = false; // If the message is actively being written.  If so, the message cannot be modified, only read


    public enum ContentType {
        BWA,
        Kalman,
        Brightness,
        Storage,
        Battery,
        Power,
        SP_Confirm // A special type that refers to any kind of confirmation message (this will be the type of any message with the CF bit set)
    }

    public BluetoothByteList() {} // A blank byte list

    public BluetoothByteList(byte[] incomingProcessedByteList, int numBytes) {
        // Extract the "raw" byte list from the "processed" byte list that we just received from the Arduino (i.e. strip off connection-level metadata and store the rest)

        // Extract the header information
        request = ByteMath.getBoolFromByte(incomingProcessedByteList[0], 7);
        confirmation = ByteMath.getBoolFromByte(incomingProcessedByteList[0], 3);

        if (confirmation) {
            content = ContentType.SP_Confirm; // If this is a confirmation message, then record so (Ugh, I was tired when I wrote this...maybe this is dumb)
        } else {
            content = contentValToType(ByteMath.getNIntFromByte(incomingProcessedByteList[0], 3, 4)); // Otherwise, record the content type
        }

        // Save the rest of the raw byte data to the rawByteList
        for (int byteNum = 0;byteNum < (numBytes - 1); byteNum++) {
            // Go through the byte list (except the first byte, which was just the header), and get the raw data (encapsulating as a Byte in the process)
            rawByteList.add(incomingProcessedByteList[byteNum + 1]);
        }
    }

    public BluetoothByteList(ContentType content, boolean request) {
        this.content = content; // Create a new byte list that will be interpreted as the content
        this.request = request;
    }

    public boolean isRequest() {
        return request;
    }

    public boolean isConfirmation() {
        return confirmation;
    }

    public ContentType getContentType() {
        return content;
    }

    public void addByte(Byte newByte) {
        if (!isWriting) {
            // Only add bytes if the byte list is not being written
            rawByteList.add(newByte);
        }
    }

    public void addBytes(List<Byte> newBytes) {
        if (!isWriting) {
            // Only add bytes if the byte list is not being written
            rawByteList.addAll(newBytes);
        }
    }

    public Byte getByte(int i) {
        return rawByteList.get(i);
    }

    public Byte getByteAndIter() {
        // Get the next byte, and iterate the pointer location
        return rawByteList.get(getPointerAndIter());
    }

    public Byte getByte() {
         return getByte(readingBytePointerLoc);
    }

    public List<Byte> getBytes(int i, int length) {
        // Does NOT iterate any pointer
        int correctedLength = Math.min(length, rawByteList.size() - i); // Ensure that we do not try to get too many items from the list
        return rawByteList.subList(i, i + correctedLength);
    }

    public List<Byte> getBytesAndIter(int length) {
        // Iterates the byte pointer
        int minLength = rawByteList.size() - readingBytePointerLoc;
        int correctedLength = Math.min(length, minLength); // Ensure that we do not try to get too many items from the list
        int startInd = getPointerAndIter(correctedLength); // Get the starting location for the sub list
        return rawByteList.subList(startInd, startInd + correctedLength);
    }

    public List<Byte> getNextMessage() {
        // Iterates both the byte and message pointers
        writingMessagePointerLoc++; // Iterate the message writing counter
        return getBytesAndIter(NUM_RAW_BYTES_PER_MESSAGE); // Returns the next 62 bytes (or less, if there are fewer than that) in the raw byte list, and iterates the byte pointer
    }

    private int getPointerAndIter() {
        return getPointerAndIter(1);
    }

    private int getPointerAndIter(int length) {
        // Iterate readingBytePointerLoc, but return the old value
        int oldPointerVal = readingBytePointerLoc;
        readingBytePointerLoc += length;
        return oldPointerVal;
    }

    public void startReading() {
        readingBytePointerLoc = 0;
        isWriting = false; // We are not writing the byte list, and are allowed to add data to the message
    }

    public boolean isDoneReading() {
        return readingBytePointerLoc >= rawByteList.size(); // If the readingBytePointer location is pointing past the rawByteList (i.e. equal to the size)
    }

    public int startWriting() {
        // Set the pointer location to zero
        readingBytePointerLoc = 0; // Initialize the byte pointer (used to traverse the raw byte list)
        writingMessagePointerLoc = 0; // Initialize the message point (used to keep track of message location in the byte list)
        isWriting = true; // We have now started writing the message, and do not want to add any more information (or else totalRequiredMessages may become incorrect)
        return getTotalRequiredMessages();
    }

    public int getTotalRequiredMessages() {
        // Get the total number of required messages to fit the byte list as it currently exists into 64-byte messages
        if (request) {
            // If it is a request for information, only one message is required
            totalRequiredMessages = 1;
        } else {
            totalRequiredMessages = (int) Math.ceil(((double) rawByteList.size()) / ((double) NUM_RAW_BYTES_PER_MESSAGE));
        }

        if (totalRequiredMessages > MAX_NUM_MESSAGES) {
            // Uh-oh...
            Log.e(TAG, "Got a connection that requires a very large number of messages (" + totalRequiredMessages + " messages, while our maximum is " + MAX_NUM_MESSAGES + ".");
        }

        return totalRequiredMessages;
    }

    public int getReadingBytePointerLoc() {
        return readingBytePointerLoc;
    }

    public int getWritingMessagePointerLoc() {
        return writingMessagePointerLoc;
    }

    public void setReadingBytePointerLoc(int readingBytePointerLoc) {
        this.readingBytePointerLoc = readingBytePointerLoc;
    }

    public void setWritingMessagePointerLoc(int writingMessagePointerLoc) {
        this.writingMessagePointerLoc = writingMessagePointerLoc;
    }

    public List<Byte> getAllRawBytes() {
        return rawByteList;
    }

    public byte[] getNextProcessedByteList() {
        // Do the processing on this byte list, adding in headers every 64 bytes as needed.
        // Returns a single 64-byte message, with header included. Subsequent calls will return each following message
        // Initialize a full write out with startWriting(), which will return the total number of 64-byte blocks to be sent, and each call to this function will return the next block, which the calling function can send once we hear back from the Arduino that it's ready
        // Create a byte list to send
        List<Byte> outList = new ArrayList<>(NUM_TOTAL_BYTES_PER_MESSAGE); // Preallocate the output byte list

            if (isWriting) {
            // If the byte list is writing mode, then write the information to an output list and send it along
            // If the byte list is not in writing mode, then output a blank byte list

            // First, write the header
            outList.addAll(generateHeader());

            // Next, send the body of the message
            // If it is a request for information, simply return the header, which is enough information to send to the Arduino for the request to be complete
            if (!request) {
                // Otherwise, populate the byte list
                outList.addAll(getNextMessage()); // This will iterate both writingMessagePointerLoc and readingBytePointerLoc
            }
        }

        // Convert the Byte List into a byte array (requires unboxing of each individual Byte, so must be done in a for loop)
        byte[] outBytes = new byte[outList.size()]; // Isn't is nice to be able to define arrays on the stack without needing to worry about memory leaks?
        for (int i = 0;i < outList.size(); i++) {
            outBytes[i] = outList.get(i);
        }
        return outBytes;
    }

    private List<Byte> generateHeader() {
        // Generate a header for writing the processed byte list
        List<Byte> headerBytes = new ArrayList<>(2);

        // First, send the header which is 2 bytes long
        byte header1 = 0;
        header1 = ByteMath.putBoolToByte(header1, request, 7);
        header1 = ByteMath.putDataToByte(header1, contentTypeToVal(content), 3, 4);
        byte header2 = 0;
        header2 = ByteMath.putDataToByte(header2, (byte) totalRequiredMessages, 4, 4);
        header2 = ByteMath.putDataToByte(header2, (byte) writingMessagePointerLoc, 4, 0);

        headerBytes.add(header1);
        headerBytes.add(header2);

        return  headerBytes;
    }

    public static byte contentTypeToVal(ContentType contentTypeIn) {
        switch (contentTypeIn) {
            case BWA:
                return 0;
            case Kalman:
                return 1;
            case Brightness:
                return 2;
            case Storage:
                return 3;
            case Battery:
                return 4;
            case Power:
                return 5;
            default:
                return 0;
        }
    }

    public static ContentType contentValToType(int contentValIn) {
        switch (contentValIn) {
            case 0:
                return ContentType.BWA;
            case 1:
                return ContentType.Kalman;
            case 2:
                return ContentType.Brightness;
            case 3:
                return ContentType.Storage;
            case 4:
                return ContentType.Battery;
            case 5:
                return ContentType.Power;
            default:
                return ContentType.BWA;
        }
    }

    public static String contentTypeToString(ContentType contentTypeIn) {
        switch (contentTypeIn) {
            case BWA:
                return "Wheel Animation";
            case Kalman:
                return "Kalman Data";
            case Brightness:
                return "Brightness Data";
            case Storage:
                return "Storage Data";
            case Battery:
                return "Battery Data";
            case Power:
                return "Power State";
            case SP_Confirm:
                return "Confirmation";
            default:
                return "Unknown Data";
        }
    }
}

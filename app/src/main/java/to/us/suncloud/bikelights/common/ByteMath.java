package to.us.suncloud.bikelights.common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

// This static class will handle all byte- and bit-level manipulations that need to be done, specifically for constructing and interpreting the byte-code that is sent to/received from the Arduino
public class ByteMath {
    // Writing data to the byte
    static public byte putZerosToByte(byte byteVal, int clearSize, int bitLoc) {
        return (byte) (byteVal & ~(((1 << clearSize) - 1) << bitLoc));
    }

    static public byte putMaskExceptDataToByte(byte byteVal, int dataSize, int bitLoc) {
        return (byte) (byteVal & (((1 << dataSize) - 1) << bitLoc));
    }

    static public byte putDataToByte(byte byteVal, byte data, int dataSize, int bitLoc) {
        byteVal = putZerosToByte(byteVal, dataSize, bitLoc);      // First, clear the indicated bits in the destination, so that new data can be placed there
        data = putMaskExceptDataToByte(data, dataSize, 0); // Second, clear all except the indicated bits in data so that no spare bits interfere with placing the data
        return (byte) (byteVal | (data << bitLoc));                               // Data is clear everywhere except the data, and byteDest is clear in the data location, so only the data pattern will be written to byteDest
    }

    static public byte putBoolToByte(byte byteVal, boolean data, int bitLoc) {
        if (data) {
            // If we are writing a 1 to the bit at bitLoc
            return (byte) (byteVal | 1 << bitLoc);
        } else {
            // If we are writing a 0 to the bit at bitLoc
            return (byte) (byteVal & ~(1 << bitLoc));
        }
    }

    static public List<Byte> putIntToByteArray(long longVal) {
        List<Byte> newLongArray = new ArrayList<>(4); // Preallocate 4 bytes

        // Only take the bottom 4 bytes!(Using a long to preserve unsigned nature
        for (int byteNum = 0;byteNum < 4; byteNum++) {
            newLongArray.add((byte) (longVal >>> (8*byteNum)));
        }

        return newLongArray;
    }

    static public List<Byte> putFloatToByteArray(float floatVal) {
        // Get the bits from the float in the correct endian order
        return putIntToByteArray(Integer.reverse(Float.floatToRawIntBits(floatVal)));
    }

    // Reading data from the byte
    static public boolean getBoolFromByte(byte dataByte, int bitPos) {
        // Extract a single bit from a byte, and interpret it as a boolean.
        // bitPos is the number of the bit (starting with the right-most bit as 0)
        return (1 & (dataByte >> bitPos)) != 0; // Shift the byte bitPos bits to the right, so the bit of interest is in the least-significant position.  Then, mask it with 1 (0b00000001)
    }

    static public int getNIntFromByte(byte dataByte, int intSize, int firstBitLoc) {
        // Get some n-sized unsigned integer from a byte.
        // NOTE: intsize MUST be less than 8!!!  No checks will be performed!!!
        // For example, an unsigned int is defined by the 3 bytes indicated by X in 0xoooXXXoo, this function should be called as getNUIntFromByte(0xoooXXXoo, 3, 2)
        byte bitMask = (byte) ((1 << intSize) - 1); // Bit-mask generation idea from John Gietzan, on Stack Exchange (https://stackoverflow.com/a/1392065)
        return ((dataByte >> firstBitLoc) & bitMask) & 0xFF; // Add the & 0xFF to do unsigned conversion magic (only take the first byte)
    }

//    static public List<Integer> getUSBytesFromSBytes(List<Byte> dataBytes) {
//        // Get an unsigned byte from a signed byte
//        List<Integer> outputList = new ArrayList<>(dataBytes.size());
//        for (int byteInd = 0;byteInd < dataBytes.size(); byteInd++) {
//            outputList.add(getUSByteFromSByte(dataBytes.get(byteInd)));
//        }
//
//        return outputList;
//    }
//
//    static public int getUSByteFromSByte(byte dataByte) {
//        return getNIntFromByte(dataByte, 8, 0);
//    }

    static public long getLongIntFromByteArray(List<Byte> byteArray) {
        // Transfer the LOWER 4 BYTES to a new 8-byte array, and fill the upper 4 bytes with 0's
        byte[] b = new byte[8];
        for (int i = 0; i < 4; i++) {
            b[i] = byteArray.get(i);
        }

        // Annoying and stupid and hacky...
        for (int i = 0; i < 4; i++) {
            b[i + 4] = 0;
        }

        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getLong() & 0xffffffffL; // Return the lower 8 bytes of the resulting long
    }

    static public float getFloatFromByteArray(List<Byte> byteArray) {
        return Float.intBitsToFloat(Integer.reverse((int) getLongIntFromByteArray(byteArray)));
    }

    // Test function (do not use!)
    public static void bytePrint(byte byteIn) {
        // If testing, remember to include at the top:
        //import java.util.List;
        //import java.util.ArrayList;
        //import java.nio.ByteBuffer;
        //import java.nio.ByteOrder;

        System.out.print("[");
        for (int i = 7;i >= 0;i--) {
            System.out.print(1 & (byteIn >> i));
            if (i != 0) {
                System.out.print(" ");
            }
        }
        System.out.println("]");
    }
}
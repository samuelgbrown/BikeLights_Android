package to.us.suncloud.bikelights.common;

import java.util.ArrayList;
import java.util.List;

// This static class will handle all byte- and bit-level manipulations that need to be done, specifically for constructing and interpreting the byte-code that is sent to/received from the Arduino
public class ByteMath {
    static public byte putZerosToByte(byte byteVal, int clearSize, int bitLoc) {
        return (byte) (byteVal & ~(((0x01 << clearSize) - 0x01) << bitLoc));
    }

    static public byte putMaskExceptDataToByte(byte byteVal, int dataSize, int bitLoc) {
        return (byte) (byteVal & (((0x01 << dataSize) - 0x01) << bitLoc));
    }

    static public byte putDataToByte(byte byteVal, byte data, int dataSize, int bitLoc) {
        byteVal = putZerosToByte(byteVal, dataSize, bitLoc);      // First, clear the indicated bits in the destination, so that new data can be placed there
        data = putMaskExceptDataToByte(data, dataSize, 0); // Second, clear all except the indicated bits in data so that no spare bits interfere with placing the data
        return (byte) (byteVal | (data << bitLoc));                               // Data is clear everywhere except the data, and byteDest is clear in the data location, so only the data pattern will be written to byteDest
    }

    static public byte putBoolToByte(byte byteVal, boolean data, int bitLoc) {
        if (data) {
            // If we are writing a 1 to the bit at bitLoc
            return (byte) (byteVal | 0x01 << bitLoc);
        } else {
            // If we are writing a 0 to the bit at bitLoc
            return (byte) (byteVal & ~(0x01 << bitLoc));
        }
    }

    // TODO: Finish writing code to convert floats and longs to byte Lists
    static public List<Byte> putIntToByteArray(int longVal) {
        List<Byte> newLongArray = new ArrayList<Byte>(4); // Preallocate 4 bytes

        for (int byteNum = 0;byteNum < 4; byteNum++) {
            newLongArray.add((byte) (longVal >>> (8*byteNum)));
        }

        return newLongArray;
    }

    static public List<Byte> putFloatToByteArray(float floatVal) {
        return putIntToByteArray(Float.floatToRawIntBits(floatVal));
    }

}
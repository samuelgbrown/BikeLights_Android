package to.us.suncloud.bikelights.common;

public class Constants {
    // Message types for Handler
    public static final int MESSAGE_READ = 0;
    public static final int MESSAGE_WRITE = 1;
    public static final int MESSAGE_TOAST = 2;

    // Meta-data for message types for Handler
    public static final String TOAST = "toast";

    // Wheel "IDs" for front or rear (or "none", used if a device is not connected to either wheel)
    public static final int ID_REAR = 3;
    public static final int ID_FRONT = 4;
    public static final int ID_NONE = 5;

    // Message for a new device connection state for use
    public static final int MESSAGE_CONNECTED = 6;
    public static final int MESSAGE_DISCONNECTED = 7;
    public static final int MESSAGE_CONNECT_FAILED = 8;

    // Types of actions to take when altering a device's state
    public static final int ACTION_DISCONNECT = 9;
    public static final int ACTION_SWITCH = 10; // Switch which wheel this device is assigned to
    public static final int ACTION_REPLACE = 11; // Remove the current device assigned to this wheel, and add a new one

    // Types of Color_'s
    public static final int COLOR_STATIC = 12;
    public static final int COLOR_DTIME = 13;
    public static final int COLOR_DSPEED = 14;
//    public static final int COLOR_D = 15;

    // Types of ImageMeta_'s
    public static final int IMAGE_IDLE = 16;
    public static final int IMAGE_CONSTROT_ = 17;
    public static final int IMAGE_CONSTROT_GREL = 18;
    public static final int IMAGE_CONSTROT_WREL = 19;
    public static final int IMAGE_SPINNER = 20;

    public static final int SINGLE_SLICE = 21;
    public static final int REPEAT_SLICE = 22;
    public static final int REPEAT_PATTERN = 23;
}

# BikeLights_Android

"BikeLights" is a system to generate speed-sensitive light patterns on the edge of a bike's wheel.  It uses an Android device to design a pattern, and an Arduino system to display it on the wheel.

TODO:  Link to video

[See Arduino portion here](https://github.com/samuelgbrown/BikeLights_Arduino)

Android portion made using [Android Studio](https://developer.android.com/studio)!

# Instructions for Installation
Note that BikeLights only works on Android devices.

To install "BikeLights":
You can either install the APK, or build the project from source.
1. (Alternate 1) **Install** the BikeLights APK
    1. **Download** the APK from the [releases section of this repository](https://github.com/samuelgbrown/BikeLights_Android/releases).
    2. **Transfer** the APK file to your phone (via USB cable, Bluetooth, etc.).
    3. **Install** the APK file.
        1. **Locate** the APK file on your file (probably in Downloads, or a similar directory).
        2. **Tap** the file name to begin the installation. Note: If you have never installed an APK from the given file manager and/or on that specific device before, the phone will prompt you to give permission and/or change your Settings to allow installing the app.
2. (Alternate 2) **Build** the BikeLights app from source
    1. **Download** and install [Android Studio](https://developer.android.com/studio) on your personal computer.
    2. **Clone** this repository, using the the "VCS" menu in Android Studio.
    3. **Open** the newly cloned repo.
    4. **Run** the app on your Android Device, by [following these steps](https://developer.android.com/studio/run/device).

# Instructions for Use
Note: The BikeLights Android app is only useful if you have the BikeLights system installed on your bike!  To install the BikeLights system on your bike, follow the [Instructions for Use](https://github.com/samuelgbrown/BikeLights_Arduino/blob/master/README.md#instructions-for-use) and [Adapting for Your Own Bike](https://github.com/samuelgbrown/BikeLights_Arduino/blob/master/README.md#adapting-for-your-own-bike) on the [BikeLights_Android](https://github.com/samuelgbrown/BikeLights_Arduino/blob/master/README.md) page.

To run "BikeLights":
1. **Connect** the BikeLights app to your BikeLights Arduino system
    1. **Click** the bluetooth icon on the menu at the top of the welcome screen
    2. **Find* the BikeLights bluetooth device, and click it
    3. **Select** which wheel (front or back) this device is mounted on.
    4. **Confirm** that the information from the BikeLights system has been loaded in the BikeLights app
2. **Design** a lighting pattern
    1. **Click** the wheel on the large bike icon that the BikeLights system is mounted on.
    2. **Define** your color palette
        1. **Click** the green "+" button at the bottom of the screen
        2. **Choose** to add a "Static", "Time-based" or "Speed-based" color
        3. **Define** the new color.
            1. Every color square represents a single RGB(W) value.  It can be defined by typing a value for RGB(W) between 0-255, or by clicking the color square and choosing a color from the picket.
            2. Static colors are defined by a single color square.
            3. Time- and Speed-based colors are defined by an array of color squares, as well as a time/speed and a Blend type
                1. For Time-based colors, the time represents the time (in ms) at which the color will be displayed in a cycle (the cycle starts at 0ms and ends at the time value of the last color square in the list).
                2. For Speed-based colors, the speed represents the speed (LED/s) at which the color will be displayed.
                3. The Blend type allows you to choose how the values in the color square array are interpolated for times/speeds that are not explicitly listed.
        4. **Apply** the settings of the new color
        5. **Change** existing colors by clicking the gear icon
        6. **Generate** a color palette that will be used to "paint" the bike wheel
    3. **Define** the Pattern
        1. **Choose** which kind of rotation pattern you would like from the top drop-down menu
            1. "Wheel-Relative Position" means that the pattern's rotation will be defined relative to the wheel's reference frame.
            2. "Ground-Relative Position" means that the patterns' rotation will be defined relative to the ground's reference frame.  In the configuration, the pattern will appear to spin at constant rotation (default is no rotation) to outside observers. (Valid only for "main" patterns)
            3. "Spinner" means that the pattern on the wheel will act like a "[spinner hubcap](https://en.wikipedia.org/wiki/Spinner_(wheel))"
        2. **Choose** to start by defining either the "main" pattern (how the wheel looks during normal operation) or the "idle" pattern (how the wheel looks when moving too slowly to get speed information, valid only for Wheel- and Ground- relative position modes).
            1. You should define both before you send the light pattern to the BikeLights system!
        4. **Define** the rotation rate (valid for Wheel- and Ground- relative position modes only).
        5. **Click** the "[Modify Pattern...]" drop-down menu
        6. **Choose** whether you would like to add a single slice of color, a repeated slice of color, or a repeated pattern on the wheel
            1. These are only presets for the next screen, meaning you may change your choice later
        7. **Define** how you would like change the pattern
            1. Note that only LEDs that are highlighted in blue in the pattern preview will be affected (all other LEDs will not be modified, so you can "stack" Pattern modifications).
            2. "Slice Width" is the width of the slice(s) of color you will add, in LEDs.
            3. "Rotation" is the offset current slice, in LEDs.
            4. The "Repeat" menu lets you copy the slice you are defining multiple times around the wheel.
            5. "Specify Pattern" lets you use multiple colors to define the slice.
                1. When the slider is off, the area below allows you to choose the single color to apply to the entire new slice.
                2. When the slider is on, the area below turns into a line of boxes the same length as the slice width.  Each box represents an LED.  You may select individual boxes (click to toggle) or click the "check" or "X" buttons to select all/none, respectively.  Once you have chosen some LEDs, click the "[Assign seleted...]" drop-down menu to choose which color to assign the selected LEDs.
        8. **Apply** the new pattern to the wheel
    4. (Optional) **Bookmark** the Pattern
        1. **Click** the bookmark icon at the top of the screen and define a name for your new Pattern so you can come back to it later.
        2. **Recover** a previous bookmark from the main menu by clicking the "Use bookmark" button under the wheel you would like you modify.
    6. **Save** the Pattern
        1. You can save by either hitting the save icon at the top, or by navigating back to the previous screen and hitting "Save" on the pop-up.
3. **Send** the Pattern
    1. **Click** the upload icon (upwards arrow) on the Bluetooth banner across the top of the main screen
4. **Control** the Pattern
    1. "LEDs On/Off" lets you toggle the LEDs easily.
    2. The "Brightness" slider lets you easily modify the brightness of all LEDs at the same time
    3. The "Battery" gauge shows how much battery is left on the BikeLights system (**currently not functional**)
    4. The "Storage" guage shows how much storage is remaining on the Arduino RAM.  Higher complexity Patterns will use more storage.
5. **Enjoy** your BikeLights system!

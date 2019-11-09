package to.us.suncloud.bikelights;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import to.us.suncloud.bikelights.common.BWA_Manager.SavedBWAFragment;
import to.us.suncloud.bikelights.common.Bluetooth.BTC_Battery;
import to.us.suncloud.bikelights.common.Bluetooth.BTC_BrightnessScale;
import to.us.suncloud.bikelights.common.Bluetooth.BTC_Kalman;
import to.us.suncloud.bikelights.common.Bluetooth.BTC_PowerState;
import to.us.suncloud.bikelights.common.Bluetooth.BTC_Storage;
import to.us.suncloud.bikelights.common.Bluetooth.BluetoothByteList;
import to.us.suncloud.bikelights.common.Bluetooth.BluetoothInteractionThread;
import to.us.suncloud.bikelights.common.Bluetooth.BluetoothMasterHandler;
import to.us.suncloud.bikelights.common.Color.Bike_Wheel_Animation;
import to.us.suncloud.bikelights.common.Color.SavedBWA;
import to.us.suncloud.bikelights.common.Constants;
import to.us.suncloud.bikelights.common.Bluetooth.NoBluetoothDialog;
import to.us.suncloud.bikelights.common.Bluetooth.ConnectionManager;
import to.us.suncloud.bikelights.common.WheelView.wheelSelectDrawable;
import to.us.suncloud.bikelights.common.Bluetooth.BluetoothFragment;
import to.us.suncloud.bikelights.common.colorpickerview.ColorPickerDialog;
import to.us.suncloud.bikelights.common.colorpickerview.ColorPickerPreferenceManager;

public class MainActivity extends AppCompatActivity implements NoBluetoothDialog.NoBluetoothInt, BluetoothMasterHandler.HandlerInt, SavedBWAFragment.savedBWAListener {
    public static final int REQUEST_ENABLE_BT = 1001;
    public static final int REQUEST_WHEEL_ACTIVITY = 1002;

    private static final String TAG = "Main Activity";

    public static final String SHARED_PREF_FILE = "BIKE_LIGHTS";

    private static final String CONNECTION_MANAGER = "connection_manager";
    public static final String BIKE_WHEEL_ANIMATION = "BIKE_WHEEL_ANIMATION";
    public static final String COLOR_LIST_R = "COLOR_LIST_R";
    public static final String COLOR_LIST_F = "COLOR_LIST_F";
    public static final String WHEEL_LOCATION = "WHEEL_LOCATION";

    private Bike_Wheel_Animation bwa_rear; // List of Color_'s that are a part of the rear wheel
    private Bike_Wheel_Animation bwa_front; // List of Color_'s that are a part of the front wheel

    // Keep a copy of the most up to date Arduino versions of the BWA's, as well, to check if the two devices are synch'd
    private Bike_Wheel_Animation arduino_last_bwa_rear; // List of Color_'s that are a part of the rear wheel
    private Bike_Wheel_Animation arduino_last_bwa_front; // List of Color_'s that are a part of the front wheel
    private int arduino_last_brightness_rear; // The last brightness known to be assigned to the rear wheel
    private int arduino_last_brightness_front; // The last brightness known to be assigned to the front wheel

    private View wheelSelectView; // The image view that draws the bike on screen
    private wheelSelectDrawable wheelDrawable; // The drawable object that controls the colors of the wheels
    private FragmentManager fm = getSupportFragmentManager(); // Get the fragment manager, to be able to display the bluetooth dialog
    private BluetoothAdapter bA = BluetoothAdapter.getDefaultAdapter();
    boolean mCurrentBluetoothStatus = false; // Is the bluetooth adapter on?
    private Button rearWheelButton; // The button that starts the rear wheel's activity
    private Button frontWheelButton; // The button that starts the front wheel's activity
    private ConstraintLayout bluetoothStatusTextBorder; // Border outline of the bluetooth status, indicates the status of the bluetooth connection
    private ImageButton bluetoothUpload; // Button to upload the BWA to the Arduino
    private ImageButton bluetoothDownload; // Button to download the BWA from the Arduino
    private TextView bluetoothStatusText; // Status text view to indicate the status of the bluetooth connection and BWAs

    private Switch rearPowerSwitch;
    private Switch frontPowerSwitch;

    private ProgressBar frontStorageProg;
    private ProgressBar rearStorageProg;

    private ProgressBar frontBatteryProg;
    private ProgressBar rearBatteryProg;

    private SeekBar frontBrightnessBar;
    private SeekBar rearBrightnessBar;

    private Button rearAssignBookmark;
    private Button frontAssignBookmark;


    private ConnectionManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get the active Settings
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialize the Bike_Wheel_Animation's (must be done here because Resources are required...I think)
        int numLEDs = Integer.parseInt(preferences.getString("num_leds", Integer.toString(getResources().getInteger(R.integer.num_leds))));
        bwa_rear = new Bike_Wheel_Animation(numLEDs); // List of Color_'s that are a part of the rear wheel
        bwa_front = new Bike_Wheel_Animation(numLEDs); // List of Color_'s that are a part of the front wheel

        // TODO: Complete recovery
        if (savedInstanceState != null) {
            // Check if there are any saved states to recover the activity
            if (savedInstanceState.containsKey(COLOR_LIST_R)) {
                // Retrieve the rear color list
                bwa_rear = (Bike_Wheel_Animation) savedInstanceState.getSerializable(COLOR_LIST_R);
            }

            if (savedInstanceState.containsKey(COLOR_LIST_F)) {
                // Retrieve the front color list
                bwa_front = (Bike_Wheel_Animation) savedInstanceState.getSerializable(COLOR_LIST_F);
            }
        }

        // Set the wheel select view's background drawable (to draw the wheels)
        wheelSelectView = findViewById(R.id.wheelSelectView);
        wheelDrawable = new wheelSelectDrawable(wheelSelectView);
        wheelSelectView.setBackground(wheelDrawable);

        // Assign the callback functions to the widgets on screen
        rearPowerSwitch = findViewById(R.id.rearSwitch);
        frontPowerSwitch = findViewById(R.id.frontSwitch);
        rearAssignBookmark = findViewById(R.id.rear_assign_bookmark);
        frontAssignBookmark = findViewById(R.id.front_assign_bookmark);
        frontStorageProg = findViewById(R.id.storageProgFront);
        rearStorageProg = findViewById(R.id.storageProgRear);
        frontBatteryProg = findViewById(R.id.batteryProgFront);
        rearBatteryProg = findViewById(R.id.batteryProgRear);
        frontBrightnessBar = findViewById(R.id.brightnessBarFront);
        rearBrightnessBar = findViewById(R.id.brightnessBarRear);

        rearPowerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Attempt to change the power state of this wheel
                onPowerStateChange(isChecked, Constants.ID_REAR);
            }
        });

        frontPowerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Attempt to change the power state of this wheel
                onPowerStateChange(isChecked, Constants.ID_FRONT);
            }
        });

        rearAssignBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SavedBWAFragment savedBWAFragment = SavedBWAFragment.newInstance(Constants.ID_REAR);
//                savedBWAFragment.show(fm, "Assign Bookmark Dialog");
                savedBWAFragment.tryToShow(getApplicationContext(), fm, "Assign Bookmark Dialog");
            }
        });

        frontAssignBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SavedBWAFragment savedBWAFragment = SavedBWAFragment.newInstance(Constants.ID_FRONT);
//                savedBWAFragment.show(fm, "Assign Bookmark Dialog");
                savedBWAFragment.tryToShow(getApplicationContext(), fm, "Assign Bookmark Dialog");
            }
        });

        frontBrightnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setBrightness(progress, Constants.ID_FRONT);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        rearBrightnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setBrightness(progress, Constants.ID_REAR);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // Initialize the battery to be out of 255
        frontBatteryProg.setMax(255);
        rearBatteryProg.setMax(255);


        // Start a wheel-view activity when clicking one of the wheel buttons
        frontWheelButton = findViewById(R.id.frontWheelButton);
        rearWheelButton = findViewById(R.id.rearWheelButton);

        frontWheelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWheelViewActivity(bwa_front, Constants.ID_FRONT);
            }
        });

        rearWheelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWheelViewActivity(bwa_rear, Constants.ID_REAR);
            }
        });

        // Check that Bluetooth is enabled, and attempt to turn it on
        setNewBluetoothStatus(false); // Assume the bluetooth is not on/available
        checkBluetooth(true);

        // Create a ConnectionManager (manages connecting and managing of bluetooth connections), and register this Activity as a Handler (receives communication directly from the Bluetooth devices)
        // TODO: If there are any Arduino connections, automatically download the BWA's (in the background), and save them to the Arduino BWA's
        // If savedInstanceState is not null, if it contains the key we want, and if the value of that key is not null...
        if (!(savedInstanceState == null) && savedInstanceState.containsKey(CONNECTION_MANAGER) && !(savedInstanceState.getSerializable(CONNECTION_MANAGER) == null)) {
            // ...then a Connection Manager was found, then save it locally
            mManager = (ConnectionManager) savedInstanceState.getSerializable(CONNECTION_MANAGER);
        } else {
            // ...if not, then no Connection Manager was attached to the incoming bundle, so create a new one
            mManager = new ConnectionManager(this);
        }

        // Register this activity to the connection manager
        mManager.registerHandler(this);

        // Register the Connection Manager's broadcast receiver via this Activity
        registerReceiver(mManager.getBroadcastReceiver(), new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        // Initialize the bluetooth bar at the top
        bluetoothStatusText = findViewById(R.id.bluetoothStatusText);
        bluetoothStatusTextBorder = findViewById(R.id.bluetoothStatusTextBorder);
        bluetoothDownload = findViewById(R.id.bluetoothDownload);
        bluetoothUpload = findViewById(R.id.bluetoothUpload);

        updateBluetoothStatus(); // Update the bluetooth GUI

        // Set the functions of the upload and download buttons
        bluetoothDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // See if we even should be getting the BWAs from the wheels.  If there is anything interesting on the Android app right now, check if the user really wants to go through with the download
                if (!(bwa_front.isEmpty() && bwa_rear.isEmpty())) {
                    // If they are not both blank, then we should confirm with the user that they want to overwrite the current BWA

                    // Build the alert dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                    builder.setTitle(getResources().getString(R.string.dialog_btc_title))
                            .setMessage(getResources().getString(R.string.dialog_btc_message_download))
                            .setPositiveButton(getResources().getString(R.string.dialog_btc_overwrite), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    bluetoothDownloadWheelSettings();
                                }
                            })
                            .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do nothing?
                                }
                            });

                    // Display the dialog
                    builder.create();

                } else {
                    // If they are both blank, then go ahead and load the Arduino BWAs anyway
                    bluetoothDownloadWheelSettings();
                }
            }
        });

        bluetoothUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // See if we even should be sending the BWAs .  If there is anything interesting on the Arduino right now, check if the user really wants to go through with the upload
                if (!(arduino_last_bwa_front.isEmpty() && arduino_last_bwa_rear.isEmpty())) {
                    // If they are not both blank, then we should confirm with the user that they want to overwrite the current BWA

                    // Build the alert dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                    builder.setTitle(getResources().getString(R.string.dialog_btc_title))
                            .setMessage(getResources().getString(R.string.dialog_btc_message_upload))
                            .setPositiveButton(getResources().getString(R.string.dialog_btc_overwrite), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    bluetoothUploadWheelSettings(); // Send the data
                                }
                            })
                            .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do nothing?
                                }
                            });

                    // Display the dialog
                    builder.create();

                } else {
                    // If they are both blank, then go ahead and upload the Android BWAs anyway
                    bluetoothUploadWheelSettings(); // Send the data
                }
            }
        });

        // Clear out memory from the ColorPickerPreferenceManager
        ColorPickerPreferenceManager.getInstance(this).clearSavedAllData();

        // Delete the color file from memory (only needs to be temporary storage...maybe)
        File colorFile = new File(getFilesDir(), ColorPickerDialog.SAVED_COLORS);
        colorFile.delete();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // TODO: Check that the number of LEDs from the Settings matches with everything that uses it (BWA...anything else?), then do...something
    }

    private void setBatteryGUIVal(int batteryVal, int wheelLoc) {
        // Get the right progress bar to affect
        ProgressBar barToChange;

        switch (wheelLoc) {
            case Constants.ID_FRONT:
                barToChange = frontBatteryProg;
                break;
            case Constants.ID_REAR:
                barToChange = rearBatteryProg;
                break;
            default:
                barToChange = frontBatteryProg;
        }

        // Set the value of the progress bar
        barToChange.setProgress(batteryVal);
    }

    private void setStorageGUIVal(int curVal, int curMax, int wheelLoc) {
        // Get the right progress bar to affect
        ProgressBar barToChange;

        switch (wheelLoc) {
            case Constants.ID_FRONT:
                barToChange = frontStorageProg;
                break;
            case Constants.ID_REAR:
                barToChange = rearStorageProg;
                break;
            default:
                barToChange = frontStorageProg;
        }

        // Set the maximum and current storage values
        barToChange.setMax(curMax);
        barToChange.setProgress(curVal);
    }

    private void setPowerStateGUIVal(boolean newPowerState, boolean fromUser, int wheelLoc) {
        // Set the GUI to the current power state (will notify the user of new power state, if they didn't cause the change)
        // First, get the objects for this wheel
        Switch powerSwitch;
        switch (wheelLoc) {
            case Constants.ID_FRONT:
                powerSwitch = frontPowerSwitch;
                break;
            case Constants.ID_REAR:
                powerSwitch = rearPowerSwitch;
                break;
            default:
                return;
        }

        // If this was not from the user, see if the user needs to be notified about this
        boolean currentPowerState = powerSwitch.isChecked();
        if (!fromUser && newPowerState != currentPowerState) {
            String newPowerStateStr = newPowerState ? "on" : "off";

            sendWheelToast("LEDs are " + newPowerStateStr, wheelLoc);
        }


        // Next, update the wheel drawable (so the wheel color can change)
        wheelDrawable.setPowerState(newPowerState, wheelLoc);
        wheelSelectView.invalidate();

        // Finally, update the power switch
        // Get the string to change the display to
        String switchString;
        if (newPowerState) {
            switchString = getApplicationContext().getResources().getString(R.string.leds_on);
        } else {
            switchString = getApplicationContext().getResources().getString(R.string.leds_off);
        }

        powerSwitch.setText(switchString);
        powerSwitch.setChecked(newPowerState);
    }


    private void startWheelViewActivity(Bike_Wheel_Animation curBWA, int wheelLocation) {
        startWheelViewActivity(curBWA, wheelLocation, false);
    }

    private void startWheelViewActivity(Bike_Wheel_Animation curBWA, int wheelLocation, boolean bwaFromBookmark) {
        Intent intent = new Intent(getApplicationContext(), WheelViewActivity.class); // Create an intent for a wheel view activity
        intent.putExtra(BIKE_WHEEL_ANIMATION, curBWA); // Send the Wheel View Activity the color list for the rear wheel
        intent.putExtra(WHEEL_LOCATION, wheelLocation); // Send the Wheel View Activity the color list for the rear wheel
        intent.putExtra(WheelViewActivity.BWA_FROM_BOOKMARK, bwaFromBookmark); // Send the Wheel View Activity the color list for the rear wheel
        startActivityForResult(intent, REQUEST_WHEEL_ACTIVITY); // Start the new activity
    }

    private void updateBluetoothStatus() {
        boolean frontSynched = bwa_front.equals(arduino_last_bwa_front);
        boolean readSynched = bwa_rear.equals(arduino_last_bwa_rear);

        int stringID = R.string.bt_status_disconnected;
        int colorID = R.color.red;
        int uploadVis = View.GONE;
        int downloadVis = View.GONE;

        if (frontSynched && readSynched) {
            stringID = R.string.bt_status_synched;
            colorID = R.color.green;

            uploadVis = View.GONE;
            downloadVis = View.GONE;
            // Fully synched
        } else if (frontSynched || readSynched) {
            // Partially synched
            stringID = R.string.bt_status_part_synched;
            colorID = R.color.yellow;

            uploadVis = View.VISIBLE;
            downloadVis = View.VISIBLE;
        } else {
            // Not synchronized
            stringID = R.string.bt_status_desynched;
            colorID = R.color.yellow;

            uploadVis = View.VISIBLE;
            downloadVis = View.VISIBLE;
        }
        // }

        // Set the string and background color for the status indicator
        bluetoothStatusText.setText(stringID);
        bluetoothStatusTextBorder.setBackgroundColor(colorID); // TODO: Check if this works?

        // Set the upload and download button visibilities
        bluetoothUpload.setVisibility(uploadVis);
        bluetoothDownload.setVisibility(downloadVis);
    }

    private void setBrightness(int brightness, int wheelLoc) {
        // Update the GUI's progress
        // Get the seekbar to set, and also set the required brightness value
        SeekBar seekBarToChange;
        switch (wheelLoc) {
            case Constants.ID_FRONT:
                seekBarToChange = frontBrightnessBar;
                arduino_last_brightness_front = brightness;
                break;
            case Constants.ID_REAR:
                seekBarToChange = rearBrightnessBar;
                arduino_last_brightness_rear = brightness;
                break;
            default:
                seekBarToChange = frontBrightnessBar;
                arduino_last_brightness_front = brightness;
        }

        seekBarToChange.setProgress(brightness);
    }

    private void bluetoothDownloadWheelSettings() {
        // Transfer the BWA's from the "last Arduino BWA's" variable to the main BWA
        bwa_front = arduino_last_bwa_front.clone();
        bwa_rear = arduino_last_bwa_rear.clone();

        // Transfer the brightness settings from the last Arduino variables to the main variables
        setBrightness(arduino_last_brightness_front, Constants.ID_FRONT);
        setBrightness(arduino_last_brightness_rear, Constants.ID_REAR);

        // Update the GUI
        updateBluetoothStatus();

        // Send a toast, letting the user know that the BWA's have been updated
        sendToast(getResources().getString(R.string.toast_downloaded_bwa));
    }

    private void bluetoothUploadWheelSettings() {
        // Upload the BWAs and brightness settings from both wheels (TODO: Perhaps, just do one at a time (i.e. can select individual wheels))?

        // Prepare the data to be sent to both wheels
         List<BluetoothByteList> byteListsToSend = new ArrayList<>();
         List<Integer> byteListDestinations = new ArrayList<>();

        byteListsToSend.add(bwa_front.toByteList());
        byteListDestinations.add(Constants.ID_FRONT);

        byteListsToSend.add(new BTC_BrightnessScale(brightnessSeekbarToFloat(Constants.ID_FRONT)).toByteList());
        byteListDestinations.add(Constants.ID_FRONT);

        byteListsToSend.add(bwa_rear.toByteList());
        byteListDestinations.add(Constants.ID_REAR);

        byteListsToSend.add(new BTC_BrightnessScale(brightnessSeekbarToFloat(Constants.ID_REAR)).toByteList());
        byteListDestinations.add(Constants.ID_REAR);

        // Send each piece of data to the wheel consecutively
        mManager.sendBytesToDevice(byteListsToSend, byteListDestinations);
    }

    private float brightnessSeekbarToFloat(int wheelLoc) {
        // Simple function that will convert the brightness bar's progress, an integer scaled from 0-255),to a float scaled 0-1.
        android.widget.SeekBar wheelSeekbar;
        switch (wheelLoc) {
            case Constants.ID_FRONT:
                wheelSeekbar = frontBrightnessBar;
                break;
            case Constants.ID_REAR:
                wheelSeekbar = rearBrightnessBar;
                break;
            default:
                wheelSeekbar = frontBrightnessBar;
        }
        return ((float) wheelSeekbar.getProgress()) / 255;
    }

    private int brightnessFloatToInt(float brightnessFloat) {
        return Math.round(brightnessFloat * 255f);
    }

    public void setBWA(Bike_Wheel_Animation bwa, int wheelLocation) {
        // Change the bike wheel animation of the chosen wheel.  We may need to also set the arduino BWAs if we are getting this straight from the bluetooth connection
        switch (wheelLocation) {
            case Constants.ID_FRONT:
                bwa_front = bwa.clone();
                break;
            case Constants.ID_REAR:
                bwa_rear = bwa.clone();
                break;
            default:
                // Uh-oh...
                Log.e(TAG, "Found unknown wheel location to save BWA");
        }

    }

    public void setArduinoBWA(Bike_Wheel_Animation bwa, int wheelLocation) {
        switch (wheelLocation) {
            case Constants.ID_FRONT:
                arduino_last_bwa_front = bwa.clone();
                break;
            case Constants.ID_REAR:
                arduino_last_bwa_rear = bwa.clone();
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(COLOR_LIST_R, bwa_rear);
        outState.putSerializable(COLOR_LIST_F, bwa_front);
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the broadcast receiver
        unregisterReceiver(mManager.getBroadcastReceiver());

        // Unregister from the ConnectionManager
        mManager.unregisterHandler(this);
    }

    private void checkBluetooth(boolean tryToTurnOn) {
        if (bA == null) {
            // There is no bluetooth adapter, app cannot work
            NoBluetoothDialog noBluetooth = NoBluetoothDialog.newInstance(this);
            noBluetooth.show(fm, "Bluetooth not available");
            return;
        }

        if (!bA.isEnabled()) {
            // If bluetooth is not enabled
            if (tryToTurnOn) {
                // If the program should prompt the user to turn on Bluetooth at this point
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                // Send a toast message that the app will be missing functionality if bluetooth is off
                String msg = "Bluetooth is not enabled, no connection to wheel devices possible.";
                sendToast(msg);
            }
        } else {
            setNewBluetoothStatus(true); // If the bluetooth adapter is enabled, then set the local variable as such
        }

        // Update the display to the user
        updateBluetoothStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // Test the new bluetooth status
                checkBluetooth(false);
                break;
            case REQUEST_WHEEL_ACTIVITY:
                // If the Wheel Activity returned, see what Color_Lists (if any) it produced
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // If the wheel activity saved any data
                    Bundle b = data.getExtras();
                    if (b.containsKey(WHEEL_LOCATION) && b.containsKey(BIKE_WHEEL_ANIMATION)) {
                        // The activity successfully returned the color list associated with the wheel given by WHEEL_LOCATION
                        setBWA((Bike_Wheel_Animation) b.getSerializable(BIKE_WHEEL_ANIMATION), b.getInt(WHEEL_LOCATION));
//                        switch (b.getInt(WHEEL_LOCATION)) {
//                            case Constants.ID_REAR:
//                                bwa_rear = (Bike_Wheel_Animation) b.getSerializable(BIKE_WHEEL_ANIMATION); // Add the included Bike_Wheel_Animation to the rear wheel
//                                break;
//                            case Constants.ID_FRONT:
//                                bwa_front = (Bike_Wheel_Animation) b.getSerializable(BIKE_WHEEL_ANIMATION); // Add the included Bike_Wheel_Animation to the front wheel
//                                break;
//                            default:
//                                // Uh-oh...
//                                Log.e(TAG, "Wheel Activity returned unknown wheel location");
//                        }
                    }
                }

                break;
        }
    }

    private void setNewBluetoothStatus(boolean newBluetoothStatus) {
        // Notify the activity of a new bluetooth status
        mCurrentBluetoothStatus = newBluetoothStatus;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                // Open the settings pane
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_bluetooth:
                // Open the bluetooth menu
                BluetoothFragment bluetoothFragment = BluetoothFragment.newInstance(mManager);
                bluetoothFragment.show(fm, "Bluetooth Dialog");
                return true;
            case R.id.action_bookmarks:
                SavedBWAFragment savedBWAFragment = SavedBWAFragment.newInstance();
//                savedBWAFragment.show(fm, "Bookmarks Manager Dialog");
                savedBWAFragment.tryToShow(getApplicationContext(), fm, "Bookmarks Manager Dialog");
            default:
                // Uh-oh...
                // Do nothing

        }

        return super.onOptionsItemSelected(item);
    }


    // Wheel state update methods
    public void onConnectionStateChange(final int wheelLoc, boolean connected) {
        // Tell the wheelDrawable what the new connection state is (so the wheel color can change)
        switch (wheelLoc) {
            case Constants.ID_FRONT:
                wheelDrawable.setFrontConnectedState(connected);
                break;
            case Constants.ID_REAR:
                wheelDrawable.setRearConnectedState(connected);
                break;
        }

        wheelSelectView.invalidate();

        // Send a toast message that a device has been either connected or disconnected
        sendConnectionToast(connected, wheelLoc);

        if (connected) {
            requestFullUpdateFromWheel(wheelLoc);
        }

        // Update the GUI
        updateBluetoothStatus();
    }

    private void requestFullUpdateFromWheel(final int wheelLoc) {
        // TODO SOON: Call this on both wheels if they exist during onCreate (should be done in a thread or something)

        // This function will spawn a thread that will request all of the relevant information from a wheel

        if (isWheelReady(wheelLoc)) {
            // Create a BluetoothInteractionThread that will consecutively request all of the data that we want
            new BluetoothInteractionThread(mManager) {
                @Override
                public void sendingOperations() {
                    // First, request the Bike wheel animation
                    requestData(BluetoothByteList.ContentType.BWA, wheelLoc);

                    // Then request the Kalman data
                    requestData(BluetoothByteList.ContentType.Kalman, wheelLoc);

                    // Then request the storage data
                    requestData(BluetoothByteList.ContentType.Storage, wheelLoc);

                    // Finally, request the battery data
                    requestData(BluetoothByteList.ContentType.Battery, wheelLoc);
                }

                @Override
                public void timeoutFun(BluetoothByteList.ContentType contentType, int wheelLoc) {
                    // If we have timed out, send an error toast
                    sendWheelToast("Error retrieving " + BluetoothByteList.contentTypeToString(contentType) + ". Skipping.", wheelLoc);
                    Log.e(TAG, "Could not receive " + BluetoothByteList.contentTypeToString(contentType) + " from " + wheelLocToString(wheelLoc) + " wheel.");
                }
            }.run();
        }
    }

    private boolean isWheelReady(int wheelLoc) {
        return mCurrentBluetoothStatus && mManager.isWheelConnected(wheelLoc);
    }

    private void requestUpdateFromWheel(final BluetoothByteList.ContentType content, final int wheelLoc) {
        // Create a BluetoothInteractionThread that will request the data that we want
        if (isWheelReady(wheelLoc)) {
            new BluetoothInteractionThread(mManager) {
                @Override
                public void sendingOperations() {
                    // Request the information
                    requestData(content, wheelLoc);
                }

                @Override
                public void timeoutFun(BluetoothByteList.ContentType contentType, int wheelLoc) {
                    // If we have timed out, send an error toast
                    sendWheelToast("Error retrieving " + BluetoothByteList.contentTypeToString(contentType) + ". Skipping.", wheelLoc);
                    Log.e(TAG, "Could not receive " + BluetoothByteList.contentTypeToString(contentType) + " from " + wheelLocToString(wheelLoc) + " wheel.");
                }
            }.run();
        }
    }


//    public void onRearConnectionStateChange(boolean connected) {
//        // Tell the wheelDrawable what the new connection state is (so the wheel color can change)
//        wheelDrawable.setRearConnectedState(connected);
//        wheelSelectView.invalidate();
//
//        // Send a toast message that a device has been either connected or disconnected
//        sendConnectionToast(connected, Constants.ID_REAR);
//
//        // Old TO_DO: Read the BWA from the newly connected device into the Arduino BWA variable, or get rid of the old saved BWA? Also, update the bluetooth status text.
//
//    }
//
//    public void onFrontConnectionStateChange(boolean connected) {
//        // Tell the wheelDrawable what the new connection state is (so the wheel color can change)
//        wheelDrawable.setFrontConnectedState(connected);
//        wheelSelectView.invalidate();
//
//        // Send a toast message that a device has been either connected or disconnected
//        sendConnectionToast(connected, Constants.ID_FRONT);
//
//        if (connected) {
//            // Old TO_DO: Read the BWA, Kalman, and battery from the newly connected device into the Arduino BWA variable. (How do I do this without the messages running into each other?  Set a "initial connection" variable, so they are rapid-fire if we connecting for the first time?)
//            // Old TO_DO: Also, update the bluetooth status text
//
//        }
//    }

    public void onPowerStateChange(boolean powered, int wheelLoc) {
        // Check that bluetooth is on
        if (!isWheelReady(wheelLoc)) {
            // If this wheel is not connected, do not allow the user to turn on the wheels
            powered = false;

            sendWheelToast("Wheel is not connected, cannot power on", wheelLoc);

            // If the bluetooth adapter is not on, attempt to turn on bluetooth
            if (!mCurrentBluetoothStatus) {
                checkBluetooth(true);
            }
        }

        // Send the power data to the wheel
        mManager.sendBytesToDevice(new BTC_PowerState(powered).toByteList(), wheelLoc);

        setPowerStateGUIVal(powered, true, wheelLoc);
    }

    private void sendConnectionToast(boolean connected, int id) {
        String connectedStr;

        // Get strings for each possible state
        if (connected) {
            connectedStr = "connected";
        } else {
            connectedStr = "disconnected";
        }

        // Create the toast message
        String msg = "Bluetooth device has been " + connectedStr;

        // Send the toast
        sendWheelToast(msg, id);
    }

    private static String wheelLocToString(int wheelLoc) {
        switch (wheelLoc) {
            case Constants.ID_FRONT:
                return "Front";

            case Constants.ID_REAR:
                return "Rear";
            default:
                return "Unknown";
        }
    }

    private void sendWheelToast(String toastStr, int wheelLoc) {
        // Send a toast message pertaining to a particular wheel
        sendToast(wheelLocToString(wheelLoc) + " wheel: " + toastStr);
    }

    private void sendToast(String toastStr) {
        // Use this Activity's Context to send a Toast message
        Toast.makeText(this, toastStr, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNoBluetoothDialogComplete(NoBluetoothDialog fragment) {
        // Dismiss the fragment
        fragment.dismiss();

        // Close the app after the user has seen the no_bluetooth dialog
        finish();
    }

    @Override
    public void receiveSelectedBWA(SavedBWA savedBWA, int wheelLocation) {
        // After selecting a bookmarked BWA to apply to a wheel, open a new activity with the wheel and this new BWA
        startWheelViewActivity(savedBWA.getBWA(), wheelLocation, true);
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Constants.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj; // Message contents
                int readNumBytes = msg.arg1; // Length of the message
                int readWheelID = msg.arg2; // Source of the message

                BluetoothByteList readByteList = new BluetoothByteList(readBuf, readNumBytes);

                // Since we now know what the information was, do different things depending on what we received
                if (readByteList.isRequest()) {
                    BluetoothByteList byteListToSend = new BluetoothByteList();
                    switch (readByteList.getContentType()) {
                        case BWA:
                            switch (readWheelID) {
                                case Constants.ID_FRONT:
                                    byteListToSend = bwa_front.toByteList();
                                    break;
                                case Constants.ID_REAR:
                                    byteListToSend = bwa_rear.toByteList();
                                    break;
                                default:
                                    // Uh-oh...
                                    return;
                            }
                            break;
                        case Kalman:
                            // Get a SharedPreferences object to find the Kalman Data
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

                            // Create a Kalman object from the preferences, and turn it into a byte list
                            byteListToSend = new BTC_Kalman(prefs).toByteList();
                            break;

                        case Brightness:
                            // Get the data from the brightness scale, put it into a BTC_Brightness object, and convert it to a byte list (TODO: Check this is right?)
                            byteListToSend = new BTC_BrightnessScale(brightnessSeekbarToFloat(readWheelID)).toByteList();

                            break;

                        case Power:
                            switch (readWheelID) {
                                case Constants.ID_FRONT:
                                    byteListToSend = new BTC_PowerState(frontPowerSwitch.isChecked()).toByteList();
                                    break;
                                case Constants.ID_REAR:
                                    byteListToSend = new BTC_PowerState(rearPowerSwitch.isChecked()).toByteList();
                                    break;
                                default:
                                    // Uh-oh...
                                    return;
                            }
                            break;

                        default:
                            // Uh-oh...
                            return;
                    }

                    // Send the byteListToSend object!
                    mManager.sendBytesToDevice(byteListToSend, readWheelID);

                } else {
                    // If this is not a request, then save the information that was sent to the Android
                    switch (readByteList.getContentType()) {
                        case BWA:
                            // Create a BWA from the byte list that we just received
                            Bike_Wheel_Animation newBWA = Bike_Wheel_Animation.fromByteList(readByteList);

                            // Save the new BWA
                            setArduinoBWA(newBWA, readWheelID);

                            break;
                        case Kalman:
                            // Create a Kalman object from the byte list that we just received
                            BTC_Kalman newKalman = BTC_Kalman.fromByteList(readByteList);

                            // TODO SOON: Do something with the Kalman object...error check?  Perhaps send a toast if anything is not right

                            break;

                        case Battery:
                            BTC_Battery newBattery = BTC_Battery.fromByteList(readByteList);

                            // Update the GUI
                            setBatteryGUIVal(newBattery.getBattery(), readWheelID);

                            break;
                        case Storage:
                            BTC_Storage newStorage = BTC_Storage.fromByteList(readByteList);

                            // Update the GUI
                            setStorageGUIVal(newStorage.getRemaining(), newStorage.getTotal(), readWheelID);
                            break;
                        case Brightness:
                            BTC_BrightnessScale newBrightnessScale = BTC_BrightnessScale.fromByteList(readByteList); // Convert the byte list to a Brightness scale
                            setBrightness(brightnessFloatToInt(newBrightnessScale.getBrightnessScale()), readWheelID); // Set the brightness scale to the corresponding wheel
                            break;
                        case Power:
                            BTC_PowerState newPowerState = BTC_PowerState.fromByteList(readByteList);

                            // Update the GUI
                            setPowerStateGUIVal(newPowerState.getPowerState(), false, readWheelID);
                            break;
                    }
                }

                break;
            case Constants.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj; // Message contents
                int writeNumBytes = msg.arg1; // Length of the message
                int writeWheelID = msg.arg2; // Source of the message
                String writeString = new String(writeBuf);
                // TO_DO: Process the written string (if needed)

                break;
            case Constants.MESSAGE_TOAST:
                sendWheelToast(msg.getData().getString(Constants.TOAST), msg.arg2);
                break;
            case Constants.MESSAGE_CONNECTED:
                // Perform the needed actions if a new device is added to the front or rear
                onConnectionStateChange(msg.arg2, true);

//                switch (msg.arg2) {
//                    case Constants.ID_FRONT:
//                        onFrontConnectionStateChange(true);
//                        break;
//                    case Constants.ID_REAR:
//                        onRearConnectionStateChange(true);
//                        break;
//                }
                break;
            case Constants.MESSAGE_DISCONNECTED:
                // Perform the needed actions if a new device is added to the front or rear
                onConnectionStateChange(msg.arg2, true);

//                switch (msg.arg2) {
//                    case Constants.ID_FRONT:
//                        onFrontConnectionStateChange(false);
//                        break;
//                    case Constants.ID_REAR:
//                        onRearConnectionStateChange(false);
//                        break;
//                }
                break;
            case Constants.MESSAGE_BLUETOOTH_STATE_CHANGE:
                checkBluetooth(false); // If the bluetooth state has changed, keep track of it
        }
    }
}
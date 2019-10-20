package to.us.suncloud.bikelights;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import to.us.suncloud.bikelights.common.BWA_Manager.SavedBWAFragment;
import to.us.suncloud.bikelights.common.Bluetooth.BTC_Battery;
import to.us.suncloud.bikelights.common.Bluetooth.BTC_BrightnessScale;
import to.us.suncloud.bikelights.common.Bluetooth.BTC_Kalman;
import to.us.suncloud.bikelights.common.Bluetooth.BTC_Storage;
import to.us.suncloud.bikelights.common.Bluetooth.BluetoothByteList;
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

    private Button rearAssignBookmark;
    private Button frontAssignBookmark;


    private ConnectionManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize the Bike_Wheel_Animation's (must be done here because Resources are required...I think)
        bwa_rear = new Bike_Wheel_Animation(getResources().getInteger(R.integer.num_leds)); // List of Color_'s that are a part of the rear wheel
        bwa_front = new Bike_Wheel_Animation(getResources().getInteger(R.integer.num_leds)); // List of Color_'s that are a part of the front wheel

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

        rearPowerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Attempt to change the power state of this wheel
                onRearPowerStateChange(isChecked);
            }
        });

        frontPowerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Attempt to change the power state of this wheel
                onFrontPowerStateChange(isChecked);
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
                                    bluetoothDownloadBWAs();
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
                    bluetoothDownloadBWAs();
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
                                    bluetoothUploadBWAs(); // Send the data
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
                    bluetoothUploadBWAs(); // Send the data
                }
            }
        });

        // Clear out memory from the ColorPickerPreferenceManager
        ColorPickerPreferenceManager.getInstance(this).clearSavedAllData();

        // Delete the color file from memory (only needs to be temporary storage...maybe)
        File colorFile = new File(getFilesDir(), ColorPickerDialog.SAVED_COLORS);
        colorFile.delete();
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
        // TODO: Probably need to change this on bluetooth status change, as well?
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

    private void bluetoothDownloadBWAs() {
        // Transfer the BWA's from the "last Arduino BWA's" variable to the main BWA
        bwa_front = arduino_last_bwa_front.clone();
        bwa_rear = arduino_last_bwa_rear.clone();

        // Update the GUI
        updateBluetoothStatus();

        // Send a toast, letting the user know that the BWA's have been updated
        sendToast(getResources().getString(R.string.toast_downloaded_bwa));
    }

    private void bluetoothUploadBWAs() {
        // TODO: Upload the BWAs from both wheels (perhaps, just do one at a time (i.e. can select individual wheels)?
        // TODO: Do this in a background thread
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
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                // Test the new bluetooth status
                checkBluetooth(false);
                break;
            case REQUEST_WHEEL_ACTIVITY:
                // If the Wheel Activity returned, see what Color_Lists (if any) it produced
                if (resultCode == Activity.RESULT_OK &&  data != null) {
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
    public void onConnectionStateChange(int wheelLoc, boolean connected) {
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
            // TODO NOW: Read the BWA, Kalman, and battery from the newly connected device into the Arduino BWA variable. (How do I do this without the messages running into each other?  Set a "initial connection" variable, so they are rapid-fire if we connecting for the first time?)
            // TODO: Put this into a function, and call the function on both wheels if they exist during onCreate (should be done in a thread or something)

        }

        // Update the GUI
        updateBluetoothStatus();
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

    private void getBWAFromDevice(int wheelLoc) {
        mManager.requestDataFromWheel(BluetoothByteList.ContentType.BWA, wheelLoc); // The response will be retrieved via the handleMessage() function
    }

    public void onRearPowerStateChange(boolean powered) {
        // Check that bluetooth is on
        if (!mCurrentBluetoothStatus) {
            // If bluetooth is not on, do not allow the user to turn on the wheels
            powered = false;

            // Attempt to turn on bluetooth
            checkBluetooth(true);
        }

        // Tell the wheelDrawable what the new connection state is (so the wheel color can change
        wheelDrawable.setRearPowerState(powered);
        wheelSelectView.invalidate();

        // Ensure that the toggle switch is in the correct position
        rearPowerSwitch.setChecked(powered);

        // Get the string to change the display to
        String switchString;
        if (powered) {
            switchString = getApplicationContext().getResources().getString(R.string.leds_on);
        } else {
            switchString = getApplicationContext().getResources().getString(R.string.leds_off);
        }

        rearPowerSwitch.setText(switchString);
    }

    public void onFrontPowerStateChange(boolean powered) {
        if (!mCurrentBluetoothStatus) {
            // If bluetooth is not on, do not allow the user to turn on the wheels
            powered = false;
        }

        // Tell the wheelDrawable what the new connection state is (so the wheel color can change
        wheelDrawable.setFrontConnectedState(powered);
        wheelSelectView.invalidate();

        // Ensure that the toggle switch is in the correct position
        frontPowerSwitch.setChecked(powered);

        // Get the string to change the display to
        String switchString;
        if (powered) {
            switchString = getApplicationContext().getResources().getString(R.string.leds_on);
        } else {
            switchString = getApplicationContext().getResources().getString(R.string.leds_off);
        }

        frontPowerSwitch.setText(switchString);
    }

    private void sendConnectionToast(boolean connected, int id) {
        String connectedStr;
        String IDStr;

        // Get strings for each possible state
        if (connected) {
            connectedStr = "connected";
        } else {
            connectedStr = "disconnected";
        }

        if (id == Constants.ID_FRONT) {
            IDStr = "front";
        } else {
            IDStr = "rear";
        }

        // Create the toast message
        String msg = "A bluetooth device has been " + connectedStr + " from the " + IDStr + " wheel.";

        // Send the toast
        sendToast(msg);
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

                // TODO NOW: Process the read byte list
                // Since we now know what the information was, do different things depending on what we received
                if (readByteList.isRequest()) {
                    // TODO NOW: If this is a request, then send back the information that is requested
                    BluetoothByteList byteListToSend;
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
                            // TODO: Get the current Kalman data, put it into a BTC_Kalman object, and convert that to a byte list
                            break;

                        case Brightness:
                            // TODO: Add a brightness scale to the main activity, get the data, put it into a BTC_Brightness object, and convert it to a byte list

                            break;

                            default:
                                // Uh-oh...
                                return;
                    }

                    // TODO: Send the byteListToSend object!!!
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

                            // TODO: Do something with the Kalman object... (probably need Settings to be set up)

                            break;

                        case Battery:
                            BTC_Battery newBattery = BTC_Battery.fromByteList(readByteList);

                            // TODO: Do something with the Battery object... (probably need to update the GUI)

                            break;
                        case Storage:
                            BTC_Storage newStorage = BTC_Storage.fromByteList(readByteList);

                            // TODO: Do something with the Storage object... (probably need to update the GUI)
                            break;
                        case Brightness:
                            BTC_BrightnessScale newBrightnessScale = BTC_BrightnessScale.fromByteList(readByteList);

                            // TODO: Do something with the Brightness scale object... (probably need Settings to be set up)
                            break;
                    }
                }

                break;
            case Constants.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj; // Message contents
                int writeNumBytes = msg.arg1; // Length of the message
                int writeWheelID = msg.arg2; // Source of the message
                String writeString = new String(writeBuf);
                // TODO: Process the written string (if needed)

                break;
            case Constants.MESSAGE_TOAST:
                sendToast(msg.getData().getString(Constants.TOAST));
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

package to.us.suncloud.bikelights;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
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

    private final static String CONNECTION_MANAGER = "connection_manager";
    public static final String BIKE_WHEEL_ANIMATION = "BIKE_WHEEL_ANIMATION";
    public static final String COLOR_LIST_R = "COLOR_LIST_R";
    public static final String COLOR_LIST_F = "COLOR_LIST_F";
    public static final String WHEEL_LOCATION = "WHEEL_LOCATION";

    private Bike_Wheel_Animation bwa_rear; // List of Color_'s that are a part of the rear wheel
    private Bike_Wheel_Animation bwa_front; // List of Color_'s that are a part of the front wheel

    // TODO: Keep a copy of the most up to date Arduino versions of the BWA's, as well, to check if the two devices are synch'd
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
        bluetoothDownload = findViewById(R.id.bluetoothDownload); // TODO: May actually not need this?  May just happen automatically on connection...
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
                // Send the Android BWAs to the Arduino
                bluetoothUploadBWAs();
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
        int downloadVis = View.GONE; // TODO: May not actually need...

        // if (CONNECTED) { // TODO
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
            // Ready to be uploaded
            stringID = R.string.bt_status_ready_to_upload;
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
        // TODO: Download the BWAs from both BWAs (perhaps, just do one at a time (i.e. can select individual wheels)?
        // TODO: Do this in a background thread
    }

    private void bluetoothUploadBWAs() {
        // TODO: Upload the BWAs from both BWAs (perhaps, just do one at a time (i.e. can select individual wheels)?
        // TODO: Do this in a background thread
    }

    public void setBWA(Bike_Wheel_Animation bwa, int wheelLocation, boolean alsoSetArduinoBWA) {
        // Change the bike wheel animation of the chosen wheel.  We may need to also set the arduino BWAs if we are getting this straight from the bluetooth connection
        switch (wheelLocation) {
            case Constants.ID_FRONT:
                bwa_front = bwa;
                break;
            case Constants.ID_REAR:
                bwa_rear = bwa; // TODO: Need to clone...?
                break;
            default:
                // Uh-oh...
                Log.e(TAG, "Found unknown wheel location to save BWA");
        }

        if (alsoSetArduinoBWA) {
            switch (wheelLocation) {
                case Constants.ID_FRONT:
                    arduino_last_bwa_front = bwa;
                    break;
                case Constants.ID_REAR:
                    arduino_last_bwa_rear = bwa; // TODO: Need to clone...?
                    break;
            }
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
                        setBWA((Bike_Wheel_Animation) b.getSerializable(BIKE_WHEEL_ANIMATION), b.getInt(WHEEL_LOCATION), false);
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
    public void onRearConnectionStateChange(boolean connected) {
        // Tell the wheelDrawable what the new connection state is (so the wheel color can change)
        wheelDrawable.setRearConnectedState(connected);
        wheelSelectView.invalidate();

        // Send a toast message that a device has been either connected or disconnected
        sendConnectionToast(connected, Constants.ID_REAR);

        // TODO: Read the BWA from the newly connected device into the Arduino BWA variable, or get rid of the old saved BWA? Also, update the bluetooth status text.

    }

    public void onFrontConnectionStateChange(boolean connected) {
        // Tell the wheelDrawable what the new connection state is (so the wheel color can change)
        wheelDrawable.setFrontConnectedState(connected);
        wheelSelectView.invalidate();

        // Send a toast message that a device has been either connected or disconnected
        sendConnectionToast(connected, Constants.ID_FRONT);

        // TODO: Read the BWA from the newly connected device into the Arduino BWA variable, or get rid of the old saved BWA? Also, update the bluetooth status text
    }

    private void getBWAFromDevice(int wheeolLoc) {
        // TODO: Do this all in a new thread!

        // TODO: First, check if the device is connected
        if (mCurrentBluetoothStatus && mManager.isWheelConnected(wheeolLoc)) {
            // If the wheel is connected, then send a message to the device to request its BWA


            // Finally, read the BWA that it sends (with timeout)
        }
    }

    private void sendBTRequest(BluetoothByteList.ContentType content, int wheelLoc) {
        // TODO: Maybe another class can be responsible for this...?  Connection manager should be lower level, and BluetoothByteList is just the byte list itself...need some kind of higher level manager?
        // Create a raw byte list for this content as a request
        BluetoothByteList requestByteList = new BluetoothByteList(content, true);


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
                String readString = new String(readBuf);
                // TODO: Process the read string

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
                switch (msg.arg2) {
                    case Constants.ID_FRONT:
                        onFrontConnectionStateChange(true);
                        break;
                    case Constants.ID_REAR:
                        onRearConnectionStateChange(true);
                        break;
                }
                break;
            case Constants.MESSAGE_DISCONNECTED:
                // Perform the needed actions if a new device is added to the front or rear
                switch (msg.arg2) {
                    case Constants.ID_FRONT:
                        onFrontConnectionStateChange(false);
                        break;
                    case Constants.ID_REAR:
                        onRearConnectionStateChange(false);
                        break;
                }
                break;
            case Constants.MESSAGE_BLUETOOTH_STATE_CHANGE:
                checkBluetooth(false); // If the bluetooth state has changed, keep track of it
        }
    }
}

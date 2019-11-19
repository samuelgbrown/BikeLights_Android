package to.us.suncloud.bikelights.common.Bluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Constants;

public class BluetoothRecyclerAdapter extends BluetoothRecyclerView.Adapter<BluetoothRecyclerAdapter.bindViewHolder> { //implements BluetoothMasterHandler.HandlerInt{
    private static String TAG = "BluetoothRecyclerAdapter";

    private ArrayList<BluetoothDevice> mDeviceList;
    private ConnectionManager mManager; // The connection manager created by the main activity
    Context mContext;

    private BluetoothDevice currentConnectingDevice = null; // The device, if any, that is currently being connected to

    // Strings for the emptyText
    private String string_prescan;
    private String string_scanning;

    BluetoothRecyclerAdapter(Context context, Set<BluetoothDevice> deviceList, ConnectionManager manager) {
        mContext = context;
        mDeviceList = new ArrayList<>(deviceList);
        mManager = manager;

        // Get a few useful resources from the Context to be used later
        Resources r = context.getResources();
        string_prescan = r.getString(R.string.available_empty_view_prescan);
        string_scanning = r.getString(R.string.available_empty_view_scanning);
    }

    public abstract class bindViewHolder extends BluetoothRecyclerView.ViewHolder {
        bindViewHolder(final View layoutView) {
            super(layoutView);
        }

        public abstract void bind(int position);
    }

    public class emptyViewHolder extends  bindViewHolder {
        IntentFilter filter; // Intent filter for getting information about Bluetooth discovery events
        View mLayoutView; // The view that contains the entire layout of this Color_ViewHolder
        TextView emptyView; // The view that displays to give the user information when the device list is empty
        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setEmptyText();
            }
        };

        public emptyViewHolder(View layoutView) {
            super(layoutView);
            mLayoutView = layoutView;

            //  Get the text view
            emptyView = mLayoutView.findViewById(R.id.simpleTextView);
            setEmptyText();

            // Register for broadcasts when a device is found, a device's state is changed, a device's name has change, or discovery begins/ends.
            filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

            mContext.registerReceiver(mReceiver, filter);
        }

        private void setEmptyText() {
            // Set the empty text based on the current discovery status
            if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
                emptyView.setText(string_scanning);
            } else {
                emptyView.setText(string_prescan);
            }
        }

        @Override
        public void bind(int position) {
        }
    }

    public class ViewHolder extends bindViewHolder implements FrontOrRearDialog.FrontRearInt, AlterConnectionDialog.alterConnectionInt, BluetoothMasterHandler.HandlerInt {
        View mLayoutView; // The view that contains the entire layout of this Color_ViewHolder
        TextView mNameView; // Displays the name/address of the device
        TextView mConnectingView; // Displays iff the device is actively being connected
        TextView mWheelView; // Displays iff the device is connected, and shows which wheel the device is connected to
        boolean mConnecting = false; // Is this device actively being connected to?
        int mDeviceInd; // The current index of this device in mDeviceList
        BluetoothDevice mDevice;
        int mConnectionLocation;
        ViewHolder mViewHolder = this;
        private ConnectionManager mManager; // The connection manager created by the main activity

        private BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mDevice != null) {
                    String action = intent.getAction();
                    if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
                        // If this is a name change action, getP the device.  If it matches this device, then update the display name
                        BluetoothDevice actionDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (actionDevice.getAddress().equals(mDevice.getAddress())) {
                            // If device whose address got changed is this Color_ViewHolder's device
                            bind(mDeviceInd); // Get the new device TODO: Do I need this?  Try removing and only using mDevice's name
//                        updateViewHolderGUI(); // Update the GUI
                        }
                    }
                }
            }
        };

        public ViewHolder(View layoutView, ConnectionManager manager) {
            super(layoutView);
            mLayoutView = layoutView;

            // Extract all of the individual views from the layout view
            mNameView = layoutView.findViewById(R.id.viewHolderTextView);
            mConnectingView = layoutView.findViewById(R.id.connectingTextView);
            mWheelView = layoutView.findViewById(R.id.FRView);

            // Register the Color_ViewHolder to receive system broadcasts about device name changes
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
            mLayoutView.getContext().registerReceiver(receiver, filter);

            // Assign the ConnectionManager, and register to receive messages about changes in connection status (connection status is received from ConnectionManager instead of systemwide broadcasts because things can go wrong in connection process that only ConnectionManager will know about)
            mManager = manager;
            mManager.registerHandler(this);

            mNameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // The user would like to alter the connection status of this device.  Do different things depending on the state of the device

                    if (mConnecting) {
                        // If we are actively attempting to connect to a device
                        // Stop connecting to this device
                        mManager.stopConnectingDevice(mDevice);

                        // Update the device information
                        updateDeviceStatus();

                        // Update the view
                        updateViewHolderGUI();
                        return;
                    }

                    if (mConnectionLocation == Constants.ID_NONE) {

                        // The device is not currently assigned to a wheel location
                        // Display a dialog to the user to ask which wheel should be connected to. If a device is already associated with the wheel, then confirm the old one's removal
//                        FrontOrRearDialog dialog = FrontOrRearDialog.newInstance(mViewHolder);
//                        dialog.show(((AppCompatActivity) mContext).getSupportFragmentManager(), "FrontOrRearDialog");

                        // Use a dialog builder
                        AlertDialog.Builder builder = new AlertDialog.Builder(mLayoutView.getContext());
                        builder.setTitle(R.string.dialog_front_rear);
                        builder.setItems(new CharSequence[]{"Front", "Rear", "Cancel"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int responseInt = -1;  // Initialize to the "cancel" response
                                switch (which) {
                                    case 0:
                                        responseInt = Constants.ID_FRONT;
                                        break;
                                    case 1:
                                        responseInt = Constants.ID_REAR;
                                        break;
                                }
                                frontRearChoice(responseInt);
                            }
                        });
                        builder.create().show();
                    } else {
                        // The device is assigned to a wheel
                        // Display a dialog to the user to ask what action should be taken with the currently connected device
                        AlterConnectionDialog dialog = AlterConnectionDialog.newInstance(mViewHolder);
                        dialog.show(((AppCompatActivity) mContext).getSupportFragmentManager(), "AlterConnectionDialog");
                    }
                }
            });

            mWheelView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TO_DO: (May need to abandon this, due to the complications of starting an activity for result without access to the Activity Context) Open a wheel-view activity on the wheel pointed to by this device.  Have it return to this window, but save the result of messing with the wheel activity to the MainActivity? Or something?
//                    if (mConnectionLocation != Constants.ID_NONE) {
//                        // If the connection location indicates that this device points to one of the wheels
//                        Intent intent = new Intent(v.getContext(), WheelViewActivity.class); // Create an intent for a wheel view activity
//                        intent.putExtra(BIKE_WHEEL_ANIMATION, cList_front); // Send the Wheel View Activity the color list for the front wheel TO_DO: How to get the color_list?
//                        intent.putExtra(WHEEL_LOCATION, Constants.ID_FRONT); // Send the Wheel View Activity the color list for the front wheel
//                        v.getContext().startActivity(intent); // Start the new activity
//                    }
                }
            });
        }

        @Override
        public void frontRearChoice(int wheelID) {
            // Gets dialog choice from FrontOrRearDialog
            if (!(wheelID == Constants.ID_FRONT || wheelID == Constants.ID_REAR)) {
                // If wheelID is not either the front or the rear wheel
                Log.e(TAG, "Got bad choice from frontRearChoice dialog.");
                return;
            }

            // Connect to the device
            mManager.connectToDevice(mDevice, wheelID);
            updateDeviceStatus();

            // Update the view
            updateViewHolderGUI();
        }

        @Override
        public void alterConnectionChoice(int choice) {
            switch (choice) {
                case Constants.ACTION_DISCONNECT:
                    // Disconnect this device (device identified to the manager by which wheel it is connected to)
                    mManager.disconnectFromDevice(mConnectionLocation);
                    break;
                case Constants.ACTION_SWITCH:
                    // Attempt to connect this device to the other wheel
                    switch (mConnectionLocation) {
                        case Constants.ID_FRONT:
                            // If it is currently connected to the frondialogt, connect it to the back
                            mManager.connectToDevice(mDevice, Constants.ID_REAR); // Function will automatically take care of switching the wheel
                            break;
                        case Constants.ID_REAR:
                            // If it is currently connected to the back, connect it to the front
                            mManager.connectToDevice(mDevice, Constants.ID_FRONT); // Function will automatically take care of switching the wheel
                            break;
                    }
                    break;
                default:
            }

            updateDeviceStatus();
        }

        private void updateViewHolderGUI() {
            if (mConnecting) {
                mConnectingView.setVisibility(View.VISIBLE);
            } else {
                mConnectingView.setVisibility(View.GONE);
            }

            switch (mConnectionLocation) {
                case Constants.ID_FRONT:
                    // For "wheel view"
                    mWheelView.setText(R.string.front_abv);
                    mWheelView.setVisibility(View.VISIBLE);

                    // For "name view"
                    mNameView.setTypeface(mNameView.getTypeface(), Typeface.BOLD);
                    break;
                case Constants.ID_REAR:
                    // For "wheel view"
                    mWheelView.setText(R.string.rear_abv);
                    mWheelView.setVisibility(View.VISIBLE);

                    // For "name view"
                    mNameView.setTypeface(mNameView.getTypeface(), Typeface.BOLD);
                    break;
                case Constants.ID_NONE:
                    // For "wheel view"
                    mWheelView.setVisibility(View.GONE);

                    // For "name view"
                    mNameView.setTypeface(mNameView.getTypeface(), Typeface.NORMAL);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            if (mDevice != null && (msg.what == Constants.MESSAGE_CONNECTED || msg.what == Constants.MESSAGE_DISCONNECTED || msg.what == Constants.MESSAGE_CONNECT_FAILED)) {
                // If any messages are received about a change in connection status...
                BluetoothDevice connectedDevice = (BluetoothDevice) msg.obj;
                if (mDevice.getAddress().equals(connectedDevice.getAddress())) {
                    // If the change in connection status happened to this device, then update the device's information from the ConnectionManager, and adjust the GUI
                    updateDeviceStatus();
                    updateViewHolderGUI();
                }
            }
        }

        public void bind(int deviceInd) {
            // Store the device, and display the name
            mDeviceInd = deviceInd;
            mDevice = mDeviceList.get(mDeviceInd);
            if (mDevice.getName() != null) {
                mNameView.setText(mDevice.getName());
            } else {
                mNameView.setText(mDevice.getAddress());
            }

            updateDeviceStatus();
        }

        private void updateDeviceStatus() {
            if (mDevice != null) {
                // Check which wheel the device is attached to (if any)
                mConnectionLocation = mManager.deviceConnectionID(mDevice);

                // Check if the device is currently being connected to
                mConnecting = mManager.deviceIsConnecting(mDevice);
            }
        }

    }


    @NonNull
    @Override
    public BluetoothRecyclerAdapter.bindViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View layoutView;

        switch (viewType) {
            case 0:
                layoutView = inflater.inflate(R.layout.simple_text_view, parent, false);
                return new emptyViewHolder(layoutView);
            case 1:
                layoutView = inflater.inflate(R.layout.bluetooth_viewholder, parent, false);
                return new ViewHolder(layoutView, mManager);
            default:
                layoutView = inflater.inflate(R.layout.bluetooth_viewholder, parent, false);
                return new ViewHolder(layoutView, mManager);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mDeviceList.size() == 0) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull bindViewHolder holder, int position) {
        // Add this device's information to the relevant viewholder (if it is an empty
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return Math.max(mDeviceList.size(), 1); // Ensure that there is always at least one View, because if the mDeviceList size is 0, then there will be an "empty" viewholder, to let the user know
    }
}

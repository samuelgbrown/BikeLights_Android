package to.us.suncloud.bikelights.common.Bluetooth;


import android.app.Activity;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;

import to.us.suncloud.bikelights.common.Constants;

public class ConnectionManager implements ReplaceDeviceDialog.ReplaceDeviceInt, Serializable {
    private String TAG = "Connection_Manager";

    private FragmentActivity context;

    private BluetoothMasterHandler mMasterHandler; // Handler to communicate with the connection thread

    private BluetoothConnectThread mConnectThread = null;

    private ConnectionManagerThread mRearManagerThread = null;
    private ConnectionManagerThread mFrontManagerThread = null;

    public ConnectionManager(Activity mainActivity) {
        context = (FragmentActivity) mainActivity;

        // Create a BluetoothMasterHandler, so that the bluetooth connections can communicate with any HandlerInt that is registered
        mMasterHandler = new BluetoothMasterHandler();
    }

    public void registerHandler(BluetoothMasterHandler.HandlerInt newHandler) {
        // Register a new Handler to receive communication about the Bluetooth Device
        // Should be called any time an object that needs to "hear" the bluetooth connection is created
        mMasterHandler.registerHandler(newHandler);
    }

    public void unregisterHandler(BluetoothMasterHandler.HandlerInt newHandler) {
        // Register a new Handler to receive communication about the Bluetooth Device
        // Should be called any time an object that needs to "hear" the bluetooth connection is destroyed
        mMasterHandler.unregisterHandler(newHandler);
    }

    public void connectToDevice(BluetoothDevice device, int id) {
        // If this device is already connected to this wheel, then send a toast to the user telling them how stupid they are
        if (deviceConnectionID(device) == id) {
            sendToast("Device already connected to this wheel.", id);
            return;
        }

        // Check if there is already a managed device.  If so, ask the user if they want to connect anyway (disconnect from the old device)
        if ((id == Constants.ID_FRONT && mFrontManagerThread != null) || (id == Constants.ID_REAR && mRearManagerThread != null)) {
            // If the user is trying to connect a wheel that is already connected to a device, ask them to confirm
            ReplaceDeviceDialog dialog = ReplaceDeviceDialog.newInstance(this, device, id);
            FragmentManager sfm = context.getFragmentManager();
            dialog.show(sfm, "Replace device");

            // Exit this function, as it will be called once again when the device to replace has been chosen by the user
            return;
        }

        // If this device is currently connected to the opposite wheel, then simply swap manager threads
        if (deviceConnectionID(device) != Constants.ID_NONE) {
            // The device has already been confirmed as not being connected to its target thread (first if-block of this function), so a switch is all that is needed
            switch (id) {
                case Constants.ID_FRONT:
                    // Switch to the front manager, and remove from the rear
                    mFrontManagerThread = mRearManagerThread;
                    mRearManagerThread = null;
                    break;
                case Constants.ID_REAR:
                    // Switch to the rear manager, and remove from the front
                    mRearManagerThread = mFrontManagerThread;
                    mFrontManagerThread = null;
                    ;
                    break;
            }
            return;
        }

        // If a thread is already connecting, then cancel it
        if (mConnectThread != null) {
            mConnectThread.close();
        }

        mConnectThread = new BluetoothConnectThread(device, id);
    }

    public int deviceConnectionID(BluetoothDevice device) {
        if (mFrontManagerThread != null && device.getAddress().equals(mFrontManagerThread.getDevice().getAddress())) {
            // If the front manager thread is not null, and the address is the same as the device passed to this function
            return Constants.ID_FRONT;
        } else if (mRearManagerThread != null && device.getAddress().equals(mRearManagerThread.getDevice().getAddress())) {
            // If the rear manager thread is not null, and the address is the same as the device passed to this function
            return Constants.ID_REAR;
        } else {
            return Constants.ID_NONE;
        }
    }

    public boolean deviceIsConnecting(BluetoothDevice device) {
        // If there is an active connect thread and its device address is the same as the device passed to this function's
        return mConnectThread != null && device.getAddress().equals(mConnectThread.getDevice().getAddress());
    }

    @Override
    public void replaceDeviceChoice(int choice, BluetoothDevice device, int id) {
        if (choice == Constants.ACTION_REPLACE) {
            // Close the selected wheel's device connection...
            disconnectFromDevice(id);

            // ... and restart the connection process.  (Go back to the function that called the dialog that exited to this function (phew))
            connectToDevice(device, id);
        } else {
            // Send a message that the ConnectionManager failed to connect the device
            sendConnectFailedMessage(device, id);
        }
    }

    public void disconnectFromDevice(int id) {
        switch (id) {
            case Constants.ID_FRONT:
                if (mFrontManagerThread != null) {
                    // If a front wheel device exists, disconnect from it
                    mFrontManagerThread.close();
                    mFrontManagerThread = null;
                }
                break;
            case Constants.ID_REAR:
                if (mRearManagerThread != null) {
                    // If a rear wheel device exists, disconnect from it
                    mRearManagerThread.close();
                    mRearManagerThread = null;
                }
                break;
        }
    }

    public void stopConnectingDevice(BluetoothDevice device) {
        // If this is the same device as ANY of the threads, close the thread
        if (mConnectThread != null) {
            if (mConnectThread.isThisDevice(device)) {
                mConnectThread.close();
            }
        }
    }

    private void sendDisconnectMessage(BluetoothDevice device, int id) {
        // Simple function interface to send a disconnection message through the Handler
        Message disconnectedMsg = mMasterHandler.obtainMessage(
                Constants.MESSAGE_DISCONNECTED, 0, id, device);
        disconnectedMsg.sendToTarget();
    }

    private void sendConnectMessage(BluetoothDevice device, int id) {
        // Simple function interface to send a disconnection message through the Handler
        Message connectMsg = mMasterHandler.obtainMessage(
                Constants.MESSAGE_CONNECTED, 0, id, device);
        connectMsg.sendToTarget();
    }

    private void sendConnectFailedMessage(BluetoothDevice device, int id) {
        // Simple function interface to send a disconnection message through the Handler
        Message connectFailedMsg = mMasterHandler.obtainMessage(
                Constants.MESSAGE_CONNECT_FAILED, 0, id, device);
        connectFailedMsg.sendToTarget();
    }

    private void sendToast(String toastStr, int id) {
        Message toastMsg =
                mMasterHandler.obtainMessage(Constants.MESSAGE_TOAST, -1, id);
        Bundle bundle = new Bundle();
        bundle.putString("toast", toastStr);
        toastMsg.setData(bundle);
        mMasterHandler.sendMessage(toastMsg);
    }

    private void manageSocketConnection(BluetoothSocket socket, int id, BluetoothDevice device) {
        // Create a new manager thread and save it
        // Called after a bluetooth device has been successfully connected (after a BluetoothConnectThread has concluded)

        ConnectionManagerThread newThread = new ConnectionManagerThread(socket, id, device);

        switch (id) {
            case Constants.ID_REAR:

                mRearManagerThread = newThread;

                break;
            case Constants.ID_FRONT:

                mFrontManagerThread = newThread;
                break;
        }
    }

    // Thread subclass that handles connecting to a new device
    private class BluetoothConnectThread extends Thread {
        private int mID;
        private BluetoothDevice mDevice;
        private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        private BluetoothSocket mSocket = null;
        private UUID uuid = UUID.fromString("9ff9d5ec-30e0-4691-abcc-0827b4e7bcef");
        private String TAG = "BluetoothConnectThread";

        public BluetoothConnectThread(BluetoothDevice device, int id) {
            mID = id;
            mDevice = device;

            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Could not create RFComm Socket from device.", e);
                close();
            }
        }

        @Override
        public void run() {
            // Stop discovery while trying to connect to the device
            mAdapter.cancelDiscovery(); // TODO: Is this working?  See no change in the available-adapter's view when a connection thread starts...

            try {
                // Try to form a connection
                mSocket.connect();

                // Manage the connection
                manageSocketConnection(mSocket, mID, mDevice);
            } catch (IOException connectE) {
                Log.e(TAG, "Could not connect to device using socket.", connectE);
                close();
            }

            // Remove reference to this thread
            mConnectThread = null;
        }

        public void close() {
            try {
                mSocket.close();
            } catch (IOException closeE) {
                Log.e(TAG, "Could not close socket .", closeE);
            }

            // Send a message that the ConnectionManager failed to connect the device
            sendConnectFailedMessage(mDevice, mID);
            sendToast("Device could not connect", mID);

            // Remove reference to this thread
            mConnectThread = null;
        }

        boolean isThisDevice(BluetoothDevice device) {
            return mDevice.getAddress().equals(device.getAddress());
        }

        public BluetoothDevice getDevice() {
            return mDevice;
        }
    }


    // Thread subclass that handles managing a running bluetooth connection
    // This class (along with a pretty sizable chunk of this project, but this class in particular) is built significantly off code from the Android Studio developer guide (https://developer.android.com/guide/topics/connectivity/bluetooth#ManageAConnection)
    private class ConnectionManagerThread extends Thread {
        private int mID;
        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;
        private final InputStream mInStream;
        private final OutputStream mOutStream;
        private byte[] mBuffer;


        ConnectionManagerThread(BluetoothSocket socket, int id, BluetoothDevice device) {
            // Get the wheel id and bluetooth socket
            mID = id;
            mSocket = socket;
            mDevice = device;

            // Preallocate variables for the input/output streams
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Acquire the input and output streams
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred trying to open the output stream", e);
                sendToast("Error occurred trying to open the output stream", mID);
                close(); // Close the connection
            }

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred trying to open the input stream", e);
                sendToast("Error occurred trying to open the input stream", mID);
                close(); // Close the connection
            }


            mInStream = tmpIn;
            mOutStream = tmpOut;

            // Once the device is being managed, send a message to notify the program
            sendConnectMessage(mDevice, mID);
        }

        public void write(byte[] bytes) {
            try {
                // Write the data to the OutputStream (usually not a blocking call, but may be)
                mOutStream.write(bytes);
                int numBytes = bytes.length;

                // Share the sent message with the UI activity.
                Message writtenMsg = mMasterHandler.obtainMessage(
                        Constants.MESSAGE_WRITE, numBytes, mID, mBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                sendToast("Couldn't send data to the device, closing the connection", mID);

                // Close the connection
                close();
            }
        }

        public void run() {
            mBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream (blocking call until data is available to read).
                    numBytes = mInStream.read(mBuffer);

                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mMasterHandler.obtainMessage(
                            Constants.MESSAGE_READ, numBytes, mID,
                            mBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void close() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }

            // Once the device is closed, send a message to notify the program
            sendDisconnectMessage(mDevice, mID);
            sendToast("Device disconnected", mID);

            // Remove reference to this thread
            switch (mID) {
                case Constants.ID_FRONT:
                    mFrontManagerThread = null;
                    break;
                case Constants.ID_REAR:
                    mRearManagerThread = null;
                    break;
            }
        }

        boolean isThisDevice(BluetoothDevice device) {
            return mDevice.getAddress().equals(device.getAddress());
        }

        BluetoothDevice getDevice() {
            return mDevice;
        }
    }
}

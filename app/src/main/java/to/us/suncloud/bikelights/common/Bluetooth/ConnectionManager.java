package to.us.suncloud.bikelights.common.Bluetooth;


import android.app.Activity;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import to.us.suncloud.bikelights.common.Constants;

public class ConnectionManager implements ReplaceDeviceDialog.ReplaceDeviceInt, Serializable {
    private static final String TAG = "Connection_Manager";

    private static final int MAX_INPUT_BUFFER_SIZE = 1024;
    private static final int COMPLETE_SIG_NUM = 10;
    private static final int MAX_WAIT_MILLISECONDS = 5000; // Maximum wait time in milliseconds

    private FragmentActivity context;

    private BluetoothMasterHandler mMasterHandler; // Handler to communicate with the connection thread

    private BluetoothConnectThread mConnectThread = null;

    private ConnectionManagerThread mRearManagerThread = null;
    private ConnectionManagerThread mFrontManagerThread = null;

    private BluetoothDevice mFrontStoredDevice = null; // Store devices, if needed, to attempt reconnection should anything happen to the connection unintentionally
    private BluetoothDevice mRearStoredDevice = null;

    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Check if bluetooth has turned on or off
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                // Get the new state
                int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_CONNECTED);

                switch (newState) {
                    case BluetoothAdapter.STATE_ON:
                        // If the bluetooth adapter has just turned on, then try reconnecting to any devices that were unintentionally disconnected
                        // TODO: Get all methods of unintentional disconnection?

                        // Let the Main Activity know that the bluetooth state has changed
                        sendBTChangeMessage();

                        // Attempt to reconnect to devices
                        attemptReconnectToStoredDevices();
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        // Let the Main Activity know that the bluetooth state has changed
                        sendBTChangeMessage();

                        // Because this is an unintentional disconnect, store the bluetooth devices to be reconnected to later
                        storeDevicesAndCloseManagedThreads();
                        break;
                }
            }
        }
    };

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

    public BroadcastReceiver getBroadcastReceiver() {
        // Return the broadcast receiver, so it can be registered and unregistered by the Activity.
        return br;
    }

    public boolean sendBytesToDevice(final List<BluetoothByteList> byteListList, final List<Integer> wheelLocs) {
        // A public interface to allow multiple pieces of information to be sent at a time
        boolean status = true; // Start out assuming that the data transfer will go well...

        // Error checks
        if (byteListList.size() != wheelLocs.size()) {
            // If the two incoming Lists are not the same size, then return an error condition
            Log.w(TAG, "sendBytesToDevice: Got mismatched data and destination lists.");
            return false;
        }

        if (byteListList.size() == 0) {
            // If the incoming Lists are of size 0, then don't send any information (but return a non-error anyway)
            Log.w(TAG, "sendBytesToDevice: Got size = 0 lists.");
            return true;
        }

        // Create a Bluetooth Interaction Thread, so that all of the writing/checking for confirmations occurs in a separate thread
        new BluetoothInteractionThread(this) {
            @Override
            public void sendingOperations() {
                // Go through each byte list in the byteListList object
                for (int listInd = 0; listInd < byteListList.size(); listInd++) {
                    BluetoothByteList thisByteList = byteListList.get(listInd);
                    int thisWheelLoc = wheelLocs.get(listInd);

                    // TODO TESTING
                    String wheelStr = "unknown";
                    switch (thisWheelLoc) {
                        case Constants.ID_FRONT:
                            wheelStr = "Front";
                            break;
                        case Constants.ID_REAR:
                            wheelStr = "Rear";
                            break;
                    }

                    String requestStr = "";
                    if (thisByteList.isRequest()) {
                        requestStr = " request";
                    }

                    Log.d(TAG, "Sending " + BluetoothByteList.contentTypeToString(thisByteList.getContentType()) + requestStr + " to " + wheelStr + " wheel...");

                    switch (thisWheelLoc) {
                        case Constants.ID_FRONT:
                            if (mFrontManagerThread == null) {
                                // If we are trying to send something to the front wheel, but we aren't connected, then don't try to send anything
                                Log.w(TAG, "Attempting to send to front wheel, but it is not connected.");
                                continue;
                            }
                            break;
                        case Constants.ID_REAR:
                            if (mRearManagerThread == null) {
                                // If we are trying to send something to the rear wheel, but we aren't connected, then don't try to send anything
                                Log.w(TAG, "Attempting to send to rear wheel, but it is not connected.");
                                continue;
                            }
                            break;
                    }

                    // First, initialize the write
                    thisByteList.startWriting();

                    boolean firstMessage = true; // Make sure that the loop runs at least once, particularly for requests (There is probably a better way to do this, e.g. querying if the byteList has sent any data [if the pointer location is past the PROCESSED byte list size, not just the raw byte list size, which is 0 for a request], but...I'm lazy)

                    // Send a single 32-byte message (with 2-byte header)
                    while (firstMessage || !thisByteList.isDoneReading()) {
                        // First, get the message to be sent
                        byte[] thisSubMessage = thisByteList.getNextProcessedByteList();

                        if (!thisByteList.isRequest()) {
                            // Next, let the BluetoothInteractionThread know that we are going to be waiting for a confirmation message
                            setContentToWaitFor(BluetoothByteList.ContentType.SP_Confirm, thisWheelLoc);
                        }

                        // Then, send the processed byte list to the device indicated
                        try {
                            switch (thisWheelLoc) {
                                case Constants.ID_FRONT:
                                    mFrontManagerThread.write(thisSubMessage);
                                    break;
                                case Constants.ID_REAR:
                                    mRearManagerThread.write(thisSubMessage);
                                    break;
                            }
                        } catch (Exception e) {
                            // Uh-oh...
                            Log.w(TAG, "Data transfer to device failed.");
                            return;
//                        status = false;
                        }

                        // Wait for a response from the Arduino
                        boolean waitSuccess = waitForData();

                        if (!waitSuccess) {
                            return;
                        } else {
                            if (!thisByteList.isRequest()) {
                                Log.d(TAG, "Received confirmation message, continuing...");
                            }
                        }

                        firstMessage = false; // This is no longer the first message
                    }

                    Log.d(TAG, "Data sent.");
                }
            }

            @Override
            public void timeoutFun(BluetoothByteList.ContentType contentType, int wheelLoc) {
                sendToast("Timed out waiting for a confirmation", wheelLoc);
                Log.w(TAG, "Timed out waiting for a confirmation");
            }
        }.start();

        return status;
    }

    public boolean sendBytesToDevice(final BluetoothByteList rawByteList, final int wheelLoc) {
        // Initialize the byte list and wheel location Lists
        List<BluetoothByteList> byteListList = new ArrayList<>(1);
        byteListList.add(rawByteList);

        List<Integer> wheelLocs = new ArrayList<>(1);
        wheelLocs.add(wheelLoc);

        // Send the data to the devices
        return sendBytesToDevice(byteListList, wheelLocs);
    }

    public boolean sendBytesToDevice(final List<BluetoothByteList> byteListList, final int wheelLoc) {
        // To send many pieces of information to the same wheel
        List<Integer> wheelLocs = new ArrayList<>(Collections.nCopies(byteListList.size(), wheelLoc));

        // Send the data to the devices
        return sendBytesToDevice(byteListList, wheelLocs);
    }

    public void requestDataFromWheel(BluetoothByteList.ContentType content, int wheelLoc) {
        // Send a request for data to the wheel location

        if (isWheelConnected(wheelLoc)) {
            // If the wheel is connected, then send a message to the device to request its BWA

            // Create a raw byte list for this content as a request
            BluetoothByteList requestByteList = new BluetoothByteList(content, true);

            // Send this request to the wheel of interest
            sendBytesToDevice(requestByteList, wheelLoc);

            // The response will be received by this ConnectionManager, and broadcast to all Handlers
        }
    }

    public void connectToDevice(BluetoothDevice device, int wheelLoc) {
        // If this device is already connected to this wheel, then send a toast to the user telling them how stupid they are
        if (deviceConnectionID(device) == wheelLoc) {
            sendToast("Device already connected to this wheel.", wheelLoc);
            return;
        }

        // Check if there is already a managed device.  If so, ask the user if they want to connect anyway (disconnect from the old device)
        if ((wheelLoc == Constants.ID_FRONT && mFrontManagerThread != null) || (wheelLoc == Constants.ID_REAR && mRearManagerThread != null)) {
            // If the user is trying to connect a wheel that is already connected to a device, ask them to confirm
            ReplaceDeviceDialog dialog = ReplaceDeviceDialog.newInstance(this, device, wheelLoc);
            FragmentManager sfm = context.getFragmentManager();
            dialog.show(sfm, "Replace device");

            // Exit this function, as it will be called once again when the device to replace has been chosen by the user
            return;
        }

        // If this device is currently connected to the opposite wheel, then simply swap manager threads
        if (deviceConnectionID(device) != Constants.ID_NONE) {
            // The device has already been confirmed as not being connected to its target thread (first if-block of this function), so a switch is all that is needed
            switch (wheelLoc) {
                case Constants.ID_FRONT:
                    // Switch to the front manager, and remove from the rear
                    mFrontManagerThread = mRearManagerThread;
                    mFrontManagerThread.setWheelID(Constants.ID_FRONT);
                    mRearManagerThread = null;

                    // Send both a connect message and a disconnect message
                    sendConnectMessage(mFrontManagerThread.getDevice(), Constants.ID_FRONT); // Connected to the rear wheel
                    sendDisconnectMessage(mFrontManagerThread.getDevice(), Constants.ID_REAR); // Disconnected from the front wheel
                    break;
                case Constants.ID_REAR:
                    // Switch to the rear manager, and remove from the front
                    mRearManagerThread = mFrontManagerThread;
                    mRearManagerThread.setWheelID(Constants.ID_REAR);
                    mFrontManagerThread = null;

                    // Send both a connect message and a disconnect message
                    sendConnectMessage(mRearManagerThread.getDevice(), Constants.ID_REAR); // Connected to the rear wheel
                    sendDisconnectMessage(mRearManagerThread.getDevice(), Constants.ID_FRONT); // Disconnected from the front wheel
                    break;
            }

            // Send a disconnect and a connect message

            return;
        }

        // If a thread is already connecting, then cancel it
        if (mConnectThread != null) {
            mConnectThread.close();
        }

        // Create a new connection thread, and start it
        mConnectThread = new BluetoothConnectThread(device, wheelLoc);
        mConnectThread.start();
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

    private void sendBTChangeMessage() {
        // Simple function interface to send a disconnection message through the Handler
        Message connectMsg = mMasterHandler.obtainMessage(
                Constants.MESSAGE_BLUETOOTH_STATE_CHANGE);
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
        // Create a new manager thread, start it, and save it
        // Called after a bluetooth device has been successfully connected (after a BluetoothConnectThread has concluded)

        ConnectionManagerThread newThread = new ConnectionManagerThread(socket, id, device);
        newThread.start();

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
    private class BluetoothConnectThread extends Thread implements Serializable {
        private int mID;
        private BluetoothDevice mDevice;
        private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        private BluetoothSocket mSocket = null;
        private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//        private UUID uuid = UUID.fromString("9ff9d5ec-30e0-4691-abcc-0827b4e7bcef");
//        private String TAG = "BluetoothConnectThread";

        public BluetoothConnectThread(BluetoothDevice device, int id) {
            Log.d(TAG, "Starting Connect Thread...");
            mID = id;
            mDevice = device;

            Log.d(TAG, "Creating RFComm Socket from device.");
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

            boolean connected = false;
            try {
                // Try to form a connection
                mSocket.connect();
                connected = true;
            } catch (IOException connectE) {
                Log.w(TAG, "Could not connect to device using socket.");
                close();
            }

            if (connected) {
                // Manage the connection
                manageSocketConnection(mSocket, mID, mDevice);
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
    private class ConnectionManagerThread extends Thread implements Serializable {
        private int mID;
        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;
        private final InputStream mInStream;
        private final OutputStream mOutStream;
        private byte[] mBuffer;


        ConnectionManagerThread(BluetoothSocket socket, int id, BluetoothDevice device) {
            Log.d(TAG, "Starting Connection Manager Thread...");

            // Get the wheel id and bluetooth socket
            mID = id;
            mSocket = socket;
            mDevice = device;

            // Preallocate variables for the input/output streams
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Acquire the input and output streams
            Log.d(TAG, "Getting output stream...");
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred trying to open the output stream", e);
                sendToast("Error occurred trying to open the output stream", mID);
                close(); // Close the connection
            }

            Log.d(TAG, "Getting input stream...");
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
            Log.d(TAG, "Connected to device.");
            sendConnectMessage(mDevice, mID);
        }

        public void setWheelID(int newID) {
            // If the wheel that this thread is connected to gets switched, accept a new wheel ID
            mID = newID;
        }

        public void write(byte[] bytes) {
            try {
                // Write the data to the OutputStream (usually not a blocking call, but may be)
                int numBytes = bytes.length;
                mOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = mMasterHandler.obtainMessage(
                        Constants.MESSAGE_WRITE, numBytes, mID, bytes);
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

            // Keep listening to the InputStream until an exception occurs.
            List<Byte> completeSignal = makeCompleteSignal();
            while (true) {
                int numBytes = 0; // bytes returned from read()
//                mBuffer = new byte[MAX_INPUT_BUFFER_SIZE];
                List<Byte> completeBuffer = new ArrayList<>(); // All bytes that have been received
                mBuffer = new byte[MAX_INPUT_BUFFER_SIZE];
//                boolean readData = false;
                Log.v(TAG, "Starting read...");
                try {
                    // Read from the InputStream (blocking call until data is available to read).
//                    while (numBytes <= COMPLETE_SIG_NUM || !readComplete(mBuffer, numBytes)) {
//                        numBytes += mInStream.read(mBuffer, numBytes, MAX_INPUT_BUFFER_SIZE - numBytes); // We're kind of trusting that there aren't any transmission errors or anything, otherwise we could get stuck in this blocking call forever...

                    while (true) {
                        // Read the incoming data to a byte array
                        int readBytes = mInStream.read(mBuffer, 0, MAX_INPUT_BUFFER_SIZE);
                        Log.d(TAG, "Received some data...(" + readBytes + " bytes)");

                        // Convert the byte[] to a List<Byte>
                        List<Byte> incomingByteList = new ArrayList<>(readBytes);
                        for (int byteNum = 0; byteNum < readBytes; byteNum++) {
                            incomingByteList.add(mBuffer[byteNum]);
                        }

                        // Add the new bytes to the complete byte list
                        completeBuffer.addAll(incomingByteList);

                        // See if this list has the completion signal in it
                        int completeSigLoc = Collections.indexOfSubList(completeBuffer, completeSignal);
                        while (completeSigLoc != -1) {
                            // If we've found the completion signal, then take everything before it, and send it to any listening message handlers (without the completion signal)
                            numBytes = completeSigLoc; // The number of bytes in this message
                            List<Byte> thisMessage = new ArrayList<>(completeBuffer.subList(0, numBytes)); // The extracted message, when complete (without the completion signal)
                            completeBuffer.subList(0, completeSigLoc + COMPLETE_SIG_NUM).clear(); // Remove the extracted message (including completion signal) from the complete mBuffer, to be added to later

                            // Before sending the message downstream, check if there is another completed message in the completeBuffer
                            if (completeBuffer.size() > 0) {
                                completeSigLoc = Collections.indexOfSubList(completeBuffer, completeSignal); // Check if there is another completion signal in the buffer
                            } else {
                                completeSigLoc = -1;
                            }

                            // If there is no content to the actual message, for whatever reason, don't bother sending it downstream
                            if (numBytes <= 0) {
                                // Now that we've removed the complete signal from completeBuffer, just continue without sending the message on
                                continue;
                            }

                            // Send the newly received message to any listening Handlers

                            // Convert the List<Byte> to a byte[]
                            byte[] thisMessageByteArray = new byte[numBytes];
                            for (int byteNum = 0; byteNum < numBytes; byteNum++) {
                                thisMessageByteArray[byteNum] = thisMessage.get(byteNum);
                            }

                            // Send the obtained bytes to the UI activity (and other Handlers)
                            Log.d(TAG, "    Sending complete message...(" + numBytes + " bytes)");
                            Message readMsg = mMasterHandler.obtainMessage(
                                    Constants.MESSAGE_READ, numBytes, mID,
                                    thisMessageByteArray);
                            readMsg.sendToTarget();
                        }
                    }
//                    // We have successfully read data!
//                    Log.d(TAG, "Received full message.");
//                    readData = true;

                    // Get rid of the completion sequence at the end
//                    numBytes -= COMPLETE_SIG_NUM;
//                    mBuffer = Arrays.copyOfRange(mBuffer, 0, numBytes);

                } catch (IOException e) {
                    Log.e(TAG, "Input stream was disconnected");
//                    readData = false;
                    return;
                }

            }
        }

        private List<Byte> makeCompleteSignal() {
            List<Byte> completeSig = new ArrayList<>(COMPLETE_SIG_NUM);
            for (int i = 0; i < COMPLETE_SIG_NUM; i++) {
                completeSig.add((byte) i);
            }
            return completeSig;
        }

        private boolean readComplete(byte[] buffer, int numBytesRead) {
            // Check if the complete signal was sent from Arduino.  The signal is to count from 0, COMPLETE_SIG_NUM number of times, one value for each byte.
            int readSignalRepNum = 0;
            for (int byteLoc = 0; byteLoc < numBytesRead; byteLoc++) {
                if (buffer[byteLoc] == readSignalRepNum) {
                    // We may have found the read signal, iterate the counter of signals we have read in a row
                    readSignalRepNum++;

                    if (readSignalRepNum >= COMPLETE_SIG_NUM) {
                        // If we have read COMPLETE_SIG_NUM of the read complete signal byte in a row, then the signal exists in the mBuffer, so return from the function successfully
                        return true;
                    }
                } else {
                    // Turns out this wasn't actually a read signal, just a coincidence...clear the coutner
                    readSignalRepNum = 0;
                }
            }

//            int nextConfirmVal = COMPLETE_SIG_NUM - 1;
//            for (int byteLoc = numBytesRead - 1; numBytesRead >= 0; numBytesRead--) {
//                if (mBuffer[byteLoc] != nextConfirmVal) {
//                    // If this is not a confirmation message, then exit the function
//                    break;
//                }
//
//                if (nextConfirmVal == 0) {
//                    // If we have successfully confirmed the confirmation signal, then exit with a positive result
//                    return true;
//                }
//                nextConfirmVal--;
//            }


//            Log.d(TAG, "    Data incomplete...");
            return false;

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

    private void storeDevicesAndCloseManagedThreads() {
        // Store both of the managed bluetooth devices, so that we can attempt to reconnect to them later

        if (mFrontManagerThread != null) {
            // If there is a front wheel bluetooth device
            mFrontStoredDevice = mFrontManagerThread.getDevice();
            mFrontManagerThread.close();
        }

        if (mRearManagerThread != null) {
            // If there is a front wheel bluetooth device
            mRearStoredDevice = mRearManagerThread.getDevice();
            mRearManagerThread.close();
        }
    }

    private void attemptReconnectToStoredDevices() {
        // See if there are any devices that were stored (due to an unintentional bluetooth disconnection), and try to connect to them again

        if (mFrontStoredDevice != null) {
            // Attempt to reconnect to the device once
            connectToDevice(mFrontStoredDevice, Constants.ID_FRONT);
            mFrontStoredDevice = null;
        }

        if (mRearStoredDevice != null) {
            // Attempt to reconnect to the device once
            connectToDevice(mRearStoredDevice, Constants.ID_REAR);
            mRearStoredDevice = null;
        }
    }

    public boolean isWheelConnected(int wheelLoc) {
        switch (wheelLoc) {
            case Constants.ID_FRONT:
                return mFrontManagerThread != null;
            case Constants.ID_REAR:
                return mRearManagerThread != null;
            default:
                return false;
        }
    }
}

package to.us.suncloud.bikelights.common.Bluetooth;

import android.os.Message;
import android.util.Log;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import to.us.suncloud.bikelights.common.Constants;


// This is a specific Thread that is useful for sending/receiving information over the Bluetooth connection, because it has a simple method for pausing execution while waiting for a specific type of content (i.e. ensuring that the bluetooth connection is called serially instead of all at once)
public abstract class BluetoothInteractionThread extends Thread implements BluetoothMasterHandler.HandlerInt {
    private static final String TAG = "SEND_THREAD";
    private static final int MAX_WAIT_MILLISECONDS = 5000; // Maximum wait time in milliseconds
    BluetoothByteList.ContentType curWaitingContent;
    int curWaitingWheelLoc;
    boolean isWaiting = false;
    public ConnectionManager manager;

    public abstract void sendingOperations(); // A function that must be overwritten by the calling function, which describes the sending/requesting of data
    public abstract void timeoutFun(BluetoothByteList.ContentType contentType, int wheelLoc); // Runs if the waitForData function gets timed out (i.e. no data of the given type is received)

    @Override
    public void run() {
        sendingOperations();

        // Once we have finished our operation, unregister this handler from the
        manager.unregisterHandler(this);
    }

    public BluetoothInteractionThread(ConnectionManager manager) {
        this.manager = manager;
        this.manager.registerHandler(this);
    }

    public void requestData(BluetoothByteList.ContentType content, int wheelLoc) {
        // First, request the data from the wheel
        manager.requestDataFromWheel(content, wheelLoc);

        // Then, wait for a response
        waitForData(content, wheelLoc);
    }

    public void waitForData(BluetoothByteList.ContentType content, int wheelLoc) {
        // This function will wait for a specific type of content from the specified wheel (if we receive the specified data, then the handleMessage() function will break us out of the loop)
        curWaitingContent = content;
        curWaitingWheelLoc = wheelLoc;
        isWaiting = true;
        long endWaitTime = Calendar.getInstance().getTimeInMillis() + MAX_WAIT_MILLISECONDS;

        while (isWaiting) { // Changed to true through the handleMessage() function
            // If we are still waiting...

            // Check the current time
            long curTime = Calendar.getInstance().getTimeInMillis();
            if (curTime >= endWaitTime) {
//                sendToast("Error retrieving " + BluetoothByteList.contentTypeToString(curWaitingContent) + " from " + wheelLocToString(curWaitingWheelLoc) + " wheel. Skipping.");
//                Log.e(TAG, "Could not receive " + BluetoothByteList.contentTypeToString(curWaitingContent) + " from " + wheelLocToString(curWaitingWheelLoc) + " wheel.");

                // Execute the timeout function defined by the user, and break out of this loop (should be a relatively safe fail-state)
                timeoutFun(content, wheelLoc);
                break;
            }

            // Otherwise, wait another few milliseconds before checking for data again
            try {
                TimeUnit.MILLISECONDS.sleep(200); // Wait between checks for the new data
            } catch (InterruptedException e) {
                Log.e(TAG, e.toString());
            }
        }

        // Ensure that the thread is no longer in a "waiting" state
        isWaiting = false;
    }

    @Override
    public void handleMessage(Message msg) {
        if (isWaiting) {
            // If we are waiting for some content to be sent back to us
            if (msg.what == Constants.MESSAGE_READ) {
                // If we received some content, read what it is
                byte[] readBuf = (byte[]) msg.obj;
                int readNumBytes = msg.arg1; // Length of the message
                int readWheelID = msg.arg2; // Source of the message

                BluetoothByteList readByteList = new BluetoothByteList(readBuf, readNumBytes);


                if (readWheelID == curWaitingWheelLoc && !readByteList.isRequest() && readByteList.getContentType() == curWaitingContent) {
                    // If this is content (not a request) from the correct wheel, that corresponds to the type of information that we want, then stop waiting
                    isWaiting = false;
                }
            }
        }
    }
}

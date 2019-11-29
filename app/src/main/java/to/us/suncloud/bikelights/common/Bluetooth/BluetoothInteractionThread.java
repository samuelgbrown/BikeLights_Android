package to.us.suncloud.bikelights.common.Bluetooth;

import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import to.us.suncloud.bikelights.common.Constants;


// This is a specific Thread that is useful for sending/receiving information over the Bluetooth connection, because it has a simple method for pausing execution while waiting for a specific type of content (i.e. ensuring that the bluetooth connection is called serially instead of all at once)
public abstract class BluetoothInteractionThread extends Thread implements BluetoothMasterHandler.HandlerInt {
    private static final String TAG = "SEND_THREAD";
    private static final int MAX_WAIT_MILLISECONDS = 5000; // Maximum wait time in milliseconds
    public ConnectionManager manager;

    // Variables used to wait for messages
    int curWaitingWheelLoc;
    BluetoothByteList.ContentType curWaitingContent;
    boolean isWaiting = false;

    public abstract void sendingOperations(); // A function that must be overwritten by the calling function, which describes the sending/requesting of data

    public abstract void timeoutFun(BluetoothByteList.ContentType contentType, int wheelLoc); // Runs if the waitForData function gets timed out (i.e. no data of the given type is received)

    @Override
    public void run() {
        Looper.prepare(); // In case the user wants to do any UI interaction (TODO: Should I instead use the runOnUIThread method, and make a class-specific sendToast function?)

        sendingOperations();

        // Once we have finished our operation, unregister this handler from the
        manager.unregisterHandler(this);
    }

    public BluetoothInteractionThread(ConnectionManager manager) {
        this.manager = manager;
        this.manager.registerHandler(this);
    }

    public void requestData(BluetoothByteList.ContentType content, int wheelLoc) {
        // First, record which type of content we are going to be waiting for (because we are calling waitForData() below)
        setContentToWaitFor(content, wheelLoc);

        // Next, request the data from the wheel
        manager.requestDataFromWheel(content, wheelLoc);

        // Then, wait for a response
        waitForData();
    }

    public void setContentToWaitFor(BluetoothByteList.ContentType contentType, int wheelLoc) {
        // **This will be used in conjunction with the waitForData function**

        // Call this function before calling the write
        curWaitingWheelLoc = wheelLoc;
        curWaitingContent = contentType;
        isWaiting = true; // We are now starting to wait for the content
    }

    public boolean waitForData() {
        // Use the default maximum wait time
        return waitForData(MAX_WAIT_MILLISECONDS);
    }

    public boolean waitForData(long maxWaitMilliseconds) {
        // **This will be used in conjunction with the setContentToWaitFor() function**

        // This function will wait for a specific type of content from the specified wheel (if we receive the specified data, then the handleMessage() function will break us out of the loop)
        // Return value TRUE if successful, FALSE if timed out
        if (isWaiting) {
            // If we are still waiting (i.e. if the message did not get sent between setContentToWaitFor() and now, which can happen if the Arduino is feeling particularly fast), then start the waiting loop...

            long startTime = Calendar.getInstance().getTimeInMillis();
            long endWaitTime = startTime + maxWaitMilliseconds; // Set the maximum amount of time that we are willing to wait

            while (isWaiting) { // Changed to true through the handleMessage() function
                // If we are still waiting...

                // Check the current time
                long curTime = Calendar.getInstance().getTimeInMillis();
                if (curTime >= endWaitTime) {
//                sendToast("Error retrieving " + BluetoothByteList.contentTypeToString(curWaitingContent) + " from " + wheelLocToString(curWaitingWheelLoc) + " wheel. Skipping.");
//                Log.e(TAG, "Could not receive " + BluetoothByteList.contentTypeToString(curWaitingContent) + " from " + wheelLocToString(curWaitingWheelLoc) + " wheel.");

                    // Execute the timeout function defined by the user, and break out of this loop (should be a relatively safe fail-state)
                    timeoutFun(curWaitingContent, curWaitingWheelLoc);
                    Log.d(TAG, "Wait for '" + BluetoothByteList.contentTypeToString(curWaitingContent) + "' data unsuccessful; timed out.");
                    return false;
                }

                // Otherwise, wait another few milliseconds before checking for data again
                try {
                    TimeUnit.MILLISECONDS.sleep(200); // Wait between checks for the new data
                } catch (InterruptedException e) {
                    Log.e(TAG, e.toString());
                }
            }

            Log.v(TAG, "    Waited " + (((float) (Calendar.getInstance().getTimeInMillis() -  startTime))/1000) + "s.");
        } else {
            Log.v(TAG, "    No wait.");
        }
        // Ensure that the thread is no longer in a "waiting" state
        isWaiting = false;


        // If we've reached here, then we've successfully received our data
        return true;
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

                // Extract the metadata from this list
                BluetoothByteList readByteList = new BluetoothByteList(readBuf, readNumBytes);

                // Determine if this message matches what we are looking for
                boolean isRequest = readByteList.isRequest();
                boolean isCorrectWheel = readWheelID == curWaitingWheelLoc;
                boolean isCorrectContent = readByteList.getContentType() == curWaitingContent;

                if (isCorrectWheel && !isRequest && isCorrectContent) {
                    // If this is content (or a confirmation, but not a request) from the correct wheel, that corresponds to the type of information that we want, then stop waiting
                    isWaiting = false;
                }

                Log.v(TAG, "Received some content.");

            }
        }
    }
}

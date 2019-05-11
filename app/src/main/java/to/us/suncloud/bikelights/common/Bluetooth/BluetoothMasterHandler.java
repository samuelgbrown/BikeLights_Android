package to.us.suncloud.bikelights.common.Bluetooth;

import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;


public class BluetoothMasterHandler extends Handler {
    private ArrayList<HandlerInt> mHandlerList = new ArrayList<>();

    // Interface that an object must implement if it will be notified by this Handler
    public interface HandlerInt {
        void handleMessage(Message msg);
    }

    public BluetoothMasterHandler() {}

    public void registerHandler(HandlerInt newHandler) {
        if (!mHandlerList.contains(newHandler)) {
            mHandlerList.add(newHandler);
        }
    }

    public void unregisterHandler(HandlerInt handlerToRemove) {
        // Remove a handler (used by ConnectionManager
        if (mHandlerList.contains(handlerToRemove)) {
            mHandlerList.remove(handlerToRemove);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        // Send this it to all registered HandlerInt's for processing
        if (!mHandlerList.isEmpty()) {
            for (HandlerInt h : mHandlerList) {
                h.handleMessage(msg);
            }
        }

    }
}

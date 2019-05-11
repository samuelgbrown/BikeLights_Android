package to.us.suncloud.bikelights.common.Bluetooth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import java.io.Serializable;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Constants;

public class ReplaceDeviceDialog extends DialogFragment {
    private static String wheelIDKey = "WHEEL_ID";
    private static String deviceIDKey = "DEVICE_ID";
    private static String ARG_LISTENER = "listener";

    private int mID;
    private BluetoothDevice mDevice;

    public static ReplaceDeviceDialog newInstance(ReplaceDeviceInt listener, BluetoothDevice device, int id) {
        Bundle args = new Bundle();
        args.putInt(wheelIDKey, id);
        args.putParcelable(deviceIDKey, device);
        args.putSerializable(ARG_LISTENER, listener);

        ReplaceDeviceDialog fragment = new ReplaceDeviceDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Save arguments from the bundle
        Bundle args = getArguments();
        if (args != null) {
            // It damn well better not be null...
            if (args.containsKey(ARG_LISTENER)) {
                mListener = (ReplaceDeviceInt) args.getSerializable(ARG_LISTENER);
            }

            if (args.containsKey(wheelIDKey)) {
                mID = args.getInt(wheelIDKey);
            }

            if (args.containsKey(deviceIDKey)) {
                mDevice = args.getParcelable(deviceIDKey);

            }
        }

        // Use a dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_replace_device);
        builder.setPositiveButton(R.string.replace, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendChoice(Constants.ACTION_REPLACE);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendChoice(0);
            }
        });

        return builder.create();
    }

    ReplaceDeviceDialog.ReplaceDeviceInt mListener;

    private void sendChoice(int choice) {
        // Send the choice to the listener, including the wheel ID and the device
        mListener.replaceDeviceChoice(choice, mDevice, mID);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

//        try {
//            mListener = (ReplaceDeviceDialog.ReplaceDeviceInt) context;
//        } catch(ClassCastException e) {
//            throw new ClassCastException(context.toString() + " must implement ReplaceDeviceDialog.ReplaceDeviceInt");
//        }

    }

    public interface ReplaceDeviceInt extends Serializable {
        void replaceDeviceChoice(int choice, BluetoothDevice device, int id);
    }
}

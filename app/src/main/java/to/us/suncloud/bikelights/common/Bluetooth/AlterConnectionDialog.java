package to.us.suncloud.bikelights.common.Bluetooth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import java.io.Serializable;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Constants;

public class AlterConnectionDialog extends DialogFragment {
    private static final String ARG_LISTENER = "listener";

    AlterConnectionDialog.alterConnectionInt mListener;

    public static AlterConnectionDialog newInstance(AlterConnectionDialog.alterConnectionInt listener) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_LISTENER, listener);

        AlterConnectionDialog fragment = new AlterConnectionDialog();
        fragment.setArguments(args);
        return fragment;
    }
    @Nullable
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Extract and save the listener
        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ARG_LISTENER)) {
                mListener = (alterConnectionInt) args.getSerializable(ARG_LISTENER);
            }
        }

        // Use a dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_alter_connection);
        builder.setPositiveButton(R.string.disconnect, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.alterConnectionChoice(Constants.ACTION_DISCONNECT);
            }
        });
        builder.setPositiveButton(R.string.switch_wheels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.alterConnectionChoice(Constants.ACTION_SWITCH);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.alterConnectionChoice(0);
            }
        });

        return builder.create();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

//        try {
//            mListener = (AlterConnectionDialog.alterConnectionInt) context;
//        } catch(ClassCastException e) {
//            throw new ClassCastException(context.toString() + " must implement AlterConnectionDialog.alterConnectionInt");
//        }

    }

    public interface alterConnectionInt extends Serializable {
        void alterConnectionChoice(int choice);
    }
}

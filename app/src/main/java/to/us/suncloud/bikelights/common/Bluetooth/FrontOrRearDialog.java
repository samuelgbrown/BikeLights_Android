package to.us.suncloud.bikelights.common.Bluetooth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import java.io.Serializable;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Constants;

public class FrontOrRearDialog extends DialogFragment {
    private static final String ARG_LISTENER = "listener";

    FrontRearInt mListener;

    public static FrontOrRearDialog newInstance(FrontRearInt listener) {

        Bundle args = new Bundle();
        args.putSerializable(ARG_LISTENER, listener);

        FrontOrRearDialog fragment = new FrontOrRearDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Extract and save the listener
        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ARG_LISTENER)) {
                mListener = (FrontRearInt) args.getSerializable(ARG_LISTENER);
            }
        }

        // Use a dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                mListener.frontRearChoice(responseInt);
            }
        });
        Dialog d = builder.create();
        return d;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

//        try {
//            mListener = (FrontRearInt) context;
//        } catch(ClassCastException e) {
//            throw new ClassCastException(context.toString() + " must implement FrontOrRearDialog.FrontRearInt");
//        }

    }

    public interface FrontRearInt extends Serializable {
        void frontRearChoice(int choice);
    }
}

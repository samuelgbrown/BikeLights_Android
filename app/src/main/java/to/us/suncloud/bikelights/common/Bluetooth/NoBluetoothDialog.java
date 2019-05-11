package to.us.suncloud.bikelights.common.Bluetooth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import java.io.Serializable;

import to.us.suncloud.bikelights.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NoBluetoothInt} interface
 * to handle interaction events.
 */
public class NoBluetoothDialog extends DialogFragment {
    private static final String ARG_LISTENER = "listener";

    private NoBluetoothInt mListener;

    public NoBluetoothDialog() {
        // Required empty public constructor
    }

    public static NoBluetoothDialog newInstance(NoBluetoothInt listener) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_LISTENER, listener);

        NoBluetoothDialog fragment = new NoBluetoothDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ARG_LISTENER)) {
                mListener = (NoBluetoothInt) args.getSerializable(ARG_LISTENER);
            }
        }

        final NoBluetoothDialog thisDialog = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(R.string.no_bluetooth_message)
                .setPositiveButton(R.string.no_bluetooth_close, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mListener.onNoBluetoothDialogComplete(thisDialog);
                            }
                        }
                );
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof NoBluetoothInt) {
//            mListener = (NoBluetoothInt) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement ModColorFragmentListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface NoBluetoothInt extends Serializable {
        void onNoBluetoothDialogComplete(NoBluetoothDialog fragment);
    }
}

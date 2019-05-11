package to.us.suncloud.bikelights.common.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import to.us.suncloud.bikelights.R;

///**
// * A simple {@link Fragment} subclass.
// * Activities that contain this fragment must implement the
// * {@link BluetoothFragment.ModColorFragmentListener} interface
// * to handle interaction events.
// * Use the {@link BluetoothFragment#newInstance} factory method to
// * create an instance of this fragment.
// */
public class BluetoothFragment extends DialogFragment implements BluetoothMasterHandler.HandlerInt {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PAIRED_DEVICES = "pairedDevices";
    private static final String CONNECTION_MANAGER = "ConnectionManager";

    // Keep track of important parameters
    private Set<BluetoothDevice> mPairedDevices;
    private Set<BluetoothDevice> mAvailableDevices;

    private Button scanButton; // The button that the user can use to scan for Bluetooth devices

    BluetoothRecyclerView availableRecyclerView;
    BluetoothRecyclerView pairedRecyclerView;

    BluetoothRecyclerAdapter availableAdapter;
    BluetoothRecyclerAdapter pairedAdapter;

    private BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
    private ConnectionManager mManager; // The connection manager created by the main activity

    // Create a broadcast receiver for bluetooth discovery
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // A bluetooth device was found!
                BluetoothDevice thisDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Add the device to the available devices list
                modifyDevicesList(thisDevice, deviceListType.AVAILABLE, deviceAction.ADD);
            }

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                // A bluetooth device's state has changed
                BluetoothDevice thisDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (thisDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    // This device is newly bonded, add it to the paired devices list
                    modifyDevicesList(thisDevice, deviceListType.PAIRED, deviceAction.ADD);
                } else if (thisDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    // This device is no longer bonded, remove from paired devices list
                    modifyDevicesList(thisDevice, deviceListType.PAIRED, deviceAction.REMOVE);
                }
            }

            if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
                // A bluetooth device's name has changed
                BluetoothDevice thisDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Replace the device in BOTH lists (if it does not exist in one of the lists, then it will be skipped)
                modifyDevicesList(thisDevice, deviceListType.PAIRED, deviceAction.REPLACE);
                modifyDevicesList(thisDevice, deviceListType.AVAILABLE, deviceAction.REPLACE);
            }

        }
    };

    private enum deviceListType {
        AVAILABLE,
        PAIRED
    }

    private enum deviceAction {
        ADD,
        REMOVE,
        REPLACE
    }

    private void modifyDevicesList(BluetoothDevice device, deviceListType type, deviceAction action) {
        // Get the original list, as well as the relevant adapter
        ArrayList<BluetoothDevice> newDeviceList;
        BluetoothRecyclerAdapter adapter;
        switch (type) {
            case PAIRED:
                newDeviceList = new ArrayList<>(mPairedDevices);
                adapter = pairedAdapter;
                break;
            case AVAILABLE:
                newDeviceList = new ArrayList<>(mAvailableDevices);
                adapter = availableAdapter;
                break;
            default:
                // If there was a problem, do nothing
                return;
        }
        ArrayList<BluetoothDevice> oldDeviceList = newDeviceList;

        // Add or remove the device as needed
        switch (action) {
            case ADD:
                newDeviceList.add(device);
                break;
            case REMOVE:
                if (newDeviceList.size() > 0) {
                    newDeviceList.remove(device);
                } else {
                    // If trying to remove a device from a size() == 0 list, skip this
                    return;
                }
                break;
            case REPLACE:
                if (newDeviceList.size() > 0) {
                    newDeviceList.remove(device);
                    // This one's a bit more complicated, I think...we need to find the device with the same address and replace the name.

                    // Loop through all devices in the list, looking for the device with the same address as "device"
                    for (int i = 0; i < newDeviceList.size(); i++) {
                        if (newDeviceList.get(i).getAddress().equals(device.getAddress())) {
                            // If we have found the device in question, remove the old one, add the new version ("device") in the same location, and break out of the loop
                            newDeviceList.remove(i);
                            newDeviceList.add(i, device);
                            break;
                        }
                    }
                } else {
                    // If trying to replace a device from a size() == 0 list, skip this
                    return;
                }
                break;
        }

        // Save the new device list to its original member variable for future reference
        switch (type) {
            case PAIRED:
                mPairedDevices = new HashSet<>(newDeviceList);
                break;
            case AVAILABLE:
                mAvailableDevices = new HashSet<>(newDeviceList);
                break;
        }

        // Update the adapter, if one has been assigned
        if (adapter != null) {
            // Use DiffUtil to calculate the difference between the two device lists
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DeviceDiffUtil(oldDeviceList, newDeviceList));

            // Deploy those differences to the adapter
            result.dispatchUpdatesTo(adapter);
        }
    }

    public BluetoothFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
     * @return A new instance of fragment BluetoothFragment.
     */
    public static BluetoothFragment newInstance(ConnectionManager manager) {
        BluetoothFragment fragment = new BluetoothFragment();

        // Get the current paired devices, and add to the fragment
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PAIRED_DEVICES, (Serializable) pairedDevices);
        args.putSerializable(CONNECTION_MANAGER, manager);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize bluetooth device lists
        if (getArguments() != null) {
            mPairedDevices = (Set<BluetoothDevice>) getArguments().getSerializable(ARG_PAIRED_DEVICES);
            mManager = (ConnectionManager) getArguments().getSerializable(CONNECTION_MANAGER);
        }
        mAvailableDevices = new HashSet<>();

        // Register for broadcasts when a device is found, a device's state is changed, a device's name has change, or discovery begins/ends.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);

        // Register the receiver (for getting Android announcements about Bluetooth Activity)
        getContext().registerReceiver(mReceiver, filter);

        // TODO: May not need to be a Message Handler?
        // Register this new fragment to the ConnectionManager (for getting custom Bluetooth announcements about the connection)
        mManager.registerHandler(this);

        // Start device discovery
        startDiscovery();
    }

    private void startDiscovery() {
        // Start device discovery, and alter the GUI accordingly
        defaultAdapter.startDiscovery();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bluetooth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get RecyclerViews
        availableRecyclerView = getView().findViewById(R.id.recyclerAvailable);
        pairedRecyclerView = getView().findViewById(R.id.recyclerPaired);

        // Get the scan button
        scanButton = getView().findViewById(R.id.button);

        // Set fixed size
        availableRecyclerView.setHasFixedSize(true);
        pairedRecyclerView.setHasFixedSize(true);

        // Set layout managers
        RecyclerView.LayoutManager layoutManagerAvailable = new LinearLayoutManager(getContext());
        availableRecyclerView.setLayoutManager(layoutManagerAvailable);

        RecyclerView.LayoutManager layoutManagerPaired = new LinearLayoutManager(getContext());
        pairedRecyclerView.setLayoutManager(layoutManagerPaired);

        // Set adapters
        availableAdapter = new BluetoothRecyclerAdapter(getContext(), mAvailableDevices, mManager);
        pairedAdapter = new BluetoothRecyclerAdapter(getContext(), mPairedDevices, mManager);

        availableRecyclerView.setAdapter(availableAdapter);
        pairedRecyclerView.setAdapter(pairedAdapter);

        // TODO: Remove?
        // Finally, set the empty text views to the recyclerviews (a custom subclassing to display a message to the user if there is nothing in the recyclerview dataset)
//        availableRecyclerView.setEmptyView(getView().findViewById(R.id.textAvailable_empty));
//        pairedRecyclerView.setEmptyView(getView().findViewById(R.id.textPaired_empty));

        // Set up the scan button
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDiscovery();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof ModColorFragmentListener) {
//            mListener = (ModColorFragmentListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement ModColorFragmentListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister the receiver (for getting Android announcements about Bluetooth Activity)
        getContext().unregisterReceiver(mReceiver);

        // Unregister this new fragment to the ConnectionManager (for getting custom Bluetooth announcements about the connection)
        mManager.unregisterHandler(this);
    }

    public void handleMessage(Message msg) {
        // If the ConnectionManager sends a message through Handler, it will end up here after the handler is done with it


    }

//    /**
//     * This interface must be implemented by activities that contain this
//     * fragment to allow an interaction in this fragment to be communicated
//     * to the activity and potentially other fragments contained in that
//     * activity.
//     * <p>
//     * See the Android Training lesson <a href=
//     * "http://developer.android.com/training/basics/fragments/communicating.html"
//     * >Communicating with Other Fragments</a> for more information.
//     */
//    public interface ModColorFragmentListener {
//        // Update argument type and name
////        void onAddRearBluetoothDevice();
////        void onAddFrontBluetoothDevice();
//    }

    private class DeviceDiffUtil extends DiffUtil.Callback {
        private ArrayList<BluetoothDevice> mOldList;
        private ArrayList<BluetoothDevice> mNewList;

        private DeviceDiffUtil(ArrayList<BluetoothDevice> oldList, ArrayList<BluetoothDevice> newList) {
            mOldList = oldList;
            mNewList = newList;
        }

        @Override
        public int getNewListSize() {
            return mNewList.size();
        }

        @Override
        public int getOldListSize() {
            return mOldList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Check if the addresses are the same
            boolean returnVal;
            try {
                returnVal = mOldList.get(oldItemPosition).getAddress().equals(mNewList.get(newItemPosition).getAddress());
            } catch (Exception e) {
                returnVal = false;
            }
            return returnVal;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // Check if the names are the same
            boolean returnVal;
            try {
                returnVal = mOldList.get(oldItemPosition).getName().equals(mNewList.get(newItemPosition).getName());
            } catch (Exception e) {
                returnVal = false;
            }
            return returnVal;
        }
    }
}

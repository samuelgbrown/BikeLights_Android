package to.us.suncloud.bikelights.common.BWA_Manager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Color.SavedBWA;
import to.us.suncloud.bikelights.common.LocalPersistence;
import to.us.suncloud.bikelights.common.ObservedRecyclerView;

import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link savedBWAListener}
 * interface.
 */
public class SavedBWAFragment extends DialogFragment {

    public final static String BWA_FILE = "BWA_FILE";

//    private final static String ARG_NEW_BWA = "ARG_NEW_BWA";
    private final static String ARG_WHEEL_LOC = "ARG_WHEEL_LOC";

    private savedBWAListener mListener;
    private SavedBWARecyclerViewAdapter adapter;

    ArrayList<SavedBWA> currentSavedBWAs;
    private int startingCount; // Number of BWAs that were in the fragment to begin with
    int wheelLocation; // The location of the wheel that this dialog must send a BWA back to
    boolean selecting; // Is the Fragment being called to select a BWA to apply to a Wheel?

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SavedBWAFragment() {

    }

    public static SavedBWAFragment newInstance() {

        Bundle args = new Bundle();

        SavedBWAFragment fragment = new SavedBWAFragment();
        fragment.setArguments(args);
        return fragment;

    }

//    public static SavedBWAFragment newInstance(SavedBWA newBWA) {
//
//        Bundle args = new Bundle();
//        args.putSerializable(ARG_NEW_BWA, newBWA);
//        SavedBWAFragment fragment = new SavedBWAFragment();
//        fragment.setArguments(args);
//        return fragment;
//    }

    public static SavedBWAFragment newInstance(int wheelLoc) {

        Bundle args = new Bundle();
        args.putInt(ARG_WHEEL_LOC, wheelLoc);
        SavedBWAFragment fragment = new SavedBWAFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public void tryToShow(Context context, FragmentManager fm, String tag) {
        // Check if the Fragment should be showed

        // First, check if the user supplied wheel location to the Fragment (indicating that we're expecting to select an animation to assign to a Wheel)
        Bundle args = getArguments();

        if (args.containsKey(ARG_WHEEL_LOC)) {
            wheelLocation = args.getInt(ARG_WHEEL_LOC);
            selecting = true;
        } else {
            selecting = false;
        }

        // First, read in the existing SavedBWA's from the file
        currentSavedBWAs = (ArrayList<SavedBWA>) LocalPersistence.readObjectFromFile(context, BWA_FILE);

        if (currentSavedBWAs == null) {
            if (selecting) {
                // If we're trying to select a BWA to use for a wheel, but none exist, then let the user know that they're a dumbass
                Toast.makeText(context, "No saved Animations found. Create one and bookmark it for later!", Toast.LENGTH_SHORT).show();
                return;
            } else {
                // If we're just managing BWA's, and there are no saved BWAs, create a new empty list of them
                currentSavedBWAs = new ArrayList<>();
            }
        }


        // Save the number of BWA's that we started with
        startingCount = currentSavedBWAs.size();

        // Finally, show the Fragment
        show(fm, tag);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        // First, check if the user supplied wheel location to the Fragment (indicating that we're expecting to select an animation to assign to a Wheel)
//        Bundle args = getArguments();
////        if (args.containsKey(ARG_NEW_BWA)) {
////            // Get the new BWA from the arguments
////            SavedBWA newBWA = (SavedBWA) args.getSerializable(ARG_NEW_BWA);
////
////            // Save this new SavedBWA to the current list
////            currentSavedBWAs.add(newBWA);
////        }
//
//        if (args.containsKey(ARG_WHEEL_LOC)) {
//            wheelLocation = args.getInt(ARG_WHEEL_LOC);
//            selecting = true;
//        } else {
//            selecting = false;
//        }
//
//        // First, read in the existing SavedBWA's from the file
//        currentSavedBWAs = (ArrayList<SavedBWA>) LocalPersistence.readObjectFromFile(getContext(), BWA_FILE);
//
//        if (currentSavedBWAs == null) {
//            if (selecting) {
//                // If we're trying to select a BWA to use for a wheel, but none exist, then let the user know that they're a dumbass
//                Toast.makeText(getContext(), "No saved Animations found. Create one and bookmark it for later!", Toast.LENGTH_SHORT).show();
//                dismiss();
//            } else {
//                // If we're just managing BWA's, and there are no saved BWAs, create a new empty list of them
//                currentSavedBWAs = new ArrayList<>();
//            }
//        }
//
//        // Save the number of BWA's that we started with
//        startingCount = currentSavedBWAs.size();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_savedbwa_list, container, false);

        // Set the adapter to the RecyclerView
        ObservedRecyclerView list = view.findViewById(R.id.bwa_recyclerView);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(view.getContext()));
        adapter = new SavedBWARecyclerViewAdapter(currentSavedBWAs, selecting);
        View emptyView = view.findViewById(R.id.bwa_empty_view);
        list.setEmptyView(emptyView); //, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        list.setAdapter(adapter);

        // Set up the message about selecting a BWA
        View selectingMessage = view.findViewById(R.id.bwa_selecting_message);
        if (selecting) {
            selectingMessage.setVisibility(View.VISIBLE);
        } else {
            selectingMessage.setVisibility(View.GONE);
        }

        // Set up the buttons
        Button cancel = view.findViewById(R.id.bwa_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.getSavedBWA_List().size() != startingCount) {
                    // If changes have occurred, check if the user wants to save them
                    checkIfWantSave();
                } else {
                    // Close this dialog fragment
                    dismiss();
                }
            }
        });

        Button done = view.findViewById(R.id.bwa_done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selecting) {
                    // If the user is SUPPOSED to be returning a BWA to save to a wheel
                    if (adapter.hasSelection()) {
                        // If the user HAS selected a BWA...

                        // Send the BWA back to the calling Activity
                        mListener.receiveSelectedBWA(adapter.getSelectedBWA(), wheelLocation);

                        // Save the BWAs to file and close the dialog
                        saveBWAsAndClose();
                    } else {
                        // Let the user know that they're a dumbass
                        Toast.makeText(getContext(), "Select an animation to assign!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Save the BWAs to file and close the dialog
                    saveBWAsAndClose();
                }
            }
        });

        return view;
    }

    private void checkIfWantSave() {
        new AlertDialog.Builder(getContext())
                .setMessage(getResources().getString(R.string.dialog_save_bwa))
                .setPositiveButton(getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveBWAsAndClose();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.discard), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Close the dialog
                        dismiss();
                    }
                })
                .show();
    }

    private void saveBWAsAndClose() {
        // Save the BWAs list to file
        LocalPersistence.writeObjectToFile(getContext(), adapter.getSavedBWA_List(), BWA_FILE);

        // Close the dialog
        dismiss();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof savedBWAListener) {
            mListener = (savedBWAListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement savedBWAListener");
        }
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
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface savedBWAListener {
        void receiveSelectedBWA(SavedBWA item, int wheelLocation);
    }
}

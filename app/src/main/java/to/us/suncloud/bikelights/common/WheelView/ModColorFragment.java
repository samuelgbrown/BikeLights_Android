package to.us.suncloud.bikelights.common.WheelView;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Color.Color_;
import to.us.suncloud.bikelights.common.Color.Color_Static;
import to.us.suncloud.bikelights.common.Constants;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ModColorFragmentListener} interface
 * to handle interaction events.
 * Use the {@link ModColorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ModColorFragment extends DialogFragment {
    private final int colorDefLayout_r = R.id.colorDefRecyclerView;

    private static final String ARG_INPUTCOLOR_ = "Color_";
//    private static final String ARG_LISTENER = "ModFragmentListener";
    private static final String ARG_POSITION = "Color_Position";
    private String currentColorChoiceStr;

    private ColorDefRecyclerView colorDefRecyclerView;
    private ColorDefRecyclerAdapter colorDefRecyclerAdapter;
    private ImageView colorViewMain; // The ImageView that represents the main view of the color as it currently is recorded
    private Button cancelButton;
    private Button applyButton;

    private ModColorFragmentListener mListener;
    private int colorPosition = -1; // The position in the calling Activity's Bike_Wheel_Animation that this modified/new Color_ will replace, initialize to -1 which represents a new Color_ to be added on the end of the Bike_Wheel_Animation

    public ModColorFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment with a new Color_.
     *
     * @return A new instance of fragment ModColorFragment.
     */
    public static ModColorFragment newInstance() { // ModColorFragmentListener mListener) {
        // Create a new Color_Static to modify
        ModColorFragment fragment = new ModColorFragment();
        Bundle args = new Bundle();
//        args.putSerializable(ARG_LISTENER, mListener);
//        args.putSerializable(ARG_POSITION, colorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using a pre-existing Color_.
     *
     * @param newColor_ The Color_ object to modify.
     * @return A new instance of fragment ModColorFragment.
     */
    public static ModColorFragment newInstance(Color_ newColor_, int colorPosition) { //ModColorFragmentListener mListener) {
        // Accept an existing Color_ to modify
        ModColorFragment fragment = new ModColorFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_INPUTCOLOR_, newColor_);
        args.putInt(ARG_POSITION, colorPosition);
//        args.putSerializable(ARG_LISTENER, mListener);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();

        Color_ colorToBeModified;
        if (arguments != null) {
            if (arguments.containsKey(ARG_INPUTCOLOR_)) {
                // If there is a Color_ passed to the Fragment, then retrieve it.
                colorToBeModified = (Color_) arguments.getSerializable(ARG_INPUTCOLOR_);
                colorPosition = arguments.getInt(ARG_POSITION);
            } else {
                // If no Color_ was passed to the Fragment, then create a brand new Color_Static
                colorToBeModified = new Color_Static();
            }
        } else {
            colorToBeModified = new Color_Static();
        }

        // Extract the listener to this fragment (which better damn well be there, so I'm not checking for its existence in the arguments!)
//        mListener = (ModColorFragmentListener) arguments.getSerializable(ARG_LISTENER);

        // Add this colorToBeModified to the Recycler Adapter
        colorDefRecyclerAdapter = new ColorDefRecyclerAdapter(colorToBeModified); // Includes doing the job of "convertDialogToColorChoice"

        currentColorChoiceStr = getColorTypeString(colorToBeModified.getColorType()); // Set the current color choice string (really only used once, it initialize the bSpinner...which I think needs to be done in the OnCreateView method...)
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layoutView = inflater.inflate(R.layout.fragment_mod_color, container, false);

        // Set up the cancel button for the animation TO_DO: Test
//        Button cancelAnim = layoutView.findViewById(R.id.anim_cancel_button);
//        cancelAnim.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                colorDefRecyclerAdapter.cancelAnim();
//            }
//        });

        // Pick out the main color view and pass it to the adapter
        colorViewMain = layoutView.findViewById(R.id.colorViewMain);
        colorDefRecyclerAdapter.setColorViewMain(colorViewMain);

        // Set up the RecyclerView
        colorDefRecyclerView = layoutView.findViewById(colorDefLayout_r);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        colorDefRecyclerView.setLayoutManager(layoutManager);
//        LinearSnapHelper lsh = new LinearSnapHelper();
//        lsh.attachToRecyclerView(colorDefRecyclerView);
        colorDefRecyclerView.setAdapter(colorDefRecyclerAdapter); // Set the adapter for this RecyclerView

        // Set up colorTypeSpinner
        Spinner spinner = layoutView.findViewById(R.id.colorTypeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(layoutView.getContext(), R.array.color_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the choice as a string
                String choice = (String) parent.getItemAtPosition(position);

                // Determine which Color_ the user would like to use
                convertDialogToColorChoice(choice);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO: SAM: Do nothing, I think?  Test.
            }
        });

        // Initialize the spinner selection
        int selectionInd = adapter.getPosition(currentColorChoiceStr);
        spinner.setSelection(selectionInd);

        // Set up the cancel and apply buttons
        cancelButton = layoutView.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        applyButton = layoutView.findViewById(R.id.apply_button);
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.receiveModifiedColor_(colorDefRecyclerAdapter.getColor_(), colorPosition);
                dismiss();
            }
        });

        return layoutView;
    }

    private void convertDialogToColorChoice(String colorString) {
        // colorString can be the string representation of one of: R.string.Color_Static, R.string.Color_dTime, R.string.Color_dVel

        // Save the new colorString (not sure if this is really needed, but it just feels wrong to leave currentColorChoiceStr without any updates...)
        currentColorChoiceStr = colorString;

        if (getResources().getString(R.string.Color_Static).equals(colorString)) {
            // Prepare the color object view (linking up callbacks and such), with no index as this is for a static color (is completely described by a single colorObj)
            colorDefRecyclerAdapter.setColorType(Constants.COLOR_STATIC);
            // prepareColorObjView_Static();

        } else {
            // Working with one of the dynamic colors. Specifically using...
            if (getResources().getString(R.string.Color_dTime).equals(colorString)) {
                // ...a Color_dTime
                colorDefRecyclerAdapter.setColorType(Constants.COLOR_DTIME);
            } else if (getResources().getString(R.string.Color_dSpeed).equals(colorString)) {
                // ...a Color_dVel
                colorDefRecyclerAdapter.setColorType(Constants.COLOR_DSPEED);
            }
        }

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ModColorFragmentListener) {
            mListener = (ModColorFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ModColorFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private String getColorTypeString(int colorTypeConstantVal) {
        switch (colorTypeConstantVal) {
            case Constants.COLOR_STATIC:
                return getContext().getResources().getString(R.string.Color_Static);
            case Constants.COLOR_DTIME:
                return getContext().getResources().getString(R.string.Color_dTime);
            case Constants.COLOR_DSPEED:
                return getContext().getResources().getString(R.string.Color_dSpeed);
            default:
                return getContext().getResources().getString(R.string.Color_Static);
        }
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
    public interface ModColorFragmentListener {
        void receiveModifiedColor_(Color_ newColor, int colorPos); // A new Color_ to replace the one at position colorPos (if colorPos == -1, then it represents a new color)
    }
}

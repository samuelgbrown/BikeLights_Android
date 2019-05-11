package to.us.suncloud.bikelights;

import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.InflateException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import to.us.suncloud.bikelights.common.BWA_Manager.SavedBWAFragment;
import to.us.suncloud.bikelights.common.Color.Color_;
import to.us.suncloud.bikelights.common.Color.SavedBWA;
import to.us.suncloud.bikelights.common.Constants;
import to.us.suncloud.bikelights.common.LocalPersistence;
import to.us.suncloud.bikelights.common.WheelView.ImageDefinePagerAdapter;
import to.us.suncloud.bikelights.common.Color.Bike_Wheel_Animation;
import to.us.suncloud.bikelights.common.ObservedRecyclerView;
import to.us.suncloud.bikelights.common.WheelView.ModColorFragment;
import to.us.suncloud.bikelights.common.WheelView.ColorListRecyclerAdapter;

public class WheelViewActivity extends AppCompatActivity implements ModColorFragment.ModColorFragmentListener, ColorListRecyclerAdapter.ColorListInterface { //, ImageDefineFragment.ImageDefineInterface {
    private final static String TAG = "WheelViewActivity";

    public final static String BWA_FROM_BOOKMARK = "BWA_FROM_BOOKMARK";
    private int wheelLocation; // The location of the wheel (front or rear) that this Activity represents (as either Constant.ID_Front or Constant.ID_Rear)

//    private int rotationSpeed = 0; // Rotation speed of the image on the wheel

    private boolean bwaIsFromBookmark = false; // Was this Activity started because the user selected a bookmark?  If so, we should assume that the user wants a chance to save before accidentally exiting (the user may want to save even without making any changes, which they wouldn't get a chance to without this check)
    //    private View wheelView; // The View that holds the image of the currently selected wheel
    private Spinner imageMetaSpinner; // Spinner to choose the type of Image meta data to store (rotation or inertia [for spinner]?
    //    private int imageMetaType = Constants.IMAGE_CONSTROT;; // The type of image meta data (constant rotation, spinner, etc.)
    private ImageDefinePagerAdapter imageDefineAdapter; // Fragment to define the meta-attribute(s) of the image (rotation speed, inertia of spinner, etc)

    private Bike_Wheel_Animation originalBikeWheelAnimation; // The version of the Bike_Wheel_Animation that was given to this Activity, left unchanged to compare to later
    private Bike_Wheel_Animation bikeWheelAnimation; // The Bike_Wheel_Animation that represents the culmination of all editables in this activity

//    private ImageMeta_ image_choice_current = new Image_Meta_ConstRot(); // Current image meta object
//    private Image_Meta_ConstRot image_choice_constrot = new Image_Meta_ConstRot();
//    private Image_Meta_Spinner image_choice_spinner = new Image_Meta_Spinner();


    private ObservedRecyclerView color_RecyclerView;
    private ColorListRecyclerAdapter wheelListAdapter; // Responsible for keeping track of the Bike_Wheel_Animation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wheel_view);

        // TODO: Learn about savedInstanceState vs thisIntent.getExtras, and deal with inputs from savedInstanceState

        // Extract data from input
        Intent thisIntent = getIntent();
        Bundle inputBundle = thisIntent.getExtras();
        if (inputBundle.containsKey(MainActivity.BIKE_WHEEL_ANIMATION)) {
            originalBikeWheelAnimation = (Bike_Wheel_Animation) inputBundle.getSerializable(MainActivity.BIKE_WHEEL_ANIMATION);
        } else {
            originalBikeWheelAnimation = new Bike_Wheel_Animation(getResources().getInteger(R.integer.num_leds));
        }

        // Keep track of an untouched version of the original BikeWheelAnimation, while making a version that can be modified and updated by the Activity
        bikeWheelAnimation = originalBikeWheelAnimation.clone();

        // Save the location of the wheel this Activity represents
        if (inputBundle.containsKey(MainActivity.WHEEL_LOCATION)) {
            wheelLocation = inputBundle.getInt(MainActivity.WHEEL_LOCATION); // Get the wheel location from the Bundle
        } else {
            // Uh-oh...
            Log.e(TAG, "Did not find wheel location in Bundle");
        }

        // Check if this BWA is from a bookmark
        if (inputBundle.containsKey(BWA_FROM_BOOKMARK)) {
            bwaIsFromBookmark = inputBundle.getBoolean(BWA_FROM_BOOKMARK);
        }

//        // Get the image meta data
//        if (inputBundle.containsKey(MainActivity.IMAGE_META)) {
//            // TODO: Make sure that MainActivity handles the ImageMeta_ object correctly
//            setImageMeta((ImageMeta_) inputBundle.getSerializable(MainActivity.IMAGE_META));
//        } else {
//            setImageMeta(new Image_Meta_ConstRot(0));
//        }

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.wheelToolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(false);


        // Set up the ImageDefineFragment adapter with the ViewPager
        ViewPager viewPager = findViewById(R.id.image_viewpager);
        imageDefineAdapter = new ImageDefinePagerAdapter(bikeWheelAnimation, getSupportFragmentManager());
        viewPager.setAdapter(imageDefineAdapter);

        // Give the ViewPager to the TabLayout
        TabLayout tabLayout = findViewById(R.id.image_Tabs);
        tabLayout.setupWithViewPager(viewPager);

        // Set up the spinner for the Image Meta type
        imageMetaSpinner = findViewById(R.id.image_meta_spinner);
        ArrayAdapter imageMetaSpinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.image_meta_types, android.R.layout.simple_spinner_item);
        imageMetaSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        imageMetaSpinner.setAdapter(imageMetaSpinnerAdapter);
        imageMetaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String choice = (String) parent.getItemAtPosition(position);

                if (getResources().getString(R.string.Constant_Speed).equals(choice)) {
                    imageDefineAdapter.setImageMeta(Constants.IMAGE_CONSTROT);
                } else if (getResources().getString(R.string.Spinner_Wheel).equals(choice)) {
                    imageDefineAdapter.setImageMeta(Constants.IMAGE_SPINNER);
                } else {
                    // Uh oh...
                    Log.e(TAG, "Got illegal ImageMetaSpinner choice.");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        int imageMetaSpinnerChoice = imageMetaTypeToSpinnerPos(originalBikeWheelAnimation.getImageMeta().getImageType());
        imageMetaSpinner.setSelection(imageMetaSpinnerChoice, false); // Set the original image meta type in the GUI

//        constRot_Container = findViewById(R.id.constrot_container);
//        spinner_Container = findViewById(R.id.spinner_container);


        // Assign this wheel list adapter to the RecyclerView
        wheelListAdapter = new ColorListRecyclerAdapter(bikeWheelAnimation.getPalette(), this); //, imageDefineFragment.getLedViewDrawable());

        // Set up the RecyclerView for the Color_'s
        color_RecyclerView = findViewById(R.id.wheelList);
        color_RecyclerView.setHasFixedSize(true);

        View emptyView = findViewById(R.id.wheelListEmptyView);
        color_RecyclerView.setEmptyView(emptyView); //, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Set the layout managers
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getBaseContext());
        color_RecyclerView.setLayoutManager(layoutManager);
        color_RecyclerView.setAdapter(wheelListAdapter);

    }

    private int imageMetaTypeToSpinnerPos(int imageMetaType) {
        switch (imageMetaType) {
            case Constants.IMAGE_CONSTROT:
                return 0;
            case Constants.IMAGE_SPINNER:
                return 1;
            default:
                // Uh-oh...
                return 0;
        }
    }

//    @Override
//    public void imageAttrUpdate(int newAttrVal) {
//
//    }

//    private void setImageMeta(int newImageType) {setImageMeta(newImageType, true);}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the toolbar menu (for now, not inflating it so that only the "up" button will be used)
        getMenuInflater().inflate(R.menu.menu_wheel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get the id of this menu item
        int id = item.getItemId();

        switch (id) {
            case R.id.wheel_save:
                // Save the information in this activity, and close it
                setThisResult();

                break;
            case R.id.wheel_bookmark:
                // Bookmark the animation as it exists now
                bookmarkBWAIfNew();

                break;
            case android.R.id.home:
                // If up navigation is utilized

//                // Save the information in this activity
//                setThisResult();

                // Ask the user if they want to save
                checkIfSave();
            default:
                // Uh oh...
        }

        return true;
    }

    private void bookmarkBWAIfNew() {
        // This function will bookmark the current BWA if it does not exist in the currently saved BWA's

        // First, get the saved BWA's
        ArrayList<SavedBWA> curSavedBWAs = (ArrayList<SavedBWA>) LocalPersistence.readObjectFromFile(getApplicationContext(), SavedBWAFragment.BWA_FILE);

        // See if the currently bookmarked BWA contain this BWA
        SavedBWA thisBWA = new SavedBWA("", getNewBikeWheelAnimation()); // A SavedBWA object that represents this BWA (the name does not get compared in the test for equality)
        if (curSavedBWAs != null && curSavedBWAs.contains(thisBWA)) {
            // If this BWA DOES exist in the bookmarks, simply let the user know via Toast
            int bwaLoc = curSavedBWAs.indexOf(thisBWA);
            Toast.makeText(this, "This Animation is already bookmarked as \"" + curSavedBWAs.get(bwaLoc).getSaveName() + "\"!", Toast.LENGTH_SHORT).show();
        } else {
            // If this BWA does NOT exist in the bookmarks, allow the user to same the animation and save it
            View layoutView = getLayoutInflater().inflate(R.layout.layout_bwa_bookmark, null); // First, inflate the view, so that the editText can be extracted
            final EditText bookmarkText = layoutView.findViewById(R.id.bookmark_name);

            final InputMethodManager imm = (InputMethodManager) bookmarkText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            new AlertDialog.Builder(this)
                    .setView(layoutView)
                    .setPositiveButton(getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Hide the keyboard (why meeeeee...)
                            imm.hideSoftInputFromWindow(bookmarkText.getWindowToken(), 0);

                            // Save this BWA with the String from the dialog
                            saveBWA(bookmarkText.getText().toString());
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Hide the keyboard (stupid Android...)
                            imm.hideSoftInputFromWindow(bookmarkText.getWindowToken(), 0);
                        }
                    })
                    .show();

            // Make the EditText show the keyboard
            bookmarkText.requestFocus();
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    private void saveBWA(String name) {
        // Create a SavedBWA from the current BWA and the name
        SavedBWA savedBWA = new SavedBWA(name, getNewBikeWheelAnimation());

        // Load the currently saved bookmarked BWAs
        ArrayList<SavedBWA> curSavedBWAs = (ArrayList<SavedBWA>) LocalPersistence.readObjectFromFile(getApplicationContext(), SavedBWAFragment.BWA_FILE);

        // Save the new BWA to the curSavedBWAs List
        if (curSavedBWAs == null) {
            curSavedBWAs = new ArrayList<>();
        }
        curSavedBWAs.add(savedBWA);

        // Finally, save this newly appended list to the file
        LocalPersistence.writeObjectToFile(getApplicationContext(), curSavedBWAs, SavedBWAFragment.BWA_FILE);
    }

    @Override
    public void onBackPressed() {
        checkIfSave();
    }

    @Override
    public void receiveModifiedColor_(Color_ newColor, int colorPos) {
        wheelListAdapter.setModColorResults(newColor, colorPos);
    }

    private void checkIfSave() {
        // If the Bike_Wheel_Animation created by this Activity is different than the original one that started it, then allow the user to save before exiting, or cancel the exiting process altogether
        Bike_Wheel_Animation newBWA = getNewBikeWheelAnimation();
        if (bwaIsFromBookmark || !newBWA.equals(originalBikeWheelAnimation)) {
            // If the user made changes, OR this BWA was selected from the bookmarks screen
            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(R.string.dialog_save_wheel))
                    .setPositiveButton(getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Save the results and close this activity
                            setThisResult();
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.discard), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Close this activity without saving
                            finish();
                        }
                    })
                    .setNeutralButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Close this dialog without doing anything else
                        }
                    })
                    .show();
        } else {
            setThisResult();
        }
    }

    private void setThisResult() {
        // This activates the setResult() function, loading it with an Intent that contains the Bike_Wheel_Animation loaded in this Activity
        // Create a new Intent with the information to send back
        Intent intent = new Intent();
        intent.putExtra(MainActivity.WHEEL_LOCATION, wheelLocation); // The location (rear/front) of this wheel

        Bike_Wheel_Animation newBWA = getNewBikeWheelAnimation();
        intent.putExtra(MainActivity.BIKE_WHEEL_ANIMATION, getNewBikeWheelAnimation()); // The list of Color_'s, the images (of both main and idle, if needed), and image meta data

        // TODO: Send the imagestack back, so the user can preserve their undo/redo buffer?
        // Set the result
        setResult(Activity.RESULT_OK, intent);

        // Close this activity
        finish();
    }

    Bike_Wheel_Animation getNewBikeWheelAnimation() {
        // Build a Bike_Wheel_Animation object
        Bike_Wheel_Animation newBikeWheelAnimation = new Bike_Wheel_Animation(bikeWheelAnimation);
        newBikeWheelAnimation.setPalette(wheelListAdapter.getColorList());
        newBikeWheelAnimation = imageDefineAdapter.putImages(newBikeWheelAnimation);
        return newBikeWheelAnimation;
    }

//    public Bike_Wheel_Animation getColorList() {
//        if (wheelListAdapter != null) {
//            return wheelListAdapter.getColorList();
//        } else {
//            return new Bike_Wheel_Animation(getResources().getInteger(R.integer.num_leds));
//        }
//    }


    @Override
    public void setSelected(int selected) {
        imageDefineAdapter.setSelectedItem(selected);
    }

    @Override
    public void setPalette(ArrayList<Color_> newPalette) {
        imageDefineAdapter.setPalette(newPalette);
    }

    @Override
    public void removeColorFromPalette(int colorIndToRemove) {
        imageDefineAdapter.removeColorFromPalette(colorIndToRemove);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}

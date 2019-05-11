package to.us.suncloud.bikelights.common.WheelView;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.util.ArrayList;

import to.us.suncloud.bikelights.common.Color.Bike_Wheel_Animation;
import to.us.suncloud.bikelights.common.Color.Color_;

public class ImageDefinePagerAdapter extends FragmentStatePagerAdapter {
    private String tabTitles[] = new String[]{"Main", "Idle"};
    private Bike_Wheel_Animation bikeWheelAnimation; // The bikeWheelAnimation (really only hung onto to hold the images for both main and (possibly) idle images)
    //    private ArrayList<Color_> palette; // The current color palette to keep track of
//    ImageMeta_ imageMeta; // The type of image meta (constant speed, spinner) that this adapter must represent
    private ImageDefineFragment fragmentMain; // List of fragments, to keep track of changes to bikeWheelAnimation
    private ImageDefineFragment fragmentIdle; // List of fragments, to keep track of changes to bikeWheelAnimation
//    int baseID = 0;

    public ImageDefinePagerAdapter(Bike_Wheel_Animation bikeWheelAnimation, FragmentManager fm) { //, ImageDefineInterface listener) {
        super(fm);
//        this.palette = bikeWheelAnimation.getPalette();
//        this.imageMeta = bikeWheelAnimation.getImageMeta();

        this.bikeWheelAnimation = bikeWheelAnimation.clone();
//        this.listener = listener;
    }

    public void setImageMeta(int imageMetaType) {
        // Update the ImageMeta_ type, which will affect which tabs get shown
        this.bikeWheelAnimation.setImageMetaType(imageMetaType);

        // Modify the main fragment's attribute type (The idle type should not have a parameter)
        if (fragmentMain != null) {
            fragmentMain.setAttributeType(imageMetaType);
        }


//        baseID+=2;
        notifyDataSetChanged();
    }

    public void setPalette(ArrayList<Color_> newPalette) {
        // Update the palette for all tabs
        this.bikeWheelAnimation.setPalette(newPalette);

        if (fragmentMain != null) {
            fragmentMain.setPalette(newPalette);
        }

        if (fragmentIdle != null) {
            fragmentIdle.setPalette(newPalette);
        }

        notifyDataSetChanged();
    }

    public void removeColorFromPalette(int colorIndToRemove) {
        this.bikeWheelAnimation.getPalette().remove(colorIndToRemove);
        if (fragmentMain != null) {
            fragmentMain.removeColorFromPalette(colorIndToRemove);
        }

        if (fragmentIdle != null) {
            fragmentIdle.removeColorFromPalette(colorIndToRemove);
        }
    }

    public void setSelectedItem(int selectedItem) {
        // Change the selected item for each fragment, if it exists
        if (fragmentMain != null) {
            fragmentMain.setSelectedItem(selectedItem);
        }

        if (fragmentIdle != null) {
            fragmentIdle.setSelectedItem(selectedItem);
        }
    }

    public Bike_Wheel_Animation putImages(Bike_Wheel_Animation bikeWheelAnimation_toModify) {
        // Add the images to the new bikeWheelAnimation
        bikeWheelAnimation_toModify.setImageMain(fragmentMain.getImage());
        if (bikeWheelAnimation.getImageMeta().supportsIdle()) {
            // If the current bikeWheelAnimation (which represents what the GUI is currently set up to handle) allows an idle, then put it into this new Bike_Wheel_Animation
            bikeWheelAnimation_toModify.setImageIdle(fragmentIdle.getImage());
        }

        // Add the image meta data
        bikeWheelAnimation_toModify.setImageMeta(fragmentMain.getImageMeta());

        return bikeWheelAnimation_toModify;
    }

    @Override
    public Fragment getItem(int position) {
        // This function CREATES each view, but they are saved to the internal member variables in the instantiateItem function
        ImageDefineFragment newFragment;
        if (position == 0) {
            // Main image
            newFragment = ImageDefineFragment.newInstance(bikeWheelAnimation, false);
//            fragmentMain = newFragment;
        } else {
            // Idle image
            newFragment = ImageDefineFragment.newInstance(bikeWheelAnimation, true);
//            fragmentIdle = newFragment;
        }

        // Return the new fragment
        return newFragment;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        // Save the Fragments that are created each time, even if this Adapter changes identity
        Fragment createdFragment = (Fragment) super.instantiateItem(container, position);

        // save the appropriate reference depending on position
        switch (position) {
            case 0:
                fragmentMain = (ImageDefineFragment) createdFragment;
                break;
            case 1:
                fragmentIdle = (ImageDefineFragment) createdFragment;
                break;
        }
        return createdFragment;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        if (object.equals(fragmentMain)) {
            return 0;
        }

        if (object.equals(fragmentIdle)) {
            if (bikeWheelAnimation.getImageMeta().supportsIdle()) {
                return 1;
            } else {
                return POSITION_NONE;
            }
        }
        return super.getItemPosition(object);
    }

//    @Override
//    public long getItemId(int position) {
////        return baseID + position;
//        return position;
//    }

    @Override
    public int getCount() {
        if (bikeWheelAnimation.getImageMeta().supportsIdle()) {
            // Supports both main and idle
            return tabTitles.length;
        } else {
            // Supports only idle
            return tabTitles.length - 1;
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }
}

package to.us.suncloud.bikelights.common.Color;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import to.us.suncloud.bikelights.common.ByteMath;

public abstract class Color_d extends Color_ {
    //    abstract int getIncrementT(); // Amount to increment t for each new colorObjMeta
    private int nextID = 0;
    private List<colorObjMeta> c = new ArrayList<>(Collections.nCopies(1, new colorObjMeta(getNewColorObjMetaID()))); // Initialize the Color_d with one colorObj

    public enum BLEND_TYPE {
        CONSTANT,
        LINEAR
    }

    static public class colorObjMeta implements Serializable {
        private int ID; // A unique identifying number (useful for when colorObj's get their orders changed around in arrays).
        private colorObj c = new colorObj();
        private long t = 0;
        private BLEND_TYPE b = BLEND_TYPE.LINEAR;

//        colorObjMeta() {
//            ID = getNewColorObjMetaID();
//        }

        colorObjMeta(int newID) {
            ID = newID;
        }

        colorObjMeta(colorObjMeta otherC) {
            this.ID = otherC.getID();
            this.c = otherC.getColorObj();
            this.t = otherC.getT();
            this.b = otherC.getB();
        }

        void setColorObj(colorObj newC) {
            c = newC;
        }

        void setT(long newT) {
            t = newT;
        }

        void setB(BLEND_TYPE newB) {
            b = newB;
        }

        colorObj getColorObj() {
            return new colorObj(c);
        }

        long getT() {
            return t;
        }

        BLEND_TYPE getB() {
            return b;
        }

        public int getID() {
            return ID;
        }

        @Override
        public boolean equals(Object obj) {
            try {
                // Extract the needed information
                colorObjMeta other = (colorObjMeta) obj;
                colorObj otherC = other.getColorObj();
                long otherT = other.getT();
                BLEND_TYPE otherB = other.getB();

                // Check if all values are equal
                return (otherC.equals(getColorObj()) && otherT == getT() && otherB == getB());

            } catch (Error e) {
                return false;
            }
        }

        public colorObjMeta clone() {
            return new colorObjMeta(this);
        }

    }

    // New byte level manipulation functions
    @Override
    public List<Byte> toByteList() {
        List<Byte> newByteList = new ArrayList<Byte>();
        newByteList.add(ByteMath.putDataToByte((byte) 0x00, getColor_Header(), 4, 4)); // Put a 1 in the indicated position to represent a Time-based Color_

        // Go through each colorObjMeta and add it to the byte list
        newByteList.add((byte) getNumColors()); // First, add the number of colors that should be expected

        for (int colorNum = 0; colorNum < getNumColors(); colorNum++) {
            // Add the RBGW color
            newByteList.addAll(c.get(colorNum).getColorObj().toByteList());

            // Add the Blend type
            switch (c.get(colorNum).getB()) {
                case CONSTANT:
                    newByteList.add((byte) 0x00);
                    break;
                case LINEAR:
                    newByteList.add((byte) 0x01);
                    break;
            }

            // Add the T value
            newByteList.addAll(getTByteCode(colorNum));
        }

        return newByteList;
    }

    abstract byte getColor_Header(); // Return the correct header for the byte list for this Color_ (1 for Time-based Color_'s and 2 for Velocity-based Color_'s)

    abstract List<Byte> getTByteCode(int i); // Return the T value in the ith colorObjMeta as a byte list, formatted correctly according to T's type

    // Old Protocol Buffer functions
//        public BikeWheelAnim.Color_.ColorObj getColorObjBuf() {
//            BikeWheelAnim.Color_.ColorObj.Builder COBuilder = BikeWheelAnim.Color_.ColorObj
//                    // Start with the ColorObj generated by c
//                    .newBuilder(c.getColorObjBuf())
//
//                    // Add in the parameter t
//                    .setT(t);
//
//            // Convert b from BLEND_TYPE to ColorObj.BlendType (yeah, I know...)
//            switch (b) {
//                case LINEAR:
//                    COBuilder.setBt(BikeWheelAnim.Color_.ColorObj.BlendType.LINEAR);
//                    break;
//                case CONSTANT:
//                    COBuilder.setBt(BikeWheelAnim.Color_.ColorObj.BlendType.CONSTANT);
//                    break;
//            }
//
//            // Build it and return
//            return COBuilder.build();
//        }
//    }
//
//    static public colorObjMeta fromProtoBufColorObjMeta(BikeWheelAnim.Color_.ColorObj messageCO, int thisID) {
//        colorObjMeta colorObjMetaOut = new colorObjMeta(thisID);
//        colorObjMetaOut.setColorObj(colorObj.fromProtoBuf(messageCO));
//        colorObjMetaOut.setT(messageCO.getT());
//
//        switch (messageCO.getBt()) {
//            case CONSTANT:
//                colorObjMetaOut.setB(BLEND_TYPE.CONSTANT);
//                break;
//            case LINEAR:
//                colorObjMetaOut.setB(BLEND_TYPE.LINEAR);
//                break;
//        }
//
//        return colorObjMetaOut;
//    }
//
//    static public List<colorObjMeta> fromProtoBufColorObjMeta(List<BikeWheelAnim.Color_.ColorObj> messageCOArray) {
//        List<colorObjMeta> colorObjMetaArrayOut = new ArrayList<>();
//
//        for (int colorInd = 0;colorInd < messageCOArray.size();colorInd++) {
//            colorObjMetaArrayOut.add(fromProtoBufColorObjMeta(messageCOArray.get(colorInd), colorInd));
//        }
//
//        return colorObjMetaArrayOut;
//    }

    private class SortByT implements Comparator<colorObjMeta> {
        @Override
        public int compare(colorObjMeta o1, colorObjMeta o2) {
            return (int) (o1.getT() - o2.getT());
        }
    }

    // Cloning methods
    int getNextID() {
        return nextID;
    }

    void setNextID(int nextID) {
        this.nextID = nextID;
    }

    protected List<colorObjMeta> getC() {
        List<colorObjMeta> resultList = new ArrayList<>(c.size());

        // Clone the entirety of c
        for (int i = 0; i < c.size(); i++) {
            resultList.add(c.get(i).clone());
        }
        return resultList;
    }

    protected void setC(List<colorObjMeta> c) {
        this.c = c;
    }

    public int getNewColorObjMetaID() {
        return nextID++;
    }

//    private List<colorObj> c = new ArrayList<>();
//    private List<Integer> t = new ArrayList<>();
//    private List<BLEND_TYPE> b = new ArrayList<>();

    Color_d() {
    }

    @Override
    public AnimatorSet modColor_Animator(final Object obj, final AnimatorSet oldAnimSet, String param) {
        // Prepare the AnimatorSet to receive the new animation
        oldAnimSet.removeAllListeners();
        oldAnimSet.cancel();
        final AnimatorSet animSet = new AnimatorSet();


        // Go through each colorObjMeta and add a segment to the animator set
        final List<Animator> animList = new ArrayList<>();

        if (getNumColors() > 1) {
            for (int colorInd = 0; colorInd < (getNumColors() - 1); colorInd++) {
                // Create a new object animation, on the supplied view, interpolating the background color between the start and end colors
                int colorStart = c.get(colorInd).getColorObj().getColorInt();
                int colorEnd = c.get((colorInd + 1)).getColorObj().getColorInt(); // % c.get((colorInd + 1)getNumColors()).getColorObj().getColorHex();
                ObjectAnimator thisAnim = ObjectAnimator.ofArgb(obj, param, colorStart, colorEnd);

                // Set the duration of the color animation
                int thisDur = Math.max(getTAnimationScale() * getTDifference(colorInd), 1);
                thisAnim.setDuration(thisDur); // Make the animation take at least 1ms

                // Set the type of transition of this animation
                thisAnim.setInterpolator(getBlendInterpolator(colorInd));

                // Add the newly created object animator to the list of animations
                animList.add(thisAnim);
            }

            // Add this list to the animation set, to be played sequentially
            animSet.playSequentially(animList);

            // Make sure that this animation set repeats infinitely
            animSet.addListener(new AnimatorListenerAdapter() {
                private boolean isCanceling = false;

                @Override
                public void onAnimationStart(Animator animation) {
                    isCanceling = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    isCanceling = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
//                    if (animation.equals(animList.get(animList.size() - 1))) {
                    // If this is the last animation, then go back to the beginning
                    if (!isCanceling) {
                        animSet.start();
                    } else {
//                        Toast.makeText(((View)obj).getContext(), "Animation ended.", Toast.LENGTH_SHORT).show();
//                        super.onAnimationEnd(animation);
                    }
//                    }
//                    Toast.makeText(((View) obj).getContext(), "Restarted Animation...", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // If there is only one color, treat this like a Color_Static
            int thisColor = c.get(0).getColorObj().getColorInt();
            ObjectAnimator thisAnim = ObjectAnimator.ofArgb(obj, param, thisColor, thisColor);
            thisAnim.setDuration(1);
            animList.add(thisAnim);

            // Add the animator Object
            animSet.playSequentially(animList);
        }

        // Start the animation
        animSet.start();

        // Return the AnimatorSet
        return animSet;
    }

    private TimeInterpolator getBlendInterpolator(int index) {
        switch (c.get(index).getB()) {
            case LINEAR:
                // Return a linear interpolator
                return new LinearInterpolator();
            case CONSTANT:
                // Return a "constant" interpolator (always returns 0)
                return new Interpolator() {
                    @Override
                    public float getInterpolation(float input) {
                        return 0;
                    }
                };
            default:
                // Uh oh...

                // Return a linear interpolator
                return new LinearInterpolator();
        }
    }

    @Override
    public int getNumColors() {
        return c.size();
    }

    int getTDifference(int ind) {
        // Get the difference in T values between the c at ind and the c at (ind + 1).
        if (ind > (getNumColors() - 2)) {
            // Keep the index within bounds ("-2" because: -1 from indexing starting at 0, another -1 from not being able to call the last index (because there is nothing after the last index)
            ind = getNumColors() - 2;
        }

        return (int) (c.get(ind + 1).getT() - c.get(ind).getT());
    }

    abstract int getTStandardDistance(); // The multiplication factor for the animation (i.e. if equal to 100, then animation will run at 100 T value per second
    public int getTAnimationScale() {
        return Math.round(1000.0f / ((float) getTStandardDistance())); // Get the time scale of the animation for displaying this color
    }

    @Override
    public boolean setColorObj(colorObj newColorObj, int index) {
        colorObjMeta thisC = c.get(index);
        thisC.setColorObj(newColorObj);
        c.set(index, thisC); // Change this colorObj

        // Modifying t may change the implicit order in c.  Check it now.
        return sortAndVerifyColorList();
    }

    public boolean setT(Long newT, int index) {
        colorObjMeta thisC = new colorObjMeta(c.get(index));
        thisC.setT(newT);
        c.set(index, thisC); // Change this colorObj

        // Modifying t may change the implicit order in c.  Check it now.
        return sortAndVerifyColorList();
    }

    public void setB(BLEND_TYPE newB, int index) {
        colorObjMeta thisC = new colorObjMeta(c.get(index));
        thisC.setB(newB);
        c.set(index, thisC); // Change this colorObj
    }

    private boolean sortAndVerifyColorList() {
        // This function will have two responsibilities: First, it will sort, then it will ensure that at t == 0 exists.  If no t == 0 object exists, it will add one.
        // The return state indicates whether or not the order of the list had to change

        boolean listChanged = !isCSorted();

        // Sort c (according to T)
        Collections.sort(c, new SortByT());

        // Ensure that there exists a t == 0 object
        if (c.get(0).getT() != 0) {
            // If the first object is not t == 0, then add a new t == 0 object to the beginning of c (doesn't need error-checking)
            c.add(0, new colorObjMeta(getNewColorObjMetaID()));
        }

        return listChanged;
    }

    private boolean isCSorted() {
        long last = 0; // No T should be less than 0
        for (int i = 0; i < c.size(); i++) {
            if (last > c.get(i).getT()) {
                return false; // If the last value is greater than or equal to the current value, then the list is not sorted
            }
            last = c.get(i).getT();
        }
        return true;
    }

    public Long getT(int index) {
        return c.get(index).getT();
    }

    public BLEND_TYPE getB(int index) {
        return c.get(index).getB();
    }

    public colorObjMeta getColorObjMeta(int index) {
        return new colorObjMeta(c.get(index));
    }

    public void addColorObjImpliedT() {
        // First, create a new colorObjMeta object
        addColorObjImpliedT(new colorObjMeta(getNewColorObjMetaID()));
    }

    public void addColorObjImpliedT(colorObjMeta oldC) {
        // First, find the T value higher than the highest current T value (so it goes to the end of the list
        long newT;
        long tDiff; // The difference between the old highest T and the new one
        if (getNumColors() == 1) {
            tDiff = getTStandardDistance();
        } else {
            tDiff = getTDifference(getNumColors() - 2);
        }
        newT = (c.get(getNumColors() - 1).getT() + tDiff);

        // Add the incoming colorObjMeta (oldC), with a new timecode
        addColorObjMeta(oldC.getColorObj(), oldC.getB(), newT);
    }

    public void addColorObjMeta(colorObjMeta oldC) {
        addColorObjMeta(oldC.getColorObj(), oldC.getB(), oldC.getT());
    }

    public void addColorObjMeta(colorObj newC, BLEND_TYPE newB, long newT) {
        // Method for adding a new (i.e. with a unique ID) colorObjMeta of arbitrary value
        colorObjMeta newColorObjMeta = new colorObjMeta(getNewColorObjMetaID()); // Create a new colorObjMeta, so that a new ID is generated

        // Save the color, blend type, and T
        newColorObjMeta.setColorObj(newC);
        newColorObjMeta.setB(newB);
        newColorObjMeta.setT(newT);

        // Add this new colorObjMeta object
        c.add(newColorObjMeta);

        // Sort the list (technically kind of inefficient for adding a large number of colorObjMetas in a row, but...this is me caring.  Could make a new method that adds colorObjs, BLEND_TYPES, and longs that arrive as a list, adding them all, and THEN sorting them...but whatever
        sortAndVerifyColorList();
    }

    public void addColorObjMetas(List<colorObjMeta> newCs) {
        // Add a number of new colorObjMetas by copying the values from the incoming List
        List<colorObjMeta> newCsCopy = new ArrayList<>(newCs.size());

        // Go through each value in the incoming newC List and make a new colorObjMeta to copy from it (done to assign an ID value to each incoming colorObjMeta, which really should only be done at creation)
        for (int colorObjInd = 0;colorObjInd < newCs.size(); colorObjInd++) {
            colorObjMeta thisC = new colorObjMeta(getNewColorObjMetaID()); // Ensure that each has a unique ID
            thisC.setColorObj(newCs.get(colorObjInd).getColorObj());
            thisC.setB(newCs.get(colorObjInd).getB());
            thisC.setT(newCs.get(colorObjInd).getT());

            // Add this new colorObjMeta
            newCsCopy.add(thisC);
        }

        // Store the new list
        setC(newCsCopy);

        // Finally, verify the new list
        sortAndVerifyColorList();
    }

    public void setColorObjMeta(colorObj newC, BLEND_TYPE newB, long newT, int index) {
        // Main method for adding a new (i.e. with a unique ID) colorObjMeta of arbitrary value
        colorObjMeta newColorObjMeta = new colorObjMeta(getNewColorObjMetaID()); // Create a new colorObjMeta, so that a new ID is generated

        // Save the color, blend type, and T
        newColorObjMeta.setColorObj(newC);
        newColorObjMeta.setB(newB);
        newColorObjMeta.setT(newT);

        // Add this new colorObjMeta object
        c.set(index, newColorObjMeta);

        // Sort the list (technically kind of inefficient for adding a large number of colorObjMetas in a row, but...this is me caring.  Could make a new method that adds colorObjs, BLEND_TYPES, and longs that arrive as a list, adding them all, and THEN sorting them...but whatever
        sortAndVerifyColorList();
    }

    @Override
    public colorObj getColorObj(int index) {
        return c.get(index).getColorObj();
    }

    public boolean removeIndex(int index) {
        if (getNumColors() != 0) {
            c.remove(index);

            // Check if the first index was just deleted. If not, ignore; if so, set the next object to t == 0 (ensures that the first object is always t == 0, but eliminates possible loop of continually trying to delete the first object that would occur by simply adding a new object each time)
            c.get(0).setT(0);

            return true; // Success
        } else {
            return false; // Failed
        }
    }

    public boolean moveIndexUp(int index) {
        // Move this color up in the GUI, which means moving it down in the array (the array is shown "up-side down" in the GUI)
        if (index != 0) {
            // If this is not already the first color (should also be GUI checks that ensure this)
            return swapColorObjMetas(index, index - 1);
        } else {
            return true;
        }
    }

    public boolean moveIndexDown(int index) {
        // Move this color down in the GUI, which means moving it up in the array (the array is shown "up-side down" in the GUI)
        if (index != (getNumColors() - 1)) {
            // If this is not already the last color (should also be GUI checks that ensure this)
            return swapColorObjMetas(index, index + 1);
        } else {
            return true;
        }
    }

    private boolean swapColorObjMetas(int ind1, int ind2) {
        // Exchange the T values between the colorObjMeta's at indices ind1 and ind2
        long tmpT = c.get(ind1).getT(); // Store ind1's T
        c.get(ind1).setT(c.get(ind2).getT()); // Set ind1's T to ind2's
        c.get(ind2).setT(tmpT); // Set ind2's T to ind1's

        // Swap the colorObjMeta's in the list
        Collections.swap(c, ind1, ind2);

        // Confirm that we're still in order
        return sortAndVerifyColorList();
    }

//    public ArrayList<BikeWheelAnim.Color_.ColorObj> getAllColorObjBufs() {
//        ArrayList<BikeWheelAnim.Color_.ColorObj> cBuf = new ArrayList<>();
//
//        for (int colorObjInd = 0;colorObjInd < getNumColors();colorObjInd++) {
//            cBuf.add(c.get(colorObjInd).getColorObjBuf());
//        }
//
//        return cBuf;
//    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (o instanceof Color_d) {
            isEqual = getC().equals(((Color_d) o).getC()); // If the c Lists are equivalent, then the entire Color_d can be assumed to be equivalent
        }

        return isEqual;
    }
}

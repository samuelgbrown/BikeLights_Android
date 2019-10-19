package to.us.suncloud.bikelights.common.WheelView;

import android.animation.AnimatorSet;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Color.Color_;
import to.us.suncloud.bikelights.common.Color.Bike_Wheel_Animation;
import to.us.suncloud.bikelights.common.Color.Color_Static;

public class LEDViewDrawable extends Drawable implements Serializable {
    private String TAG = "LEDViewDrawable";

    private static final int frame_period = 100; // The view update frequency in ms
    private Timer timer = new Timer();
    private boolean doRedraws = true; // Should the animation be running?
//    private Bike_Wheel_Animation lastColorList; // The color list that is currently being animated in the drawable (used to see when changes to the animation are necessary)

    private List<Paint> colorPaints; // The paints currently being used to color the canvas in the drawable
    private List<AnimatorSet> paintAnimatorSets; // The AnimatorSets that will be used to animate each paint color
    private Paint highlightPaint; // The paint used to outline the currently selected Color_
    private Paint backgroundPaint; // The paint used as the background of the View (pretty much an eraser)

    // Parameters for the drawing
    private float ledDutyCycle = .7f; // [0 - 1] Ratio of the "colored" parts of the wheel to the "empty" parts
    private float highlightDutyCycle = .95f; // Additional duty cycle to add a highlight layer to each LED
    private float ledHalfDuty = ledDutyCycle / 2;
    private float highlightHalfDuty = highlightDutyCycle / 2;

    private int rotationOffset = 0; // The rotational offset of the image on the LED wheel

    private static final int numLEDs = 120;
    private ArrayList<Boolean> selection = new ArrayList<>(Collections.nCopies(numLEDs, false)); // The current index in image that is selected (-1 represents no selection)
    //    private Bike_Wheel_Animation bikeWheelAnimation; // The color list that the drawable will use to draw the wheel
    private ArrayList<Color_> palette; // The palette of Color_'s that are possible to use
    private ArrayList<Integer> image;  // The image that will be drawn
    private View wheelView; // View that this drawable will be used on
    private Bitmap bitmap = null; // Bitmap that will be drawn to the canvas
    private Canvas bitmapCanvas = null; // Canvas created using bitmap

    public LEDViewDrawable(final View wheelView, int numLEDs) {
        this.wheelView = wheelView;

        // Create a timer that will invalidate the view every frame_period seconds
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Cause the wheelView to redraw
                if (doRedraws) {
                    wheelView.postInvalidate();
                }
            }
        }, 0, frame_period);

        // Set up constant paints
        highlightPaint = new Paint();
        highlightPaint.setColor(ContextCompat.getColor(wheelView.getContext(), R.color.wheelCross));
        highlightPaint.setAntiAlias(true);

        backgroundPaint = new Paint();
        backgroundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        backgroundPaint.setAntiAlias(true);

//        bikeWheelAnimation = new Bike_Wheel_Animation(numLEDs);
        // Initialize the image and palette
        palette = new ArrayList<Color_>(Collections.nCopies(1, new Color_Static()));
        image = new ArrayList<>(Collections.nCopies(numLEDs, 0));
        precalcPaints();
    }

    public void setAnimationRunning(boolean doRedraws) {
        this.doRedraws = doRedraws;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
//     If using: set current "draw" method to "drawOnBitmap" ,and set to private
        // Bitmap method borrowed from Ali Muzaffar of Medium.com (https://medium.com/@ali.muzaffar/android-why-your-canvas-shapes-arent-smooth-aa2a3f450eb5)
        int width = getBounds().width();
        int height = getBounds().height();
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width,
                    height,
                    Bitmap.Config.ARGB_8888);
            bitmapCanvas = new Canvas(bitmap);
        }
        bitmapCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // Clear the bitmap
        drawOnBitmap(bitmapCanvas);
        Paint p = new Paint();
        canvas.drawBitmap(bitmap, 0, 0, p);
    }

    private void drawOnBitmap(Canvas canvas) {
        // Get the Bike_Wheel_Animation information (colors and image)

//        List<Integer> image = getImage();  //getBikeWheelAnimation().getImageMain();

        // Extract and set parameters
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        float diamRatio = .95f; // The ratio between the smallest canvas dimension and the outer edge of the wheel; defines the outer diameter
        int ledWidth = 20; // Width in dp of the LED's to be displayed
        int highlightWidth = 2; // Width in dp of the highlight around selected LED's

        int boundingBoxDiameter = Math.min(width, height); // Get the diameter of the bounding box
        int LEDWheelRad = Math.round(((float) boundingBoxDiameter * diamRatio) / 2); // Get the diameter of the wheel
        int LEDWhiteWheelRad = LEDWheelRad - ledWidth;
        int highlightWhiteWheelRad = LEDWheelRad - ledWidth - highlightWidth;
        int highlightWheelRad = LEDWheelRad + highlightWidth;


        // Set up the rectangle/oval that we will be drawing over
        int xCenter = width / 2;
        int yCenter = height / 2;
        RectF ledOval = new RectF(xCenter - LEDWheelRad, yCenter - LEDWheelRad, xCenter + LEDWheelRad, yCenter + LEDWheelRad);
        RectF highlightOval = new RectF(xCenter - highlightWheelRad, yCenter - highlightWheelRad, xCenter + highlightWheelRad, yCenter + highlightWheelRad);
        RectF ledWhiteOval = new RectF(xCenter - LEDWhiteWheelRad, yCenter - LEDWhiteWheelRad, xCenter + LEDWhiteWheelRad, yCenter + LEDWhiteWheelRad);
        RectF highlightWhiteOval = new RectF(xCenter - highlightWhiteWheelRad, yCenter - highlightWhiteWheelRad, xCenter + highlightWhiteWheelRad, yCenter + highlightWhiteWheelRad);

//        precalcPaints();

//        Paint testPaint = new Paint();
//        testPaint.setColorObj(ContextCompat.getColorObj(wheelView.getContext(), R.color.colorAccent));
//        testPaint.setAntiAlias(true);

        float sweepAngle = (1 / (float) numLEDs) * 360f; // sweep angle going clockwise
        for (int i = 0; i < numLEDs; i++) {
            int thisLEDPos = i + rotationOffset;  // Get the LED number that should be drawn at this position, modified by the rotationOffset (which is animated via ObjectAnimator to show the image rotation speed)

            float startAngle = ((float) thisLEDPos / (float) numLEDs) * 360f; // thisLEDPos == 0 (start angle == 0˚) is located on the "right" side of the wheel ("front" side)
            float midPoint = startAngle + sweepAngle / 2;

            // Define the specific start/sweep angles that will be used
            float ledStart = midPoint - ledHalfDuty * sweepAngle;
            float ledSweep = sweepAngle * ledDutyCycle;
            float highlightStart = midPoint - highlightHalfDuty * sweepAngle;
            float highlightSweep = sweepAngle * highlightDutyCycle;

            // Determine which color is going to be drawn on this LED slot, and get its Paint
            int thisColorNum = image.get(i);
            Paint thisColorPaint = colorPaints.get(thisColorNum);

            // Draw highlight color
            boolean thisDoHighlight = selection.get(i); // Determine if this LED should be highlighted
            if (thisDoHighlight) {
                canvas.drawArc(highlightOval, highlightStart, highlightSweep, true, highlightPaint);
            }

            // Draw LED color
            canvas.drawArc(ledOval, ledStart, ledSweep, true, thisColorPaint);

            // Draw white space (to make a "ring" instead of a "wheel")
            if (thisDoHighlight) {
                // If highlighting, draw a second (inner) highlight boundary...
                canvas.drawArc(ledWhiteOval, highlightStart, highlightSweep, true, highlightPaint);
            } else {
                // If not highlighting, use a larger white-space wheel
                canvas.drawArc(ledWhiteOval, startAngle, sweepAngle, true, backgroundPaint);
            }
        }

        // Clear the center with a small white-space wheel
        canvas.drawArc(highlightWhiteOval, 0, 360f, true, backgroundPaint);
    }

    private void precalcPaints() {
        // Set up paints, if needed
//        if (lastColorList == null || !lastColorList.equals(palette)) {
        // If the last Bike_Wheel_Animation to be drawn is different than the Bike_Wheel_Animation that is going to be drawn now, then recreate all of the paints and associated animations
//        List<Color_> palette = getBikeWheelAnimation().getPalette();

        colorPaints = new ArrayList<>(getNumColors());
        paintAnimatorSets = new ArrayList<>(Collections.nCopies(getNumColors(), new AnimatorSet()));
        for (int colorInd = 0; colorInd < getNumColors(); colorInd++) {
            // Create a new Paint object
            Paint newPaint = new Paint();
            newPaint.setAntiAlias(true);

            // Set up an animator for the Paint, by giving it to its corresponding Color_ in palette
            AnimatorSet newSet = palette.get(colorInd).modColor_Animator(newPaint, paintAnimatorSets.get(colorInd), "color");
            paintAnimatorSets.set(colorInd, newSet);

            // TO_DO: If this doesn't work, first test that the timer is calling invalidate() properly; Try setting up a test button that invalidates the wheelView (or calls invalidateSelf()) to see if anything changes

            // Set this new Paint object
            colorPaints.add(newPaint);
        }

//        lastColorList = bikeWheelAnimation;
//        }
    }

    public void setSelection(int newSelection) {
        // Get the new newSelection, as an index in the ColorList
        if (newSelection < getNumColors()) {
            for (int ind = 0; ind < selection.size(); ind++) {
                selection.set(ind, newSelection == getImage().get(ind)); //getBikeWheelAnimation().getIMain(ind)); // Set the selection array to true if the new selection index matches the one in the Image, and false otherwise
            }
        } else {
            selection = new ArrayList<>(Collections.nCopies(numLEDs, false));
            Log.e(TAG, "Received newSelection out of bounds of image.");
        }

        // Redraw
//        invalidateSelf(); //
    }

    public void setSelection(ArrayList<Boolean> newSelection) {
        if (newSelection.size() == numLEDs) {
            selection = newSelection; // Set the new selection
        }
    }

//    public void setBikeWheelAnimation(Bike_Wheel_Animation bikeWheelAnimation) {
//        this.bikeWheelAnimation = bikeWheelAnimation; // TO_DO: Make sure that this interacts with slice-width and slice-repeat properly (is only the OLD image where there isn't a selection/repeat of the selection)
//
//        // Recalculate the paints with this new Bike_Wheel_Animation
//        precalcPaints();
//    }


    public void setPalette(ArrayList<Color_> palette) {
        this.palette = new ArrayList<>(palette);

        // Recalculate the paints with this new Bike_Wheel_Animation
        precalcPaints();
    }

    public void setImage(ArrayList<Integer> image) {
        // Update the image that will be displayed
        this.image = image;
    }

    private int getNumColors() {
        return palette.size();
    }

//    private Bike_Wheel_Animation getBikeWheelAnimation() {
//        return bikeWheelAnimation;
//    }

    private ArrayList<Integer> getImage() {
        return image;
    }

    public ArrayList<Color_> getPalette() {
        return palette;
    }

    // Setter for rotationOffset so that ObjectAnimator can affect it
    public void setRotationOffset(int rotationOffset) {
        this.rotationOffset = rotationOffset;
    }

    @Override
    public void setAlpha(int alpha) {
        // Do nothing
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        // Do nothing
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

}

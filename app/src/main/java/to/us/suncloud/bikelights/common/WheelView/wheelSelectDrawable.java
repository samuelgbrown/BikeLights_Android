package to.us.suncloud.bikelights.common.WheelView;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Constants;

import static to.us.suncloud.bikelights.common.WheelView.wheelSelectDrawable.wheelPowerState.*;
import static to.us.suncloud.bikelights.common.WheelView.wheelSelectDrawable.wheelConnectedState.*;

public class wheelSelectDrawable extends Drawable {
     enum wheelPowerState {
         wheelOff,
         wheelOn
     }
     enum wheelConnectedState {
         wheelNotConnected,
         wheelConnected
     }

    private wheelPowerState rearPState = wheelOff;
    private wheelPowerState frontPState = wheelOff;
    private wheelConnectedState rearCState = wheelNotConnected;
    private wheelConnectedState frontCState = wheelNotConnected;

    private boolean testingPos = false;
    private View mParentView;

    public wheelSelectDrawable(View parentView) {
        // Construct this drawable class
        mParentView = parentView;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        // Extract and set parameters
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int xCenter = width/2;
        int yCenter = height/2;
        int xDiff = Math.round((float) width/3.23f); //445;
        int xShift = 0; //-5;
        int yDiff = width/10; //150;
        int wheelRadius = Math.round((float) width/5.2f); //275;
        int wheelWidth = 30;
        int crossWidth = 2;

        // Calculate the locations of the centers of each wheel
        // TODO Later: Remake the location of the center of the wheels as a function of the width, so it will work in portrait or landscape mode
        int rearX = xCenter - xDiff + xShift;
        int frontX = xCenter + xDiff + xShift;

        int wheelY = yCenter + yDiff;

        // Setup paints
        Paint rearPaint = new Paint();
        Paint frontPaint = new Paint();
        Paint crossPaint;

        rearPaint.setStrokeWidth(wheelWidth);
        rearPaint.setStyle(Paint.Style.STROKE);
        frontPaint.setStrokeWidth(wheelWidth);
        frontPaint.setStyle(Paint.Style.STROKE);

        switch (rearCState) {
            case wheelConnected:
                switch (rearPState) {
                    case wheelOff:
                        rearPaint.setColor(c(R.color.wheelConnected));
                        break;
                    case wheelOn:
                        rearPaint.setColor(c(R.color.wheelOn));
                        break;
                }
                break;
            case wheelNotConnected:
                rearPaint.setColor(c(R.color.wheelOff));
                break;
        }

        switch (frontCState) {
            case wheelConnected:
                switch (frontPState) {
                    case wheelOff:
                        frontPaint.setColor(c(R.color.wheelConnected));
                        break;
                    case wheelOn:
                        frontPaint.setColor(c(R.color.wheelOn));
                        break;
                }
                break;
            case wheelNotConnected:
                frontPaint.setColor(c(R.color.wheelOff));
                break;
        }

        // Draw the wheels
        canvas.drawCircle(rearX, wheelY, wheelRadius, rearPaint);
        canvas.drawCircle(frontX, wheelY, wheelRadius, frontPaint);

        // Set up cross drawing (used to check the position of the wheel centers)
        if (testingPos){
            // Set up the crosses paint
            crossPaint = new Paint();
            crossPaint.setColor(c(R.color.wheelCross));
            crossPaint.setStrokeWidth(crossWidth);

            // Draw the crosses
            canvas.drawLine(0, wheelY, width, wheelY, crossPaint);
            canvas.drawLine(rearX, 0, rearX, height, crossPaint);
            canvas.drawLine(frontX, 0, frontX, height, crossPaint);
        }
    }

    private int c(int colorResource) {
        // Shortcut for the ContextCompat.getColorObj() method
        return ContextCompat.getColor(mParentView.getContext(), colorResource);
    }

    public void setPowerState(boolean isWheelOn, int wheelLoc) {
        switch (wheelLoc) {
            case Constants.ID_FRONT:
                if (isWheelOn) {
                    frontPState = wheelOn;
                } else {
                    frontPState = wheelOff;
                }
                break;
            case Constants.ID_REAR:
                if (isWheelOn) {
                    rearPState = wheelOn;
                } else {
                    rearPState = wheelOff;
                }
                break;
        }
    }

    public void setRearConnectedState(boolean isWheelConnected) {
        if (isWheelConnected) {
            rearCState = wheelConnected;
        } else {
            rearCState = wheelNotConnected;
        }
    }

    public void setFrontConnectedState(boolean isWheelConnected) {
        if (isWheelConnected) {
            frontCState = wheelConnected;
        } else {
            frontCState = wheelNotConnected;
        }
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

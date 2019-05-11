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
        int xDiff = 445;
        int xShift = -5;
        int yDiff = 150;
        int wheelRadius = 275;
        int wheelWidth = 30;
        int crossWidth = 2;

        // Calculate the locations of the centers of each wheel
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

        switch (rearPState) {
            case wheelOn:
                switch (rearCState) {
                    case wheelConnected:
                        rearPaint.setColor(c(R.color.wheelConnected));
                    case wheelNotConnected:
                        rearPaint.setColor(c(R.color.wheelOn));
                }
            case wheelOff:
                rearPaint.setColor(c(R.color.wheelOff));
        }

        switch (frontPState) {
            case wheelOn:
                switch (frontCState) {
                    case wheelConnected:
                        frontPaint.setColor(c(R.color.wheelConnected));
                    case wheelNotConnected:
                        frontPaint.setColor(c(R.color.wheelOn));
                }
            case wheelOff:
                frontPaint.setColor(c(R.color.wheelOff));
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

    public void setRearPowerState(boolean isWheelOn) {
        if (isWheelOn) {
            rearPState = wheelOn;
        } else {
            rearPState = wheelOff;
        }
    }

    public void setFrontPowerState(boolean isWheelOn) {
        if (isWheelOn) {
            frontPState = wheelOn;
        } else {
            frontPState = wheelOff;
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

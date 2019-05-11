package to.us.suncloud.bikelights.common.colorpickerview;

import java.io.Serializable;

public class SavedColor implements Serializable {
    private int x;
    private int y;
    private int color;
    private int brightness;


    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    SavedColor(int x, int y, int color, int brightness) {
        setX(x);
        setY(y);
        setColor(color);
        setBrightness(brightness);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    @Override
    public boolean equals(Object obj) {
        SavedColor otherSavedColor = (SavedColor) obj;
        return ((otherSavedColor.getBrightness() == getBrightness()) && (otherSavedColor.getColor() == getColor()) && (otherSavedColor.getX() == getX()) && (otherSavedColor.getY() == getY()));
    }
}

package to.us.suncloud.bikelights.common.Color;

import android.text.InputFilter;
import android.text.Spanned;

public class ColorPortionInputFilter implements InputFilter {
    // The minimum and maximum values that will be allowed
    private static final int MIN = 0;
    private static final int MAX = 255;

    public ColorPortionInputFilter() {}

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            int input = Integer.parseInt(dest.toString() + source.toString()); // No idea why this, just got it from (https://acomputerengineer.wordpress.com/2015/12/16/limit-number-range-in-edittext-in-android-using-inputfilter/)
            if (inRange(input)) {
                return null;
            } else {
                return "";
            }
        } catch (NumberFormatException nfe) {
            return "";
        }
    }

    private boolean inRange(int input) {
        return MIN <= input && input <= MAX;
    }
}

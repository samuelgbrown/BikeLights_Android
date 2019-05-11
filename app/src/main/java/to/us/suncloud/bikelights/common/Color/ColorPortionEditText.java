package to.us.suncloud.bikelights.common.Color;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputFilter;
import android.util.AttributeSet;

public class ColorPortionEditText extends AppCompatEditText {
    public ColorPortionEditText(Context context) {
        super(context);
        setFilters();
    }

    public ColorPortionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFilters();
    }

    public ColorPortionEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFilters();
    }

    private void setFilters() {
        this.setFilters(new InputFilter[]{new ColorPortionInputFilter()});
    }
}

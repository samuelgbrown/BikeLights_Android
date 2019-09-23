package to.us.suncloud.bikelights.common.WheelView;

import android.content.Context;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatSpinner;

public class SpinnerSelectListener extends AppCompatSpinner {
    OnItemSelectedListener listener;

    public SpinnerSelectListener(Context context) {
        super(context);
    }

    public SpinnerSelectListener(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);
        if (listener != null)
            listener.onItemSelected(null, null, position, 0);
    }

    public void setListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }
}

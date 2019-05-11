package to.us.suncloud.bikelights.common.colorpickerview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.colorpickerview.flag.FlagView;

public class BikeLightsFlagView extends FlagView {
    View background;

    public BikeLightsFlagView(Context context, int layout) {
        super(context, layout);
        background = findViewById(R.id.patternView);
//        background.setBackground(new BitmapDrawable(getResources(), ));
    }

    @Override
    public void onRefresh(ColorEnvelope colorEnvelope) {
        // Set the tint of the flag
//        background.setBackgroundTintList(ColorStateList.valueOf(colorEnvelope.getColor()));
        Drawable d = background.getBackground();
        d.setColorFilter(colorEnvelope.getColor(), PorterDuff.Mode.SRC_ATOP);
    }
}

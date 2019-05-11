package to.us.suncloud.bikelights.common.Color;

import android.content.Context;
import android.widget.ImageView;

import to.us.suncloud.bikelights.common.colorpickerview.ColorEnvelope;
import to.us.suncloud.bikelights.common.colorpickerview.flag.FlagView;

public class ColorFlag extends FlagView {
    ImageView flagView;
    public ColorFlag(Context context, int layoutID) {
        super(context, layoutID);
        flagView = findViewById(layoutID);
    }

    @Override
    public void onRefresh(ColorEnvelope colorEnvelope) {
        flagView.setColorFilter(colorEnvelope.getColor());
    }
}

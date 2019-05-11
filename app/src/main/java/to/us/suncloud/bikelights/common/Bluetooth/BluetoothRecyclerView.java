package to.us.suncloud.bikelights.common.Bluetooth;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import to.us.suncloud.bikelights.common.ObservedRecyclerView;

public class BluetoothRecyclerView extends ObservedRecyclerView {
    BluetoothRecyclerView(Context context) {
        super(context);
    }

    public BluetoothRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    //    public void setEmptyString(String emptyString) {
//        // Update the text of the mEmptyView
//        ((TextView)mEmptyView).setText(emptyString);
//    }
}

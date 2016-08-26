package nodomain.freeyourgadget.gadgetbridge.externalevents;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;

public class mybroadcastreceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.v("alarm1","updated steps ");
        Toast.makeText(context, "I'm running"  , Toast.LENGTH_SHORT).show();
    }

}

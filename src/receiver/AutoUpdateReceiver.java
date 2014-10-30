package receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoUpdateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent arg1) {

		Intent intent = new Intent(context,AutoUpdateReceiver.class);
		context.startService(intent);
	}

}

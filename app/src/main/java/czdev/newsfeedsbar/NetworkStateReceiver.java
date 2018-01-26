package czdev.newsfeedsbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;


import static czdev.newsfeedsbar.NewsFeedsBar.getServiceNewsStatus;

public class NetworkStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.d("app", "Network connectivity change");
        if (intent.getExtras() != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
            if (ni != null && ni.getState() == NetworkInfo.State.CONNECTED) {
                if(NewsFeedsBar.mContext != null) {
                    NewsFeedsBar.refreshListNews(true);
                    if(getServiceNewsStatus()) {
                        NewsFeedsBar.restartServiceNews(true);
                    }
                }

                Log.i("app", "Network " + ni.getTypeName() + " connected");
            } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                Log.d("app", "There's no network connectivity");
            }
        }
    }
}

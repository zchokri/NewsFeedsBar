package czdev.newsfeedsbar;

/**
 * Created by ZAGROUBA CHOKRI on 09/01/2018.
 */

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.google.gson.Gson;

import static czdev.newsfeedsbar.Constants.*;


public class SplashScreen extends AppCompatActivity {

    public static RetrieveFeedTask retrieveFeedTask;
    public static SharedPreferences mPrefs;
    public static Activity splashActivity;
    public static ProgressDialog mProgressDialog;
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkDrawOverlayPermission() {
        Log.v("App", "Package Name: " + getApplicationContext().getPackageName());
        /** check if we already  have permission to draw over other apps**/
        if (!Settings.canDrawOverlays(this)) {
            Log.v("App", "Requesting Permission" + Settings.canDrawOverlays(this));
            /** if not construct intent to request permission**/
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getApplicationContext().getPackageName()));
            /* request permission via start activity for result */
            startActivityForResult(intent, REQUEST_CODE);
        } else {
            Log.v("App", "We already have permission for it.");
            startSplashScreen();
        }
    }
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        Log.v("App", "OnActivity Result.");
        //check if received result code
        //  is equal our requested code for draw permission
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startSplashScreen();
                }
            }
        }
    }

        public void startSplashScreen()
        {
            /**
             * Showing splashscreen while making network calls to download necessary
             * data before launching the app Will use AsyncTask to make http call
             */
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Please wait...");
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();

            Log.d(TAG_LOG,"startSplashScreen");
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(getSavedFeeds() == null && isNetworkConnected()) {
                        retrieveFeedTask = new RetrieveFeedTask(getBaseContext(), true);
                        retrieveFeedTask.readUrls();
                        if(retrieveFeedTask.getFeed() != null) {
                            saveCurrentFeeds(retrieveFeedTask.getFeed());
                        }
                    }else
                    {
                        Intent i = new Intent(getBaseContext(), NewsFeedsBar.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        mProgressDialog.dismiss();
                        finish();
                    }
                }
            }, 200);
        }


    private  boolean isNetworkConnected() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE); // 1
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo(); // 2
        return networkInfo != null && networkInfo.isConnected(); // 3
    }
    public void saveCurrentFeeds(Feed tmpFeed) {
        mPrefs = getSharedPreferences(FEED_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(tmpFeed);
        prefsEditor.putString("sSavedFeed", json);
        prefsEditor.commit();
        Log.v(TAG_LOG,"saveFeed" + tmpFeed.toString());

    }
    public Feed getSavedFeeds() {
        Feed tmpFeed = null;
        mPrefs = getSharedPreferences(FEED_PREFS_NAME, MODE_PRIVATE);
        tmpFeed = new Gson().fromJson(mPrefs.getString("sSavedFeed", null), Feed.class);
        Log.v(TAG_LOG,"getSaved" + tmpFeed);
        return tmpFeed;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            splashActivity = this;
            setContentView(R.layout.activity_splash);
            startSplashScreen();
        }
    }
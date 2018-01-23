package czdev.newsfeedsbar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.sun.org.apache.regexp.internal.RE;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import static czdev.newsfeedsbar.Constants.*;


public class NewsFeedsBar extends AppCompatActivity {

    NotificationCompat.Builder mBuilder;
    public static FloatingActionButton fab;
    Animation animSideDown;
    public int  mRefreshDelay = 0;
    public int  mLanguageId= 0;
    public Set<String> mRessources = null;
    public static Feed mFeed;
    public static SharedPreferences mPrefs;
    public static ListView listView = null;
    public static  Context mContext = null;
    public static SharedPreferences sharedPreferences = null;
    SharedPreferences defaultSharedPreferences = null;
    public static Activity newsBarActivity =null;
    private final Handler handler = new Handler();

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        listView = (ListView) findViewById(R.id.listView);

        mContext = getBaseContext();
        newsBarActivity = this;
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        Log.d(TAG_LOG, "News Bar Running! " + isMyServiceRunning(MyService.class));
        if(SplashScreen.retrieveFeedTask != null) {
            mFeed = SplashScreen.retrieveFeedTask.getFeed();
        }else
        {
            mFeed = getSavedFeeds();
        }

        if(mFeed == null /*get latest news*/)
        {
            //force reload
            RetrieveFeedTask retrieveFeedTask = (new RetrieveFeedTask(mContext, false));
            retrieveFeedTask.readUrls();
            mFeed = retrieveFeedTask.getFeed();
            saveCurrentFeeds(mFeed);
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // refresh handle
        doTheAutoRefresh();
        setSupportActionBar(toolbar);

        if(mFeed != null) {

            listView.setAdapter(new CustomListAdapter(this, mFeed.getMessages()));

            mRessources = defaultSharedPreferences.getStringSet("news_bar_resources",new HashSet<String>());
            mLanguageId = Integer.parseInt(defaultSharedPreferences.getString("news_bar_lang","0"));

            if(mLanguageId == 0)
            {
                listView.setTextDirection(View.TEXT_DIRECTION_RTL);
                listView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }else
            {
                listView.setTextDirection(View.TEXT_DIRECTION_LTR);
                listView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }
            // a. Animate list view slide down
            //animSideDown = AnimationUtils.loadAnimation(getApplicationContext(),
              //      R.anim.slide_down);
           // listView.startAnimation(animSideDown);

            Snackbar.make(listView, "Click on START Button to show News Bar .. ", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            //end of Animate list view slide down

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Object o = listView.getItemAtPosition(i);
                    FeedMessage feedMessage = (FeedMessage) o;
                    Intent ViewIntent = new Intent(mContext, ViewURL.class);
                    ViewIntent.putExtra("link", feedMessage.getLink());
                    startActivity(ViewIntent);
                }
            });

            mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher_background)
                            .setContentTitle("NewsFeedsBar")
                            .setContentText("Take it Now !");

            Intent resultIntent = new Intent(this, NewsFeedsBar.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);
            mBuilder.setContentIntent(pendingIntent);
            fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setImageResource(getServiceNewsStatus()?R.drawable.pause:R.drawable.play);
            //changeLanguageTo("fr");
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {


                    if(getServiceNewsStatus())
                    {
                        stopServiceNews();
                        defaultSharedPreferences.edit().putBoolean("show_preview",false).commit();
                    }else {
                        startServiceNews(false);
                        defaultSharedPreferences.edit().putBoolean("show_preview",true).commit();
                    }
                }
            });

        }else
        {
            Snackbar.make(listView, "Please check your network connection ", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private void doTheAutoRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(getServiceNewsStatus())
                {
                    restartServiceNews(true);
                }
                mRefreshDelay = Integer.parseInt(defaultSharedPreferences.getString("news_bar_refresh_delay","60"));
                Log.d(TAG_LOG, "refresh time   " + mRefreshDelay);
                refreshListNews();
                doTheAutoRefresh();
            }
        }, mRefreshDelay * 1000);
    }

    public static void restartServiceNews(Boolean reloadFeeds)
    {
        stopServiceNews();
        startServiceNews(reloadFeeds);
    }

    public static void startServiceNews(Boolean reloadFeeds)
    {
        sharedPreferences = mContext.getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        Intent svc = new Intent(mContext, MyService.class);
        if(reloadFeeds) {
            svc.setAction("reload");
        }else
        {
            svc.setAction("no");
        }
        mContext.startService(svc);
        prefsEditor.putBoolean("service_status", true);
        prefsEditor.commit();
        fab.setImageResource(R.drawable.pause);

    }

    public static void stopServiceNews()
    {
        sharedPreferences = mContext.getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        //check if the service is already stopped
        if(!sharedPreferences.getBoolean("service_status", false))
            return;
        Intent svc = new Intent(mContext, MyService.class);
        mContext.stopService(svc);
        prefsEditor.putBoolean("service_status", false);
        prefsEditor.commit();
        fab.setImageResource(R.drawable.play);

    }

    public static boolean getServiceNewsStatus()
    {
        sharedPreferences = mContext.getSharedPreferences("user_prefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean("service_status", false);
    }

    public static void refreshListNews() {
        //force reload
        RetrieveFeedTask retrieveFeedTask = (new RetrieveFeedTask(mContext, false));
        retrieveFeedTask.readUrls();
        mFeed = retrieveFeedTask.getFeed();
        if (mFeed == null) {
            Snackbar.make(listView, "Please Check your Internet connection", Snackbar.LENGTH_LONG)
                    .setActionTextColor(Color.RED)
                    .setAction("Action", null).show();

        } else
        {
            CustomListAdapter customListAdapter = new CustomListAdapter(mContext, mFeed.getMessages());
            listView.setAdapter(customListAdapter);
            customListAdapter.notifyDataSetChanged();
            Snackbar.make(listView, "News updated ", Snackbar.LENGTH_LONG).show();
          }

    }

    public void saveCurrentFeeds(Feed tmpFeed) {
        mPrefs = getSharedPreferences(FEED_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(tmpFeed);
        prefsEditor.putString("sSavedFeed", json);
        prefsEditor.commit();
        Log.v(TAG_LOG,"saveFeed" + tmpFeed);

    }
    public Feed getSavedFeeds() {
        Feed tmpFeed = null;
        mPrefs = getSharedPreferences(FEED_PREFS_NAME, MODE_PRIVATE);
        tmpFeed = new Gson().fromJson(mPrefs.getString("sSavedFeed", null), Feed.class);
        Log.v(TAG_LOG,"getSaved" + tmpFeed.toString());
        return tmpFeed;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(SplashScreen.splashActivity != null) {
            SplashScreen.splashActivity.finish();
        }
        if(SplashScreen.view != null) {
            SplashScreen.windowManager.removeView(SplashScreen.view);
            SplashScreen.view = null;
        }
        setContentView(R.layout.activity_news_feeds_bar);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_app, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(NewsFeedsBar.this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_refresh) {
            // refresh and change adapter
            refreshListNews();
            saveCurrentFeeds(mFeed);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        //Intent svc = new Intent(mContext, MyService.class);
       // svc.putExtra("feed_key", feed);
       // startService(svc);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public void onBackPressed() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        int imageResource = android.R.drawable.ic_dialog_alert;
        Drawable image = getResources().getDrawable(imageResource);

        builder.setTitle("Exit").setMessage("want to exit?").setIcon(image).setCancelable(false).setPositiveButton("yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        }).setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        alert.show();


    }

}


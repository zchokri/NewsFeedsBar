package czdev.newsfeedsbar;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;


public class NewsFeedsBar extends AppCompatActivity {

    public static String TAG_LOG = "NewsBar";
    NotificationCompat.Builder mBuilder;
    public static FloatingActionButton fab;
    int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 10001;
    String rssResult = "";
    Animation animSideDown;
    private boolean isPaused = false;
    public int  mRefreshDelay = 0;
    public int  mLanguageId= 0;
    public Set<String> mRessources = null;
    public int mPosition = 0;
    public Feed currentFeed;

    public static  Context mContext = null;
    public static SharedPreferences sharedPreferences = null;
    SharedPreferences defaultSharedPreferences = null;
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        final ListView listView = (ListView) findViewById(R.id.listView);

        mContext = getBaseContext();

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        Log.d(TAG_LOG, "News Bar Running! " + isMyServiceRunning(MyService.class));
        currentFeed = SplashScreen.retrieveFeedTask.getFeed();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(currentFeed != null) {
            System.out.println(currentFeed);
            for (FeedMessage message : currentFeed.getMessages()) {
                System.out.println(message);
                rssResult += message.getTitle();
                rssResult += " ~[~]~ ";

            }


            listView.setAdapter(new CustomListAdapter(this, currentFeed.getMessages()));

            mRessources = defaultSharedPreferences.getStringSet("news_bar_resources",new HashSet<String>());
            Log.d(TAG_LOG, "mRessources  " + mRessources.toString());



            mRefreshDelay = Integer.parseInt(defaultSharedPreferences.getString("news_bar_refresh_delay","0"));

            switch (mRefreshDelay){
                case 0:
                    Log.d(TAG_LOG, "mRefreshDelay" + mRefreshDelay);

                    break;
                case 1:
                    Log.d(TAG_LOG, "mRefreshDelay" + mRefreshDelay);

                    break;
                case 2:
                    Log.d(TAG_LOG, "mRefreshDelay" + mRefreshDelay);

                    break;
                default:
                    Log.d(TAG_LOG, "mRefreshDelay" + mRefreshDelay);

                    break;
            }
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
            animSideDown = AnimationUtils.loadAnimation(getApplicationContext(),
                    R.anim.slide_down);
            listView.startAnimation(animSideDown);

            Snackbar.make(listView, "Click on START Button to show News Bar .. ", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            //end of Animate list view slide down

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Object o = listView.getItemAtPosition(i);
                    FeedMessage feedMessage = (FeedMessage) o;
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(feedMessage.getLink()));
                    startActivity(browserIntent);
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
            sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
            fab.setImageResource(getServiceNewsStatus()?R.drawable.pause:R.drawable.play);

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
        Intent svc = new Intent(mContext, MyService.class);
        mContext.stopService(svc);
        prefsEditor.putBoolean("service_status", false);
        prefsEditor.commit();
        fab.setImageResource(R.drawable.play);

    }

    public static boolean getServiceNewsStatus()
    {
        sharedPreferences = mContext.getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean service_status = sharedPreferences.getBoolean("service_status", false);
        return service_status;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen.splashActivity.finish();
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
        getMenuInflater().inflate(R.menu.menu_customiser_screen_shot, menu);
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


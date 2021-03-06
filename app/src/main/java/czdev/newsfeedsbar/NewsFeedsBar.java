package czdev.newsfeedsbar;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.google.gson.Gson;
import com.wooplr.spotlight.SpotlightConfig;
import com.wooplr.spotlight.SpotlightView;
import java.util.HashSet;
import java.util.Set;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static czdev.newsfeedsbar.Constants.*;


public class NewsFeedsBar extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener  {

    NotificationCompat.Builder mBuilder;
    public static FloatingActionButton fab;
    Animation animSideDown;
    public int  mRefreshDelay = 0;
    public int  mLanguageId= 0;
    public Set<String> mRessources = null;
    public static Feed mFeed;
    public static SharedPreferences mPrefs;
    private static RecyclerView mRecyclerView;
    private static CustomListAdapter adapter;
    private static StaggeredGridLayoutManager staggeredGridLayoutManager;
    public static  Context mContext = null;
    public static SharedPreferences sharedPreferences = null;
    public  static SharedPreferences defaultSharedPreferences = null;
    public static Activity newsBarActivity =null;
    public static SearchView searchView = null;
    public static SpotlightView play = null;
    private final Handler handler = new Handler();
    public SpotlightConfig config = null;
    public static AlertDialog  alertConnection = null;
    public static SwipeRefreshLayout mSwipeRefreshLayout;
    ComponentName component;
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        fab = (FloatingActionButton) findViewById(R.id.fab);


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Feed newmFeed = new Feed("","","","","");

                for (FeedMessage feedMessage : mFeed.entries)
                {
                    if (feedMessage.getTitle().toLowerCase().contains(newText.toLowerCase()))
                    {
                        newmFeed.getMessages().add(feedMessage);
                    }
                }
                Log.d(TAG_LOG, "onQueryTextChange " );
                mRecyclerView.setLayoutManager(staggeredGridLayoutManager);
                adapter.updateData(newmFeed.getMessages());
                return false;
            }
           });

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
            RetrieveFeedTask retrieveFeedTask = (new RetrieveFeedTask(mContext, false, false));
            retrieveFeedTask.readUrls();
            mFeed = retrieveFeedTask.getFeed();
            saveCurrentFeeds(mFeed);
        }
        // refresh handle
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                doTheAutoRefresh();
            }
        }, 300000);

        if(mFeed != null) {

            mRecyclerView.setLayoutManager(staggeredGridLayoutManager);
            adapter = new CustomListAdapter(mContext, mFeed.getMessages());
            mRecyclerView.setAdapter(adapter);
            RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
            itemAnimator.setAddDuration(300);
            itemAnimator.setRemoveDuration(300);
            mRecyclerView.setItemAnimator(itemAnimator);
            mRessources = defaultSharedPreferences.getStringSet("news_bar_resources",new HashSet<String>());
            mLanguageId = Integer.parseInt(defaultSharedPreferences.getString("news_bar_lang","2"));

            if(mLanguageId == 0)
            {
                mRecyclerView.setTextDirection(View.TEXT_DIRECTION_RTL);
                mRecyclerView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }else
            {
                mRecyclerView.setTextDirection(View.TEXT_DIRECTION_LTR);
                mRecyclerView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }

            mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher_background)
                            .setContentTitle("NewsFeedsBar")
                            .setContentText("Take it Now !");

            Intent resultIntent = new Intent(this, NewsFeedsBar.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);
            mBuilder.setContentIntent(pendingIntent);
            fab.setImageResource(getServiceNewsStatus()?R.drawable.pause:R.drawable.play);
            //changeLanguageTo("fr");
            get_Started();

        }else
        {
            if (!isNetworkConnected()) {
                alertRequestInternet();
            }
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                if(getServiceNewsStatus())
                {
                    stopServiceNews();
                    defaultSharedPreferences.edit().putBoolean("show_preview",false).commit();
                }else if(mFeed != null)
                {
                    startServiceNews(false);
                    defaultSharedPreferences.edit().putBoolean("show_preview",true).commit();
                }
            }
        });

    }


    private  static void alertRequestInternet()
    {
        if( alertConnection == null) {
            alertConnection = new AlertDialog.Builder(newsBarActivity)
                    .setTitle("No Internet Connection")
                    .setMessage("It looks like your internet connection is off. Please turn it " +
                            "on and try again")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            alertConnection = null;
                        }
                    }).setIcon(android.R.drawable.ic_dialog_alert).show();
        }

    }
    private void get_Started() {
         play  = new SpotlightView.Builder(this)
                 .setConfiguration(config)
                .headingTvText("Play Breaking News Bar")
                .subHeadingTvText("Click Play to start \nBreaking and latest News bar.")
                .target(fab)
                .enableDismissAfterShown(true)
                .usageId("250") //UNIQUE ID
                .show();
    }

    private void doTheAutoRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(getServiceNewsStatus())
                {
                    stopServiceNews();
                }
                mRefreshDelay = Integer.parseInt(defaultSharedPreferences.getString("news_bar_refresh_delay","300"));
                Log.d(TAG_LOG, "refresh time   " + mRefreshDelay);
                refreshListNews(false);
                if(getServiceNewsStatus())
                startServiceNews(false);
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
        defaultSharedPreferences.edit().putBoolean("show_preview",true).commit();
        if(fab != null) {
            fab.setImageResource(R.drawable.pause);
        }

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
        defaultSharedPreferences.edit().putBoolean("show_preview",false).commit();
        if(fab != null) {
            fab.setImageResource(R.drawable.play);
        }

    }

    public static boolean getServiceNewsStatus()
    {
        sharedPreferences = mContext.getSharedPreferences("user_prefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean("service_status", false);
    }

    public static void refreshListNews(boolean force_refresh) {
        //force reload
        RetrieveFeedTask retrieveFeedTask = (new RetrieveFeedTask(mContext, false, force_refresh));
        if(mPrefs.getString("refresh_requested","Yes").contains("Yes")) {
            retrieveFeedTask.readUrls();
            mFeed = retrieveFeedTask.getFeed();
            if (mFeed != null) {
                mRecyclerView.setLayoutManager(staggeredGridLayoutManager);
                adapter.updateData(mFeed.getMessages());
                Log.d(TAG_LOG, "updateData " );
                saveCurrentFeeds(mFeed);
                Toast.makeText(mContext, "News updated ", Toast.LENGTH_LONG).show();
            } else {
                alertRequestInternet();
                Toast.makeText(mContext, "No Internet Connection !  ", Toast.LENGTH_LONG).show();

            }
        }else
        {
            Log.v(TAG_LOG, "refresh_requested => " + mPrefs.getString("refresh_requested","Yes"));
            if (!isNetworkConnected()) {
                alertRequestInternet();
            }else {
                if (mFeed != null) {
                    Toast.makeText(mContext, "News already updated ", Toast.LENGTH_LONG).show();
                }
            }



        }
        mSwipeRefreshLayout.setRefreshing(false);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {

            if(resultCode == RESULT_OK){
                Log.v(TAG_LOG, "refresh_requested RESULT_OK => " + mPrefs.getString("refresh_requested","Yes"));
            }
            if (resultCode == RESULT_CANCELED) {
                Log.v(TAG_LOG, "refresh_requested RESULT_CANCELED => " + mPrefs.getString("refresh_requested","Yes"));
            }
        }
    }

    public static void saveCurrentFeeds(Feed tmpFeed) {
        if(tmpFeed != null) {
            mPrefs = mContext.getSharedPreferences(FEED_PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            Gson gson = new Gson();
            String json = gson.toJson(tmpFeed);
            prefsEditor.putString("sSavedFeed", json);
            prefsEditor.commit();
        }

    }
    public Feed getSavedFeeds() {
        Feed tmpFeed = null;
        mPrefs = getSharedPreferences(FEED_PREFS_NAME, MODE_PRIVATE);
        tmpFeed = new Gson().fromJson(mPrefs.getString("sSavedFeed", null), Feed.class);
        return tmpFeed;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(SplashScreen.splashActivity != null) {
            if(SplashScreen.mProgressDialog != null) {
                SplashScreen.mProgressDialog.dismiss();
            }
            SplashScreen.splashActivity.finish();
        }
        setContentView(R.layout.main_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mContext = getBaseContext();
        newsBarActivity = this;
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        component = new ComponentName(mContext, NetworkStateReceiver.class);
        initSharedDefaultValues();
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        staggeredGridLayoutManager = new StaggeredGridLayoutManager(1,StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(staggeredGridLayoutManager);
        searchView = (SearchView) findViewById(R.id.searchView);
        searchView.onActionViewExpanded();
        searchView.setIconified(true);
        config = new SpotlightConfig();
        config.setDismissOnBackpress(true);
        config.setDismissOnTouch(true);
        config.setLineAndArcColor(Color.parseColor("#eb273f"));
        config.setSubHeadingTvColor(Color.parseColor("#ffffff"));
        config.setHeadingTvSize(32);
        config.setRevealAnimationEnabled(true);
        config.setHeadingTvColor(Color.parseColor("#eb273f"));
        config.setFadingTextDuration(400);
        config.setMaskColor(Color.parseColor("#dc000000"));
        config.setSubHeadingTvSize(16);
        config.setLineAnimationDuration(400);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshListNews(false);
            }
        });
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
    private static boolean isNetworkConnected() {
        ConnectivityManager connMgr = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE); // 1
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo(); // 2
        return networkInfo != null && networkInfo.isConnected(); // 3
    }


    @Override
    protected void onDestroy() {
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
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    public void initSharedDefaultValues() {

        mPrefs = mContext.getSharedPreferences(FEED_PREFS_NAME, MODE_PRIVATE);

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_manage) {
           startActivityForResult(new Intent(NewsFeedsBar.this, SettingsActivity.class),1);

        } else if (id == R.id.nav_share) {

            Intent myShareIntent = new Intent(Intent.ACTION_SEND);
            myShareIntent.setType("text/plain");
            myShareIntent.putExtra(Intent.EXTRA_TEXT, " News Bar Application : "
                    + "https://play.google.com/store/apps/details?id=czdev.newsfeedsbar");
            myShareIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(Intent.createChooser(myShareIntent,"News Bar Share using").setFlags(FLAG_ACTIVITY_NEW_TASK));

        } else if (id == R.id.nav_send) {

            String uriText =
                    "mailto:" +
                            "?subject=" + Uri.encode("News Bar Application") +
                            "&body=" + Uri.encode("Breaking and lastest News, Got it under  https://play.google.com/store/apps/details?id=czdev.newsfeedsbar");

            Uri uri = Uri.parse(uriText);

            Intent sendIntent = new Intent(Intent.ACTION_SENDTO);
            sendIntent.setData(uri);
            startActivity(Intent.createChooser(sendIntent, "Send email"));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}


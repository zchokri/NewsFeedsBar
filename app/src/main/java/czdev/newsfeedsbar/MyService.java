package czdev.newsfeedsbar;

import android.app.Service;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static czdev.newsfeedsbar.Constants.*;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MyService extends Service {

    SharedPreferences mPrefs;
    private View view;
    WindowManager.LayoutParams p = null;
    WindowManager windowManager = null;
    LayoutInflater layoutInflater = null;
    View popupView = null;
    View showHideView = null;
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;
    public HorizontalScrollView horizontalScrollView = null;
    public LinearLayout linearLayout = null;
    public RelativeLayout relativeLayout = null;

    public Button btnResumeNews = null;

    public Animation myRotation = null;
    private long lastPressTime;
    Feed mFeed;
    private boolean isPaused = false;
    public int scrollX = 0;
    private int mStartingPosition = 0;
    private int step = 0;
    private int screenBarPosition = 0;
    public int mLanguageId = 0;
    public int currentSpeed = 0;
    public int textSize = 0;
    public int  mRefreshDelay = 0;
    private final Handler handler = new Handler();
    String rssResult = "";
    SharedPreferences defaultSharedPreferences = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if(linearLayout != null) {
            linearLayout.removeAllViewsInLayout();
            linearLayout.removeAllViews();
        }
        if(popupView != null) {
            windowManager.removeView(popupView);
            popupView = null;
        }
        if(view != null) {
            windowManager.removeView(view);
            view = null;
        }
        Log.d(TAG_LOG, "onDestroy service " );

        super.onDestroy();
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent !=null && intent.getExtras()!=null)
            mFeed = (Feed) intent.getSerializableExtra("feed_key");

        if(intent  != null && intent.getAction().toString().equals("reload"))
        {
           RetrieveFeedTask retrieveFeedTask =  (new RetrieveFeedTask(this,false));
           retrieveFeedTask.readUrls();
           mFeed = retrieveFeedTask.getFeed();

        }

        if(mFeed == null)
        {
            mFeed = getSavedFeeds();
        }
        else {

            if(SplashScreen.retrieveFeedTask != null) {
                mFeed = SplashScreen.retrieveFeedTask.getFeed();
            }else
            {
                //force reload
                RetrieveFeedTask retrieveFeedTask =  (new RetrieveFeedTask(this,false));
                retrieveFeedTask.readUrls();
                mFeed = retrieveFeedTask.getFeed();
                saveCurrentFeeds(mFeed);

            }
        }
        if(mFeed != null) {
            saveCurrentFeeds(mFeed);
        }
        else {
            mFeed = getSavedFeeds();
        }
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //doTheAutoRefresh();
        showWindowManager();
        return super.onStartCommand(intent, flags, startId);
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

    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                // downward swipe
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    Toast.makeText(popupView.getContext(), "Downward Swipe", Toast.LENGTH_SHORT).show();
                    horizontalScrollView.setVisibility(View.INVISIBLE);
                    btnResumeNews.setVisibility(View.VISIBLE);
                }
              } catch (Exception e) {
                // nothing
            }
            startAutoScroll();
            return false;
        }

    }

     public void showWindowManager() {


         gestureDetector = new GestureDetector(getApplicationContext(),new MyGestureDetector());

        p = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT > Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);


        mLanguageId = Integer.parseInt(defaultSharedPreferences.getString("news_bar_lang","0"));
        Log.d(TAG_LOG, "mLanguageId  " + mLanguageId);
        screenBarPosition = Integer.parseInt(defaultSharedPreferences.getString("news_bar_display_position","0"));
        Log.d(TAG_LOG, "screenBarPosition" + screenBarPosition);
        Set<String> multiSelectListPreference = defaultSharedPreferences.getStringSet("news_bar_text_style", Collections.<String>emptySet() );
        boolean boldText = multiSelectListPreference.contains("0");
        boolean italicText = multiSelectListPreference.contains("1");

        if(screenBarPosition == 0) {
            p.gravity = Gravity.TOP;
        }else {
            p.gravity = Gravity.BOTTOM;
        }

        currentSpeed  = Integer.parseInt(defaultSharedPreferences.getString("news_bar_display_speed","200"));
        textSize  = Integer.parseInt(defaultSharedPreferences.getString("news_bar_display_text_size","10"));

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutInflater =
                (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        popupView = layoutInflater.inflate(R.layout.wm_shape, null);
        relativeLayout = popupView.findViewById(R.id.rl);
        btnResumeNews = popupView.findViewById(R.id.resumeNewsBar);
        btnResumeNews.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                horizontalScrollView.setVisibility(View.VISIBLE);
                btnResumeNews.setVisibility(View.INVISIBLE);
            }
        });
        horizontalScrollView = popupView.findViewById(R.id.horizontalScrollView);
        //arbic = 0
        if(mLanguageId == 0)
        {
            horizontalScrollView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }else
        {
            horizontalScrollView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }

        linearLayout = popupView.findViewById(R.id.linearLayout);
        for (final FeedMessage message : mFeed.getMessages()) {
            ImageView imageView = new ImageView(this);
            final TextView textView = new TextView(this);
            if(message.getLink().contains("cnn")) {
                imageView.setImageResource(R.drawable.cnn2);
            }else if(message.getLink().contains("24"))
            {
                imageView.setImageResource(R.drawable.f24);

            } if(message.getLink().contains("jaze")) {
                imageView.setImageResource(R.drawable.jsc);

            }

            linearLayout.addView(imageView);
            if(boldText && italicText)
                textView.setTypeface(textView.getTypeface(), Typeface.BOLD_ITALIC);
            else if (boldText)
                textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            else if (italicText)
                textView.setTypeface(textView.getTypeface(), Typeface.ITALIC);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(textSize);
            textView.setText(Html.fromHtml(" - " +message.getTitle() + " - "));
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setOnTouchListener(new textViewOnTouchListener());
            textView.setOnClickListener(new DoubleClickListener() {

                @Override
                public void onSingleClick(View v) {

                }

                @Override
                public void onDoubleClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getLink()));
                    browserIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    startActivity(browserIntent);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isPaused = false;
                    textView.setBackgroundColor(Color.parseColor("#808080"));
                    startAutoScroll();
                }
            });
            linearLayout.addView(textView);
            //System.out.println(message);
            rssResult += message.getTitle();
            rssResult += " ~[~]~ ";

        }

        windowManager.addView(popupView, p);

        horizontalScrollView.setOnTouchListener(new horizontalScrollViewOnTouchListener());
        startAutoScroll();
        horizontalScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                startAutoScroll();

            }
        });

    }

    public void startAutoScroll() {
        DisplayMetrics displaymetrics = getApplicationContext().getResources().getDisplayMetrics();
        int screenWidth = displaymetrics.widthPixels;

        if(mLanguageId != 0) {
            //fr en
            mStartingPosition = 0;
            step = 10;
        }else
        {
            //ar
            mStartingPosition =  horizontalScrollView.getChildAt(0).getMeasuredWidth() - screenWidth;
            step = -10;
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //scrollX = scrollX + 5;
                if(!isPaused) {

                     scrollX = scrollX + step;
                    if (scrollX > 0) {
                        horizontalScrollView.scrollTo(scrollX, 0);
                    } else {
                        // to repeat scrolling
                        scrollX = mStartingPosition;
                        horizontalScrollView.scrollTo(mStartingPosition, 0);
                    }
                }


            }
        }, currentSpeed);

    }

    private final class horizontalScrollViewOnTouchListener implements View.OnTouchListener {
        public boolean onTouch(View view, MotionEvent motionEvent) {

            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    isPaused = true;
                    Log.d(TAG_LOG, "ACTION_DOWN isPaused " + isPaused);
                    horizontalScrollView.setBackgroundColor(Color.RED);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(TAG_LOG, "ACTION_UP isPaused " + isPaused);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isPaused = false;
                    horizontalScrollView.setBackgroundColor(Color.parseColor("#808080"));
                    startAutoScroll();
                    break;
            }

            for (int i = 0; i < horizontalScrollView.getChildCount(); i++) {
                View child = horizontalScrollView.getChildAt(i);
                if (child.getVisibility() == View.VISIBLE) {
                    if(child == linearLayout)
                    {
                        for (int j = 0; j < linearLayout.getChildCount(); j++) {
                            View child2 = linearLayout.getChildAt(j);
                            int[] l = new int[2];
                            child2.getLocationOnScreen(l);
                            int x = l[0];
                            int y = l[1];
                            DisplayMetrics displaymetrics = getApplicationContext().getResources().getDisplayMetrics();
                            float screenWidth = displaymetrics.widthPixels;
                            if (x > screenWidth / 2  && x < (screenWidth + 3/4 * screenWidth)) {
                                child2.dispatchTouchEvent(motionEvent);
                            }
                        }
                    }
                }
            }
            return gestureDetector.onTouchEvent(motionEvent);
        }
    }

    private final class textViewOnTouchListener implements View.OnTouchListener {
        public boolean onTouch(View view, MotionEvent motionEvent) {

            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    isPaused = true;
                    horizontalScrollView.setBackgroundColor(Color.RED);
                    break;
                case MotionEvent.ACTION_UP:
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isPaused = false;
                    horizontalScrollView.setBackgroundColor(Color.parseColor("#808080"));
                    startAutoScroll();
                    break;
            }

            return gestureDetector.onTouchEvent(motionEvent);

        }
    }
    public abstract class DoubleClickListener implements OnClickListener {

        private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds

        long lastClickTime = 0;

        @Override
        public void onClick(View v) {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
                onDoubleClick(v);
                lastClickTime = 0;
            } else {
                onSingleClick(v);
            }
            lastClickTime = clickTime;
        }

        public abstract void onSingleClick(View v);
        public abstract void onDoubleClick(View v);
    }
}
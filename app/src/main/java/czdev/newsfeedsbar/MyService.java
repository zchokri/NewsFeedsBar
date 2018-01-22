package czdev.newsfeedsbar;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MyService extends Service implements OnClickListener {

    public static final String FEED_PREFS_NAME = "FEED_PREFS";
    SharedPreferences mPrefs;
    public String TAG_LOG = "NewsBar";
    private View view;
    WindowManager.LayoutParams p = null;
    WindowManager windowManager = null;
    LayoutInflater layoutInflater = null;
    View popupView = null;
    public HorizontalScrollView horizontalScrollView = null;
    public LinearLayout linearLayout = null;
    public Animation myRotation = null;
    private static final long DOUBLE_PRESS_INTERVAL = 250; // in millis
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
    int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 10001;
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
    public void onClick(View v) {
        Toast.makeText(this, "Overlay button click event", Toast.LENGTH_SHORT).show();
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
        }else {
            if(SplashScreen.retrieveFeedTask != null) {
                mFeed = SplashScreen.retrieveFeedTask.getFeed();
            }else
            {
                //force reload
                RetrieveFeedTask retrieveFeedTask =  (new RetrieveFeedTask(this,false));
                retrieveFeedTask.readUrls();
                mFeed = retrieveFeedTask.getFeed();

            }
        }
        if(mFeed != null) {
            saveCurrentFeeds();
        }
        else {
            mPrefs = getSharedPreferences(FEED_PREFS_NAME, MODE_PRIVATE);
            Gson gson = new Gson();
            String json = mPrefs.getString("SerializableObject", "");
            mFeed = gson.fromJson(json, Feed.class);
        }
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //doTheAutoRefresh();
        showWindowManager();
        return super.onStartCommand(intent, flags, startId);
    }

    public void showWindowManager() {

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

            myRotation = AnimationUtils.loadAnimation(popupView.getContext(), R.anim.rotator);
            myRotation.setRepeatCount(Animation.INFINITE);
            imageView.startAnimation(myRotation);
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
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG_LOG, "onClick textView " );
                    // Get current time in nano seconds.
                    long pressTime = System.currentTimeMillis();
                    // If double click...
                    if (pressTime - lastPressTime <= DOUBLE_PRESS_INTERVAL) {
                        textView.setBackgroundColor(Color.BLUE);
                        isPaused = true;
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
                    // record the last time the menu button was pressed.
                    lastPressTime = pressTime;

                }
            });
            stripUnderlines(textView);
            linearLayout.addView(textView);
            //System.out.println(message);
            rssResult += message.getTitle();
            rssResult += " ~[~]~ ";

        }

        windowManager.addView(popupView, p);

        horizontalScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
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
                return false;
            }


        });


        startAutoScroll();
        horizontalScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                startAutoScroll();

            }
        });

    }

    public void saveCurrentFeeds() {
        mPrefs = getSharedPreferences(FEED_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(mFeed);
        prefsEditor.putString("SerializableObject", json);
        prefsEditor.commit();
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
    private void doTheAutoRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRefreshDelay = Integer.parseInt(defaultSharedPreferences.getString("news_bar_refresh_delay","60"));
                Log.d(TAG_LOG, "refresh time   " + mRefreshDelay);
                RetrieveFeedTask retrieveFeedTask =  (new RetrieveFeedTask(getApplicationContext(),false));
                retrieveFeedTask.readUrls();
                mFeed = retrieveFeedTask.getFeed();
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
                showWindowManager();
                doTheAutoRefresh();

            }
        }, mRefreshDelay * 1000);
    }

    private void stripUnderlines(TextView textView) {
        Spannable s = new SpannableString(textView.getText());
        URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
        for (URLSpan span: spans) {
            int start = s.getSpanStart(span);
            int end = s.getSpanEnd(span);
            s.removeSpan(span);
            span = new URLSpanNoUnderline(span.getURL());
            s.setSpan(span, start, end, 0);
        }
        textView.setText(s);
    }

    private class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }
        @Override public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setColor(Color.WHITE);
        }
    }
}
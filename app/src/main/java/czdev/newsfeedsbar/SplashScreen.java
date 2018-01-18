package czdev.newsfeedsbar;

/**
 * Created by ZAGROUBA CHOKRI on 09/01/2018.
 */

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.VideoView;

import com.google.gson.Gson;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import static czdev.newsfeedsbar.MyService.FEED_PREFS_NAME;
import static czdev.newsfeedsbar.NewsFeedsBar.mContext;

public class SplashScreen extends Activity {

    private static Feed mFeed;
    private static RetrieveFeedTask retrieveFeedTask;
    RelativeLayout.LayoutParams absParams = null;
    int counter = 0;
    private VideoView videoView;
    WindowManager.LayoutParams p;
    WindowManager windowManager;
    LayoutInflater layoutInflater;
    public String TAG_LOG = "NewsBar";
    public static SharedPreferences mPrefs;
    public final static int REQUEST_CODE = -1010101;

    View view;

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
            p = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT > Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    PixelFormat.TRANSLUCENT);


            p.gravity = Gravity.CENTER;

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            layoutInflater =
                    (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.activity_splash, null);
            windowManager.addView(view, p);
            videoView = view.findViewById(R.id.videoViewSplash);

            try {
                // ID of video file.
                videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.splash));
                videoView.start();

            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }

            /**
             * Showing splashscreen while making network calls to download necessary
             * data before launching the app Will use AsyncTask to make http call
             */
            retrieveFeedTask = new RetrieveFeedTask();
            retrieveFeedTask.execute("http://www.aljazeera.com/xml/rss/all.xml", "https://arabic.cnn.com/rss" );
            mPrefs = getSharedPreferences(FEED_PREFS_NAME, MODE_PRIVATE);
        }



        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.v("App","Build Version Greater than or equal to M: "+Build.VERSION_CODES.M);
                checkDrawOverlayPermission();
            }else{
                Log.v("App","OS Version Less than M");
                //No need for Permission as less then M OS.
            }


    }

    public  static  Feed getFeed()
    {
        try {

            mFeed = retrieveFeedTask.get();
            if(mFeed == null) {
                Gson gson = new Gson();
                String json = mPrefs.getString("SerializableObject", "");
                mFeed = gson.fromJson(json, Feed.class);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return mFeed;
    }
    /**
     * Async Task to make http call
     */
    private class RetrieveFeedTask extends AsyncTask<String, String, Feed> {

        static final String TITLE = "title";
        static final String DESCRIPTION = "description";
        static final String CHANNEL = "channel";
        static final String LANGUAGE = "language";
        static final String COPYRIGHT = "copyright";
        static final String LINK = "link";
        static final String AUTHOR = "author";
        static final String ITEM = "item";
        static final String PUB_DATE = "pubDate";
        static final String GUID = "guid";
        public Context context = null;

        private String getCharacterData(XMLEvent event, XMLEventReader eventReader)
                throws XMLStreamException {
            String result = "";
            event = eventReader.nextEvent();
            if (event instanceof Characters) {
                result = event.asCharacters().getData();
            }
            return result;
        }

        protected Feed doInBackground(String... urls) {

                try {
                    Feed feed = null;
                    boolean isFeedHeader = true;
                    for (int i = 0; i < urls.length; i++) {
                        Log.d(TAG_LOG, "url " + i + "   " + urls[i]  );
                        URL url = new URL(urls[i]);
                        URLConnection urlCon = url.openConnection();
                        try {
                            // Set header values intial to the empty string
                            String description = "";
                            String title = "";
                            String link = "";
                            String language = "";
                            String copyright = "";
                            String author = "";
                            String pubdate = "";
                            String guid = "";

                            // First create a new XMLInputFactory
                            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                            Log.d(TAG_LOG, "First create a new XMLInputFactory " + i + "   " + urls[i]  );
                            // Setup a new eventReader
                            Log.d(TAG_LOG, "Setup a new eventReader  " + i + "   " + urls[i]  );
                            InputStream in = urlCon.getInputStream();
                            XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
                            // read the XML document
                            Log.d(TAG_LOG, "read the XML document  " + i + "   " + urls[i]  );

                            while (eventReader.hasNext()) {
                                XMLEvent event = eventReader.nextEvent();
                                if (event.isStartElement()) {
                                    String localPart = event.asStartElement().getName()
                                            .getLocalPart();
                                    switch (localPart) {
                                        case ITEM:
                                            if (isFeedHeader) {
                                                isFeedHeader = false;
                                                feed = new Feed(title, link, description, language,
                                                        copyright, pubdate);
                                                Log.d(TAG_LOG, "isFeedHeader " + i + "   " + urls[i]  );

                                            }
                                            event = eventReader.nextEvent();
                                            break;
                                        case TITLE:
                                            title = getCharacterData(event, eventReader);
                                            break;
                                        case DESCRIPTION:
                                            description = getCharacterData(event, eventReader);
                                            break;
                                        case LINK:
                                            link = getCharacterData(event, eventReader);
                                            break;
                                        case GUID:
                                            guid = getCharacterData(event, eventReader);
                                            break;
                                        case LANGUAGE:
                                            language = getCharacterData(event, eventReader);
                                            break;
                                        case AUTHOR:
                                            author = getCharacterData(event, eventReader);
                                            break;
                                        case PUB_DATE:
                                            pubdate = getCharacterData(event, eventReader);
                                            break;
                                        case COPYRIGHT:
                                            copyright = getCharacterData(event, eventReader);
                                            break;
                                    }
                                } else if (event.isEndElement()) {
                                    if (event.asEndElement().getName().getLocalPart() == (ITEM)) {
                                        FeedMessage message = new FeedMessage();
                                        message.setAuthor(author);
                                        message.setDescription(description);
                                        message.setGuid(pubdate);
                                        message.setLink(link);
                                        message.setTitle(title);
                                        feed.getMessages().add(message);
                                        event = eventReader.nextEvent();
                                        continue;
                                    }
                                }
                            }
                        } catch (XMLStreamException | NetworkOnMainThreadException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    Log.d(TAG_LOG, "feed.getMessages().size   " + feed.getMessages().size() );
                    return feed;

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

        }


        @Override
        protected void onPostExecute(Feed result) {
            super.onPostExecute(result);
            // After completing http call
            // will close this activity and lauch main activity
            windowManager.removeView(view);
            videoView.stopPlayback();
            Intent i = new Intent(SplashScreen.this, NewsFeedsBar.class);
            startActivity(i);

            // close this activity
            finish();
        }

    }

    }
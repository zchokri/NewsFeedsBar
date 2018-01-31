package czdev.newsfeedsbar;

/**
 * Created by Eskan on 18/01/2018.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.NetworkOnMainThreadException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;
import static czdev.newsfeedsbar.Constants.*;


/**
 * Async Task to make http call
 */
public class RetrieveFeedTask extends AsyncTask< List<String>, String, List<String>> implements Observer {

    public Context mContext = null;
    private static ArrayList<Feed> feeds = new ArrayList<>();
    private XMLParser xmlParser;
    SharedPreferences mPrefs;
    SharedPreferences defaultSharedPreferences;
    private OnTaskCompleted onComplete;


    public int mLanguageId = 0;
    Boolean mStartMainActivity = false;
    public  static long lastRefreshDate = new Date(System.currentTimeMillis()).getTime();
    public  static long currentRefreshDate = lastRefreshDate;

    public RetrieveFeedTask(Context ctx,boolean startMainActivity, boolean force_refresh)  {


        this.mContext = ctx;
        this.mStartMainActivity = startMainActivity;
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        Log.d(TAG_LOG, "Try to retreive data RetrieveFeedTask");
        mPrefs = this.mContext.getSharedPreferences(FEED_PREFS_NAME, MODE_PRIVATE);
        currentRefreshDate = new Date(System.currentTimeMillis()).getTime();
        Log.d(TAG_LOG, "lastRefreshDate " + lastRefreshDate);
        Log.d(TAG_LOG, "currentRefreshDate " + currentRefreshDate);

        if(mPrefs.getString("refresh_requested","Yes").contains("Yes")) {
            force_refresh = true;
        }

        if((currentRefreshDate > (lastRefreshDate + 60000) )|| force_refresh) {
            lastRefreshDate = currentRefreshDate;

            mPrefs.edit().putString("refresh_requested", "Yes").apply();
            Log.d(TAG_LOG, "refresh_requested Yes " );

        }else
        {
            mPrefs.edit().putString("refresh_requested", "No").apply();
            Log.d(TAG_LOG, "refresh_requested No " );


        }

        xmlParser = new XMLParser();
        xmlParser.addObserver(this);

    }
    public interface OnTaskCompleted {
        void onTaskCompleted(ArrayList<Feed> list);

        void onError();
    }

    public void onFinish(OnTaskCompleted onComplete) {
        this.onComplete = onComplete;
    }

    @Override
    public void update(Observable o, Object arg) {
        feeds = (ArrayList<Feed>) arg;
        onComplete.onTaskCompleted(feeds);
    }



    private String getCharacterData(XMLEvent event, XMLEventReader eventReader)
            throws XMLStreamException {
        String result = "";
        event = eventReader.nextEvent();
        if (event instanceof Characters) {
            result = event.asCharacters().getData();
        }
        return result;
    }

    private static int getDate()
    {
        DateFormat dateFormat = new SimpleDateFormat("dd");
        return Integer.parseInt(dateFormat.format(new Date()));
    }

    private int getNewsDaySelected()
    {
        String day = defaultSharedPreferences.getString("news_day","0");
        return Integer.parseInt(day);
    }

    protected List<String> doInBackground(List<String>... urls) {

        Response response = null;
        List<String> GlobalResponse = new ArrayList<String>();;
        OkHttpClient client = new OkHttpClient();

        int news_day = getNewsDaySelected();
        Feed feed = null;
        boolean isFeedHeader = true;
            Log.i("RSS Parser ", "urls[0].size() " + urls[0].size());
        for (int i = 0; i < urls[0].size(); i++) {
            Log.i("tag", "rls[0].get " + i  + "-> "+ urls[0].get(i));

            Request request = new Request.Builder()
                    .url(urls[0].get(i))
                    .build();

            try {
                response = client.newCall(request).execute();
                if (response.isSuccessful())
                    GlobalResponse.add(response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return GlobalResponse;
    }



    @Override
    protected void onPostExecute(List<String> result) {
        super.onPostExecute(result);

        if (result != null) {
            try {
                for(String res : result) {
                    xmlParser.parseXML(res);
                }
                Log.i("RSS Parser ", "All RSS parsed correctly!");
            } catch (Exception e) {
                e.printStackTrace();
                onComplete.onError();
            }
        } else
            onComplete.onError();
        // After completing http call
        // will close this activity and lauch main activity
        if(mStartMainActivity) {
            Intent i = new Intent(mContext, NewsFeedsBar.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(i);
        }
    }




}
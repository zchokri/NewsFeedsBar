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
import android.util.Log;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import static android.content.Context.MODE_PRIVATE;
import static czdev.newsfeedsbar.Constants.*;


/**
 * Async Task to make http call
 */
public class RetrieveFeedTask extends AsyncTask< String, String, Feed> {

    public Context mContext = null;
    private static Feed mFeed;
    SharedPreferences mPrefs;
    SharedPreferences defaultSharedPreferences;
    public int mLanguageId = 0;
    Boolean mStartMainActivity = false;
    public  static long lastRefreshDate = 0;
    public  static long currentRefreshDate = 0;

    public RetrieveFeedTask(Context ctx,boolean startMainActivity)  {
        this.mContext = ctx;
        this.mStartMainActivity = startMainActivity;
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        Log.d(TAG_LOG, "Try to retreive data RetrieveFeedTask");
        mPrefs = this.mContext.getSharedPreferences(FEED_PREFS_NAME, MODE_PRIVATE);
        lastRefreshDate = mPrefs.getLong("lastRefreshDate",new Date(System.currentTimeMillis()).getTime());
        currentRefreshDate = new Date(System.currentTimeMillis()).getTime();
        Log.d(TAG_LOG, "lastRefreshDate " + lastRefreshDate);
        Log.d(TAG_LOG, "currentRefreshDate " + currentRefreshDate);

        if(currentRefreshDate > (lastRefreshDate + 300000) ) {
            mPrefs.edit().putLong("currentRefreshDate", lastRefreshDate).apply();
            mPrefs.edit().putString("refresh_requested", "Yes").apply();
            Log.d(TAG_LOG, "refresh_requested Yes " );

        }else
        {
            mPrefs.edit().putString("refresh_requested", "No").apply();
            Log.d(TAG_LOG, "refresh_requested No " );


        }

    }

    public void readUrls() {

        mLanguageId = Integer.parseInt(defaultSharedPreferences.getString("news_bar_lang","0"));
        Set<String> resources = defaultSharedPreferences.getStringSet("news_bar_resources", null );
        UrlsParser urlsParser = new UrlsParser(mContext, mLanguageId, resources);
        urlsParser.execute();

        try {
            execute(urlsParser.get().toArray(new String[urlsParser.get().size()]));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
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

    protected Feed doInBackground(String... urls) {
        try {
            int news_day = getNewsDaySelected();
            Feed feed = null;
            boolean isFeedHeader = true;
            for (int i = 0; i < urls.length; i++) {
                //Log.d(TAG_LOG, "url " + i + "   " + urls[i] );
                URL url = new URL(urls[i]);
                URLConnection urlCon = url.openConnection();
                try {
                    // Set header values intial to the empty string
                    String description = "";
                    String title = "";
                    String link = "";
                    String language = "";
                    String author = "";
                    String pubdate = "";

                    // First create a new XMLInputFactory
                    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                    // Setup a new eventReader
                    InputStream in = urlCon.getInputStream();
                    urlCon.setConnectTimeout(5000);
                    XMLEventReader eventReader = inputFactory.createXMLEventReader(in);

                    // read the XML document
                    while (eventReader.hasNext()) {
                        XMLEvent event = eventReader.nextEvent();
                        if (event.isStartElement()) {
                            String localPart = event.asStartElement().getName()
                                    .getLocalPart();
                            switch (localPart) {
                                case ITEM:
                                    if (isFeedHeader) {
                                        isFeedHeader = false;
                                        feed = new Feed(title, link, description, language, pubdate);
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
                                case LANGUAGE:
                                    language = getCharacterData(event, eventReader);
                                    break;
                                case AUTHOR:
                                    author = getCharacterData(event, eventReader);
                                    break;
                                case PUB_DATE:
                                    pubdate = getCharacterData(event, eventReader);
                                    break;
                            }
                        } else if (event.isEndElement()) {
                            if (event.asEndElement().getName().getLocalPart().equals(ITEM) &&
                                    getDate() - Integer.parseInt(pubdate.split(" ")[1]) <= news_day) {
                                    FeedMessage message = new FeedMessage();
                                    message.setAuthor(author);
                                    message.setDescription(description);
                                    message.setData(pubdate);
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
            return feed;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }


    public Feed getFeed()
    {
        try {
            mFeed = get();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return mFeed;
    }

    @Override
    protected void onPostExecute(Feed result) {
        super.onPostExecute(result);
        // After completing http call
        // will close this activity and lauch main activity
        if(mStartMainActivity) {
            Intent i = new Intent(mContext, NewsFeedsBar.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(i);
        }
    }

}
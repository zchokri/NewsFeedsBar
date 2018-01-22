package czdev.newsfeedsbar;

/**
 * Created by Eskan on 18/01/2018.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.sun.org.apache.xerces.internal.parsers.XMLParser;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Array;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import static czdev.newsfeedsbar.SettingsActivity.TAG_LOG;

/**
 * Async Task to make http call
 */
public class RetrieveFeedTask extends AsyncTask< String, String, Feed> {

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
    public Context mContext = null;
    public String mUrls ="";
    private static Feed mFeed;
    public static final String FEED_PREFS_NAME = "FEED_PREFS";
    SharedPreferences mPrefs;
    SharedPreferences defaultSharedPreferences;
    public int mLanguageId = 0;
    Boolean mStartMainActivity = false;


    public RetrieveFeedTask(Context ctx,boolean startMainActivity)  {
        this.mContext = ctx;
        this.mStartMainActivity = startMainActivity;
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public void readUrls() {
        mLanguageId = Integer.parseInt(defaultSharedPreferences.getString("news_bar_lang","0"));
        Set<String> ressources = defaultSharedPreferences.getStringSet("news_bar_resources", null );
        List<String> urls = UrlsParser.getMyurls(mContext, mLanguageId, ressources);

    if(urls != null)
        execute(urls.toArray(new String[urls.size()]));
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

    protected Feed doInBackground(String... urls) {

        try {
            Feed feed = null;
            boolean isFeedHeader = true;
            Log.d(TAG_LOG, "urls " + urls );
            for (int i = 0; i < urls.length; i++) {
                Log.d(TAG_LOG, "url " + i + "   " + urls[i] );
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
                    Log.d(TAG_LOG, "Setup a new eventReader  " + i + "   " + urls[i]);
                    InputStream in = urlCon.getInputStream();
                    XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
                    // read the XML document
                    Log.d(TAG_LOG, "read the XML document  " + i + "   " +  urls[i]);

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


    public Feed getFeed()
    {
        try {
            mFeed = get();
            /*if(mFeed == null) {
                Gson gson = new Gson();
                String json = mPrefs.getString("SerializableObject", "");
                mFeed = gson.fromJson(json, Feed.class);
            }*/

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
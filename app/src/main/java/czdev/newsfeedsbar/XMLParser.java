package czdev.newsfeedsbar;

import android.provider.DocumentsContract;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;

/**
 * Created by czagroub on 31/01/2018.
 */

public class XMLParser extends Observable {

    private ArrayList<Feed> mFeeds;
    private Feed feedMessage;

    public XMLParser() {
        mFeeds = new ArrayList<>();
        feedMessage = new Feed();
    }

    public void parseXML(String xml) throws XmlPullParserException, IOException {

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

        factory.setNamespaceAware(false);
        XmlPullParser xmlPullParser = factory.newPullParser();

        xmlPullParser.setInput(new StringReader(xml));
        boolean insideItem = false;
        int eventType = xmlPullParser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {

                if (xmlPullParser.getName().equalsIgnoreCase("item")) {

                    insideItem = true;

                } else if (xmlPullParser.getName().equalsIgnoreCase("title")) {

                    if (insideItem) {
                        String title = xmlPullParser.nextText();
                        feedMessage.setTitle(title);
                    }

                } else if (xmlPullParser.getName().equalsIgnoreCase("link")) {

                    if (insideItem) {
                        String link = xmlPullParser.nextText();
                        feedMessage.setLink(link);
                    }

                } else if (xmlPullParser.getName().equalsIgnoreCase("dc:creator")) {

                    if (insideItem) {
                        String author = xmlPullParser.nextText();
                        feedMessage.setAuthor(author);
                    }

                } else if (xmlPullParser.getName().equalsIgnoreCase("category")) {

                    if (insideItem) {
                        String category = xmlPullParser.nextText();
                        feedMessage.addCategory(category);
                    }

                } else if (xmlPullParser.getName().equalsIgnoreCase("description")) {

                    if (insideItem) {
                        String description = xmlPullParser.nextText();
                        feedMessage.setDescription(description);
                    }

                } else if (xmlPullParser.getName().equalsIgnoreCase("pubDate")) {
                    @SuppressWarnings("deprecation")
                    Date pubDate = new Date(xmlPullParser.nextText());
                    feedMessage.setPubDate(pubDate);
                }

            } else if (eventType == XmlPullParser.END_TAG && xmlPullParser.getName().equalsIgnoreCase("item")) {
                insideItem = false;
                mFeeds.add(feedMessage);
                feedMessage = new Feed();
            }
            eventType = xmlPullParser.next();
        }
        triggerObserver();
    }


    private void triggerObserver() {
        setChanged();
        notifyObservers(mFeeds);
    }
}
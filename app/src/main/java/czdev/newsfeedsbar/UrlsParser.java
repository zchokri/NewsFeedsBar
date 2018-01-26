package czdev.newsfeedsbar;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import static czdev.newsfeedsbar.Constants.*;

/**
 * Created by mlahmadi on 19/01/2018.
 */

public class UrlsParser extends AsyncTask< String, String, List<String>> {


    private Context mContext;
    private int itemLang;
    private Set<String> resources;

    public UrlsParser(Context ctx, int itemLang, Set<String> resources)  {
        this.mContext = ctx;
        this.itemLang = itemLang;
        this.resources = resources;
    }
    private static String getLink(XMLEvent event, XMLEventReader eventReader)
            throws XMLStreamException {
        String result = "";
        event = eventReader.nextEvent();
        if (event instanceof Characters) {
            result = event.asCharacters().getData();
        }
        return result;
    }

    private static List<String> getResourcesNames(Set<String> resources)
    {
        List<String> resourcesNames = new ArrayList<String>();
        if (resources.contains("0"))
            resourcesNames.add("CNN");
        if (resources.contains("1"))
            resourcesNames.add("AlJazzeera");
        if (resources.contains("2"))
            resourcesNames.add("BBC");
        if (resources.contains("3"))
            resourcesNames.add("France24");

        return resourcesNames;
    }

    private static String getLanguage(int itemLang)
    {
        String language = "Arabic";
        if (itemLang == 1 )
            language = "Frensh";
        else if (itemLang == 2 )
            language = "English";

        return language;
    }


    @Override
    protected List<String> doInBackground(String... strings) {

        List<String> urls = new ArrayList<String>();
        String language = getLanguage(itemLang);
        List<String> resourcesNames = new ArrayList<String>();
        if (resources != null)
            resourcesNames = getResourcesNames(resources);

        try
        {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(mContext.getResources().openRawResource(R.raw.links));

            // Setup a new eventReader
            XMLEventReader eventReader = inputFactory.createXMLEventReader(streamReader);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    String localPart = event.asStartElement().getName().getLocalPart();
                    if (localPart.toLowerCase().contains(language.toLowerCase())) {
                        for(String resource:resourcesNames) {
                            if (localPart.toLowerCase().contains(resource.toLowerCase()))
                            {
                                urls.add( getLink(event, eventReader));
                            }
                        }
                        if (resourcesNames.size() == 0)
                        {
                            urls.add( getLink(event, eventReader));
                        }

                        event = eventReader.nextEvent();
                    }
                } else if (event.isEndElement()) {
                    if (event.asEndElement().getName().getLocalPart().equals("links")) {
                        return urls;
                    }
                }
            }

            return urls;
        }
        catch (XMLStreamException ex)
        {
            Log.d(TAG_LOG, "catch..getMyurls function");
        }
        return null;
    }
}

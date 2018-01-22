package czdev.newsfeedsbar;

import android.content.Context;
import android.util.Log;

import com.sun.org.apache.xalan.internal.xsltc.compiler.Parser;
import com.sun.org.apache.xerces.internal.parsers.XMLParser;

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

import static czdev.newsfeedsbar.SettingsActivity.TAG_LOG;

/**
 * Created by mlahmadi on 19/01/2018.
 */

public class UrlsParser {

    private static String getLink(XMLEvent event, XMLEventReader eventReader)
            throws XMLStreamException {
        String result = "";
        event = eventReader.nextEvent();
        if (event instanceof Characters) {
            result = event.asCharacters().getData();
        }
        return result;
    }

    private static List<String> getRessourcesNames(Set<String> ressources)
    {
        List<String> ressourcesNames = new ArrayList<String>();
        if (ressources.contains("0"))
            ressourcesNames.add("CNN");
        if (ressources.contains("1"))
            ressourcesNames.add("AlJazzeera");
        if (ressources.contains("2"))
            ressourcesNames.add("BBC");
        if (ressources.contains("3"))
            ressourcesNames.add("France24");

        return ressourcesNames;
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


    public static List<String> getMyurls(Context context, int itemLang, Set<String> ressources)
    {
        List<String> urls = new ArrayList<String>();
        String language = getLanguage(itemLang);
        List<String> ressourcesNames = new ArrayList<String>();
        if (ressources != null)
            ressourcesNames = getRessourcesNames(ressources);

        try
        {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(context.getResources().openRawResource(R.raw.links));

            // Setup a new eventReader
            XMLEventReader eventReader = inputFactory.createXMLEventReader(streamReader);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    String localPart = event.asStartElement().getName().getLocalPart();
                    if (localPart.toLowerCase().contains(language.toLowerCase())) {
                        for(String resource:ressourcesNames) {
                            if (localPart.toLowerCase().contains(resource.toLowerCase()))
                            {
                                urls.add( getLink(event, eventReader));
                            }
                        }
                        if (ressourcesNames.size() == 0)
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

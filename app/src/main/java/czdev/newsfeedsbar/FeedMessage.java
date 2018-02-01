package czdev.newsfeedsbar;

import java.io.Serializable;

/**
 * Created by ZAGROUBA CHOKRI on 02/01/2018.
 */

/*
 * Represents one RSS message
 */
public class FeedMessage implements Serializable {

    String title;
    String description;
    String link;
    String author;
    String date;

    public FeedMessage()  {
    }


    public FeedMessage(String title, String description, String link)  {
        this.title= title;
        this.description = description;
        this.link= link;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getData() {
        return date;
    }

    public void setData(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "FeedMessage [title=" + title + ", description=" + description
                + ", link=" + link + ", author=" + author + ", date=" + date
                + "]";
    }

}
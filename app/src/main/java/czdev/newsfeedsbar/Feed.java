package czdev.newsfeedsbar;

/**
 * Created by ZAGROUBA CHOKRI on 02/01/2018.
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
 * Stores an RSS feed
 */
public class Feed implements Serializable {

    private String title;
    private String author;
    private String link;
    private Date pubDate;
    private String description;
    private String content;
    private List<String> categories;


    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getLink() {
        return link;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public String getDescription() {
        return description;
    }

    public String getContent() {
        return content;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void addCategory(String category) {
        if (categories == null)
            categories = new ArrayList<>();
        categories.add(category);
    }

    @Override
    public String toString() {
        return "Article{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", link='" + link + '\'' +
                ", pubDate=" + pubDate +
                ", description='" + description + '\'' +
                ", content='" + content + '\'' +
                ", categories=" + categories +
                '}';
    }

}
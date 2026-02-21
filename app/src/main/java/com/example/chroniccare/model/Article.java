package com.example.chroniccare.model;

public class Article {
    public String title;
    public String url;
    public String imageUrl;
    public String readTime;
    public String[] conditions;

    public Article(String title, String url, String imageUrl, String readTime, String... conditions) {
        this.title = title;
        this.url = url;
        this.imageUrl = imageUrl;
        this.readTime = readTime;
        this.conditions = conditions;
    }
}

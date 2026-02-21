package com.example.chroniccare.utils;

import com.example.chroniccare.model.Article;
import java.util.ArrayList;
import java.util.List;

public class ArticleProvider {
    
    private static final List<Article> ALL_ARTICLES = new ArrayList<>();
    
    static {
        // Diabetes articles
        ALL_ARTICLES.add(new Article(
            "Managing Blood Sugar Levels",
            "https://www.healthline.com/health/diabetes/how-to-lower-blood-sugar",
            "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=400",
            "7 min read",
            "diabetes", "type 2 diabetes", "type 1 diabetes"
        ));
        
        ALL_ARTICLES.add(new Article(
            "Best Foods for Diabetes Control",
            "https://www.healthline.com/nutrition/16-best-foods-for-diabetics",
            "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400",
            "10 min read",
            "diabetes", "type 2 diabetes"
        ));
        
        // High BP articles
        ALL_ARTICLES.add(new Article(
            "How to Lower Blood Pressure Naturally",
            "https://www.healthline.com/nutrition/13-ways-to-lower-blood-pressure-naturally",
            "https://images.unsplash.com/photo-1505576399279-565b52d4ac71?w=400",
            "8 min read",
            "high bp", "hypertension", "blood pressure"
        ));
        
        ALL_ARTICLES.add(new Article(
            "DASH Diet for High Blood Pressure",
            "https://www.healthline.com/nutrition/dash-diet",
            "https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=400",
            "12 min read",
            "high bp", "hypertension"
        ));
        
        // Heart disease articles
        ALL_ARTICLES.add(new Article(
            "Heart-Healthy Diet and Lifestyle",
            "https://www.healthline.com/nutrition/heart-healthy-foods",
            "https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=400",
            "9 min read",
            "heart disease", "cardiac", "cholesterol"
        ));
        
        // Asthma articles
        ALL_ARTICLES.add(new Article(
            "Managing Asthma: Tips and Triggers",
            "https://www.healthline.com/health/asthma",
            "https://images.unsplash.com/photo-1584515933487-779824d29309?w=400",
            "6 min read",
            "asthma", "respiratory"
        ));
        
        // Thyroid articles
        ALL_ARTICLES.add(new Article(
            "Understanding Thyroid Health",
            "https://www.healthline.com/health/thyroid-disorders",
            "https://images.unsplash.com/photo-1559757175-5700dde675bc?w=400",
            "8 min read",
            "thyroid", "hypothyroid", "hyperthyroid"
        ));
        
        // Common wellness articles (shown to everyone)
        ALL_ARTICLES.add(new Article(
            "Latest Health News & Research",
            "https://www.healthline.com/health-news",
            "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=400",
            "5 min read",
            "common"
        ));
        
        ALL_ARTICLES.add(new Article(
            "How to Maintain a Healthy Lifestyle",
            "https://www.healthline.com/health/how-to-maintain-a-healthy-lifestyle",
            "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400",
            "8 min read",
            "common"
        ));
        
        ALL_ARTICLES.add(new Article(
            "27 Health and Nutrition Tips",
            "https://www.healthline.com/nutrition/27-health-and-nutrition-tips",
            "https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=400",
            "10 min read",
            "common"
        ));
        
        ALL_ARTICLES.add(new Article(
            "Longevity and Healthy Aging",
            "https://www.healthlinemedia.com/insights/content-that-defined-2025",
            "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400",
            "6 min read",
            "common"
        ));
        
        ALL_ARTICLES.add(new Article(
            "Wellness Trends Shaping 2026",
            "https://activewellness.com/20-wellness-trends-shaping-how-we-move-feel-in-2026/",
            "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400",
            "12 min read",
            "common"
        ));
    }
    
    public static List<Article> getPersonalizedArticles(String userConditions) {
        List<Article> personalizedArticles = new ArrayList<>();
        
        // Add condition-specific articles first
        if (userConditions != null && !userConditions.isEmpty()) {
            String conditionsLower = userConditions.toLowerCase();
            
            for (Article article : ALL_ARTICLES) {
                if (article.conditions != null) {
                    for (String condition : article.conditions) {
                        if (!condition.equals("common") && conditionsLower.contains(condition.toLowerCase())) {
                            personalizedArticles.add(article);
                            break;
                        }
                    }
                }
            }
        }
        
        // Add common wellness articles
        for (Article article : ALL_ARTICLES) {
            if (article.conditions != null) {
                for (String condition : article.conditions) {
                    if (condition.equals("common")) {
                        personalizedArticles.add(article);
                        break;
                    }
                }
            }
        }
        
        // Limit to 6 articles
        return personalizedArticles.size() > 6 
            ? personalizedArticles.subList(0, 6) 
            : personalizedArticles;
    }
}

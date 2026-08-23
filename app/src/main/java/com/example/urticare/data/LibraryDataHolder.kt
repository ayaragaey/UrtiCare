package com.example.urticare.data

import android.content.Context
import org.json.JSONObject

data class FAQItem(
    val question: String,
    val answer: String
)

data class LibraryTopic(
    val title: String,
    val summary: String,
    val keywords: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val relatedArticles: List<String> = emptyList(),
    val faq: List<FAQItem> = emptyList()
)

data class LibrarySection(
    val title: String,
    val description: String,
    val topics: List<LibraryTopic>
)

object LibraryDataHolder {
    data class ArticleData(
        val titles: Map<String, String>,
        val content: Map<String, String>
    )

    private var articlesMap: Map<String, ArticleData> = emptyMap()
    private var sectionsList: List<LibrarySection> = emptyList()
    private var isLoaded = false

    fun loadIfNeeded(context: Context) {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            try {
                val jsonString = context.assets.open("urticare_library.json").bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                
                // Parse articles
                val articlesJson = root.getJSONObject("articles")
                val tempArticlesMap = mutableMapOf<String, ArticleData>()
                val articleKeys = articlesJson.keys()
                while (articleKeys.hasNext()) {
                    val key = articleKeys.next()
                    val articleObj = articlesJson.getJSONObject(key)
                    
                    val titlesObj = articleObj.getJSONObject("titles")
                    val titlesMap = mutableMapOf<String, String>()
                    val titleKeys = titlesObj.keys()
                    while (titleKeys.hasNext()) {
                        val lang = titleKeys.next()
                        titlesMap[lang] = titlesObj.getString(lang)
                    }
                    
                    val contentObj = articleObj.getJSONObject("content")
                    val contentMap = mutableMapOf<String, String>()
                    val contentKeys = contentObj.keys()
                    while (contentKeys.hasNext()) {
                        val lang = contentKeys.next()
                        contentMap[lang] = contentObj.getString(lang)
                    }
                    
                    tempArticlesMap[key] = ArticleData(titlesMap, contentMap)
                }
                articlesMap = tempArticlesMap

                // Parse sections
                val sectionsArr = root.getJSONArray("sections")
                val tempSectionsList = mutableListOf<LibrarySection>()
                for (i in 0 until sectionsArr.length()) {
                    val sectionObj = sectionsArr.getJSONObject(i)
                    val sectionTitle = sectionObj.getString("title")
                    val sectionDesc = sectionObj.getString("description")
                    
                    val topicsArr = sectionObj.getJSONArray("topics")
                    val tempTopicsList = mutableListOf<LibraryTopic>()
                    for (j in 0 until topicsArr.length()) {
                        val topicObj = topicsArr.getJSONObject(j)
                        val topicTitle = topicObj.getString("title")
                        val topicSummary = topicObj.getString("summary")
                        
                        val keywordsArr = topicObj.optJSONArray("keywords")
                        val keywords = mutableListOf<String>()
                        if (keywordsArr != null) {
                            for (k in 0 until keywordsArr.length()) {
                                keywords.add(keywordsArr.getString(k))
                            }
                        }

                        val synonymsArr = topicObj.optJSONArray("synonyms")
                        val synonyms = mutableListOf<String>()
                        if (synonymsArr != null) {
                            for (k in 0 until synonymsArr.length()) {
                                synonyms.add(synonymsArr.getString(k))
                            }
                        }

                        val relatedArticlesArr = topicObj.optJSONArray("relatedArticles")
                        val relatedArticles = mutableListOf<String>()
                        if (relatedArticlesArr != null) {
                            for (k in 0 until relatedArticlesArr.length()) {
                                relatedArticles.add(relatedArticlesArr.getString(k))
                            }
                        }

                        val faqArr = topicObj.optJSONArray("faq")
                        val faq = mutableListOf<FAQItem>()
                        if (faqArr != null) {
                            for (k in 0 until faqArr.length()) {
                                val faqObj = faqArr.getJSONObject(k)
                                val q = faqObj.optString("question", "")
                                val a = faqObj.optString("answer", "")
                                faq.add(FAQItem(q, a))
                            }
                        }

                        tempTopicsList.add(LibraryTopic(topicTitle, topicSummary, keywords, synonyms, relatedArticles, faq))
                    }
                    
                    tempSectionsList.add(LibrarySection(sectionTitle, sectionDesc, tempTopicsList))
                }
                sectionsList = tempSectionsList
                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getArticles(context: Context): Map<String, ArticleData> {
        loadIfNeeded(context)
        return articlesMap
    }

    fun getSections(context: Context): List<LibrarySection> {
        loadIfNeeded(context)
        return sectionsList
    }
}

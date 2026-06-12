package com.nyora.hasan72341.suggestions.data

import com.nyora.hasan72341.tachiyomi.network.await
import com.nyora.hasan72341.core.network.BaseHttpClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AnilistResponse(val data: AnilistData)

@Serializable
data class AnilistMultiResponse(val data: AnilistMultiRailsResponse)

@Serializable
data class AnilistMultiRailsResponse(
    val trending: AnilistPage,
    val popular: AnilistPage,
    val topRated: AnilistPage,
    val seasonal: AnilistPage,
    val newReleases: AnilistPage,
    val action: AnilistPage,
    val romance: AnilistPage,
)

@Serializable
data class AnilistData(val Page: AnilistPage)

@Serializable
data class AnilistPage(val media: List<AnilistMedia>)

@Serializable
data class AnilistMedia(
    val id: Int,
    val title: AnilistTitle,
    val coverImage: AnilistCoverImage,
    val description: String? = null,
    val averageScore: Int? = null,
    val genres: List<String> = emptyList()
)

@Serializable
data class AnilistTitle(val romaji: String? = null, val english: String? = null, val native: String? = null) {
    fun preferred(): String = english ?: romaji ?: native ?: "Unknown"
}

@Serializable
data class AnilistCoverImage(val extraLarge: String? = null, val large: String? = null) {
    fun preferred(): String = extraLarge ?: large ?: ""
}

@Singleton
class AnilistRepository @Inject constructor(
    @BaseHttpClient private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val anilistUrl = "https://graphql.anilist.co"

    suspend fun getTrendingManga(limit: Int = 20): List<AnilistMedia> {
        val query = """
            query {
              Page(page: 1, perPage: $limit) {
                media(type: MANGA, sort: TRENDING_DESC) {
                  id
                  title { romaji english native }
                  coverImage { extraLarge large }
                  description
                  averageScore
                  genres
                }
              }
            }
        """.trimIndent()
        return executeQuery(query)
    }

    suspend fun getPopularManga(limit: Int = 20): List<AnilistMedia> {
        val query = """
            query {
              Page(page: 1, perPage: $limit) {
                media(type: MANGA, sort: POPULARITY_DESC) {
                  id
                  title { romaji english native }
                  coverImage { extraLarge large }
                  description
                  averageScore
                  genres
                }
              }
            }
        """.trimIndent()
        return executeQuery(query)
    }

    suspend fun getMultiRails(limit: Int = 20): AnilistMultiRailsResponse? {
        val query = """
            query {
              trending: Page(page: 1, perPage: $limit) {
                media(type: MANGA, sort: TRENDING_DESC) {
                  id title { romaji english native } coverImage { extraLarge large } genres
                }
              }
              popular: Page(page: 1, perPage: $limit) {
                media(type: MANGA, sort: POPULARITY_DESC) {
                  id title { romaji english native } coverImage { extraLarge large } genres
                }
              }
              topRated: Page(page: 1, perPage: $limit) {
                media(type: MANGA, sort: SCORE_DESC) {
                  id title { romaji english native } coverImage { extraLarge large } genres
                }
              }
              seasonal: Page(page: 1, perPage: $limit) {
                media(type: MANGA, sort: POPULARITY_DESC, season: SUMMER, seasonYear: 2026) {
                  id title { romaji english native } coverImage { extraLarge large } genres
                }
              }
              newReleases: Page(page: 1, perPage: $limit) {
                media(type: MANGA, sort: START_DATE_DESC) {
                  id title { romaji english native } coverImage { extraLarge large } genres
                }
              }
              action: Page(page: 1, perPage: $limit) {
                media(type: MANGA, sort: POPULARITY_DESC, genre: "Action") {
                  id title { romaji english native } coverImage { extraLarge large } genres
                }
              }
              romance: Page(page: 1, perPage: $limit) {
                media(type: MANGA, sort: POPULARITY_DESC, genre: "Romance") {
                  id title { romaji english native } coverImage { extraLarge large } genres
                }
              }
            }
        """.trimIndent()
        
        val requestBody = "{\"query\":\"${query.replace("\n", " ").replace("\"", "\\\"")}\"}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(anilistUrl)
            .post(requestBody)
            .build()

        return okHttpClient.newCall(request).await().use { response ->
            if (!response.isSuccessful) return@use null
            val body = response.body?.string() ?: return@use null
            json.decodeFromString<AnilistMultiResponse>(body).data
        }
    }

    private suspend fun executeQuery(query: String): List<AnilistMedia> {
        val requestBody = "{\"query\":\"${query.replace("\n", " ").replace("\"", "\\\"")}\"}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(anilistUrl)
            .post(requestBody)
            .build()

        return okHttpClient.newCall(request).await().use { response ->
            if (!response.isSuccessful) return@use emptyList()
            val body = response.body?.string() ?: return@use emptyList()
            json.decodeFromString<AnilistResponse>(body).data.Page.media
        }
    }
}

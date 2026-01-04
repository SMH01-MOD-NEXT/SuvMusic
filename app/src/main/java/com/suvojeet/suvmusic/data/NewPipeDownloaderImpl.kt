package com.suvojeet.suvmusic.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NPRequest
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

/**
 * Custom Downloader implementation for NewPipe Extractor using OkHttp.
 * Includes session cookies for authenticated requests.
 */
class NewPipeDownloaderImpl(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager
) : Downloader() {
    
    companion object {
        private const val USER_AGENT = 
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    
    override fun execute(request: NPRequest): Response {
        val requestBuilder = Request.Builder()
            .url(request.url())
            .method(request.httpMethod(), null)
            .header("User-Agent", USER_AGENT)
        
        // Add headers from the request
        request.headers().forEach { (key, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }
        
        // Add cookies if available for YouTube Music requests
        if (request.url().contains("youtube.com") || request.url().contains("googlevideo.com")) {
            sessionManager.getCookies()?.let { cookies ->
                requestBuilder.addHeader("Cookie", cookies)
            }
        }
        
        // Handle request body for POST
        if (request.httpMethod() == "POST" && request.dataToSend() != null) {
            val body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json; charset=utf-8"),
                request.dataToSend()
            )
            requestBuilder.post(body)
        }
        
        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        
        if (response.code == 429) {
            throw ReCaptchaException("Rate limited", request.url())
        }
        
        val responseBody = response.body?.string() ?: ""
        
        // Convert headers to the format NewPipe expects
        val responseHeaders = mutableMapOf<String, MutableList<String>>()
        response.headers.forEach { pair ->
            responseHeaders.getOrPut(pair.first) { mutableListOf() }.add(pair.second)
        }
        
        return Response(
            response.code,
            response.message,
            responseHeaders,
            responseBody,
            request.url()
        )
    }
}

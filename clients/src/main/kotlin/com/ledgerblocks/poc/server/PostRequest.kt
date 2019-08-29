package com.ledgerblocks.poc.server

import java.net.HttpURLConnection
import java.net.URL

class PostRequest {

    fun tokenRequest(uuid: String, type: String): String{

        var urlString: String? = null
        var token: String? = null
        if (type == "b") {
            urlString = "http://localhost:50005/api/lb/token?uuid=$uuid"
        }
        if (type == "m") {
            urlString = "http://localhost:50006/api/lb/token?uuid=$uuid"
        }
        val url = URL(urlString)

        with (url.openConnection() as HttpURLConnection){
            requestMethod = "POST"
            println("Send POST Request: $url, Response Code: $responseCode")
            inputStream.bufferedReader().use{
                it.lines().forEach{ line ->
                    token = line
                }
            }
        }
        return token.toString()
    }

}
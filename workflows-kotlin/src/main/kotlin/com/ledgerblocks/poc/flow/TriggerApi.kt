package com.ledgerblocks.poc.flow

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*

//class TriggerApi {

    fun requestDeductLoan(bUUID1: UUID, amtToPay1: Int) {

        //var reqParam = URLEncoder.encode("bUUID1", "UTF-8") + "=" + URLEncoder.encode(bUUID1.toString(), "UTF-8")
        // reqParam += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(amtToPay1.toString(), "UTF-8")
        //val mURL = URL("http://localhost:50005/api/lb/deductLoan?bUUID=$bUUID1&amtToPay=$amtToPay1")

        var urlString = "http://localhost:50005/api/lb/deductLoan?bUUID=$bUUID1&amtToPay=$amtToPay1"
        var token: String? = null
        val url = URL(urlString)

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            println("Send POST Request: $url, Response Code: $responseCode")
            inputStream.bufferedReader().use {
                it.lines().forEach { line ->
                    token = line
                }
            }
        }
    }


    fun requestAddTokens(mUUID1: UUID, amtToPay1: Int) {

        //var reqParam = URLEncoder.encode("bUUID1", "UTF-8") + "=" + URLEncoder.encode(mUUID1.toString(), "UTF-8")
       // reqParam += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(amtToPay1.toString(), "UTF-8")

        var urlString = "http://localhost:50006/api/lb/addTokens?mUUID=$mUUID1&amtToPay=$amtToPay1"
        var token: String? = null
        val url = URL(urlString)
       // val mURL = URL("http://localhost:50006/api/lb/addTokens?mUUID=$mUUID1&amtToPay=$amtToPay1")

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            println("Send POST Request: $url, Response Code: $responseCode")
            inputStream.bufferedReader().use {
                it.lines().forEach { line ->
                    token = line
                }
            }
        }
    }
//}
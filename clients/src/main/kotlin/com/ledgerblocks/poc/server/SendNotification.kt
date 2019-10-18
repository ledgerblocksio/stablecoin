package com.ledgerblocks.poc.server

import org.json.JSONArray
import org.json.JSONObject

import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

object SendNotification {
    private val BASE_URL = "https://fcm.googleapis.com/fcm/send"
    private val TITLE = "LedgerBlocks Loan"
    private val SERVER_KEY = "AAAAqAkKKXM:APA91bGAv9QpcNxkDDrYj4HK_XHm0dm7Dv77GD6UVG4AcD-K47VeC9jfb9m3Mkm15ByakPu0vVAwhe2NDwArm0fRNjUxv4xivgdJpyihvgWQGR8vkqjjSsjSwdINg8C2y-4ri1g6TpQf"

    /**
     * Create HttpURLConnection that can be used for both retrieving and publishing.*
     * @return Base HttpURLConnection.
     * @throws IOException
     */
    val connection: HttpURLConnection
        @Throws(IOException::class)
        get() {
            val url = URL(BASE_URL)
            val httpURLConnection = url.openConnection() as HttpURLConnection
            httpURLConnection.setRequestProperty("Authorization", "key=$SERVER_KEY")
            httpURLConnection.setRequestProperty("Content-Type", "application/json; UTF-8")
            return httpURLConnection
        }

    @Throws(IOException::class)
    fun postMessage(fcmToken: String, message: String) {

        val notificationMessage = buildNotificationMessage(fcmToken, message)
        println("FCM request body for message using common notification object:")
        sendMessage(notificationMessage)
    }

    /**
     * Construct the body of a notification message request.
     *
     * @return JSON of notification message.
     */
    private fun buildNotificationMessage(fcmToken: String, message: String): JSONObject {

        var regId: JSONArray? = null
        var objData: JSONObject? = null
        var data: JSONObject? = null
        var notif: JSONObject? = null

        regId = JSONArray()
        regId.put(fcmToken)

        data = JSONObject()
        data.put("message", message)
        notif = JSONObject()
        notif.put("title", TITLE)
        notif.put("text", message)

        objData = JSONObject()
        objData.put("registration_ids", regId)
        objData.put("data", data)
        objData.put("notification", notif)

        return objData
    }

    /**
     * Send request to FCM message using HTTP.
     *
     * @param fcmMessage Body of the HTTP request.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun sendMessage(fcmMessage: JSONObject) {
        val connection = connection
        connection.doOutput = true
        val outputStream = DataOutputStream(connection.outputStream)
        outputStream.writeBytes(fcmMessage.toString())
        outputStream.flush()
        outputStream.close()

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val response = inputstreamToString(connection.inputStream)
            println("Message sent to Firebase for delivery, response: $response")
        } else {
            println("Unable to send message to Firebase:")
            val response = inputstreamToString(connection.errorStream)
            println(response)
        }
    }

    /**
     * Read contents of InputStream into String.
     *
     * @param inputStream InputStream to read.
     * @return String containing contents of InputStream.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun inputstreamToString(inputStream: InputStream): String {
        val stringBuilder = StringBuilder()
        val scanner = Scanner(inputStream)
        while (scanner.hasNext()) {
            stringBuilder.append(scanner.nextLine())
        }
        return stringBuilder.toString()
    }
}
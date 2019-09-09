package com.ledgerblocks.poc.server

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonArray

import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.Arrays
import java.util.Properties
import java.util.Scanner

object SendNotification {


    private val BASE_URL = "https://fcm.googleapis.com/fcm/send"
    private val TITLE = "LedgerBlocks"
    private val SERVER_KEY = "AAAAqAkKKXM:APA91bGAv9QpcNxkDDrYj4HK_XHm0dm7Dv77GD6UVG4AcD-K47VeC9jfb9m3Mkm15ByakPu0vVAwhe2NDwArm0fRNjUxv4xivgdJpyihvgWQGR8vkqjjSsjSwdINg8C2y-4ri1g6TpQf"

    /**
     * Create HttpURLConnection that can be used for both retrieving and publishing.
     *
     * @return Base HttpURLConnection.
     * @throws IOException
     */
    private// [START use_access_token]
    // [END use_access_token]
    val connection: HttpURLConnection
        @Throws(IOException::class)
        get() {
            val url = URL(BASE_URL)
            val httpURLConnection = url.openConnection() as HttpURLConnection

            val currentDirectory = System.getProperty("user.home")
            val fis = FileInputStream("$currentDirectory/access/fcm-loan.properties")
            println("Current Dir: ${currentDirectory}")
            val properties = Properties()
            properties.load(fis)
            val server_key = properties.getProperty("SERVER_KEY")
            fis.close()


            httpURLConnection.setRequestProperty("Authorization", "key=$server_key")
            httpURLConnection.setRequestProperty("Content-Type", "application/json; UTF-8")
            return httpURLConnection
        }

    @Throws(IOException::class)
    fun Message(fcmToken: String, message: String): String {
        //String fcmToken = "";
        //String message = "LedgerBlocks - Test message";

        val notificationMessage = buildNotificationMessage(fcmToken, message)
        println("FCM request body for message using common notification object:")
        prettyPrint(notificationMessage)
        //sendMessage(notificationMessage);
        //"Notified Successfully";

        return "Sent successfully"
    }

    /**
     * Construct the body of a notification message request.
     *
     * @return JSON of notification message.
     */
    private fun buildNotificationMessage(fcmToken: String, message: String): JsonObject {

        var regId: JsonArray? = null
        var objData: JsonObject? = null
        var data: JsonObject? = null
        var notif: JsonObject? = null

        regId = JsonArray()
        regId.add(fcmToken)

        data = JsonObject()
        data.addProperty("message", message)
        notif = JsonObject()
        notif.addProperty("title", TITLE)
        notif.addProperty("text", message)

        objData = JsonObject()
        objData.add("registartion_ids", regId)
        objData.add("data", data)
        objData.add("notification", notif)

        return objData
    }

    /**
     * Pretty print a JsonObject.
     *
     * @param jsonObject JsonObject to pretty print.
     */
    private fun prettyPrint(jsonObject: JsonObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        println(gson.toJson(jsonObject) + "\n")
    }

    /**
     * Send request to FCM message using HTTP.
     *
     * @param fcmMessage Body of the HTTP request.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun sendMessage(fcmMessage: JsonObject) {
        val connection = connection
        connection.doOutput = true
        val outputStream = DataOutputStream(connection.outputStream)
        outputStream.writeBytes(fcmMessage.toString())
        outputStream.flush()
        outputStream.close()

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val response = inputstreamToString(connection.inputStream)
            println("Message sent to Firebase for delivery, response:")
            println(response)
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
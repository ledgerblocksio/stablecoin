package com.ledgerblocks.poc.flow;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class PostRequest {


    public static void requestDeductLoan( UUID bUUID1, int amtToPay1) throws IOException {
        URL urlForGetRequest = new URL("https//localhost:50005/api/lb/deductLoan?bUUID=${bUUID1}&amtToPay=amtToPay1");
        String readLine = null;
        HttpURLConnection conection = (HttpURLConnection) urlForGetRequest.openConnection();
        conection.setRequestMethod("POST");
       // conection.setRequestProperty("userId", "a1bcdef"); // set userId its a sample here
        int responseCode = conection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conection.getInputStream()));
            StringBuffer response = new StringBuffer();
            while ((readLine = in .readLine()) != null) {
                response.append(readLine);
            } in .close();
            // print result
            System.out.println("JSON String Result " + response.toString());
            //GetAndPost.POSTRequest(response.toString());
        } else {
            System.out.println("POST NOT WORKED");
        }
    }


    public static void
    requestAddTokens(UUID mUUID1, int amtToPay1) throws IOException {
        URL urlForGetRequest = new URL("https//localhost:50006/api/lb/addTokens?mUUID=mUUID1&amtToPay=amtToPay1");
        String readLine = null;
        HttpURLConnection conection = (HttpURLConnection) urlForGetRequest.openConnection();
        conection.setRequestMethod("POST");
        // conection.setRequestProperty("userId", "a1bcdef"); // set userId its a sample here
        int responseCode = conection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conection.getInputStream()));
            StringBuffer response = new StringBuffer();
            while ((readLine = in .readLine()) != null) {
                response.append(readLine);
            } in .close();
            // print result
            System.out.println("JSON String Result " + response.toString());
            //GetAndPost.POSTRequest(response.toString());
        } else {
            System.out.println("POST NOT WORKED");


        }


    }

}

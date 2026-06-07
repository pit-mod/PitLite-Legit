package com.pitlite.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class EventUtils {

    public static String fetchEvents() {
        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL("https://raw.githubusercontent.com/BrookeAFK/brookeafk-api/main/events.js");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");
            InputStream responseStream = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(responseStream));

            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
            rd.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return response.toString();
    }
}

package com.archos.mediascraper;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class TVMazeAPI {
    private static final String TVMAZE_API_URL = "https://api.tvmaze.com/lookup/shows?imdb=";
    private static final Logger LOGGER = Logger.getLogger(TVMazeAPI.class.getName());

    public static int fetchRuntimeFromTVMaze(String imdbId) {
        if (imdbId == null || imdbId.isEmpty()) {
            LOGGER.warning("IMDb ID is null or empty.");
            return 0;
        }

        try {
            URL url = new URL(TVMAZE_API_URL + imdbId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) { // Success
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse JSON response
                JSONObject json = new JSONObject(response.toString());
                if (json.has("averageRuntime") && !json.isNull("averageRuntime")) {
                    return json.getInt("averageRuntime"); // Return the averageRuntime
                } else {
                    LOGGER.warning("averageRuntime not found in response for IMDb ID: " + imdbId);
                }
            } else {
                LOGGER.warning("TVMaze API responded with code: " + responseCode);
            }
        } catch (Exception e) {
            LOGGER.severe("Error fetching runtime from TVMaze: " + e.getMessage());
        }

        return 0; // Return 0 if averageRuntime is not found
    }
}

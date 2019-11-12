package AirplaneLocator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Selective characteristics of a plane
 */
public class Plane {
    /**
     * Nested class to hold data read from API JSON format
     */
    private static class JsonPlaneData {

        private int time;
        private Object[][] states;

        public void setTime(int time) {
            this.time = time;
        }

        public int getTime() {
            return this.time;
        }

        Object[][] getStates() {
            return this.states;
        }

        public void setStates(Object[][] states) {
            this.states = states;
        }

        @Override
        public String toString() {
            return "TIME: " + this.time + "\n" + "STATES: " + Arrays.toString(this.states);
        }
    }

    /**
     * Creates a plane!
     * @param latitude latitude of plane in degrees
     * @param longitude longitude of plane in degrees
     * @param geometricAltitude geometric altitude of plane in meters
     * @param originCountry origin country of plane
     * @param ICAO24ID ID of plane
     * @param callSign call sign of plane
     * @param geodesicDistance Euclidean distance of plane compared to a set of coordinates
     * @param grounded True if grounded
     * @throws MalformedURLException Should never occur.
     */
    public Plane(Double latitude, Double longitude, Double geometricAltitude, String originCountry, String ICAO24ID, String callSign, Double geodesicDistance, boolean grounded) throws MalformedURLException {
        this.latitude = latitude;
        this.longitude = longitude;
        this.geometricAltitude = geometricAltitude;
        this.originCountry = originCountry;
        this.ICAO24ID = ICAO24ID;
        this.callSign = callSign;
        this.EuclideanDistance = geodesicDistance;
        this.grounded = grounded;
    }

    /**
     * For purposes of creating a plane with no relation to another pair of coordinates (no geodesic distance).
     * @param latitude latitude of plane in degrees
     * @param longitude longitude of plane in degrees
     * @param geometricAltitude geometric altitude of plane in meters
     * @param originCountry origin country of plane
     * @param ICAO24ID ID of plane
     * @param callSign call sign of plane
     * @param grounded True if grounded
     * @throws MalformedURLException
     */
    public Plane(Double latitude, Double longitude, Double geometricAltitude, String originCountry, String ICAO24ID, String callSign, boolean grounded) throws MalformedURLException {
        this.latitude = latitude;
        this.longitude = longitude;
        this.geometricAltitude = geometricAltitude;
        this.originCountry = originCountry;
        this.ICAO24ID = ICAO24ID;
        this.callSign = callSign;
        this.grounded = grounded;
    }

    //Declarations
    private String callSign;
    private Double latitude;
    private Double longitude;
    private Double geometricAltitude;
    private String originCountry;
    private String ICAO24ID;
    private Double EuclideanDistance;
    private boolean grounded;

    /**
     * Provides all values stored in plane in a nice output. Does not print geodesic distance if plane was not constructed
     * with one.
     * @return String representation of Plane
     */
    @Override
    public String toString() {
        String string = "";
        string += "Callsign: " + this.callSign + "\n";
        string += "Latitude: " + this.latitude + " Longitude: " + this.longitude + "\n";
        string += "Geometric Altitude: " + this.geometricAltitude + "m\n";
        string += "Origin Country: " + this.originCountry + "\n";
        string += "ICAO24ID: " + this.ICAO24ID;
        return string;
    }

    /**
     * Gets closest plane to a pair of coordinates.
     * @param latitude latitude being compared to
     * @param longitude longitude being compared to
     * @return Closest plane
     * @throws IOException Should never happen
     */
    static Plane getClosestPlane(double latitude, double longitude) throws IOException {
        JsonPlaneData jsonPlaneData = getJsonPlaneData();
        return getClosestPlane(getPlanes(latitude, longitude, jsonPlaneData));
    }

    /**
     * Fills an arraylist with all the planes
     * @param userLatitude latitude being compared to
     * @param userLongitude longitude being compared to
     * @param jsonPlane JSON data
     * @return ArrayList of plane objects
     * @throws MalformedURLException Should never happen
     */
    private static ArrayList<Plane> getPlanes(double userLatitude, double userLongitude, JsonPlaneData jsonPlane) throws MalformedURLException {
        ArrayList<Plane> planes = new ArrayList<>();
        for (int i = 0; i < jsonPlane.getStates().length; i++) {
            Object[][] states = jsonPlane.getStates();
            Plane plane;
            try {
                double longitude = (double) states[i][5];
                double latitude = (double) states[i][6];
                String callSign = (String) states[i][1];
                String originCountry = (String) states[i][2];
                Double geometricAltitude = Double.parseDouble(states[i][13].toString());
                Double geodesicDistance = euclideanDistance(userLatitude, userLongitude, latitude, longitude, geometricAltitude);
                String ICAO24ID = (String) states[i][0];
                plane = new Plane(latitude, longitude, geometricAltitude, originCountry, ICAO24ID, callSign,  geodesicDistance, (boolean) states[i][8]);
            }
            catch (RuntimeException exc) {
                plane = new Plane(-1.0, -1.0, 0.0, "Unavailable", "Unavailable", "Unavailable", Double.MAX_VALUE, true);
            }
            planes.add(plane);
        }
        return planes;
    }

    /**
     * Helped to find closest plane based on euclidean distance.
     * @param planes list of all planes
     * @return Closest plane
     */
    private static Plane getClosestPlane(ArrayList<Plane> planes) {
        double min = planes.get(0).EuclideanDistance;
        int minIndex = 0;
        for (int i = 0; i < planes.size(); i++) {
            if (!planes.get(i).grounded) {
                double d = planes.get(i).EuclideanDistance;
                if (d < min) {
                    min = d;
                    minIndex = i;
                }
            }
        }
        return planes.get(minIndex);
    }

    /**
     * Accesses OpenSky API and retrieves plane data
     * @return Object containing the deserialized data
     * @throws IOException Should never happen
     */
    private static JsonPlaneData getJsonPlaneData() throws IOException {
        URL openSkyAPI = new URL("https://opensky-network.org/api/states/all");
        HttpURLConnection API = (HttpURLConnection) openSkyAPI.openConnection();
        API.setRequestMethod("GET");
        API.setDoOutput(true);
        API.setReadTimeout(55*1000);
        try {
            API.connect();
        }
        catch (UnknownHostException uhe) {
            System.err.println("Couldn't open API");
            System.exit(0);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(API.getInputStream()));
        String jsonString = reader.readLine();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, JsonPlaneData.class);
    }

    //Computes euclidean distance between two pairs of coordinates
    private static double euclideanDistance(double origLat, double origLong, double destLat, double destLong, double altitude) {
        return Math.sqrt(Math.pow((origLat - destLat), 2) + Math.pow((origLong - destLong), 2) + Math.pow((altitude), 2));
    }

}

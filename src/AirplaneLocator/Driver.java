package AirplaneLocator;

import java.io.IOException;
import java.util.Scanner;

public class Driver {

    //TODO: Add support for command line arguments
    public static void main(String[] args) throws IOException {
        double latitude;
        double longitude;
        char latCardinal;
        char longCardinal;
        if (args.length < 1) {
            System.out.println("Enter a latitude and longitude (degrees, cardinal directions optional, ctrl-d to end input");
            Scanner scnr = new Scanner(System.in);
            latitude = scnr.nextDouble();
            if (scnr.hasNext("[nNsS]")) {
                latCardinal = scnr.next().charAt(0);
                if (Character.toLowerCase(latCardinal) == 's') {
                    //TODO: fix this
                    latitude = -latitude;
                }
            }
            longitude = scnr.nextDouble();
            if (scnr.hasNext("[eEwW]")) {
                longCardinal = scnr.next().charAt(0);
                if (Character.toLowerCase(longCardinal) == 'w') {
                    longitude = -longitude;
                }
            }
            Plane airplane;
            airplane = Plane.getClosestPlane(latitude, longitude);
            System.out.println(airplane.toString());
        }
    }
}

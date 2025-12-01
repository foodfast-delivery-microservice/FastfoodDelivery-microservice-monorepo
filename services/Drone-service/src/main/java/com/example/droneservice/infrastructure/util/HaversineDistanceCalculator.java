package com.example.droneservice.infrastructure.util;

/**
 * Haversine Distance Calculator
 * Calculates the great-circle distance between two GPS coordinates
 * and simulates drone movement along the path.
 */
public class HaversineDistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     * 
     * @param lat1 Latitude of point 1 (degrees)
     * @param lon1 Longitude of point 1 (degrees)
     * @param lat2 Latitude of point 2 (degrees)
     * @param lon2 Longitude of point 2 (degrees)
     * @return Distance in kilometers
     */
    public static double calculate(double lat1, double lon1, double lat2, double lon2) {
        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formula
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculate the next GPS position when moving from current to target
     * at a given speed for a given time interval.
     * 
     * @param current         Current GPS position
     * @param target          Target GPS position
     * @param speedKmh        Speed in km/h
     * @param intervalSeconds Time interval in seconds
     * @return Next GPS position
     */
    public static GpsCoordinate calculateNextPosition(GpsCoordinate current,
            GpsCoordinate target,
            double speedKmh,
            int intervalSeconds) {

        // Calculate distance that can be traveled in this interval
        double distanceCanTravel = (speedKmh / 3600.0) * intervalSeconds; // km

        // Calculate total distance to target
        double totalDistance = calculate(
                current.getLatitude(), current.getLongitude(),
                target.getLatitude(), target.getLongitude());

        // If we can reach the target in this interval, return target
        if (distanceCanTravel >= totalDistance) {
            return new GpsCoordinate(target.getLatitude(), target.getLongitude());
        }

        // Calculate the fraction of the journey we can complete
        double fraction = distanceCanTravel / totalDistance;

        // Linear interpolation (simplified - good enough for short distances)
        double nextLat = current.getLatitude() +
                (target.getLatitude() - current.getLatitude()) * fraction;
        double nextLon = current.getLongitude() +
                (target.getLongitude() - current.getLongitude()) * fraction;

        return new GpsCoordinate(nextLat, nextLon);
    }

    /**
     * Calculate bearing (direction) from one point to another
     * 
     * @param lat1 Latitude of point 1 (degrees)
     * @param lon1 Longitude of point 1 (degrees)
     * @param lat2 Latitude of point 2 (degrees)
     * @param lon2 Longitude of point 2 (degrees)
     * @return Bearing in degrees (0-360)
     */
    public static double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double dLon = Math.toRadians(lon2 - lon1);

        double y = Math.sin(dLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360; // Normalize to 0-360
    }
}

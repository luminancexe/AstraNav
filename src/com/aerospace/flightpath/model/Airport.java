package com.aerospace.flightpath.model;

/**
 * Airport node containing aeronautical airport data (IATA/ICAO codes, runway count, country).
 */
public class Airport extends Node {
    private final String iataCode;
    private final String icaoCode;
    private final String country;
    private final int runwayCount;

    public Airport(String iataCode, String icaoCode, String name, String country,
                   double latitude, double longitude, double elevationMeters,
                   NodeType type, int runwayCount) {
        super(iataCode, name, new Coordinate(latitude, longitude, elevationMeters), type);
        this.iataCode = iataCode;
        this.icaoCode = icaoCode;
        this.country = country;
        this.runwayCount = runwayCount;
    }

    public String getIataCode() {
        return iataCode;
    }

    public String getIcaoCode() {
        return icaoCode;
    }

    public String getCountry() {
        return country;
    }

    public int getRunwayCount() {
        return runwayCount;
    }
}

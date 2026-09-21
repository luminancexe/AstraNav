package com.aerospace.flightpath.model;

/**
 * Aerodynamic and performance profile for aircraft or UAV drone.
 */
public class AircraftProfile {
    private final String id;
    private final String name;
    private final String category; // "COMMERCIAL", "CARGO", "UAV_DRONE", "EVTOL"
    private final double cruiseAirspeedKmh;
    private final double baseFuelBurnKgPerHour; // or battery kWh for drone
    private final double emptyWeightKg;
    private final double maxPayloadKg;
    private final double cruiseAltitudeMeters;

    public AircraftProfile(String id, String name, String category, double cruiseAirspeedKmh,
                           double baseFuelBurnKgPerHour, double emptyWeightKg,
                           double maxPayloadKg, double cruiseAltitudeMeters) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.cruiseAirspeedKmh = cruiseAirspeedKmh;
        this.baseFuelBurnKgPerHour = baseFuelBurnKgPerHour;
        this.emptyWeightKg = emptyWeightKg;
        this.maxPayloadKg = maxPayloadKg;
        this.cruiseAltitudeMeters = cruiseAltitudeMeters;
    }

    public static AircraftProfile boeing787() {
        return new AircraftProfile(
                "B787", "Boeing 787-9 Dreamliner", "COMMERCIAL",
                903.0,  // Cruise ~487 knots (~903 km/h)
                5400.0, // ~5,400 kg/hr fuel burn
                128850.0, 52500.0, 11000.0
        );
    }

    public static AircraftProfile cargo747() {
        return new AircraftProfile(
                "B748F", "Boeing 747-8 Freighter", "CARGO",
                910.0,
                10200.0, // High fuel burn
                197130.0, 137700.0, 10500.0
        );
    }

    public static AircraftProfile droneReaper() {
        return new AircraftProfile(
                "MQ9", "MQ-9 Reaper Long-Range UAV", "UAV_DRONE",
                313.0,  // 169 knots
                95.0,   // Light jet fuel burn
                2223.0, 1701.0, 7500.0
        );
    }

    public static AircraftProfile evtolCity() {
        return new AircraftProfile(
                "EVTOL", "AeroSky Electric eVTOL", "EVTOL",
                200.0,  // 200 km/h
                45.0,   // Equivalent battery burn (kg-equiv / energy units)
                1800.0, 450.0, 1200.0
        );
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getCruiseAirspeedKmh() {
        return cruiseAirspeedKmh;
    }

    public double getBaseFuelBurnKgPerHour() {
        return baseFuelBurnKgPerHour;
    }

    public double getEmptyWeightKg() {
        return emptyWeightKg;
    }

    public double getMaxPayloadKg() {
        return maxPayloadKg;
    }

    public double getCruiseAltitudeMeters() {
        return cruiseAltitudeMeters;
    }

    /**
     * Calculates estimated fuel burn (kg) for a flight leg given distance (km),
     * effective ground speed (km/h), and payload fraction (0.0 to 1.0).
     */
    public double estimateFuelConsumptionKg(double distanceKm, double groundSpeedKmh, double payloadFraction) {
        if (groundSpeedKmh <= 0.0) return Double.POSITIVE_INFINITY;
        double flightTimeHours = distanceKm / groundSpeedKmh;

        double payloadWeight = maxPayloadKg * Math.min(1.0, Math.max(0.0, payloadFraction));
        double grossWeight = emptyWeightKg + payloadWeight;

        // Weight-induced drag multiplier: +1.5% fuel per 10,000 kg above empty
        double weightFactor = 1.0 + ((grossWeight - emptyWeightKg) / 100000.0) * 0.15;

        return flightTimeHours * baseFuelBurnKgPerHour * weightFactor;
    }
}

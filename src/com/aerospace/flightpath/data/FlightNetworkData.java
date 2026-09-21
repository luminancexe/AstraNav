package com.aerospace.flightpath.data;

import com.aerospace.flightpath.dsa.graph.AdjacencyListGraph;
import com.aerospace.flightpath.dsa.graph.Graph;
import com.aerospace.flightpath.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * High-fidelity aeronautical network dataset.
 * Models major international hubs, RNAV waypoints, North Atlantic Tracks (NAT),
 * Trans-Pacific corridors, realistic prevailing jetstream wind aloft vectors,
 * and restricted military/storm airspace zones.
 */
public class FlightNetworkData {

    public static class NetworkBundle {
        public final Graph graph;
        public final List<RestrictedZone> restrictedZones;
        public final List<Airport> airports;
        public final List<Waypoint> waypoints;

        public NetworkBundle(Graph graph, List<RestrictedZone> restrictedZones,
                             List<Airport> airports, List<Waypoint> waypoints) {
            this.graph = graph;
            this.restrictedZones = restrictedZones;
            this.airports = airports;
            this.waypoints = waypoints;
        }
    }

    public static NetworkBundle createGlobalNetwork() {
        Graph graph = new AdjacencyListGraph();
        List<Airport> airports = new ArrayList<>();
        List<Waypoint> waypoints = new ArrayList<>();
        List<RestrictedZone> restrictedZones = new ArrayList<>();

        // 1. Major International Airports
        Airport jfk = new Airport("JFK", "KJFK", "New York John F. Kennedy", "USA", 40.6413, -73.7781, 4.0, Node.NodeType.MAJOR_HUB, 4);
        Airport lhr = new Airport("LHR", "EGLL", "London Heathrow", "UK", 51.4700, -0.4543, 25.0, Node.NodeType.MAJOR_HUB, 2);
        Airport cdg = new Airport("CDG", "LFPG", "Paris Charles de Gaulle", "France", 49.0097, 2.5479, 119.0, Node.NodeType.MAJOR_HUB, 4);
        Airport fra = new Airport("FRA", "EDDF", "Frankfurt Airport", "Germany", 50.0379, 8.5622, 111.0, Node.NodeType.MAJOR_HUB, 4);
        Airport ord = new Airport("ORD", "KORD", "Chicago O'Hare", "USA", 41.9742, -87.9073, 204.0, Node.NodeType.MAJOR_HUB, 8);
        Airport sfo = new Airport("SFO", "KSFO", "San Francisco Intl", "USA", 37.6213, -122.3790, 4.0, Node.NodeType.MAJOR_HUB, 4);
        Airport lax = new Airport("LAX", "KLAX", "Los Angeles Intl", "USA", 33.9416, -118.4085, 38.0, Node.NodeType.MAJOR_HUB, 4);
        Airport dxb = new Airport("DXB", "OMDB", "Dubai International", "UAE", 25.2532, 55.3657, 19.0, Node.NodeType.MAJOR_HUB, 2);
        Airport hnd = new Airport("HND", "RJTT", "Tokyo Haneda", "Japan", 35.5494, 139.7798, 11.0, Node.NodeType.MAJOR_HUB, 4);
        Airport sin = new Airport("SIN", "WSSS", "Singapore Changi", "Singapore", 1.3644, 103.9915, 7.0, Node.NodeType.MAJOR_HUB, 3);
        Airport hkg = new Airport("HKG", "VHHH", "Hong Kong Intl", "Hong Kong", 22.3080, 113.9185, 9.0, Node.NodeType.MAJOR_HUB, 3);
        Airport doh = new Airport("DOH", "OTHH", "Hamad Intl Doha", "Qatar", 25.2731, 51.6081, 13.0, Node.NodeType.MAJOR_HUB, 2);
        Airport syd = new Airport("SYD", "YSSY", "Sydney Kingsford Smith", "Australia", -33.9399, 151.1753, 6.0, Node.NodeType.MAJOR_HUB, 3);
        Airport yyz = new Airport("YYZ", "CYYZ", "Toronto Pearson", "Canada", 43.6777, -79.6248, 173.0, Node.NodeType.MAJOR_HUB, 5);
        Airport mia = new Airport("MIA", "KMIA", "Miami International", "USA", 25.7959, -80.2870, 3.0, Node.NodeType.MAJOR_HUB, 4);
        Airport mad = new Airport("MAD", "LEMD", "Madrid-Barajas", "Spain", 40.4839, -3.5680, 610.0, Node.NodeType.MAJOR_HUB, 4);

        // Regional Airports
        Airport bos = new Airport("BOS", "KBOS", "Boston Logan", "USA", 42.3656, -71.0096, 6.0, Node.NodeType.REGIONAL_AIRPORT, 6);
        Airport dbl = new Airport("DUB", "EIDW", "Dublin Airport", "Ireland", 53.4264, -6.2499, 74.0, Node.NodeType.REGIONAL_AIRPORT, 2);
        Airport sea = new Airport("SEA", "KSEA", "Seattle-Tacoma Intl", "USA", 47.4502, -122.3088, 132.0, Node.NodeType.REGIONAL_AIRPORT, 3);
        Airport anc = new Airport("ANC", "PANC", "Ted Stevens Anchorage", "USA", 61.1743, -149.9962, 46.0, Node.NodeType.REGIONAL_AIRPORT, 3);

        airports.addAll(Arrays.asList(jfk, lhr, cdg, fra, ord, sfo, lax, dxb, hnd, sin, hkg, doh, syd, yyz, mia, mad, bos, dbl, sea, anc));
        for (Airport a : airports) {
            graph.addNode(a);
        }

        // 2. High-Altitude Aeronautical Waypoints (North Atlantic Tracks, US Continental, Pacific, Eurasian)
        // North Atlantic Tracks (Eastbound & Westbound)
        Waypoint natGander = new Waypoint("YQX_VOR", "Gander VOR/Oceanic Gate", 48.9536, -54.5244, "NAT-GATE-W", 31000, 41000, Node.NodeType.OCEANIC_FIX);
        Waypoint natCymon = new Waypoint("CYMON", "NAT Fix CYMON", 52.0000, -50.0000, "NAT-TRACK-A", 33000, 41000, Node.NodeType.OCEANIC_FIX);
        Waypoint nat5440 = new Waypoint("54N40W", "Mid-Atlantic Fix 54N/40W", 54.0000, -40.0000, "NAT-TRACK-A", 33000, 41000, Node.NodeType.OCEANIC_FIX);
        Waypoint nat5530 = new Waypoint("55N30W", "Mid-Atlantic Fix 55N/30W", 55.0000, -30.0000, "NAT-TRACK-A", 33000, 41000, Node.NodeType.OCEANIC_FIX);
        Waypoint natDogal = new Waypoint("DOGAL", "NAT Fix DOGAL", 53.0000, -15.0000, "NAT-TRACK-A", 33000, 41000, Node.NodeType.OCEANIC_FIX);
        Waypoint natShannon = new Waypoint("SHA_VOR", "Shannon VOR/Oceanic Gate", 52.7214, -8.9248, "NAT-GATE-E", 28000, 41000, Node.NodeType.OCEANIC_FIX);

        // Southern Atlantic Alternative Corridor (Avoids polar storms)
        Waypoint natBermuda = new Waypoint("BDA_FIX", "Bermuda Airway Fix", 32.3640, -64.6787, "AZORES-CORR", 29000, 39000, Node.NodeType.OCEANIC_FIX);
        Waypoint natAzores = new Waypoint("SMA_VOR", "Santa Maria Azores Oceanic Gate", 36.9749, -25.1706, "AZORES-CORR", 31000, 41000, Node.NodeType.OCEANIC_FIX);
        Waypoint natLisbon = new Waypoint("LIS_FIX", "Lisbon Airway Entry", 38.7742, -9.1342, "AZORES-CORR", 28000, 38000, Node.NodeType.AIRWAY_WAYPOINT);

        // US Continental Cross-Country Airways
        Waypoint jfkDep = new Waypoint("JFK_DEP", "New York Departure Fix", 41.2000, -74.5000, "J70", 10000, 24000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint clevelandVor = new Waypoint("CLE_VOR", "Cleveland Navigational Fix", 41.3578, -81.8597, "J70", 24000, 38000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint iowaVor = new Waypoint("IOW_VOR", "Iowa Waypoint Fix", 41.6850, -91.5420, "J70", 28000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint denverPass = new Waypoint("DEN_FIX", "Rockies Airway Crossing", 39.8561, -104.6737, "J70", 30000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint saltLake = new Waypoint("SLC_VOR", "Salt Lake VOR Waypoint", 40.7884, -111.9778, "J70", 30000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint sierraNevada = new Waypoint("RENO_FIX", "Sierra Nevada Inbound Waypoint", 39.4991, -119.7681, "J70", 26000, 39000, Node.NodeType.AIRWAY_WAYPOINT);

        // US Southern Corridor (JFK -> MIA / LAX)
        Waypoint washDc = new Waypoint("DCA_FIX", "Mid-Atlantic Corridor Fix", 38.8512, -77.0402, "J42", 18000, 35000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint atlantaFix = new Waypoint("ATL_VOR", "Atlanta Airway Hub Fix", 33.6407, -84.4277, "J42", 20000, 38000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint dallasFix = new Waypoint("DFW_VOR", "Dallas Metroplex Waypoint", 32.8998, -97.0403, "J20", 24000, 39000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint phoenixFix = new Waypoint("PHX_VOR", "Phoenix Desert Airway Fix", 33.4352, -112.0101, "J20", 25000, 39000, Node.NodeType.AIRWAY_WAYPOINT);

        // European & Middle East Airways
        Waypoint englishChannel = new Waypoint("DVR_VOR", "Dover Channel Crossing", 51.1333, 1.3500, "UL607", 15000, 35000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint alpineCrossing = new Waypoint("ZUR_FIX", "Zurich Alpine Corridor", 47.4582, 8.5555, "UN871", 24000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint balkansWay = new Waypoint("BEG_VOR", "Balkan Corridor Fix", 44.8184, 20.3091, "UL602", 28000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint istanbulGate = new Waypoint("IST_FIX", "Bosporus Airway Gate", 41.2753, 28.7519, "UL602", 29000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint ankaraVor = new Waypoint("ANK_VOR", "Anatolia Crossing Waypoint", 39.9500, 32.6833, "UM688", 31000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint baghdadFix = new Waypoint("BGW_FIX", "Mesopotamia Corridor", 33.2625, 44.2344, "UM688", 31000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint gulfEntry = new Waypoint("BAH_VOR", "Arabian Gulf Oceanic Gate", 26.2708, 50.6336, "UP574", 28000, 41000, Node.NodeType.AIRWAY_WAYPOINT);

        // Asian & Pacific Corridors
        Waypoint karachiFix = new Waypoint("KHI_VOR", "Arabian Sea Crossing Fix", 24.9065, 67.1608, "P574", 29000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint mumbaiFix = new Waypoint("BOM_VOR", "India Continental Gate", 19.0896, 72.8656, "N571", 31000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint bayOfBengal = new Waypoint("BOB_FIX", "Bay of Bengal Oceanic Fix", 13.0000, 85.0000, "N571", 33000, 41000, Node.NodeType.OCEANIC_FIX);
        Waypoint phuketFix = new Waypoint("HKT_VOR", "Andaman Sea Gate", 8.1132, 98.3169, "M774", 30000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint bangkokFix = new Waypoint("BKK_VOR", "Indochina Transit Hub", 13.6900, 100.7501, "M774", 28000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint vietnamCoast = new Waypoint("DAN_FIX", "South China Sea West Fix", 16.0439, 108.1994, "A1", 31000, 41000, Node.NodeType.OCEANIC_FIX);
        Waypoint manilaGate = new Waypoint("MNL_FIX", "Luzon Transit Gate", 14.5086, 121.0194, "A1", 28000, 41000, Node.NodeType.AIRWAY_WAYPOINT);
        Waypoint okinawaFix = new Waypoint("OKA_VOR", "Ryukyu Islands Airway", 26.1958, 127.6458, "G581", 29000, 41000, Node.NodeType.AIRWAY_WAYPOINT);

        // Transpacific Oceanic Routes (SFO/LAX <-> HND/SYD)
        Waypoint norpac1 = new Waypoint("ALCAN_WPT", "Alaska Aleutian Gateway", 55.0000, -160.0000, "NOPAC-1", 33000, 41000, Node.NodeType.OCEANIC_FIX);
        Waypoint norpac2 = new Waypoint("KAMCH_WPT", "Kamchatka Oceanic Transit", 52.0000, 160.0000, "NOPAC-1", 33000, 41000, Node.NodeType.OCEANIC_FIX);
        Waypoint pacMidway = new Waypoint("MDW_FIX", "Mid-Pacific Hawaii Corridor", 28.2072, -177.3800, "PACOTS-S", 31000, 41000, Node.NodeType.OCEANIC_FIX);
        Waypoint pacGuam = new Waypoint("GUM_VOR", "Guam Transit Waypoint", 13.4839, 144.7958, "PACOTS-S", 29000, 41000, Node.NodeType.OCEANIC_FIX);
        Waypoint pacFiji = new Waypoint("NAN_VOR", "South Pacific Fiji Corridor", -17.7554, 177.4433, "S-PAC", 31000, 41000, Node.NodeType.OCEANIC_FIX);

        waypoints.addAll(Arrays.asList(
                natGander, natCymon, nat5440, nat5530, natDogal, natShannon,
                natBermuda, natAzores, natLisbon,
                jfkDep, clevelandVor, iowaVor, denverPass, saltLake, sierraNevada,
                washDc, atlantaFix, dallasFix, phoenixFix,
                englishChannel, alpineCrossing, balkansWay, istanbulGate, ankaraVor, baghdadFix, gulfEntry,
                karachiFix, mumbaiFix, bayOfBengal, phuketFix, bangkokFix, vietnamCoast, manilaGate, okinawaFix,
                norpac1, norpac2, pacMidway, pacGuam, pacFiji
        ));

        for (Waypoint w : waypoints) {
            graph.addNode(w);
        }

        // 3. Realistic Wind Aloft (Jetstream winds from 270° at 60-110 knots in North Atlantic and Pacific)
        double jetStreamSpeed = 95.0; // knots
        double jetStreamHeading = 265.0; // blowing from west to east
        double calmWindSpeed = 15.0;

        // North Atlantic Track Corridor (JFK/BOS -> GANDER -> OCEANIC -> SHANNON/LHR)
        // High eastbound tailwinds (+95 knots tailwind), severe westbound headwind!
        graph.addBidirectionalEdge(jfk, bos, calmWindSpeed, 240.0, "J121");
        graph.addBidirectionalEdge(bos, natGander, 40.0, 260.0, "NAT-FEEDER");
        graph.addBidirectionalEdge(jfk, natGander, 50.0, 260.0, "NAT-ENTRY");
        graph.addBidirectionalEdge(yyz, natGander, 45.0, 270.0, "NAT-FEEDER-N");

        graph.addBidirectionalEdge(natGander, natCymon, jetStreamSpeed, jetStreamHeading, "NAT-TRACK-A");
        graph.addBidirectionalEdge(natCymon, nat5440, jetStreamSpeed, jetStreamHeading, "NAT-TRACK-A");
        graph.addBidirectionalEdge(nat5440, nat5530, jetStreamSpeed, jetStreamHeading, "NAT-TRACK-A");
        graph.addBidirectionalEdge(nat5530, natDogal, jetStreamSpeed, jetStreamHeading, "NAT-TRACK-A");
        graph.addBidirectionalEdge(natDogal, natShannon, jetStreamSpeed, jetStreamHeading, "NAT-TRACK-A");

        graph.addBidirectionalEdge(natShannon, dbl, 30.0, 250.0, "NAT-EXIT-IRL");
        graph.addBidirectionalEdge(dbl, lhr, 25.0, 250.0, "UL975");
        graph.addBidirectionalEdge(natShannon, lhr, 35.0, 260.0, "NAT-EXIT-UK");
        graph.addBidirectionalEdge(natShannon, cdg, 35.0, 270.0, "NAT-EXIT-FR");

        // Southern Transatlantic Alternative Route (via Bermuda & Azores - avoids North Atlantic storms)
        graph.addBidirectionalEdge(jfk, natBermuda, 30.0, 290.0, "BMA-TRACK");
        graph.addBidirectionalEdge(mia, natBermuda, 25.0, 270.0, "CARIB-FEEDER");
        graph.addBidirectionalEdge(natBermuda, natAzores, 35.0, 280.0, "MID-ATL-S");
        graph.addBidirectionalEdge(natAzores, natLisbon, 20.0, 310.0, "AZORES-EUR");
        graph.addBidirectionalEdge(natLisbon, mad, 15.0, 300.0, "UN864");
        graph.addBidirectionalEdge(mad, cdg, 20.0, 250.0, "UN867");
        graph.addBidirectionalEdge(mad, lhr, 25.0, 250.0, "UN866");

        // US Transcontinental Corridor (JFK/ORD/SFO/LAX)
        graph.addBidirectionalEdge(jfk, jfkDep, 15.0, 270.0, "J70-DEP");
        graph.addBidirectionalEdge(jfkDep, clevelandVor, 45.0, 275.0, "J70-E");
        graph.addBidirectionalEdge(clevelandVor, ord, 40.0, 270.0, "J70-MID");
        graph.addBidirectionalEdge(yyz, clevelandVor, 25.0, 260.0, "Q822");
        graph.addBidirectionalEdge(ord, iowaVor, 50.0, 270.0, "J70-C");
        graph.addBidirectionalEdge(iowaVor, denverPass, 55.0, 270.0, "J70-W");
        graph.addBidirectionalEdge(denverPass, saltLake, 60.0, 260.0, "J70-MTN");
        graph.addBidirectionalEdge(saltLake, sierraNevada, 55.0, 270.0, "J70-DESERT");
        graph.addBidirectionalEdge(sierraNevada, sfo, 35.0, 280.0, "J70-ARR");
        graph.addBidirectionalEdge(sierraNevada, sea, 30.0, 290.0, "V23-N");
        graph.addBidirectionalEdge(sfo, lax, 20.0, 315.0, "COASTAL-1");

        // US Southern Trunk (JFK/MIA/DFW/LAX)
        graph.addBidirectionalEdge(jfk, washDc, 20.0, 260.0, "V1-EAST");
        graph.addBidirectionalEdge(washDc, atlantaFix, 30.0, 270.0, "J42-S");
        graph.addBidirectionalEdge(atlantaFix, mia, 20.0, 280.0, "J79");
        graph.addBidirectionalEdge(atlantaFix, dallasFix, 35.0, 270.0, "J20-E");
        graph.addBidirectionalEdge(ord, dallasFix, 30.0, 270.0, "J35");
        graph.addBidirectionalEdge(dallasFix, phoenixFix, 40.0, 265.0, "J20-W");
        graph.addBidirectionalEdge(phoenixFix, lax, 25.0, 270.0, "J20-ARR");
        graph.addBidirectionalEdge(phoenixFix, sfo, 25.0, 280.0, "J92");

        // European Cross-Network
        graph.addBidirectionalEdge(lhr, englishChannel, 20.0, 250.0, "UL607");
        graph.addBidirectionalEdge(englishChannel, cdg, 15.0, 240.0, "UM733");
        graph.addBidirectionalEdge(cdg, fra, 25.0, 260.0, "UM163");
        graph.addBidirectionalEdge(cdg, alpineCrossing, 30.0, 270.0, "UN871");
        graph.addBidirectionalEdge(fra, alpineCrossing, 25.0, 270.0, "UL608");
        graph.addBidirectionalEdge(alpineCrossing, balkansWay, 30.0, 270.0, "UL602-W");
        graph.addBidirectionalEdge(balkansWay, istanbulGate, 35.0, 280.0, "UL602-E");

        // Middle East & Gulf Corridors
        graph.addBidirectionalEdge(istanbulGate, ankaraVor, 30.0, 270.0, "UM688-W");
        graph.addBidirectionalEdge(ankaraVor, baghdadFix, 35.0, 280.0, "UM688-E");
        graph.addBidirectionalEdge(baghdadFix, gulfEntry, 25.0, 310.0, "UP574");
        graph.addBidirectionalEdge(gulfEntry, doh, 15.0, 320.0, "GULF-DOH");
        graph.addBidirectionalEdge(gulfEntry, dxb, 15.0, 320.0, "GULF-DXB");
        graph.addBidirectionalEdge(dxb, doh, 10.0, 330.0, "GULF-SHUTTLE");

        // Asia / Indian Ocean Corridors
        graph.addBidirectionalEdge(dxb, karachiFix, 25.0, 280.0, "P574");
        graph.addBidirectionalEdge(karachiFix, mumbaiFix, 25.0, 270.0, "N571");
        graph.addBidirectionalEdge(mumbaiFix, bayOfBengal, 30.0, 270.0, "N571-IND");
        graph.addBidirectionalEdge(bayOfBengal, phuketFix, 25.0, 260.0, "M774-W");
        graph.addBidirectionalEdge(phuketFix, sin, 20.0, 250.0, "M774-S");
        graph.addBidirectionalEdge(phuketFix, bangkokFix, 15.0, 240.0, "M774-N");
        graph.addBidirectionalEdge(bangkokFix, sin, 20.0, 250.0, "A464");
        graph.addBidirectionalEdge(bangkokFix, vietnamCoast, 25.0, 260.0, "A1-W");
        graph.addBidirectionalEdge(vietnamCoast, hkg, 30.0, 260.0, "A1-E");
        graph.addBidirectionalEdge(sin, vietnamCoast, 25.0, 250.0, "M768");
        graph.addBidirectionalEdge(sin, manilaGate, 25.0, 260.0, "N892");
        graph.addBidirectionalEdge(hkg, manilaGate, 20.0, 270.0, "A583");
        graph.addBidirectionalEdge(hkg, okinawaFix, 30.0, 260.0, "G581");
        graph.addBidirectionalEdge(okinawaFix, hnd, 40.0, 265.0, "G581-ARR");

        // Transpacific Northern Arc (SEA/SFO <-> HND via Alaska)
        graph.addBidirectionalEdge(sea, anc, 45.0, 280.0, "NOPAC-IN");
        graph.addBidirectionalEdge(sfo, anc, 40.0, 270.0, "NOPAC-CAL");
        graph.addBidirectionalEdge(anc, norpac1, 75.0, 260.0, "NOPAC-1");
        graph.addBidirectionalEdge(norpac1, norpac2, 85.0, 260.0, "NOPAC-2");
        graph.addBidirectionalEdge(norpac2, hnd, 65.0, 270.0, "NOPAC-OUT");

        // Transpacific Central & Southern Corridors (SFO/LAX <-> SYD / HND via Hawaii & Pacific Islands)
        graph.addBidirectionalEdge(sfo, pacMidway, 40.0, 260.0, "PACOTS-M1");
        graph.addBidirectionalEdge(lax, pacMidway, 35.0, 260.0, "PACOTS-M2");
        graph.addBidirectionalEdge(pacMidway, pacGuam, 30.0, 260.0, "PACOTS-M3");
        graph.addBidirectionalEdge(pacGuam, hnd, 35.0, 270.0, "PACOTS-M4");
        graph.addBidirectionalEdge(pacGuam, manilaGate, 25.0, 270.0, "PACOTS-M5");

        graph.addBidirectionalEdge(lax, pacFiji, 30.0, 270.0, "SPAC-1");
        graph.addBidirectionalEdge(pacFiji, syd, 35.0, 280.0, "SPAC-2");
        graph.addBidirectionalEdge(sin, syd, 30.0, 270.0, "AUST-IND");

        // 4. Restricted Airspace Zones (No-Fly Zones)
        // Zone A: Severe North Atlantic Storm Cell (directly centered on 54N40W)
        // When active, forces flights to divert south via Azores or north via Greenland!
        RestrictedZone atlanticStorm = new RestrictedZone(
                "STORM_NAT_ALPHA",
                "Severe North Atlantic Storm Cell Alpha",
                RestrictedZone.ZoneType.SEVERE_STORM,
                new Coordinate(54.0, -40.0),
                550.0, // 550 km radius storm cell
                0.0, 45000.0
        );

        // Zone B: Nevada Special Military Operations Area (MOA) / Area 51 Prohibited Airspace
        RestrictedZone militaryNevada = new RestrictedZone(
                "MOA_NEVADA",
                "Nevada Military Operations Airway Restriction (R-4808)",
                RestrictedZone.ZoneType.MILITARY_PROHIBITED,
                new Coordinate(37.2, -116.0),
                320.0,
                0.0, 60000.0
        );

        // Zone C: Eastern European Active Conflict Temporary Flight Restriction (TFR)
        RestrictedZone balkansConflict = new RestrictedZone(
                "TFR_BALKANS",
                "Carpathian Active Air Defense Exercise TFR",
                RestrictedZone.ZoneType.VIP_TFR,
                new Coordinate(45.5, 23.0),
                380.0,
                0.0, 50000.0
        );

        restrictedZones.add(atlanticStorm);
        restrictedZones.add(militaryNevada);
        restrictedZones.add(balkansConflict);

        return new NetworkBundle(graph, restrictedZones, airports, waypoints);
    }
}

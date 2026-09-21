package com.aerospace.flightpath.server;

import com.aerospace.flightpath.algorithm.*;
import com.aerospace.flightpath.data.FlightNetworkData;
import com.aerospace.flightpath.dsa.graph.Graph;
import com.aerospace.flightpath.dsa.sort.RouteSorter;
import com.aerospace.flightpath.model.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * High-performance embedded HTTP server for the Aerospace Flight Path Optimizer.
 * Runs on standard Java SE with zero third-party dependencies.
 */
public class FlightServer {
    private final int port;
    private final FlightNetworkData.NetworkBundle networkBundle;
    private final Map<String, AircraftProfile> aircraftProfiles = new LinkedHashMap<>();
    private HttpServer server;

    public FlightServer(int port) {
        this.port = port;
        this.networkBundle = FlightNetworkData.createGlobalNetwork();

        // Register default aircraft profiles
        AircraftProfile b787 = AircraftProfile.boeing787();
        AircraftProfile b748f = AircraftProfile.cargo747();
        AircraftProfile mq9 = AircraftProfile.droneReaper();
        AircraftProfile evtol = AircraftProfile.evtolCity();

        aircraftProfiles.put(b787.getId(), b787);
        aircraftProfiles.put(b748f.getId(), b748f);
        aircraftProfiles.put(mq9.getId(), mq9);
        aircraftProfiles.put(evtol.getId(), evtol);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Endpoints
        server.createContext("/api/network", new NetworkHandler());
        server.createContext("/api/route", new RouteHandler());
        server.createContext("/api/reroute", new RerouteHandler());
        server.createContext("/api/benchmark", new BenchmarkHandler());
        server.createContext("/api/toggle-node", new ToggleNodeHandler());
        server.createContext("/api/toggle-zone", new ToggleZoneHandler());

        // Static Web Asset Handler
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("==================================================================");
        System.out.println(" ✈️  ASTRANAV — AEROSPACE NAVIGATION & PATHFINDING ENGINE ONLINE");
        System.out.printf (" 🌐  Dashboard URL: http://localhost:%d\n", port);
        System.out.printf (" 📡  API Endpoints: http://localhost:%d/api/network\n", port);
        System.out.println("==================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =========================================================================
    // API HANDLERS
    // =========================================================================

    private class NetworkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{");

            // 1. Airports
            sb.append("\"airports\":[");
            List<Airport> airports = networkBundle.airports;
            for (int i = 0; i < airports.size(); i++) {
                Airport a = airports.get(i);
                sb.append(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"iata\":\"%s\",\"icao\":\"%s\",\"country\":\"%s\"," +
                                "\"lat\":%.5f,\"lon\":%.5f,\"alt\":%.1f,\"active\":%b,\"type\":\"%s\"}",
                        JsonHelper.escape(a.getId()), JsonHelper.escape(a.getName()), a.getIataCode(), a.getIcaoCode(),
                        JsonHelper.escape(a.getCountry()), a.getCoordinate().getLatitude(), a.getCoordinate().getLongitude(),
                        a.getCoordinate().getAltitudeMeters(), a.isActive(), a.getType().name()));
                if (i < airports.size() - 1) sb.append(",");
            }
            sb.append("],");

            // 2. Waypoints
            sb.append("\"waypoints\":[");
            List<Waypoint> waypoints = networkBundle.waypoints;
            for (int i = 0; i < waypoints.size(); i++) {
                Waypoint w = waypoints.get(i);
                sb.append(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"airway\":\"%s\"," +
                                "\"lat\":%.5f,\"lon\":%.5f,\"alt\":%.1f,\"active\":%b,\"type\":\"%s\"}",
                        JsonHelper.escape(w.getId()), JsonHelper.escape(w.getName()), JsonHelper.escape(w.getAirwayIdentifier()),
                        w.getCoordinate().getLatitude(), w.getCoordinate().getLongitude(),
                        w.getCoordinate().getAltitudeMeters(), w.isActive(), w.getType().name()));
                if (i < waypoints.size() - 1) sb.append(",");
            }
            sb.append("],");

            // 3. Flight Segments (Airways)
            sb.append("\"segments\":[");
            List<FlightSegment> segments = new ArrayList<>(networkBundle.graph.getAllSegments());
            for (int i = 0; i < segments.size(); i++) {
                FlightSegment s = segments.get(i);
                sb.append(String.format("{\"source\":\"%s\",\"target\":\"%s\",\"distanceKm\":%.1f,\"bearing\":%.1f," +
                                "\"windSpeed\":%.1f,\"windDir\":%.1f,\"airway\":\"%s\",\"active\":%b}",
                        s.getSource().getId(), s.getTarget().getId(), s.getDistanceKm(), s.getInitialBearingDegrees(),
                        s.getWindVector().getSpeedKnots(), s.getWindVector().getDirectionDegrees(),
                        JsonHelper.escape(s.getAirwayCode()), s.isActive()));
                if (i < segments.size() - 1) sb.append(",");
            }
            sb.append("],");

            // 4. Restricted Zones
            sb.append("\"restrictedZones\":[");
            List<RestrictedZone> zones = networkBundle.restrictedZones;
            for (int i = 0; i < zones.size(); i++) {
                RestrictedZone z = zones.get(i);
                sb.append(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"active\":%b," +
                                "\"lat\":%.5f,\"lon\":%.5f,\"radiusKm\":%.1f,\"floorFt\":%.0f,\"ceilingFt\":%.0f}",
                        z.getId(), JsonHelper.escape(z.getName()), z.getType().name(), z.isActive(),
                        z.getCenter().getLatitude(), z.getCenter().getLongitude(), z.getRadiusKm(),
                        z.getFloorFt(), z.getCeilingFt()));
                if (i < zones.size() - 1) sb.append(",");
            }
            sb.append("],");

            // 5. Aircraft Profiles
            sb.append("\"aircraftProfiles\":[");
            int aIdx = 0;
            for (AircraftProfile ac : aircraftProfiles.values()) {
                sb.append(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"category\":\"%s\",\"cruiseSpeed\":%.1f,\"fuelBurn\":%.1f}",
                        ac.getId(), JsonHelper.escape(ac.getName()), ac.getCategory(), ac.getCruiseAirspeedKmh(), ac.getBaseFuelBurnKgPerHour()));
                if (++aIdx < aircraftProfiles.size()) sb.append(",");
            }
            sb.append("]");

            sb.append("}");

            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class RouteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> req = JsonHelper.parseSimpleJson(body);

            String originId = (String) req.getOrDefault("origin", "JFK");
            String destId = (String) req.getOrDefault("destination", "LHR");
            String algo = ((String) req.getOrDefault("algorithm", "ASTAR")).toUpperCase();
            String objStr = ((String) req.getOrDefault("objective", "DISTANCE")).toUpperCase();
            String aircraftId = (String) req.getOrDefault("aircraft", "B787");
            boolean avoidRestricted = (boolean) req.getOrDefault("avoidRestricted", true);

            Node origin = networkBundle.graph.getNode(originId);
            Node dest = networkBundle.graph.getNode(destId);
            AircraftProfile aircraft = aircraftProfiles.getOrDefault(aircraftId, AircraftProfile.boeing787());

            OptimizationObjective objective = OptimizationObjective.DISTANCE;
            try {
                objective = OptimizationObjective.valueOf(objStr);
            } catch (Exception ignored) {}

            List<RestrictedZone> activeZones = avoidRestricted ? networkBundle.restrictedZones : Collections.emptyList();

            if (origin == null || dest == null) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Invalid origin or destination node\"}");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{");

            if ("YEN_K".equals(algo)) {
                List<RouteResult> kRoutes = YenKShortestPaths.findKShortestPaths(
                        networkBundle.graph, origin, dest, objective, aircraft, activeZones, 4);
                sb.append("\"routes\":[");
                for (int i = 0; i < kRoutes.size(); i++) {
                    serializeRouteResult(sb, kRoutes.get(i));
                    if (i < kRoutes.size() - 1) sb.append(",");
                }
                sb.append("]");
            } else if ("DFS".equals(algo)) {
                List<RouteResult> dfsRoutes = BfsDfsRouter.findAllPathsDfs(
                        networkBundle.graph, origin, dest, aircraft, activeZones, 8, 5);
                sb.append("\"routes\":[");
                for (int i = 0; i < dfsRoutes.size(); i++) {
                    serializeRouteResult(sb, dfsRoutes.get(i));
                    if (i < dfsRoutes.size() - 1) sb.append(",");
                }
                sb.append("]");
            } else {
                RouteResult res;
                if ("DIJKSTRA".equals(algo)) {
                    res = DijkstraRouter.findRoute(networkBundle.graph, origin, dest, objective, aircraft, activeZones);
                } else if ("BFS".equals(algo)) {
                    res = BfsDfsRouter.findShortestHopsRoute(networkBundle.graph, origin, dest, aircraft, activeZones);
                } else {
                    res = AStarRouter.findRoute(networkBundle.graph, origin, dest, objective, aircraft, activeZones);
                }

                sb.append("\"route\":");
                serializeRouteResult(sb, res);
            }

            sb.append("}");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class RerouteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> req = JsonHelper.parseSimpleJson(body);

            String originId = (String) req.getOrDefault("origin", "JFK");
            String destId = (String) req.getOrDefault("destination", "LHR");
            String aircraftId = (String) req.getOrDefault("aircraft", "B787");
            String objStr = ((String) req.getOrDefault("objective", "DISTANCE")).toUpperCase();

            @SuppressWarnings("unchecked")
            List<String> disabledList = (List<String>) req.get("disabledNodes");
            Set<String> disabledSet = disabledList != null ? new HashSet<>(disabledList) : Collections.emptySet();

            Node origin = networkBundle.graph.getNode(originId);
            Node dest = networkBundle.graph.getNode(destId);
            AircraftProfile aircraft = aircraftProfiles.getOrDefault(aircraftId, AircraftProfile.boeing787());
            OptimizationObjective objective = OptimizationObjective.DISTANCE;
            try { objective = OptimizationObjective.valueOf(objStr); } catch (Exception ignored) {}

            DynamicRerouter.RerouteAnalysis analysis = DynamicRerouter.planDetour(
                    networkBundle.graph, origin, dest, objective, aircraft, disabledSet, networkBundle.restrictedZones);

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"distanceDeltaKm\":").append(analysis.getDistanceDeltaKm()).append(",");
            sb.append("\"timeDeltaHours\":").append(analysis.getTimeDeltaHours()).append(",");
            sb.append("\"fuelDeltaKg\":").append(analysis.getFuelDeltaKg()).append(",");
            sb.append("\"originalRoute\":");
            serializeRouteResult(sb, analysis.getOriginalRoute());
            sb.append(",");
            sb.append("\"reroutedRoute\":");
            serializeRouteResult(sb, analysis.getReroutedRoute());
            sb.append("}");

            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class BenchmarkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Node jfk = networkBundle.graph.getNode("JFK");
            Node lhr = networkBundle.graph.getNode("LHR");
            Node sfo = networkBundle.graph.getNode("SFO");
            Node dxb = networkBundle.graph.getNode("DXB");
            AircraftProfile ac = AircraftProfile.boeing787();

            List<RouteResult> dijkstraList = new ArrayList<>();
            List<RouteResult> aStarList = new ArrayList<>();
            List<RouteResult> bfsList = new ArrayList<>();

            // Test Pair 1: Transatlantic JFK -> LHR
            dijkstraList.add(DijkstraRouter.findRoute(networkBundle.graph, jfk, lhr, OptimizationObjective.DISTANCE, ac, null));
            aStarList.add(AStarRouter.findRoute(networkBundle.graph, jfk, lhr, OptimizationObjective.DISTANCE, ac, null));
            bfsList.add(BfsDfsRouter.findShortestHopsRoute(networkBundle.graph, jfk, lhr, ac, null));

            // Test Pair 2: Transcontinental JFK -> SFO
            dijkstraList.add(DijkstraRouter.findRoute(networkBundle.graph, jfk, sfo, OptimizationObjective.DISTANCE, ac, null));
            aStarList.add(AStarRouter.findRoute(networkBundle.graph, jfk, sfo, OptimizationObjective.DISTANCE, ac, null));
            bfsList.add(BfsDfsRouter.findShortestHopsRoute(networkBundle.graph, jfk, sfo, ac, null));

            // Test Pair 3: Intercontinental LHR -> DXB
            dijkstraList.add(DijkstraRouter.findRoute(networkBundle.graph, lhr, dxb, OptimizationObjective.DISTANCE, ac, null));
            aStarList.add(AStarRouter.findRoute(networkBundle.graph, lhr, dxb, OptimizationObjective.DISTANCE, ac, null));
            bfsList.add(BfsDfsRouter.findShortestHopsRoute(networkBundle.graph, lhr, dxb, ac, null));

            StringBuilder sb = new StringBuilder();
            sb.append("{\"benchmarkResults\":[");
            for (int i = 0; i < 3; i++) {
                RouteResult d = dijkstraList.get(i);
                RouteResult a = aStarList.get(i);
                RouteResult b = bfsList.get(i);
                String testName = (i == 0) ? "Transatlantic (JFK -> LHR)" : (i == 1) ? "Transcontinental (JFK -> SFO)" : "Eurasia (LHR -> DXB)";

                sb.append(String.format("{\"test\":\"%s\"," +
                                "\"dijkstra\":{\"dist\":%.1f,\"timeMicros\":%d,\"nodes\":%d}," +
                                "\"aStar\":{\"dist\":%.1f,\"timeMicros\":%d,\"nodes\":%d}," +
                                "\"bfs\":{\"dist\":%.1f,\"timeMicros\":%d,\"nodes\":%d}}",
                        testName,
                        d.getTotalDistanceKm(), d.getExecutionTimeMicros(), d.getNodesExplored(),
                        a.getTotalDistanceKm(), a.getExecutionTimeMicros(), a.getNodesExplored(),
                        b.getTotalDistanceKm(), b.getExecutionTimeMicros(), b.getNodesExplored()));
                if (i < 2) sb.append(",");
            }
            sb.append("]}");

            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class ToggleNodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> req = JsonHelper.parseSimpleJson(body);
            String nodeId = (String) req.get("nodeId");
            boolean active = (boolean) req.getOrDefault("active", true);

            Node node = networkBundle.graph.getNode(nodeId);
            if (node != null) {
                node.setActive(active);
                sendJsonResponse(exchange, 200, String.format("{\"success\":true,\"nodeId\":\"%s\",\"active\":%b}", nodeId, active));
            } else {
                sendJsonResponse(exchange, 404, "{\"error\":\"Node not found\"}");
            }
        }
    }

    private class ToggleZoneHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> req = JsonHelper.parseSimpleJson(body);
            String zoneId = (String) req.get("zoneId");
            boolean active = (boolean) req.getOrDefault("active", true);

            boolean found = false;
            for (RestrictedZone z : networkBundle.restrictedZones) {
                if (z.getId().equalsIgnoreCase(zoneId)) {
                    z.setActive(active);
                    found = true;
                    break;
                }
            }

            if (found) {
                sendJsonResponse(exchange, 200, String.format("{\"success\":true,\"zoneId\":\"%s\",\"active\":%b}", zoneId, active));
            } else {
                sendJsonResponse(exchange, 404, "{\"error\":\"Zone not found\"}");
            }
        }
    }

    // Static Web Asset Handler
    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Clean path
            if (path.startsWith("/")) path = path.substring(1);
            Path filePath = Paths.get("web", path);

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                String notFound = "404 Not Found: " + path;
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".png")) contentType = "image/png";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";

            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static void serializeRouteResult(StringBuilder sb, RouteResult res) {
        sb.append("{");
        sb.append("\"algorithm\":\"").append(JsonHelper.escape(res.getAlgorithmName())).append("\",");
        sb.append("\"objective\":\"").append(res.getObjective().name()).append("\",");
        sb.append("\"success\":").append(res.isSuccess()).append(",");
        sb.append("\"message\":\"").append(JsonHelper.escape(res.getMessage())).append("\",");
        sb.append("\"totalDistanceKm\":").append(String.format(Locale.US, "%.2f", res.getTotalDistanceKm())).append(",");
        sb.append("\"totalDistanceNm\":").append(String.format(Locale.US, "%.2f", res.getTotalDistanceNauticalMiles())).append(",");
        sb.append("\"totalTimeHours\":").append(String.format(Locale.US, "%.3f", res.getTotalTimeHours())).append(",");
        sb.append("\"formattedTime\":\"").append(res.getFormattedFlightTime()).append("\",");
        sb.append("\"totalFuelKg\":").append(String.format(Locale.US, "%.1f", res.getTotalFuelKg())).append(",");
        sb.append("\"nodesExplored\":").append(res.getNodesExplored()).append(",");
        sb.append("\"executionTimeMicros\":").append(res.getExecutionTimeMicros()).append(",");

        // Path Nodes
        sb.append("\"path\":[");
        List<Node> path = res.getPath();
        for (int i = 0; i < path.size(); i++) {
            Node n = path.get(i);
            sb.append(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"lat\":%.5f,\"lon\":%.5f,\"alt\":%.0f,\"type\":\"%s\"}",
                    n.getId(), JsonHelper.escape(n.getName()), n.getCoordinate().getLatitude(),
                    n.getCoordinate().getLongitude(), n.getCoordinate().getAltitudeMeters(), n.getType().name()));
            if (i < path.size() - 1) sb.append(",");
        }
        sb.append("],");

        // Explored Node IDs (for search visualization)
        sb.append("\"exploredNodes\":[");
        List<String> explored = res.getExploredNodeIds();
        if (explored != null) {
            for (int i = 0; i < explored.size(); i++) {
                sb.append("\"").append(explored.get(i)).append("\"");
                if (i < explored.size() - 1) sb.append(",");
            }
        }
        sb.append("]");

        sb.append("}");
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

package com.aerospace.flightpath;

import com.aerospace.flightpath.cli.CliRunner;
import com.aerospace.flightpath.server.FlightServer;

/**
 * Main application entry point for the Aerospace Flight Path & Route Optimizer.
 *
 * Usage:
 *   java com.aerospace.flightpath.Main          # Launches Web Operations Dashboard (Port 8080)
 *   java com.aerospace.flightpath.Main --cli    # Launches Terminal Interactive CLI Mode
 *   java com.aerospace.flightpath.Main --server # Launches Web Server Only
 */
public class Main {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        boolean cliMode = false;
        int port = DEFAULT_PORT;

        for (int i = 0; i < args.length; i++) {
            if ("--cli".equalsIgnoreCase(args[i])) {
                cliMode = true;
            } else if ("--port".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                try {
                    port = Integer.parseInt(args[++i]);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (cliMode) {
            CliRunner.run(args);
        } else {
            try {
                FlightServer server = new FlightServer(port);
                server.start();

                System.out.println("Press Ctrl+C to stop server, or use '--cli' flag to run in terminal mode.");

                // Keep process alive
                Thread.currentThread().join();
            } catch (Exception e) {
                System.err.println("Error starting server: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

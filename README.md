# 🚀 AstraNav — Aerospace Navigation & Pathfinding Engine

A high-performance aerospace simulator and optimal flight corridor planner implemented in **Java (Standard Edition)** paired with an interactive **HTML5 Radar Operations Dashboard**.

This project models global and regional airspace, oceanic waypoint tracks, jetstream winds aloft, and dynamic restricted no-fly zones using fundamental **Data Structures and Algorithms (DSA)** written from first principles.

---

## 📑 Table of Contents
1. [Core DSA Concepts Implemented](#-core-dsa-concepts-implemented)
2. [Aerospace Physics & Geodesy Models](#-aerospace-physics--geodesy-models)
3. [Architecture Overview](#-architecture-overview)
4. [Quick Start & Running the Project](#-quick-start--running-the-project)
5. [Interactive Web Radar Dashboard](#-interactive-web-radar-dashboard)
6. [Automated Test Suite & Benchmarks](#-automated-test-suite--benchmarks)

---

## 🧠 Core DSA Concepts Implemented

| DSA Concept | Implementation File | Key Operations & Complexity | Purpose in Flight Planning |
| :--- | :--- | :--- | :--- |
| **Custom Binary Min-Heap** | [`MinHeap.java`](file:///d:/Projects/AstraNav/src/com/aerospace/flightpath/dsa/heap/MinHeap.java) | `insert`: $O(\log N)$, `extractMin`: $O(\log N)$, `peek`: $O(1)$ | Core priority queue for candidate path ranking |
| **Indexed Min-Heap** | [`IndexedMinHeap.java`](file:///d:/Projects/AstraNav/src/com/aerospace/flightpath/dsa/heap/IndexedMinHeap.java) | `decreaseKey`: $O(\log N)$, `insertOrDecrease`: $O(\log N)$ | Classical textbook optimal Dijkstra's algorithm |
| **Adjacency List Graph** | [`AdjacencyListGraph.java`](file:///d:/Projects/AstraNav/src/com/aerospace/flightpath/dsa/graph/AdjacencyListGraph.java) | Space: $O(V + E)$, Neighbors: $O(\text{deg}(V))$ | Sparse global flight corridor graph traversal |
| **Adjacency Matrix Graph** | [`AdjacencyMatrixGraph.java`](file:///d:/Projects/AstraNav/src/com/aerospace/flightpath/dsa/graph/AdjacencyMatrixGraph.java) | Space: $O(V^2)$, Edge Lookup: $O(1)$ | Direct airway connectivity check & density analysis |
| **Dijkstra's Algorithm** | [`DijkstraRouter.java`](file:///d:/Projects/AstraNav/src/com/aerospace/flightpath/algorithm/DijkstraRouter.java) | Time: $O((V + E) \log V)$, Space: $O(V)$ | Single-source shortest path for distance/time/fuel |
| **A* Heuristic Search** | [`AStarRouter.java`](file:///d:/Projects/AstraNav/src/com/aerospace/flightpath/algorithm/AStarRouter.java) | $f(n) = g(n) + h(n)$ with Haversine heuristic | Guaranteed optimal route with 3x–5x fewer nodes explored |
| **Breadth-First Search (BFS)** | [`BfsDfsRouter.java`](file:///d:/Projects/AstraNav/src/com/aerospace/flightpath/algorithm/BfsDfsRouter.java) | Time: $O(V + E)$ | Minimum waypoint hops route (unweighted shortest path) |
| **Depth-First Search (DFS)** | [`BfsDfsRouter.java`](file:///d:/Projects/AstraNav/src/com/aerospace/flightpath/algorithm/BfsDfsRouter.java) | Backtracking + 3-Color Cycle Detection | Enumerate viable flight corridors and detect routing loops |
| **Yen's K-Shortest Paths** | [`YenKShortestPaths.java`](file:///d:/Projects/AstraNav/src/com/aerospace/flightpath/algorithm/YenKShortestPaths.java) | Time: $O(K \cdot V \cdot (E + V \log V))$ | Generates top $K$ loopless alternative flight paths |
| **Ray-Casting Geometry** | [`AirspaceGeometry.java`](file:///d:/Projects/AstraNav/src/com/aerospace/flightpath/dsa/geometry/AirspaceGeometry.java) | $O(V)$ Point-in-Polygon & Segment-Circle | Restricted military airspace & storm cell collision avoidance |
| **Divide & Conquer Sorters** | [`RouteSorter.java`](file:///d:/Projects/AstraNav/src/com/aerospace/flightpath/dsa/sort/RouteSorter.java) | QuickSort (median-of-3) & MergeSort: $O(N \log N)$ | Ranking flight route alternatives and diversion airports |

---

## ✈️ Aerospace Physics & Geodesy Models

### 1. Great-Circle Geodesic Distance (Haversine Formula)
Computes the true spherical distance between two aeronautical fixes $(\phi_1, \lambda_1)$ and $(\phi_2, \lambda_2)$:
$$a = \sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)$$
$$d = 2 R \cdot \operatorname{atan2}\left(\sqrt{a}, \sqrt{1-a}\right) \quad (\text{where } R = 6371\text{ km})$$

### 2. Initial Flight Track Bearing
Initial true heading angle $\theta$ in degrees $[0^\circ, 360^\circ)$:
$$\theta = \operatorname{atan2}\left(\sin(\Delta\lambda)\cos(\phi_2), \; \cos(\phi_1)\sin(\phi_2) - \sin(\phi_1)\cos(\phi_2)\cos(\Delta\lambda)\right)$$

### 3. Atmospheric Wind Triangle & Ground Speed
Given true airspeed $V_{\text{TAS}}$, flight course $\theta$, and wind aloft vector $(W_{\text{speed}}, W_{\text{dir}})$:
$$V_{\text{tailwind}} = W_{\text{speed}} \cos(W_{\text{to}} - \theta)$$
$$V_{\text{crosswind}} = W_{\text{speed}} \sin(W_{\text{to}} - \theta)$$
$$V_{\text{ground}} = \sqrt{V_{\text{TAS}}^2 - V_{\text{crosswind}}^2} + V_{\text{tailwind}}$$

Eastbound transoceanic flights along the North Atlantic Tracks (NAT) catch the jetstream (+95 knot tailwind), increasing ground speed to over 1050 km/h and significantly cutting fuel burn, while westbound flights detour along southern tracks to avoid severe headwinds.

### 4. Aerodynamic Fuel Burn Equation
$$\text{Fuel} = \left(\frac{d}{V_{\text{ground}}}\right) \cdot \dot{m}_{\text{base}} \cdot \left(1.0 + \frac{m_{\text{payload}}}{100\,000} \cdot 0.15\right) \cdot \kappa_{\text{turbulence}}$$

---

## 🏗️ Architecture Overview

```
d:\Projects\AstraNav/
├── src/com/aerospace/flightpath/
│   ├── model/                  # Domain models (Airport, Waypoint, FlightSegment, AircraftProfile, RestrictedZone)
│   ├── dsa/
│   │   ├── heap/               # Custom MinHeap & IndexedMinHeap (O(log N) decreaseKey)
│   │   ├── graph/              # Graph interface, AdjacencyListGraph, AdjacencyMatrixGraph
│   │   ├── sort/               # QuickSort (median-of-three) & MergeSort implementations
│   │   └── geometry/           # Ray-casting point-in-polygon & segment-circle intersection
│   ├── algorithm/              # DijkstraRouter, AStarRouter, BfsDfsRouter, YenKShortestPaths, DynamicRerouter
│   ├── data/                   # Global aeronautical dataset (airports, waypoints, jetstream, no-fly zones)
│   ├── server/                 # Built-in zero-dependency HTTP server & JSON serializer
│   ├── cli/                    # Interactive CLI runner with ASCII flight plans and benchmarks
│   ├── test/                   # Comprehensive 29-test automated DSA verification suite
│   └── Main.java               # Application entry point
├── web/                        # Modern Aerospace HUD Operations Dashboard (Canvas, JS, CSS)
├── build.bat / build.ps1       # One-click compilation scripts
└── run.bat / run.ps1           # Launch scripts
```

---

## ⚡ Quick Start & Running the Project

### Prerequisites
- Java SE JDK 17+ (JDK 25 LTS verified).
- No external dependencies, Maven, or Gradle required! Runs 100% out of the box.

### 1. Compile the Project
```cmd
.\build.bat
```
*(or via PowerShell: `powershell -ExecutionPolicy Bypass -File .\build.ps1`)*

### 2. Launch the Web Operations Dashboard (Port 8080)
```cmd
.\run.bat
```
Then open your browser at **`http://localhost:8080`**.

### 3. Run in Terminal CLI Mode
```cmd
.\run.bat --cli
```

---

## 🌐 Interactive Web Radar Dashboard Features

- **Interactive Flight Radar**: Full zoom and pan across global aeronautical coordinates.
- **Dynamic Waypoint Toggling**: Click any navigational waypoint to declare it `CLOSED (STORM/OUTAGE)` and observe instant dynamic detour rerouting in milliseconds.
- **Multi-Objective Optimization**:
  - `Shortest Distance`: Minimizes geodesic distance (km).
  - `Fastest Route`: Leverages real-time wind vectors and jet streams.
  - `Lowest Fuel`: Optimizes aircraft thrust, payload factor, and turbulence drag.
- **Aircraft Profile Customizer**: Commercial Jetliner (Boeing 787-9), Cargo Freighter (B747-8), Long-Range UAV Drone (MQ-9 Reaper), and Urban eVTOL.
- **Live Flight Simulation**: Supersonic jet silhouette animating smoothly along the selected flight corridor with particle afterburner trails and real-time heading angles.
- **Search Tree Visualizer**: Toggle visual markers showing the exact nodes evaluated by A* versus Dijkstra.

---

## 🧪 Automated Test Suite & Benchmarks

Run the complete 29-test automated test suite:
```cmd
java -cp out com.aerospace.flightpath.test.DsaTestSuite
```

### Benchmark Sample Results:
```
===============================================================================
                    DSA BENCHMARK: A* vs DIJKSTRA vs BFS                       
===============================================================================
Route Test Case            | A* (Nodes / µs)  | Dijkstra (Nodes / µs) | BFS (Nodes / µs)
-------------------------------------------------------------------------------
JFK -> LHR (North Atlantic)| 9 nodes / 140 µs | 28 nodes / 295 µs      | 24 nodes / 85 µs
JFK -> SFO (US Transcont.) | 11 nodes / 95 µs | 34 nodes / 220 µs      | 29 nodes / 65 µs
LHR -> DXB (Eurasia)       | 12 nodes / 115 µs| 36 nodes / 280 µs      | 31 nodes / 75 µs
-------------------------------------------------------------------------------
Observation: A* with the Great-Circle Haversine heuristic evaluates 3x to 5x fewer nodes
than Dijkstra, proving significant search space reduction while guaranteeing optimal cost!
```

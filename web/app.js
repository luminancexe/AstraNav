/**
 * AstraNav — Aerospace Navigation & Pathfinding Engine
 * Interactive Radar Map & Mission Operations Dashboard
 * Pairs with Java backend DSA Engine (Dijkstra, A*, BFS, DFS, Yen's K-Shortest).
 */

// State
let networkData = null;
let currentRouteResult = null;
let currentMultiRoutes = null;
let disabledNodes = new Set();
let aircraftProfiles = {};

// Viewport / Camera State (Pan & Zoom)
let panX = 0;
let panY = 0;
let zoom = 1.0;
let isDragging = false;
let dragStartX = 0;
let dragStartY = 0;

// Canvas & Context
let canvas = null;
let ctx = null;

// Animation State
let isSimulating = false;
let animProgress = 0.0; // 0 to 1 along the path
let animSpeed = 0.003;
let planeHeading = 0;
let planePos = { x: 0, y: 0 };
let trailParticles = [];

// DOM Elements
const selectOrigin = document.getElementById('select-origin');
const selectDest = document.getElementById('select-destination');
const selectAlgo = document.getElementById('select-algorithm');
const selectObjective = document.getElementById('select-objective');
const selectAircraft = document.getElementById('select-aircraft');
const btnCompute = document.getElementById('btn-compute');
const btnAnimate = document.getElementById('btn-animate');
const btnBenchmark = document.getElementById('btn-benchmark');
const btnReset = document.getElementById('btn-reset');
const tooltip = document.getElementById('tooltip');
const benchmarkModal = document.getElementById('benchmark-modal');
const btnCloseModal = document.getElementById('btn-close-modal');

// Layer Toggles
const chkAirways = document.getElementById('chk-airways');
const chkWaypoints = document.getElementById('chk-waypoints');
const chkWinds = document.getElementById('chk-winds');
const chkZones = document.getElementById('chk-zones');
const chkSearchTree = document.getElementById('chk-search-tree');

// Initialize
window.addEventListener('DOMContentLoaded', async () => {
  initCanvas();
  setupEventListeners();
  await loadNetworkData();
  centerViewOnTransatlantic();
  computeRoute();
  requestAnimationFrame(renderLoop);
});

function initCanvas() {
  canvas = document.getElementById('flight-canvas');
  ctx = canvas.getContext('2d');
  resizeCanvas();
  window.addEventListener('resize', resizeCanvas);
}

function resizeCanvas() {
  const rect = canvas.parentElement.getBoundingClientRect();
  canvas.width = rect.width;
  canvas.height = rect.height;
}

function centerViewOnTransatlantic() {
  // Center roughly between North America and Europe
  zoom = Math.min(canvas.width / 1400, canvas.height / 800) * 1.5;
  const centerLat = 46.0;
  const centerLon = -40.0;
  const screenCenter = latLonToScreenRaw(centerLat, centerLon);
  panX = canvas.width / 2 - screenCenter.x * zoom;
  panY = canvas.height / 2 - screenCenter.y * zoom;
}

// =========================================================================
// PROJECTION & COORDINATE CONVERSION (Equirectangular / Mercator Hybrid)
// =========================================================================

function latLonToWorld(lat, lon) {
  // Map world coordinate: lon [-180, 180] -> [0, 2000], lat [-80, 80] -> [1200, 0]
  const x = ((lon + 180.0) / 360.0) * 2400.0;
  // Standard Mercator-like latitude scaling
  const latRad = Math.max(-82, Math.min(82, lat)) * Math.PI / 180.0;
  const mercN = Math.log(Math.tan((Math.PI / 4.0) + (latRad / 2.0)));
  const y = 600.0 - (mercN * 180.0);
  return { x, y };
}

function worldToScreen(world) {
  return {
    x: world.x * zoom + panX,
    y: world.y * zoom + panY
  };
}

function screenToWorld(screen) {
  return {
    x: (screen.x - panX) / zoom,
    y: (screen.y - panY) / zoom
  };
}

function latLonToScreen(lat, lon) {
  return worldToScreen(latLonToWorld(lat, lon));
}

function latLonToScreenRaw(lat, lon) {
  return latLonToWorld(lat, lon);
}

function screenToLatLon(screenX, screenY) {
  const world = screenToWorld({ x: screenX, y: screenY });
  const lon = (world.x / 2400.0) * 360.0 - 180.0;
  const mercN = (600.0 - world.y) / 180.0;
  const latRad = 2.0 * (Math.atan(Math.exp(mercN)) - Math.PI / 4.0);
  const lat = latRad * 180.0 / Math.PI;
  return { lat, lon };
}

// =========================================================================
// DATA FETCHING & API INTERACTION
// =========================================================================

async function loadNetworkData() {
  try {
    const res = await fetch('/api/network');
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    networkData = await res.json();

    document.getElementById('header-node-count').textContent =
      (networkData.airports.length + networkData.waypoints.length).toString();
    document.getElementById('header-edge-count').textContent =
      networkData.segments.length.toString();

    populateDropdowns();
  } catch (err) {
    console.error('Failed to load flight network data:', err);
    document.getElementById('engine-status-text').textContent = 'OFFLINE';
    document.getElementById('backend-status').querySelector('.status-indicator').className = 'status-indicator';
  }
}

function populateDropdowns() {
  selectOrigin.innerHTML = '';
  selectDest.innerHTML = '';

  const sortedAirports = [...networkData.airports].sort((a, b) => a.id.localeCompare(b.id));

  sortedAirports.forEach(a => {
    const opt1 = document.createElement('option');
    opt1.value = a.id;
    opt1.textContent = `${a.id} - ${a.name} (${a.country})`;
    selectOrigin.appendChild(opt1);

    const opt2 = document.createElement('option');
    opt2.value = a.id;
    opt2.textContent = `${a.id} - ${a.name} (${a.country})`;
    selectDest.appendChild(opt2);
  });

  selectOrigin.value = 'JFK';
  selectDest.value = 'LHR';
}

async function computeRoute() {
  if (!networkData) return;

  const payload = {
    origin: selectOrigin.value,
    destination: selectDest.value,
    algorithm: selectAlgo.value,
    objective: selectObjective.value,
    aircraft: selectAircraft.value,
    avoidRestricted: true,
    disabledNodes: Array.from(disabledNodes)
  };

  btnCompute.disabled = true;
  btnCompute.innerHTML = '<span class="btn-icon">⏳</span> CALCULATING...';

  try {
    const res = await fetch('/api/route', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    btnCompute.disabled = false;
    btnCompute.innerHTML = '<span class="btn-icon">⚡</span> COMPUTE ROUTE';

    if (data.routes) {
      currentMultiRoutes = data.routes;
      currentRouteResult = data.routes[0];
    } else if (data.route) {
      currentMultiRoutes = null;
      currentRouteResult = data.route;
    }

    updateTelemetry(currentRouteResult);
    updateDsaAnalytics(currentRouteResult);
    updateFlightPlanTable(currentRouteResult);
    updateContingencyCard();

    // Reset simulation
    animProgress = 0.0;
    trailParticles = [];
  } catch (err) {
    console.error('Routing request error:', err);
    btnCompute.disabled = false;
    btnCompute.innerHTML = '<span class="btn-icon">⚡</span> COMPUTE ROUTE';
  }
}

// =========================================================================
// TELEMETRY & UI UPDATES
// =========================================================================

function updateTelemetry(res) {
  if (!res || !res.success) {
    document.getElementById('val-distance').innerHTML = `-- <small>km</small>`;
    document.getElementById('val-distance-nm').textContent = `-- NM`;
    document.getElementById('val-time').textContent = `--`;
    document.getElementById('val-time-hrs').textContent = `-- hrs`;
    document.getElementById('val-fuel').innerHTML = `-- <small>kg</small>`;
    document.getElementById('val-hops').textContent = `--`;
    return;
  }

  document.getElementById('val-distance').innerHTML = `${Math.round(res.totalDistanceKm).toLocaleString()} <small>km</small>`;
  document.getElementById('val-distance-nm').textContent = `${Math.round(res.totalDistanceNm).toLocaleString()} NM`;
  document.getElementById('val-time').textContent = res.formattedTime;
  document.getElementById('val-time-hrs').textContent = `${res.totalTimeHours.toFixed(2)} hrs`;
  document.getElementById('val-fuel').innerHTML = `${Math.round(res.totalFuelKg).toLocaleString()} <small>kg</small>`;

  const burnRate = res.totalTimeHours > 0 ? Math.round(res.totalFuelKg / res.totalTimeHours) : 0;
  document.getElementById('val-fuel-burnrate').textContent = `~${burnRate.toLocaleString()} kg/hr`;

  const hops = res.path.length > 1 ? res.path.length - 1 : 0;
  document.getElementById('val-hops').textContent = hops.toString();
  document.getElementById('val-hop-nodes').textContent = `${Math.max(0, hops - 1)} waypoints`;
}

function updateDsaAnalytics(res) {
  if (!res) return;

  document.getElementById('dsa-solver-name').textContent = res.algorithm;
  document.getElementById('dsa-nodes-count').textContent = `${res.nodesExplored} nodes visited`;
  document.getElementById('dsa-latency').textContent = `${res.executionTimeMicros} µs (${(res.executionTimeMicros / 1000).toFixed(2)} ms)`;

  const totalNodes = networkData ? (networkData.airports.length + networkData.waypoints.length) : 50;
  const prunedPct = Math.max(0, 100.0 - (res.nodesExplored / totalNodes * 100.0)).toFixed(1);
  document.getElementById('dsa-efficiency').textContent = `${prunedPct}% search space pruned`;

  // Time Complexity badge
  const algo = selectAlgo.value;
  let complexity = 'O((V + E) log V)';
  if (algo === 'BFS') complexity = 'O(V + E)';
  else if (algo === 'DFS') complexity = 'O(b^d) Backtracking';
  else if (algo === 'YEN_K') complexity = 'O(K * V * (E + V log V))';
  document.getElementById('dsa-time-complexity').textContent = complexity;

  // Explored tags
  const tagsContainer = document.getElementById('explored-tags-list');
  tagsContainer.innerHTML = '';
  if (res.exploredNodes && res.exploredNodes.length > 0) {
    res.exploredNodes.forEach((id, idx) => {
      const tag = document.createElement('span');
      tag.className = 'tag-node';
      tag.textContent = `#${idx + 1} ${id}`;
      tagsContainer.appendChild(tag);
    });
  } else {
    tagsContainer.innerHTML = '<span class="no-tags">Direct path optimal</span>';
  }
}

function updateFlightPlanTable(res) {
  const tbody = document.getElementById('flight-plan-tbody');
  tbody.innerHTML = '';

  if (!res || !res.success || !res.path || res.path.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="placeholder-row">${res ? res.message : 'No flight plan'}</td></tr>`;
    return;
  }

  const path = res.path;
  for (let i = 0; i < path.length; i++) {
    const node = path[i];
    const tr = document.createElement('tr');

    const legNum = i === 0 ? 'DEP' : (i === path.length - 1 ? 'ARR' : `WPT ${i}`);
    let course = '--°';
    let dist = '0 km';
    let speed = '~900 km/h';

    if (i > 0) {
      const prev = path[i - 1];
      const d = haversineKm(prev.lat, prev.lon, node.lat, node.lon);
      dist = `${Math.round(d)} km`;
      course = `${Math.round(bearingDeg(prev.lat, prev.lon, node.lat, node.lon))}°`;
    }

    tr.innerHTML = `
      <td><strong>${legNum}</strong></td>
      <td><span class="highlight">${node.id}</span></td>
      <td>${formatNodeType(node.type)}</td>
      <td>${course}</td>
      <td>${dist}</td>
      <td>${speed}</td>
    `;
    tbody.appendChild(tr);
  }
}

function updateContingencyCard() {
  const container = document.getElementById('contingency-body');
  if (disabledNodes.size === 0) {
    container.innerHTML = `<div class="status-ok">✔ Normal flight corridors nominal. No waypoint outages active.</div>`;
  } else {
    const list = Array.from(disabledNodes).join(', ');
    container.innerHTML = `
      <div class="status-alert">
        ⚠ CONTINGENCY ACTIVE: ${disabledNodes.size} waypoint(s) closed: <strong>${list}</strong>
      </div>
      <div style="margin-top: 4px; font-size: 11px; color: var(--text-dim);">
        Detour paths dynamically recalculated avoiding disabled airspace nodes.
      </div>
    `;
  }
}

function formatNodeType(type) {
  if (type === 'MAJOR_HUB') return 'HUB';
  if (type === 'REGIONAL_AIRPORT') return 'REGIONAL';
  if (type === 'OCEANIC_FIX') return 'OCEANIC';
  if (type === 'AIRWAY_WAYPOINT') return 'VOR/FIX';
  return type || 'FIX';
}

// =========================================================================
// RENDER LOOP & CANVAS DRAWING
// =========================================================================

function renderLoop() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  drawWorldBackground();
  drawGridLines();

  if (chkAirways.checked) drawAirways();
  if (chkWinds.checked) drawWindVectors();
  if (chkZones.checked) drawRestrictedZones();

  if (chkSearchTree.checked && currentRouteResult && currentRouteResult.exploredNodes) {
    drawSearchExplorationTree();
  }

  drawRoutes();

  if (chkWaypoints.checked) drawWaypoints();
  drawAirports();

  if (isSimulating && currentRouteResult && currentRouteResult.path && currentRouteResult.path.length > 1) {
    updateFlightSimulation();
    drawFlightSimulation();
  }

  requestAnimationFrame(renderLoop);
}

function drawWorldBackground() {
  // Atmospheric Space gradient
  const grad = ctx.createRadialGradient(canvas.width / 2, canvas.height / 2, 50, canvas.width / 2, canvas.height / 2, canvas.width);
  grad.addColorStop(0, '#0a101f');
  grad.addColorStop(1, '#03050a');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, canvas.width, canvas.height);
}

function drawGridLines() {
  ctx.strokeStyle = 'rgba(0, 243, 255, 0.05)';
  ctx.lineWidth = 1;

  // Parallels
  for (let lat = -60; lat <= 80; lat += 20) {
    ctx.beginPath();
    const p1 = latLonToScreen(lat, -180);
    const p2 = latLonToScreen(lat, 180);
    ctx.moveTo(p1.x, p1.y);
    ctx.lineTo(p2.x, p2.y);
    ctx.stroke();
  }

  // Meridians
  for (let lon = -180; lon <= 180; lon += 30) {
    ctx.beginPath();
    const p1 = latLonToScreen(-70, lon);
    const p2 = latLonToScreen(80, lon);
    ctx.moveTo(p1.x, p1.y);
    ctx.lineTo(p2.x, p2.y);
    ctx.stroke();
  }
}

function drawAirways() {
  if (!networkData || !networkData.segments) return;

  ctx.strokeStyle = 'rgba(0, 243, 255, 0.12)';
  ctx.lineWidth = 1.2;
  ctx.setLineDash([3, 5]);

  const nodesMap = getNodeMap();

  networkData.segments.forEach(seg => {
    const s = nodesMap[seg.source];
    const t = nodesMap[seg.target];
    if (!s || !t) return;

    const p1 = latLonToScreen(s.lat, s.lon);
    const p2 = latLonToScreen(t.lat, t.lon);

    ctx.beginPath();
    ctx.moveTo(p1.x, p1.y);
    ctx.lineTo(p2.x, p2.y);
    ctx.stroke();
  });

  ctx.setLineDash([]);
}

function drawWindVectors() {
  // Visual jetstream wind barb arrows across North Atlantic corridor
  const time = Date.now() * 0.001;
  const windStreamPoints = [
    { lat: 50, lon: -60 }, { lat: 52, lon: -45 }, { lat: 54, lon: -30 },
    { lat: 52, lon: -15 }, { lat: 48, lon: 0 }, { lat: 42, lon: -115 },
    { lat: 41, lon: -95 }, { lat: 41, lon: -75 }
  ];

  ctx.strokeStyle = 'rgba(0, 255, 157, 0.35)';
  ctx.fillStyle = 'rgba(0, 255, 157, 0.35)';
  ctx.lineWidth = 1.5;

  windStreamPoints.forEach((wpt, i) => {
    const p = latLonToScreen(wpt.lat, wpt.lon);
    const angleRad = Math.toRadians ? Math.toRadians(15) : (15 * Math.PI / 180);
    const len = 22 * zoom;

    const wave = Math.sin(time * 2 + i) * 3;
    const endX = p.x + Math.cos(angleRad) * len + wave;
    const endY = p.y + Math.sin(angleRad) * len;

    ctx.beginPath();
    ctx.moveTo(p.x, p.y);
    ctx.lineTo(endX, endY);
    ctx.stroke();

    // Arrowhead
    ctx.beginPath();
    ctx.arc(endX, endY, 2.5, 0, Math.PI * 2);
    ctx.fill();
  });
}

function drawRestrictedZones() {
  if (!networkData || !networkData.restrictedZones) return;

  networkData.restrictedZones.forEach(zone => {
    if (!zone.active) return;

    const p = latLonToScreen(zone.lat, zone.lon);
    // Radius in screen pixels
    const worldP = latLonToWorld(zone.lat, zone.lon);
    const edgeP = latLonToWorld(zone.lat, zone.lon + (zone.radiusKm / (111.32 * Math.cos(zone.lat * Math.PI / 180))));
    const radiusPx = Math.abs(edgeP.x - worldP.x) * zoom;

    // Outer Hazard Pulse Ring
    ctx.beginPath();
    ctx.arc(p.x, p.y, radiusPx, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(255, 0, 85, 0.15)';
    ctx.fill();

    ctx.strokeStyle = 'rgba(255, 0, 85, 0.8)';
    ctx.lineWidth = 2;
    ctx.setLineDash([6, 6]);
    ctx.stroke();
    ctx.setLineDash([]);

    // Warning Tag
    ctx.font = '9px "JetBrains Mono"';
    ctx.fillStyle = '#ff0055';
    ctx.textAlign = 'center';
    ctx.fillText(`⛔ RESTRICTED: ${zone.name.toUpperCase()}`, p.x, p.y - radiusPx - 6);
  });
}

function drawSearchExplorationTree() {
  // Visualizes the nodes evaluated by A* or Dijkstra
  const explored = currentRouteResult.exploredNodes;
  const nodesMap = getNodeMap();

  ctx.fillStyle = 'rgba(0, 243, 255, 0.4)';
  ctx.strokeStyle = 'rgba(0, 243, 255, 0.2)';
  ctx.lineWidth = 1;

  explored.forEach(id => {
    const node = nodesMap[id];
    if (!node) return;
    const p = latLonToScreen(node.lat, node.lon);

    ctx.beginPath();
    ctx.arc(p.x, p.y, 7 * Math.max(0.6, Math.min(1.4, zoom)), 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
  });
}

function drawRoutes() {
  if (currentMultiRoutes && currentMultiRoutes.length > 1) {
    const colors = ['#00f3ff', '#ffb703', '#c084fc', '#00ff9d'];
    // Draw alternate paths behind primary
    for (let k = currentMultiRoutes.length - 1; k >= 0; k--) {
      drawSinglePath(currentMultiRoutes[k].path, colors[k % colors.length], k === 0 ? 3.5 : 2.0, k !== 0);
    }
  } else if (currentRouteResult && currentRouteResult.success && currentRouteResult.path) {
    drawSinglePath(currentRouteResult.path, '#00f3ff', 3.5, false);
  }
}

function drawSinglePath(path, color, width, isDashed) {
  if (!path || path.length < 2) return;

  ctx.save();
  ctx.strokeStyle = color;
  ctx.lineWidth = width;
  if (isDashed) {
    ctx.setLineDash([6, 6]);
  } else {
    // Neon glow effect
    ctx.shadowColor = color;
    ctx.shadowBlur = 12;
  }

  ctx.beginPath();
  for (let i = 0; i < path.length; i++) {
    const p = latLonToScreen(path[i].lat, path[i].lon);
    if (i === 0) ctx.moveTo(p.x, p.y);
    else ctx.lineTo(p.x, p.y);
  }
  ctx.stroke();
  ctx.restore();
}

function drawWaypoints() {
  if (!networkData || !networkData.waypoints) return;

  networkData.waypoints.forEach(w => {
    const p = latLonToScreen(w.lat, w.lon);
    const isDisabled = disabledNodes.has(w.id);

    ctx.save();
    ctx.translate(p.x, p.y);

    if (isDisabled) {
      // Red X marker for closed waypoints
      ctx.strokeStyle = '#ff0055';
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      ctx.moveTo(-6, -6); ctx.lineTo(6, 6);
      ctx.moveTo(6, -6); ctx.lineTo(-6, 6);
      ctx.stroke();

      ctx.font = '9px "JetBrains Mono"';
      ctx.fillStyle = '#ff0055';
      ctx.fillText(`[CLOSED] ${w.id}`, 8, 3);
    } else {
      // Cyan Diamond Fix Marker
      ctx.fillStyle = 'rgba(0, 243, 255, 0.8)';
      ctx.strokeStyle = '#00f3ff';
      ctx.lineWidth = 1.2;

      ctx.beginPath();
      ctx.moveTo(0, -4);
      ctx.lineTo(4, 0);
      ctx.lineTo(0, 4);
      ctx.lineTo(-4, 0);
      ctx.closePath();
      ctx.fill();
      ctx.stroke();

      if (zoom > 1.2) {
        ctx.font = '8px "JetBrains Mono"';
        ctx.fillStyle = 'rgba(255, 255, 255, 0.6)';
        ctx.fillText(w.id, 6, 3);
      }
    }
    ctx.restore();
  });
}

function drawAirports() {
  if (!networkData || !networkData.airports) return;

  networkData.airports.forEach(a => {
    const p = latLonToScreen(a.lat, a.lon);
    const isOrig = selectOrigin.value === a.id;
    const isDest = selectDest.value === a.id;

    ctx.save();

    if (isOrig || isDest) {
      // Glowing target halo
      ctx.strokeStyle = isOrig ? '#00ff9d' : '#ffb703';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(p.x, p.y, 10, 0, Math.PI * 2);
      ctx.stroke();

      ctx.fillStyle = isOrig ? '#00ff9d' : '#ffb703';
    } else {
      ctx.fillStyle = '#ffffff';
    }

    // Airport center dot
    ctx.beginPath();
    ctx.arc(p.x, p.y, 4, 0, Math.PI * 2);
    ctx.fill();

    // IATA Label
    ctx.font = 'bold 11px "Orbitron"';
    ctx.fillStyle = isOrig ? '#00ff9d' : (isDest ? '#ffb703' : '#ffffff');
    ctx.shadowColor = 'rgba(0,0,0,0.8)';
    ctx.shadowBlur = 4;
    ctx.fillText(a.id, p.x + 8, p.y + 4);

    ctx.restore();
  });
}

// =========================================================================
// FLIGHT SIMULATION ANIMATION
// =========================================================================

function updateFlightSimulation() {
  const path = currentRouteResult.path;
  if (path.length < 2) return;

  animProgress += animSpeed;
  if (animProgress > 1.0) animProgress = 0.0;

  // Find segment index
  const totalLegs = path.length - 1;
  const currentLegFloat = animProgress * totalLegs;
  const legIdx = Math.min(totalLegs - 1, Math.floor(currentLegFloat));
  const legFraction = currentLegFloat - legIdx;

  const p1 = latLonToScreen(path[legIdx].lat, path[legIdx].lon);
  const p2 = latLonToScreen(path[legIdx + 1].lat, path[legIdx + 1].lon);

  planePos.x = p1.x + (p2.x - p1.x) * legFraction;
  planePos.y = p1.y + (p2.y - p1.y) * legFraction;

  planeHeading = Math.atan2(p2.y - p1.y, p2.x - p1.x);

  // Particle trail
  if (Math.random() < 0.6) {
    trailParticles.push({
      x: planePos.x,
      y: planePos.y,
      alpha: 1.0,
      size: 3
    });
  }
}

function drawFlightSimulation() {
  // Render particle afterburner trail
  for (let i = trailParticles.length - 1; i >= 0; i--) {
    const pt = trailParticles[i];
    pt.alpha -= 0.02;
    pt.size += 0.1;
    if (pt.alpha <= 0) {
      trailParticles.splice(i, 1);
      continue;
    }

    ctx.fillStyle = `rgba(0, 243, 255, ${pt.alpha * 0.7})`;
    ctx.beginPath();
    ctx.arc(pt.x, pt.y, pt.size, 0, Math.PI * 2);
    ctx.fill();
  }

  // Render Airplane Silhouette
  ctx.save();
  ctx.translate(planePos.x, planePos.y);
  ctx.rotate(planeHeading);

  // Supersonic jet icon
  ctx.fillStyle = '#ffffff';
  ctx.shadowColor = '#00f3ff';
  ctx.shadowBlur = 10;

  ctx.beginPath();
  ctx.moveTo(10, 0);       // Nose
  ctx.lineTo(-6, -8);     // Wing tip left
  ctx.lineTo(-4, -2);     // Wing inner left
  ctx.lineTo(-9, -5);     // Tail left
  ctx.lineTo(-7, 0);      // Engine nozzle
  ctx.lineTo(-9, 5);      // Tail right
  ctx.lineTo(-4, 2);      // Wing inner right
  ctx.lineTo(-6, 8);      // Wing tip right
  ctx.closePath();
  ctx.fill();

  ctx.restore();
}

// =========================================================================
// INTERACTION & EVENT HANDLERS
// =========================================================================

function setupEventListeners() {
  // Button Clicks
  btnCompute.addEventListener('click', computeRoute);
  btnAnimate.addEventListener('click', () => {
    isSimulating = !isSimulating;
    btnAnimate.innerHTML = isSimulating
      ? '<span class="btn-icon">⏸</span> PAUSE FLIGHT'
      : '<span class="btn-icon">▶</span> SIMULATE FLIGHT';
  });

  btnBenchmark.addEventListener('click', runBenchmark);
  btnCloseModal.addEventListener('click', () => benchmarkModal.close());

  btnReset.addEventListener('click', () => {
    disabledNodes.clear();
    updateContingencyCard();
    computeRoute();
  });

  // Select Changes
  selectOrigin.addEventListener('change', computeRoute);
  selectDest.addEventListener('change', computeRoute);
  selectAlgo.addEventListener('change', computeRoute);
  selectObjective.addEventListener('change', computeRoute);
  selectAircraft.addEventListener('change', computeRoute);

  // Zoom Controls
  document.getElementById('btn-zoom-in').addEventListener('click', () => { zoom *= 1.25; });
  document.getElementById('btn-zoom-out').addEventListener('click', () => { zoom /= 1.25; });
  document.getElementById('btn-zoom-reset').addEventListener('click', centerViewOnTransatlantic);

  // Pan & Drag on Canvas
  canvas.addEventListener('mousedown', (e) => {
    if (e.button === 0) {
      isDragging = true;
      dragStartX = e.clientX - panX;
      dragStartY = e.clientY - panY;
    }
  });

  window.addEventListener('mousemove', (e) => {
    if (isDragging) {
      panX = e.clientX - dragStartX;
      panY = e.clientY - dragStartY;
    }

    // Coordinates HUD
    const rect = canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;

    if (mouseX >= 0 && mouseX <= canvas.width && mouseY >= 0 && mouseY <= canvas.height) {
      const geo = screenToLatLon(mouseX, mouseY);
      const latStr = `${Math.abs(geo.lat).toFixed(2)}°${geo.lat >= 0 ? 'N' : 'S'}`;
      const lonStr = `${Math.abs(geo.lon).toFixed(2)}°${geo.lon >= 0 ? 'E' : 'W'}`;
      document.getElementById('mouse-coords').textContent = `${latStr}, ${lonStr}`;

      checkHoverNode(mouseX, mouseY, e.clientX, e.clientY);
    } else {
      tooltip.style.display = 'none';
    }
  });

  window.addEventListener('mouseup', () => { isDragging = false; });

  canvas.addEventListener('wheel', (e) => {
    e.preventDefault();
    const rect = canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;

    const zoomFactor = e.deltaY < 0 ? 1.15 : 0.87;
    const newZoom = Math.max(0.3, Math.min(8.0, zoom * zoomFactor));

    panX = mouseX - (mouseX - panX) * (newZoom / zoom);
    panY = mouseY - (mouseY - panY) * (newZoom / zoom);
    zoom = newZoom;
  });

  // Click on Waypoint to toggle outage / reroute
  canvas.addEventListener('click', (e) => {
    const rect = canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;

    const clickedNode = findNodeAtScreenPos(mouseX, mouseY);
    if (clickedNode) {
      if (disabledNodes.has(clickedNode.id)) {
        disabledNodes.delete(clickedNode.id);
      } else {
        disabledNodes.add(clickedNode.id);
      }
      updateContingencyCard();
      computeRoute();
    }
  });
}

function findNodeAtScreenPos(screenX, screenY) {
  if (!networkData) return null;
  const nodes = [...networkData.waypoints, ...networkData.airports];
  for (const n of nodes) {
    const p = latLonToScreen(n.lat, n.lon);
    const dist = Math.hypot(p.x - screenX, p.y - screenY);
    if (dist < 12) return n;
  }
  return null;
}

function checkHoverNode(screenX, screenY, clientX, clientY) {
  const node = findNodeAtScreenPos(screenX, screenY);
  if (node) {
    canvas.style.cursor = 'pointer';
    tooltip.style.display = 'block';
    tooltip.style.left = `${clientX + 14}px`;
    tooltip.style.top = `${clientY + 14}px`;

    const statusText = disabledNodes.has(node.id) ? '<span style="color: #ff0055;">● CLOSED (STORM)</span>' : '<span style="color: #00ff9d;">● OPERATIONAL</span>';

    tooltip.innerHTML = `
      <div style="font-weight: bold; color: var(--cyan-glow);">${node.name} [${node.id}]</div>
      <div>Type: ${node.type} | Status: ${statusText}</div>
      <div>Coords: ${node.lat.toFixed(4)}°, ${node.lon.toFixed(4)}°</div>
      <div style="font-size: 10px; color: var(--text-dim); margin-top: 4px;">Click to toggle availability & reroute</div>
    `;
  } else {
    canvas.style.cursor = isDragging ? 'grabbing' : 'crosshair';
    tooltip.style.display = 'none';
  }
}

async function runBenchmark() {
  benchmarkModal.showModal();
  const container = document.getElementById('benchmark-results-container');
  container.innerHTML = '<div class="loader-spinner">Benchmarking algorithms in Java...</div>';

  try {
    const res = await fetch('/api/benchmark');
    const data = await res.json();

    let html = `
      <table class="benchmark-table">
        <thead>
          <tr>
            <th>TEST ROUTE</th>
            <th>A* NODES / TIME</th>
            <th>DIJKSTRA NODES / TIME</th>
            <th>BFS NODES / TIME</th>
          </tr>
        </thead>
        <tbody>
    `;

    data.benchmarkResults.forEach(r => {
      html += `
        <tr>
          <td><strong>${r.test}</strong></td>
          <td style="color: var(--green-glow);"><strong>${r.aStar.nodes} nodes</strong> (${r.aStar.timeMicros} µs)</td>
          <td style="color: var(--cyan-glow);">${r.dijkstra.nodes} nodes (${r.dijkstra.timeMicros} µs)</td>
          <td style="color: var(--amber-warn);">${r.bfs.nodes} nodes (${r.bfs.timeMicros} µs)</td>
        </tr>
      `;
    });

    html += `
        </tbody>
      </table>
      <div style="margin-top: 14px; font-size: 12px; color: var(--text-dim); line-height: 1.6;">
        💡 <strong>Key DSA Takeaway:</strong> A* Search uses the admissible Great-Circle Haversine heuristic
        to guide its priority queue beam towards the destination, evaluating <strong>3x to 5x fewer nodes</strong>
        than Dijkstra's omnidirectional exploration while mathematically guaranteeing the identical optimal flight path!
      </div>
    `;

    container.innerHTML = html;
  } catch (err) {
    container.innerHTML = `<div style="color: #ff0055;">Failed to load benchmark: ${err.message}</div>`;
  }
}

// =========================================================================
// GEODESIC HELPERS
// =========================================================================

function getNodeMap() {
  const map = {};
  if (networkData) {
    networkData.airports.forEach(a => map[a.id] = a);
    networkData.waypoints.forEach(w => map[w.id] = w);
  }
  return map;
}

function haversineKm(lat1, lon1, lat2, lon2) {
  const R = 6371.0;
  const dLat = (lat2 - lat1) * Math.PI / 180.0;
  const dLon = (lon2 - lon1) * Math.PI / 180.0;
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180.0) * Math.cos(lat2 * Math.PI / 180.0) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function bearingDeg(lat1, lon1, lat2, lon2) {
  const lat1Rad = lat1 * Math.PI / 180.0;
  const lat2Rad = lat2 * Math.PI / 180.0;
  const dLon = (lon2 - lon1) * Math.PI / 180.0;
  const y = Math.sin(dLon) * Math.cos(lat2Rad);
  const x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon);
  return (Math.atan2(y, x) * 180.0 / Math.PI + 360.0) % 360.0;
}

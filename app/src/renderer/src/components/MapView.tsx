import { useEffect, useRef, useState } from "react";
import maplibregl from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";

const SOURCE_ID = "ac-pts";
const LAYER_ID  = "ac-circles";

type PointGeoJSON = {
  type: "FeatureCollection";
  features: Array<{
    type: "Feature";
    geometry: { type: "Point"; coordinates: [number, number] };
    properties: Record<string, string>;
  }>;
};

function buildGeoJSON(
  headers: string[], rows: string[][], xCol: string, yCol: string
): PointGeoJSON {
  const xi = headers.indexOf(xCol);
  const yi = headers.indexOf(yCol);
  const features: PointGeoJSON["features"] = [];
  for (const row of rows) {
    const lng = parseFloat(row[xi]);
    const lat = parseFloat(row[yi]);
    if (!isNaN(lng) && !isNaN(lat) && lng >= -180 && lng <= 180 && lat >= -90 && lat <= 90) {
      features.push({
        type: "Feature",
        geometry: { type: "Point", coordinates: [lng, lat] },
        properties: Object.fromEntries(headers.map((h, i) => [h, row[i] ?? ""])),
      });
    }
  }
  return { type: "FeatureCollection", features };
}

export function MapView({ headers, rows }: { headers: string[]; rows: string[][] }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef       = useRef<maplibregl.Map | null>(null);
  const [xCol, setXCol] = useState(headers[0] ?? "");
  const [yCol, setYCol] = useState(headers.length > 1 ? headers[1] : headers[0] ?? "");
  const [pointCount, setPointCount] = useState(0);
  const [mapReady, setMapReady] = useState(false);

  // ── Initialize map once ──────────────────────────────────────────────────────
  useEffect(() => {
    if (!containerRef.current) return;

    const map = new maplibregl.Map({
      container: containerRef.current,
      style: {
        version: 8,
        sources: {
          osm: {
            type: "raster",
            tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
            tileSize: 256,
            attribution: "© <a href='https://www.openstreetmap.org/copyright'>OpenStreetMap</a>",
          },
        },
        layers: [{ id: "osm-tiles", type: "raster", source: "osm" }],
      },
      center: [127.0, 37.5],
      zoom: 5,
      // preserveDrawingBuffer prevents WebGL canvas from going black during React re-renders
      preserveDrawingBuffer: true,
    } as unknown as maplibregl.MapOptions);

    map.addControl(new maplibregl.NavigationControl({ visualizePitch: false }), "top-right");
    map.addControl(new maplibregl.ScaleControl(), "bottom-left");
    map.once("load", () => setMapReady(true));
    mapRef.current = map;

    return () => {
      setMapReady(false);
      map.remove();
      mapRef.current = null;
    };
  }, []);

  // ── Update point layer when columns / data change ────────────────────────────
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !mapReady) return;

    const geoJSON = buildGeoJSON(headers, rows, xCol, yCol);
    setPointCount(geoJSON.features.length);

    const src = map.getSource(SOURCE_ID) as maplibregl.GeoJSONSource | undefined;
    if (src) {
      src.setData(geoJSON as Parameters<typeof src.setData>[0]);
    } else {
      map.addSource(SOURCE_ID, { type: "geojson", data: geoJSON as maplibregl.GeoJSONSourceSpecification["data"] });
      map.addLayer({
        id: LAYER_ID,
        type: "circle",
        source: SOURCE_ID,
        paint: {
          "circle-radius": ["interpolate", ["linear"], ["zoom"], 4, 3, 12, 8],
          "circle-color": "#3b82f6",
          "circle-stroke-color": "#fff",
          "circle-stroke-width": 1,
          "circle-opacity": 0.85,
        },
      });
    }

    if (geoJSON.features.length > 0) {
      const bounds = new maplibregl.LngLatBounds();
      for (const f of geoJSON.features) {
        bounds.extend(f.geometry.coordinates);
      }
      map.fitBounds(bounds, { padding: 60, maxZoom: 14, duration: 500 });
    }
  }, [mapReady, headers, rows, xCol, yCol]);

  // ── Click popup ──────────────────────────────────────────────────────────────
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !mapReady) return;

    let popup: maplibregl.Popup | null = null;

    const handleClick = (e: maplibregl.MapMouseEvent & { features?: maplibregl.MapGeoJSONFeature[] }) => {
      const feat = e.features?.[0];
      if (!feat || feat.geometry.type !== "Point") return;
      const [lng, lat] = feat.geometry.coordinates as [number, number];
      const html = Object.entries(feat.properties ?? {})
        .map(([k, v]) => `<div class="mv-pop-row"><b>${k}</b><span>${v}</span></div>`)
        .join("");
      popup?.remove();
      popup = new maplibregl.Popup({ maxWidth: "260px", closeButton: true })
        .setLngLat([lng, lat])
        .setHTML(`<div class="mv-popup">${html}</div>`)
        .addTo(map);
    };

    const handleEnter = () => { map.getCanvas().style.cursor = "pointer"; };
    const handleLeave = () => { map.getCanvas().style.cursor = "";        };

    map.on("click",      LAYER_ID, handleClick);
    map.on("mouseenter", LAYER_ID, handleEnter);
    map.on("mouseleave", LAYER_ID, handleLeave);

    return () => {
      map.off("click",      LAYER_ID, handleClick);
      map.off("mouseenter", LAYER_ID, handleEnter);
      map.off("mouseleave", LAYER_ID, handleLeave);
      popup?.remove();
    };
  }, [mapReady]);

  async function handleExportPNG() {
    const map = mapRef.current;
    if (!map) return;
    // Wait for the next render frame so preserveDrawingBuffer captures current content
    await new Promise<void>((resolve) => {
      map.once("render", resolve);
      map.triggerRepaint();
    });
    const base64 = map.getCanvas().toDataURL("image/png").split(",")[1];
    await window.aidclaude.export.saveBinary(
      "map.png",
      [{ name: "PNG 이미지", extensions: ["png"] }],
      base64
    );
  }

  return (
    <div className="mv-root">
      {/* Toolbar */}
      <div className="mv-toolbar">
        <label className="mv-label">
          경도(X)
          <select className="mv-sel" value={xCol} onChange={(e) => setXCol(e.target.value)}>
            {headers.map((h) => <option key={h} value={h}>{h}</option>)}
          </select>
        </label>
        <label className="mv-label">
          위도(Y)
          <select className="mv-sel" value={yCol} onChange={(e) => setYCol(e.target.value)}>
            {headers.map((h) => <option key={h} value={h}>{h}</option>)}
          </select>
        </label>
        <span className={`mv-count${pointCount === 0 ? " mv-count-zero" : ""}`}>
          {pointCount > 0 ? `${pointCount.toLocaleString()}개 포인트` : "유효한 좌표 없음"}
        </span>
        <button
          type="button"
          className="mv-export-btn"
          onClick={handleExportPNG}
          disabled={!mapReady}
          title="지도를 PNG로 저장"
        >
          ↓ PNG
        </button>
      </div>
      {/* Map container */}
      <div ref={containerRef} className="mv-map" />
    </div>
  );
}

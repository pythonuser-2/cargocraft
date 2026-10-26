package com.cargocraft;

import com.cargocraft.db.CargoDatabaseManager;
import com.cargocraft.engine.SafetyEngine;
import com.cargocraft.engine.SafetyEngine.SafetyReport;
import com.cargocraft.engine.SpatialPackingSimulator;
import com.cargocraft.engine.SpatialPackingSimulator.SimulationResult;
import com.cargocraft.engine.SpatialPackingSimulator.VoxelPlacement;
import com.cargocraft.model.CargoItem;
import com.cargocraft.model.Container;
import com.cargocraft.model.Material;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CargoHttpServer {
    private static final int PORT = 8080;
    private static final CargoDatabaseManager db = new CargoDatabaseManager();
    private static final Container defaultContainer = new Container(1, "ISO 20ft", 28000.0, 5.9, 2.35, 2.39);

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/cargo", new CargoApiHandler());
        server.setExecutor(null);
        System.out.println("CargoCraft REST API active at http://localhost:" + PORT + "/api/cargo");
        server.start();
    }

    static class CargoApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleGet(exchange);
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handlePost(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGet(HttpExchange exchange) throws IOException {
            List<CargoItem> items = db.fetchCargoLogs();
            SimulationResult pack = SpatialPackingSimulator.packItems(defaultContainer, items);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"containerVolume\":").append(defaultContainer.getVolumeEnvelope()).append(",");
            json.append("\"utilization\":").append(String.format("%.1f", pack.volumeUtilizationPercentage)).append(",");
            json.append("\"totalWeight\":").append(String.format("%.1f", pack.totalWeightPacked)).append(",");
            json.append("\"items\":[");

            for (int i = 0; i < pack.placedItems.size(); i++) {
                VoxelPlacement vp = pack.placedItems.get(i);
                SafetyReport rep = SafetyEngine.evaluateFreight(vp.item, defaultContainer, SafetyEngine.G_SEA);

                json.append("{")
                    .append("\"id\":").append(vp.item.getItemId()).append(",")
                    .append("\"name\":\"").append(vp.item.getItemName()).append("\",")
                    .append("\"length\":").append(vp.item.getLength()).append(",")
                    .append("\"width\":").append(vp.item.getWidth()).append(",")
                    .append("\"height\":").append(vp.item.getHeight()).append(",")
                    .append("\"weight\":").append(vp.item.getWeight()).append(",")
                    .append("\"x\":").append(vp.posX).append(",")
                    .append("\"y\":").append(vp.posY).append(",")
                    .append("\"z\":").append(vp.posZ).append(",")
                    .append("\"stressMpa\":").append(String.format("%.2f", rep.maxVonMisesStress)).append(",")
                    .append("\"safetyFactor\":").append(String.format("%.2f", rep.safetyFactor)).append(",")
                    .append("\"status\":\"").append(rep.isStructuralSafe ? "SAFE" : "ALERT").append("\"")
                    .append("}");

                if (i < pack.placedItems.size() - 1) json.append(",");
            }
            json.append("]}");

            byte[] responseBytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }

        private void handlePost(HttpExchange exchange) throws IOException {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                body.append(line);
            }

            String payload = body.toString();
            String name = extractString(payload, "itemName", "Cargo Item");
            double l = extractDouble(payload, "length", 1.0);
            double w = extractDouble(payload, "width", 1.0);
            double h = extractDouble(payload, "height", 1.0);
            double wt = extractDouble(payload, "weight", 500.0);
            String geom = extractString(payload, "geometryProfile", "Rectangular Cuboid");
            String density = extractString(payload, "densityClass", "Standard");
            String matName = extractString(payload, "material", "Steel");

            CargoItem item = new CargoItem(0, name, l, w, h, wt, geom, density, Material.fromName(matName));
            boolean saved = db.saveCargoLog(item);

            String response = "{\"success\":" + saved + ",\"assignedId\":" + item.getItemId() + "}";
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }

        private String extractString(String json, String key, String defaultVal) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return defaultVal;
            start += pattern.length();
            int end = json.indexOf("\"", start);
            return end == -1 ? defaultVal : json.substring(start, end);
        }

        private double extractDouble(String json, String key, double defaultVal) {
            String pattern = "\"" + key + "\":";
            int start = json.indexOf(pattern);
            if (start == -1) return defaultVal;
            start += pattern.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            try {
                return Double.parseDouble(json.substring(start, end).replace("\"", "").trim());
            } catch (Exception e) {
                return defaultVal;
            }
        }
    }
}
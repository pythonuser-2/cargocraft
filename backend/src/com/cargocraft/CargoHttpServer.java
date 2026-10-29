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
import java.util.Locale;

public class CargoHttpServer {
    private static final int PORT = 8080;
    private static final CargoDatabaseManager db = new CargoDatabaseManager();
    
    // Dynamic container runtime state captured from frontend inputs
    private static String dynamicContainerName = "ISO 20ft";
    private static double dynamicMaxWeight = 28000.0;
    private static double dynamicLength = 5.9;
    private static double dynamicWidth = 2.35;
    private static double dynamicHeight = 2.39;

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
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePost(exchange);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleDelete(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGet(HttpExchange exchange) throws IOException {
            List<CargoItem> items = db.fetchCargoLogs();
            Container activeContainer = new Container(1, dynamicContainerName, dynamicMaxWeight, dynamicLength, dynamicWidth, dynamicHeight);
            SimulationResult pack = SpatialPackingSimulator.packItems(activeContainer, items);
            double totalCargoWeight = items.stream().mapToDouble(CargoItem::getWeight).sum();

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"containerName\":\"").append(activeContainer.getContainerName()).append("\",");
            json.append("\"containerMaxWeight\":").append(activeContainer.getMaxWeightLimit()).append(",");
            json.append("\"containerVolume\":").append(activeContainer.getVolumeEnvelope()).append(",");
            json.append("\"utilization\":").append(String.format(Locale.US, "%.1f", pack.volumeUtilizationPercentage)).append(",");
            json.append("\"totalWeight\":").append(String.format(Locale.US, "%.1f", totalCargoWeight)).append(",");
            json.append("\"items\":[");

            boolean first = true;

            for (int i = 0; i < pack.placedItems.size(); i++) {
                VoxelPlacement vp = pack.placedItems.get(i);
                SafetyReport rep = SafetyEngine.evaluateFreight(vp.item, activeContainer, SafetyEngine.G_SEA);

                if (!first) json.append(",");
                first = false;

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
                    .append("\"stressMpa\":").append(String.format(Locale.US, "%.2f", rep.maxVonMisesStress)).append(",")
                    .append("\"safetyFactor\":").append(String.format(Locale.US, "%.2f", rep.safetyFactor)).append(",")
                    .append("\"status\":\"SAFE\",")
                    .append("\"reason\":\"Successfully placed in spatial grid\"")
                    .append("}");
            }

            for (int i = 0; i < pack.unplacedItems.size(); i++) {
                CargoItem item = pack.unplacedItems.get(i);
                
                String reason = "Spatial capacity / volume overflow";
                if (item.getWeight() > activeContainer.getMaxWeightLimit()) {
                    reason = "WEIGHT ERROR: Item weight (" + item.getWeight() + " kg) exceeds container max limit (" + activeContainer.getMaxWeightLimit() + " kg)";
                } else if (item.getLength() > activeContainer.getLength() || 
                           item.getWidth() > activeContainer.getWidth() || 
                           item.getHeight() > activeContainer.getHeight()) {
                    reason = "SPACE ERROR: Dimensions (" + item.getLength() + "m × " + item.getWidth() + "m × " + item.getHeight() + "m) exceed container physical envelope limits (" + activeContainer.getLength() + "m × " + activeContainer.getWidth() + "m × " + activeContainer.getHeight() + "m)";
                }

                if (!first) json.append(",");
                first = false;

                json.append("{")
                    .append("\"id\":").append(item.getItemId()).append(",")
                    .append("\"name\":\"").append(item.getItemName()).append(" (EXCLUDED)\",")
                    .append("\"length\":").append(item.getLength()).append(",")
                    .append("\"width\":").append(item.getWidth()).append(",")
                    .append("\"height\":").append(item.getHeight()).append(",")
                    .append("\"weight\":").append(item.getWeight()).append(",")
                    .append("\"x\":0.0,\"y\":0.0,\"z\":0.0,")
                    .append("\"stressMpa\":0.0,")
                    .append("\"safetyFactor\":0.0,")
                    .append("\"status\":\"EXCLUDED\",")
                    .append("\"reason\":\"").append(reason).append("\"")
                    .append("}");
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
            BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                body.append(line);
            }

            String payload = body.toString();
            
            // Extract dynamic container specifications sent from input.html
            dynamicContainerName = extractString(payload, "containerName", "ISO 20ft");
            dynamicMaxWeight = extractDouble(payload, "maxWeight", 28000.0);
            dynamicLength = extractDouble(payload, "containerLength", 5.9);
            dynamicWidth = extractDouble(payload, "containerWidth", 2.35);
            dynamicHeight = extractDouble(payload, "containerHeight", 2.39);

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

        private void handleDelete(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            boolean success = false;

            // Check if a specific ID was passed, e.g., /api/cargo/5
            if (path.matches(".*/api/cargo/\\d+$")) {
                String idStr = path.substring(path.lastIndexOf('/') + 1);
                try {
                    int itemId = Integer.parseInt(idStr);
                    success = db.deleteCargoLog(itemId);
                } catch (NumberFormatException e) {
                    success = false;
                }
            } else {
                // Otherwise clear all logs
                success = db.clearAllLogs();
            }

            String response = "{\"success\":" + success + "}";
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(success ? 200 : 400, responseBytes.length);
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
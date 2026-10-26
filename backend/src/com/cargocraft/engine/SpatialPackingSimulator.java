package com.cargocraft.engine;

import com.cargocraft.model.CargoItem;
import com.cargocraft.model.Container;

import java.util.ArrayList;
import java.util.List;

public class SpatialPackingSimulator {

    public static class VoxelPlacement {
        public CargoItem item;
        public double posX, posY, posZ; // Origin coordinates inside container

        public VoxelPlacement(CargoItem item, double posX, double posY, double posZ) {
            this.item = item;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
        }

        public String getCoordsSummary() {
            return String.format("[%s] Position: (X: %.2fm, Y: %.2fm, Z: %.2fm)",
                item.getItemName(), posX, posY, posZ);
        }
    }

    public static class SimulationResult {
        public List<VoxelPlacement> placedItems = new ArrayList<>();
        public List<CargoItem> unplacedItems = new ArrayList<>();
        public double totalContainerVolume;
        public double usedVolume;
        public double volumeUtilizationPercentage;
        public double totalWeightPacked;

        @Override
        public String toString() {
            return String.format(
                "--- 3D Spatial Packing Simulation Result ---\n" +
                "Items Successfully Packed: %d\n" +
                "Items Unplaced (Overflow): %d\n" +
                "Container Volume: %.2f m³ | Utilized: %.2f m³ (%.1f%%)\n" +
                "Total Mass In Container: %.2f kg",
                placedItems.size(), unplacedItems.size(),
                totalContainerVolume, usedVolume, volumeUtilizationPercentage,
                totalWeightPacked
            );
        }
    }

    public static SimulationResult packItems(Container container, List<CargoItem> items) {
        SimulationResult result = new SimulationResult();
        result.totalContainerVolume = container.getVolumeEnvelope();

        // Sort items by density/weight descending (heaviest items at bottom foundation)
        List<CargoItem> queue = new ArrayList<>(items);
        queue.sort((a, b) -> Double.compare(b.getWeight(), a.getWeight()));

        double currentX = 0.0;
        double currentY = 0.0;
        double currentZ = 0.0;
        double maxRowY = 0.0;
        double maxLayerZ = 0.0;

        for (CargoItem item : queue) {
            // Check weight capacity
            if (result.totalWeightPacked + item.getWeight() > container.getMaxWeightLimit()) {
                result.unplacedItems.add(item);
                continue;
            }

            // Check if item fits along length (X)
            if (currentX + item.getLength() > container.getLength()) {
                // Move to next line in width (Y)
                currentX = 0.0;
                currentY += maxRowY;
                maxRowY = 0.0;
            }

            // Check if item fits along width (Y)
            if (currentY + item.getWidth() > container.getWidth()) {
                // Move to next vertical tier (Z)
                currentX = 0.0;
                currentY = 0.0;
                maxRowY = 0.0;
                currentZ += maxLayerZ;
                maxLayerZ = 0.0;
            }

            // Check if item fits along height (Z)
            if (currentZ + item.getHeight() > container.getHeight()) {
                // Volume envelope exceeded
                result.unplacedItems.add(item);
                continue;
            }

            // Item placed successfully
            result.placedItems.add(new VoxelPlacement(item, currentX, currentY, currentZ));
            result.totalWeightPacked += item.getWeight();
            result.usedVolume += item.calculateVolume();

            // Track boundaries
            maxRowY = Math.max(maxRowY, item.getWidth());
            maxLayerZ = Math.max(maxLayerZ, item.getHeight());
            currentX += item.getLength();
        }

        result.volumeUtilizationPercentage = (result.usedVolume / result.totalContainerVolume) * 100.0;
        return result;
    }
}
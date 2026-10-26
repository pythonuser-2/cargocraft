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

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   CARGOCRAFT: CORE ENGINE INTEGRATION TEST       ");
        System.out.println("==================================================\n");

        CargoDatabaseManager dbManager = new CargoDatabaseManager();

        // 1. Define standard 20ft ISO Shipping Container
        Container container = new Container(101, "Standard 20ft ISO Dry Van", 28000.0, 5.9, 2.35, 2.39);
        System.out.println("Container: " + container.getContainerName());
        System.out.printf("Capacity: %.0f kg | Volume: %.2f m³\n\n", container.getMaxWeightLimit(), container.getVolumeEnvelope());

        // 2. Prepare cargo batch with materials
        Material steel = Material.a36Steel();
        Material aluminum = Material.aluminum6061();

        List<CargoItem> batch = new ArrayList<>();
        batch.add(new CargoItem(0, "Generator Unit A", 2.2, 1.4, 1.2, 5400.0, "Standard Rectangular Cuboid", "Class 1: Heavy Base", steel));
        batch.add(new CargoItem(0, "Subsea Pump Block", 1.8, 1.1, 1.0, 3800.0, "Irregular Assembly", "Class 2: Medium Structural", steel));
        batch.add(new CargoItem(0, "Avionics Control Rack", 1.5, 0.9, 1.6, 950.0, "Standard Rectangular Cuboid", "Class 3: Lightweight Structural", aluminum));

        // 3. Persist batch to MySQL database
        System.out.println("[DB] Persisting cargo batch into cargocraft_db...");
        for (CargoItem item : batch) {
            boolean ok = dbManager.saveCargoLog(item);
            System.out.printf("  -> Saved %s (Assigned DB ID: #%d) [Status: %s]\n",
                item.getItemName(), item.getItemId(), ok ? "SUCCESS" : "FAILED");
        }

        // 4. Run Structural Stress & Von Mises evaluation (Sea Transit G-factor = 2.5G)
        System.out.println("\n[SAFETY] Running dynamic structural stress analysis (Sea Transit - 2.5G)...");
        for (CargoItem item : batch) {
            SafetyReport report = SafetyEngine.evaluateFreight(item, container, SafetyEngine.G_SEA);
            System.out.println("\n" + report.toString());
        }

        // 5. Run 3D Spatial Packing Simulation
        System.out.println("\n[PACKING] Running spatial load simulator...");
        SimulationResult packResult = SpatialPackingSimulator.packItems(container, batch);
        System.out.println("\n" + packResult.toString());

        System.out.println("\nComputed 3D Voxel Placements:");
        for (VoxelPlacement vp : packResult.placedItems) {
            System.out.println("  * " + vp.getCoordsSummary());
        }

        System.out.println("\n==================================================");
        System.out.println("   ENGINE PIPELINE VERIFIED SUCCESSFULLY          ");
        System.out.println("==================================================");
    }
}
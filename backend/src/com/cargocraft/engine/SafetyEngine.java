package com.cargocraft.engine;

import com.cargocraft.model.CargoItem;
import com.cargocraft.model.Container;

public class SafetyEngine {

    // Dynamic G-force factors based on transit mode
    public static final double G_ROAD = 1.8;
    public static final double G_RAIL = 2.0;
    public static final double G_SEA  = 2.5;

    public static class SafetyReport {
        public double totalMass;
        public double totalDynamicLoad; // Newtons
        public double contactArea;      // m²
        public double surfacePressure;   // kPa
        public double maxVonMisesStress; // MPa
        public double safetyFactor;
        public boolean isCapacitySafe;
        public boolean isStructuralSafe;
        public String riskEvaluation;

        @Override
        public String toString() {
            return String.format(
                "--- Structural Safety & Stress Report ---\n" +
                "Total Mass: %.2f kg\n" +
                "Dynamic Load: %.2f kN\n" +
                "Contact Area: %.2f m²\n" +
                "Surface Pressure: %.2f kPa\n" +
                "Estimated Von Mises Stress: %.2f MPa\n" +
                "Safety Factor (Yield / Stress): %.2f\n" +
                "Container Weight Limit Valid: %s\n" +
                "Structural Integrity Status: %s\n" +
                "Risk Evaluation: %s",
                totalMass, totalDynamicLoad / 1000.0, contactArea, surfacePressure,
                maxVonMisesStress, safetyFactor,
                isCapacitySafe ? "PASSED" : "EXCEEDED",
                isStructuralSafe ? "SAFE" : "STRUCTURAL FAILURE RISK",
                riskEvaluation
            );
        }
    }

    public static SafetyReport evaluateFreight(CargoItem item, Container container, double transitGForce) {
        SafetyReport report = new SafetyReport();
        report.totalMass = item.getWeight();
        report.isCapacitySafe = container.validateCapacity(report.totalMass);

        // 1. Dynamic Load under transit acceleration: F = m * (g * G_factor)
        double g = 9.81;
        report.totalDynamicLoad = report.totalMass * g * transitGForce;

        // 2. Base Contact Area (Length x Width)
        report.contactArea = Math.max(item.getLength() * item.getWidth(), 0.001);

        // 3. Surface Pressure (P = F / A) in Pascals -> kPa
        double pressurePa = report.totalDynamicLoad / report.contactArea;
        report.surfacePressure = pressurePa / 1000.0;

        // 4. Simulated Von Mises stress concentrated at base interface (MPa)
        // Profile stress multipliers: Rectangular (1.0x), Cylindrical/Irregular (1.35x - 1.6x)
        double stressConcentration = item.getGeometryProfile().toLowerCase().contains("cuboid") ? 1.05 : 1.45;
        report.maxVonMisesStress = (pressurePa * stressConcentration) / 1_000_000.0;

        // 5. Safety Factor calculation against Material Yield Strength
        double yield = item.getMaterial().getYieldStrength();
        report.safetyFactor = report.maxVonMisesStress > 0 ? (yield / report.maxVonMisesStress) : 999.0;
        report.isStructuralSafe = report.safetyFactor >= 1.5;

        // 6. Qualitative Risk Evaluation
        if (report.safetyFactor < 1.0) {
            report.riskEvaluation = "CRITICAL: Plastic deformation or floor failure imminent under dynamic load.";
        } else if (report.safetyFactor < 1.5) {
            report.riskEvaluation = "WARNING: Marginal safety threshold. Additional dunnage/load-spreading mats required.";
        } else {
            report.riskEvaluation = "OPTIMAL: Cargo stresses well within material elastic limits.";
        }

        return report;
    }
}
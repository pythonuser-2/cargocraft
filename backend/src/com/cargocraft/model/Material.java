package com.cargocraft.model;

public class Material {
    private int materialId;
    private String materialName;
    private double yieldStrength; // in MPa
    private double density;       // in kg/m^3

    public Material(int materialId, String materialName, double yieldStrength, double density) {
        this.materialId = materialId;
        this.materialName = materialName;
        this.yieldStrength = yieldStrength;
        this.density = density;
    }

    public static Material a36Steel() {
        return new Material(1, "Structural Carbon Steel (A36)", 250.0, 7850.0);
    }

    public static Material aluminum6061() {
        return new Material(2, "6061-T6 Aluminum", 276.0, 2700.0);
    }

    public static Material fromName(String name) {
        if (name != null && name.toLowerCase().contains("aluminum")) {
            return aluminum6061();
        }
        return a36Steel();
    }

    public String getMechanicalProperties() {
        return String.format("%s [Yield: %.1f MPa, Density: %.1f kg/m³]", 
            materialName, yieldStrength, density);
    }

    public int getMaterialId() { return materialId; }
    public String getMaterialName() { return materialName; }
    public double getYieldStrength() { return yieldStrength; }
    public double getDensity() { return density; }
}
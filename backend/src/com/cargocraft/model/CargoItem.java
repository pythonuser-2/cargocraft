package com.cargocraft.model;

public class CargoItem {
    private int itemId;
    private String itemName;
    private double length;
    private double width;
    private double height;
    private double weight;
    private String geometryProfile;
    private String densityClass;
    private Material material;

    public CargoItem(int itemId, String itemName, double length, double width, double height, 
                     double weight, String geometryProfile, String densityClass, Material material) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.length = length;
        this.width = width;
        this.height = height;
        this.weight = weight;
        this.geometryProfile = geometryProfile;
        this.densityClass = densityClass;
        this.material = material;
    }

    public double calculateVolume() {
        return length * width * height;
    }

    public String getCargoSummary() {
        return String.format("#%d: %s (%.2fm x %.2fm x %.2fm, %.1f kg, Profile: %s, Class: %s)",
            itemId, itemName, length, width, height, weight, geometryProfile, densityClass);
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public double getLength() { return length; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getWeight() { return weight; }
    public String getGeometryProfile() { return geometryProfile; }
    public String getDensityClass() { return densityClass; }
    public Material getMaterial() { return material; }
}
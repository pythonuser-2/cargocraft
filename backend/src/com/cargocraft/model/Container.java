package com.cargocraft.model;

public class Container {
    private int containerId;
    private String containerName;
    private double maxWeightLimit;
    private double length;
    private double width;
    private double height;

    public Container(int containerId, String containerName, double maxWeightLimit, 
                     double length, double width, double height) {
        this.containerId = containerId;
        this.containerName = containerName;
        this.maxWeightLimit = maxWeightLimit;
        this.length = length;
        this.width = width;
        this.height = height;
    }

    public boolean validateCapacity(double totalWeight) {
        return totalWeight <= this.maxWeightLimit;
    }

    public double getVolumeEnvelope() {
        return length * width * height;
    }

    public int getContainerId() { return containerId; }
    public String getContainerName() { return containerName; }
    public double getMaxWeightLimit() { return maxWeightLimit; }
    public double getLength() { return length; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}
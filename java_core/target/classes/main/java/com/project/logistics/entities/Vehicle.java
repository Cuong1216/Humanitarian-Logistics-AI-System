package main.java.com.project.logistics.entities;

public class Vehicle {
    private String vehicleId;
    private int loadCapacity;
    private String status; // "AVAILABLE", "DISPATCHED", "MAINTENANCE"

    public Vehicle() {}
    public Vehicle(String vehicleId, int loadCapacity, String status) {
        this.vehicleId = vehicleId;
        this.loadCapacity = loadCapacity;
        this.status = status;
    }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public int getLoadCapacity() { return loadCapacity; }
    public void setLoadCapacity(int loadCapacity) { this.loadCapacity = loadCapacity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() { return vehicleId + " [" + status + "] cap:" + loadCapacity; }
}

package main.java.com.project.logistics.entities;

import java.util.ArrayList;
import java.util.List;

public class SupportCenter extends Location {
    private List<String> currentSupplies = new ArrayList<>();
    private int vehicleCount;

    public SupportCenter() {}
    public SupportCenter(double lat, double lon, String address, int vehicleCount) {
        super(lat, lon, address);
        this.vehicleCount = vehicleCount;
    }

    public List<String> getCurrentSupplies() { return currentSupplies; }
    public void setCurrentSupplies(List<String> currentSupplies) { this.currentSupplies = currentSupplies; }
    public int getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(int vehicleCount) { this.vehicleCount = vehicleCount; }
}

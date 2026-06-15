package com.project.logistics.entities;

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

    public String getResourcesString() {
        if (currentSupplies == null || currentSupplies.isEmpty()) return "Không có";
        java.util.List<String> translated = new java.util.ArrayList<>();
        for (String s : currentSupplies) {
            switch(s.toLowerCase().trim()) {
                case "food": translated.add("Lương thực"); break;
                case "water": translated.add("Nước sạch"); break;
                case "medical": translated.add("Y tế/Thuốc men"); break;
                case "shelter": translated.add("Nhà ở/Bạt che"); break;
                case "rescue": translated.add("Cứu hộ/Áo phao"); break;
                case "transport": translated.add("Vận chuyển"); break;
                case "sanitation": translated.add("Vệ sinh"); break;
                default: translated.add(s);
            }
        }
        return String.join(", ", translated);
    }
}

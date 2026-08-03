package com.humanitarian.logistics.core.logistics.dto;

import java.util.List;

public class RouteResult {
    private List<RouteStep> steps;
    private double totalCost;

    public RouteResult() {}

    public RouteResult(List<RouteStep> steps, double totalCost) {
        this.steps = steps;
        this.totalCost = totalCost;
    }

    public List<RouteStep> getSteps() { return steps; }
    public void setSteps(List<RouteStep> steps) { this.steps = steps; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
}

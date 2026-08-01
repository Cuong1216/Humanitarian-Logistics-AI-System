package com.project.logistics.dto;

public class RouteStep {
    private int seq;
    private long node;
    private long edge;
    private double cost;
    private boolean isFlooded;
    private String pathWkt; // WKT (Well-Known Text) geometry, e.g. "LINESTRING(105 21, 106 22)"

    public RouteStep() {}

    public RouteStep(int seq, long node, long edge, double cost, boolean isFlooded, String pathWkt) {
        this.seq = seq;
        this.node = node;
        this.edge = edge;
        this.cost = cost;
        this.isFlooded = isFlooded;
        this.pathWkt = pathWkt;
    }

    public int getSeq() { return seq; }
    public void setSeq(int seq) { this.seq = seq; }

    public long getNode() { return node; }
    public void setNode(long node) { this.node = node; }

    public long getEdge() { return edge; }
    public void setEdge(long edge) { this.edge = edge; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public boolean isFlooded() { return isFlooded; }
    public void setFlooded(boolean flooded) { isFlooded = flooded; }

    public String getPathWkt() { return pathWkt; }
    public void setPathWkt(String pathWkt) { this.pathWkt = pathWkt; }
}

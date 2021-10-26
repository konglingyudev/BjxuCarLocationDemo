package com.kong.bjxyqxun.entity;

import android.graphics.Color;

import com.amap.api.maps.model.LatLng;

import java.util.List;

public class PolylineLine {
    private int color;
    private List<LatLng> points;
    private int count = 0;
    private boolean show;

    public PolylineLine(int color, List<LatLng> points, int count, boolean show) {
        this.color = color;
        this.points = points;
        this.count = count;
        this.show = show;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public List<LatLng> getPoints() {
        return points;
    }

    public void setPoints(List<LatLng> points) {
        this.points = points;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }
}

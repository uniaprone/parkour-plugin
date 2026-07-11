package org.zzq.pathSelection;

import java.util.ArrayList;
import java.util.List;

public class SelectionPath {
    private final String name;
    private final List<SelectionRegion> regions;

    public SelectionPath(String name, List<SelectionRegion> regions) {
        this.name = name;
        this.regions = new ArrayList<>(regions);
    }

    public String getName() {
        return name;
    }

    public List<SelectionRegion> getRegions() {
        return regions;
    }

    public int getRegionCount() {
        return regions.size();
    }

    public SelectionRegion getRegion(int index) {
        if (index < 0 || index >= regions.size()) {
            return null;
        }
        return regions.get(index);
    }
}
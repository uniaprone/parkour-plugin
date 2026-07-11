package org.zzq.pathSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerEditSession {
    private final UUID playerId;
    private final String editingPathName;
    private final List<SelectionRegion> regions;
    private SelectionRegion currentRegion;

    public PlayerEditSession(UUID playerId, String editingPathName) {
        this.playerId = playerId;
        this.editingPathName = editingPathName;
        this.regions = new ArrayList<>();
        this.currentRegion = null;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getEditingPathName() {
        return editingPathName;
    }

    public List<SelectionRegion> getRegions() {
        return regions;
    }

    public SelectionRegion getCurrentRegion() {
        return currentRegion;
    }

    public void setCurrentRegion(SelectionRegion region) {
        this.currentRegion = region;
    }

    public void addRegion(SelectionRegion region) {
        this.regions.add(region);
    }

    public boolean removeRegion(int index) {
        if (index < 0 || index >= regions.size()) {
            return false;
        }
        regions.remove(index);
        return true;
    }

    public int getRegionCount() {
        return regions.size();
    }

    public void clearCurrentRegion() {
        this.currentRegion = null;
    }

    public void finishCurrentRegion() {
        if (currentRegion != null && currentRegion.isValid()) {
            regions.add(currentRegion);
            currentRegion = null;
        }
    }

    // 新增：检查是否有选区
    public boolean hasRegions() {
        return !regions.isEmpty();
    }
}
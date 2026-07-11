package org.zzq.pathSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerSelectionSession {
    private final UUID playerId;
    private SelectionRegion currentSelection;
    private final List<SelectionRegion> selections;

    public PlayerSelectionSession(UUID playerId) {
        this.playerId = playerId;
        this.selections = new ArrayList<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public SelectionRegion getCurrentSelection() {
        return currentSelection;
    }

    public void setCurrentSelection(SelectionRegion currentSelection) {
        this.currentSelection = currentSelection;
    }

    public List<SelectionRegion> getSelections() {
        return selections;
    }

    public void addSelection(SelectionRegion region) {
        this.selections.add(region);
    }

    public void clearSelections() {
        this.selections.clear();
        this.currentSelection = null;
    }

    public boolean hasSelections() {
        return !selections.isEmpty();
    }

    public int getSelectionCount() {
        return selections.size();
    }
}

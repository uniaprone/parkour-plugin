package org.zzq.pathSelection;

import org.bukkit.Location;

public class SelectionRegion {
    private Location startPoint;
    private Location endPoint;
    private final long createdAt;

    public SelectionRegion() {
        this.createdAt = System.currentTimeMillis();
    }

    public Location getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Location startPoint) {
        this.startPoint = startPoint;
    }

    public Location getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Location endPoint) {
        this.endPoint = endPoint;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isValid() {
        return startPoint != null && endPoint != null &&
                startPoint.getWorld().equals(endPoint.getWorld());
    }

    public Location getMinPoint() {
        if (!isValid()) return null;

        return new Location(startPoint.getWorld(),
                Math.min(startPoint.getX(), endPoint.getX()),
                Math.min(startPoint.getY(), endPoint.getY()),
                Math.min(startPoint.getZ(), endPoint.getZ()));
    }

    public Location getMaxPoint() {
        if (!isValid()) return null;

        return new Location(startPoint.getWorld(),
                Math.max(startPoint.getX(), endPoint.getX()),
                Math.max(startPoint.getY(), endPoint.getY()),
                Math.max(startPoint.getZ(), endPoint.getZ()));
    }

    public boolean contains(Location location) {
        if (!isValid()) {
            return false;
        }

        if (!startPoint.getWorld().equals(location.getWorld())) {
            return false;
        }

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        // 获取方块坐标
        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();

        // 计算包含两个选中方块的区域
        int x1 = startPoint.getBlockX();
        int y1 = startPoint.getBlockY();
        int z1 = startPoint.getBlockZ();
        int x2 = endPoint.getBlockX();
        int y2 = endPoint.getBlockY();
        int z2 = endPoint.getBlockZ();

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        // 使用精确的比较，包含边界
        return blockX >= minX && blockX <= maxX &&
                blockY >= minY && blockY <= maxY &&
                blockZ >= minZ && blockZ <= maxZ;
    }

    public SelectionRegion clone() {
        SelectionRegion clone = new SelectionRegion();
        if (this.startPoint != null) {
            clone.startPoint = this.startPoint.clone();
        }
        if (this.endPoint != null) {
            clone.endPoint = this.endPoint.clone();
        }
        return clone;
    }
}

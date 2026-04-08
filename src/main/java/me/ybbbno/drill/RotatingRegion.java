package me.ybbbno.drill;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class RotatingRegion {

    private final World world;
    private final Location center;
    private final List<StructureBlock> structureBlocks = new ArrayList<>();
    private final List<SavedBlock> savedBlocks = new ArrayList<>();
    private double angle = 0;

    public record StructureBlock(BlockDisplay display, Vector offset) {}
    public record SavedBlock(Location location, Material material, String blockDataString) {} // <-- Сохраняем состояние

    public RotatingRegion(Location center) {
        this.world = center.getWorld();

        Location pos1 = center.clone().add(-4, -1, 1);
        Location pos2 = center.clone().add(0, 1, -1);

        this.center = center.add(0.5, 0.5, 0.5);

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    Block block = loc.getBlock();
                    if (block.getType().name().toLowerCase().contains("tuff")) {
                        savedBlocks.add(new SavedBlock(loc, block.getType(), block.getBlockData().getAsString()));

                        Vector offset = loc.toVector().subtract(center.toVector());
                        BlockDisplay display = world.spawn(loc, BlockDisplay.class);
                        display.setBlock(block.getBlockData());
                        structureBlocks.add(new StructureBlock(display, offset));
                          block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    public void rotate(double angleP) {
        angle += angleP;
        for (StructureBlock sb : structureBlocks) {
            Vector rotated = rotateAroundX(sb.offset(), angle);
            Location newPos = center.clone().add(rotated);

            sb.display().teleport(newPos);
            sb.display().setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf().rotateX((float) angle),
                    new Vector3f(1, 1, 1),
                    new Quaternionf()
            ));
        }
    }

    public void restoreBlocks() {
        for (SavedBlock saved : savedBlocks) {
            Block block = saved.location.getBlock();
            block.setType(saved.material, false);
            block.setBlockData(Bukkit.createBlockData(saved.blockDataString));
        }
        for (StructureBlock sb : structureBlocks) {
            sb.display().remove();
        }
    }

    private Vector rotateAroundX(Vector v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vector(v.getX(), v.getY() * cos - v.getZ() * sin, v.getY() * sin + v.getZ() * cos);
    }
}
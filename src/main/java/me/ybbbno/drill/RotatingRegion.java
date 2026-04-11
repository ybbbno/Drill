package me.ybbbno.drill;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class RotatingRegion {

    private final RotatingAxis axis;
    private final World world;
    private final Location center;
    private final List<StructureBlock> structureBlocks = new ArrayList<>();
    private final List<SavedBlock> savedBlocks = new ArrayList<>();
    private double angle = 0;

    public record StructureBlock(BlockDisplay display, Vector offset) {}
    public record SavedBlock(Location location, Material material, String blockDataString) {}

    public RotatingRegion(@NotNull Location center, String material, RotatingAxis axis, @NotNull Location pos1, @NotNull Location pos2) {
        this.axis = axis;
        this.world = center.getWorld();

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
                    if (block.getType().name().toLowerCase().contains(material)) {
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

        structureBlocks.forEach(sb -> rotateAroundVector(
                sb,
                rotationVector(axis, sb.offset(), angle),
                leftRotationQuaternion(axis, (float) angle)
        ));
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

    private void rotateAroundVector(@NotNull StructureBlock sb, Vector rotationVector, Quaternionf leftRotation) {
        Location newPos = center.clone().add(rotationVector);

        sb.display().teleport(newPos);
        sb.display().setTransformation(new Transformation(
                new Vector3f(),
                leftRotation,
                new Vector3f(1, 1, 1),
                new Quaternionf()
        ));
    }

    private static @NotNull Quaternionf leftRotationQuaternion(@NotNull RotatingAxis axis, float angle) {
        return switch (axis) {
            case X -> new Quaternionf().rotateX(angle);
            case Y -> new Quaternionf().rotateY(angle);
            case Z -> new Quaternionf().rotateZ(angle);
        };
    }

    private static @NotNull Vector rotationVector(@NotNull RotatingAxis axis, @NotNull Vector v, double angle) {
        return switch (axis) {
            case X -> rotationVectorAroundX(v, angle);
            case Y -> rotationVectorAroundY(v, angle);
            case Z -> rotationVectorAroundZ(v, angle);
        };
    }

    @Contract("_, _ -> new")
    private static @NotNull Vector rotationVectorAroundX(@NotNull Vector v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vector(
                v.getX(),
                v.getY() * cos - v.getZ() * sin,
                v.getY() * sin + v.getZ() * cos
        );
    }

    @Contract("_, _ -> new")
    private static @NotNull Vector rotationVectorAroundY(@NotNull Vector v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vector(
                v.getX() * cos + v.getZ() * sin,
                v.getY(),
                -v.getX() * sin + v.getZ() * cos
        );
    }

    @Contract("_, _ -> new")
    private static @NotNull Vector rotationVectorAroundZ(@NotNull Vector v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vector(
                v.getX() * cos - v.getY() * sin,
                v.getX() * sin + v.getY() * cos,
                v.getZ()
        );
    }
}
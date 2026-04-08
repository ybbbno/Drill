package me.ybbbno.magneticpillar.drill;

import me.deadybbb.ybmj.PluginProvider;
import me.ybbbno.customsounds.CustomSoundsAPI;
import me.ybbbno.customsounds.VoicechatManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Powerable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BreakBedrockEvent implements Listener {
    private final PluginProvider plugin;
    private final VoicechatManager managerS;
    private final Location centerLoc;
    private final Location leverLoc;
    private boolean is_active = false;
    private boolean is_stop = false;

    public BreakBedrockEvent(PluginProvider plugin) {
        this.plugin = plugin;
        this.managerS = CustomSoundsAPI.api().manager();
        centerLoc = new Location(plugin.getServer().getWorld("world"), -227, 71, 175);
        leverLoc = new Location(plugin.getServer().getWorld("world"), -233, 67,193);
    }

//    @EventHandler
//    public void onLeverClick(PlayerInteractEvent event) {
//        if (is_stop) return;
//        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
//
//        Block block = event.getClickedBlock();
//
//        if (block == null || block.getType() != Material.LEVER) return;
//        if (!block.getLocation().equals(leverLoc)) return;
//
//        Powerable lever = (Powerable) block.getBlockData();
//
//        if (!is_active && !lever.isPowered()) {
//            is_active = true;
//            startEvent(centerLoc);
//        } else if (is_active) {
//            event.setCancelled(true);
//        }
//    }

//    @EventHandler
//    public void onBlockBreak(BlockBreakEvent event) {
//        Block block = event.getBlock();
//        if (block.getType() != Material.BEDROCK) return;
//
//        startEvent(new Location(block.getWorld(), -32, 67, 42));
//    }

    public void startEvent(Location center) {
        RotatingRegion region = new RotatingRegion(center);

        managerS.createAudioPlayer(center, 50);
        managerS.loadTrack(plugin.getDataPath().resolve("drill.mp3").toAbsolutePath().toString(), center, 100);
        startBurningEffectsTask(center);

        Location soundLocation = center.clone().add(-6, 100, 0);
        managerS.createAudioPlayer(soundLocation, 300);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            managerS.loadTrack(plugin.getDataPath().resolve("pillar-bedrock-destroy.mp3").toAbsolutePath().toString(), soundLocation, 500);
        }, 1000L);

        Location blockLocation = center.clone().add(-5, 0, 0);
        managerS.createAudioPlayer(blockLocation, 50);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            managerS.loadTrack(plugin.getDataPath().resolve("bedrock-break.mp3").toAbsolutePath().toString(), blockLocation, 100);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                blockLocation.getWorld().getBlockAt(blockLocation).breakNaturally();
            }, 131L);
        }, 2200);

        final int start_duration = 304;
        final int end_duration = 2120;
        final int interval_drill = 47;
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int tick = 0;
            double angle = 0;

            @Override
            public void run() {
                if (is_stop) return;
                if (tick <= start_duration) {
                    angle = 0.01;
                } else if (tick % interval_drill == 0) {
                    angle = 0.01;
                    center.getWorld().spawnParticle(Particle.ASH,
                            center.clone().add(-4.4, 0, 0),
                            20, 0, 0, 0.2);
                } else if (tick <= end_duration) {
                    angle = Math.min(angle + 0.001, 0.05);
                    center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                            center.clone().add(-4.4, 0, 0),
                            0, 0, 0, 0);
                } else {
                    is_active = false;
                    is_stop = true;
                    Bukkit.getScheduler().runTaskLater(plugin, region::restoreBlocks, 789L);
                    return;
                }
                region.rotate(angle);
                tick++;
            }
        }, 22L, 1L);
    }

    public void startBurningEffectsTask(Location center) {
        // centerLoc = new Location(plugin.getServer().getWorld("world"), -221, 68, 181);
        Location pos1 = center.clone().add(1, 3, -1);
        Location pos2 = center.clone().add(1, 3, 1);
        List<Location> smokeLocations = Arrays.asList(pos1, pos2);
        World world = center.getWorld();
        if (world == null) return;

        BukkitRunnable smokeTask = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick++;

                int particlesPerLocation;
                if (tick <= 2120) {
                    if (tick % 60 == 0) {
                        particlesPerLocation = 1;
                    } else {
                        particlesPerLocation = 0;
                    }
                } else {
                    particlesPerLocation = 2;
                }

                if (particlesPerLocation == 1) {
                    for (Location loc : smokeLocations) {
                        world.spawnParticle(
                                Particle.CAMPFIRE_SIGNAL_SMOKE,
                                loc, 0, 0, 1, 0, 0.1
                        );
                    }
                } else if (particlesPerLocation == 2) {
                    for (int i = 0; i < 2; i++) {
                        for (Location loc : smokeLocations) {
                            world.spawnParticle(
                                    Particle.CAMPFIRE_SIGNAL_SMOKE,
                                    loc, 0, 0, 1, 0, 0.1
                            );
                        }
                    }
                }

                if (tick > 2120 + 789) {
                    this.cancel();
                }
            }
        };

        smokeTask.runTaskTimer(plugin, 0L, 1L);
    }
}

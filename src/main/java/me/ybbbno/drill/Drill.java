package me.ybbbno.drill;

import me.deadybbb.ybmj.PluginProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Drill extends PluginProvider {

    @Override
    public void onEnable() {
        saveResource("drill.mp3", false);
        saveResource("bedrock-break.mp3", false);
        saveResource("pillar-bedrock-destroy.mp3", false);

        getServer().getPluginManager().registerEvents(new BreakBedrockEvent(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

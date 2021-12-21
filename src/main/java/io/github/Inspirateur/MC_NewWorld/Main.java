package io.github.Inspirateur.MC_NewWorld;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.UUID;

public class Main extends JavaPlugin implements Plugin, Listener {
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        System.out.println("MC New World is enabled");
    }

    @EventHandler
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        // able to detect if a player entered a beacon
    }

    private boolean isInPvP(UUID playerID) {
        return false;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // able to detect if a player entered spawn
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        // to reward pvp players
    }
}

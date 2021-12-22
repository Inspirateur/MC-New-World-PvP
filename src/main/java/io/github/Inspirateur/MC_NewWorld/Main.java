package io.github.Inspirateur.MC_NewWorld;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class Main extends JavaPlugin implements Plugin, Listener {
    private Pacifists pacifists;
    private final int SPTimer = 30;
    private HashMap<UUID, Integer> justGotBeacon;
    private HashSet<UUID> inSpawn;
    private HashMap<UUID, Integer> justLeftSP;
    private Decays<UUID> decays;

    private void playerLeftSP(UUID playerID) {
        // if the player has PvP enabled
        if (!pacifists.is(playerID)) {
            justLeftSP.put(playerID, SPTimer);
            Player player = Bukkit.getPlayer(playerID);
            if (player != null) {
                player.sendMessage("You just left a Safe Point with PvP enabled, you will enter PvP mode in 30 seconds.");
            }
        }
    }

    private void enablePvP(UUID playerID) {
        Player player = Bukkit.getPlayer(playerID);
        if(player != null) {
            player.sendMessage("PvP activated");
        }
    }

    @Override
    public void onEnable() {
        inSpawn = new HashSet<>();
        pacifists = new Pacifists();
        justGotBeacon = new HashMap<>();
        justLeftSP = new HashMap<>();
        decays = new Decays<>();
        decays.addDecay(justGotBeacon, playerID -> {
            // left beacon
            if (!inSpawn.contains(playerID)) {
                playerLeftSP(playerID);
            }
        });
        decays.addDecay(justLeftSP, playerID -> {
            // end of PvP immunity
            if (!pacifists.is(playerID)) {
                enablePvP(playerID);
            }
        });
        int period = 2;
        BukkitRunnable updateDecay = new BukkitRunnable() {
            @Override
            public void run() {
                decays.tick(period);
            }
        };
        updateDecay.runTaskTimer(this, 0, period);
        Bukkit.getPluginManager().registerEvents(this, this);
        System.out.println("MC New World is enabled");
    }

    private boolean isInSafePoint(UUID playerID) {
        return inSpawn.contains(playerID) || justGotBeacon.containsKey(playerID);
    }

    private boolean hasImmunity(UUID playerID) {
        return inSpawn.contains(playerID) || justLeftSP.containsKey(playerID);
    }

    private void pvp(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        if (player != null) {
            if (isInSafePoint(player.getUniqueId())) {
                if(args.length == 0) {
                    boolean isPacifist = pacifists.is(player.getUniqueId());
                    player.sendMessage(String.format("Your PvP status is currently set to %b", isPacifist));
                } else {
                    try {
                        boolean isPacifist = Boolean.parseBoolean(args[0]);
                        pacifists.set(player.getUniqueId(), isPacifist);
                        player.sendMessage(String.format(
                                "PvP status succesfully set to %b\n" +
                                "Note: PvP remains inactive inside Safe Points and %d seconds after you leave it.",
                                isPacifist, SPTimer
                        ));
                    } catch (RuntimeException e) {
                        player.sendMessage(e.toString());
                    }
                }
            } else {
                player.sendMessage("You cannot change your PvP status outside Safe Points.");
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if ("pvp".equals(label)) {
            pvp(sender, args);
        }
        return true;
    }

    @EventHandler
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        // detect if a player entered a beacon
        if (event.getCause() == EntityPotionEffectEvent.Cause.BEACON) {
            Entity e = event.getEntity();
            if (e instanceof Player) {
                // beacon grants effect every 4 sec, we put 5 sec just to be sure
                justGotBeacon.put(e.getUniqueId(), 5);
            }
        }
    }

    private boolean isInPvP(UUID playerID) {
        return !hasImmunity(playerID) && !pacifists.is(playerID);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();
        // detect if a player entered spawn
        if (player.getLocation().distanceSquared(player.getWorld().getSpawnLocation()) < Math.pow(getServer().getSpawnRadius(), 2)) {
            inSpawn.add(playerID);
        } else {
            // player just left spawn
            if (inSpawn.contains(playerID)) {
                inSpawn.remove(playerID);
                justLeftSP.put(playerID, 30);
            }
        }
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        // to reward pvp players
        UUID playerID = event.getPlayer().getUniqueId();
        if (isInPvP(playerID)) {
            event.setAmount((int)(event.getAmount()*1.5));
        }
    }
}

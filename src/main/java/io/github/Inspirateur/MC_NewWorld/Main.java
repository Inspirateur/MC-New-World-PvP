package io.github.Inspirateur.MC_NewWorld;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import java.util.*;


public class Main extends JavaPlugin implements Plugin, Listener, TabCompleter {
	private Pacifists pacifists;
	private final int SP_TIMER = 30;
	private final int SPAWN_RANGE = 40;
	private final List<Integer> RANGES = Arrays.asList(2000, 5000, 10000);
	private final List<Double> XP_BOOSTS = Arrays.asList(2., 1.5, 1.2, 1.1);
	private final List<Integer> LUCK_BOOSTS = Arrays.asList(3, 2, 1, 0);
	private HashMap<UUID, Integer> justGotBeacon;
	private HashSet<UUID> inSpawn;
	private HashMap<UUID, Integer> justLeftSP;
	private Decays<UUID> decays;
	private Team pvpTeam;

	private int getPvPRangeID(Location location) {
		int val = Math.max(Math.abs(location.getBlockX()), Math.abs(location.getBlockZ()));
		int i = 0;
		while (i < RANGES.size() && val >= RANGES.get(i)) {
			i += 1;
		}
		return i;
	}

	private double getXPBoost(Location location) {
		return XP_BOOSTS.get(getPvPRangeID(location));
	}

	private int getLuckBoost(Location location) {
		return LUCK_BOOSTS.get(getPvPRangeID(location));
	}

	private String getPvPRewardMsg(double XPBoost, int luckBoost) {
		return String.format("%.1f xp multiplier, Luck %d", XPBoost, luckBoost);
	}

	private String getPvPRewardMsg(Location location) {
		return getPvPRewardMsg(getXPBoost(location), getLuckBoost(location));
	}



	private void playerLeftSP(UUID playerID) {
		// if the player has PvP enabled
		if (!pacifists.is(playerID)) {
			justLeftSP.put(playerID, SP_TIMER);
			System.out.println(playerID.toString()+ " has left a Safe Point");
			Player player = Bukkit.getPlayer(playerID);
			if (player != null) {
				player.sendMessage("You just left a Safe Point with PvP enabled, you will enter PvP mode in 30 seconds.");
			}
		}
	}

	private void enablePvP(UUID playerID) {
		Player player = Bukkit.getPlayer(playerID);
		if(player != null) {
			Location location = player.getLocation();
			double XPBoost = getXPBoost(location);
			int luckBoost = getLuckBoost(location);
			if (luckBoost > 0) {
				player.addPotionEffect(
					new PotionEffect(PotionEffectType.LUCK, Integer.MAX_VALUE, luckBoost-1, true, false)
				);
			}
			player.sendMessage("PvP is active, "+getPvPRewardMsg(XPBoost, luckBoost));
			pvpTeam.addPlayer(player);
		}
	}

	private void disablePvP(Player player) {
		player.removePotionEffect(PotionEffectType.LUCK);
		pvpTeam.removePlayer(player);
	}

	private Team getPvpTeam() {
		var scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		try {
			pvpTeam = scoreboard.getTeam("pvp");
		} catch (IllegalArgumentException e) {
			pvpTeam = scoreboard.registerNewTeam("pvp");
			pvpTeam.color(NamedTextColor.RED);
		}
		return pvpTeam;
	}

	@Override
	public void onEnable() {
		pacifists = new Pacifists();
		pvpTeam = getPvpTeam();
		inSpawn = new HashSet<>();
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
		Bukkit.getPluginManager().registerEvents(this, this);
		int period = 2;
		BukkitRunnable updateDecay = new BukkitRunnable() {
			@Override
			public void run() {
				decays.tick(period);
			}
		};
		int TICKS_SEC = 20;
		updateDecay.runTaskTimer(this, 0, period*TICKS_SEC);
		System.out.println("MC New World is enabled");
	}

	@Override
	public void onDisable() {
		pacifists.save();
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
					boolean isPvP = !pacifists.is(player.getUniqueId());
					player.sendMessage(String.format("Your PvP status is currently set to %b", isPvP));
				} else {
					try {
						boolean isPvP = Boolean.parseBoolean(args[0]);
						pacifists.set(player.getUniqueId(), !isPvP);
						player.sendMessage(String.format(
								"PvP status succesfully set to %b\n" +
										ChatColor.GRAY + "" + ChatColor.ITALIC +
										"Note: PvP remains inactive inside Safe Points and %d seconds after you leave it.",
								isPvP, SP_TIMER
						));
					} catch (RuntimeException e) {
						player.sendMessage(e.toString());
					}
				}
			} else {
				player.sendMessage("You cannot change your PvP status outside Safe Points.\nGo to the spawn or to a beacon to reach a Safe Point.");
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

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if ("pvp".equals(label)) {
			return Arrays.asList("true", "false");
		}
		return new ArrayList<>();
	}

	@EventHandler
	public void onEntityPotionEffect(EntityPotionEffectEvent event) {
		// detect if a player entered a beacon
		if (event.getCause() == EntityPotionEffectEvent.Cause.BEACON) {
			Entity e = event.getEntity();
			if (e instanceof Player player) {
				UUID playerID = player.getUniqueId();
				if (!isInSafePoint(playerID)) {
					// the player was not in a safe point before that
					justLeftSP.remove(playerID);
					player.sendMessage("You entered a beacon, it is a Safe Point from which you can enable/disable PvP");
					disablePvP(player);
				}
				// beacon grants effect every 4 sec, we put 5 sec just to be sure
				justGotBeacon.put(playerID, 5);
			}
		}
	}

	private boolean isInPvP(UUID playerID) {
		return !hasImmunity(playerID) && !pacifists.is(playerID);
	}

	private void checkSpawn(Player player) {
		UUID playerID = player.getUniqueId();
		// detect if a player entered spawn
		if (player.getWorld().getEnvironment() == World.Environment.NORMAL && player.getLocation().distance(player.getWorld().getSpawnLocation()) < SPAWN_RANGE) {
			// player is in spawn
			if (!isInSafePoint(playerID)) {
				// player just got in spawn
				justLeftSP.remove(playerID);
				player.sendMessage("Welcome to the spawn, it is a Safe Point from which you can enable/disable PvP");
				disablePvP(player);
			}
			inSpawn.add(playerID);
		} else {
			// player just left spawn
			if (inSpawn.contains(playerID)) {
				inSpawn.remove(playerID);
				playerLeftSP(playerID);
			}
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		checkSpawn(player);
		// adjust pvp rewards
		if (isInPvP(player.getUniqueId())) {
			int oldRangeID = getPvPRangeID(event.getFrom());
			int newRangeID = getPvPRangeID(event.getTo());
			if (oldRangeID != newRangeID) {
				// the player changed PVP range, change the Luck boost (the XP boost is computed on xp gain)
				player.removePotionEffect(PotionEffectType.LUCK);
				int luckBoost = getLuckBoost(event.getTo());
				if (luckBoost > 0) {
					player.addPotionEffect(
							new PotionEffect(PotionEffectType.LUCK, Integer.MAX_VALUE, luckBoost-1, true, false)
					);
				}
				// inform the player
				if (newRangeID >= RANGES.size()) {
					player.sendMessage(
						String.format("You just exited the %d bloc range, your new PvP rewards are:\n", RANGES.get(RANGES.size()-1))+getPvPRewardMsg(event.getTo())
					);
				} else {
					player.sendMessage(String.format("You entered the %d bloc range, your new PvP rewards are:\n", RANGES.get(newRangeID))+getPvPRewardMsg(event.getTo()));
				}
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		checkSpawn(player);
		UUID playerID = player.getUniqueId();
		if (isInPvP(playerID)) {
			enablePvP(playerID);
		}
	}

	@EventHandler
	public void onPlayerExpChange(PlayerExpChangeEvent event) {
		// to reward pvp players
		Player player = event.getPlayer();
		if (isInPvP(player.getUniqueId())) {
			event.setAmount((int)(event.getAmount()*getXPBoost(player.getLocation())));
		}
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		// if a player is going to get hurt
		if (event.getEntity() instanceof Player damagee) {
			// check if the damager is another player
			Player damager = null;
			if (event.getDamager() instanceof Player) {
				damager = (Player) event.getDamager();
			} else if (event.getDamager() instanceof Projectile projectile) {
				if (projectile.getShooter() instanceof Player) {
					damager = (Player) projectile.getShooter();
				}
			}
			if (damager != null) {
				// if one of them are pacifists, cancel the event
				if (!isInPvP(damagee.getUniqueId()) || !isInPvP(damager.getUniqueId())) {
					event.setCancelled(true);
				}
			}
		}
	}
}

package me.TheTealViper.PortalTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/*
 * PortalGun
 * Made By: TheTealViper
 * 
 * This plugin is functional but is not really intended to work standalone.
 * It is meant to be a modified framework or perhaps a tutorial for those
 * interested in an explanation as to how it works.
 * I have left comments willy nilly. I don't really have any standards here.
 * If you have any questions, add me on Discord @TheTealViper#2424 and feel
 * free to ask :). (Do note I am very busy with school atm, however).
 */

public class PortalTest extends JavaPlugin implements Listener{
	public static double DEFAULT_X_RADIUS = .5d, DEFAULT_Y_RADIUS = 1d, 
			DEFAULT_ANGLE_SEGMENTS = 20, DEFAULT_RADIAL_SEGMENTS = 4;
	public static Plugin plugin;
	public static Map<Player, Long> cooldownMap = new HashMap<Player, Long>();
	public static List<Player> pendingCancelDamage = new ArrayList<Player>();
	
	
	private Map<Player, PlayerPortal> leftPortalMap = new HashMap<Player, PlayerPortal>();
	private Map<Player, PlayerPortal> rightPortalMap = new HashMap<Player, PlayerPortal>();
	private static long tpCooldown = 250;
	private static Vector velocity = null;

	public void onEnable() {
		plugin = this;
		Bukkit.getServer().getScheduler().cancelTasks(this);
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		saveDefaultConfig();
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {public void run() {
			for(PlayerPortal pp : leftPortalMap.values()) {
				for(Location loc : pp.particleLocList) {
					loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 0, new DustOptions(Color.BLUE, 1));
				}
			}
			for(PlayerPortal pp : rightPortalMap.values()) {
				for(Location loc : pp.particleLocList) {
					loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 0, new DustOptions(Color.ORANGE, 1));
				}
			}
		}}, 0, 1);
	}
	
	public void onDisable() {
		
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		cooldownMap.put(e.getPlayer(), System.currentTimeMillis());
	}
	
	private double raycastIncrement = .05d;
	@EventHandler
	public void onClick(PlayerInteractEvent e) {
		if(e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
			if(item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 9999999) {
				e.setCancelled(true);
				Thread thread = new Thread(new Runnable() {public void run() {
					try {
						boolean foundBlock = false;
						int i = 0;
						Location lastLocation = null;
						while(!foundBlock) {
							Vector facing = e.getPlayer().getLocation().getDirection().clone();
							Vector shift = new Vector(facing.getX() * raycastIncrement * i, facing.getY() * raycastIncrement * i, facing.getZ() * raycastIncrement * i);
							Location loc = e.getPlayer().getEyeLocation().clone().add(shift);
							if(!loc.getBlock().getType().equals(Material.AIR)) {
								BlockFace face = loc.getBlock().getFace(lastLocation.getBlock());
								if(face != null) {
									Vector normal = null;
									switch(face) {
									case UP:
										normal = new Vector(0,1,0);
										break;
									case DOWN:
										normal = new Vector(0,-1,0);
										break;
									case EAST:
										normal = new Vector(1,0,0);
										break;
									case SOUTH:
										normal = new Vector(0,0,-1);
										break;
									case WEST:
										normal = new Vector(-1,0,0);
										break;
									case NORTH:
										normal = new Vector(0,0,1);
										break;
									default:
										break;
									}
									leftPortalMap.put(e.getPlayer(), new PlayerPortal(e.getPlayer(), normal, lastLocation));
									foundBlock = true;
								}
							}else {
								lastLocation = loc;
								i++;
								if(i > 10000)
									break;
							}
						}
					}catch(Exception e) {
						//Do absolutely nothing because we don't care
					}
				}});
				thread.start();
			}
		}
		
		if(e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
			if(item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 9999999) {
				e.setCancelled(true);
				Thread thread = new Thread(new Runnable() {public void run() {
					boolean foundBlock = false;
					int i = 0;
					Location lastLocation = null;
					while(!foundBlock) {
						Vector facing = e.getPlayer().getLocation().getDirection().clone();
						Vector shift = new Vector(facing.getX() * raycastIncrement * i, facing.getY() * raycastIncrement * i, facing.getZ() * raycastIncrement * i);
						Location loc = e.getPlayer().getEyeLocation().clone().add(shift);
						if(!loc.getBlock().getType().equals(Material.AIR)) {
							BlockFace face = loc.getBlock().getFace(lastLocation.getBlock());
							if(face != null) {
								Vector normal = null;
								switch(face) {
								case UP:
									normal = new Vector(0,1,0);
									break;
								case DOWN:
									normal = new Vector(0,-1,0);
									break;
								case EAST:
									normal = new Vector(1,0,0);
									break;
								case SOUTH:
									normal = new Vector(0,0,-1);
									break;
								case WEST:
									normal = new Vector(-1,0,0);
									break;
								case NORTH:
									normal = new Vector(0,0,1);
									break;
								default:
									break;
								}
								rightPortalMap.put(e.getPlayer(), new PlayerPortal(e.getPlayer(), normal, lastLocation));
//								drawOval(normal, lastLocation);
								foundBlock = true;
							}
						}else {
							lastLocation = loc;
							i++;
							if(i > 10000)
								break;
						}
					}
				}});
				thread.start();
			}
		}
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		if(e.getMessage().equals("portalgun")) {
			ItemStack item = new ItemStack(Material.CAKE);
			ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Material.CAKE);
			meta.setCustomModelData(9999999);
			item.setItemMeta(meta);
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {public void run() {
				e.getPlayer().getWorld().dropItem(e.getPlayer().getLocation(), item);
			}}, 1);
		}
		if(e.getMessage().equals("f")) {
			PlayerPortal.debugRaycast(velocity.clone(), e.getPlayer().getLocation());
		}
	}

	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		if(!cooldownMap.containsKey(e.getPlayer()))
			cooldownMap.put(e.getPlayer(), 0L);
		
		if(System.currentTimeMillis() - cooldownMap.get(e.getPlayer()) <= tpCooldown)
			return;
		
		velocity = e.getTo().clone().subtract(e.getFrom().clone()).toVector().multiply(1.3);
		if(e.getPlayer().isOnGround()) {
			if(pendingCancelDamage.contains(e.getPlayer()))
				e.getPlayer().setFallDistance(0);
			pendingCancelDamage.remove(e.getPlayer());
		}
		
		PlayerPortal usedPortal = null;
		for(PlayerPortal pp : leftPortalMap.values()) {
			if(e.getFrom().distanceSquared(pp.center) == 0) {
				continue;
			}
			
			if(pp.inEllipse(velocity.clone(), e.getPlayer().getLocation()) || pp.inEllipse(velocity.clone(), e.getPlayer().getLocation().add(0, .5, 0).add(e.getPlayer().getLocation().getDirection().clone().setY(0).multiply(.4)))) {
				usedPortal = pp;
				
				Player portalOwner = usedPortal.p;
				
				if(rightPortalMap.containsKey(portalOwner)) {
					cooldownMap.put(e.getPlayer(), System.currentTimeMillis());
					PlayerPortal.teleport(e.getPlayer(),velocity.clone(), usedPortal, rightPortalMap.get(portalOwner));
				}
				
				break;
			}
		}
		
		if(usedPortal == null) {
			for(PlayerPortal pp : rightPortalMap.values()) {
				if(e.getFrom().distanceSquared(pp.center) == 0) {
					continue;
				}
				
				if(pp.inEllipse(velocity.clone(), e.getPlayer().getLocation()) || pp.inEllipse(velocity.clone(), e.getPlayer().getLocation().add(0, .5, 0).add(e.getPlayer().getLocation().getDirection().clone().setY(0).multiply(.4)))) {
					usedPortal = pp;
					
					Player portalOwner = usedPortal.p;
					
					if(leftPortalMap.containsKey(portalOwner)) {
						cooldownMap.put(e.getPlayer(), System.currentTimeMillis());
						PlayerPortal.teleport(e.getPlayer(),velocity.clone(), usedPortal, leftPortalMap.get(portalOwner));
					}
					
					break;
				}
			}
		}
	}
	
	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if(e.getEntityType().equals(EntityType.PLAYER)) {
			if(e.getCause().equals(DamageCause.FALL)) {
				Player p = (Player) e.getEntity();
				if(pendingCancelDamage.contains(p)) {
					e.setCancelled(true);
					e.setDamage(0);
					pendingCancelDamage.remove(p);
				}
			}
			
			if(e.getCause().equals(DamageCause.SUFFOCATION)) {
				Player p = (Player) e.getEntity();
				if(pendingCancelDamage.contains(p)) {
					e.setCancelled(true);
					e.setDamage(0);
				}
			}
		}
	}
	
}

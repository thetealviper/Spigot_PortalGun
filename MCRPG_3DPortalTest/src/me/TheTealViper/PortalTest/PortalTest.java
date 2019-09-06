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
 * 
 * There are three main functional parts to this plugin. I will provide more
 * in depth comments in the code itself, but here is an overveiw of the means
 * of achieving the intended goals of each of the functional parts.
 * 1. Handling the portal gun
 * 2. Creating the portals
 * 3. Handling teleportation
 * 
 * 1. Create a custom item. I could have used a vanilla item but that is so
 * boring I would die. To do this, I used photoshop to make the .png texture of
 * the model and blockbench for format the .json model itself. If you don't know
 * what that means, this is a plugin tutorial not a texturing tutorial don't
 * worry about it. Just use the texture pack I give you blablabla. So now that
 * I've skipped a super crucial yet out-of-scope part of this tutorial and we
 * have created the item, we need to give it to a player. If you are making a
 * plugin surely you will have a truly functional way in mind of how to do this
 * alas each plugin will handle this differently so for the tutorial I simply
 * made typing "portalgun" in chat give you the gun. That's the front end
 * experience of giving a gun but how do we ACTUALLY in the backend give a player
 * a custom textured item? There are two ways. One utilizes the damage values of
 * tools (which I don't like) and the other is using a custom tag in the item's
 * meta. Both of these rely on backend resource pack stuff which I'm not covering
 * so sorry but not sorry I'm lazy. Now that we have done all the backend meta
 * tag stuff, we simply give them the item and apply the tag the itemstack's meta.
 * Tada custom portal gun. We also override the left and right clicks to do stuff.
 * That requires a lot of math so refer to the actual code for that. Long story
 * short we are raytracing baby. 
 * 
 * 2. Boom they have left/right clicked and we raytraced that bitch and we know
 * EXACTLY where to make the portal... how do we do it? That involves a lot MORE
 * math. Linear Algebra to be exact. If you're in college and you're thinking
 * "should I take linear algebra?" you 100% should if you're doing computer
 * science as a hobby let alone a major. Long story short, each portal will be
 * created in the world out of particles and will need it's own relative coordinate 
 * axes as the velocity of the player entering the portals will have to be conserved.
 * If you are entering a portal upwards relative to it's position, you will want to
 * continue upwards relative to the portal you pop out of. That's just showbiz baby.
 * 
 * 3. The player needs to teleport when they walk into the portal or this whole
 * project is a bust. This, believe it or not, takes more math. Check the code.
 */

public class PortalTest extends JavaPlugin implements Listener {
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

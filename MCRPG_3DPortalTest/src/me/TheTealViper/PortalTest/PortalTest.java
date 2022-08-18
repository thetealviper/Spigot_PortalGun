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
import org.bukkit.event.player.PlayerDropItemEvent;
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
	
	/*
	 * I used a leftportal and rightportal map because it's just easier to link them that
	 * way okay. Don't yell at me I'm trying my best.
	 */
	private Map<Player, PlayerPortal> leftPortalMap = new HashMap<Player, PlayerPortal>();
	private Map<Player, PlayerPortal> rightPortalMap = new HashMap<Player, PlayerPortal>();
	/*
	 * The tp cooldown is a cooldown required between teleports. This was necessary as if
	 * you go through a portal say on a wall and come out a portal facing upwards on the ground,
	 * you would immediately be in the portal on the ground and teleported back to the original
	 * portal and vice versa infinitely. This cooldown gives the player enough time to hopefully
	 * hop through the portal on the ground and move out of it's entrance bounds. The downside
	 * is now you can only go so fast in a situation where a portal is on the ceiling directly
	 * facing one on the ground. *plays small violin* I tried to make it a boolean based system
	 * where you only teleport once you've stepped out of the teleporter bounds and then back in
	 * but it wasn't working and I have too much schoolwork to do man. Not worth the time.
	 */
	private static long tpCooldown = 250;
	/*
	 * The velocity vector represents the player's velocity relative to the WORLD.
	 */
	private static Vector velocity = null;
	/*
	 * Raycast increment represents stuff about raycasting you'll see in the actual code how it
	 * works. Don't ask too many questions. Not yet. Keep scrolling.
	 */
	private double raycastIncrement = .05d;

	public void onEnable() {
		/*
		 * Alright so on enable, the noteworthy thing we are doing is starting a scheduled task
		 * which will repeat endlessly every tick. This task's job is to "draw" the portal with
		 * particles. Refer to the portal code to see how we obtain the locations, but basically
		 * each portal has the locations where the particles should spawn saved as a list. We
		 * sort through all the portals and within each portal all the locations and spawn a
		 * redstone particle which isn't necessarily red. I chose redstone because it can be any
		 * color and still fundamentally be the same design. A downside to using redstone as the
		 * particle is it is very laggy and because of that optimizations had to be done to how
		 * many particles could be spawned (and with it how nice the portal looks).
		 */
		
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
		/*
		 * This is clearly one of the most useful functions of the bunch.
		 */
		
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		/*
		 * The sole purpose of this code is to fix null errors. That's it man. Fix those errors at any cost.
		 */
		cooldownMap.put(e.getPlayer(), System.currentTimeMillis());
	}
	
	@EventHandler
	public void onClick(PlayerInteractEvent e) {
		/*
		 * Alright. The PlayerInteractEvent. What are we doing here. The basic rundown goes like this. Is the
		 * player clicking? If so, is the player clicking with the portal gun? If so... blablabla. So first
		 * we cancel the event. This is because the portal gun is actually a cake. This was not done for any
		 * reason other than the meme. It actually requires more work to fix because we made it a cake but no
		 * biggie. Basically we are cancelling the event because if you left click a block in creative mode,
		 * the block breaks. If you right click any block with a cake, you place the cake. We do not want either.
		 * Cancel the event. Now, in order to not lag up the server we do the next part in a new thread. If you
		 * don't know what that means, I'm refusing to teach you. Look it up you lazy dum dum. Many things in
		 * spigot can't be done in async threads without hacking the information back into sync but this is fine.
		 * Trust me. So, where are we now? We know the player clicked with the portal gun and we started a new thread.
		 * The reason we started the thread is because of the math we about to do up in this bitchhhh. We need to
		 * raytrace to find out what block they are looking at. Basically what we do, is we draw a line directly
		 * forward from the player's camera view. We check points every x distance on the line until the point is
		 * INSIDE of a block. Note we are not expecting to have a line hit the wall of the block that is foolish.
		 * We are just simply checking points which are very close to eachother until we end up INSIDE of a block.
		 * Looking at the code should explain that part but the other noteworthy part is the variable lastLocation.
		 * Why do we keep lastLocation and what does all that cryptic code do Aaron I can hear you ask. Basically,
		 * if we are currently on the point which is inside the block, that means the last point was NOT inside
		 * the block. So now we have the block itself, and the block directly before that block relative to our
		 * player's sightline. We need both of these to see which face of the block you are looking at. Remember
		 * how I said we are only checking if the point is IN the block? Well that tells us nothing about what
		 * side of the block we are looking at (which we need to make a portal on the side). So what we do is
		 * use the information from the point inside the block and the point directly outside the block to get
		 * which side of the block we are looking at. An important note is the rest of the plugin is essentially
		 * hardcoded relative to which face you're looking at. Technically the plugin can handle portals placed
		 * in any orientation, but for SHOOTING portals on walls and floors, I only took into account the 6 main
		 * faces. This does not account for signs placed at 45 degrees or free standing portals.
		 */
		
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
							//This next line does NOT allow our hit detection to account for non-primitive 1x1 blocks.
							//For example, half slabs and stairs will trigger a "hit" even if the raycast is hovering a space which is physically air
							// because that air is located within the 1x1 block which identifies as slabs or stairs.
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
										normal = new Vector(0,0,1);
										break;
									case WEST:
										normal = new Vector(-1,0,0);
										break;
									case NORTH:
										normal = new Vector(0,0,-1);
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
										normal = new Vector(0,0,1);
										break;
									case WEST:
										normal = new Vector(-1,0,0);
										break;
									case NORTH:
										normal = new Vector(0,0,-1);
										break;
									default:
										break;
									}
									rightPortalMap.put(e.getPlayer(), new PlayerPortal(e.getPlayer(), normal, lastLocation));
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
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		/*
		 * This is my ghetto way of giving a portal gun along with an easy way of debugging stuff for portal
		 * hit detection. I had to make it a single letter because I had to type it whilst moving. If you've
		 * ever tried typing a message while trying to run in game... it's not easy without a hacked client
		 * with hotkeys so yeah.
		 */
		
		if(e.getMessage().equals("portalgun")) {
			ItemStack item = new ItemStack(Material.CAKE);
			ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Material.CAKE);
			meta.setCustomModelData(9999999);
			item.setItemMeta(meta);
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {public void run() {
				e.getPlayer().getWorld().dropItem(e.getPlayer().getLocation(), item);
			}}, 1);
		}
		
	}

	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		/*
		 * Okay so here is where some portal hit detection magic happens. First check, make sure their cooldown for teleportation
		 * is up so they don't get stuck in an infinite loop of teleportation. Second, get their velocity. Spigot
		 * has a built in getVelocity function but it's fucking shit so ignore that. We get velocity the good old
		 * fashion way: final position - initial position. Third, we do some stuff to make sure they don't get
		 * damaged by fall damage nor suffocation damage. Those happen sometimes and so yeah we don't want that.
		 * Fourth, now we are doing hit detection baby. For hit detection, there are actually two checks. There is a check
		 * directly at your feet for portals on the ground and a check shifted forwards from your eyes a bit to represent
		 * where the front of your character model would be for portals on a wall. What about roof portals I hear you hollar.
		 * You can't possibly enter roof portals dum dum. Well for the most part anyway you can't jump 2+ blocks to go through 
		 * the portal you feel me. You can still come out roof portals. Anyway whatever. There are two points on your body checked
		 * for if they are in the portal. Your feetsies and a bit in front of your camera view. This is because if you walk into a wall
		 * and we are checking your feetsies, they actually won't be in the portal which is on the wall as you can't walk that close to walls.
		 * Your player model stops you.
		 */

		if(!cooldownMap.containsKey(e.getPlayer()))
			cooldownMap.put(e.getPlayer(), 0L);
		
		if(System.currentTimeMillis() - cooldownMap.get(e.getPlayer()) <= tpCooldown)
			return;
		
		//We multiply this number (x1.3) arbitrarily because the portal is located ON a wall, but the player can never truly "touch" the wall
		// so we need to magnify the velocity semi-arbitrarily until a normal walking speed stretches the raycasted distance enough to
		// intercept the portal. Trial and error kinda thing
		velocity = e.getTo().clone().subtract(e.getFrom().clone()).toVector().multiply(1.3);
		if(e.getPlayer().isOnGround()) {
			if(pendingCancelDamage.contains(e.getPlayer()))
				e.getPlayer().setFallDistance(0);
			pendingCancelDamage.remove(e.getPlayer());
		}
		
		PlayerPortal usedPortal = null;
		for(PlayerPortal pp : leftPortalMap.values()) {
			// I updated the teleport code to NOT simply teleport the player to the center of the portal because that made them float so this code is useless now. Could be re-implemented properly but I'm lazy. TODO
//			if(e.getFrom().distanceSquared(pp.center) == 0) {
//				continue;
//			}
			
			//Check if body is in vertical portal or if head is in horizontal portal
			if(pp.inEllipse(velocity.clone(), e.getTo().clone()) || pp.inEllipse(velocity.clone(), e.getTo().clone().add(0, e.getPlayer().getEyeHeight(), 0))) {
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
				// I updated the teleport code to NOT simply teleport the player to the center of the portal because that made them float so this code is useless now. Could be re-implemented properly but I'm lazy. TODO
//				if(e.getFrom().distanceSquared(pp.center) == 0) {
//					continue;
//				}
				
				if(pp.inEllipse(velocity.clone(), e.getTo().clone()) || pp.inEllipse(velocity.clone(), e.getTo().clone().add(0, e.getPlayer().getEyeHeight(), 0))) {
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
		/*
		 * This is the remaining code that stop the fall and suffocation damage. Shouldn't
		 * be too hard to understand.
		 */
		
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
	
	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent e) {
		ItemStack item = e.getItemDrop().getItemStack();
		if(item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 9999999) {
			if(leftPortalMap.containsKey(e.getPlayer()))
				leftPortalMap.remove(e.getPlayer());
			if(rightPortalMap.containsKey(e.getPlayer()))
				rightPortalMap.remove(e.getPlayer());
		}
	}
	
}

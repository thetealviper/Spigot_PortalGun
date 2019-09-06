package me.TheTealViper.PortalTest;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PlayerPortal {
	/*
	 * Some may say hey Aaron isn't the normal vector the same as zDir to which I say
	 * shutthefuckupletmedomything
	 */
	public Vector normal;
	/*
	 * The center location is the center of the portal believe it or not
	 */
	public Location center;
	/*
	 * This is the player who owns the portal. This can have many utilities in an actual application
	 * of this plugin
	 */
	public Player p;
	/*
	 * This is where the task which is begun in the onEnable() code gets it's locations from to spawn
	 * particles.
	 */
	public List<Location> particleLocList = new ArrayList<Location>();
	/*
	 * These are unit vectors indicating the portals relative coordinate system
	 */
	Vector xDir, yDir, zDir;
	
	/*
	 * This is just a random thing for the debug. Not necessary at all. Junk code.
	 */
	private static List<Location> goodLocs = new ArrayList<Location>();
	
	public PlayerPortal(Player p, Vector normal, Location center) {
		this.p = p;
		this.center = center;
		this.normal = normal;
		updatePoints();
	}
	
	public void updatePoints() {
		/*
		 * Here is where the points are determined for the portal. Much like the raytracing, there is some significant math
		 * going on here which you don't want lagging your server so you start it on a separate thread. There are no async
		 * issues so don't worry about it. Basically, for each particle location, we determine it with the equation of an
		 * oval. How it's going to work is imagine an empty oval. What we will do is draw a few evenly spaced points from
		 * the center to the rightmost edge of oval. Then, we will rotate a bit around the oval and place the same amount
		 * of equally spaced points again. Rotate a bit more, place, etc. Continue this until you've rotated around the entire
		 * oval. Tada, that's how you get an oval. The variable t gives the angle increment and the variable r gives the amount
		 * of equally spaced points I was talking about. If the angle increment is smaller and the amount of equally spaced particles
		 * is greater, then the oval is filled in more and more. This lags a lot, however, and will actually immediately crash
		 * clients if they ever render more than 4000 particles at once. This is a good balance that I found. Alter numbers as
		 * you see fit. There is a bit of code that transforms the oval particle locations from portal space to world space
		 * but if you don't know linear algebra I can't explain it to you. Also, I know I have a lot of repetitive code in
		 * my scalar multiplication but it's because spigot's vector scalar multiplication function wasn't working as intended for me so I had to
		 * manually do everything the long way.
		 */
		
		Thread thread = new Thread(new Runnable() {public void run() {
			particleLocList.add(center);
			yDir = vectorsEqual(normal, new Vector(0,1,0)) || vectorsEqual(normal, new Vector(0,-1,0)) ? p.getLocation().getDirection().clone().setY(0).normalize() : new Vector(0,1,0);
			xDir = normal.clone().crossProduct(yDir.clone());
			zDir = normal.clone();
			for(double t = 0;t < PortalTest.DEFAULT_ANGLE_SEGMENTS;t++) {
				for(int r = 0;r < PortalTest.DEFAULT_RADIAL_SEGMENTS;r++) {
					double x = (r / PortalTest.DEFAULT_RADIAL_SEGMENTS * PortalTest.DEFAULT_X_RADIUS) * Math.cos(t / PortalTest.DEFAULT_ANGLE_SEGMENTS * 2 * Math.PI);
					double y = (r / PortalTest.DEFAULT_RADIAL_SEGMENTS * PortalTest.DEFAULT_Y_RADIUS) * Math.sin(t / PortalTest.DEFAULT_ANGLE_SEGMENTS * 2 * Math.PI);
					double z = 0d;
					Vector xDirShift = new Vector(xDir.getX() * x, xDir.getY() * x, xDir.getZ() * x);
					Vector yDirShift = new Vector(yDir.getX() * y, yDir.getY() * y, yDir.getZ() * y);
					Vector zDirShift = new Vector(zDir.getX() * z, zDir.getY() * z, zDir.getZ() * z);
					Vector shift = new Vector(xDirShift.getX() + yDirShift.getX() + zDirShift.getX(), xDirShift.getY() + yDirShift.getY() + zDirShift.getY(), xDirShift.getZ() + yDirShift.getZ() + zDirShift.getZ());
					particleLocList.add(center.clone().add(shift));
				}
			}
		}});
		thread.start();
	}
	
	public static void teleport(Player p, Vector velocity, PlayerPortal from, PlayerPortal to) {
		/*
		 * This code really isn't too interesting. It takes the player, teleports them to the new portal, and then converts their velocity
		 * relative to the old portal into a velocity relative to the new portal and then converts that to worldspace velocity.
		 */
		
		PortalTest.pendingCancelDamage.add(p);
		
		double multiplier = .4; //This is for vertical portals. If they are horizontal use the next if condition.
		if(vectorsEqual(to.normal, new Vector(0,-1,0))) { //If the portal is downwards, the multiplier grows so they teleport further down because their feet will spawn in the portal and their head will be in blocks
			multiplier = 1.2;
		}
		Location loc = to.center.clone().add(to.normal.clone().multiply(multiplier));
		p.teleport(new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), p.getLocation().getYaw(), p.getLocation().getPitch()));
		double xVelocity = velocity.clone().dot(from.xDir.clone());
		double yVelocity = velocity.clone().dot(from.yDir.clone());
		double zVelocity = velocity.clone().dot(from.zDir.clone()) * -1;
		Vector xDummy = new Vector(to.xDir.getX() * xVelocity, to.xDir.getY() * xVelocity, to.xDir.getZ() * xVelocity);
		Vector yDummy = new Vector(to.yDir.getX() * yVelocity, to.yDir.getY() * yVelocity, to.yDir.getZ() * yVelocity);
		Vector zDummy = new Vector(to.zDir.getX() * zVelocity, to.zDir.getY() * zVelocity, to.zDir.getZ() * zVelocity);
		Vector shift = new Vector(xDummy.getX() + yDummy.getX() + zDummy.getX(), xDummy.getY() + yDummy.getY() + zDummy.getY(), xDummy.getZ() + yDummy.getZ() + zDummy.getZ());
		p.setVelocity(shift);
	}
	
	public static boolean vectorsEqual(Vector v1, Vector v2) {
		/*
		 * This function was just necessary. No explanation needed.
		 */
		
		if(v1.getX() != v2.getX())
			return false;
		if(v1.getY() != v2.getY())
			return false;
		if(v1.getZ() != v2.getZ())
			return false;
		return true;
	}
	
	public boolean inEllipse_BROKENFUNCTION(Location loc) {
		/*
		 * This is dead code and remnants of my first attempt to do portal hit detection. If you want to know how NOT to do it
		 * this is how you don't do it because the teleport radius is just wayyy too far from the portal itself. You're being teleported
		 * by walking into thin air man. Sucks ass. Don't use this.
		 */
		
		double numerator1 = Math.pow(loc.getX() - center.getX(), 2);
		double denominator1 = Math.pow(PortalTest.DEFAULT_X_RADIUS, 2);
		
		double numerator2 = Math.pow(loc.getZ() - center.getZ(),  2);
		double denominator2 = Math.pow(PortalTest.DEFAULT_Y_RADIUS, 2); //I call it y radius but it's actually the z radius. I just programmed the backend of the portals to consider it y
		
		if(numerator1 / denominator1 + numerator2 / denominator2 <= 1) {
			if(Math.abs(loc.getY() - center.getY()) < .1d) //Now we know it is contained within the loop but how far away from the loop is it
				return true;
			else
				return false;
		} else
			return false;
	}
	
	/*
	 * Iteration distance is used as a raycast distance variable which takes an input of speed. Because the server only updates 20 tps, it is possible
	 * that you have moved through a portal so fast that you go through it on your screen but the server will have completely missed it if you use a
	 * static distance to raycast. So what I do is raycast a distance relative to your speed. .01 seemed to be a good balance. Don't question it.
	 */
	private static double iterationDistance = .01;
	public boolean inEllipse(Vector velocity, Location loc) {
		/*
		 * This is basically a handoff function. To see if you're in the ellipse, we raycast to see if a distance x in front of you is "in" the portal.
		 * This distance x varies from 0 to whatever maximum distance is provided by your speed times the iteration distance.
		 */
		
		Vector v = velocity.clone().multiply(1); //A velocity with mag .2 should have a .1 gap to enter the portal so multiply velocity by .5 to set the ratio of 1 velocity : .5 blocks
		Vector dir = velocity.clone().normalize();
		int iterations = (int) (v.length() / iterationDistance);
		for(int i = 0;i < iterations;i++) {
			Location testLoc = loc.clone().add(dir.clone().multiply(iterationDistance * i));
			if(inEllipse(testLoc)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean inEllipse(Location loc) {
		/*
		 * Alright here is the big boy. Here we go man oof. So first off we need to understand some things. There is a
		 * world coordinate system and a portal coordinate system. Each portal has it's up direction as a y coordinate,
		 * rightward as an x, and an out of monitor toward you as a human z direction. For hit detection, we are first
		 * checking if the player is in the oval with an unrestricted oval z value. This makes a cylindrical kinda shape
		 * whose face is an ellipse rather than a circle if you were to imagine it in 3d view. So first we check if each
		 * point from the raycast of the last inEllipse function is in the OVAL with unrestricted portal z axis. Then, we
		 * check how far the z's are from eachother. I used .2 as the distance in either direction essentially giving a
		 * buffer zone of .4 units. If we were to try to check if the player was ACTUALLY IN THE OVAL OF THE PORTAL, they
		 * never would be. The coordinates are measured to like 16 decimal places. Do you honestly believe you can move so
		 * precise that you stand perfectly at a decimal that looks like .0000000000000001 by choice?! Hell no. So you
		 * give some buffer room. Technically we are teleporting them when they are NEAR the portal and not TECHNICALLY IN IT.
		 * If you think this is cheating, you're wrong. It's the only way. You can mess with the buffer zone which I set to .2
		 * but keep in mind the fact that if players are moving very fast, the packet updates to the server aren't really the
		 * best. It is possible that they move so fast that they just teleport straight through even the buffer zone and don't
		 * get teleported.
		 */
		
		try {
			Vector locationDeltaFromCenter = loc.clone().subtract(center).toVector();
			double distanceFromCenterInDirection1AkaPortalY = locationDeltaFromCenter.clone().dot(yDir.clone());
			double distanceFromCenterInDirection2AkaPortalX = locationDeltaFromCenter.clone().dot(xDir.clone());
			double distanceFromCenterInDirection3AkaPortalZ = locationDeltaFromCenter.clone().dot(zDir.clone());
			
			double numerator1 = Math.pow(distanceFromCenterInDirection1AkaPortalY, 2);
			double denominator1 = Math.pow(PortalTest.DEFAULT_Y_RADIUS, 2);
			
			double numerator2 = Math.pow(distanceFromCenterInDirection2AkaPortalX, 2);
			double denominator2 = Math.pow(PortalTest.DEFAULT_X_RADIUS, 2);
			
			if(numerator1 / denominator1 + numerator2 / denominator2 <= 1) {
				if(Math.abs(distanceFromCenterInDirection3AkaPortalZ) < .2d) //Now we know it is contained within the loop but how far away from the loop is it
					return true;
				else
					return false;
			} else
				return false;
		}catch (Exception e) {
			//Do absolutely nothing because we don't care that yDir is null for whatever reason
		}
		return false;
	}
	
	public static void debugRaycast(Vector velocity, Location loc) {
		/*
		 * Quite frankly I don't feel like annotating this. It's just.... it doesn't matter. Junk code.
		 */
		
		Vector v = velocity.clone().multiply(1); //A velocity with mag .2 should have a .1 gap to enter the portal so multiply velocity by .5 to set the ratio of 1 velocity : .5 blocks
		Vector dir = velocity.clone().normalize();
		int iterations = (int) (v.length() / iterationDistance);
		for(int i = 0;i < iterations;i++) {
			Location testLoc = loc.clone().add(dir.clone().multiply(iterationDistance * i));
			goodLocs.add(testLoc.clone());
		}
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(PortalTest.plugin, new Runnable() {public void run() {
			for(Location loc : goodLocs) {
				loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 0, new DustOptions(Color.GREEN, 1));
			}
		}}, 0, 1);
	}
}

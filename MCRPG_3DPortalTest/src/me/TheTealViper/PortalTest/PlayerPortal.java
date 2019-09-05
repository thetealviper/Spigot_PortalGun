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
	public Vector normal;
	public Location center;
	public Player p;
	public List<Location> particleLocList = new ArrayList<Location>();
	public List<Location> teleportLocList = new ArrayList<Location>(); //This list will contain all the points to check distance from to see if teleporting
	Vector xDir, yDir, zDir;
	
	private static List<Location> badLocs = new ArrayList<Location>();
	private static List<Location> goodLocs = new ArrayList<Location>();
	
	public PlayerPortal(Player p, Vector normal, Location center) {
		this.p = p;
		this.center = center;
		this.normal = normal;
		updatePoints();
	}
	
	public void updatePoints() {
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
					
					//If r is at the magical spots to check for teleporting, 0, pi/2, pi, 3pi/2, and center
					if(r == 0 ) {//|| (r == PortalTest.DEFAULT_RADIAL_SEGMENTS - 1 && t % PortalTest.DEFAULT_ANGLE_SEGMENTS / 4d == 0)) {
						teleportLocList.add(center.clone().add(shift));
					}
				}
			}
		}});
		thread.start();
	}
	
	public static void teleport(Player p, Vector velocity, PlayerPortal from, PlayerPortal to) {
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
		if(v1.getX() != v2.getX())
			return false;
		if(v1.getY() != v2.getY())
			return false;
		if(v1.getZ() != v2.getZ())
			return false;
		return true;
	}
	
	public boolean inEllipse_BROKENFUNCTION(Location loc) {
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
	
	private static double iterationDistance = .01;
	public boolean inEllipse(Vector velocity, Location loc) {
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
		Vector v = velocity.clone().multiply(1); //A velocity with mag .2 should have a .1 gap to enter the portal so multiply velocity by .5 to set the ratio of 1 velocity : .5 blocks
		Vector dir = velocity.clone().normalize();
		int iterations = (int) (v.length() / iterationDistance);
		for(int i = 0;i < iterations;i++) {
			Location testLoc = loc.clone().add(dir.clone().multiply(iterationDistance * i));
			goodLocs.add(testLoc.clone());
		}
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(PortalTest.plugin, new Runnable() {public void run() {
			for(Location loc : badLocs) {
				loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 0, new DustOptions(Color.RED, 1));
			}
			for(Location loc : goodLocs) {
				loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 0, new DustOptions(Color.GREEN, 1));
			}
		}}, 0, 1);
	}
}

package me.TheTealViper.PortalTest.Utils;

import java.util.Random;

import org.bukkit.ChatColor;

public class TextUtils {
	//Braille Resource Pack Located @ 28
	//⺁-Normal Damage
	//⺇-Heart
	//⺎-Mana
	//⺄-Envelope
	
	public static String makeColors(String s) {
		s = s.replaceAll("&0", ChatColor.BLACK + "");
		s = s.replaceAll("&1", ChatColor.DARK_BLUE + "");
		s = s.replaceAll("&2", ChatColor.DARK_GREEN + "");
		s = s.replaceAll("&3", ChatColor.DARK_AQUA + "");
		s = s.replaceAll("&4", ChatColor.DARK_RED + "");
		s = s.replaceAll("&5", ChatColor.DARK_PURPLE + "");
		s = s.replaceAll("&6", ChatColor.GOLD + "");
		s = s.replaceAll("&7", ChatColor.GRAY + "");
		s = s.replaceAll("&8", ChatColor.DARK_GRAY + "");
		s = s.replaceAll("&9", ChatColor.BLUE + "");
		s = s.replaceAll("&a", ChatColor.GREEN + "");
		s = s.replaceAll("&b", ChatColor.AQUA + "");
		s = s.replaceAll("&c", ChatColor.RED + "");
		s = s.replaceAll("&d", ChatColor.LIGHT_PURPLE + "");
		s = s.replaceAll("&f", ChatColor.WHITE + "");
		s = s.replaceAll("&e", ChatColor.YELLOW + "");
		s = s.replaceAll("&l", ChatColor.BOLD + "");
		s = s.replaceAll("&m", ChatColor.STRIKETHROUGH + "");
		s = s.replaceAll("&n", ChatColor.UNDERLINE + "");
		s = s.replaceAll("&o", ChatColor.ITALIC + "");
		s = s.replaceAll("&r", ChatColor.RESET + "");
		return s;
	}
	
	public static String randomColor() {
		Random random = new Random();
		int i = 0;
		while(i == 0 || i == 7 || i == 8 || i == 14)
			i = random.nextInt(16);
		String cdata = i + "";
		if(i == 10)
			cdata = "a";
		else if(i == 11)
			cdata = "b";
		else if(i == 12)
			cdata = "c";
		else if(i == 13)
			cdata = "d";
		else if(i == 15)
			cdata = "e";
		String color = makeColors("&" + cdata);
		return color;
	}
	
}

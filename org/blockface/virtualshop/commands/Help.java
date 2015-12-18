package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Help {

    public static void execute(CommandSender sender, VirtualShop plugin)
    {
    	final String listPrefix = ChatColor.RED + " • ";
    	
        sender.sendMessage("------------------- " + Chatty.getPrefix() + ChatColor.GOLD + " -------------------");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/buy " + ChatColor.GOLD + "<amount> " + ChatColor.BLUE + "<item> " + ChatColor.YELLOW + "[maxprice]" + ChatColor.WHITE + " - buy items.");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/sell " + ChatColor.GOLD + "<amount> " + ChatColor.BLUE + "<item> " + ChatColor.YELLOW + "<price>" + ChatColor.WHITE + " - sell items.");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/cancel " + ChatColor.BLUE + "<item> " + ChatColor.WHITE + " - remove item from shop.");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/find " + ChatColor.BLUE + "<item> " + ChatColor.WHITE + ChatColor.LIGHT_PURPLE + "[page] " + ChatColor.WHITE + " - find offers for the item.");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/stock " + ChatColor.AQUA + "[player] " + ChatColor.LIGHT_PURPLE + "[page] " + ChatColor.WHITE + " - browse offers.");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/sales " + ChatColor.AQUA + "[player] " + ChatColor.LIGHT_PURPLE + "[page] " + ChatColor.WHITE + " - view transaction log.");
        sender.sendMessage("-" + ChatColor.GOLD + "Oo" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "_______________________" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "oO" + ChatColor.WHITE + "-");
    }
}

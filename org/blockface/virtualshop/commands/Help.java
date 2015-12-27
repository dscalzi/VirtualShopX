package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Help implements CommandExecutor{

	VirtualShop plugin;
	
	public Help(VirtualShop plugin){
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			Chatty.denyBeta(sender);
			return true;
		}
		
		this.execute(sender);
		return true;
	}
	
    public void execute(CommandSender sender){
    	final String listPrefix = ChatColor.RED + " � ";
    	String vmCmds = "version";
    	if(sender.hasPermission("virtualshop.access.admin"))
    		vmCmds += ", reload";
    	
        sender.sendMessage("------------------- " + Chatty.getPrefix() + ChatColor.GOLD + " -------------------");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/buy " + ChatColor.GOLD + "<amount> " + ChatColor.BLUE + "<item> " + ChatColor.YELLOW + "[maxprice]" + ChatColor.WHITE + " - buy items.");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/sell " + ChatColor.GOLD + "<amount> " + ChatColor.BLUE + "<item> " + ChatColor.YELLOW + "<price>" + ChatColor.WHITE + " - sell items.");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/cancel " + ChatColor.BLUE + "<item> " + ChatColor.WHITE + " - remove item from shop.");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/find " + ChatColor.BLUE + "<item> " + ChatColor.WHITE + ChatColor.LIGHT_PURPLE + "[page] " + ChatColor.WHITE + " - find offers for the item.");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/stock " + ChatColor.AQUA + "[player] " + ChatColor.LIGHT_PURPLE + "[page] " + ChatColor.WHITE + " - browse offers.");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/sales " + ChatColor.AQUA + "[player] " + ChatColor.LIGHT_PURPLE + "[page] " + ChatColor.WHITE + " - view transaction log.");
        sender.sendMessage(listPrefix + ChatColor.GOLD + "/vm " + ChatColor.RED + "[" + vmCmds + "]" + ChatColor.WHITE + " - Virtual Shop's technical commands.");
        sender.sendMessage("-" + ChatColor.GOLD + "Oo" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "_______________________" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "oO" + ChatColor.WHITE + "-");
    }
}

package org.blockface.virtualshop.commands;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.ConfigManager;
import org.blockface.virtualshop.util.PageList;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class VM implements CommandExecutor{

	VirtualShop plugin;
	
	public VM(VirtualShop plugin){
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			Chatty.denyBeta(sender);
			return true;
		}
		
		String cmd = command.getName();
		if(cmd.equalsIgnoreCase("shop")){
			if(args.length > 0){
				try{
    				int page = Integer.parseInt(args[0]);
    				this.cmdList(sender, page);
    				return true;
    			} catch (NumberFormatException e){
    				Chatty.sendError(sender, "Page does not exist");
					return true;
    			}
			}
			this.cmdList(sender, 1);
			return true;
		}
		
		if(cmd.equalsIgnoreCase("vm")){
			if(args.length > 0){
				if(args[0].equalsIgnoreCase("help")){
					if(args.length > 1){
						try{
		    				int page = Integer.parseInt(args[1]);
		    				this.vmList(sender, page);
		    				return true;
		    			} catch (NumberFormatException e){
		    				Chatty.sendError(sender, "Page does not exist");
							return true;
		    			}
					}
					this.vmList(sender, 1);
					return true;
				}
				if(args[0].equalsIgnoreCase("reload")){
					this.cmdReload(sender);
					return true;
				}
				if(args[0].equalsIgnoreCase("version")){
					this.cmdVersion(sender);
					return true;
				}
			}
			this.vmList(sender, 1);
			return true;
		}
		
		return true;
	}
	
	public void cmdList(CommandSender sender, int page){
    	final String listPrefix = ChatColor.RED + " • ";
    	
    	List<String> cmds = new ArrayList<String>();
        cmds.add(listPrefix + ChatColor.GOLD + "/buy " + ChatColor.GOLD + "<amount> " + ChatColor.BLUE + "<item> " + ChatColor.YELLOW + "[maxprice]" + ChatColor.WHITE + " - buy items.");
        cmds.add(listPrefix + ChatColor.GOLD + "/sell " + ChatColor.GOLD + "<amount> " + ChatColor.BLUE + "<item> " + ChatColor.YELLOW + "<price>" + ChatColor.WHITE + " - sell items.");
        cmds.add(listPrefix + ChatColor.GOLD + "/cancel " + ChatColor.BLUE + "<item> " + ChatColor.WHITE + " - remove item from shop.");
        cmds.add(listPrefix + ChatColor.GOLD + "/find " + ChatColor.BLUE + "<item> " + ChatColor.WHITE + ChatColor.GRAY + "[page] " + ChatColor.WHITE + " - find offers for the item.");
        cmds.add(listPrefix + ChatColor.GOLD + "/stock " + ChatColor.AQUA + "[player] " + ChatColor.GRAY + "[page] " + ChatColor.WHITE + " - browse offers.");
        cmds.add(listPrefix + ChatColor.GOLD + "/sales " + ChatColor.AQUA + "[player] " + ChatColor.GRAY + "[page] " + ChatColor.WHITE + " - view transaction log.");
        cmds.add(listPrefix + ChatColor.GOLD + "/vm" + ChatColor.WHITE + " - Virtual Shop's technical commands.");
        
        PageList<String> commands = new PageList<>(cmds, 7);
        
        List<String> finalMsg = new ArrayList<String>();
        finalMsg.add("------------------- " + Chatty.getPrefix() + ChatColor.GOLD + " -------------------");
        finalMsg.add(ChatColor.GOLD + "              Command List - <Required> [Optional]");
        
        try {
			for(String s : commands.getPage(page)){
				finalMsg.add(s);
			}
		} catch (BadLocationException e) {
			Chatty.sendError(sender, "Page does not exist");
			return;
		}
        
        finalMsg.add(ChatColor.WHITE + "-" + ChatColor.GOLD + "Oo" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "_____• " + ChatColor.GRAY + "Page " + page + " of " + commands.getTotalPages() + ChatColor.GOLD + " •_____" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "oO" + ChatColor.WHITE + "-");
        
        for(String s : finalMsg)
        	sender.sendMessage(s);
        
	}
	
	public void vmList(CommandSender sender, int page){
		final String listPrefix = ChatColor.RED + " • ";
		
		List<String> cmds = new ArrayList<String>();
		cmds.add(listPrefix + ChatColor.GOLD + "/shop " + ChatColor.GRAY + "[page]" + ChatColor.WHITE + " - View merchant commands.");
		cmds.add(listPrefix + ChatColor.GOLD + "/vm help " + ChatColor.GRAY + "[page]" + ChatColor.WHITE + " - VirtualShop's technical commands.");
		cmds.add(listPrefix + ChatColor.GOLD + "/vm version" + ChatColor.WHITE + " - View plugin's version.");
		if(sender.hasPermission("virtualshop.access.admin")){
			cmds.add(listPrefix + ChatColor.GOLD + "/vm reload" + ChatColor.WHITE + " - Reload the plugin's configuration.");
		}
		
		PageList<String> commands = new PageList<>(cmds, 7);
		
		List<String> finalMsg = new ArrayList<String>();
		finalMsg.add("------------------- " + Chatty.getPrefix() + ChatColor.GOLD + " -------------------");
        finalMsg.add(ChatColor.GOLD + "              Command List - <Required> [Optional]");
		
        try {
			for(String s : commands.getPage(page)){
				finalMsg.add(s);
			}
		} catch (BadLocationException e) {
			Chatty.sendError(sender, "Page does not exist");
			return;
		}
        
        finalMsg.add(ChatColor.WHITE + "-" + ChatColor.GOLD + "Oo" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "_____• " + ChatColor.GRAY + "Page " + page + " of " + commands.getTotalPages() + ChatColor.GOLD + " •_____" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "oO" + ChatColor.WHITE + "-");
        
        for(String s : finalMsg)
        	sender.sendMessage(s);
        
	}
	
	public void cmdReload(CommandSender sender){
		ConfigManager.loadConfig(plugin);
		Chatty.initialize(plugin);
		Chatty.sendSuccess(sender, ChatColor.GREEN + "Configuration successfully reloaded.");
	}
	
	public void cmdVersion(CommandSender sender){
		sender.sendMessage(ConfigManager.getPrefix() + " " + ConfigManager.getColor() + "Virtual Shop version " + plugin.getDescription().getVersion());
	}
}

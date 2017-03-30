/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.commands;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.MessageManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Transaction;
import com.dscalzi.virtualshop.util.PageList;
import com.dscalzi.virtualshop.util.UUIDUtil;

import java.util.ArrayList;
import java.util.List;

public class Sales implements CommandExecutor, TabCompleter{
	
	private VirtualShop plugin;
	private final MessageManager mm;
	private final ConfigManager cm;
	private final DatabaseManager dbm;
	
	public Sales(VirtualShop plugin){
		this.plugin = plugin;
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!sender.hasPermission("virtualshop.merchant.sales.individual")){
            mm.noPermissions(sender);
            return true;
        }
		
		try{
			execute(sender, args);
    	} catch (LinkageError e){
    		mm.sendError(sender, "Linkage error occurred. Please restart the server to fix.");
    	}
		return true;
	}
	
	@SuppressWarnings("deprecation")
	public void execute(CommandSender sender, String[] args) throws LinkageError {
		final ChatColor baseColor = cm.getBaseColor();
		final ChatColor trimColor = cm.getTrimColor();
		
		boolean fullServerRecord = false;
		final String serverConstant = "@s";
		
		OfflinePlayer target = null;
		int requestedPage = 1;
		
		if(args.length > 0){
			boolean isString = false;
			try{
				requestedPage = Integer.parseInt(args[0]);
			} catch(NumberFormatException e){
				isString = true;
			}
			if(isString){
				//First try by UUID, if it fails try by player name.
				args[0] = args[0].replaceAll("['\"]", "");
	        	try {
	        		target = plugin.getServer().getOfflinePlayer(UUIDUtil.formatFromInput(args[0]));
	        	} catch(IllegalArgumentException e){
	        		target = plugin.getServer().getOfflinePlayer(args[0]);
	        	}
	        	if(args[0].equalsIgnoreCase(serverConstant)){
	        		if(!sender.hasPermission("virtualshop.merchant.sales.*")){
	        			mm.sendError(sender, "You do not have permission to lookup the full server transaction log.");
	        			return;
	        		}
	        		args[0] = cm.getServerName();
	        		fullServerRecord = true;
	        	}
	        	if(args.length > 1){
		        	try{
						requestedPage = Integer.parseInt(args[1]);
		        	} catch (NumberFormatException e){
		        		requestedPage = 1;
		        	}
	        	}
			}
		}
		
		if(target == null){
			if(sender instanceof Player)
				target = (Player)sender;
			else
				if(!fullServerRecord){
					mm.sendError(sender, "You must either specify a player to lookup or give the argument " + serverConstant + ".");
					return;
				}
		}
		
		List<Transaction> transactions = null;
		try{
			transactions = fullServerRecord ? dbm.getTransactions() : dbm.getTransactions(target.getUniqueId());
		} catch (NullPointerException e){
			mm.noTransactions(sender, (target.getName() == null) ? args[0] : target.getName());
			return;
		}
		if(transactions.size() < 1){
        	mm.noTransactions(sender, (target.getName() == null) ? args[0] : target.getName());
        	return;
        }
		
		PageList<Transaction> sales = new PageList<Transaction>(7, transactions);
		List<Transaction> page = null;
		try{
			page = sales.getPage(requestedPage-1);
		} catch (IndexOutOfBoundsException e){
			if(requestedPage == 1)
				mm.noTransactions(sender, cm.getServerName());
			else
				mm.invalidPage(sender);
			return;
		}
		
		String headerContent = trimColor + "" + ChatColor.BOLD + "< " + baseColor + ChatColor.BOLD + "T" + baseColor + "ransaction " + ChatColor.BOLD + "L" + baseColor + "og ◄► " + ((fullServerRecord) ? cm.getServerName() : target.getName()) + trimColor + ChatColor.BOLD + " >";
		String header = mm.formatHeaderLength(headerContent, this.getClass());
		String footer = baseColor + "-" + trimColor + "Oo" + baseColor + "__________" + trimColor + "_____• " + ChatColor.GRAY + "Page " + requestedPage + " of " + sales.size() + trimColor + " •_____" + baseColor + "__________" + trimColor + "oO" + baseColor + "-";
		
		sender.sendMessage(header);
		for(Transaction t : page){
			if(t.isEnchanted()){
				if(sender instanceof Player)
					mm.sendRawFormattedMessage((Player)sender, mm.formatEnchantedTransaction(t));
				else
					sender.sendMessage(mm.formatTransaction(t, true));
			} else
				sender.sendMessage(mm.formatTransaction(t));
		}
		sender.sendMessage(footer);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> ret = new ArrayList<String>();
		
		if(args.length == 1){
			if(sender.hasPermission("virtualshop.merchant.sales.*")){
				plugin.getServer().getOnlinePlayers().forEach(player -> {if(player.getName().toLowerCase().startsWith(args[0].toLowerCase())) ret.add(player.getName());});
				if("@s".startsWith(args[0].toLowerCase()))
					ret.add("@s");
			}
		}
		
		return ret.size() > 0 ? ret : null;
	}

}

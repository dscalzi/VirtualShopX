/*
 * VirtualShopX
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshopx.command;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.dscalzi.virtualshopx.VirtualShopX;
import com.dscalzi.virtualshopx.managers.ConfigManager;
import com.dscalzi.virtualshopx.managers.DatabaseManager;
import com.dscalzi.virtualshopx.managers.MessageManager;
import com.dscalzi.virtualshopx.objects.Offer;
import com.dscalzi.virtualshopx.util.PageList;
import com.dscalzi.virtualshopx.util.UUIDUtil;

import java.util.ArrayList;
import java.util.List;

public class Stock implements CommandExecutor, TabCompleter{
	
	private VirtualShopX plugin;
	private final MessageManager mm;
	private final ConfigManager cm;
	private final DatabaseManager dbm;
	
	public Stock(VirtualShopX plugin){
		this.plugin = plugin;
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!sender.hasPermission("virtualshopx.merchant.stock.individual")){
            mm.noPermissions(sender);
            return true;
        }
		
		try{
			this.execute(sender, args);
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
		final String serverConstant = "-s";
		
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
	        		if(!sender.hasPermission("virtualshopx.merchant.stock.*")){
	        			mm.sendError(sender, "You do not have permission to lookup the full server stock.");
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
		
		List<Offer> stock = null;
		try{
			List<Offer> rstock = fullServerRecord ? dbm.getAllRegularOffers() : dbm.searchRegularBySeller(target.getUniqueId());
			List<Offer> estock = fullServerRecord ? dbm.getAllEnchantedOffers(false) : dbm.searchEnchantedBySeller(target.getUniqueId(), false);
			stock = new ArrayList<Offer>();
			if(rstock != null)
				stock.addAll(rstock);
			if(estock != null)
				stock.addAll(estock);
		} catch (NullPointerException e){
			mm.noStock(sender, (target.getName() == null) ? args[0] : target.getName());
			return;
		}
		if(stock.size() < 1){
        	mm.noStock(sender, (target.getName() == null) ? args[0] : target.getName());
        	return;
        }
		
		PageList<Offer> sales = new PageList<Offer>(7, stock);
		List<Offer> page = null;
		try{
			page = sales.getPage(requestedPage-1);
		} catch (IndexOutOfBoundsException e){
			if(requestedPage == 1)
				mm.noTransactions(sender, cm.getServerName());
			else
				mm.invalidPage(sender);
			return;
		}
		
		String headerContent = trimColor + "" + ChatColor.BOLD + "< " + baseColor + ChatColor.BOLD + "I" + baseColor + "tem " + ChatColor.BOLD + "S" + baseColor + "tock ◄► " + ((fullServerRecord) ? cm.getServerName() : target.getName()) + trimColor + ChatColor.BOLD + " >";
		String header = mm.formatHeaderLength(headerContent, this.getClass());
		String footer = baseColor + "-" + trimColor + "Oo" + baseColor + "__________" + trimColor + "_____• " + ChatColor.GRAY + "Page " + requestedPage + " of " + sales.size() + trimColor + " •_____" + baseColor + "__________" + trimColor + "oO" + baseColor + "-";
		
		sender.sendMessage(header);
		for(Offer o : page){
			if(o.isEnchanted()){
				if(sender instanceof Player)
					mm.sendRawFormattedMessage((Player)sender, mm.formatEOffer(o));
				else
					sender.sendMessage(mm.formatOffer(o, true));
			} else
				sender.sendMessage(mm.formatOffer(o));
		}
		sender.sendMessage(footer);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> ret = new ArrayList<String>();
		
		if(args.length == 1){
			if(sender.hasPermission("virtualshopx.merchant.stock.*")){
				plugin.getServer().getOnlinePlayers().forEach(player -> {if(player.getName().toLowerCase().startsWith(args[0].toLowerCase())) ret.add(player.getName());});
				if("-s".startsWith(args[0].toLowerCase()))
					ret.add("-s");
			}
		}
		
		return ret.size() > 0 ? ret : null;
	}

}


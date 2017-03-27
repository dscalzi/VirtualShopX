/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.MessageManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.Reloader;
import com.dscalzi.vsreloader.PluginUtil;

import javafx.util.Pair;

public class VS implements CommandExecutor, TabCompleter{

	private static final Pattern redirects = Pattern.compile("^(?iu)(buy|sell|cancel|find|reprice|stock|sales|ebuy|esell|ecancel)");
	private static final Pattern confirmables = Pattern.compile("^(?iu)(buy|sell|cancel|reprice|ebuy|esell|ecancel)");
	
	private VirtualShop plugin;
	private final MessageManager mm;
	private final ConfigManager cm;
	private final DatabaseManager dbm;
	final ItemDB idb;
	
	public VS(VirtualShop plugin){
		this.plugin = plugin;
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.idb = ItemDB.getInstance();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		String cmd = command.getName();
		if(cmd.equalsIgnoreCase("shop") || (cmd.equals("vs") && args.length > 0 && args[0].equalsIgnoreCase("shop"))){
			if(args.length > 0){
				try{
    				int page = (args[0].equalsIgnoreCase("shop") ? Integer.parseInt(args[1]) : Integer.parseInt(args[0]));
    				this.cmdList(sender, page);
    				return true;
    			} catch (NumberFormatException e){
    				mm.invalidPage(sender);
					return true;
    			} catch (ArrayIndexOutOfBoundsException e){
    				this.cmdList(sender, 1);
    				return true;
    			}
			}
			this.cmdList(sender, 1);
			return true;
		}
		
		if(cmd.equalsIgnoreCase("vs")){
			if(args.length > 0){
				if(args[0].equalsIgnoreCase("help")){
					if(args.length > 1){
						try{
		    				int page = Integer.parseInt(args[1]);
		    				this.vsList(sender, page);
		    				return true;
		    			} catch (NumberFormatException e){
		    				mm.invalidPage(sender);
							return true;
		    			}
					}
					this.vsList(sender, 1);
					return true;
				}
				if(redirects.matcher(args[0]).matches()){
					this.redirectCommand(sender, args);
					return true;
				}
				if(args[0].equalsIgnoreCase("lookup")){
					this.cmdLookup(sender, args);
					return true;
				}
				if(args[0].equalsIgnoreCase("formatmarket")){
					if(args.length > 1){
						this.formatMarket(sender, args[1]);
						return true;
					}
					this.formatMarket(sender);
					return true;
				}
				if(args[0].equalsIgnoreCase("uuidnamesync")){
					if(args.length > 1){
						this.cmdUUIDNameSync(sender, Optional.of(args[1]));
						return true;
					}
					this.cmdUUIDNameSync(sender, Optional.empty());
					return true;
				}
				if(args[0].equalsIgnoreCase("update2uuid")){
					this.cmdupdateuuid(sender);
					return true;
				}
				if(args[0].equalsIgnoreCase("fullreload")){
					this.cmdFullReload(sender);
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
				try{
    				int page = Integer.parseInt(args[0]);
    				this.vsList(sender, page);
    				return true;
    			} catch (NumberFormatException e){
    				mm.invalidPage(sender);
					return true;
    			}
			}
			this.vsList(sender, 1);
			return true;
		}
		
		return true;
	}
	
	public void cmdupdateuuid(CommandSender sender){
		
		if(!sender.hasPermission("virtualshop.*")){
			mm.noPermissions(sender);
            return;
		}
		
		try{
			@SuppressWarnings("deprecation")
			Pair<Integer, Integer> sfs = dbm.updateDatabase();
			mm.sendSuccess(sender, "Database updated. " + sfs.getKey() + " successful, " + sfs.getValue() + " failed.");
		} catch(Exception ex){
			ex.printStackTrace();
			mm.sendError(sender, "FAILED");
		}
	}
	
	public void cmdList(CommandSender sender, int page){
        mm.shopList(sender, page);
	}
	
	public void vsList(CommandSender sender, int page){
		mm.vsList(sender, page);
	}
	
	@SuppressWarnings("deprecation")
	public void cmdLookup(CommandSender sender, String[] args){
		
		if(!sender.hasPermission("virtualshop.merchant.lookup")){
			mm.noPermissions(sender);
			return;
		}
    	
		ItemStack target = null;
		
		//Hand
		if(args.length == 1 || (args.length > 1 && (args[1].equalsIgnoreCase("hand") || args[1].equalsIgnoreCase("mainhand") || args[1].equalsIgnoreCase("offhand")))){
			if(!(sender instanceof Player)){
				mm.mustSpecifyItem(sender);
				return;
			}
			Player p = (Player)sender;
			if(args.length > 1 && args[1].equalsIgnoreCase("offhand")) target = p.getInventory().getItemInOffHand();
			else target = p.getInventory().getItemInMainHand();
			
			if(target == null){
				mm.lookupFailedNull(sender);
				return;
			}
		}
		
		if(args.length > 1 && target == null){
			target = idb.get(args[1], 0);
			if(target == null){
				target = idb.unsafeLookup(args[1]);
				if(target == null){
					mm.lookupFailedNotFound(sender, args[1]);
					return;
				}
			}
		}
		
		//Final check for security.
		if(target == null){
			mm.lookupFailedUnknown(sender);
			return;
		}
		
		//Blocked items.
		if(target.getTypeId() == 440){
			mm.lookupUnsuported(sender);
			return;
		}
		
		mm.formatLookupResults(sender, target, idb.getAliases(target));
	}
	
	@SuppressWarnings("deprecation")
	public void formatMarket(CommandSender sender){
		
		if(!sender.hasPermission("virtualshop.admin.formatmarket")){
			mm.noPermissions(sender);
            return;
		}
		
		int amt = 0;
		for(Offer o : dbm.getAllOffers()){
			if(o.getPrice() > cm.getMaxPrice(o.getItem().getData().getItemTypeId(), o.getItem().getData().getData())){
				dbm.updatePrice(o.getId(), cm.getMaxPrice(o.getItem().getData().getItemTypeId(), o.getItem().getData().getData()));
				++amt;
			}
		}
		if(amt == 0)
			mm.listingsAlreadyFormatted(sender);
		else
			mm.listingsFormatted(sender, amt);
	}
	
	@SuppressWarnings("deprecation")
	public void formatMarket(CommandSender sender, String itm){
		
		if(!sender.hasPermission("virtualshop.admin.formatmarket")){
			mm.noPermissions(sender);
            return;
		}
		
		int amt = 0;
		ItemStack item = idb.get(itm, 0);
		if(itm.equalsIgnoreCase("hand") && sender instanceof Player){
			Player player = (Player)sender;
			item = new ItemStack(player.getItemInHand().getType(), 0, player.getItemInHand().getDurability());
			itm = idb.reverseLookup(item);
		}
		if(item == null){
			mm.wrongItem(sender, itm);
			return;
		}
		for(Offer o : dbm.getAllOffers()){
			boolean isSameItem = idb.reverseLookup(item).equalsIgnoreCase(idb.reverseLookup(o.getItem()));
			if(o.getPrice() > cm.getMaxPrice(o.getItem().getData().getItemTypeId(), o.getItem().getData().getData()) && isSameItem){
				dbm.updatePrice(o.getId(), cm.getMaxPrice(o.getItem().getData().getItemTypeId(), o.getItem().getData().getData()));
				++amt;
			}
		}
		if(amt == 0)
			mm.listingsAlreadyFormatted(sender);
		else
			mm.listingsFormatted(sender, amt);
	}
	
	public void cmdUUIDNameSync(CommandSender sender, Optional<String> uuid){
		
		if(!sender.hasPermission("virtualshop.admin.uuidnamesync")){
			mm.noPermissions(sender);
            return;
		}
		
		if(uuid.isPresent()){
			UUID target;
			try {
				target = UUID.fromString(uuid.get());
			} catch(IllegalArgumentException e){
				mm.invalidUUIDFormat(sender);
				return;
			}
			Pair<Boolean, Integer> response = dbm.syncNameToUUID(target);
			if(response.getKey()) mm.accountSynced(sender);
			else if(response.getValue() == 1) mm.accountAlreadySynced(sender);
			else if(response.getValue() == 404) mm.accountNotFound(sender);
			else mm.queryError(sender);
		} else {
			int updated = dbm.syncNameToUUID();
			if(updated > 0) mm.accountsSynced(sender, updated);
			else mm.accountsAlreadySynced(sender);
		}
		
	}
	
	private void redirectCommand(CommandSender sender, String[] args){
		if(args.length > 0){
			CommandExecutor exec = plugin.getCommand(args[0]).getExecutor();
			if(exec == null) throw new IllegalArgumentException();
			String[] newArgs = new String[args.length-1];
			System.arraycopy(args, 1, newArgs, 0, newArgs.length);
			exec.onCommand(sender, null, "vs " + args[0], newArgs);
		}
	}
	
	public void cmdFullReload(CommandSender sender){
		
		if(!sender.hasPermission("virtualshop.developer.fullreload")){
			if(sender.hasPermission("virtualshop.admin.reload")){
				mm.sendError(sender, "You do not have permission to reload the plugin, reloading config instead.");
				this.cmdReload(sender);
				return;
			}
			mm.noPermissions(sender);
            return;
		}
		if(plugin.getServer().getPluginManager().getPlugin("VSReloader") == null){
			if(sender.hasPermission("virtualshop.admin.reload")){
				mm.sendError(sender, "VS Reloader not found, reloading config instead.");
				cmdReload(sender);
			} else {
				mm.sendError(sender, "VS Reloader not found, could not reload the plugin.");
			}
			return;
		}
		DatabaseManager.getInstance().terminate();
		mm.sendMessage(sender, "Begining reload of VirtualShop, this may lag the server for a few seconds..");
		/* TODO Test async relaod.
		 * 
		 * Thread th = new Thread(() -> {
		 *      PluginUtil.reload(plugin);
		 * }).start();
		 * 
		 */
		Runnable r = () -> {
			PluginUtil.reload(plugin);
		};
		r.run();
		mm.sendSuccess(sender, "Plugin successfully reloaded.");
	}
	
	public void cmdReload(CommandSender sender){
		
		if(!sender.hasPermission("virtualshop.admin.reload")){
			mm.noPermissions(sender);
            return;
		}
		
		try {
			ItemDB.reload();
		} catch (IOException e) {
			mm.sendError(sender, "Reference file 'items.csv' not found. Shutting down!");
            plugin.getPluginLoader().disablePlugin(plugin);
		}
		ConfigManager.reload();
		MessageManager.reload();
		DatabaseManager.reload();
		Reloader.reload();
		
		mm.sendSuccess(sender, "Configuration successfully reloaded.");
		
	}
	
	/**
	 *  Never restrict this command.
	 */
	public void cmdVersion(CommandSender sender){
		mm.versionMessage(sender);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		List<String> ret = new ArrayList<String>();
		
		if(args.length == 1){
			if(sender.hasPermission("virtualshop.merchant.regular.buy") && "buy".startsWith(args[0].toLowerCase())) 
				ret.add("buy");
			if(sender.hasPermission("virtualshop.merchant.enchanted.buy") && "buy".startsWith(args[0].toLowerCase())) 
				ret.add("ebuy");
			if(sender.hasPermission("virtualshop.merchant.regular.sell") && "sell".startsWith(args[0].toLowerCase())) 
				ret.add("sell");
			if(sender.hasPermission("virtualshop.merchant.enchanted.sell") && "sell".startsWith(args[0].toLowerCase())) 
				ret.add("ssell");
			if(sender.hasPermission("virtualshop.merchant.regular.cancel") && "cancel".startsWith(args[0].toLowerCase())) 
				ret.add("cancel");
			if(sender.hasPermission("virtualshop.merchant.enchanted.cancel") && "cancel".startsWith(args[0].toLowerCase())) 
				ret.add("ecancel");
			if(sender.hasPermission("virtualshop.merchant.sales.individual") && "sales".startsWith(args[0].toLowerCase())) 
				ret.add("sales");
			if(sender.hasPermission("virtualshop.merchant.stock.individual") && "stock".startsWith(args[0].toLowerCase())) 
				ret.add("stock");
			if(sender.hasPermission("virtualshop.merchant.find") && "find".startsWith(args[0].toLowerCase())) 
				ret.add("find");
			if(sender.hasPermission("virtualshop.merchant.lookup") && "lookup".startsWith(args[0].toLowerCase())) 
				ret.add("lookup");
			if(sender.hasPermission("virtualshop.merchant.reprice") && "reprice".startsWith(args[0].toLowerCase())) 
				ret.add("reprice");
			if(sender.hasPermission("virtualshop.admin.formatmarket") && "formatmarket".startsWith(args[0].toLowerCase())) 
				ret.add("formatmarket");
			if(sender.hasPermission("virtualshop.admin.reload") && "reload".startsWith(args[0].toLowerCase())) 
				ret.add("reload");
			if(sender.hasPermission("virtualshop.admin.uuidnamesync") && "uuidnamesync".startsWith(args[0].toLowerCase())) 
				ret.add("uuidnamesync");
			if(sender.hasPermission("virtualshop.developer.fullreload") && "fullreload".startsWith(args[0].toLowerCase())) 
				ret.add("fullreload");
		}
		
		if(args.length == 2){
			boolean a = sender.hasPermission("virtualshop.merchant.sales.*") && "sales".startsWith(args[0].toLowerCase());
			boolean b = sender.hasPermission("virtualshop.merchant.stock.*") && "stock".startsWith(args[0].toLowerCase());
			if(a | b){
				plugin.getServer().getOnlinePlayers().forEach(player -> {if(player.getName().toLowerCase().startsWith(args[1].toLowerCase())) ret.add(player.getName());});
				if("@s".startsWith(args[1].toLowerCase()))
					ret.add("@s");
			}
		}
		
		if(confirmables.matcher(args[0]).matches()){
			if(args.length == 2)
				if("confirm".startsWith(args[1].toLowerCase()))
					ret.add("confirm");
			
			if(args.length == 3)
				if("toggle".startsWith(args[2].toLowerCase()))
					ret.add("toggle");
		}
		
		return ret.size() > 0 ? ret : null;
	}
}

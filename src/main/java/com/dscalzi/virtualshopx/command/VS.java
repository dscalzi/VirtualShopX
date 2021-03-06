/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import com.dscalzi.itemcodexlib.component.ItemEntry;
import com.dscalzi.virtualshopx.VirtualShopX;
import com.dscalzi.virtualshopx.managers.ConfigManager;
import com.dscalzi.virtualshopx.managers.DatabaseManager;
import com.dscalzi.virtualshopx.managers.MessageManager;
import com.dscalzi.virtualshopx.objects.Offer;
import com.dscalzi.virtualshopx.util.ItemDB;
import com.dscalzi.virtualshopx.util.Reloader;
import com.dscalzi.vsxreloader.PluginUtil;

public class VS implements CommandExecutor, TabCompleter{

	private static final Pattern redirects = Pattern.compile("^(?iu)(buy|sell|cancel|find|reprice|stock|sales|ebuy|esell|ecancel|ereprice)");
	private static final Pattern confirmables = Pattern.compile("^(?iu)(buy|sell|cancel|reprice|ebuy|esell|ecancel|ereprice)");
	
	private VirtualShopX plugin;
	private final MessageManager mm;
	private final ConfigManager cm;
	private final DatabaseManager dbm;
	final ItemDB idb;
	
	public VS(VirtualShopX plugin){
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
	
	public void cmdList(CommandSender sender, int page){
        mm.shopList(sender, page);
	}
	
	public void vsList(CommandSender sender, int page){
		mm.vsList(sender, page);
	}
	
	public void cmdLookup(CommandSender sender, String[] args){
		
		if(!sender.hasPermission("virtualshopx.merchant.lookup")){
			mm.noPermissions(sender);
			return;
		}
    	
		Optional<ItemEntry> target = null;
		ItemStack searchItem = null;
		
		//Hand
		if(args.length == 1 || (args.length > 1 && (args[1].equalsIgnoreCase("hand") || args[1].equalsIgnoreCase("mainhand") || args[1].equalsIgnoreCase("offhand")))){
			if(!(sender instanceof Player)){
				mm.mustSpecifyItem(sender);
				return;
			}
			Player p = (Player)sender;
			if(args.length > 1 && args[1].equalsIgnoreCase("offhand")) searchItem = p.getInventory().getItemInOffHand();
			else searchItem = p.getInventory().getItemInMainHand();
			
			if(searchItem == null){
				mm.lookupFailedNull(sender);
				return;
			} else {
			    target = ItemDB.getInstance().getByItemStack(searchItem);
			}
		}
		
		if(target == null && args.length > 1) {
		    target = ItemDB.getInstance().get(args[1]);
		    
		    if(!target.isPresent()) {
		        searchItem = idb.unsafeLookup(args[1]);
                if(searchItem == null){
                    mm.lookupFailedNotFound(sender, args[1]);
                    return;
                }
		    } else {
		        searchItem = target.get().getItemStack();
		    }
		}
		
		//Final check for security.
		if(searchItem == null){
			mm.lookupFailedUnknown(sender);
			return;
		}
		
		if(target.isPresent()) {
		    mm.formatLookupResults(sender, searchItem, target.get().hasLegacy() ? target.get().getLegacy().toString() : null, target.get().getAliases());
		} else {
		    mm.formatLookupResults(sender, searchItem, null, new ArrayList<String>());
		}
	}
	
	public void formatMarket(CommandSender sender){
		
		if(!sender.hasPermission("virtualshopx.admin.formatmarket")){
			mm.noPermissions(sender);
            return;
		}
		
		int amt = 0;
		for(Offer o : dbm.getAllRegularOffers()){
			if(o.getPrice() > cm.getMaxPrice(o.getItem().getType())){
				dbm.updatePrice(o.getId(), cm.getMaxPrice(o.getItem().getType()));
				++amt;
			}
		}
		if(amt == 0)
			mm.listingsAlreadyFormatted(sender);
		else
			mm.listingsFormatted(sender, amt);
	}
	
    public void formatMarket(CommandSender sender, String itm){
		
		if(!sender.hasPermission("virtualshopx.admin.formatmarket")){
			mm.noPermissions(sender);
            return;
		}
		
		int amt = 0;
		ItemStack item = idb.get(itm, 0);
		if(itm.equalsIgnoreCase("hand") && sender instanceof Player){
			Player player = (Player)sender;
			PlayerInventory piv = player.getInventory();
			item = new ItemStack(piv.getItemInMainHand().getType());
			itm = idb.getItemAlias(item);
		}
		if(item == null){
			mm.wrongItem(sender, itm);
			return;
		}
		for(Offer o : dbm.getAllRegularOffers()){
			boolean isSameItem = idb.getItemAlias(item).equalsIgnoreCase(idb.getItemAlias(o.getItem()));
			if(o.getPrice() > cm.getMaxPrice(o.getItem().getType()) && isSameItem){
				dbm.updatePrice(o.getId(), cm.getMaxPrice(o.getItem().getType()));
				++amt;
			}
		}
		if(amt == 0)
			mm.listingsAlreadyFormatted(sender);
		else
			mm.listingsFormatted(sender, amt);
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
		
		if(!sender.hasPermission("virtualshopx.developer.fullreload")){
			if(sender.hasPermission("virtualshopx.admin.reload")){
				mm.sendError(sender, "You do not have permission to reload the plugin, reloading config instead.");
				this.cmdReload(sender);
				return;
			}
			mm.noPermissions(sender);
            return;
		}
		if(plugin.getServer().getPluginManager().getPlugin("VSXReloader") == null){
			if(sender.hasPermission("virtualshopx.admin.reload")){
				mm.sendError(sender, "VSXReloader not found, reloading config instead.");
				cmdReload(sender);
			} else {
				mm.sendError(sender, "VSXReloader not found, could not reload the plugin.");
			}
			return;
		}
		DatabaseManager.getInstance().terminate();
		mm.sendMessage(sender, "Begining reload of VirtualShopX, this may lag the server for a few seconds..");
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
		
		if(!sender.hasPermission("virtualshopx.admin.reload")){
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
			if(sender.hasPermission("virtualshopx.merchant.regular.buy") && "buy".startsWith(args[0].toLowerCase())) 
				ret.add("buy");
			if(sender.hasPermission("virtualshopx.merchant.enchanted.buy") && "ebuy".startsWith(args[0].toLowerCase())) 
				ret.add("ebuy");
			if(sender.hasPermission("virtualshopx.merchant.regular.sell") && "sell".startsWith(args[0].toLowerCase())) 
				ret.add("sell");
			if(sender.hasPermission("virtualshopx.merchant.enchanted.sell") && "esell".startsWith(args[0].toLowerCase())) 
				ret.add("esell");
			if(sender.hasPermission("virtualshopx.merchant.regular.cancel") && "cancel".startsWith(args[0].toLowerCase())) 
				ret.add("cancel");
			if(sender.hasPermission("virtualshopx.merchant.enchanted.cancel") && "ecancel".startsWith(args[0].toLowerCase())) 
				ret.add("ecancel");
			if(sender.hasPermission("virtualshopx.merchant.regular.reprice") && "reprice".startsWith(args[0].toLowerCase())) 
				ret.add("reprice");
			if(sender.hasPermission("virtualshopx.merchant.enchanted.reprice") && "ereprice".startsWith(args[0].toLowerCase())) 
				ret.add("ereprice");
			if(sender.hasPermission("virtualshopx.merchant.sales.individual") && "sales".startsWith(args[0].toLowerCase())) 
				ret.add("sales");
			if(sender.hasPermission("virtualshopx.merchant.stock.individual") && "stock".startsWith(args[0].toLowerCase())) 
				ret.add("stock");
			if(sender.hasPermission("virtualshopx.merchant.find") && "find".startsWith(args[0].toLowerCase())) 
				ret.add("find");
			if(sender.hasPermission("virtualshopx.merchant.lookup") && "lookup".startsWith(args[0].toLowerCase())) 
				ret.add("lookup");
			if(sender.hasPermission("virtualshopx.admin.formatmarket") && "formatmarket".startsWith(args[0].toLowerCase())) 
				ret.add("formatmarket");
			if(sender.hasPermission("virtualshopx.admin.reload") && "reload".startsWith(args[0].toLowerCase())) 
				ret.add("reload");
			if(sender.hasPermission("virtualshopx.developer.fullreload") && "fullreload".startsWith(args[0].toLowerCase())) 
				ret.add("fullreload");
		}
		
		if(args.length == 2){
			boolean a = sender.hasPermission("virtualshopx.merchant.sales.*") && "sales".startsWith(args[0].toLowerCase());
			boolean b = sender.hasPermission("virtualshopx.merchant.stock.*") && "stock".startsWith(args[0].toLowerCase());
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

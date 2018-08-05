/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.command;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.dscalzi.virtualshopx.VirtualShopX;
import com.dscalzi.virtualshopx.managers.ConfigManager;
import com.dscalzi.virtualshopx.managers.DatabaseManager;
import com.dscalzi.virtualshopx.managers.MessageManager;
import com.dscalzi.virtualshopx.objects.Offer;
import com.dscalzi.virtualshopx.util.InputUtil;
import com.dscalzi.virtualshopx.util.ItemDB;
import com.dscalzi.virtualshopx.util.PageList;

import java.util.List;

public class Find implements CommandExecutor{
	
	@SuppressWarnings("unused")
	private VirtualShopX plugin;
	private final MessageManager mm;
	private final ConfigManager cm;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	public Find(VirtualShopX plugin){
		this.plugin = plugin;
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.idb = ItemDB.getInstance();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!sender.hasPermission("virtualshopx.merchant.regular.find")){
            mm.noPermissions(sender);
            return true;
        }
		if(args.length < 1){
			mm.sendError(sender, "You need to specify the item.");
			return true;
		}
		
		 this.execute(sender, args);
		return true;
	}
	
    public void execute(CommandSender sender, String[] args){
    	final ChatColor baseColor = cm.getBaseColor();
    	final ChatColor trimColor = cm.getTrimColor();
    	
    	ItemStack item = idb.get(args[0], 0);
    	
    	if(sender instanceof Player){
    		PlayerInventory im = ((Player)sender).getInventory();
    		if(args[0].matches("^(?iu)(hand|mainhand|offhand)")){
    			item = new ItemStack(args[0].equalsIgnoreCase("offhand") ? im.getItemInOffHand() : im.getItemInMainHand());
    			if(item.getType() == Material.AIR){
    				mm.holdingNothing(sender);
    				return;
    			}
    			args[0] = idb.getItemAlias(item);
    		}
    	}
    	if(item == null){
    		mm.wrongItem(sender, args[0]);
    		return;
    	}
    	
    	List<Offer> offers = dbm.getPrices(item);
    	if(offers.size() == 0){
    		mm.noListings(sender, args[0]);
            return;
    	}
    	
    	int requestedPage = 1;
    	if(args.length > 1) requestedPage = InputUtil.parseInt(args[1]);
    	
    	PageList<Offer> listings = new PageList<Offer>(7, offers);
    	
        String headerContent = trimColor + "" + ChatColor.BOLD + "< " + baseColor + ChatColor.BOLD + "L" + baseColor + "istings ◄► " + ChatColor.BOLD + Character.toUpperCase(args[0].charAt(0)) + baseColor + args[0].substring(1).toLowerCase() + trimColor + ChatColor.BOLD + " >";
        String header = mm.formatHeaderLength(headerContent, this.getClass());
        String footer = baseColor + "-" + trimColor + "Oo" + baseColor + "__________" + trimColor + "_____• " + ChatColor.GRAY + "Page " + requestedPage + " of " + listings.size() + trimColor + " •_____" + baseColor + "__________" + trimColor + "oO" + baseColor + "-";
        
        List<Offer> pageContent = null;
        try{
        	pageContent = listings.getPage(requestedPage-1);
        } catch(IndexOutOfBoundsException e){
        	mm.invalidPage(sender);
			return;
        }
        
        sender.sendMessage(header);
        for(Offer o : pageContent)
        	sender.sendMessage(mm.formatOffer(o));
        sender.sendMessage(footer);        
    }
}

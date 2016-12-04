package com.dscalzi.virtualshop.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.ChatManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.Numbers;
import com.dscalzi.virtualshop.util.PageList;

import java.util.List;

public class Find implements CommandExecutor{
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	public Find(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.idb = ItemDB.getInstance();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!sender.hasPermission("virtualshop.merchant.find")){
            cm.noPermissions(sender);
            return true;
        }
		if(args.length < 1){
			cm.sendError(sender, "You need to specify the item.");
			return true;
		}
		
		 this.execute(sender, args);
		return true;
	}
	
    public void execute(CommandSender sender, String[] args){
    	final String baseColor = configM.getBaseColor();
    	final String trimColor = configM.getTrimColor();
    	
    	ItemStack item = idb.get(args[0], 0);
    	if(item == null){
    		cm.wrongItem(sender, args[0]);
    		return;
    	}
    	
    	List<Offer> offers = dbm.getPrices(item);
    	if(offers.size() == 0){
    		cm.sendError(sender, "No one is selling " + cm.formatItem(args[0]));
            return;
    	}
    	
    	int requestedPage = 1;
    	if(args.length > 1) requestedPage = Numbers.parseInteger(args[1]);
    	
    	PageList<Offer> listings = new PageList<Offer>(7, offers);
    	
        String headerContent = trimColor + "" + ChatColor.BOLD + "< " + baseColor + ChatColor.BOLD + "L" + baseColor + "istings ◄► " + ChatColor.BOLD + Character.toUpperCase(args[0].charAt(0)) + baseColor + args[0].substring(1).toLowerCase() + trimColor + ChatColor.BOLD + " >";
        String header = cm.formatHeaderLength(headerContent, this.getClass());
        String footer = baseColor + "-" + trimColor + "Oo" + baseColor + "__________" + trimColor + "_____• " + ChatColor.GRAY + "Page " + requestedPage + " of " + listings.size() + trimColor + " •_____" + baseColor + "__________" + trimColor + "oO" + baseColor + "-";
        
        List<Offer> pageContent = null;
        try{
        	pageContent = listings.getPage(requestedPage-1);
        } catch(IndexOutOfBoundsException e){
        	cm.sendError(sender, "Page does not exist");
			return;
        }
        
        sender.sendMessage(header);
        for(Offer o : pageContent)
        	sender.sendMessage(cm.formatOffer(o));
        sender.sendMessage(footer);        
    }
}

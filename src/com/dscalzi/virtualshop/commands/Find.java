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
import com.dscalzi.virtualshop.util.ItemDb;
import com.dscalzi.virtualshop.util.Numbers;
import com.dscalzi.virtualshop.util.PageList;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;

public class Find implements CommandExecutor{
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	
	public Find(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!sender.hasPermission("virtualshop.find")){
            cm.noPermissions(sender);
            return true;
        }
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			cm.denyBeta(sender);
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
    	
    	ItemStack item = ItemDb.get(args[0], 0);
    	if(item == null){
    		cm.wrongItem(sender, args[0]);
    		return;
    	}
    	
    	List<Offer> offers = DatabaseManager.getPrices(item);
    	if(offers.size() == 0){
    		cm.sendError(sender, "No one is selling " + cm.formatItem(args[0]));
            return;
    	}
    	
    	int requestedPage = 1;
    	if(args.length > 1) requestedPage = Numbers.parseInteger(args[1]);
    	
    	PageList<Offer> listings = new PageList<Offer>(offers, 7);
    	List<String> finalMsg = new ArrayList<String>();
    	
        int charCount = 74;
        String header = trimColor + "" + ChatColor.BOLD + "< " + baseColor + ChatColor.BOLD + "L" + baseColor + "istings ◄► " + ChatColor.BOLD + Character.toUpperCase(args[0].charAt(0)) + baseColor + args[0].substring(1) + trimColor + ChatColor.BOLD + " >";
        charCount -= header.length()-1;
        if(charCount % 2 == 0)
        	charCount -= 1;
        String left = baseColor + "";
        String right = trimColor + "";
        for(int i=0; i<charCount/2-1; ++i) left += "-";
        for(int i=0; i<charCount/2-1; ++i) right += "-";
        finalMsg.add(left + header + right);
        
        try {
			for(Offer o : listings.getPage(requestedPage)){
				finalMsg.add(cm.formatOffer(o));
			}
		} catch (BadLocationException e) {
			cm.sendError(sender, "Page does not exist");
			return;
		}
        finalMsg.add(baseColor + "-" + trimColor + "Oo" + baseColor + "__________" + trimColor + "_____• " + ChatColor.GRAY + "Page " + requestedPage + " of " + listings.getTotalPages() + trimColor + " •_____" + baseColor + "__________" + trimColor + "oO" + baseColor + "-");
        
        for(String s : finalMsg)
        	sender.sendMessage(s);
        
    }
}

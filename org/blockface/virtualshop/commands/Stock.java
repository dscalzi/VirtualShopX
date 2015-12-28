package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.objects.Offer;
import org.blockface.virtualshop.util.Numbers;
import org.blockface.virtualshop.util.PageList;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;

public class Stock implements CommandExecutor{
	
	private VirtualShop plugin;
	
	public Stock(VirtualShop plugin){
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!sender.hasPermission("virtualshop.stock")){
            Chatty.noPermissions(sender);
            return true;
        }
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			Chatty.denyBeta(sender);
			return true;
		}
		if(args.length > 0 && args[0].contains("'")){
        	Chatty.noStock(sender, args[0]);
        	return true;
        }
		
		try{
			this.execute(sender, args);
    	} catch (LinkageError e){
    		Chatty.sendError(sender, "Linkage error occurred. Please restart the server to fix.");
    	}
		return true;
	}
	
    @SuppressWarnings("deprecation")
	public void execute(CommandSender sender, String[] args) throws LinkageError {
    	
        OfflinePlayer target;
        int start = 1;
        List<Offer> offers;
        offers = DatabaseManager.getBestPrices();
        String header = ChatColor.GOLD + "" + ChatColor.BOLD + "< " + ChatColor.WHITE + ChatColor.BOLD + "I" + ChatColor.WHITE + "tem " + ChatColor.BOLD + "S" + ChatColor.WHITE + "tock ◄► AtlasNetwork " + ChatColor.GOLD + ChatColor.BOLD + " >";;
        if(args.length>0)  
        	start = Numbers.parseInteger(args[0]);
        if(start < 0){
        	target = plugin.getServer().getOfflinePlayer(args[0]);
            String seller = args[0];
			if(args.length > 1) 
				start = Numbers.parseInteger(args[1]);
			if(start < 0) 
				start = 1;
			try{
				offers = DatabaseManager.searchBySeller(seller);
			} catch (NullPointerException e){
				Chatty.noStock(sender, target.getName());
			}
            if(offers.size() < 1){
            	Chatty.noStock(sender, target.getName());
            	return;
            }
            for(Offer o : offers){
            	if(o.seller.toLowerCase().indexOf(target.getName().toLowerCase()) != -1){
            		header = ChatColor.GOLD + "" + ChatColor.BOLD + "< " + ChatColor.WHITE + ChatColor.BOLD + "I" + ChatColor.WHITE + "tem " + ChatColor.BOLD + "S" + ChatColor.WHITE + "tock ◄► " + o.seller + ChatColor.GOLD + ChatColor.BOLD + " >";
            		break;
            	}
            }
        }
        
        int charCount = 74;
        charCount -= header.length()-1;
        if(charCount % 2 == 0)
        	charCount -= 1;
        String left = ChatColor.WHITE + "";
        String right = ChatColor.GOLD + "";
        for(int i=0; i<charCount/2-1; ++i)
        	left += "-";
        for(int i=0; i<charCount/2-1; ++i)
        	right += "-";
        
        PageList<Offer> stock = new PageList<>(offers, 7);
        List<String> finalMsg = new ArrayList<String>();
        finalMsg.add(left + header + right);
        
        try {
			for(Offer o : stock.getPage(start)){
				finalMsg.add(Chatty.formatOffer(o));
			}
		} catch (BadLocationException e) {
			Chatty.sendError(sender, "Page does not exist");
			return;
		}
        
        finalMsg.add(ChatColor.WHITE + "-" + ChatColor.GOLD + "Oo" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "_____• " + ChatColor.GRAY + "Page " + start + " of " + stock.getTotalPages() + ChatColor.GOLD + " •_____" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "oO" + ChatColor.WHITE + "-");
        
        
        for(String s : finalMsg)
        	sender.sendMessage(s);
        
    }

}


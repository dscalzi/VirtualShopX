package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.objects.Offer;
import org.blockface.virtualshop.util.Numbers;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

public class Stock
{
    @SuppressWarnings("deprecation")
	public static void execute(CommandSender sender, String[] args, VirtualShop plugin)
    {
        if(!sender.hasPermission("virtualshop.stock")){
            Chatty.noPermissions(sender);
            return;
        }
        if(args.length > 0 && args[0].contains("'")){
        	Chatty.noStock(sender, args[0]);
        	return;
        }
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
			start = (start -1) * 8;
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
            	if(o.seller.contains(target.getName())){
            		header = ChatColor.GOLD + "" + ChatColor.BOLD + "< " + ChatColor.WHITE + ChatColor.BOLD + "I" + ChatColor.WHITE + "tem " + ChatColor.BOLD + "S" + ChatColor.WHITE + "tock ◄► " + o.seller + ChatColor.GOLD + ChatColor.BOLD + " >";
            		break;
            	}
            }
        }
        else 
        	start = (start-1) * 8;

        int page = start/8 + 1;
        int pages = offers.size()/8 + 1;
        if(page > pages){
            start = 0;
            page = 1;
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
        
        sender.sendMessage(left + header + right);
        
        int count =0;
        for(Offer o : offers){
            if(count==start+8) 
            	break;
            if(count >= start){
                sender.sendMessage(Chatty.formatOffer(o));
            }
            count++;
        }
        
        sender.sendMessage(ChatColor.WHITE + "-" + ChatColor.GOLD + "Oo" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "_____• " + ChatColor.GRAY + "Page " + page + " of " + pages + ChatColor.GOLD + " •_____" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "oO" + ChatColor.WHITE + "-");
        
    }

}


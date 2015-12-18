package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.objects.Offer;
import org.blockface.virtualshop.util.ItemDb;
import org.blockface.virtualshop.util.Numbers;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Find
{
    public static void execute(CommandSender sender, String[] args, VirtualShop plugin)
    {
        if(!sender.hasPermission("virtualshop.find"))
        {
            Chatty.noPermissions(sender);
            return;
        }
        if(args.length < 1)
		{
			Chatty.sendError(sender, "You need to specify the item.");
			return;
		}
		ItemStack item = ItemDb.get(args[0], 0);
		if(item==null)
		{
			Chatty.wrongItem(sender, args[0]);
			return;
		}
        int page = 1;
		List<Offer> offers = DatabaseManager.getPrices(item);
        if(args.length>1)  page = Numbers.parseInteger(args[1]);
		if(offers.size()==0)
		{
			Chatty.sendError(sender, "No one is selling " + Chatty.formatItem(args[0]) + ".");
            return;
		}

        int start = (page-1) * 9;
        int pages = offers.size()/9 + 1;
		int count=0;
        if(page > pages)
        {
            start = 0;
            page = 1;
        }
        int charCount = 74;
        String header = ChatColor.GOLD + "" + ChatColor.BOLD + "< " + ChatColor.WHITE + ChatColor.BOLD + "L" + ChatColor.WHITE + "istings ◄► " + ChatColor.BOLD + Character.toUpperCase(args[0].charAt(0)) + ChatColor.WHITE + args[0].substring(1) + ChatColor.GOLD + ChatColor.BOLD + " >";
        charCount -= header.length()-1;
        if(charCount % 2 == 0)
        	charCount -= 1;
        String left = ChatColor.WHITE + "";
        String right = ChatColor.GOLD + "";
        for(int i=0; i<charCount/2-1; ++i){
        	left += "-";
        }
        for(int i=0; i<charCount/2-1; ++i){
        	right += "-";
        }
        sender.sendMessage(left + header + right);
        //sender.sendMessage(ChatColor.DARK_GRAY + "---------------" + ChatColor.GRAY + "Page (" + ChatColor.RED + page + ChatColor.GRAY + " of " + ChatColor.RED +pages + ChatColor.GRAY + ")" + ChatColor.DARK_GRAY + "---------------");
        for(Offer o : offers)
        {
            if(count==start+9) break;
            if(count >= start)
            {
                sender.sendMessage(Chatty.formatOffer(o));
            }
            count++;
		}
        sender.sendMessage(ChatColor.WHITE + "-" + ChatColor.GOLD + "Oo" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "_____• " + ChatColor.GRAY + "Page " + page + " of " + pages + ChatColor.GOLD + " •_____" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "oO" + ChatColor.WHITE + "-");
    }
}

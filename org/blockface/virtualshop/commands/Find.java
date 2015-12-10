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
			Chatty.sendError(sender, "No one is selling " + args[0]);
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
        sender.sendMessage(ChatColor.DARK_GRAY + "---------------" + ChatColor.GRAY + "Page (" + ChatColor.RED + page + ChatColor.GRAY + " of " + ChatColor.RED +pages + ChatColor.GRAY + ")" + ChatColor.DARK_GRAY + "---------------");
        for(Offer o : offers)
        {
            if(count==start+9) break;
            if(count >= start)
            {
                sender.sendMessage(Chatty.formatOffer(o));
            }
            count++;
		}
    }
}

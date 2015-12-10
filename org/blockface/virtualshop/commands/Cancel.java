package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.objects.Offer;
import org.blockface.virtualshop.util.InventoryManager;
import org.blockface.virtualshop.util.ItemDb;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Cancel {

    public static void execute(CommandSender sender, String[] args, VirtualShop plugin)
    {
        if(!(sender instanceof Player))
        {
            Chatty.denyConsole(sender);
            return;
        }
        if(!sender.hasPermission("virtualshop.cancel"))
        {
            Chatty.noPermissions(sender);
            return;
        }
        if(args.length < 1)
        {
            Chatty.sendError(sender, "You must specify an item.");
            return;
        }
        ItemStack item = ItemDb.get(args[0], 0);
		if(item==null)
		{
			Chatty.wrongItem(sender, args[0]);
			return;
		}
        Player player = (Player)sender;
        int total = 0;
        for(Offer o: DatabaseManager.getSellerOffers(player.getName(),item))
        {
            total += o.item.getAmount();
        }
		if(total == 0)
		{
			Chatty.sendError(sender,"You do not have any " + args[0] + " for sale.");
			return;
		}
        item.setAmount(total);
        new InventoryManager(player).addItem(item);
        DatabaseManager.removeSellerOffers(player,item);
        Chatty.sendSuccess(sender, "Removed " + Chatty.formatAmount(total) + " " + Chatty.formatItem(args[0]));


    }

}

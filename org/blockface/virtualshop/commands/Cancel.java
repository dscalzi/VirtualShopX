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
        if(!(sender instanceof Player)){
            Chatty.denyConsole(sender);
            return;
        }
        if(!sender.hasPermission("virtualshop.cancel")){
            Chatty.noPermissions(sender);
            return;
        }
        if(args.length < 2){
            Chatty.sendError(sender, "Proper usage is /cancel <amount> <item>");
            return;
        }
        
        ItemStack item = ItemDb.get(args[1], 0);
        int cancelAmt = 0;
        
		if(item==null)
		{
			Chatty.wrongItem(sender, args[1]);
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
			Chatty.sendError(sender,"You do not have any " + args[1] + " for sale.");
			return;
		}
		
		if(args[0].equalsIgnoreCase("all")){
			cancelAmt = total;
		}
		else{
        	try{
        		cancelAmt = Integer.parseInt(args[0]);
        		if(cancelAmt < 1){
        			Chatty.sendError(sender, "That is not a proper number");
            		return;
        		}
        	} catch (NumberFormatException e){
        		Chatty.sendError(sender, "That is not a proper number");
        		return;
        	}
		}
		
		if(cancelAmt > total)
			cancelAmt = total;
		
        item.setAmount(cancelAmt);
        new InventoryManager(player).addItem(item);
        int a = 0;
        double oPrice = 0;
        for(Offer o: DatabaseManager.getSellerOffers(player.getName(),item)){
        	a += o.item.getAmount();
        	oPrice += o.price;
        }
        DatabaseManager.removeSellerOffers(player,item);
        a -= cancelAmt;
        if(a > 0){
        	item.setAmount(a);
        	Offer o = new Offer(player.getName(),item,oPrice);
        	DatabaseManager.addOffer(o);
        }
        Chatty.sendSuccess(sender, "Removed " + Chatty.formatAmount(cancelAmt) + " " + Chatty.formatItem(args[1]));


    }

}

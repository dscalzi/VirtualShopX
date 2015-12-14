package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.objects.Offer;
import org.blockface.virtualshop.util.InventoryManager;
import org.blockface.virtualshop.util.ItemDb;
import org.bukkit.GameMode;
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
        Player player = (Player)sender;
        if(!sender.hasPermission("virtualshop.cancel")){
            Chatty.noPermissions(sender);
            return;
        }
        if((player.getGameMode() != GameMode.SURVIVAL) && (player.getGameMode() != GameMode.ADVENTURE)){
        	Chatty.invalidGamemode(sender, player.getGameMode());
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
        			Chatty.numberFormat(sender);
            		return;
        		}
        	} catch (NumberFormatException e){
        		Chatty.numberFormat(sender);
        		return;
        	}
		}
		
		if(cancelAmt > total)
			cancelAmt = total;
		
        item.setAmount(cancelAmt);
        ItemStack[] inv = player.getInventory().getContents();
        int openNum = 0;
        for(int i=0; i<inv.length; ++i){
    		if(inv[i] == null){
    			openNum += 64;
    			continue;
    		} else if(inv[i].getType() == item.getType()){
    			openNum += (64-inv[i].getAmount());
    		}
    	}
        new InventoryManager(player).addItem(item);
        if(openNum < cancelAmt){
        	item.setAmount(cancelAmt-openNum);
        	player.getWorld().dropItem(player.getLocation(), item);
        }
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

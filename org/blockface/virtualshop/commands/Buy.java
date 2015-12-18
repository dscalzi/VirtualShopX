package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.objects.Offer;
import org.blockface.virtualshop.objects.Transaction;
import org.blockface.virtualshop.util.InventoryManager;
import org.blockface.virtualshop.util.ItemDb;
import org.blockface.virtualshop.util.Numbers;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@SuppressWarnings("deprecation")
public class Buy {

    public static void execute(CommandSender sender, String[] args, VirtualShop plugin)
    {
		if(!(sender instanceof Player))	{
			Chatty.denyConsole(sender);
			return;
		}
		Player player = (Player)sender;
        if(!sender.hasPermission("virtualshop.buy")){
            Chatty.noPermissions(sender);
            return;
        }
        if((player.getGameMode() != GameMode.SURVIVAL) && (player.getGameMode() != GameMode.ADVENTURE)){
        	Chatty.invalidGamemode(sender, player.getGameMode());
        	return;
        }
		if(args.length < 2)
		{
			Chatty.sendError(sender, "Proper usage is /buy <amount> <item> [maxprice]");
			return;
		}
		int amount = Numbers.parseInteger(args[0]);
		if(amount < 0)		{
			Chatty.numberFormat(sender);
			return;
		}
		
		if(amount == Numbers.ALL){
			Chatty.numberFormat(sender);
			return;
		}
		
        double maxprice = Double.MAX_VALUE-1;
        if(args.length > 2){
        	maxprice = Numbers.parseDouble(args[2]);
        	if(maxprice < 0){
        		Chatty.numberFormat(sender);
        		return;
        	}
        }

		ItemStack item = ItemDb.get(args[1], 0);
		if(item==null)
		{
			Chatty.wrongItem(sender, args[1]);
			return;
		}
        int bought = 0;
        double spent = 0;
        InventoryManager im = new InventoryManager(player);
        List<Offer> offers = DatabaseManager.getItemOffers(item);
        if(offers.size()==0) {
            Chatty.sendError(sender,"There is no " + Chatty.formatItem(args[1])+ " for sale.");
            return;
        }
        
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
        
        boolean tooHigh = false;
        @SuppressWarnings("unused")
		boolean boughtAny = false;
        
        for(Offer o: offers)
        {
            if(o.price > maxprice){
            	tooHigh = true;
            	continue;
            }
            if(o.seller.equals(player.getName())) continue;
            if((amount - bought) >= o.item.getAmount())
            {
                int canbuy = o.item.getAmount();
                double cost = o.price * canbuy;

                //Revise amounts if not enough money.
                if(!plugin.hasEnough(player.getName(), cost))
                {
                    canbuy = (int)(VirtualShop.econ.getBalance(player.getName()) / o.price);
                    cost = canbuy*o.price;
                    if(canbuy < 1)
                    {
							Chatty.sendError(player,"Ran out of money!");
							break;
                    }
                }
                bought += canbuy;
                spent += cost;
                VirtualShop.econ.withdrawPlayer(player.getName(), cost);
                VirtualShop.econ.depositPlayer(o.seller, cost);
                Chatty.sendSuccess(o.seller, Chatty.formatSeller(player.getName()) + " just bought " + Chatty.formatAmount(canbuy) + " " + Chatty.formatItem(args[1]) + " for " + Chatty.formatPrice(cost));
                int left = o.item.getAmount() - canbuy;
                if(left < 1) DatabaseManager.deleteItem(o.id);
                else DatabaseManager.updateQuantity(o.id, left);
                Transaction t = new Transaction(o.seller, player.getName(), o.item.getTypeId(), o.item.getDurability(), canbuy, cost);
                DatabaseManager.logTransaction(t);
                boughtAny = true;
            }
            else
            {
                int canbuy = amount - bought;
                double cost = canbuy * o.price;

                //Revise amounts if not enough money.
                if(!plugin.hasEnough(player.getName(), cost))
                {
                    canbuy = (int)(VirtualShop.econ.getBalance(player.getName()) / o.price);
                    cost = canbuy*o.price;
                    if(canbuy < 1)
                    {
							Chatty.sendError(player,"Ran out of money!");
							break;
                    }
                }
                bought += canbuy;
                spent += cost;
                VirtualShop.econ.withdrawPlayer(player.getName(), cost);
                VirtualShop.econ.depositPlayer(o.seller, cost);
                Chatty.sendSuccess(o.seller, Chatty.formatSeller(player.getName()) + " just bought " + Chatty.formatAmount(canbuy) + " " + Chatty.formatItem(args[1]) + " for " + Chatty.formatPrice(cost));
                int left = o.item.getAmount() - canbuy;
                DatabaseManager.updateQuantity(o.id, left);
                Transaction t = new Transaction(o.seller, player.getName(), o.item.getTypeId(), o.item.getDurability(), canbuy, cost);
                DatabaseManager.logTransaction(t);
            }
            if(bought >= amount) break;

        }

        item.setAmount(bought);
        if(openNum < bought){
        	item.setAmount(bought-openNum);
        	player.getWorld().dropItem(player.getLocation(), item);
        }
        if(bought > 0) im.addItem(item);
        if(tooHigh && bought == 0 && args.length > 2)
        	Chatty.sendError(player,"No one is selling " + Chatty.formatItem(args[1]) + " cheaper than " + Chatty.formatPrice(maxprice));
        else
        	Chatty.sendSuccess(player,"Managed to buy " + Chatty.formatAmount(bought) + " " + Chatty.formatItem(args[1]) + " for " + Chatty.formatPrice(spent));
    }
}

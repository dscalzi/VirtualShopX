package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.ConfigManager;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.objects.Offer;
import org.blockface.virtualshop.util.InventoryManager;
import org.blockface.virtualshop.util.ItemDb;
import org.blockface.virtualshop.util.Numbers;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Sell
{
    public static void execute(CommandSender sender, String[] args, VirtualShop plugin)
    {
        if(!(sender instanceof Player))
        {
            Chatty.denyConsole(sender);
            return;
        }
        if(!sender.hasPermission("virtualshop.sell"))
        {
            Chatty.noPermissions(sender);
            return;
        }
        if(args.length < 3)
		{
			Chatty.sendError(sender, "Proper usage is /sell <amount> <item> <price>");
			return;
		}
        float price = Numbers.parseFloat(args[2]);
		int amount = Numbers.parseInteger(args[0]);
		if(amount < 0 || price < 0)
		{
			Chatty.numberFormat(sender);
			return;
		}
        Player player = (Player)sender;
        ItemStack item = ItemDb.get(args[1], amount);
		if(args[1].equalsIgnoreCase("hand"))
		{
			item=new ItemStack(player.getItemInHand().getType(),amount, player.getItemInHand().getDurability());
			args[1] = ItemDb.reverseLookup(item);
		}
		if(item==null)
		{
			Chatty.wrongItem(sender, args[1]);
			return;
		}
        InventoryManager im = new InventoryManager(player);
        if(amount == Numbers.ALL){
        	ItemStack[] inv = player.getInventory().getContents();
        	int total = 0;
        	for(int i=0; i<inv.length; ++i){
        		if(inv[i] == null){
        			continue;
        		} else if(inv[i].getType() == item.getType()){
        			total += inv[i].getAmount();
        			player.sendMessage("" + total);
        		}
        	}
        	item.setAmount(total);
        }
		if(!im.contains(item,true,true))
		{
			Chatty.sendError(sender, "You do not have " + Chatty.formatAmount(item.getAmount()) + " " + Chatty.formatItem(args[1]));
			return;
		}
        im.remove(item, true, true);
        int a = 0;
        for(Offer o: DatabaseManager.getSellerOffers(player.getName(),item)) {a += o.item.getAmount();}
        DatabaseManager.removeSellerOffers(player,item);
        item.setAmount(item.getAmount() + a);
        Offer o = new Offer(player.getName(),item,price);
		DatabaseManager.addOffer(o);
        if(ConfigManager.broadcastOffers())
        {
			Chatty.broadcastOffer(o);
			return;
		}

    }
}

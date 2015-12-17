package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.ConfigManager;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.objects.Offer;
import org.blockface.virtualshop.util.InventoryManager;
import org.blockface.virtualshop.util.ItemDb;
import org.blockface.virtualshop.util.Numbers;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Sell
{
    @SuppressWarnings("deprecation")
	public static void execute(CommandSender sender, String[] args, VirtualShop plugin)
    {
        if(!(sender instanceof Player))
        {
            Chatty.denyConsole(sender);
            return;
        }
        Player player = (Player)sender;
        if(!sender.hasPermission("virtualshop.sell"))
        {
            Chatty.noPermissions(sender);
            return;
        }
        if((player.getGameMode() != GameMode.SURVIVAL) && (player.getGameMode() != GameMode.ADVENTURE)){
        	Chatty.invalidGamemode(sender, player.getGameMode());
        	return;
        }
        if(args.length < 3)
		{
			Chatty.sendError(sender, "Proper usage is /sell <amount> <item> <price>");
			return;
		}
        double price = Numbers.parseDouble(args[2]);
		int amount = Numbers.parseInteger(args[0]);
		if(amount < 0 || price < 0)
		{
			Chatty.numberFormat(sender);
			return;
		}
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
		if(price > ConfigManager.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData())){
			Chatty.priceTooHigh(sender, args[1], ConfigManager.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData()));
			return;
		}
        InventoryManager im = new InventoryManager(player);
        if(amount == Numbers.ALL && args[0].equalsIgnoreCase("all")){
        	ItemStack[] inv = player.getInventory().getContents();
        	int total = 0;
        	for(int i=0; i<inv.length; ++i){
        		if(inv[i] == null){
        			continue;
        		} else if(inv[i].getType() == item.getType()){
        			total += inv[i].getAmount();
        		}
        	}
        	item.setAmount(total);
        }
		if(!im.contains(item,true,true))
		{
			if(item.getAmount() == 0)
        		Chatty.sendError(sender, "You do not have any " + Chatty.formatItem(args[1]));
			else
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

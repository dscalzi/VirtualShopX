package com.dscalzi.virtualshop.commands;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.ChatManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.InventoryManager;

public class Cancel implements CommandExecutor{

	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	
	public Cancel(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.idb = ItemDB.getInstance();
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
            cm.denyConsole(sender);
            return true;
        }
		if(!sender.hasPermission("virtualshop.merchant.cancel")){
            cm.noPermissions(sender);
            return true;
        }
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			cm.denyBeta(sender);
			return true;
		}
		Player player = (Player)sender;
		if(!configM.getAllowedWorlds().contains(player.getWorld().getName())){
			cm.invalidWorld(sender, command.getName(), player.getWorld());
			return true;
		}
		if((player.getGameMode() != GameMode.SURVIVAL) && (player.getGameMode() != GameMode.ADVENTURE)){
        	cm.invalidGamemode(sender, command.getName(), player.getGameMode());
        	return true;
        }
		if(args.length < 2){
            cm.sendError(sender, "Proper usage is /cancel <amount> <item>");
            return true;
        }
		
		this.execute(player, args);
		return true;
	}
	
    public void execute(Player player, String[] args){
        
        ItemStack item = idb.get(args[1], 0);
        int cancelAmt = 0;
        
		if(item==null)
		{
			cm.wrongItem(player, args[1]);
			return;
		}
		
        int total = 0;
        for(Offer o: dbm.getSellerOffers(player.getUniqueId(),item))
        {
            total += o.getItem().getAmount();
        }
		if(total == 0)
		{
			cm.sendError(player,"You do not have any " + args[1] + " for sale.");
			return;
		}
		
		if(args[0].equalsIgnoreCase("all")){
			cancelAmt = total;
		}
		else{
        	try{
        		cancelAmt = Integer.parseInt(args[0]);
        		if(cancelAmt < 1){
        			cm.numberFormat(player);
            		return;
        		}
        	} catch (NumberFormatException e){
        		cm.numberFormat(player);
        		return;
        	}
		}
		
		if(cancelAmt > total)
			cancelAmt = total;
		
        item.setAmount(cancelAmt);
        new InventoryManager(player).addItem(item);
        int a = 0;
        double oPrice = 0;
        for(Offer o: dbm.getSellerOffers(player.getUniqueId(),item)){
        	a += o.getItem().getAmount();
        	oPrice += o.getPrice();
        }
        dbm.removeSellerOffers(player.getUniqueId(),item);
        a -= cancelAmt;
        if(a > 0){
        	item.setAmount(a);
        	Offer o = new Offer(player.getUniqueId(),item,oPrice);
        	dbm.addOffer(o);
        }
        cm.sendSuccess(player, "Removed " + cm.formatAmount(cancelAmt) + " " + cm.formatItem(args[1]));


    }

}

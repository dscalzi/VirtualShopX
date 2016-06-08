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
import com.dscalzi.virtualshop.util.InventoryManager;
import com.dscalzi.virtualshop.util.ItemDb;

public class Cancel implements CommandExecutor{

	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	
	public Cancel(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
            cm.denyConsole(sender);
            return true;
        }
		if(!sender.hasPermission("virtualshop.cancel")){
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
        
        ItemStack item = ItemDb.get(args[1], 0);
        int cancelAmt = 0;
        
		if(item==null)
		{
			cm.wrongItem(player, args[1]);
			return;
		}
		
        int total = 0;
        for(Offer o: DatabaseManager.getSellerOffers(player.getName(),item))
        {
            total += o.item.getAmount();
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
        cm.sendSuccess(player, "Removed " + cm.formatAmount(cancelAmt) + " " + cm.formatItem(args[1]));


    }

}

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
import com.dscalzi.virtualshop.managers.ConfirmationManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.CancelData;
import com.dscalzi.virtualshop.objects.Confirmable;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.InventoryManager;

public class Cancel implements CommandExecutor, Confirmable{

	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	private final ConfirmationManager confirmations;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	
	public Cancel(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
		this.confirmations = ConfirmationManager.getInstance();
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
		if(args.length > 0){
			if(args[0].equalsIgnoreCase("confirm")){
				if(args.length > 1){
					if(args.length > 2 && args[1].equalsIgnoreCase("toggle") ){
						toggleConfirmations(player, label, args);
						return true;
					}
					if(args[1].equalsIgnoreCase("toggle")){
						toggleConfirmations(player, label, args);
						return true;
					}
				}
				this.confirm(player);
				return true;
			}
		}
		if(args.length < 2){
            cm.sendError(sender, "Proper usage is /" + label + " <amount> <item>");
            return true;
        }
		
		confirmations.unregister(this.getClass(), player);
		this.execute(player, label, args);
		return true;
	}
	
	public void execute(Player player, String label, String[] args){
		if(!dbm.getToggle(player.getUniqueId(), this.getClass())){
			if(this.validateData(player, args)){
				this.finalizeCancel(player, (CancelData)confirmations.retrieve(this.getClass(), player));
				return;
			}
			return;
		}
		if(this.validateData(player, args))
			cm.cancelConfirmation(player, label, (CancelData)confirmations.retrieve(this.getClass(), player));
	}
	
    public boolean validateData(Player player, String[] args){
        
        ItemStack item = idb.get(args[1], 0);
        int cancelAmt = 0;
        
		if(item==null){
			cm.wrongItem(player, args[1]);
			return false;
		}
		
        int total = 0;
        for(Offer o: dbm.getSellerOffers(player.getUniqueId(),item)){
            total += o.getItem().getAmount();
        }
		if(total == 0){
			cm.noSpecificStock(player, args[1]);
			return false;
		}
		
		if(args[0].equalsIgnoreCase("all")){
			cancelAmt = total;
		}
		else{
        	try{
        		cancelAmt = Integer.parseInt(args[0]);
        		if(cancelAmt < 1){
        			cm.numberFormat(player);
            		return false;
        		}
        	} catch (NumberFormatException e){
        		cm.numberFormat(player);
        		return false;
        	}
		}
		
		if(cancelAmt > total)
			cancelAmt = total;
		
        item.setAmount(cancelAmt);

        CancelData data = new CancelData(cancelAmt, total, new InventoryManager(player).getFreeSpace(item), item, System.currentTimeMillis(), args);
        confirmations.register(this.getClass(), player, data);
        return true;
    }
    
    private void finalizeCancel(Player player, CancelData data){
    	ItemStack item = data.getItem();
    	int cancelAmt = data.getAmount();
    	
    	new InventoryManager(player).addItem(item);
        int a = 0;
        double oPrice = 0;
        for(Offer o: dbm.getSellerOffers(player.getUniqueId(), item)){
        	a += o.getItem().getAmount();
        	oPrice += o.getPrice();
        }
        dbm.removeSellerOffers(player.getUniqueId(), item);
        a -= cancelAmt;
        if(a > 0){
        	item.setAmount(a);
        	Offer o = new Offer(player.getUniqueId(), item, oPrice);
        	dbm.addOffer(o);
        }
        cm.sendSuccess(player, "Removed " + cm.formatAmount(cancelAmt) + " " + cm.formatItem(data.getArgs()[1]));
    }
    
    private void confirm(Player player){
		if(!confirmations.contains(this.getClass(), player)){
			cm.invalidConfirmation(player);
			return;
		}
		CancelData initialData = (CancelData) confirmations.retrieve(this.getClass(), player);
		validateData(player, initialData.getArgs());
		CancelData currentData = (CancelData) confirmations.retrieve(this.getClass(), player);
		long timeElapsed = System.currentTimeMillis() - initialData.getTransactionTime();
		if(timeElapsed > 15000){
			cm.confirmationExpired(player);
			confirmations.unregister(this.getClass(), player);
			return;
		}
		if(!currentData.equals(initialData)){
			cm.invalidConfirmData(player);
			confirmations.unregister(this.getClass(), player);
			return;
		}
		finalizeCancel(player, initialData);
	}
    
    private void toggleConfirmations(Player player, String label, String[] args){
		if(args.length < 3){
			cm.sendMessage(player, "You may turn cancel confirmations on or off using /" + label + " confirm toggle <on/off>");
			return;
		}
		String value = args[2];
		if(value.equalsIgnoreCase("on")){
			cm.sendSuccess(player, "Cancel confirmations turned on. To undo this /" + label + " confirm toggle off");
			dbm.updateToggle(player.getUniqueId(), this.getClass(), true);
			return;
		}
			
		if(value.equalsIgnoreCase("off")){
			cm.sendSuccess(player, "Cancel confirmations turned off. To undo this /" + label + " confirm toggle on");
			confirmations.unregister(this.getClass(), player);
			dbm.updateToggle(player.getUniqueId(), this.getClass(), false);
			return;
		}
		cm.sendMessage(player, "You may turn Cancel confirmations on or off using /" + label + " confirm toggle <on/off>");
	}

}

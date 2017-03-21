/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.commands;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.MessageManager;
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
	private final MessageManager mm;
	private final ConfigManager cm;
	private final ConfirmationManager confirmations;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	
	public Cancel(VirtualShop plugin){
		this.plugin = plugin;
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.confirmations = ConfirmationManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.idb = ItemDB.getInstance();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
            mm.denyConsole(sender);
            return true;
        }
		if(!sender.hasPermission("virtualshop.merchant.cancel.regular")){
            mm.noPermissions(sender);
            return true;
        }
		Player player = (Player)sender;
		if(!cm.getAllowedWorlds().contains(player.getWorld().getName())){
			mm.invalidWorld(sender, command.getName(), player.getWorld());
			return true;
		}
		if((player.getGameMode() != GameMode.SURVIVAL) && (player.getGameMode() != GameMode.ADVENTURE)){
        	mm.invalidGamemode(sender, command.getName(), player.getGameMode());
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
            mm.sendError(sender, "Proper usage is /" + label + " <amount> <item>");
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
			mm.cancelConfirmation(player, label, (CancelData)confirmations.retrieve(this.getClass(), player));
	}
	
    public boolean validateData(Player player, String[] args){
        
        ItemStack item = idb.get(args[1], 0);
        int cancelAmt = 0;
        
        PlayerInventory im = player.getInventory();
        if(args[1].equalsIgnoreCase("hand") || args[1].equalsIgnoreCase("mainhand")){
			item = new ItemStack(im.getItemInMainHand());
			if(item.getType() == Material.AIR){
				mm.holdingNothing(player);
				return false;
			}
			args[1] = idb.reverseLookup(item);
		} else if(args[1].equalsIgnoreCase("offhand")){
			item = new ItemStack(im.getItemInOffHand());
			if(item.getType() == Material.AIR){
				mm.holdingNothing(player);
				return false;
			}
			args[1] = idb.reverseLookup(item);
		}
        
		if(item==null){
			mm.wrongItem(player, args[1]);
			return false;
		}
		
        int total = 0;
        for(Offer o: dbm.getSellerOffers(player.getUniqueId(),item)){
            total += o.getItem().getAmount();
        }
		if(total == 0){
			mm.noSpecificStock(player, args[1]);
			return false;
		}
		
		if(args[0].equalsIgnoreCase("all")){
			cancelAmt = total;
		}
		else{
        	try{
        		cancelAmt = Integer.parseInt(args[0]);
        		if(cancelAmt < 1){
        			mm.numberFormat(player);
            		return false;
        		}
        	} catch (NumberFormatException e){
        		mm.numberFormat(player);
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
        mm.sendSuccess(player, "Removed " + mm.formatAmount(cancelAmt) + " " + mm.formatItem(data.getArgs()[1], true) + mm.getSuccessColor() + ".");
    }
    
    private void confirm(Player player){
		if(!confirmations.contains(this.getClass(), player)){
			mm.invalidConfirmation(player);
			return;
		}
		CancelData initialData = (CancelData) confirmations.retrieve(this.getClass(), player);
		validateData(player, initialData.getArgs());
		CancelData currentData = (CancelData) confirmations.retrieve(this.getClass(), player);
		long timeElapsed = System.currentTimeMillis() - initialData.getTransactionTime();
		if(timeElapsed > cm.getConfirmationTimeout(this.getClass())){
			mm.confirmationExpired(player);
			confirmations.unregister(this.getClass(), player);
			return;
		}
		if(!currentData.equals(initialData)){
			mm.invalidConfirmData(player);
			confirmations.unregister(this.getClass(), player);
			return;
		}
		finalizeCancel(player, initialData);
	}
    
    private void toggleConfirmations(Player player, String label, String[] args){
		if(args.length < 3){
			mm.sendMessage(player, "You may turn cancel confirmations on or off using /" + label + " confirm toggle <on/off>");
			return;
		}
		String value = args[2];
		if(value.equalsIgnoreCase("on")){
			mm.sendSuccess(player, "Cancel confirmations turned on. To undo this /" + label + " confirm toggle off");
			dbm.updateToggle(player.getUniqueId(), this.getClass(), true);
			return;
		}
			
		if(value.equalsIgnoreCase("off")){
			mm.sendSuccess(player, "Cancel confirmations turned off. To undo this /" + label + " confirm toggle on");
			confirmations.unregister(this.getClass(), player);
			dbm.updateToggle(player.getUniqueId(), this.getClass(), false);
			return;
		}
		mm.sendMessage(player, "You may turn Cancel confirmations on or off using /" + label + " confirm toggle <on/off>");
	}

}

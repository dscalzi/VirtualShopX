/*
 * VirtualShopX
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshopx.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.dscalzi.virtualshopx.VirtualShopX;
import com.dscalzi.virtualshopx.managers.ConfigManager;
import com.dscalzi.virtualshopx.managers.ConfirmationManager;
import com.dscalzi.virtualshopx.managers.DatabaseManager;
import com.dscalzi.virtualshopx.managers.MessageManager;
import com.dscalzi.virtualshopx.objects.Confirmable;
import com.dscalzi.virtualshopx.objects.Offer;
import com.dscalzi.virtualshopx.objects.dataimpl.CancelData;
import com.dscalzi.virtualshopx.util.InventoryManager;
import com.dscalzi.virtualshopx.util.ItemDB;

public class Cancel implements CommandExecutor, Confirmable, TabCompleter{

	@SuppressWarnings("unused")
	private VirtualShopX plugin;
	private final MessageManager mm;
	private final ConfigManager cm;
	private final ConfirmationManager confirmations;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	
	public Cancel(VirtualShopX plugin){
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
		if(!sender.hasPermission("virtualshopx.merchant.regular.cancel")){
            mm.noPermissions(sender);
            return true;
        }
		Player player = (Player)sender;
		if(!cm.getAllowedWorlds().contains(player.getWorld().getName())){
			mm.invalidWorld(sender, command.getName(), player.getWorld());
			return true;
		}
		if(!(cm.getAllowedGamemodes().contains(player.getGameMode().name()))){
        	mm.invalidGamemode(sender, command.getName(), player.getGameMode());
        	return true;
        }
		if(args.length > 0){
			if(args[0].equalsIgnoreCase("confirm")){
				if(args.length > 1){
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
    	boolean enabled = dbm.getToggle(player.getUniqueId(), this.getClass());
		if(!enabled){
			mm.confirmationToggleMsg(player, label, true, this.getClass());
			dbm.updateToggle(player.getUniqueId(), this.getClass(), true);
			return;
		} else {
			mm.confirmationToggleMsg(player, label, false, this.getClass());
			confirmations.unregister(this.getClass(), player);
			dbm.updateToggle(player.getUniqueId(), this.getClass(), false);
			return;
		}
	}
    
    @Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> ret = new ArrayList<String>();
		
		if(args.length == 1)
			if("confirm".startsWith(args[0].toLowerCase()))
				ret.add("confirm");
		
		if(args.length == 2)
			if("toggle".startsWith(args[1].toLowerCase()))
				ret.add("toggle");
		
		return ret.size() > 0 ? ret : null;
	}

}

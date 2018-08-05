/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.command;

import org.bukkit.Bukkit;
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
import com.dscalzi.virtualshopx.objects.Transaction;
import com.dscalzi.virtualshopx.objects.dataimpl.TransactionData;
import com.dscalzi.virtualshopx.util.InputUtil;
import com.dscalzi.virtualshopx.util.InventoryManager;
import com.dscalzi.virtualshopx.util.ItemDB;

import java.util.ArrayList;
import java.util.List;

public class Buy implements CommandExecutor, Confirmable, TabCompleter{

	@SuppressWarnings("unused")
	private VirtualShopX plugin;
	private final MessageManager mm;
	private final ConfigManager cm;
	private final ConfirmationManager confirmations;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	public Buy(VirtualShopX plugin){
		this.plugin = plugin;
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.confirmations = ConfirmationManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.idb = ItemDB.getInstance();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		/* Validating Sender conditions */
		
		if(!(sender instanceof Player)){
			mm.denyConsole(sender);
			return true;
		}
		if(!sender.hasPermission("virtualshopx.merchant.regular.buy")){
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
		
		/* Too few arguments */
		if(args.length < 2){
			mm.sendError(sender, "Proper usage is /" + label + " <amount> <item> [maxprice]");
			return true;
		}
		
		this.execute(player, label, args);
		return true;
	}
	
	private void execute(Player player, String label, String[] args){
		if(!dbm.getToggle(player.getUniqueId(), this.getClass())){
			if(this.validateData(player, args)){
				this.finalizeTransaction(player, (TransactionData) confirmations.retrieve(this.getClass(), player));
				return;
			}
			return;
		}
		if(this.validateData(player, args))
			mm.buyConfirmation(player, label, (TransactionData) confirmations.retrieve(this.getClass(), player));
	}
	
	private boolean validateData(Player player, String[] args){
		//Set data
		int amount = InputUtil.parseInt(args[0]);
		ItemStack item = idb.get(args[1], 0);
		PlayerInventory im = player.getInventory();
		double maxprice;
		//Validate data
		if(amount < 1)		{
			mm.numberFormat(player);
			return false;
		}
		if(amount == Integer.MAX_VALUE && args[0].equalsIgnoreCase("all")){
			mm.numberFormat(player);
			return false;
		}
		if(args[1].matches("^(?iu)(hand|mainhand|offhand)")){
			item = new ItemStack(args[1].equalsIgnoreCase("offhand") ? im.getItemInOffHand() : im.getItemInMainHand());
			if(item.getType() == Material.AIR){
				mm.holdingNothing(player);
				return false;
			}
			args[1] = idb.getItemAlias(item);
		}
		if(item==null){
			mm.wrongItem(player, args[1]);
			return false;
		}
        if(args.length > 2){
        	maxprice = InputUtil.parseDouble(args[2]);
        	if(maxprice < 0){
        		mm.numberFormat(player);
        		return false;
        	}
        } else {
        	maxprice = cm.getMaxPrice(item.getType());
        }
		//Check for listings
		List<Offer> offers = dbm.getItemOffers(item);
        if(offers.size()==0) {
            mm.sendError(player,"There is no " + mm.formatItem(args[1], true)+ cm.getErrorColor() + " for sale.");
            return false;
        }
        
        //Validate request with market.
        TransactionData temp = new TransactionData(amount, item, -1, maxprice, offers, System.currentTimeMillis(), args) ;
        TransactionData data = parseListings(player, temp, false);
        
        if(!data.canContinue()){
        	return false;
        }
        
        //Submit Data
        confirmations.register(this.getClass(), player, data);
		return true;
	}
	
	private void finalizeTransaction(Player player, TransactionData data){
		parseListings(player, data, true);
		confirmations.unregister(this.getClass(), player);
	}
	
	private TransactionData parseListings(Player player, TransactionData data, boolean finalize){
		boolean canContinue = true;
		int amount = data.getAmount();
		ItemStack item = data.getItem();
		double maxprice = data.getMaxPrice();
		String[] args = data.getArgs();
		List<Offer> offers = data.getOffers();
		InventoryManager im = new InventoryManager(player);
		
        int bought = 0;
        double spent = 0;
        boolean tooHigh = false;
        boolean anyLessThanMax = false;;
        
        for(Offer o: offers){
            if(o.getPrice() > maxprice){
            	tooHigh = true;
            	continue;
            }
            if(o.getSeller().equals(player.getName())) 
            	continue;
            
            int canbuy = amount-bought;
            int stock = o.getItem().getAmount();
            if(canbuy > stock)
            	canbuy = stock;  
            double cost = o.getPrice() * canbuy;
            anyLessThanMax = true;
            
            //Revise amounts if not enough money.
            if(!VirtualShopX.hasEnough(player, cost)){
            	canbuy = (int)(VirtualShopX.getEconomy().getBalance(player) / o.getPrice());
                cost = canbuy*o.getPrice();
                amount = bought+canbuy;
                tooHigh = true;
                if(canbuy < 1){
                	mm.ranOutOfMoney(player);
                	canContinue = false;
					break;
                } else {
                	if(!finalize)
                		mm.sendError(player, "You only have enough money for " + mm.formatAmount(bought+canbuy) + " " + mm.formatItem(args[1], true) + cm.getErrorColor() + ".");
                }
            }
            bought += canbuy;
            spent += cost;
            if(finalize){
            	VirtualShopX.getEconomy().withdrawPlayer(player, cost);
            	VirtualShopX.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(o.getSellerUUID()), cost);
            	mm.sendSuccess(o.getSeller(), mm.formatBuyer(player.getName()) + cm.getSuccessColor() + " just bought " + mm.formatAmount(canbuy) + " " + mm.formatItem(args[1], true) + cm.getSuccessColor() + " for " + mm.formatPrice(cost));
            	int left = o.getItem().getAmount() - canbuy;
            	if(left < 1) 
            		dbm.deleteItem(o.getId());
            	else 
            		dbm.updateQuantity(o.getId(), left);
            	Transaction t = new Transaction(o.getSellerUUID(), player.getUniqueId(), o.getItem().getType(), canbuy, cost, System.currentTimeMillis());
            	dbm.logTransaction(t);
            }
            if(bought >= amount) 
            	break;
            if(tooHigh)
            	break;
        }
        if(!tooHigh && !finalize){
        	if(bought == 0){
            	mm.sendError(player,"There is no " + mm.formatItem(args[1], true) + cm.getErrorColor() + " for sale.");
            	canContinue = false;
            }
        	if(bought < amount && bought > 0)
        		mm.sendError(player, "There's only " + mm.formatAmount(bought) + " " + mm.formatItem(args[1], true) + cm.getErrorColor() + " for sale.");
        }
        item.setAmount(bought);
        if(finalize){
        	if(bought > 0) im.addItem(item);
        }
        if(tooHigh && bought == 0 && args.length > 2 && !anyLessThanMax){
        	mm.sendError(player,"No one is selling " + mm.formatItem(args[1], true) + cm.getErrorColor() + " cheaper than " + mm.formatPrice(maxprice));
        	canContinue = false;
        }else{
        	if(finalize)
        		mm.sendSuccess(player,"Managed to buy " + mm.formatAmount(bought) + " " + mm.formatItem(args[1], true) + cm.getSuccessColor() + " for " + mm.formatPrice(spent) + cm.getSuccessColor() + ".");
        }
        return new TransactionData(bought, item, spent, maxprice, offers, System.currentTimeMillis(), args, canContinue);
	}
	
	private void confirm(Player player){
		if(!confirmations.contains(this.getClass(), player)){
			mm.invalidConfirmation(player);
			return;
		}
		TransactionData initialData = (TransactionData) confirmations.retrieve(this.getClass(), player);
		validateData(player, initialData.getArgs());
		TransactionData currentData = (TransactionData) confirmations.retrieve(this.getClass(), player);
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
		finalizeTransaction(player, initialData);
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

package com.dscalzi.virtualshop.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.ChatManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.ConfirmationManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Confirmable;
import com.dscalzi.virtualshop.objects.ListingData;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.Numbers;

public class UpdatePrice implements CommandExecutor, Confirmable{

	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	private final ConfirmationManager confirmations;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	public UpdatePrice(VirtualShop plugin){
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
		if(!sender.hasPermission("virtualshop.merchant.updateprice")){
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
			cm.sendError(sender, "Proper usage is /" + label + " <item> <newprice>");
			return true;
		}
		
		
		this.execute(player, label, args);
		return true;
	}

	private void execute(Player player, String label, String[] args){
		if(!dbm.getUpdateToggle(player.getUniqueId())){
			if(this.validateData(player, args)){
				this.updateListing(player, (ListingData) confirmations.retrieve(this.getClass(), player));
				return;
			}
			return;
		}
		if(this.validateData(player, args))
			cm.updateConfirmation(player, label, (ListingData) confirmations.retrieve(this.getClass(), player));
	}
	
	@SuppressWarnings("deprecation")
	private boolean validateData(Player player, String[] args){
		final int amt = 0;
		ItemStack item = idb.get(args[0], amt);
		double newPrice = Numbers.parseDouble(args[1]);
		PlayerInventory im = player.getInventory();
		if(newPrice < 0){
			cm.numberFormat(player);
			return false;
		}
		if(args[0].equalsIgnoreCase("hand") || args[0].equalsIgnoreCase("mainhand")){
			item = new ItemStack(im.getItemInMainHand());
			if(item.getType() == Material.AIR){
				cm.holdingNothing(player);
				return false;
			}
			item.setAmount(amt);
			args[1] = idb.reverseLookup(item);
		} else if(args[1].equalsIgnoreCase("offhand")){
			item = new ItemStack(im.getItemInOffHand());
			if(item.getType() == Material.AIR){
				cm.holdingNothing(player);
				return false;
			}
			item.setAmount(amt);
			args[1] = idb.reverseLookup(item);
		}
		if(item==null){
			cm.wrongItem(player, args[1]);
			return false;
		}
		if(newPrice > configM.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData())){
			cm.priceTooHigh(player, args[1], configM.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData()));
			return false;
		}
		int currentlyListed = 0;
		double oldPrice = -1;
        for(Offer o: dbm.getSellerOffers(player.getUniqueId(),item)){
        	currentlyListed += o.getItem().getAmount();
        	oldPrice = o.getPrice();
        }
        if(currentlyListed == 0){
        	cm.noSpecificStock(player, args[0]);
        	return false;
        }
        if(newPrice == oldPrice){
        	cm.sendError(player, "Your current price for this item is the same as the requested new price.");
        	return false;
        }
        
        //Submit data
        ListingData data = new ListingData(0, item, newPrice, currentlyListed, oldPrice, System.currentTimeMillis(), args);
        this.confirmations.register(this.getClass(), player, data);
        return true;
	}
	
	private void updateListing(Player player, ListingData data){
		double newPrice = data.getPrice();
        dbm.updatePrice(player.getUniqueId(), newPrice);
		confirmations.unregister(this.getClass(), player);
        if(configM.broadcastOffers())
        {
        	cm.broadcastPriceUpdate(player, data);
			return;
		}
	}
	
	private void confirm(Player player){
		if(!confirmations.contains(this.getClass(), player)){
			cm.invalidConfirmation(player);
			return;
		}
		ListingData initialData = (ListingData) confirmations.retrieve(this.getClass(), player);
		validateData(player, initialData.getArgs());
		ListingData currentData = (ListingData) confirmations.retrieve(this.getClass(), player);
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
		updateListing(player, initialData);
	}
	
	/**
	 * Handles a player's request to toggle the requirement for a confirmation when they attempt to update an item's price.
	 * 
	 * @param player - The command sender.
	 * @param label - The alias of the command.
	 * @param args - Initial arguments returned by onCommand.
	 */
	private void toggleConfirmations(Player player, String label, String[] args){
		if(args.length < 3){
			cm.sendMessage(player, "You may turn update price confirmations on or off using /" + label + " confirm toggle <on/off>");
			return;
		}
		String value = args[2];
		if(value.equalsIgnoreCase("on")){
			cm.sendSuccess(player, "Update price confirmations turned on. To undo this /" + label + " confirm toggle off");
			dbm.updateUpdateToggle(player.getUniqueId(), true);
			return;
		}
			
		if(value.equalsIgnoreCase("off")){
			cm.sendSuccess(player, "Update price confirmations turned off. To undo this /" + label + " confirm toggle on");
			confirmations.unregister(this.getClass(), player);
			dbm.updateUpdateToggle(player.getUniqueId(), false);
			return;
		}
		cm.sendMessage(player, "You may turn update price confirmations on or off using /" + label + " confirm toggle <on/off>");
	}
	
}

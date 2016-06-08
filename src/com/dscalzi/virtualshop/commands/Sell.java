package com.dscalzi.virtualshop.commands;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshop.ChatManager;
import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.ListingData;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.util.InventoryManager;
import com.dscalzi.virtualshop.util.ItemDb;
import com.dscalzi.virtualshop.util.Numbers;

/**
 * Sell CommandExecutor to handle user requests to create listings on the Virtual Market.
 */
public class Sell implements CommandExecutor{
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private Map<Player, ListingData> confirmations;
	
	/**
	 * Initialize a new sell command executor.
	 * @param plugin - An instance of the main class of the current plugin.
	 */
	public Sell(VirtualShop plugin){
		this.plugin = plugin;
		this.confirmations = new HashMap<Player, ListingData>();
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
            ChatManager.denyConsole(sender);
            return true;
        }
		if(!sender.hasPermission("virtualshop.sell")){
            ChatManager.noPermissions(sender);
            return true;
        }
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			ChatManager.denyBeta(sender);
			return true;
		}
		Player player = (Player)sender;
		if(!ConfigManager.getAllowedWorlds().contains(player.getWorld().getName())){
			ChatManager.invalidWorld(sender, command.getName(), player.getWorld());
			return true;
		}
		if((player.getGameMode() != GameMode.SURVIVAL) && (player.getGameMode() != GameMode.ADVENTURE)){
        	ChatManager.invalidGamemode(sender, command.getName(), player.getGameMode());
        	return true;
        }
		if(args.length > 0){
			if(args[0].equalsIgnoreCase("confirm")){
				if(args.length > 1){
					if(args.length > 2 && args[1].equalsIgnoreCase("toggle") ){
						toggleConfirmations(player, args);
						return true;
					}
					if(args[1].equalsIgnoreCase("toggle")){
						toggleConfirmations(player, args);
						return true;
					}
				}
				this.confirm(player);
				return true;
			}
		}
		if(args.length < 3){
			ChatManager.sendError(sender, "Proper usage is /sell <amount> <item> <price>");
			return true;
		}
		if(this.confirmations.containsKey(player))
			this.confirmations.remove(player);
		
		this.execute(player, args);
		return true;
	}
	
	/**
	 * Execution controller for the /sell command. Checks to see whether or not the user has opted out of the confirmation
	 * and handles their request accordingly.
	 * 
	 * @param player - The command sender.
	 * @param args - Initial arguments returned by onCommand
	 */
	private void execute(Player player, String[] args){
		if(!DatabaseManager.getSellToggle(player.getName())){
			if(this.validateData(player, args)){
				this.createListing(player, confirmations.get(player));
				return;
			}
			return;
		}
		if(this.validateData(player, args))
			ChatManager.sellConfirmation(player, confirmations.get(player));
	}
	
	/**
	 * Method to validate the data provided by the player via the /sell command.
	 * 
	 * @param player - The command sender.
	 * @param args - Initial arguments returned by onCommand to be verified.
	 * @return True if all of the parameters the player supplies are valid to initiate the creation of a listing.
	 *  	   If any data is not valid, returns false.
	 */
	@SuppressWarnings("deprecation")
	private boolean validateData(Player player, String[] args){
		//Set Data
		int amount = Numbers.parseInteger(args[0]);
		ItemStack item = ItemDb.get(args[1], amount);
		double price = Numbers.parseDouble(args[2]);
		InventoryManager im = new InventoryManager(player);
		//Validate Data
		if(amount < 0 || price < 0){
			ChatManager.numberFormat(player);
			return false;
		}
		if(args[1].equalsIgnoreCase("hand")){
			item = new ItemStack(player.getItemInHand().getType(),amount, player.getItemInHand().getDurability());
			args[1] = ItemDb.reverseLookup(item);
		}
		if(item==null){
			ChatManager.wrongItem(player, args[1]);
			return false;
		}
		if(amount == Numbers.ALL && args[0].equalsIgnoreCase("all")){
        	ItemStack[] inv = player.getInventory().getContents();
        	int total = 0;
        	for(int i=0; i<inv.length; ++i){
        		if(inv[i] == null)
        			continue;
        		else if(ItemDb.reverseLookup(inv[i]).equals(ItemDb.reverseLookup(item)))
        			total += inv[i].getAmount();
        	}
        	amount = total;
        	item.setAmount(total);
        }
		if(price > ConfigManager.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData())){
			ChatManager.priceTooHigh(player, args[1], ConfigManager.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData()));
			return false;
		}
		if(!im.contains(item,true,true)){
			if(item.getAmount() == 0)
        		ChatManager.sendError(player, "You do not have any " + ChatManager.formatItem(args[1]));
			else
				ChatManager.sendError(player, "You do not have " + ChatManager.formatAmount(item.getAmount()) + " " + ChatManager.formatItem(args[1]));
			return false;
		}
		//Database checks
		int currentlyListed = 0;
		double oldPrice = -1;
        for(Offer o: DatabaseManager.getSellerOffers(player.getName(),item)){
        	currentlyListed += o.item.getAmount();
        	oldPrice = o.price;
        }
        
        //Submit data
        ListingData data = new ListingData(amount, item, price, currentlyListed, oldPrice, System.currentTimeMillis(), args);
        this.confirmations.put(player, data);
        return true;
	}
	
	/**
	 * Method to create a Virtual Shop listing. Only call this method with validated data as no additional checks will be made.
	 * 
	 * @param player - The command sender
	 * @param data - ListingData object which has all of verified data stored.
	 */
	private void createListing(Player player, ListingData data){
		ItemStack item = data.getItem();
		double price = data.getPrice();
		InventoryManager im = new InventoryManager(player);
		im.remove(item, true, true);
        DatabaseManager.removeSellerOffers(player,item);
        item.setAmount(item.getAmount() + data.getCurrentListings());
        Offer o = new Offer(player.getName(),item,price);
		DatabaseManager.addOffer(o);
		confirmations.remove(player);
        if(ConfigManager.broadcastOffers())
        {
			ChatManager.broadcastOffer(o);
			return;
		}
	}
	
	/**
	 * Handles sell confirmations. Checks whether or not the confirmation is valid and runs accordingly.
	 * 
	 * @param player - The command sender.
	 */
	private void confirm(Player player){
		if(!confirmations.containsKey(player)){
			ChatManager.sendError(player, "Nothing to confirm!");
			return;
		}
		ListingData initialData = confirmations.get(player);
		validateData(player, initialData.getArgs());
		ListingData currentData = confirmations.get(player);
		long timeElapsed = System.currentTimeMillis() - initialData.getTransactionTime();
		if(timeElapsed > 15000){
			ChatManager.sendError(player, "Transaction expired, please try again!");
			confirmations.remove(player);
			return;
		}
		if(!currentData.equals(initialData)){
			ChatManager.sendError(player, "Data changed, please try again!");
			confirmations.remove(player);
			return;
		}
		createListing(player, initialData);
	}
	
	/**
	 * Handles a player's request to toggle the requirement for a confirmation when they attempt to sell an item.
	 * 
	 * @param player - The command sender.
	 * @param args - Initial arguments returned by onCommand.
	 */
	private void toggleConfirmations(Player player, String[] args){
		if(args.length < 3){
			ChatManager.sendMessage(player, "You may turn sell confirmations on or off using /sell confirm toggle <on/off>");
			return;
		}
		String value = args[2];
		if(value.equalsIgnoreCase("on")){
			ChatManager.sendSuccess(player, "Sell confirmations turned on. To undo this /sell confirm toggle off");
			DatabaseManager.updateSellToggle(player.getName(), true);
			return;
		}
			
		if(value.equalsIgnoreCase("off")){
			ChatManager.sendSuccess(player, "Sell confirmations turned off. To undo this /sell confirm toggle on");
			confirmations.remove(player);
			DatabaseManager.updateSellToggle(player.getName(), false);
			return;
		}
		ChatManager.sendMessage(player, "You may turn sell confirmations on or off using /sell confirm toggle <on/off>");
	}
}

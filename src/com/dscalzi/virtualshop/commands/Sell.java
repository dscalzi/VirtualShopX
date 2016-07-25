package com.dscalzi.virtualshop.commands;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.ChatManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.ListingData;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.util.InventoryManager;
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.Numbers;

/**
 * Sell CommandExecutor to handle user requests to create listings on the Virtual Market.
 */
public class Sell implements CommandExecutor{
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	private Map<Player, ListingData> confirmations;
	
	/**
	 * Initialize a new sell command executor.
	 * @param plugin - An instance of the main class of the current plugin.
	 */
	public Sell(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.idb = ItemDB.getInstance();
		this.confirmations = new HashMap<Player, ListingData>();
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
            cm.denyConsole(sender);
            return true;
        }
		if(!sender.hasPermission("virtualshop.sell")){
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
			cm.sendError(sender, "Proper usage is /sell <amount> <item> <price>");
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
		if(!dbm.getSellToggle(player.getUniqueId())){
			if(this.validateData(player, args)){
				this.createListing(player, confirmations.get(player));
				return;
			}
			return;
		}
		if(this.validateData(player, args))
			cm.sellConfirmation(player, confirmations.get(player));
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
		ItemStack item = idb.get(args[1], amount);
		double price = Numbers.parseDouble(args[2]);
		PlayerInventory im = player.getInventory();
		//Validate Data
		if(amount < 0 || price < 0){
			cm.numberFormat(player);
			return false;
		}
		if(args[1].equalsIgnoreCase("hand") || args[1].equalsIgnoreCase("mainhand")){
			item = new ItemStack(im.getItemInMainHand().getType(),amount, im.getItemInMainHand().getDurability());
			args[1] = idb.reverseLookup(item);
		} else if(args[1].equalsIgnoreCase("offhand")){
			item = new ItemStack(im.getItemInOffHand().getType(),amount, im.getItemInOffHand().getDurability());
			args[1] = idb.reverseLookup(item);
		}
		if(item==null){
			cm.wrongItem(player, args[1]);
			return false;
		}
		if(amount == Numbers.ALL && args[0].equalsIgnoreCase("all")){
        	ItemStack[] inv = im.getContents();
        	int total = 0;
        	for(int i=0; i<inv.length-5; ++i){
        		if(inv[i] == null)
        			continue;
        		else if(idb.reverseLookup(inv[i]).equals(idb.reverseLookup(item))){
        			total += inv[i].getAmount();
        		}
        	}
        	if(idb.reverseLookup(im.getItemInOffHand()).equals(idb.reverseLookup(item))){
        		total += im.getItemInOffHand().getAmount();
        	}
        	amount = total;
        	item.setAmount(total);
        }
		if(price > configM.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData())){
			cm.priceTooHigh(player, args[1], configM.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData()));
			return false;
		}
		ItemStack temp = new ItemStack(item);
		temp.setAmount(item.getAmount()-im.getItemInOffHand().getAmount());
		if(!im.contains(temp) && im.getItemInOffHand().getAmount()+item.getAmount() == amount){
			if(item.getAmount() == 0)
        		cm.sendError(player, "You do not have any " + cm.formatItem(args[1]));
			else
				cm.sendError(player, "You do not have " + cm.formatAmount(item.getAmount()) + " " + cm.formatItem(args[1]));
			return false;
		}
		//Database checks
		int currentlyListed = 0;
		double oldPrice = -1;
        for(Offer o: dbm.getSellerOffers(player.getUniqueId(),item)){
        	currentlyListed += o.getItem().getAmount();
        	oldPrice = o.getPrice();
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
		if(player.getInventory().getItemInOffHand().isSimilar(item)) player.getInventory().setItemInOffHand(null);
        dbm.removeSellerOffers(player.getUniqueId(),item);
        item.setAmount(item.getAmount() + data.getCurrentListings());
        Offer o = new Offer(player.getUniqueId(),item,price);
		dbm.addOffer(o);
		confirmations.remove(player);
        if(configM.broadcastOffers())
        {
			cm.broadcastOffer(o);
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
			cm.invalidConfirmation(player);
			return;
		}
		ListingData initialData = confirmations.get(player);
		validateData(player, initialData.getArgs());
		ListingData currentData = confirmations.get(player);
		long timeElapsed = System.currentTimeMillis() - initialData.getTransactionTime();
		if(timeElapsed > 15000){
			cm.confirmationExpired(player);
			confirmations.remove(player);
			return;
		}
		if(!currentData.equals(initialData)){
			cm.invalidConfirmData(player);
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
			cm.sendMessage(player, "You may turn sell confirmations on or off using /sell confirm toggle <on/off>");
			return;
		}
		String value = args[2];
		if(value.equalsIgnoreCase("on")){
			cm.sendSuccess(player, "Sell confirmations turned on. To undo this /sell confirm toggle off");
			dbm.updateSellToggle(player.getUniqueId(), true);
			return;
		}
			
		if(value.equalsIgnoreCase("off")){
			cm.sendSuccess(player, "Sell confirmations turned off. To undo this /sell confirm toggle on");
			confirmations.remove(player);
			dbm.updateSellToggle(player.getUniqueId(), false);
			return;
		}
		cm.sendMessage(player, "You may turn sell confirmations on or off using /sell confirm toggle <on/off>");
	}
}

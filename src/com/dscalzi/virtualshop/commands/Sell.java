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
import com.dscalzi.virtualshop.managers.ChatManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.ConfirmationManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Confirmable;
import com.dscalzi.virtualshop.objects.ListingData;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.InventoryManager;
import com.dscalzi.virtualshop.util.Numbers;

/**
 * Sell CommandExecutor to handle user requests to create listings on the Virtual Market.
 */
public class Sell implements CommandExecutor, Confirmable{
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	private final ConfirmationManager confirmations;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	//private Map<Player, ListingData> confirmations;
	
	/**
	 * Initialize a new sell command executor.
	 * @param plugin - An instance of the main class of the current plugin.
	 */
	public Sell(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
		this.confirmations = ConfirmationManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.idb = ItemDB.getInstance();
		//this.confirmations = new HashMap<Player, ListingData>();
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
            cm.denyConsole(sender);
            return true;
        }
		if(!sender.hasPermission("virtualshop.merchant.sell")){
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
		if(args.length < 3){
			cm.sendError(sender, "Proper usage is /" + label + " <amount> <item> <price>");
			return true;
		}
		confirmations.unregister(this.getClass(), player);
		
		this.execute(player, label, args);
		return true;
	}
	
	/**
	 * Execution controller for the /sell command. Checks to see whether or not the user has opted out of the confirmation
	 * and handles their request accordingly.
	 * 
	 * @param player - The command sender.
	 * @param args - Initial arguments returned by onCommand
	 */
	private void execute(Player player, String label, String[] args){
		if(!dbm.getToggle(player.getUniqueId(), this.getClass())){
			if(this.validateData(player, args)){
				this.createListing(player, (ListingData)confirmations.retrieve(this.getClass(), player));
				return;
			}
			return;
		}
		if(this.validateData(player, args))
			cm.sellConfirmation(player, label, (ListingData)confirmations.retrieve(this.getClass(), player));
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
		InventoryManager invM = new InventoryManager(player);
		//Validate Data
		if(amount < 0 || price < 0){
			cm.numberFormat(player);
			return false;
		}
		if(args[1].equalsIgnoreCase("hand") || args[1].equalsIgnoreCase("mainhand")){
			item = new ItemStack(im.getItemInMainHand());
			if(item.getType() == Material.AIR){
				cm.holdingNothing(player);
				return false;
			}
			item.setAmount(amount);
			args[1] = idb.reverseLookup(item);
		} else if(args[1].equalsIgnoreCase("offhand")){
			item = new ItemStack(im.getItemInOffHand());
			if(item.getType() == Material.AIR){
				cm.holdingNothing(player);
				return false;
			}
			item.setAmount(amount);
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
        		else if(inv[i].isSimilar(item)){
        			total += inv[i].getAmount();
        		}
        	}
        	if(im.getItemInOffHand().isSimilar(item)){
        		total += im.getItemInOffHand().getAmount();
        	}
        	amount = total;
        	item.setAmount(total);
        }
		if(price > configM.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData())){
			cm.priceTooHigh(player, args[1], configM.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData()));
			return false;
		}
		
		if(!(invM.contains(item))){
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
        this.confirmations.register(this.getClass(), player, data);
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
		im.removeItem(item);
        dbm.removeSellerOffers(player.getUniqueId(),item);
        item.setAmount(item.getAmount() + data.getCurrentListings());
        Offer o = new Offer(player.getUniqueId(),item,price);
		dbm.addOffer(o);
		confirmations.unregister(this.getClass(), player);
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
		createListing(player, initialData);
	}
	
	/**
	 * Handles a player's request to toggle the requirement for a confirmation when they attempt to sell an item.
	 * 
	 * @param player - The command sender.
	 * @param label - The alias of the command.
	 * @param args - Initial arguments returned by onCommand.
	 */
	private void toggleConfirmations(Player player, String label, String[] args){
		if(args.length < 3){
			cm.sendMessage(player, "You may turn sell confirmations on or off using /" + label + " confirm toggle <on/off>");
			return;
		}
		String value = args[2];
		if(value.equalsIgnoreCase("on")){
			cm.sendSuccess(player, "Sell confirmations turned on. To undo this /" + label + " confirm toggle off");
			dbm.updateToggle(player.getUniqueId(), this.getClass(), true);
			return;
		}
			
		if(value.equalsIgnoreCase("off")){
			cm.sendSuccess(player, "Sell confirmations turned off. To undo this /" + label + " confirm toggle on");
			confirmations.unregister(this.getClass(), player);
			dbm.updateToggle(player.getUniqueId(), this.getClass(), false);
			return;
		}
		cm.sendMessage(player, "You may turn sell confirmations on or off using /" + label + " confirm toggle <on/off>");
	}
}

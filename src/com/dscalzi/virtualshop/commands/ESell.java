package com.dscalzi.virtualshop.commands;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.MessageManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.ConfirmationManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Confirmable;
import com.dscalzi.virtualshop.objects.EListingData;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.util.InventoryManager;
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.Numbers;

public class ESell implements CommandExecutor, Confirmable{

	private final MessageManager mm;
	private final ConfigManager cm;
	private final ConfirmationManager confirmations;
	private final DatabaseManager dbm;
	private final ItemDB idb;
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	
	public ESell(VirtualShop plugin){
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
		if(!sender.hasPermission("virtualshop.merchant.sell")){
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
		if(args.length < 1){
			mm.sendError(sender, "Proper usage is /" + label + " <price>");
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
				this.createListing(player, (EListingData)confirmations.retrieve(this.getClass(), player));
				return;
			}
			return;
		}
		if(this.validateData(player, args))
			mm.eSellConfirmation(player, label, (EListingData)confirmations.retrieve(this.getClass(), player));
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
		PlayerInventory im = player.getInventory();
		ItemStack item = new ItemStack(im.getItemInMainHand());
		double price = Numbers.parseDouble(args[0]);
		InventoryManager invM = new InventoryManager(player);
		//Validate Data
		
		if(!idb.hasEnchantments(item)){
			mm.notEnchanted(player);
			return false;
		}
		
		if(price < 0){
			mm.numberFormat(player);
			return false;
		}
		
		if(!(invM.contains(item))){
			if(item.getAmount() == 0)
				mm.sendError(player, "You do not have any " + mm.formatItem(args[1]) + mm.getErrorColor() + ".");
			else
				mm.sendError(player, "You do not have " + mm.formatAmount(item.getAmount()) + " " + mm.formatItem(args[1]) + mm.getErrorColor() + ".");
			return false;
		}
        
        if(price > cm.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData())){
			mm.priceTooHigh(player, args[1], cm.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData()));
			return false;
		}
        
        if(item.hasItemMeta()){
        	ItemMeta meta = item.getItemMeta();
        	if(meta.hasDisplayName()) meta.setDisplayName(null);
        	if(meta.hasLore()) meta.setLore(null);
        	item.setItemMeta(meta);
        }
        
        //Submit data
        EListingData data = new EListingData(item, price, price, System.currentTimeMillis(), args);
        this.confirmations.register(this.getClass(), player, data);
        return true;
	}
	
	/**
	 * Method to create a Virtual Shop listing. Only call this method with validated data as no additional checks will be made.
	 * 
	 * @param player - The command sender
	 * @param data - ListingData object which has all of verified data stored.
	 */
	private void createListing(Player player, EListingData data){
		ItemStack item = data.getItem();
		double price = data.getPrice();
		InventoryManager im = new InventoryManager(player);
		im.removeItem(item);
        Offer o = new Offer(player.getUniqueId(), item, price);
        String edata = idb.formatEnchantData(idb.getEnchantments(item));
		dbm.addEOffer(o, edata);
		confirmations.unregister(this.getClass(), player);
        if(cm.broadcastOffers())
        {
			mm.broadcastEnchantedOffer(o);
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
			mm.invalidConfirmation(player);
			return;
		}
		EListingData initialData = (EListingData) confirmations.retrieve(this.getClass(), player);
		validateData(player, initialData.getArgs());
		EListingData currentData = (EListingData) confirmations.retrieve(this.getClass(), player);
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
			mm.sendMessage(player, "You may turn sell confirmations on or off using /" + label + " confirm toggle <on/off>");
			return;
		}
		String value = args[2];
		if(value.equalsIgnoreCase("on")){
			mm.sendSuccess(player, "Sell confirmations turned on. To undo this /" + label + " confirm toggle off");
			dbm.updateToggle(player.getUniqueId(), this.getClass(), true);
			return;
		}
			
		if(value.equalsIgnoreCase("off")){
			mm.sendSuccess(player, "Sell confirmations turned off. To undo this /" + label + " confirm toggle on");
			confirmations.unregister(this.getClass(), player);
			dbm.updateToggle(player.getUniqueId(), this.getClass(), false);
			return;
		}
		mm.sendMessage(player, "You may turn sell confirmations on or off using /" + label + " confirm toggle <on/off>");
	}

}

package org.blockface.virtualshop.commands;

import java.util.HashMap;
import java.util.Map;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.ConfigManager;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.objects.Offer;
import org.blockface.virtualshop.objects.TransactionData;
import org.blockface.virtualshop.util.InventoryManager;
import org.blockface.virtualshop.util.ItemDb;
import org.blockface.virtualshop.util.Numbers;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;

public class Sell implements CommandExecutor{
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private Map<Player, TransactionData> confirmations;
	
	public Sell(VirtualShop plugin){
		this.plugin = plugin;
		this.confirmations = new HashMap<Player, TransactionData>();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
            Chatty.denyConsole(sender);
            return true;
        }
		if(!sender.hasPermission("virtualshop.sell")){
            Chatty.noPermissions(sender);
            return true;
        }
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			Chatty.denyBeta(sender);
			return true;
		}
		Player player = (Player)sender;
		if(!ConfigManager.getAllowedWorlds().contains(player.getWorld().getName())){
			Chatty.invalidWorld(sender, command.getName(), player.getWorld());
			return true;
		}
		if((player.getGameMode() != GameMode.SURVIVAL) && (player.getGameMode() != GameMode.ADVENTURE)){
        	Chatty.invalidGamemode(sender, command.getName(), player.getGameMode());
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
			Chatty.sendError(sender, "Proper usage is /sell <amount> <item> <price>");
			return true;
		}
		if(this.confirmations.containsKey(player))
			this.confirmations.remove(player);
		
		this.execute(player, args);
		return true;
	}
	
	private void execute(Player player, String[] args){
		if(!DatabaseManager.getSellToggle(player.getName())){
			if(this.validateData(player, args)){
				this.createListing(player, confirmations.get(player));
				return;
			}
			return;
		}
		this.validateData(player, args);
		Chatty.sellConfirmation(player, confirmations.get(player));
	}
	
	@SuppressWarnings("deprecation")
	private boolean validateData(Player player, String[] args){
		//Set Data
		int amount = Numbers.parseInteger(args[0]);
		ItemStack item = ItemDb.get(args[1], amount);
		double price = Numbers.parseDouble(args[2]);
		InventoryManager im = new InventoryManager(player);
		//Validate Data
		if(amount < 0 || price < 0){
			Chatty.numberFormat(player);
			return false;
		}
		if(args[1].equalsIgnoreCase("hand")){
			item = new ItemStack(player.getItemInHand().getType(),amount, player.getItemInHand().getDurability());
			args[1] = ItemDb.reverseLookup(item);
		}
		if(item==null){
			Chatty.wrongItem(player, args[1]);
			return false;
		}
		if(price > ConfigManager.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData())){
			Chatty.priceTooHigh(player, args[1], ConfigManager.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData()));
			return false;
		}
		if(!im.contains(item,true,true)){
			if(item.getAmount() == 0)
        		Chatty.sendError(player, "You do not have any " + Chatty.formatItem(args[1]));
			else
				Chatty.sendError(player, "You do not have " + Chatty.formatAmount(item.getAmount()) + " " + Chatty.formatItem(args[1]));
			return false;
		}
		if(amount == Numbers.ALL && args[0].equalsIgnoreCase("all")){
        	ItemStack[] inv = player.getInventory().getContents();
        	int total = 0;
        	for(int i=0; i<inv.length; ++i){
        		if(inv[i] == null)
        			continue;
        		else if(inv[i].getType() == item.getType())
        			total += inv[i].getAmount();
        	}
        	item.setAmount(total);
        }
		//Database checks
		int currentlyListed = 0;
		double oldPrice = -1;
        for(Offer o: DatabaseManager.getSellerOffers(player.getName(),item)){
        	currentlyListed += o.item.getAmount();
        	oldPrice = o.price;
        }
        
        //Submit data
        TransactionData data = new TransactionData(amount, item, price, currentlyListed, oldPrice, System.currentTimeMillis(), args);
        this.confirmations.put(player, data);
        return true;
	}
	
	public void createListing(Player player, TransactionData data){
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
			Chatty.broadcastOffer(o);
			return;
		}
	}
	
	private void confirm(Player player){
		if(!confirmations.containsKey(player)){
			Chatty.sendError(player, ChatColor.RED + "Nothing to confirm!");
			return;
		}
		TransactionData initialData = confirmations.get(player);
		validateData(player, initialData.getArgs());
		TransactionData currentData = confirmations.get(player);
		long timeElapsed = System.currentTimeMillis() - initialData.getTransactionTime();
		if(timeElapsed > 15000){
			Chatty.sendError(player, ChatColor.RED + "Transaction expired, please try again!");
			return;
		}
		if(!currentData.equals(initialData)){
			Chatty.sendError(player, ChatColor.RED + "Data changed, please try again!");
			return;
		}
		createListing(player, initialData);
	}
	
	private void toggleConfirmations(Player player, String[] args){
		if(args.length < 3){
			Chatty.sendMessage(player, "You may turn sell confirmations on or off using /sell confirm toggle <on/off>");
			return;
		}
		String value = args[2];
		if(value.equalsIgnoreCase("on")){
			Chatty.sendSuccess(player, ChatColor.GREEN + "Sell confirmations turned on. To undo this /sell confirm toggle off");
			DatabaseManager.updateSellToggle(player.getName(), true);
			return;
		}
			
		if(value.equalsIgnoreCase("off")){
			Chatty.sendSuccess(player, ChatColor.GREEN + "Sell confirmations turned off. To undo this /sell confirm toggle on");
			DatabaseManager.updateSellToggle(player.getName(), false);
			return;
		}
		Chatty.sendMessage(player, "You may turn sell confirmations on or off using /sell confirm toggle <on/off>");
	}
}

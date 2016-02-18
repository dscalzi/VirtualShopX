package com.dscalzi.virtualshop.commands;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshop.Chatty;
import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.objects.Transaction;
import com.dscalzi.virtualshop.objects.TransactionData;
import com.dscalzi.virtualshop.util.InventoryManager;
import com.dscalzi.virtualshop.util.ItemDb;
import com.dscalzi.virtualshop.util.Numbers;

import net.md_5.bungee.api.ChatColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Buy implements CommandExecutor{

	private VirtualShop plugin;
	private Map<Player, TransactionData> confirmations;
	
	public Buy(VirtualShop plugin){
		this.plugin = plugin;
		this.confirmations = new HashMap<Player, TransactionData>();
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
			Chatty.denyConsole(sender);
			return true;
		}
		if(!sender.hasPermission("virtualshop.buy")){
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
		if(args.length < 2){
			Chatty.sendError(sender, "Proper usage is /buy <amount> <item> [maxprice]");
			return true;
		}
		
		this.execute(player, args);
		return true;
	}
	
	private void execute(Player player, String[] args){
		if(!DatabaseManager.getBuyToggle(player.getName())){
			if(this.validateData(player, args)){
				this.finalizeTransaction(player, confirmations.get(player));
				return;
			}
			return;
		}
		if(this.validateData(player, args))
			Chatty.buyConfirmation(player, confirmations.get(player));
	}
	
	@SuppressWarnings("deprecation")
	private boolean validateData(Player player, String[] args){
		//Set data
		int amount = Numbers.parseInteger(args[0]);
		ItemStack item = ItemDb.get(args[1], 0);
		double maxprice;
		//Validate data
		if(amount < 1)		{
			Chatty.numberFormat(player);
			return false;
		}
		if(amount == Numbers.ALL && args[0].equalsIgnoreCase("all")){
			Chatty.numberFormat(player);
			return false;
		}
		if(item==null){
			Chatty.wrongItem(player, args[1]);
			return false;
		}
        if(args.length > 2){
        	maxprice = Numbers.parseDouble(args[2]);
        	if(maxprice < 0){
        		Chatty.numberFormat(player);
        		return false;
        	}
        } else {
        	maxprice = ConfigManager.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData());
        }
		//Check for listings
		List<Offer> offers = DatabaseManager.getItemOffers(item);
        if(offers.size()==0) {
            Chatty.sendError(player,"There is no " + Chatty.formatItem(args[1])+ " for sale.");
            return false;
        }
        
        //Validate request with market.
        TransactionData temp = new TransactionData(amount, item, -1, maxprice, offers, System.currentTimeMillis(), args) ;
        TransactionData data = parseListings(player, temp, false);
        
        if(!data.canContinue()){
        	return false;
        }
        
        //Submit Data
        confirmations.put(player, data);
		return true;
	}
	
	private void finalizeTransaction(Player player, TransactionData data){
		parseListings(player, data, true);
		confirmations.remove(player);
	}
	
	
	@SuppressWarnings("deprecation")
	private TransactionData parseListings(Player player, TransactionData data, boolean finalize){
		boolean canContinue = true;
		int amount = data.getAmount();
		ItemStack item = data.getItem();
		double maxprice = data.getMaxPrice();
		String[] args = data.getArgs();
		List<Offer> offers = data.getOffers();
		InventoryManager im = new InventoryManager(player);
		
		int openNum = 0;
		if(finalize){
			ItemStack[] inv = player.getInventory().getContents();
        	for(int i=0; i<inv.length; ++i){
        		if(inv[i] == null){
        			openNum += 64;
        			continue;
        		} else if(inv[i].getType() == item.getType()){
        			openNum += (64-inv[i].getAmount());
        		}
        	}
		}
		
        int bought = 0;
        double spent = 0;
        boolean tooHigh = false;
        boolean anyLessThanMax = false;;
        
        for(Offer o: offers){
            if(o.price > maxprice){
            	tooHigh = true;
            	continue;
            }
            if(o.seller.equals(player.getName())) 
            	continue;
            
            int canbuy = amount-bought;
            int stock = o.item.getAmount();
            if(canbuy > stock)
            	canbuy = stock;  
            double cost = o.price * canbuy;
            anyLessThanMax = true;
            
            //Revise amounts if not enough money.
            if(!plugin.hasEnough(player.getName(), cost)){
            	canbuy = (int)(VirtualShop.econ.getBalance(player.getName()) / o.price);
                cost = canbuy*o.price;
                amount = bought+canbuy;
                tooHigh = true;
                if(canbuy < 1){
                	Chatty.sendError(player, ChatColor.RED + "Ran out of money!");
                	canContinue = false;
					break;
                } else {
                	if(!finalize)
                		Chatty.sendError(player, ChatColor.RED + "You only have enough money for " + Chatty.formatAmount(bought+canbuy) + " " + Chatty.formatItem(args[1]) + ChatColor.RED + ".");
                }
            }
            bought += canbuy;
            spent += cost;
            if(finalize){
            	VirtualShop.econ.withdrawPlayer(player.getName(), cost);
            	VirtualShop.econ.depositPlayer(o.seller, cost);
            	Chatty.sendSuccess(o.seller, Chatty.formatSeller(player.getName()) + " just bought " + Chatty.formatAmount(canbuy) + " " + Chatty.formatItem(args[1]) + " for " + Chatty.formatPrice(cost));
            	int left = o.item.getAmount() - canbuy;
            	if(left < 1) 
            		DatabaseManager.deleteItem(o.id);
            	else 
            		DatabaseManager.updateQuantity(o.id, left);
            	Transaction t = new Transaction(o.seller, player.getName(), o.item.getTypeId(), o.item.getDurability(), canbuy, cost);
            	DatabaseManager.logTransaction(t);
            }
            if(bought >= amount) 
            	break;
            if(tooHigh)
            	break;
        }
        if(!tooHigh && !finalize){
        	if(bought == 0){
            	Chatty.sendError(player,"There is no " + Chatty.formatItem(args[1]) + " for sale.");
            	canContinue = false;
            }
        	if(bought < amount && bought > 0)
        		Chatty.sendError(player, ChatColor.RED + "There's only " + Chatty.formatAmount(bought) + " " + Chatty.formatItem(args[1]) + ChatColor.RED + " for sale.");
        }
        item.setAmount(bought);
        if(finalize){
        	if(openNum < bought){
        		item.setAmount(bought-openNum);
        		player.getWorld().dropItem(player.getLocation(), item);
        	}
        	if(bought > 0) im.addItem(item);
        }
        if(tooHigh && bought == 0 && args.length > 2 && !anyLessThanMax){
        	Chatty.sendError(player,"No one is selling " + Chatty.formatItem(args[1]) + " cheaper than " + Chatty.formatPrice(maxprice));
        	canContinue = false;
        }else{
        	if(finalize)
        		Chatty.sendSuccess(player,"Managed to buy " + Chatty.formatAmount(bought) + " " + Chatty.formatItem(args[1]) + " for " + Chatty.formatPrice(spent));
        }
        return new TransactionData(bought, item, spent, maxprice, offers, System.currentTimeMillis(), args, canContinue);
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
			confirmations.remove(player);
			return;
		}
		if(!currentData.equals(initialData)){
			Chatty.sendError(player, ChatColor.RED + "Data changed, please try again!");
			confirmations.remove(player);
			return;
		}
		finalizeTransaction(player, initialData);
	}
	
	private void toggleConfirmations(Player player, String[] args){
		if(args.length < 3){
			Chatty.sendMessage(player, "You may turn buy confirmations on or off using /buy confirm toggle <on/off>");
			return;
		}
		String value = args[2];
		if(value.equalsIgnoreCase("on")){
			Chatty.sendSuccess(player, ChatColor.GREEN + "Buy confirmations turned on. To undo this /buy confirm toggle off");
			DatabaseManager.updateBuyToggle(player.getName(), true);
			return;
		}
			
		if(value.equalsIgnoreCase("off")){
			Chatty.sendSuccess(player, ChatColor.GREEN + "Buy confirmations turned off. To undo this /buy confirm toggle on");
			confirmations.remove(player);
			DatabaseManager.updateBuyToggle(player.getName(), false);
			return;
		}
		Chatty.sendMessage(player, "You may turn buy confirmations on or off using /buy confirm toggle <on/off>");
	}
}

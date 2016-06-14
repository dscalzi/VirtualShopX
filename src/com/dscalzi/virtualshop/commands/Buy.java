package com.dscalzi.virtualshop.commands;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.ChatManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.objects.Transaction;
import com.dscalzi.virtualshop.objects.TransactionData;
import com.dscalzi.virtualshop.util.InventoryManager;
import com.dscalzi.virtualshop.util.ItemDb;
import com.dscalzi.virtualshop.util.Numbers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Buy implements CommandExecutor{

	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	private final DatabaseManager dbm;
	private Map<Player, TransactionData> confirmations;
	
	public Buy(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.confirmations = new HashMap<Player, TransactionData>();
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
			cm.denyConsole(sender);
			return true;
		}
		if(!sender.hasPermission("virtualshop.buy")){
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
		if(args.length < 2){
			cm.sendError(sender, "Proper usage is /buy <amount> <item> [maxprice]");
			return true;
		}
		
		this.execute(player, args);
		return true;
	}
	
	private void execute(Player player, String[] args){
		if(!dbm.getBuyToggle(player.getName())){
			if(this.validateData(player, args)){
				this.finalizeTransaction(player, confirmations.get(player));
				return;
			}
			return;
		}
		if(this.validateData(player, args))
			cm.buyConfirmation(player, confirmations.get(player));
	}
	
	@SuppressWarnings("deprecation")
	private boolean validateData(Player player, String[] args){
		//Set data
		int amount = Numbers.parseInteger(args[0]);
		ItemStack item = ItemDb.get(args[1], 0);
		double maxprice;
		//Validate data
		if(amount < 1)		{
			cm.numberFormat(player);
			return false;
		}
		if(amount == Numbers.ALL && args[0].equalsIgnoreCase("all")){
			cm.numberFormat(player);
			return false;
		}
		if(item==null){
			cm.wrongItem(player, args[1]);
			return false;
		}
        if(args.length > 2){
        	maxprice = Numbers.parseDouble(args[2]);
        	if(maxprice < 0){
        		cm.numberFormat(player);
        		return false;
        	}
        } else {
        	maxprice = configM.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData());
        }
		//Check for listings
		List<Offer> offers = dbm.getItemOffers(item);
        if(offers.size()==0) {
            cm.sendError(player,"There is no " + cm.formatItem(args[1])+ configM.getErrorColor() + " for sale.");
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
            if(!VirtualShop.hasEnough(player.getName(), cost)){
            	canbuy = (int)(VirtualShop.econ.getBalance(player.getName()) / o.getPrice());
                cost = canbuy*o.getPrice();
                amount = bought+canbuy;
                tooHigh = true;
                if(canbuy < 1){
                	cm.sendError(player, "Ran out of money!");
                	canContinue = false;
					break;
                } else {
                	if(!finalize)
                		cm.sendError(player, "You only have enough money for " + cm.formatAmount(bought+canbuy) + " " + cm.formatItem(args[1]) + configM.getErrorColor() + ".");
                }
            }
            bought += canbuy;
            spent += cost;
            if(finalize){
            	VirtualShop.econ.withdrawPlayer(player.getName(), cost);
            	VirtualShop.econ.depositPlayer(o.getSeller(), cost);
            	cm.sendSuccess(o.getSeller(), cm.formatSeller(player.getName()) + configM.getSuccessColor() + " just bought " + cm.formatAmount(canbuy) + " " + cm.formatItem(args[1]) + configM.getSuccessColor() + " for " + cm.formatPrice(cost));
            	int left = o.getItem().getAmount() - canbuy;
            	if(left < 1) 
            		dbm.deleteItem(o.getId());
            	else 
            		dbm.updateQuantity(o.getId(), left);
            	Transaction t = new Transaction(o.getSeller(), player.getName(), o.getItem().getTypeId(), o.getItem().getDurability(), canbuy, cost);
            	dbm.logTransaction(t);
            }
            if(bought >= amount) 
            	break;
            if(tooHigh)
            	break;
        }
        if(!tooHigh && !finalize){
        	if(bought == 0){
            	cm.sendError(player,"There is no " + cm.formatItem(args[1]) + configM.getErrorColor() + " for sale.");
            	canContinue = false;
            }
        	if(bought < amount && bought > 0)
        		cm.sendError(player, "There's only " + cm.formatAmount(bought) + " " + cm.formatItem(args[1]) + configM.getErrorColor() + " for sale.");
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
        	cm.sendError(player,"No one is selling " + cm.formatItem(args[1]) + configM.getErrorColor() + " cheaper than " + cm.formatPrice(maxprice));
        	canContinue = false;
        }else{
        	if(finalize)
        		cm.sendSuccess(player,"Managed to buy " + cm.formatAmount(bought) + " " + cm.formatItem(args[1]) + configM.getSuccessColor() + " for " + cm.formatPrice(spent));
        }
        return new TransactionData(bought, item, spent, maxprice, offers, System.currentTimeMillis(), args, canContinue);
	}
	
	private void confirm(Player player){
		if(!confirmations.containsKey(player)){
			cm.invalidConfirmation(player);
			return;
		}
		TransactionData initialData = confirmations.get(player);
		validateData(player, initialData.getArgs());
		TransactionData currentData = confirmations.get(player);
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
		finalizeTransaction(player, initialData);
	}
	
	private void toggleConfirmations(Player player, String[] args){
		if(args.length < 3){
			cm.sendMessage(player, "You may turn buy confirmations on or off using /buy confirm toggle <on/off>");
			return;
		}
		String value = args[2];
		if(value.equalsIgnoreCase("on")){
			cm.sendSuccess(player, "Buy confirmations turned on. To undo this /buy confirm toggle off");
			dbm.updateBuyToggle(player.getName(), true);
			return;
		}
			
		if(value.equalsIgnoreCase("off")){
			cm.sendSuccess(player, "Buy confirmations turned off. To undo this /buy confirm toggle on");
			confirmations.remove(player);
			dbm.updateBuyToggle(player.getName(), false);
			return;
		}
		cm.sendMessage(player, "You may turn buy confirmations on or off using /buy confirm toggle <on/off>");
	}
}

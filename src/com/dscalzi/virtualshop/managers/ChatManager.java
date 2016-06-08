package com.dscalzi.virtualshop.managers;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.objects.ListingData;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.objects.Transaction;
import com.dscalzi.virtualshop.objects.TransactionData;
import com.dscalzi.virtualshop.util.ItemDb;

public class ChatManager {

	private static boolean initialized;
	private static ChatManager instance;
	
	private VirtualShop plugin;
	private Logger logger;
	private String prefix;
    private String color;
    private String eColor;
    private String sColor;
    private final ConfigManager configM;
	
	private ChatManager(Plugin plugin){
		this.plugin = (VirtualShop)plugin;
		this.logger = Logger.getLogger("minecraft");
		this.configM = ConfigManager.getInstance();
		this.assignVars();
		
		logInfo(plugin.getDescription().getName() + " is loading.");
	}
	
	private void assignVars(){
		this.prefix = configM.getPrefix();
		this.color = configM.getColor();
		this.eColor = configM.getErrorColor();
		this.sColor = configM.getSuccessColor();
	}
	
	public static void initialize(Plugin plugin){
		if(!initialized){
			instance = new ChatManager(plugin);
			initialized = true;
		}
	}
	
	public static boolean reload(){
		if(!initialized) return false;
		getInstance().assignVars();
		return true;
	}
	
	public static ChatManager getInstance(){
		return ChatManager.instance;
	}
	
	/* Message distribution */
	
	public void logInfo(String message){
		logger.info(message);
	}
	
	public void sendMessage(CommandSender sender, String message){
    	sender.sendMessage(this.prefix + " " + message);
    }
	
	public void sendError(CommandSender sender, String message){
        sender.sendMessage(this.prefix + this.eColor + " " + message);
    }
	
	public void sendSuccess(CommandSender sender, String message){
        sender.sendMessage(this.prefix + this.sColor + " " + message);
    }
	
	//For potentially offline players
	public boolean sendSuccess(String p, String message){
    	Player player = this.plugin.getServer().getPlayer(p);
		if(player == null) return false;
        sendSuccess(player, message);
		return true;
    }
	
	public void sendGlobal(String message){
        this.plugin.getServer().broadcastMessage(this.prefix + " " + message);
    }
	
	public void broadcastOffer(Offer o){
        sendGlobal(formatOffer(o));
    }
	
	/* Accessors */
	
	public Logger getLogger(){
        return this.logger;
    }
	
	public String getPrefix(){
        return this.prefix;
    }
	
	public String getColor(){
    	return this.color;
    }
	
	/* Predetermined Messages */
	
	public void denyConsole(CommandSender sender){
		sendError(sender, "You must be in-game to do this.");
	}
	
	public void denyBeta(CommandSender sender){
		sendError(sender, "VIRTUAL MARKET IS CURRENTLY RESTRICTED FOR BETA TESTING!");
	}
	
	public void wrongItem(CommandSender sender, String item){
		sendError(sender, "What is " + item + "?");
	}
	
	public void priceTooHigh(CommandSender sender, String item, long priceLimit){
    	sendError(sender, "Woah, you're selling your " + formatItem(item) + this.eColor + " for a rather high price. To avoid scamming, we've set the limit for that item to $" + priceLimit);
    }
	
	public void numberFormat(CommandSender sender){
		sendError(sender, "That is not a proper number.");
	}
	
	public void invalidGamemode(CommandSender sender, String cmd, GameMode mode){
		sendError(sender, "You cannot " + cmd + " in " + mode.toString().toLowerCase() + " mode!");
	}
	
	public void invalidWorld(CommandSender sender, String cmd, World world){
		sendError(sender, "You cannot " + cmd + " in " + world.getName() + "!");
	}
	
	public void noPermissions(CommandSender sender){
		sendError(sender, "You do not have permission to do this");
	}
	
	public void noTransactions(CommandSender sender, String name){
		sendError(sender, "No transaction data found for " + name);
	}
	
	public void noStock(CommandSender sender, String name){
		sendError(sender, name + " is not selling any items.");
	}
	
	public void sellConfirmation(Player player, ListingData data){
    	if(data.getCurrentListings() < 1)
    		sendMessage(player, "You are about to create a listing for " + formatAmount(data.getAmount()) + " " + formatItem(ItemDb.reverseLookup(data.getItem())) + " for " + formatPrice(data.getPrice()) + " each. Please type" + ChatColor.GREEN + " /sell confirm" + this.color + " within 15 seconds to complete the transaction.");
    	else{
    		String common = "You are about to add " + formatAmount(data.getAmount()) + " " + formatItem(ItemDb.reverseLookup(data.getItem())) + " to your current listing";
    		if(data.getOldPrice() == data.getPrice())
    			sendMessage(player, common + ". Please type" + ChatColor.GREEN + " /sell confirm" + this.color + " within 15 seconds to complete the transaction.");
    		if(data.getOldPrice() > data.getPrice())
    			sendMessage(player, common + " for a lower price of " + formatPrice(data.getPrice()) + " each. Please type" + ChatColor.GREEN + " /sell confirm" + this.color + " within 15 seconds to complete the transaction.");
    		if(data.getOldPrice() < data.getPrice())
    			sendMessage(player, common + " for a higher price of " + formatPrice(data.getPrice()) + " each. Please type" + ChatColor.GREEN + " /sell confirm" + this.color + " within 15 seconds to complete the transaction.");
    	}
    }
    
    public void buyConfirmation(Player player, TransactionData data){
    	sendMessage(player, "You are about to buy " + formatAmount(data.getAmount()) + " " + formatItem(ItemDb.reverseLookup(data.getItem())) + " for a total price of " + formatPrice(data.getPrice()) + ". Please type" + ChatColor.GREEN + " /buy confirm" + this.color + " within 15 seconds to complete the transaction.");
    }
	
	/* Formatting */
	
	public String formatSeller(String seller){
		return ChatColor.RED + seller + this.color;
	}

	public String formatAmount(Integer amount){
		return ChatColor.GOLD + amount.toString() + this.color;
	}

	public String formatItem(String item){
		return ChatColor.BLUE + item.toLowerCase() + this.color;
	}

	public String formatPrice(double price){
		return ChatColor.YELLOW + VirtualShop.econ.format(price) + this.color;
	}

	public String formatBuyer(String buyer){
		return ChatColor.AQUA + buyer.toString() + this.color;
	}
	
	public String formatOffer(Offer o){
        return formatSeller(o.seller) + ": " + formatAmount(o.item.getAmount()) + " " + formatItem(ItemDb.reverseLookup(o.item)) + " for " + formatPrice(o.price) + " each.";
    }
	
	public String formatTransaction(Transaction t){
		return formatSeller(t.seller)+ " --> " + formatBuyer(t.buyer) + ": " + formatAmount(t.item.getAmount())+" " + formatItem(ItemDb.reverseLookup(t.item)) + " for "+ formatPrice(t.cost);
	}
	
	public static String capitalize(String s){
		if(s.length() < 1)
			return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
}

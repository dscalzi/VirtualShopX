package com.dscalzi.virtualshop;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.objects.ListingData;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.objects.Transaction;
import com.dscalzi.virtualshop.objects.TransactionData;
import com.dscalzi.virtualshop.util.ItemDb;

public class ChatManager
{
    private static String prefix;
    private static String color;
    private static String eColor;
    private static String sColor;
    private static VirtualShop plugin;
	private static Logger logger;

    public static void initialize(VirtualShop p){
		logger = Logger.getLogger("minecraft");
        plugin = p;
        prefix = ConfigManager.getPrefix();
        color = ConfigManager.getColor();
        eColor = ConfigManager.getErrorColor();
        sColor = ConfigManager.getSuccessColor();
        logInfo(plugin.getDescription().getName() + " is loading.");
    }

	public static void logInfo(String message){
		logger.info(message);
	}

    public static void sendError(CommandSender sender, String message){
        sender.sendMessage(prefix + eColor + " " + message);
    }

    public static void sendSuccess(CommandSender sender, String message){
        sender.sendMessage(prefix + sColor + " " + message);
    }

    public static void sendMessage(CommandSender sender, String message){
    	sender.sendMessage(prefix + " " + message);
    }
    
    public static Boolean sendSuccess(String sender, String message){
    	Player player = plugin.getServer().getPlayer(sender);
		if(player == null) return false;
        sendSuccess(player,message);
		return true;
    }

    public static void sendGlobal(String message){
        plugin.getServer().broadcastMessage(prefix + " " + message);
    }

    public static Logger getLogger(){
        return logger;
    }

    public static String getPrefix(){
        return prefix;
    }

    public static void wrongItem(CommandSender sender, String item){
		sendError(sender, "What is " + item + "?");
	}

    public static void priceTooHigh(CommandSender sender, String item, long priceLimit){
    	sendError(sender, "Woah, you're selling your " + formatItem(item) + " for a rather high price. To avoid scamming, we've set the limit for that item to $" + priceLimit);
    }
    
	public static void denyConsole(CommandSender sender){
		sendError(sender, "You must be in-game to do this.");
	}

	public static void numberFormat(CommandSender sender){
		sendError(sender, "That is not a proper number.");
	}

	public static void denyBeta(CommandSender sender){
		sendError(sender, "VIRTUAL MARKET IS CURRENTLY RESTRICTED FOR BETA TESTING!");
	}
	
	public static void invalidGamemode(CommandSender sender, String cmd, GameMode mode){
		sendError(sender, "You cannot " + cmd + " in " + mode.toString().toLowerCase() + " mode!");
	}
	
	public static void invalidWorld(CommandSender sender, String cmd, World world){
		sendError(sender, "You cannot " + cmd + " in " + world.getName() + "!");
	}
	
	public static String formatSeller(String seller){
		return ChatColor.RED + seller + color;
	}

	public static String formatAmount(Integer amount){
		return ChatColor.GOLD + amount.toString() + color;
	}

	public static String formatItem(String item){
		return ChatColor.BLUE + item.toLowerCase() + color;
	}

	public static String formatPrice(double price){
		return ChatColor.YELLOW + VirtualShop.econ.format(price) + color;
	}

	public static String formatBuyer(String buyer){
		return ChatColor.AQUA + buyer.toString() + color;
	}

	public static void noPermissions(CommandSender sender){
		sendError(sender, "You do not have permission to do this");
	}

	public static void noTransactions(CommandSender sender, String name){
		sendError(sender, "No transaction data found for " + name);
	}
	
	public static void noStock(CommandSender sender, String name){
		sendError(sender, name + " is not selling any items.");
	}
	
    public static void broadcastOffer(Offer o){
         sendGlobal(formatOffer(o));
    }

    public static String formatOffer(Offer o){
        return formatSeller(o.seller) + ": " + formatAmount(o.item.getAmount()) + " " + formatItem(ItemDb.reverseLookup(o.item)) + " for " + formatPrice(o.price) + " each.";
    }
    
    public static void sellConfirmation(Player player, ListingData data){
    	if(data.getCurrentListings() < 1)
    		sendMessage(player, "You are about to create a listing for " + formatAmount(data.getAmount()) + " " + formatItem(ItemDb.reverseLookup(data.getItem())) + " for " + formatPrice(data.getPrice()) + " each. Please type" + ChatColor.GREEN + " /sell confirm" + ChatManager.color + " within 15 seconds to complete the transaction.");
    	else{
    		String common = "You are about to add " + formatAmount(data.getAmount()) + " " + formatItem(ItemDb.reverseLookup(data.getItem())) + " to your current listing";
    		if(data.getOldPrice() == data.getPrice())
    			sendMessage(player, common + ". Please type" + ChatColor.GREEN + " /sell confirm" + ChatManager.color + " within 15 seconds to complete the transaction.");
    		if(data.getOldPrice() > data.getPrice())
    			sendMessage(player, common + " for a lower price of " + formatPrice(data.getPrice()) + " each. Please type" + ChatColor.GREEN + " /sell confirm" + ChatManager.color + " within 15 seconds to complete the transaction.");
    		if(data.getOldPrice() < data.getPrice())
    			sendMessage(player, common + " for a higher price of " + formatPrice(data.getPrice()) + " each. Please type" + ChatColor.GREEN + " /sell confirm" + ChatManager.color + " within 15 seconds to complete the transaction.");
    	}
    }
    
    public static void buyConfirmation(Player player, TransactionData data){
    	sendMessage(player, "You are about to buy " + formatAmount(data.getAmount()) + " " + formatItem(ItemDb.reverseLookup(data.getItem())) + " for a total price of " + formatPrice(data.getPrice()) + ". Please type" + ChatColor.GREEN + " /buy confirm" + ChatManager.color + " within 15 seconds to complete the transaction.");
    }

    public static String formatTransaction(Transaction t){
		return formatSeller(t.seller)+ " --> " + formatBuyer(t.buyer) + ": " + formatAmount(t.item.getAmount())+" " + formatItem(ItemDb.reverseLookup(t.item)) + " for "+ formatPrice(t.cost);

	}
    
    public static String capitalize(String s){
		if(s.length() < 1)
			return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
    
    public static String getColor(){
    	return ChatManager.color;
    }

}

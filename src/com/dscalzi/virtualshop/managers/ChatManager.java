package com.dscalzi.virtualshop.managers;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.commands.Find;
import com.dscalzi.virtualshop.commands.Sales;
import com.dscalzi.virtualshop.commands.Stock;
import com.dscalzi.virtualshop.commands.VS;
import com.dscalzi.virtualshop.objects.ListingData;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.objects.Transaction;
import com.dscalzi.virtualshop.objects.TransactionData;
import com.dscalzi.virtualshop.util.ItemDb;

public final class ChatManager {

	private static boolean initialized;
	private static ChatManager instance;
	
	private VirtualShop plugin;
	private String prefix;
    private String color;
    private String eColor;
    private String sColor;
    private final ConfigManager configM;
	
	private ChatManager(Plugin plugin){
		this.plugin = (VirtualShop)plugin;
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
		plugin.getLogger().info(message);
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
        return plugin.getLogger();
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
		sendError(sender, "Virtual Shop is currently restricted for beta testing. If you think this is a mistake contact the server administrators.");
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
	
	public void invalidConfirmation(CommandSender sender){
		sendError(sender, "Nothing to confirm!");
	}
	
	public void invalidConfirmData(CommandSender sender){
		sendError(sender, "Transaction data changed, please try again!");
	}
	
	public void confirmationExpired(CommandSender sender){
		sendError(sender, "Transaction expired, please try again!");
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
		return ChatColor.GOLD + configM.getLocalization().formatAmt(amount) + this.color;
	}

	public String formatItem(String item){
		return ChatColor.BLUE + item.toLowerCase() + this.color;
	}

	public String formatPrice(double price){
		return ChatColor.YELLOW + configM.getLocalization().formatPrice(price) + this.color;
	}

	public String formatBuyer(String buyer){
		return ChatColor.AQUA + buyer.toString() + this.color;
	}
	
	public String formatOffer(Offer o){
        return formatSeller(o.getSeller()) + ": " + formatAmount(o.getItem().getAmount()) + " " + formatItem(ItemDb.reverseLookup(o.getItem())) + " for " + formatPrice(o.getPrice()) + " each.";
    }
	
	public String formatTransaction(Transaction t){
		return formatSeller(t.getSeller())+ " --> " + formatBuyer(t.getBuyer()) + ": " + formatAmount(t.getItem().getAmount())+" " + formatItem(ItemDb.reverseLookup(t.getItem())) + " for "+ formatPrice(t.getCost());
	}
	
	public String formatHeaderLength(String header, Class<? extends CommandExecutor> clazz){
		int length = header.length();
		if(clazz == VS.class) length = ChatColor.stripColor(header).length()+18;
		if(clazz == Find.class)	length = ChatColor.stripColor(header).length()+17;
		if(clazz == Stock.class) length = ChatColor.stripColor(header).length()+17;
		if(clazz == Sales.class) length = ChatColor.stripColor(header).length()+18;
		int textLength = configM.getPackSpacing()-length;
		String left = ConfigManager.getInstance().getBaseColor() + "";
        String right = ConfigManager.getInstance().getTrimColor() + "";
        int halfLength = textLength/2;
        for(int i=0; i<halfLength; ++i) left += "-";
        for(int i=0; i<halfLength; ++i) right += "-";
        if(header.length() % 2 == 0) right = right.substring(0, right.length()-1);
        if(header.length() % 2 == 1 && clazz == Find.class) right = right.substring(0, right.length()-1);
        return left + header + right;
    }
	
	public static String capitalize(String s){
		if(s.length() < 1)
			return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
}

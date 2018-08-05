/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.managers;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshopx.VirtualShopX;
import com.dscalzi.virtualshopx.command.Buy;
import com.dscalzi.virtualshopx.command.Cancel;
import com.dscalzi.virtualshopx.command.Find;
import com.dscalzi.virtualshopx.command.Reprice;
import com.dscalzi.virtualshopx.command.Sales;
import com.dscalzi.virtualshopx.command.Sell;
import com.dscalzi.virtualshopx.command.Stock;
import com.dscalzi.virtualshopx.command.VS;
import com.dscalzi.virtualshopx.command.enchanted.EBuy;
import com.dscalzi.virtualshopx.command.enchanted.ECancel;
import com.dscalzi.virtualshopx.command.enchanted.ESell;
import com.dscalzi.virtualshopx.objects.Confirmable;
import com.dscalzi.virtualshopx.objects.Offer;
import com.dscalzi.virtualshopx.objects.Transaction;
import com.dscalzi.virtualshopx.objects.dataimpl.CancelData;
import com.dscalzi.virtualshopx.objects.dataimpl.ECancelData;
import com.dscalzi.virtualshopx.objects.dataimpl.EListingData;
import com.dscalzi.virtualshopx.objects.dataimpl.ETransactionData;
import com.dscalzi.virtualshopx.objects.dataimpl.ListingData;
import com.dscalzi.virtualshopx.objects.dataimpl.TransactionData;
import com.dscalzi.virtualshopx.util.ItemDB;
import com.dscalzi.virtualshopx.util.PageList;
import com.dscalzi.virtualshopx.util.ReflectionUtil;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public final class MessageManager {

	private static final char b = (char)8226;
	private static final char face = (char)12484;
	private static boolean initialized;
	private static MessageManager instance;
	
    private final ConfigManager cm;
    private final ItemDB idb;
	
	private VirtualShopX plugin;
	private String prefix;
    private ChatColor color;
    private ChatColor eColor;
    private ChatColor sColor;
    private ChatColor bColor;
    private ChatColor tColor;
    private ChatColor dColor;
    private ChatColor aColor;
    private ChatColor iColor;
    private ChatColor pColor;
    private ChatColor buColor;
    private ChatColor seColor;
	private SimpleDateFormat f;
	
	private MessageManager(VirtualShopX plugin){
		this.plugin = plugin;
		this.cm = ConfigManager.getInstance();
		this.idb = ItemDB.getInstance();
		this.assignVars();
		
		logInfo(plugin.getDescription().getName() + " is loading.");
	}
	
	private void assignVars(){
		this.prefix = cm.getPrefix();
		this.color = cm.getMessageColor();
		this.eColor = cm.getErrorColor();
		this.sColor = cm.getSuccessColor();
		this.bColor = cm.getBaseColor();
		this.tColor = cm.getTrimColor();
		this.dColor = cm.getDescriptionColor();
		this.aColor = cm.getAmountColor();
		this.iColor = cm.getItemColor();
		this.pColor = cm.getPriceColor();
		this.buColor = cm.getBuyerColor();
		this.seColor = cm.getSellerColor();
		f = new SimpleDateFormat("EEE, d MMMM yyyy \n"+getBaseColor()+"'  at' hh:mm:ssa zzz");
	}
	
	public static void initialize(VirtualShopX plugin){
		if(!initialized){
			instance = new MessageManager(plugin);
			initialized = true;
		}
	}
	
	public static boolean reload(){
		if(!initialized) return false;
		getInstance().assignVars();
		return true;
	}
	
	public static MessageManager getInstance(){
		return MessageManager.instance;
	}
	
	/* ---------- Message distribution ---------- */
	
	//Logging
	
	public void logInfo(String message){
		plugin.getLogger().info(message);
	}
	
	public void logError(String message, boolean severe){
		if (severe)
			plugin.getLogger().severe(message);
		else
			plugin.getLogger().warning(message);
	}
	
	//String bukkit messages
	
	public void sendRawMessage(CommandSender sender, String message){
		sender.sendMessage(message);
	}
	
	public void sendMessage(CommandSender sender, String message){
    	sendRawMessage(sender, this.prefix + " " + message);
    }
	
	public void sendError(CommandSender sender, String message){
		sendRawMessage(sender, this.prefix + this.eColor + " " + message);
    }
	
	public void sendSuccess(CommandSender sender, String message){
		sendRawMessage(sender, this.prefix + this.sColor + " " + message);
    }
	
	//Formatted spigot messages
	
	public void sendFormattedMessage(Player p, ArrayList<BaseComponent> components){
		sendFormattedMessage(p, components.toArray(new BaseComponent[components.size()]));
	}
	
	public void sendFormattedMessage(Player p, BaseComponent... components){
		BaseComponent bc = new TextComponent(this.prefix + " ");
		BaseComponent[] fullComponents = new BaseComponent[components.length+1];
		System.arraycopy(components, 0, fullComponents, 1, components.length);
		fullComponents[0] = bc;
		p.spigot().sendMessage(fullComponents);
	}
	
	public void sendRawFormattedMessage(Player p, ArrayList<BaseComponent> components){
		sendRawFormattedMessage(p, components.toArray(new BaseComponent[components.size()]));
	}
	
	public void sendRawFormattedMessage(Player p, BaseComponent... components){
		p.spigot().sendMessage(components);
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
	
	public void sendFormattedGlobal(BaseComponent... components){
		for(Player p : this.plugin.getServer().getOnlinePlayers()){
			sendFormattedMessage(p, components);
		}
	}
	
	public void sendFormattedGlobal(ArrayList<BaseComponent> components){
		for(Player p : this.plugin.getServer().getOnlinePlayers()){
			sendFormattedMessage(p, components);
		}
	}
	
	public void broadcastOffer(Offer o){
        sendGlobal(formatOffer(o));
    }
	
	public void broadcastEnchantedOffer(Offer o){
		sendFormattedGlobal(formatEOffer(o));
	}
	
	public void broadcastPriceUpdate(Player player, ListingData data){
		ItemStack item = data.getItem();
		double oldPrice = data.getOldPrice(), newPrice = data.getPrice(), difference = newPrice-oldPrice;
		item.setAmount(data.getCurrentListings());
		Offer o = new Offer(player.getUniqueId(), item, newPrice);
		sendGlobal(formatOffer(o) + ((difference < 0) ? getSuccessColor() + " (▼" + cm.getLocalization().formatPrice(Math.abs(difference)) +  ")" : getErrorColor() + " (▲" + cm.getLocalization().formatPrice(difference) +  ")"));
	}
	
	public void broadcastEnchantedPriceUpdate(Player player, EListingData data){
		ItemStack item = data.getItem();
		double oldPrice = data.getOldPrice(), newPrice = data.getPrice(), difference = newPrice-oldPrice;
		Offer o = new Offer(player.getUniqueId(), item, newPrice);
		ArrayList<BaseComponent> eO = formatEOffer(o);
		ComponentBuilder b = (difference < 0) ? new ComponentBuilder(" (▼" + cm.getLocalization().formatPrice(Math.abs(difference)) +  ")").color(getSuccessColor().asBungee()) : new ComponentBuilder(" (▲" + cm.getLocalization().formatPrice(difference) +  ")").color(getErrorColor().asBungee());
		eO.addAll(Arrays.asList(b.create()));
		sendFormattedGlobal(eO);
	}
	
	public String getString(String message, Object... args){
		if(message == null) return null;
		if(args.length > 0){
			if(message.contains("'")) message = message.replace("'", "''");
			message = MessageFormat.format(message, args);
		}
		return message;
	}
	
	/* Accessors */
	
	public Logger getLogger(){
        return plugin.getLogger();
    }
	
	public String getPrefix(){
        return this.prefix;
    }
	
	public ChatColor getColor(){
    	return this.color;
    }
	
	public ChatColor getErrorColor(){
		return this.eColor;
	}
	
	public ChatColor getSuccessColor(){
		return this.sColor;
	}
	
	public ChatColor getBaseColor(){
		return this.bColor;
	}
	
	public ChatColor getTrimColor(){
		return this.tColor;
	}
	
	public ChatColor getDescriptionColor(){
		return this.dColor;
	}
	
	public ChatColor getAmountColor(){
		return this.aColor;
	}
	
	public ChatColor getItemColor(){
		return this.iColor;
	}
	
	public ChatColor getPriceColor(){
		return this.pColor;
	}
	
	public ChatColor getBuyerColor(){
		return this.buColor;
	}
	
	public ChatColor getSellerColor(){
		return this.seColor;
	}
	
	/* Predetermined Messages */
	
	public void denyConsole(CommandSender sender){
		sendError(sender, "You must be in-game to do this.");
	}
	
	public void wrongItem(CommandSender sender, String item){
		sendError(sender, getString("What is {0}?", item));
	}
	
	public void notEnchanted(CommandSender sender){
		sendError(sender, "That item is not enchanted, please use /sell instead.");
	}
	
	public void isEnchanted(CommandSender sender){
		sendError(sender, "That item is enchanted, please use /esell instead.");
	}
	
	public void holdingNothing(CommandSender sender){
		sendError(sender, "You are not holding anything in that hand.");
	}
	
	public void priceTooHigh(CommandSender sender, String item, double priceLimit){
    	sendError(sender, getString("Woah, you're selling your {0} for a rather high price. To avoid scamming, we've set the limit for that item to {1}.", formatItem(item)+getErrorColor(), formatPrice(priceLimit)+getErrorColor()));
    }
	
	public void ranOutOfMoney(CommandSender sender){
		sendError(sender, "Ran out of money!");
	}
	
	public void mustSpecifyItem(CommandSender sender){
		sendError(sender, "You must specify an item!");
	}
	
	public void cantBuyOwnItem(CommandSender sender){
		sendError(sender, "You cannot buy your own item!");
	}
	
	public void specifyDefinitePrice(CommandSender sender, String item, boolean enchanted){
		sendError(sender, "No one is currently selling any " + formatItem(item, true, enchanted) + getErrorColor() + ", please specify a definite price.");
	}
	
	public void alreadyCheapest(CommandSender sender, String item, boolean enchanted){
		sendError(sender, "You already have the lowest price for " + formatItem(item, true, enchanted) + getErrorColor() + ", please specify a definite price.");
	}
	
	public void priceTooLow(CommandSender sender){
		sendError(sender, "Your price must be greater than zero.");
	}
	
	public void lookupFailedNull(CommandSender sender){
		sendError(sender, "Lookup failed. You are not holding an item nor did you specify one.");
	}
	
	public void lookupFailedNotFound(CommandSender sender, String item){
		sendError(sender, getString("Lookup failed. Could not find {0} in the database.", item));
	}
	
	public void lookupFailedUnknown(CommandSender sender){
		sendError(sender, "Lookup failed, internal error has occurred.");
	}
	
	public void lookupUnsuported(CommandSender sender){
		sendError(sender, "The VS does not yet support this item. Try again soon!");
	}
	
	public void formatLookupResults(CommandSender sender, ItemStack target, List<String> aliases){
		String formattedAliasList;
		if(aliases.size() < 1){
			formattedAliasList = cm.getErrorColor() + "Could not find this item in the database. It's either damaged, not included in the VS, or from a recent update!";
		} else {
			formattedAliasList = tColor + "Viable Names: " + aliases.toString();
			formattedAliasList = formattedAliasList.replaceAll("\\[", tColor + "\\[" + bColor);
			formattedAliasList = formattedAliasList.replaceAll("\\]", tColor + "\\]" + bColor);
		}
		
		String topLine = getPrefix() + " " + formatItem(target.getType().toString(), true).toUpperCase() + tColor + " - " + formatItem(target.getType().name(), true);
		sendRawMessage(sender, topLine);
		sendRawMessage(sender, bColor + formattedAliasList);
	}
	
	public void listingsAlreadyFormatted(CommandSender sender){
		sendSuccess(sender, "All listings are already correctly formatted!");
	}
	
	public void listingsFormatted(CommandSender sender, int amt){
		sendSuccess(sender, getString("Successfully formatted {0} listings.", amt));
	}
	
	public void invalidUUIDFormat(CommandSender sender){
		sendError(sender, "Invalid UUID format.");
	}
	
	public void queryError(CommandSender sender){
		sendError(sender, "There was an error while querying the database.");
	}
	
	public void invalidPage(CommandSender sender){
		sendError(sender, "Page does not exist.");
	}
	
	public void numberFormat(CommandSender sender){
		sendError(sender, "That is not a proper number.");
	}
	
	public void invalidGamemode(CommandSender sender, String cmd, GameMode mode){
		sendError(sender, getString("You cannot {0} in {1} mode!", cmd, mode.toString().toLowerCase()));
	}
	
	public void invalidWorld(CommandSender sender, String cmd, World world){
		sendError(sender, getString("You cannot {0} in {1}!", cmd, world.getName()));
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
		sendError(sender, "You do not have permission to do this.");
	}
	
	public void noTransactions(CommandSender sender, String name){
		sendError(sender, getString("No transaction data found for {0}.", name));
	}
	
	public void noStock(CommandSender sender, String name){
		sendError(sender, getString("{0} is not selling any items.", name));
	}
	
	public void noSpecificStock(CommandSender sender, String itemName){
		sendError(sender, getString("You do not have any {0} for sale.", formatItem(itemName) + getErrorColor()));
	}
	
	public void noListings(CommandSender sender, String itemName){
		sendError(sender, getString("No one is selling {0}.", formatItem(itemName) + getErrorColor()));
	}
	
	public void notSellingEnchanted(Player player, ItemStack item){
		ComponentBuilder b = new ComponentBuilder("You are not selling any ").color(getErrorColor().asBungee());
    	b.append(" and must specify a price.", FormatRetention.FORMATTING);
    	
    	ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
    	a.add(1, formatEnchantedItem(idb.getItemAlias(item), ItemDB.getCleanedItem(item)));
    	
    	sendFormattedMessage(player, a);
	}
	
	public void specifyDefinitePriceEnchanted(Player player, ItemStack item){
		ComponentBuilder b = new ComponentBuilder("No one is currently selling any ").color(getErrorColor().asBungee());
    	b.append(", please specify a definite price.", FormatRetention.FORMATTING);
    	
    	ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
    	a.add(1, formatEnchantedItem(idb.getItemAlias(item), ItemDB.getCleanedItem(item)));
    	
    	sendFormattedMessage(player, a);
	}
	
	public void alreadyCheapestEnchanted(Player player, ItemStack item){
		ComponentBuilder b = new ComponentBuilder("You already have the lowest price for ").color(getErrorColor().asBungee());
    	b.append(", please specify a definite price.", FormatRetention.FORMATTING);
    	
    	ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
    	a.add(1, formatEnchantedItem(idb.getItemAlias(item), ItemDB.getCleanedItem(item)));

    	sendFormattedMessage(player, a);
	}
	
	public void sellConfirmation(Player player, String label, ListingData data){
		
    	if(data.getCurrentListings() < 1)
    		sendMessage(player, getString("You are about to create a listing for {0} {1} for {2} each.", formatAmount(data.getAmount()), formatItem(idb.getItemAlias(data.getItem())), formatPrice(data.getPrice())));
    	else{
    		String common = getString("You are about to add {0} {1} to your current listing", formatAmount(data.getAmount()), formatItem(idb.getItemAlias(data.getItem())));
    		if(data.getOldPrice() == data.getPrice())
    			sendMessage(player, common + ".");
    		else if(data.getOldPrice() > data.getPrice())
    			sendMessage(player, common + getString(" for a lower price of {0} each.", formatPrice(data.getPrice())));
    		else
    			sendMessage(player, common + getString(" for a higher price of {0} each.", formatPrice(data.getPrice())));
    	}
    	
    	confirmationMsg(player, label, Sell.class);
    }
    
	public void eSellConfirmation(Player player, String label, EListingData data){
		ComponentBuilder b = new ComponentBuilder("You are about to create a listing for an enchanted ").color(getColor().asBungee());
		b.append(" for ");
		b.append(formatPrice(data.getPrice()), FormatRetention.NONE).color(getPriceColor().asBungee());
		b.append(" each.", FormatRetention.NONE).color(getColor().asBungee());
		
		ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
		a.add(1, formatEnchantedItem(idb.getItemAlias(data.getCleanedItem()), data.getCleanedItem()));
		
    	sendFormattedMessage(player, a);
    	confirmationMsg(player, label, ESell.class);
	}
	
    public void buyConfirmation(Player player, String label, TransactionData data){
    	sendMessage(player, getString("You are about to buy {0} {1} for a total price of {2}.",
    			formatAmount(data.getAmount()), formatItem(idb.getItemAlias(data.getItem())), formatPrice(data.getPrice())));
    	confirmationMsg(player, label, Buy.class);
    }
    
    public void eBuyConfirmation(Player player, String label, ETransactionData data){
    	ComponentBuilder b = new ComponentBuilder("You are about to buy an enchanted ").color(getColor().asBungee());
    	b.append(" for ");
    	b.append(formatPrice(data.getPrice()), FormatRetention.NONE).color(getPriceColor().asBungee());
    	b.append(".", FormatRetention.NONE).color(getColor().asBungee());
    	
    	ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
    	a.add(1, formatEnchantedItem(idb.getItemAlias(data.getCleanedItem()), data.getCleanedItem()));
    	
    	sendFormattedMessage(player, a);
    	confirmationMsg(player, label, EBuy.class);
    }
    
    public void cancelConfirmation(Player player, String label, CancelData data){
    	sendMessage(player, getString("You are about to cancel {0} {1}.", formatAmount(data.getAmount()), formatItem(idb.getItemAlias(data.getItem()))));
    	if(data.getAmount() > data.getInventorySpace())
    			sendError(player, getString("Currently, you have space for {0}. Excess will be dropped around you.", (data.getInventorySpace() == 0 ? "none" : "only " + formatAmount(data.getInventorySpace()) + getErrorColor())));
    	
    	confirmationMsg(player, label, Cancel.class);
    }
    
    public void eCancelConfirmation(Player player, String label, ECancelData data){
    	ComponentBuilder b = new ComponentBuilder("You are about to cancel an enchanted ").color(getColor().asBungee());
    	b.append(".", FormatRetention.NONE).color(getColor().asBungee());
    	
    	ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
    	a.add(1, formatEnchantedItem(idb.getItemAlias(data.getCleanedItem()), data.getCleanedItem()));
    	
    	sendFormattedMessage(player, a);
    	
    	if(1 > data.getInventorySpace())
			sendError(player, getString("Currently, you have space for none. Excess will be dropped around you."));
    	confirmationMsg(player, label, ECancel.class);
    }
    
    public void repriceConfirmation(Player player, String label, ListingData data){
    	String quantity = (data.getOldPrice() > data.getPrice()) ? "lower" : "higher";
    	sendMessage(player, getString("You are about to update the price of your {0} for a {1} price of {2} each.", formatItem(idb.getItemAlias(data.getItem())), quantity, formatPrice(data.getPrice())));
    	
    	confirmationMsg(player, label, Reprice.class);
    }
    
    public void eRepriceConfirmation(Player player, String label, EListingData data){
    	String quantity = (data.getOldPrice() > data.getPrice()) ? "lower" : "higher";
    	ComponentBuilder b = new ComponentBuilder("You are about to update the price of your enchanted ").color(getColor().asBungee());
    	b.append(" for a " + quantity + " price of ", FormatRetention.NONE).color(getColor().asBungee());
    	b.append(formatPrice(data.getPrice()), FormatRetention.NONE).color(getPriceColor().asBungee());
    	b.append(".", FormatRetention.NONE).color(getColor().asBungee());
    	
    	ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
    	a.add(1, formatEnchantedItem(idb.getItemAlias(data.getCleanedItem()), data.getCleanedItem()));
    	
    	sendFormattedMessage(player, a);
    	
    	confirmationMsg(player, label, ECancel.class);
    }
    
    public void confirmationMsg(Player player, String label, Class<? extends Confirmable> origin){
    	
    	String type = origin == Reprice.class || origin == Cancel.class || origin == ECancel.class ? "request" : "transaction";
    	
    	ComponentBuilder b = new ComponentBuilder("Please type ");
    	b.color(getColor().asBungee());
    	b.append(getString("/{0} confirm", label), FormatRetention.NONE);
    	b.color(getSuccessColor().asBungee());
    	b.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + label + " confirm"));
    	b.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to confirm!").color(getSuccessColor().asBungee()).create()));
    	b.append(getString(" within {0} seconds to complete the {1}.", (cm.getConfirmationTimeout(origin)/1000), type), FormatRetention.NONE);
    	b.color(getColor().asBungee());
    	
    	sendFormattedMessage(player, b.create());
    }
	
    public void confirmationToggleMsg(Player player, String label, boolean enabled, Class<? extends Confirmable> origin){
    	String name = origin.getSimpleName();
    	
    	ComponentBuilder b = new ComponentBuilder(getString("{0} confirmations turned {1}. To undo this ", name, enabled ? "on" : "off")).color(getSuccessColor().asBungee());
    	b.append(getString("/{0} confirm toggle", label));
    	b.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + label + " confirm toggle"));
    	b.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to toggle!").color(getSuccessColor().asBungee()).create()));
    	b.append(".", FormatRetention.FORMATTING);
    	
    	sendFormattedMessage(player, b.create());
    }
    
    public void ebuySuccess(Player player, ItemStack item){
    	ComponentBuilder b = new ComponentBuilder("Managed to buy an enchanted ").color(getSuccessColor().asBungee());
    	b.append(".");
    	ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
    	a.add(1, formatEnchantedItem(idb.getItemAlias(item), item));
    	
    	sendFormattedMessage(player, a);
    }
    
    public void ebuySuccessVendor(Player buyer, UUID sellerUUID, ItemStack item, double price){
    	Player p = plugin.getServer().getPlayer(sellerUUID);
    	if(p == null) return;
    	ComponentBuilder b = new ComponentBuilder(buyer.getName()).color(getBuyerColor().asBungee());
    	b.append(" just bought your enchanted ", FormatRetention.NONE).color(getSuccessColor().asBungee());
    	b.append(" for ");
    	b.append(formatPrice(price), FormatRetention.NONE).color(getPriceColor().asBungee());
    	b.append(".", FormatRetention.NONE).color(getSuccessColor().asBungee());
    	
    	ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
    	a.add(2, formatEnchantedItem(idb.getItemAlias(item), item));
    	
    	sendFormattedMessage(p, a);
    }
        
    public void ecancelSuccess(Player player, ItemStack item){
    	ComponentBuilder b = new ComponentBuilder("Removed an enchanted ").color(getSuccessColor().asBungee());
    	b.append(".");
    	ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
    	a.add(1, formatEnchantedItem(idb.getItemAlias(item), item));
    	
    	sendFormattedMessage(player, a);
    }
    
    public void versionMessage(CommandSender sender){
		sendMessage(sender, this.plugin.getName() + " Version " + plugin.getDescription().getVersion() + 
				"\n" + bColor + "| " + sColor + "Metrics" + bColor+ " | " + color + "https://bstats.org/plugin/bukkit/VirtualShopX" + 
				"\n" + bColor + "| " + sColor + "Source" + bColor + " | " + color + "https://github.com/dscalzi/VirtualShopX");
	}
    
    /* Command Lists */
    
    public void shopList(CommandSender sender, int page){
    	final String listPrefix = ChatColor.RED.toString() + b + " ";
    	
    	List<String> cmds = new ArrayList<String>();
        cmds.add(listPrefix + tColor + "/vs" + dColor + " - VirtualShopX's technical commands.");
        cmds.add(listPrefix + tColor + "/buy " + aColor + "<amount> " + iColor + "<item> " + pColor + "[maxprice]" + dColor + " - Buy items.");
        cmds.add(listPrefix + tColor + "/sell " + aColor + "<amount> " + iColor + "<item> " + pColor + "<price>" + dColor + " - Sell items.");
        cmds.add(listPrefix + tColor + "/reprice " + iColor + "<item> " + pColor + "<price>" + dColor + " - Reprice a listing.");
        cmds.add(listPrefix + tColor + "/cancel "  + aColor + "<amount> " + iColor + "<item>" + dColor + " - Remove item from shop.");
        cmds.add(listPrefix + tColor + "/find " + iColor + "<item> " + ChatColor.GRAY + "[page]" + dColor + " - Find offers for the item.");
        cmds.add(listPrefix + tColor + "/stock " + seColor + "[player] " + ChatColor.GRAY + "[page]" + dColor + " - Browse offers.");
        cmds.add(listPrefix + tColor + "/sales " + seColor + "[player] " + ChatColor.GRAY + "[page]" + dColor + " - View transaction log.");
        cmds.add(listPrefix + tColor + "/ebuy " + iColor + "<item> " + dColor + " - Browse and buy enchanted items.");
        cmds.add(listPrefix + tColor + "/esell " + iColor + "<item> " + dColor + " - Sell enchanted items." );
        cmds.add(listPrefix + tColor + "/ecancel " + iColor + "<item> " + dColor + " - Cancel enchanted items.");
        cmds.add(listPrefix + tColor + "/ereprice " + iColor + "<item> " + pColor + "<price>" + dColor + " - Reprice an enchanted listing.");
        cmds.add(listPrefix + tColor + "/<command> " + sColor + "confirm " + ChatColor.DARK_GREEN + "toggle " + dColor + " - Toggle confirmations for <command>.");
        
        PageList<String> commands = new PageList<String>(6, cmds);
        
        String header = formatHeaderLength(" " + getPrefix() + " ", VS.class);
        String commandKey = tColor + "              Command List - <Required> [Optional]";
        String footer = bColor + "-" + tColor + "Oo" + bColor + "__________" + tColor + "_____• " + ChatColor.GRAY + "Page " + page + " of " + commands.size() + tColor + " •_____" + bColor + "__________" + tColor + "oO" + bColor + "-";
        
        List<String> pageContent = null;
        try {
        	pageContent = commands.getPage(page-1);
		} catch (IndexOutOfBoundsException e) {
			invalidPage(sender);
			return;
		}
        
        sendRawMessage(sender, header);
        sendRawMessage(sender, commandKey);
        for(String s : pageContent)
        	sendRawMessage(sender, s);
        sendRawMessage(sender, footer);
    }
    
    public void vsList(CommandSender sender, int page){
    	final String listPrefix = ChatColor.RED.toString() + b + " ";
		
		List<String> cmds = new ArrayList<String>();
		cmds.add(listPrefix + tColor + "/shop " + ChatColor.GRAY + "[page]" + dColor + " - View merchant commands.");
		cmds.add(listPrefix + tColor + "/vs help " + ChatColor.GRAY + "[page]" + dColor + " - VirtualShopX's technical commands.");
		cmds.add(listPrefix + tColor + "/vs lookup " + iColor + "[item]" + dColor + " - Browse item name information.");
		cmds.add(listPrefix + tColor + "/vs version" + dColor + " - View plugin's version.");
		if(sender.hasPermission("virtualshopx.admin.formatmarket"))
			cmds.add(listPrefix + tColor + "/vs formatmarket " + iColor + "[item]" + dColor + " - Reprice all items who's market price exceeds the set limit.");
		if(sender.hasPermission("virtualshopx.admin.reload"))
			cmds.add(listPrefix + tColor + "/vs reload" + dColor + " - Reload the plugin's configuration.");
		if(sender.hasPermission("virtualshopx.developer.fullreload"))
			cmds.add(listPrefix + tColor + "/vs fullreload" + dColor + " - Reload the entire plugin's jar file.");
		
		
		PageList<String> commands = new PageList<String>(6, cmds);
		
		String header = formatHeaderLength(" " + getPrefix() + " ", VS.class);
        String commandKey = tColor + "              Command List - <Required> [Optional]";
        String footer = bColor + "-" + tColor + "Oo" + bColor + "__________" + tColor + "_____• " + ChatColor.GRAY + "Page " + page + " of " + commands.size() + tColor + " •_____" + bColor + "__________" + tColor + "oO" + bColor + "-";
		
        List<String> pageContent = null;
        try {
        	pageContent = commands.getPage(page-1);
		} catch (IndexOutOfBoundsException e) {
			invalidPage(sender);
			return;
		}
        
        sendRawMessage(sender, header);
        sendRawMessage(sender, commandKey);
        for(String s : pageContent)
        	sendRawMessage(sender, s);
        sendRawMessage(sender, footer);
    }
    
	/* Formatting */
	
	public String formatSeller(String seller){
		return getSellerColor() + seller + getColor();
	}

	public String formatAmount(Integer amount){
		return getAmountColor() + cm.getLocalization().formatAmt(amount) + getColor();
	}

	public String formatItem(String item){
		return formatItem(item, true, false);
	}
	
	public String formatItem(String item, boolean forceCase){
		return formatItem(item, forceCase, false);
	}
	
	public String formatItem(String item, boolean forceCase, boolean enchanted){
		return getItemColor() + (enchanted ? "" + ChatColor.ITALIC : "") + (forceCase ? item.toLowerCase() : item) + getColor();
	}
	
	public String formatPrice(double price){
		return getPriceColor() + cm.getLocalization().formatPrice(price) + getColor();
	}

	public String formatBuyer(String buyer){
		return getBuyerColor() + buyer.toString() + getColor();
	}
	
	public String formatOffer(Offer o){
        return formatOffer(o, false);
    }
	
	public String formatOffer(Offer o, boolean enchanted){
        return formatSeller(o.getSeller()) + ": " + formatAmount(o.getItem().getAmount()) + " " + formatItem(idb.getItemAlias(o.getItem()), enchanted) + " for " + formatPrice(o.getPrice()) + " each.";
    }
	
	public BaseComponent[] formatOffer0(Offer o){
		ComponentBuilder b = new ComponentBuilder(o.getSeller()).color(getSellerColor().asBungee());
		b.append(": ", FormatRetention.NONE).color(getColor().asBungee());
		b.append(formatAmount(o.getItem().getAmount()) + " ", FormatRetention.NONE).color(getAmountColor().asBungee());
		String item = idb.getItemAlias(o.getItem());
		b.append(item, FormatRetention.NONE).color(getItemColor().asBungee());
		b.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vs find " + item));
    	b.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(getItemColor() + "Click to view other\n" + getItemColor() + "listings for this item.")));
		b.append(" for ", FormatRetention.NONE).color(getPriceColor().asBungee());
		b.append(formatPrice(o.getPrice()), FormatRetention.NONE).color(getPriceColor().asBungee());
		b.append(" each.", FormatRetention.NONE).color(getColor().asBungee());
		return b.create();
	}
	
	public String formatTransactionConsole(Transaction t){
		return formatTransactionConsole(t, false);
	}
	
	public String formatTransactionConsole(Transaction t, boolean enchanted){
		return formatSeller(t.getSeller())+ " --> " + formatBuyer(t.getBuyer()) + ": " + formatAmount(t.getItem().getAmount())+" " + formatItem(idb.getItemAlias(t.getItem()), true, enchanted) + " for "+ formatPrice(t.getCost()) + ".";
	}
	
	public ArrayList<BaseComponent> formatTransaction(Transaction t){
		ComponentBuilder b = new ComponentBuilder(t.getSeller()).color(getSellerColor().asBungee());
		b.append(" --> ", FormatRetention.NONE).color(getColor().asBungee());
		b.append(t.getBuyer(), FormatRetention.NONE).color(getBuyerColor().asBungee());
		b.append(": ", FormatRetention.NONE).color(getColor().asBungee());
		b.append(t.getItem().getAmount() + " ", FormatRetention.NONE).color(getAmountColor().asBungee());
		b.append(formatItem(idb.getItemAlias(t.getItem()))).color(getItemColor().asBungee());
		b.append(" for ", FormatRetention.NONE).color(getColor().asBungee());
		b.append(formatPrice(t.getCost()) + "", FormatRetention.NONE).color(getPriceColor().asBungee());
		b.append(". ", FormatRetention.NONE).color(getColor().asBungee());
		b.append("(?)", FormatRetention.FORMATTING);
		b.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(getBaseColor() + "- " + getTrimColor() + "Transaction Time" + getBaseColor() + " -\n  " + getBaseColor() + formatTemporal(t.getTimestamp()))));
		
		ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
		
    	return a;
	}
	
	public ArrayList<BaseComponent> formatEnchantedTransaction(Transaction t){
		ComponentBuilder b = new ComponentBuilder(t.getSeller()).color(getSellerColor().asBungee());
		b.append(" --> ", FormatRetention.NONE).color(getColor().asBungee());
		b.append(t.getBuyer(), FormatRetention.NONE).color(getBuyerColor().asBungee());
		b.append(": ", FormatRetention.NONE).color(getColor().asBungee());
		b.append(t.getItem().getAmount() + "", FormatRetention.NONE).color(getAmountColor().asBungee());
		b.append(" ");
		//ITEM GOES HERE
		b.append(" for ", FormatRetention.NONE).color(getColor().asBungee());
		b.append(formatPrice(t.getCost()) + "", FormatRetention.NONE).color(getPriceColor().asBungee());
		b.append(". ", FormatRetention.NONE).color(getColor().asBungee());
		b.append("(?)", FormatRetention.FORMATTING);
		b.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(getBaseColor() + "- " + getTrimColor() + "Transaction Time" + getBaseColor() + " -\n  " + getBaseColor() + formatTemporal(t.getTimestamp()))));
		
		ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
    	a.add(6, formatEnchantedItem(idb.getItemAlias(t.getItem()), t.getItem()));
		
    	return a;
	}
	
	public BaseComponent formatEnchantedItem(String item, ItemStack itemStack){
		BaseComponent enchanted = new TextComponent(item.toLowerCase());
		enchanted.setItalic(true);
		enchanted.setColor(ChatColor.BLUE.asBungee());
		enchanted.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(convertItemStackToJson(itemStack)).create()));
		
		return enchanted;
	}

    private String convertItemStackToJson(ItemStack itemStack) {
        // ItemStack methods to get a net.minecraft.server.ItemStack object for serialization
        Class<?> craftItemStackClazz = ReflectionUtil.getOCBClass("inventory.CraftItemStack");
        Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemStackClazz, "asNMSCopy", ItemStack.class);

        // NMS Method to serialize a net.minecraft.server.ItemStack to a valid Json string
        Class<?> nmsItemStackClazz = ReflectionUtil.getNMSClass("ItemStack");
        Class<?> nbtTagCompoundClazz = ReflectionUtil.getNMSClass("NBTTagCompound");
        Method saveNmsItemStackMethod = ReflectionUtil.getMethod(nmsItemStackClazz, "save", nbtTagCompoundClazz);

        Object nmsNbtTagCompoundObj; // This will just be an empty NBTTagCompound instance to invoke the saveNms method
        Object nmsItemStackObj; // This is the net.minecraft.server.ItemStack object received from the asNMSCopy method
        Object itemAsJsonObject; // This is the net.minecraft.server.ItemStack after being put through saveNmsItem method

        try {
            nmsNbtTagCompoundObj = nbtTagCompoundClazz.newInstance();
            nmsItemStackObj = asNMSCopyMethod.invoke(null, itemStack);
            itemAsJsonObject = saveNmsItemStackMethod.invoke(nmsItemStackObj, nmsNbtTagCompoundObj);
        } catch (Throwable t) {
           	logError("Failed to serialize itemstack to nms item", true);
            return null;
        }

        // Return a string representation of the serialized object
        return itemAsJsonObject.toString();
    }
	
	public ArrayList<BaseComponent> formatEOffer(Offer o){
		ComponentBuilder b = new ComponentBuilder(o.getSeller()).color(getSellerColor().asBungee());
		b.append(": an enchanted ", FormatRetention.NONE).color(getColor().asBungee());
		//b.append(o.getItem().getAmount() + " ", FormatRetention.NONE).color(getAmountColor().asBungee());
		b.append(" for ", FormatRetention.NONE).color(getColor().asBungee());
		b.append(formatPrice(o.getPrice()) + "", FormatRetention.NONE).color(getPriceColor().asBungee());
		b.append(".", FormatRetention.NONE).color(getColor().asBungee());
		BaseComponent item = formatEnchantedItem(idb.getItemAlias(o.getItem()), o.getItem());
		ArrayList<BaseComponent> a = new ArrayList<BaseComponent>(Arrays.asList(b.create()));
		a.add(2, item);
		return a;
	}
	
	public String formatHeaderLength(String header, Class<? extends CommandExecutor> clazz){
		int length = header.length();
		if(clazz == VS.class) length = ChatColor.stripColor(header).length()+18;
		if(clazz == Find.class)	length = ChatColor.stripColor(header).length()+17;
		if(clazz == Stock.class) length = ChatColor.stripColor(header).length()+17;
		if(clazz == Sales.class) length = ChatColor.stripColor(header).length()+18;
		if(clazz == EBuy.class) length = ChatColor.stripColor(header).length()+40;
		int textLength = cm.getPackSpacing()-length;
		String left = this.bColor + "";
        String right = this.tColor + "";
        int halfLength = textLength/2;
        for(int i=0; i<halfLength; ++i) left += "-";
        for(int i=0; i<halfLength; ++i) right += "-";
        if(header.length() % 2 == 0 && clazz != EBuy.class) right = right.substring(0, right.length()-1);
        if(header.length() % 2 == 1 && clazz == Find.class) right = right.substring(0, right.length()-1);
        return left + header + right;
    }
	
	public String formatTemporal(long sinceEpoch){
		if(sinceEpoch == 0) return "    ¯\\_("+face+")_/¯";
		return f.format(new Date(sinceEpoch)).replace("AM", "am").replace("PM","pm");
	}
	
	/* Static Utility */
	
	public static String capitalize(String s){
		if(s.length() < 1)
			return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}
	
	public static String hideString(String s){
		String hidden = "";
        for (char c : s.toCharArray()) hidden += ChatColor.COLOR_CHAR + c;
        return hidden;
	}
	
	public static List<String> getStringsWithPrefix(String prefix, List<String> args){
		List<String> r = new ArrayList<String>();
		for(String s : args)
			if(s.toLowerCase().startsWith(prefix.toLowerCase()))
				r.add(s);	
		return r;
	}
}

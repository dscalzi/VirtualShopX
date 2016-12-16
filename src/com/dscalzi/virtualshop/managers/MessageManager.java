package com.dscalzi.virtualshop.managers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.commands.Buy;
import com.dscalzi.virtualshop.commands.Cancel;
import com.dscalzi.virtualshop.commands.EFind;
import com.dscalzi.virtualshop.commands.ESell;
import com.dscalzi.virtualshop.commands.Find;
import com.dscalzi.virtualshop.commands.Sales;
import com.dscalzi.virtualshop.commands.Sell;
import com.dscalzi.virtualshop.commands.Stock;
import com.dscalzi.virtualshop.commands.Reprice;
import com.dscalzi.virtualshop.commands.VS;
import com.dscalzi.virtualshop.objects.CancelData;
import com.dscalzi.virtualshop.objects.EListingData;
import com.dscalzi.virtualshop.objects.ListingData;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.objects.Transaction;
import com.dscalzi.virtualshop.objects.TransactionData;
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.ReflectionUtil;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public final class MessageManager {

	private static boolean initialized;
	private static MessageManager instance;
	
    private final ConfigManager cm;
    private final ItemDB idb;
	
	private VirtualShop plugin;
	private String prefix;
    private ChatColor color;
    private ChatColor eColor;
    private ChatColor sColor;
    private ChatColor bColor;
    private ChatColor tColor;
    private ChatColor dcolor;
	
	private MessageManager(VirtualShop plugin){
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
		this.dcolor = cm.getDescriptionColor();
	}
	
	public static void initialize(VirtualShop plugin){
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
	
	public void sendFormattedMessage(Player p, BaseComponent... components){
		BaseComponent bc = new TextComponent(this.prefix + " ");
		BaseComponent[] fullComponents = new BaseComponent[components.length+1];
		System.arraycopy(components, 0, fullComponents, 1, components.length);
		fullComponents[0] = bc;
		p.spigot().sendMessage(fullComponents);
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
		sendGlobal(formatOffer(o) + ((difference < 0) ? ChatColor.GREEN + " (▼" + cm.getLocalization().formatPrice(Math.abs(difference)) +  ")" : ChatColor.RED + " (▲" + cm.getLocalization().formatPrice(difference) +  ")"));
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
		return this.dcolor;
	}
	
	/* Predetermined Messages */
	
	public void denyConsole(CommandSender sender){
		sendError(sender, "You must be in-game to do this.");
	}
	
	public void wrongItem(CommandSender sender, String item){
		sendError(sender, "What is " + item + "?");
	}
	
	public void notEnchanted(CommandSender sender){
		sendError(sender, "That item is not enchanted, please sell it using /sell instead.");
	}
	
	public void holdingNothing(CommandSender sender){
		sendError(sender, "You are not holding anything in that hand.");
	}
	
	public void priceTooHigh(CommandSender sender, String item, long priceLimit){
    	sendError(sender, "Woah, you're selling your " + formatItem(item) + this.eColor + " for a rather high price. To avoid scamming, we've set the limit for that item to $" + priceLimit);
    }
	
	public void invalidPage(CommandSender sender){
		sendError(sender, "Page does not exist.");
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
		sendError(sender, "You do not have permission to do this.");
	}
	
	public void noTransactions(CommandSender sender, String name){
		sendError(sender, "No transaction data found for " + name);
	}
	
	public void noStock(CommandSender sender, String name){
		sendError(sender, name + " is not selling any items.");
	}
	
	public void noSpecificStock(CommandSender sender, String itemName){
		sendError(sender, "You do not have any " + formatItem(itemName) + this.eColor + " for sale.");
	}
	
	public void noListings(CommandSender sender, String itemName){
		sendError(sender, "No one is selling " + formatItem(itemName) + this.eColor + ".");
	}
	
	public void sellConfirmation(Player player, String label, ListingData data){
		String confirmLine = "Please type" + ChatColor.GREEN + " /" + label + " confirm" + this.color + " within " + (cm.getConfirmationTimeout(Sell.class)/1000) + " seconds to complete the transaction.";
		
    	if(data.getCurrentListings() < 1)
    		sendMessage(player, "You are about to create a listing for " + formatAmount(data.getAmount()) + " " + formatItem(idb.reverseLookup(data.getItem())) + " for " + formatPrice(data.getPrice()) + " each. " + confirmLine);
    	else{
    		String common = "You are about to add " + formatAmount(data.getAmount()) + " " + formatItem(idb.reverseLookup(data.getItem())) + " to your current listing";
    		if(data.getOldPrice() == data.getPrice())
    			sendMessage(player, common + ". " + confirmLine);
    		if(data.getOldPrice() > data.getPrice())
    			sendMessage(player, common + " for a lower price of " + formatPrice(data.getPrice()) + " each. " + confirmLine);
    		if(data.getOldPrice() < data.getPrice())
    			sendMessage(player, common + " for a higher price of " + formatPrice(data.getPrice()) + " each. " + confirmLine);
    	}
    }
    
	public void eSellConfirmation(Player player, String label, EListingData data){
		String confirmLine = this.color + "Please type" + ChatColor.GREEN + " /" + label + " confirm"; 
		String confirmLine2 = this.color + " within " + (cm.getConfirmationTimeout(ESell.class)/1000) + " seconds to complete the transaction.";
		
		TextComponent before = new TextComponent(this.color + "You are about to create a listing for an enchanted ");
		TextComponent after = new TextComponent(this.color + " for " + formatPrice(data.getPrice()) + " each. ");
		TextComponent confirm = new TextComponent(confirmLine);
		TextComponent confirm2 = new TextComponent(confirmLine2);
		confirm2.setColor(this.color.asBungee());
		
    	sendFormattedMessage(player, before, formatEnchantedItem(idb.reverseLookup(data.getItem()), data.getItem()), after);
    	player.spigot().sendMessage(confirm, confirm2);
	}
	
    public void buyConfirmation(Player player, String label, TransactionData data){
    	sendMessage(player, "You are about to buy " + formatAmount(data.getAmount()) + " " + formatItem(idb.reverseLookup(data.getItem())) + " for a total price of " + formatPrice(data.getPrice()) + ". Please type" + ChatColor.GREEN + " /" + label + " confirm" + this.color + " within " + (cm.getConfirmationTimeout(Buy.class)/1000) + " seconds to complete the transaction.");
    }
    
    public void cancelConfirmation(Player player, String label, CancelData data){
    	sendMessage(player, "You are about to cancel " + formatAmount(data.getAmount()) + " " + formatItem(idb.reverseLookup(data.getItem())) + ". " + ((data.getAmount() > data.getInventorySpace()) ? ChatColor.RED + "Currently, you have space for " + (data.getInventorySpace() == 0 ? "none" : "only " + formatAmount(data.getInventorySpace())) + ChatColor.RED + ". Excess will be dropped around you. " + this.color : "") + "Please type" + ChatColor.GREEN + " /" + label + " confirm" + this.color + " within " + (cm.getConfirmationTimeout(Cancel.class)/1000) + " seconds to complete the request.");
    }
    
    public void repriceConfirmation(Player player, String label, ListingData data){
    	String quantity = (data.getOldPrice() > data.getPrice()) ? "lower" : "higher";
    	sendMessage(player, "You are about to update the price of your " + formatItem(idb.reverseLookup(data.getItem())) + " for a " + quantity + " price of " + formatPrice(data.getPrice()) + " each. Please type" + ChatColor.GREEN + " /" + label + " confirm" + this.color + " within " + (cm.getConfirmationTimeout(Reprice.class)/1000) + " seconds to complete the transaction.");
    }
	
	/* Formatting */
	
	public String formatSeller(String seller){
		return ChatColor.RED + seller + this.color;
	}

	public String formatAmount(Integer amount){
		return ChatColor.GOLD + cm.getLocalization().formatAmt(amount) + this.color;
	}

	public String formatItem(String item){
		return ChatColor.BLUE + item.toLowerCase() + this.color;
	}
	
	public BaseComponent formatEnchantedItem(String item, ItemStack itemStack){
		
		BaseComponent enchanted = new TextComponent(ChatColor.BLUE + "" + ChatColor.ITALIC + item.toLowerCase() + this.color);
		
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
	
	public String formatPrice(double price){
		return ChatColor.YELLOW + cm.getLocalization().formatPrice(price) + this.color;
	}

	public String formatBuyer(String buyer){
		return ChatColor.AQUA + buyer.toString() + this.color;
	}
	
	public String formatOffer(Offer o){
        return formatSeller(o.getSeller()) + ": " + formatAmount(o.getItem().getAmount()) + " " + formatItem(idb.reverseLookup(o.getItem())) + " for " + formatPrice(o.getPrice()) + " each.";
    }
	
	public BaseComponent[] formatEOffer(Offer o){
		BaseComponent[] component = new BaseComponent[3];
		component[0] = new TextComponent(formatSeller(o.getSeller()) + ": " + formatAmount(o.getItem().getAmount()) + " ");
		component[1] = formatEnchantedItem(idb.reverseLookup(o.getItem()), o.getItem());
		component[2] = new TextComponent(this.color + " for " + formatPrice(o.getPrice()) + " each.");
		return component;
	}
	
	public String formatTransaction(Transaction t){
		return formatSeller(t.getSeller())+ " --> " + formatBuyer(t.getBuyer()) + ": " + formatAmount(t.getItem().getAmount())+" " + formatItem(idb.reverseLookup(t.getItem())) + " for "+ formatPrice(t.getCost());
	}
	
	public String formatHeaderLength(String header, Class<? extends CommandExecutor> clazz){
		int length = header.length();
		if(clazz == VS.class) length = ChatColor.stripColor(header).length()+18;
		if(clazz == Find.class)	length = ChatColor.stripColor(header).length()+17;
		if(clazz == Stock.class) length = ChatColor.stripColor(header).length()+17;
		if(clazz == Sales.class) length = ChatColor.stripColor(header).length()+18;
		if(clazz == EFind.class) length = ChatColor.stripColor(header).length()+40;
		int textLength = cm.getPackSpacing()-length;
		String left = this.bColor + "";
        String right = this.tColor + "";
        int halfLength = textLength/2;
        for(int i=0; i<halfLength; ++i) left += "-";
        for(int i=0; i<halfLength; ++i) right += "-";
        if(header.length() % 2 == 0 && clazz != EFind.class) right = right.substring(0, right.length()-1);
        if(header.length() % 2 == 1 && clazz == Find.class) right = right.substring(0, right.length()-1);
        return left + header + right;
    }
	
	/* Static Utility */
	
	public static String capitalize(String s){
		if(s.length() < 1)
			return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}
	
	public static List<String> getStringsWithPrefix(String prefix, List<String> args){
		List<String> r = new ArrayList<String>();
		for(String s : args)
			if(s.toLowerCase().startsWith(prefix.toLowerCase()))
				r.add(s);	
		return r;
	}
}

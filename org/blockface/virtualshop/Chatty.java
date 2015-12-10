package org.blockface.virtualshop;

import java.util.logging.Logger;

import org.blockface.virtualshop.objects.Offer;
import org.blockface.virtualshop.objects.Transaction;
import org.blockface.virtualshop.util.ItemDb;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Chatty
{
    private static String prefix;
    private static VirtualShop plugin;
	private static Logger logger;

    public static void initialize(VirtualShop p)
    {
		logger = Logger.getLogger("minecraft");
        plugin = p;
        prefix = "[Shop] " + ChatColor.WHITE;
        logInfo(plugin.getDescription().getName() + " is loading.");
    }

	public static void logInfo(String message)
	{
		logger.info(message);
	}

    public static void sendError(CommandSender sender, String message)
    {
        sender.sendMessage(ChatColor.RED + prefix  + message);
    }

    public static void sendSuccess(CommandSender sender, String message)
    {
        sender.sendMessage(ChatColor.DARK_GREEN + prefix  + message);
    }

    public static Boolean sendSuccess(String sender, String message)
    {   Player player = plugin.getServer().getPlayer(sender);
		if(player == null) return false;
        sendSuccess(player,message);
		return true;
    }

    public static void sendGlobal(String message)
    {
        plugin.getServer().broadcastMessage(ChatColor.DARK_GREEN + prefix  + message);
    }

    public static Logger getLogger() {
        return logger;
    }

    public static String getPrefix() {
        return prefix;
    }

    public static void wrongItem(CommandSender sender, String item)
	{

		sendError(sender, "What is " + item + "?");
	}

	public static void denyConsole(CommandSender sender)
	{
		sendError(sender, "You must be in-game to do this.");
	}

	public static void numberFormat(CommandSender sender)
	{
		sendError(sender, "That is not a proper number.");
	}

	public static String formatSeller(String seller)
	{
		return ChatColor.RED + seller + ChatColor.WHITE;
	}

	public static String formatAmount(Integer amount)
	{
		return ChatColor.GOLD + amount.toString() + ChatColor.WHITE;
	}

	public static String formatItem(String item)
	{
		return ChatColor.BLUE + item.toLowerCase() + ChatColor.WHITE;
	}

	public static String formatPrice(double price)
	{
		return ChatColor.YELLOW + VirtualShop.econ.format(price) + ChatColor.WHITE;
	}

	public static String formatBuyer(String buyer)
	{
		return ChatColor.AQUA + buyer.toString() + ChatColor.WHITE;
	}

	public static void noPermissions(CommandSender sender)
	{
		sendError(sender, "You do not have permission to do this");
	}

    public static void broadcastOffer(Offer o) {
         sendGlobal(formatOffer(o));
    }

    public static String formatOffer(Offer o)
    {
        return formatSeller(o.seller) + ": " + formatAmount(o.item.getAmount()) + " " + formatItem(ItemDb.reverseLookup(o.item)) + " for " + formatPrice(o.price) + " each.";
    }

    public static String formatTransaction(Transaction t)
	{
		return formatSeller(t.seller)+ " --> " + formatBuyer(t.buyer) + ": " + formatAmount(t.item.getAmount())+" " + formatItem(ItemDb.reverseLookup(t.item)) + " for "+ formatPrice(t.cost);

	}

}

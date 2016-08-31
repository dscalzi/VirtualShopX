package com.dscalzi.virtualshop.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.swing.text.BadLocationException;

import org.bukkit.ChatColor;
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
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.PageList;
import com.dscalzi.vsreloader.PluginUtil;

import javafx.util.Pair;

public class VS implements CommandExecutor{

	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	private final DatabaseManager dbm;
	final ItemDB idb;
	
	public VS(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.idb = ItemDB.getInstance();
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			cm.denyBeta(sender);
			return true;
		}
		
		String cmd = command.getName();
		if(cmd.equalsIgnoreCase("shop") || (cmd.equals("vs") && args.length > 0 && args[0].equalsIgnoreCase("shop"))){
			if(args.length > 0){
				try{
    				int page = (args[0].equalsIgnoreCase("shop") ? Integer.parseInt(args[1]) : Integer.parseInt(args[0]));
    				this.cmdList(sender, page);
    				return true;
    			} catch (NumberFormatException e){
    				cm.sendError(sender, "Page does not exist");
					return true;
    			} catch (ArrayIndexOutOfBoundsException e){
    				this.cmdList(sender, 1);
    				return true;
    			}
			}
			this.cmdList(sender, 1);
			return true;
		}
		
		if(cmd.equalsIgnoreCase("vs")){
			if(args.length > 0){
				if(args[0].equalsIgnoreCase("help")){
					if(args.length > 1){
						try{
		    				int page = Integer.parseInt(args[1]);
		    				this.vsList(sender, page);
		    				return true;
		    			} catch (NumberFormatException e){
		    				cm.sendError(sender, "Page does not exist");
							return true;
		    			}
					}
					this.vsList(sender, 1);
					return true;
				}
				if(args[0].matches("^(?iu)(buy|sell|cancel|find|updateprice|stock|sales)")){
					this.redirectCommand(sender, args);
					return true;
				}
				if(args[0].equalsIgnoreCase("lookup")){
					this.cmdLookup(sender, args);
					return true;
				}
				if(args[0].equalsIgnoreCase("formatmarket")){
					if(args.length > 1){
						this.formatMarket(sender, args[1]);
						return true;
					}
					this.formatMarket(sender);
					return true;
				}
				if(args[0].equalsIgnoreCase("uuidnamesync")){
					if(args.length > 1){
						this.cmdUUIDNameSync(sender, Optional.of(args[1]));
						return true;
					}
					this.cmdUUIDNameSync(sender, Optional.empty());
					return true;
				}
				if(args[0].equalsIgnoreCase("update2uuid")){
					this.cmdupdateuuid(sender);
					return true;
				}
				if(args[0].equalsIgnoreCase("reload")){
					this.cmdReload(sender);
					return true;
				}
				if(args[0].equalsIgnoreCase("reloadconfig")){
					this.cmdReloadConfig(sender);
					return true;
				}
				if(args[0].equalsIgnoreCase("version")){
					this.cmdVersion(sender);
					return true;
				}
			}
			this.vsList(sender, 1);
			return true;
		}
		
		return true;
	}
	
	public void cmdupdateuuid(CommandSender sender){
		
		if(!sender.hasPermission("virtualshop.access.admin")){
			cm.noPermissions(sender);
            return;
		}
		
		try{
			@SuppressWarnings("deprecation")
			Pair<Integer, Integer> sfs = dbm.updateDatabase();
			cm.sendSuccess(sender, "Database updated. " + sfs.getKey() + " successful, " + sfs.getValue() + " failed.");
		} catch(Exception ex){
			ex.printStackTrace();
			cm.sendError(sender, "FAILED");
		}
	}
	
	public void cmdList(CommandSender sender, int page){
    	final String listPrefix = ChatColor.RED + " � ";
    	final String baseColor = configM.getBaseColor();
    	final String trimColor = configM.getTrimColor();
    	final String descColor = configM.getDescriptionColor();
    	
    	List<String> cmds = new ArrayList<String>();
        cmds.add(listPrefix + trimColor + "/buy " + ChatColor.GOLD + "<amount> " + ChatColor.BLUE + "<item> " + ChatColor.YELLOW + "[maxprice]" + descColor + " - Buy items.");
        cmds.add(listPrefix + trimColor + "/sell " + ChatColor.GOLD + "<amount> " + ChatColor.BLUE + "<item> " + ChatColor.YELLOW + "<price>" + descColor + " - Sell items.");
        cmds.add(listPrefix + trimColor + "/updateprice " + ChatColor.BLUE + "<item> " + ChatColor.YELLOW + "<price>" + descColor + " - Update the price of a listing.");
        cmds.add(listPrefix + trimColor + "/cancel "  + ChatColor.GOLD + "<amount> " + ChatColor.BLUE + "<item>" + descColor + " - Remove item from shop.");
        cmds.add(listPrefix + trimColor + "/find " + ChatColor.BLUE + "<item> " + ChatColor.GRAY + "[page]" + descColor + " - Find offers for the item.");
        cmds.add(listPrefix + trimColor + "/stock " + ChatColor.AQUA + "[player] " + ChatColor.GRAY + "[page]" + descColor + " - Browse offers.");
        cmds.add(listPrefix + trimColor + "/sales " + ChatColor.AQUA + "[player] " + ChatColor.GRAY + "[page]" + descColor + " - View transaction log.");
        cmds.add(listPrefix + trimColor + "/vs" + descColor + " - Virtual Shop's technical commands.");
        cmds.add(listPrefix + trimColor + "/buy " + ChatColor.GREEN + "confirm " + ChatColor.DARK_GREEN + "[toggle " + ChatColor.YELLOW + "<on/off>" + ChatColor.DARK_GREEN + "]" + descColor + " - Toggle buy confirmations.");
        cmds.add(listPrefix + trimColor + "/sell " + ChatColor.GREEN + "confirm " + ChatColor.DARK_GREEN + "[toggle " + ChatColor.YELLOW + "<on/off>" + ChatColor.DARK_GREEN + "]" + descColor + " - Toggle sell confirmations.");
        cmds.add(listPrefix + trimColor + "/updateprice " + ChatColor.GREEN + "confirm " + ChatColor.DARK_GREEN + "[toggle " + ChatColor.YELLOW + "<on/off>" + ChatColor.DARK_GREEN + "]" + descColor + " - Toggle update price confirmations.");
        
        PageList<String> commands = new PageList<>(cmds, 7);
        
        List<String> finalMsg = new ArrayList<String>();
        finalMsg.add(cm.formatHeaderLength(" " + cm.getPrefix() + " ", this.getClass()));
        finalMsg.add(trimColor + "              Command List - <Required> [Optional]");
        
        try {
			for(String s : commands.getPage(page)){
				finalMsg.add(s);
			}
		} catch (BadLocationException e) {
			cm.sendError(sender, "Page does not exist");
			return;
		}
        
        finalMsg.add(baseColor + "-" + trimColor + "Oo" + baseColor + "__________" + trimColor + "_____� " + ChatColor.GRAY + "Page " + page + " of " + commands.getTotalPages() + trimColor + " �_____" + baseColor + "__________" + trimColor + "oO" + baseColor + "-");
        
        for(String s : finalMsg)
        	sender.sendMessage(s);
        
	}
	
	public void vsList(CommandSender sender, int page){
		final String listPrefix = ChatColor.RED + " � ";
		final String baseColor = configM.getBaseColor();
    	final String trimColor = configM.getTrimColor();
    	final String descColor = configM.getDescriptionColor();
		
		List<String> cmds = new ArrayList<String>();
		cmds.add(listPrefix + trimColor + "/shop " + ChatColor.GRAY + "[page]" + descColor + " - View merchant commands.");
		cmds.add(listPrefix + trimColor + "/vs help " + ChatColor.GRAY + "[page]" + descColor + " - VirtualShop's technical commands.");
		cmds.add(listPrefix + trimColor + "/vs lookup " + ChatColor.BLUE + "[item]" + descColor + " - Browse item name information.");
		cmds.add(listPrefix + trimColor + "/vs version" + descColor + " - View plugin's version.");
		if(sender.hasPermission("virtualshop.admin.formatmarket"))
			cmds.add(listPrefix + trimColor + "/vs formatmarket " + ChatColor.BLUE + "[item]" + descColor + " - Reprice all items who's market price exceeds the set limit.");
		if(sender.hasPermission("virtualshop.admin.reload"))
			cmds.add(listPrefix + trimColor + "/vs reload" + descColor + " - Reload the entire plugin's jar file.");
		if(sender.hasPermission("virtualshop.admin.reloadconfig"))
			cmds.add(listPrefix + trimColor + "/vs reloadconfig" + descColor + " - Reload the plugin's configuration.");
		if(sender.hasPermission("virtualshop.admin.uuidnamesync"))
			cmds.add(listPrefix + trimColor + "/vs uuidnamesync [uuid]" + descColor + " - Syncs database names with uuids.");
		
		
		PageList<String> commands = new PageList<>(cmds, 6);
		
		List<String> finalMsg = new ArrayList<String>();
		finalMsg.add(cm.formatHeaderLength(" " + cm.getPrefix() + " ", this.getClass()));
        finalMsg.add(trimColor + "              Command List - <Required> [Optional]");
		
        try {
			for(String s : commands.getPage(page)){
				finalMsg.add(s);
			}
		} catch (BadLocationException e) {
			cm.sendError(sender, "Page does not exist");
			return;
		}
        
        finalMsg.add(baseColor + "-" + trimColor + "Oo" + baseColor + "__________" + trimColor + "_____� " + ChatColor.GRAY + "Page " + page + " of " + commands.getTotalPages() + trimColor + " �_____" + baseColor + "__________" + trimColor + "oO" + baseColor + "-");
        
        for(String s : finalMsg)
        	sender.sendMessage(s);
        
	}
	
	@SuppressWarnings("deprecation")
	public void cmdLookup(CommandSender sender, String[] args){
		
		if(!sender.hasPermission("virtualshop.merchant.lookup")){
			cm.noPermissions(sender);
			return;
		}
		
		final String baseColor = configM.getBaseColor();
    	final String trimColor = configM.getTrimColor();
    	
		ItemStack target = null;
		
		//Hand
		if(args.length == 1 || (args.length > 1 && (args[1].equalsIgnoreCase("hand") || args[1].equalsIgnoreCase("mainhand") || args[1].equalsIgnoreCase("offhand")))){
			if(!(sender instanceof Player)){
				cm.sendError(sender, "You must specify an item!");
				return;
			}
			Player p = (Player)sender;
			if(args.length > 1 && args[1].equalsIgnoreCase("offhand")) target = p.getInventory().getItemInOffHand();
			else target = p.getInventory().getItemInMainHand();
			
			if(target == null){
				cm.sendError(sender, "Lookup failed. You are not holding an item nor did you specify one.");
				return;
			}
		}
		
		if(args.length > 1 && target == null){
			target = idb.get(args[1], 0);
			if(target == null){
				target = idb.unsafeLookup(args[1]);
				if(target == null){
					cm.sendError(sender, "Lookup failed. Could not find " + args[1] + " in the database.");
					return;
				}
			}
		}
		
		//Final check for security.
		if(target == null){
			cm.sendError(sender, "Lookup failed, internal error has occurred.");
			return;
		}
		
		//Blocked items.
		if(target.getTypeId() == 440){
			cm.sendError(sender, "The vs does not yet support this item. Try again soon!");
			return;
		}
		
		List<String> aliases = idb.getAliases(target);
		String formattedAliasList;
		
		
		if(aliases.size() < 1){
			formattedAliasList = configM.getErrorColor() + "Could not find this item in the database. It's either damaged, not included in the VS, or from a recent update!";
		} else {
			formattedAliasList = trimColor + "Viable Names: " + aliases.toString();
			formattedAliasList = formattedAliasList.replaceAll("\\[", trimColor + "\\[" + baseColor);
			formattedAliasList = formattedAliasList.replaceAll("\\]", trimColor + "\\]" + baseColor);
		}
		
		String topLine = cm.getPrefix() + " " + cm.formatItem(target.getType().toString()).toUpperCase() + trimColor + " - " + cm.formatItem(target.getTypeId() + ":" + target.getData().getData());
		sender.sendMessage(topLine);
		sender.sendMessage(baseColor + formattedAliasList);
		
	}
	
	@SuppressWarnings("deprecation")
	public void formatMarket(CommandSender sender){
		
		if(!sender.hasPermission("virtualshop.admin.formatmarket")){
			cm.noPermissions(sender);
            return;
		}
		
		int amt = 0;
		for(Offer o : dbm.getAllOffers()){
			if(o.getPrice() > configM.getMaxPrice(o.getItem().getData().getItemTypeId(), o.getItem().getData().getData())){
				dbm.updatePrice(o.getId(), configM.getMaxPrice(o.getItem().getData().getItemTypeId(), o.getItem().getData().getData()));
				++amt;
			}
		}
		if(amt == 0)
			cm.sendSuccess(sender, "All listings are already correctly formatted!");
		else
			cm.sendSuccess(sender, "Successfully formatted " + amt + " listings.");
	}
	
	@SuppressWarnings("deprecation")
	public void formatMarket(CommandSender sender, String itm){
		
		if(!sender.hasPermission("virtualshop.admin.formatmarket")){
			cm.noPermissions(sender);
            return;
		}
		
		int amt = 0;
		ItemStack item = idb.get(itm, 0);
		if(itm.equalsIgnoreCase("hand") && sender instanceof Player){
			Player player = (Player)sender;
			item = new ItemStack(player.getItemInHand().getType(), 0, player.getItemInHand().getDurability());
			itm = idb.reverseLookup(item);
		}
		if(item == null){
			cm.wrongItem(sender, itm);
			return;
		}
		for(Offer o : dbm.getAllOffers()){
			boolean isSameItem = idb.reverseLookup(item).equalsIgnoreCase(idb.reverseLookup(o.getItem()));
			if(o.getPrice() > configM.getMaxPrice(o.getItem().getData().getItemTypeId(), o.getItem().getData().getData()) && isSameItem){
				dbm.updatePrice(o.getId(), configM.getMaxPrice(o.getItem().getData().getItemTypeId(), o.getItem().getData().getData()));
				++amt;
			}
		}
		if(amt == 0)
			cm.sendSuccess(sender, "All listings are already correctly formatted!");
		else
			cm.sendSuccess(sender, "Successfully formatted " + amt + " listings.");
	}
	
	public void cmdUUIDNameSync(CommandSender sender, Optional<String> uuid){
		
		if(!sender.hasPermission("virtualshop.admin.uuidnamesync")){
			cm.noPermissions(sender);
            return;
		}
		
		if(uuid.isPresent()){
			UUID target;
			try {
				target = UUID.fromString(uuid.get());
			} catch(IllegalArgumentException e){
				cm.sendError(sender, "Invalid UUID format.");
				return;
			}
			Pair<Boolean, Integer> response = dbm.syncNameToUUID(target);
			if(response.getKey()) cm.sendSuccess(sender, "The account associated with the specified UUID has been synced.");
			else if(response.getValue() == 1) cm.sendError(sender, "The account associated with the specified UUID is already synced.");
			else if(response.getValue() == 404) cm.sendError(sender, "The account associated with the specified UUID was not found in the database.");
			else cm.sendError(sender, "There was an error while querying the database.");
		} else {
			int updated = dbm.syncNameToUUID();
			if(updated > 0) cm.sendSuccess(sender, "Successfully synced " + updated + ((updated == 1) ? " account." : " accounts."));
			else cm.sendSuccess(sender, "All account names and UUIDs are already synced!");
		}
		
	}
	
	private void redirectCommand(CommandSender sender, String[] args){
		if(args.length > 0){
			CommandExecutor exec = plugin.getCommand(args[0]).getExecutor();
			if(exec == null) throw new IllegalArgumentException();
			String[] newArgs = new String[args.length-1];
			System.arraycopy(args, 1, newArgs, 0, newArgs.length);
			exec.onCommand(sender, null, "vs " + args[0], newArgs);
		}
	}
	
	public void cmdReload(CommandSender sender){
		
		if(!sender.hasPermission("virtualshop.developer.reload")){
			if(sender.hasPermission("virtualshop.admin.reloadconfig")){
				cm.sendError(sender, "You do not have permission to reload the plugin, reloading config instead.");
				this.cmdReloadConfig(sender);
				return;
			}
			cm.noPermissions(sender);
            return;
		}
		if(plugin.getServer().getPluginManager().getPlugin("VSReloader") == null){
			if(sender.hasPermission("virtualshop.admin.reloadconfig")){
				cm.sendError(sender, "VS Reloader not found, reloading config instead.");
				cmdReloadConfig(sender);
			} else {
				cm.sendError(sender, "VS Reloader not found, could not reload the plugin.");
			}
			return;
		}
		Runnable r = () -> {
			PluginUtil.reload(plugin);
		};
		r.run();
		cm.sendSuccess(sender, "Plugin successfully reloaded.");
	}
	
	public void cmdReloadConfig(CommandSender sender){
		
		if(!sender.hasPermission("virtualshop.admin.reloadconfig")){
			cm.noPermissions(sender);
            return;
		}
		
		try {
			ItemDB.reload();
		} catch (IOException e) {
			cm.sendError(sender, "Reference file 'items.csv' not found. Shutting down!");
            plugin.getPluginLoader().disablePlugin(plugin);
		}
		ConfigManager.reload();
		ChatManager.reload();
		
		cm.sendSuccess(sender, "Configuration successfully reloaded.");
		
	}
	
	/**
	 *  Never restrict this command.
	 */
	public void cmdVersion(CommandSender sender){
		ChatManager.getInstance().sendMessage(sender, "Virtual Shop version " + plugin.getDescription().getVersion());
	}
}

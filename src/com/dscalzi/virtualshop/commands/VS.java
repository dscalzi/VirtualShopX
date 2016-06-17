package com.dscalzi.virtualshop.commands;

import java.util.ArrayList;
import java.util.List;

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
import com.dscalzi.virtualshop.util.ItemDb;
import com.dscalzi.virtualshop.util.PageList;

public class VS implements CommandExecutor{

	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	private final DatabaseManager dbm;
	
	public VS(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			cm.denyBeta(sender);
			return true;
		}
		
		String cmd = command.getName();
		if(cmd.equalsIgnoreCase("shop")){
			if(args.length > 0){
				try{
    				int page = Integer.parseInt(args[0]);
    				this.cmdList(sender, page);
    				return true;
    			} catch (NumberFormatException e){
    				cm.sendError(sender, "Page does not exist");
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
				if(args[0].equalsIgnoreCase("formatmarket")){
					if(args.length > 1){
						this.formatMarket(sender, args[1]);
						return true;
					}
					this.formatMarket(sender);
					return true;
				}
				if(args[0].equalsIgnoreCase("reload")){
					this.cmdReload(sender);
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
	
	public void cmdList(CommandSender sender, int page){
    	final String listPrefix = ChatColor.RED + " • ";
    	final String baseColor = configM.getBaseColor();
    	final String trimColor = configM.getTrimColor();
    	final String descColor = configM.getDescriptionColor();
    	
    	List<String> cmds = new ArrayList<String>();
        cmds.add(listPrefix + trimColor + "/buy " + ChatColor.GOLD + "<amount> " + ChatColor.BLUE + "<item> " + ChatColor.YELLOW + "[maxprice]" + descColor + " - buy items.");
        cmds.add(listPrefix + trimColor + "/sell " + ChatColor.GOLD + "<amount> " + ChatColor.BLUE + "<item> " + ChatColor.YELLOW + "<price>" + descColor + " - sell items.");
        cmds.add(listPrefix + trimColor + "/cancel "  + ChatColor.GOLD + "<amount> " + ChatColor.BLUE + "<item> " + descColor + " - remove item from shop.");
        cmds.add(listPrefix + trimColor + "/find " + ChatColor.BLUE + "<item> " + ChatColor.GRAY + "[page] " + descColor + " - find offers for the item.");
        cmds.add(listPrefix + trimColor + "/stock " + ChatColor.AQUA + "[player] " + ChatColor.GRAY + "[page] " + descColor + " - browse offers.");
        cmds.add(listPrefix + trimColor + "/sales " + ChatColor.AQUA + "[player] " + ChatColor.GRAY + "[page] " + descColor + " - view transaction log.");
        cmds.add(listPrefix + trimColor + "/vs" + descColor + " - Virtual Shop's technical commands.");
        cmds.add(listPrefix + trimColor + "/buy " + ChatColor.GREEN + "confirm " + ChatColor.DARK_GREEN + "[toggle " + ChatColor.YELLOW + "<on/off>" + ChatColor.DARK_GREEN + "]" + descColor + " - Turn buy confirmations on/off.");
        cmds.add(listPrefix + trimColor + "/sell " + ChatColor.GREEN + "confirm " + ChatColor.DARK_GREEN + "[toggle " + ChatColor.YELLOW + "<on/off>" + ChatColor.DARK_GREEN + "]" + descColor + " - Turn sell confirmations on/off.");
        
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
        
        finalMsg.add(baseColor + "-" + trimColor + "Oo" + baseColor + "__________" + trimColor + "_____• " + ChatColor.GRAY + "Page " + page + " of " + commands.getTotalPages() + trimColor + " •_____" + baseColor + "__________" + trimColor + "oO" + baseColor + "-");
        
        for(String s : finalMsg)
        	sender.sendMessage(s);
        
	}
	
	public void vsList(CommandSender sender, int page){
		final String listPrefix = ChatColor.RED + " • ";
		final String baseColor = configM.getBaseColor();
    	final String trimColor = configM.getTrimColor();
    	final String descColor = configM.getDescriptionColor();
		
		List<String> cmds = new ArrayList<String>();
		cmds.add(listPrefix + trimColor + "/shop " + ChatColor.GRAY + "[page]" + descColor + " - View merchant commands.");
		cmds.add(listPrefix + trimColor + "/vs help " + ChatColor.GRAY + "[page]" + descColor + " - VirtualShop's technical commands.");
		cmds.add(listPrefix + trimColor + "/vs version" + descColor + " - View plugin's version.");
		if(sender.hasPermission("virtualshop.access.admin")){
			cmds.add(listPrefix + trimColor + "/vs formatmarket " + ChatColor.BLUE + "[item]" + descColor + " - Reprice all items who's market price exceeds the set limit.");
			cmds.add(listPrefix + trimColor + "/vs reload" + descColor + " - Reload the plugin's configuration.");
		}
		
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
        
        finalMsg.add(baseColor + "-" + trimColor + "Oo" + baseColor + "__________" + trimColor + "_____• " + ChatColor.GRAY + "Page " + page + " of " + commands.getTotalPages() + trimColor + " •_____" + baseColor + "__________" + trimColor + "oO" + baseColor + "-");
        
        for(String s : finalMsg)
        	sender.sendMessage(s);
        
	}
	
	@SuppressWarnings("deprecation")
	public void formatMarket(CommandSender sender){
		
		if(!sender.hasPermission("virtualshop.access.admin")){
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
		
		if(!sender.hasPermission("virtualshop.access.admin")){
			cm.noPermissions(sender);
            return;
		}
		
		int amt = 0;
		ItemStack item = ItemDb.get(itm, 0);
		if(itm.equalsIgnoreCase("hand") && sender instanceof Player){
			Player player = (Player)sender;
			item = new ItemStack(player.getItemInHand().getType(), 0, player.getItemInHand().getDurability());
			itm = ItemDb.reverseLookup(item);
		}
		if(item == null){
			cm.wrongItem(sender, itm);
			return;
		}
		for(Offer o : dbm.getAllOffers()){
			boolean isSameItem = ItemDb.reverseLookup(item).equalsIgnoreCase(ItemDb.reverseLookup(o.getItem()));
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
	
	public void cmdReload(CommandSender sender){
		
		if(!sender.hasPermission("virtualshop.access.admin")){
			cm.noPermissions(sender);
            return;
		}
		
		ConfigManager.reload();
		ChatManager.reload();
		
		cm.sendSuccess(sender, "Configuration successfully reloaded.");
	}
	
	public void cmdVersion(CommandSender sender){
		ChatManager.getInstance().sendMessage(sender, "Virtual Shop version " + plugin.getDescription().getVersion());
	}
}

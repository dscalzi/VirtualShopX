package com.dscalzi.virtualshop.commands;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.ChatManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.managers.UUIDManager;
import com.dscalzi.virtualshop.objects.Transaction;
import com.dscalzi.virtualshop.util.PageList;

import java.util.List;

public class Sales implements CommandExecutor{
	
	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	private final DatabaseManager dbm;
	private final UUIDManager uuidm;
	
	public Sales(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
		this.uuidm = UUIDManager.getInstance();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!sender.hasPermission("virtualshop.merchant.sales")){
            cm.noPermissions(sender);
            return true;
        }
		
		try{
			execute(sender, args);
    	} catch (LinkageError e){
    		cm.sendError(sender, "Linkage error occurred. Please restart the server to fix.");
    	}
		return true;
	}
	
	@SuppressWarnings("deprecation")
	public void execute(CommandSender sender, String[] args) throws LinkageError {
		final String baseColor = configM.getBaseColor();
		final String trimColor = configM.getTrimColor();
		
		boolean fullServerRecord = false;
		final String serverConstant = "@s";
		
		OfflinePlayer target = null;
		int requestedPage = 1;
		
		if(args.length > 0){
			boolean isString = false;
			try{
				requestedPage = Integer.parseInt(args[0]);
			} catch(NumberFormatException e){
				isString = true;
			}
			if(isString){
				//First try by UUID, if it fails try by player name.
				args[0] = args[0].replaceAll("['\"]", "");
	        	try {
	        		target = plugin.getServer().getOfflinePlayer(uuidm.formatFromInput(args[0]));
	        	} catch(IllegalArgumentException e){
	        		target = plugin.getServer().getOfflinePlayer(args[0]);
	        	}
	        	if(args[0].equalsIgnoreCase(serverConstant)){
	        		if(!sender.hasPermission("virtualshop.merchant.sales.server")){
	        			cm.sendError(sender, "You do not have permission to lookup the full server transaction log.");
	        			return;
	        		}
	        		args[0] = configM.getServerName();
	        		fullServerRecord = true;
	        	}
	        	if(args.length > 1){
		        	try{
						requestedPage = Integer.parseInt(args[1]);
		        	} catch (NumberFormatException e){
		        		requestedPage = 1;
		        	}
	        	}
			}
		}
		
		if(target == null){
			if(sender instanceof Player)
				target = (Player)sender;
			else
				if(!fullServerRecord){
					cm.sendError(sender, "You must either specify a player to lookup or give the argument " + serverConstant + ".");
					return;
				}
		}
		
		List<Transaction> transactions = null;
		try{
			transactions = fullServerRecord ? dbm.getTransactions() : dbm.getTransactions(target.getUniqueId());
		} catch (NullPointerException e){
			cm.noTransactions(sender, (target.getName() == null) ? args[0] : target.getName());
			return;
		}
		if(transactions.size() < 1){
        	cm.noTransactions(sender, (target.getName() == null) ? args[0] : target.getName());
        	return;
        }
		
		PageList<Transaction> sales = new PageList<Transaction>(7, transactions);
		List<Transaction> page = null;
		try{
			page = sales.getPage(requestedPage-1);
		} catch (IndexOutOfBoundsException e){
			if(requestedPage == 1)
				cm.noTransactions(sender, configM.getServerName());
			else
				cm.sendError(sender, "Page does not exist");
			return;
		}
		
		String headerContent = trimColor + "" + ChatColor.BOLD + "< " + baseColor + ChatColor.BOLD + "T" + baseColor + "ransaction " + ChatColor.BOLD + "L" + baseColor + "og ◄► " + ((fullServerRecord) ? configM.getServerName() : target.getName()) + trimColor + ChatColor.BOLD + " >";
		String header = cm.formatHeaderLength(headerContent, this.getClass());
		String footer = baseColor + "-" + trimColor + "Oo" + baseColor + "__________" + trimColor + "_____• " + ChatColor.GRAY + "Page " + requestedPage + " of " + sales.size() + trimColor + " •_____" + baseColor + "__________" + trimColor + "oO" + baseColor + "-";
		
		sender.sendMessage(header);
		for(Transaction t : page){
			sender.sendMessage(cm.formatTransaction(t));
		}
		sender.sendMessage(footer);
		
	}

}

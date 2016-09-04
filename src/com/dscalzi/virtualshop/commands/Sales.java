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
import com.dscalzi.virtualshop.util.Numbers;
import com.dscalzi.virtualshop.util.PageList;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;

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
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!sender.hasPermission("virtualshop.merchant.sales")){
            cm.noPermissions(sender);
            return true;
        }
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			cm.denyBeta(sender);
			return true;
		}
		if(args.length > 0 && args[0].contains("'")){
        	cm.noTransactions(sender, args[0]);
        	return true;
        }
		
		try{
			execute(sender, args);
			/*
			new BukkitRunnable(){
				
				@Override
				public void run(){
					execute(sender, args);
				}
	        }.runTaskAsynchronously(plugin);
	        */
    	} catch (LinkageError e){
    		cm.sendError(sender, "Linkage error occurred. Please restart the server to fix.");
    	}
		return true;
	}
	
    public void execute(CommandSender sender, String[] args) throws LinkageError {
    	final String baseColor = configM.getBaseColor();
    	final String trimColor = configM.getTrimColor();
    	
        OfflinePlayer target = (sender instanceof Player) ? plugin.getServer().getOfflinePlayer(((Player)sender).getUniqueId()) : null;
        
        int start = 1;
        List<Transaction> transactions = null;
        transactions = dbm.getTransactions();
        String header = trimColor + "" + ChatColor.BOLD + "< " + baseColor + ChatColor.BOLD + "T" + baseColor + "ransaction " + ChatColor.BOLD + "L" + baseColor + "og ◄► " + configM.getServerName() + trimColor + ChatColor.BOLD + " >";
        //If /sales args, check to see if it's a number
        if(args.length>0)  
        	start = Numbers.parseInteger(args[0]);
        //If /sales args is not a number (String)
        if(start < 0){
        	//First try by UUID, if it fails try by player name.
        	try {
        		target = plugin.getServer().getOfflinePlayer(uuidm.formatFromInput(args[0]));
        	} catch(IllegalArgumentException e){
        		target = plugin.getServer().getOfflinePlayer(uuidm.formatFromInput(args[0]));
        	}
			if(args.length > 1) 
				start = Numbers.parseInteger(args[1]);
			if(start < 0) 
				start = 1;
			try{
				transactions = dbm.getTransactions(target.getUniqueId());
			} catch (NullPointerException e){
				cm.noTransactions(sender, (target.getName() == null) ? args[0] : target.getName());
			}
            if(transactions.size() < 1){
            	cm.noTransactions(sender, (target.getName() == null) ? args[0] : target.getName());
            	return;
            }
            for(Transaction t : transactions){
            	if(t.getSeller().toLowerCase().indexOf(target.getName().toLowerCase()) != -1){
            		header = trimColor + "" + ChatColor.BOLD + "< " + baseColor + ChatColor.BOLD + "T" + baseColor + "ransaction " + ChatColor.BOLD + "L" + baseColor + "og ◄► " + t.getSeller() + trimColor + ChatColor.BOLD + " >";
            		break;
            	}
            	else if(t.getBuyer().toLowerCase().indexOf(target.getName().toLowerCase()) != -1){
            		header = trimColor + "" + ChatColor.BOLD + "< " + baseColor + ChatColor.BOLD + "T" + baseColor + "ransaction " + ChatColor.BOLD + "L" + baseColor + "og ◄► " + t.getBuyer() + trimColor + ChatColor.BOLD + " >";
            		break;
            	}
            }
        }
        
        PageList<Transaction> sales = new PageList<>(transactions, 7);
        List<String> finalMsg = new ArrayList<String>();
        finalMsg.add(cm.formatHeaderLength(header, this.getClass()));
        
        try {
			for(Transaction t : sales.getPage(start)){
			    finalMsg.add(cm.formatTransaction(t));
			}
		} catch (BadLocationException e) {
			if(start == 1)
				cm.noTransactions(sender, configM.getServerName());
			else
				cm.sendError(sender, "Page does not exist");
			return;
		}

        finalMsg.add(baseColor + "-" + trimColor + "Oo" + baseColor + "__________" + trimColor + "_____• " + ChatColor.GRAY + "Page " + start + " of " + sales.getTotalPages() + trimColor + " •_____" + baseColor + "__________" + trimColor + "oO" + baseColor + "-");
        
        
        for(String s : finalMsg)
        	sender.sendMessage(s);
        

    }


}

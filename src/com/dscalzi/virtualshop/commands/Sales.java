package com.dscalzi.virtualshop.commands;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.dscalzi.virtualshop.Chatty;
import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Transaction;
import com.dscalzi.virtualshop.util.Numbers;
import com.dscalzi.virtualshop.util.PageList;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;

public class Sales implements CommandExecutor{
	
	private VirtualShop plugin;
	
	public Sales(VirtualShop plugin){
		this.plugin = plugin;
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!sender.hasPermission("virtualshop.sales")){
            Chatty.noPermissions(sender);
            return true;
        }
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			Chatty.denyBeta(sender);
			return true;
		}
		if(args.length > 0 && args[0].contains("'")){
        	Chatty.noTransactions(sender, args[0]);
        	return true;
        }
		
		try{
			this.execute(sender, args);
    	} catch (LinkageError e){
    		Chatty.sendError(sender, "Linkage error occurred. Please restart the server to fix.");
    	}
		return true;
	}
	
    @SuppressWarnings("deprecation")
	public void execute(CommandSender sender, String[] args) throws LinkageError {
        
        OfflinePlayer target;
        int start = 1;
        List<Transaction> transactions;
        transactions = DatabaseManager.getTransactions();
        String header = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "< " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "T" + ChatColor.LIGHT_PURPLE + "ransaction " + ChatColor.BOLD + "L" + ChatColor.LIGHT_PURPLE + "og ◄► " + ConfigManager.getServerName() + " " + ChatColor.DARK_PURPLE + ChatColor.BOLD + " >";
        //If /sales args, check to see if it's a number
        if(args.length>0)  
        	start = Numbers.parseInteger(args[0]);
        //If /sales args is not a number (String)
        if(start < 0){
            String search = args[0];
            target = plugin.getServer().getOfflinePlayer(args[0]);
			if(args.length > 1) 
				start = Numbers.parseInteger(args[1]);
			if(start < 0) 
				start = 1;
			try{
				transactions = DatabaseManager.getTransactions(search);
			} catch (NullPointerException e){
				Chatty.noTransactions(sender, target.getName());
			}
            if(transactions.size() < 1){
            	Chatty.noTransactions(sender, target.getName());
            	return;
            }
            for(Transaction t : transactions){
            	if(t.seller.toLowerCase().indexOf(target.getName().toLowerCase()) != -1){
            		header = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "< " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "T" + ChatColor.LIGHT_PURPLE + "ransaction " + ChatColor.BOLD + "L" + ChatColor.LIGHT_PURPLE + "og ◄► " + t.seller + ChatColor.DARK_PURPLE + ChatColor.BOLD + " >";
            		break;
            	}
            	else if(t.buyer.toLowerCase().indexOf(target.getName().toLowerCase()) != -1){
            		header = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "< " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "T" + ChatColor.LIGHT_PURPLE + "ransaction " + ChatColor.BOLD + "L" + ChatColor.LIGHT_PURPLE + "og ◄► " + t.buyer + ChatColor.DARK_PURPLE + ChatColor.BOLD + " >";
            		break;
            	}
            }
        }
        
        int charCount = 74;
        charCount -= header.length()-1;
        if(charCount % 2 == 0)
        	charCount -= 1;
        String left = ChatColor.LIGHT_PURPLE + "";
        String right = ChatColor.DARK_PURPLE + "";
        for(int i=0; i<charCount/2-1; ++i)
        	left += "-";
        for(int i=0; i<charCount/2-1; ++i)
        	right += "-";
        
        PageList<Transaction> sales = new PageList<>(transactions, 7);
        List<String> finalMsg = new ArrayList<String>();
        finalMsg.add(left + header + right);
        
        try {
			for(Transaction t : sales.getPage(start)){
			    finalMsg.add(Chatty.formatTransaction(t));
			}
		} catch (BadLocationException e) {
			Chatty.sendError(sender, "Page does not exist");
			return;
		}

        finalMsg.add(ChatColor.LIGHT_PURPLE + "-" + ChatColor.DARK_PURPLE + "Oo" + ChatColor.LIGHT_PURPLE + "__________" + ChatColor.DARK_PURPLE + "_____• " + ChatColor.GRAY + "Page " + start + " of " + sales.getTotalPages() + ChatColor.DARK_PURPLE + " •_____" + ChatColor.LIGHT_PURPLE + "__________" + ChatColor.DARK_PURPLE + "oO" + ChatColor.LIGHT_PURPLE + "-");
        
        
        for(String s : finalMsg)
        	sender.sendMessage(s);
        

    }


}

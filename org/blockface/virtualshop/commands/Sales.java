package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.objects.Transaction;
import org.blockface.virtualshop.util.Numbers;
import org.blockface.virtualshop.util.PageList;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;

public class Sales
{
    @SuppressWarnings("deprecation")
	public static void execute(CommandSender sender, String[] args, VirtualShop plugin)
    {
        if(!sender.hasPermission("virtualshop.sales")){
            Chatty.noPermissions(sender);
            return;
        }
        
        if(args.length > 0 && args[0].contains("'")){
        	Chatty.noTransactions(sender, args[0]);
        	return;
        }
        
        OfflinePlayer target;
        int start = 1;
        List<Transaction> transactions;
        transactions = DatabaseManager.getTransactions();
        String header = ChatColor.GOLD + "" + ChatColor.BOLD + "< " + ChatColor.WHITE + ChatColor.BOLD + "T" + ChatColor.WHITE + "ransaction " + ChatColor.BOLD + "L" + ChatColor.WHITE + "og ◄► AtlasNetwork " + ChatColor.GOLD + ChatColor.BOLD + " >";
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
            		header = ChatColor.GOLD + "" + ChatColor.BOLD + "< " + ChatColor.WHITE + ChatColor.BOLD + "T" + ChatColor.WHITE + "ransaction " + ChatColor.BOLD + "L" + ChatColor.WHITE + "og ◄► " + t.seller + ChatColor.GOLD + ChatColor.BOLD + " >";
            		break;
            	}
            	else if(t.buyer.toLowerCase().indexOf(target.getName().toLowerCase()) != -1){
            		header = ChatColor.GOLD + "" + ChatColor.BOLD + "< " + ChatColor.WHITE + ChatColor.BOLD + "T" + ChatColor.WHITE + "ransaction " + ChatColor.BOLD + "L" + ChatColor.WHITE + "og ◄► " + t.buyer + ChatColor.GOLD + ChatColor.BOLD + " >";
            		break;
            	}
            }
        }
        
        int charCount = 74;
        charCount -= header.length()-1;
        if(charCount % 2 == 0)
        	charCount -= 1;
        String left = ChatColor.WHITE + "";
        String right = ChatColor.GOLD + "";
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

        finalMsg.add(ChatColor.WHITE + "-" + ChatColor.GOLD + "Oo" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "_____• " + ChatColor.GRAY + "Page " + start + " of " + sales.getTotalPages() + ChatColor.GOLD + " •_____" + ChatColor.WHITE + "__________" + ChatColor.GOLD + "oO" + ChatColor.WHITE + "-");
        
        
        for(String s : finalMsg)
        	sender.sendMessage(s);
        

    }


}

package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.objects.Transaction;
import org.blockface.virtualshop.util.Numbers;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class Sales
{
    public static void execute(CommandSender sender, String[] args, VirtualShop plugin)
    {
        if(!sender.hasPermission("virtualshop.sales"))
        {
            Chatty.noPermissions(sender);
            return;
        }
        int start = 1;
        List<Transaction> transactions;
        transactions = DatabaseManager.getTransactions();
        if(args.length>0)  start = Numbers.parseInteger(args[0]);
        if(start < 0)
        {
            String search = args[0];
			if(args.length > 1) start = Numbers.parseInteger(args[1]);
			if(start < 0) start = 1;
			start = (start -1) * 9;
            transactions = DatabaseManager.getTransactions(search);
        }
        else start = (start-1) * 9;

        int page = start/9 + 1;
        int pages = transactions.size()/9 + 1;
        if(page > pages)
        {
            start = 0;
            page = 1;
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "---------------" + ChatColor.GRAY + "Page (" + ChatColor.RED + page + ChatColor.GRAY + " of " + ChatColor.RED +pages + ChatColor.GRAY + ")" + ChatColor.DARK_GRAY + "---------------");
        int count =0;
        for(Transaction t : transactions)
        {
            if(count==start+9) break;
            if(count >= start)
            {
                sender.sendMessage(Chatty.formatTransaction(t));
            }
            count++;
        }


    }


}

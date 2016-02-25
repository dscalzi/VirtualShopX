package com.dscalzi.virtualshop.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshop.Chatty;
import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.util.ItemDb;
import com.dscalzi.virtualshop.util.Numbers;

import java.util.List;

public class Find implements CommandExecutor{
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	
	public Find(VirtualShop plugin){
		this.plugin = plugin;
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!sender.hasPermission("virtualshop.find")){
            Chatty.noPermissions(sender);
            return true;
        }
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			Chatty.denyBeta(sender);
			return true;
		}
		if(args.length < 1){
			Chatty.sendError(sender, "You need to specify the item.");
			return true;
		}
		
		 this.execute(sender, args);
		return true;
	}
	
    public void execute(CommandSender sender, String[] args)
    {
		ItemStack item = ItemDb.get(args[0], 0);
		if(item==null)
		{
			Chatty.wrongItem(sender, args[0]);
			return;
		}
        int page = 1;
		List<Offer> offers = DatabaseManager.getPrices(item);
        if(args.length>1)  page = Numbers.parseInteger(args[1]);
		if(offers.size()==0)
		{
			Chatty.sendError(sender, "No one is selling " + Chatty.formatItem(args[0]) + ".");
            return;
		}

        int start = (page-1) * 9;
        int pages = offers.size()/9 + 1;
		int count=0;
        if(page > pages)
        {
            start = 0;
            page = 1;
        }
        int charCount = 74;
        String header = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "< " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "L" + ChatColor.LIGHT_PURPLE + "istings ◄► " + ChatColor.BOLD + Character.toUpperCase(args[0].charAt(0)) + ChatColor.LIGHT_PURPLE + args[0].substring(1) + ChatColor.DARK_PURPLE + ChatColor.BOLD + " >";
        charCount -= header.length()-1;
        if(charCount % 2 == 0)
        	charCount -= 1;
        String left = ChatColor.LIGHT_PURPLE + "";
        String right = ChatColor.DARK_PURPLE + "";
        for(int i=0; i<charCount/2-1; ++i){
        	left += "-";
        }
        for(int i=0; i<charCount/2-1; ++i){
        	right += "-";
        }
        sender.sendMessage(left + header + right);
        for(Offer o : offers)
        {
            if(count==start+9) break;
            if(count >= start)
            {
                sender.sendMessage(Chatty.formatOffer(o));
            }
            count++;
		}
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "-" + ChatColor.DARK_PURPLE + "Oo" + ChatColor.LIGHT_PURPLE + "__________" + ChatColor.DARK_PURPLE + "_____• " + ChatColor.GRAY + "Page " + page + " of " + pages + ChatColor.DARK_PURPLE + " •_____" + ChatColor.LIGHT_PURPLE + "__________" + ChatColor.DARK_PURPLE + "oO" + ChatColor.LIGHT_PURPLE + "-");
    }
}

package com.dscalzi.virtualshop.commands;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.ChatManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.util.Numbers;
import com.dscalzi.virtualshop.util.PageList;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;

public class Stock implements CommandExecutor{
	
	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	private final DatabaseManager dbm;
	
	public Stock(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!sender.hasPermission("virtualshop.stock")){
            cm.noPermissions(sender);
            return true;
        }
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			cm.denyBeta(sender);
			return true;
		}
		if(args.length > 0 && args[0].contains("'")){
        	cm.noStock(sender, args[0]);
        	return true;
        }
		
		try{
			this.execute(sender, args);
    	} catch (LinkageError e){
    		cm.sendError(sender, "Linkage error occurred. Please restart the server to fix.");
    	}
		return true;
	}
	
    @SuppressWarnings("deprecation")
	public void execute(CommandSender sender, String[] args) throws LinkageError {
    	final String baseColor = configM.getBaseColor();
    	final String trimColor = configM.getTrimColor();
    	
        OfflinePlayer target;
        int start = 1;
        List<Offer> offers;
        offers = dbm.getBestPrices();
        String header = trimColor + "" + ChatColor.BOLD + "< " + baseColor + ChatColor.BOLD + "I" + baseColor + "tem " + ChatColor.BOLD + "S" + baseColor + "tock ◄► " + configM.getServerName() + trimColor + ChatColor.BOLD + " >";;
        if(args.length>0)  
        	start = Numbers.parseInteger(args[0]);
        if(start < 0){
        	target = plugin.getServer().getOfflinePlayer(args[0]);
            String seller = args[0];
			if(args.length > 1) 
				start = Numbers.parseInteger(args[1]);
			if(start < 0) 
				start = 1;
			try{
				offers = dbm.searchBySeller(seller);
			} catch (NullPointerException e){
				cm.noStock(sender, target.getName());
			}
            if(offers.size() < 1){
            	cm.noStock(sender, target.getName());
            	return;
            }
            for(Offer o : offers){
            	if(o.getSeller().toLowerCase().indexOf(target.getName().toLowerCase()) != -1){
            		header = trimColor + "" + ChatColor.BOLD + "< " + baseColor + ChatColor.BOLD + "I" + baseColor + "tem " + ChatColor.BOLD + "S" + baseColor + "tock ◄► " + o.getSeller() + trimColor + ChatColor.BOLD + " >";
            		break;
            	}
            }
        }
        
        int charCount = 74;
        charCount -= header.length()-1;
        if(charCount % 2 == 0)
        	charCount -= 1;
        String left = baseColor + "";
        String right = trimColor + "";
        for(int i=0; i<charCount/2-1; ++i)
        	left += "-";
        for(int i=0; i<charCount/2-1; ++i)
        	right += "-";
        
        PageList<Offer> stock = new PageList<>(offers, 7);
        List<String> finalMsg = new ArrayList<String>();
        finalMsg.add(left + header + right);
        
        try {
			for(Offer o : stock.getPage(start)){
				finalMsg.add(cm.formatOffer(o));
			}
		} catch (BadLocationException e) {
			if(start == 1)
				cm.sendError(sender, "There are no items on the market");
			else
				cm.sendError(sender, "Page does not exist");
			return;
		}
        
        finalMsg.add(baseColor + "-" + trimColor + "Oo" + baseColor + "__________" + trimColor + "_____• " + ChatColor.GRAY + "Page " + start + " of " + stock.getTotalPages() + trimColor + " •_____" + baseColor + "__________" + trimColor + "oO" + baseColor + "-");
        
        
        for(String s : finalMsg)
        	sender.sendMessage(s);
        
    }

}


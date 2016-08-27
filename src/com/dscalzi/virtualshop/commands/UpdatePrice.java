package com.dscalzi.virtualshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.managers.ChatManager;
import com.dscalzi.virtualshop.managers.ConfigManager;
import com.dscalzi.virtualshop.managers.DatabaseManager;

public class UpdatePrice implements CommandExecutor{

	private VirtualShop plugin;
	private final ChatManager cm;
	private final ConfigManager configM;
	private final DatabaseManager dbm;
	
	public UpdatePrice(VirtualShop plugin){
		this.plugin = plugin;
		this.cm = ChatManager.getInstance();
		this.configM = ConfigManager.getInstance();
		this.dbm = DatabaseManager.getInstance();
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
            cm.denyConsole(sender);
            return true;
        }
		if(!sender.hasPermission("virtualshop.merchant.updateprice")){
			cm.noPermissions(sender);
			return true;
		}
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			cm.denyBeta(sender);
			return true;
		}
		Player player = (Player)sender;
		if(!configM.getAllowedWorlds().contains(player.getWorld().getName())){
			cm.invalidWorld(sender, command.getName(), player.getWorld());
			return true;
		}
		if(args.length < 2){
			cm.sendError(sender, "Proper usage is /" + label + " <item> <newprice>");
			return true;
		}
		
		
		return true;
	}

	
	
	
}

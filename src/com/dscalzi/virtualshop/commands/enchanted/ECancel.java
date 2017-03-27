/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.commands.enchanted;

import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.objects.Confirmable;

public class ECancel implements CommandExecutor, Confirmable{

	private VirtualShop plugin;
	//private Map<Player, InventoryCache> activeInventories;
	private Map<Player, String> latestLabel;
	
	private ItemStack[] utility;
	
	public ECancel(VirtualShop plugin){
		
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		return false;
	}

	/*
	 * Make utility class that handles opening inventories and keeps track of the inventory,
	 * the originating class that opened it, and the player. Listen to the close event in that class
	 * and determine if the inventory that was opened was closed. If so remove it from
	 * the cache.
	 * 
	 * Use this class to open inventories exclusively. Allow inventory to be retrieved to have it
	 * be populated as it needs to be specifically.
	 */
	
}

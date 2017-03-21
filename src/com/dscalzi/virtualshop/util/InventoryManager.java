/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.util;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.MaterialData;

/**
 * New Inventory manager to comply with 1.9 inventories.
 * 
 * 
 * @author Daniel Scalzi
 *
 */
public class InventoryManager {

	private final Player _p;
	private final PlayerInventory _inv;
	
	public InventoryManager(final Player p){
		if(p == null) throw new NullPointerException();
		this._p = p;
		this._inv = p.getInventory();
	}
	
	@SuppressWarnings("deprecation")
	public static byte handleData(MaterialData data) {
    	try {
    		if(data!=null) return data.getData(); 
    		else return ((byte)(0));
    	} catch(Exception ex) {
    		return ((byte)(0));
    	}
    }
	
	public static byte handleData(short data) {
    	try { 
    		return ((byte)(data));
    	} catch(Exception ex) {
    		return ((byte)(0));
    	}
    }
	
	public boolean contains(ItemStack item){
		
		//Item without quantities cannot be held
		if(item.getAmount() <= 0) return false;
		
		int itemAmount = 0;
		
		for(Map.Entry<Integer, ? extends ItemStack> entry : _inv.all(item.getType()).entrySet()){
			ItemStack i = entry.getValue();
			if(i.isSimilar(item)) itemAmount += i.getAmount();
				
		}
		if(_inv.getItemInOffHand().isSimilar(item)) itemAmount += _inv.getItemInOffHand().getAmount();
		
		if(item.getAmount() <= itemAmount) return true;
		
		return false;
	}
	
	public void addItem(ItemStack item){
		
		Map<Integer, ItemStack> overflow = _inv.addItem(item);
		
		int dropAmount = 0;
		
		if(_p.getLocation() != null){
    	    for(ItemStack i : overflow.values()){
    	    	dropAmount += i.getAmount();
    	    }
    	}
		
		if(dropAmount == 0) return;
		
		if(_inv.getItemInOffHand().isSimilar(item)){
			if(_inv.getItemInOffHand().getAmount() < 64){
				ItemStack offhand = _inv.getItemInOffHand();
				int newAmt = (dropAmount + offhand.getAmount()) > 64 ? 64 : dropAmount + offhand.getAmount();
				dropAmount -= newAmt - offhand.getAmount();
				offhand = new ItemStack(item);
				offhand.setAmount(newAmt);
				_inv.setItemInOffHand(offhand);
				if(dropAmount <= 0) return;
			}
		} else if(_inv.getItemInOffHand().getType() == Material.AIR){
			ItemStack offhand = _inv.getItemInOffHand();
			offhand = new ItemStack(item);
			int newAmt = (dropAmount >= 64) ? 64 : dropAmount;
			dropAmount -= newAmt;
			offhand.setAmount(newAmt);
			_inv.setItemInOffHand(offhand);
		}
		
		while(dropAmount > 0){
    		int amtToDrop = 64;
    		if(dropAmount < 64) amtToDrop = dropAmount;
    		item.setAmount(amtToDrop);
    		_p.getWorld().dropItem(_p.getLocation(), item);
    		dropAmount -= amtToDrop;
    	}
	}
	
	public int getFreeSpace(ItemStack item){
		ItemStack[] inv = _inv.getContents();
        int openNum = 0;
        //Adjusting length, in 1.9+ .getContents includes armor. We only want inventory.
        for(int i=0; i<inv.length-5; ++i){
    		if(inv[i] == null){
    			openNum += 64;
    			continue;
    		} else if(inv[i].isSimilar(item)){
    			openNum += (64-inv[i].getAmount());
    		}
    	}
        if(_inv.getItemInOffHand().isSimilar(item)){
        	openNum += (64-_inv.getItemInOffHand().getAmount());
        } else if(_inv.getItemInOffHand().getType() == Material.AIR){
        	openNum += 64;
        }
        return openNum;
	}
	
	public void removeItem(ItemStack item) {
    	int requestedDeletion = item.getAmount();
    	    	
    	for(Map.Entry<Integer,? extends ItemStack> entry : _inv.all(item.getType()).entrySet()) {
    		ItemStack i = entry.getValue();
    		if(item.isSimilar(i)){
    			if(requestedDeletion>=i.getAmount()) {
    				requestedDeletion -= i.getAmount();
    				
    				_inv.clear(entry.getKey());
    			} else {
    				if(requestedDeletion <= 0) break;
    				
    				ItemStack rep = entry.getValue();
    				rep.setAmount(rep.getAmount()-requestedDeletion);
    				_inv.setItem(entry.getKey(), rep);
    				
    				requestedDeletion = 0;
    				
    				break;
    			}
    		}
    	}
    	if(requestedDeletion > 0){
    		if(_inv.getItemInOffHand().isSimilar(item)){
    			ItemStack reduced = _inv.getItemInOffHand();
    			int reducedNumber = reduced.getAmount()-requestedDeletion <= 0 ? 0 : reduced.getAmount()-requestedDeletion;
    			requestedDeletion -= item.getAmount() <= 0 ? 0 : item.getAmount();
    			reduced.setAmount(reducedNumber);
    			_inv.setItemInOffHand(reduced);
    		}
    	}
    }
	
}

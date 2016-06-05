package com.dscalzi.virtualshop.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.MaterialData;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("deprecation")
public class InventoryManager {
    private final PlayerInventory _inv;
    private final Player _player;
    private final Location _loc;
    
    public InventoryManager(final PlayerInventory inv) {
        _inv = inv;
        _player = null;
        _loc = null;
    }
    
    public InventoryManager(final Player player) {
    	_inv = player.getInventory();
    	_player = player;
    	_loc = null;
    }
    
    public InventoryManager(final PlayerInventory inv, final Player player) {
    	_inv = inv;
    	_player = player;
    	_loc = null;
    }
    
    public InventoryManager(final PlayerInventory inv, final Location loc) {
    	_inv = inv;
    	_player = null;
    	_loc = loc;
    }
    
    public InventoryManager(final Player player, final Location loc) {
    	_inv = player.getInventory();
    	_player = null;
    	_loc = loc;
    }
    
    public static byte handleData(MaterialData data) {
    	try {
    		if(data!=null) return data.getData(); else return ((byte)(0));
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
    
    public ItemStack quantify(ItemStack type) {
    	return this.quantify(type, true, false);
    }
    
    public ItemStack quantify(ItemStack type, boolean meta) {
    	return this.quantify(type, meta, false);
    }
    
    public ItemStack quantify(ItemStack type, boolean meta, boolean durability) {
    	int amount = 0;
    	    	
    	for(Map.Entry<Integer,? extends ItemStack> which : _inv.all(type.getType()).entrySet()) if((type.getType().getMaxDurability()<=0) ? ((!meta)||(handleData(type.getData())==handleData(which.getValue().getDurability()))) : ((!durability)||(type.getDurability()==which.getValue().getDurability()))) amount += which.getValue().getAmount();
    	
    	return new ItemStack(type.getType(), amount, type.getDurability(), handleData(type.getData()));
    }
    
    public boolean contains(ItemStack stack) {
    	return this.contains(stack, true, false);
    }
    
    public boolean contains(ItemStack stack, boolean meta) {
    	return this.contains(stack, meta, false);
    }
    
    public boolean contains(ItemStack stack, boolean meta, boolean durability) {
    	return (((stack.getAmount()>0)&&(this.quantify(stack, meta, durability).getAmount()>=stack.getAmount()))||((stack.getAmount()==0)&&(this.quantify(stack, meta, durability).getAmount()>=1)));
    }
    
    public ItemStack addItem(ItemStack stack) {

        HashMap<Integer, ItemStack> remaining = _inv.addItem(stack);
    	
    	if(_player!=null && _loc!=null){
    	    for(ItemStack derp : remaining.values()){
    	        _player.getWorld().dropItemNaturally(_loc, derp);
    	    }
    	}
    	
    	return new ItemStack(stack.getType(), 0, stack.getDurability(), handleData(stack.getData()));
    }
    
    public ItemStack remove(ItemStack stack) {
    	return this.remove(stack, true);
    }
    
    public ItemStack remove(ItemStack stack, boolean meta) {
    	return this.remove(stack, meta, false);
    }
    
    public ItemStack remove(ItemStack stack, boolean meta, boolean durability) {
    	int amount = stack.getAmount();
    	    	
    	for(Map.Entry<Integer,? extends ItemStack> which : _inv.all(stack.getType()).entrySet()) {
    		if((stack.getType().getMaxDurability()<=0) ? ((!meta)||(handleData(stack.getData())==handleData(which.getValue().getDurability()))) : ((!durability)||(stack.getDurability()==which.getValue().getDurability())))  {
    			if(amount>=which.getValue().getAmount()) {
    				amount -= which.getValue().getAmount();
    				
    				_inv.clear(which.getKey());
    			} else {
    				if(amount<=0) break;
    				
    				ItemStack rep = which.getValue();
    				
    				rep.setAmount(rep.getAmount()-amount);
    				
    				_inv.setItem(which.getKey(), rep);
    				
    				amount = 0;
    			}
    		}
    	}
    	
    	return new ItemStack(stack.getType(), (stack.getAmount()-amount), stack.getDurability(), handleData(stack.getData()));
    }
}
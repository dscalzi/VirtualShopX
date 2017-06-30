/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.objects.dataimpl;

import java.util.Map;

import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshop.objects.VsDataCache;
import com.dscalzi.virtualshop.util.ItemDB;

public class ECancelData implements VsDataCache{

	private static final long serialVersionUID = -6990185031969663445L;
	
	private Map<String,Object> serializedItem;
	private long timeSince;
	
	private final int INVENTORYSPACE;
	private final double PRICE;
	private transient ItemStack ITEM;
	private transient ItemStack CLEANEDITEM;
	private long TIME;
	
	public ECancelData(ItemStack item, double price, int inventorySpace, long systemTime){
		this.ITEM = item;
		this.CLEANEDITEM = ItemDB.getCleanedItem(item);
		this.PRICE = price;
		this.INVENTORYSPACE = inventorySpace;
		this.TIME = systemTime;
	}
	
	public int getInventorySpace(){
		return this.INVENTORYSPACE;
	}
	
	public ItemStack getItem(){
		return this.ITEM;
	}
	
	public ItemStack getCleanedItem(){
		return CLEANEDITEM;
	}
	
	public double getPrice(){
		return this.PRICE;
	}
	
	@Override
	public long getTransactionTime() {
		return this.TIME;
	}

	@Override
	public void serialize() {
		serializedItem = getItem().serialize();
		timeSince = System.currentTimeMillis() - TIME;
	}

	@Override
	public void deserialize() {
		ITEM = ItemStack.deserialize(this.serializedItem);
		CLEANEDITEM = ItemDB.getCleanedItem(ITEM);
		TIME = System.currentTimeMillis() - timeSince;
	}
	
	@Override
	public boolean equals(Object other){
		if(!(other instanceof ECancelData))
			return false;
		ECancelData o = (ECancelData) other;
		if(this == other) 
			return true;
		if(this.getPrice() != o.getPrice())
			return false;
		if(this.getInventorySpace() != o.getInventorySpace())
			return false;
		if(!this.getItem().equals(o.getItem()))
			return false;
		return true;
	}

}

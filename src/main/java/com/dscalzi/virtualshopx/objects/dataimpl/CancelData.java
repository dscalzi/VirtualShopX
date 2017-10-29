/*
 * VirtualShopX
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshopx.objects.dataimpl;

import java.util.Map;

import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshopx.objects.VsDataCache;

public class CancelData implements VsDataCache{

	private static final long serialVersionUID = -6990185031969663445L;
	
	private Map<String,Object> serializedItem;
	private long timeSince;
	
	private final int AMOUNT;
	private final int LISTINGAMOUNT;
	private final int INVENTORYSPACE;
	private transient ItemStack ITEM;
	private long TIME;
	private final String[] ARGS;
	
	public CancelData(int amount, int listingAmount, int inventorySpace, ItemStack item, long systemTime, String[] args){
		this.AMOUNT = amount;
		this.LISTINGAMOUNT = listingAmount;
		this.INVENTORYSPACE = inventorySpace;
		this.ITEM = item;
		this.TIME = systemTime;
		this.ARGS = args;
	}
	
	public int getAmount(){
		return this.AMOUNT;
	}
	
	public int getListingAmount(){
		return this.LISTINGAMOUNT;
	}
	
	public int getInventorySpace(){
		return this.INVENTORYSPACE;
	}
	
	public ItemStack getItem(){
		return this.ITEM;
	}
	
	public String[] getArgs(){
		return this.ARGS;
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
		TIME = System.currentTimeMillis() - timeSince;
	}
	
	@Override
	public boolean equals(Object other){
		if(!(other instanceof CancelData))
			return false;
		CancelData o = (CancelData) other;
		if(this == other) 
			return true;
		if(this.getAmount() != o.getAmount())
			return false;
		if(this.getListingAmount() != o.getListingAmount())
			return false;
		if(this.getInventorySpace() != o.getInventorySpace())
			return false;
		if(!this.getItem().equals(o.getItem()))
			return false;
		return true;
	}

}

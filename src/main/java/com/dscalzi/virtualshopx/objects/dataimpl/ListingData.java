/*
 * VirtualShopX
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshopx.objects.dataimpl;

import java.util.Map;

import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshopx.objects.VsDataCache;

public class ListingData implements VsDataCache{

	private static final long serialVersionUID = -8546925459706301446L;
	
	private Map<String,Object> serializedItem;
	private long timeSince;
	
	private final int AMOUNT;
	private transient ItemStack ITEM;
	private final double PRICE;
	private final int CURRENTLYLISTED;
	private final double OLDPRICE;
	private long TIME;
	private final String[] ARGS;
	
	public ListingData(int amount, ItemStack item, double price, int currentlyListed, double oldPrice, long systemTime, String[] args){
		this.AMOUNT = amount;
		this.ITEM = item;
		this.PRICE = price;
		this.CURRENTLYLISTED = currentlyListed;
		this.OLDPRICE = oldPrice;
		this.TIME = systemTime;
		this.ARGS = args;
	}
	
	public int getAmount(){
		return this.AMOUNT;
	}
	
	public ItemStack getItem(){
		return this.ITEM;
	}
	
	public double getPrice(){
		return this.PRICE;
	}
	
	public int getCurrentListings(){
		return this.CURRENTLYLISTED;
	}
	
	public double getOldPrice(){
		return this.OLDPRICE;
	}
	
	public long getTransactionTime(){
		return this.TIME;
	}
	
	public String[] getArgs(){
		return this.ARGS;
	}
	
	public void serialize(){
		serializedItem = getItem().serialize();
		timeSince = System.currentTimeMillis() - TIME;
	}
	
	public void deserialize(){
		ITEM = ItemStack.deserialize(this.serializedItem);
		TIME = System.currentTimeMillis() - timeSince;
	}
	
	/** 
	 * Compares the transaction data excluding the system time, and initial string arguments.
	 */
	public boolean equals(Object other){
		if(!(other instanceof ListingData))
			return false;
		ListingData o = (ListingData) other;
		if (this == other) 
            return true;
		if(this.getAmount() != o.getAmount())
			return false;
		if(!this.getItem().equals(o.getItem()))
			return false;
		if(this.getCurrentListings() != o.getCurrentListings())
			return false;
		if(this.getOldPrice() != o.getOldPrice())
			return false;
		if(this.getPrice() != o.getPrice())
			return false;
		return true;
	}
	
}

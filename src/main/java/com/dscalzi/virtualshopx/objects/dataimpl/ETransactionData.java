/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.objects.dataimpl;

import java.util.Map;

import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshopx.objects.VsDataCache;
import com.dscalzi.virtualshopx.util.ItemDB;

public class ETransactionData implements VsDataCache{

	private static final long serialVersionUID = 1L;

	private Map<String,Object> serializedItem;
	private long timeSince;
	
	private transient ItemStack ITEM;
	private transient ItemStack CLEANEDITEM;
	private final double PRICE;
	private long TIME;
	
	public ETransactionData(ItemStack item, double price, long time){
		this.ITEM = item;
		this.CLEANEDITEM = ItemDB.getCleanedItem(item);
		this.PRICE = price;
		this.TIME = time;
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
	public long getTransactionTime(){
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
	
	/** 
	 * Compares the transaction data excluding the system time.
	 */
	public boolean equals(Object other){
		if(!(other instanceof ETransactionData))
			return false;
		ETransactionData o = (ETransactionData) other;
		if (this == other) 
            return true;
		if(!this.getItem().equals(o.getItem()))
			return false;
		if(this.getPrice() != o.getPrice())
			return false;
		return true;
	}

}

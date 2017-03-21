/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.objects;

import java.util.List;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

/**
 * Data Caching for use in /buy
 */
public class TransactionData implements VsDataCache{

	private static final long serialVersionUID = 3811036933174589119L;
	
	private Map<String,Object> serializedItem;
	private long timeSince;
	
	private final int AMOUNT;
	private transient ItemStack ITEM;
	private final double PRICE;
	private final double MAXPRICE;
	private final List<Offer> OFFERS;
	private long TIME;
	private final String[] ARGS;
	
	private final boolean canContinue;
	
	public TransactionData(int amount, ItemStack item, double price, double maxPrice, List<Offer> offers, long systemTime, String[] args){
		this(amount, item, price, maxPrice, offers, systemTime, args, true);
	}
	public TransactionData(int amount, ItemStack item, double price, double maxPrice, List<Offer> offers, long systemTime, String[] args, boolean canContinue){
		this.AMOUNT = amount;
		this.ITEM = item;
		this.PRICE = price;
		this.MAXPRICE = maxPrice;
		this.OFFERS = offers;
		this.TIME = systemTime;
		this.ARGS = args;
		this.canContinue = canContinue;
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
	
	public double getMaxPrice(){
		return this.MAXPRICE;
	}
	
	public List<Offer> getOffers(){
		return this.OFFERS;
	}
	
	public long getTransactionTime(){
		return this.TIME;
	}
	
	public String[] getArgs(){
		return this.ARGS;
	}
	
	public boolean canContinue(){
		return this.canContinue;
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
		if(!(other instanceof TransactionData))
			return false;
		TransactionData o = (TransactionData) other;
		if (this == other) 
            return true;
		if(this.getAmount() != o.getAmount())
			return false;
		if(!this.getItem().equals(o.getItem()))
			return false;
		if(this.getPrice() != o.getPrice())
			return false;
		if(this.getMaxPrice() != o.getMaxPrice())
			return false;
		if(!this.getOffers().equals(o.getOffers()))
			return false;
		if(this.canContinue() != o.canContinue())
			return false;
		return true;
	}
	
}

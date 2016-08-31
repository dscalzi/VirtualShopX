package com.dscalzi.virtualshop.objects;

import org.bukkit.inventory.ItemStack;

public class ListingData implements VsDataCache{

	private static final long serialVersionUID = -8546925459706301446L;
	
	private final int AMOUNT;
	private final ItemStack ITEM;
	private final double PRICE;
	private final int CURRENTLYLISTED;
	private final double OLDPRICE;
	private final long TIME;
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
	
	/** 
	Compares the transaction data excluding the system time, and initial string arguments.
	*/
	public boolean equals(ListingData other){
		if (this == other) 
            return true;
		if(this.getAmount() != other.getAmount())
			return false;
		if(!this.getItem().equals(other.getItem()))
			return false;
		if(this.getCurrentListings() != other.getCurrentListings())
			return false;
		if(this.getOldPrice() != other.getOldPrice())
			return false;
		if(this.getPrice() != other.getPrice())
			return false;
		return true;
	}
}

package com.dscalzi.virtualshop.objects;

import java.util.Map;

import org.bukkit.inventory.ItemStack;

public class EListingData implements VsDataCache{

private static final long serialVersionUID = -8546925459706301446L;
	
	private Map<String,Object> serializedItem;
	private long timeSince;
	
	private transient ItemStack ITEM;
	private final double PRICE;
	private final double OLDPRICE;
	private long TIME;
	private final String[] ARGS;
	
	public EListingData(ItemStack item, double price, double oldPrice, long systemTime, String[] args){
		this.ITEM = item;
		this.PRICE = price;
		this.OLDPRICE = oldPrice;
		this.TIME = systemTime;
		this.ARGS = args;
	}
	
	public ItemStack getItem(){
		return this.ITEM;
	}
	
	public double getPrice(){
		return this.PRICE;
	}
	
	public double getOldPrice(){
		return this.OLDPRICE;
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
	
	/** 
	 * Compares the transaction data excluding the system time, and initial string arguments.
	 */
	public boolean equals(Object other){
		if(!(other instanceof EListingData))
			return false;
		EListingData o = (EListingData) other;
		if (this == other) 
            return true;
		if(!this.getItem().equals(o.getItem()))
			return false;
		if(this.getOldPrice() != o.getOldPrice())
			return false;
		if(this.getPrice() != o.getPrice())
			return false;
		return true;
	}

}

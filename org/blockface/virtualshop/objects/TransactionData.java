package org.blockface.virtualshop.objects;

import java.util.List;

import org.bukkit.inventory.ItemStack;

/**
Data Caching for use in /buy
*/
public class TransactionData {

	private final int AMOUNT;
	private final ItemStack ITEM;
	private final double PRICE;
	private final double MAXPRICE;
	private final List<Offer> OFFERS;
	private final long TIME;
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
	
	/** 
	Compares the transaction data excluding the system time, and initial string arguments.
	*/
	public boolean equals(TransactionData other){
		if (this == other) 
            return true;
		if(this.getAmount() != other.getAmount())
			return false;
		if(!this.getItem().equals(other.getItem()))
			return false;
		if(this.getPrice() != other.getPrice())
			return false;
		if(this.getMaxPrice() != other.getMaxPrice())
			return false;
		if(!this.getOffers().equals(other.getOffers()))
			return false;
		if(this.canContinue() != other.canContinue())
			return false;
		return true;
	}
	
}

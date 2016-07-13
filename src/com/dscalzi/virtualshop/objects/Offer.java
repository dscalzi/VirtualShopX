package com.dscalzi.virtualshop.objects;

import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshop.managers.UUIDManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class Offer
{
    private ItemStack item;
    private double price;
    private String seller;
    private int id;

	public Offer(String seller, int id, short damage, double price, int amount){
        this.item = new ItemStack(id,amount,damage);
        this.seller = seller;
        this.price = price;
    }

    public Offer(String seller, ItemStack item, double price){
        this.seller = seller;
        this.item = item;
        this.price = price;
    }

    public static List<Offer> listOffers(ResultSet result){
    	UUIDManager uuidm = UUIDManager.getInstance();
        List<Offer> ret = new ArrayList<Offer>();
        try {
            while(result.next()){
                Offer o = new Offer(uuidm.playerFromUUID(UUID.fromString(result.getString("uuid"))).getName(), result.getInt("item"), (short)result.getInt("damage"),result.getDouble("price"),result.getInt("amount"));
                o.setId(result.getInt("id"));
                ret.add(o);
            }
        } catch (SQLException e) {
        }
        return ret;
    }
    
    public boolean equals(Object obj){
    	if(!(obj instanceof Offer))
    		return false;
    	Offer other = (Offer)obj;
    	if(other == this)
    		return true;
    	if(!this.getItem().equals(other.getItem()))
    		return false;
    	if(this.getPrice() != other.getPrice())
    		return false;
    	if(!this.getSeller().equals(other.getSeller()))
    		return false;
    	return true;
    }
    
    public ItemStack getItem() { return item; }

	public void setItem(ItemStack item) { this.item = item; }

	public double getPrice() { return price; }

	public void setPrice(double price) { this.price = price; }

	public String getSeller() {	return seller; }

	public void setSeller(String seller) { this.seller = seller; }

	public int getId() { return id; }

	protected void setId(int id) {	this.id = id;}
}

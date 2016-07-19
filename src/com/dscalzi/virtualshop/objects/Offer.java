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
	private UUIDManager uuidm = UUIDManager.getInstance();
	
	private UUID sellerUUID;
    private ItemStack item;
    private double price;
    private int id;

	public Offer(UUID sellerUUID, int id, short damage, double price, int amount){
        this.item = new ItemStack(id,amount,damage);
        this.sellerUUID = sellerUUID;
        this.price = price;
    }

    public Offer(UUID sellerUUID, ItemStack item, double price){
        this.sellerUUID = sellerUUID;
        this.item = item;
        this.price = price;
    }

    public static List<Offer> listOffers(ResultSet result){
        List<Offer> ret = new ArrayList<Offer>();
        try {
            while(result.next()){
                Offer o = new Offer(UUID.fromString(result.getString("uuid")), result.getInt("item"), (short)result.getInt("damage"),result.getDouble("price"),result.getInt("amount"));
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
    	if(!this.getSellerUUID().equals(other.getSellerUUID()))
    		return false;
    	return true;
    }
    
    public ItemStack getItem() { return item; }

	public void setItem(ItemStack item) { this.item = item; }

	public double getPrice() { return price; }

	public void setPrice(double price) { this.price = price; }

	/* Retrieve seller name dynamically */
	public String getSeller() {	return uuidm.getPlayerName(getSellerUUID()).get(); }
	
	public UUID getSellerUUID() {	return sellerUUID; }

	public void setSellerUUID(UUID sellerUUID) { this.sellerUUID = sellerUUID; }

	public int getId() { return id; }

	protected void setId(int id) {	this.id = id;}
}

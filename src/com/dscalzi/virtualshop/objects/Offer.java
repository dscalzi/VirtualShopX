/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.objects;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.dscalzi.virtualshop.managers.MessageManager;
import com.dscalzi.virtualshop.util.ItemDB;
import com.dscalzi.virtualshop.util.UUIDUtil;

import net.md_5.bungee.api.ChatColor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class Offer {
	
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
    
    public static List<Offer> listEnchantedOffers(ResultSet result){
    	List<Offer> ret = new ArrayList<Offer>();
    	try{
    		while(result.next()){
    			ItemStack item = new ItemStack(result.getInt("item"), 1, (short)result.getInt("data"));
    			Map<Enchantment, Integer> enchantments = ItemDB.parseEnchantData(result.getString("edata"));
    			ItemDB.addEnchantments(item, enchantments);
    			ItemMeta meta = item.getItemMeta();
    			Double price = result.getDouble("price");
    			Offer o = new Offer(UUID.fromString(result.getString("uuid")), item, price);
    			List<String> desc = new ArrayList<String>();
    			desc.add("");
    			desc.add(ChatColor.YELLOW + "Price: " + MessageManager.getInstance().formatPrice(price));
    			desc.add(ChatColor.RED + "Seller: " + o.getSeller());
    			meta.setLore(desc);
    			item.setItemMeta(meta);
    			o.setItem(item);
    			o.setId(result.getInt("id"));
    			ret.add(o);
    		}
    	} catch (SQLException e){
    		//Empty Catch
    	}
    	return ret;
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((item == null) ? 0 : item.hashCode());
		long temp;
		temp = Double.doubleToLongBits(price);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((sellerUUID == null) ? 0 : sellerUUID.hashCode());
		return result;
	}

    @Override
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
	public String getSeller() {	
		Optional<String> name = UUIDUtil.getPlayerName(getSellerUUID());
		return name.isPresent() ? name.get() : getSellerUUID().toString(); 
	}
	
	public UUID getSellerUUID() {	return sellerUUID; }

	public void setSellerUUID(UUID sellerUUID) { this.sellerUUID = sellerUUID; }

	public int getId() { return id; }

	protected void setId(int id) {	this.id = id;}
}

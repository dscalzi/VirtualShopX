/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.objects;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshopx.managers.DatabaseManager;
import com.dscalzi.virtualshopx.util.ItemDB;
import com.dscalzi.virtualshopx.util.UUIDUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Transaction {
	
    private UUID sellerUUID;
    private UUID buyerUUID;
    private ItemStack item;
    private double cost;
    private long timestamp;

    public Transaction(UUID sellerUUID, UUID buyerUUID, Material m, int amount, double cost, long timestamp){
        this(sellerUUID, buyerUUID, m, amount, cost, timestamp, null);
    }
    
    public Transaction(UUID sellerUUID, UUID buyerUUID, Material m, int amount, double cost, long timestamp, String edata){
    	this.sellerUUID = sellerUUID;
        this.buyerUUID = buyerUUID;
        this.item = new ItemStack(m, amount);
        if(edata != null) ItemDB.addEnchantments(item, ItemDB.deserializeEnchantmentData(edata));
        this.cost = cost;
        this.timestamp = timestamp;
    }

	public static List<Transaction> listTransactions(ResultSet result){
        List<Transaction> ret = new ArrayList<Transaction>();
        try {
            while(result.next()){
                Transaction t = new Transaction(UUID.fromString(result.getString(DatabaseManager.KEY_SELLER_UUID)), UUID.fromString(result.getString(DatabaseManager.KEY_BUYER_UUID)), Material.valueOf(result.getString(DatabaseManager.KEY_MATERIAL)), result.getInt(DatabaseManager.KEY_QUANTITY), result.getDouble(DatabaseManager.KEY_COST), result.getLong(DatabaseManager.KEY_TIMESTAMP), result.getString(DatabaseManager.KEY_ENCHANTMENT_DATA));
                ret.add(t);
            }
        } catch (SQLException e) {
        }
        return ret;
    }

	public String getSeller() { 
		Optional<String> name = UUIDUtil.getPlayerName(getSellerUUID());
		return name.isPresent() ? name.get() : getSellerUUID().toString();
	}
	
	public UUID getSellerUUID() { return sellerUUID; }
	
	public String getBuyer() { 
		Optional<String> name = UUIDUtil.getPlayerName(getBuyerUUID());
		return name.isPresent() ? name.get() : getBuyerUUID().toString(); 
	}

	public UUID getBuyerUUID() { return buyerUUID; }

	public ItemStack getItem() { return item; }
	
	public boolean isEnchanted() { return ItemDB.hasEnchantments(item); }

	public double getCost() { return cost; }
	
	public long getTimestamp() { return timestamp; }

}

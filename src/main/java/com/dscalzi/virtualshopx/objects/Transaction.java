/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.objects;

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

@SuppressWarnings("deprecation")
public class Transaction {
	
    private UUID sellerUUID;
    private UUID buyerUUID;
    private ItemStack item;
    private double cost;
    private long timestamp;

    public Transaction(UUID sellerUUID, UUID buyerUUID, int id, short damage, int amount, double cost, long timestamp){
        this(sellerUUID, buyerUUID, id, damage, amount, cost, timestamp, null);
    }
    
    public Transaction(UUID sellerUUID, UUID buyerUUID, int id, short damage, int amount, double cost, long timestamp, String edata){
    	this.sellerUUID = sellerUUID;
        this.buyerUUID = buyerUUID;
        this.item = new ItemStack(id,amount, damage);
        if(edata != null) ItemDB.addEnchantments(item, ItemDB.parseEnchantData(edata));
        this.cost = cost;
        this.timestamp = timestamp;
    }

	public static List<Transaction> listTransactions(ResultSet result){
        List<Transaction> ret = new ArrayList<Transaction>();
        try {
            while(result.next()){
                Transaction t = new Transaction(UUID.fromString(result.getString(DatabaseManager.VENDOR_UUID)), UUID.fromString(result.getString(DatabaseManager.BUYER_UUID)), result.getInt(DatabaseManager.ITEM_ID), result.getShort(DatabaseManager.ITEM_DATA), result.getInt(DatabaseManager.QUANTITY), result.getDouble(DatabaseManager.COST), result.getLong(DatabaseManager.TIMESTAMP), result.getString(DatabaseManager.ITEM_EDATA));
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

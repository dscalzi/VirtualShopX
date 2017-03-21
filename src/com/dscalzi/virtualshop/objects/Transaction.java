/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.objects;

import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshop.managers.UUIDManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class Transaction
{
	private UUIDManager uuidm = UUIDManager.getInstance();
	
    private UUID sellerUUID;
    private UUID buyerUUID;
    private ItemStack item;
    private double cost;

    public Transaction(UUID sellerUUID, UUID buyerUUID, int id, int damage, int amount, double cost){
        this.sellerUUID = sellerUUID;
        this.buyerUUID = buyerUUID;
        this.item = new ItemStack(id,amount,(short)damage);
        this.cost = cost;
    }

	public static List<Transaction> listTransactions(ResultSet result){
        List<Transaction> ret = new ArrayList<Transaction>();
        try {
            while(result.next())
            {
                Transaction t = new Transaction(UUID.fromString(result.getString("seller_uuid")), UUID.fromString(result.getString("buyer_uuid")), result.getInt("item"), result.getInt("damage"), result.getInt("amount"), result.getDouble("cost"));
                ret.add(t);
            }
        } catch (SQLException e) {
        }
        return ret;
    }

	public String getSeller() { 
		Optional<String> name = uuidm.getPlayerName(getSellerUUID());
		return name.isPresent() ? name.get() : getSellerUUID().toString();
	}
	
	public UUID getSellerUUID() {	return sellerUUID; }
	
	public String getBuyer() { 
		Optional<String> name = uuidm.getPlayerName(getBuyerUUID());
		return name.isPresent() ? name.get() : getBuyerUUID().toString(); 
	}

	public UUID getBuyerUUID() { return buyerUUID; }

	public ItemStack getItem() { return item; }

	public double getCost() { return cost; }

}

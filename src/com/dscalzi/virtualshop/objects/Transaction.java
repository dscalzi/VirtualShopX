package com.dscalzi.virtualshop.objects;

import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshop.managers.UUIDManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class Transaction
{
    private String seller;
    private String buyer;
    private ItemStack item;
    private double cost;

    public Transaction(String seller, String buyer, int id, int damage, int amount, double cost){
        this.seller = seller;
        this.buyer = buyer;
        this.item = new ItemStack(id,amount,(short)damage);
        this.cost = cost;
    }

	public static List<Transaction> listTransactions(ResultSet result){
		UUIDManager uuidm = UUIDManager.getInstance();
        List<Transaction> ret = new ArrayList<Transaction>();
        try {
            while(result.next())
            {
                Transaction t = new Transaction(uuidm.playerFromUUID(UUID.fromString(result.getString("seller_uuid"))).getName(), uuidm.playerFromUUID(UUID.fromString(result.getString("buyer_uuid"))).getName(),result.getInt("item"), result.getInt("damage"),result.getInt("amount"),result.getDouble("cost"));
                ret.add(t);
                System.out.println(t.getBuyer());
            }
        } catch (SQLException e) {
        }
        return ret;
    }

	public String getSeller() {	return seller; }

	public String getBuyer() { return buyer; }

	public ItemStack getItem() { return item; }

	public double getCost() { return cost; }

}

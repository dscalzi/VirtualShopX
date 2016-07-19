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
                System.out.println(t.getBuyer());
            }
        } catch (SQLException e) {
        }
        return ret;
    }

	public String getSeller() { return uuidm.getPlayerName(getSellerUUID()).get(); }
	
	public UUID getSellerUUID() {	return sellerUUID; }
	
	public String getBuyer() { return uuidm.getPlayerName(getBuyerUUID()).get(); }

	public UUID getBuyerUUID() { return buyerUUID; }

	public ItemStack getItem() { return item; }

	public double getCost() { return cost; }

}

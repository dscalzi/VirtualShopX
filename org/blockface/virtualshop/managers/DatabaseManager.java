package org.blockface.virtualshop.managers;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.objects.Offer;
import org.blockface.virtualshop.objects.Transaction;
import org.blockface.virtualshop.persistance.Database;
import org.blockface.virtualshop.persistance.MySQLDB;
import org.blockface.virtualshop.persistance.SQLiteDB;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@SuppressWarnings("deprecation")
public class DatabaseManager
{
    private static Database database;

    public static void initialize()
    {
        if(ConfigManager.usingMySQL()) loadMySQL();
        else loadSQLite();
    }

    private static void loadSQLite() {
        database = new SQLiteDB();
        try {
            database.load();
        } catch (Exception e) {
            Chatty.logInfo("Fatal error.");
        }
    }

    private static void loadMySQL() {
        database = new MySQLDB();
        try {
            database.load();
        } catch (Exception e) {
            loadSQLite();
        }
    }

    public static void close() {
        database.unload();
    }

    public static void addOffer(Offer offer){
			String query = "insert into stock(seller,item,amount,price,damage) values('" +offer.seller +"',"+ offer.item.getType().getId() + ","+offer.item.getAmount() +","+offer.price+"," + offer.item.getDurability()+")";
			database.query(query);
	}
    
    public static boolean isPlayerInToggles(String merchant){
    	String query = "select * from toggles where merchant='" + merchant + "'";
    	ResultSet result = database.query(query);
    	try {
    		return result.getString("merchant").equalsIgnoreCase(merchant);
		}catch (SQLException e) {
			return false;
		}
    }
    
    public static void addPlayerToToggles(String merchant){
    	String query = "insert into toggles(merchant,buyconfirm,sellconfirm) values('" + merchant + "',1,1)";
    	database.query(query);
    }
    
    public static void updateSellToggle(String merchant, boolean value){
    	if(!isPlayerInToggles(merchant))
    		addPlayerToToggles(merchant);
    	int dataval = 0;
    	if(value)
    		dataval = 1;
    	String query = "update toggles set sellconfirm=" + dataval + " where merchant='" + merchant + "'";
		database.query(query);
    }
    
    public static void updateBuyToggle(String merchant, boolean value){
    	if(!isPlayerInToggles(merchant))
    		addPlayerToToggles(merchant);
    	int dataval = 0;
    	if(value)
    		dataval = 1;
    	String query = "update toggles set buyconfirm=" + dataval + " where merchant='" + merchant + "'";
		database.query(query);
    }
    
    public static boolean getSellToggle(String merchant){
    	if(!isPlayerInToggles(merchant))
    		addPlayerToToggles(merchant);
    	String query = "select * from toggles where merchant='" + merchant + "'";
    	ResultSet result = database.query(query);
    	try {
			return result.getBoolean("sellconfirm");
		} catch (SQLException e) {
			return false;
		}
    }
    
    public static boolean getBuyToggle(String merchant){
    	if(!isPlayerInToggles(merchant))
    		addPlayerToToggles(merchant);
    	String query = "select * from toggles where merchant='" + merchant + "'";
    	ResultSet result = database.query(query);
    	try { 
			return result.getBoolean("buyconfirm");
		} catch (SQLException e) {
			return false;
		}
    }
    
    public static List<Offer> getAllOffers(){
    	String query = "select * from stock order by price asc";
    	return Offer.listOffers(database.query(query));
    }

    public static List<Offer> getItemOffers(ItemStack item)
	{
		String query = "select * from stock where item=" + item.getTypeId()+ " and damage=" + item.getDurability() + " order by price asc";
		return Offer.listOffers(database.query(query));
	}

    public static List<Offer> getSellerOffers(String player, ItemStack item)
	{
		String query = "select * from stock where seller = '" + player + "' and item =" + item.getTypeId() + " and damage=" + item.getDurability();
		return Offer.listOffers(database.query(query));
	}

    public static void removeSellerOffers(Player player, ItemStack item)
	{
		String query = "delete from stock where seller = '" + player.getName() + "' and item =" + item.getTypeId() + " and damage = " + item.getDurability();
		database.query(query);
	}

    public static void deleteItem(int id)
	{
		String query = "delete from stock where id="+id;
		database.query(query);
	}

    public static void updateQuantity(int id, int quantity)
	{
		String query = "update stock set amount="+quantity+" where id=" + id;
		database.query(query);
	}

    public static void updatePrice(int id, double price){
    	String query = "update stock set price="+price+" where id=" + id;
    	database.query(query);
    }
    
    public static void logTransaction(Transaction transaction)
	{
		String query = "insert into transactions(seller,buyer,item,amount,cost,damage) values('" +transaction.seller +"','"+ transaction.buyer + "'," + transaction.item.getTypeId() + ","+ transaction.item.getAmount() +","+transaction.cost+","+transaction.item.getDurability()+")";
		database.query(query);
	}

    public static List<Offer> getBestPrices()
    {
        String query = "select f.* from (select item,min(price) as minprice from stock group by item) as x inner join stock as f on f.item = x.item and f.price = x.minprice";
        return Offer.listOffers(database.query(query));
    }

    public static List<Offer> searchBySeller(String seller)
    {
		return Offer.listOffers(database.query("select * from stock where seller like '%" + seller +  "%'"));
    }

    public static List<Transaction> getTransactions()
	{
		return Transaction.listTransactions(database.query("select * from transactions order by id desc"));
	}

	public static List<Transaction> getTransactions(String search)
	{
		return Transaction.listTransactions(database.query("select * from transactions where seller like '%" + search +"%' OR buyer like '%" + search +"%' order by id"));
	}

    public static List<Offer> getPrices(ItemStack item)
	{
		String query = "select * from stock where item=" + item.getTypeId() + " AND damage=" + item.getDurability() + " order by price asc limit 0,10";
		return Offer.listOffers(database.query(query));
	}
}

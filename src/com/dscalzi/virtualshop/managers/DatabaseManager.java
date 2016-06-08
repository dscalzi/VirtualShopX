package com.dscalzi.virtualshop.managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.objects.Transaction;
import com.dscalzi.virtualshop.persistance.Database;
import com.dscalzi.virtualshop.persistance.MySQLDB;
import com.dscalzi.virtualshop.persistance.SQLiteDB;

public class DatabaseManager {

	private static boolean initialized;
	private static DatabaseManager instance;
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private Database database;
	private ConfigManager configM;
	private ChatManager cm;
	
	private DatabaseManager(Plugin plugin){
		this.plugin = (VirtualShop)plugin;
		this.configM = ConfigManager.getInstance();
		this.cm = ChatManager.getInstance();
		
		if(configM.usingMySQL()) loadMySQL();
        else loadSQLite();
	}
	
	private void loadSQLite() {
        this.database = new SQLiteDB();
        try {
            this.database.load();
        } catch (Exception e) {
            cm.logInfo("Fatal error.");
        }
    }

    private void loadMySQL() {
        this.database = new MySQLDB();
        try {
            this.database.load();
        } catch (Exception e) {
            loadSQLite();
        }
    }
	
	public static void initialize(Plugin plugin){
		if(!initialized){
			instance = new DatabaseManager(plugin);
			initialized = true;
		}
	}
    
	public static DatabaseManager getInstance(){
		return DatabaseManager.instance;
	}
	
    public void close() {
        this.database.unload();
    }

    public void addOffer(Offer offer){
			@SuppressWarnings("deprecation")
			String query = "insert into vshop_stock(seller,item,amount,price,damage) values('" +offer.seller +"',"+ offer.item.getType().getId() + ","+offer.item.getAmount() +","+offer.price+"," + offer.item.getDurability()+")";
			this.database.query(query);
	}
    
    public boolean isPlayerInToggles(String merchant){
    	String query = "select * from vshop_toggles where merchant='" + merchant + "'";
    	try {
    		ResultSet result = this.database.query(query);
    		if(!result.next()) {
    			return false;
    		}
    		else {
    			if(result.getString("merchant").equalsIgnoreCase(merchant)) {
    				return true;
    			}
    		}
    		return false;
		}catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
    }
    
    public void addPlayerToToggles(String merchant){
    	String query = "insert into vshop_toggles(merchant,buyconfirm,sellconfirm) values('" + merchant + "',1,1)";
    	this.database.query(query);
    }
    
    public void updateSellToggle(String merchant, boolean value){
    	if(!isPlayerInToggles(merchant))
    		addPlayerToToggles(merchant);
    	int dataval = 0;
    	if(value)
    		dataval = 1;
    	String query = "update vshop_toggles set sellconfirm=" + dataval + " where merchant='" + merchant + "'";
		this.database.query(query);
    }
    
    public void updateBuyToggle(String merchant, boolean value){
    	if(!isPlayerInToggles(merchant))
    		addPlayerToToggles(merchant);
    	int dataval = 0;
    	if(value)
    		dataval = 1;
    	String query = "update vshop_toggles set buyconfirm=" + dataval + " where merchant='" + merchant + "'";
		this.database.query(query);
    }
    
    public boolean getSellToggle(String merchant){
    	if(!isPlayerInToggles(merchant))
    		addPlayerToToggles(merchant);
    	String query = "select * from vshop_toggles where merchant='" + merchant + "'";
    	try {
    		ResultSet result = this.database.query(query);
    		result.next();    		
			return result.getBoolean("sellconfirm");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
    }
    
    public boolean getBuyToggle(String merchant){
    	if(!isPlayerInToggles(merchant))
    		addPlayerToToggles(merchant);
    	String query = "select * from vshop_toggles where merchant='" + merchant + "'";
    	try {
    		ResultSet result = this.database.query(query);
    		result.next();
			return result.getBoolean("buyconfirm");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
    }
    
    public List<Offer> getAllOffers(){
    	String query = "select * from vshop_stock order by price asc";
    	return Offer.listOffers(this.database.query(query));
    }

    public List<Offer> getItemOffers(ItemStack item) {
		@SuppressWarnings("deprecation")
		String query = "select * from vshop_stock where item=" + item.getTypeId()+ " and damage=" + item.getDurability() + " order by price asc";
		return Offer.listOffers(this.database.query(query));
	}

    public List<Offer> getSellerOffers(String player, ItemStack item) {
		@SuppressWarnings("deprecation")
		String query = "select * from vshop_stock where seller = '" + player + "' and item =" + item.getTypeId() + " and damage=" + item.getDurability();
		return Offer.listOffers(this.database.query(query));
	}

    public void removeSellerOffers(Player player, ItemStack item) {
		@SuppressWarnings("deprecation")
		String query = "delete from vshop_stock where seller = '" + player.getName() + "' and item =" + item.getTypeId() + " and damage = " + item.getDurability();
		this.database.query(query);
	}

    public void deleteItem(int id) {
		String query = "delete from vshop_stock where id="+id;
		this.database.query(query);
	}

    public void updateQuantity(int id, int quantity) {
		String query = "update vshop_stock set amount="+quantity+" where id=" + id;
		this.database.query(query);
	}

    public void updatePrice(int id, double price) {
    	String query = "update vshop_stock set price="+price+" where id=" + id;
    	this.database.query(query);
    }
    
    public void logTransaction(Transaction transaction) {
		@SuppressWarnings("deprecation")
		String query = "insert into vshop_transactions(seller,buyer,item,amount,cost,damage) values('" +transaction.seller +"','"+ transaction.buyer + "'," + transaction.item.getTypeId() + ","+ transaction.item.getAmount() +","+transaction.cost+","+transaction.item.getDurability()+")";
		this.database.query(query);
	}

    public List<Offer> getBestPrices() {
        String query = "select f.* from (select item,min(price) as minprice from vshop_stock group by item) as x inner join vshop_stock as f on f.item = x.item and f.price = x.minprice";
        ResultSet result = this.database.query(query);
		List<Offer> prices = Offer.listOffers(result);
		return prices;
    }

    public List<Offer> searchBySeller(String seller) {
    	String query = "select * from vshop_stock where seller like '%" + seller +  "%'";
    	ResultSet result = this.database.query(query);
		List<Offer> prices = Offer.listOffers(result);
		return prices;
    }

    public List<Transaction> getTransactions() {
		return Transaction.listTransactions(this.database.query("select * from vshop_transactions order by id desc"));
	}

	public List<Transaction> getTransactions(String search) {
		return Transaction.listTransactions(this.database.query("select * from vshop_transactions where seller like '%" + search +"%' OR buyer like '%" + search +"%' order by id"));
	}

    public List<Offer> getPrices(ItemStack item) {
		@SuppressWarnings("deprecation")
		String query = "select * from vshop_stock where item=" + item.getTypeId() + " AND damage=" + item.getDurability() + " order by price asc limit 0,10";
		ResultSet result = this.database.query(query);
		List<Offer> prices = Offer.listOffers(result);
		return prices;
	}
	
}

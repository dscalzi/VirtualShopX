package org.blockface.virtualshop.commands;

import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.VirtualShop;
import org.blockface.virtualshop.managers.ConfigManager;
import org.blockface.virtualshop.managers.DatabaseManager;
import org.blockface.virtualshop.objects.Offer;
import org.blockface.virtualshop.objects.Transaction;
import org.blockface.virtualshop.util.InventoryManager;
import org.blockface.virtualshop.util.ItemDb;
import org.blockface.virtualshop.util.Numbers;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Buy implements CommandExecutor{

	private VirtualShop plugin;
	private Map<String, ArrayList<Object>> confirmations; //0 keepBuying, 1 checked, 2 args, 3 time
	
	public Buy(VirtualShop plugin){
		this.plugin = plugin;
		this.confirmations = new HashMap<String, ArrayList<Object>>();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)){
			Chatty.denyConsole(sender);
			return true;
		}
		if(!sender.hasPermission("virtualshop.buy")){
            Chatty.noPermissions(sender);
            return true;
        }
		if(VirtualShop.BETA && !sender.hasPermission("virtualshop.access.beta")){
			Chatty.denyBeta(sender);
			return true;
		}
		Player player = (Player)sender;
		if((player.getGameMode() != GameMode.SURVIVAL) && (player.getGameMode() != GameMode.ADVENTURE)){
        	Chatty.invalidGamemode(sender, player.getGameMode());
        	return true;
        }
		if(args.length > 0){
			if(args[0].equalsIgnoreCase("confirm")){
				this.cmdConfirm(player);
				return true;
			}
		}
		if(args.length < 2){
			Chatty.sendError(sender, "Proper usage is /buy <amount> <item> [maxprice]");
			return true;
		}
		
		this.execute(player, args);
		return true;
	}
	
    @SuppressWarnings("deprecation")
	public void execute(Player player, String[] args){
		int amount = Numbers.parseInteger(args[0]);
		if(amount < 0)		{
			Chatty.numberFormat(player);
			return;
		}
		
		if(amount == Numbers.ALL){
			Chatty.numberFormat(player);
			return;
		}
		
        double maxprice = Double.MAX_VALUE-1;
        if(args.length > 2){
        	maxprice = Numbers.parseDouble(args[2]);
        	if(maxprice < 0){
        		Chatty.numberFormat(player);
        		return;
        	}
        }

		ItemStack item = ItemDb.get(args[1], 0);
		if(item==null)
		{
			Chatty.wrongItem(player, args[1]);
			return;
		}
        int bought = 0;
        double spent = 0;
        InventoryManager im = new InventoryManager(player);
        List<Offer> offers = DatabaseManager.getItemOffers(item);
        if(offers.size()==0) {
            Chatty.sendError(player,"There is no " + Chatty.formatItem(args[1])+ " for sale.");
            return;
        }
        
        ItemStack[] inv = player.getInventory().getContents();
        int openNum = 0;
        for(int i=0; i<inv.length; ++i){
    		if(inv[i] == null){
    			openNum += 64;
    			continue;
    		} else if(inv[i].getType() == item.getType()){
    			openNum += (64-inv[i].getAmount());
    		}
    	}
        
        boolean tooHigh = false;
        @SuppressWarnings("unused")
		boolean boughtAny = false;
        
        if(!confirmations.containsKey(player.getName())){
        	ArrayList<Object> data = new ArrayList<Object>();
    		data.add(false);
    		data.add(false);
    		data.add(args);
    		data.add(System.currentTimeMillis());
    		confirmations.put(player.getName(), data);
        }
        
        for(Offer o: offers){
            if(o.price > maxprice){
            	tooHigh = true;
            	continue;
            }
            if(o.seller.equals(player.getName())) continue;
            
            if(o.price > ConfigManager.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData())){
            	if(!((Boolean)confirmations.get(player.getName()).get(1))){
            		Chatty.sendError(player, ChatColor.RED + "Some of the items you are trying to buy are listed for more than our price limit of " + Chatty.formatPrice(ConfigManager.getMaxPrice(item.getData().getItemTypeId(), item.getData().getData())) + ChatColor.RED + ". To continue buying please type " + ChatColor.GREEN + "/buy confirm" + ChatColor.RED + ". Command expires in 10 seconds.");
            		ArrayList<Object> data = new ArrayList<Object>();
            		data.add(false);
            		data.add(false);
            		args[0] = Integer.toString(amount-bought);
            		data.add(args);
            		data.add(System.currentTimeMillis());
            		confirmations.put(player.getName(), data);
            		if(!((Boolean)confirmations.get(player.getName()).get(0)) && !((Boolean)confirmations.get(player.getName()).get(1)))
    					break;
            		if(!((Boolean)confirmations.get(player.getName()).get(0)) && ((Boolean)confirmations.get(player.getName()).get(1))){
            			break;
            		}
            	}
            }
            
            if((amount - bought) >= o.item.getAmount()){
                int canbuy = o.item.getAmount();
                double cost = o.price * canbuy;

                //Revise amounts if not enough money.
                if(!plugin.hasEnough(player.getName(), cost)){
                    canbuy = (int)(VirtualShop.econ.getBalance(player.getName()) / o.price);
                    cost = canbuy*o.price;
                    if(canbuy < 1){
						Chatty.sendError(player,"Ran out of money!");
						break;
                    }
                }
                bought += canbuy;
                spent += cost;
                VirtualShop.econ.withdrawPlayer(player.getName(), cost);
                VirtualShop.econ.depositPlayer(o.seller, cost);
                Chatty.sendSuccess(o.seller, Chatty.formatSeller(player.getName()) + " just bought " + Chatty.formatAmount(canbuy) + " " + Chatty.formatItem(args[1]) + " for " + Chatty.formatPrice(cost));
                int left = o.item.getAmount() - canbuy;
                if(left < 1) DatabaseManager.deleteItem(o.id);
                else DatabaseManager.updateQuantity(o.id, left);
                Transaction t = new Transaction(o.seller, player.getName(), o.item.getTypeId(), o.item.getDurability(), canbuy, cost);
                DatabaseManager.logTransaction(t);
                boughtAny = true;
            } else {
                int canbuy = amount - bought;
                double cost = canbuy * o.price;

                //Revise amounts if not enough money.
                if(!plugin.hasEnough(player.getName(), cost)){
                    canbuy = (int)(VirtualShop.econ.getBalance(player.getName()) / o.price);
                    cost = canbuy*o.price;
                    if(canbuy < 1){
						Chatty.sendError(player,"Ran out of money!");
						break;
                    }
                }
                bought += canbuy;
                spent += cost;
                VirtualShop.econ.withdrawPlayer(player.getName(), cost);
                VirtualShop.econ.depositPlayer(o.seller, cost);
                Chatty.sendSuccess(o.seller, Chatty.formatSeller(player.getName()) + " just bought " + Chatty.formatAmount(canbuy) + " " + Chatty.formatItem(args[1]) + " for " + Chatty.formatPrice(cost));
                int left = o.item.getAmount() - canbuy;
                DatabaseManager.updateQuantity(o.id, left);
                Transaction t = new Transaction(o.seller, player.getName(), o.item.getTypeId(), o.item.getDurability(), canbuy, cost);
                DatabaseManager.logTransaction(t);
            }
            if(bought >= amount) break;

        }

        item.setAmount(bought);
        if(openNum < bought){
        	item.setAmount(bought-openNum);
        	player.getWorld().dropItem(player.getLocation(), item);
        }
        if(bought > 0) im.addItem(item);
        if(tooHigh && bought == 0 && args.length > 2)
        	Chatty.sendError(player,"No one is selling " + Chatty.formatItem(args[1]) + " cheaper than " + Chatty.formatPrice(maxprice));
        else
        	Chatty.sendSuccess(player,"Managed to buy " + Chatty.formatAmount(bought) + " " + Chatty.formatItem(args[1]) + " for " + Chatty.formatPrice(spent));
    }
    
	public void cmdConfirm(Player player){
    	if(confirmations.containsKey(player.getName())){
    		long timeElapse = System.currentTimeMillis() - (Long)confirmations.get(player.getName()).get(3);
    		if(timeElapse > 10000){
    			Chatty.sendError(player, "Nothing to confirm!");
    			confirmations.remove(player.getName());
    			return;
    		}
    		ArrayList<Object> temp = new ArrayList<Object>();
    		temp.add(true);
    		temp.add(true);
    		temp.add(confirmations.get(player.getName()).get(2));
    		confirmations.remove(player.getName());
    		confirmations.put(player.getName(), temp);
    		Chatty.sendSuccess(player, "Confirmed, proccessing..");
    		this.execute(player, (String[])confirmations.get(player.getName()).get(2));
    		confirmations.remove(player.getName());
    	} else {
    		Chatty.sendError(player, "Nothing to confirm!");
    	}
    }
}

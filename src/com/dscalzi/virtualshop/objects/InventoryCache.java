package com.dscalzi.virtualshop.objects;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryCache {

	private Inventory inventory;
	private ItemStack item;
	private int page;
	
	public InventoryCache(Inventory inventory, ItemStack item, int page){
		this.inventory = inventory;
		this.item = item;
		this.page = page;
	}
	
	public Inventory getInventory() { return inventory; }

	public ItemStack getItem() { return item; }

	public int getPage() { return page; }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((inventory == null) ? 0 : inventory.hashCode());
		result = prime * result + ((item == null) ? 0 : item.hashCode());
		result = prime * result + page;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InventoryCache other = (InventoryCache) obj;
		if (inventory == null) {
			if (other.inventory != null)
				return false;
		} else if (!inventory.equals(other.inventory))
			return false;
		if (item == null) {
			if (other.item != null)
				return false;
		} else if (!item.equals(other.item))
			return false;
		if (page != other.page)
			return false;
		return true;
	}
	
}

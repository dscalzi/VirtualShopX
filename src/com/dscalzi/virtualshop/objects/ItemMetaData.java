package com.dscalzi.virtualshop.objects;

/**
 * Utility class to store ItemStack meta data.
 * No validation will be done in this class.
 * 
 * @author Daniel Scalzi
 *
 */
public class ItemMetaData {

	private int itemID;
	//For potions
	private short data;
	
	public ItemMetaData(Integer itemID, Short data){
		this.itemID = itemID;
		this.data = data;
	}
	
	public static ItemMetaData parseItemMetaData(String s) throws IllegalArgumentException{
		String[] parts = s.split(":");
		if(parts.length > 2) throw new IllegalArgumentException();
		try {
			if(parts.length == 1)
				return new ItemMetaData(Integer.parseInt(parts[0]), (short)0);
				return new ItemMetaData(Integer.parseInt(parts[0]), Short.parseShort(parts[1]));
		} catch (Exception e){
			throw new IllegalArgumentException();
		}
	}
	
	public int getTypeID(){ return this.itemID; }
	
	public short getData(){ return this.data; }
	
	@Override
	public String toString(){
		return getTypeID() + ":" + getData();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + data;
		result = prime * result + itemID;
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
		ItemMetaData other = (ItemMetaData) obj;
		if (data != other.data)
			return false;
		if (itemID != other.itemID)
			return false;
		return true;
	}
}

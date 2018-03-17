/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.objects;

import java.io.Serializable;

/**
 * Interface required for a confirmation data class.
 * 
 * @author Daniel Scalzi
 *
 */
public interface VsDataCache extends Serializable{
	
	public boolean equals(Object other);
	
	public long getTransactionTime();
	
	public void serialize();
	
	public void deserialize();
	
}

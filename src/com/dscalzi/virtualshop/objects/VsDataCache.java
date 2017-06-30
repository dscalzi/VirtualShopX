/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.objects;

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

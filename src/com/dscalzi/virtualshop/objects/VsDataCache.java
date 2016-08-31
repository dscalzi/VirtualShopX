package com.dscalzi.virtualshop.objects;

import java.io.Serializable;

public interface VsDataCache extends Serializable{
	
	public boolean equals(Object other);
	
	public long getTransactionTime();
	
}

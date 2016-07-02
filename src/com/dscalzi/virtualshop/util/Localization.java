package com.dscalzi.virtualshop.util;

import java.text.NumberFormat;
import java.util.Locale;

import com.dscalzi.virtualshop.VirtualShop;

public enum Localization {

	US(Locale.US),
	EU(Locale.ITALY);
	
	private Locale locale;
	
	Localization(Locale locale){
		this.locale = locale;
	}
	
	public String formatAmt(Integer amt){
		return NumberFormat.getIntegerInstance(locale).format(amt);
	}
	
	public String formatPrice(Double price){
		String symbol = VirtualShop.econ.format(0.0).replaceFirst("0", "loc").split("loc")[0];
		
		return symbol + NumberFormat.getCurrencyInstance(locale).format(price).substring(1);
	}
}

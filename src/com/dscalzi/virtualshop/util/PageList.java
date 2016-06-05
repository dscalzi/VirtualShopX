package com.dscalzi.virtualshop.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.text.BadLocationException;

public class PageList<E> {

	private List<E> itemList;
	private Map<E, Integer> itemMap;
	private int totalPages;
	
	private final int DIVISOR;
	
	public PageList(List<E> cmdList, int itemsPerPage){
		this.itemList = cmdList;
		this.itemMap = new LinkedHashMap<>();
		this.totalPages = 0;
		this.DIVISOR = itemsPerPage;
		sortPages();
	}
	
	private void sortPages(){
		int index = 0;
		for(E e : this.itemList){
			this.itemMap.put(e, (index/this.DIVISOR)+1);
			++index;
		}
		if(index%this.DIVISOR == 0)
			this.totalPages = (index/this.DIVISOR);
		else
			this.totalPages = (index/this.DIVISOR)+1;
	}
	
	public Integer getTotalPages(){
		return this.totalPages;
	}
	
	public List<E> getPage(int page) throws BadLocationException{
		
		if(page < 1 || page > totalPages){
			throw new BadLocationException("Page does not exist", page);
		}
		
		List<E> finalList = new ArrayList<>();
		Iterator<Map.Entry<E, Integer>> iterator = itemMap.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<E, Integer> e = iterator.next();
			E msg = e.getKey();
			int pg = e.getValue();
			if(pg != page)
				continue;
			finalList.add(msg);
		}
		
		return finalList;
	}
	
	
}

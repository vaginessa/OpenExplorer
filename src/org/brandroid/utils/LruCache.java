package org.brandroid.utils;

public class LruCache<K, V> extends android.support.v4.util.LruCache<K, V>
{
	public static final int CACHE_SIZE = 100;
	
	
	/**
	 *  Creates an LruCache of a predefined CACHE_SIZE 
	 */
	public LruCache() {
		super(CACHE_SIZE);
	}
	
	public boolean containsKey(K key)
	{
		return super.get(key) != null;
	}
}

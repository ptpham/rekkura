package rekkura.util;

import java.util.Collections;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.*;

public class Synchron {
	public static boolean lightSleep(long time) {
		try { Thread.sleep(time); } 
		catch (Throwable e) { return false; }
		return true;
	}
	
	public static boolean lightSleep(int time) {
		return lightSleep((long)time);
	}
	
	public static boolean lightWait(Object o) {
		try { o.wait(); } 
		catch (Throwable e) { return false; }
		return true;
	}
	
	public static boolean lightAcquire(Semaphore sema) {
		try { sema.acquire(); } 
		catch (Throwable e) { return false; }
		return true;
	}

	public static boolean lightAwait(Lock lock, Condition cond) {
		lock.lock();
		try { cond.await(); } 
		catch (Throwable e) { return false; }
		finally { lock.unlock(); }
		return true;
	}
	
	public static void signal(Lock lock, Condition cond) {
		lock.lock();
		cond.signal();
		lock.unlock();
	}
	
	public static <U, V> Multimap<U, V> newHashMultimap() {
		return Multimaps.synchronizedSetMultimap(HashMultimap.<U,V>create());
	}
	
	public static <U, V> Map<U, V> newHashmap() {
		return Collections.synchronizedMap(Maps.<U, V>newHashMap());
	}
	
	/**
	 * This does not actually created a synchronized multiset yet.
	 * Waiting on Guava to implement it because I am too lazy right now.
	 * @return
	 */
	public static <U> Multiset<U> newHashMultiset() {
		return HashMultiset.create();
	}
	
	public static <U> Vector<U> newVector() { return new Vector<U>(); }
}

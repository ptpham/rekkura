package rekkura.util;

import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;

public class Synchron {
	public static boolean lightSleep(long time) {
		try { Thread.sleep(time); } 
		catch (InterruptedException e) { return false; }
		return true;
	}
	
	public static boolean lightSleep(int time) {
		return lightSleep((long)time);
	}
	
	public static boolean lightWait(Object o) {
		try { o.wait(); } 
		catch (InterruptedException e) { return false; }
		return true;
	}
	
	public static boolean lightAcquire(Semaphore sema) {
		try { sema.acquire(); } 
		catch (InterruptedException e) { return false; }
		return true;
	}

	public static boolean lightAwait(Lock lock, Condition cond) {
		lock.lock();
		try { cond.await(); } 
		catch (InterruptedException e) { return false; }
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

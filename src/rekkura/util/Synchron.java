package rekkura.util;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

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
}

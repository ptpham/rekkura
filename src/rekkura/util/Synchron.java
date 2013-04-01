package rekkura.util;

public class Synchron {
	public static boolean lightSleep(long time) {
		try { Thread.sleep(time); } 
		catch (InterruptedException e) { return false; }
		return true;
	}
	
	public static boolean lightSleep(int time) {
		return lightSleep((long)time);
	}
	
	public static void lightWait(Object o) {
		try { o.wait(); } 
		catch (InterruptedException e) { }
	}
}

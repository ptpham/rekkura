package rekkura.util;

public abstract class Limiter {
	
	public abstract void begin();
	
	/**
	 * This method may alter the status of the condition
	 * the limiter is intended to check.
	 * @return
	 */
	public abstract boolean exceeded();
	
	public static class Operations extends Limiter {
		public volatile long max = Long.MAX_VALUE, cur;
		public volatile boolean failed;
		
		public void begin() {
			failed = false;
			cur = 0l;
		}
		
		public boolean exceeded() {
			boolean result = this.cur++ >= this.max;
			failed |= result;
			return result;
		}
	}
	
	public static class Time extends Limiter {
		public volatile long max = Long.MAX_VALUE, begin;
		public volatile boolean failed;

		public void begin() {
			begin = System.currentTimeMillis();
			failed = false;
		}
		
		public boolean exceeded() {
			boolean result = System.currentTimeMillis() - this.begin > max;
			failed |= result;
			return result;
		}
	}
	
	public static Limiter.Operations forOperations() { return forOperations(Long.MAX_VALUE); }
	public static Limiter.Operations forOperations(long max) {
		Operations result = new Operations();
		result.max = max;
		return result;
	}
	
	public static Limiter.Time forTime() { return forTime(Long.MAX_VALUE); }
	public static Limiter.Time forTime(long max) {
		Time result = new Time();
		result.max = max;
		return result;
	}
	
	public static Limiter combine(final Limiter... limits) {
		return new Limiter() {
			@Override public void begin() 
			{ for (Limiter limit : limits) limit.begin(); }

			@Override
			public boolean exceeded() {
				boolean result = false;
				for (Limiter limit : limits) result |= limit.exceeded();
				return result;
			}
		};
	}
}

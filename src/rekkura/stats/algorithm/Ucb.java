package rekkura.stats.algorithm;

import java.util.Map;

import rekkura.util.RankedCarry;

import com.google.common.collect.Maps;

public class Ucb {
	public static class Entry {
		private volatile double mean;
		private volatile int count;
		
		public synchronized void update(double value) {
			count++;
			double delta = value - mean;
			mean += delta/count;
		}
		
		public synchronized double upper(double c, double total) {
			if (count == 0) return Double.MAX_VALUE;
 			return expected() + c*Math.sqrt(2*Math.log(total)/count);
		}
		
		public synchronized double expected() { return mean; }
		
		public synchronized void clear() {
			this.count = 0;
			this.mean = 0;
		}
		
		@Override
		public String toString() {
			return "[E=" + expected() + ", N=" + count + "]";
		}
	}
	
	public static class Suggestor<U> implements Agent.Standard<U> {
		private volatile int total = 1;
		public final double c;
		public final Map<U, Entry> entries = Maps.newHashMap();
		
		public Suggestor(Iterable<U> actions, double c) {
			for (U action : actions) entries.put(action, new Entry());
			this.c = c;
		}
		
		@Override
		public synchronized U explore() {
			RankedCarry<Double, U> best = RankedCarry.createReverseNatural(-Double.MAX_VALUE, null);
			for (Map.Entry<U, Entry> entry : entries.entrySet()) {
				best.consider(entry.getValue().upper(c, total), entry.getKey());
			}
			return best.carry;
		}
		
		@Override
		public synchronized void inform(U action, double value) {
			entries.get(action).update(value);
			this.total++;
		}

		@Override
		public synchronized U play() {
			RankedCarry<Double, U> best = RankedCarry.createReverseNatural(-Double.MAX_VALUE, null);
			for (Map.Entry<U, Entry> entry : entries.entrySet()) {
				best.consider(entry.getValue().expected(), entry.getKey());
			}
			return best.carry;
		}

		public synchronized void clear() {
			for (Entry entry : entries.values()) entry.clear();
		}
	}

}

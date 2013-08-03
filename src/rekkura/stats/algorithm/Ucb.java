package rekkura.stats.algorithm;

import java.util.Map;

import rekkura.util.RankedCarry;

import com.google.common.collect.Maps;

public class Ucb {
	public static class Entry {
		private double cumulative;
		private int count;
		
		public synchronized void update(double value) {
			cumulative += value;
			count++;
		}
		
		public synchronized double upper(double c, double total) {
			if (count == 0) return Double.MAX_VALUE;
 			return expected() + c*Math.sqrt(2*Math.log(total)/count);
		}
		
		public synchronized double expected() { return cumulative/count; }
		
		public synchronized void clear() {
			this.cumulative = 0;
			this.count = 0;
		}
	}
	
	public static class Suggestor<U> {
		private int total = 1;
		public final double c;
		public final Map<U, Entry> entries = Maps.newHashMap();
		
		public Suggestor(Iterable<U> actions, double c) {
			for (U action : actions) entries.put(action, new Entry());
			this.c = c;
		}
		
		public synchronized U suggest() {
			RankedCarry<Double, U> best = RankedCarry.createReverseNatural(-Double.MAX_VALUE, null);
			for (Map.Entry<U, Entry> entry : entries.entrySet()) {
				best.consider(entry.getValue().upper(c, total), entry.getKey());
			}
			return best.getCarry();
		}
		
		public synchronized void inform(U action, double value) {
			entries.get(action).update(value);
			this.total++;
		}

		public synchronized U play() {
			RankedCarry<Double, U> best = RankedCarry.createReverseNatural(-Double.MAX_VALUE, null);
			for (Map.Entry<U, Entry> entry : entries.entrySet()) {
				best.consider(entry.getValue().expected(), entry.getKey());
			}
			return best.getCarry();
		}

		public void clear() {
			for (Entry entry : entries.values()) entry.clear();
		}
	}

}

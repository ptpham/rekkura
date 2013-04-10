package rekkura.util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * This holds utilities for iterating over the cartesian product of sets of items.
 * @author ptpham
 *
 */
public class Cartesian {
	
	public static <U> AdvancingIterator<U> asIterator(List<List<U>> candidates) {
		return new AdvancingIterator<U>(candidates);
	}
	
	public static <U> Iterable<List<U>> asIterable(final List<List<U>> candidates) {
		if (candidates.size() == 0) return Lists.newArrayList();
		return new Iterable<List<U>>() {
			@Override public Iterator<List<U>> iterator() {
				return asIterator(candidates);
			}
		};
	}
	
	public static <U> int size(List<? extends Iterable<U>> candidates) {
		if (Colut.empty(candidates)) return 0;
		
		int product = 1;
		for (Iterable<U> iterable : candidates) {
			product *= Iterables.size(iterable);
		}
		return product;
	}
	
	/**
	 * Allows skipping over an element in a given dimension.
	 * @author ptpham
	 *
	 * @param <U>
	 */
	public static class AdvancingIterator<U> implements Iterator<List<U>> {
		public final int spaceSize;
		
		private final int[] positions;
		private final List<List<U>> candidates;
		private List<U> next;

		private AdvancingIterator(List<List<U>> candidates) {
			this.candidates = candidates;
			
			int size = 1;
			for (List<U> slice : candidates) { size *= slice.size(); }
			
			this.spaceSize = size;
			this.positions = new int[this.candidates.size()];
		}
	
		@Override public boolean hasNext() {
			prepareNext();
			return this.next != null; 
		}

		@Override
		public List<U> next() {
			if (!hasNext()) throw new NoSuchElementException();
			List<U> result = next;
			next = null;
			return result;
		}
		
		private void increment() {
			for (int i = positions.length - 1; i >= 0; i--) {
				this.positions[i]++;
				if (this.positions[i] < this.candidates.get(i).size()) break;
				if (i > 0) this.positions[i] = 0;
			}
		}
		
		private void prepareNext() {
			if (next != null) return;
			
			List<U> result = Lists.newArrayList();
			for (int i = 0; i < candidates.size(); i++) {
				List<U> slice = candidates.get(i);
				if (positions[i] >= slice.size()) break;
				result.add(slice.get(positions[i]));
			}
			
			if (result.size() == this.candidates.size()) {
				this.next = result;
				increment();
			}
		}
		
		public int dimensions() { return this.candidates.size(); }
		
		/**
		 * This forces a move forward in the given dimension. If
		 * the subspace based at the given dimension has not been
		 * explored, no change will occur. Calling {@code hasNext()}
		 * counts as looking into the subspace.
		 */
		public void advance(int dim) {
			boolean zeroed = false;
			for (int i = dim + 1; i < this.positions.length; i++) {
				if (positions[i] > 0) zeroed = true;
				this.positions[i] = 0;
			}
			if (zeroed) this.positions[dim]++;
		}
	
		@Override public void remove() 
		{ throw new IllegalAccessError("Remove not allowed!"); }
	}
}

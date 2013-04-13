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
		
		private final int[] positions, sliceSizes;
		private final List<List<U>> candidates;
		private List<U> prepared;
		private boolean hasPrepared;

		private AdvancingIterator(List<List<U>> candidates) {
			this.candidates = candidates;
			
			int size = 1;
			for (List<U> slice : candidates) { size *= slice.size(); }
			
			this.spaceSize = size;
			this.positions = new int[this.candidates.size()];
			this.sliceSizes = new int[this.candidates.size()];
			for (int i = 0; i < this.candidates.size(); i++) {
				this.sliceSizes[i] = this.candidates.get(i).size();
			}
			this.prepared = Colut.newArrayListOfNulls(this.candidates.size());
		}
	
		@Override public boolean hasNext() {
			if (this.candidates.size() == 0) return false;
			prepareNext();
			return isPrepared();
		}

		private boolean isPrepared() {
			return this.hasPrepared;
		}

		@Override
		public List<U> next() {
			if (!hasNext()) throw new NoSuchElementException();
			List<U> result = Lists.newArrayList(prepared);
			this.hasPrepared = false;
			return result;
		}
		
		private void increment() {
			for (int i = positions.length - 1; i >= 0; i--) {
				this.positions[i]++;
				if (this.positions[i] < this.sliceSizes[i]) break;
				if (i > 0) this.positions[i] = 0;
			}
		}
		
		private void prepareNext() {
			if (isPrepared()) return;
			if (this.positions.length == 0) return;
			if (this.positions[0] >= this.sliceSizes[0]) return;
			
			this.hasPrepared = true;
			for (int i = 0; i < this.sliceSizes.length; i++) {
				prepared.set(i, candidates.get(i).get(positions[i]));
			}
			
			increment();
		}
		
		public int dimensions() { return this.candidates.size(); }
		
		/**
		 * This forces a move forward in the given dimension. If
		 * the subspace based at the given dimension hasPrepared not been
		 * explored, no change will occur. Calling {@code hasNext()}
		 * counts as looking into the subspace.
		 */
		public void advance(int dim) {
			boolean nonZeroes = false;
			for (int i = dim + 1; i < this.positions.length; i++) {
				if (positions[i] > 0) {
					nonZeroes = true;
					break;
				}
			}
			
			if (!nonZeroes) return;
			
			for (int i = dim + 1; i < this.positions.length; i++) {
				this.positions[i] = this.sliceSizes[i] - 1;
			}
			this.increment();
		}
	
		@Override public void remove() 
		{ throw new IllegalAccessError("Remove not allowed!"); }
	}
}

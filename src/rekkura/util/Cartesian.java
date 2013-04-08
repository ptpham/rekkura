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
		private int current;
		
		private final int spaceSize, candidateSize;
		private final List<List<U>> candidates;

		private AdvancingIterator(List<List<U>> candidates) {
			this.candidates = candidates;
			if (this.candidates.size() == 0) {
				List<U> single = Lists.newArrayList();
				single.add(null);
				this.candidates.add(single);
			}
			
			int size = 1;
			for (List<U> slice : candidates) { size *= slice.size(); }
			
			this.spaceSize = size;
			this.candidateSize = this.candidates.size();
		}
	
		@Override public boolean hasNext() { return this.current < this.spaceSize; }
	
		@Override
		public List<U> next() {
			if (!hasNext()) throw new NoSuchElementException();
			List<U> result = Lists.newArrayListWithCapacity(candidateSize);
			int descender = this.current;
			for (int i = 0; i < candidateSize; i++) {
				int sliceSize = this.candidates.get(i).size();
				int index = descender % sliceSize;
				result.add(this.candidates.get(i).get(index));
				descender /= sliceSize;
			}
			this.current++;
			return result;
		}
		
		public int dimensions() { return this.candidates.size(); }
		
		/**
		 * This forces a move forward in the given dimension. There 
		 * may be no next after an advance so hasNext should be 
		 * called to verify.
		 * @param dim
		 */
		public void advance(int dim) {
			int subspace = 1;
			for (int i = 0; i < dim; i++) { subspace *= this.candidates.get(i).size(); }
			this.current += subspace;
		}
	
		@Override public void remove() 
		{ throw new IllegalAccessError("Remove not allowed!"); }
	}
}

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
	
	public static <U> CartesianIterator<U> asIterator(List<List<U>> candidates) {
		return new CartesianIterator<U>(candidates);
	}
	
	public static <U> Iterable<List<U>> asIterable(final List<List<U>> candidates) {
		if (candidates.size() == 0) return Lists.newArrayList();
		return new Iterable<List<U>>() {
			@Override public Iterator<List<U>> iterator() {
				return asIterator(candidates);
			}
		};
	}
	
	public static <U> int size(List<Iterable<U>> candidates) {
		if (Colut.empty(candidates)) return 0;
		
		int product = 1;
		for (Iterable<U> iterable : candidates) {
			product *= Iterables.size(iterable);
		}
		return product;
	}
	
	public static class CartesianIterator<U> implements Iterator<List<U>> {
		private int current;
		
		private final int spaceSize, candidateSize;
		private final List<List<U>> candidates;

		@SuppressWarnings("unchecked")
		private CartesianIterator(List<List<U>> candidates) {
			this.candidates = candidates;
			if (this.candidates.size() == 0) this.candidates.add(Lists.<U>newArrayList((U)null));
			
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
	
		@Override public void remove() 
		{ throw new IllegalAccessError("Remove not allowed!"); }
	}
}

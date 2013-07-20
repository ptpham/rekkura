package rekkura.util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * This holds utilities for iterating over the cartesian product of sets of items.
 * @author ptpham
 *
 */
public class Cartesian {
	
	public static <U> ListListIterator<U> asIterator(List<List<U>> candidates) {
		return new ListListIterator<U>(candidates);
	}
	
	public static <U> Iterable<List<U>> asIterable(final List<List<U>> candidates) {
		if (candidates.size() == 0) return Lists.newArrayList();
		return new Iterable<List<U>>() {
			@Override public Iterator<List<U>> iterator()
			{ return asIterator(candidates); }
		};
	}

	public static <U> MultimapIterator<U> asIterator(Multimap<U,U> edges, List<U> roots, int limit) {
		return new MultimapIterator<U>(edges, roots, limit);
	}
	
	public static <U> Iterable<List<U>> asIterable(final Multimap<U,U> edges,
		final List<U> roots, final int limit) {
		if (roots.size() == 0) return Lists.newArrayList();
		return new Iterable<List<U>>() {
			@Override public Iterator<List<U>> iterator()
			{ return asIterator(edges, roots, limit); }
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
	
	public static interface AdvancingIterator<U> extends Iterator<List<U>> {
		public void advance(int dim);
	}
	
	/**
	 * Allows skipping over an element in a given dimension.
	 * @author ptpham
	 *
	 * @param <U>
	 */
	public static class ListListIterator<U> implements AdvancingIterator<U> {
		public final int spaceSize;
		
		private final int[] positions, sliceSizes;
		private final List<List<U>> candidates;
		private List<U> prepared;
		private boolean hasPrepared;

		private ListListIterator(List<List<U>> candidates) {
			this.candidates = candidates;
			
			int size = candidates.size() > 0 ? 1 : 0;
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
			if (spaceSize == 0) return false;
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
		private void increment() {
			Cartesian.increment(this.positions, this.sliceSizes);
		}
		
		/**
		 * This forces a move forward in the given dimension. If
		 * the subspace based at the given dimension hasPrepared not been
		 * explored, no change will occur. Calling {@code hasNext()}
		 * counts as looking into the subspace.
		 */
		public void advance(int dim) {
			if (!Cartesian.hasNonZerosBeyond(this.positions, dim)) return;
			Cartesian.maximizeBeyond(this.positions, this.sliceSizes, dim);
			this.increment();
		}

		@Override public void remove() 
		{ throw new IllegalAccessError("Remove not allowed!"); }
	}
	
	public static class MultimapIterator<U> implements AdvancingIterator<U> {
		private final List<List<U>> elems;
		private final int[] positions, sizes;
		private final Multimap<U,U> edges;
		
		private List<U> next;

		public MultimapIterator(Multimap<U,U> edges, List<U> roots, int limit) {
			Preconditions.checkArgument(limit >= 0);
			this.elems = Lists.newArrayList();
			this.positions = new int[limit];
			this.sizes = new int[limit];
			this.edges = edges;
			
			this.elems.add(roots);
			if (limit > 0) this.sizes[0] = roots.size();
		}
		
		@Override
		public boolean hasNext() {
			prepareNext();
			return next != null;
		}

		@Override
		public List<U> next() {
			if (!hasNext()) throw new NoSuchElementException();
			List<U> result = this.next;
			next = null;
			return result;
		}
		
		private void increment() {
			int root = Cartesian.increment(this.positions, this.sizes);
			while (elems.size() > root + 1) Colut.popEnd(elems);
		}
		
		private void prepareNext() {
			if (next != null) return;
			if (this.positions.length == 0) return;
			if (this.positions[0] >= this.sizes[0]) return;
			
			while (elems.size() < this.positions.length) {
				int pos = elems.size();
				U last = Colut.end(elems).get(this.positions[pos-1]);
				List<U> next = Lists.newArrayList(this.edges.get(last));
				if (next.isEmpty()) break;
				
				this.elems.add(next);
				this.positions[pos] = 0;
				this.sizes[pos] = next.size();
			}
			
			this.next = Lists.newArrayList();
			for (int i = 0; i < elems.size(); i++) {
				this.next.add(this.elems.get(i).get(this.positions[i]));
			}
			
			increment();
		}

		@Override
		public void advance(int dim) {
			if (!Cartesian.hasNonZerosBeyond(this.positions, dim)) return;
			Cartesian.maximizeBeyond(this.positions, this.sizes, dim);
			increment();
		}
		
		@Override public void remove() 
		{ throw new IllegalAccessError("Remove not allowed!"); }
	}
	
	/**
	 * This increment will go beyond the size boundary in the
	 * zeroeth position instead of overflowing.
	 * @param pos
	 * @param sizes
	 * @return
	 */
	public static int increment(int[] pos, int[] sizes) {
		for (int i = pos.length - 1; i >= 0; i--) {
			if (++pos[i] < sizes[i]) return i;
			if (i > 0) pos[i] = 0;
		}
		return 0;
	}

	public static boolean hasNonZerosBeyond(int[] pos, int dim) {
		boolean nonZeroes = false;
		for (int i = dim + 1; i < pos.length; i++) {
			if (pos[i] > 0) {
				nonZeroes = true;
				break;
			}
		}
		return nonZeroes;
	}

	public static void maximizeBeyond(int[] pos, int[] sizes, int dim) {
		for (int i = dim + 1; i < pos.length; i++) {
			pos[i] = sizes[i] - 1;
		}
	}
}

package rekkura.util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * This holds utilities for iterating over the cartesian product of sets of items.
 * @author ptpham
 *
 */
public class Cartesian {
	
	public static <U> CartesianIterator<U> asIterator(List<Iterable<U>> candidates) {
		return new CartesianIterator<U>(candidates);
	}
	
	public static <U> Iterable<List<U>> asIterable(final List<Iterable<U>> candidates) {
		return new Iterable<List<U>>() {
			@Override public Iterator<List<U>> iterator() {
				return asIterator(candidates);
			}
		};
	}
	
	public static class CartesianIterator<U> implements Iterator<List<U>> {
		private List<U> state, next;
		private Stack<Iterator<U>> ongoing = new Stack<Iterator<U>>();
		private List<Iterable<U>> candidates;
		
		@SuppressWarnings("unchecked")
		private CartesianIterator(List<Iterable<U>> candidates) {
			for (Iterable<U> iterable : candidates) {
				Preconditions.checkArgument(iterable.iterator().hasNext(), 
						"Each candidate iterable must have at least one candidate.");
			}
	
			this.candidates = Lists.newArrayList(candidates);
			if (this.candidates.size() == 0) this.candidates.add(Lists.<U>newArrayList((U)null));
			this.state = Lists.newArrayListWithCapacity(candidates.size());
			replenish();
		}
	
		private void replenish() {
			while (this.ongoing.size() < this.candidates.size()) {
				int curSize = this.ongoing.size();
				this.ongoing.push(this.candidates.get(curSize).iterator());
			}
		}
		
		public void prepareNext() {
			if (next != null) return;
			
			// Remove expended iterators
			while (ongoing.size() > 0 && !ongoing.peek().hasNext()) {
				this.ongoing.pop();
				while (state.size() > ongoing.size()) Colut.removeEnd(state);
			}	
			int depletedSize = ongoing.size();
			if (ongoing.size() == 0) return;
			replenish();
				
			int begin = Math.min(state.size(), depletedSize - 1);
			for (int i = begin; i < this.candidates.size(); i++) {
				U u = ongoing.get(i).next();
				Colut.addAt(this.state, i, u);
			}
			
			this.next = Lists.newArrayList(this.state);
		}
	
		@Override 
		public boolean hasNext() {
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
	
		@Override public void remove() 
		{ throw new IllegalAccessError("Remove not allowed!"); }
	}
}

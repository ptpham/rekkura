package rekkura.util;

import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.base.Preconditions;

/**
 * Iterate over slices of a list.
 * @author ptpham
 *
 * @param <U>
 */
public class Sliding<U> {

	private static final int DEFAULT_WINDOW = 2;
	
	public static class Iterator<U> implements java.util.Iterator<List<U>>  { 
		private final List<U> base;
		private final int size;
	
		private boolean removeAvailable;
		private int position;
		
		public Iterator(List<U> base) { this(base, DEFAULT_WINDOW); }
		public Iterator(List<U> base, int size) {
			Preconditions.checkArgument(size > 0);
			this.base = base;
			this.size = size;
		}
	
		@Override public boolean hasNext() {
			return position + size <= this.base.size();
		}
	
		@Override public List<U> next() {
			removeAvailable = true;
			int end = position + size;
			return Colut.slice(base, position++, end);
		}
	
		/**
		 * Removes the base of the last list returned by next.
		 */
		@Override
		public void remove() {
			if (!removeAvailable) throw new NoSuchElementException();
			this.base.remove(position - 1);
			removeAvailable = false;
			position--;
		}
	}
	
	private static <U> Iterable<List<U>> iterable(final List<U> list, final int size) {
		return new Iterable<List<U>>() {
			@Override public java.util.Iterator<List<U>> iterator() { return new Iterator<U>(list, size); }
		};
	}
	
	public static <U> Iterable<List<U>> on(List<U> list) { return iterable(list, DEFAULT_WINDOW); }
	public static <U> Iterable<List<U>> on(List<U> list, int size) { return iterable(list, size); }
}

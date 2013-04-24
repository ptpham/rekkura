package rekkura.util;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

public class List2D<U> {
	private List<List<U>> values = Lists.newArrayList();
	private int width, height;
	
	public List2D(int width, int height) { resize(width, height); }
	public void resize(int width, int height) { resizeWith(width, height, null); }
	
	public void resizeWith(int width, int height, U value) {
		values = Colut.resize(values, height);
		for (int i = 0; i < height; i++) {
			List<U> current = values.get(i);
			values.set(i, Colut.resizeWith(current, width, value));
		}
		this.width = width;
		this.height = height;
	}
	
	public U get(int i, int j) { return values.get(i).get(j); }
	public void set(int i, int j, U val) { values.get(i).set(j, val); }
	
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	
	public Iterator<List<U>> rowIterator() { return values.iterator(); }
	
	public Iterable<List<U>> onRows() {
		return new Iterable<List<U>>() {
			@Override public Iterator<List<U>> iterator() { return rowIterator(); }
		};
	}

	public Iterator<U> valueIterator() {
		return new NestedIterator<List<U>, U>(this.values.iterator()) {
			@Override protected Iterator<U> prepareNext(List<U> u) { return u.iterator(); }
		};
	}
	
	public Iterable<U> onValues() {
		return new Iterable<U>() {
			@Override public Iterator<U> iterator() { return valueIterator(); }
		};
	}
	
	public Iterator<U> columnIterator(final int column) {
		return new Iterator<U>() {
			Iterator<List<U>> row = rowIterator();
			@Override public boolean hasNext() { return row.hasNext(); }
			@Override public U next() { return row.next().get(column); }
			@Override public void remove() { throw new IllegalAccessError("Remove not allowed!"); }
		};
	}
	
	public Iterable<U> onColumn(final int column) {
		return new Iterable<U>() {
			@Override public Iterator<U> iterator() { return columnIterator(column); }
		};
	}
	
	public static <U> List2D<U> create() { return new List2D<U>(0, 0); }
	public static <U> List2D<U> create(int width, int height) { return new List2D<U>(width, height); }
}

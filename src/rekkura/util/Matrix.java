package rekkura.util;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

public class Matrix<U> {
	private List<List<U>> values = Lists.newArrayList();
	private int rows, columns;
	
	public Matrix(int width, int height) { resize(width, height); }
	public void resize(int width, int height) { resizeWith(width, height, null); }
	
	public void resizeWith(int rows, int columns, U value) {
		values = Colut.resize(values, rows);
		for (int i = 0; i < rows; i++) {
			List<U> current = values.get(i);
			values.set(i, Colut.resizeWith(current, columns, value));
		}
		this.rows = rows;
		this.columns = columns;
	}
	
	public U get(int i, int j) { return values.get(i).get(j); }
	public void set(int i, int j, U val) { values.get(i).set(j, val); }
	
	public int getNumRows() { return rows; }
	public int getNumCols() { return columns; }
	
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
	
	public Iterable<U> onRow(int row) { return this.values.get(row); }
	
	public static <U> Matrix<U> create() { return new Matrix<U>(0, 0); }
	public static <U> Matrix<U> create(int rows, int columns) { return new Matrix<U>(rows, columns); }
}
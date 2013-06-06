package rekkura.util;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class DenseLattice<U> {
	private final List<U> values;
	public final ImmutableList<Integer> dims;
	public final int size;
	
	public DenseLattice(Integer dim0, Integer... others) {
		this.dims = ImmutableList.copyOf(Colut.prepend(dim0, Arrays.asList(others)));
		
		int size = 1;
		for (Integer dim : dims)  size *= dim;
		this.size = size;
		Preconditions.checkArgument(size >= 0, "DenseLattice size can not be negative!");
		
		this.values = Colut.newArrayListOfNulls(size);
	}
	
	public U get(int... vector) {
		int offset = getOffset(vector);
		return Colut.get(values, offset);
	}
	
	public void set(U value, int... vector) {
		int offset = getOffset(vector);
		Colut.set(values, offset, value);
	}
	
	@Override
	public String toString() {
		return values.toString();
	}
	
	private int getOffset(int... vector) {
		int offset = 0;
		int current = 1;
		for (int i = 0; i < dims.size(); i++) {
			int position = i < vector.length ? vector[i] : 0;
			offset += current*position;
			current *= dims.get(i);
		}
		return offset;
	}
	
	public static <U> DenseLattice<U> create(int dim0, Integer... others) {
		return new DenseLattice<U>(dim0, others);
	}
	
	public static <U> DenseLattice<U> asLattice(List<U> data) {
		DenseLattice<U> result = create(data.size());
		for (int i = 0; i < data.size(); i++) result.set(data.get(i), i);
		return result;
	}
}
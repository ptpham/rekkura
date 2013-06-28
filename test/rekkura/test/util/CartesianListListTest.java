package rekkura.test.util;

import java.util.List;

import rekkura.util.Cartesian;

import com.google.common.collect.Lists;

public class CartesianListListTest extends CartesianTest {
	
	@Override
	protected Cartesian.ListListIterator<Integer> constructIterator(
			List<Integer> base, int dims) {
		List<List<Integer>> candidates = Lists.newArrayList();
		for (int i = 0; i < dims; i++) candidates.add(base);
			
		Cartesian.ListListIterator<Integer> iterator = Cartesian.asIterator(candidates);
		return iterator;
	}
}

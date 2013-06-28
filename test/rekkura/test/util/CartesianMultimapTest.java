package rekkura.test.util;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.util.Cartesian;
import rekkura.util.Cartesian.AdvancingIterator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class CartesianMultimapTest extends CartesianTest {

	@Test
	public void uneven() {
		Multimap<Integer,Integer> edges = HashMultimap.create();
		edges.put(1, 2);
		edges.put(1, 3);
		edges.put(3, 4);
		edges.put(3, 5);
		
		List<Integer> roots = Lists.newArrayList(1);
		int seen = runAdvancingIterator(Cartesian.asIterator(edges, roots, 3));
		Assert.assertEquals(3, seen);
	}
	
	@Override
	protected AdvancingIterator<Integer> constructIterator(List<Integer> base, int dims) {
		Multimap<Integer,Integer> edges = HashMultimap.create();
		for (int i : base) { for (int j : base) { edges.put(i, j); } }
		return Cartesian.asIterator(edges, base, dims);
	}

}

package rekkura.test.util;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.util.Cartesian;

import com.google.common.collect.Lists;

public abstract class CartesianTest {
	
	@Test
	public void innerEmtpy() {
		List<Integer> base = Lists.newArrayList();
		int seen = runAdvancingIterator(constructIterator(base, 2));
		Assert.assertEquals(0, seen);
	}
	
	@Test(timeout=100)
	public void outerEmtpy() {
		List<Integer> base = Lists.newArrayList();
		int seen = runAdvancingIterator(constructIterator(base, 0));
		Assert.assertEquals(0, seen);
	}
	
	@Test
	public void completeCube() {
		List<Integer> base = Lists.newArrayList(1, 2, 3, 4, 5);
		int seen = runAdvancingIterator(constructIterator(base, 2));
		Assert.assertEquals(25, seen);
	}
	
	@Test
	public void advanceFirstDim() {
		List<Integer> base = Lists.newArrayList(1, 2, 3, 4, 5);
		Cartesian.AdvancingIterator<Integer> iterator = constructIterator(base, 2);
		
		iterator.next();
		iterator.advance(0);
		int seen = runAdvancingIterator(iterator);
		Assert.assertEquals(20, seen);
	}
	
	@Test
	public void advanceLastDim() {
		List<Integer> base = Lists.newArrayList(1, 2, 3, 4, 5);
		Cartesian.AdvancingIterator<Integer> iterator = constructIterator(base, 2);
		
		// This advance should not do anything because an advance on
		// an unexplored dimension will fail.
		iterator.advance(1);
		int seen = runAdvancingIterator(iterator);
		Assert.assertEquals(25, seen);
	}
	
	@Test
	public void advanceMany() {
		int dims = 10;
		List<Integer> base = Lists.newArrayList(1, 2);
		Cartesian.AdvancingIterator<Integer> iterator = constructIterator(base, dims);
		
		for (int i = 0; i < dims - 1; i++) {
			iterator.next();
			iterator.advance(i);
		}
		int seen = runAdvancingIterator(iterator);
		Assert.assertEquals(base.size(), seen);
	}
	
	@Test
	public void advanceIncrement() {
		int dims = 3;
		List<Integer> base = Lists.newArrayList(0, 1);
		Cartesian.AdvancingIterator<Integer> iterator = constructIterator(base, dims);
		
		for (int i = 0; i < 2; i++) {
			iterator.next();
			iterator.advance(1);
		}
		int seen = runAdvancingIterator(iterator);
		Assert.assertEquals(base.size()*base.size(), seen);
	}

	protected abstract Cartesian.AdvancingIterator<Integer>
	constructIterator(List<Integer> base, int dims);
	
	protected int runAdvancingIterator(Cartesian.AdvancingIterator<Integer> iterator) {
		int seen = 0;
		while (iterator.hasNext()) {
			iterator.next();
			seen++;
		}
		return seen;
	}
}

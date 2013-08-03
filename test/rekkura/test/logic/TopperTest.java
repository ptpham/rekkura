package rekkura.test.logic;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import rekkura.state.algorithm.Topper;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class TopperTest {
	
	@Test
	public void stronglyConnectedBasic() {
		Multimap<Integer, Integer> edges = toMultimap(new int[][]{
			{0, 1}, {1, 2}, {2, 1}, {1, 3}, {3, 1}, {6, 5}, {5, 6}, {5, 1}});
		
		Set<HashSet<Integer>> expected = Sets.newHashSet();
		expected.add(Sets.newHashSet(1, 2, 3));
		expected.add(Sets.newHashSet(5, 6));
		
		Set<Set<Integer>> actual = Sets.newHashSet(Topper.stronglyConnected(edges));
		assertEquals(expected, actual);
	}
	
	@Test
	public void stronglyConnectedRing() {
		Multimap<Integer, Integer> edges = toMultimap(new int[][]{
			{0, 1}, {1, 2}, {2, 3}, {3, 0}});
		
		Set<HashSet<Integer>> expected = Sets.newHashSet();
		expected.add(Sets.newHashSet(0, 1, 2, 3));
		
		Set<Set<Integer>> actual = Sets.newHashSet(Topper.stronglyConnected(edges));
		assertEquals(expected, actual);
	}
	
	@Test
	public void stronglyConnectedFigureEight() {
		Multimap<Integer, Integer> edges = toMultimap(new int[][]{
			{0, 1}, {1, 2}, {2, 0}, {2, 3}, {3, 4}, {4, 2}});
		
		Set<HashSet<Integer>> expected = Sets.newHashSet();
		expected.add(Sets.newHashSet(0, 1, 2, 3, 4));
		
		Set<Set<Integer>> actual = Sets.newHashSet(Topper.stronglyConnected(edges));
		assertEquals(expected, actual);
	}
	
	@Test
	public void stronglyConnectedClique() {
		Multimap<Integer, Integer> edges = HashMultimap.create();
		for (int i = 0; i < 100; i++) edges.put(i + 100, i/2);
		for (int i = 0; i < 50; i++) for (int j = 0; j < 50; j++) edges.put(i, j);
		
		List<Set<Integer>> comps = Topper.stronglyConnected(edges);
		Assert.assertEquals(1, comps.size());
		Assert.assertEquals(50, comps.get(0).size());
	}
	
	@Test
	public void stronglyConnectedComplex() {
		Multimap<Integer, Integer> edges = toMultimap(new int[][]{
			{0, 1}, {1, 2}, {1, 6}, {6, 21}, {2, 3}, 
			{2, 4}, {4, 5}, {5, 2}, {3, 1}, {4, 7}, 
			{7, 8}, {8, 9}, {9,7}, {9, 10}, {10, 11}, {11, 10},
			{10, 12}, {12, 13}, {13, 14}, {14, 15}, 
			{13, 12}, {12, 16}, {16, 12}, {13, 2}, {15, 17},
			{17, 18}, {18, 15}, {18, 19}, {19, 20}, {20, 19}});
		
		Set<HashSet<Integer>> expected = Sets.newHashSet();
		expected.add(Sets.newHashSet(17, 18, 15));
		expected.add(Sets.newHashSet(1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 16));
		expected.add(Sets.newHashSet(19, 20));
		
		Set<Set<Integer>> actual = Sets.newHashSet(Topper.stronglyConnected(edges));
		assertEquals(expected, actual);
	}
	
	
	@Test
	public void singleNodeStronglyConnected() {
		Multimap<Integer, Integer> edges = HashMultimap.create();
		
		int[][] edgesRaw = { {0, 0} };
		for (int[] edge : edgesRaw) { edges.put(edge[0], edge[1]); }
		
		List<HashSet<Integer>> expected = Lists.newArrayList();
		expected.add(Sets.newHashSet(0));

		List<Set<Integer>> actual = Topper.stronglyConnected(edges);
		assertEquals(expected, actual);
	}
	
	@Test
	public void generalTopSort() {
		// TODO: write a better test
		throw new NotImplementedException();
	}
	
	@Test
	public void reduceDirected() {
		Multimap<Integer, Integer> edges = toMultimap(new int[][]{
				{0, 1}, {1, 2}, {2, 3}, {2, 4}, {3, 5}} );
		Multimap<Integer, Integer> reduction = Topper.reduceDirected(edges);
		
		Multimap<Integer, Integer> expected = toMultimap(new int[][]{
				{0, 2}, {2, 4}, {2, 5}} );
		assertEquals(expected, reduction);
	}

	private Multimap<Integer, Integer> toMultimap(int[][] edgesRaw) {
		Multimap<Integer, Integer> edges = HashMultimap.create();
		for (int[] edge : edgesRaw) { edges.put(edge[0], edge[1]); }
		return edges;
	}
}

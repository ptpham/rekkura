package rekkura.test.logic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.logic.Topper;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class TopperTest {
	
	@Test
	@SuppressWarnings("unchecked")
	public void stronglyConnected() {
		Multimap<Integer, Integer> edges = HashMultimap.create();
		Set<Integer> roots = Sets.newHashSet(1);
		
		int[][] edgesRaw = { {0, 1}, {1, 2}, {1, 6}, {2, 3}, 
				{2, 4}, {4, 5}, {5, 2}, {3, 1}, {4, 7}, 
				{7, 8}, {8, 9}, {9,7}, {9, 10}, {10, 11}, {11, 10},
				{10, 12}, {12, 13}, {13, 14}, {14, 15}, 
				{13, 12}, {12, 16}, {16, 12}, {13, 2}, {15, 17},
				{17, 18}, {18, 15}, {18, 19}, {19, 20}, {20, 19}};
		
		for (int[] edge : edgesRaw) { edges.put(edge[0], edge[1]); }
		
		List<HashSet<Integer>> expected = Lists.newArrayList(Sets.newHashSet(17, 18, 15), 
				Sets.newHashSet(1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 16),
				Sets.newHashSet(19, 20));
		List<Set<Integer>> actual = Topper.stronglyConnected(edges, roots);
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void generalTopSort() {
		Multimap<Integer, Integer> edges = HashMultimap.create();
		
		int[][] edgesRaw = { {0, 1}, {1, 2}, {1, 6}, {2, 3}, 
				{2, 4}, {4, 5}, {5, 2}, {3, 1}, {4, 7}, 
				{7, 8}, {8, 9}, {9,7}, {9, 10}, {10, 11}, {11, 10},
				{10, 12}, {12, 13}, {13, 14}, {14, 15}, 
				{13, 12}, {12, 16}, {16, 12}, {13, 2}, {15, 17},
				{17, 18}, {18, 15}, {18, 19}, {19, 20}, {20, 19}};
		
		for (int[] edge : edgesRaw) { edges.put(edge[0], edge[1]); }
		Set<Integer> roots = Topper.findRoots(edges);

		int[][] expectedRaw = { {0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6},
				{6, 16}, {7, 7}, {8, 8}, {9, 9}, {10, 10}, {11, 11}, 
				{12, 12}, {13, 13}, {14, 15}, {15, 19}, {17, 17}, 
				{16, 14}, {19, 20}, {18, 18}, {20, 21} };
		
		Multiset<Integer> expected = HashMultiset.create();
		for (int[] raw : expectedRaw) expected.add(raw[0], raw[1]);
		
		Assert.assertEquals(expected, Topper.generalTopSort(edges, roots));
	}
}
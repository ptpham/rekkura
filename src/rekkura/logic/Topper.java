package rekkura.logic;

import java.util.*;

import rekkura.model.Dob;
import rekkura.util.OTMUtil;
import rekkura.util.UnionFind;

import com.google.common.collect.*;

/**
 * 
 * @author ptpham
 */
public class Topper {

	/**
	 * This topological sort is capable of dealing with cycles. The map
	 * passed in represents directed edges.
	 * @param edges
	 * @return
	 */
	public static <U> Multiset<U> generalTopSort(Multimap<U, U> edges, Collection<U> roots) {
		Multimap<U, U> copyEdges = HashMultimap.create(edges);
		Set<U> copyRoots = Sets.newHashSet(roots);
		return computeGeneralTopSort(copyEdges, copyRoots);
	}

	/**
	 * This version is going to modify the maps that are passed in.
	 * @param edges
	 * @param roots
	 * @param result
	 * @return
	 */
	private static <U> Multiset<U> computeGeneralTopSort(Multimap<U, U> edges, Set<U> roots) {
		Multiset<U> result = HashMultiset.create();
		Set<U> touched = Sets.newHashSet();
		
		while (edges.size() > 0 && roots.size() > 0) {
			// Peel back a well-ordered layer and append to our ordering
			int resultSize = result.elementSet().size();
			Multiset<U> peel = topSort(edges, roots);
			for (U u : peel.elementSet()) { result.add(u, resultSize + peel.count(u)); }
			
			// Construct a new set of roots and edges for the next iteration
			roots.clear();
			touched.addAll(peel);
			Iterables.addAll(roots, OTMUtil.valueIterable(edges, peel));
			
			for (U node : peel.elementSet()) edges.removeAll(node);
			roots.removeAll(touched);
		}
		
		return result;
	}
	
	/**
	 * This method will perform topological sort from the given nodes to the first
	 * wavefront of cycles. If a given "root" has incoming edges, the sort from 
	 * that node will terminate immediately (but the root will still be included
	 * in the ordering). 
	 * @param edges
	 * @param roots
	 * @return
	 */
	public static <U> Multiset<U> topSort(Multimap<U, U> edges, Collection<U> roots) {
		Multiset<U> result = HashMultiset.create();
		Stack<U> zeroEdges = new Stack<U>();
		
		// Initialize incoming counts
		Multiset<U> incoming = HashMultiset.create();
		for (Map.Entry<U, U> entry : edges.entries()) { incoming.add(entry.getValue()); }
		
		// Run topological sort
		int nextPriority = 0;
		for (U root : roots) { zeroEdges.push(root); }
		while (zeroEdges.size() > 0) {
			U next = zeroEdges.pop();
			result.add(next, nextPriority++);
			
			for (U u : edges.get(next)) { 
				incoming.remove(u); 
				if (incoming.count(u) == 0) zeroEdges.push(u);
			}
		}
		
		return result;
	}

	public static <U> List<Set<U>> stronglyConnected(Multimap<U, U> edges, Set<U> roots) {
		if (edges == null || edges.size() == 0) return Lists.newArrayList();
		
		UnionFind<U> ufind = new UnionFind<U>();
		Map<U, Integer> seen = Maps.newHashMap();
		
		for (U root : roots) stronglyConnectedFrom(root, edges, seen, ufind);
		
		List<Set<U>> result = Lists.newArrayList();
		HashMultimap<U, U> map = ufind.asBackwardMap();
		for (U key : map.keySet()) result.add(map.get(key));
		return result;
	}
	
	private static <U> int stronglyConnectedFrom(U node, Multimap<U, U> edges, 
			Map<U, Integer> seen, UnionFind<U> ufind) {
		if (seen.containsKey(node)) return seen.get(node);

		int index = seen.size();
		seen.put(node, index);
		
		int result = index;
		for (U adjacent : edges.get(node)) {
			int lowest = stronglyConnectedFrom(adjacent, edges, seen, ufind);
			if (lowest < index) {
				ufind.union(node, adjacent);
				result = Math.min(lowest, result);
			}
		}
		
		seen.put(node, Integer.MAX_VALUE);
			
		return result;
	}
	
}




















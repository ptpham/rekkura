package rekkura.logic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import rekkura.util.Colut;
import rekkura.util.OtmUtil;
import rekkura.util.UnionFind;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

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
		List<Set<U>> components = stronglyConnected(edges, roots);
		
		while (edges.size() > 0 && roots.size() > 0) {
			// Peel back a well-ordered layer and append to our ordering
			int resultSize = result.elementSet().size();
			Multiset<U> peel = topSort(edges, roots);
			for (U u : peel.elementSet()) { result.add(u, resultSize + peel.count(u)); }
			
			// Construct a new set of roots and edges for the next iteration
			// The new roots are the remaining children of the nodes we removed.
			roots.clear();
			touched.addAll(peel);
			Iterables.addAll(roots, OtmUtil.valueIterable(edges, peel));
			
			for (U node : peel.elementSet()) edges.removeAll(node);
			roots.removeAll(touched);
			
			// If a strongly connected component has been reached,
			// induce an ordering over it and append to our ordering
			Set<U> exposed = Sets.newHashSet();
			for (Set<U> component : components) {
				if (Colut.containsNone(component, roots)) continue;
				
				for (U node : component) { result.add(node, 1 + result.entrySet().size()); }
				touched.addAll(component);
				
				Iterables.addAll(exposed, OtmUtil.valueIterable(edges, component));
				exposed.removeAll(component);
				roots.removeAll(component);
			}
			
			// Compute new roots if we had to remove components
			if (components.size() > 0) roots.addAll(exposed);
		}
		
		return result;
	}
	
	/**
	 * Finds the set of nodes with no incoming edges.
	 * @param edges
	 * @return
	 */
	public static <U> Set<U> findRoots(Multimap<U, U> edges) {
		Set<U> allChildren = Sets.newHashSet(edges.values());
		Set<U> roots = Sets.newHashSet();
		for (U parent : edges.keySet()) {
			if (!allChildren.contains(parent)) roots.add(parent);
		}
		
		return roots;
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
		int nextPriority = 1;
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




















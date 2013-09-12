package rekkura.state.algorithm;

import java.util.*;

import rekkura.util.Colut;
import rekkura.util.OtmUtil;
import rekkura.util.UnionFind;

import com.google.common.collect.*;

/**
 * This class may move in the future. It contains very general
 * graph algorithms.
 * @author ptpham
 */
public class Topper {

	public static <U> List<U> toList(Multiset<U> nodes) {
		return Colut.flatten(toPartitionedList(nodes));
	}

	public static <U> List<List<U>> toPartitionedList(Multiset<U> nodes) {
		Multimap<Integer, U> indexed = OtmUtil.invertMultiset(nodes);
		List<Integer> indices = Lists.newArrayList(indexed.keySet());
		Collections.sort(indices);

		List<List<U>> result = Lists.newArrayList();
		for (Integer idx : indices) result.add(Lists.newArrayList(indexed.get(idx)));
		return result;
	}

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
		List<Set<U>> components = stronglyConnected(edges);

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
			Stack<U> nextZeros = new Stack<U>();
			while (zeroEdges.size() > 0) {
				U next = zeroEdges.pop();
				result.add(next, nextPriority);

				for (U u : edges.get(next)) { 
					incoming.remove(u); 
					if (incoming.count(u) == 0) nextZeros.push(u);
				}
			}
			zeroEdges = nextZeros;
			nextPriority++;
		}

		return result;
	}

	public static <U> List<Set<U>> connected(Multimap<U, U> edges) {
		UnionFind<U> uf = new UnionFind<U>();
		for (Map.Entry<U,U> entry : edges.entries()) uf.union(entry.getKey(), entry.getValue());
		return uf.asListOfSets();
	}

	public static <U> List<Set<U>> stronglyConnected(Multimap<U, U> edges) {
		if (edges == null || edges.size() == 0) return Lists.newArrayList();
		Multimap<U,U> copy = HashMultimap.create(edges);

		UnionFind<U> uf = new UnionFind<U>();
		while (copy.size() > 0) {
			U node = Colut.any(copy.keySet());
			stronglyConnectedFrom(copy, node, uf);
		}

		return uf.asListOfSets();
	}

	private static <U> void stronglyConnectedFrom(Multimap<U,U> edges, U root, UnionFind<U> uf) {
		Multiset<UnionFind<U>.Node> active = HashMultiset.create();
		Set<U> passed = Sets.newHashSet(), marked = Sets.newHashSet();
		Deque<U> stack = new ArrayDeque<U>();

		stack.addLast(root);
		passed.add(root);

		while (stack.size() > 0) {
			U node = stack.peekLast(), next = Colut.any(edges.get(node));
			if (next != null) {
				edges.remove(node, next);
				stack.addLast(next);

				boolean added = passed.add(next);
				boolean existing = uf.contains(next) && active.contains(uf.find(next));
				if (!added || existing) {
					int increment = 0;
					increment += marked.add(node) ? 1 : 0;
					increment += marked.add(next) ? 2 : 1;
					UnionFind<U>.Node rep = mergeSets(node, next, active, uf);
					active.add(rep, increment);
				}
			} else {
				UnionFind<U>.Node rep = uf.softFind(node);
				if (marked.contains(node)) active.remove(rep);
				passed.remove(node);
				stack.pollLast();

				if (stack.size() > 0 && uf.contains(node) && active.count(uf.find(node)) > 0) {
					mergeSets(stack.peekLast(), node, active, uf);
				}
			}
		}
	}

	private static <U> UnionFind<U>.Node mergeSets(U node, U next,
			Multiset<UnionFind<U>.Node> active, UnionFind<U> uf) {
		UnionFind<U>.Node lost = uf.union(node, next);
		UnionFind<U>.Node present = uf.find(node);
		if (lost == null) return present;

		int outstanding = active.count(lost);
		active.remove(lost, outstanding);
		active.add(present, outstanding);
		return present;
	}


	/**
	 * This returns a set of edges pointing toward the origin.
	 * @param origin
	 * @param edges
	 * @return
	 */
	public static <U> Multimap<U, U> dijkstra(U origin, Multimap<U, U> edges) {
		Multimap<U, U> result = HashMultimap.create();
		Multiset<U> counts = HashMultiset.create();

		List<U> toExplore = Lists.newArrayList();
		toExplore.add(origin);

		while (toExplore.size() > 0) {
			toExplore = dijkstraExpansion(edges, counts, toExplore, result);
		}

		return result;
	}

	private static <U> List<U> dijkstraExpansion(Multimap<U, U> edges,
			Multiset<U> counts, List<U> toExplore, Multimap<U, U> result) {
		List<U> exploreNext = Lists.newArrayList();
		for (U src : toExplore) {
			for (U dst : edges.get(src)) {
				if (dst.equals(src)) continue;

				int proposed = counts.count(src) + 1;
				int current = counts.count(dst);
				if (current == 0 || current > proposed) {
					result.removeAll(dst);
					counts.setCount(dst, proposed);
					exploreNext.add(dst);
					result.put(dst, src);
				} else if (current == proposed) {
					result.put(dst, src);
				}
			}
		}
		return exploreNext;
	}

	/**
	 * The edges passed in should be directed acyclic. 
	 * @param src
	 * @param dst
	 * @param edges
	 * @return
	 */
	public static <U> List<List<U>> getPaths(U src, U dst, Multimap<U, U> edges) {
		List<List<U>> result = Lists.newArrayList();

		// Base case -- return a list with just the destination
		if (src == dst) {
			List<U> inner = Lists.newArrayList();
			inner.add(dst);
			result.add(inner);
			return result;
		}

		for (U u : edges.get(src)) {
			List<List<U>> subpaths = getPaths(u, dst, edges);
			for (List<U> subpath : subpaths) {
				subpath.add(0, src);
				result.add(subpath);
			}
		}

		return result;
	}

	/**
	 * Constructs a new graph that only has edges from lower topological
	 * layers to higher topological layers as defined by the dijkstra's 
	 * graph from the target.
	 * @param target
	 * @param edges
	 * @return
	 */
	public static <U> Multimap<U, U> dagifyDijkstra(U target, Multimap<U, U> edges) {
		Multimap<U, U> result = HashMultimap.create();
		Multimap<U, U> dijkstra = Topper.dijkstra(target, edges);

		List<U> roots = Lists.newArrayList();
		roots.add(target);

		Multimap<U, U> dijkstraInverse = HashMultimap.create();
		Multimaps.invertFrom(dijkstra, dijkstraInverse);
		Multiset<U> ordering = Topper.topSort(dijkstraInverse, roots);

		for (Map.Entry<U, U> edge : edges.entries()) {
			U src = edge.getKey();
			U dst = edge.getValue();
			if (ordering.count(src) < ordering.count(dst)) result.put(src, dst);
		}
		return result;
	}


	/**
	 * This method indices a subgraph over the given graph such that there are
	 * no degenerate nodes (nodes with exactly one incoming and one outgoing edges).
	 * @param edges
	 * @return
	 */
	public static <U> Multimap<U, U> reduceDirected(Multimap<U, U> edges) {
		Multimap<U, U> inverted = HashMultimap.create();
		Multimaps.invertFrom(edges, inverted);

		Set<U> degen = degeneratedOuts(edges);
		degen.retainAll(degeneratedOuts(inverted));

		Multimap<U, U> result = HashMultimap.create();

		for (U parent : edges.keySet()) {
			if (degen.contains(parent)) continue;
			for (U child : edges.get(parent)) {
				while (degen.contains(child)) child = Colut.any(edges.get(child));
				result.put(parent, child);
			}
		}

		return result;
	}

	public static <U> boolean hasCycle(Multimap<U, U> edges) {
		Set<U> all = Colut.union(edges.keySet(), edges.values());
		return topSort(edges, findRoots(edges)).elementSet().size() < all.size();
	}

	public static <U> Multimap<U,U> induceSubgraph(Multimap<U,U> base, Collection<U> target) {
		Multimap<U,U> result = HashMultimap.create();
		for (U node : target) {
			for (U other : base.get(node)) {
				if (target.contains(other)) result.put(node, other);
			}
		}
		return result;
	}

	private static <U> Set<U> degeneratedOuts(Multimap<U, U> edges) {
		Set<U> result = Sets.newHashSet();
		for (U u : edges.keySet()) if (edges.get(u).size() == 1) result.add(u);
		return result;
	}
}




















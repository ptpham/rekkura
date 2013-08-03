package rekkura.util;

import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;

/**
 * A classic data structure. ^__^
 * @author ptpham
 *
 * @param <U>
 */
public class UnionFind<U> {

	public class Node {
		public final U value;
		private Node parent;
		private int depth;
		
		public Node(U value) { this.value = value; }
		@Override public String toString() { return "[Node: " + value + "]"; }
	}
	
	private Cache<U, Node> objectLookup = Cache.create(new Function<U, Node>() {
		@Override public Node apply(U u) { return new Node(u); }
	});
	
	public boolean contains(U value) {
		return objectLookup.stored.containsKey(value);
	}
	
	public Node find(U value) {
		Node node = objectLookup.get(value);
		return find(node);
	}
	
	public Node softFind(U value) {
		if (!contains(value)) return null;
		return find(value);
	}

	private Node find(Node node) {
		List<Node> seen = Lists.newArrayList();
		while (node.parent != null) {
			seen.add(node);
			node = node.parent;
		}
		
		// At this point the node variable is the root
		for (Node child : seen) child.parent = node;
		return node;
	}
	
	/**
	 * @param first
	 * @param second
	 * @return the node that is no longer the representative
	 * of any set or null if first and second were in the same set.
	 */
	public Node union(U first, U second) {
		Node top = find(first);
		Node bottom = find(second);
		if (top == bottom) return null;
		
		if (top.depth > bottom.depth) {
			Node temp = bottom;
			bottom = top;
			top = temp;
		}
		
		bottom.parent = top;
		top.depth = Math.max(top.depth, bottom.depth + 1);
		return bottom;
	}
	
	/**
	 * This method returns a map from the labels of sets to 
	 * the elements of the sets.
	 * @return
	 */
	public HashMultimap<U, U> asBackwardMap() {
		HashMultimap<U, U> result = HashMultimap.create();
		for (Node node : objectLookup.stored.values()) {
			Node parent = find(node);
			result.put(parent.value, node.value);
		}
		
		return result;
	}
	
	public List<Set<U>> asListOfSets() {
		List<Set<U>> result = Lists.newArrayList();
		HashMultimap<U,U> map = asBackwardMap();
		for (U key : map.keySet()) result.add(map.get(key));
		return result;
	}
}

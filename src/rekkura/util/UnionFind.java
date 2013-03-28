package rekkura.util;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;

public class UnionFind<U> {

	public class Node {
		public final U value;
		private Node parent;
		private int depth;
		
		public Node(U value) { this.value = value; }
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
	
	public void union(U first, U second) {
		Node top = find(first);
		Node bottom = find(second);
		if (top == bottom) return;
		
		if (top.depth > bottom.depth) {
			Node temp = bottom;
			bottom = top;
			top = temp;
		}
		
		bottom.parent = top;
		top.depth = Math.max(top.depth, bottom.depth + 1);
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
}
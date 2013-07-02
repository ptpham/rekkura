package rekkura.state.algorithm;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;


public class BackwardTraversal<N, D> {
	public interface Visitor<N, D> { Set<D> expandNode(N node); }
	
	/**
	 * This should be the directed graph that gives for each node,
	 * the nodes it depends on.
	 */
	public final Multimap<N, N> graph;
	
	/**
	 * The prover will not expand any rule that has non-zero entries here.
	 */
	public final HashMultimap<N, D> known = HashMultimap.create();
	
	public final HashMultimap<N, D> pending = HashMultimap.create();
	
	/**
	 * This multiset gives the index of the strongly connected component
	 * the rule belongs to.
	 */
	public final Map<N, Integer> indices = Maps.newHashMap();
	
	/**
	 * This maintains the strongly connected component sets. This
	 * set does not include rules that are not in a strongly connected
	 * component.
	 */
	public final List<Set<N>> components;
	
	/**
	 * This array is used to coordinate across a strongly connected
	 * component. The first node reached in the component becomes the 
	 * root of the component and continues to ask into the component
	 * as long as new dobs are being generated.
	 */
	private final boolean[] rooted;
	private final Set<N> asking = Sets.newHashSet();
	
	private final Visitor<N,D> visitor;
	
	public BackwardTraversal(Visitor<N,D> visitor, Multimap<N,N> graph) {
		this.graph = graph;
		this.visitor = visitor;
		this.components = Topper.stronglyConnected(graph);
		for (int i = 0; i < components.size(); i++) {
			Set<N> cycle = components.get(i);
			for (N rule : cycle) indices.put(rule, i);
		}
		
		rooted = new boolean[components.size()];
	}

	public void clear() {
		this.pending.clear();
		this.asking.clear();
		this.known.clear();
		
		Arrays.fill(rooted, false);
	}

	public boolean ask(N rule, Set<D> result) {
		if (known.containsKey(rule)) {
			result.addAll(known.get(rule));
			return false;
		}
		
		boolean inComponent = this.indices.containsKey(rule);
		if (inComponent) {
			int index = this.indices.get(rule);
			return expandComponentRule(rule, result, index);
		} else return expandRuleToMap(rule, result, this.known);
	}

	private boolean expandComponentRule(N rule, Set<D> result, int index) {
		boolean root = !this.rooted[index];
		if (!this.asking.add(rule)) return false;
		
		boolean expanded = false;
		if (root) expanded = expandRuleAsRoot(rule, result, index);
		else expanded = expandRuleToMap(rule, result, this.pending);
		
		this.asking.remove(rule);
		return expanded;
	}

	/**
	 * The root (the first node reached in this strongly connected
	 * component) acts as the base for a loop that continues
	 * as long as new dobs are being generated. Once everything 
	 * has been generated, the dobs in pending are move to known
	 * for the entire component.
	 * @param rule
	 * @param result
	 * @param index
	 * @return
	 */
	private boolean expandRuleAsRoot(N rule, Set<D> result, int index) {
		boolean expanded = false;

		this.rooted[index] = true;
		while (true) {
			boolean current = expandRuleToMap(rule, result, this.pending);
			expanded |= current;
			if (!current) break;
		}
		this.rooted[index] = false;
		
		for (N node : this.components.get(index)) {
			this.known.putAll(node, this.pending.get(node));
			this.pending.removeAll(node);
		}
		
		return expanded;
	}

	private boolean expandRuleToMap(N rule, Set<D> result, Multimap<N, D> map) {
		boolean expanded = false;
		for (N parent : this.graph.get(rule)) {
			expanded |= ask(parent, result);
		}

		Set<D> generated = this.visitor.expandNode(rule);
		map.putAll(rule, generated);
		expanded |= result.addAll(generated);
		return expanded;
	}
}

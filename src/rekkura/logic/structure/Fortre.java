package rekkura.logic.structure;

import java.util.*;

import rekkura.logic.algorithm.Unifier;
import rekkura.logic.model.Dob;
import rekkura.state.algorithm.Topper;
import rekkura.util.Colut;
import rekkura.util.OtmUtil;

import com.google.common.collect.*;

/**
 * (Form tree) This class is responsible for making it easy to 
 * go from a {@link Dob} to the set of dobs that unify with it.
 * @author ptpham
 *
 */
public class Fortre {
	public final Dob root;
	public final Pool pool;
	
	private SetMultimap<Dob, Dob> allChildren = HashMultimap.create();
	private SetMultimap<Dob, Dob> cognates = HashMultimap.create();

	private static final String ROOT_VAR_NAME = "[FR]";
	
	/**
	 * This constructor requires the full set of variables that
	 * will potentially be seen during the lifetime of this form tree.
	 * @param allVars
	 */
	public Fortre(Iterable<Dob> allForms, Pool pool) {
		this.root = new Dob(ROOT_VAR_NAME);
		this.pool = pool;
		this.pool.allVars.add(root);

		construct(allForms, pool);
	}
	
	private void construct(Iterable<Dob> raw, Pool pool) {
		// Find the symmetrizing components
		List<Dob> allForms = Lists.newArrayList(Sets.newHashSet(homogenizeWith(raw, root, pool)));
		Multimap<Dob, Dob> symmetricEdges = computeSymmetrizingEdges(allForms, root, pool);
		List<Set<Dob>> symmetrizingComponents = Topper.stronglyConnected(symmetricEdges);
		
		// Create the generalization forms by compressing each component
		Set<Dob> symmetrized = Sets.newHashSet(allForms);
		for (Set<Dob> component : symmetrizingComponents) {
			symmetrized.addAll(computeGeneralization(component, root, pool));
		}
		
		// Find subset relationships
		this.cognates.putAll(computeCognates(symmetrized, pool));
		Colut.set(symmetrized, this.cognates.keySet());
		symmetrized.add(root);
		
		Multimap<Dob, Dob> subsets = computeSubsetEdges(pool, symmetrized);
		this.allChildren.putAll(computeFormEdges(subsets, root));
	}

	public static Multimap<Dob, Dob> computeFormEdges(Multimap<Dob, Dob> subsets, Dob root) {
		Multimap<Dob,Dob> result = HashMultimap.create();
		Multiset<Dob> ordering = Topper.topSort(subsets, Sets.newHashSet(root));
		Multimap<Integer,Dob> partitions = OtmUtil.invertMultiset(ordering);
		int max = Collections.max(partitions.keySet());
		for (int i = 1; i < max; i++) {
			Collection<Dob> parents = partitions.get(i);
			Collection<Dob> children = partitions.get(i+1);
			for (Dob parent : parents) {
				for (Dob child : subsets.get(parent)) {
					if (children.contains(child)) result.put(parent, child);
				}
			}
		}
		return result;
	}

	public static Multimap<Dob, Dob> computeSubsetEdges(Pool pool,
			Set<Dob> symmetrized) {
		Multimap<Dob, Dob> subsets = HashMultimap.create();
		for (Dob child : symmetrized) {
			for (Dob parent : symmetrized) {
				if (parent == child) continue;
				if (Unifier.homogenousSubset(parent, child, pool)) {
					subsets.put(parent, child);
				}
			}
		}
		return subsets;
	}
	
	public static List<Dob> homogenizeWith(Iterable<Dob> dobs, Dob var, Pool pool) {
		List<Dob> result = Lists.newArrayList();
		for (Dob dob : dobs) result.add(pool.dobs.submerge(Unifier.homogenize(dob, var, pool.allVars)));
		return result;
	}

	private static SetMultimap<Dob,Dob> computeCognates(Iterable<Dob> allForms, Pool pool) {
		// Find cognates (forms that unify against each other)
		SetMultimap<Dob,Dob> result = HashMultimap.create();
		Multimap<Dob, Dob> cognateEdges = computeCognateEdges(allForms, pool.allVars);
		List<Set<Dob>> cognateComponents = Topper.stronglyConnected(cognateEdges);
		
		// Store cognates from strongly connected components
		List<Dob> filteredForms = Lists.newArrayList();
		Set<Dob> allCognates = Colut.union(cognateComponents);
		
		// Store forms that do not belong to a cognate component
		for (Dob dob : allForms) {
			if (!allCognates.contains(dob)) {
				filteredForms.add(dob);
				result.put(dob, dob);
			}
		}
		
		// Store a representative from each cognate component
		for (Set<Dob> component : cognateComponents) {
			Dob representative = Colut.any(component);
			filteredForms.add(representative);
			for (Dob other : component) {
				if (representative == other) continue;
				result.put(representative, other);
			}
		}
		return result;
	}

	public static Set<Dob> computeGeneralization(Iterable<Dob> component, Dob var, Pool pool) {
		Set<Dob> result = Sets.newHashSet(component);
		Deque<Dob> working = new ArrayDeque<Dob>();
		Iterables.addAll(working, component);
		
		while (working.size() > 0) {
			Dob first = working.pop();
			
			Set<Dob> addition = Sets.newHashSet();
			for (Dob second : result) {
				Dob generated = pool.dobs.submerge(Unifier.symmetrize(first, second, var, pool));
				if (generated != null && !result.contains(generated)) {
					working.add(generated);
					addition.add(generated);
				}
			}
			result.addAll(addition);
		}
		
		return result;
	}

	public static Multimap<Dob, Dob> computeCognateEdges(Iterable<Dob> allForms, Set<Dob> allVars) {
		Multimap<Dob, Dob> cognateEdges = HashMultimap.create();
		for (Dob first : allForms) {
			for (Dob second : allForms) {
				if (first == second) continue;
				if (Unifier.unifyVars(first, second, allVars) != null) {
					cognateEdges.put(first, second);
				}
			}
		}
		return cognateEdges;
	}

	public static Multimap<Dob, Dob> computeSymmetrizingEdges(List<Dob> allForms, Dob var, Pool pool) {		
		Multimap<Dob, Dob> symmetricEdges = HashMultimap.create();
		for (int i = 0; i < allForms.size(); i++) {
			Dob first = allForms.get(i);
			if (Colut.containsNone(first.fullIterable(), pool.allVars)) continue;
			for (int j = i + 1; j < allForms.size(); j++) {
				Dob second = allForms.get(j);
				if (first == second) continue;
				if (Unifier.symmetrize(first, second, var, pool) != null) { 
					symmetricEdges.put(first, second); 
					symmetricEdges.put(second, first);
				}
			}
		}
		return symmetricEdges;
	}
	
	public boolean contains(Dob dob) { return this.allChildren.containsKey(dob); }
	
	private class SubtreeIterator implements Iterator<Dob> {
		Stack<Dob> unexplored = new Stack<Dob>();
		public SubtreeIterator(Dob root) { unexplored.add(root); }
		@Override public boolean hasNext() { return unexplored.size() > 0; }
		@Override public void remove() { throw new IllegalAccessError("Remove not allowed!"); }

		@Override
		public Dob next() {
			if (!hasNext()) throw new NoSuchElementException();
			Dob next = unexplored.pop();
			unexplored.addAll(Fortre.this.allChildren.get(next));
			return next;
		}
	};
	
	private class CognateIterator implements Iterator<Dob> {
		Stack<Dob> unexplored = new Stack<Dob>();
		
		public CognateIterator(Iterator<Dob> existing) { 
			while (existing.hasNext()) {
				Dob next = existing.next();
				unexplored.add(next);
				unexplored.addAll(Fortre.this.cognates.get(next));
			}
		}
		
		@Override public boolean hasNext() { return unexplored.size() > 0; }
		@Override public void remove() { throw new IllegalAccessError("Remove not allowed!"); }

		@Override public Dob next() {
			if (!hasNext()) throw new NoSuchElementException();
			return unexplored.pop();
		}
	};	
	
	public Iterable<Dob> getSubtreeIterable(final Dob dob) {
		return new Iterable<Dob>() {
			@Override public Iterator<Dob> iterator() { return new SubtreeIterator(dob); }
		};
	}
	
	/**
	 * Returns the path from the root down to the last node N
	 * such that N unifies with the given dob but the siblings
	 * of N do not. Also, the dob must not unify with N.
	 * @param dob
	 * @return
	 */
	public List<Dob> getTrunk(Dob dob) {
		
		List<Dob> path = Lists.newArrayList();
		Dob cur = this.root;
		
		while (cur != null) {
			path.add(cur);
			if (!Unifier.homogenousSubset(cur, dob, pool)) break;

			Set<Dob> curChildren = this.allChildren.get(cur);
			if (Colut.empty(curChildren)) break;
			cur = downwardUnify(dob, curChildren, pool);
		}
		return path;
	}
	
	public Dob getTrunkEnd(Dob dob) { return Colut.end(getTrunk(dob)); }
	
	/**
	 * Returns an iterable that covers the subtree from
	 * the end of the trunk down to the given dob.
	 * @param dob
	 * @return
	 */
	public Iterable<Dob> getSubtree(Dob dob) {
		List<Dob> trunk = this.getTrunk(dob);
		return getSubtree(trunk);
	}
	
	public Iterable<Dob> getSubtree(List<Dob> trunk) {
		if (trunk.size() == 0) return Lists.newArrayList();
		return this.getSubtreeIterable(Colut.end(trunk));
	}
	
	public Iterable<Dob> getCognateSubtree(List<Dob> trunk) {
		return getCognateIterable(getSubtree(trunk));
	}
	
	public Iterable<Dob> getAllCognates() {
		return getCognateSubtree(Lists.newArrayList(this.root));
	}
	
	public Iterable<Dob> getSpine(Dob dob) {
		List<Dob> trunk = getTrunk(dob);
		Iterable<Dob> subtree = getSubtree(trunk);
		Colut.removeEnd(trunk);
		return Iterables.concat(trunk, subtree);
	}
	
	public Iterable<Dob> getSpine(List<Dob> trunk) {
		return Iterables.concat(trunk, getSubtree(trunk));
	}

	public Iterable<Dob> getCognateSpine(Dob dob) {
		return getCognateSpine(getTrunk(dob));
	}
	
	public Iterable<Dob> getCognateSpine(List<Dob> trunk) {
		return getCognateIterable(getSpine(trunk));
	}
	
	public boolean isVacuousTrunk(List<Dob> trunk) {
		return trunk == null || trunk.size() < 1;
	}
	
	protected Iterable<Dob> getCognateIterable(final Iterable<Dob> dobs) {
		return new Iterable<Dob>() {
			@Override public Iterator<Dob> iterator() {
				return new CognateIterator(dobs.iterator());
			}
		};
	}
	
	/**
	 * Tries to unify the children with the dob. 
	 * If there is exactly one unification, the child is returned, 
	 * else null is returned.
	 * @param dob
	 * @param children
	 * @return
	 */
	public static Dob downwardUnify(Dob dob, Collection<Dob> children, Pool pool) {
		Dob result = null;
		for (Dob child : children) {
			if (Unifier.homogenousSubset(child, dob, pool)) {
				if (result == null) result = child;
				else return null;
			}
		}
		
		return result;
	}
	
	protected void debugPrint() {
		for (Dob dob : allChildren.keySet()) {
			System.out.println(dob);
			for (Dob child : allChildren.get(dob)) {
				System.out.println("\t" + child);
			}
			System.out.println();
		}
	}
}

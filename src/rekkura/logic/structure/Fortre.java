package rekkura.logic.structure;

import java.util.*;

import rekkura.logic.algorithm.Unifier;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Vars;
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

	private static final String ROOT_VAR_NAME = "[ROOT]";
	
	/**
	 * This constructor requires the full set of variables that
	 * will potentially be seen during the lifetime of this form tree.
	 * @param allVars
	 */
	public Fortre(Iterable<Dob> allForms, Pool pool) {
		this.root = new Dob(ROOT_VAR_NAME);
		this.pool = pool;
		this.pool.allVars.add(root);

		construct(Lists.newArrayList(allForms), pool);
	}
	
	private void construct(List<Dob> allForms, Pool pool) {
		// Find cognates (forms that unify against each other)
		Multimap<Dob, Dob> cognateEdges = computeCognateEdges(allForms, pool.allVars);
		List<Set<Dob>> cognateComponents = Topper.stronglyConnected(cognateEdges);
		
		// Store cognates from strongly connected components
		List<Dob> filteredForms = Lists.newArrayList();
		Set<Dob> allCognates = Colut.union(cognateComponents);
		
		for (Dob dob : allForms) if (!allCognates.contains(dob)) filteredForms.add(dob);
		for (Set<Dob> component : cognateComponents) {
			Dob representative = Colut.any(component);
			filteredForms.add(representative);
			for (Dob other : component) {
				if (representative == other) continue;
				this.cognates.put(representative, other);
			}
		}
		
		// Find the symmetrizing components
		Set<Dob> allVars = pool.allVars;
		Multimap<Dob, Dob> symmetricEdges = computeSymmetrizingEdges(filteredForms, allVars, pool);
		List<Set<Dob>> symmetrizingComponents = Topper.stronglyConnected(symmetricEdges);
		
		// Create the generalization forms by compressing each component
		Set<Dob> symmetrized = Sets.newHashSet(filteredForms);
		for (Set<Dob> component : symmetrizingComponents) {
			if (component.size() < 2) continue;
			Dob generalization = computeGeneralization(Colut.any(component), symmetricEdges, pool.context);

			symmetrized.removeAll(component);
			symmetrized.add(pool.dobs.submerge(generalization));
		}
		
		// Find subset relationships
		symmetrized.add(root);
		Multimap<Dob, Dob> subsets = HashMultimap.create();
		for (Dob child : symmetrized) {
			for (Dob parent : symmetrized) {
				if (parent == child) continue;
				if (Unifier.unifyVars(parent, child, allVars) != null) {
					subsets.put(parent, child);
				}
			}
		}
		
		// Top sort and extract final tree
		Multiset<Dob> ordering = Topper.topSort(subsets, Sets.newHashSet(root));
		Multimap<Integer,Dob> partitions = OtmUtil.invertMultiset(ordering);
		int max = Collections.max(partitions.keySet());
		for (int i = 1; i < max; i++) {
			Collection<Dob> parents = partitions.get(i);
			Collection<Dob> children = partitions.get(i+1);
			for (Dob parent : parents) {
				for (Dob child : subsets.get(parent)) {
					if (children.contains(child)) allChildren.put(parent, child);
				}
			}
		}
	}

	public static Dob computeGeneralization(Dob root, 
			Multimap<Dob, Dob> edges, Vars.Context context) {
		Dob generalization = root;
		Stack<Dob> working = new Stack<Dob>();
		Set<Dob> seen = Sets.newHashSet(generalization);
		working.addAll(edges.get(generalization));
		
		while (working.size() > 0) {
			Dob next = working.pop();
			if (seen.contains(next)) continue;
			seen.add(next);
			
			generalization = Unifier.symmetrizeBothSides(generalization, next, context);
			working.addAll(edges.get(next));
		}
		
		return generalization;
	}

	public static Multimap<Dob, Dob> computeCognateEdges(List<Dob> allForms, Set<Dob> allVars) {
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

	public static Multimap<Dob, Dob> computeSymmetrizingEdges(List<Dob> allForms, Set<Dob> allVars, Pool pool) {
		Vars.Context context = Vars.copy("tmp", pool.context);
		
		Multimap<Dob, Dob> symmetricEdges = HashMultimap.create();
		for (Dob first : allForms) {
			if (Colut.containsNone(first.fullIterable(), allVars)) continue;
			for (Dob second : allForms) {
				if (first == second) continue;
				if (Unifier.isSymmetricPair(first, second, context)) { 
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
		
		Set<Dob> allVars = pool.allVars;
		while (cur != null) {
			path.add(cur);
			if (Unifier.unifyVars(dob, cur, allVars) != null) break;

			Set<Dob> curChildren = this.allChildren.get(cur);
			if (Colut.empty(curChildren)) break;
			cur = downwardUnify(dob, curChildren, allVars);
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
	
	public Iterable<Dob> getCognateIterable(final Iterable<Dob> dobs) {
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
	public static Dob downwardUnify(Dob dob, Collection<Dob> children, Set<Dob> vars) {
		Dob result = null;
		for (Dob child : children) {
			if (Unifier.unifyVars(child, dob, vars) != null) {
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

package rekkura.logic;

import java.util.*;

import rekkura.model.Dob;
import rekkura.model.Vars;
import rekkura.util.Colut;

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
		for (Set<Dob> component : cognateComponents) {
			Dob representative = Colut.any(component);
			for (Dob other : component) {
				if (representative == other) continue;
				this.cognates.put(representative, other);
				allForms.remove(other);
			}
		}
		
		// Find the symmetrizing components
		Set<Dob> allVars = pool.allVars;
		Multimap<Dob, Dob> symmetricEdges = computeSymmetrizingEdges(allForms, allVars, pool);
		List<Set<Dob>> symmetrizingComponents = Topper.stronglyConnected(symmetricEdges);
		
		// Create the generalization forms by compressing each component
		List<Dob> symmetrized = Lists.newArrayList(allForms);
		for (Set<Dob> component : symmetrizingComponents) {
			if (component.size() < 2) continue;
			Dob generalization = computeGeneralization(component, symmetricEdges, pool.context);

			generalization = pool.dobs.submerge(generalization);
			// Make sure the generalization is not a cognate of 
			// something that we already have.
			boolean cognate = false;
			for (Dob form : allForms) { cognate |= Unifier.equivalent(form, generalization, allVars); }
			if (!cognate) symmetrized.add(generalization);
		}
		
		// Find subset relationships
		for (Dob child : symmetrized) {
			List<Dob> parents = Lists.newArrayList();
			
			for (Dob parent : symmetrized) {
				if (parent == child) continue;
				if (Unifier.unifyVars(parent, child, allVars) != null) {
					parents.add(parent);
				}
			}
			
			if (parents.size() == 1) {
				this.allChildren.put(parents.get(0), child);
			} else if (parents.size() == 0) {
				this.allChildren.put(root, child);
			}
		}
	}

	public static Dob computeGeneralization(Collection<Dob> component, 
			Multimap<Dob, Dob> edges, Vars.Context context) {
		
		// Copy things to avoid modifying them
		component = Sets.newHashSet(component);
		edges = HashMultimap.create(edges);
		
		Stack<Dob> remaining = new Stack<Dob>();
		Dob generalization = Colut.popAny(component);
		remaining.addAll(edges.get(generalization));
		
		while (remaining.size() > 0) {
			Dob next = remaining.pop();
			generalization = Unifier.symmetrizeBothSides(generalization, next, context);
			
			Collection<Dob> adjacent = edges.get(next);
			remaining.addAll(adjacent);
			component.removeAll(adjacent);
			edges.removeAll(next);
		}
		
		if (component.size() > 0) return null;
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

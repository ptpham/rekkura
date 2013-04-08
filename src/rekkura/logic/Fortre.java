package rekkura.logic;

import java.util.*;

import rekkura.model.Dob;
import rekkura.util.CachingSupplier;
import rekkura.util.Colut;

import com.google.common.collect.*;

/**
 * (Form tree) This class is responsible for making it easy to 
 * go from a dob to the set of dobs that unify with it.
 * Don't add the same dob twice, guys.
 * @author ptpham
 *
 */
public class Fortre {
	public final Dob root;
	public final Pool pool;
	
	public final Set<Dob> allVars;
	public final Set<Dob> generated;
	
	private SetMultimap<Dob, Dob> allChildren = HashMultimap.create();
	private SetMultimap<Dob, Dob> cognates = HashMultimap.create();
	private CachingSupplier<Dob> vargen = new Dob.PrefixedGenerator("FTV");

	private static final String ROOT_VAR_NAME = "[ROOT]";
	
	/**
	 * This constructor requires the full set of variables that
	 * will potentially be seen during the lifetime of this form tree.
	 * @param allVars
	 */
	public Fortre(Collection<Dob> allVars, Iterable<Dob> allForms, Pool pool) {
		this.allVars = Sets.newHashSet(allVars);
		this.generated = Sets.newHashSet();
		this.root = new Dob(ROOT_VAR_NAME);
		this.allVars.add(root);
		this.pool = pool;

		construct(Lists.newArrayList(allForms), pool);
	}
	
	private void construct(List<Dob> allForms, Pool pool) {
		
		// Find cognates (forms that unify against each other)
		Multimap<Dob, Dob> cognateEdges = HashMultimap.create();
		for (Dob first : allForms) {
			for (Dob second : allForms) {
				if (first == second) continue;
				if (Unifier.unifyVars(first, second, allVars) != null) {
					cognateEdges.put(first, second);
				}
			}
		}
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
		Multimap<Dob, Dob> symmetricEdges = HashMultimap.create();
		for (Dob first : allForms) {
			for (Dob second : allForms) {
				if (first == second) continue;
				if (Unifier.isSymmetricPair(first, second, allVars)) { 
					symmetricEdges.put(first, second); 
					symmetricEdges.put(second, first);
				}
			}
		}
		
		List<Set<Dob>> symmetrizingComponents = Topper.stronglyConnected(symmetricEdges);
		
		// Create the generalization forms by compressing each component
		List<Dob> symmetrized = Lists.newArrayList(allForms);
		for (Set<Dob> component : symmetrizingComponents) {
			if (component.size() == 0) continue;
			Stack<Dob> remaining = new Stack<Dob>();
			Dob generalization = Colut.popAny(component);
			remaining.addAll(symmetricEdges.get(generalization));
			
			while (remaining.size() > 0) {
				Dob next = remaining.pop();
				generalization = Unifier.computeSymmetricGeneralization(generalization, next, allVars, vargen);
				vargen.deposit(allVars);
				remaining.addAll(symmetricEdges.get(next));
				symmetricEdges.removeAll(next);
			}
			
			generalization = pool.submerge(generalization);
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
		if (trunk.size() <= 1) return Lists.newArrayList();
		return this.getSubtreeIterable(Colut.end(trunk));
	}
	
	public Iterable<Dob> getCognateSubtree(List<Dob> trunk) {
		return getCognateIterable(getSubtree(trunk));
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

	public Iterable<? extends Dob> getCognateSplay(Dob dob) {
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

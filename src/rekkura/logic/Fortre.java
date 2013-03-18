package rekkura.logic;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import rekkura.model.Dob;
import rekkura.util.Colut;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * (Form tree) This class is responsible for making it easy to 
 * go from a dob to the set of dobs that unify with it.
 * Don't add the same dob twice, guys.
 * @author ptpham
 *
 */
public class Fortre {
	public final Dob root;
	public final Unifier unifier = new Unifier();

	private final Set<Dob> allVars;
	
	private SetMultimap<Dob, Dob> allChildren = HashMultimap.create();
	private SetMultimap<Dob, Dob> cognates = HashMultimap.create();
	
	private static final String ROOT_VAR_NAME = "[ROOT]";
	
	/**
	 * This constructor requires the full set of variables that
	 * will potentially be seen during the lifetime of this form tree.
	 * @param allVars
	 */
	public Fortre(Collection<Dob> allVars, Iterable<Dob> allForms) {
		this.allVars = Sets.newHashSet(allVars);
		this.root = new Dob(ROOT_VAR_NAME);
		this.allVars.add(root);
		
		Set<Dob> deduped = Sets.newHashSet(allForms);
		for (Dob form : deduped) addForm(form);
		compress();
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
	 * of N do not.
	 * @param dob
	 * @return
	 */
	public List<Dob> getUnifyTrunk(Dob dob) {
		
		List<Dob> path = Lists.newArrayList();
		Dob cur = this.root;
		
		while (cur != null) {
			path.add(cur);
			
			Set<Dob> curChildren = this.allChildren.get(cur);
			if (Colut.empty(curChildren)) break;
			cur = downwardUnify(dob, curChildren);
		}
		
		return path;
	}
	
	/**
	 * Returns an iterable that covers the subtree from
	 * the end of the trunk down to the given dob.
	 * @param dob
	 * @return
	 */
	public Iterable<Dob> getUnifySubtree(Dob dob) {
		List<Dob> trunk = this.getUnifyTrunk(dob);
		return getUnifySubtree(trunk);
	}
	
	public Iterable<Dob> getUnifySubtree(List<Dob> trunk) {
		if (trunk.size() <= 1) return Lists.newArrayList();
		return this.getSubtreeIterable(Colut.end(trunk));
	}
	
	public Iterable<Dob> getCognateSubtree(List<Dob> trunk) {
		return getCognateIterable(getUnifySubtree(trunk));
	}
	
	public Iterable<Dob> getSplay(List<Dob> trunk) {
		return Iterables.concat(trunk, getUnifySubtree(trunk));
	}
	
	public Iterable<Dob> getCognateSplay(List<Dob> trunk) {
		return getCognateIterable(getSplay(trunk));
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
	 * The dob is added at the lowest level such that all of its 
	 * ancestors unify with it. If there is a non-trivial subset of
	 * its siblings that unify with it and it does not unify with any
	 * of its siblings, then the siblings will be added as children 
	 * of the new dob. 
	 * @param dob
	 */
	protected void addForm(Dob dob) {
		List<Dob> trunk = getUnifyTrunk(dob);
		Dob end = Colut.end(trunk);
		Set<Dob> endChildren = this.allChildren.get(end);

		if (Colut.nonEmpty(endChildren)) {
			Set<Dob> up = upwardUnify(dob, endChildren);
			endChildren.removeAll(up);
			this.allChildren.putAll(dob, up);
		}
		
		// Add the dob as a child of insertion location
		this.allChildren.put(end, dob);
	}
	
	/**
	 * This method will remove all nodes X such that X unifies
	 * with the parent of X and the parent of X has exactly 
	 * one child.
	 */
	protected void compress() {
		Set<Dob> candidates = Sets.newHashSet(this.allChildren.keySet());
		while (candidates.size() > 0) { candidates = compressOnce(candidates); }
	}

	private Set<Dob> compressOnce(Set<Dob> candidates) {
		Set<Dob> remaining = Sets.newHashSet();
		SetMultimap<Dob, Dob> replacement = HashMultimap.create();
		for (Dob parent : candidates) {
			Set<Dob> children = allChildren.get(parent);
			Dob child = Colut.any(children);
			
			if (children.size() == 1 &&
				unifier.unifyVars(child, parent, allVars) != null) {
				children = allChildren.get(child);
				if (children.size() == 1) remaining.add(parent);
				this.cognates.put(parent, child);
			}
			replacement.putAll(parent, children);
		}
		
		this.allChildren = replacement;
		return remaining;
	}
	
	/**
	 * Tries to unify the children with the dob. 
	 * If there is exactly one unification, the child is returned, 
	 * else null is returned.
	 * @param dob
	 * @param children
	 * @return
	 */
	private Dob downwardUnify(Dob dob, Set<Dob> children) {
		Dob result = null;
		for (Dob child : children) {
			if (unifier.unifyVars(child, dob, allVars) != null) {
				if (result == null) result = child;
				else break;
			}
		}
		return result;
	}
	
	/**
	 * Tries to unify the dob with the children.
	 * Returns the set of children with which the dob successfully unified.
	 * @param dob
	 * @param children
	 * @return
	 */
	private Set<Dob> upwardUnify(Dob dob, Set<Dob> children) {
		Set<Dob> result = Sets.newHashSet();
		for (Dob child : children) {
			if (unifier.unifyVars(dob, child, allVars) != null) {
				result.add(child);
			}
		}
		return result;
	}
}

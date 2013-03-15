package rekkura.logic;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import rekkura.model.Dob;
import rekkura.util.Colut;

import com.google.common.collect.HashMultimap;
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

	private final Unifier unifier = new Unifier();
	private final Set<Dob> allVars;
	
	private SetMultimap<Dob, Dob> allChildren = HashMultimap.create();
	
	private static final String DUMMY_VAR_NAME = "[DUMMY]";
	
	/**
	 * This constructor requires the full set of variables that
	 * will potentially be seen during the lifetime of this form tree.
	 * @param allVars
	 */
	public Fortre(Collection<Dob> allVars, Collection<Dob> allForms) {
		this.allVars = Sets.newHashSet(allVars);
		if (this.allVars.size() == 0) {
			Dob dummy = new Dob(DUMMY_VAR_NAME);
			this.allVars.add(dummy);
		}
		
		// Pick an arbitrary variable as the root
		this.root = Colut.any(this.allVars);
		
		for (Dob form : allForms) addForm(form);
		compress();
	}
	
	public boolean contains(Dob dob) { return this.allChildren.containsKey(dob); }
	
	private class SubtreeIterator implements Iterator<Dob> {
		Set<Dob> unexplored = Sets.newHashSet();
		public SubtreeIterator(Dob root) { unexplored.add(root); }
		@Override public boolean hasNext() { return Colut.nonEmpty(unexplored); }
		@Override public void remove() { throw new IllegalAccessError("Remove not allowed!"); }

		@Override
		public Dob next() {
			Dob next = Colut.popAny(unexplored);
			Colut.addAll(unexplored, Fortre.this.allChildren.get(next));
			return next;
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
		if (trunk.size() <= 1) return Lists.newArrayList();
		return this.getSubtreeIterable(Colut.end(trunk));
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
		SetMultimap<Dob, Dob> replacement = HashMultimap.create();
		for (Dob parent : allChildren.keySet()) {
			Set<Dob> children = allChildren.get(parent);
			Dob child = Colut.any(children);
			
			if (children.size() == 1 &&
				unifier.unifyVars(child, parent, allVars) != null) {
				children = allChildren.get(child);
			}
			replacement.putAll(parent, children);
		}
		
		this.allChildren = replacement;
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

package rekkura.logic;

import java.util.Map;
import java.util.Set;

import rekkura.model.Dob;
import rekkura.util.OTMUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * (Form tree) This class is responsible for making it easy to 
 * go from a dob to the set of dobs that unify with it.
 * Don't add the same dob twice, guys.
 * @author ptpham
 *
 */
public class Fortre {

	private final Unifier unifier = new Unifier();
	private final Map<Dob, Set<Dob>> children = Maps.newHashMap();
	private final Set<Dob> allVars;
	
	public final Dob root;
	
	/**
	 * This constructor requires the full set of variables that
	 * will potentially be seen during the lifetime of this form tree.
	 * @param allVars
	 */
	public Fortre(Set<Dob> allVars) {
		Preconditions.checkArgument(allVars.size() > 0, "No variables provided!");
		this.allVars = allVars;
		
		// Pick an arbitrary variable as the root
		this.root = allVars.iterator().next();
	}
	
	public boolean contains(Dob dob) { return this.children.containsKey(dob); }
	
	/**
	 * The dob is added at the lowest level such that it unifies
	 * will all of its ancestors. If there is a non-trivial subset of
	 * its siblings that unify with it and it does not unify with any
	 * of its siblings, then the siblings will be added as children 
	 * of the new dob. 
	 * @param dob
	 */
	public void addDob(Dob dob) {
		Dob cur = this.root;
		while (true) {
			Set<Dob> children = this.children.get(cur);
			if (children == null || children.size() == 0) break;
			
			Dob down = downwardUnify(dob, children);
			
			if (down != null) { cur = down; } 
			else if (down == null) {
				Set<Dob> up = upwardUnify(dob, children);
				children.removeAll(up);
				this.children.put(dob, up);
				break;
			}
		}
		
		// Add the dob as a child of insertion location
		OTMUtil.safePut(this.children, cur, dob);
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

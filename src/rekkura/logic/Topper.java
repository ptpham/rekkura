package rekkura.logic;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import rekkura.model.Dob;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 
 * @author ptpham
 */
public class Topper {

	public Unifier unifer = new Unifier();
	
	/**
	 * Computes for each target dob the set of source dobs that unify with it.
	 * @param dobs
	 * @param vars
	 * @return
	 */
	public Map<Dob, Set<Dob>> dependencies(Collection<Dob> targetDobs, 
			Collection<Dob> sourceDobs, Set<Dob> vars) {
		Map<Dob, Set<Dob>> result = Maps.newHashMapWithExpectedSize(targetDobs.size());
		
		for (Dob dob : targetDobs) { result.put(dob, Sets.<Dob>newHashSet()); }
		
		for (Dob target : targetDobs) {
			for (Dob source : sourceDobs) {
				Map<Dob, Dob> unify = unifer.unifyVars(source, target, vars);
				if (unify == null) continue;
				
				result.get(target).add(source);
			}
		}
		
		return result;
	}
	
}

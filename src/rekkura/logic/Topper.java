package rekkura.logic;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import rekkura.model.Dob;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

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
	public Multimap<Dob, Dob> dependencies(Collection<Dob> targetDobs, 
			Collection<Dob> sourceDobs, Set<Dob> vars) {
		Multimap<Dob, Dob> result = HashMultimap.create(targetDobs.size(), sourceDobs.size());
		
		for (Dob target : targetDobs) {
			for (Dob source : sourceDobs) {
				Map<Dob, Dob> unify = unifer.unifyVars(source, target, vars);
				if (unify == null) continue;
				result.put(target, source);
			}
		}
		
		return result;
	}
	
}

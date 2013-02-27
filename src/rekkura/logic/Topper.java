package rekkura.logic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.model.Dob;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
	public Map<Dob, List<Dob>> dependencies(Collection<Dob> targetDobs, 
			Collection<Dob> sourceDobs, Set<Dob> vars) {
		Map<Dob, List<Dob>> result = Maps.newHashMapWithExpectedSize(targetDobs.size());
		
		for (Dob dob : targetDobs) { result.put(dob, Lists.<Dob>newArrayList()); }
		
		for (Dob target : targetDobs) {
			for (Dob source : sourceDobs) {
				Map<Dob, Dob> unify = unifer.unifyVars(source, target, vars);
				if (unify == null) continue;
				
				result.get(target).add(source);
			}
		}
		
		return result;
	}
	
	public List<Dob> order(Map<Dob, List<Dob>> deps) {
		return null;
	}
}

package rekkura.logic;

import java.util.List;
import java.util.Map;

import rekkura.model.Dob;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Latticer {
	
	/**
	 * This extracts a map from the given grounds to submerged dobs
	 * that represent the assignments to the given variables in the
	 * given order.
	 * @param vars
	 * @param base
	 * @param grounds
	 * @param pool
	 * @return
	 */
	public static Map<Dob, Dob> represent(Iterable<Dob> grounds,
		Dob base, List<Dob> vars, Pool pool) {
		
		Map<Dob, Dob> result = Maps.newHashMap();
		
		for (Dob ground : grounds) {
			Map<Dob, Dob> unify = Unifier.unify(base, ground);
			if (unify == null) continue;
			if (!unify.keySet().containsAll(vars)) continue;
			
			List<Dob> assign = Lists.newArrayList();
			for (Dob var : vars) assign.add(unify.get(var));
			
			result.put(ground, pool.dobs.submerge(new Dob(assign)));
		}
		
		return result;
	}
}

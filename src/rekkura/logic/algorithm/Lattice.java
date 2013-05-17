package rekkura.logic.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.model.Dob;
import rekkura.logic.structure.Pool;
import rekkura.util.OtmUtil;

import com.google.common.collect.*;

public class Lattice {
	public final Request req;

	/**
	 * These are edges in the lattice space.
	 */
	public final Multimap<Dob, Dob> edges = HashMultimap.create();
	public final Map<Dob, Dob> firstInto = Maps.newHashMap();
	public final Map<Dob, Dob> secondInto = Maps.newHashMap();
	
	private Lattice(Request req) { this.req = req; }
	
	/**
	 * These dobs should be from the same variable scope.
	 * @author ptpham
	 *
	 */
	public static class Request {
		public final Dob first;
		public final Dob second;
		public final Dob base;
		
		public Request(Dob first, Dob second, Dob base) {
			this.first = first;
			this.second = second;
			this.base = base;
		}
	}
	
	public static Lattice extract(Request req,
			List<Dob> vars, Iterable<Dob> grounds, Pool pool) {
		Lattice result = new Lattice(req);
		
		Varpar vp = partition(req.first, req.second, vars);
		Map<Dob, Dob> first = represent(grounds, req.base, vp.first, pool);
		Map<Dob, Dob> second = represent(grounds, req.base, vp.second, pool);
		result.firstInto.putAll(first);
		result.secondInto.putAll(second);
		result.edges.putAll(OtmUtil.joinBase(first, second));
		return result;
	}
	
	
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
	
	public static class Varpar {
		public final List<Dob> first = Lists.newArrayList();
		public final List<Dob> second = Lists.newArrayList();
		public final List<Dob> both = Lists.newArrayList();
	}

	/**
	 * This method will return the partitions of the given variables
	 * based on their membership in first and second.
	 * @param first
	 * @param second
	 * @param pivot
	 * @param vars
	 * @return
	 */
	public static Varpar partition(Dob first, Dob second, List<Dob> vars) {
		Varpar result = new Varpar();
		
		Set<Dob> firstVars = Sets.newHashSet(first.fullIterable());
		Set<Dob> secondVars = Sets.newHashSet(second.fullIterable());
		
		for (Dob dob : vars) {
			boolean inFirst = firstVars.contains(dob);
			boolean inSecond = secondVars.contains(dob);
			
			if (inFirst && !inSecond) result.first.add(dob);
			if (inSecond && !inFirst) result.second.add(dob);
			if (inFirst && inSecond) result.both.add(dob);
		}
		
		return result;
	}
	
	
}

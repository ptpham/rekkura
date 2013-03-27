package rekkura.logic.perf;

import java.util.Map;

import rekkura.logic.Unifier;
import rekkura.model.Dob;
import rekkura.model.Logimos.DobSpace;
import rekkura.util.Cache;
import rekkura.util.OTMUtil;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class GroundScope {
	/**
	 * These hold the mappings from a body form B to grounds 
	 * that are known to successfully unify with B.
	 * Memory is O(FG) but it will only store the things that
	 * are true in any given proving cycle.
	 */
	public final Multimap<Dob, Dob> unisuccess = HashMultimap.create();
	
	/**
	 * This holds the mapping from a body form to the sets of replacements
	 * for its various children.
	 * Memory is O(FV).
	 */
	public final Cache<Dob, DobSpace> unispaces = 
		Cache.create(new Function<Dob, DobSpace>() {
			@Override public DobSpace apply(Dob dob) { return new DobSpace(dob); }
		});
	
	public void storeGround(Dob ground, Dob body) {
		unisuccess.put(body, ground);
		storeVariableReplacements(ground, body);
	}

	public void storeVariableReplacements(Dob ground, Dob body) {
		Map<Dob, Dob> unify = Unifier.unify(body, ground);
		DobSpace space = this.unispaces.get(body);
		OTMUtil.putAll(space.replacements, unify);
	}
}

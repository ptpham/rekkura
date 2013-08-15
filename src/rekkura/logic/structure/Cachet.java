package rekkura.logic.structure;

import java.util.List;
import java.util.Set;

import rekkura.logic.algorithm.Unifier;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.util.Cache;
import rekkura.util.Colut;
import rekkura.util.OtmUtil;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * It's french. <br>
 * This class caches a bunch of things that are fun to use in logical manipulation.
 * It started out as a "performance" class, but it's more of a "logical scope" now.
 * @author ptpham
 *
 */
public class Cachet {
	
	/**
	 * This maps ground dobs to their canonical dobs (the dob at 
	 * the end of the unify trunk).
	 */
	public Cache<Dob, Dob> canonicalForms = 
		Cache.create(new Function<Dob, Dob>() {
			@Override public Dob apply(Dob dob) { 
				return Colut.end(rta.fortre.getTrunk(dob));
			}
		});
	
	public Cache<Dob, List<Dob>> canonicalSpines = 
		Cache.create(new Function<Dob, List<Dob>>() {
			@Override public List<Dob> apply(Dob dob) {
				return Lists.newArrayList(rta.fortre.getSpine(dob));
			}
		});
	
	/**
	 * This caches form spines for given canonical dobs.
	 */
	public Cache<Dob, List<Dob>> spines = 
		Cache.create(new Function<Dob, List<Dob>>() {
			@Override public List<Dob> apply(Dob dob) 
			{ return canonicalSpines.get(canonicalForms.get(dob)); }
		});
	
	/**
	 * This caches the list of rules affected by each canonical form.
	 */
	public Cache<Dob, List<Rule>> canonicalRules = 
		Cache.create(new Function<Dob, List<Rule>>() {
			@Override public List<Rule> apply(Dob dob) { 
				return Lists.newArrayList(rta.getAffectedRules(dob));
			}
		});
	
	public Cache<Dob, List<Rule>> affectedRules = 
		Cache.create(new Function<Dob, List<Rule>>() {
			@Override public List<Rule> apply(Dob dob) 
			{ return canonicalRules.get(canonicalForms.get(dob)); }
		});
	
	/**
	 * These hold the mappings from a body form B to grounds 
	 * that are known to successfully unify with B.
	 * Memory is O(FG) but it will only store the things that
	 * are true in any given proving cycle.
	 */
	public final Multimap<Dob, Dob> formToGrounds = HashMultimap.create();
	
	public final Ruletta rta;

	public Cachet(Ruletta rta) { this.rta = rta; }

	public List<Dob> getUnifiableForms(Dob dob) {
		List<Dob> result = Lists.newArrayList();
		Set<Dob> vars = rta.fortre.pool.allVars;
		for (Dob node : spines.get(dob)) {
			if (Unifier.unifyVars(node, dob, vars) != null) {
				result.add(node);
			}
		}
		return result;
	}
	
    public void storeAllGround(Iterable<Dob> grounds) {
        for (Dob ground : grounds) storeGround(ground);
    }

	public void storeGround(Dob ground) {
		storeGroundAt(ground, canonicalForms.get(ground));
	}
	
	public void storeGroundAt(Dob ground, Dob body) {
		formToGrounds.put(body, ground);
	}
	
	/**
	 * This method returns an iterable over all exhausted ground dobs 
	 * that potentially unify with the given body term.
	 * @param dob
	 * @return
	 */
	public Iterable<Dob> getGroundCandidates(Dob dob) {
		return OtmUtil.valueIterable(formToGrounds, spines.get(dob));
	}
	
	
	/**
	 * Returns a list that contains the assignment domain of each positive
	 * body term in the given rule assuming that we want to expand the given
	 * dob at the given position.
	 * @param rule
	 * @param position
	 * @param dob
	 * @return
	 */
	public ListMultimap<Atom, Dob> getSupport(Rule rule) {
		ListMultimap<Atom, Dob> candidates = ArrayListMultimap.create();
		
		for (int i = 0; i < rule.body.size(); i++) {
			Atom atom = rule.body.get(i);
			if (!atom.truth) continue;
			candidates.putAll(atom, getGroundCandidates(atom.dob));
		}
		return candidates;
	}
	
}

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
import com.google.common.collect.*;

/**
 * It's french. <br>
 * This class caches a bunch of things that are fun to use in logical manipulation.
 * It started out as a "performance" class, but it's more of a "logical scope" now.
 * @author ptpham
 *
 */
public class Cachet {
	public final Fortre fortre;

	/**
	 * This maps ground dobs to their canonical dobs (the dob at 
	 * the end of the unify trunk).
	 */
	public Cache<Dob, Dob> canonicalForms = 
		Cache.create(new Function<Dob, Dob>() {
			@Override public Dob apply(Dob dob) { 
				return Colut.end(fortre.getTrunk(dob));
			}
		});

	public Cache<Dob, List<Dob>> canonicalSpines = 
		Cache.create(new Function<Dob, List<Dob>>() {
			@Override public List<Dob> apply(Dob dob) {
				return Lists.newArrayList(fortre.getSpine(dob));
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
				return Lists.newArrayList(getAffectedRules(dob));
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

	public Cachet(Ruletta rta, Pool pool) {
		this.rta = rta;
		
		Set<Dob> allTerms = Sets.newHashSet();
		for (Atom atom : Rule.asAtomIterator(rta.allRules)) { allTerms.add(atom.dob); }
		this.fortre = new Fortre(allTerms, rta.homvar, pool);
	}

	public List<Dob> getUnifiableForms(Dob dob) {
		List<Dob> result = Lists.newArrayList();
		Set<Dob> vars = fortre.pool.allVars;
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
	
	
	/**
	 * This returns the list of rules mapped to by forms that unify
	 * against the given query.
	 * @param query a dob with variables that you want to unify against
	 * forms in the form tree.
	 * @param map the place where you want to look up rules after you 
	 * have filtered the forms you are interested in.
	 * @return
	 */
	public Iterable<Rule> getRulesWith(Dob query, Multimap<Dob, Rule> map) {
		List<Dob> forms = Unifier.retainSuccesses(query, 
			this.fortre.getCognateSpine(query), this.fortre.pool.allVars);
		return OtmUtil.valueIterable(map, forms);
	}
	
	/**
	 * Returns the set of rules that are mapped to by the forms in the
	 * spine of the given dob.
	 * @param query
	 * @param map
	 * @return
	 */
	public Iterable<Rule> getSpineRules(Dob query, Multimap<Dob, Rule> map) {
		return OtmUtil.valueIterable(map, this.fortre.getCognateSpine(query));
	}

	/**
	 * result method takes a dob and returns the rules where
	 * it can be applied. result set must be a subset of the rules whose bodies are 
	 * touched by the subtree of the fortre rooted at the end of the trunk.
	 * @param dob
	 * @return
	 */
	public Set<Rule> getAffectedRules(Dob dob) {
		Set<Dob> subtree = Sets.newHashSet();
		Iterables.addAll(subtree, fortre.getSpine(dob));
		return Sets.newHashSet(OtmUtil.valueIterable(rta.bodyToRule, subtree));
	}

}

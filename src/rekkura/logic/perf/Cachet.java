package rekkura.logic.perf;

import java.util.List;

import rekkura.logic.Ruletta;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Cache;
import rekkura.util.Colut;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * It's french.
 * This class caches a bunch of things that are fun to use in logical manipulation.
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
				List<Dob> splay = Lists.newArrayList(rta.fortre.getSpine(dob));
				Colut.remove(splay, rta.fortre.root);
				return splay;
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
	
	public final Ruletta rta;

	public Cachet(Ruletta rta) { this.rta = rta; }
}

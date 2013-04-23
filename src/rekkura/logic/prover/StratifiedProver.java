package rekkura.logic.prover;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import rekkura.logic.Cachet;
import rekkura.logic.Pool;
import rekkura.logic.Ruletta;
import rekkura.logic.Terra;
import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

/**
 * The set of rules provided to this prover must satisfy the following: <br>
 * - No negative heads <br>
 * - Stratified negation: From a rule R and its descendants, it must 
 * not be possible to generate a grounded dob that unifies with a 
 * negative term in the body of R.<br>
 * - Safety: Every variable that appears in a negative term must appear
 * in a positive body term. <br>
 * @author ptpham
 *
 */
public abstract class StratifiedProver {
	public final Ruletta rta;
	public final Cachet cachet;
	public final Pool pool = new Pool();
	public final Set<Dob> truths = Sets.newHashSet();
	
	/**
	 * This dob is used as a trigger for fully grounded rules
	 * that are entirely negative.
	 */
	protected Dob vacuous = new Dob("[VACUOUS]");
	
	public abstract Set<Dob> proveAll(Iterable<Dob> truths);

	public StratifiedProver(Collection<Rule> rules) {
		Set<Rule> submerged = Sets.newHashSet();
		
		for (Rule rule : rules) { submerged.add(pool.rules.submerge(rule)); }
		submerged = preprocess(submerged, this.vacuous);
		
		this.rta = Ruletta.create(submerged, pool);
		for (Rule rule : rta.allRules) {
			Preconditions.checkArgument(rule.head.truth, "Rules must have positive heads!");
		}
		
		this.cachet = new Cachet(rta);
	}

	
	/**
	 * This method adds a vacuous positive term to rules that 
	 * have bodies that are entirely negative and grounded.
	 * It will also reorder rules so that negative terms come last.
	 * 
	 * This method will not ruin a submersion.
	 * @param rules
	 * @return
	 */
	public static Set<Rule> preprocess(Collection<Rule> rules, Dob vacuous) {
		Set<Rule> result = Sets.newHashSet();
		
		for (Rule rule : rules) {
			List<Atom> positives = rule.getPositives();
			List<Atom> negatives = rule.getNegatives();
			
			boolean grounded = true;
			for (Atom atom : negatives) {
				if (!rule.isGrounded(atom.dob)) grounded = false;
			}
			
			if (grounded && positives.size() == 0) {
				positives.add(new Atom(vacuous, true));
			}
			
			result.add(new Rule(rule.head, 
				Iterables.concat(positives, negatives), rule.vars, rule.distinct));
		}
		
		return result;
	}
	

	/**
	 * This stores the given dob after submerging it.
	 * @param dob submerged version of the dob
	 * @return
	 */
	public Dob preserveTruth(Dob dob) {
		dob = this.pool.dobs.submerge(dob);
		storeTruth(dob);
		return dob;
	}

	/**
	 * This method makes sure that the given dob is indexable from the 
	 * last node in the trunk of the fortre. 
	 * @param dob
	 * @return
	 */
	public boolean storeTruth(Dob dob) {
		boolean added = truths.add(dob);
		this.cachet.storeGround(dob);
		return added;
	}
	
	public void preserveTruths(Iterable<Dob> truths) {
		for (Dob dob : truths) preserveTruth(dob);
	}
	
	/**
	 * So basically the whole system exists to support this method.
	 * @param rule
	 * @return
	 */
	public Set<Dob> expandRule(Rule rule) {	
		// Prepare the domains of each positive body in the rule
		ListMultimap<Atom, Dob> assignments = Terra.getBodySpace(rule, cachet);
		return Terra.applyBodyExpansion(rule, assignments, pool, truths);
	}
	
	public static interface Factory { StratifiedProver create(Collection<Rule> rules); }

	public static Factory FORWARD_FACTORY = new Factory() {
		@Override public StratifiedProver create(Collection<Rule> rules) {
			return new StratifiedForward(rules);
		}
	};
	
	public static Factory BACKWARD_FACTORY = new Factory() {
		@Override public StratifiedProver create(Collection<Rule> rules) {
			return new StratifiedBackward(rules);
		}
	};
}

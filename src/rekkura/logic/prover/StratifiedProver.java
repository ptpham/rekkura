package rekkura.logic.prover;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.algorithm.Terra;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.model.Unification;
import rekkura.logic.structure.Cachet;
import rekkura.logic.structure.Pool;
import rekkura.logic.structure.Ruletta;
import rekkura.util.Cartesian;
import rekkura.util.Colut;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
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
	
	public abstract Set<Dob> proveAll(Iterable<Dob> truths);

	public StratifiedProver(Collection<Rule> rules) {
		this.rta = Ruletta.create(rules, pool);
		for (Rule rule : rta.allRules) {
			Preconditions.checkArgument(rule.head.truth, "Rules must have positive heads!");
		}
		
		this.cachet = new Cachet(rta);
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
	
	protected Set<Dob> expandRule(Rule rule,
		Set<Dob> truths, Multimap<Atom,Dob> support, Pool pool) {
		List<Atom> expanders = Terra.getGreedyExpanders(rule, support);
		List<Atom> check = Colut.deselect(rule.body, expanders);
		Cartesian.AdvancingIterator<Unification> iterator =
			Terra.getBodySpaceIterator(rule, expanders, support, truths);
	
		List<Map<Dob,Dob>> unifies = Terra.applyExpansion(rule, iterator, check, pool, truths);
		return Terra.renderHeads(unifies, rule, pool);
	}

	
	public static interface Factory { StratifiedProver create(Collection<Rule> rules); }

	public static Factory FORWARD_FACTORY = new Factory() {
		@Override public StratifiedProver create(Collection<Rule> rules) {
			return new StratifiedForward(rules);
		}
	};
	
	public static Factory BACKWARD_STANDARD_FACTORY = new Factory() {
		@Override public StratifiedProver create(Collection<Rule> rules) {
			return new StratifiedBackward.Standard(rules);
		}
	};
}

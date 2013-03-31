package rekkura.logic.prover;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.Cachet;
import rekkura.logic.Pool;
import rekkura.logic.Ruletta;
import rekkura.logic.Terra;
import rekkura.logic.Unifier;
import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Cartesian;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class StratifiedProver {
	public final Ruletta rta;
	public final Cachet cachet;
	public final Pool pool = new Pool();
	public final Set<Dob> truths = Sets.newHashSet();

	private static final int DEFAULT_VARIABLE_SPACE_MIN = 512;
	public int variableSpaceMin = DEFAULT_VARIABLE_SPACE_MIN;
	
	/**
	 * This dob is used as a trigger for fully grounded rules
	 * that are entirely negative.
	 */
	protected Dob vacuous = new Dob("[VACUOUS]");
	
	public abstract Set<Dob> proveAll(Iterable<Dob> truths);

	public StratifiedProver(Collection<Rule> rules) {
		Set<Rule> submerged = Sets.newHashSet();
		
		for (Rule rule : rules) { submerged.add(pool.submerge(rule)); }
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
			
			rule.body.clear();
			rule.body.addAll(positives);
			rule.body.addAll(negatives);
			
			result.add(rule);
		}
		
		return result;
	}
	

	/**
	 * This method makes sure that the given dob is indexable from the 
	 * last node in the trunk of the fortre.
	 * @param dob
	 * @return
	 */
	public Dob storeTruth(Dob dob) {
		dob = this.pool.submerge(dob);
		truths.add(dob);
		this.cachet.storeGround(dob);
		return dob;
	}
	
	public void storeTruths(Iterable<Dob> truths) {
		for (Dob dob : truths) storeTruth(dob);
	}
	
	/**
	 * So basically the whole system exists to support this method.
	 * @param rule
	 * @return
	 */
	public Set<Dob> expandRule(Rule rule) {	
		Set<Dob> result = Sets.newHashSet();
			
		// Prepare the domains of each positive body in the rule
		List<Iterable<Dob>> assignments = Terra.getBodySpace(rule, cachet);
		int bodySpaceSize = Cartesian.size(assignments);
		
		// Decide whether to expand by terms or by variables based on the relative
		// sizes of the replacements. This test is only triggered for a sufficiently 
		// large body size because it costs more time to generate the variable space.
		boolean useVariables = (variableSpaceMin <= 0);
		if (useVariables || bodySpaceSize > variableSpaceMin) {
			List<Iterable<Dob>> variables = Terra.getVariableSpace(rule, cachet);
			useVariables |= bodySpaceSize > Cartesian.size(variables);
			if (useVariables) assignments = variables;
		}
		
		// Iterate through the Cartesian product of possibilities
		Map<Dob, Dob> unify = Maps.newHashMap();
		
		List<List<Dob>> space = Lists.newArrayListWithCapacity(assignments.size());
		for (Iterable<Dob> iterable : assignments) { space.add(Lists.newArrayList(iterable)); }
		
		for (List<Dob> assignment : Cartesian.asIterable(space)) {
			Map<Dob, Dob> success = null;

			if (!useVariables) success = Terra.applyBodies(rule, assignment, pool, truths);
			else success = Terra.applyVariables(rule, assignment, pool, truths);
			
			// If we manage to unify against all bodies, apply the substitution
			// to the head and render it. If the generated head still has variables
			// in it, then do not add it to the result.
			if (success != null && rule.vars.size() == success.size()
				&& rule.evaluateDistinct(success)) {
				Dob generated = this.pool.submerge(Unifier.replace(rule.head.dob, success));
				result.add(generated);
			}
			
			unify.clear();
		}
		
		return result;
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

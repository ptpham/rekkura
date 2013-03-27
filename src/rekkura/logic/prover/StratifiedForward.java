package rekkura.logic.prover;

import java.util.*;

import rekkura.logic.Pool;
import rekkura.logic.Ruletta;
import rekkura.logic.Terra;
import rekkura.logic.Unifier;
import rekkura.logic.perf.Cachet;
import rekkura.logic.perf.GroundScope;
import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Cartesian;
import rekkura.util.Colut;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;

/**
 * The set of rules provided to this prover must satisfy the following:
 * - No negative heads
 * - Stratified negation: From a rule R and its descendants, it must 
 * not be possible to generate a grounded dob that unifies with a 
 * negative term in the body of R.
 * - Safety: Every variable that appears in a negative term must appear
 * in a positive body term.
 * @author ptpham
 *
 */
public class StratifiedForward {
	public final Ruletta rta;
	public final Cachet cachet;
	public final Pool pool = new Pool();
	public final GroundScope scope = new GroundScope();
	public final Set<Dob> truths = Sets.newHashSet();

	private static final int DEFAULT_VARIABLE_SPACE_MIN = 512;
	public int variableSpaceMin = DEFAULT_VARIABLE_SPACE_MIN;

	private Multimap<Integer, Rule> pendingRules 
		= TreeMultimap.create(Ordering.natural(), Ordering.arbitrary());
	
	/**
	 * This dob is used as a trigger for fully grounded rules
	 * that are entirely negative.
	 */
	private Dob vacuous = new Dob("[VACUOUS]");
	
	public StratifiedForward(Collection<Rule> rules) {
		Set<Rule> submerged = Sets.newHashSet();
		
		for (Rule rule : rules) { submerged.add(pool.submerge(rule)); }
		submerged = preprocess(submerged);
		
		this.rta = Ruletta.create(submerged, pool);
		for (Rule rule : rta.allRules) {
			Preconditions.checkArgument(rule.head.truth, "Rules must have positive heads!");
		}
		
		this.cachet = new Cachet(rta);
		clear();
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
	private Set<Rule> preprocess(Collection<Rule> rules) {
		Set<Rule> result = Sets.newHashSet();
		
		for (Rule rule : rules) {
			List<Atom> positives = rule.getPositives();
			List<Atom> negatives = rule.getNegatives();
			
			boolean grounded = true;
			for (Atom atom : negatives) {
				if (!rule.isGrounded(atom.dob)) grounded = false;
			}
			
			if (grounded && positives.size() == 0) {
				positives.add(new Atom(this.vacuous, true));
			}
			
			rule.body.clear();
			rule.body.addAll(positives);
			rule.body.addAll(negatives);
			
			result.add(rule);
		}
		
		return result;
	}

	public void reset(Iterable<Dob> truths) {
		clear();
		this.queueTruth(vacuous);
		for (Dob truth : truths) if (truth != null) queueTruth(truth);
	}
	
	public Set<Dob> proveAll(Iterable<Dob> truths) {
		this.reset(truths);
		Set<Dob> result = Sets.newHashSet();
		while (this.hasMore()) result.addAll(this.proveNext());
		return result;
	}
	
	/**
	 * Initialize private variables that track the state of the prover
	 */
	public void clear() {
		this.truths.clear();
		this.pendingRules.clear();
		this.scope.unisuccess.clear();
	}

	/**
	 * Add a dob that is true. The dob will be stored (attached to the last
	 * node on its unify trunk of the fortre) and potential assignments for
	 * the dob will be generated privately.
	 * 
	 * If a truth is queued by the user after the prover starts proving,
	 * there is no longer a guarantee of correctness if rules have negative
	 * body terms.
	 * @param dob
	 */
	protected Dob queueTruth(Dob dob) {
		dob = this.pool.submerge(dob);
		if (truths.contains(dob)) return this.vacuous;
		
		Iterable<Rule> generated = this.cachet.affectedRules.get(dob);

		// Add rules with their topological priority
		for (Rule rule : generated) {
			int priority = this.rta.ruleOrder.count(rule);
			this.pendingRules.put(priority, rule);
		}
		
		storeGround(dob);

		return dob;
	}

	/**
	 * This method makes sure that the given dob is indexable from the 
	 * last node in the trunk of the fortre.
	 * @param dob
	 * @return
	 */
	private void storeGround(Dob dob) {
		truths.add(dob);

		// The root of the subtree is the end of the trunk.
		Dob end = this.cachet.canonicalForms.get(dob);
		if (end != null) this.scope.storeGround(dob, end);
	}

	public boolean hasMore() { return this.pendingRules.size() > 0; }
	
	public List<Dob> proveNext() {
		if (!hasMore()) throw new NoSuchElementException();
		Rule rule = Colut.popAny(this.pendingRules.values());	
		Set<Dob> generated = expandRule(rule);
		
		// Submerge all of the newly generated dobs
		List<Dob> result = Lists.newArrayListWithCapacity(generated.size());
		for (Dob dob : generated) {
			Dob submerged =queueTruth(dob);
			if (submerged != vacuous) result.add(submerged);
		}
		
		return result;
	}

	public Set<Dob> expandRule(Rule rule) {		
		Set<Dob> result = Sets.newHashSet();
		
		// Prepare the domains of each positive body in the rule
		List<Iterable<Dob>> assignments = Terra.getBodySpace(rule, cachet, scope);
		int bodySpaceSize = Cartesian.size(assignments);
		
		// Decide whether to expand by terms or by variables based on the relative
		// sizes of the replacements. This test is only triggered for a sufficiently 
		// large body size because it costs more time to generate the variable space.
		boolean useVariables = (variableSpaceMin <= 0);
		if (useVariables || bodySpaceSize > variableSpaceMin) {
			List<Iterable<Dob>> variables = Terra.getVariableSpace(rule, cachet, scope);
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
			if (success != null && rule.vars.size() == success.size()) {
				Dob generated = this.pool.submerge(Unifier.replace(rule.head.dob, success));
				result.add(generated);
			}
			
			unify.clear();
		}
				
		return result;
	}
	
	public static Iterator<List<Dob>> asIterator(final StratifiedForward prover) {
		return new Iterator<List<Dob>>() {
			@Override public boolean hasNext() { return prover.hasMore(); }
			@Override public List<Dob> next() { return prover.proveNext(); }
			@Override public void remove() { throw new IllegalAccessError("Remove not allowed!"); }
		};
	}
}

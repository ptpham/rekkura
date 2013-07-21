package rekkura.logic.prover;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.algorithm.Gondwana;
import rekkura.logic.algorithm.Terra;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Cachet;
import rekkura.logic.structure.Pool;
import rekkura.logic.structure.Ruletta;
import rekkura.stats.algorithm.Ucb;
import rekkura.stats.algorithm.Ucb.Suggestor;
import rekkura.util.Cache;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
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
	
	public abstract Set<Dob> proveAll(Iterable<Dob> truths);

	public StratifiedProver(Collection<Rule> rules) {
		this.rta = Ruletta.create(pool.rules.submerge(rules), pool);
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
	
	private static enum Method {
		BODY_SPACE,
		LINEAR_JOIN
	}
	
	private Cache<Rule, Ucb.Suggestor<Method>> suggestors = 
		Cache.create(new Function<Rule,Ucb.Suggestor<Method>>() {
			@Override public Suggestor<Method> apply(Rule rule) {
				return new Ucb.Suggestor<Method>(Arrays.asList(Method.values()), 30);
			}
		});
	
	protected Set<Dob> expandRule(Rule rule,
		Set<Dob> truths, Cachet cachet, Pool pool) {
		Ucb.Suggestor<Method> suggestor = suggestors.get(rule);
		
		Method method = suggestor.suggest();
		long begin = System.currentTimeMillis();
		Set<Dob> generated = expandRuleWithMethod(rule, truths, cachet, pool, method);
		double value = 1.0/(System.currentTimeMillis() - begin);
		if (Double.isInfinite(value) && method != Method.BODY_SPACE) value = 0;
		suggestor.inform(method, value);
		
		return generated;
	}
	
	public static Set<Dob> expandRuleWithMethod(Rule rule, Set<Dob> truths, Cachet cachet, Pool pool, Method method) {
		ListMultimap<Atom, Dob> support = Terra.getBodySpace(rule, cachet);
		List<Map<Dob,Dob>> unifies = null;
		switch (method) {
		case BODY_SPACE:
			unifies = Terra.applyBodyExpansion(rule, support, pool, truths);
			break;
		case LINEAR_JOIN:
			unifies = Gondwana.applyLinearJoin(rule, support, pool, truths);
			break;
		}
		return Terra.renderHeads(unifies, rule, pool);
	}
	
	public static Set<Dob> expandWithLinearJoin(Rule rule, Set<Dob> truths, Cachet cachet, Pool pool) {
		ListMultimap<Atom, Dob> assignments = Terra.getBodySpace(rule, cachet);
		List<Map<Dob,Dob>> unifies = Gondwana.applyLinearJoin(rule, assignments, pool, truths);
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

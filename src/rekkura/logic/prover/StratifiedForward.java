package rekkura.logic.prover;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Colut;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

/**
 * This class will attempt to push things that are true
 * forward in the rule graph. This is generally inefficient 
 * if you want to know everything that could be derived from
 * a particular rule. For that use case use {@link StratifiedBackward}. <br>
 * <br>
 * This prover is meant for more exploratory searching.
 * @author ptpham
 *
 */
public class StratifiedForward extends StratifiedProver {

	private Multimap<Integer, Rule> pendingRules 
		= TreeMultimap.create(Ordering.natural(), Ordering.arbitrary());

	private List<Rule> bodyless = Lists.newArrayList();
	
	public StratifiedForward(Collection<Rule> rules) {
		super(rules);
		for (Rule rule : this.rta.allRules) if (rule.body.size() == 0) bodyless.add(rule);
		clear();
	}

	public void reset(Iterable<Dob> truths) {
		clear();
		this.queueRules(bodyless);
		for (Dob truth : truths) if (truth != null) queueTruth(truth);
	}
	
	public Set<Dob> proveAll(Iterable<Dob> truths) {
		this.reset(truths);
		Set<Dob> result = Sets.newHashSet();
		while (this.hasMore()) result.addAll(this.proveNext());
		return result;
	}
	
	public void clear() {
		this.truths.clear();
		this.pendingRules.clear();
		this.cachet.unisuccess.clear();
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
		dob = this.pool.dobs.submerge(dob);
		boolean added = this.storeTruth(dob);
		if (!added) return dob;
		
		queueRules(this.cachet.affectedRules.get(dob));
		return dob;
	}

	/**
	 * Add rules with their topological priority
	 * @param generated
	 */
	private void queueRules(Iterable<Rule> generated) {
		for (Rule rule : generated) {
			int priority = this.rta.ruleOrder.count(rule);
			this.pendingRules.put(priority, rule);
		}
	}

	public boolean hasMore() { return this.pendingRules.size() > 0; }
	
	public List<Dob> proveNext() {
		if (!hasMore()) throw new NoSuchElementException();
		Rule rule = Colut.popAny(this.pendingRules.values());
		
		Set<Dob> generated = expandRule(rule);
		
		// Submerge all of the newly generated dobs
		List<Dob> result = Lists.newArrayListWithCapacity(generated.size());
		for (Dob dob : generated) {
			Dob submerged = queueTruth(dob);
			result.add(submerged);
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

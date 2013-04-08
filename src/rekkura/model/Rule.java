package rekkura.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import rekkura.fmt.StandardFormat;
import rekkura.util.Colut;
import rekkura.util.NestedIterator;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Unlike the other logical objects, rules are mutable.
 * 
 * There is also a lot of code here for the sake of avoiding
 * double for loops. O__O
 * @author ptpham
 *
 */
public class Rule {
	public Atom head;
	public List<Atom> body = Lists.newArrayList();
	public List<Dob> vars = Lists.newArrayList();
	public List<Distinct> distinct = Lists.newArrayList();
	
	public Rule() { }
	
	public Rule(Atom head, Collection<Atom> body, Collection<Dob> variables) {
		this(head, body, variables, Lists.<Distinct>newArrayList());
	}
	
	public Rule(Atom head, Collection<Atom> body, 
			Collection<Dob> variables, Collection<Distinct> distinct) {
		this.head = head;
		this.body = Lists.newArrayList(body);
		this.vars = Lists.newArrayList(variables);
		this.distinct = Lists.newArrayList(distinct);
	}
	
	public Rule(Rule other) {
		this(other.head, other.body, other.vars, other.distinct);
	}
	
	public boolean isGrounded(Dob dob) {
		return Colut.containsNone(dob.fullIterable(), vars);
	}
	
	public static class Distinct {
		public final Dob first, second;
		public Distinct(Dob first, Dob second) {
			this.first = first;
			this.second = second;
		}
		
		@Override
		public String toString() {
			return StandardFormat.inst.toString(this);
		}
	}
	
	/**
	 * This method checks that the given unification does not 
	 * violate the distinct constraints defined by this rule.
	 * @param unify
	 * @return
	 */
	public boolean evaluateDistinct(Map<Dob, Dob> unify) {
		if (this.distinct.size() == 0) return true;
		
		for (Distinct entry : this.distinct) {
			Dob first = entry.first;
			Dob second = entry.second;
			
			if (this.vars.contains(first)) first = unify.get(first);
			if (this.vars.contains(second)) second = unify.get(second);
			
			if (first == second) return false;
		}
		
		return true;
	}
	
	public List<Atom> getPositives() {
		List<Atom> positives = Lists.newArrayList();
		for (Atom atom : this.body) { if (atom.truth) positives.add(atom); }
		return positives;
	}
	
	public List<Atom> getNegatives() {
		List<Atom> negatives = Lists.newArrayList();
		for (Atom atom : this.body) { if (!atom.truth) negatives.add(atom); }
		return negatives;
	}
	
	public static Rule asVacuousRule(Dob dob) {
		return asVacuousRule(dob, Lists.<Dob>newArrayList());
	}
	
	public static Rule asVacuousRule(Dob dob, Collection<Dob> vars) {
		Rule result = new Rule();
		result.head = new Atom(dob, true);
		result.vars.addAll(vars);
		return result;
	}
	
	public static Iterator<Atom> atomIteratorFromRule(final Rule rule) {
		return Iterators.concat(rule.body.iterator(), Iterators.forArray(rule.head));
	}
	
	public static Iterator<Atom> atomIteratorFromRules(final Iterator<Rule> rules) {
		return new NestedIterator<Rule, Atom>(rules) {
			@Override protected Iterator<Atom> prepareNext(Rule u) { return atomIteratorFromRule(u); }
		};
	}
	
	public static Iterator<Atom> atomIteratorFromRules(final Collection<Rule> rules) {
		return atomIteratorFromRules(rules.iterator());
	}
	
	public static Iterable<Atom> atomIterableFromRule(Rule rule) {
		return atomIterableFromRules(Lists.newArrayList(rule));
	}
	
	public static Iterable<Atom> atomIterableFromRules(final Collection<Rule> rules) {
		return new Iterable<Atom>() {
			@Override public Iterator<Atom> iterator() { return atomIteratorFromRules(rules); }
		};
	}
	
	public static Iterable<Dob> dobIterableFromRule(Rule rule) {
		return Atom.dobIterableFromAtoms(atomIterableFromRule(rule));
	}
	
	@Override public String toString() { return StandardFormat.inst.toString(this); }

}


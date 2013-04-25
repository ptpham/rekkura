package rekkura.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import rekkura.fmt.StandardFormat;
import rekkura.util.Colut;
import rekkura.util.NestedIterator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Like the other logical objects, rules are immutable.
 * 
 * There is also a lot of code here for the sake of avoiding
 * double for loops. O__O
 * @author ptpham
 *
 */
public class Rule {
	public static final ImmutableSet<Rule> EMPTY_SET = ImmutableSet.of();
	public static final ImmutableList<Rule> EMPTY_LIST = ImmutableList.of();

	public final Atom head;
	public final ImmutableList<Atom> body;
	public final ImmutableList<Distinct> distinct;
	public final ImmutableList<Dob> vars;
	
	public static class Builder {
		public Atom head;
		public final List<Atom> body = Lists.newArrayList();
		public final List<Distinct> distinct = Lists.newArrayList();
		public final List<Dob> vars = Lists.newArrayList();
	}
	
	public Rule(Atom head, Iterable<Atom> body, Iterable<Dob> variables) {
		this(head, body, variables, Lists.<Distinct>newArrayList());
	}
	
	public Rule(Atom head, Iterable<Atom> body, 
			Iterable<Dob> variables, Iterable<Distinct> distinct) {
		this.head = head;
		this.body = ImmutableList.copyOf(body);
		this.vars = ImmutableList.copyOf(variables);
		this.distinct = ImmutableList.copyOf(distinct);
	}
	
	public Rule(Rule.Builder builder) {
		this(builder.head, builder.body, builder.vars, builder.distinct);
	}
	
	public Rule.Builder toBuilder() {
		Rule.Builder builder = new Rule.Builder();
		builder.head = this.head;
		builder.body.addAll(this.body);
		builder.vars.addAll(this.vars);
		builder.distinct.addAll(this.distinct);
		
		return builder;
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
	
	/**
	 * Reference equality comparison of the logic components of rules.
	 * @param first
	 * @param second
	 * @return
	 */
	public static boolean refeq(Rule first, Rule second) {
		return first.head == second.head && Colut.containsSame(first.body, second.body)
				&& Colut.containsSame(first.vars, second.vars) 
				&& Colut.containsSame(first.distinct, second.distinct);
	}
	
	/**
	 * This also requires that the ordering of the logical componets in the
	 * rules is the same.
	 * @param first
	 * @param second
	 * @return
	 */
	public static boolean orderedRefeq(Rule first, Rule second) {
		return first.head == second.head && first.body.equals(second.body)
				&& first.vars.equals(second.vars) 
				&& first.distinct.equals(second.distinct);
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
		return new Rule(new Atom(dob, true), 
			Lists.<Atom>newArrayList(), vars);
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


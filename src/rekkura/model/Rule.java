package rekkura.model;

import java.util.*;

import rekkura.fmt.StandardFormat;
import rekkura.util.Colut;
import rekkura.util.NestedIterator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
		
		/**
		 * A canonized distinct is one that respects dob ordering between
		 * its first and second fields.
		 * @param distinct
		 * @param vars
		 * @return
		 */
		public static Distinct canonize(Distinct distinct, Collection<Dob> vars) {
			List<Dob> dobs = Lists.newArrayList(distinct.first, distinct.second);
			Comparator<Dob> comp = Dob.getComparator(vars);
			if (comp.compare(distinct.first, distinct.second) == 0) comp = Dob.getComparator();
			
			Collections.sort(dobs, comp);
			if (dobs.get(0) == distinct.first) return distinct;
			return new Distinct(distinct.second, distinct.first);
		}
		
		public static Comparator<Distinct> getComparator(final Collection<Dob> vars) {
			return new Comparator<Rule.Distinct>() {
				@Override public int compare(Distinct arg0, Distinct arg1) {
					return Distinct.compare(arg0, arg1, vars);
				}
			};
		}
		
		public static int compare(Distinct arg0, Distinct arg1,
				final Collection<Dob> vars) {
			Distinct left = Distinct.canonize(arg0, vars);
			Distinct right = Distinct.canonize(arg1, vars);
			
			int compare = Dob.compareStructure(left.first, right.first, vars);
			if (compare != 0) return compare;
			
			compare = Dob.compareStructure(left.second, right.second, vars);
			return compare;
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
	
	/**
	 * This method checks if the first and second rules are structurally
	 * equivalent under the union of their variable sets assuming that
	 * the ordering of atoms and distincts are fixed.
	 * @param first
	 * @param second
	 * @return
	 */
	public static boolean orderedStructEq(Rule first, Rule second) {
		if (first.vars.size() != second.vars.size()) return false;
		if (first.body.size() != second.body.size()) return false;
		if (first.distinct.size() != second.distinct.size()) return false;
		
		Set<Dob> vars = Sets.newHashSet(first.vars);
		vars.addAll(second.vars);
		
		int compare = Dob.compareStructure(first.head.dob, second.head.dob, vars);
		if (compare != 0) return false;
		
		for (int i = 0; i < first.body.size(); i++) {
			Atom firstAtom = first.body.get(i);
			Atom secondAtom = second.body.get(i);
			compare = Dob.compareStructure(firstAtom.dob, secondAtom.dob, vars);
			if (compare != 0) return false;
		}
		
		for (int i = 0; i < first.distinct.size(); i++) {
			Distinct left = first.distinct.get(i);
			Distinct right = second.distinct.get(i);
			compare = Distinct.compare(left, right, vars);
		}
		
		return true;
	}
	

	/**
	 * This method creates a new copy of the rule such that the constituent
	 * components are ordered and referentially duplicate elements are removed.
	 * After canonization, it is possible to test for equality using the
	 * ordered structural comparison.
	 * @param rule
	 * @return
	 */
	public static Rule canonize(final Rule rule) {
		List<Dob> vars = Lists.newArrayList(rule.vars);
		List<Atom> body = Lists.newArrayList(rule.body);
		List<Rule.Distinct> distincts = Lists.newArrayListWithCapacity(rule.distinct.size());
		for (Distinct distinct : rule.distinct) { distincts.add(Distinct.canonize(distinct, rule.vars)); }
		
		Collections.sort(vars, Dob.getComparator());
		Collections.sort(body, Atom.getComparator(rule.vars));
		Collections.sort(distincts, Distinct.getComparator(rule.vars));
		
		vars = Colut.filterAdjacentRefeq(vars);
		body = Colut.filterAdjacentRefeq(body);
		distincts = Colut.filterAdjacentRefeq(distincts);
		return new Rule(rule.head, body, vars, distincts);
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
	
	public Set<Dob> getVariablesOf(Dob dob) {
		Set<Dob> result = Sets.newHashSet(dob.fullIterable());
		result.retainAll(this.vars);
		return result;
	}
	
	public static Rule asVacuous(Dob dob) {
		return asVacuous(dob, Lists.<Dob>newArrayList());
	}
	
	public static boolean isVacuous(Rule rule) {
		return rule.body.size() == 0 && rule.distinct.size() == 0;
	}
	
	public static Rule asVacuous(Dob dob, Collection<Dob> vars) {
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


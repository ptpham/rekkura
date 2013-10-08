package rekkura.logic.model;

import java.util.*;

import rekkura.logic.format.StandardFormat;
import rekkura.util.Colut;
import rekkura.util.NestedIterator;

import com.google.common.collect.*;

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
		public Rule build() { return new Rule(this); }
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
	
	private Rule(Rule.Builder builder) {
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
		
		public static List<Distinct> keepUnrelated(Iterable<Distinct> distincts,
				Collection<Dob> diff) {
			List<Distinct> filtered = Lists.newArrayList();
			for (Distinct distinct : distincts) {
				if (diff.contains(distinct.first)) continue;
				if (diff.contains(distinct.second)) continue;
				filtered.add(distinct);
			}
			return filtered;
		}
	}
	
	public static enum Canonization { VARS, BODY, DISTINCT }
	
	/**
	 * This method checks that the given unification does not 
	 * violate the distinct constraints defined by this rule.
	 * @param unify
	 * @return
	 */
	public boolean evaluateDistinct(Map<Dob, Dob> unify) {
		return evaluateDistinct(unify, this.vars, this.distinct);
	}

	public static boolean evaluateDistinct(Map<Dob, Dob> unify, 
		Collection<Dob> vars, List<Distinct> distinct) {
		if (distinct.size() == 0) return true;
		
		for (Distinct entry : distinct) {
			Dob first = entry.first;
			Dob second = entry.second;
			
			if (vars.contains(first)) first = unify.get(first);
			if (vars.contains(second)) second = unify.get(second);
			
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
	public static int compareOrderedStructural(Rule first, Rule second) {
		int compare = compareSizes(first, second);
		if (compare != 0) return compare;
		
		Set<Dob> vars = Sets.newHashSet(first.vars);
		vars.addAll(second.vars);
		
		compare = Dob.compareStructure(first.head.dob, second.head.dob, vars);
		if (compare != 0) return compare;
		
		for (int i = 0; i < first.body.size(); i++) {
			Atom firstAtom = first.body.get(i);
			Atom secondAtom = second.body.get(i);
			compare = Dob.compareStructure(firstAtom.dob, secondAtom.dob, vars);
			if (compare != 0) return compare;
		}
		
		for (int i = 0; i < first.distinct.size(); i++) {
			Distinct left = first.distinct.get(i);
			Distinct right = second.distinct.get(i);
			compare = Distinct.compare(left, right, vars);
		}
		
		return 0;
	}

	public static int compareSizes(Rule first, Rule second) {
		int left, right;
		
		left = first.vars.size();
		right = second.vars.size();
		if (left != right) return left - right;
		
		left = first.body.size();
		right = second.body.size();
		if (left != right) return left - right;
		
		left = first.distinct.size();
		right = second.distinct.size();
		if (left != right) return left - right;
		
		return 0;
	}

	public static final Comparator<Rule> COMPARATOR_ORDERED_STRUCTURAL = 
		new Comparator<Rule>() {
			@Override public int compare(Rule first, Rule second)
			{ return compareOrderedStructural(first, second); }
		};
	
	/**
	 * This method creates a new copy of the rule such that the constituent
	 * components are ordered and referentially duplicate elements are removed.
	 * After canonization, it is possible to test for equality using the
	 * ordered structural comparison.
	 * @param rule
	 * @return
	 */
	public static Rule canonize(final Rule rule) { return canonize(rule, null); }
	public static Rule canonize(final Rule rule, EnumSet<Canonization> type) {
		boolean vdist = type == null || type.contains(Canonization.VARS);
		boolean bdist = type == null || type.contains(Canonization.BODY);
		boolean ddist = type == null || type.contains(Canonization.DISTINCT);
		
		List<Dob> vars = Lists.newArrayList(rule.vars);
		List<Atom> body = Lists.newArrayList(rule.body);
		List<Rule.Distinct> distincts = Lists.newArrayListWithCapacity(rule.distinct.size());
		for (Distinct distinct : rule.distinct) {
			if (ddist) distincts.add(Distinct.canonize(distinct, rule.vars));
			else distincts.add(distinct);
		}
		
		if (vdist) {
			Collections.sort(vars, Dob.getComparator());
			vars = Colut.filterAdjacentRefeq(vars);
		}

		if (bdist) {
			Collections.sort(body, Atom.getComparator(rule.vars));
			body = Colut.filterAdjacentRefeq(body);
		}

		if (ddist) {
			Collections.sort(distincts, Distinct.getComparator(rule.vars));
			distincts = Colut.filterAdjacentRefeq(distincts);
		}

		return new Rule(rule.head, body, vars, distincts);
	}
	
	public Set<Dob> getVariablesOf(Dob dob) {
		return getVariablesOf(dob, this.vars);
	}
	
	public static Set<Dob> getVariablesOf(Dob dob, Collection<Dob> vars) {
		Set<Dob> result = Sets.newHashSet(dob.fullIterable());
		result.retainAll(vars);
		return result;
	}
	
	public static Set<Dob> getVariablesOf(Iterable<Atom> atoms, Collection<Dob> allVars) {
		Set<Dob> vars = Sets.newHashSet();
		for (Dob dob : Atom.asDobIterable(atoms)) {
			vars.addAll(Rule.getVariablesOf(dob, allVars));
		}
		return vars;
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
	
	public static Iterator<Atom> asAtomIterator(final Rule rule) {
		return Iterators.concat(rule.body.iterator(), Iterators.forArray(rule.head));
	}
	
	public static Iterator<Atom> asAtomIterator(final Iterator<Rule> rules) {
		return new NestedIterator<Rule, Atom>(rules) {
			@Override protected Iterator<Atom> prepareNext(Rule u) { return asAtomIterator(u); }
		};
	}
	
	public static Iterable<Atom> asAtomIterable(Rule rule) {
		return asAtomIterator(Lists.newArrayList(rule));
	}
	
	public static Iterable<Atom> asAtomIterator(final Iterable<Rule> rules) {
		return new Iterable<Atom>() {
			@Override public Iterator<Atom> iterator() { return asAtomIterator(rules.iterator()); }
		};
	}
	
	public static Iterator<Atom> asHeadIterator(final Iterator<Rule> rules) {
		return new NestedIterator<Rule, Atom>(rules) {
			@Override protected Iterator<Atom> prepareNext(Rule u) { return Iterators.forArray(u.head); }
		};
	}
	
	public static Iterable<Atom> asHeadIterator(final Iterable<Rule> rules) {
		return new Iterable<Atom>() {
			@Override public Iterator<Atom> iterator() { return asHeadIterator(rules.iterator()); }
		};
	}
	
	public static Iterable<Dob> dobIterableFromRule(Rule rule) {
		return Iterables.concat(Atom.asDobIterable(asAtomIterable(rule)),
			dobIterableFromDistincts(rule.distinct));
	}
	
	public static List<Dob> dobIterableFromDistincts(Iterable<Distinct> distincts) {
		List<Dob> result = Lists.newArrayList();
		for (Distinct distinct : distincts) {
			result.add(distinct.first);
			result.add(distinct.second);
		}
		return result;
	}
	
	@Override public String toString() { return StandardFormat.inst.toString(this); }

}


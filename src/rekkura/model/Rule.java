package rekkura.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import rekkura.util.NestedIterator;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
	public Set<Dob> vars = Sets.newHashSet();
	
	public Rule() { }
	public Rule(Atom head, Collection<Atom> body, Collection<Dob> variables) {
		this.head = head;
		this.body = Lists.newArrayList(body);
		this.vars = Sets.newHashSet(variables);
	}
	
	public static Iterator<Atom> atomIteratorFromRule(final Rule rule) {
		return new Iterator<Atom>() {
			Iterator<Atom> body = rule.body.iterator();
			Atom head = rule.head;
			
			@Override public boolean hasNext() {
				return body.hasNext() || head != null; 
			}

			@Override
			public Atom next() {
				if (head != null) { 
					Atom result = head;
					head = null;
					return result;
				}
				return body.next();
			}

			@Override public void remove() {}
		};
	}
	
	public static Iterator<Atom> atomIteratorFromRules(final Collection<Rule> rules) {
		return atomIteratorFromRules(rules.iterator());
	}
	
	public static Iterator<Atom> atomIteratorFromRules(final Iterator<Rule> rules) {
		return new NestedIterator<Rule, Atom>(rules) {
			@Override protected Iterator<Atom> prepareNext(Rule u) { return u.body.iterator(); }
		};
	}

	
	public static Iterable<Atom> atomIterableFromRules(final Collection<Rule> rules) {
		return new Iterable<Atom>() {
			@Override public Iterator<Atom> iterator() { return atomIteratorFromRules(rules); }
		};
	}
	
	public static Iterator<Dob> dobIteratorFromRule(final Rule rule) {
		return Iterators.concat(Atom.dobIteratorFromAtoms(atomIteratorFromRule(rule)), 
				rule.vars.iterator());
	}
	
	public static Iterator<Dob> dobIteratorFromRules(final Collection<Rule> rules) {
		return new NestedIterator<Rule, Dob>(rules.iterator()) {
			@Override protected Iterator<Dob> prepareNext(Rule u) { return dobIteratorFromRule(u); }
		};
	}
	
	public static Iterable<Dob> dobIterableFromRules(final Collection<Rule> rules) {
		return new Iterable<Dob>() {
			@Override public Iterator<Dob> iterator() { return dobIteratorFromRules(rules); }
		};
	}
}

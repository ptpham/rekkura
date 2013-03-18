package rekkura.logic;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.OTMUtil;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * This class maintains mappings and sets that are important for 
 * working with a set of rules.
 * @author ptpham
 *
 */
public class Ruletta {

	public Set<Rule> allRules;
	public Set<Dob> allVars, allDobs, posDobs, negDobs;
	public Multimap<Dob, Rule> bodyToRule, headToRule;
	
	public void construct(Collection<Rule> rules) {
		this.allRules = Sets.newHashSet(rules);
		
		this.allVars = Sets.newHashSet();
		this.allDobs = Sets.newHashSet();
		this.posDobs = Sets.newHashSet();
		this.negDobs = Sets.newHashSet();

		this.bodyToRule = HashMultimap.create();
		this.headToRule = HashMultimap.create();
		
		for (Dob dob : Rule.dobIterableFromRules(this.allRules)) { allDobs.add(dob); }
		for (Atom atom : Rule.atomIterableFromRules(this.allRules)) {
			if (atom.truth) posDobs.add(atom.dob);
			else negDobs.add(atom.dob);
		}
		
		// Prepare data structures to compute dependencies
		for (Rule rule : this.allRules) { 
			this.allVars.addAll(rule.vars);
			
			this.headToRule.put(rule.head.dob, rule);
			for (Dob body : Atom.dobIterableFromAtoms(rule.body)) {
				this.bodyToRule.put(body, rule);	
			}
		}
	}

	public Iterable<Dob> getAllTerms() {
		return Iterables.concat(this.headToRule.keySet(), this.bodyToRule.keySet()); 
	}
	
	public Iterator<Rule> ruleIteratorFromBodyDobs(Iterator<Dob> dobs) {
		return OTMUtil.valueIterator(bodyToRule, dobs);
	}
	
	public Iterable<Rule> ruleIterableFromBodyDobs(final Iterable<Dob> dobs) {
		return OTMUtil.valueIterable(bodyToRule, dobs);
	}
}

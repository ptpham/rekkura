package rekkura.logic;

import java.util.List;

import rekkura.fmt.LogicFormat;
import rekkura.fmt.StandardFormat;
import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Submerger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * A pool represents a set of dobs that can be compared 
 * with reference equality. Submerging a dob means to 
 * construct a corresponding dob such that all sub-trees
 * of the dob can be compared with reference equality against
 * all other dobs currently in the pool.
 * @author ptpham
 *
 */
public class Pool {

	public final LogicFormat fmt = new StandardFormat();
	public final Submerger<Dob> dobs = createDobSubmerger();
	public final Submerger<Atom> atoms = createAtomSubmerger();
	public final Submerger<Rule> rules = createRuleSubmerger();
	
	private Submerger<Dob> createDobSubmerger() {
		return new Submerger<Dob>() {
			@Override public Dob fromString(String s) { return fmt.dobFromString(s); }
			@Override public String toString(Dob u) { return fmt.toString(u); }
			@Override public Dob process(Dob u) { return handleUnseen(u); }
		};
	}

	private Submerger<Atom> createAtomSubmerger() {
		return new Submerger<Atom>() {
			@Override public Atom fromString(String s) { return fmt.atomFromString(s); }
			@Override public String toString(Atom u) { return fmt.toString(u); }
			@Override public Atom process(Atom u) { return handleUnseen(u); }
		};
	}
	
	private Submerger<Rule> createRuleSubmerger() {
		return new Submerger<Rule>() {
			@Override public Rule fromString(String s) { return fmt.ruleFromString(s); }
			@Override public String toString(Rule u) { return fmt.toString(u); }
			@Override public Rule process(Rule u) { return handleUnseen(u); }
		};
	}
	
	private Dob handleUnseen(Dob dob) {
		boolean changed = false;
		List<Dob> newChildren = Lists.newArrayListWithCapacity(dob.size());
		for (int i = 0; i < dob.size(); i++) {
			Dob child = dob.at(i);
			Dob submerged = dobs.submerge(child);
			if (child != submerged) changed = true;
			newChildren.add(submerged);
		}
		
		if (changed) return new Dob(newChildren);
		return dob;
	}
	
	private Atom handleUnseen(Atom atom) {
		return new Atom(dobs.submerge(atom.dob), atom.truth);
	}
	
	private Rule handleUnseen(Rule rule) {
		List<Rule.Distinct> distincts = Lists.newArrayList();
		for (Rule.Distinct distinct : rule.distinct) {
			distincts.add(new Rule.Distinct(dobs.submerge(distinct.first), 
					dobs.submerge(distinct.second)));
		}
		
		return new Rule(
			atoms.submerge(rule.head),
			atoms.submerge(rule.body),
			Sets.newHashSet(dobs.submerge(rule.vars)),
			distincts);
	}
}

package rekkura.logic.structure;

import java.util.List;
import java.util.Set;

import rekkura.logic.format.LogicFormat;
import rekkura.logic.format.StandardFormat;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.model.Vars;
import rekkura.util.CachingSupplier;
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
	private CachingSupplier<Dob> vargen = new Dob.PrefixedSupplier("PLG");
	
	public final LogicFormat fmt = new StandardFormat();
	public final Submerger<Dob> dobs = createDobSubmerger();
	public final Submerger<Atom> atoms = createAtomSubmerger();
	public final Submerger<Rule> rules = createRuleSubmerger();
	public final Set<Dob> allVars = Sets.newHashSet();
	public final Vars.Context context = Vars.asContext(allVars, vargen);
	
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
		Dob dob = dobs.submerge(atom.dob);
		if (dob == atom.dob) return atom;
		return new Atom(dob, atom.truth);
	}
	
	private Rule handleUnseen(Rule rule) {
		Atom head = atoms.submerge(rule.head);
		
		List<Atom> body = Lists.newArrayList();
		for (Atom term : rule.body) body.add(atoms.submerge(term));
		
		List<Rule.Distinct> distincts = Lists.newArrayList();
		for (Rule.Distinct distinct : rule.distinct) distincts.add(handleUnseen(distinct));
		
		List<Dob> vars = Lists.newArrayList();
		for (Dob var : rule.vars) vars.add(dobs.submerge(var));
		
		Rule result = new Rule(head, body, vars, distincts);
		if (Rule.orderedRefeq(rule, result)) result = rule;
		return Rule.canonize(result);
	}

	private Rule.Distinct handleUnseen(Rule.Distinct distinct) {
		Dob first = dobs.submerge(distinct.first);
		Dob second = dobs.submerge(distinct.second);
		if (first == distinct.first && second == distinct.second) return distinct;
		return new Rule.Distinct(first, second);
	}
}

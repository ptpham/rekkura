package rekkura.logic.structure;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.algorithm.Unifier;
import rekkura.logic.format.LogicFormat;
import rekkura.logic.format.StandardFormat;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.util.CachingSupplier;
import rekkura.util.Colut;
import rekkura.util.Submerger;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;

/**
 * A pool represents a set of dobs that can be compared 
 * with reference equality. Submerging a dob means to 
 * construct a corresponding dob such that all sub-trees
 * of the dob can be compared with reference equality against
 * all other dobs currently in the pool.
 * <br>
 * A pool assumes that the set of all dobs noted as variables in
 * the rules it maintains is disjoint of the set of all dobs that
 * are not noted as variables in the rules it maintains. By making
 * this assumption, it becomes unnecessary to recompute the union
 * of variable sets.
 * 
 * @author ptpham
 *
 */
public class Pool {
	public final CachingSupplier<Dob> vargen = new Dob.PrefixedSupplier("PGV");
	public final CachingSupplier<Dob> constgen = new Dob.PrefixedSupplier("PGC");
	
	public final LogicFormat fmt = new StandardFormat();
	public final Submerger<Dob> dobs = createDobSubmerger();
	public final Submerger<Atom> atoms = createAtomSubmerger();
	public final Submerger<Rule> rules = createRuleSubmerger();
	public final Set<Dob> allVars = vargen.created;
	
	private final Map<Class<?>, Submerger<?>> submergers = 
		ImmutableMap.of(Dob.class, dobs, Atom.class, atoms, Rule.class, rules);
		
	public Map<Dob, Dob> submergeUnify(Map<Dob, Dob> unify) {
		Map<Dob, Dob> result = Maps.newHashMap();
		
		for (Map.Entry<Dob, Dob> entry : unify.entrySet()) {
			Dob key = dobs.submerge(entry.getKey());
			Dob value = dobs.submerge(entry.getValue());
			result.put(key, value);
		}
		return result;
	}
	
	public <U,V> Map<U, V> submergeStrings(Map<String, String> raw, Class<U> key, Class<V> value) {
		Map<U, V> result = Maps.newHashMap();
		Submerger<U> keySub = getSubmerger(key);
		Submerger<V> valSub = getSubmerger(value);
		for (Map.Entry<String, String> entry : raw.entrySet()) {
			U u = keySub.submergeString(entry.getKey());
			V v = valSub.submergeString(entry.getValue());
			result.put(u, v);
		}
		return result;
	}
	
	public <U,V> Multimap<U, V> submergeStrings(Multimap<String, String> raw, Class<U> key, Class<V> value) {
		Multimap<U, V> result = HashMultimap.create();
		Submerger<U> keySub = getSubmerger(key);
		Submerger<V> valSub = getSubmerger(value);
		for (Map.Entry<String, String> entry : raw.entries()) {
			U u = keySub.submergeString(entry.getKey());
			V v = valSub.submergeString(entry.getValue());
			result.put(u, v);
		}
		return result;
	}
	
	public static Set<Rule> rulesWithHeadContainingAny(Set<Dob> targets, Iterable<Rule> rules) {
		Set<Rule> result = Sets.newHashSet();
		for (Rule rule : rules) {
			if (Colut.containsAny(rule.head.dob.fullIterable(), targets)) {
				result.add(rule);
			}
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private <U> Submerger<U> getSubmerger(Class<U> cls) {
		Preconditions.checkArgument(this.submergers.containsKey(cls), "Invalid submersion class target!");
		return (Submerger<U>)this.submergers.get(cls);
	}
	
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
		this.allVars.addAll(vars);
		
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

	public Multimap<Rule, Dob> submerge(Multimap<Rule, Dob> map) {
		Multimap<Rule, Dob> result = HashMultimap.create();
		for (Rule rule : map.keySet()) {
			for (Dob dob : map.get(rule)) {
				result.put(rules.submerge(rule), dobs.submerge(dob));
			}
		}
		return result;
	}
	
	public Dob render(Dob dob, Map<Dob,Dob> unify) {
		return dobs.submerge(Unifier.replace(dob, unify));
	}
	
	public Atom render(Atom atom, Map<Dob,Dob> unify) {
		return atoms.submerge(Unifier.replace(atom, unify));
	}
	
	public Rule render(Rule rule, Map<Dob,Dob> unify) {
		return rules.submerge(Unifier.replace(rule, unify, rule.vars));
	}
}

package rekkura.logic.algorithm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.model.Rule.Distinct;
import rekkura.logic.structure.Pool;
import rekkura.util.Colut;
import rekkura.util.OtmUtil;

import com.google.common.base.Supplier;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * This class is responsible for working with combinations of rules and atoms. <br>
 * @author ptpham
 *
 */
public class Comprender {

	/**
	 * Generates all rules that can be constructed by merging
	 * the rules in the path in order using the given operation
	 * @param path
	 * @param pool
	 * @param op
	 * @return
	 */
	public static List<Rule> mergeAll(List<Rule> path, Pool pool, Merge.Operation op) {
		List<Rule> result = Lists.newArrayList();
		if (path.size() < 2) {
			result.addAll(path);
			return result;
		}
		
		Rule src = Colut.first(path);
		List<Rule> compressed = mergeAll(Colut.slice(path, 1, path.size()), pool, op);
		for (Rule dst : compressed) result.addAll(Merge.applyOperation(src, dst, op, pool));
				
		return result;
	}
	
	/**
	 * 
	 * @param targets
	 * @param slicers
	 * @param pool
	 * @return keys are original rules and values are the rules generated from those rules
	 */
	public static Multimap<Rule, Rule> slice(Iterable<Rule> targets, Set<Rule> slicers, Pool pool) {
		Multimap<Rule, Rule> result = HashMultimap.create();
		Map<Set<Atom>, Rule> generated = Maps.newHashMap();
		
		for (Rule rule : targets) {
			if (slicers.contains(rule)) continue;
			
			Multimap<Dob,Dob> edges = Unifier.nonConflicting(Atom.asDobIterable(rule.body),
				Atom.asDobIterable(Rule.asHeadIterator(slicers)), pool);
			if (edges.size() == 0) continue;
			
			// Slice out the part of the rule covered by the statics
			List<Atom> bodyIntermediate = Lists.newArrayList();
			List<Atom> bodyTarget = Lists.newArrayList();
			for (Atom atom : rule.body) {
				if (edges.containsKey(atom.dob)) bodyIntermediate.add(atom);
				else bodyTarget.add(atom);
			}
			
			// Figure out if our slice is trivial
			if (bodyIntermediate.size() < 2) continue;
			Multimap<Dob,Dob> childToDob = HashMultimap.create();
			for (Atom atom : bodyIntermediate) OtmUtil.addAllEdges(childToDob, 
				atom.dob.childIterable(), Lists.newArrayList(atom.dob));
			
			boolean cartesian = true;
			for (Dob var : rule.vars) if (childToDob.get(var).size() > 1) cartesian = false;
			if (cartesian) continue;
			
			// Construct new rules given the slice
			Set<Atom> witness = Sets.newHashSet(bodyIntermediate);
			Rule intermediate = generated.get(witness);
			if (intermediate == null) {
				Rule rawIntermediate = Comprender.asRule(bodyIntermediate,
					rule.vars, rule.distinct, pool.constgen);
				intermediate = pool.rules.submerge(rawIntermediate);
				pool.rules.submerge(rawIntermediate);
				generated.put(witness, intermediate);
			}
			
			bodyTarget.add(intermediate.head);
			Rule rawTarget = new Rule(rule.head, bodyTarget, rule.vars, rule.distinct);
			
			result.put(rule, intermediate);
			result.put(rule, pool.rules.submerge(rawTarget));
		}
		
		return result;
	}
	
	public static Rule lift(Rule src, Rule dst, int dstPosition, Pool pool) {
		Merge.Result merge = Merge.compute(src, dst, dstPosition, pool);
		if (merge == null) return null;
		
		int groundings = 0;
		for (Dob var : merge.srcUnify.keySet()) {
			if (!pool.allVars.contains(merge.srcUnify.get(var))) groundings++;
		} if (groundings == 0) return null;
				 
		return Unifier.replace(src, merge.srcUnify, merge.vars);
	}
	
	public static List<Rule> lift(Rule src, Rule dst, Pool pool) {
		List<Rule> result = Lists.newArrayList();
		for (int i = 0; i < dst.body.size(); i++) {
			Rule lifted = lift(src, dst, i, pool);
			if (lifted != null) result.add(lifted);
		}
		return result;
	}
	
	public static Rule asRule(Iterable<Atom> atoms, Collection<Dob> allVars,
		Iterable<Distinct> distincts, Supplier<Dob> ground) {
		Set<Dob> vars = Rule.getVariablesOf(atoms, allVars);
		
		Set<Dob> diff = Colut.difference(allVars, vars);
		List<Distinct> filtered = Distinct.keepUnrelated(distincts, diff);
		
		Dob varDob = new Dob(Lists.newArrayList(vars));
		Atom head = new Atom(new Dob(ground.get(), varDob), true);
		
		return new Rule(head, atoms, vars, filtered);
	}
}

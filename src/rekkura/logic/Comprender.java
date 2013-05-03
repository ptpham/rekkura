package rekkura.logic;

import java.util.List;
import java.util.Set;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Colut;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * This class is responsible for working with combinations of rules. <br>
 * - mergeRules(src, dst): incorporate the terms in src into dst using
 * naive de Morgan to resolve negations. Merging all paths in a set of 
 * rules will not preserve negations failure. 
 * <br>
 * - pushVars(src, dst): generate the unification that assigns variables
 * in src to dobs in dst <br>
 * - pullRule(src, dst): generate a copy of src that is partially 
 * grounded by information in dst <br>
 * @author ptpham
 *
 */
public class Comprender {

	/**
	 * Generates all rules that can be constructed by merging
	 * the rules in the path in order.
	 * @param path
	 * @return
	 */
	public static List<Rule> mergeAll(List<Rule> path, Pool pool) {
		List<Rule> result = Lists.newArrayList();
		if (path.size() < 2) {
			result.addAll(path);
			return result;
		}
		
		Rule src = Colut.first(path);
		List<Rule> compressed = mergeAll(Colut.slice(path, 1, path.size()), pool);
		for (Rule dst : compressed) result.addAll(mergeRules(src, dst, pool));
				
		return result;
	}
	
	/**
	 * This method will return all possible ways that the head of 
	 * the source rule might fit into the body of the destination rule.
	 * @param src
	 * @param dst
	 * @return
	 */
	public static List<Rule> mergeRules(Rule src, Rule dst, Pool pool) {
		List<Rule> result = Lists.newArrayList();
		
		for (int i = 0; i < dst.body.size(); i++) {
			Merger.Request req = new Merger.Request(src, dst, i);
			Merger.Result merge = Merger.compute(req, pool);
			if (merge == null) continue;
			
			// Apply the unifications to their respective rules
			Set<Dob> varUnion = Sets.newHashSet();
			varUnion.addAll(src.vars);
			varUnion.addAll(dst.vars);
			
			Rule srcFixed = pool.rules.submerge(Unifier.replace(src, merge.srcUnify, varUnion));
			Rule dstFixed = pool.rules.submerge(Unifier.replace(dst, merge.dstUnify, varUnion));
			
			// Construct the body separately because the unification replace may have changed
			// the canonical ordering of the terms in the destination body
			List<Atom> fixedBody = Unifier.replace(dst, merge.dstUnify, varUnion).body;
			List<Atom> dstFilteredBody = Colut.filterAt(fixedBody, req.dstPosition);
			List<Atom> dstFixedBody = pool.atoms.submerge(dstFilteredBody);
			
			boolean pivotTruth = dst.body.get(req.dstPosition).truth;
			List<List<Atom>> bodies = generateMergeBodies(srcFixed, dstFixedBody, pivotTruth);
			
			// Construct the variables for the new rule. This set
			// should contain all of the vars in the fixed source, 
			// and all of the vars that were left untouched in the destination.
			Set<Dob> vars = Sets.newHashSet(dstFixed.vars);
			vars.removeAll(merge.dstUnify.keySet());
			vars.addAll(srcFixed.vars);
			
			for (List<Atom> body : bodies) {
				Rule merged = new Rule(dstFixed.head, body, vars);
				result.add(merged);
			}
		}
		
		return result;
	}

	private static List<List<Atom>> generateMergeBodies(Rule src, 
		List<Atom> dstBody, boolean pivotTruth) {
		
		List<List<Atom>> bodies = Lists.newArrayList();
		if (pivotTruth) {
			List<Atom> body = Lists.newArrayList(dstBody);
			body.addAll(src.body);
			bodies.add(body);
			return bodies;
		}
		
		// Use naive de Morgan's rule here. Application of 
		// de Morgan's rule here is an approximation because
		// there may be higher order relationships across terms.
		for (Atom term : src.body) {
			List<Atom> body = Lists.newArrayList(dstBody);
			body.add(new Atom(term.dob, !term.truth));
			bodies.add(body);
		}
		return bodies;
	}
}

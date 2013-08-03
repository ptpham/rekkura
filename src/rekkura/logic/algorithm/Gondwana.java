package rekkura.logic.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.model.Unification;
import rekkura.util.Cartesian;
import rekkura.util.Cartesian.AdvancingIterator;
import rekkura.util.Colut;
import rekkura.util.DualMultimap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class Gondwana {
	
	public static DualMultimap<Dob,List<Dob>> termOverlaps(Iterable<Dob> terms, List<Dob> vars) {
		DualMultimap<Dob,List<Dob>> result = DualMultimap.create();
		
		for (Dob first : terms) {
			List<Dob> outer = Lists.newArrayList(first.fullIterable());
			for (Dob second : terms) {
				Set<Dob> overlap = Colut.intersection(second.fullIterable(), outer);
				if (overlap.size() == 0) continue;
				List<Dob> subset = Colut.select(vars, overlap);
				result.putDual(first, subset);
				result.putDual(second, subset);
			}
		}
		
		return result;
	}
	
	public static Cartesian.AdvancingIterator<Unification> getLinearJoinIterator(Rule rule, 
			List<Atom> expanders, Multimap<Atom, Dob> support, Set<Dob> truths) {
		AdvancingIterator<Unification> result = getLinearJoinInternal(rule, expanders, support, truths);
		if (result == null) return Terra.getBodySpaceIterator(rule, expanders, support, truths);
		return result;
	}
	
	private static AdvancingIterator<Unification> getLinearJoinInternal(Rule rule, 
		List<Atom> expanders, Multimap<Atom, Dob> support, Set<Dob> truths) {

		// If we have a cover with a single term, we are not interested
		// in doing the linear join dance.
		List<Atom> positives = Atom.filterPositives(rule.body);
		Terra.sortBySupportSize(positives, support);
		if (expanders == null || expanders.size() < 2) return null;
		List<List<Dob>> schemas = linearOverlaps(expanders, rule.vars);
		return generateUnificationSpace(rule, support, expanders, schemas);
	}

	private static AdvancingIterator<Unification> generateUnificationSpace(
			Rule rule, Multimap<Atom, Dob> support, List<Atom> cover,
			List<List<Dob>> schemas) {
		Map<Map<Dob,Dob>, Unification> bridges = buildLinearBridges(rule, support, cover, schemas);
		Multimap<Unification,Unification> space = HashMultimap.create();
		Map<Dob,Unification> last = Maps.newHashMap();
		List<Unification> roots = Lists.newArrayList();
		for (int i = 1; i < cover.size(); i++) {
			List<Dob> schema = schemas.get(i-1);
			Atom first = cover.get(i-1);
			Atom second = cover.get(i);
			
			for (Dob ground : support.get(first)) {
				Map<Dob,Dob> unify = Unifier.unify(first.dob, ground);
				if (unify == null) continue;
				Map<Dob,Dob> retained = Colut.retainAll(schema, unify);
				Unification full = last.get(ground);
				if (full == null) full = Unification.from(unify, rule.vars);
				space.put(full, bridges.get(retained));
				if (i == 1) roots.add(full);
			}

			last.clear();
			for (Dob ground : support.get(second)) {
				Map<Dob,Dob> unify = Unifier.unify(second.dob, ground);
				if (unify == null) continue;
				Map<Dob,Dob> retained = Colut.retainAll(schema, unify);
				Unification full = Unification.from(unify, rule.vars);
				space.put(bridges.get(retained), full);
				last.put(ground, full);
			}
		}

		int depth = 2*cover.size() - 1;
		AdvancingIterator<Unification> iterator = Cartesian.asIterator(space, roots, depth);
		return iterator;
	}

	private static Map<Map<Dob,Dob>, Unification> buildLinearBridges(Rule rule,
			Multimap<Atom, Dob> support, List<Atom> cover,
			List<List<Dob>> expanders) {
		Map<Map<Dob,Dob>, Unification> bridges = Maps.newHashMap();
		for (int i = 1; i < cover.size(); i++) {
			Atom first = cover.get(i);
			List<Dob> schema = expanders.get(i-1);
			
			Set<Map<Dob,Dob>> uniques = Sets.newHashSet();
			for (Dob ground : support.get(first)) {
				Map<Dob,Dob> unify = Unifier.unify(first.dob, ground);
				if (unify == null) continue;
				unify = Colut.retainAll(schema, unify);
				uniques.add(unify);
			}
			
			for (Map<Dob,Dob> unify : uniques) {
				bridges.put(unify, Unification.from(unify, rule.vars));
			}
		}
		return bridges;
	}

	private static List<List<Dob>> linearOverlaps(List<Atom> targets, List<Dob> vars) {
		List<List<Dob>> result = Lists.newArrayList();
		for (int i = 1; i < targets.size(); i++) {
			Dob first = targets.get(i).dob, second = targets.get(i-1).dob;
			List<Dob> fullSecond = Lists.newArrayList(second.fullIterable());
			Set<Dob> overlap = Colut.intersection(first.fullIterable(), fullSecond);
			List<Dob> subset = Colut.select(vars, overlap);
			result.add(subset);
		}
		
		return result;
	}
}
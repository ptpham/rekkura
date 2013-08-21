
package rekkura.logic.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.model.Unification;
import rekkura.logic.structure.Pool;
import rekkura.util.Cartesian;
import rekkura.util.Colut;
import rekkura.util.OtmUtil;

import com.google.common.collect.*;

public abstract class Expansion {
	public abstract Set<Dob> expand(Rule rule, Set<Dob> truths, Multimap<Atom,Dob> support, Pool pool);
	
	public static class Counters {
		public int unifies;
	}
	
	public final Counters counters = new Counters();
	public static Standard getStandard() { return new Standard(); }
	public static Partitioning getPartitioning() { return new Partitioning(); }
	
	public static class Standard extends Expansion {
		@Override public Set<Dob> expand(Rule rule,
			Set<Dob> truths, Multimap<Atom, Dob> support, Pool pool) {
			return standard(rule, truths, support, pool, counters);
		}
	}
	
	public static class Partitioning extends Expansion {
		
		@Override
		public Set<Dob> expand(Rule rule, Set<Dob> truths, Multimap<Atom, Dob> support, Pool pool) {
			List<Atom> positives = Atom.filterPositives(rule.body);
			List<List<Unification>> space = Terra.getUnificationSpace(rule, support, positives);

			// Partition the space based on the greedy ordering of high overlap variables
			List<Dob> candidates = Terra.getPartitionCandidates(Atom.asDobIterable(positives), rule.vars);
			List<List<List<Unification>>> partitions = partitionSpace(rule.vars, candidates, space);
			
			// Do the final rendering
			Set<Dob> result = Sets.newHashSet();
			for (List<List<Unification>> partition : partitions) {
				Map<Atom,Integer> sizes = Maps.newHashMap();
				for (int i = 0; i < positives.size(); i++) sizes.put(positives.get(i), partition.get(i).size());
				
				List<Atom> expanders = Terra.getGreedyVarCoverExpanders(rule, sizes);
				List<Atom> check = Colut.remove(rule.body, expanders);

				Cartesian.AdvancingIterator<Unification> iterator = Cartesian.asIterator(partition);
				result.addAll(applyAndRender(rule, iterator, check, pool, truths));
			}
				
			return result;
		}
		
	}
	
	public static Multimap<Atom,Dob> getTrivialSupport(Rule rule, Set<Dob> truths) {
		Multimap<Atom,Dob> result = HashMultimap.create();
		for (Atom atom : rule.body) {
			if (!atom.truth) continue;
			result.putAll(atom, truths);
		}
		return result;
	}
	
	public static Set<Dob> standard(Rule rule, Set<Dob> truths, Multimap<Atom,Dob> support, Pool pool) {
		return standard(rule, truths, support, pool, null);
	}
	
	private static Set<Dob> standard(Rule rule, Set<Dob> truths, 
		Multimap<Atom,Dob> support, Pool pool, Counters counters) {		
		Map<Atom,Integer> sizes = OtmUtil.getNumValues(support);
		
		List<Atom> expanders = Terra.getGreedyVarCoverExpanders(rule, sizes);
		List<Atom> check = Colut.remove(rule.body, expanders);
		Cartesian.AdvancingIterator<Unification> iterator =
			Terra.getUnificationIterator(rule, expanders, support, truths);
		
		Set<Dob> result = applyAndRender(rule, iterator, check, pool, truths);
		if (counters != null) counters.unifies += iterator.traversed();
		return result;
	}
	
	protected static List<List<List<Unification>>> partitionSpace(
		List<Dob> vars, List<Dob> candidates, List<List<Unification>> space) {
		List<List<List<Unification>>> result = Lists.newArrayList();

		// Index and find a atom that we can use as a guide
		int pos = -1;
		Set<Dob> guide = null;
		while (candidates.size() > 0 && guide == null) {
			pos = vars.indexOf(Colut.popAny(candidates));
			for (int i = 0; i < space.size(); i++) {
				Multiset<Dob> uniques = HashMultiset.create();
				for (Unification unify : space.get(i)) uniques.add(unify.assigned[pos]);
				if (uniques.elementSet().size() > 1) {
					guide = uniques.elementSet();
					break;
				}
			}
		}
		
		// If we could not partition the space (i.e. could not find a guide),
		// just return the full space
		if (guide == null) { result.add(space); return result; }
		
		// Expand each assignment defined by the guide
		for (Dob assign : guide) {
			List<List<Unification>> partitioned = Lists.newArrayList();
			for (int i = 0; i < space.size(); i++) {
				List<Unification> slice = null;
				ListMultimap<Dob, Unification> index = Terra.indexBy(space.get(i), pos);
				if (index.containsKey(assign)) slice = index.get(assign);
				else slice = index.get(null);
				partitioned.add(slice);
			}
			
			// Recursive expansion to handle the other variables
			List<List<List<Unification>>> children =
				partitionSpace(vars, Lists.newArrayList(candidates), partitioned);
			for (List<List<Unification>> child : children) result.add(child);
		}
		return result;
	}
	
	protected static Set<Dob> applyAndRender(Rule rule, Cartesian.AdvancingIterator<Unification> iterator,
		List<Atom> check, Pool pool, Set<Dob> truths) {
		List<Map<Dob,Dob>> unifies = Terra.applyUnifications(rule, iterator, check, pool, truths);
		return Terra.renderHeads(unifies, rule, pool);
	}

}

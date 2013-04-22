package rekkura.logic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.fmt.LogicFormat;
import rekkura.fmt.StandardFormat;
import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
	Map<String, Dob> dobMap = Maps.newHashMap();
	Set<Dob> known = Sets.newHashSet();
	
	public Dob submerge(Dob dob) {
		if (known.contains(dob)) return dob;
		
		String stringed = fmt.toString(dob);
		Dob existing = dobMap.get(stringed);
		if (existing == null) {
			existing = handleUnseen(dob);
			dobMap.put(stringed, existing);
		}
		
		this.known.add(existing);
		return existing;
	}

	public Atom submerge(Atom atom) {
		return new Atom(submerge(atom.dob), atom.truth);
	}
	
	public List<Dob> submergeDobs(Collection<Dob> source) {
		List<Dob> result = Lists.newArrayListWithCapacity(source.size());
		for (Dob dob : source) { result.add(submerge(dob)); }
		return result;
	}
	
	public List<Atom> submergeAtoms(Collection<Atom> source) {
		List<Atom> result = Lists.newArrayListWithCapacity(source.size());
		for (Atom atom : source) { result.add(submerge(atom)); }
		return result;
	}
	
	public Rule submerge(Rule rule) {
		List<Rule.Distinct> distincts = Lists.newArrayList();
		for (Rule.Distinct distinct : rule.distinct) {
			distincts.add(new Rule.Distinct(submerge(distinct.first), 
					submerge(distinct.second)));
		}
		
		return new Rule(
			submerge(rule.head),
			submergeAtoms(rule.body),
			Sets.newHashSet(submergeDobs(rule.vars)),
			distincts);
	}

	private Dob handleUnseen(Dob dob) {
		boolean changed = false;
		List<Dob> newChildren = Lists.newArrayListWithCapacity(dob.size());
		for (int i = 0; i < dob.size(); i++) {
			Dob child = dob.at(i);
			Dob submerged = submerge(child);
			if (child != submerged) changed = true;
			newChildren.add(submerged);
		}
		
		if (changed) return new Dob(newChildren);
		return dob;
	}

	public List<Rule> submergeRules(Collection<Rule> path) {
		List<Rule> result = Lists.newArrayList();
		for (Rule rule : path) { result.add(submerge(rule)); }
		return result;
	}
}

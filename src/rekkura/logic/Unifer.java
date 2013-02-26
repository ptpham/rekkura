package rekkura.logic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.model.Dob;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Unifer {
	
	public Dob replace(Dob base, Map<Dob, Dob> substitution) {
		if (substitution.containsKey(base)) return substitution.get(base);
		
		boolean changed = false;
		List<Dob> newChildren = Lists.newArrayListWithCapacity(base.size());
		for (int i = 0; i < base.size(); i++) {
			Dob child = base.at(i);
			Dob replaced = replace(child, substitution);
			if (child != replaced) changed = true;
			newChildren.add(replaced);
		}
		
		if (changed) return new Dob(newChildren);
		return base;
	}
	
	private boolean unify(Dob base, Dob target, Map<Dob, Dob> current) {
		boolean mismatch = false;
		if (base.size() != target.size()) mismatch = true;
		else if(base.isTerminal() && target.isTerminal()
				&& !base.name.equals(target.name)) mismatch = true;
		
		if (mismatch) {
			Dob existing = current.get(base);
			if (existing != null && existing != target) return false;
			current.put(base, target);
		} else if (base != target) {
			for (int i = 0; i < base.size(); i++) {
				if(!unify(base.at(i), target.at(i), current)) return false;
			}
		}
		
		return true;
	}

	public Map<Dob, Dob> unify(Dob base, Dob target) {
		Map<Dob, Dob> result = Maps.newHashMap();
		if (!unify(base, target, result)) return null;
		return result;
	}
	
	public Map<Dob, Dob> unifyVars(Dob base, Dob target, Set<Dob> vars) {
		Map<Dob, Dob> result = unify(base, target);
		if (!vars.containsAll(result.keySet())) return null;
		return result;
	}
}

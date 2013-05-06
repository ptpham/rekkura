package rekkura.logic;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import rekkura.model.Dob;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class Latticer {

	public static Multimap<Dob, Dob> detect(Dob first, Dob second, Dob base, Collection<Dob> grounds) {
		List<Dob> vars = Lists.newArrayList(first, second);
		List<Map<Dob, Dob>> unifications = Lists.newArrayList();
		for (Dob ground : grounds) {
			Map<Dob, Dob> unify = Unifier.unifyVars(base, ground, vars);
			if (unify != null) unifications.add(unify);
		}
		
		return Topper.extractGraph(first, second, unifications);
	}
	
}

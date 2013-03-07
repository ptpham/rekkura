package rekkura.fmt;

import java.util.Collection;
import java.util.List;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Lists;

public abstract class LogicFormat {

	public abstract String toString(Dob dob);
	public abstract Dob dobFromString(String s);

	public abstract String toString(Atom atom);
	public abstract Atom atomFromString(String s);

	public abstract String toString(Rule rule);
	public abstract Rule ruleFromString(String s);
	
	public List<Rule> rulesFromStrings(Collection<String> strings) {
		List<Rule> result = Lists.newArrayList();
		for (String s : strings) { result.add(ruleFromString(s)); }
		return result;
	}
	
	public List<Atom> atomsFromStrings(Collection<String> strings) {
		List<Atom> result = Lists.newArrayList();
		for (String s : strings) { result.add(atomFromString(s)); }
		return result;
	}
	
	public List<Dob> dobsFromStrings(Collection<String> strings) {
		List<Dob> result = Lists.newArrayList();
		for (String s : strings) { result.add(dobFromString(s)); }
		return result;
	}
}
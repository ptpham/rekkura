package rekkura.fmt;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
	
	public abstract String toString(Rule.Distinct distinct);
	
	public Rule canonize(Rule rule) {
		List<Dob> vars = Lists.newArrayList(rule.vars);
		List<Atom> body = Lists.newArrayList(rule.body);
		
		Collections.sort(vars, new Comparator<Dob>() {
			@Override public int compare(Dob arg0, Dob arg1) {
				String first = LogicFormat.this.toString(arg0);
				String second = LogicFormat.this.toString(arg1);
				return first.compareTo(second);
			}
		});
		
		Collections.sort(body, new Comparator<Atom>() {
			@Override public int compare(Atom arg0, Atom arg1) {
				if (arg0.truth && !arg1.truth) return -1;
				if (!arg0.truth && arg1.truth) return 1;
				String first = LogicFormat.this.toString(arg0);
				String second = LogicFormat.this.toString(arg1);
				return first.compareTo(second);
			}
		});
		
		return new Rule(rule.head, body, vars, rule.distinct);
	}
	
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
	
	public List<String> rulesToStrings(Collection<Rule> rules) {
		List<String> result = Lists.newArrayList();
		for (Rule rule : rules) { result.add(toString(rule)); }
		return result;
	}
	
	public List<String> atomsToStrings(Collection<Atom> atoms) {
		List<String> result = Lists.newArrayList();
		for (Atom atom : atoms) { result.add(toString(atom)); }
		return result;
	}
	
	public List<String> dobsToStrings(Collection<Dob> dobs) {
		List<String> result = Lists.newArrayList();
		for (Dob dob : dobs) { result.add(toString(dob)); }
		return result;
	}
}
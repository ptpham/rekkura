package rekkura.fmt;

import java.util.List;
import java.util.Set;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Cartesian;
import rekkura.util.Colut;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Parsing code is the bane of my existence.
 * @author "ptpham"
 *
 */
public class KifFormat extends LogicFormat {

	public static final String DISTINCT_NAME = "distinct";
	public static final String OR_NAME = "or";
	
	@Override
	public String toString(Dob dob) {
		StringBuilder builder = new StringBuilder();
		append(dob, builder);
		return builder.toString();
	}
	
	private void append(Dob dob, StringBuilder builder) {
		if (dob.isTerminal()) {
			builder.append(dob.name);
			return;
		}
		
		builder.append('(');
		for (int i = 0; i < dob.size(); i++) {
			append(dob.at(i), builder);
			if (i < dob.size() - 1) builder.append(' ');
		}
		builder.append(')');
	}

	@Override
	public Dob dobFromString(String s) {
		s = s.replace("(", " (");
		s = s.trim();
		
		if (!s.startsWith("(") || !s.endsWith(")")) return new Dob(s);
		
		s = s.substring(1, s.length() - 1);
		
		List<Dob> children = Lists.newArrayList();
		String[] parts = s.split("\\s+");
		StringBuilder current = new StringBuilder();
		int nest = 0;
		for (String part : parts) {
			current.append(part);
			current.append(' ');
			
			for (char c : part.toCharArray()) {
				if (c == '(') nest++;
				if (c == ')') nest--;
			}
			
			if (nest == 0) {
				String raw = current.toString().trim();
				if (raw.length() == 0) continue;
				children.add(dobFromString(current.toString()));
				current = new StringBuilder();
			}
		}
		
		return new Dob(children);
	}

	@Override
	public String toString(Atom atom) {
		if (atom.truth) return toString(atom.dob);
		return "(not" + toString(atom.dob) + ")";
	}

	@Override
	public Atom atomFromString(String s) {
		Dob dob = dobFromString(s);
		while (dob.size() == 1) dob = dob.at(0);
		
		Dob first = dob.size() > 0 ? dob.at(0) : null;
		if (first != null && first.isTerminal() 
				&& first.name.equals("not")) {
			Dob second = dob.at(1);
			return new Atom(second, false);
		} else return new Atom(dob, true);
	}

	@Override
	public String toString(Rule rule) {
		Preconditions.checkArgument(rule.head.truth);
		StringBuilder distinct = new StringBuilder();
		if (rule.distinct.size() > 0) distinct.append(' ');
		for (Rule.Distinct pair : rule.distinct) {
			appendDistinct(distinct, pair);
		}
		
		return "(<= " + toString(rule.head) + " " 
				+ Joiner.on(" ").join(atomsToStrings(rule.body)) + 
				distinct.toString() + ")";
	}
	
	@Override
	public String toString(Rule.Distinct distinct) {
		StringBuilder builder = new StringBuilder();
		appendDistinct(builder, distinct);
		return builder.toString();
	}

	private void appendDistinct(StringBuilder distinct, Rule.Distinct pair) {
		distinct.append("(distinct ");
		this.append(pair.first, distinct);
		distinct.append(' ');
		this.append(pair.second, distinct);
		distinct.append(")");
	}

	@Override
	public Rule ruleFromString(String s) {
		Dob raw = dobFromString(s);
		
		Preconditions.checkArgument(raw.at(0).isTerminal());
		Preconditions.checkArgument(raw.at(0).name.equals("<="));
		
		Rule result = new Rule();
		result.head = new Atom(raw.at(1), true);
		
		List<Dob> body = Colut.slice(raw.childCopy(), 2, raw.size());
		for (Dob elem : body) {
			if (elem.size() == 3 && elem.at(0).name.equals(DISTINCT_NAME)) {
				result.distinct.add(new Rule.Distinct(elem.at(1), elem.at(2)));
				continue;
			}
			
			Atom converted = atomFromString(toString(elem));
			result.body.add(converted); 
		}
		
		Iterable<Dob> terms = Rule.dobIterableFromRule(result);
		Set<String> seen = Sets.newHashSet();
		for (Dob value : Dob.fullIterable(terms)) {
			String name = value.name;
			if (!value.isTerminal()) continue;
			if (name.length() < 2) continue;
			if (name.charAt(0) != '?') continue;
			if (seen.contains(name)) continue;
			result.vars.add(value);
			seen.add(name);
		}
		
		return result;
	}

	public List<Rule> deor(Rule rule) {
		List<List<Atom>> termLists = Lists.newArrayList();
		for (Atom term : rule.body) {
			Dob dob = term.dob;
			Dob firstChild = dob.at(0);
			List<Atom> alternates = Lists.newArrayList();
			if (firstChild == null || !firstChild.name.equals(OR_NAME)) {
				alternates.add(term);
			} else {
				for (int i = 1; i < dob.size(); i++) {
					alternates.add(new Atom(dob.at(i), term.truth));
				}
			}
			termLists.add(alternates);
		}

		List<Rule> result = Lists.newArrayList();
		for (List<Atom> body : Cartesian.asIterable(termLists)) {
			Rule flattened = new Rule(rule);
			flattened.body = body;
			result.add(flattened);
		}
		
		// Vacuous rules will have a body space of size 0 ...
		if (result.size() == 0) result.add(rule);
		return result;
	}
}

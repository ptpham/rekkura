package rekkura.logic.format;

import java.util.List;
import java.util.Set;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.util.Cartesian;
import rekkura.util.Colut;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Parsing code is the bane of my existence.
 * This code is not very good for a variety of reasons.
 * The most important one is that it doesn't implement a one to
 * one mapping between dob and string: <b>gasp</b>
 * <br> <br>
 * If an internal node in a dob has exactly one child, that 
 * child will be attached to the node's parent. This was
 * necessary for some shamefully hacky reason. However, I have
 * little respect for KIF and this class exists for interop
 * with the outside world. Therefore, I don't care.
 * @author "ptpham"
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
		
		if (children.size() == 1) return children.get(0);
		return new Dob(children);
	}

	@Override
	public String toString(Atom atom) {
		if (atom.truth) return toString(atom.dob);
		return "(not " + toString(atom.dob) + ")";
	}

	@Override
	public Atom atomFromString(String s) {
		Dob dob = dobFromString(s);
		while (dob.size() == 1) dob = dob.at(0);
		
		Dob first = dob.size() > 0 ? dob.at(0) : null;
		if (first != null && first.isTerminal() 
				&& first.name.equals("not")) {
			Dob second = dob.at(1);
			if (second == null) throw new Error("Kif parse error: " + s);
			return new Atom(second, false);
		} else return new Atom(dob, true);
	}

	@Override
	public String toString(Rule rule) {
		Preconditions.checkArgument(rule.head.truth);
		
		List<String> terms = Lists.newArrayList(toString(rule.head));
		terms.addAll(atomsToStrings(rule.body));
		for (Rule.Distinct pair : rule.distinct) {
			terms.add(toString(pair));
		}
		
		return "(<= " + Joiner.on(" ").join(terms) + ")";
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
		
		Atom head = new Atom(raw.at(1), true);
		
		List<Dob> bodyDobs = Colut.slice(raw.childCopy(), 2, raw.size());
		List<Atom> body = Lists.newArrayList();
		List<Rule.Distinct> distincts = Lists.newArrayList();
		for (Dob elem : bodyDobs) {
			if (elem.size() == 3 && elem.at(0).name.equals(DISTINCT_NAME)) {
				distincts.add(new Rule.Distinct(elem.at(1), elem.at(2)));
				continue;
			}
			
			Atom converted = atomFromString(toString(elem));
			body.add(converted); 
		}
		
		List<Dob> terms = Lists.newArrayList(bodyDobs);
		terms.add(head.dob);
		
		Set<String> seen = Sets.newHashSet();
		List<Dob> vars = Lists.newArrayList();
		for (Dob value : Dob.fullIterable(terms)) {
			String name = value.name;
			if (!value.isTerminal()) continue;
			if (name.length() < 2) continue;
			if (name.charAt(0) != '?') continue;
			if (seen.contains(name)) continue;
			vars.add(value);
			seen.add(name);
		}
		
		return new Rule(head, body, vars, distincts);
	}

	public List<Rule> deor(Rule rule) {
		List<List<Atom>> termLists = Lists.newArrayList();
		for (Atom term : rule.body) {
			Dob dob = term.dob;
			Dob firstChild = dob.size() > 0 ? dob.at(0) : null;
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
			Rule flattened = new Rule(rule.head, body, rule.vars, rule.distinct);
			result.add(flattened);
		}
		
		// Vacuous rules will have a body space of size 0 ...
		if (result.size() == 0) result.add(rule);
		return result;
	}
	
	/**
	 * This converts dobs in Kif format to their proper rule representations.
	 * This needs to happen because Rekkura does not internally represent a
	 * distinction between a ground relation (proposition) and a rule in a 
	 * game description.
	 * @param rawRules
	 * @return
	 */
	public static List<Rule> dobsToRules(List<Dob> rawRules) {
		KifFormat fmt = KifFormat.inst;
		List<Rule> rules = Lists.newArrayList();
		for (Dob rawRule : rawRules) { 
			try { rules.add(fmt.ruleFromString(fmt.toString(rawRule))); }
			catch (Exception e) { rules.add(Rule.asVacuous(rawRule)); }
		}
		
		rules = deorPass(rules);
		return rules;
	}

	/**
	 *  In a glorious future in which we don't have ORs anymore,
	 *  this class could be made more general and this could be removed.
	 * @param rules
	 * @return
	 */
	public static List<Rule> deorPass(List<Rule> rules) {
		KifFormat fmt = KifFormat.inst;
		List<Rule> result = Lists.newArrayList();
		for (Rule rule : rules) {
			for (Rule expanded : fmt.deor(rule)) {
				Rule cleaned = fmt.ruleFromString(fmt.toString(expanded));
				result.add(cleaned);
			}
		}
		return result;
	}
	
	public static final KifFormat inst = new KifFormat();
}

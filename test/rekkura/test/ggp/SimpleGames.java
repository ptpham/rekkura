package rekkura.test.ggp;

import java.util.Arrays;
import java.util.List;

import rekkura.fmt.KifFormat;
import rekkura.fmt.StandardFormat;
import rekkura.ggp.net.GgpProtocol;
import rekkura.model.Rule;

import com.google.common.collect.Lists;

public class SimpleGames {

	public static List<Rule> getTrivial() {
		List<String> rules = Lists.newArrayList(
			"{|<((role)(robot)),true>:-}",
			"{|<((init)(p)),true>:-}",
			"{|<((legal)(robot)(sing)),true>:-}",
			"{|<((legal)(robot)(noop)),true>:-}",
			"{|<((next)(q)),true>:-<((true)(p)),true> <((does)(robot)(sing)),true>}",
			"{|<((next)(z)),true>:-<((true)(p)),true> <((does)(robot)(sing)),false>}",
			"{|<(terminal),true>:-<((true)(q)),true>}",
			"{|<(terminal),true>:-<((true)(z)),true>}",
			"{|<((goal)(p)(100)),true>:-<((true)(q)),true>}",
			"{|<((goal)(p)(0)),true>:-<((true)(z)),true>}"
		);
		return StandardFormat.inst.rulesFromStrings(rules);
	}
	
	public static List<Rule> getTicTacToe() {
		List<String> rules = Lists.newArrayList(
		      "{|<((role)(x)),true>:-}",
		      "{|<((role)(o)),true>:-}",
		      "{|<((index)(1)),true>:-}",
		      "{|<((index)(2)),true>:-}",
		      "{|<((index)(3)),true>:-}",
		      "{|<((init)((control)(x))),true>:-}",
		      "{(X)(Y)|<((init)((cell)(empty)(X)(Y))),true>:-<((index)(X)),true><((index)(Y)),true>}",
		      "{(R)(X)(Y)|<((legal)(R)((mark)(X)(Y))),true>:-<((true)((control)(R))),true> <((true)((cell)(empty)(X)(Y))),true>}",
		      "{(R)|<((legal)(R)(noop)),true>:-<((role)(R)),true><((true)((control)(R))),false>}",
		      "{(R)(X)(Y)|<((next)((cell)(R)(X)(Y))),true>:-<((does)(R)((mark)(X)(Y))),true>}",
		      "{|<((next)((control)(x))),true>:-<((true)((control)(o))),true>}",
		      "{|<((next)((control)(o))),true>:-<((true)((control)(x))),true>}",
		      "{(X)(Y)(R)|<((touched)(X)(Y)),true>:-<((does)(R)((mark)(X)(Y))),true>}",
		      "{(S)(X)(Y)|<((next)((cell)(S)(X)(Y))),true>:-<((true)((cell)(S)(X)(Y))),true><((touched)(X)(Y)),false>}",
		      "{(R)(X)|<((line)(R)(X)),true>:-<((role)(R)),true><((true)((cell)(R)(X)(1))),true><((true)((cell)(R)(X)(2))),true><((true)((cell)(R)(X)(3))),true>}",
		      "{(R)(X)|<((line)(R)(X)),true>:-<((role)(R)),true><((true)((cell)(R)(1)(X))),true><((true)((cell)(R)(2)(X))),true><((true)((cell)(R)(3)(X))),true>}",
		      "{(R)|<((diag)(R)),true>:-<((role)(R)),true><((true)((cell)(R)(1)(1))),true><((true)((cell)(R)(2)(2))),true><((true)((cell)(R)(3)(3))),true>}",
		      "{(R)|<((diag)(R)),true>:-<((role)(R)),true><((true)((cell)(R)(3)(1))),true><((true)((cell)(R)(2)(2))),true><((true)((cell)(R)(1)(3))),true>}",
		      "{(R)(X)|<((win)(R)),true>:-<((line)(R)(X)),true>}",
		      "{(R)|<((win)(R)),true>:-<((diag)(R)),true>}",
		      "{(R)|<((goal)(R)(100)),true>:-<((win)(R)),true>}",
		      "{(R)|<((goal)(R)(0)),true>:-<((role)(R)),true><(terminal),true><((win)(R)),false>}",
		      "{(R)|<(terminal),true>:-<((win)(R)),true>}",
		      "{(X)(Y)|<(empty),true>:-<((true)((cell)(empty)(X)(Y))),true>}",
		      "{|<(terminal),true>:-<(empty),false>}"
		);
		return StandardFormat.inst.rulesFromStrings(rules);
	}
	
	public static List<Rule> getConnectFour() {
		String[] raw = {
				"(role white)", "(role black)",
				"(init (cell 1 1 b))", "(init (cell 1 2 b))",
				"(init (cell 1 3 b))", "(init (cell 1 4 b))",
				"(init (cell 1 5 b))", "(init (cell 1 6 b))",
				"(init (cell 2 1 b))", "(init (cell 2 2 b))",
				"(init (cell 2 3 b))", "(init (cell 2 4 b))",
				"(init (cell 2 5 b))", "(init (cell 2 6 b))",
				"(init (cell 3 1 b))", "(init (cell 3 2 b))",
				"(init (cell 3 3 b))", "(init (cell 3 4 b))",
				"(init (cell 3 5 b))", "(init (cell 3 6 b))",
				"(init (cell 4 1 b))", "(init (cell 4 2 b))",
				"(init (cell 4 3 b))", "(init (cell 4 4 b))",
				"(init (cell 4 5 b))", "(init (cell 4 6 b))",
				"(init (cell 5 1 b))", "(init (cell 5 2 b))",
				"(init (cell 5 3 b))", "(init (cell 5 4 b))",
				"(init (cell 5 5 b))", "(init (cell 5 6 b))",
				"(init (cell 6 1 b))", "(init (cell 6 2 b))",
				"(init (cell 6 3 b))", "(init (cell 6 4 b))",
				"(init (cell 6 5 b))", "(init (cell 6 6 b))",
				"(init (cell 7 1 b))", "(init (cell 7 2 b))",
				"(init (cell 7 3 b))", "(init (cell 7 4 b))",
				"(init (cell 7 5 b))", "(init (cell 7 6 b))",
				"(init (control white))",
				"(succ 1 2)", "(succ 2 3)",
				"(succ 3 4)", "(succ 4 5)",
				"(succ 5 6)", "(succ 6 7)",
				"(<= (cm ?c ?r) (or (true (cell ?c ?r x)) (true (cell ?c ?r o))))",
				"(<= (sequential ?a ?b ?c ?d) (succ ?a ?b) (succ ?b ?c) (succ ?c ?d))",
				"(<= (top-unused ?c ?r) (true (cell ?c ?r b)) (cm ?c ?s) (succ ?s ?r))",
				"(<= (top-unused ?c 1) (true (cell ?c 1 b)))",
				"(<= (plays-on ?c ?r) (does ?x (drop ?c)) (top-unused ?c ?r))",
				"(<= (next (cell ?c ?r ?x)) (true (cell ?c ?r ?x)) (not (plays-on ?c ?r)))",
				"(<= (next (control white)) (true (control black)))",
				"(<= (next (control black)) (true (control white)))",
				"(<= (legal ?x (drop ?c)) (true (cell ?c 6 b)) (true (control ?x)))",
				"(<= (legal white noop) (true (control black)))",
				"(<= (legal black noop) (true (control white)))",
				"(<= (next (cell ?c ?r x)) (does white (drop ?c)) (top-unused ?c ?r))",
				"(<= (next (cell ?c ?r o)) (does black (drop ?c)) (top-unused ?c ?r))",
				"(<= (row ?x) (sequential ?a ?b ?c ?d) (true (cell ?a ?r ?x)) (true (cell ?b ?r ?x)) (true (cell ?c ?r ?x)) (true (cell ?d ?r ?x)))",
				"(<= (col  ?x) (sequential ?a ?b ?c ?d) (true (cell ?e ?a ?x)) (true (cell ?e ?b ?x)) (true (cell ?e ?c ?x)) (true (cell ?e ?d ?x)))",
				"(<= (diag1 ?x) (sequential ?a ?b ?c ?d) (sequential ?e ?f ?g ?h) (true (cell ?a ?e ?x)) (true (cell ?b ?f ?x)) (true (cell ?c ?g ?x)) (true (cell ?d ?h ?x)))",
				"(<= (diag2 ?x) (sequential ?a ?b ?c ?d) (sequential ?e ?f ?g ?h) (true (cell ?a ?h ?x)) (true (cell ?b ?g ?x)) (true (cell ?c ?f ?x)) (true (cell ?d ?e ?x)))",
				"(<= (connfour ?x) (or (col ?x) (row ?x) (diag1 ?x) (diag2 ?x)))",
				"(<= (goal ?x 50) (not (connfour x)) (not (connfour o)) (role ?x))",
				"(<= (goal white 100) (connfour x))",
				"(<= (goal black 0) (connfour x))",
				"(<= (goal white 0) (connfour o))",
				"(<= (goal black 100) (connfour o))",
				"(<= terminal (or (connfour x) (connfour o)))",
				"(<= not-filled (true (cell ?c 6 b)))",
				"(<= terminal (not not-filled))" };
		
		KifFormat fmt = new KifFormat();
		List<Rule> original = fmt.rulesFromStrings(Arrays.asList(raw));
		return GgpProtocol.deorPass(original, fmt);
	}
}

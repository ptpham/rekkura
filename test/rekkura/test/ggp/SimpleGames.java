package rekkura.test.ggp;

import java.util.List;

import rekkura.logic.format.KifFormat;
import rekkura.logic.format.StandardFormat;
import rekkura.logic.model.Rule;

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
			"{|<((goal)(robot)(100)),true>:-<((true)(q)),true>}",
			"{|<((goal)(robot)(10)),true>:-<((true)(z)),true>}"
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
		      "{(R)(Q)|<((goal)(R)(0)),true>:-<((role)(R)),true><((win)(Q)),true><((win)(R)),false>}",
		      "{(R)|<((goal)(R)(50)),true>:-<((role)(R)),true><((win)(x)),false><((win)(o)),false>}",
		      "{(R)|<(terminal),true>:-<((win)(R)),true>}",
		      "{(X)(Y)|<(empty),true>:-<((true)((cell)(empty)(X)(Y))),true>}",
		      "{|<(terminal),true>:-<(empty),false>}"
		);
		return StandardFormat.inst.rulesFromStrings(rules);
	}
	
	public static List<Rule> getConnectFour() {
		String[] raw = {
		        "(<= (role white))", "(<= (role black))",
		        "(<= (init (cell 1 1 b)))", "(<= (init (cell 1 2 b)))",
		        "(<= (init (cell 1 3 b)))", "(<= (init (cell 1 4 b)))",
		        "(<= (init (cell 1 5 b)))", "(<= (init (cell 1 6 b)))",
		        "(<= (init (cell 2 1 b)))", "(<= (init (cell 2 2 b)))",
		        "(<= (init (cell 2 3 b)))", "(<= (init (cell 2 4 b)))",
		        "(<= (init (cell 2 5 b)))", "(<= (init (cell 2 6 b)))",
		        "(<= (init (cell 3 1 b)))", "(<= (init (cell 3 2 b)))",
		        "(<= (init (cell 3 3 b)))", "(<= (init (cell 3 4 b)))",
		        "(<= (init (cell 3 5 b)))", "(<= (init (cell 3 6 b)))",
		        "(<= (init (cell 4 1 b)))", "(<= (init (cell 4 2 b)))",
		        "(<= (init (cell 4 3 b)))", "(<= (init (cell 4 4 b)))",
		        "(<= (init (cell 4 5 b)))", "(<= (init (cell 4 6 b)))",
		        "(<= (init (cell 5 1 b)))", "(<= (init (cell 5 2 b)))",
		        "(<= (init (cell 5 3 b)))", "(<= (init (cell 5 4 b)))",
		        "(<= (init (cell 5 5 b)))", "(<= (init (cell 5 6 b)))",
		        "(<= (init (cell 6 1 b)))", "(<= (init (cell 6 2 b)))",
		        "(<= (init (cell 6 3 b)))", "(<= (init (cell 6 4 b)))",
		        "(<= (init (cell 6 5 b)))", "(<= (init (cell 6 6 b)))",
		        "(<= (init (cell 7 1 b)))", "(<= (init (cell 7 2 b)))",
		        "(<= (init (cell 7 3 b)))", "(<= (init (cell 7 4 b)))",
		        "(<= (init (cell 7 5 b)))", "(<= (init (cell 7 6 b)))",
		        "(<= (init (control white)))",
		        "(<= (succ 1 2))", "(<= (succ 2 3))",
		        "(<= (succ 3 4))", "(<= (succ 4 5))",
		        "(<= (succ 5 6))", "(<= (succ 6 7))",
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
		
		return KifFormat.stringsToRules(raw);
	}

	public static List<Rule> getPilgrimage() {
		String[] raw = {
			"(<= (role red))",
			"(<= (role blue))",

			"(<= (init (builder red 2 2)))",
			"(<= (init (builder red 2 4)))",
			"(<= (init (builder red 2 5)))",
			"(<= (init (builder blue 5 5)))",
			"(<= (init (builder blue 5 2)))",
			"(<= (init (builder blue 5 3)))",
			"(<= (init (phase red build_terrain)))",
			"(<= (init (phase blue build_terrain)))",
			"(<= (init (control red)))",

			"(<= (index 1))",
			"(<= (index 2))",
			"(<= (index 3))",
			"(<= (index 4))",
			"(<= (index 5))",
			"(<= (index 6))",

			"(<= (height 1))",
			"(<= (height 2))",
			"(<= (height 3))",
			"(<= (height 4))",
			"(<= (height 5))",
			"(<= (height_end 5))",

			"(<= (height_succ 0 1))",
			"(<= (height_succ 1 2))",
			"(<= (height_succ 2 3))",
			"(<= (height_succ 3 4))",
			"(<= (height_succ 4 5))",

			"(<= (board_succ 1 2))",
			"(<= (board_succ 2 3))",
			"(<= (board_succ 3 4))",
			"(<= (board_succ 4 5))",
			"(<= (board_succ 5 6))",

			"(<= (phase_list build_terrain))",
			"(<= (phase_list place_pilgrim))",
			"(<= (phase_list pilgrimage))",

			"(<= (height_score 0 100))",
			"(<= (height_score 1 40))",
			"(<= (height_score 2 30))",
			"(<= (height_score 3 20))",
			"(<= (height_score 4 10))",
			"(<= (height_score 5 0))",

			"(<= (succ 0 1))",
			"(<= (succ 1 2))",
			"(<= (succ 2 3))",
			"(<= (succ 3 4))",
			"(<= (succ 4 5))",
			"(<= (succ 5 6))",
			"(<= (succ 6 7))",
			"(<= (succ 7 8))",
			"(<= (succ 8 9))",
			"(<= (succ 9 10))",
			"(<= (succ 10 11))",
			"(<= (succ 11 12))",
			"(<= (succ 12 13))",
			"(<= (succ 13 14))",
			"(<= (succ 14 15))",
			"(<= (succ 15 16))",
			"(<= (succ 16 17))",
			"(<= (succ 17 18))",
			"(<= (succ 18 19))",
			"(<= (succ 19 20))",
			"(<= (succ 20 21))",

			"(<= (base (cell ?i ?j ?h)) (index ?i) (index ?j) (height ?h))",
			"(<= (base (builder ?p ?i ?j)) (role ?p) (index ?i) (index ?j))",
			"(<= (base (pilgrim ?p ?i ?j)) (role ?p) (index ?i) (index ?j))",
			"(<= (base (control ?p)) (role ?p))",
			"(<= (base (phase ?p ?e)) (role ?p) (phase_list ?e))",
			"(<= (base (moves ?p ?s)) (role ?p) (succ ?s ?o))",
			"(<= (input ?p noop) (role ?p))",
			"(<= (input ?p (move ?i ?j ?m ?n)) (role ?p) (index ?i) (index ?j) (index ?m) (index ?n))",
			"(<= (input ?p (raise ?i ?j)) (role ?p) (index ?i) (index ?j))",
			"(<= (input ?p (place_pilgrim ?i ?j)) (role ?p) (index ?i) (index ?j))",

			"(<= (legal ?p noop) (role ?p) (not (has_action ?p)))",
			"(<= (legal ?p (place_pilgrim ?i ?j)) (legal_pilgrim ?p ?i ?j))",
			"(<= (legal ?p (move ?i ?j ?m ?n)) (legal_move ?p ?i ?j ?m ?n))",
			"(<= (legal ?p (raise ?i ?j)) (legal_raise ?p ?i ?j))",

			"(<= (has_action ?p) (legal_move ?p ?i ?j ?m ?n))",
			"(<= (has_action ?p) (legal_raise ?p ?i ?j))",
			"(<= (has_action ?p) (legal_pilgrim ?p ?i ?j))",

			"(<= (legal_move ?p ?i ?j ?m ?n) (can_move_pieces ?p) (piece ?p ?m ?n) (free_path ?i ?j ?m ?n))",
			"(<= (legal_raise ?p ?i ?j) (true (control ?p)) (true (phase ?p build_terrain)) (true (builder ?p ?i ?j)) (height_end ?h) (not (true (cell ?i ?j ?h))))",
			"(<= (legal_pilgrim ?p ?i ?j) (true (control ?p)) (true (phase ?p place_pilgrim)) (true (builder ?p ?i ?j)))",

			"(<= (can_move_pieces ?p) (true (phase ?p build_terrain)) (not (true (control ?p))))",
			"(<= (can_move_pieces ?p) (true (phase ?p pilgrimage)) (true (control ?p)))",

			"(<= (piece ?p ?i ?j) (true (builder ?p ?i ?j)))",
			"(<= (piece ?p ?i ?j) (true (pilgrim ?p ?i ?j)))",

			"(<= (free_path ?i ?j ?m ?n) (adjacent ?i ?j ?m ?n) (same_height ?i ?j ?m ?n) (not (filled ?i ?j)))",
			"(<= (free_path ?i ?j ?m ?n) (adjacent ?i ?j ?m ?n) (next_height ?i ?j ?m ?n) (not (filled ?i ?j)))",

			"(<= (adjacent ?i ?j ?m ?j) (board_succ ?i ?m) (index ?j))",
			"(<= (adjacent ?i ?j ?m ?j) (board_succ ?m ?i) (index ?j))",
			"(<= (adjacent ?i ?j ?i ?n) (board_succ ?j ?n) (index ?i))",
			"(<= (adjacent ?i ?j ?i ?n) (board_succ ?n ?j) (index ?i))",

			"(<= (same_height ?i ?j ?m ?n) (true (cell ?i ?j ?h)) (true (cell ?m ?n ?h)))",
			"(<= (same_height ?i ?j ?m ?n) (not (has_height ?i ?j)) (not (has_height ?m ?n)))",
			"(<= (next_height ?i ?j ?m ?n) (true (cell ?i ?j ?g)) (true (cell ?m ?n ?h)) (height_succ ?g ?h))",
			"(<= (next_height ?i ?j ?m ?n) (true (cell ?i ?j ?g)) (true (cell ?m ?n ?h)) (height_succ ?h ?g))",
			"(<= (next_height ?i ?j ?m ?n) (not (has_height ?i ?j)) (true (cell ?m ?n 1)))",
			"(<= (next_height ?i ?j ?m ?n) (not (has_height ?m ?n)) (true (cell ?i ?j 1)))",

			"(<= (has_height ?i ?j) (true (cell ?i ?j ?h)))",

			"(<= (filled ?i ?j) (true (pilgrim ?p ?i ?j)))",
			"(<= (filled ?i ?j) (true (builder ?p ?i ?j)))",

			"(<= (next (builder ?p ?i ?j)) (true (builder ?p ?i ?j)) (not (removed ?i ?j)))",
			"(<= (next (pilgrim ?p ?i ?j)) (true (pilgrim ?p ?i ?j)) (not (removed ?i ?j)))",

			"(<= (next (builder ?p ?i ?j)) (true (builder ?p ?m ?n)) (actual_move ?p ?i ?j ?m ?n))",
			"(<= (next (pilgrim ?p ?i ?j)) (true (pilgrim ?p ?m ?n)) (actual_move ?p ?i ?j ?m ?n))",
			"(<= (next (pilgrim ?p ?i ?j)) (does ?p (place_pilgrim ?i ?j)))",

			"(<= (next (phase ?p ?e)) (true (phase ?p ?e)) (not (phase_transition ?p)))",
			"(<= (next (control red)) (true (control blue)))",
			"(<= (next (control blue)) (true (control red)))",

			"(<= (next (cell ?i ?j 1)) (does ?p (raise ?i ?j)) (not (has_height ?i ?j)))",
			"(<= (next (cell ?i ?j ?h)) (true (cell ?i ?j ?h)) (not (cell_raise ?i ?j)))",
			"(<= (next (cell ?i ?j ?h)) (does ?p (raise ?i ?j)) (true (cell ?i ?j ?g)) (height_succ ?g ?h))",

			"(<= (next (moves ?p ?s)) (true (moves ?p ?o)) (succ ?s ?o))",
			"(<= (next (moves ?p 0)) (true (moves ?p 0)))",

			"(<= (next (phase ?p place_pilgrim)) (build_terrain_transition ?p))",

			"(<= (next (phase ?p pilgrimage)) (place_pilgrim_transition ?p))",
			"(<= (next (moves ?p 20)) (place_pilgrim_transition ?p))",


			"(<= (actual_move ?p ?i ?j ?m ?n) (does ?p (move ?i ?j ?m ?n)) (true (phase ?p pilgrimage)))",
			"(<= (actual_move ?p ?i ?j ?m ?n) (does ?p (move ?i ?j ?m ?n)) (true (phase ?p build_terrain)) (not (move_conflict)))",

			"(<= (removed ?m ?n) (actual_move ?p ?i ?j ?m ?n))",
			"(<= (removed ?i ?j) (does ?p (place_pilgrim ?i ?j)))",

			"(<= (move_conflict) (does ?p (move ?i ?j ?m ?n)) (does ?q (move ?i ?j ?k ?l)) (distinct ?p ?q))",

			"(<= (cell_raise ?i ?j) (does ?p (raise ?i ?j)))",

			"(<= (phase_transition ?p) (build_terrain_transition ?p))",
			"(<= (build_terrain_transition ?p) (true (control ?p)) (true (phase ?p build_terrain)) (not (has_raise ?p)))",

			"(<= (has_raise ?p) (legal_raise ?p ?i ?j))",

			"(<= (phase_transition ?p) (place_pilgrim_transition ?p))",
			"(<= (place_pilgrim_transition ?p) (true (control ?p)) (true (phase ?p place_pilgrim)))",

			"(<= terminal (complete ?p))",

			"(<= terminal (true (moves red 0)) (true (moves blue 0)) (true (phase ?p pilgrimage)))",
			"(<= (complete ?p) (true (phase ?p pilgrimage)) (pilgrim_height ?p 0))",
			"(<= (pilgrim_height ?p 0) (true (pilgrim ?p ?i ?j)) (not (has_height ?i ?j)))",
			"(<= (pilgrim_height ?p ?h) (true (pilgrim ?p ?i ?j)) (true (cell ?i ?j ?h)))",

			"(<= (goal red 100) (complete red) (not (complete blue)))",
			"(<= (goal red 50) (complete red) (complete blue))",
			"(<= (goal red 0) (not (complete red)) (complete blue))",
			"(<= (goal red 0) (not (true (phase red pilgrimage))))",
			"(<= (goal red ?s) (not (complete red)) (not (complete blue)) (pilgrim_height red ?h) (height_score ?h ?s))",

			"(<= (goal blue 100) (not (complete red)) (complete blue))",
			"(<= (goal blue 50) (complete red) (complete blue))",
			"(<= (goal blue 0) (complete red) (not (complete blue)))",
			"(<= (goal blue 0) (not (true (phase blue pilgrimage))))",
			"(<= (goal blue ?s) (not (complete red)) (not (complete blue)) (pilgrim_height blue ?h) (height_score ?h ?s))",
		};
		
		return KifFormat.stringsToRules(raw);
	}

}

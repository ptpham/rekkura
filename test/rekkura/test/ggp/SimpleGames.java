package rekkura.test.ggp;

import java.util.List;

import com.google.common.collect.Lists;

import rekkura.fmt.StandardFormat;
import rekkura.model.Rule;

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
			      "{(R)(X)(Y)|<((legal)(R)(mark)(X)(Y)),true>:-<((role)(R)),true> <((empty)(X)(Y)),true>}",
			      "{(R)(X)(Y)|<((empty)(X)(Y)),true>:-<((role)(R)),true><((index)(X)),true><((index)(Y)),true><((true)(cell)(R)(X)(Y)),false>}",
			      "{(R)(X)(Y)|<((next)(cell)(R)(X)(Y)),true>:-<((does)(R)(mark)(X)(Y)),true>}",
			      "{(R)(X)(Y)|<((next)(cell)(R)(X)(Y)),true>:-<((true)(cell)(R)(X)(Y)),true>}",
			      "{(R)(X)|<((line)(R)(X)),true>:-<((true)(cell)(R)(X)(1)),true><((true)(cell)(R)(X)(2)),true><((true)(cell)(R)(X)(3)),true>}",
			      "{(R)(X)|<((line)(R)(X)),true>:-<((true)(cell)(R)(1)(X)),true><((true)(cell)(R)(2)(X)),true><((true)(cell)(R)(3)(X)),true>}",
			      "{(R)|<((diag)(R)),true>:-<((true)(cell)(R)(1)(1)),true><((true)(cell)(R)(2)(2)),true><((true)(cell)(R)(3)(3)),true>}",
			      "{(R)|<((diag)(R)),true>:-<((true)(cell)(R)(3)(1)),true><((true)(cell)(R)(2)(2)),true><((true)(cell)(R)(1)(3)),true>}",
			      "{(R)(X)|<((win)(R)),true>:-<((line)(R)(X)),true>}",
			      "{(R)|<((win)(R)),true>:-<((diag)(R)),true>}",
			      "{(R)|<((goal)(R)(100)),true>:-<((win)(R)),true>}",
			      "{(R)|<((goal)(R)(0)),true>:-<((role)(R)),true><(terminal),true><((win)(R)),false>}",
			      "{(R)|<(terminal),true>:-<((win)(R)),true>}",
			      "{(X)(Y)|<(terminal),true>:-<((index)(X)),true><((index)(Y)),true><((empty)(X)(Y)),false>}"

		);
		return StandardFormat.inst.rulesFromStrings(rules);
	}
}

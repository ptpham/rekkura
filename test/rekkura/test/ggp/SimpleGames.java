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
		      "{(R)|<((goal)(R)(50)),true>:-<((role)(R)),true><((win)(x)),false><((win)(o)),false>}",
		      "{(R)|<((goal)(R)(0)),true>:-<((role)(R)),true><((win)(R)),false>}",
		      "{(R)|<(terminal),true>:-<((win)(R)),true>}",
		      "{(X)(Y)|<(empty),true>:-<((true)((cell)(empty)(X)(Y))),true>}",
		      "{|<(terminal),true>:-<(empty),false>}"
		);
		return StandardFormat.inst.rulesFromStrings(rules);
	}
}

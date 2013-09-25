package rekkura.test.logic.algorithm;

import rekkura.logic.algorithm.Renderer;

public class ChainingTest extends RendererTest {
	@Override protected Renderer getExpansion() {
		Renderer.Chaining result = Renderer.newChaining();
		return result;
	}
}

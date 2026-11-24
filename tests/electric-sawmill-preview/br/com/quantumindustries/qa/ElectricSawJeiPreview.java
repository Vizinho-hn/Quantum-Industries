package br.com.quantumindustries.qa;

@mezz.jei.api.JeiPlugin
public final class ElectricSawJeiPreview implements mezz.jei.api.IModPlugin {
  static mezz.jei.api.runtime.IJeiRuntime runtime;

  public net.minecraft.resources.Identifier getPluginUid() {
    return net.minecraft.resources.Identifier.parse("quantumindustries:electric_saw_qa");
  }

  public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime r) {
    runtime = r;
  }

  static void show() {
    if (runtime == null) throw new AssertionError("JEI runtime unavailable");
    var type =
        runtime
            .getRecipeManager()
            .getRecipeType(
                net.minecraft.resources.Identifier.parse("quantumindustries:electric_sawmill"))
            .orElseThrow();
    runtime.getRecipesGui().showTypes(java.util.List.of(type));
  }
}

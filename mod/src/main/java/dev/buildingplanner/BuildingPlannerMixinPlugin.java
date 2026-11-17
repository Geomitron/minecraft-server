package dev.buildingplanner;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Skips the hooks into a mod that isn't installed: Effortless Building's without it, and Litematica's without Litematica
 * or MaLiLib. They run only on players' computers, and players may leave them out.
 */
public final class BuildingPlannerMixinPlugin implements IMixinConfigPlugin {
	static final String EFFORTLESS_MIXIN_PACKAGE = "dev.buildingplanner.mixin.effortless.";
	static final String EFFORTLESS_MOD_ID = "effortlessbuilding";
	static final String LITEMATICA_MIXIN_PACKAGE = "dev.buildingplanner.mixin.litematica.";

	private final Predicate<String> modLoaded;

	public BuildingPlannerMixinPlugin() {
		this(id -> FabricLoader.getInstance().isModLoaded(id));
	}

	BuildingPlannerMixinPlugin(Predicate<String> modLoaded) {
		this.modLoaded = modLoaded;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.startsWith(EFFORTLESS_MIXIN_PACKAGE)) {
			return modLoaded.test(EFFORTLESS_MOD_ID);
		}
		if (mixinClassName.startsWith(LITEMATICA_MIXIN_PACKAGE)) {
			return modLoaded.test("litematica") && modLoaded.test("malilib");
		}
		return true;
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}

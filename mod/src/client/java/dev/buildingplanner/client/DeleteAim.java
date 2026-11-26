package dev.buildingplanner.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.buildingplanner.input.BoxCorners;
import dev.buildingplanner.layer.DeleteMarks;
import dev.buildingplanner.layer.Layer;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

/**
 * Delete's sweep (see "Delete": "Marking by sweeping"): each frame while Delete is on, with no screen open, Minecraft's
 * sprint key held, and neither the left button down nor Delete's box drawn ({@link MarkBox}), it aims as hook E9 aims,
 * from where the player aims from ({@link Aim}), at ghost blocks up to {@link BoxCorners#REACH} from the eyes, a real
 * block in front of a ghost block stopping the aim, along the look directions between the last frame's and this one's
 * ({@link DeleteMarks#swept}), and marks or unmarks each ghost block it meets. It runs where MaLiLib draws after the world, once a frame, registered before the marks' outline
 * ({@link MarksOutline}), which then draws the marks as they stand.
 */
final class DeleteAim implements IRenderer {
	private final Supplier<ClientSession> session;
	/** The view at the sweep's last frame, or null before its first. */
	private DeleteMarks.View last;

	private DeleteAim(Supplier<ClientSession> session) {
		this.session = session;
	}

	/** The sweep for the session {@code session} gives, or none while it gives null, run once a frame. */
	static DeleteAim registered(Supplier<ClientSession> session) {
		DeleteAim aim = new DeleteAim(session);
		RenderEventHandler.getInstance().registerWorldLastRenderer(aim);
		return aim;
	}

	@Override
	public void onRenderWorldLast(RenderTarget fb, CameraRenderState cameraState, Frustum culling,
		RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler) {
		ClientSession now = session.get();
		if (now == null) {
			last = null;
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Entity from = Aim.entity();
		DeleteMarks marks = now.marks();
		if (!now.deleteOn() || mc.gui.screen() != null || !mc.options.keySprint.isDown() || from == null
			|| mc.level == null) {
			marks.endSweep();
			last = null;
			return;
		}
		if (!marks.sweeping()) {
			marks.startSweep();
			last = null;
		}
		// The hold goes on, but while the left button is down or the box is drawn, the sweep marks nothing.
		if (mc.options.keyAttack.isDown() || now.markBox().on()) {
			last = null;
			return;
		}
		profiler.push("buildingplanner_delete_aim");
		DeleteMarks.View view = new DeleteMarks.View(from.getEyePosition(), from.getLookAngle());
		Layer layer = now.replica().layer();
		marks.sweep(last, view, tracer(mc.level, layer), layer);
		last = view;
		profiler.pop();
	}

	/**
	 * Aiming as hook E9 does, at the nearer of a real block and a ghost block up to {@link BoxCorners#REACH} away; only a
	 * ghost block nearer than any real block is met.
	 */
	static DeleteMarks.Tracer tracer(ClientLevel level, Layer layer) {
		Minecraft mc = Minecraft.getInstance();
		return (eye, look) -> {
			ClipContext context = new ClipContext(eye, eye.add(look.scale(BoxCorners.REACH)), ClipContext.Block.OUTLINE,
				ClipContext.Fluid.NONE, mc.player);
			BlockHitResult ghost = GhostAim.ghostHit(layer, true, level, context);
			BlockHitResult hit = GhostAim.nearer(level.clip(context), ghost, eye);
			if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
				return new DeleteMarks.Hit(null, BoxCorners.REACH);
			}
			Vec3 at = hit.getLocation();
			return new DeleteMarks.Hit(hit == ghost ? hit.getBlockPos() : null, eye.distanceTo(at));
		};
	}
}

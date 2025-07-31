package dev.kizuna.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kizuna.Kawaii;
import dev.kizuna.api.events.Event;
import dev.kizuna.api.events.impl.GameLeftEvent;
import dev.kizuna.api.events.impl.OpenScreenEvent;
import dev.kizuna.api.events.impl.TickEvent;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.impl.client.ClientSetting;
import dev.kizuna.mod.modules.impl.player.InteractTweaks;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.MacWindowUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.ResourcePack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static dev.kizuna.core.Manager.mc;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient extends ReentrantThreadExecutor<Runnable> {
	@Inject(method = "<init>", at = @At("TAIL"))
	void postWindowInit(RunArgs args, CallbackInfo ci) {
		try {
			FontRenderers.createDefault(8f);
			FontRenderers.Calibri = FontRenderers.create("calibri", Font.BOLD, 11f);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void onSetScreen(Screen screen, CallbackInfo info) {
		OpenScreenEvent event = new OpenScreenEvent(screen);
		Kawaii.EVENT_BUS.post(event);

		if (event.isCancelled()) info.cancel();
	}
	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
	private void onDisconnect(Screen screen, CallbackInfo info) {
		if (world != null) {
			Kawaii.EVENT_BUS.post(new GameLeftEvent());
		}
	}

	@Shadow
	@Final
	public InGameHud inGameHud;

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
	private void clearTitleMixin(Screen screen, CallbackInfo info) {
		if (ClientSetting.INSTANCE.titleFix.getValue()) {
			inGameHud.clearTitle();
			inGameHud.setDefaultTitleFade();
		}
	}
	@Shadow
	public int attackCooldown;

	@Shadow
	public ClientPlayerEntity player;

	@Shadow
	public HitResult crosshairTarget;

	@Shadow
	public ClientPlayerInteractionManager interactionManager;

	@Final
	@Shadow
	public ParticleManager particleManager;

	@Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
	private void handleBlockBreaking(boolean breaking, CallbackInfo ci) {
		if (this.attackCooldown <= 0 && this.player.isUsingItem() && InteractTweaks.INSTANCE.multiTask()) {
			if (breaking && this.crosshairTarget != null && this.crosshairTarget.getType() == HitResult.Type.BLOCK) {
				BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
				BlockPos blockPos = blockHitResult.getBlockPos();
				if (!this.world.getBlockState(blockPos).isAir()) {
					Direction direction = blockHitResult.getSide();
					if (this.interactionManager.updateBlockBreakingProgress(blockPos, direction)) {
						this.particleManager.addBlockBreakingParticles(blockPos, direction);
						this.player.swingHand(Hand.MAIN_HAND);
					}
				}
			} else {
				this.interactionManager.cancelBlockBreaking();
			}
			ci.cancel();
		}
	}
	@Shadow
	public ClientWorld world;

	public MixinMinecraftClient(String string) {
		super(string);
	}

	@Inject(at = @At("HEAD"), method = "tick()V")
	public void tickHead(CallbackInfo info) {
		Kawaii.EVENT_BUS.post(new TickEvent(Event.Stage.Pre));
	}
	@Inject(at = @At("TAIL"), method = "tick()V")
	public void tickTail(CallbackInfo info) {
		Kawaii.EVENT_BUS.post(new TickEvent(Event.Stage.Post));
	}


	/**
	 * @author me
	 * @reason title
	 */
	@Overwrite
	private String getWindowTitle() {
		if (ClientSetting.INSTANCE == null) {
			return Kawaii.NAME + ": Loading..";
		}
		if (ClientSetting.INSTANCE.titleOverride.getValue()) {
			return ClientSetting.INSTANCE.windowTitle.getValue();
		}
		StringBuilder stringBuilder = new StringBuilder(ClientSetting.INSTANCE.windowTitle.getValue());

		stringBuilder.append(" ");
		stringBuilder.append(SharedConstants.getGameVersion().getName());

		ClientPlayNetworkHandler clientPlayNetworkHandler = this.getNetworkHandler();
		if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isOpen()) {
			stringBuilder.append(" - ");
			ServerInfo serverInfo = this.getCurrentServerEntry();
			if (this.server != null && !this.server.isRemote()) {
				stringBuilder.append(I18n.translate("title.singleplayer"));
			} else if (serverInfo != null && serverInfo.isRealm()) {
				stringBuilder.append(I18n.translate("title.multiplayer.realms"));
			} else if (this.server == null && (serverInfo == null || !serverInfo.isLocal())) {
				stringBuilder.append(I18n.translate("title.multiplayer.other"));
			} else {
				stringBuilder.append(I18n.translate("title.multiplayer.lan"));
			}
		}

		return stringBuilder.toString();
	}
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setIcon(Lnet/minecraft/resource/ResourcePack;Lnet/minecraft/client/util/Icons;)V"))
	private void onChangeIcon(Window instance, ResourcePack resourcePack, Icons icons) throws IOException {
		RenderSystem.assertInInitPhase();

		if (GLFW.glfwGetPlatform() == 393218) {
			MacWindowUtil.setApplicationIconImage(icons.getMacIcon(resourcePack));
			return;
		}

		setWindowIcon(Kawaii.class.getResourceAsStream("/assets/minecraft/icon.png"), Kawaii.class.getResourceAsStream("/assets/minecraft/icon.png"));
	}

	public void setWindowIcon(InputStream img16x16, InputStream img32x32) {
		try (MemoryStack memorystack = MemoryStack.stackPush()) {
			GLFWImage.Buffer buffer = GLFWImage.malloc(2, memorystack);
			java.util.List<InputStream> imgList = java.util.List.of(img16x16, img32x32);
			List<ByteBuffer> buffers = new ArrayList<>();

			for (int i = 0; i < imgList.size(); i++) {
				NativeImage nativeImage = NativeImage.read(imgList.get(i));
				ByteBuffer bytebuffer = MemoryUtil.memAlloc(nativeImage.getWidth() * nativeImage.getHeight() * 4);

				bytebuffer.asIntBuffer().put(nativeImage.copyPixelsRgba());
				buffer.position(i);
				buffer.width(nativeImage.getWidth());
				buffer.height(nativeImage.getHeight());
				buffer.pixels(bytebuffer);

				buffers.add(bytebuffer);
			}

			try {
				if (GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_WAYLAND) {
					GLFW.glfwSetWindowIcon(mc.getWindow().getHandle(), buffer);
				}
			} catch (Exception ignored) {
			}
			buffers.forEach(MemoryUtil::memFree);
		} catch (IOException ignored) {
		}
	}

	@Shadow
	private IntegratedServer server;

	@Shadow
	public ClientPlayNetworkHandler getNetworkHandler() {
		return null;
	}

	@Shadow
	public ServerInfo getCurrentServerEntry() {
		return null;
	}
}

package dev.kizuna.asm.mixins;

import dev.kizuna.Kawaii;
import dev.kizuna.api.events.impl.TimerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.render.RenderTickCounter$Dynamic")
public class MixinRenderTickCounter {
	@Inject(method = "getTickDelta(Z)F", at = @At("RETURN"), cancellable = true)
	private void onGetTickDelta(boolean ignoreFreeze, CallbackInfoReturnable<Float> cir) {
		TimerEvent event = new TimerEvent();
		Kawaii.EVENT_BUS.post(event);
		if (!event.isCancelled()) {
			float tickDelta = cir.getReturnValueF();
			if (event.isModified()) {
				cir.setReturnValue(tickDelta * event.get());
			} else {
				cir.setReturnValue(tickDelta * Kawaii.TIMER.get());
			}
		}
	}
}

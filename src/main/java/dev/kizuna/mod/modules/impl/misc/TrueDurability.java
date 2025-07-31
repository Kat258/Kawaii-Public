package dev.kizuna.mod.modules.impl.misc;

import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.DurabilityEvent;
import dev.kizuna.mod.modules.Module;

public class TrueDurability extends Module {

    public TrueDurability() {
        super("TrueDurability", Category.Misc);
        setChinese("耐久度修正");
    }

    @EventHandler
    public void onDurability(DurabilityEvent event) {
        int dura = event.getItemDamage();
        if (event.getDamage() < 0) {
            dura = event.getDamage();
        }
        event.cancel();
        event.setDamage(dura);
    }
}

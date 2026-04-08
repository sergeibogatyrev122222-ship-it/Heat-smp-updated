package dev.heatsmp.listeners;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.rods.RodManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class RodListener implements Listener {

    private final HeatSMPPlugin plugin;

    public RodListener(HeatSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        var item = player.getInventory().getItemInMainHand();
        RodManager.RodType type = plugin.getRodManager().getRodType(item);
        if (type == null) return;

        event.setCancelled(true);
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            plugin.getRodManager().useRightClick(player, type);
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            plugin.getRodManager().useLeftClick(player, type);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        // Check if victim has Molten Armor active
        if (plugin.getRodManager().isMoltenArmorActive(victim)) {
            event.setCancelled(true);
            plugin.getRodManager().absorbHitAsMoltenArmor(victim, attacker);
        }
    }
}

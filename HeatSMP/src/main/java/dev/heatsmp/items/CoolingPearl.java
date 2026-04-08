package dev.heatsmp.items;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.heat.HeatParticles;
import dev.heatsmp.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CoolingPearl {

    private final HeatSMPPlugin plugin;
    private final HeatParticles particles;
    private final NamespacedKey key;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CoolingPearl(HeatSMPPlugin plugin) {
        this.plugin = plugin;
        this.particles = new HeatParticles(plugin);
        this.key = new NamespacedKey(plugin, "cooling_pearl");
        registerRecipe();
    }

    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("❄ Cooling Pearl")
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(Arrays.asList(
                Component.text("❄ Instantly reduces your heat by 30.")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Forged from the coldest depths of the ice biome.")
                        .color(NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        // Mark with persistent data so we can identify this item
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isCoolingPearl(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public void use(org.bukkit.entity.Player player) {
        long cooldownMs = (long) (plugin.getConfig().getDouble("cooling-pearl.cooldown-seconds", 20) * 1000);
        double heatReduction = plugin.getConfig().getDouble("cooling-pearl.heat-reduction", 30);

        UUID id = player.getUniqueId();
        Long last = cooldowns.get(id);
        if (last != null && (System.currentTimeMillis() - last) < cooldownMs) {
            double remaining = (cooldownMs - (System.currentTimeMillis() - last)) / 1000.0;
            MessageUtil.send(player, "cooling-pearl-cooldown", Map.of("time", String.format("%.1f", remaining)));
            return;
        }

        plugin.getHeatManager().removeHeat(player, heatReduction);
        particles.spawnCoolingPearlEffect(player);
        cooldowns.put(id, System.currentTimeMillis());

        // Consume one pearl
        var hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        MessageUtil.send(player, "cooling-pearl-used");
    }

    private void registerRecipe() {
        // Recipe: Ice block in center, surrounded by ender pearls
        // Craft: 1 Ender Pearl (center) + 4 Ice around it
        ShapedRecipe recipe = new ShapedRecipe(key, createItem());
        recipe.shape(" I ", "IPI", " I ");
        recipe.setIngredient('I', Material.ICE);
        recipe.setIngredient('P', Material.ENDER_PEARL);

        plugin.getServer().addRecipe(recipe);
    }

    public void remove(UUID uuid) {
        cooldowns.remove(uuid);
    }
}

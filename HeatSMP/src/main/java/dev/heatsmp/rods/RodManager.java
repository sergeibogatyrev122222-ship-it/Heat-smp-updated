package dev.heatsmp.rods;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.heat.HeatManager;
import dev.heatsmp.heat.HeatParticles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class RodManager {

    public enum RodType {
        EMBER, SCORCH, INFERNO, MOLTEN
    }

    private final HeatSMPPlugin plugin;
    private final HeatParticles particles;
    private final NamespacedKey rodKey;

    // Cooldown maps per rod per ability (true = right, false = left)
    private final Map<UUID, Long> emberRightCd = new HashMap<>();
    private final Map<UUID, Long> emberLeftCd = new HashMap<>();
    private final Map<UUID, Long> scorchRightCd = new HashMap<>();
    private final Map<UUID, Long> scorchLeftCd = new HashMap<>();
    private final Map<UUID, Long> infernRightCd = new HashMap<>();
    private final Map<UUID, Long> infernLeftCd = new HashMap<>();
    private final Map<UUID, Long> moltenRightCd = new HashMap<>();
    private final Map<UUID, Long> moltenLeftCd = new HashMap<>();

    // Molten armor active set
    private final Set<UUID> moltenArmorActive = new HashSet<>();

    public RodManager(HeatSMPPlugin plugin) {
        this.plugin = plugin;
        this.particles = new HeatParticles(plugin);
        this.rodKey = new NamespacedKey(plugin, "rod_type");
        registerRecipes();
    }

    // ─── Item Creation ────────────────────────────────────────────────────────

    public ItemStack createRod(RodType type) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();

        switch (type) {
            case EMBER -> {
                meta.displayName(Component.text("🔥 Ember Rod").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
                meta.lore(List.of(
                    Component.text("Right Click: ").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Pull nearby players toward you").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                    Component.text("Left Click: ").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Blast yourself backward").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                    Component.text("Heat Cost: 15 | Tier I").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                ));
            }
            case SCORCH -> {
                meta.displayName(Component.text("💥 Scorch Rod").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
                meta.lore(List.of(
                    Component.text("Right Click: ").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Ground slam — crash down dealing heat damage").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                    Component.text("Left Click: ").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Launch yourself into the air").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                    Component.text("Heat Cost: 25 | Tier II").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                ));
            }
            case INFERNO -> {
                meta.displayName(Component.text("🌋 Inferno Rod").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
                meta.lore(List.of(
                    Component.text("Right Click: ").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Steal 20 heat from nearest player").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                    Component.text("Left Click: ").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Dump 15 heat onto nearest player").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                    Component.text("Heat Cost: 35 | Tier III").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                ));
            }
            case MOLTEN -> {
                meta.displayName(Component.text("☄ Molten Rod").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
                meta.lore(List.of(
                    Component.text("Right Click: ").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Molten Armor — absorb hits as heat").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                    Component.text("Left Click: ").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Heat Nova — detonate all your heat").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                    Component.text("Heat Cost: 50 | Tier IV ⭐").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                ));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(rodKey, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    public RodType getRodType(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String val = meta.getPersistentDataContainer().get(rodKey, PersistentDataType.STRING);
        if (val == null) return null;
        try { return RodType.valueOf(val); } catch (Exception e) { return null; }
    }

    // ─── Recipes ──────────────────────────────────────────────────────────────

    private void registerRecipes() {
        // EMBER ROD: Blaze Rod + 2 sticks in column
        ShapedRecipe ember = new ShapedRecipe(new NamespacedKey(plugin, "ember_rod"), createRod(RodType.EMBER));
        ember.shape(" B ", " S ", " S ");
        ember.setIngredient('B', Material.BLAZE_ROD);
        ember.setIngredient('S', Material.STICK);
        plugin.getServer().addRecipe(ember);

        // SCORCH ROD: Ember components + fire charge
        ShapedRecipe scorch = new ShapedRecipe(new NamespacedKey(plugin, "scorch_rod"), createRod(RodType.SCORCH));
        scorch.shape("FBF", " S ", " S ");
        scorch.setIngredient('B', Material.BLAZE_ROD);
        scorch.setIngredient('S', Material.BLAZE_ROD);
        scorch.setIngredient('F', Material.FIRE_CHARGE);
        plugin.getServer().addRecipe(scorch);

        // INFERNO ROD: 3 blaze rods + magma blocks
        ShapedRecipe inferno = new ShapedRecipe(new NamespacedKey(plugin, "inferno_rod"), createRod(RodType.INFERNO));
        inferno.shape("MBM", "MBM", " B ");
        inferno.setIngredient('B', Material.BLAZE_ROD);
        inferno.setIngredient('M', Material.MAGMA_BLOCK);
        plugin.getServer().addRecipe(inferno);

        // MOLTEN ROD: Inferno-tier + netherite + lava bucket
        ShapedRecipe molten = new ShapedRecipe(new NamespacedKey(plugin, "molten_rod"), createRod(RodType.MOLTEN));
        molten.shape("NLN", "MBM", "MBM");
        molten.setIngredient('B', Material.BLAZE_ROD);
        molten.setIngredient('M', Material.MAGMA_BLOCK);
        molten.setIngredient('N', Material.NETHERITE_INGOT);
        molten.setIngredient('L', Material.LAVA_BUCKET);
        plugin.getServer().addRecipe(molten);
    }

    // ─── Right Click Abilities ─────────────────────────────────────────────────

    public void useRightClick(Player player, RodType type) {
        HeatManager hm = plugin.getHeatManager();
        switch (type) {
            case EMBER -> emberPull(player, hm);
            case SCORCH -> scorchSlam(player, hm);
            case INFERNO -> infernoSteal(player, hm);
            case MOLTEN -> moltenArmor(player, hm);
        }
    }

    public void useLeftClick(Player player, RodType type) {
        HeatManager hm = plugin.getHeatManager();
        switch (type) {
            case EMBER -> emberBlast(player, hm);
            case SCORCH -> scorchLaunch(player, hm);
            case INFERNO -> infernoDump(player, hm);
            case MOLTEN -> heatNova(player, hm);
        }
    }

    // ─── EMBER ROD ────────────────────────────────────────────────────────────

    private void emberPull(Player player, HeatManager hm) {
        if (onCooldown(emberRightCd, player, 6000)) { sendCd(player, emberRightCd, player.getUniqueId(), 6000); return; }
        if (hm.getHeat(player) < 15) { noHeat(player); return; }

        Location center = player.getLocation();
        int pulled = 0;
        for (Entity e : player.getNearbyEntities(8, 8, 8)) {
            if (!(e instanceof Player target)) continue;
            Vector dir = center.toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.8);
            dir.setY(0.3);
            target.setVelocity(dir);
            // Particles on pulled player
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0,1,0), 8, 0.3, 0.5, 0.3, 0.05);
            pulled++;
        }
        if (pulled == 0) { player.sendMessage(Component.text("No players nearby!").color(NamedTextColor.GRAY)); return; }

        hm.addHeat(player, 15);
        emberRightCd.put(player.getUniqueId(), System.currentTimeMillis());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);
        player.sendMessage(Component.text("🔥 Ember Pull! Drew in " + pulled + " player(s).").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
    }

    private void emberBlast(Player player, HeatManager hm) {
        if (onCooldown(emberLeftCd, player, 5000)) { sendCd(player, emberLeftCd, player.getUniqueId(), 5000); return; }
        if (hm.getHeat(player) < 15) { noHeat(player); return; }

        Vector dir = player.getLocation().getDirection().negate().multiply(2.2);
        dir.setY(0.5);
        player.setVelocity(dir);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0,1,0), 15, 0.5, 0.3, 0.5, 0.08);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.2f);

        hm.addHeat(player, 15);
        emberLeftCd.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(Component.text("🔥 Ember Blast! You launched backward.").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
    }

    // ─── SCORCH ROD ───────────────────────────────────────────────────────────

    private void scorchSlam(Player player, HeatManager hm) {
        if (onCooldown(scorchRightCd, player, 8000)) { sendCd(player, scorchRightCd, player.getUniqueId(), 8000); return; }
        if (hm.getHeat(player) < 25) { noHeat(player); return; }

        // Launch up first, then crash down
        player.setVelocity(new Vector(0, 2.5, 0));
        hm.addHeat(player, 25);
        scorchRightCd.put(player.getUniqueId(), System.currentTimeMillis());

        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                ticks++;
                if (!player.isOnline()) { cancel(); return; }
                // Once falling, crash down
                if (ticks > 15 && player.getVelocity().getY() < 0) {
                    player.setVelocity(new Vector(0, -3.5, 0));
                    cancel();
                    // Schedule impact
                    new BukkitRunnable() {
                        @Override public void run() {
                            if (!player.isOnline()) return;
                            Location impact = player.getLocation();
                            impact.getWorld().spawnParticle(Particle.FLAME, impact, 40, 2, 0.2, 2, 0.1);
                            impact.getWorld().spawnParticle(Particle.LAVA, impact, 20, 1.5, 0.2, 1.5, 0);
                            impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
                            for (Entity e : player.getNearbyEntities(4, 2, 4)) {
                                if (!(e instanceof Player t)) continue;
                                t.damage(4.0, player);
                                hm.addHeat(t, 12);
                                Vector kb = t.getLocation().subtract(impact).toVector().normalize().multiply(1.5);
                                kb.setY(0.6);
                                t.setVelocity(kb);
                            }
                        }
                    }.runTaskLater(plugin, 5L);
                }
                if (ticks > 60) cancel();
            }
        }.runTaskTimer(plugin, 5L, 1L);

        player.sendMessage(Component.text("💥 Ground Slam incoming!").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
    }

    private void scorchLaunch(Player player, HeatManager hm) {
        if (onCooldown(scorchLeftCd, player, 7000)) { sendCd(player, scorchLeftCd, player.getUniqueId(), 7000); return; }
        if (hm.getHeat(player) < 25) { noHeat(player); return; }

        player.setVelocity(new Vector(0, 3.2, 0));
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 20, 0.5, 0.1, 0.5, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);

        hm.addHeat(player, 25);
        scorchLeftCd.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(Component.text("💥 Scorch Launch! Soaring upward.").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
    }

    // ─── INFERNO ROD ──────────────────────────────────────────────────────────

    private void infernoSteal(Player player, HeatManager hm) {
        if (onCooldown(infernRightCd, player, 10000)) { sendCd(player, infernRightCd, player.getUniqueId(), 10000); return; }
        if (hm.getHeat(player) < 35) { noHeat(player); return; }

        Player target = getNearestPlayer(player, 10);
        if (target == null) { player.sendMessage(Component.text("No player in range!").color(NamedTextColor.GRAY)); return; }

        double steal = Math.min(20, hm.getHeat(target));
        hm.removeHeat(target, steal);
        hm.addHeat(player, steal + 35); // heat cost added too

        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0,1,0), 20, 0.4, 0.6, 0.4, 0.1);
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0,1,0), 15, 0.3, 0.5, 0.3, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1f, 0.6f);

        infernRightCd.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(Component.text("🌋 Heat Stolen from " + target.getName() + "! +" + (int)steal + " heat.").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        target.sendMessage(Component.text("🌋 " + player.getName() + " stole " + (int)steal + " heat from you!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    }

    private void infernoDump(Player player, HeatManager hm) {
        if (onCooldown(infernLeftCd, player, 8000)) { sendCd(player, infernLeftCd, player.getUniqueId(), 8000); return; }
        if (hm.getHeat(player) < 35) { noHeat(player); return; }

        Player target = getNearestPlayer(player, 10);
        if (target == null) { player.sendMessage(Component.text("No player in range!").color(NamedTextColor.GRAY)); return; }

        hm.removeHeat(player, 15 + 35);
        hm.addHeat(target, 15);

        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0,1,0), 25, 0.4, 0.6, 0.4, 0.08);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1f, 1.5f);

        infernLeftCd.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(Component.text("🌋 Heat Dumped onto " + target.getName() + "!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        target.sendMessage(Component.text("🌋 " + player.getName() + " dumped heat onto you! +15 heat!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    }

    // ─── MOLTEN ROD ───────────────────────────────────────────────────────────

    private void moltenArmor(Player player, HeatManager hm) {
        if (onCooldown(moltenRightCd, player, 20000)) { sendCd(player, moltenRightCd, player.getUniqueId(), 20000); return; }
        if (hm.getHeat(player) < 50) { noHeat(player); return; }

        double heat = hm.getHeat(player);
        int duration = heat >= 90 ? 160 : 100; // 8s or 5s in ticks

        moltenArmorActive.add(player.getUniqueId());
        hm.addHeat(player, 50);
        moltenRightCd.put(player.getUniqueId(), System.currentTimeMillis());

        // Orbit particles while active
        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (!player.isOnline() || tick >= duration) {
                    moltenArmorActive.remove(player.getUniqueId());
                    player.sendMessage(Component.text("☄ Molten Armor faded.").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                    cancel(); return;
                }
                if (tick % 3 == 0) {
                    double angle = (tick * 15) * Math.PI / 180;
                    Location loc = player.getLocation().add(0, 1, 0);
                    loc.add(Math.cos(angle) * 1.2, 0, Math.sin(angle) * 1.2);
                    player.getWorld().spawnParticle(Particle.LAVA, loc, 1, 0, 0, 0, 0);
                    player.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0.05, 0.05, 0.05, 0.01);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.sendMessage(Component.text("☄ Molten Armor active! Hits are absorbed as heat.").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
    }

    public boolean isMoltenArmorActive(Player player) {
        return moltenArmorActive.contains(player.getUniqueId());
    }

    public void absorbHitAsMoltenArmor(Player player, Player attacker) {
        HeatManager hm = plugin.getHeatManager();
        hm.addHeat(attacker, 18); // attacker gets heated for hitting
        // Flash orange on attacker
        attacker.getWorld().spawnParticle(Particle.FLAME, attacker.getLocation().add(0,1,0), 10, 0.3, 0.5, 0.3, 0.06);
        attacker.sendMessage(Component.text("☄ Your attack was absorbed by Molten Armor!").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
    }

    private void heatNova(Player player, HeatManager hm) {
        if (onCooldown(moltenLeftCd, player, 25000)) { sendCd(player, moltenLeftCd, player.getUniqueId(), 25000); return; }
        if (hm.getHeat(player) < 50) { noHeat(player); return; }

        double heat = hm.getHeat(player);
        double damage = heat / 10.0; // 10 heat = 1 heart
        Location center = player.getLocation().clone();

        // Knockback and damage nearby players
        for (Entity e : player.getNearbyEntities(6, 4, 6)) {
            if (!(e instanceof Player t)) continue;
            t.damage(damage, player);
            hm.addHeat(t, (int)(heat * 0.3));
            Vector kb = t.getLocation().subtract(center).toVector().normalize().multiply(2.5);
            kb.setY(0.8);
            t.setVelocity(kb);
        }

        // Reset player heat with penalties
        hm.resetHeat(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, true, true));
        moltenLeftCd.put(player.getUniqueId(), System.currentTimeMillis());

        // === SPIRAL PARTICLE EFFECT ===
        spawnHeatNovaSpiral(center, heat);

        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
        player.sendMessage(Component.text("☄ HEAT NOVA! Dealt " + String.format("%.1f", damage) + " hearts of damage!").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
    }

    private void spawnHeatNovaSpiral(Location center, double heat) {
        // Phase 1: Outward burst (0-10 ticks)
        new BukkitRunnable() {
            int tick = 0;
            final double radius = Math.min(heat / 10.0, 6);
            @Override public void run() {
                if (tick > 10) { cancel(); startSpiral(center, radius); return; }
                double progress = (double) tick / 10;
                double r = radius * progress;
                for (int i = 0; i < 20; i++) {
                    double angle = (i / 20.0) * 2 * Math.PI;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    Location p = center.clone().add(x, 0.5, z);
                    Particle particle = (i % 3 == 0) ? Particle.FLAME : (i % 3 == 1) ? Particle.LAVA : Particle.LARGE_SMOKE;
                    center.getWorld().spawnParticle(particle, p, 1, 0.1, 0.1, 0.1, 0.02);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startSpiral(Location center, double maxRadius) {
        // Phase 2: Inward spiral (ticks 10-40)
        new BukkitRunnable() {
            int tick = 0;
            final int totalTicks = 30;
            double spiralAngle = 0;
            @Override public void run() {
                if (tick >= totalTicks) { spawnImplosion(center); cancel(); return; }

                double progress = 1.0 - ((double) tick / totalTicks); // 1 → 0
                double r = maxRadius * progress;
                spiralAngle += 25; // degrees per tick — fast spiral

                // Spawn multiple spiral arms
                for (int arm = 0; arm < 3; arm++) {
                    double angle = Math.toRadians(spiralAngle + (arm * 120));
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    double y = 0.3 + (1 - progress) * 1.5; // rises as it spirals in

                    Location p = center.clone().add(x, y, z);

                    // Color cycle: red → orange → yellow
                    Particle color;
                    if (tick < 10) color = Particle.FLAME;
                    else if (tick < 20) color = Particle.LAVA;
                    else color = Particle.FLAME;

                    center.getWorld().spawnParticle(color, p, 3, 0.05, 0.05, 0.05, 0.01);
                }

                // Accelerating whoosh sound
                if (tick % 8 == 0) {
                    float pitch = 0.5f + (tick / (float) totalTicks) * 1.5f;
                    center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.5f, pitch);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 10L, 1L);
    }

    private void spawnImplosion(Location center) {
        // Phase 3: Implosion puff
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.05);
        center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(0, 0.5, 0), 10, 0.2, 0.2, 0.2, 0.02);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 1.5f);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Player getNearestPlayer(Player player, double range) {
        Player nearest = null;
        double closest = range * range;
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (!(e instanceof Player t)) continue;
            double dist = t.getLocation().distanceSquared(player.getLocation());
            if (dist < closest) { closest = dist; nearest = t; }
        }
        return nearest;
    }

    private boolean onCooldown(Map<UUID, Long> map, Player player, long ms) {
        Long last = map.get(player.getUniqueId());
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < ms;
    }

    private void sendCd(Player player, Map<UUID, Long> map, UUID id, long ms) {
        Long last = map.get(id);
        if (last == null) return;
        double remaining = (ms - (System.currentTimeMillis() - last)) / 1000.0;
        player.sendMessage(Component.text("Ability on cooldown for " + String.format("%.1f", remaining) + "s!").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
    }

    private void noHeat(Player player) {
        player.sendMessage(Component.text("Not enough heat to use that ability!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    }

    public void clearCooldowns(UUID uuid) {
        emberRightCd.remove(uuid); emberLeftCd.remove(uuid);
        scorchRightCd.remove(uuid); scorchLeftCd.remove(uuid);
        infernRightCd.remove(uuid); infernLeftCd.remove(uuid);
        moltenRightCd.remove(uuid); moltenLeftCd.remove(uuid);
        moltenArmorActive.remove(uuid);
    }
}

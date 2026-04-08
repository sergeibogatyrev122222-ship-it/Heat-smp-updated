package dev.heatsmp.commands;

import dev.heatsmp.HeatSMPPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RecipeCommand implements CommandExecutor {

    private final HeatSMPPlugin plugin;

    public RecipeCommand(HeatSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendList(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "cooling_pearl", "coolingpearl", "pearl" -> sendCoolingPearl(sender);
            case "ember", "ember_rod", "emberrod" -> sendEmberRod(sender);
            case "scorch", "scorch_rod", "scorchrod" -> sendScorchRod(sender);
            case "inferno", "inferno_rod", "infernorod" -> sendInfernoRod(sender);
            case "molten", "molten_rod", "moltenrod" -> sendMoltenRod(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown item. Use /recipe to see all items.").color(NamedTextColor.RED));
            }
        }
        return true;
    }

    private void sendList(CommandSender sender) {
        sender.sendMessage(sep());
        sender.sendMessage(title("📖 HeatSMP Recipes"));
        sender.sendMessage(Component.text("  /recipe pearl").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
            .append(Component.text(" — ❄ Cooling Pearl").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        sender.sendMessage(Component.text("  /recipe ember").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            .append(Component.text(" — 🔥 Ember Rod (Tier I)").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        sender.sendMessage(Component.text("  /recipe scorch").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
            .append(Component.text(" — 💥 Scorch Rod (Tier II)").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        sender.sendMessage(Component.text("  /recipe inferno").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
            .append(Component.text(" — 🌋 Inferno Rod (Tier III)").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        sender.sendMessage(Component.text("  /recipe molten").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false)
            .append(Component.text(" — ☄ Molten Rod (Tier IV)").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        sender.sendMessage(sep());
    }

    private void sendCoolingPearl(CommandSender sender) {
        sender.sendMessage(sep());
        sender.sendMessage(title("❄ Cooling Pearl"));
        sender.sendMessage(grid(" I ", "IPI", " I "));
        sender.sendMessage(Component.text("  I = Ice    P = Ender Pearl").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.text("  Right click to instantly remove 30 heat.").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(sep());
    }

    private void sendEmberRod(CommandSender sender) {
        sender.sendMessage(sep());
        sender.sendMessage(title("🔥 Ember Rod — Tier I"));
        sender.sendMessage(grid(" B ", " S ", " S "));
        sender.sendMessage(Component.text("  B = Blaze Rod    S = Stick").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.text("  Right Click: Pull nearby players toward you (15 heat)").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.text("  Left Click:  Blast yourself backward (15 heat)").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(sep());
    }

    private void sendScorchRod(CommandSender sender) {
        sender.sendMessage(sep());
        sender.sendMessage(title("💥 Scorch Rod — Tier II"));
        sender.sendMessage(grid("FBF", " B ", " B "));
        sender.sendMessage(Component.text("  B = Blaze Rod    F = Fire Charge").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.text("  Right Click: Ground Slam — crash down dealing heat damage (25 heat)").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.text("  Left Click:  Launch yourself into the air (25 heat)").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(sep());
    }

    private void sendInfernoRod(CommandSender sender) {
        sender.sendMessage(sep());
        sender.sendMessage(title("🌋 Inferno Rod — Tier III"));
        sender.sendMessage(grid("MBM", "MBM", " B "));
        sender.sendMessage(Component.text("  B = Blaze Rod    M = Magma Block").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.text("  Right Click: Steal 20 heat from nearest player (35 heat)").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.text("  Left Click:  Dump 15 heat onto nearest player (35 heat)").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(sep());
    }

    private void sendMoltenRod(CommandSender sender) {
        sender.sendMessage(sep());
        sender.sendMessage(title("☄ Molten Rod — Tier IV ⭐"));
        sender.sendMessage(grid("NLN", "MBM", "MBM"));
        sender.sendMessage(Component.text("  B = Blaze Rod    M = Magma Block").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.text("  N = Netherite Ingot    L = Lava Bucket").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.text("  Right Click: Molten Armor — absorb hits as heat (50 heat)").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.text("  Left Click:  Heat Nova — detonate all heat as damage (50 heat)").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.text("  ⚠ Heat Nova leaves you with Weakness + Slowness!").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(sep());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Component sep() {
        return Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private Component title(String text) {
        return Component.text("  " + text).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false);
    }

    private Component grid(String row1, String row2, String row3) {
        String fmt = "  [%s] [%s] [%s]\n  [%s] [%s] [%s]\n  [%s] [%s] [%s]";
        String r1 = padRow(row1), r2 = padRow(row2), r3 = padRow(row3);
        return Component.text(String.format("  [%c] [%c] [%c]", r1.charAt(0), r1.charAt(1), r1.charAt(2))).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            .appendNewline()
            .append(Component.text(String.format("  [%c] [%c] [%c]", r2.charAt(0), r2.charAt(1), r2.charAt(2))).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
            .appendNewline()
            .append(Component.text(String.format("  [%c] [%c] [%c]", r3.charAt(0), r3.charAt(1), r3.charAt(2))).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
    }

    private String padRow(String row) {
        // Remove spaces to get 3 chars
        return row.replace(" ", "_");
    }
}

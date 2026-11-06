package com.example.punchsnowball;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Locale;
/** Visual-only formatter. Never changes gameplay tags, enchantments, attributes or item models. */
final class FrostTooltips {
    private record Ability(String title, String keys, String description, String cooldownPath, int defaultSeconds) {}
    private record Profile(String id, String title, String motto, String passive,
                           int colorFrom, int colorTo, Ability[] abilities) {}
    private static Profile p(String id, String title, String motto, String passive,
                            int a, int b, Ability... abilities) {
        return new Profile(id, title, motto, passive, a, b, abilities);
    }
    private static Ability a(String title, String keys, String desc, String path, int seconds) {
        return new Ability(title, keys, desc, path, seconds);
    }
    static String identify(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        var data = item.getItemMeta().getPersistentDataContainer();
        if (data.has(NamespacedKey.fromString("punchsnowball:punch_snowball"), PersistentDataType.BYTE)) return "snowball";
        if (data.has(NamespacedKey.fromString("punchsnowball:infinite_tnt"), PersistentDataType.BYTE)) return "tnt";
        if (data.has(NamespacedKey.fromString("punchsnowball:freeze_windcharge"), PersistentDataType.BYTE)) return "windcharge";
        if (data.has(NamespacedKey.fromString("punchsnowball:storm_trident"), PersistentDataType.BYTE)) return "grapple";
        if (data.has(NamespacedKey.fromString("punchsnowball:riptide_trident"), PersistentDataType.BYTE)) return "riptide";
        if (data.has(NamespacedKey.fromString("punchsnowball:homing_firework"), PersistentDataType.BYTE)) return "firework";
        if (data.has(NamespacedKey.fromString("punchsnowball:lava_tsunami"), PersistentDataType.BYTE)) return "lava";
        if (data.has(NamespacedKey.fromString("punchsnowball:battleaxe"), PersistentDataType.BYTE)) return "battleaxe";
        if (data.has(NamespacedKey.fromString("punchsnowball:frost_shield"), PersistentDataType.BYTE)) return "shield";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_sword"), PersistentDataType.BYTE)) return "sword";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_spear"), PersistentDataType.BYTE)) return "spear";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_fortune_pickaxe"), PersistentDataType.BYTE)
                || data.has(NamespacedKey.fromString("punchsnowball:frozen_silk_pickaxe"), PersistentDataType.BYTE)) {
            String mode = data.get(NamespacedKey.fromString("punchsnowball:pickaxe_mode"), PersistentDataType.STRING);
            if ("silk".equals(mode)) return "pickaxe_silk";
            return item.getItemMeta().hasEnchant(org.bukkit.enchantments.Enchantment.SILK_TOUCH) ? "pickaxe_silk" : "pickaxe_fortune";
        }
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_bow"), PersistentDataType.BYTE)) return "bow";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_crossbow"), PersistentDataType.BYTE)) return "crossbow";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_helmet"), PersistentDataType.BYTE)) return "helmet";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_chestplate"), PersistentDataType.BYTE)) return "chestplate";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_leggings"), PersistentDataType.BYTE)) return "leggings";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_boots"), PersistentDataType.BYTE)) return "boots";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_fishing_rod"), PersistentDataType.BYTE)) return "rod";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_horn"), PersistentDataType.BYTE)) return "horn";
        if (data.has(NamespacedKey.fromString("punchsnowball:frozen_totem"), PersistentDataType.BYTE)) return "totem";
        return null;
    }
    private static Profile profile(String id, JavaPlugin plugin) {
        double bonusHearts = Math.max(0.0, Math.min(20.0,
            plugin.getConfig().getDouble("armor.chestplate.bonus-hearts", 5.0)));
        String heartText = (bonusHearts == Math.rint(bonusHearts))
            ? Long.toString(Math.round(bonusHearts))
            : String.format(Locale.ROOT, "%.1f", bonusHearts);
        boolean fireResistanceEnabled = plugin.getConfig().getBoolean("armor.leggings.fire-resistance-enabled", true);
        boolean miningHasteEnabled = plugin.getConfig().getBoolean("pickaxes.enabled", true)
                && plugin.getConfig().getBoolean("pickaxes.mining-haste.enabled", true);
        int miningHasteDelay = Math.max(1, plugin.getConfig().getInt("pickaxes.mining-haste.delay-seconds", 5));
        int miningHasteLevel = Math.max(1, Math.min(5,
                plugin.getConfig().getInt("pickaxes.mining-haste.amplifier", 1) + 1));
        String pickaxePassiveSummary = miningHasteEnabled
                ? "Haste " + roman(miningHasteLevel) + " after " + miningHasteDelay + " seconds of continuous mining."
                : "Mining Mastery is disabled in config.";
        String pickaxePassive = miningHasteEnabled
                ? "Mine continuously for " + miningHasteDelay + " seconds to gain Haste " + roman(miningHasteLevel)
                    + "; the effect ends when mining stops or you switch tools."
                : "Enable pickaxes.mining-haste.enabled and pickaxes.enabled in config to restore this passive.";
        return switch (id) {
            case "snowball" -> p("snowball", "Frozen Snowball", "Winter answers every throw.", "Infinite ammunition; each hit chills enemies.", 0x86F4FF, 0x2B8BD8,
                a("Frozen Knockback", "THROW", "Strong knockback + Slowness II", "snowball.cooldown-seconds", 10));
            case "tnt" -> p("tnt", "Frozen TNT of Destruction", "Ice hides an explosive heart.", "Reusable explosive; ignites immediately.", 0x9EEBFF, 0xE45DA6,
                a("Icy Detonation", "PLACE", "1.5s fuse; small blast through armor.", "tnt.cooldown-seconds", 10));
            case "windcharge" -> p("windcharge", "Frozen Windcharge of Coldness", "One breath locks the battlefield.", "Infinite icy charges.", 0x71F7FF, 0x51A1FF,
                a("Absolute Freeze", "THROW", "Freeze target movement briefly.", "windcharge.cooldown-seconds", 20));
            case "grapple" -> p("grapple", "Frozen Trident of Loyalty", "All paths lead back to winter.", "Loyalty III; returns to its wielder.", 0x9EF4FF, 0x286AE0,
                a("Frozen Grapple", "THROW", "Pull the hit target toward you.", "grapple.cooldown-seconds", 20));
            case "riptide" -> p("riptide", "Frozen Trident of the Depths", "The sea remembers thunder.", "Riptide III mobility anywhere; three lightning hits per ten successful attacks.", 0x7BF2FF, 0x5C66E9,
                a("Thunderfall", "RIPTIDE HIT", "Three armor-piercing lightning hits every ten successful, charged melee hits.", "riptide.cooldown-seconds", 20));
            case "firework" -> p("firework", "Frozen Firework of Cold Skies", "A thousand stars, none warm.", "Reusable launch; skyward mobility.", 0x98F2FF, 0xC576EE,
                a("Sky Hunter", "RIGHT-CLICK", "Launch five homing glowing rockets.", "firework.cooldown-seconds", 45));
            case "lava" -> p("lava", "Frozen Flint and Steel of Inferno", "Even a frozen world can burn.", "Unbreakable; creates no real lava.", 0xFFCE75, 0xEC614F,
                a("Inferno Wave", "RIGHT-CLICK", "6x6 lava wave; caster suffers fire pulses.", "lava.cooldown-seconds", 30));
            case "battleaxe" -> p("battleaxe", "Frozen Battleaxe", "The storm swings twice.", "Unbreakable enchanted axe.", 0xC6F2FF, 0x5194D7,
                a("Spinning Frost", "RIGHT-CLICK", "Throw an axe that freezes its target.", "battleaxe.cooldown-seconds", 40));
            case "shield" -> p("shield", "Frozen Shield of Defiance", "An unyielding wall of winter.", "Time your block to parry, launch attackers, and gain brief shield-break immunity.", 0x9BFFEC, 0x478BD6,
                a("Frozen Defiance", "SNEAK + RIGHT", "Perfect parry grants 5s shield-break immunity; failed parry stuns shield for 3s.", "shield.cooldown-seconds", 60));
            case "sword" -> p("sword", "Frozen Sword", "Leave winter in your wake.",
                "Sharpness V; +10% main-hand movement speed; melee damage capped at "
                    + String.format(Locale.ROOT, "%.1f", plugin.getConfig().getDouble("sword.melee-damage-health-points", 8.0) / 2.0)
                    + " hearts before armor.", 0xD5F6FF, 0x3A8CE8,
                a("Crystal Dash", "RIGHT-CLICK", "Lunge 9 blocks; each direct hit deals exactly 3 hearts through armor and applies Slowness.", "sword.cooldown-seconds", 25));
            case "spear" -> p("spear", "Frozen Spear of Ice Spikes", "The earth itself grows teeth.", "Unbreakable; Lunge III and Sharpness VI.", 0xAFF3FF, 0x3695B8,
                a("Glacial Eruption", "LEFT-CLICK", "An advancing line of ice spikes erupts.", "spear.cooldown-seconds", 30));
            case "pickaxe_fortune" -> p("pickaxe_fortune", "Frozen Pickaxe of Duality", "Unearth what winter guards.",
                "Fortune V active; 3x3 / 1x1 mining mode. " + pickaxePassiveSummary, 0xA7F7FF, 0x337EDB,
                a("Mining Mastery", "PASSIVE", pickaxePassive, null, 0),
                a("Duality Switch", "RIGHT-CLICK", "Toggle Fortune V / Silk Touch I.", null, 0),
                a("Mining Size", "SNEAK + RIGHT", "Toggle 3x3 area mining or normal 1x1 mining.", null, 0));
            case "pickaxe_silk" -> p("pickaxe_silk", "Frozen Pickaxe of Duality", "Unearth what winter guards.",
                "Silk Touch I active; 3x3 / 1x1 mining mode. " + pickaxePassiveSummary, 0x9DFAE5, 0x5068DE,
                a("Mining Mastery", "PASSIVE", pickaxePassive, null, 0),
                a("Duality Switch", "RIGHT-CLICK", "Toggle Silk Touch I / Fortune V.", null, 0),
                a("Mining Size", "SNEAK + RIGHT", "Toggle 3x3 area mining or normal 1x1 mining.", null, 0));
            case "bow" -> p("bow", "Frozen Bow of Arrowfall", "Clouds rain crystal arrows.", "Power VII; Infinity I; Flame V.", 0xC4F3FF, 0x5065DF,
                a("Arrowfall", "SNEAK + RIGHT", "Prepare, charge, fire a 10x10 arrow rain.", "bow.cooldown-seconds", 45));
            case "crossbow" -> p("crossbow", "Frozen Crossbow of the Warden", "Sculk sings beneath the ice.", "Multishot icicles deal half a heart each.", 0x56F6E3, 0x236FAC,
                a("Warden Shriek", "SNEAK + RIGHT", "Next shot sends a 20-block sonic blast.", "crossbow.shriek-cooldown-seconds", 40));
            case "helmet" -> p("helmet", "Frozen Helmet of Revelation", "Nothing escapes the blizzard.", "Protection III; Strength I while worn.", 0xA5F8FF, 0x5288D8,
                a("Winter Sight", "SNEAK 5s", "Reveal nearby players within 15 blocks.", "armor.helmet.reveal-cooldown-seconds", 120));
            case "chestplate" -> p("chestplate", "Frozen Chestplate of Vitality", "A second wind beneath the ice.", "Protection III; 8 armor; 3 armor toughness; +" + heartText + " hearts while worn.", 0xE4FCFF, 0x658EE9,
                a("Vitality", "PASSIVE", "Gain additional maximum hearts while wearing this chestplate.", null, 0));
            case "leggings" -> p("leggings", "Frozen Leggings of Fire Resistance", "Walk through flame untouched.", fireResistanceEnabled ? "Protection III; Fire Resistance I while equipped." : "Fire Resistance passive disabled in config.", 0xFFCC83, 0xD76A47,
                a("Ember Ward", "PASSIVE", fireResistanceEnabled ? "Fire Resistance I while these leggings are worn." : "Enable armor.leggings.fire-resistance-enabled in config to restore this passive.", null, 0));
            case "boots" -> p("boots", "Frozen Boots of Freedom", "Run across the edge of the world.", "Protection III; Speed II; immune to fall damage.", 0xB9FAFF, 0x4A8BC7,
                a("Coldstride", "PASSIVE", "Move swiftly and land without fall damage.", null, 0));
            case "rod" -> p("rod", "Frozen Fishing Rod", "A tether to the frozen world.", "Anchor persists when item is put away.", 0xA7EEFF, 0x498ED7,
                a("Ice Anchor", "RIGHT-CLICK", "Attach to a solid block; use again to pull.", null, 0),
                a("Anchor Break", "40+ BLOCKS", "Beyond the limit, the anchor breaks.", null, 0));
            case "horn" -> p("horn", "Frozen Horn of the Hordes", "Answer the call of winter.", "Summons armored melee Strays and Power II archer Strays; they ignore trusted players.", 0xDFF3FF, 0x6A83F4,
                a("Call of the Strays", "RIGHT-CLICK", "Summon sword-wielding undead allies.", "horn.cooldown-seconds", 60));
            case "totem" -> p("totem", "Frozen Totem of Winter's Blessing", "Winter favors the loyal.", "Aura heals friends and weakens enemies.", 0x98F5EA, 0x756CEB,
                a("Winter Blessing", "RIGHT-CLICK", "A moving aura of Regen I and Weakness I.", "totem.cooldown-seconds", 45));
            default -> null;
        };
    }
    private static String roman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }
    private static Component noItalic(Component c) { return c.decoration(TextDecoration.ITALIC, false); }
    private static Component solid(String value, int color) {
        return noItalic(Component.text(value, TextColor.color(color)));
    }
    private static Component gradient(String value, int start, int end) {
        Component output = Component.empty();
        int steps = Math.max(1, value.length() - 1);
        for (int i = 0; i < value.length(); i++) {
            double t = (double) i / steps;
            int r = (int) Math.round(((start >> 16) & 255) * (1 - t) + ((end >> 16) & 255) * t);
            int g = (int) Math.round(((start >> 8) & 255) * (1 - t) + ((end >> 8) & 255) * t);
            int b = (int) Math.round((start & 255) * (1 - t) + (end & 255) * t);
            output = output.append(solid(String.valueOf(value.charAt(i)), (r << 16) | (g << 8) | b));
        }
        return output;
    }
    private static void addWrapped(List<Component> lore, String text, int color) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() > 0 && line.length() + word.length() + 1 > 43) {
                lore.add(solid(line.toString(), color));
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) lore.add(solid(line.toString(), color));
    }
    static boolean apply(JavaPlugin plugin, ItemStack item) {
        String id = identify(item);
        if (id == null) return false;
        Profile profile = profile(id, plugin);
        if (profile == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (plugin.getConfig().getBoolean("tooltips.gradient-text", true)) {
            meta.displayName(gradient("✦ " + profile.title() + " ✦", profile.colorFrom(), profile.colorTo()));
            List<Component> lore = new ArrayList<>();
            lore.add(gradient("✧ FROSTPVP   •   LEGENDARY GEAR ✧", profile.colorFrom(), profile.colorTo()));
            lore.add(solid(profile.motto(), 0xB4BDCE));
            lore.add(Component.empty());
            lore.add(gradient("PASSIVE   |   WINTER'S GIFT", profile.colorFrom(), profile.colorTo()));
            addWrapped(lore, profile.passive(), 0xA8F2F9);
            for (int i = 0; i < profile.abilities().length; i++) {
                Ability ability = profile.abilities()[i];
                lore.add(Component.empty());
                lore.add(gradient((i + 1) + "  |  " + ability.title().toUpperCase(), profile.colorFrom(), profile.colorTo()));
                String detail = ability.keys();
                if (ability.cooldownPath() != null) {
                    detail += "  •  " + plugin.getConfig().getInt(ability.cooldownPath(), ability.defaultSeconds()) + "s";
                }
                lore.add(solid(detail, 0x89C7FF));
                addWrapped(lore, ability.description(), 0xD2D8E6);
            }
            lore.add(Component.empty());
            lore.add(gradient("❄ FROST CLAIMS THE WORTHY ❄", profile.colorFrom(), profile.colorTo()));
            meta.lore(lore);
        }
        if (!plugin.getConfig().getBoolean("tooltips.gradient-text", true)
                && ("pickaxe_fortune".equals(id) || "pickaxe_silk".equals(id))) {
            meta.displayName(noItalic(Component.text(profile.title())));
            List<Component> plainLore = new ArrayList<>();
            plainLore.add(noItalic(Component.text("FROSTPVP  |  FROZEN PICKAXE")));
            plainLore.add(Component.empty());
            plainLore.add(noItalic(Component.text("PASSIVE  |  MINING MASTERY")));
            addWrapped(plainLore, profile.passive(), 0xA8F2F9);
            plainLore.add(Component.empty());
            plainLore.add(noItalic(Component.text("RIGHT-CLICK  |  Switch Fortune V / Silk Touch I")));
            plainLore.add(noItalic(Component.text("SNEAK + RIGHT-CLICK  |  Toggle 3x3 / 1x1 mining")));
            meta.lore(plainLore);
        }
        // Only customizes tooltips, NEVER the item model or worn armor texture.
        boolean backgrounds = plugin.getConfig().getBoolean("tooltips.custom-background-enabled", false);
        meta.setTooltipStyle(backgrounds ? NamespacedKey.fromString("frost:" + id) : null);
        item.setItemMeta(meta);
        return true;
    }
}

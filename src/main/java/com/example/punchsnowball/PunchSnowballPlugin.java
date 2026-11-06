package com.example.punchsnowball;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.player.PlayerShieldDisableEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import net.kyori.adventure.text.Component;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ArmorStand;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.BoundingBox;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.FluidCollisionMode;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.event.entity.EntityDamageByBlockEvent;

import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.util.StringUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.Event;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.projectiles.ProjectileSource;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.entity.Stray;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.AbstractArrow;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public final class PunchSnowballPlugin extends JavaPlugin implements Listener, TabCompleter {
    private FrostEvents frostEvents;
    private final Map<UUID, Long> lastPlayerChatAtNanos = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastPlayerChatMessage = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPlayerChatMessageAt = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> combatOpponents = new ConcurrentHashMap<>();
    private final Map<UUID, Long> combatExpiresAt = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> combatBars = new ConcurrentHashMap<>();
    private volatile long chatCooldownNanos = 1_500_000_000L;
    // Trust is directional: a player protects the people THEY trust from THEIR weapons.
    // Stored by UUID to survive player name changes and plugin/server restarts.
    private final Map<UUID, Map<UUID, String>> trustedPlayers = new HashMap<>();
    private File trustFile;
    private FileConfiguration trustFileData;

    private record GuiSetting(String path, String title, Material icon,
                              double step, double minimum, double maximum,
                              boolean integer, boolean toggle) {}

    private static GuiSetting num(String path, String title, Material icon,
                                  double step, double min, double max, boolean integer) {
        return new GuiSetting(path, title, icon, step, min, max, integer, false);
    }
    private static GuiSetting flag(String path, String title, Material icon) {
        return new GuiSetting(path, title, icon, 0, 0, 1, false, true);
    }

    private static final Map<String, List<GuiSetting>> GUI_SETTINGS = new LinkedHashMap<>();
    static {
        GUI_SETTINGS.put("snowball", List.of(
            num("snowball.cooldown-seconds", "Cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("snowball.slowness-seconds", "Slowness (seconds)", Material.COBWEB, 1, 1, 60, true),
            num("snowball.horizontal-velocity", "Horizontal knockback", Material.FEATHER, 0.1, 0, 5, false),
            num("snowball.vertical-velocity", "Vertical knockback", Material.SLIME_BALL, 0.05, 0, 2, false)
        ));
        GUI_SETTINGS.put("tnt", List.of(
            num("tnt.cooldown-seconds", "Cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("tnt.fuse-seconds", "Fuse (seconds)", Material.REDSTONE_TORCH, 0.5, 0.5, 30, false),
            num("tnt.radius-blocks", "Effect radius (blocks)", Material.TNT, 0.5, 0.5, 16, false),
            num("tnt.explosion-power", "Explosion strength", Material.FIRE_CHARGE, 0.25, 0, 4, false),
            flag("tnt.break-blocks", "Break blocks", Material.IRON_PICKAXE),
            num("tnt.damage-health-points", "Damage (health points)", Material.REDSTONE, 1, 0, 40, false)
        ));
        GUI_SETTINGS.put("windcharge", List.of(
            num("windcharge.cooldown-seconds", "Cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("windcharge.freeze-seconds", "Freeze duration (seconds)", Material.POWDER_SNOW_BUCKET, 1, 1, 30, true),
            flag("windcharge.powder-snow-overlay", "Powder snow screen", Material.SNOW_BLOCK)
        ));
        GUI_SETTINGS.put("grapple", List.of(
            num("grapple.cooldown-seconds", "Grapple cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("grapple.horizontal-velocity", "Pull speed", Material.LEAD, 0.1, 0.1, 3, false),
            num("grapple.vertical-velocity", "Vertical pull", Material.FEATHER, 0.05, 0, 1.2, false),
            num("trident.fire-seconds", "Fire duration (seconds)", Material.FLINT_AND_STEEL, 1, 0, 60, true)
        ));
        GUI_SETTINGS.put("riptide", List.of(
            num("riptide.cooldown-seconds", "Cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("riptide.damage-per-lightning-hit-health-points", "Damage per lightning hit", Material.LIGHTNING_ROD, 1, 0, 40, false),
            num("riptide.ticks-between-strikes", "Ticks between strikes", Material.REPEATER, 1, 1, 100, true)
        ));
        GUI_SETTINGS.put("firework", List.of(
            num("firework.cooldown-seconds", "Cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("firework.missiles", "Number of rockets", Material.FIREWORK_ROCKET, 1, 1, 20, true),
            num("firework.damage-health-points", "Damage per rocket", Material.REDSTONE, 0.5, 0, 40, false),
            num("firework.target-range-blocks", "Range from launcher (blocks)", Material.SPYGLASS, 1, 1, 160, false),
            num("firework.sky-delay-ticks", "Sky launch delay (ticks)", Material.CLOCK, 5, 5, 100, true),
            num("firework.homing-speed", "Rocket speed", Material.FEATHER, 0.05, 0.2, 2, false),
            num("firework.glowing-seconds", "Glowing on impact (seconds)", Material.GLOWSTONE_DUST, 1, 1, 120, true)
        ));
        GUI_SETTINGS.put("spear", List.of(
            num("spear.cooldown-seconds", "Cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("spear.spike-count", "Number of ice spikes", Material.PACKED_ICE, 1, 1, 24, true),
            num("spear.spike-spacing-blocks", "Spacing between spikes", Material.ICE, 0.25, 0.5, 4.0, false),
            num("spear.spike-distance-growth", "Extra distance per new spike", Material.SPYGLASS, 0.05, 0, 0.8, false),
            num("spear.spike-size-growth", "Ice spike size growth", Material.PACKED_ICE, 0.05, 0, 0.65, false),
            num("spear.ticks-between-spikes", "Delay between spikes (ticks)", Material.REPEATER, 1, 1, 20, true),
            num("spear.damage-health-points", "Damage per target (health)", Material.REDSTONE, 0.5, 0, 30, false),
            num("spear.launch-velocity", "Launch enemies upward", Material.FEATHER, 0.05, 0.1, 2.0, false)
        ));
        GUI_SETTINGS.put("totem", List.of(
            num("totem.cooldown-seconds", "Cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("totem.active-seconds", "Aura duration (seconds)", Material.SNOW_BLOCK, 1, 1, 90, true),
            num("totem.radius-blocks", "Aura radius (blocks)", Material.SPYGLASS, 1, 1, 24, true),
            num("totem.effect-refresh-ticks", "Refresh effects (ticks)", Material.REPEATER, 1, 5, 40, true)
        ));
        GUI_SETTINGS.put("sword", List.of(
            num("sword.cooldown-seconds", "Cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("sword.dash-blocks", "Dash length (blocks)", Material.FEATHER, 1, 1, 30, true),
            num("sword.dash-speed-blocks-per-tick", "Initial lunge strength", Material.SUGAR, 0.1, 0.6, 3.0, false),
            num("sword.damage-health-points", "Direct-hit damage (health)", Material.REDSTONE, 0.5, 0, 40, false),
            num("sword.trail-duration-seconds", "Trail persists (seconds)", Material.BLUE_ICE, 1, 1, 20, true),
            num("sword.slowness-seconds", "Trail slowness duration", Material.COBWEB, 1, 1, 30, true),
            num("sword.slowness-level", "Slowness level", Material.POWDER_SNOW_BUCKET, 1, 1, 5, true)
        ));
        GUI_SETTINGS.put("shield", List.of(
            num("shield.cooldown-seconds", "Cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("shield.active-seconds", "Stun immunity duration", Material.SHIELD, 1, 1, 60, true),
            num("shield.knockback-horizontal", "Reflect strength", Material.FEATHER, 0.05, 0.1, 3, false),
            num("shield.knockback-vertical", "Reflect lift", Material.SLIME_BALL, 0.05, 0.0, 1.5, false)
        ));
        GUI_SETTINGS.put("battleaxe", List.of(
            num("battleaxe.cooldown-seconds", "Cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("battleaxe.stun-seconds", "Freeze on hit (seconds)", Material.POWDER_SNOW_BUCKET, 1, 1, 30, true),
            num("battleaxe.throw-speed", "Axe travel speed", Material.FEATHER, 0.1, 0.2, 3.0, false),
            num("battleaxe.max-range-blocks", "Maximum travel distance", Material.SPYGLASS, 1, 4, 96, false)
        ));
        GUI_SETTINGS.put("bow", List.of(
            num("bow.cooldown-seconds", "Bow ability cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("bow.rain-duration-seconds", "Arrow rain duration (seconds)", Material.ARROW, 1, 1, 30, true),
            num("bow.radius-blocks", "Rain side length (blocks)", Material.SPYGLASS, 1, 2, 32, true),
            num("bow.damage-health-points", "Damage per arrow (health)", Material.REDSTONE, .5, 0, 20, false),
            num("bow.blindness-seconds", "Blindness duration (seconds)", Material.INK_SAC, 1, 1, 60, true),
            num("bow.arrows-per-wave", "Arrows every wave", Material.SPECTRAL_ARROW, 1, 1, 10, true),
            num("bow.wave-interval-ticks", "Ticks between waves", Material.REPEATER, 1, 2, 20, true)
        ));
        GUI_SETTINGS.put("rod", List.of(
            num("rod.max-anchor-distance-blocks", "Anchor break distance", Material.LEAD, 1, 10, 80, true),
            num("rod.pull-strength", "Grapple pull strength", Material.FISHING_ROD, 0.15, 0.4, 5.0, false)
        ));
        GUI_SETTINGS.put("tooltips", List.of(
            flag("tooltips.custom-background-enabled", "Custom tooltip backgrounds", Material.NAME_TAG)
        ));
        GUI_SETTINGS.put("chat", List.of(
            num("chat.cooldown-seconds", "Chat cooldown (seconds)", Material.CLOCK, 0.5, 0, 15, false),
            flag("chat.anti-repeat-enabled", "Block repeated messages", Material.PAPER),
            num("chat.anti-repeat-window-seconds", "Repeat detection window", Material.REPEATER, 5, 5, 300, true)
        ));
        GUI_SETTINGS.put("cooldowns", List.of(
            num("cooldowns.windcharge-seconds", "Wind Charge cooldown", Material.WIND_CHARGE, 1, 0, 300, true),
            num("cooldowns.ender-pearl-seconds", "Ender Pearl cooldown", Material.ENDER_PEARL, 1, 0, 300, true),
            num("cooldowns.lunge-seconds", "Lunge cooldown", Material.NETHERITE_SPEAR, 1, 0, 300, true)
        ));
        GUI_SETTINGS.put("combat", List.of(
            flag("combat.enabled", "Enable combat tagging", Material.IRON_SWORD),
            num("combat.tag-seconds", "Combat timer (seconds)", Material.CLOCK, 1, 5, 120, true),
            flag("combat.kill-on-logout", "Kill combat loggers", Material.SKELETON_SKULL),
            flag("combat.broadcast-combat-log", "Broadcast combat log message", Material.BELL)
        ));
        GUI_SETTINGS.put("hud", List.of(
            flag("hud.enabled", "Show cooldown action bar", Material.CLOCK)
        ));
        GUI_SETTINGS.put("visuals", List.of(
            flag("visuals.passive-frost-particles", "Particles around Frost item holders", Material.SNOWBALL),
            num("visuals.particle-interval-ticks", "Particle interval (ticks)", Material.CLOCK, 1, 2, 60, true),
            num("visuals.particle-count", "Particles per burst", Material.SNOWBALL, 1, 1, 20, true)
        ));
        GUI_SETTINGS.put("horn", List.of(
            num("horn.cooldown-seconds", "Cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("horn.skeleton-count", "Skeletons spawned", Material.SKELETON_SKULL, 1, 1, 20, true),
            num("horn.despawn-seconds", "Minion lifetime", Material.SOUL_SAND, 1, 5, 180, true),
            num("horn.spawn-radius-blocks", "Spawn radius", Material.BONE, 1, 2, 20, true)
        ));
        GUI_SETTINGS.put("crossbow", List.of(
            num("crossbow.shriek-cooldown-seconds", "Shriek cooldown (seconds)", Material.CLOCK, 5, 5, 300, true),
            num("crossbow.shriek-range-blocks", "Shriek range (blocks)", Material.SCULK_SHRIEKER, 1, 5, 50, true),
            num("crossbow.shriek-damage-health-points", "Shriek damage (health)", Material.REDSTONE, 1, 0, 40, false),
            num("crossbow.icicle-damage-health-points", "Icicle hit damage (health)", Material.PACKED_ICE, 0.5, 0, 20, false)
        ));
        GUI_SETTINGS.put("helmet", List.of(
            num("armor.helmet.reveal-radius-blocks", "Reveal radius", Material.SPYGLASS, 1, 1, 40, true),
            num("armor.helmet.crouch-seconds", "Crouch charge time", Material.CLOCK, 1, 1, 15, true),
            num("armor.helmet.reveal-cooldown-seconds", "Reveal cooldown", Material.CLOCK, 10, 10, 600, true),
            num("armor.helmet.glowing-seconds", "Reveal glow duration", Material.GLOWSTONE_DUST, 1, 1, 60, true)
        ));
        GUI_SETTINGS.put("chestplate", List.of(
            num("armor.chestplate.bonus-hearts", "Bonus maximum hearts", Material.GOLDEN_APPLE, 0.5, 0, 20, false)
        ));
        GUI_SETTINGS.put("leggings", List.of(
            flag("armor.leggings.fire-resistance-enabled", "Enable Fire Resistance", Material.BLAZE_POWDER)
        ));
        GUI_SETTINGS.put("boots", List.of(
            flag("armor.boots.speed-enabled", "Enable Speed II", Material.SUGAR),
            flag("armor.boots.fall-immunity", "Ignore fall damage", Material.FEATHER)
        ));
        GUI_SETTINGS.put("pickaxe", List.of(
            flag("pickaxes.enabled", "Pickaxe mining enabled", Material.NETHERITE_PICKAXE),
            flag("pickaxes.mining-haste.enabled", "Mining Mastery passive", Material.POTION),
            num("pickaxes.mining-haste.delay-seconds", "Mining time to activate Haste", Material.CLOCK, 1, 1, 30, true),
            num("pickaxes.mining-haste.amplifier", "Haste amplifier (0 = Haste I)", Material.GLOWSTONE_DUST, 1, 0, 4, true)
        ));
        GUI_SETTINGS.put("lava", List.of(
            num("lava.cooldown-seconds", "Cooldown (seconds)", Material.CLOCK, 1, 1, 300, true),
            num("lava.radius-blocks", "Half-width of wave (blocks)", Material.LAVA_BUCKET, 0.5, 0.5, 12, false),
            num("lava.damage-health-points", "Damage (health points)", Material.BLAZE_POWDER, 1, 0, 40, false),
            num("lava.wave-duration-ticks", "Wave duration (ticks)", Material.MAGMA_BLOCK, 2, 4, 100, true),
            num("lava.fire-pulses", "Burn pulses (half-hearts)", Material.BLAZE_POWDER, 1, 0, 10, true),
            num("lava.fire-pulse-health-points", "Burn damage per pulse", Material.FIRE_CHARGE, 0.5, 0, 10, false)
        ));
    }
        // This group edits the shared 3x3 mining behavior of both custom pickaxes.
    private static final Map<String, List<String>> CONFIG_CATEGORIES = Map.of(
        "weapons", List.of("snowball", "tnt", "windcharge", "grapple", "riptide", "firework", "lava", "battleaxe", "shield", "sword", "spear", "totem", "pickaxe", "bow", "crossbow", "rod", "horn"),
        "armor", List.of("helmet", "chestplate", "leggings", "boots"),
        "server", List.of("chat", "cooldowns", "combat", "hud"),
        "presentation", List.of("visuals", "tooltips")
    );
    private static final Map<Integer, String> CATEGORY_SLOTS = Map.of(
        20, "weapons", 22, "armor", 24, "server", 31, "presentation"
    );
    private static final int[] CATEGORY_GROUP_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int[] SETTING_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    private static final class FrostMenu implements InventoryHolder {
        final String group;
        final Inventory inventory;
        FrostMenu(String group) {
            this.group = group;
            this.inventory = Bukkit.createInventory(this, 54,
                    Component.text(group.isEmpty() ? "Frost - Weapon Config" : "Frost - " + group));
        }
        @Override public Inventory getInventory() { return inventory; }
    }

    private static final class FrostWeaponMenu implements InventoryHolder {
        final Inventory inventory;
        FrostWeaponMenu() {
            inventory = Bukkit.createInventory(this, 45, Component.text("Frost - Weapon Collection"));
        }
        @Override public Inventory getInventory() { return inventory; }
    }

    private static final Map<Integer, String> WEAPON_SLOTS = Map.ofEntries(
        Map.entry(10, "snowball"), Map.entry(11, "tnt"),
        Map.entry(12, "windcharge"), Map.entry(13, "grapple"),
        Map.entry(14, "riptide"), Map.entry(15, "firework"),
        Map.entry(16, "lava"), Map.entry(21, "sword"),
        Map.entry(22, "battleaxe"), Map.entry(23, "shield"),
        Map.entry(24, "spear"), Map.entry(25, "totem"),
        Map.entry(19, "pickaxe"), Map.entry(20, "bow"),
        Map.entry(28, "crossbow"), Map.entry(29, "helmet"),
        Map.entry(30, "chestplate"), Map.entry(31, "leggings"), Map.entry(32, "boots"),
        Map.entry(17, "rod"), Map.entry(26, "horn")
    );

    private NamespacedKey crossbowKey;
    private NamespacedKey icicleBoltKey;
    private final Set<UUID> activeIcicleBolts = new HashSet<>();
    private NamespacedKey shriekBoltKey;
    private final Set<UUID> preparedShrieks = new HashSet<>();
    private final Map<UUID, Long> nextShriekUse = new HashMap<>();
    private int shriekCooldownTicks;
    private double shriekRange, shriekDamage, icicleDamage;

    private NamespacedKey helmetKey, chestplateKey, leggingsKey, bootsKey;
    private final Map<UUID, Long> helmetSneakSince = new HashMap<>();
    private final Map<UUID, Long> nextRevealUse = new HashMap<>();
    private final Set<UUID> helmetNeedsStandUp = new HashSet<>();
    private int revealCrouchTicks, revealCooldownTicks, revealGlowTicks;
    private double revealRadius;
    private boolean leggingsFireResistanceEnabled, bootsSpeedEnabled, bootsFallImmunity;
    private final Map<UUID, Long> lavaBurnUntil = new HashMap<>();
    private int lavaFirePulses;
    private double lavaFireDamage;

    private NamespacedKey fortunePickaxeKey;
    private NamespacedKey pickaxeModeKey;
    private NamespacedKey pickaxeMiningModeKey;
    private NamespacedKey bowKey;
    private NamespacedKey preparedArrowKey;
    private NamespacedKey fallingArrowKey;
    private NamespacedKey bowOwnerKey;
    private final Set<UUID> preparedBows = new HashSet<>();
    private final Map<UUID, Long> nextBowUse = new HashMap<>();
    private int bowCooldownTicks, bowRainTicks, bowWaveIntervalTicks, bowArrowsPerWave, bowBlindnessTicks;
    private double bowRainSide, bowArrowDamage;
    private NamespacedKey silkPickaxeKey;
    private boolean pickaxesEnabled;
    private int pickaxesRadius;
    private final Map<UUID, BlockFace> lastMiningFace = new HashMap<>();
    private final Map<UUID, Location> lastMiningClick = new HashMap<>();
    private final Set<UUID> activeMiningBursts = new HashSet<>();
    // Pickaxe passive: continuous mining for the configured delay grants Haste II.
    // Swing timestamps distinguish active block mining from merely holding the tool.
    private final Map<UUID, Long> pickaxeMiningStartedAt = new HashMap<>();
    private final Map<UUID, Long> pickaxeLastMiningSwingAt = new HashMap<>();
    private final Map<UUID, PotionEffect> previousPickaxeHaste = new HashMap<>();
    private final Set<UUID> pickaxeHasteApplied = new HashSet<>();

    private NamespacedKey spearKey;
    private NamespacedKey totemKey;
    private NamespacedKey fishingRodKey;
    private NamespacedKey hornKey;
    private NamespacedKey hornOwnerKey;
    private NamespacedKey[] frostItemMarkers;
    private boolean passiveFrostParticles = true;
    private int frostParticleInterval = 10;
    private int frostParticleCount = 5;
    private int spearCooldownTicks, spearSpikeCount, spearSpikeInterval;
    private double spearSpikeSpacing, spearDistanceGrowth, spearSizeGrowth, spearDamage, spearLaunchVelocity;
    private int totemCooldownTicks, totemActiveTicks, totemEffectRefresh;
    private double totemRadius;
    private final Map<UUID, Long> nextSpearUse = new HashMap<>();
    private final Map<UUID, Long> nextTotemUse = new HashMap<>();
    private final Map<UUID, Long> activeTotemAuras = new HashMap<>();
    private final List<FlyingSpear> flyingSpears = new ArrayList<>();

    private static final class FlyingSpear {
        final ArmorStand visual;
        final UUID owner;
        final Vector heading;
        final Vector horizontal;
        final Set<UUID> damaged = new HashSet<>();
        int age = 0;
        FlyingSpear(ArmorStand visual, UUID owner, Vector heading, Vector horizontal) {
            this.visual = visual;
            this.owner = owner;
            this.heading = heading;
            this.horizontal = horizontal;
        }
    }

    private NamespacedKey swordKey;
    private NamespacedKey frozenSwordSpeedKey;
    private NamespacedKey frozenSwordPlayerSpeedKey;
    private double swordMeleeDamage;
    private int swordCooldownTicks;
    private double swordDashBlocks;
    private double swordDashSpeed;
    private double swordDamage;
    private int swordTrailTicks;
    private int swordSlowTicks;
    private int swordSlowLevel;
    private final Map<UUID, Long> nextSwordUse = new HashMap<>();
    private final Map<UUID, SwordDash> activeDashes = new HashMap<>();
    private final List<IceTrailPoint> swordTrails = new ArrayList<>();

    // Duel invitations are held only while players are online and expire in ticks.
    private final Map<UUID, Map<UUID, Long>> pendingDuels = new HashMap<>();
    private final Map<UUID, UUID> activeDuelPartners = new HashMap<>();
    private final Set<UUID> duelCountdownPlayers = new HashSet<>();
    private final Map<UUID, Location> duelCountdownAnchors = new HashMap<>();
    private int duelRequestTimeoutTicks = 1200;
    private int duelArenaSearchAttempts = 120;
    private int duelCountdownSeconds = 3;
    private double duelSeparationBlocks = 20.0;

    private record DuelSpots(Location first, Location second) {}

    // Dash moves a limited distance each server tick, stopping at solid blocks.
    // A hit list ensures each target takes direct-hit damage just once per dash.
    private static final class SwordDash {
        final UUID userId;
        final Vector direction;
        final Set<UUID> hit = new HashSet<>();
        double traveled = 0;
        int age = 0;
        Location lastPosition;
        SwordDash(UUID userId, Vector direction, Location origin) {
            this.userId = userId;
            this.direction = direction;
            this.lastPosition = origin.clone();
        }
    }

    private static final class IceTrailPoint {
        final UUID ownerId;
        final Location location;
        final long expires;
        IceTrailPoint(UUID ownerId, Location location, long expires) {
            this.ownerId = ownerId;
            this.location = location;
            this.expires = expires;
        }
    }

    private NamespacedKey shieldKey;
    private int shieldCooldownTicks;
    private int shieldActiveTicks;
    private double shieldReflectHorizontal;
    private double shieldReflectVertical;
    private final Map<UUID, Long> nextShieldUse = new HashMap<>();
    private final Map<UUID, Long> shieldActiveUntil = new HashMap<>();
    // A parry is successful only shortly after the player raises the custom shield.
    private final Map<UUID, Long> shieldRaisedAt = new HashMap<>();
    private final Map<UUID, Long> shieldStunnedUntil = new HashMap<>();
    private int shieldParryWindowTicks;
    private int shieldStunTicks;
    // Saved ready messages by user + weapon type; expire exactly once.
    private final Map<UUID, Map<String, Long>> readyAt = new HashMap<>();
    private final Map<UUID, Long> snowballNext = new HashMap<>();

    private NamespacedKey battleaxeKey;
    private final Map<UUID, Long> nextBattleaxeUse = new HashMap<>();
    private final List<FlyingAxe> activeAxes = new ArrayList<>();
    private int battleaxeCooldownTicks;
    private int battleaxeStunTicks;
    private double battleaxeSpeed;
    private double battleaxeRange;

    private static final class FlyingAxe {
        final ArmorStand display;
        final UUID ownerId;
        final Vector velocity;
        final double maxDistance;
        double traveled = 0;
        int age = 0;
        FlyingAxe(ArmorStand display, UUID ownerId, Vector velocity, double maxDistance) {
            this.display = display;
            this.ownerId = ownerId;
            this.velocity = velocity;
            this.maxDistance = maxDistance;
        }
    }

    private NamespacedKey snowballKey;
    private NamespacedKey tntKey;
    private NamespacedKey tntOwnerKey;
    private NamespacedKey freezeChargeKey;
    private NamespacedKey tridentKey;
    private final Map<UUID, Long> nextWindUse = new HashMap<>();
    private final Map<UUID, Long> nextTridentUse = new HashMap<>();
    private int tridentCooldownTicks;
    private double lightningDamage;
    private int lightningSpacingTicks;
    private int tridentFireTicks;
    private final Map<UUID, FrozenPlayer> frozenPlayers = new HashMap<>();
    private final Map<UUID, FrozenMob> frozenMobs = new HashMap<>();

    private static final class FrozenMob {
        final boolean previouslyHadAI;
        final int previousFreezeTicks;
        long releaseTick;
        FrozenMob(boolean ai, int frozenTicks, long until) {
            previouslyHadAI = ai;
            previousFreezeTicks = frozenTicks;
            releaseTick = until;
        }
    }

    private int windCooldownTicks;
    private int freezeDurationTicks;
    private boolean windFreezeOverlay;

    private static final class FrozenPlayer {
        final Location anchor;
        final int previousFreezeTicks;
        long releaseTick;
        FrozenPlayer(Location anchor, int previousFreezeTicks, long releaseTick) {
            this.anchor = anchor;
            this.previousFreezeTicks = previousFreezeTicks;
            this.releaseTick = releaseTick;
        }
    }
    private final Map<UUID, Long> nextTntUse = new HashMap<>();
    private NamespacedKey riptideTridentKey;
    private NamespacedKey fireworksKey;
    private NamespacedKey lavaKey;
    private final Map<UUID, Long> nextLavaUse = new HashMap<>();
    private int lavaCooldownTicks;
    private double lavaRadius;
    private double lavaDamage;
    private int lavaWaveTicks;
    private final Map<UUID, GrapplePull> activeGrapples = new HashMap<>();
    private static final class GrapplePull {
        final UUID shooterId;
        final UUID victimId;
        final long expiresAt;
        GrapplePull(UUID shooterId, UUID victimId, long expiresAt) {
            this.shooterId = shooterId;
            this.victimId = victimId;
            this.expiresAt = expiresAt;
        }
    }
    private final Map<UUID, Long> nextRiptideAbility = new HashMap<>();
    private final Map<UUID, Integer> riptideHitCounters = new HashMap<>();
    private double hornArcherChance = 0.25;
    private final Map<UUID, Long> nextFireworkUse = new HashMap<>();
    private final Map<UUID, Location> fishingAnchors = new HashMap<>();
    // Keep the solid block separately from the player-facing grapple point.
    private final Map<UUID, Location> fishingAnchorBlocks = new HashMap<>();
    private double anchorBreakDistance = 40.0;
    private double anchorPullStrength = 2.75;
    private final Map<UUID, Long> nextHornUse = new HashMap<>();
    private int hornCooldownTicks = 20 * 60;
    private int hornSkeletonCount = 5;
    private int hornDespawnTicks = 20 * 25;
    private double hornSpawnRadius = 6.0;
    private final List<HomingRocket> activeRockets = new ArrayList<>();
    private int grappleCooldownTicks;
    private double grapplePullHorizontal;
    private double grapplePullVertical;
    private int riptideCooldownTicks;
    private int fireworkCooldownTicks;
    private int fireworkCount;
    private double fireworkDamage;
    private double fireworkTargetRange;
    private int fireworkSkyDelay;
    private double fireworkSpeed;
    private int fireworkGlowingTicks;

    private static final class HomingRocket {
        final Firework rocket;
        final UUID targetId;
        final UUID ownerId;
        final long expiresAt;
        HomingRocket(Firework rocket, UUID targetId, UUID ownerId, long expiresAt) {
            this.rocket = rocket;
            this.targetId = targetId;
            this.ownerId = ownerId;
            this.expiresAt = expiresAt;
        }
    }


    private int snowballCooldownTicks;
    private int slownessTicks;
    private double snowballHorizontal;
    private double snowballVertical;
    private int tntCooldownTicks;
    private int tntFuseTicks;
    private double tntRadius;
    private float tntPower;
    private boolean tntBreakBlocks;
    private double tntDamage;

    /**
     * Paper chooses the data directory from plugin.yml's name. When upgrading
     * PunchSnowball -> FrostPVP, copy saved settings and trust lists exactly
     * once, before any default config is created. Existing FrostPVP files are
     * never overwritten, and old files remain intact as a backup.
     */
    private void migratePreviousDataFolder() {
        File destination = getDataFolder();
        if (!destination.isDirectory() && !destination.mkdirs()) {
            getLogger().warning("Unable to create FrostPVP configuration folder: " + destination);
            return;
        }
        File parent = destination.getParentFile();
        if (parent == null) return;
        File previous = new File(parent, "PunchSnowball");
        if (!previous.isDirectory()) return;
        for (String fileName : List.of("config.yml", "trusted.yml")) {
            File original = new File(previous, fileName);
            File copy = new File(destination, fileName);
            if (!original.isFile() || copy.exists()) continue;
            try {
                java.nio.file.Files.copy(original.toPath(), copy.toPath());
                getLogger().info("Migrated " + fileName + " from plugins/PunchSnowball to plugins/FrostPVP.");
            } catch (IOException exception) {
                getLogger().severe("Could not migrate " + fileName + ": " + exception.getMessage());
            }
        }
    }

    @Override
    public void onEnable() {
        snowballKey = NamespacedKey.fromString("punchsnowball:punch_snowball");
        tntKey = NamespacedKey.fromString("punchsnowball:infinite_tnt");
        tntOwnerKey = NamespacedKey.fromString("punchsnowball:tnt_owner");
        freezeChargeKey = NamespacedKey.fromString("punchsnowball:freeze_windcharge");
        // Existing Storm Tridents from v1.5 become Grapple Tridents.
        tridentKey = NamespacedKey.fromString("punchsnowball:storm_trident");
        riptideTridentKey = NamespacedKey.fromString("punchsnowball:riptide_trident");
        fireworksKey = NamespacedKey.fromString("punchsnowball:homing_firework");
        lavaKey = NamespacedKey.fromString("punchsnowball:lava_tsunami");
        battleaxeKey = NamespacedKey.fromString("punchsnowball:battleaxe");
        shieldKey = NamespacedKey.fromString("punchsnowball:frost_shield");
        swordKey = NamespacedKey.fromString("punchsnowball:frozen_sword");
        frozenSwordSpeedKey = NamespacedKey.fromString("punchsnowball:frozen_sword_speed");
        frozenSwordPlayerSpeedKey = new NamespacedKey(this, "frozen_sword_player_speed");
        spearKey = NamespacedKey.fromString("punchsnowball:frozen_spear");
        fortunePickaxeKey = NamespacedKey.fromString("punchsnowball:frozen_fortune_pickaxe");
        silkPickaxeKey = NamespacedKey.fromString("punchsnowball:frozen_silk_pickaxe");
        pickaxeModeKey = NamespacedKey.fromString("punchsnowball:pickaxe_mode");
        pickaxeMiningModeKey = NamespacedKey.fromString("punchsnowball:pickaxe_mining_mode");
        bowKey = NamespacedKey.fromString("punchsnowball:frozen_bow");
        crossbowKey = NamespacedKey.fromString("punchsnowball:frozen_crossbow");
        icicleBoltKey = NamespacedKey.fromString("punchsnowball:icicle_bolt");
        shriekBoltKey = NamespacedKey.fromString("punchsnowball:shriek_bolt");
        helmetKey = NamespacedKey.fromString("punchsnowball:frozen_helmet");
        chestplateKey = NamespacedKey.fromString("punchsnowball:frozen_chestplate");
        leggingsKey = NamespacedKey.fromString("punchsnowball:frozen_leggings");
        bootsKey = NamespacedKey.fromString("punchsnowball:frozen_boots");
        preparedArrowKey = NamespacedKey.fromString("punchsnowball:bow_prepared_arrow");
        fallingArrowKey = NamespacedKey.fromString("punchsnowball:bow_falling_arrow");
        bowOwnerKey = NamespacedKey.fromString("punchsnowball:bow_owner");
        totemKey = NamespacedKey.fromString("punchsnowball:frozen_totem");
        fishingRodKey = NamespacedKey.fromString("punchsnowball:frozen_fishing_rod");
        hornKey = NamespacedKey.fromString("punchsnowball:frozen_horn");
        hornOwnerKey = NamespacedKey.fromString("punchsnowball:frozen_horn_owner");
        frostItemMarkers = new NamespacedKey[] {
            snowballKey, tntKey, freezeChargeKey, tridentKey, riptideTridentKey,
            fireworksKey, lavaKey, battleaxeKey, shieldKey, swordKey, spearKey,
            totemKey, fortunePickaxeKey, silkPickaxeKey, bowKey, crossbowKey,
            helmetKey, chestplateKey, leggingsKey, bootsKey, fishingRodKey, hornKey
        };
        migratePreviousDataFolder();
        saveDefaultConfig();
        // Merge new defaults into existing configs without losing unrelated edits.
        getConfig().options().copyDefaults(true);
        getConfig().set("tooltips.gradient-text", true);
        // Remove settings for the retired chestplate-reflection and leggings-Resistance passives.
        getConfig().set("armor.chestplate.reflect-chance-percent", null);
        getConfig().set("armor.leggings.resistance-enabled", null);
        saveConfig();
        refreshChatCooldown();
        // Migrate the older slide-style dash's 1.0 speed to the new impulse default.
        if (Math.abs(getConfig().getDouble("sword.dash-speed-blocks-per-tick", 1.0) - 1.0) < 0.0001) {
            getConfig().set("sword.dash-speed-blocks-per-tick", 1.8);
            saveConfig();
            loadSettings();
        }
        migrateV27Defaults();
        migrateV323Defaults();
        loadSettings();
        loadTrustedPlayers();
        Bukkit.getPluginManager().registerEvents(this, this);
        frostEvents = new FrostEvents(this);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            tickFrozenPlayers();
            tickGrapples();
            tickHomingRockets();
            tickFlyingAxes();
            tickSwordDashes();
            tickFrozenSwordPlayerSpeed();
            tickSwordTrails();
            tickFlyingSpears();
            tickTotemAuras();
            if (ticksPassed % 20 == 0) {
                tickCombat();
                tickFrozenArmor();
                for (Player online : Bukkit.getOnlinePlayers()) normalizeFrozenArmorProtection(online);
            }
            tickHelmetCrouch();
            if (ticksPassed % 2 == 0) tickIcicleBolts();
            tickPassiveFrostParticles();
            tickPickaxeMiningPassive();
            checkReadyMessages();
            if (ticksPassed % 5 == 0) tickFrostCooldownActionBar();
        }, 1L, 1L);
        Bukkit.getScheduler().runTask(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                clearInventoryModels(player);
                normalizeFrozenWeapons(player);
                refreshFrostTooltips(player);
            }
        });
    }

    @Override
    public void onDisable() {
        if (frostEvents != null) frostEvents.shutdown();
        for (UUID uuid : List.copyOf(frozenPlayers.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) releaseFrozen(player);
        }
        frozenPlayers.clear();
        for (var entry : frozenMobs.entrySet()) {
            Entity mob = Bukkit.getEntity(entry.getKey());
            if (mob instanceof LivingEntity living) releaseFrozenMob(living, entry.getValue());
        }
        frozenMobs.clear();
        for (HomingRocket rocket : activeRockets) {
            if (rocket.rocket.isValid()) rocket.rocket.remove();
        }
        activeRockets.clear();
        activeGrapples.clear();
        for (FlyingAxe axe : activeAxes) {
            if (axe.display.isValid()) axe.display.remove();
        }
        activeAxes.clear();
        activeDashes.clear();
        swordTrails.clear();
        for (FlyingSpear spear : flyingSpears) {
            if (spear.visual.isValid()) spear.visual.remove();
        }
        flyingSpears.clear();
        activeTotemAuras.clear();
        nextSpearUse.clear();
        nextTotemUse.clear();
        nextSwordUse.clear();
        preparedBows.clear();
        nextBowUse.clear();
        preparedShrieks.clear(); nextShriekUse.clear(); activeIcicleBolts.clear();
        helmetSneakSince.clear(); nextRevealUse.clear(); helmetNeedsStandUp.clear();
        lavaBurnUntil.clear();
        activeMiningBursts.clear();
        lastMiningFace.clear();
        lastMiningClick.clear();
        for (Player player : Bukkit.getOnlinePlayers()) clearPickaxeMiningState(player);
        pickaxeMiningStartedAt.clear();
        pickaxeLastMiningSwingAt.clear();
        previousPickaxeHaste.clear();
        pickaxeHasteApplied.clear();
        readyAt.clear();
        shieldActiveUntil.clear();
        shieldRaisedAt.clear();
        shieldStunnedUntil.clear();
        nextShieldUse.clear();
        pendingDuels.clear();
        activeDuelPartners.clear();
        duelCountdownPlayers.clear();
        duelCountdownAnchors.clear();
    }

    private long ticksPassed = 0;

    private void tickFrozenPlayers() {
        ticksPassed++;
        for (UUID uuid : List.copyOf(frozenPlayers.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            FrozenPlayer frozen = frozenPlayers.get(uuid);
            if (frozen == null) continue;
            if (player == null || !player.isOnline() || player.isDead() || ticksPassed >= frozen.releaseTick) {
                if (player != null) releaseFrozen(player);
                else frozenPlayers.remove(uuid);
                continue;
            }
            player.setVelocity(new Vector(0, 0, 0));
            if (windFreezeOverlay) {
                player.setFreezeTicks(player.getMaxFreezeTicks());
            }
            if (ticksPassed % 5 == 0) {
                player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        player.getLocation().add(0, 1.0, 0), 8,
                        0.35, 0.65, 0.35, 0.008);
            }
        }
        Iterator<Map.Entry<UUID, FrozenMob>> mobIterator = frozenMobs.entrySet().iterator();
        while (mobIterator.hasNext()) {
            var entry = mobIterator.next();
            Entity entity = Bukkit.getEntity(entry.getKey());
            FrozenMob frozen = entry.getValue();
            if (!(entity instanceof LivingEntity mob) || !mob.isValid() || mob.isDead()
                    || ticksPassed >= frozen.releaseTick) {
                if (entity instanceof LivingEntity mob) releaseFrozenMob(mob, frozen);
                mobIterator.remove();
                continue;
            }
            mob.setVelocity(new Vector());
            if (mob instanceof Mob aiMob) aiMob.setAI(false);
            if (windFreezeOverlay) mob.setFreezeTicks(mob.getMaxFreezeTicks());
            if (ticksPassed % 5 == 0) mob.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    mob.getLocation().add(0, mob.getHeight() * 0.55, 0), 9, .4, .5, .4, .01);
        }
    }

    private void releaseFrozenMob(LivingEntity mob, FrozenMob frozen) {
        if (mob instanceof Mob aiMob) aiMob.setAI(frozen.previouslyHadAI);
        if (windFreezeOverlay) mob.setFreezeTicks(Math.min(frozen.previousFreezeTicks, mob.getMaxFreezeTicks()));
    }

    private void freezeTarget(LivingEntity target, int durationTicks) {
        if (target instanceof Player player) { freezePlayer(player, durationTicks); return; }
        UUID id = target.getUniqueId();
        FrozenMob previous = frozenMobs.get(id);
        if (previous == null) {
            previous = new FrozenMob(!(target instanceof Mob mob) || mob.hasAI(),
                    target.getFreezeTicks(), ticksPassed + durationTicks);
            frozenMobs.put(id, previous);
        } else {
            previous.releaseTick = Math.max(previous.releaseTick, ticksPassed + durationTicks);
        }
        target.setVelocity(new Vector());
        if (target instanceof Mob mob) mob.setAI(false);
        if (windFreezeOverlay) target.setFreezeTicks(target.getMaxFreezeTicks());
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_POWDER_SNOW_BREAK, 1f, .8f);
    }

    private boolean isValidFrostTarget(UUID ownerId, LivingEntity target) {
        if (!target.isValid() || target.isDead() || target instanceof ArmorStand
                || target.getUniqueId().equals(ownerId)
                || trusts(ownerId, target.getUniqueId())) return false;
        if (target instanceof Player player) {
            return player.isOnline() && player.getGameMode() != GameMode.CREATIVE
                    && player.getGameMode() != GameMode.SPECTATOR;
        }
        return true;
    }

    private List<LivingEntity> nearbyFrostTargets(Location center, double horizontal, double vertical, UUID ownerId) {
        List<LivingEntity> result = new ArrayList<>();
        for (Entity entity : center.getWorld().getNearbyEntities(center, horizontal, vertical, horizontal)) {
            if (entity instanceof LivingEntity living && isValidFrostTarget(ownerId, living)) result.add(living);
        }
        return result;
    }

    private boolean hasShieldImmunity(Player player) {
        return ticksPassed < shieldActiveUntil.getOrDefault(player.getUniqueId(), 0L);
    }

    // Prevent all Frost stuns (wind charge and battleaxe) while the shield buff is active.
    private boolean isFrozen(Player player) {
        UUID id = player.getUniqueId();
        return frozenPlayers.containsKey(id) || duelCountdownPlayers.contains(id);
    }

    private void freezePlayer(Player player) {
        freezePlayer(player, freezeDurationTicks);
    }

    private void freezePlayer(Player player, int durationTicks) {
        if (hasShieldImmunity(player)) return;
        UUID uuid = player.getUniqueId();
        FrozenPlayer alreadyFrozen = frozenPlayers.get(uuid);
        if (alreadyFrozen == null) {
            alreadyFrozen = new FrozenPlayer(player.getLocation().clone(),
                    player.getFreezeTicks(), ticksPassed + durationTicks);
            frozenPlayers.put(uuid, alreadyFrozen);
        } else {
            alreadyFrozen.releaseTick = Math.max(alreadyFrozen.releaseTick, ticksPassed + durationTicks);
        }
        player.setVelocity(new Vector());
        if (windFreezeOverlay) player.setFreezeTicks(player.getMaxFreezeTicks());
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_POWDER_SNOW_BREAK, 1.0f, 0.8f);
    }

    private void releaseFrozen(Player player) {
        FrozenPlayer frozen = frozenPlayers.remove(player.getUniqueId());
        if (frozen != null && windFreezeOverlay) {
            player.setFreezeTicks(Math.min(frozen.previousFreezeTicks, player.getMaxFreezeTicks()));
        }
    }

    private void loadTrustedPlayers() {
        trustedPlayers.clear();
        trustFile = new File(getDataFolder(), "trusted.yml");
        trustFileData = YamlConfiguration.loadConfiguration(trustFile);
        ConfigurationSection section = trustFileData.getConfigurationSection("trusted");
        if (section == null) return;
        for (String ownerString : section.getKeys(false)) {
            UUID owner;
            try { owner = UUID.fromString(ownerString); }
            catch (IllegalArgumentException ignored) { continue; }
            ConfigurationSection members = section.getConfigurationSection(ownerString);
            if (members == null) continue;
            Map<UUID, String> entries = new HashMap<>();
            for (String targetString : members.getKeys(false)) {
                try {
                    UUID target = UUID.fromString(targetString);
                    entries.put(target, members.getString(targetString, targetString));
                } catch (IllegalArgumentException ignored) { }
            }
            if (!entries.isEmpty()) trustedPlayers.put(owner, entries);
        }
    }

    private void persistTrustedPlayers() {
        trustFileData.set("trusted", null);
        for (Map.Entry<UUID, Map<UUID, String>> owner : trustedPlayers.entrySet()) {
            for (Map.Entry<UUID, String> trusted : owner.getValue().entrySet()) {
                trustFileData.set("trusted." + owner.getKey() + "." + trusted.getKey(), trusted.getValue());
            }
        }
        try { trustFileData.save(trustFile); }
        catch (IOException ex) { getLogger().severe("Unable to write trusted.yml: " + ex.getMessage()); }
    }

    private boolean trusts(UUID owner, UUID possibleFriend) {
        return owner != null && trustedPlayers.getOrDefault(owner, Map.of()).containsKey(possibleFriend);
    }
    private boolean trusts(Player owner, Player possibleFriend) {
        return owner != null && possibleFriend != null && trusts(owner.getUniqueId(), possibleFriend.getUniqueId());
    }

    private ItemStack guiItem(Material type, String name, String... lines) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + name);
        List<String> lore = new ArrayList<>();
        for (String line : lines) lore.add(ChatColor.GRAY + line);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillMenu(Inventory inv) {
        ItemStack filler = guiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
    }

    private String groupLabel(String group) {
        return switch (group) {
            case "snowball" -> "Frozen Snowball";
            case "tnt" -> "Frozen TNT of Destruction";
            case "windcharge" -> "Frozen Windcharge of Coldness";
            case "grapple" -> "Frozen Trident of Loyalty";
            case "riptide" -> "Frozen Trident of the Depths";
            case "firework" -> "Frozen Firework of Cold Skies";
            case "lava" -> "Frozen Flint and Steel of Inferno";
            case "battleaxe" -> "Frozen Battleaxe";
            case "shield" -> "Frozen Shield of Defiance";
            case "sword" -> "Frozen Sword";
            case "spear" -> "Frozen Spear of Ice Spikes";
            case "totem" -> "Frozen Totem of Winter's Blessing";
            case "pickaxe_fortune", "pickaxe_silk", "pickaxe" -> "Frozen Pickaxe of Duality";
            case "bow" -> "Frozen Bow of Arrowfall";
            case "crossbow" -> "Frozen Crossbow of the Warden";
            case "helmet" -> "Frozen Helmet of Revelation";
            case "chestplate" -> "Frozen Chestplate of Vitality";
            case "leggings" -> "Frozen Leggings of Fire Resistance";
            case "boots" -> "Frozen Boots of Freedom";
            case "rod" -> "Frozen Fishing Rod";
            case "horn" -> "Frozen Horn of the Hordes";
            case "hud" -> "Cooldown Action Bar";
            case "visuals" -> "Passive Frost Particles";
            case "tooltips" -> "Legendary Item Tooltips";
            case "chat" -> "Chat & Anti-Spam";
            case "cooldowns" -> "Vanilla Item Cooldowns";
            case "combat" -> "Combat System";
            case "weapons" -> "Weapon Settings";
            case "armor" -> "Armor Settings";
            case "server" -> "Server Settings";
            case "presentation" -> "Visual & Tooltip Settings";
            default -> group;
        };
    }

    private Material groupMaterial(String group) {
        return switch (group) {
            case "snowball" -> Material.SNOWBALL;
            case "tnt" -> Material.TNT;
            case "windcharge" -> Material.WIND_CHARGE;
            case "grapple", "riptide" -> Material.TRIDENT;
            case "firework" -> Material.FIREWORK_ROCKET;
            case "lava" -> Material.FLINT_AND_STEEL;
            case "battleaxe" -> Material.NETHERITE_AXE;
            case "shield" -> Material.SHIELD;
            case "sword" -> Material.NETHERITE_SWORD;
            case "spear" -> Material.NETHERITE_SPEAR;
            case "totem" -> Material.TOTEM_OF_UNDYING;
            case "pickaxe", "pickaxe_fortune", "pickaxe_silk" -> Material.NETHERITE_PICKAXE;
            case "bow" -> Material.BOW;
            case "crossbow" -> Material.CROSSBOW;
            case "helmet" -> Material.NETHERITE_HELMET;
            case "chestplate" -> Material.NETHERITE_CHESTPLATE;
            case "leggings" -> Material.NETHERITE_LEGGINGS;
            case "boots" -> Material.NETHERITE_BOOTS;
            case "rod" -> Material.FISHING_ROD;
            case "horn" -> Material.GOAT_HORN;
            case "hud" -> Material.CLOCK;
            case "visuals" -> Material.SNOWBALL;
            case "tooltips" -> Material.NAME_TAG;
            case "chat" -> Material.PAPER;
            case "cooldowns" -> Material.CLOCK;
            case "combat" -> Material.IRON_SWORD;
            case "weapons" -> Material.NETHERITE_SWORD;
            case "armor" -> Material.NETHERITE_CHESTPLATE;
            case "server" -> Material.COMPARATOR;
            case "presentation" -> Material.GLOW_INK_SAC;
            default -> Material.PAPER;
        };
    }

    private void openConfigHome(Player player) {
        FrostMenu menu = new FrostMenu("");
        fillMenu(menu.inventory);
        for (Map.Entry<Integer, String> entry : CATEGORY_SLOTS.entrySet()) {
            String category = entry.getValue();
            menu.inventory.setItem(entry.getKey(), guiItem(groupMaterial(category), groupLabel(category),
                    "Contains " + CONFIG_CATEGORIES.get(category).size() + " configuration sections"));
        }
        menu.inventory.setItem(49, guiItem(Material.BOOK, "How to use",
                "Choose a category first", "Changes save automatically"));
        player.openInventory(menu.inventory);
    }

    private void openConfigCategory(Player player, String category) {
        List<String> groups = CONFIG_CATEGORIES.get(category);
        if (groups == null) return;
        FrostMenu menu = new FrostMenu("category:" + category);
        fillMenu(menu.inventory);
        for (int i = 0; i < groups.size() && i < CATEGORY_GROUP_SLOTS.length; i++) {
            String group = groups.get(i);
            menu.inventory.setItem(CATEGORY_GROUP_SLOTS[i], guiItem(groupMaterial(group), groupLabel(group),
                    "Click to configure this section"));
        }
        menu.inventory.setItem(49, guiItem(Material.ARROW, "Back", "Return to main categories"));
        player.openInventory(menu.inventory);
    }

    private ItemStack settingItem(GuiSetting setting) {
        if (setting.toggle()) {
            boolean enabled = getConfig().getBoolean(setting.path());
            return guiItem(setting.icon(), setting.title(),
                    "Current: " + (enabled ? "ON" : "OFF"), "Click to toggle");
        }
        double value = getConfig().getDouble(setting.path());
        String display = setting.integer() ? String.valueOf((int) Math.round(value))
                : String.format(Locale.US, "%.2f", value);
        return guiItem(setting.icon(), setting.title(),
                "Current: " + display, "Left click: +" + setting.step(),
                "Right click: -" + setting.step(), "Shift: x5 change");
    }

    private void openConfigGroup(Player player, String group) {
        List<GuiSetting> settings = GUI_SETTINGS.get(group);
        if (settings == null) return;
        FrostMenu menu = new FrostMenu(group);
        fillMenu(menu.inventory);
        for (int i = 0; i < settings.size(); i++) {
            menu.inventory.setItem(SETTING_SLOTS[i], settingItem(settings.get(i)));
        }
        menu.inventory.setItem(49, guiItem(Material.ARROW, "Back", "Return to configuration category"));
        menu.inventory.setItem(53, guiItem(Material.EMERALD, "Saved automatically",
                "Changes take effect immediately", "Reopen your item to update its lore"));
        player.openInventory(menu.inventory);
    }

    @EventHandler
    public void onConfigMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof FrostMenu menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !player.hasPermission("frost.config")) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
        if (menu.group.isEmpty()) {
            String category = CATEGORY_SLOTS.get(slot);
            if (category != null) Bukkit.getScheduler().runTask(this, () -> openConfigCategory(player, category));
            return;
        }
        if (menu.group.startsWith("category:")) {
            String category = menu.group.substring("category:".length());
            if (slot == 49) {
                Bukkit.getScheduler().runTask(this, () -> openConfigHome(player));
                return;
            }
            List<String> groups = CONFIG_CATEGORIES.getOrDefault(category, List.of());
            for (int i = 0; i < groups.size() && i < CATEGORY_GROUP_SLOTS.length; i++) {
                if (CATEGORY_GROUP_SLOTS[i] == slot) {
                    String group = groups.get(i);
                    Bukkit.getScheduler().runTask(this, () -> openConfigGroup(player, group));
                    return;
                }
            }
            return;
        }
        if (slot == 49) {
            String category = CONFIG_CATEGORIES.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(menu.group)).map(Map.Entry::getKey).findFirst().orElse("weapons");
            Bukkit.getScheduler().runTask(this, () -> openConfigCategory(player, category));
            return;
        }
        List<GuiSetting> settings = GUI_SETTINGS.get(menu.group);
        if (settings == null) return;
        int idx = -1;
        for (int i = 0; i < settings.size(); i++) {
            if (SETTING_SLOTS[i] == slot) { idx = i; break; }
        }
        if (idx < 0) return;
        GuiSetting setting = settings.get(idx);
        if (setting.toggle()) {
            getConfig().set(setting.path(), !getConfig().getBoolean(setting.path()));
        } else {
            double direction = event.getClick().isRightClick() ? -1 : 1;
            double increment = event.isShiftClick() ? 5 : 1;
            double updated = getConfig().getDouble(setting.path()) + direction * increment * setting.step();
            updated = Math.max(setting.minimum(), Math.min(setting.maximum(), updated));
            if (setting.integer()) getConfig().set(setting.path(), (int) Math.round(updated));
            else getConfig().set(setting.path(), Math.round(updated * 100.0) / 100.0);
        }
        saveConfig();
        loadSettings();
        if ("tooltips".equals(menu.group) || "chestplate".equals(menu.group) || "leggings".equals(menu.group)) {
            for (Player online : Bukkit.getOnlinePlayers()) refreshFrostTooltips(online);
        }
        menu.inventory.setItem(slot, settingItem(setting));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.65f, 1.2f);
    }

    @EventHandler
    public void onConfigMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FrostMenu) event.setCancelled(true);
    }

    private void loadSettings() {
        // Frost item tooltips are always enabled; only optional background styling is configurable.
        getConfig().set("tooltips.gradient-text", true);
        duelRequestTimeoutTicks = Math.max(20, getConfig().getInt("duel.request-timeout-seconds", 60) * 20);
        duelArenaSearchAttempts = Math.max(20, Math.min(500, getConfig().getInt("duel.arena-search-attempts", 120)));
        duelCountdownSeconds = Math.max(1, Math.min(10, getConfig().getInt("duel.countdown-seconds", 3)));
        duelSeparationBlocks = Math.max(8.0, Math.min(40.0, getConfig().getDouble("duel.separation-blocks", 20.0)));
        passiveFrostParticles = getConfig().getBoolean("visuals.passive-frost-particles", true);
        frostParticleInterval = Math.max(2, getConfig().getInt("visuals.particle-interval-ticks", 10));
        frostParticleCount = Math.max(1, Math.min(20, getConfig().getInt("visuals.particle-count", 5)));
        pickaxesEnabled = getConfig().getBoolean("pickaxes.enabled", true);
        pickaxesRadius = 1; // Exactly 3x3 or 1x1 is selected per pickaxe.
        shriekCooldownTicks = Math.max(20, getConfig().getInt("crossbow.shriek-cooldown-seconds", 40) * 20);
        shriekRange = Math.max(1, getConfig().getDouble("crossbow.shriek-range-blocks", 20.0));
        shriekDamage = Math.max(0, getConfig().getDouble("crossbow.shriek-damage-health-points", 8.0));
        icicleDamage = Math.max(0, getConfig().getDouble("crossbow.icicle-damage-health-points", 1.0));
        revealCrouchTicks = Math.max(1, getConfig().getInt("armor.helmet.crouch-seconds", 5) * 20);
        revealCooldownTicks = Math.max(1, getConfig().getInt("armor.helmet.reveal-cooldown-seconds", 120) * 20);
        revealGlowTicks = Math.max(1, getConfig().getInt("armor.helmet.glowing-seconds", 10) * 20);
        revealRadius = Math.max(1, getConfig().getDouble("armor.helmet.reveal-radius-blocks", 15));
        leggingsFireResistanceEnabled = getConfig().getBoolean("armor.leggings.fire-resistance-enabled", true);
        bootsSpeedEnabled = getConfig().getBoolean("armor.boots.speed-enabled", true);
        bootsFallImmunity = getConfig().getBoolean("armor.boots.fall-immunity", true);
        lavaFirePulses = Math.max(0, getConfig().getInt("lava.fire-pulses", 4));
        lavaFireDamage = Math.max(0, getConfig().getDouble("lava.fire-pulse-health-points", 1.0));
        bowCooldownTicks = Math.max(20, getConfig().getInt("bow.cooldown-seconds", 45) * 20);
        bowRainTicks = Math.max(20, getConfig().getInt("bow.rain-duration-seconds", 5) * 20);
        bowRainSide = Math.max(2.0, getConfig().getDouble("bow.radius-blocks", 10.0));
        bowArrowDamage = Math.max(0.0, getConfig().getDouble("bow.damage-health-points", 2.0));
        bowBlindnessTicks = Math.max(20, getConfig().getInt("bow.blindness-seconds", 10) * 20);
        bowArrowsPerWave = Math.max(1, Math.min(10, getConfig().getInt("bow.arrows-per-wave", 3)));
        bowWaveIntervalTicks = Math.max(2, getConfig().getInt("bow.wave-interval-ticks", 5));
        snowballCooldownTicks = Math.max(1, getConfig().getInt("snowball.cooldown-seconds", 10) * 20);
        slownessTicks = Math.max(1, getConfig().getInt("snowball.slowness-seconds", 2) * 20);
        snowballHorizontal = Math.max(0.0, getConfig().getDouble("snowball.horizontal-velocity", 2.45));
        snowballVertical = Math.max(0.0, getConfig().getDouble("snowball.vertical-velocity", 0.40));
        tntCooldownTicks = Math.max(1, getConfig().getInt("tnt.cooldown-seconds", 10) * 20);
        tntFuseTicks = Math.max(1, (int) Math.round(getConfig().getDouble("tnt.fuse-seconds", 1.5) * 20.0));
        tntRadius = Math.max(0.5, getConfig().getDouble("tnt.radius-blocks", 2.0));
        tntPower = (float) Math.max(0.0, Math.min(4.0, getConfig().getDouble("tnt.explosion-power", 2.5)));
        tntBreakBlocks = getConfig().getBoolean("tnt.break-blocks", true);
        tntDamage = Math.max(0.0, getConfig().getDouble("tnt.damage-health-points", 4.0));
        windCooldownTicks = Math.max(1, getConfig().getInt("cooldowns.windcharge-seconds", getConfig().getInt("windcharge.cooldown-seconds", 20)) * 20);
        freezeDurationTicks = Math.max(1, getConfig().getInt("windcharge.freeze-seconds", 3) * 20);
        windFreezeOverlay = getConfig().getBoolean("windcharge.powder-snow-overlay", true);
        tridentCooldownTicks = Math.max(1, getConfig().getInt("trident.cooldown-seconds", 20) * 20);
        lightningDamage = Math.max(0.0, getConfig().getDouble("trident.damage-per-strike-health-points", 3.0));
        lightningSpacingTicks = Math.max(1, getConfig().getInt("trident.ticks-between-strikes", 8));
        tridentFireTicks = Math.max(0, getConfig().getInt("trident.fire-seconds", 8) * 20);
        grappleCooldownTicks = Math.max(1, getConfig().getInt("grapple.cooldown-seconds", 20) * 20);
        grapplePullHorizontal = Math.max(0.1, Math.min(3.0, getConfig().getDouble("grapple.horizontal-velocity", 1.5)));
        grapplePullVertical = Math.max(0.0, Math.min(1.2, getConfig().getDouble("grapple.vertical-velocity", 0.35)));
        riptideCooldownTicks = Math.max(1, getConfig().getInt("riptide.cooldown-seconds", 25) * 20);
        lightningDamage = Math.max(0.0, getConfig().getDouble("riptide.damage-per-lightning-hit-health-points", 3.0));
        lightningSpacingTicks = Math.max(1, getConfig().getInt("riptide.ticks-between-strikes", 8));
        fireworkCooldownTicks = Math.max(1, getConfig().getInt("firework.cooldown-seconds", 45) * 20);
        fireworkCount = Math.max(1, Math.min(20, getConfig().getInt("firework.missiles", 5)));
        fireworkDamage = Math.max(0.0, getConfig().getDouble("firework.damage-health-points", 1.0));
        fireworkTargetRange = Math.max(1.0, getConfig().getDouble("firework.target-range-blocks", 64.0));
        fireworkSkyDelay = Math.max(5, getConfig().getInt("firework.sky-delay-ticks", 25));
        fireworkSpeed = Math.max(0.2, Math.min(2.0, getConfig().getDouble("firework.homing-speed", 1.2)));
        fireworkGlowingTicks = Math.max(20, getConfig().getInt("firework.glowing-seconds", 15) * 20);
        lavaCooldownTicks = Math.max(1, getConfig().getInt("lava.cooldown-seconds", 30) * 20);
        lavaRadius = Math.max(0.5, getConfig().getDouble("lava.radius-blocks", 3.0));
        lavaDamage = Math.max(0.0, getConfig().getDouble("lava.damage-health-points", 6.0));
        lavaWaveTicks = Math.max(4, getConfig().getInt("lava.wave-duration-ticks", 18));
        battleaxeCooldownTicks = Math.max(1, getConfig().getInt("battleaxe.cooldown-seconds", 40) * 20);
        battleaxeStunTicks = Math.max(1, getConfig().getInt("battleaxe.stun-seconds", 5) * 20);
        battleaxeSpeed = Math.max(0.2, Math.min(3.0, getConfig().getDouble("battleaxe.throw-speed", 1.3)));
        battleaxeRange = Math.max(4.0, getConfig().getDouble("battleaxe.max-range-blocks", 30.0));
        shieldCooldownTicks = Math.max(1, getConfig().getInt("shield.cooldown-seconds", 60) * 20);
        shieldActiveTicks = Math.max(1, getConfig().getInt("shield.parry-immunity-seconds", 5) * 20);
        shieldParryWindowTicks = Math.max(1, getConfig().getInt("shield.parry-window-ticks", 6));
        shieldStunTicks = Math.max(1, getConfig().getInt("shield.failed-parry-stun-seconds", 3) * 20);
        shieldReflectHorizontal = Math.max(0.1, getConfig().getDouble("shield.parry-knockback-horizontal", 3.2));
        shieldReflectVertical = Math.max(0.0, getConfig().getDouble("shield.parry-knockback-vertical", 0.85));
        swordCooldownTicks = Math.max(1, getConfig().getInt("sword.cooldown-seconds", 25) * 20);
        swordDashBlocks = Math.max(1.0, Math.min(30.0, getConfig().getDouble("sword.dash-blocks", 9.0)));
        swordDashSpeed = Math.max(0.6, Math.min(3.0, getConfig().getDouble("sword.dash-speed-blocks-per-tick", 1.8)));
        // Frozen Sword balance caps also correct older v3.4.1 configs automatically.
        // Dash damage is true damage and is capped at exactly 3 hearts (6 health points).
        // Normal melee retains vanilla charge scaling, but neither a full hit nor a crit
        // can exceed 4 hearts (8 health points) before armor and Protection are applied.
        boolean swordConfigCorrected = false;
        double configuredSwordDashDamage = getConfig().getDouble("sword.damage-health-points", 6.0);
        if (Double.compare(configuredSwordDashDamage, 6.0) != 0) {
            configuredSwordDashDamage = 6.0;
            getConfig().set("sword.damage-health-points", 6.0);
            swordConfigCorrected = true;
        }
        swordDamage = Math.max(0.0, configuredSwordDashDamage);
        double configuredSwordMeleeCap = getConfig().getDouble("sword.melee-damage-health-points", 8.0);
        if (configuredSwordMeleeCap > 8.0) {
            configuredSwordMeleeCap = 8.0;
            getConfig().set("sword.melee-damage-health-points", 8.0);
            swordConfigCorrected = true;
        }
        swordMeleeDamage = Math.max(1.0, Math.min(8.0, configuredSwordMeleeCap));
        if (swordConfigCorrected) saveConfig();
        swordTrailTicks = Math.max(1, getConfig().getInt("sword.trail-duration-seconds", 5) * 20);
        swordSlowTicks = Math.max(1, getConfig().getInt("sword.slowness-seconds", 3) * 20);
        swordSlowLevel = Math.max(1, Math.min(5, getConfig().getInt("sword.slowness-level", 2)));
        spearCooldownTicks = Math.max(20, getConfig().getInt("spear.cooldown-seconds", 30) * 20);
        spearSpikeCount = Math.max(1, Math.min(24, getConfig().getInt("spear.spike-count", 8)));
        spearSpikeInterval = Math.max(1, getConfig().getInt("spear.ticks-between-spikes", 4));
        spearSpikeSpacing = Math.max(0.5, getConfig().getDouble("spear.spike-spacing-blocks", 1.5));
        spearDistanceGrowth = Math.max(0, Math.min(0.8, getConfig().getDouble("spear.spike-distance-growth", 0.22)));
        spearSizeGrowth = Math.max(0, Math.min(0.65, getConfig().getDouble("spear.spike-size-growth", 0.23)));
        spearDamage = Math.max(0, getConfig().getDouble("spear.damage-health-points", 3.0));
        spearLaunchVelocity = Math.max(0.1, getConfig().getDouble("spear.launch-velocity", 0.8));
        totemCooldownTicks = Math.max(20, getConfig().getInt("totem.cooldown-seconds", 45) * 20);
        totemActiveTicks = Math.max(20, getConfig().getInt("totem.active-seconds", 12) * 20);
        totemRadius = Math.max(1, getConfig().getDouble("totem.radius-blocks", 10.0));
        totemEffectRefresh = Math.max(5, getConfig().getInt("totem.effect-refresh-ticks", 10));
        anchorBreakDistance = Math.max(10.0, getConfig().getDouble("rod.max-anchor-distance-blocks", 40.0));
        anchorPullStrength = Math.max(0.4, getConfig().getDouble("rod.pull-strength", 2.75));
        hornCooldownTicks = Math.max(20, getConfig().getInt("horn.cooldown-seconds", 60) * 20);
        hornSkeletonCount = Math.max(1, Math.min(20, getConfig().getInt("horn.skeleton-count", 5)));
        hornDespawnTicks = Math.max(20, getConfig().getInt("horn.despawn-seconds", 25) * 20);
        hornSpawnRadius = Math.max(2.0, getConfig().getDouble("horn.spawn-radius-blocks", 6.0));
        hornArcherChance = Math.max(0.0, Math.min(1.0, getConfig().getDouble("horn.archer-chance", 0.25)));
    }

    // Upgrade only untouched old defaults, preserving customized server values.
    private void migrateV323Defaults() {
        if (getConfig().getBoolean("migration.v3-2-3-done", false)) return;
        if (Math.abs(getConfig().getDouble("rod.pull-strength", 1.65) - 1.65) < .0001)
            getConfig().set("rod.pull-strength", 2.75);
        if (Math.abs(getConfig().getDouble("events.titan-health", 500.0) - 500.0) < .0001)
            getConfig().set("events.titan-health", 6000.0);
        getConfig().set("migration.v3-2-3-done", true);
        saveConfig();
    }

    // Unsafe enchantment is decorative on non-tools; glint override makes the
    // purple shimmer reliably visible even if the item type doesn't use durability.
    // Migrate unchanged old defaults, but preserve custom values set by an OP.
    private void migrateV27Defaults() {
        if (getConfig().getBoolean("migration.v2-7-done", false)) return;
        if (getConfig().getInt("snowball.cooldown-seconds", 7) == 7)
            getConfig().set("snowball.cooldown-seconds", 10);
        if (Math.abs(getConfig().getDouble("tnt.fuse-seconds", 3.0) - 3.0) < .001)
            getConfig().set("tnt.fuse-seconds", 1.5);
        if (Math.abs(getConfig().getDouble("tnt.radius-blocks", 4.0) - 4.0) < .001)
            getConfig().set("tnt.radius-blocks", 2.0);
        if (Math.abs(getConfig().getDouble("tnt.explosion-power", 2.5) - 2.5) < .001)
            getConfig().set("tnt.explosion-power", 2.0);
        getConfig().set("migration.v2-7-done", true);
        saveConfig();
    }

    private ItemStack decorativeGlint(ItemStack item) {
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSnowball() {
        ItemStack item = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frozen Snowball");
        meta.setLore(List.of(
                ChatColor.GRAY + "Infinite throws",
                ChatColor.GRAY + "" + (snowballCooldownTicks / 20) + "-second cooldown",
                ChatColor.LIGHT_PURPLE + "Strong knockback + Slowness II"));
        meta.getPersistentDataContainer().set(snowballKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return decorativeGlint(item);
    }

    private ItemStack makeTnt() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Frozen TNT of Destruction");
        meta.setLore(List.of(
                ChatColor.GRAY + "Right-click a block to ignite",
                ChatColor.GRAY + "Reusable - never consumed",
                ChatColor.YELLOW + "" + (tntFuseTicks / 20.0) + "-second fuse",
                ChatColor.RED + "" + (tntCooldownTicks / 20) + "-second cooldown",
                ChatColor.LIGHT_PURPLE + "Small blast, configurable damage"));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(tntKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return decorativeGlint(item);
    }

    private ItemStack makeWindCharge() {
        ItemStack item = new ItemStack(Material.WIND_CHARGE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frozen Windcharge of Coldness");
        meta.setLore(List.of(
                ChatColor.GRAY + "Infinite throws",
                ChatColor.BLUE + "Freezes hit players for " + (freezeDurationTicks / 20) + " seconds",
                ChatColor.GRAY + "Prevents moving and using items",
                ChatColor.YELLOW + "" + (windCooldownTicks / 20) + "-second cooldown"));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(freezeChargeKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return decorativeGlint(item);
    }

    private ItemStack makeTrident() {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frozen Trident of Loyalty");
        meta.addEnchant(Enchantment.IMPALING, 6, true);
        // Sharpness isn't a normal trident enchantment; 'true' permits it.
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
        meta.addEnchant(Enchantment.LOYALTY, 3, true);
        meta.setUnbreakable(true);
        meta.setLore(List.of(
                ChatColor.GRAY + "Impaling VI | Sharpness V | Fire Aspect II | Loyalty III",
                ChatColor.GRAY + "Unbreakable",
                ChatColor.AQUA + "Continuously grapples players toward you",
                ChatColor.YELLOW + "Grapple cooldown: " + (grappleCooldownTicks / 20) + "s"));
        meta.getPersistentDataContainer().set(tridentKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeRiptideTrident() {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_AQUA + "Frozen Trident of the Depths");
        meta.addEnchant(Enchantment.RIPTIDE, 3, true);
        meta.addEnchant(Enchantment.IMPALING, 5, true);
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.setUnbreakable(true);
        meta.setLore(List.of(
                ChatColor.GRAY + "Riptide III | Impaling V | Sharpness V | Unbreakable",
                ChatColor.AQUA + "Every 10 successful melee hits: 3 lightning hits",
                ChatColor.GRAY + "Lightning triggers on hits 4, 7, and 10",
                ChatColor.GRAY + "Each lightning hit: " + (lightningDamage / 2.0) + " hearts through armor",
                ChatColor.YELLOW + "Riptide activation cooldown: " + (riptideCooldownTicks / 20) + "s",
                ChatColor.GRAY + "Right-click to launch, even outside water or rain"));
        meta.getPersistentDataContainer().set(riptideTridentKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeFirework() {
        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET);
        FireworkMeta meta = (FireworkMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Frozen Firework of Cold Skies");
        meta.setEnchantmentGlintOverride(true);
        meta.setPower(1);
        meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.AQUA, Color.PURPLE).withFade(Color.WHITE).trail(true).build());
        meta.setLore(List.of(
                ChatColor.GRAY + "Right-click to launch skyward",
                ChatColor.GRAY + "Reusable: never consumed",
                ChatColor.LIGHT_PURPLE + "Shoots " + fireworkCount + " homing fireworks",
                ChatColor.GRAY + "Each hit grants " + (fireworkGlowingTicks / 20) + "s of Glowing",
                ChatColor.YELLOW + "Cooldown: " + (fireworkCooldownTicks / 20) + "s"));
        meta.getPersistentDataContainer().set(fireworksKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return decorativeGlint(item);
    }

    private ItemStack makeLavaTsunami() {
        ItemStack item = new ItemStack(Material.FLINT_AND_STEEL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Frozen Flint and Steel of Inferno");
        meta.setUnbreakable(true);
        meta.setEnchantmentGlintOverride(true);
        meta.setLore(List.of(
                ChatColor.GRAY + "Right-click: unleash a rolling lava wave",
                ChatColor.YELLOW + "6x6 area, 3 hearts of damage by default",
                ChatColor.GRAY + "Particle wave; no actual lava blocks",
                ChatColor.RED + "Cooldown: " + (lavaCooldownTicks / 20) + " seconds"));
        meta.getPersistentDataContainer().set(lavaKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return decorativeGlint(item);
    }

    private ItemStack makeBattleaxe() {
        ItemStack item = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frozen Battleaxe");
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
        meta.setEnchantmentGlintOverride(true);
        meta.setLore(List.of(
                ChatColor.GRAY + "Sharpness V | Efficiency V | Silk Touch I | Unbreakable",
                ChatColor.GRAY + "Right-click to throw a spinning axe",
                ChatColor.BLUE + "Direct hits stun for " + (battleaxeStunTicks / 20) + " seconds",
                ChatColor.YELLOW + "Cooldown: " + (battleaxeCooldownTicks / 20) + " seconds",
                ChatColor.GRAY + "Returns instantly; infinite throws"));
        meta.getPersistentDataContainer().set(battleaxeKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSword() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frozen Sword");
        meta.setUnbreakable(true);
        meta.removeEnchant(Enchantment.SHARPNESS);
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.SWEEPING_EDGE, 4, true);
        meta.addEnchant(Enchantment.LOOTING, 4, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 3, true);
        meta.setEnchantmentGlintOverride(true);
        meta.setLore(List.of(
            ChatColor.GRAY + "Sharpness V | Efficiency V | Sweeping Edge IV | Looting IV",
            ChatColor.GRAY + "Fire Aspect III | Unbreakable",
            ChatColor.AQUA + "+10% movement speed while held",
            ChatColor.GRAY + "Melee cap: " + (swordMeleeDamage / 2.0) + " hearts before armor",
            ChatColor.BLUE + "Right-click: dash " + (int) swordDashBlocks + " blocks",
            ChatColor.GRAY + "Creates a trail of Slowness " + swordSlowLevel,
            ChatColor.RED + "Dash hit: " + (swordDamage / 2.0) + " hearts through armor",
            ChatColor.YELLOW + "Cooldown: " + (swordCooldownTicks / 20) + " seconds"));
        meta.getPersistentDataContainer().set(swordKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeShield() {
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frozen Shield of Defiance");
        meta.setUnbreakable(true);
        meta.setEnchantmentGlintOverride(true);
        meta.setLore(List.of(
                ChatColor.GRAY + "Raise just before impact to parry",
                ChatColor.LIGHT_PURPLE + "Perfect parry launches the attacker away",
                ChatColor.BLUE + "Successful parry: " + (shieldActiveTicks / 20) + "s shield-break immunity",
                ChatColor.RED + "Failed parry: shield stunned for " + (shieldStunTicks / 20) + "s"));
        meta.getPersistentDataContainer().set(shieldKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return decorativeGlint(item);
    }

    private ItemStack makeSpear() {
        ItemStack item = new ItemStack(Material.NETHERITE_SPEAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frozen Spear of Ice Spikes");
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.LOOTING, 4, true);
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.LUNGE, 3, true);
        meta.addEnchant(Enchantment.KNOCKBACK, 3, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 3, true);
        meta.setEnchantmentGlintOverride(true);
        meta.setLore(List.of(
                ChatColor.GRAY + "Looting IV | Sharpness V | Lunge III",
                ChatColor.GRAY + "Knockback III | Fire Aspect III",
                ChatColor.BLUE + "Left-click: throw into the ground",
                ChatColor.AQUA + "An advancing line of ice spikes erupts",
                ChatColor.GRAY + "Damages and launches enemies upward",
                ChatColor.YELLOW + "Cooldown: " + (spearCooldownTicks / 20) + " seconds"));
        meta.getPersistentDataContainer().set(spearKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeTotem() {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frozen Totem of Winter's Blessing");
        meta.setLore(List.of(
                ChatColor.RED + "Does NOT prevent death or resurrect you",
                ChatColor.BLUE + "Right-click: activate a moving icy aura",
                ChatColor.GREEN + "You + trusted friends: Regeneration I",
                ChatColor.LIGHT_PURPLE + "Other players: Weakness I",
                ChatColor.AQUA + "Radius: " + (int) totemRadius + " blocks for " + (totemActiveTicks / 20) + "s",
                ChatColor.YELLOW + "Cooldown: " + (totemCooldownTicks / 20) + " seconds"));
        meta.getPersistentDataContainer().set(totemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return decorativeGlint(item);
    }

    private void applyPickaxeMode(ItemStack item, boolean fortune) {
        ItemMeta meta = item.getItemMeta();
        String miningMode = meta.getPersistentDataContainer().get(pickaxeMiningModeKey, PersistentDataType.STRING);
        if (miningMode == null) miningMode = "3x3";
        meta.removeEnchant(Enchantment.FORTUNE);
        meta.removeEnchant(Enchantment.SILK_TOUCH);
        meta.addEnchant(Enchantment.EFFICIENCY, 8, true);
        meta.addEnchant(fortune ? Enchantment.FORTUNE : Enchantment.SILK_TOUCH, fortune ? 5 : 1, true);
        meta.setUnbreakable(true);
        meta.setEnchantmentGlintOverride(true);
        meta.setDisplayName(ChatColor.AQUA + "Frozen Pickaxe of Duality "
                + ChatColor.GRAY + "[" + (fortune ? "Fortune V" : "Silk Touch") + " | " + miningMode + "]");
        boolean miningHasteEnabled = getConfig().getBoolean("pickaxes.enabled", true)
                && getConfig().getBoolean("pickaxes.mining-haste.enabled", true);
        int miningHasteDelay = Math.max(1, getConfig().getInt("pickaxes.mining-haste.delay-seconds", 5));
        int miningHasteLevel = Math.max(1, Math.min(5,
                getConfig().getInt("pickaxes.mining-haste.amplifier", 1) + 1));
        meta.setLore(List.of(
                ChatColor.GRAY + "Efficiency VIII | Unbreakable",
                ChatColor.BLUE + "Drop mode: " + (fortune ? "Fortune V" : "Silk Touch I"),
                ChatColor.YELLOW + "Right-click: switch Fortune / Silk Touch",
                ChatColor.AQUA + "Sneak + right-click: switch 3x3 / 1x1",
                ChatColor.GRAY + (miningMode.equals("3x3") ? "Area mining: 3x3" : "Area mining: disabled (1x1)"),
                miningHasteEnabled
                        ? ChatColor.GOLD + "PASSIVE - MINING MASTERY: mine continuously for " + miningHasteDelay + "s to gain Haste " + toRoman(miningHasteLevel)
                        : ChatColor.DARK_GRAY + "PASSIVE - MINING MASTERY: disabled in config",
                ChatColor.GRAY + "Haste ends when you stop mining or switch tools"));
        meta.getPersistentDataContainer().set(pickaxeModeKey, PersistentDataType.STRING,
                fortune ? "fortune" : "silk");
        meta.getPersistentDataContainer().set(pickaxeMiningModeKey, PersistentDataType.STRING, miningMode);
        meta.getPersistentDataContainer().set(fortunePickaxeKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }

    private ItemStack makeFrozenPickaxe(boolean fortune) {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        applyPickaxeMode(item, fortune);
        return item;
    }

    private ItemStack makeBow() {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frozen Bow of Arrowfall");
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.INFINITY, 1, true);
        meta.addEnchant(Enchantment.POWER, 7, true);
        meta.addEnchant(Enchantment.LOOTING, 3, true);
        meta.addEnchant(Enchantment.FLAME, 5, true);
        meta.setEnchantmentGlintOverride(true);
        meta.setLore(List.of(
                ChatColor.GRAY + "Power VII | Looting III | Flame V | Infinity I",
                ChatColor.GRAY + "Shift + right-click: prepare the arrow rain",
                ChatColor.BLUE + "Charge the bow and shoot an arrow at the destination",
                ChatColor.AQUA + "10x10 icy arrow rain for 5 seconds",
                ChatColor.RED + "Each arrow: 1 heart | Blindness II for 10s",
                ChatColor.YELLOW + "Cooldown: " + (bowCooldownTicks / 20) + " seconds"));
        meta.getPersistentDataContainer().set(bowKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeCrossbow() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frozen Crossbow of the Warden");
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.POWER, 6, true);
        meta.addEnchant(Enchantment.LOOTING, 3, true);
        meta.addEnchant(Enchantment.MULTISHOT, 1, true);
        meta.addEnchant(Enchantment.INFINITY, 1, true);
        meta.addEnchant(Enchantment.PIERCING, 4, true);
        meta.addEnchant(Enchantment.FLAME, 1, true);
        meta.setLore(List.of(ChatColor.GRAY + "Power VI | Looting III | Multishot",
                ChatColor.GRAY + "Infinity I | Piercing IV | Flame I | Unbreakable",
                ChatColor.AQUA + "Each ice shard: half a heart",
                ChatColor.BLUE + "Sneak + right-click: prepare Warden Shriek",
                ChatColor.RED + "Shriek: 4 hearts up to 20 blocks",
                ChatColor.YELLOW + "Ability cooldown: " + shriekCooldownTicks/20 + "s"));
        meta.getPersistentDataContainer().set(crossbowKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeFishingRod() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frozen Fishing Rod");
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.setEnchantmentGlintOverride(true);
        meta.setLore(List.of(
                ChatColor.GRAY + "Right-click a SOLID block to set your anchor",
                ChatColor.GRAY + "Right-click again: powerful one-use grapple",
                ChatColor.AQUA + "Anchor survives switching items in your inventory",
                ChatColor.YELLOW + "Anchor breaks beyond about " + (int) anchorBreakDistance + " blocks"));
        meta.getPersistentDataContainer().set(fishingRodKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeHorn() {
        ItemStack item = new ItemStack(Material.GOAT_HORN);
        MusicInstrumentMeta meta = (MusicInstrumentMeta) item.getItemMeta();
        meta.setInstrument(MusicInstrument.CALL_GOAT_HORN);
        meta.setDisplayName(ChatColor.AQUA + "Frozen Horn of the Hordes");
        meta.setEnchantmentGlintOverride(true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.setLore(List.of(
                ChatColor.GRAY + "Goat Horn (Call)",
                ChatColor.BLUE + "Right-click: summon an icy skeleton horde",
                ChatColor.AQUA + "Minions only attack untrusted players",
                ChatColor.YELLOW + "Cooldown: " + (hornCooldownTicks / 20) + " seconds"));
        meta.getPersistentDataContainer().set(hornKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeFrozenArmor(Material type, NamespacedKey key, String name, String lore, Enchantment[] enchants, int[] levels) {
        ItemStack item = new ItemStack(type);
        ArmorMeta meta = (ArmorMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + name);
        meta.setUnbreakable(true);
        meta.setEnchantmentGlintOverride(true);
        meta.setTrim(new ArmorTrim(TrimMaterial.DIAMOND, TrimPattern.RIB));
        for (int i=0; i<enchants.length; i++) meta.addEnchant(enchants[i], levels[i], true);
        meta.setLore(List.of(ChatColor.GRAY + "Protection III | Diamond Rib Trim | Unbreakable", ChatColor.BLUE + lore));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void ensureChestplateAttribute(ItemMeta meta, Attribute attribute, NamespacedKey key, double amount) {
        Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);
        if (modifiers != null) {
            for (AttributeModifier modifier : new ArrayList<>(modifiers)) {
                if (!key.equals(modifier.getKey())) continue;
                boolean correct = Math.abs(modifier.getAmount() - amount) < 0.0001
                    && modifier.getOperation() == AttributeModifier.Operation.ADD_NUMBER
                    && EquipmentSlotGroup.CHEST.equals(modifier.getSlotGroup());
                if (correct) return;
                meta.removeAttributeModifier(attribute, modifier);
            }
        }
        meta.addAttributeModifier(attribute, new AttributeModifier(
            key, amount, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
    }

    private ItemStack makeHelmet() {
        return makeFrozenArmor(Material.NETHERITE_HELMET, helmetKey, "Frozen Helmet of Revelation",
            "Protection III | Strength I | Sneak 5s: reveal players within 15 blocks (120s CD)",
            new Enchantment[]{Enchantment.PROTECTION, Enchantment.AQUA_AFFINITY, Enchantment.RESPIRATION},
            new int[]{3, 2, 3});
    }
    private ItemStack makeChestplate() {
        double bonusHearts = Math.max(0.0, Math.min(20.0,
            getConfig().getDouble("armor.chestplate.bonus-hearts", 5.0)));
        ItemStack item = makeFrozenArmor(Material.NETHERITE_CHESTPLATE, chestplateKey,
            "Frozen Chestplate of Vitality", "Protection III | 8 Armor | 3 Armor Toughness | Gain +" + bonusHearts + " hearts while worn",
            new Enchantment[]{Enchantment.PROTECTION}, new int[]{3});
        ItemMeta meta = item.getItemMeta();
        // Adding a custom MAX_HEALTH modifier makes the item's attribute component
        // explicit. Re-add Netherite chestplate's native defense values so its armor
        // points and toughness are not accidentally lost by that custom component.
        ensureChestplateAttribute(meta, Attribute.ARMOR,
            new NamespacedKey(this, "frozen_chestplate_armor"), 8.0);
        ensureChestplateAttribute(meta, Attribute.ARMOR_TOUGHNESS,
            new NamespacedKey(this, "frozen_chestplate_toughness"), 3.0);
        ensureChestplateAttribute(meta, Attribute.KNOCKBACK_RESISTANCE,
            new NamespacedKey(this, "frozen_chestplate_knockback_resistance"), 0.1);
        if (bonusHearts > 0.0) {
            NamespacedKey healthKey = new NamespacedKey(this, "frozen_chestplate_bonus_health");
            ensureChestplateAttribute(meta, Attribute.MAX_HEALTH, healthKey, bonusHearts * 2.0);
        }
        item.setItemMeta(meta);
        return item;
    }
    private ItemStack makeLeggings() {
        return makeFrozenArmor(Material.NETHERITE_LEGGINGS, leggingsKey, "Frozen Leggings of Fire Resistance",
            "Protection III | Fire Resistance I while equipped",
            new Enchantment[]{Enchantment.PROTECTION, Enchantment.SWIFT_SNEAK}, new int[]{3, 5});
    }
    private ItemStack makeBoots() {
        return makeFrozenArmor(Material.NETHERITE_BOOTS, bootsKey, "Frozen Boots of Freedom",
            "Protection III | Speed II | No fall damage while equipped",
            new Enchantment[]{Enchantment.PROTECTION, Enchantment.DEPTH_STRIDER}, new int[]{3, 5});
    }

    // A legacy Frost item may still carry the v2.8/v2.9 custom item model.
    // Clear only items with our persistent-data markers, leaving vanilla items alone.
    private boolean isAnyFrostItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        for (NamespacedKey marker : frostItemMarkers) {
            if (meta.getPersistentDataContainer().has(marker, PersistentDataType.BYTE)) return true;
        }
        return false;
    }

    private boolean clearLegacyFrostModel(ItemStack item) {
        if (!isAnyFrostItem(item)) return false;
        ItemMeta meta = item.getItemMeta();
        boolean changed = false;
        if (meta.hasItemModel()) {
            meta.setItemModel(null);
            changed = true;
        }
        // Revert old resource-pack armor to its natural Netherite equipment asset.
        if (item.getItemMeta() instanceof ArmorMeta && meta.hasEquippable()) {
            var oldEquip = meta.getEquippable();
            NamespacedKey oldModel = oldEquip.getModel();
            if (oldModel != null && oldModel.getNamespace().equals("frost")) {
                ItemMeta vanillaMeta = new ItemStack(item.getType()).getItemMeta();
                meta.setEquippable(vanillaMeta.getEquippable());
                changed = true;
            }
        }
        if (changed) item.setItemMeta(meta);
        return changed;
    }

    private int clearInventoryModels(Player player) {
        int updated = 0;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (clearLegacyFrostModel(item)) {
                player.getInventory().setItem(slot, item);
                updated++;
            }
        }
        if (updated > 0) player.updateInventory();
        return updated;
    }

    private boolean refreshFrozenArmorPassiveMetadata(ItemStack item) {
        if (isGroupItem(item, "chestplate")) {
            ItemMeta meta = item.getItemMeta();
            double bonusHearts = Math.max(0.0, Math.min(20.0,
                getConfig().getDouble("armor.chestplate.bonus-hearts", 5.0)));
            double bonusHealth = bonusHearts * 2.0;
            ensureChestplateAttribute(meta, Attribute.ARMOR,
                new NamespacedKey(this, "frozen_chestplate_armor"), 8.0);
            ensureChestplateAttribute(meta, Attribute.ARMOR_TOUGHNESS,
                new NamespacedKey(this, "frozen_chestplate_toughness"), 3.0);
            ensureChestplateAttribute(meta, Attribute.KNOCKBACK_RESISTANCE,
                new NamespacedKey(this, "frozen_chestplate_knockback_resistance"), 0.1);
            NamespacedKey healthKey = new NamespacedKey(this, "frozen_chestplate_bonus_health");
            if (bonusHealth > 0.0) {
                ensureChestplateAttribute(meta, Attribute.MAX_HEALTH, healthKey, bonusHealth);
            } else {
                Collection<AttributeModifier> healthModifiers = meta.getAttributeModifiers(Attribute.MAX_HEALTH);
                if (healthModifiers != null) {
                    for (AttributeModifier modifier : new ArrayList<>(healthModifiers)) {
                        if (healthKey.equals(modifier.getKey())) meta.removeAttributeModifier(Attribute.MAX_HEALTH, modifier);
                    }
                }
            }
            String hearts = (bonusHearts == Math.rint(bonusHearts))
                ? Long.toString(Math.round(bonusHearts)) : String.format(Locale.ROOT, "%.1f", bonusHearts);
            meta.setDisplayName(ChatColor.AQUA + "Frozen Chestplate of Vitality");
            meta.setLore(List.of(ChatColor.GRAY + "Diamond Rib Trim | Unbreakable",
                ChatColor.BLUE + "Protection III | 8 Armor | 3 Armor Toughness",
                ChatColor.BLUE + (bonusHearts > 0.0
                    ? "Gain +" + hearts + " hearts while worn"
                    : "Extra maximum health disabled in config")));
            item.setItemMeta(meta);
            return true;
        }
        if (isGroupItem(item, "leggings")) {
            ItemMeta meta = item.getItemMeta();
            boolean enabled = getConfig().getBoolean("armor.leggings.fire-resistance-enabled", true);
            meta.setDisplayName(ChatColor.AQUA + "Frozen Leggings of Fire Resistance");
            meta.setLore(List.of(ChatColor.GRAY + "Diamond Rib Trim | Unbreakable",
                ChatColor.BLUE + (enabled ? "Fire Resistance I while equipped"
                    : "Fire Resistance passive disabled in config")));
            item.setItemMeta(meta);
            return true;
        }
        return false;
    }

    private int refreshFrostTooltips(Player player) {
        int updated = 0;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            boolean armorUpdated = refreshFrozenArmorPassiveMetadata(stack);
            boolean tooltipUpdated = FrostTooltips.apply(this, stack);
            if (armorUpdated || tooltipUpdated) {
                player.getInventory().setItem(slot, stack);
                updated++;
            }
        }
        if (updated > 0) player.updateInventory();
        return updated;
    }

    @EventHandler
    public void onFrostPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) return;
            int updated = clearInventoryModels(player);
            int swordUpdates = normalizeFrozenWeapons(player);
            refreshFrostTooltips(player);
            if (updated > 0) player.sendMessage(ChatColor.AQUA + "[Frost] Restored vanilla textures on "
                    + updated + " Frost items.");
            if (swordUpdates > 0) player.sendMessage(ChatColor.AQUA
                    + "[Frost] Updated " + swordUpdates + " Frost weapon(s): enchantments and item attributes normalized.");
        }, 10L);
    }

    // Upgrade existing marked Frost weapons without replacing their PDC identity.
    // The Frozen Sword modifier applies only while the sword is in MAINHAND.
    private int normalizeFrozenWeapons(Player player) {
        int updated = normalizeFrozenSwords(player);
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) continue;
            boolean changed = false;
            ItemMeta meta = item.getItemMeta();
            if (isSpecial(item, Material.NETHERITE_SPEAR, spearKey)) {
                if (meta.getEnchantLevel(Enchantment.SHARPNESS) != 5) {
                    meta.removeEnchant(Enchantment.SHARPNESS);
                    meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                    changed = true;
                }
                if (meta.getEnchantLevel(Enchantment.LUNGE) != 3) {
                    meta.removeEnchant(Enchantment.LUNGE);
                    meta.addEnchant(Enchantment.LUNGE, 3, true);
                    changed = true;
                }
                if (meta.hasLore()) {
                    List<String> lore = new ArrayList<>(meta.getLore());
                    for (int i = 0; i < lore.size(); i++) {
                        if (ChatColor.stripColor(lore.get(i)).contains("Sharpness")) {
                            String updatedLine = ChatColor.GRAY + "Looting IV | Sharpness V | Lunge III";
                            if (!updatedLine.equals(lore.get(i))) {
                                lore.set(i, updatedLine);
                                changed = true;
                            }
                            break;
                        }
                    }
                    meta.setLore(lore);
                }
            } else if (isSpecial(item, Material.CROSSBOW, crossbowKey)) {
                if (meta.getEnchantLevel(Enchantment.FLAME) != 1) {
                    meta.removeEnchant(Enchantment.FLAME);
                    meta.addEnchant(Enchantment.FLAME, 1, true);
                    changed = true;
                }
                if (meta.hasLore()) {
                    List<String> lore = new ArrayList<>(meta.getLore());
                    if (lore.size() > 1 && !ChatColor.stripColor(lore.get(1)).contains("Flame I")) {
                        lore.set(1, ChatColor.GRAY + "Infinity I | Piercing IV | Flame I | Unbreakable");
                        meta.setLore(lore);
                        changed = true;
                    }
                }
            } else if (isSpecial(item, Material.NETHERITE_SWORD, swordKey)) {
                if (meta.getEnchantLevel(Enchantment.EFFICIENCY) != 5) {
                    meta.removeEnchant(Enchantment.EFFICIENCY);
                    meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                    changed = true;
                }
                if (meta.hasLore()) {
                    List<String> lore = new ArrayList<>(meta.getLore());
                    if (!lore.isEmpty() && !ChatColor.stripColor(lore.get(0)).contains("Efficiency V")) {
                        lore.set(0, ChatColor.GRAY + "Sharpness V | Efficiency V | Sweeping Edge IV | Looting IV");
                        meta.setLore(lore);
                        changed = true;
                    }
                }
            }
            if (changed) {
                item.setItemMeta(meta);
                FrostTooltips.apply(this, item);
                player.getInventory().setItem(slot, item);
                updated++;
            }
        }
        return updated;
    }

    private int normalizeFrozenSwords(Player player) {
        int updated = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (!isSpecial(item, Material.NETHERITE_SWORD, swordKey)) continue;
            ItemMeta meta = item.getItemMeta();
            boolean changed = false;
            Collection<AttributeModifier> oldSpeedModifiers = meta.getAttributeModifiers(Attribute.MOVEMENT_SPEED);
            if (oldSpeedModifiers != null) {
                for (AttributeModifier modifier : new ArrayList<>(oldSpeedModifiers)) {
                    if (frozenSwordSpeedKey.equals(modifier.getKey())) {
                        meta.removeAttributeModifier(Attribute.MOVEMENT_SPEED, modifier);
                        changed = true;
                    }
                }
            }
            if (meta.getEnchantLevel(Enchantment.SHARPNESS) != 5) {
                meta.removeEnchant(Enchantment.SHARPNESS);
                meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                changed = true;
            }
            if (changed) {
                item.setItemMeta(meta);
                FrostTooltips.apply(this, item);
                player.getInventory().setItem(slot, item);
                updated++;
            }
        }
        return updated;
    }

    private void tickPassiveFrostParticles() {
        if (!passiveFrostParticles || ticksPassed % frostParticleInterval != 0) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            boolean carriesFrost = false;
            for (ItemStack item : player.getInventory().getContents()) {
                if (isAnyFrostItem(item)) { carriesFrost = true; break; }
            }
            if (!carriesFrost) {
                for (ItemStack armor : player.getInventory().getArmorContents()) {
                    if (isAnyFrostItem(armor)) { carriesFrost = true; break; }
                }
            }
            if (!carriesFrost) continue;
            player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    player.getLocation().add(0.0, 1.0, 0.0), frostParticleCount,
                    0.35, 0.85, 0.35, 0.013);
        }
    }

    ItemStack frostEventReward(String group) {
        return makeItemForGroup(group);
    }

    private ItemStack makeItemForGroup(String group) {
        ItemStack result = switch (group) {
            case "snowball" -> makeSnowball();
            case "tnt" -> makeTnt();
            case "windcharge" -> makeWindCharge();
            case "grapple" -> makeTrident();
            case "riptide" -> makeRiptideTrident();
            case "firework" -> makeFirework();
            case "lava" -> makeLavaTsunami();
            case "battleaxe" -> makeBattleaxe();
            case "shield" -> makeShield();
            case "sword" -> makeSword();
            case "spear" -> makeSpear();
            case "totem" -> makeTotem();
            case "pickaxe", "pickaxe_fortune" -> makeFrozenPickaxe(true);
            case "pickaxe_silk" -> makeFrozenPickaxe(false);
            case "bow" -> makeBow();
            case "crossbow" -> makeCrossbow();
            case "helmet" -> makeHelmet();
            case "chestplate" -> makeChestplate();
            case "leggings" -> makeLeggings();
            case "boots" -> makeBoots();
            case "rod" -> makeFishingRod();
            case "horn" -> makeHorn();
            default -> null;
        };
        FrostTooltips.apply(this, result);
        return result;
    }

    private void openWeaponMenu(Player player) {
        FrostWeaponMenu menu = new FrostWeaponMenu();
        fillMenu(menu.inventory);
        for (Map.Entry<Integer, String> entry : WEAPON_SLOTS.entrySet()) {
            ItemStack weapon = makeItemForGroup(entry.getValue());
            if (weapon != null) menu.inventory.setItem(entry.getKey(), weapon);
        }
        menu.inventory.setItem(40, guiItem(Material.BOOK, "Weapon Collection",
                "Click any weapon to receive one", "Your trust list applies to all abilities"));
        player.openInventory(menu.inventory);
    }

    @EventHandler
    public void onWeaponMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof FrostWeaponMenu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.isOp()) return;
        int slot = event.getRawSlot();
        String group = WEAPON_SLOTS.get(slot);
        if (group == null) return;
        ItemStack weapon = makeItemForGroup(group);
        if (weapon == null) return;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(weapon);
        if (leftovers.isEmpty()) {
            // Vanilla Infinity requires one ordinary arrow in inventory.
            if ((group.equals("bow") || group.equals("crossbow")) && !player.getInventory().contains(Material.ARROW)) {
                player.getInventory().addItem(new ItemStack(Material.ARROW));
            }
            player.sendMessage(ChatColor.GREEN + "Received " + weapon.getItemMeta().getDisplayName());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.15f);
        } else {
            player.sendMessage(ChatColor.RED + "Your inventory is full.");
        }
    }

    @EventHandler
    public void onWeaponMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FrostWeaponMenu)
            event.setCancelled(true);
    }

    // Paper provides a hurt flash without adding extra damage or invulnerability frames.
    private void showAbilityImpact(LivingEntity target) {
        if (target.isValid() && !target.isDead()
                && (!(target instanceof Player player) || (player.isOnline()
                        && player.getGameMode() != GameMode.CREATIVE
                        && player.getGameMode() != GameMode.SPECTATOR))) {
            target.playHurtAnimation(0.0f);
        }
    }

    // Sonic-boom damage bypasses armor and Protection without directly setting health.
    // Natural damage handling therefore still allows ordinary Totems of Undying to activate.
    private void dealTrueDamage(LivingEntity target, double amount) {
        dealTrueDamage(target, amount, null);
    }

    // Properly attributes Frost ability hits to their player so PvP kills, event
    // targets, and boss damage contributions can be recorded; still lets vanilla
    // resurrection totems work normally (unlike direct setHealth changes).
    private void dealTrueDamage(LivingEntity target, double amount, UUID attackerId) {
        if (!target.isValid() || target.isDead() || amount <= 0) return;
        if (target instanceof Player player && (player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR)) return;
        target.setNoDamageTicks(0);
        DamageSource.Builder builder = DamageSource.builder(DamageType.SONIC_BOOM);
        Entity attacker = attackerId == null ? null : Bukkit.getEntity(attackerId);
        if (attacker != null && attacker.isValid()) builder.withCausingEntity(attacker).withDirectEntity(attacker);
        target.damage(amount, builder.build());
        // Visible and audible feedback from the particle-based attacks.
        target.playHurtAnimation(0.0f);
        Sound sound = target instanceof Player ? Sound.ENTITY_PLAYER_HURT : Sound.ENTITY_GENERIC_HURT;
        target.getWorld().playSound(target.getLocation(), sound, 0.8f, 1.0f);
    }

    private void startReadyTimer(Player player, String group, int durationTicks) {
        readyAt.computeIfAbsent(player.getUniqueId(), unused -> new HashMap<>())
                .put(group, ticksPassed + durationTicks);
    }

    private boolean isHoldingGroup(Player player, String group) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return isGroupItem(main, group) || isGroupItem(off, group);
    }

    private boolean isGroupItem(ItemStack item, String group) {
        return switch (group) {
            case "snowball" -> isSpecial(item, Material.SNOWBALL, snowballKey);
            case "tnt" -> isSpecial(item, Material.TNT, tntKey);
            case "windcharge" -> isSpecial(item, Material.WIND_CHARGE, freezeChargeKey);
            case "grapple" -> isSpecial(item, Material.TRIDENT, tridentKey);
            case "riptide" -> isSpecial(item, Material.TRIDENT, riptideTridentKey);
            case "firework" -> isSpecial(item, Material.FIREWORK_ROCKET, fireworksKey);
            case "lava" -> isSpecial(item, Material.FLINT_AND_STEEL, lavaKey);
            case "battleaxe" -> isSpecial(item, Material.NETHERITE_AXE, battleaxeKey);
            case "shield" -> isSpecial(item, Material.SHIELD, shieldKey);
            case "sword" -> isSpecial(item, Material.NETHERITE_SWORD, swordKey);
            case "spear" -> isSpecial(item, Material.NETHERITE_SPEAR, spearKey);
            case "totem" -> isSpecial(item, Material.TOTEM_OF_UNDYING, totemKey);
            case "pickaxe_fortune", "pickaxe" -> isSpecial(item, Material.NETHERITE_PICKAXE, fortunePickaxeKey)
                    || isSpecial(item, Material.NETHERITE_PICKAXE, silkPickaxeKey);
            case "pickaxe_silk" -> isSpecial(item, Material.NETHERITE_PICKAXE, silkPickaxeKey);
            case "bow" -> isSpecial(item, Material.BOW, bowKey);
            case "crossbow" -> isSpecial(item, Material.CROSSBOW, crossbowKey);
            case "helmet" -> isSpecial(item, Material.NETHERITE_HELMET, helmetKey);
            case "chestplate" -> isSpecial(item, Material.NETHERITE_CHESTPLATE, chestplateKey);
            case "leggings" -> isSpecial(item, Material.NETHERITE_LEGGINGS, leggingsKey);
            case "boots" -> isSpecial(item, Material.NETHERITE_BOOTS, bootsKey);
            case "rod" -> isSpecial(item, Material.FISHING_ROD, fishingRodKey);
            case "horn" -> isSpecial(item, Material.GOAT_HORN, hornKey);
            default -> false;
        };
    }

    private void checkReadyMessages() {
        Iterator<Map.Entry<UUID, Map<String, Long>>> users = readyAt.entrySet().iterator();
        while (users.hasNext()) {
            var entry = users.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) { users.remove(); continue; }
            Iterator<Map.Entry<String, Long>> timers = entry.getValue().entrySet().iterator();
            while (timers.hasNext()) {
                var timer = timers.next();
                if (ticksPassed < timer.getValue()) continue;
                // The action bar now shows READY; avoid noisy chat notifications.
                timers.remove();
            }
            if (entry.getValue().isEmpty()) users.remove();
        }
        shieldActiveUntil.entrySet().removeIf(entry -> ticksPassed >= entry.getValue());
        shieldRaisedAt.entrySet().removeIf(entry -> ticksPassed - entry.getValue() > shieldParryWindowTicks + 2);
        shieldStunnedUntil.entrySet().removeIf(entry -> ticksPassed >= entry.getValue());
    }

    // Sovereign-style action-bar cooldown HUD; does not require a resource pack.
    // The server timers remain authoritative, so swapping or dropping items cannot reset cooldowns.
    private void tickFrostCooldownActionBar() {
        if (!getConfig().getBoolean("hud.enabled", true)) return;
        // Preserve the event target/capture action bar during competitive events.
        if (frostEvents != null && frostEvents.reservesActionBar()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack held = player.getInventory().getItemInMainHand();
            String group = null;
            for (String candidate : List.of("snowball", "tnt", "windcharge", "grapple", "riptide", "firework",
                    "lava", "battleaxe", "shield", "sword", "spear", "totem", "pickaxe", "bow", "crossbow", "rod", "horn")) {
                if (isGroupItem(held, candidate)) { group = candidate; break; }
            }
            if (group == null) continue;
            long remaining = Math.max(0L, readyAt.getOrDefault(player.getUniqueId(), Map.of())
                    .getOrDefault(group, 0L) - ticksPassed);
            String status = remaining <= 0 ? ChatColor.GREEN + "READY"
                    : ChatColor.AQUA + String.format(Locale.US, "%.1fs", remaining / 20.0);
            String label = switch (group) {
                case "sword" -> "CRYSTAL DASH";
                case "crossbow" -> "WARDEN SHRIEK";
                case "bow" -> "ARROWFALL";
                case "helmet" -> "REVELATION";
                case "horn" -> "FROST HORDE";
                case "rod" -> "GRAPPLE";
                case "riptide" -> "LIGHTNING";
                case "grapple" -> "TRIDENT PULL";
                case "totem" -> "WINTER BLESSING";
                case "windcharge" -> "FREEZING BURST";
                default -> group.toUpperCase(Locale.ROOT);
            };
            player.sendActionBar(Component.text(ChatColor.DARK_AQUA + "❄ " + ChatColor.WHITE + label
                    + ChatColor.DARK_GRAY + "  •  " + status));
        }
    }

    @SuppressWarnings("unused")
    private void strikePlayerThreeTimes(LivingEntity target, UUID ownerId) {
        UUID targetId = target.getUniqueId();
        for (int n = 0; n < 3; n++) {
            long delay = 1L + (long) n * lightningSpacingTicks;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Entity entity = Bukkit.getEntity(targetId);
                if (!(entity instanceof LivingEntity current) || !isValidFrostTarget(ownerId, current)) return;
                current.getWorld().strikeLightningEffect(current.getLocation());
                dealTrueDamage(current, lightningDamage, ownerId);
            }, delay);
        }
    }

    private boolean startGrappleCooldown(Player owner) {
        UUID id = owner.getUniqueId();
        if (ticksPassed < nextTridentUse.getOrDefault(id, 0L)) return false;
        nextTridentUse.put(id, ticksPassed + grappleCooldownTicks);
        startReadyTimer(owner, "grapple", grappleCooldownTicks);
        return true;
    }

    // Keep dragging the player every server tick rather than delivering
    // one tiny initial impulse. Recalculate toward the owner's current location.
    private void pullVictimToShooter(LivingEntity victim, Player shooter) {
        if (!isValidFrostTarget(shooter.getUniqueId(), victim)
                || !victim.getWorld().equals(shooter.getWorld())) return;
        if (victim.getLocation().distanceSquared(shooter.getLocation()) < 5) return;
        activeGrapples.put(victim.getUniqueId(),
                new GrapplePull(shooter.getUniqueId(), victim.getUniqueId(), ticksPassed + 140));
        victim.getWorld().spawnParticle(Particle.CLOUD,
                victim.getLocation().add(0, 1, 0), 18, .3, .4, .3, .02);
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, .8f);
    }

    private void tickGrapples() {
        Iterator<GrapplePull> iterator = activeGrapples.values().iterator();
        while (iterator.hasNext()) {
            GrapplePull pull = iterator.next();
            Entity victimEntity = Bukkit.getEntity(pull.victimId);
            LivingEntity victim = victimEntity instanceof LivingEntity living ? living : null;
            Player shooter = Bukkit.getPlayer(pull.shooterId);
            if (ticksPassed >= pull.expiresAt || victim == null || shooter == null
                    || !shooter.isOnline() || shooter.isDead() || !victim.isValid() || victim.isDead()
                    || !victim.getWorld().equals(shooter.getWorld())
                    || !isValidFrostTarget(pull.shooterId, victim)
                    || (victim instanceof Player p && isFrozen(p))) {
                iterator.remove();
                continue;
            }
            Location destination = shooter.getLocation().clone().add(0, 0.15, 0);
            Vector delta = destination.toVector().subtract(victim.getLocation().toVector());
            Vector flat = delta.clone().setY(0);
            // Stop next to the player: don't drag two bodies into the same block.
            if (flat.lengthSquared() <= 4.0 && Math.abs(delta.getY()) <= 1.5) {
                victim.setVelocity(new Vector());
                iterator.remove();
                continue;
            }
            if (flat.lengthSquared() > 0.0001) flat.normalize().multiply(grapplePullHorizontal);
            else flat.zero();
            // Pull upward for obstacles/different elevations, downhill if needed.
            double y = Math.max(-0.45, Math.min(0.65,
                    delta.getY() * 0.30 + (victim.isOnGround() ? grapplePullVertical : 0.08)));
            flat.setY(y);
            victim.setVelocity(flat);
            if (ticksPassed % 3 == 0) {
                victim.getWorld().spawnParticle(Particle.CLOUD,
                        victim.getLocation().add(0, 0.8, 0), 4, .25, .25, .25, 0.01);
            }
        }
    }

    private boolean isSpecial(ItemStack item, Material expected, NamespacedKey key) {
        return item != null && item.getType() == expected && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                .has(key, PersistentDataType.BYTE);
    }

    // Right-clicking with the custom shield starts a short parry window.
    // A shield that is still raised when hit after this window suffers a 3-second stun.
    @EventHandler(priority = EventPriority.HIGH)
    public void onShieldActivation(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (!isSpecial(event.getItem(), Material.SHIELD, shieldKey)) return;
        Player user = event.getPlayer();
        UUID id = user.getUniqueId();
        long stunnedUntil = shieldStunnedUntil.getOrDefault(id, 0L);
        if (ticksPassed < stunnedUntil) {
            event.setCancelled(true);
            return;
        }
        if (isFrozen(user)) return;
        // Do not cancel the interaction: vanilla still raises the shield for blocking.
        shieldRaisedAt.put(id, ticksPassed);
    }

    // While the shield buff is active, ordinary axe hits cannot disable the shield.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void preventShieldDisable(PlayerShieldDisableEvent event) {
        Player defender = event.getPlayer();
        if (hasShieldImmunity(defender) && (isGroupItem(defender.getInventory().getItemInMainHand(), "shield")
                || isGroupItem(defender.getInventory().getItemInOffHand(), "shield"))) {
            event.setCancelled(true);
        }
    }

    // A hit inside the raise window is a successful parry. A late block still
    // absorbs damage normally, but disables shield use briefly as a failed parry.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void reflectShieldAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender) || !defender.isBlocking()) return;
        if (!isHoldingGroup(defender, "shield")) return;
        UUID defenderId = defender.getUniqueId();
        long stunnedUntil = shieldStunnedUntil.getOrDefault(defenderId, 0L);
        if (ticksPassed < stunnedUntil) return;

        LivingEntity attacker = null;
        if (event.getDamager() instanceof LivingEntity living) attacker = living;
        else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof LivingEntity living) attacker = living;
        if (attacker == null || !isValidFrostTarget(defenderId, attacker)) return;
        if (!attacker.getWorld().equals(defender.getWorld())
                || attacker.getLocation().distanceSquared(defender.getLocation()) > 36.0) return;

        Long raisedAt = shieldRaisedAt.remove(defenderId);
        boolean successfulParry = raisedAt != null
                && ticksPassed - raisedAt <= shieldParryWindowTicks;
        if (!successfulParry) {
            shieldStunnedUntil.put(defenderId, ticksPassed + shieldStunTicks);
            defender.setCooldown(Material.SHIELD, shieldStunTicks);
            defender.getWorld().playSound(defender.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.85f);
            defender.sendMessage(ChatColor.RED + "[Frost] Failed parry! Your shield is stunned for "
                    + (shieldStunTicks / 20) + " seconds.");
            return;
        }

        // Successful parry: negate the hit, launch the attacker, and grant
        // five seconds of immunity to shield-breaking effects.
        event.setCancelled(true);
        shieldActiveUntil.put(defenderId, ticksPassed + shieldActiveTicks);
        Vector away = attacker.getLocation().toVector().subtract(defender.getLocation().toVector()).setY(0);
        if (away.lengthSquared() < 0.0001) away = defender.getLocation().getDirection().setY(0);
        if (away.lengthSquared() > 0.0001) {
            attacker.setVelocity(away.normalize().multiply(shieldReflectHorizontal).setY(shieldReflectVertical));
        }
        defender.getWorld().playSound(defender.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.65f);
        defender.getWorld().spawnParticle(Particle.SNOWFLAKE,
                defender.getEyeLocation(), 28, .5, .45, .5, .06);
        defender.sendMessage(ChatColor.AQUA + "[Frost] Perfect parry! Shield-break immunity for "
                + (shieldActiveTicks / 20) + " seconds.");
    }

    @EventHandler
    public void onThrow(PlayerLaunchProjectileEvent event) {
        if (event.getProjectile() instanceof WindCharge charge
                && isSpecial(event.getItemStack(), Material.WIND_CHARGE, freezeChargeKey)) {
            Player player = event.getPlayer();
            UUID owner = player.getUniqueId();
            if (isFrozen(player) || ticksPassed < nextWindUse.getOrDefault(owner, 0L)
                    || player.hasCooldown(Material.WIND_CHARGE)) {
                event.setCancelled(true);
                return;
            }
            nextWindUse.put(owner, ticksPassed + windCooldownTicks);
            startReadyTimer(player, "windcharge", windCooldownTicks);
            charge.getPersistentDataContainer().set(freezeChargeKey, PersistentDataType.BYTE, (byte) 1);
            event.setShouldConsume(false);
            // The long ability timer is tracked server-side, not as a dark hotbar overlay.
            return;
        }
        if (!(event.getProjectile() instanceof Snowball snowball)) return;
        if (!isSpecial(event.getItemStack(), Material.SNOWBALL, snowballKey)) return;
        Player player = event.getPlayer();
        if (isFrozen(player) || ticksPassed < snowballNext.getOrDefault(player.getUniqueId(), 0L)) {
            event.setCancelled(true);
            return;
        }
        snowball.getPersistentDataContainer().set(snowballKey, PersistentDataType.BYTE, (byte) 1);
        event.setShouldConsume(false);
        snowballNext.put(player.getUniqueId(), ticksPassed + snowballCooldownTicks);
        startReadyTimer(player, "snowball", snowballCooldownTicks);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTridentThrown(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!isSpecial(trident.getItemStack(), Material.TRIDENT, tridentKey)) return;
        if (!(trident.getShooter() instanceof Player player)) return;
        if (isFrozen(player)) { event.setCancelled(true); return; }
        trident.getPersistentDataContainer().set(tridentKey, PersistentDataType.BYTE, (byte) 1);
        trident.setLoyaltyLevel(3);
    }

    // Riptide can be activated anywhere. In water/rain, vanilla Riptide handles
    // propulsion; in dry conditions, provide a matching directional launch.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRiptideStart(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        if (!isSpecial(event.getItem(), Material.TRIDENT, riptideTridentKey)) return;
        UUID id = player.getUniqueId();
        if (isFrozen(player) || ticksPassed < nextRiptideAbility.getOrDefault(id, 0L)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "[Frost] Riptide is on cooldown.");
            return;
        }
        startRiptideCooldown(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDryRiptideUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!isSpecial(event.getItem(), Material.TRIDENT, riptideTridentKey)) return;
        if (player.isInWaterOrRainOrBubbleColumn()) return; // Let vanilla Riptide run when wet.
        event.setCancelled(true);
        UUID id = player.getUniqueId();
        if (isFrozen(player)) return;
        if (ticksPassed < nextRiptideAbility.getOrDefault(id, 0L)) {
            player.sendMessage(ChatColor.RED + "[Frost] Riptide is on cooldown.");
            return;
        }
        Vector launch = player.getLocation().getDirection().normalize().multiply(2.35);
        launch.setY(Math.max(0.55, launch.getY() + 0.35));
        player.setVelocity(launch);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation(), 24, 0.45, 0.5, 0.45, 0.15);
        startRiptideCooldown(player);
    }

    private void startRiptideCooldown(Player player) {
        UUID id = player.getUniqueId();
        nextRiptideAbility.put(id, ticksPassed + riptideCooldownTicks);
        startReadyTimer(player, "riptide", riptideCooldownTicks);
    }

    // Cancel vanilla hit damage too, not only Frost special effects.
    // Without this, a trusted friend could still take vanilla trident damage.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void preventFriendlyFrostDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (event.getDamager() instanceof Player attacker) {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            if ((isSpecial(weapon, Material.TRIDENT, tridentKey)
                    || isSpecial(weapon, Material.TRIDENT, riptideTridentKey))
                    && trusts(attacker, victim)) event.setCancelled(true);
            return;
        }
        if (event.getDamager() instanceof Projectile shot && shot.getShooter() instanceof Player shooter) {
            boolean isFrostProjectile =
                    (shot instanceof Trident trident
                        && (shot.getPersistentDataContainer().has(tridentKey, PersistentDataType.BYTE)
                        || isSpecial(trident.getItemStack(), Material.TRIDENT, tridentKey)))
                    || shot.getPersistentDataContainer().has(freezeChargeKey, PersistentDataType.BYTE)
                    || shot.getPersistentDataContainer().has(snowballKey, PersistentDataType.BYTE)
                    || (shot instanceof Arrow arrow && isSpecial(arrow.getWeapon(), Material.BOW, bowKey));
            if (isFrostProjectile && trusts(shooter, victim)) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTridentPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim) || attacker.equals(victim)) return;
        if (isFrozen(attacker) || !isValidFrostTarget(attacker.getUniqueId(), victim)) return;
        ItemStack hand = attacker.getInventory().getItemInMainHand();
        if (isSpecial(hand, Material.TRIDENT, tridentKey)) {
            if (startGrappleCooldown(attacker)) pullVictimToShooter(victim, attacker);
            return;
        }
        if (!isSpecial(hand, Material.TRIDENT, riptideTridentKey)) return;
        // Count only registered, uncancelled hits made with a nearly/full attack bar.
        // The MONITOR priority runs after protection and cancelled-hit handlers.
        if (event.getFinalDamage() <= 0.0 || attacker.getAttackCooldown() < 0.90f) return;
        UUID id = attacker.getUniqueId();
        int hit = riptideHitCounters.getOrDefault(id, 0) + 1;
        if (hit > 10) hit = 1;
        riptideHitCounters.put(id, hit);
        if (hit != 4 && hit != 7 && hit != 10) return;
        UUID victimId = victim.getUniqueId();
        Bukkit.getScheduler().runTask(this, () -> {
            Entity entity = Bukkit.getEntity(victimId);
            if (!(entity instanceof LivingEntity current) || !isValidFrostTarget(id, current)) return;
            current.getWorld().strikeLightningEffect(current.getLocation());
            dealTrueDamage(current, lightningDamage, id);
            current.getWorld().playSound(current.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.45f, 1.5f);
        });
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile instanceof Trident trident
                && (trident.getPersistentDataContainer().has(tridentKey, PersistentDataType.BYTE)
                || isSpecial(trident.getItemStack(), Material.TRIDENT, tridentKey))) {
            if (event.getHitEntity() instanceof LivingEntity target
                    && trident.getShooter() instanceof Player shooter
                    && isValidFrostTarget(shooter.getUniqueId(), target)) {
                target.setFireTicks(Math.max(target.getFireTicks(), tridentFireTicks));
                if (startGrappleCooldown(shooter)) {
                    showAbilityImpact(target);
                    pullVictimToShooter(target, shooter);
                }
            }
            return;
        }
        if (projectile instanceof WindCharge
                && projectile.getPersistentDataContainer().has(freezeChargeKey, PersistentDataType.BYTE)) {
            // Remove the special wind charge immediately to suppress its ordinary wind blast.
            // Freeze only the directly hit player; block hits do nothing.
            event.setCancelled(true);
            if (event.getHitEntity() instanceof LivingEntity target
                    && projectile.getShooter() instanceof Player shooter
                    && isValidFrostTarget(shooter.getUniqueId(), target)) {
                showAbilityImpact(target);
                freezeTarget(target, freezeDurationTicks);
            }
            projectile.remove();
            return;
        }
        if (!(projectile instanceof Snowball)) return;
        if (!projectile.getPersistentDataContainer().has(snowballKey, PersistentDataType.BYTE)) return;
        if (!(event.getHitEntity() instanceof LivingEntity target)) return;
        if (projectile.getShooter() instanceof Player shooter
                && !isValidFrostTarget(shooter.getUniqueId(), target)) return;

        Vector direction = projectile.getVelocity().clone().setY(0);
        if (direction.lengthSquared() < 0.0001 && projectile.getShooter() instanceof Player shooter) {
            direction = target.getLocation().toVector()
                    .subtract(shooter.getLocation().toVector()).setY(0);
        }
        if (direction.lengthSquared() < 0.0001) direction = new Vector(0, 0, 1);
        direction.normalize().multiply(snowballHorizontal).setY(snowballVertical);
        showAbilityImpact(target);
        target.setVelocity(direction);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                slownessTicks, 1, false, true, true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void stopWindExplosion(ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof WindCharge charge
                && charge.getPersistentDataContainer().has(freezeChargeKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFrozenMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        UUID id = event.getPlayer().getUniqueId();
        Location duelAnchor = duelCountdownAnchors.get(id);
        if (duelCountdownPlayers.contains(id) && duelAnchor != null) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (!from.getWorld().equals(to.getWorld()) || from.getX() != to.getX()
                    || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                Location locked = duelAnchor.clone();
                locked.setYaw(to.getYaw());
                locked.setPitch(to.getPitch());
                event.setTo(locked);
            }
            return;
        }
        FrozenPlayer frozen = frozenPlayers.get(id);
        if (frozen == null) return;
        event.setTo(frozen.anchor.clone());
    }

    // Stunned players cannot interact with blocks or use weapons, but can still
    // eat ANY edible item, including normal and enchanted golden apples.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventFrozenUse(PlayerInteractEvent event) {
        if (!isFrozen(event.getPlayer())) return;
        ItemStack held = event.getItem();
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && held != null && held.getType().isEdible()) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.ALLOW);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventFrozenEntityUse(PlayerInteractEntityEvent event) {
        if (isFrozen(event.getPlayer())) event.setCancelled(true);
    }

    // Intentionally allow vanilla PlayerItemConsumeEvent: food remains usable while stunned.

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventFrozenAnimation(PlayerAnimationEvent event) {
        if (isFrozen(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventFrozenPlace(BlockPlaceEvent event) {
        if (isFrozen(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventFrozenBreak(BlockBreakEvent event) {
        if (isFrozen(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventFrozenBlockDamage(BlockDamageEvent event) {
        if (isFrozen(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventFrozenDrop(PlayerDropItemEvent event) {
        if (isFrozen(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventFrozenSwap(PlayerSwapHandItemsEvent event) {
        if (isFrozen(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventFrozenAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && isFrozen(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventFreezingDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FREEZE
                && event.getEntity() instanceof Player player && isFrozen(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFrozenQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        UUID quitterId = quitter.getUniqueId();
        UUID opponentId = combatOpponents.get(quitterId);
        if (getConfig().getBoolean("combat.enabled", true) && getConfig().getBoolean("combat.kill-on-logout", true)
                && combatExpiresAt.getOrDefault(quitterId, 0L) > System.currentTimeMillis()) {
            Player opponent = opponentId == null ? null : Bukkit.getPlayer(opponentId);
            if (getConfig().getBoolean("combat.broadcast-combat-log", true))
                Bukkit.broadcastMessage(ChatColor.RED + quitter.getName() + " has combat logged!");
            if (opponent != null && opponent.isOnline()) quitter.damage(1000.0, opponent);
            else quitter.setHealth(0.0);
        }
        clearCombat(quitterId);
        if (opponentId != null) clearCombat(opponentId);
        lastPlayerChatMessage.remove(quitterId);
        lastPlayerChatMessageAt.remove(quitterId);
        lastPlayerChatAtNanos.remove(quitterId);
        clearPickaxeMiningState(event.getPlayer());
        releaseFrozen(event.getPlayer());
        endDuelFor(event.getPlayer().getUniqueId(), "Your opponent disconnected. The duel is over.");
        fishingAnchors.remove(event.getPlayer().getUniqueId());
        fishingAnchorBlocks.remove(event.getPlayer().getUniqueId());
        UUID playerId = event.getPlayer().getUniqueId();
        shieldRaisedAt.remove(playerId);
        shieldStunnedUntil.remove(playerId);
        shieldActiveUntil.remove(playerId);
    }


    // -------------------------------------------------------------
    // Frozen Bow: prepare with sneak/right-click; next charged shot marks
    // the impact location. Rain falls for 5 seconds from the sky above it.
    // -------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onBowPrepare(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isGroupItem(event.getItem(), "bow")) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return; // regular right click draws the bow
        event.setCancelled(true);
        if (isFrozen(player)) return;
        if (ticksPassed < nextBowUse.getOrDefault(player.getUniqueId(), 0L)) {
            player.sendMessage(ChatColor.RED + "[Frost] Frozen Bow is on cooldown.");
            return;
        }
        preparedBows.add(player.getUniqueId());
        player.sendMessage(ChatColor.AQUA + "[Frost] " + ChatColor.GREEN
                + "Ability prepared. Charge the bow.");
        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1f, 1f);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFrozenBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isGroupItem(event.getBow(), "bow")) return;
        UUID id = player.getUniqueId();
        if (!preparedBows.contains(id)) return;
        if (ticksPassed < nextBowUse.getOrDefault(id, 0L)) {
            preparedBows.remove(id);
            return;
        }
        // A partial shot is allowed, but the player must actually fire an arrow.
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        preparedBows.remove(id);
        nextBowUse.put(id, ticksPassed + bowCooldownTicks);
        startReadyTimer(player, "bow", bowCooldownTicks);
        arrow.getPersistentDataContainer().set(preparedArrowKey, PersistentDataType.BYTE, (byte) 1);
        arrow.getPersistentDataContainer().set(bowOwnerKey, PersistentDataType.STRING, id.toString());
        arrow.setDamage(0);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        player.sendMessage(ChatColor.AQUA + "[Frost] Frozen arrow fired! Await impact...");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBowArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        var pdc = arrow.getPersistentDataContainer();
        String ownerString = pdc.get(bowOwnerKey, PersistentDataType.STRING);
        if (ownerString == null) return;
        UUID owner;
        try { owner = UUID.fromString(ownerString); }
        catch (IllegalArgumentException ex) { arrow.remove(); return; }
        if (pdc.has(preparedArrowKey, PersistentDataType.BYTE)) {
            Location landing = event.getHitEntity() != null
                    ? event.getHitEntity().getLocation() : arrow.getLocation();
            arrow.remove();
            startFrozenArrowRain(landing, owner);
        } else if (pdc.has(fallingArrowKey, PersistentDataType.BYTE)) {
            if (event.getHitEntity() instanceof LivingEntity target && isValidFrostTarget(owner, target)) {
                dealTrueDamage(target, bowArrowDamage, owner);
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                        bowBlindnessTicks, 1, true, true, true));
                target.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        target.getLocation().add(0, target.getHeight() * .5, 0), 22, .4, .5, .4, .02);
            }
            arrow.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void preventExtraBowArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (arrow.getPersistentDataContainer().has(preparedArrowKey, PersistentDataType.BYTE)
                || arrow.getPersistentDataContainer().has(fallingArrowKey, PersistentDataType.BYTE)) {
            event.setCancelled(true); // only custom arrow rain does exactly 1 heart per hit
        }
    }

    private void startFrozenArrowRain(Location landing, UUID shooterId) {
        Location center = landing.clone();
        if (center.getWorld() == null) return;
        center.getWorld().playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1f, .8f);
        center.getWorld().spawnParticle(Particle.SNOWFLAKE,
                center.clone().add(0, 1, 0), 120, bowRainSide / 2, 1.1, bowRainSide / 2, .02);
        // A real falling Arrow projectile for each streak; duration is fixed.
        for (int step = 0; step < bowRainTicks; step += bowWaveIntervalTicks) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (center.getWorld() == null) return;
                Player shooter = Bukkit.getPlayer(shooterId);
                for (int i = 0; i < bowArrowsPerWave; i++) {
                    double x = (ThreadLocalRandom.current().nextDouble() - .5) * bowRainSide;
                    double z = (ThreadLocalRandom.current().nextDouble() - .5) * bowRainSide;
                    // Bias some arrows toward visible enemies to make impacts meaningful.
                    List<LivingEntity> candidates = nearbyFrostTargets(center,
                            bowRainSide / 2, 12, shooterId);
                    if (i == 0 && !candidates.isEmpty()) {
                        LivingEntity target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
                        x = target.getLocation().getX() - center.getX();
                        z = target.getLocation().getZ() - center.getZ();
                    }
                    Location sky = center.clone().add(x, 12.0, z);
                    if (!sky.getBlock().isPassable()) continue;
                    Arrow falling = center.getWorld().spawnArrow(sky, new Vector(0, -1, 0), 1.8f, 0f);
                    falling.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    falling.setDamage(0);
                    if (shooter != null) falling.setShooter(shooter);
                    falling.getPersistentDataContainer().set(fallingArrowKey, PersistentDataType.BYTE, (byte) 1);
                    falling.getPersistentDataContainer().set(bowOwnerKey, PersistentDataType.STRING,
                            shooterId.toString());
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE, sky,
                            6, .2, .3, .2, .02);
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (falling.isValid()) falling.remove();
                    }, 45L);
                }
                center.getWorld().playSound(center, Sound.ENTITY_ARROW_SHOOT, .45f, 1.4f);
            }, step);
        }
    }

    // -------------------------------------------------------------
    // Frost 3x3 pickaxes. Mine the middle block normally; after it breaks,
    // break up to eight adjacent blocks on the same plane. Synthetic
    // BlockBreakEvents let region/protection plugins veto each extra block.
    // Only pickaxe-preferred, ordinary non-container blocks can be affected.
    // -------------------------------------------------------------
    private boolean isFrostPickaxe(ItemStack stack) {
        return isGroupItem(stack, "pickaxe_fortune") || isGroupItem(stack, "pickaxe_silk");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void switchPickaxeMode(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isFrostPickaxe(event.getItem())) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (isFrozen(player)) return;
        ItemStack held = player.getInventory().getItemInMainHand();
        ItemMeta meta = held.getItemMeta();
        String current = meta.getPersistentDataContainer().get(pickaxeModeKey, PersistentDataType.STRING);
        if (current == null) current = meta.hasEnchant(Enchantment.SILK_TOUCH) ? "silk" : "fortune";
        boolean fortune = current.equals("fortune");
        String miningMode = meta.getPersistentDataContainer().get(pickaxeMiningModeKey, PersistentDataType.STRING);
        if (miningMode == null) miningMode = "3x3";
        if (player.isSneaking()) {
            miningMode = miningMode.equals("3x3") ? "1x1" : "3x3";
            meta.getPersistentDataContainer().set(pickaxeMiningModeKey, PersistentDataType.STRING, miningMode);
            held.setItemMeta(meta);
            applyPickaxeMode(held, fortune);
            FrostTooltips.apply(this, held);
            player.getInventory().setItemInMainHand(held);
            player.sendMessage(ChatColor.AQUA + "[Frost] Mining size: " + ChatColor.GREEN + miningMode);
        } else {
            boolean switchToFortune = current.equals("silk");
            applyPickaxeMode(held, switchToFortune);
            FrostTooltips.apply(this, held);
            player.getInventory().setItemInMainHand(held);
            player.sendMessage(ChatColor.AQUA + "[Frost] Drop mode: " + ChatColor.GREEN
                    + (switchToFortune ? "Fortune V" : "Silk Touch I"));
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, .6f, 1.3f);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void trackPickaxeFace(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.LEFT_CLICK_BLOCK
                || !isFrostPickaxe(event.getItem()) || event.getClickedBlock() == null) return;
        UUID id = event.getPlayer().getUniqueId();
        lastMiningFace.put(id, event.getBlockFace());
        lastMiningClick.put(id, event.getClickedBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beginFrozenPickaxeMining(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (!pickaxesEnabled || player.getGameMode() != GameMode.SURVIVAL || isFrozen(player)
                || !isFrostPickaxe(player.getInventory().getItemInMainHand())) {
            clearPickaxeMiningState(player);
            return;
        }
        UUID id = player.getUniqueId();
        pickaxeMiningStartedAt.putIfAbsent(id, ticksPassed);
        pickaxeLastMiningSwingAt.put(id, ticksPassed);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void refreshFrozenPickaxeMiningSwing(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!pickaxeMiningStartedAt.containsKey(id)) return;
        if (!pickaxesEnabled || player.getGameMode() != GameMode.SURVIVAL || isFrozen(player)
                || !isFrostPickaxe(player.getInventory().getItemInMainHand()) || !hasMineableTarget(player)) {
            clearPickaxeMiningState(player);
            return;
        }
        pickaxeLastMiningSwingAt.put(id, ticksPassed);
    }

    private boolean hasMineableTarget(Player player) {
        Block target = player.getTargetBlockExact(6);
        return target != null && !target.isPassable() && !target.getType().isAir()
                && target.getType().getHardness() > 0.0f;
    }

    private void tickPickaxeMiningPassive() {
        int delayTicks = Math.max(1, getConfig().getInt("pickaxes.mining-haste.delay-seconds", 5)) * 20;
        int amplifier = Math.max(0, Math.min(4, getConfig().getInt("pickaxes.mining-haste.amplifier", 1)));
        boolean enabled = pickaxesEnabled && getConfig().getBoolean("pickaxes.mining-haste.enabled", true);
        for (UUID id : List.copyOf(pickaxeMiningStartedAt.keySet())) {
            Player player = Bukkit.getPlayer(id);
            Long started = pickaxeMiningStartedAt.get(id);
            Long lastSwing = pickaxeLastMiningSwingAt.get(id);
            if (player == null || !player.isOnline() || player.isDead() || started == null || lastSwing == null
                    || !enabled || player.getGameMode() != GameMode.SURVIVAL || isFrozen(player)
                    || !isFrostPickaxe(player.getInventory().getItemInMainHand())
                    || ticksPassed - lastSwing > 8 || !hasMineableTarget(player)) {
                if (player != null) clearPickaxeMiningState(player);
                else removePickaxeMiningTracking(id);
                continue;
            }
            if (ticksPassed - started >= delayTicks) applyPickaxeMiningHaste(player, amplifier);
        }
    }

    private void applyPickaxeMiningHaste(Player player, int amplifier) {
        UUID id = player.getUniqueId();
        if (!pickaxeHasteApplied.contains(id)) {
            PotionEffect previous = player.getPotionEffect(PotionEffectType.HASTE);
            // An already stronger external Haste effect is left untouched.
            if (previous != null && previous.getAmplifier() > amplifier) return;
            previousPickaxeHaste.put(id, previous);
            pickaxeHasteApplied.add(id);
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, amplifier, true, false, true), true);
    }

    private void clearPickaxeMiningState(Player player) {
        UUID id = player.getUniqueId();
        pickaxeMiningStartedAt.remove(id);
        pickaxeLastMiningSwingAt.remove(id);
        if (pickaxeHasteApplied.remove(id)) {
            player.removePotionEffect(PotionEffectType.HASTE);
            PotionEffect previous = previousPickaxeHaste.remove(id);
            if (previous != null && previous.getDuration() > 0)
                player.addPotionEffect(previous, true);
        } else {
            previousPickaxeHaste.remove(id);
        }
    }

    private void removePickaxeMiningTracking(UUID id) {
        pickaxeMiningStartedAt.remove(id);
        pickaxeLastMiningSwingAt.remove(id);
        previousPickaxeHaste.remove(id);
        pickaxeHasteApplied.remove(id);
    }

    private BlockFace estimateMiningFace(Player player) {
        Vector view = player.getEyeLocation().getDirection();
        double x = Math.abs(view.getX()), y = Math.abs(view.getY()), z = Math.abs(view.getZ());
        if (y >= x && y >= z) return view.getY() > 0 ? BlockFace.UP : BlockFace.DOWN;
        if (x >= z) return view.getX() > 0 ? BlockFace.EAST : BlockFace.WEST;
        return view.getZ() > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFrostPickaxeMine(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (activeMiningBursts.contains(id) || !pickaxesEnabled
                || player.getGameMode() != GameMode.SURVIVAL || isFrozen(player)) return;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isFrostPickaxe(held)) return;
        String miningMode = held.getItemMeta().getPersistentDataContainer()
                .get(pickaxeMiningModeKey, PersistentDataType.STRING);
        if ("1x1".equals(miningMode)) return;
        Block center = event.getBlock();
        Location storedClick = lastMiningClick.get(id);
        BlockFace face = storedClick != null && storedClick.equals(center.getLocation())
                ? lastMiningFace.getOrDefault(id, estimateMiningFace(player))
                : estimateMiningFace(player);
        // Keep the original event vanilla; defer neighbors one tick so the
        // main block's successful break and any protection cancellation finish first.
        ItemStack tool = held.clone();
        int radius = 1; // Exactly 3x3 when area mining is enabled.
        Bukkit.getScheduler().runTask(this, () -> {
            if (!player.isOnline() || !player.getWorld().equals(center.getWorld())
                    || !isFrostPickaxe(player.getInventory().getItemInMainHand())) return;
            String currentMiningMode = player.getInventory().getItemInMainHand().getItemMeta()
                    .getPersistentDataContainer().get(pickaxeMiningModeKey, PersistentDataType.STRING);
            if ("1x1".equals(currentMiningMode)) return;
            if (player.getLocation().distanceSquared(center.getLocation().add(.5, .5, .5)) > 64) return;
            // Canceled original breaks leave their block intact.
            if (center.getType() != Material.AIR) return;
            if (!activeMiningBursts.add(id)) return;
            try {
                for (int a = -radius; a <= radius; a++) {
                    for (int b = -radius; b <= radius; b++) {
                        if (a == 0 && b == 0) continue;
                        Block neighbor = switch (face) {
                            case UP, DOWN -> center.getRelative(a, 0, b);
                            case NORTH, SOUTH -> center.getRelative(a, b, 0);
                            default -> center.getRelative(0, a, b);
                        };
                        if (!safeExtraMine(neighbor, tool)) continue;
                        BlockBreakEvent extra = new BlockBreakEvent(neighbor, player);
                        Bukkit.getPluginManager().callEvent(extra);
                        if (!extra.isCancelled() && safeExtraMine(neighbor, tool)) {
                            // Paper applies Fortune/Silk Touch to natural drops.
                            neighbor.breakNaturally(tool, true, true);
                        }
                    }
                }
            } finally {
                activeMiningBursts.remove(id);
            }
        });
    }

    private boolean safeExtraMine(Block block, ItemStack tool) {
        Material type = block.getType();
        return type.isBlock() && type.isSolid() && type.getHardness() > 0.0f
                && type.getHardness() < 50.0f
                && block.isPreferredTool(tool)
                && !(block.getState() instanceof TileState);
    }

    // -------------------------------------------------------------
    // Frozen Spear: left-click air/block launches a non-consumable visual spear,
    // followed by sequential ground spikes. Ordinary EntityDamageByEntityEvent
    // melee hits are left completely untouched so the spear can stab normally.
    // Each target is affected at most once per ability throw.
    // -------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onSpearUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isGroupItem(event.getItem(), "spear")) return;
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        event.setCancelled(true); // ability replaces the vanilla left-click action
        launchFrozenSpear(event.getPlayer());
    }

    // The ability is intentionally bound to LEFT_CLICK_AIR / LEFT_CLICK_BLOCK.
    // Do not intercept EntityDamageByEntityEvent for the spear: attributed ability damage
    // is also represented as an entity-damage event, and cancelling it here would cancel
    // both the spear's normal melee hits and the ability's own damage.
    private void launchFrozenSpear(Player owner) {
        if (isFrozen(owner) || owner.isDead()) return;
        UUID id = owner.getUniqueId();
        if (ticksPassed < nextSpearUse.getOrDefault(id, 0L)) return;

        Vector look = owner.getEyeLocation().getDirection().normalize();
        Vector flat = look.clone().setY(0);
        if (flat.lengthSquared() < 0.0001) flat = owner.getLocation().getDirection().setY(0);
        if (flat.lengthSquared() < 0.0001) flat = new Vector(0, 0, 1);
        flat.normalize();
        Vector travel = flat.clone().multiply(0.9).setY(-0.36);
        Location origin = owner.getEyeLocation().add(flat.clone().multiply(1.1));
        ArmorStand visual = owner.getWorld().spawn(origin, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setArms(true);
            stand.setSilent(true);
            stand.setInvulnerable(true);
            stand.setPersistent(false);
            stand.getEquipment().setItemInMainHand(makeSpear());
        });
        flyingSpears.add(new FlyingSpear(visual, id, travel, flat));
        nextSpearUse.put(id, ticksPassed + spearCooldownTicks);
        startReadyTimer(owner, "spear", spearCooldownTicks);
        owner.getWorld().playSound(owner.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.9f, 0.7f);
    }

    private void tickFlyingSpears() {
        Iterator<FlyingSpear> it = flyingSpears.iterator();
        while (it.hasNext()) {
            FlyingSpear spear = it.next();
            Player owner = Bukkit.getPlayer(spear.owner);
            if (owner == null || !owner.isOnline() || !spear.visual.isValid()
                    || owner.getWorld() != spear.visual.getWorld()) {
                if (spear.visual.isValid()) spear.visual.remove();
                it.remove();
                continue;
            }
            spear.age++;
            Location here = spear.visual.getLocation();
            Location next = here.clone().add(spear.heading);

            // Check the whole movement segment, not just the spear stand's center.
            // This catches players and mobs even when the visual travels between them
            // during one server tick.
            LivingEntity directHit = findSpearEntityHit(here, next, spear.owner, spear.damaged);
            if (directHit != null) {
                spear.damaged.add(directHit.getUniqueId());
                dealTrueDamage(directHit, spearDamage, spear.owner);
                if (!directHit.isDead()) {
                    Vector velocity = directHit.getVelocity();
                    velocity.setY(Math.max(velocity.getY(), spearLaunchVelocity));
                    directHit.setVelocity(velocity);
                }
                Location impactPoint = directHit.getLocation().clone();
                impactPoint.getWorld().spawnParticle(Particle.BLOCK, impactPoint.clone().add(0, 0.9, 0),
                        28, 0.35, 0.45, 0.35, 0.04, Material.PACKED_ICE.createBlockData());
                impactPoint.getWorld().playSound(impactPoint, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.65f);
                spear.visual.remove();
                it.remove();
                Location hit = findSpearGround(impactPoint, impactPoint.getBlockY());
                eruptSpearSpikes(spear.owner, hit, spear.horizontal, spear.damaged);
                continue;
            }

            // The spear is a display entity, so test actual world collisions manually.
            boolean impact = spear.age >= 12 ||
                    !next.getBlock().isPassable() || !next.clone().add(0, -0.6, 0).getBlock().isPassable();
            if (!impact) {
                spear.visual.teleport(next);
                next.getWorld().spawnParticle(Particle.SNOWFLAKE, next, 6, 0.12, 0.12, 0.12, 0.01);
            } else {
                spear.visual.remove();
                it.remove();
                Location hit = findSpearGround(next, owner.getLocation().getBlockY());
                hit.getWorld().playSound(hit, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.65f);
                eruptSpearSpikes(spear.owner, hit, spear.horizontal, spear.damaged);
            }
        }
    }

    // Tests a short flight segment against real entity hitboxes. Sampling the segment
    // avoids tunnelling through an entity between the spear's per-tick positions.
    private LivingEntity findSpearEntityHit(Location from, Location to, UUID ownerId, Set<UUID> alreadyHit) {
        Location midpoint = from.clone().add(to.toVector().subtract(from.toVector()).multiply(0.5));
        for (Entity entity : midpoint.getWorld().getNearbyEntities(midpoint, 1.4, 1.4, 1.4)) {
            if (!(entity instanceof LivingEntity living)
                    || !isValidFrostTarget(ownerId, living)
                    || alreadyHit.contains(living.getUniqueId())) continue;
            BoundingBox hitbox = living.getBoundingBox().clone().expand(0.30);
            for (int sample = 0; sample <= 12; sample++) {
                double t = sample / 12.0;
                double x = from.getX() + (to.getX() - from.getX()) * t;
                double y = from.getY() + (to.getY() - from.getY()) * t;
                double z = from.getZ() + (to.getZ() - from.getZ()) * t;
                if (hitbox.contains(x, y, z)) return living;
            }
        }
        return null;
    }

    // Stay aligned with uneven terrain; no real ice blocks are placed.
    private Location findSpearGround(Location point, int preferredY) {
        int x = point.getBlockX(), z = point.getBlockZ();
        int startY = Math.max(point.getBlockY(), preferredY + 1);
        for (int y = startY + 2; y >= startY - 6; y--) {
            if (y < point.getWorld().getMinHeight() + 1 || y >= point.getWorld().getMaxHeight()) continue;
            if (!point.getWorld().getBlockAt(x, y - 1, z).isPassable()
                    && point.getWorld().getBlockAt(x, y, z).isPassable()) {
                return new Location(point.getWorld(), x + 0.5, y, z + 0.5);
            }
        }
        return point.clone();
    }

    private void eruptSpearSpikes(UUID ownerId, Location landing, Vector direction, Set<UUID> hit) {
        // Every new spike grows in height/width AND reaches farther than the last.
        // Packed-ice BLOCK particles look chunky without replacing or griefing terrain.
        for (int index = 0; index < spearSpikeCount; index++) {
            final int offset = index;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Player owner = Bukkit.getPlayer(ownerId);
                if (owner == null || !owner.isOnline() || owner.isDead()) return;
                double distance = (offset + 1) * spearSpikeSpacing
                        + spearDistanceGrowth * (offset * (offset + 1)) / 2.0;
                Location step = landing.clone().add(direction.clone().multiply(distance));
                Location spike = findSpearGround(step, step.getBlockY());
                if (!spike.getWorld().equals(owner.getWorld())) return;
                double scale = 1.0 + offset * spearSizeGrowth;
                double height = 2.4 + offset * spearSizeGrowth * 1.75;
                double baseRadius = Math.min(2.8, 0.85 * scale);
                int bands = Math.min(22, 12 + offset);
                // Layer broad chunky packed-ice shards into a tapered spire.
                for (int layer = 0; layer < bands; layer++) {
                    double relative = (double) layer / Math.max(1, bands - 1);
                    double y = height * relative;
                    double radius = Math.max(0.10, baseRadius * (1.0 - Math.pow(relative, 1.35)));
                    Location slice = spike.clone().add(0, y, 0);
                    int blocks = Math.max(8, (int)(12 + baseRadius * 9));
                    spike.getWorld().spawnParticle(Particle.BLOCK, slice, blocks,
                            radius, .12, radius, 0.0, Material.PACKED_ICE.createBlockData());
                    spike.getWorld().spawnParticle(Particle.SNOWFLAKE, slice, 5,
                            radius * 1.15, .22, radius * 1.15, .02);
                }
                // Ice shock ring at the base emphasizes how wide later spikes are.
                for (int n = 0; n < 18; n++) {
                    double angle = 2.0 * Math.PI * n / 18.0;
                    Location edge = spike.clone().add(Math.cos(angle) * baseRadius, 0.22,
                            Math.sin(angle) * baseRadius);
                    spike.getWorld().spawnParticle(Particle.BLOCK, edge, 9,
                            0.22, 0.24, 0.22, 0, Material.BLUE_ICE.createBlockData());
                }
                spike.getWorld().playSound(spike, Sound.BLOCK_GLASS_BREAK, .75f,
                        Math.max(.5f, 1.35f - offset * .08f));
                for (LivingEntity target : nearbyFrostTargets(spike, baseRadius + 1.5, height + 2.0, ownerId)) {
                    if (hit.contains(target.getUniqueId())) continue;
                    BoundingBox box = target.getBoundingBox();

                    // Check overlap with the spike's actual volume. The old feet-only
                    // distance check missed targets standing on slightly uneven ground
                    // or whose hitbox overlapped the ice even when their feet did not.
                    double nearestX = Math.max(box.getMinX(), Math.min(spike.getX(), box.getMaxX()));
                    double nearestZ = Math.max(box.getMinZ(), Math.min(spike.getZ(), box.getMaxZ()));
                    double dx = nearestX - spike.getX();
                    double dz = nearestZ - spike.getZ();
                    double hitRadius = baseRadius + 0.30;
                    boolean horizontalOverlap = dx * dx + dz * dz <= hitRadius * hitRadius;
                    boolean verticalOverlap = box.getMaxY() >= spike.getY() - 0.20
                            && box.getMinY() <= spike.getY() + height;
                    if (horizontalOverlap && verticalOverlap) {
                        hit.add(target.getUniqueId());
                        dealTrueDamage(target, spearDamage, ownerId);
                        if (!target.isDead()) {
                            Vector velocity = target.getVelocity();
                            velocity.setY(Math.max(velocity.getY(), spearLaunchVelocity));
                            target.setVelocity(velocity);
                        }
                    }
                }
            }, (long) index * spearSpikeInterval);
        }
    }

    // Frozen Totem aura follows its caster for the configurable duration.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTotemUse(PlayerInteractEvent event) {
        if (!isGroupItem(event.getItem(), "totem")) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);
        Player owner = event.getPlayer();
        if (isFrozen(owner) || owner.isDead()) return;
        UUID id = owner.getUniqueId();
        if (ticksPassed < nextTotemUse.getOrDefault(id, 0L)) return;
        nextTotemUse.put(id, ticksPassed + totemCooldownTicks);
        activeTotemAuras.put(id, ticksPassed + totemActiveTicks);
        startReadyTimer(owner, "totem", totemCooldownTicks);
        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
    }

    private void tickTotemAuras() {
        Iterator<Map.Entry<UUID, Long>> iterator = activeTotemAuras.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> active = iterator.next();
            Player owner = Bukkit.getPlayer(active.getKey());
            if (owner == null || !owner.isOnline() || owner.isDead() || ticksPassed >= active.getValue()) {
                iterator.remove();
                continue;
            }
            Location at = owner.getLocation();
            if (ticksPassed % 4 == 0) {
                // Colored icy circle always follows the player, without placing blocks.
                for (int n = 0; n < 40; n++) {
                    double angle = Math.PI * 2.0 * n / 40.0;
                    Location ring = at.clone().add(Math.cos(angle) * totemRadius, 0.25,
                            Math.sin(angle) * totemRadius);
                    at.getWorld().spawnParticle(Particle.SNOWFLAKE, ring, 1, 0, 0, 0, 0);
                    if (n % 2 == 0) at.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, ring, 1, 0, 0, 0, 0);
                }
            }
            if (ticksPassed % totemEffectRefresh != 0) continue;
            for (Entity entity : at.getWorld().getNearbyEntities(at, totemRadius, totemRadius, totemRadius)) {
                if (!(entity instanceof LivingEntity target) || target instanceof ArmorStand
                        || target.isDead() || !target.isValid()
                        || (target instanceof Player p && (p.getGameMode() == GameMode.SPECTATOR
                            || p.getGameMode() == GameMode.CREATIVE))
                        || target.getLocation().distanceSquared(at) > totemRadius * totemRadius) continue;
                boolean ally = target.equals(owner) || trusts(owner.getUniqueId(), target.getUniqueId());
                PotionEffectType type = ally ? PotionEffectType.REGENERATION : PotionEffectType.WEAKNESS;
                // Short buffs naturally expire when someone leaves the moving aura.
                target.addPotionEffect(new PotionEffect(type,
                        Math.max(30, totemEffectRefresh + 14), 0, true, true, true));
                if (ticksPassed % 20 == 0) target.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        target.getLocation().add(0, 1.0, 0), 10, 0.3, 0.5, 0.3, 0.02);
            }
        }
    }

    // Prevent only the custom Frost Totem from resurrecting its holder.
    // Vanilla totems remain fully functional.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void preventFrozenTotemResurrection(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        EquipmentSlot usedHand = event.getHand();
        if (usedHand == null) return;
        ItemStack used = usedHand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (isGroupItem(used, "totem")) event.setCancelled(true);
    }

    // Frozen Sword: a 9-block default forward dash over several ticks.
    // Uses an explicit swept-player hit check so fast movement doesn't miss targets.
    @EventHandler(priority = EventPriority.HIGH)
    public void onSwordUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!isSpecial(event.getItem(), Material.NETHERITE_SWORD, swordKey)) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);
        Player user = event.getPlayer();
        if (isFrozen(user) || user.isDead() || user.isInsideVehicle()) return;
        UUID id = user.getUniqueId();
        if (ticksPassed < nextSwordUse.getOrDefault(id, 0L) || activeDashes.containsKey(id)) return;
        // Horizontal dash follows yaw, keeping the user at the same elevation.
        Vector direction = user.getEyeLocation().getDirection().setY(0);
        if (direction.lengthSquared() < 0.00001) return;
        direction.normalize();
        nextSwordUse.put(id, ticksPassed + swordCooldownTicks);
        startReadyTimer(user, "sword", swordCooldownTicks);
        SwordDash dash = new SwordDash(id, direction, user.getLocation());
        activeDashes.put(id, dash);
        // Physics-based lunge instead of repeatedly teleporting/visually sliding.
        // A SINGLE knockback-like impulse: no more artificial velocity overwrite each tick.
        Vector initialVelocity = direction.clone().multiply(swordDashSpeed);
        initialVelocity.setY(user.isOnGround() ? 0.32 : Math.max(.15, user.getVelocity().getY()));
        user.setVelocity(initialVelocity);
        user.setFallDistance(0);
        addSwordTrail(id, user.getLocation());
        user.getWorld().playSound(user.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.75f);
    }

    private void addSwordTrail(UUID ownerId, Location where) {
        swordTrails.add(new IceTrailPoint(ownerId, where.clone(), ticksPassed + swordTrailTicks));
        where.getWorld().spawnParticle(Particle.SNOWFLAKE, where.clone().add(0, 0.3, 0),
                12, 0.35, 0.12, 0.35, 0.015);
    }

    // Conservative player-sized clearance test: don't teleport into walls.
    private boolean swordPathClear(Location destination) {
        org.bukkit.World world = destination.getWorld();
        for (double dx : new double[] {-0.28, 0.28}) {
            for (double dz : new double[] {-0.28, 0.28}) {
                for (double dy : new double[] {0.10, 0.95, 1.75}) {
                    if (!world.getBlockAt(destination.clone().add(dx, dy, dz)).isPassable())
                        return false;
                }
            }
        }
        return true;
    }

    private void tickSwordDashes() {
        Iterator<Map.Entry<UUID, SwordDash>> iterator = activeDashes.entrySet().iterator();
        while (iterator.hasNext()) {
            SwordDash dash = iterator.next().getValue();
            Player user = Bukkit.getPlayer(dash.userId);
            if (user == null || !user.isOnline() || user.isDead()
                    || isFrozen(user) || user.isInsideVehicle()) {
                iterator.remove();
                continue;
            }
            Location current = user.getLocation().clone();
            Location previous = dash.lastPosition;
            if (!previous.getWorld().equals(current.getWorld())) { iterator.remove(); continue; }
            // Process the player's REAL movement this tick, including natural gravity.
            double moved = Math.hypot(current.getX() - previous.getX(), current.getZ() - previous.getZ());
            if (moved > 3.0) { iterator.remove(); continue; } // outside teleport or plugin move
            if (moved > .035) {
                hitSwordDashTargets(user, dash, previous, current);
                double points = Math.max(1, Math.ceil(moved / 0.35));
                for (int j = 1; j <= points; j++) {
                    Location where = previous.clone().add(current.toVector().subtract(previous.toVector())
                            .multiply(j / points));
                    addSwordTrail(dash.userId, where);
                }
                dash.traveled += moved;
            }
            dash.lastPosition = current;
            user.setFallDistance(0);
            dash.age++;
            // Do not repeatedly force the player's direction or speed.
            // Physics handles inertia, gravity and collisions naturally.
            if (dash.traveled >= swordDashBlocks || dash.age >= 18 || !user.isValid()) {
                iterator.remove();
                continue;
            }
            if (ticksPassed % 2 == 0) {
                user.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        current.clone().add(0, .8, 0), 12, .35, .6, .35, .035);
            }
        }
    }

    private void hitSwordDashTargets(Player owner, SwordDash dash, Location from, Location to) {
        Vector displacement = to.toVector().subtract(from.toVector());
        double lengthSquared = displacement.lengthSquared();
        if (lengthSquared < 0.000001) return;
        Location midpoint = from.clone().add(to.toVector().subtract(from.toVector()).multiply(.5));
        for (LivingEntity other : nearbyFrostTargets(midpoint, 2.5, 2.5, owner.getUniqueId())) {
            if (dash.hit.contains(other.getUniqueId())) continue;
            // Horizontal swept capsule with vertical tolerance for the player's body.
            Vector delta = other.getLocation().toVector().subtract(from.toVector());
            double projection = Math.max(0, Math.min(1, delta.dot(displacement) / lengthSquared));
            Vector nearest = from.toVector().add(displacement.clone().multiply(projection));
            Vector targetFeet = other.getLocation().toVector();
            double horizontalSq = Math.pow(nearest.getX() - targetFeet.getX(), 2)
                    + Math.pow(nearest.getZ() - targetFeet.getZ(), 2);
            if (horizontalSq > 1.15 * 1.15
                    || Math.abs(nearest.getY() - targetFeet.getY()) > 1.6) continue;
            dash.hit.add(other.getUniqueId());
            dealTrueDamage(other, swordDamage, owner.getUniqueId());
            other.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    other.getLocation().add(0, 1.0, 0), 30, 0.4, 0.65, 0.4, 0.045);
            other.getWorld().playSound(other.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.1f);
        }
    }

    private void tickSwordTrails() {
        Iterator<IceTrailPoint> iterator = swordTrails.iterator();
        while (iterator.hasNext()) {
            IceTrailPoint trail = iterator.next();
            if (ticksPassed >= trail.expires) { iterator.remove(); continue; }
            if (ticksPassed % 5 == 0) {
                trail.location.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    trail.location.clone().add(0, 0.35, 0), 2, 0.25, 0.1, 0.25, 0.006);
            }
            for (LivingEntity target : nearbyFrostTargets(trail.location, 1.3, 1.6, trail.ownerId)) {
                Location t = target.getLocation();
                double dx = t.getX() - trail.location.getX();
                double dz = t.getZ() - trail.location.getZ();
                if (dx * dx + dz * dz > 0.85 * 0.85
                        || Math.abs(t.getY() - trail.location.getY()) > 1.4) continue;
                PotionEffect existing = target.getPotionEffect(PotionEffectType.SLOWNESS);
                if (existing != null && existing.getAmplifier() > swordSlowLevel - 1) continue;
                if (existing != null && existing.getAmplifier() == swordSlowLevel - 1
                        && existing.getDuration() > swordSlowTicks - 12) continue;
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                        swordSlowTicks, swordSlowLevel - 1, true, true, true));
            }
        }
    }

    // Frozen Battleaxe: visual flying axe, not a dropped item. The original stays in inventory.
    @EventHandler(priority = EventPriority.HIGH)
    public void onBattleaxeUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!isSpecial(event.getItem(), Material.NETHERITE_AXE, battleaxeKey)) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);
        Player user = event.getPlayer();
        if (isFrozen(user)) return;
        UUID ownerId = user.getUniqueId();
        if (ticksPassed < nextBattleaxeUse.getOrDefault(ownerId, 0L)) return;
        nextBattleaxeUse.put(ownerId, ticksPassed + battleaxeCooldownTicks);
        startReadyTimer(user, "battleaxe", battleaxeCooldownTicks);
        throwBattleaxe(user);
    }

    private void throwBattleaxe(Player owner) {
        Vector direction = owner.getEyeLocation().getDirection().normalize();
        Location origin = owner.getEyeLocation().add(direction.clone().multiply(1.1));
        ArmorStand stand = origin.getWorld().spawn(origin, ArmorStand.class, spawned -> {
            spawned.setVisible(false);
            spawned.setGravity(false);
            spawned.setMarker(true);
            spawned.setSmall(true);
            spawned.setArms(true);
            spawned.setInvulnerable(true);
            spawned.setPersistent(false);
            spawned.setSilent(true);
            spawned.getEquipment().setItemInMainHand(makeBattleaxe());
        });
        activeAxes.add(new FlyingAxe(stand, owner.getUniqueId(),
                direction.multiply(battleaxeSpeed), battleaxeRange));
        owner.getWorld().playSound(owner.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.35f);
    }

    private void tickFlyingAxes() {
        Iterator<FlyingAxe> iterator = activeAxes.iterator();
        while (iterator.hasNext()) {
            FlyingAxe flying = iterator.next();
            ArmorStand axe = flying.display;
            Player owner = Bukkit.getPlayer(flying.ownerId);
            if (!axe.isValid() || owner == null || !owner.isOnline()
                    || !owner.getWorld().equals(axe.getWorld()) || flying.traveled >= flying.maxDistance) {
                if (axe.isValid()) axe.remove();
                iterator.remove();
                continue;
            }
            Location from = axe.getLocation();
            Vector displacement = flying.velocity.clone();
            Location to = from.clone().add(displacement);
            double lengthSq = displacement.lengthSquared();
            // Stop at walls, rather than passing through solid blocks.
            if (from.getWorld().rayTraceBlocks(from, displacement.clone(),
                    displacement.length(), FluidCollisionMode.NEVER, true) != null) {
                axe.remove();
                iterator.remove();
                continue;
            }
            LivingEntity struck = null;
            double firstHitFraction = Double.MAX_VALUE;
            Location center = from.clone().add(displacement.clone().multiply(.5));
            for (LivingEntity candidate : nearbyFrostTargets(center,
                    Math.max(2.5, displacement.length() + 1), 2.5, owner.getUniqueId())) {
                Location midpoint = candidate.getLocation().add(0, 0.95, 0);
                Vector toMid = midpoint.toVector().subtract(from.toVector());
                double fraction = Math.max(0, Math.min(1, toMid.dot(displacement) / lengthSq));
                Vector nearest = from.toVector().add(displacement.clone().multiply(fraction));
                if (nearest.distanceSquared(midpoint.toVector()) <= 1.45 * 1.45
                        && fraction < firstHitFraction) {
                    struck = candidate;
                    firstHitFraction = fraction;
                }
            }
            if (struck != null) {
                // Reuse the Wind Charge's server-enforced immobility/interaction prevention.
                showAbilityImpact(struck);
                freezeTarget(struck, battleaxeStunTicks);
                struck.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        struck.getLocation().add(0, 1.0, 0), 35, 0.5, 0.65, 0.5, 0.04);
                struck.getWorld().playSound(struck.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.7f);
                axe.remove();
                iterator.remove();
                continue;
            }
            flying.age++;
            flying.traveled += displacement.length();
            axe.teleport(to);
            // Rotate the visible held axe continuously, like a thrown battleaxe.
            axe.setRightArmPose(new EulerAngle(flying.age * 0.75, 0.0, flying.age * 0.55));
            if (flying.age % 2 == 0) {
                axe.getWorld().spawnParticle(Particle.SNOWFLAKE, to, 3, 0.18, 0.18, 0.18, 0.012);
            }
        }
    }

    // The custom firework is manually spawned; the item is never consumed.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireworkUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!isSpecial(event.getItem(), Material.FIREWORK_ROCKET, fireworksKey)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        event.setCancelled(true);
        Player user = event.getPlayer();
        if (isFrozen(user)) return;
        UUID id = user.getUniqueId();
        long until = nextFireworkUse.getOrDefault(id, 0L);
        if (ticksPassed < until) return;
        Location location;
        if (event.getClickedBlock() != null) {
            location = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0.2, 0.5);
        } else {
            location = user.getLocation().add(user.getLocation().getDirection().multiply(1.5));
            location.add(0, 0.3, 0);
        }
        nextFireworkUse.put(id, ticksPassed + fireworkCooldownTicks);
        startReadyTimer(user, "firework", fireworkCooldownTicks);
        launchBarrage(location, user);
    }

    private Firework spawnDisplayFirework(Location spot, Color color1, Color color2, int life) {
        Firework fw = spot.getWorld().spawn(spot, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.clearEffects();
        meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL)
                .withColor(color1, color2).withFade(Color.WHITE).trail(true).build());
        meta.setPower(2);
        fw.setFireworkMeta(meta);
        fw.setTicksToDetonate(life);
        fw.getPersistentDataContainer().set(fireworksKey, PersistentDataType.BYTE, (byte) 1);
        return fw;
    }

    private void launchBarrage(Location spot, Player owner) {
        // IMPORTANT: acquire candidates around the GROUNDED LAUNCH POINT.
        // Searching after ascending means every target beneath the firework
        // is too far away, even when standing beside the caster.
        UUID ownerId = owner.getUniqueId();
        LivingEntity initialTarget = findNearestEnemy(spot, ownerId, fireworkTargetRange);
        Firework launch = spawnDisplayFirework(spot, Color.AQUA, Color.PURPLE,
                fireworkSkyDelay + 10);
        launch.setVelocity(new Vector(0, 0.95, 0));
        Bukkit.getScheduler().runTaskLater(this, () -> {
            // Even if the display rocket has been removed, the barrage can
            // still launch from a safe height over the original launch point.
            Location high = launch.isValid() ? launch.getLocation().clone()
                    : spot.clone().add(0, Math.min(18.0, fireworkSkyDelay * 0.75), 0);
            if (launch.isValid()) launch.detonate();
            // Retarget if the original nearest target has become invalid.
            LivingEntity nearest = initialTarget;
            if (nearest == null || !isValidFrostTarget(ownerId, nearest)
                    || !nearest.getWorld().equals(spot.getWorld())) {
                nearest = findNearestEnemy(spot, ownerId, fireworkTargetRange);
            }
            if (nearest == null) {
                Player user = Bukkit.getPlayer(ownerId);
                if (user != null) user.sendMessage(ChatColor.GRAY +
                        "Firework barrage: no untrusted target within "
                        + (int) fireworkTargetRange + " blocks of launch.");
                return;
            }
            UUID targetId = nearest.getUniqueId();
            for (int i = 0; i < fireworkCount; i++) {
                int index = i;
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    Entity entity = Bukkit.getEntity(targetId);
                    if (!(entity instanceof LivingEntity enemy)
                            || !isValidFrostTarget(ownerId, enemy)
                            || !enemy.getWorld().equals(high.getWorld())) return;
                    double angle = (Math.PI * 2 * index) / Math.max(1, fireworkCount);
                    Location spawnAt = high.clone().add(Math.cos(angle) * 1.2, 0.25,
                            Math.sin(angle) * 1.2);
                    Firework missile = spawnDisplayFirework(spawnAt, Color.RED, Color.ORANGE, 160);
                    missile.setShotAtAngle(true);
                    activeRockets.add(new HomingRocket(missile, targetId, ownerId, ticksPassed + 145));
                }, i * 4L);
            }
        }, fireworkSkyDelay);
    }

    private LivingEntity findNearestEnemy(Location location, UUID owner, double radius) {
        LivingEntity selected = null;
        double minDist = radius * radius;
        for (LivingEntity candidate : nearbyFrostTargets(location, radius, radius, owner)) {
            double dist = candidate.getLocation().distanceSquared(location);
            if (dist < minDist) { selected = candidate; minDist = dist; }
        }
        return selected;
    }

    private void tickHomingRockets() {
        Iterator<HomingRocket> iter = activeRockets.iterator();
        while (iter.hasNext()) {
            HomingRocket active = iter.next();
            Firework rocket = active.rocket;
            Entity entity = Bukkit.getEntity(active.targetId);
            LivingEntity target = entity instanceof LivingEntity living ? living : null;
            if (!rocket.isValid() || ticksPassed >= active.expiresAt || target == null
                    || !isValidFrostTarget(active.ownerId, target)
                    || !rocket.getWorld().equals(target.getWorld())) {
                if (rocket.isValid()) rocket.detonate();
                iter.remove();
                continue;
            }
            Vector toTarget = target.getEyeLocation().toVector().subtract(rocket.getLocation().toVector());
            if (toTarget.lengthSquared() <= 2.25) {
                // One hit per missile. 1 health point = half a heart by default.
                dealTrueDamage(target, fireworkDamage, active.ownerId);
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,
                        fireworkGlowingTicks, 0, false, true, true));
                rocket.detonate();
                iter.remove();
                continue;
            }
            rocket.setVelocity(toTarget.normalize().multiply(fireworkSpeed));
        }
    }

    // The fireworks apply only our configured damage when they touch a target.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void cancelVanillaFireworkDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework firework
                && firework.getPersistentDataContainer().has(fireworksKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    // Non-griefing molten wave: particles only, no placed lava or fire.
    // 6x6 square centered on the caster by default (3 blocks each axis).
    @EventHandler(priority = EventPriority.HIGH)
    public void onLavaUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!isSpecial(event.getItem(), Material.FLINT_AND_STEEL, lavaKey)) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true); // don't ignite blocks or use up the item
        Player owner = event.getPlayer();
        if (isFrozen(owner)) return;
        UUID ownerId = owner.getUniqueId();
        if (ticksPassed < nextLavaUse.getOrDefault(ownerId, 0L)) return;
        nextLavaUse.put(ownerId, ticksPassed + lavaCooldownTicks);
        startReadyTimer(owner, "lava", lavaCooldownTicks);
        launchLavaTsunami(owner);
    }

    private void launchLavaTsunami(Player owner) {
        UUID ownerId = owner.getUniqueId();
        Location origin = owner.getLocation().clone();
        double range = lavaRadius;
        int duration = lavaWaveTicks;
        double damage = lavaDamage;
        Set<UUID> alreadyHit = new HashSet<>();
        origin.getWorld().playSound(origin, Sound.BLOCK_LAVA_POP, 1.4f, 0.65f);
        for (int tick = 0; tick <= duration; tick += 2) {
            final int elapsed = tick;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                double outer = range * elapsed / duration;
                // Render the front as an expanding square around the caster.
                for (double x = -outer; x <= outer; x += 0.65) {
                    spawnLavaWaveParticle(origin.clone().add(x, 0.3, -outer));
                    spawnLavaWaveParticle(origin.clone().add(x, 0.3, outer));
                }
                for (double z = -outer; z <= outer; z += 0.65) {
                    spawnLavaWaveParticle(origin.clone().add(-outer, 0.3, z));
                    spawnLavaWaveParticle(origin.clone().add(outer, 0.3, z));
                }
                for (LivingEntity target : nearbyFrostTargets(origin, range + 1, 3.5, ownerId)) {
                    if (alreadyHit.contains(target.getUniqueId())) continue;
                    Location point = target.getLocation();
                    double dx = Math.abs(point.getX() - origin.getX());
                    double dz = Math.abs(point.getZ() - origin.getZ());
                    if (dx <= range && dz <= range
                            && Math.abs(point.getY() - origin.getY()) <= 2.5
                            && Math.max(dx, dz) <= outer + 0.55) {
                        alreadyHit.add(target.getUniqueId());
                        dealTrueDamage(target, damage, ownerId);
                        startLavaBurn(target, ownerId);
                        target.getWorld().spawnParticle(Particle.FLAME,
                                target.getLocation().add(0, 1, 0), 20, 0.4, 0.7, 0.4, 0.02);
                    }
                }
            }, tick);
        }
    }

    private void startLavaBurn(LivingEntity target, UUID ownerId) {
        if (lavaFirePulses <= 0 || lavaFireDamage <= 0 || target.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) return;
        UUID id = target.getUniqueId();
        // Retain visual flames; suppress vanilla fire damage while our exact 1 hp pulses occur.
        target.setFireTicks(Math.max(target.getFireTicks(), lavaFirePulses * 20));
        lavaBurnUntil.put(id, ticksPassed + lavaFirePulses * 20L + 2);
        for (int i = 1; i <= lavaFirePulses; i++) {
            final int pulse = i;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Entity entity = Bukkit.getEntity(id);
                if (entity instanceof LivingEntity victim && victim.isValid() && !victim.isDead()
                        && !victim.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
                    dealTrueDamage(victim, lavaFireDamage, ownerId);
                    victim.getWorld().spawnParticle(Particle.FLAME,
                        victim.getLocation().add(0, victim.getHeight()*.5, 0), 8, .25, .3, .25, .02);
                }
                if (pulse == lavaFirePulses) {
                    lavaBurnUntil.remove(id);
                    if (entity instanceof LivingEntity victim) victim.setFireTicks(0);
                }
            }, i * 20L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void preventExtraLavaFireDamage(EntityDamageEvent event) {
        if ((event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE)
                && ticksPassed < lavaBurnUntil.getOrDefault(event.getEntity().getUniqueId(), 0L)) {
            event.setCancelled(true);
        }
    }

    private void spawnLavaWaveParticle(Location place) {
        place.getWorld().spawnParticle(Particle.LAVA, place, 2, 0.1, 0.1, 0.1, 0.01);
        place.getWorld().spawnParticle(Particle.FLAME, place, 3, 0.15, 0.25, 0.15, 0.01);
    }

    // -------------------- Frozen crossbow --------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onCrossbowPrepare(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isGroupItem(event.getItem(), "crossbow")) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player user = event.getPlayer();
        if (!user.isSneaking()) return;
        event.setCancelled(true);
        if (isFrozen(user)) return;
        if (ticksPassed < nextShriekUse.getOrDefault(user.getUniqueId(), 0L)) {
            user.sendMessage(ChatColor.RED + "[Frost] Warden Shriek is cooling down.");
            return;
        }
        preparedShrieks.add(user.getUniqueId());
        user.sendMessage(ChatColor.AQUA + "[Frost] Shriek prepared. Load and fire the crossbow!");
        user.playSound(user.getLocation(), Sound.ENTITY_WARDEN_ANGRY, 0.8f, 1.1f);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLoadFrozenCrossbow(EntityLoadCrossbowEvent event) {
        if (isGroupItem(event.getCrossbow(), "crossbow")) event.setConsumeItem(false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFrozenCrossbowShot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player user) || !isGroupItem(event.getBow(), "crossbow")) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        UUID owner = user.getUniqueId();
        arrow.getPersistentDataContainer().set(icicleBoltKey, PersistentDataType.STRING, owner.toString());
        activeIcicleBolts.add(arrow.getUniqueId());
        arrow.setFireTicks(100);
        arrow.setDamage(0);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        if (preparedShrieks.contains(owner) && ticksPassed >= nextShriekUse.getOrDefault(owner, 0L)) {
            preparedShrieks.remove(owner);
            nextShriekUse.put(owner, ticksPassed + shriekCooldownTicks);
            startReadyTimer(user, "crossbow", shriekCooldownTicks);
            arrow.getPersistentDataContainer().set(shriekBoltKey, PersistentDataType.BYTE, (byte)1);
            // Snapshot the user's aim, so shriek extends from their position on shot.
            Vector heading = user.getEyeLocation().getDirection().normalize();
            Location origin = user.getEyeLocation().clone();
            Bukkit.getScheduler().runTask(this, () -> {
                if (!user.isOnline()) return;
                castWardenShriek(user, origin, heading);
            });
        }
    }

    private void castWardenShriek(Player caster, Location origin, Vector heading) {
        UUID owner = caster.getUniqueId();
        Set<UUID> hit = new HashSet<>();
        caster.getWorld().playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.1f, .95f);
        for (double d = 0; d < shriekRange; d += .65) {
            Location at = origin.clone().add(heading.clone().multiply(d));
            if (at.getBlock().getType().isSolid()) break;
            if (((int)(d * 10)) % 13 < 7)
                caster.getWorld().spawnParticle(Particle.SONIC_BOOM, at, 1, 0, 0, 0, 0);
            for (LivingEntity living : nearbyFrostTargets(at, 1.5, 1.8, owner)) {
                if (hit.add(living.getUniqueId())) dealTrueDamage(living, shriekDamage, owner);
            }
        }
    }

    private void tickIcicleBolts() {
        Iterator<UUID> iterator = activeIcicleBolts.iterator();
        while (iterator.hasNext()) {
            Entity entity = Bukkit.getEntity(iterator.next());
            if (!(entity instanceof Arrow arrow) || !arrow.isValid() || arrow.isInBlock()) {
                iterator.remove();
                continue;
            }
            arrow.getWorld().spawnParticle(Particle.SNOWFLAKE, arrow.getLocation(), 5,
                    .09, .09, .09, .003);
            arrow.getWorld().spawnParticle(Particle.BLOCK, arrow.getLocation(), 2,
                    .08, .08, .08, .02, Material.PACKED_ICE.createBlockData());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onIcicleImpact(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        String ownerText = arrow.getPersistentDataContainer().get(icicleBoltKey, PersistentDataType.STRING);
        if (ownerText == null) return;
        activeIcicleBolts.remove(arrow.getUniqueId());
        UUID owner;
        try { owner = UUID.fromString(ownerText); } catch (IllegalArgumentException e) { arrow.remove(); return; }
        if (event.getHitEntity() instanceof LivingEntity victim && isValidFrostTarget(owner, victim)) {
            victim.setFireTicks(Math.max(victim.getFireTicks(), 100));
            dealTrueDamage(victim, icicleDamage, owner);
            victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0, .9, 0),
                    26, .45, .5, .45, .02);
        }
        arrow.getWorld().spawnParticle(Particle.BLOCK, arrow.getLocation(), 12,
                .2, .2, .2, 0.1, Material.PACKED_ICE.createBlockData());
        arrow.remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventVanillaIcicleHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Arrow arrow
                && arrow.getPersistentDataContainer().has(icicleBoltKey, PersistentDataType.STRING))
            event.setCancelled(true);
    }

    // -------------------- Frozen armor --------------------
    private boolean armorEquipped(Player player, EquipmentSlot slot, String type) {
        ItemStack item = switch (slot) {
            case HEAD -> player.getInventory().getHelmet();
            case CHEST -> player.getInventory().getChestplate();
            case LEGS -> player.getInventory().getLeggings();
            case FEET -> player.getInventory().getBoots();
            default -> null;
        };
        return isGroupItem(item, type);
    }

    private void refreshArmorEffect(Player user, PotionEffectType type, int amplifier) {
        PotionEffect current = user.getPotionEffect(type);
        if (current == null || (current.getAmplifier() <= amplifier && current.getDuration() < 35))
            user.addPotionEffect(new PotionEffect(type, 60, amplifier, true, false, true));
    }

    // The +10% modifier belongs to the PLAYER, not the sword item. It is active only
    // while a tagged Frozen Sword is in the main hand and is removed immediately otherwise.
    private void tickFrozenSwordPlayerSpeed() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            var attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (attribute == null) continue;
            boolean hasModifier = attribute.getModifiers().stream()
                    .anyMatch(modifier -> frozenSwordPlayerSpeedKey.equals(modifier.getKey()));
            boolean holdingSword = isSpecial(player.getInventory().getItemInMainHand(), Material.NETHERITE_SWORD, swordKey);
            if (holdingSword && !hasModifier) {
                attribute.addModifier(new AttributeModifier(frozenSwordPlayerSpeedKey, 0.10,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            } else if (!holdingSword && hasModifier) {
                for (AttributeModifier modifier : new ArrayList<>(attribute.getModifiers()))
                    if (frozenSwordPlayerSpeedKey.equals(modifier.getKey())) attribute.removeModifier(modifier);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrozenSwordMeleeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!isSpecial(attacker.getInventory().getItemInMainHand(), Material.NETHERITE_SWORD, swordKey)) return;
        // Keep vanilla attack-charge scaling, but cap the final pre-armor melee result.
        // This removes the old artificial 6-heart minimum and prevents critical hits
        // from multiplying Frozen Sword damage beyond the configured cap.
        if (event.getDamage() > swordMeleeDamage) event.setDamage(swordMeleeDamage);
    }

    private boolean hasFrostItem(ItemStack item) { return isAnyFrostItem(item); }

    private boolean inventoryContainsFrost(Player player, java.util.function.Predicate<ItemStack> matcher) {
        for (ItemStack item : player.getInventory().getStorageContents()) if (matcher.test(item)) return true;
        for (ItemStack item : player.getInventory().getArmorContents()) if (matcher.test(item)) return true;
        return matcher.test(player.getInventory().getItemInOffHand());
    }

    private boolean matchesOwnerQuery(ItemStack item, String query) {
        String q = query.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return switch (q) {
            case "frozensword", "sword" -> isGroupItem(item, "sword");
            case "armor", "frozenset", "armorset" -> isGroupItem(item, "helmet") || isGroupItem(item, "chestplate") || isGroupItem(item, "leggings") || isGroupItem(item, "boots");
            case "frozenbattleaxe", "battleaxe", "axe" -> isGroupItem(item, "battleaxe");
            case "frozenspear", "spear" -> isGroupItem(item, "spear");
            case "frozenpickaxe", "pickaxe", "fortune", "silk" -> isGroupItem(item, q.equals("silk") ? "pickaxe_silk" : "pickaxe");
            case "frozenbow", "bow" -> isGroupItem(item, "bow");
            case "frozenshield", "shield" -> isGroupItem(item, "shield");
            case "frozencrossbow", "crossbow" -> isGroupItem(item, "crossbow");
            case "frozenhelmet", "helmet" -> isGroupItem(item, "helmet");
            case "frozenchestplate", "chestplate" -> isGroupItem(item, "chestplate");
            case "frozenleggings", "leggings" -> isGroupItem(item, "leggings");
            case "frozenboots", "boots" -> isGroupItem(item, "boots");
            case "frozenrod", "rod", "fishingrod" -> isGroupItem(item, "rod");
            case "frozenhorn", "horn" -> isGroupItem(item, "horn");
            case "frozentrident", "grappletrident", "trident", "grapple" -> isGroupItem(item, "grapple");
            case "depthstrident", "riptidetrident", "riptide" -> isGroupItem(item, "riptide");
            case "frozentotem", "totem" -> isGroupItem(item, "totem");
            default -> false;
        };
    }

    // Keep Protection III on every tagged Frost armor piece. The chestplate's
    // explicit armor/toughness modifiers restore Netherite defense attributes.
    private int normalizeFrozenArmorProtection(Player player) {
        int changedCount = 0;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getStorageContents().length; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (normalizeFrozenArmorItem(item)) {
                inventory.setItem(slot, item);
                changedCount++;
            }
        }
        ItemStack helmet = inventory.getHelmet();
        if (normalizeFrozenArmorItem(helmet)) { inventory.setHelmet(helmet); changedCount++; }
        ItemStack chestplate = inventory.getChestplate();
        if (normalizeFrozenArmorItem(chestplate)) { inventory.setChestplate(chestplate); changedCount++; }
        ItemStack leggings = inventory.getLeggings();
        if (normalizeFrozenArmorItem(leggings)) { inventory.setLeggings(leggings); changedCount++; }
        ItemStack boots = inventory.getBoots();
        if (normalizeFrozenArmorItem(boots)) { inventory.setBoots(boots); changedCount++; }
        if (changedCount > 0) player.updateInventory();
        return changedCount;
    }

    private boolean normalizeFrozenArmorItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        NamespacedKey marker = switch (item.getType()) {
            case NETHERITE_HELMET -> helmetKey;
            case NETHERITE_CHESTPLATE -> chestplateKey;
            case NETHERITE_LEGGINGS -> leggingsKey;
            case NETHERITE_BOOTS -> bootsKey;
            default -> null;
        };
        if (marker == null || !item.getItemMeta().getPersistentDataContainer().has(marker, PersistentDataType.BYTE)) return false;

        boolean changed = refreshFrozenArmorPassiveMetadata(item);
        ItemMeta meta = item.getItemMeta();
        if (meta.getEnchantLevel(Enchantment.PROTECTION) != 3) {
            meta.removeEnchant(Enchantment.PROTECTION);
            meta.addEnchant(Enchantment.PROTECTION, 3, true);
            item.setItemMeta(meta);
            changed = true;
        }
        if (changed) FrostTooltips.apply(this, item);
        return changed;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrostItemContainerClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory clicked = event.getClickedInventory();
        boolean externalInventory = clicked != null && !(clicked instanceof org.bukkit.inventory.PlayerInventory);
        boolean shiftIntoContainer = event.isShiftClick() && event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY
                && event.getClickedInventory() instanceof org.bukkit.inventory.PlayerInventory;
        boolean hotbarFrostSwap = externalInventory && event.getHotbarButton() >= 0
                && hasFrostItem(((Player) event.getWhoClicked()).getInventory().getItem(event.getHotbarButton()));
        if ((externalInventory && (hasFrostItem(event.getCurrentItem()) || hasFrostItem(event.getCursor())))
                || shiftIntoContainer && hasFrostItem(event.getCurrentItem()) || hotbarFrostSwap) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrostItemContainerDrag(InventoryDragEvent event) {
        if (!hasFrostItem(event.getOldCursor())) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrostItemHopperMove(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
        if (hasFrostItem(event.getItem())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrostItemHopperPickup(org.bukkit.event.inventory.InventoryPickupItemEvent event) {
        if (hasFrostItem(event.getItem().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobFrostItemPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player) && hasFrostItem(event.getItem().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrostItemFrameUse(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof org.bukkit.entity.ItemFrame
                && (hasFrostItem(event.getPlayer().getInventory().getItemInMainHand())
                || hasFrostItem(event.getPlayer().getInventory().getItemInOffHand()))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrostArmorStandManipulate(org.bukkit.event.player.PlayerArmorStandManipulateEvent event) {
        if (hasFrostItem(event.getPlayerItem()) || hasFrostItem(event.getArmorStandItem())) event.setCancelled(true);
    }

    private void tickFrozenArmor() {
        for (Player user : Bukkit.getOnlinePlayers()) {
            if (armorEquipped(user, EquipmentSlot.HEAD, "helmet"))
                refreshArmorEffect(user, PotionEffectType.STRENGTH, 0);
            if (leggingsFireResistanceEnabled && armorEquipped(user, EquipmentSlot.LEGS, "leggings"))
                refreshArmorEffect(user, PotionEffectType.FIRE_RESISTANCE, 0);
            if (bootsSpeedEnabled && armorEquipped(user, EquipmentSlot.FEET, "boots"))
                refreshArmorEffect(user, PotionEffectType.SPEED, 1);
        }
    }

    @EventHandler
    public void onHelmetSneak(PlayerToggleSneakEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        helmetSneakSince.remove(id);
        if (!event.isSneaking()) helmetNeedsStandUp.remove(id);
    }

    private void tickHelmetCrouch() {
        for (Player user : Bukkit.getOnlinePlayers()) {
            UUID id = user.getUniqueId();
            if (!user.isSneaking() || !armorEquipped(user, EquipmentSlot.HEAD, "helmet")) {
                helmetSneakSince.remove(id);
                if (!user.isSneaking()) helmetNeedsStandUp.remove(id);
                continue;
            }
            if (helmetNeedsStandUp.contains(id)) continue;
            long since = helmetSneakSince.computeIfAbsent(id, ignored -> ticksPassed);
            if (ticksPassed - since < revealCrouchTicks) continue;
            helmetSneakSince.remove(id);
            helmetNeedsStandUp.add(id);
            if (ticksPassed < nextRevealUse.getOrDefault(id, 0L)) continue;
            nextRevealUse.put(id, ticksPassed + revealCooldownTicks);
            startReadyTimer(user, "helmet", revealCooldownTicks);
            Location location = user.getLocation();
            int count = 0;
            for (Player target : location.getWorld().getPlayers()) {
                if (target.equals(user) || target.getGameMode() == GameMode.SPECTATOR) continue;
                if (target.getLocation().distanceSquared(location) > revealRadius * revealRadius) continue;
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, revealGlowTicks, 0, true, true, true));
                count++;
            }
            user.getWorld().spawnParticle(Particle.SNOWFLAKE, location.clone().add(0, 1.3, 0), 75,
                revealRadius * .5, .6, revealRadius * .5, .03);
            user.playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.1f, .7f);
            user.sendMessage(ChatColor.AQUA + "[Frost] Revealed " + count + " players!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrozenBootsFall(EntityDamageEvent event) {
        if (bootsFallImmunity && event.getEntity() instanceof Player user
                && event.getCause() == EntityDamageEvent.DamageCause.FALL
                && armorEquipped(user, EquipmentSlot.FEET, "boots")) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFishingRodUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isSpecial(event.getItem(), Material.FISHING_ROD, fishingRodKey)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        Location anchor = fishingAnchors.get(id);
        if (anchor == null) {
            // The cursor must actually strike a SOLID block; no air anchors.
            // Ignore liquids but respect hitboxes, including block faces.
            var hit = player.rayTraceBlocks(anchorBreakDistance, FluidCollisionMode.NEVER);
            if (hit == null || hit.getHitBlock() == null || !hit.getHitBlock().getType().isSolid()) {
                player.sendMessage(ChatColor.RED + "[Frost] Aim at a solid block to set your anchor.");
                return;
            }
            Block surface = hit.getHitBlock();
            Location attachment = hit.getHitPosition().toLocation(player.getWorld());
            if (hit.getHitBlockFace() != null)
                attachment.add(hit.getHitBlockFace().getDirection().multiply(.55));
            fishingAnchors.put(id, attachment);
            fishingAnchorBlocks.put(id, surface.getLocation());
            player.sendMessage(ChatColor.AQUA + "[Frost] Anchor attached! Right-click again to grapple.");
            player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1f, 1.2f);
            player.getWorld().spawnParticle(Particle.END_ROD, attachment, 16, .15, .15, .15, .01);
            return;
        }
        Location anchorBlock = fishingAnchorBlocks.get(id);
        if (anchorBlock == null || !anchorBlock.getWorld().equals(player.getWorld())
                || !anchorBlock.getBlock().getType().isSolid()
                || !anchor.getWorld().equals(player.getWorld())
                || anchor.distanceSquared(player.getLocation()) > anchorBreakDistance * anchorBreakDistance) {
            breakFishingAnchor(player, "Your anchor broke. Set a new one.");
            return;
        }
        // The second use CONSUMES the anchor, even when already very close.
        fishingAnchors.remove(id);
        fishingAnchorBlocks.remove(id);
        Vector direction = anchor.toVector().subtract(player.getLocation().toVector());
        double distance = direction.length();
        if (distance < 1.2) {
            player.sendMessage(ChatColor.YELLOW + "[Frost] Anchor released (already close to it). Set a new one.");
            return;
        }
        double strength = Math.min(4.3, anchorPullStrength + distance * .048);
        Vector velocity = direction.normalize().multiply(strength);
        velocity.setY(Math.min(1.40, Math.max(.40, velocity.getY() + .68)));
        player.setFallDistance(0);
        player.setVelocity(velocity);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.2f, 1.2f);
        player.getWorld().spawnParticle(Particle.CLOUD,
                player.getLocation().add(0, 1, 0), 30, .25, .15, .25, .06);
        player.sendMessage(ChatColor.AQUA + "[Frost] Grappled! Anchor consumed; set another one.");
    }

    private void breakFishingAnchor(Player player, String reason) {
        UUID id = player.getUniqueId();
        fishingAnchors.remove(id);
        fishingAnchorBlocks.remove(id);
        player.sendMessage(ChatColor.RED + "[Frost] " + reason);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, .8f, .8f);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnchorDistanceCheck(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location anchor = fishingAnchors.get(player.getUniqueId());
        if (anchor == null) return;
        Location block = fishingAnchorBlocks.get(player.getUniqueId());
        if (block == null || !block.getWorld().equals(player.getWorld())
                || !block.getBlock().getType().isSolid()
                || !anchor.getWorld().equals(player.getWorld())
                || anchor.distanceSquared(player.getLocation()) > anchorBreakDistance * anchorBreakDistance) {
            breakFishingAnchor(player, "Your anchor broke. Set a new one.");
        }
    }

    private Player nearestUntrustedPlayer(Player owner, Location center, double radius) {
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Player target)) continue;
            if (!isValidFrostTarget(owner.getUniqueId(), target)) continue;
            double dist = target.getLocation().distanceSquared(center);
            if (dist < bestDist) {
                bestDist = dist;
                best = target;
            }
        }
        return best;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHornUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isSpecial(event.getItem(), Material.GOAT_HORN, hornKey)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (ticksPassed < nextHornUse.getOrDefault(id, 0L)) {
            player.sendMessage(ChatColor.RED + "[Frost] Frozen Horn is on cooldown.");
            return;
        }
        nextHornUse.put(id, ticksPassed + hornCooldownTicks);
        startReadyTimer(player, "horn", hornCooldownTicks);
        player.playSound(player.getLocation(), MusicInstrument.CALL_GOAT_HORN.getSound(), 1.4f, 0.85f);
        Location base = player.getLocation();
        for (int i = 0; i < hornSkeletonCount; i++) {
            double angle = (Math.PI * 2.0 * i) / Math.max(1, hornSkeletonCount);
            double radius = Math.max(2.0, hornSpawnRadius - (i % 2));
            Location spawn = base.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            Block floor = spawn.getWorld().getHighestBlockAt(spawn);
            spawn = floor.getLocation().add(0.5, 1.0, 0.5);
            Stray stray = spawn.getWorld().spawn(spawn, Stray.class);
            stray.customName(Component.text("Frozen Horde Stray"));
            stray.setCustomNameVisible(false);
            stray.getPersistentDataContainer().set(hornOwnerKey, PersistentDataType.STRING, id.toString());
            stray.setCanPickupItems(false);
            boolean archer = ThreadLocalRandom.current().nextDouble() < hornArcherChance;
            if (archer) {
                ItemStack bow = new ItemStack(Material.BOW);
                ItemMeta bowMeta = bow.getItemMeta();
                bowMeta.addEnchant(Enchantment.POWER, 2, true);
                bow.setItemMeta(bowMeta);
                stray.getEquipment().setItemInMainHand(bow);
                stray.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                stray.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                stray.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                stray.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
            } else {
                ItemStack sword = new ItemStack(Material.IRON_SWORD);
                ItemMeta sm = sword.getItemMeta();
                sm.addEnchant(Enchantment.SHARPNESS, 3, true);
                sword.setItemMeta(sm);
                stray.getEquipment().setItemInMainHand(sword);
                ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
                ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
                ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                for (ItemStack armor : List.of(helmet, chest, legs, boots)) {
                    ItemMeta armorMeta = armor.getItemMeta();
                    armorMeta.addEnchant(Enchantment.PROTECTION, 1, true);
                    armor.setItemMeta(armorMeta);
                }
                stray.getEquipment().setHelmet(helmet);
                stray.getEquipment().setChestplate(chest);
                stray.getEquipment().setLeggings(legs);
                stray.getEquipment().setBoots(boots);
            }
            stray.setShouldBurnInDay(false);
            stray.getEquipment().setItemInMainHandDropChance(0.0f);
            stray.getEquipment().setHelmetDropChance(0.0f);
            stray.getEquipment().setChestplateDropChance(0.0f);
            stray.getEquipment().setLeggingsDropChance(0.0f);
            stray.getEquipment().setBootsDropChance(0.0f);
            stray.setRemoveWhenFarAway(true);
            stray.getWorld().spawnParticle(Particle.BLOCK, spawn, 30, 0.35, 0.6, 0.35, 0.02, Material.PACKED_ICE.createBlockData());
            Player target = nearestUntrustedPlayer(player, spawn, 32);
            if (target != null) stray.setTarget(target);
            Bukkit.getScheduler().runTaskLater(this, () -> { if (stray.isValid()) stray.remove(); }, hornDespawnTicks);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHornMinionTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Stray stray)) return;
        String ownerRaw = stray.getPersistentDataContainer().get(hornOwnerKey, PersistentDataType.STRING);
        if (ownerRaw == null) return;
        UUID ownerId;
        try { ownerId = UUID.fromString(ownerRaw); } catch (IllegalArgumentException ex) { return; }
        if (!(event.getTarget() instanceof Player target) || !isValidFrostTarget(ownerId, target)) {
            event.setCancelled(true);
            Player owner = Bukkit.getPlayer(ownerId);
            if (owner != null) {
                Player replacement = nearestUntrustedPlayer(owner, stray.getLocation(), 32);
                if (replacement != null) stray.setTarget(replacement);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectFromHornMinions(EntityDamageByEntityEvent event) {
        // Also protect friends if a summoned Stray somehow fires a projectile.
        Stray stray = null;
        if (event.getDamager() instanceof Stray direct) stray = direct;
        else if (event.getDamager() instanceof Projectile shot && shot.getShooter() instanceof Stray shooter)
            stray = shooter;
        if (stray == null) return;
        String ownerRaw = stray.getPersistentDataContainer().get(hornOwnerKey, PersistentDataType.STRING);
        if (ownerRaw == null) return;
        UUID ownerId;
        try { ownerId = UUID.fromString(ownerRaw); } catch (IllegalArgumentException ex) { return; }
        if (!(event.getEntity() instanceof Player victim) || !isValidFrostTarget(ownerId, victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceTnt(PlayerInteractEvent event) {
        // Only process the main hand; prevents the single click being handled twice.
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!isSpecial(event.getItem(), Material.TNT, tntKey)) return;

        // Prevent vanilla placement AND consumption. It remains in inventory.
        event.setCancelled(true);
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        if (now < nextTntUse.getOrDefault(player.getUniqueId(), 0L)) return;

        Block placeBlock = event.getClickedBlock().getRelative(event.getBlockFace());
        if (!placeBlock.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No space to place Infinite TNT here.");
            return;
        }

        Location spot = placeBlock.getLocation().add(0.5, 0, 0.5);
        TNTPrimed primed = spot.getWorld().spawn(spot, TNTPrimed.class);
        primed.setSource(player);
        primed.setFuseTicks(tntFuseTicks);
        primed.setYield(tntPower); // Actual explosion size; 4.0 = vanilla TNT.
        primed.setIsIncendiary(false);
        primed.getPersistentDataContainer().set(tntKey, PersistentDataType.BYTE, (byte) 1);
        primed.getPersistentDataContainer().set(tntOwnerKey, PersistentDataType.STRING,
                player.getUniqueId().toString());

        nextTntUse.put(player.getUniqueId(), now + (tntCooldownTicks * 50L));
        startReadyTimer(player, "tnt", tntCooldownTicks);
    }

    // Block damage: a real TNT explosion, limited to the selected radius.
    // Apply separately controlled player damage on the next tick.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed primed)) return;
        if (!primed.getPersistentDataContainer().has(tntKey, PersistentDataType.BYTE)) return;

        Location center = event.getLocation().clone();
        if (!tntBreakBlocks) {
            event.blockList().clear();
        } else {
            event.blockList().removeIf(block ->
                    block.getLocation().add(0.5, 0.5, 0.5).distanceSquared(center)
                            > tntRadius * tntRadius);
        }

        String ownerId = primed.getPersistentDataContainer()
                .get(tntOwnerKey, PersistentDataType.STRING);
        UUID ownerUuid = null;
        if (ownerId != null) {
            try { ownerUuid = UUID.fromString(ownerId); }
            catch (IllegalArgumentException ignored) { }
        }
        double effectRadius = tntRadius;
        double effectDamage = tntDamage;

        // Identify the victims NOW, before vanilla explosion knockback moves them away.
        List<UUID> victims = new ArrayList<>();
        for (LivingEntity living : nearbyFrostTargets(center, effectRadius, effectRadius, ownerUuid)) {
            Location middle = living.getLocation().add(0, living.getHeight() / 2.0, 0);
            if (middle.distanceSquared(center) <= effectRadius * effectRadius)
                victims.add(living.getUniqueId());
        }
        UUID finalOwnerUuid = ownerUuid;
        Bukkit.getScheduler().runTask(this, () -> {
            for (UUID victimId : victims) {
                Entity entity = Bukkit.getEntity(victimId);
                if (entity instanceof LivingEntity victim && isValidFrostTarget(finalOwnerUuid, victim))
                    dealTrueDamage(victim, effectDamage, finalOwnerUuid);
            }
        });
    }

    // Prevent vanilla TNT's larger, armor-dependent damage. Custom damage is
    // handled in onExplosion, for a consistent setting in config.yml.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVanillaExplosionDamage(EntityDamageEvent event) {
        Entity direct = event.getDamageSource().getDirectEntity();
        if (direct instanceof TNTPrimed primed
                && primed.getPersistentDataContainer().has(tntKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            return;
        }
        if (event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof TNTPrimed primed
                && primed.getPersistentDataContainer().has(tntKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    private OfflinePlayer lookupKnownPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        // Uses the server's known player data. Don't assume nonexistent names are valid.
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline : null;
    }

    private boolean manageTrust(CommandSender sender, String action, String[] args) {
        if (!(sender instanceof Player owner)) {
            sender.sendMessage(ChatColor.RED + "Trust commands must be used in-game.");
            return true;
        }
        if (!owner.hasPermission("frost.trust")) {
            owner.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        UUID uuid = owner.getUniqueId();
        if (action.equals("trusted")) {
            Map<UUID, String> names = trustedPlayers.getOrDefault(uuid, Map.of());
            owner.sendMessage(ChatColor.AQUA + "Trusted players (" + names.size() + "): "
                    + ChatColor.WHITE + (names.isEmpty() ? "None" : String.join(", ", names.values())));
            return true;
        }
        if (args.length != 2) {
            owner.sendMessage(ChatColor.YELLOW + "Usage: /frost " + action + " <player>");
            return true;
        }
        OfflinePlayer target = lookupKnownPlayer(args[1]);
        if (target == null) {
            owner.sendMessage(ChatColor.RED + "Player not found in server history. They need to join once.");
            return true;
        }
        if (target.getUniqueId().equals(uuid)) {
            owner.sendMessage(ChatColor.RED + "You don't need to trust yourself.");
            return true;
        }
        Map<UUID, String> names = trustedPlayers.computeIfAbsent(uuid, ignored -> new HashMap<>());
        String displayName = target.getName() == null ? args[1] : target.getName();
        if (action.equals("trust")) {
            names.put(target.getUniqueId(), displayName);
            owner.sendMessage(ChatColor.GREEN + "Trusted " + displayName
                    + ". Your Frost weapons won't harm or target them.");
        } else {
            if (names.remove(target.getUniqueId()) == null) {
                owner.sendMessage(ChatColor.YELLOW + displayName + " was not on your trust list.");
                return true;
            }
            if (names.isEmpty()) trustedPlayers.remove(uuid);
            owner.sendMessage(ChatColor.YELLOW + "Untrusted " + displayName + ".");
        }
        persistTrustedPlayers();
        return true;
    }

    private void resetWeaponCooldowns(Player player) {
        UUID id = player.getUniqueId();
        nextSpearUse.remove(id); nextTotemUse.remove(id); nextSwordUse.remove(id);
        nextShieldUse.remove(id); shieldRaisedAt.remove(id); shieldStunnedUntil.remove(id); shieldActiveUntil.remove(id); nextBattleaxeUse.remove(id); nextWindUse.remove(id);
        nextTridentUse.remove(id); nextLavaUse.remove(id); nextRiptideAbility.remove(id);
        nextFireworkUse.remove(id); nextBowUse.remove(id); nextTntUse.remove(id);
        nextShriekUse.remove(id); preparedShrieks.remove(id); nextRevealUse.remove(id); snowballNext.remove(id);
        nextHornUse.remove(id); fishingAnchors.remove(id); fishingAnchorBlocks.remove(id);
        helmetSneakSince.remove(id); helmetNeedsStandUp.remove(id);
        readyAt.remove(id);
        preparedBows.remove(id);
        activeDashes.remove(id);
        // Clear vanilla item cooldowns and material-based restrictions as well.
        for (Material material : List.of(Material.SNOWBALL, Material.TNT, Material.WIND_CHARGE,
                Material.TRIDENT, Material.FIREWORK_ROCKET, Material.FLINT_AND_STEEL,
                Material.NETHERITE_AXE, Material.NETHERITE_SWORD, Material.NETHERITE_SPEAR,
                Material.TOTEM_OF_UNDYING, Material.SHIELD, Material.BOW, Material.CROSSBOW,
                Material.FISHING_ROD, Material.GOAT_HORN)) {
            player.setCooldown(material, 0);
        }
        // Cooldowns set on particular item stacks need explicit clearing too.
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            for (String group : List.of("snowball", "tnt", "windcharge", "grapple", "riptide",
                    "firework", "lava", "battleaxe", "shield", "sword", "spear", "totem", "bow", "crossbow", "rod", "horn")) {
                if (isGroupItem(item, group)) {
                    player.setCooldown(item, 0);
                    break;
                }
            }
        }
        // TNT's clock is millisecond-based; removing the map clears it too.
    }

    // Border-safe duels: invitations expire, arenas use safe surface blocks,
    // player positions are checked against the current world border before teleport.
    private boolean handleDuelCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use duel commands.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /frost duel <player> | /frost duel accept | /frost duel <player> accept");
            return true;
        }
        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length == 1) return acceptSinglePendingDuel(player);
            if (args.length == 2) {
                Player challenger = Bukkit.getPlayerExact(args[1]);
                if (challenger == null) player.sendMessage(ChatColor.RED + "That challenger is not online.");
                else return acceptDuelRequest(player, challenger);
                return true;
            }
            player.sendMessage(ChatColor.YELLOW + "Usage: /frost duel accept [challenger]");
            return true;
        }
        Player other = Bukkit.getPlayerExact(args[0]);
        if (other == null) {
            player.sendMessage(ChatColor.RED + "That player is not online.");
            return true;
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("accept")) return acceptDuelRequest(player, other);
        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /frost duel <player> | /frost duel <player> accept");
            return true;
        }
        return sendDuelRequest(player, other);
    }

    private boolean sendDuelRequest(Player challenger, Player target) {
        if (challenger.equals(target)) {
            challenger.sendMessage(ChatColor.RED + "You cannot duel yourself.");
            return true;
        }
        if (activeDuelPartners.containsKey(challenger.getUniqueId())
                || activeDuelPartners.containsKey(target.getUniqueId())) {
            challenger.sendMessage(ChatColor.RED + "One of you is already in a duel.");
            return true;
        }
        if (isFrozen(challenger) || isFrozen(target)) {
            challenger.sendMessage(ChatColor.RED + "Neither player can be frozen when a duel begins.");
            return true;
        }
        UUID challengerId = challenger.getUniqueId(), targetId = target.getUniqueId();
        pendingDuels.computeIfAbsent(targetId, ignored -> new LinkedHashMap<>())
                .put(challengerId, ticksPassed + duelRequestTimeoutTicks);
        challenger.sendMessage(ChatColor.AQUA + "[Frost] Duel request sent to " + target.getName() + ".");
        target.sendMessage(ChatColor.GOLD + "[Frost] " + challenger.getName() + " challenged you to a duel!");
        target.sendMessage(ChatColor.YELLOW + "Accept with /frost duel " + challenger.getName() + " accept");
        Map<UUID, Long> requests = getValidDuelRequests(targetId);
        if (requests != null && requests.size() == 1)
            target.sendMessage(ChatColor.YELLOW + "Or type /frost duel accept.");
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.4f);
        return true;
    }

    private Map<UUID, Long> getValidDuelRequests(UUID targetId) {
        Map<UUID, Long> requests = pendingDuels.get(targetId);
        if (requests == null) return null;
        requests.entrySet().removeIf(entry -> entry.getValue() < ticksPassed || Bukkit.getPlayer(entry.getKey()) == null);
        if (requests.isEmpty()) {
            pendingDuels.remove(targetId);
            return null;
        }
        return requests;
    }

    private boolean acceptSinglePendingDuel(Player accepter) {
        Map<UUID, Long> requests = getValidDuelRequests(accepter.getUniqueId());
        if (requests == null) {
            accepter.sendMessage(ChatColor.RED + "You don't have any pending duel requests.");
            return true;
        }
        if (requests.size() != 1) {
            accepter.sendMessage(ChatColor.YELLOW + "You have multiple requests. Use /frost duel <player> accept.");
            return true;
        }
        Player challenger = Bukkit.getPlayer(requests.keySet().iterator().next());
        if (challenger == null) {
            accepter.sendMessage(ChatColor.RED + "That duel request has expired.");
            return true;
        }
        return acceptDuelRequest(accepter, challenger);
    }

    private boolean acceptDuelRequest(Player accepter, Player challenger) {
        if (accepter.equals(challenger)) {
            accepter.sendMessage(ChatColor.RED + "You cannot accept your own duel request.");
            return true;
        }
        Map<UUID, Long> requests = getValidDuelRequests(accepter.getUniqueId());
        if (requests == null || !requests.containsKey(challenger.getUniqueId())) {
            accepter.sendMessage(ChatColor.RED + "You don't have a pending request from " + challenger.getName() + ".");
            return true;
        }
        if (activeDuelPartners.containsKey(accepter.getUniqueId())
                || activeDuelPartners.containsKey(challenger.getUniqueId())) {
            accepter.sendMessage(ChatColor.RED + "One of you is already in a duel.");
            return true;
        }
        if (isFrozen(accepter) || isFrozen(challenger)) {
            accepter.sendMessage(ChatColor.RED + "A duel cannot start while either player is frozen.");
            return true;
        }
        return beginDuel(challenger, accepter);
    }

    private boolean beginDuel(Player firstPlayer, Player secondPlayer) {
        World arenaWorld = Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
                .findFirst().orElse(firstPlayer.getWorld());
        DuelSpots spots = findDuelSpots(arenaWorld);
        if (spots == null) {
            firstPlayer.sendMessage(ChatColor.RED + "[Frost] Couldn't find two safe duel spots inside the world border. No teleport was performed.");
            secondPlayer.sendMessage(ChatColor.RED + "[Frost] Couldn't find a safe duel arena inside the world border. No teleport was performed.");
            return true;
        }
        Location oldFirst = firstPlayer.getLocation().clone(), oldSecond = secondPlayer.getLocation().clone();
        // Refuse the duel if either player is already outside their current border;
        // that guarantees rollback never teleports someone to an out-of-border spot.
        if (!isInsideBorder(oldFirst, 1.0) || !isInsideBorder(oldSecond, 1.0)
                || !isInsideBorder(spots.first(), 1.0) || !isInsideBorder(spots.second(), 1.0)) {
            firstPlayer.sendMessage(ChatColor.RED + "[Frost] A player or arena location is outside its world border. Duel cancelled.");
            secondPlayer.sendMessage(ChatColor.RED + "[Frost] A player or arena location is outside its world border. Duel cancelled.");
            return true;
        }
        boolean firstMoved = firstPlayer.teleport(spots.first());
        boolean secondMoved = firstMoved && secondPlayer.teleport(spots.second());
        boolean bothStillSafe = firstPlayer.getWorld().equals(spots.first().getWorld())
                && secondPlayer.getWorld().equals(spots.second().getWorld())
                && isInsideBorder(firstPlayer.getLocation(), 1.0)
                && isInsideBorder(secondPlayer.getLocation(), 1.0);
        if (!firstMoved || !secondMoved || !bothStillSafe) {
            // Original positions were checked above, so these rollback teleports
            // are also guaranteed to remain inside their respective borders.
            if (firstMoved) firstPlayer.teleport(oldFirst);
            if (secondMoved) secondPlayer.teleport(oldSecond);
            firstPlayer.sendMessage(ChatColor.RED + "[Frost] Duel teleport was cancelled; no player was sent outside a world border.");
            secondPlayer.sendMessage(ChatColor.RED + "[Frost] Duel teleport was cancelled; no player was sent outside a world border.");
            return true;
        }
        UUID firstId = firstPlayer.getUniqueId(), secondId = secondPlayer.getUniqueId();
        clearPendingDuelsFor(firstId);
        clearPendingDuelsFor(secondId);
        activeDuelPartners.put(firstId, secondId);
        activeDuelPartners.put(secondId, firstId);
        duelCountdownPlayers.add(firstId);
        duelCountdownPlayers.add(secondId);
        duelCountdownAnchors.put(firstId, firstPlayer.getLocation().clone());
        duelCountdownAnchors.put(secondId, secondPlayer.getLocation().clone());
        firstPlayer.setFallDistance(0);
        secondPlayer.setFallDistance(0);
        String spacing = String.format(Locale.US, "%.0f", duelSeparationBlocks);
        firstPlayer.sendMessage(ChatColor.AQUA + "[Frost] Duel accepted! You are " + spacing + " blocks from your opponent.");
        secondPlayer.sendMessage(ChatColor.AQUA + "[Frost] Duel accepted! You are " + spacing + " blocks from your opponent.");
        runDuelCountdown(firstId, secondId, duelCountdownSeconds);
        return true;
    }

    private DuelSpots findDuelSpots(World world) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double range = border.getSize() / 2.0 - (duelSeparationBlocks / 2.0 + 3.0);
        if (range <= 0.0) return null;
        // Search only already-loaded chunks. A duel request must not generate
        // hundreds of new chunks just to find an arena.
        Chunk[] loaded = world.getLoadedChunks();
        if (loaded.length == 0) return null;
        List<Chunk> candidates = Arrays.stream(loaded)
                .filter(chunk -> Math.abs((chunk.getX() * 16.0 + 8.0) - center.getX()) <= range + 16.0
                        && Math.abs((chunk.getZ() * 16.0 + 8.0) - center.getZ()) <= range + 16.0)
                .toList();
        if (candidates.isEmpty()) return null;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double half = duelSeparationBlocks / 2.0;
        for (int i = 0; i < duelArenaSearchAttempts; i++) {
            Chunk sample = candidates.get(random.nextInt(candidates.size()));
            double cx = sample.getX() * 16.0 + random.nextDouble(1.0, 15.0);
            double cz = sample.getZ() * 16.0 + random.nextDouble(1.0, 15.0);
            double angle = random.nextDouble(0.0, Math.PI * 2.0);
            double dx = Math.cos(angle) * half, dz = Math.sin(angle) * half;
            Location one = findSafeSurfaceLocation(world, cx - dx, cz - dz);
            Location two = findSafeSurfaceLocation(world, cx + dx, cz + dz);
            if (one == null || two == null || Math.abs(one.getY() - two.getY()) > 3.0) continue;
            if (!isInsideBorder(one, 1.0) || !isInsideBorder(two, 1.0)) continue;
            double distance = one.distance(two);
            if (Math.abs(distance - duelSeparationBlocks) > 0.5) continue;
            one.setDirection(two.toVector().subtract(one.toVector()));
            two.setDirection(one.toVector().subtract(two.toVector()));
            return new DuelSpots(one, two);
        }
        return null;
    }

    private Location findSafeSurfaceLocation(World world, double x, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        if (!world.isChunkLoaded(bx >> 4, bz >> 4)) return null;
        int y = world.getHighestBlockYAt(bx, bz, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
        if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 1) return null;
        // Preserve the exact sampled X/Z coordinates, so a flat arena gives
        // exactly the configured horizontal separation (20 blocks by default).
        Location loc = new Location(world, x, y, z);
        return isSafeDuelLocation(loc) ? loc : null;
    }

    private boolean isSafeDuelLocation(Location loc) {
        if (loc.getWorld() == null || !isInsideBorder(loc, 1.0)) return false;
        World world = loc.getWorld();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 1) return false;
        Block floor = world.getBlockAt(x, y - 1, z);
        Block feet = world.getBlockAt(x, y, z), head = world.getBlockAt(x, y + 1, z);
        Material ground = floor.getType();
        if (!ground.isSolid() || floor.isLiquid() || isUnsafeDuelGround(ground)) return false;
        return feet.isPassable() && head.isPassable() && !feet.isLiquid() && !head.isLiquid();
    }

    private boolean isUnsafeDuelGround(Material type) {
        return switch (type) {
            case MAGMA_BLOCK, CACTUS, CAMPFIRE, SOUL_CAMPFIRE, FIRE, SOUL_FIRE,
                    LAVA, WATER, POWDER_SNOW, SWEET_BERRY_BUSH, WITHER_ROSE,
                    POINTED_DRIPSTONE, COBWEB -> true;
            default -> false;
        };
    }

    private boolean isInsideBorder(Location loc, double margin) {
        if (loc.getWorld() == null) return false;
        WorldBorder border = loc.getWorld().getWorldBorder();
        Location center = border.getCenter();
        double half = border.getSize() / 2.0 - margin;
        return half > 0.0 && Math.abs(loc.getX() - center.getX()) <= half
                && Math.abs(loc.getZ() - center.getZ()) <= half;
    }

    private void runDuelCountdown(UUID oneId, UUID twoId, int seconds) {
        Player one = Bukkit.getPlayer(oneId), two = Bukkit.getPlayer(twoId);
        if (one == null || two == null || !one.isOnline() || !two.isOnline()
                || !twoId.equals(activeDuelPartners.get(oneId))) {
            endDuelFor(oneId, "The duel was cancelled because a player left.");
            return;
        }
        if (seconds <= 0) {
            duelCountdownPlayers.remove(oneId); duelCountdownPlayers.remove(twoId);
            duelCountdownAnchors.remove(oneId); duelCountdownAnchors.remove(twoId);
            one.sendTitle(ChatColor.AQUA + "0", ChatColor.GREEN + "FIGHT!  Good luck", 0, 35, 5);
            two.sendTitle(ChatColor.AQUA + "0", ChatColor.GREEN + "FIGHT!  Good luck", 0, 35, 5);
            one.playSound(one.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            two.playSound(two.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            one.sendMessage(ChatColor.GREEN + "[Frost] Duel started! Only your opponent can damage you in PvP.");
            two.sendMessage(ChatColor.GREEN + "[Frost] Duel started! Only your opponent can damage you in PvP.");
            return;
        }
        one.setVelocity(new Vector()); two.setVelocity(new Vector());
        one.setFallDistance(0); two.setFallDistance(0);
        one.sendTitle(ChatColor.AQUA.toString() + seconds, ChatColor.GRAY + "Frost Duel", 0, 20, 0);
        two.sendTitle(ChatColor.AQUA.toString() + seconds, ChatColor.GRAY + "Frost Duel", 0, 20, 0);
        one.playSound(one.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.0f + (3 - Math.min(3, seconds)) * 0.15f);
        two.playSound(two.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.0f + (3 - Math.min(3, seconds)) * 0.15f);
        Bukkit.getScheduler().runTaskLater(this, () -> runDuelCountdown(oneId, twoId, seconds - 1), 20L);
    }

    private void clearPendingDuelsFor(UUID id) {
        pendingDuels.remove(id);
        for (Map<UUID, Long> requests : pendingDuels.values()) requests.remove(id);
        pendingDuels.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private void endDuelFor(UUID id, String reason) {
        UUID partnerId = activeDuelPartners.remove(id);
        activeDuelPartners.remove(partnerId);
        duelCountdownPlayers.remove(id); duelCountdownAnchors.remove(id);
        if (partnerId != null) {
            duelCountdownPlayers.remove(partnerId); duelCountdownAnchors.remove(partnerId);
            Player partner = Bukkit.getPlayer(partnerId);
            if (partner != null && partner.isOnline()) partner.sendMessage(ChatColor.YELLOW + "[Frost] " + reason);
        }
        clearPendingDuelsFor(id);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void preventDuelInterference(EntityDamageByEntityEvent event) {
        Player victim = event.getEntity() instanceof Player p ? p : null;
        Player attacker = null;
        if (event.getDamager() instanceof Player p) attacker = p;
        else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) attacker = p;
        if (victim == null && attacker == null) return;
        if ((victim != null && duelCountdownPlayers.contains(victim.getUniqueId()))
                || (attacker != null && duelCountdownPlayers.contains(attacker.getUniqueId()))) {
            event.setCancelled(true); return;
        }
        if (attacker != null && activeDuelPartners.containsKey(attacker.getUniqueId())) {
            UUID expected = activeDuelPartners.get(attacker.getUniqueId());
            if (victim == null || !expected.equals(victim.getUniqueId())) {
                event.setCancelled(true); return;
            }
        }
        if (victim != null && activeDuelPartners.containsKey(victim.getUniqueId())) {
            UUID expected = activeDuelPartners.get(victim.getUniqueId());
            if (attacker == null || !expected.equals(attacker.getUniqueId())) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void preventDuelCountdownDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p && duelCountdownPlayers.contains(p.getUniqueId()))
            event.setCancelled(true);
    }

    private Player combatAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void tagCombat(EntityDamageByEntityEvent event) {
        if (!getConfig().getBoolean("combat.enabled", true)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = combatAttacker(event);
        if (attacker == null || attacker.equals(victim) || attacker.getWorld() != victim.getWorld()) return;
        int seconds = Math.max(5, getConfig().getInt("combat.tag-seconds", 15));
        long expiry = System.currentTimeMillis() + seconds * 1000L;
        combatOpponents.put(victim.getUniqueId(), attacker.getUniqueId());
        combatOpponents.put(attacker.getUniqueId(), victim.getUniqueId());
        combatExpiresAt.put(victim.getUniqueId(), expiry);
        combatExpiresAt.put(attacker.getUniqueId(), expiry);
        updateCombatBar(victim, attacker, seconds);
        updateCombatBar(attacker, victim, seconds);
    }

    private void updateCombatBar(Player player, Player opponent, int seconds) {
        BossBar bar = combatBars.computeIfAbsent(player.getUniqueId(), id -> Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID));
        if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
        bar.setTitle(ChatColor.RED + "IN COMBAT " + ChatColor.GRAY + "with " + ChatColor.WHITE + opponent.getName()
                + ChatColor.GRAY + " | " + ChatColor.RED + seconds + "s");
        bar.setProgress(1.0);
    }

    private void clearCombat(UUID id) {
        combatOpponents.remove(id);
        combatExpiresAt.remove(id);
        BossBar bar = combatBars.remove(id);
        if (bar != null) bar.removeAll();
    }

    @EventHandler
    public void onCombatDeath(PlayerDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        UUID opponent = combatOpponents.get(id);
        clearCombat(id);
        if (opponent != null) clearCombat(opponent);
    }

    private void tickCombat() {
        long now = System.currentTimeMillis();
        for (UUID id : new HashSet<>(combatExpiresAt.keySet())) {
            long expiry = combatExpiresAt.getOrDefault(id, 0L);
            Player player = Bukkit.getPlayer(id);
            UUID opponentId = combatOpponents.get(id);
            Player opponent = opponentId == null ? null : Bukkit.getPlayer(opponentId);
            if (now >= expiry || player == null || !player.isOnline() || opponent == null || !opponent.isOnline()) {
                clearCombat(id);
                if (opponentId != null && !combatExpiresAt.containsKey(opponentId)) clearCombat(opponentId);
                continue;
            }
            int left = (int) Math.ceil((expiry - now) / 1000.0);
            BossBar bar = combatBars.get(id);
            if (bar != null) {
                bar.setTitle(ChatColor.RED + "IN COMBAT " + ChatColor.GRAY + "with " + ChatColor.WHITE + opponent.getName()
                        + ChatColor.GRAY + " | " + ChatColor.RED + left + "s");
                int total = Math.max(5, getConfig().getInt("combat.tag-seconds", 15));
                bar.setProgress(Math.max(0.0, Math.min(1.0, (expiry - now) / (total * 1000.0))));
            }
        }
    }

    @EventHandler
    public void endDuelOnDeath(PlayerDeathEvent event) {
        Player eliminated = event.getEntity();
        UUID partnerId = activeDuelPartners.get(eliminated.getUniqueId());
        if (partnerId == null) return;
        endDuelFor(eliminated.getUniqueId(), "Your opponent was eliminated. Duel complete!");
    }

    private boolean applyConfiguredCooldown(Player player, Material material, String path) {
        int seconds = Math.max(0, getConfig().getInt(path, 0));
        if (seconds <= 0) return false;
        int ticks = seconds * 20;
        if (player.hasCooldown(material)) return true;
        player.setCooldown(material, ticks);
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void enforceWindChargeCooldown(PlayerLaunchProjectileEvent event) {
        Player player = event.getPlayer();
        Material cooldownMaterial;
        String configPath;
        String label;
        if (event.getProjectile() instanceof org.bukkit.entity.WindCharge) {
            if (isSpecial(event.getItemStack(), Material.WIND_CHARGE, freezeChargeKey)) return;
            cooldownMaterial = Material.WIND_CHARGE;
            configPath = "cooldowns.windcharge-seconds";
            label = "Wind Charge";
        } else if (event.getProjectile() instanceof org.bukkit.entity.EnderPearl) {
            cooldownMaterial = Material.ENDER_PEARL;
            configPath = "cooldowns.ender-pearl-seconds";
            label = "Ender Pearl";
        } else return;
        if (player.hasCooldown(cooldownMaterial)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text(label + " cooldown: " + (player.getCooldown(cooldownMaterial) / 20) + "s", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }
        int seconds = Math.max(0, getConfig().getInt(configPath, 0));
        if (seconds > 0) player.setCooldown(cooldownMaterial, seconds * 20);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void enforceLungeCooldown(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getEnchantLevel(Enchantment.LUNGE) <= 0) return;
        Player player = event.getPlayer();
        Material type = item.getType();
        if (player.hasCooldown(type)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("Lunge cooldown: " + (player.getCooldown(type) / 20) + "s", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }
        int seconds = Math.max(0, getConfig().getInt("cooldowns.lunge-seconds", 8));
        if (seconds > 0) Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) player.setCooldown(type, seconds * 20);
        });
    }

    // Non-operators may send one chat message every configured interval.
    // AsyncChatEvent runs off-thread, so the cooldown map is concurrent and
    // feedback is scheduled back onto the server thread.
    private void refreshChatCooldown() {
        double seconds = Math.max(0.0, getConfig().getDouble("chat.cooldown-seconds", 1.5));
        chatCooldownNanos = (long) (seconds * 1_000_000_000L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void enforcePlayerChatCooldown(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (getConfig().getBoolean("chat.anti-repeat-enabled", true) && !plain.isEmpty()) {
            long nowMillis = System.currentTimeMillis();
            long window = Math.max(1, getConfig().getLong("chat.anti-repeat-window-seconds", 30)) * 1000L;
            String previous = lastPlayerChatMessage.get(player.getUniqueId());
            long previousAt = lastPlayerChatMessageAt.getOrDefault(player.getUniqueId(), 0L);
            if (previous != null && previous.equalsIgnoreCase(plain) && nowMillis - previousAt <= window) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(this, () -> { if (player.isOnline()) player.sendMessage(ChatColor.RED + "[Frost] Repeating the same message is not allowed."); });
                return;
            }
            lastPlayerChatMessage.put(player.getUniqueId(), plain);
            lastPlayerChatMessageAt.put(player.getUniqueId(), nowMillis);
        }
        long cooldownNanos = chatCooldownNanos;
        if (cooldownNanos <= 0L) return;
        long now = System.nanoTime();
        long[] remaining = {0L};
        lastPlayerChatAtNanos.compute(player.getUniqueId(), (id, previous) -> {
            if (previous == null || now - previous >= cooldownNanos) return now;
            remaining[0] = cooldownNanos - (now - previous);
            return previous;
        });
        if (remaining[0] > 0L) {
            event.setCancelled(true);
            long millis = Math.max(1L, (remaining[0] + 999_999L) / 1_000_000L);
            Bukkit.getScheduler().runTask(this, () -> {
                if (player.isOnline()) player.sendMessage(ChatColor.RED + "[Frost] Please wait "
                        + String.format(Locale.US, "%.1f", millis / 1000.0)
                        + "s before sending another message.");
            });
        }
    }

    private void sendCooldownStatus(Player player) {
        Map<String, Long> active = readyAt.getOrDefault(player.getUniqueId(), Map.of());
        player.sendMessage(ChatColor.AQUA + "[Frost] Your ability cooldowns:");
        for (String group : List.of("snowball", "tnt", "windcharge", "grapple", "riptide",
                "firework", "lava", "battleaxe", "shield", "sword", "spear", "totem", "bow", "crossbow", "helmet", "rod", "horn")) {
            long until = active.getOrDefault(group, 0L);
            long remaining = Math.max(0, until - ticksPassed);
            // TNT stores a wall-clock timestamp; readiness timer is authoritative for UI.
            player.sendMessage(ChatColor.GRAY + groupLabel(group) + ": "
                    + (remaining <= 0 ? ChatColor.GREEN + "READY" :
                    ChatColor.YELLOW + String.format(Locale.US, "%.1fs", remaining / 20.0)));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("frost")) return false;
        if (args.length == 0 || args[0].equalsIgnoreCase("weapons") || args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Open /frost inside Minecraft to choose your weapons.");
                return true;
            }
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "You don't have access to the weapon menu.");
                return true;
            }
            openWeaponMenu(player);
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("checkowner")) {
            if (!sender.hasPermission("frost.checkowner")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
            if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + "Usage: /frost checkowner <frozensword|battleaxe|spear|pickaxe|bow|crossbow|armor>"); return true; }
            String query = args[1];
            List<String> owners = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (inventoryContainsFrost(online, item -> matchesOwnerQuery(item, query))) owners.add(online.getName());
            }
            if (owners.isEmpty()) sender.sendMessage(ChatColor.YELLOW + "[Frost] No online player currently has " + query + " in their inventory/equipment.");
            else sender.sendMessage(ChatColor.AQUA + "[Frost] " + query + " found on: " + ChatColor.WHITE + String.join(", ", owners));
            return true;
        }
        if (action.equals("duel")) return handleDuelCommand(sender, Arrays.copyOfRange(args, 1, args.length));
        if (action.equals("event")) {
            return frostEvents.handleCommand(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        if (action.equals("help")) {
            sender.sendMessage(ChatColor.AQUA + "Frost commands:");
            sender.sendMessage(ChatColor.GRAY + "/frost" + ChatColor.YELLOW + " (weapon selection GUI)");
            sender.sendMessage(ChatColor.GRAY + "/frost config" + ChatColor.YELLOW + " (OP settings GUI)");
            sender.sendMessage(ChatColor.GRAY + "/frost event" + ChatColor.YELLOW + " (OP event setup GUI)");
            sender.sendMessage(ChatColor.GRAY + "/frost event <type> <frost item> [minutes]");
            sender.sendMessage(ChatColor.GRAY + "/frost event stop | status");
            sender.sendMessage(ChatColor.GRAY + "/frost checkowner <frost item>");
            sender.sendMessage(ChatColor.GRAY + "/frost duel <player> | /frost duel accept | /frost duel <player> accept");
            sender.sendMessage(ChatColor.GRAY + "/frost trust <player>");
            sender.sendMessage(ChatColor.GRAY + "/frost untrust <player>");
            sender.sendMessage(ChatColor.GRAY + "/frost trusted");
            sender.sendMessage(ChatColor.GRAY + "/frost reload" + ChatColor.YELLOW + " (OP)");
            sender.sendMessage(ChatColor.GRAY + "/frost resetcooldowns [player]" + ChatColor.YELLOW + " (OP)");
            sender.sendMessage(ChatColor.GRAY + "/frost restyle" + ChatColor.YELLOW + " (update existing Frost tooltips)");
            sender.sendMessage(ChatColor.GRAY + "/frost cleartextures" + ChatColor.YELLOW + " (restore old Frost items to vanilla appearance)");
            return true;
        }
        if (action.equals("trust") || action.equals("untrust") || action.equals("trusted")) {
            return manageTrust(sender, action, args);
        }
        if (action.equals("config")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "The config GUI is available in-game only.");
                return true;
            }
            if (!player.hasPermission("frost.config")) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            openConfigHome(player);
            return true;
        }
        if (action.equals("cleartextures") || action.equals("reskin")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Run /frost cleartextures in-game.");
                return true;
            }
            int updated = clearInventoryModels(player);
            player.sendMessage(ChatColor.AQUA + "[Frost] Restored vanilla textures on "
                    + updated + " existing Frost items. Your abilities and enchantments are unchanged.");
            return true;
        }
        if (action.equals("restyle")) {
            if (sender instanceof Player player) {
                int count = refreshFrostTooltips(player);
                player.sendMessage(ChatColor.AQUA + "[Frost] Refreshed " + count + " Frost item tooltips.");
            } else sender.sendMessage(ChatColor.RED + "Restyling inventory items requires an in-game player.");
            return true;
        }
        if (action.equals("resetcooldowns")) {
            if (!sender.hasPermission("frost.resetcooldowns")) {
                sender.sendMessage(ChatColor.RED + "No permission."); return true;
            }
            Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1])
                    : (sender instanceof Player player ? player : null);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Use /frost resetcooldowns [online player]");
                return true;
            }
            resetWeaponCooldowns(target);
            sender.sendMessage(ChatColor.GREEN + "Reset all Frost cooldowns for " + target.getName() + ".");
            if (!target.equals(sender)) target.sendMessage(ChatColor.GREEN + "[Frost] Your cooldowns were reset!");
            return true;
        }
        if (action.equals("reload")) {
            if (!sender.hasPermission("frost.reload")) {
                sender.sendMessage(ChatColor.RED + "No permission."); return true;
            }
            reloadConfig(); loadSettings(); refreshChatCooldown();
            sender.sendMessage(ChatColor.GREEN + "Frost config reloaded!");
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Unknown Frost command. Try /frost help.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> choices = new ArrayList<>(List.of("help"));
            if (sender.isOp()) choices.addAll(List.of("menu", "weapons"));
            if (sender.hasPermission("frost.trust")) choices.addAll(List.of("trust", "untrust", "trusted"));
            if (sender.hasPermission("frost.config")) choices.add("config");
            choices.add("event");
            choices.add("duel");
            if (sender.hasPermission("frost.checkowner")) choices.add("checkowner");
            if (sender.hasPermission("frost.reload")) choices.add("reload");
            choices.add("cleartextures");
            choices.add("restyle");
            if (sender.hasPermission("frost.resetcooldowns")) choices.add("resetcooldowns");
            return StringUtil.copyPartialMatches(args[0], choices, new ArrayList<>());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("checkowner")) {
            return StringUtil.copyPartialMatches(args[1], List.of("frozensword", "battleaxe", "spear", "pickaxe", "bow", "crossbow", "shield", "helmet", "chestplate", "leggings", "boots", "totem", "horn", "rod"), new ArrayList<>());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("duel")) {
            List<String> options = new ArrayList<>(List.of("accept"));
            options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return StringUtil.copyPartialMatches(args[1], options, new ArrayList<>());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("duel")) {
            List<String> options = args[1].equalsIgnoreCase("accept")
                    ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of("accept");
            return StringUtil.copyPartialMatches(args[2], options, new ArrayList<>());
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("event")) {
            return frostEvents.tabComplete(Arrays.copyOfRange(args, 1, args.length));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("resetcooldowns")
                && sender.hasPermission("frost.resetcooldowns")) {
            return StringUtil.copyPartialMatches(args[1],
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), new ArrayList<>());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("trust")
                || args[0].equalsIgnoreCase("untrust")) && sender.hasPermission("frost.trust")) {
            return StringUtil.copyPartialMatches(args[1],
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}

package com.example.punchsnowball;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Stray;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Frost event subsystem. One active server-wide event at a time.
 * Runs entirely on the server's main thread; event-owned entities/blocks are
 * tagged or tracked and removed when the event ends or the plugin disables.
 */
final class FrostEvents implements Listener {
    private final PunchSnowballPlugin plugin;
    private final NamespacedKey relicMarker;
    private final NamespacedKey crystalMarker;
    private final NamespacedKey bingoBookMarker;
    private final Random random = new Random();
    private final Map<UUID, Setup> adminSetups = new HashMap<>();
    private Active active;
    private long generation;

    private static final LinkedHashMap<String, String> TYPES = new LinkedHashMap<>();
    private static final LinkedHashMap<String, String> REWARDS = new LinkedHashMap<>();
    static {
        TYPES.put("meteor", "Frozen Meteor");
        TYPES.put("koth", "King of the Hill");
        TYPES.put("chosen", "The Chosen One");
        TYPES.put("titan", "Frost Titan");
        TYPES.put("treasure", "Treasure Hunt");
        TYPES.put("assassin", "Assassin Hunt");
        TYPES.put("shatteredshrines", "Shattered Shrines");
        TYPES.put("whiteout", "Whiteout");
        TYPES.put("crystalfallrush", "Crystalfall Rush");
        TYPES.put("bingo", "Frost Bingo");
        TYPES.put("wither", "Frost Wither");

        REWARDS.put("sword", "Frozen Sword");
        REWARDS.put("battleaxe", "Frozen Battleaxe");
        REWARDS.put("spear", "Frozen Spear");
        REWARDS.put("bow", "Frozen Bow");
        REWARDS.put("crossbow", "Frozen Crossbow");
        REWARDS.put("shield", "Frozen Shield");
        REWARDS.put("grapple", "Loyalty Trident");
        REWARDS.put("riptide", "Riptide Trident");
        REWARDS.put("pickaxe", "Frozen Pickaxe");
        REWARDS.put("snowball", "Frozen Snowball");
        REWARDS.put("tnt", "Frozen TNT");
        REWARDS.put("windcharge", "Frozen Wind Charge");
        REWARDS.put("firework", "Frozen Firework");
        REWARDS.put("lava", "Frozen Flint and Steel");
        REWARDS.put("totem", "Frozen Totem");
        REWARDS.put("rod", "Frozen Fishing Rod");
        REWARDS.put("horn", "Frozen Horn");
        REWARDS.put("helmet", "Frozen Helmet");
        REWARDS.put("chestplate", "Frozen Chestplate");
        REWARDS.put("leggings", "Frozen Leggings");
        REWARDS.put("boots", "Frozen Boots");
    }

    private static final int[] TYPE_SLOTS = {10, 12, 14, 16, 28, 30, 32, 34, 25, 31, 33};
    private static final int[] REWARD_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static class Setup {
        String type = "meteor";
        String reward = "sword";
        int minutes = 20;
    }

    private enum BingoKind { BREAK, PICKUP, CRAFT, SMELT, KILL }
    private record BingoQuest(BingoKind kind, String target, int amount, String description) {}
    private static final class BingoCell {
        final BingoQuest quest;
        int progress;
        boolean completed;
        BingoCell(BingoQuest quest) { this.quest = quest; }
    }

    private static class EventMenu implements InventoryHolder {
        final String view;
        final Inventory inventory;
        EventMenu(String view) {
            this.view = view;
            inventory = Bukkit.createInventory(this, 54,
                    Component.text(view.equals("rewards") ? "Frost Events - Rewards" : "Frost Events - Setup"));
        }
        @Override public Inventory getInventory() { return inventory; }
    }

    private static class Active {
        final String type;
        final String reward;
        final World world;
        final UUID token = UUID.randomUUID();
        final int totalSeconds;
        int seconds;
        final BossBar bar;
        Location origin;
        Location objective;
        Block objectiveBlock;
        BlockData oldBlockData;
        // All impact edits are tracked and restored when the event ends.
        final Map<Block, BlockData> impactOriginal = new LinkedHashMap<>();
        final Map<Block, BlockData> impactPlaced = new LinkedHashMap<>();
        // Ticket the entire meteor flight corridor so the falling visuals and impact
        // still run even if no players are close enough to load its chunks.
        final Set<Long> meteorChunkTickets = new LinkedHashSet<>();
        final List<Entity> visuals = new ArrayList<>();
        final Map<UUID, Integer> points = new HashMap<>();
        final Map<UUID, UUID> targets = new HashMap<>();
        final Map<UUID, Double> bossDamage = new HashMap<>();
        UUID chosenHolder;
        UUID lastHolder;
        UUID chosenGlowPlayer;
        boolean chosenOriginalGlowing;
        ItemDisplay chosenHeartDisplay;
        long lastChosenTagMillis;
        String chosenLastCoordinates = "Unknown";
        boolean treasureUnderground;
        int treasureDepth;
        Location treasureEntrance;
        Warden titan;
        Wither wither;
        double witherRaidHealth;
        double witherMaxRaidHealth;
        int witherNextAbility = 7;
        int witherAttackIndex;
        boolean witherEnraged;
        boolean witherDefeated;
        double titanRaidHealth;
        double titanMaxRaidHealth;
        int titanNextAbility = 6;
        int titanAttackIndex;
        long titanMovementStep;
        long titanNextMeleeStep;
        int titanStrafeDirection = 1;
        long titanLastSuccessfulStep;
        boolean titanDefeated;
        boolean meteorLanded;
        boolean enraged;
        long serial;
        final List<Location> shrineLocations = new ArrayList<>();
        final List<Location> crystalLocations = new ArrayList<>();
        final Map<Integer, UUID> shrineOwners = new HashMap<>();
        final Map<Integer, UUID> shrineCapturers = new HashMap<>();
        final Map<Integer, Integer> shrineCaptureProgress = new HashMap<>();
        int whiteoutRadius;
        int lastWhiteoutShrinkSecond;
        int lastCrystalWaveSecond;
        final Map<UUID, List<BingoCell>> bingoBoards = new HashMap<>();

        Active(String type, String reward, World world, int duration, BossBar bar) {
            this.type = type;
            this.reward = reward;
            this.world = world;
            this.totalSeconds = duration;
            this.bar = bar;
        }
    }

    boolean reservesActionBar() {
        return active != null && ("assassin".equals(active.type) || "koth".equals(active.type));
    }

    FrostEvents(PunchSnowballPlugin plugin) {
        this.plugin = plugin;
        this.relicMarker = NamespacedKey.fromString("punchsnowball:frost_event_relic");
        this.crystalMarker = NamespacedKey.fromString("punchsnowball:frost_event_crystal");
        this.bingoBookMarker = NamespacedKey.fromString("punchsnowball:frost_bingo_book");
        // v3.2.6's 24 HP cap followed by 0.30 scaling made every powerful hit
        // appear to deal ~7 raid HP. Upgrade ONLY those old stock defaults.
        if (!plugin.getConfig().getBoolean("events.titan-hit-scaling-v327", false)) {
            double oldCap = plugin.getConfig().getDouble("events.titan-max-damage-per-hit", 24.0);
            double oldScale = plugin.getConfig().getDouble("events.titan-damage-received-multiplier", 0.30);
            if (Math.abs(oldCap - 24.0) < 0.0001)
                plugin.getConfig().set("events.titan-max-damage-per-hit", 96.0);
            if (Math.abs(oldScale - 0.30) < 0.0001)
                plugin.getConfig().set("events.titan-damage-received-multiplier", 1.0);
            plugin.getConfig().set("events.titan-hit-scaling-v327", true);
            plugin.saveConfig();
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        // A Warden with AI disabled does not walk in response to ordinary velocity.
        // Drive reliable, collision-checked positional steps 10x/second instead.
        // Its normal AI, sonic boom and uncapped melee remain disabled.
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickTitanMovement, 2L, 2L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickWitherMovement, 2L, 2L);
        // Keep a purely visual, non-pickup Heart of the Sea above the relic holder.
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickChosenHeartDisplay, 2L, 2L);
    }

    private String label(String type) { return TYPES.getOrDefault(type, type); }
    private String rewardName(String reward) { return REWARDS.getOrDefault(reward, reward); }
    private int configuredInt(String key, int fallback) { return plugin.getConfig().getInt("events." + key, fallback); }
    private double configuredDouble(String key, double fallback) { return plugin.getConfig().getDouble("events." + key, fallback); }
    private String prefix() { return ChatColor.AQUA + "[Frost Event] " + ChatColor.RESET; }
    private void announce(String message) { Bukkit.broadcastMessage(prefix() + message); }

    private static String normalize(String s) { return s.toLowerCase(Locale.ROOT).replace("_", "").replace("-", ""); }
    private String findType(String input) {
        String name = normalize(input);
        if (name.equals("frosttitan") || name.equals("boss")) return "titan";
        if (name.equals("frostwither") || name.equals("witherboss")) return "wither";
        if (name.equals("kingofthehill")) return "koth";
        if (name.equals("treasurehunt")) return "treasure";
        if (name.equals("frozenmeteor")) return "meteor";
        if (name.equals("chosenone")) return "chosen";
        if (name.equals("assassinhunt")) return "assassin";
        if (name.equals("shrines") || name.equals("shatteredshrine")) return "shatteredshrines";
        if (name.equals("crystalfall") || name.equals("crystalfallrush")) return "crystalfallrush";
        return TYPES.containsKey(name) ? name : null;
    }
    private String findReward(String input) {
        String name = normalize(input);
        for (String key : REWARDS.keySet()) {
            if (name.equals(key) || name.equals("frost" + key) || name.equals("frozen" + key)) return key;
        }
        if (name.equals("frozentrident")) return "grapple";
        if (name.equals("frosttridentdepths")) return "riptide";
        if (name.equals("frostpickaxe")) return "pickaxe";
        return null;
    }

    boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("frost.event")) {
                sender.sendMessage(prefix() + ChatColor.RED + "Only event administrators can open this menu."); return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix() + "Use /frost event <type> <reward> [minutes] from console."); return true;
            }
            openSetup(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("status")) {
            status(sender);
            return true;
        }
        if (!sender.hasPermission("frost.event")) {
            sender.sendMessage(prefix() + ChatColor.RED + "No permission."); return true;
        }
        if (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("cancel")) {
            if (active == null) sender.sendMessage(prefix() + "There is no active event.");
            else { announce("Event stopped by an administrator."); closeEvent(); }
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(prefix() + "Usage: /frost event <meteor|koth|chosen|titan|wither|bingo|treasure|assassin|shatteredshrines|whiteout|crystalfallrush> <frost item> [minutes]");
            sender.sendMessage(prefix() + "Or open /frost event for the GUI.");
            return true;
        }
        String type = findType(args[0]);
        String reward = findReward(args[1]);
        if (type == null || reward == null) {
            sender.sendMessage(prefix() + ChatColor.RED + "Unknown event or reward. Use /frost event for the GUI."); return true;
        }
        int minutes = configuredInt("default-duration-minutes", 20);
        if (args.length >= 3) {
            try {
                String raw = args[2].toLowerCase(Locale.ROOT).replace("minutes", "").replace("min", "").replace("m", "");
                minutes = Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                sender.sendMessage(prefix() + "Duration example: 20m"); return true;
            }
        }
        start(sender, type, reward, minutes);
        return true;
    }

    List<String> tabComplete(String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(TYPES.keySet());
            suggestions.add("status"); suggestions.add("stop");
            return suggestions.stream().filter(v -> v.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("status") && !args[0].equalsIgnoreCase("stop"))
            return REWARDS.keySet().stream().map(v -> "frost" + v)
                    .filter(v -> v.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 3) return List.of("10m", "15m", "20m", "30m", "45m");
        return List.of();
    }

    private Setup getSetup(Player player) {
        return adminSetups.computeIfAbsent(player.getUniqueId(), unused -> {
            Setup setup = new Setup();
            setup.minutes = configuredInt("default-duration-minutes", 20);
            return setup;
        });
    }
    private ItemStack icon(Material type, String title, String... lore) {
        ItemStack stack = new ItemStack(type);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + title);
        if (lore.length > 0) meta.setLore(Arrays.stream(lore).map(v -> ChatColor.GRAY + v).toList());
        stack.setItemMeta(meta);
        return stack;
    }
    private void fill(Inventory inv) {
        ItemStack background = icon(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, background);
    }
    private Material typeIcon(String type) {
        return switch (type) {
            case "meteor" -> Material.BLUE_ICE;
            case "koth" -> Material.BEACON;
            case "chosen" -> Material.HEART_OF_THE_SEA;
            case "titan" -> Material.WARDEN_SPAWN_EGG;
            case "treasure" -> Material.CHEST;
            case "assassin" -> Material.IRON_SWORD;
            case "shatteredshrines" -> Material.AMETHYST_BLOCK;
            case "whiteout" -> Material.SNOW_BLOCK;
            case "crystalfallrush" -> Material.AMETHYST_SHARD;
            case "bingo" -> Material.WRITTEN_BOOK;
            case "wither" -> Material.WITHER_SKELETON_SKULL;
            default -> Material.SNOW_BLOCK;
        };
    }
    private void openSetup(Player player) {
        Setup setup = getSetup(player);
        EventMenu menu = new EventMenu("setup");
        fill(menu.inventory);
        int idx = 0;
        for (String type : TYPES.keySet()) {
            String chosen = setup.type.equals(type) ? ChatColor.GREEN + "Selected" : ChatColor.GRAY + "Click to select";
            menu.inventory.setItem(TYPE_SLOTS[idx++], icon(typeIcon(type), label(type), chosen));
        }
        menu.inventory.setItem(38, icon(Material.NETHER_STAR, "Reward: " + rewardName(setup.reward), "Click to select any Frost item"));
        menu.inventory.setItem(40, icon(Material.CLOCK, "Duration: " + setup.minutes + " minutes", "Left click: next duration", "Right click: previous duration"));
        menu.inventory.setItem(42, icon(Material.EMERALD_BLOCK, "START EVENT", "Event: " + label(setup.type), "Prize: " + rewardName(setup.reward)));
        menu.inventory.setItem(44, icon(Material.BARRIER, "STOP ACTIVE EVENT", "Immediately end the current event"));
        menu.inventory.setItem(49, icon(Material.BOOK, "Event status", active == null ? "No event active" : label(active.type) + " is active"));
        player.openInventory(menu.inventory);
    }
    private void openRewards(Player player) {
        EventMenu menu = new EventMenu("rewards");
        fill(menu.inventory);
        int idx = 0;
        for (String group : REWARDS.keySet()) {
            ItemStack item = plugin.frostEventReward(group);
            if (item == null) item = icon(Material.SNOW_BLOCK, rewardName(group));
            else {
                item = item.clone();
                ItemMeta meta = item.getItemMeta();
                List<String> lore = new ArrayList<>(meta.hasLore() ? meta.getLore() : List.of());
                lore.add(ChatColor.GOLD + "Click to choose this reward");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            menu.inventory.setItem(REWARD_SLOTS[idx++], item);
        }
        menu.inventory.setItem(49, icon(Material.ARROW, "Back to event setup"));
        player.openInventory(menu.inventory);
    }
    private static final int[] DURATIONS = {5, 10, 15, 20, 30, 45, 60};
    @EventHandler(ignoreCancelled = true)
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof EventMenu menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !player.hasPermission("frost.event")) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;
        Setup setup = getSetup(player);
        if (menu.view.equals("rewards")) {
            for (int i = 0; i < REWARD_SLOTS.length; i++) {
                if (slot != REWARD_SLOTS[i]) continue;
                setup.reward = new ArrayList<>(REWARDS.keySet()).get(i);
                openSetup(player);
                return;
            }
            if (slot == 49) openSetup(player);
            return;
        }
        for (int i = 0; i < TYPE_SLOTS.length; i++) {
            if (slot == TYPE_SLOTS[i]) {
                setup.type = new ArrayList<>(TYPES.keySet()).get(i);
                openSetup(player);
                return;
            }
        }
        if (slot == 38) { openRewards(player); return; }
        if (slot == 40) {
            int index = 0;
            for (int i = 0; i < DURATIONS.length; i++) if (DURATIONS[i] == setup.minutes) index = i;
            index = (index + (event.isRightClick() ? DURATIONS.length - 1 : 1)) % DURATIONS.length;
            setup.minutes = DURATIONS[index];
            openSetup(player);
            return;
        }
        if (slot == 42) {
            player.closeInventory();
            start(player, setup.type, setup.reward, setup.minutes);
            return;
        }
        if (slot == 44) {
            player.closeInventory();
            if (active != null) { announce("Event stopped by an administrator."); closeEvent(); }
            else player.sendMessage(prefix() + "No active event.");
            return;
        }
        if (slot == 49) { player.closeInventory(); status(player); }
    }
    @EventHandler(ignoreCancelled = true)
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof EventMenu) { event.setCancelled(true); return; }
        if (active == null || !active.type.equals("chosen")) return;
        int size = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(raw -> raw < size)
                && event.getNewItems().values().stream().anyMatch(item -> isRelic(item, active)))
            event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true)
    public void protectChosenRelicInStorage(InventoryClickEvent event) {
        if (active == null || !active.type.equals("chosen")) return;
        if (event.getView().getTopInventory().getHolder() instanceof EventMenu) return;
        // Never let the event relic be hidden inside a chest, shulker, or ender chest.
        if (event.getView().getTopInventory() instanceof org.bukkit.inventory.PlayerInventory) return;
        if (isRelic(event.getCursor(), active)
                || (event.getRawSlot() < event.getView().getTopInventory().getSize()
                    && isRelic(event.getCurrentItem(), active))) {
            event.setCancelled(true);
        }
    }

    private void status(CommandSender sender) {
        if (active == null) { sender.sendMessage(prefix() + "No active event."); return; }
        sender.sendMessage(prefix() + "Active: " + label(active.type) + " | Prize: " + rewardName(active.reward)
                + " | Time left: " + Math.max(0, active.totalSeconds - active.seconds) + "s");
        if (active.objective != null) sender.sendMessage(prefix() + "Location: " + loc(active.objective));
        if (active.type.equals("assassin")) sender.sendMessage(prefix() + "Only eliminations of your assigned target score points.");
        if (active.type.equals("wither") || active.type.equals("titan")) {
            Map<UUID, Double> damage = active.bossDamage;
            List<Map.Entry<UUID, Double>> ranked = damage.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed()).limit(3).toList();
            for (int i = 0; i < ranked.size(); i++) {
                Player p = Bukkit.getPlayer(ranked.get(i).getKey());
                sender.sendMessage(prefix() + "#" + (i + 1) + " " + (p == null ? ranked.get(i).getKey().toString().substring(0, 8) : p.getName())
                        + ": " + String.format(Locale.ROOT, "%.1f", ranked.get(i).getValue()) + " damage");
            }
        }
        if (active.type.equals("assassin") || active.type.equals("koth")
                || active.type.equals("shatteredshrines") || active.type.equals("crystalfallrush") || active.type.equals("whiteout")
                || active.type.equals("bingo")) {
            List<Map.Entry<UUID, Integer>> leaders = active.points.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed()).limit(3).toList();
            for (int i = 0; i < leaders.size(); i++) {
                var entry = leaders.get(i);
                Player player = Bukkit.getPlayer(entry.getKey());
                sender.sendMessage(prefix() + "#" + (i+1) + " "
                        + (player == null ? entry.getKey().toString().substring(0,8) : player.getName())
                        + ": " + entry.getValue() + (active.type.equals("koth") ? " seconds" :
                        active.type.equals("crystalfallrush") ? " crystals" :
                        active.type.equals("whiteout") ? " safe seconds" :
                        active.type.equals("bingo") ? " completed quests" :
                        active.type.equals("shatteredshrines") ? " shrine points" : " kills"));
            }
        }
    }
    private String loc(Location l) { return l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ(); }

    private boolean eligible(Player player) {
        return player.isOnline() && !player.isDead() && player.getGameMode() != GameMode.CREATIVE
                && player.getGameMode() != GameMode.SPECTATOR;
    }
    private List<Player> participants() {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (eligible(player)) {
                players.add(player);
            }
        }
        return players;
    }
    private void start(CommandSender sender, String type, String reward, int minutes) {
        if (active != null) { sender.sendMessage(prefix() + ChatColor.RED + "Finish or stop the current event first."); return; }
        if (minutes < 1 || minutes > 120) {
            sender.sendMessage(prefix() + "Duration must be from 1 to 120 minutes."); return;
        }
        List<Player> players = participants();
        if (players.isEmpty()) { sender.sendMessage(prefix() + "At least one survival player must be online."); return; }
        if ((type.equals("assassin") || type.equals("whiteout")) && players.size() < 2) {
            sender.sendMessage(prefix() + label(type) + " requires at least two online survival players."); return;
        }
        Player host = sender instanceof Player p ? p : players.get(0);
        World world = host.getWorld();
        if (type.equals("whiteout") && players.stream().filter(p -> p.getWorld().equals(world)).count() < 2) {
            sender.sendMessage(prefix() + "Whiteout requires at least two survival players in the event world."); return;
        }
        if ((type.equals("meteor") || type.equals("treasure") || type.equals("titan") || type.equals("wither")
                || type.equals("shatteredshrines") || type.equals("whiteout") || type.equals("crystalfallrush"))
                && world.getEnvironment() != World.Environment.NORMAL) {
            sender.sendMessage(prefix() + "Start the " + label(type) + " event from an Overworld location.");
            return;
        }
        Location origin = host.getLocation().clone();
        if (!world.getWorldBorder().isInside(origin)) {
            sender.sendMessage(prefix() + ChatColor.RED + "Stand inside the world border before starting this event."); return;
        }
        Location objective = null;
        if (type.equals("treasure")) {
            // Treasure Hunt independently samples its full 2,500 x 2,500 event area.
            // No terrain search is tied to the administrator's current location.
            objective = null;
        }
        if (type.equals("meteor")) {
            Location site = meteorSurfaceLocation(origin);
            if (site == null) {
                sender.sendMessage(prefix() + ChatColor.RED + "No safe, flat natural terrain inside the world border for a meteor crater. Try another location.");
                return;
            }
            objective = site;
        }
        if (type.equals("titan") || type.equals("wither")) {
            String rangeKey = type.equals("wither") ? "wither-spawn-range-blocks" : "boss-spawn-range-blocks";
            int fallbackRange = type.equals("wither") ? 55 : 40;
            objective = randomSurfaceLocation(origin, Math.max(16, configuredInt(rangeKey, fallbackRange)));
            if (objective == null) {
                sender.sendMessage(prefix() + ChatColor.RED + "Could not find a safe surface for " + label(type) + "."); return;
            }
        }
        if (type.equals("crystalfallrush")) {
            int altarRange = Math.max(8, configuredInt("crystalfall-altar-range-blocks", 32));
            for (int attempt = 0; attempt < 12 && objective == null; attempt++) {
                Location candidate = randomSurfaceLocation(origin, altarRange, 8);
                if (candidate == null || candidate.distanceSquared(origin) < 8 * 8) continue;
                boolean playerNearby = players.stream().anyMatch(p -> p.getWorld().equals(world)
                        && p.getLocation().distanceSquared(candidate) < 5 * 5);
                if (!playerNearby) objective = candidate;
            }
            if (objective == null) {
                sender.sendMessage(prefix() + ChatColor.RED + "Could not find a safe location for the Crystalfall altar."); return;
            }
        }
        BossBar bar = Bukkit.createBossBar(ChatColor.AQUA + "Frost Event: " + label(type), BarColor.BLUE, BarStyle.SEGMENTED_10);
        Active event = new Active(type, reward, world, minutes * 60, bar);
        event.serial = ++generation;
        event.origin = origin;
        event.objective = objective;
        active = event;
        for (Player p : Bukkit.getOnlinePlayers()) bar.addPlayer(p);
        announce(ChatColor.WHITE + label(type) + ChatColor.AQUA + " started! Prize: " + ChatColor.GOLD + rewardName(reward)
                + ChatColor.AQUA + ". Duration: " + minutes + "m.");
        switch (type) {
            case "meteor" -> startMeteor(event);
            case "treasure" -> {
                if (!startTreasure(event)) {
                    sender.sendMessage(prefix() + ChatColor.RED +
                            "Could not find safe natural terrain for the treasure shrine. Event cancelled.");
                    closeEvent(); return;
                }
                int nearX = Math.floorDiv(event.objective.getBlockX(), 200) * 200;
                int nearZ = Math.floorDiv(event.objective.getBlockZ(), 200) * 200;
                announce("An icy treasure shrine appeared in " + world.getName() + " near X=" +
                        nearX + ", Z=" + nearZ + " (within about 200 blocks). New clues every 50s!");
                if (event.treasureUnderground) announce(ChatColor.GOLD + "UNDERGROUND VAULT! " +
                        ChatColor.AQUA + "Look for a small icy entrance above a ladder shaft. " +
                        "The chest is about " + event.treasureDepth + " blocks beneath the surface.");
                else announce("The chest is in an icy surface shrine. Look for blue-ice pillars and lights.");
            }
            case "koth" -> {
                event.objective = origin;
                announce("Capture the hill at " + loc(origin) + ". Only the sole player inside earns progress!");
            }
            case "chosen" -> startChosen(event, players);
            case "assassin" -> {
                for (Player p : players) assignTarget(event, p);
                announce("Hunt YOUR assigned target! Check the action bar for their name and distance.");
            }
            case "titan" -> spawnTitan(event);
            case "wither" -> spawnWither(event);
            case "shatteredshrines" -> {
                if (!startShatteredShrines(event)) {
                    sender.sendMessage(prefix() + ChatColor.RED + "Could not find three safe shrine locations inside the world border. Event cancelled.");
                    closeEvent(); return;
                }
            }
            case "whiteout" -> startWhiteout(event);
            case "crystalfallrush" -> {
                if (!startCrystalfallRush(event)) {
                    sender.sendMessage(prefix() + ChatColor.RED + "Could not prepare a safe Crystalfall altar. Event cancelled.");
                    closeEvent(); return;
                }
            }
            case "bingo" -> startBingo(event, players);
            default -> { }
        }
    }

    private boolean placeTemporaryEventBlock(Active event, Location location, Material material) {
        if (location == null || location.getWorld() != event.world) return false;
        Block block = location.getBlock();
        if (!block.isPassable() || block.getState() instanceof TileState) return false;
        if (participants().stream().anyMatch(player -> player.getWorld().equals(event.world)
                && player.getLocation().distanceSquared(location) < 2.5 * 2.5)) return false;
        BlockData original = block.getBlockData().clone();
        BlockData placed = material.createBlockData();
        event.impactOriginal.put(block, original);
        block.setBlockData(placed, false);
        event.impactPlaced.put(block, placed.clone());
        return true;
    }

    private Location findSeparatedSite(Active event, List<Location> used, int radius, double separation) {
        for (int attempt = 0; attempt < 12; attempt++) {
            Location candidate = randomSurfaceLocation(event.origin, radius, 8);
            if (candidate == null || !event.world.getWorldBorder().isInside(candidate)) continue;
            if (candidate.distanceSquared(event.origin) < 20 * 20) continue;
            boolean tooClose = used.stream().anyMatch(other -> other.distanceSquared(candidate) < separation * separation);
            if (!tooClose) return candidate;
        }
        return null;
    }

    private boolean startShatteredShrines(Active event) {
        int range = Math.max(120, configuredInt("shrine-spawn-range-blocks", 450));
        List<Location> sites = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Location site = findSeparatedSite(event, sites, range, 65.0);
            if (site == null || !placeTemporaryEventBlock(event, site, Material.LODESTONE)) return false;
            sites.add(site);
            event.shrineLocations.add(site.clone());
            event.world.spawnParticle(Particle.END_ROD, site.clone().add(0.5, 1.1, 0.5), 35, .8, .8, .8, .025);
        }
        announce("Three ancient shrines have appeared! Stand within 4 blocks of a shrine for 5 seconds to claim it, then defend it to score.");
        announceShrineHints(event, 200, true);
        announce("Tip: a contested shrine pauses scoring. You earn points only while you remain close enough to defend your claimed shrine.");
        return true;
    }

    private void announceShrineHints(Active event, int precision, boolean opening) {
        int shown = 0;
        for (int i = 0; i < event.shrineLocations.size(); i++) {
            if (event.shrineOwners.containsKey(i)) continue;
            Location location = event.shrineLocations.get(i);
            int x = Math.floorDiv(location.getBlockX(), precision) * precision;
            int z = Math.floorDiv(location.getBlockZ(), precision) * precision;
            announce("Shrine " + (i + 1) + " " + (opening ? "first clue" : "updated clue")
                    + ": somewhere near X=" + x + ", Z=" + z + " (about " + precision + " blocks of uncertainty).");
            shown++;
        }
        if (shown == 0) announce("All three shrines are claimed. Keep defending them to build your score!");
    }

    private void startWhiteout(Active event) {
        event.objective = event.origin.clone();
        event.whiteoutRadius = Math.max(24, configuredInt("whiteout-start-radius-blocks", 80));
        event.lastWhiteoutShrinkSecond = 0;
        announce("WHITEOUT! Stay inside the snow-safe zone around X=" + event.origin.getBlockX()
                + ", Z=" + event.origin.getBlockZ() + ". The safe area shrinks every "
                + Math.max(10, configuredInt("whiteout-shrink-interval-seconds", 30)) + " seconds.");
        announce("Tip: watch the boss bar and move toward the center before the blizzard closes in. Players outside take storm damage.");
    }

    private ItemStack createEventCrystal() {
        ItemStack stack = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frost Crystal");
        meta.setLore(List.of(ChatColor.GRAY + "Crystalfall Rush objective", ChatColor.YELLOW + "Return to the Frost Altar and right-click it to score"));
        meta.getPersistentDataContainer().set(crystalMarker, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    private boolean isEventCrystal(ItemStack stack) {
        return stack != null && !stack.getType().isAir() && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer().has(crystalMarker, PersistentDataType.BYTE);
    }

    private List<Item> liveCrystals(Active event) {
        return event.world.getEntitiesByClass(Item.class).stream()
                .filter(item -> item.isValid() && isEventCrystal(item.getItemStack())).toList();
    }

    private int liveCrystalCount(Active event) {
        return liveCrystals(event).size();
    }

    private int carriedCrystalCount() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (ItemStack stack : player.getInventory().getContents())
                if (isEventCrystal(stack)) count += stack.getAmount();
            if (isEventCrystal(player.getItemOnCursor())) count += player.getItemOnCursor().getAmount();
        }
        return count;
    }

    private void spawnCrystals(Active event, int amount) {
        int range = Math.max(100, configuredInt("crystalfall-spawn-range-blocks", 300));
        for (int i = 0; i < amount; i++) {
            Location site = null;
            for (int attempt = 0; attempt < 24; attempt++) {
                Location candidate = randomSurfaceLocation(event.origin, range, 8);
                if (candidate == null || candidate.distanceSquared(event.objective) < 24 * 24) continue;
                boolean crowded = liveCrystals(event).stream()
                        .map(Entity::getLocation).anyMatch(other -> other.distanceSquared(candidate) < 12 * 12);
                if (!crowded) { site = candidate; break; }
            }
            if (site == null) continue;
            Item dropped = event.world.dropItem(site.clone().add(0, .15, 0), createEventCrystal());
            dropped.setPickupDelay(8);
            dropped.setCustomName(ChatColor.AQUA + "Frost Crystal");
            dropped.setCustomNameVisible(true);
            event.visuals.add(dropped);
            event.crystalLocations.add(site.clone());
            event.world.spawnParticle(Particle.END_ROD, site.clone().add(0, .6, 0), 18, .3, .45, .3, .02);
        }
    }

    private boolean startCrystalfallRush(Active event) {
        if (!placeTemporaryEventBlock(event, event.objective, Material.LODESTONE)) return false;
        event.objectiveBlock = event.objective.getBlock();
        spawnCrystals(event, Math.max(4, configuredInt("crystalfall-initial-crystals", 8)));
        announce("Frost crystals have scattered across the area! Collect the amethyst shards and return them to the LODESTONE altar at "
                + loc(event.objective) + ". Right-click the altar to bank your crystals.");
        announceCrystalfallHints(event, 200, true);
        announce("Tip: carry several crystals before returning to the altar. Banked crystals count toward the first player to "
                + Math.max(1, configuredInt("crystalfall-target-crystals", 12)) + " points.");
        event.lastCrystalWaveSecond = 0;
        return true;
    }

    private void announceCrystalfallHints(Active event, int precision, boolean opening) {
        List<Location> live = liveCrystals(event).stream()
                .map(Entity::getLocation).limit(5).toList();
        if (live.isEmpty()) {
            announce("Crystalfall hint: no loose crystals are on the ground right now. A fresh wave is coming soon; check your inventory for crystals to bank.");
            return;
        }
        announce("Crystalfall " + (opening ? "first" : "updated") + " hints — " + live.size() + " crystal location(s):");
        int index = 1;
        for (Location location : live) {
            int x = Math.floorDiv(location.getBlockX(), precision) * precision;
            int z = Math.floorDiv(location.getBlockZ(), precision) * precision;
            announce("Crystal clue " + index++ + ": near X=" + x + ", Z=" + z + " (within about " + precision + " blocks).");
        }
        announce("Tip: listen for the crystal chime and look for floating amethyst shards with aqua particles.");
    }

    private void depositCrystals(Active event, Player player) {
        int deposited = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (!isEventCrystal(stack)) continue;
            deposited += stack.getAmount();
            player.getInventory().setItem(slot, null);
        }
        if (deposited <= 0) {
            player.sendMessage(prefix() + "You do not have any Frost Crystals to bank.");
            return;
        }
        int score = event.points.merge(player.getUniqueId(), deposited, Integer::sum);
        announce(player.getName() + " banked " + deposited + " Frost Crystal(s)! Score: " + score + "/"
                + Math.max(1, configuredInt("crystalfall-target-crystals", 12)) + ".");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.3f);
        player.getWorld().spawnParticle(Particle.END_ROD, event.objective.clone().add(.5, 1, .5), 30, .6, .7, .6, .04);
        player.updateInventory();
        if (score >= Math.max(1, configuredInt("crystalfall-target-crystals", 12)))
            win(event, player.getUniqueId(), "Banked the target number of Frost Crystals");
    }

    private void tickCrystalfallRush(Active event) {
        if (event.seconds % 4 == 0 && event.objective != null)
            event.world.spawnParticle(Particle.END_ROD, event.objective.clone().add(.5, 1.0, .5), 8, .4, .5, .4, .01);
        int outstanding = liveCrystalCount(event) + carriedCrystalCount();
        if (event.seconds - event.lastCrystalWaveSecond >= 30 && outstanding < 4) {
            spawnCrystals(event, 4 - outstanding);
            event.lastCrystalWaveSecond = event.seconds;
            announce("A new wave of Frost Crystals has fallen! Search around the event area.");
        }
        if (event.seconds > 0 && event.seconds % 60 == 0) {
            int precision = event.seconds < 120 ? 100 : 50;
            announceCrystalfallHints(event, precision, false);
        }
    }

    private void tickShatteredShrines(Active event) {
        for (int index = 0; index < event.shrineLocations.size(); index++) {
            Location shrine = event.shrineLocations.get(index);
            if (event.seconds % 3 == 0)
                event.world.spawnParticle(Particle.ENCHANT, shrine.clone().add(.5, .8, .5), 10, .7, .35, .7, .04);
            List<Player> nearby = participants().stream().filter(p -> p.getWorld().equals(event.world))
                    .filter(p -> p.getLocation().distanceSquared(shrine) <= 4.5 * 4.5).toList();
            if (nearby.size() != 1) {
                event.shrineCapturers.remove(index);
                event.shrineCaptureProgress.remove(index);
                if (nearby.size() > 1) {
                    for (Player p : nearby) p.sendActionBar(Component.text("⚔ Shrine " + (index + 1) + " is contested!"));
                }
                continue;
            }
            Player player = nearby.get(0);
            UUID id = player.getUniqueId();
            UUID owner = event.shrineOwners.get(index);
            if (id.equals(owner)) {
                int points = event.points.merge(id, 1, Integer::sum);
                player.sendActionBar(Component.text("❄ Defending Shrine " + (index + 1) + " | Event score: " + points));
                continue;
            }
            UUID capturer = event.shrineCapturers.get(index);
            if (!id.equals(capturer)) {
                event.shrineCapturers.put(index, id);
                event.shrineCaptureProgress.put(index, 0);
            }
            int progress = event.shrineCaptureProgress.merge(index, 1, Integer::sum);
            player.sendActionBar(Component.text("✦ Claiming Shrine " + (index + 1) + " | " + progress + "/5 seconds"));
            if (progress >= 5) {
                event.shrineOwners.put(index, id);
                event.shrineCapturers.remove(index);
                event.shrineCaptureProgress.remove(index);
                int score = event.points.merge(id, 5, Integer::sum);
                announce(player.getName() + " claimed Shattered Shrine " + (index + 1) + "! Control it to earn points. Score: " + score + ".");
                player.playSound(shrine, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
                event.world.spawnParticle(Particle.TOTEM_OF_UNDYING, shrine.clone().add(.5, 1, .5), 45, .8, 1, .8, .06);
            }
        }
        if (event.seconds > 0 && event.seconds % 60 == 0)
            announceShrineHints(event, event.seconds < 120 ? 100 : 50, false);
    }

    private void tickWhiteout(Active event) {
        event.bar.setTitle(ChatColor.AQUA + "Whiteout | Safe radius: " + event.whiteoutRadius
                + " blocks | " + Math.max(0, event.totalSeconds - event.seconds) + "s");
        int interval = Math.max(10, configuredInt("whiteout-shrink-interval-seconds", 30));
        if (event.seconds > 0 && event.seconds % interval == 0) {
            if (event.whiteoutRadius > 10)
                event.whiteoutRadius = Math.max(10, event.whiteoutRadius - Math.max(2, configuredInt("whiteout-shrink-blocks", 6)));
            int shift = Math.max(8, configuredInt("whiteout-shift-range-blocks", 35));
            Location movedZone = randomSurfaceLocation(event.objective, shift, 12);
            if (movedZone != null) event.objective = movedZone;
            event.lastWhiteoutShrinkSecond = event.seconds;
            announce("The Whiteout safe zone shifted to X=" + event.objective.getBlockX() + ", Z="
                    + event.objective.getBlockZ() + " and its radius is now " + event.whiteoutRadius + " blocks!");
        }
        if (event.objective != null && event.seconds % 2 == 0) {
            // Draw a readable perimeter instead of a diffuse snow cloud.
            for (int point = 0; point < 48; point++) {
                double angle = (Math.PI * 2.0 * point) / 48.0;
                Location marker = event.objective.clone().add(
                        Math.cos(angle) * event.whiteoutRadius, .35, Math.sin(angle) * event.whiteoutRadius);
                event.world.spawnParticle(Particle.SNOWFLAKE, marker, 1, .12, .10, .12, .005);
            }
        }
        for (Player player : participants()) {
            if (!player.getWorld().equals(event.world)) continue;
            double dx = player.getLocation().getX() - event.objective.getX();
            double dz = player.getLocation().getZ() - event.objective.getZ();
            double horizontalDistanceSquared = dx * dx + dz * dz;
            if (horizontalDistanceSquared <= (double) event.whiteoutRadius * event.whiteoutRadius) {
                int safeTime = event.points.merge(player.getUniqueId(), 1, Integer::sum);
                if (event.seconds % 5 == 0) player.sendActionBar(Component.text("❄ Whiteout safe zone | "
                        + event.whiteoutRadius + " blocks | Safe time: " + safeTime + "s"));
            } else {
                if (event.seconds % 5 == 0) {
                    player.damage(1.0);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, false, true));
                    player.sendMessage(prefix() + ChatColor.RED + "The blizzard is freezing you! Return to the safe zone.");
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0, 1, 0), 24, .7, .8, .7, .04);
                }
            }
        }
        List<Player> survivors = participants().stream().filter(p -> p.getWorld().equals(event.world)).toList();
        if (survivors.size() == 1) win(event, survivors.get(0).getUniqueId(), "Last survivor of the Whiteout");
        else if (survivors.isEmpty() && event.seconds > 1) win(event, leader(event.points), "Longest survival time inside the Whiteout safe zone");
    }

    /** Checks candidate and final chest/meteor positions against the actual, current border. */
    private Location randomSurfaceLocation(Location origin, int radius) {
        return randomSurfaceLocation(origin, radius, 28);
    }

    private Location randomSurfaceLocation(Location origin, int radius, int samples) {
        World world = origin.getWorld();
        WorldBorder border = world.getWorldBorder();
        double half = border.getSize() / 2.0;
        Location center = border.getCenter();
        double margin = Math.min(8.0, Math.max(1.0, half / 5.0));
        double minX = center.getX() - half + margin, maxX = center.getX() + half - margin;
        double minZ = center.getZ() - half + margin, maxZ = center.getZ() + half - margin;
        if (minX >= maxX || minZ >= maxZ) return null;
        for (int i = 0; i < Math.max(1, samples); i++) {
            int x = (int) Math.floor(Math.max(minX, Math.min(maxX, origin.getX() + random.nextInt(radius * 2 + 1) - radius)));
            int z = (int) Math.floor(Math.max(minZ, Math.min(maxZ, origin.getZ() + random.nextInt(radius * 2 + 1) - radius)));
            Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (!ground.getType().isSolid()) continue;
            Location p = new Location(world, x + .5, ground.getY() + 1, z + .5);
            if (!border.isInside(p) || !border.isInside(p.clone().add(3, 0, 3))
                    || !border.isInside(p.clone().add(-3, 0, -3))) continue;
            if (!p.getBlock().isPassable() || !p.clone().add(0, 1, 0).getBlock().isPassable()) continue;
            if (ground.getY() < world.getMinHeight() + 2 || ground.getY() > world.getMaxHeight() - 7) continue;
            return p;
        }
        return null;
    }

    // -----------------------------------------------------------------
    // Frost Bingo: every participant receives an individually randomized
    // 5x5 quest card. The first player to finish any complete row, column,
    // or either diagonal receives the selected event reward.
    // -----------------------------------------------------------------
    private List<BingoQuest> bingoQuestPool() {
        List<BingoQuest> quests = new ArrayList<>();
        addBingoQuest(quests, BingoKind.BREAK, Material.OAK_LOG, 8, "Break 8 Oak Logs");
        addBingoQuest(quests, BingoKind.BREAK, Material.BIRCH_LOG, 8, "Break 8 Birch Logs");
        addBingoQuest(quests, BingoKind.BREAK, Material.SPRUCE_LOG, 8, "Break 8 Spruce Logs");
        addBingoQuest(quests, BingoKind.BREAK, Material.STONE, 12, "Break 12 Stone");
        addBingoQuest(quests, BingoKind.BREAK, Material.COBBLED_DEEPSLATE, 10, "Mine 10 Cobbled Deepslate");
        addBingoQuest(quests, BingoKind.BREAK, Material.COAL_ORE, 4, "Mine 4 Coal Ore");
        addBingoQuest(quests, BingoKind.BREAK, Material.DEEPSLATE_COAL_ORE, 3, "Mine 3 Deepslate Coal");
        addBingoQuest(quests, BingoKind.BREAK, Material.IRON_ORE, 4, "Mine 4 Iron Ore");
        addBingoQuest(quests, BingoKind.BREAK, Material.DEEPSLATE_IRON_ORE, 3, "Mine 3 Deepslate Iron");
        addBingoQuest(quests, BingoKind.BREAK, Material.COPPER_ORE, 4, "Mine 4 Copper Ore");
        addBingoQuest(quests, BingoKind.BREAK, Material.GOLD_ORE, 2, "Mine 2 Gold Ore");
        addBingoQuest(quests, BingoKind.BREAK, Material.REDSTONE_ORE, 2, "Mine 2 Redstone Ore");
        addBingoQuest(quests, BingoKind.BREAK, Material.LAPIS_ORE, 2, "Mine 2 Lapis Ore");
        addBingoQuest(quests, BingoKind.BREAK, Material.DIAMOND_ORE, 1, "Mine 1 Diamond Ore");
        addBingoQuest(quests, BingoKind.PICKUP, Material.COBBLESTONE, 16, "Collect 16 Cobblestone");
        addBingoQuest(quests, BingoKind.PICKUP, Material.RAW_IRON, 8, "Collect 8 Raw Iron");
        addBingoQuest(quests, BingoKind.PICKUP, Material.RAW_COPPER, 8, "Collect 8 Raw Copper");
        addBingoQuest(quests, BingoKind.PICKUP, Material.COAL, 8, "Collect 8 Coal");
        addBingoQuest(quests, BingoKind.PICKUP, Material.DIAMOND, 2, "Collect 2 Diamonds");
        addBingoQuest(quests, BingoKind.PICKUP, Material.REDSTONE, 8, "Collect 8 Redstone");
        addBingoQuest(quests, BingoKind.PICKUP, Material.LAPIS_LAZULI, 8, "Collect 8 Lapis Lazuli");
        addBingoQuest(quests, BingoKind.PICKUP, Material.EMERALD, 1, "Collect 1 Emerald");
        addBingoQuest(quests, BingoKind.PICKUP, Material.WHEAT, 12, "Collect 12 Wheat");
        addBingoQuest(quests, BingoKind.PICKUP, Material.CARROT, 8, "Collect 8 Carrots");
        addBingoQuest(quests, BingoKind.PICKUP, Material.POTATO, 8, "Collect 8 Potatoes");
        addBingoQuest(quests, BingoKind.PICKUP, Material.SUGAR_CANE, 12, "Collect 12 Sugar Cane");
        addBingoQuest(quests, BingoKind.PICKUP, Material.LEATHER, 4, "Collect 4 Leather");
        addBingoQuest(quests, BingoKind.PICKUP, Material.FEATHER, 4, "Collect 4 Feathers");
        addBingoQuest(quests, BingoKind.PICKUP, Material.BONE, 4, "Collect 4 Bones");
        addBingoQuest(quests, BingoKind.PICKUP, Material.STRING, 6, "Collect 6 String");
        addBingoQuest(quests, BingoKind.CRAFT, Material.OAK_PLANKS, 16, "Craft 16 Oak Planks");
        addBingoQuest(quests, BingoKind.CRAFT, Material.TORCH, 8, "Craft 8 Torches");
        addBingoQuest(quests, BingoKind.CRAFT, Material.BREAD, 3, "Craft 3 Bread");
        addBingoQuest(quests, BingoKind.SMELT, Material.GLASS, 8, "Smelt 8 Glass");
        addBingoQuest(quests, BingoKind.SMELT, Material.IRON_INGOT, 8, "Smelt 8 Iron Ingots");
        addBingoQuest(quests, BingoKind.SMELT, Material.GOLD_INGOT, 4, "Smelt 4 Gold Ingots");
        addBingoQuest(quests, BingoKind.CRAFT, Material.SHIELD, 1, "Craft a Shield");
        addBingoQuest(quests, BingoKind.CRAFT, Material.BUCKET, 1, "Craft a Bucket");
        addBingoQuest(quests, BingoKind.CRAFT, Material.BOW, 1, "Craft a Bow");
        addBingoQuest(quests, BingoKind.CRAFT, Material.IRON_PICKAXE, 1, "Craft an Iron Pickaxe");
        addBingoQuest(quests, BingoKind.CRAFT, Material.FURNACE, 1, "Craft a Furnace");
        addBingoQuest(quests, BingoKind.CRAFT, Material.ARROW, 8, "Craft 8 Arrows");
        addBingoQuest(quests, BingoKind.KILL, "ZOMBIE", 3, "Defeat 3 Zombies");
        addBingoQuest(quests, BingoKind.KILL, "SKELETON", 2, "Defeat 2 Skeletons");
        addBingoQuest(quests, BingoKind.KILL, "CREEPER", 1, "Defeat a Creeper");
        addBingoQuest(quests, BingoKind.KILL, "SPIDER", 2, "Defeat 2 Spiders");
        addBingoQuest(quests, BingoKind.KILL, "DROWNED", 1, "Defeat a Drowned");
        addBingoQuest(quests, BingoKind.KILL, "ENDERMAN", 1, "Defeat an Enderman");
        addBingoQuest(quests, BingoKind.KILL, "WITCH", 1, "Defeat a Witch");
        addBingoQuest(quests, BingoKind.KILL, "SLIME", 2, "Defeat 2 Slimes");
        return quests;
    }

    private void addBingoQuest(List<BingoQuest> quests, BingoKind kind, Material target, int amount, String description) {
        quests.add(new BingoQuest(kind, target.name(), amount, description));
    }

    private void addBingoQuest(List<BingoQuest> quests, BingoKind kind, String target, int amount, String description) {
        quests.add(new BingoQuest(kind, target, amount, description));
    }

    private void startBingo(Active event, List<Player> players) {
        for (Player player : players) assignBingoBoard(event, player);
        announce("FROST BINGO! Everyone has a different randomized 5x5 quest book. Complete any full row, column, or either diagonal to win the selected prize!");
        announce("Quest progress only counts after the event starts. Your Bingo Book updates as you complete tasks.");
    }

    private void assignBingoBoard(Active event, Player player) {
        if (active != event || event.bingoBoards.containsKey(player.getUniqueId())) return;
        List<BingoQuest> pool = bingoQuestPool();
        java.util.Collections.shuffle(pool, random);
        List<BingoCell> board = pool.subList(0, 25).stream().map(BingoCell::new).toList();
        event.bingoBoards.put(player.getUniqueId(), new ArrayList<>(board));
        ItemStack book = createBingoBook(event, board);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(book);
        for (ItemStack leftover : leftovers.values()) player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        player.sendMessage(prefix() + ChatColor.AQUA + "Your personal 5x5 Frost Bingo card is in your inventory. First complete row, column, or diagonal wins!");
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, .8f, 1.1f);
    }

    private ItemStack createBingoBook(Active event, List<BingoCell> board) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("Frost Bingo");
        meta.setAuthor("Frost Events");
        meta.setDisplayName(ChatColor.AQUA + "Frost Bingo 5x5");
        meta.getPersistentDataContainer().set(bingoBookMarker, PersistentDataType.STRING, event.token.toString());
        applyBingoPages(meta, board);
        book.setItemMeta(meta);
        return book;
    }

    private void applyBingoPages(BookMeta meta, List<BingoCell> board) {
        List<String> pages = new ArrayList<>();
        for (int row = 0; row < 5; row++) {
            StringBuilder page = new StringBuilder("FROST BINGO — 5x5\nRow ").append(row + 1)
                    .append(" of 5\nWin with a full row, column, or diagonal.\n\n");
            for (int col = 0; col < 5; col++) {
                BingoCell cell = board.get(row * 5 + col);
                page.append(cell.completed ? "[✓] " : "[ ] ").append(cell.quest.description);
                if (!cell.completed) page.append(" (").append(Math.min(cell.progress, cell.quest.amount))
                        .append('/').append(cell.quest.amount).append(')');
                page.append('\n');
            }
            pages.add(page.toString());
        }
        meta.setPages(pages);
    }

    private boolean isBingoBook(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(bingoBookMarker, PersistentDataType.STRING);
    }

    private void updateBingoBook(Active event, Player player, List<BingoCell> board) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (!isBingoBook(item)) continue;
            BookMeta meta = (BookMeta) item.getItemMeta();
            applyBingoPages(meta, board);
            item.setItemMeta(meta);
        }
        if (isBingoBook(player.getItemOnCursor())) {
            ItemStack cursor = player.getItemOnCursor();
            BookMeta meta = (BookMeta) cursor.getItemMeta();
            applyBingoPages(meta, board);
            cursor.setItemMeta(meta);
            player.setItemOnCursor(cursor);
        }
        player.updateInventory();
    }

    private void progressBingo(Player player, BingoKind kind, String target, int amount) {
        Active event = active;
        if (event == null || !event.type.equals("bingo") || amount <= 0 || !eligible(player)) return;
        List<BingoCell> board = event.bingoBoards.get(player.getUniqueId());
        if (board == null) return;
        boolean changed = false;
        for (BingoCell cell : board) {
            if (cell.completed || cell.quest.kind != kind || !cell.quest.target.equals(target)) continue;
            cell.progress = Math.min(cell.quest.amount, cell.progress + amount);
            if (cell.progress >= cell.quest.amount) {
                cell.completed = true;
                changed = true;
                player.sendMessage(prefix() + ChatColor.GREEN + "Bingo quest complete: " + cell.quest.description + "!");
                player.playSound(player.getLocation(), Sound.UI_TOAST_IN, .7f, 1.3f);
            } else changed = true;
        }
        if (!changed) return;
        int completed = (int) board.stream().filter(cell -> cell.completed).count();
        event.points.put(player.getUniqueId(), completed);
        updateBingoBook(event, player, board);
        player.sendActionBar(Component.text("Frost Bingo | " + completed + "/25 quests | Complete a full row, column, or diagonal!"));
        String line = completedBingoLine(board);
        if (line != null && active == event) win(event, player.getUniqueId(), "Completed a " + line);
    }

    private String completedBingoLine(List<BingoCell> board) {
        for (int row = 0; row < 5; row++) {
            boolean full = true;
            for (int col = 0; col < 5; col++) full &= board.get(row * 5 + col).completed;
            if (full) return "row " + (row + 1);
        }
        for (int col = 0; col < 5; col++) {
            boolean full = true;
            for (int row = 0; row < 5; row++) full &= board.get(row * 5 + col).completed;
            if (full) return "column " + (col + 1);
        }
        boolean firstDiagonal = true, secondDiagonal = true;
        for (int index = 0; index < 5; index++) {
            firstDiagonal &= board.get(index * 5 + index).completed;
            secondDiagonal &= board.get(index * 5 + (4 - index)).completed;
        }
        if (firstDiagonal) return "top-left to bottom-right diagonal";
        if (secondDiagonal) return "top-right to bottom-left diagonal";
        return null;
    }

    private void tickBingo(Active event) {
        for (Player player : participants()) {
            List<BingoCell> board = event.bingoBoards.get(player.getUniqueId());
            if (board == null) continue;
            int completed = (int) board.stream().filter(cell -> cell.completed).count();
            if (event.seconds % 10 == 0) player.sendActionBar(Component.text(
                    "Frost Bingo | " + completed + "/25 quests | First full row/column/diagonal wins"));
        }
    }

    private void removeBingoBooks(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++)
            if (isBingoBook(player.getInventory().getItem(slot))) player.getInventory().setItem(slot, null);
        if (isBingoBook(player.getItemOnCursor())) player.setItemOnCursor(null);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBingoBlockBreak(BlockBreakEvent event) {
        if (active == null || !active.type.equals("bingo")) return;
        progressBingo(event.getPlayer(), BingoKind.BREAK, event.getBlock().getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBingoItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || active == null || !active.type.equals("bingo")) return;
        ItemStack pickedUp = event.getItem().getItemStack();
        progressBingo(player, BingoKind.PICKUP, pickedUp.getType().name(), pickedUp.getAmount());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBingoCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || active == null || !active.type.equals("bingo")) return;
        ItemStack result = event.getRecipe().getResult();
        if (result != null) {
            int crafts = 1;
            if (event.isShiftClick() && event.getInventory() instanceof org.bukkit.inventory.CraftingInventory crafting) {
                int possible = Integer.MAX_VALUE;
                for (ItemStack ingredient : crafting.getMatrix()) {
                    if (ingredient == null || ingredient.getType().isAir()) continue;
                    possible = Math.min(possible, ingredient.getAmount());
                }
                if (possible != Integer.MAX_VALUE) crafts = Math.max(1, possible);
            }
            progressBingo(player, BingoKind.CRAFT, result.getType().name(), result.getAmount() * crafts);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBingoSmelt(FurnaceExtractEvent event) {
        if (active == null || !active.type.equals("bingo")) return;
        Player player = event.getPlayer();
        progressBingo(player, BingoKind.SMELT, event.getItemType().name(), event.getItemAmount());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBingoMobKill(EntityDeathEvent event) {
        if (active == null || !active.type.equals("bingo")) return;
        Player killer = event.getEntity().getKiller();
        if (killer != null) progressBingo(killer, BingoKind.KILL, event.getEntityType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void protectBingoBooksFromStorage(InventoryClickEvent event) {
        if (active == null || !active.type.equals("bingo")
                || event.getView().getTopInventory() instanceof org.bukkit.inventory.PlayerInventory) return;
        boolean cursor = isBingoBook(event.getCursor());
        boolean top = event.getRawSlot() >= 0 && event.getRawSlot() < event.getView().getTopInventory().getSize()
                && isBingoBook(event.getCurrentItem());
        boolean shift = event.isShiftClick() && isBingoBook(event.getCurrentItem());
        boolean hotbar = event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                && event.getHotbarButton() >= 0 && isBingoBook(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
        if (cursor || top || shift || hotbar) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void protectBingoBooksFromStorageDrag(InventoryDragEvent event) {
        if (active == null || !active.type.equals("bingo")) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)
                && (isBingoBook(event.getOldCursor()) || event.getNewItems().values().stream().anyMatch(this::isBingoBook)))
            event.setCancelled(true);
    }

    private int meteorRadius() {
        return Math.max(4, Math.min(8, configuredInt("meteor-impact-radius-blocks", 7)));
    }

    /** No player builds, farms, trees, water, or tile entities are overwritten.
     *  The entire crater footprint, not just its center, must be within the border. */
    private boolean naturalMeteorSite(Location center) {
        World world = center.getWorld();
        WorldBorder border = world.getWorldBorder();
        int radius = meteorRadius();
        int x = center.getBlockX(), z = center.getBlockZ();
        int centerY = center.getBlockY() - 1;
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius + 1) continue;
                Location at = new Location(world, x + dx + .5, centerY, z + dz + .5);
                if (!border.isInside(at)) return false;
                if (distance > radius + .1) continue;
                Block ground = world.getHighestBlockAt(x + dx, z + dz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                if (Math.abs(ground.getY() - centerY) > 2) return false;
                Material material = ground.getType();
                if (!(material == Material.GRASS_BLOCK || material == Material.DIRT
                        || material == Material.COARSE_DIRT || material == Material.PODZOL
                        || material == Material.STONE || material == Material.ANDESITE
                        || material == Material.DIORITE || material == Material.GRANITE
                        || material == Material.TUFF || material == Material.MOSS_BLOCK
                        || material == Material.MYCELIUM || material == Material.SNOW_BLOCK
                        || material == Material.SAND || material == Material.RED_SAND
                        || material == Material.GRAVEL || material == Material.MUD)) return false;
                // Don't overwrite tree leaves, walls, roofs or other player constructions above the ground.
                for (int above = 1; above <= 7; above++) {
                    Material m = ground.getRelative(0, above, 0).getType();
                    if (!m.isAir() && m != Material.SHORT_GRASS && m != Material.TALL_GRASS
                            && m != Material.FERN && m != Material.LARGE_FERN
                            && m != Material.SNOW) return false;
                }
            }
        }
        return centerY - 3 > world.getMinHeight() && centerY + 9 < world.getMaxHeight();
    }

    private Location meteorSurfaceLocation(Location origin) {
        World world = origin.getWorld();
        WorldBorder border = world.getWorldBorder();
        double half = border.getSize() / 2.0;
        Location center = border.getCenter();
        // A 2,500 x 2,500 block square centered on absolute world (0, 0).
        // Intersect it with the current border, leaving room for the crater
        // and the meteor's diagonal approach. Never clamp to an edge.
        double radius = Math.max(250.0,
                Math.min(1250.0, configuredDouble("meteor-area-side-blocks", 2500.0) / 2.0));
        double margin = meteorRadius() + 24.0;
        double minX = Math.max(-radius + margin, center.getX() - half + margin);
        double maxX = Math.min( radius - margin, center.getX() + half - margin);
        double minZ = Math.max(-radius + margin, center.getZ() - half + margin);
        double maxZ = Math.min( radius - margin, center.getZ() + half - margin);
        if (minX >= maxX || minZ >= maxZ) return null;
        double minHostDistance = Math.max(0.0,
                configuredDouble("meteor-min-host-distance-blocks", 325.0));
        for (int i = 0; i < 500; i++) {
            double x = minX + random.nextDouble() * (maxX - minX);
            double z = minZ + random.nextDouble() * (maxZ - minZ);
            double dx = x - origin.getX(), dz = z - origin.getZ();
            if (dx * dx + dz * dz < minHostDistance * minHostDistance) continue;
            Block ground = world.getHighestBlockAt((int)Math.floor(x), (int)Math.floor(z),
                    HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Location location = new Location(world, ground.getX() + .5,
                    ground.getY() + 1, ground.getZ() + .5);
            if (naturalMeteorSite(location)) return location;
        }
        return null;
    }

    private BlockDisplay newMeteorDisplay(Active event, Location at, Material material, float size) {
        BlockDisplay display = event.world.spawn(at, BlockDisplay.class);
        display.setBlock(material.createBlockData());
        display.setTransformation(new Transformation(
                new Vector3f(-size / 2f, -size / 2f, -size / 2f),
                new Quaternionf(), new Vector3f(size, size, size), new Quaternionf()));
        // Zero is deliberate. Repeated interpolated teleports can be perpetually
        // restarted by newer clients, leaving a large display apparently frozen.
        // Our explicit server-side keyframes are visually reliable in 1.21.11.
        display.setTeleportDuration(0);
        display.setViewRange(2.0f);
        event.visuals.add(display);
        return display;
    }

    private void meteorEdit(Active event, Block block, Material material) {
        if (block.getType() == material) return;
        event.impactOriginal.putIfAbsent(block, block.getBlockData().clone());
        block.setType(material, false);
        event.impactPlaced.put(block, block.getBlockData().clone());
    }

    /** A real, block-built shallow crater with a sunken reward chest, blue-ice
     *  shockwave rings and tall uneven crystal spikes. Restored when event ends. */
    private boolean meteorImpact(Active event, Location landing) {
        if (!naturalMeteorSite(landing)) return false;
        int radius = meteorRadius();
        World world = landing.getWorld();
        int x = landing.getBlockX(), z = landing.getBlockZ();
        int floorY = landing.getBlockY() - 3;
        int side = radius * 2 + 1;
        int[][] surface = new int[side][side];
        // Snapshot terrain height before ANY edits, so later columns aren't affected.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                surface[dx + radius][dz + radius] = world.getHighestBlockAt(x + dx, z + dz,
                        HeightMap.MOTION_BLOCKING_NO_LEAVES).getY();
            }
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + .12) continue;
                int wx = x + dx, wz = z + dz;
                int groundY = surface[dx + radius][dz + radius];
                int pattern = Math.floorMod(wx * 31 + wz * 17, 11);
                if (d <= 2.6) {
                    // Carve the center shallowly and fill its floor with ice.
                    for (int y = floorY + 1; y <= groundY + 2; y++)
                        meteorEdit(event, world.getBlockAt(wx, y, wz), Material.AIR);
                    meteorEdit(event, world.getBlockAt(wx, floorY, wz),
                            pattern % 3 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE);
                } else if (d <= 4.0) {
                    meteorEdit(event, world.getBlockAt(wx, groundY, wz),
                            pattern % 4 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE);
                } else if (pattern < 7 || Math.abs(dx) <= 1 || Math.abs(dz) <= 1 || Math.abs(dx) == Math.abs(dz)) {
                    // Jagged ice cracks radiating away from the impact.
                    meteorEdit(event, world.getBlockAt(wx, groundY, wz),
                            pattern % 3 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE);
                    if (d >= 4.8 && d < radius - .2 && pattern % 3 == 0)
                        meteorEdit(event, world.getBlockAt(wx, groundY + 1, wz), Material.BLUE_ICE);
                }
            }
        }
        // Towering shards encircle (but never cover) the central chest.
        for (int n = 0; n < 12; n++) {
            double angle = n * (Math.PI * 2 / 12);
            double distance = 4.2 + (n % 3) * .7;
            int dx = (int)Math.round(Math.cos(angle) * distance);
            int dz = (int)Math.round(Math.sin(angle) * distance);
            if (Math.abs(dx) > radius || Math.abs(dz) > radius) continue;
            int groundY = surface[dx + radius][dz + radius];
            int height = 2 + (n * 7 % 5);
            for (int h = 1; h <= height; h++) {
                meteorEdit(event, world.getBlockAt(x + dx, groundY + h, z + dz),
                        h == height ? Material.ICE : (n % 3 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE));
            }
        }
        // The chest sits inside the crater, rather than appearing alone on the grass.
        event.objective = new Location(world, x + .5, floorY + 1, z + .5);
        return placeObjectiveChest(event);
    }

    // Minecraft unloads unobserved chunks, including chunks containing display
    // entities. A meteor can spawn hundreds of blocks from all players, so it must
    // hold chunk tickets for the whole approach and crater until event cleanup.
    private void keepMeteorChunksLoaded(Active event, Location landing) {
        int r = meteorRadius() + 5;
        int minX = landing.getBlockX() - 11 - r;
        int maxX = landing.getBlockX() + r;
        int minZ = landing.getBlockZ() - 9 - r;
        int maxZ = landing.getBlockZ() + r;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                if (event.world.addPluginChunkTicket(cx, cz, plugin)) {
                    long packed = (((long) cx) << 32) | (cz & 0xffffffffL);
                    event.meteorChunkTickets.add(packed);
                }
            }
        }
    }

    private Location meteorFlightPosition(Location landing, double altitude, double progress) {
        // Accelerate for the drop: one continuous path, independent of any client
        // interpolation or the display entities' own movement state.
        double eased = progress * progress;
        return landing.clone().add(-11.0 * (1.0 - eased),
                altitude * (1.0 - eased), -9.0 * (1.0 - eased));
    }

    private void removeMeteorVisuals(Active event, List<BlockDisplay> parts) {
        for (BlockDisplay part : parts) {
            if (part != null && part.isValid()) part.remove();
            event.visuals.remove(part);
        }
        parts.clear();
    }

    private void finishMeteorLanding(Active event, Location landing,
                                     List<BlockDisplay> parts) {
        // The countdown—not a display entity's lifetime—controls the impact.
        removeMeteorVisuals(event, parts);
        if (active != event) return;
        if (!event.world.getWorldBorder().isInside(landing)
                || !meteorImpact(event, landing)) {
            announce(ChatColor.RED + "The meteor could not land safely. Event cancelled.");
            closeEvent();
            return;
        }
        event.meteorLanded = true;
        Location blast = event.objective.clone().add(0, 2, 0);
        event.world.spawnParticle(Particle.EXPLOSION, blast, 16, 4.0, 1.8, 4.0);
        event.world.spawnParticle(Particle.SNOWFLAKE, blast, 280, 6.0, 2.0, 6.0, .14);
        event.world.spawnParticle(Particle.BLOCK, blast, 150, 4.0, 2.5, 4.0,
                .32, Material.BLUE_ICE.createBlockData());
        event.world.playSound(blast, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, .55f);
        event.world.playSound(blast, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.7f, .65f);
        event.world.strikeLightningEffect(blast);
        String impactMessage = "BOOM! Frozen Meteor struck the ground! Find the chest INSIDE the icy crater and right-click it to win!";
        if (plugin.getConfig().getBoolean("events.meteor-reveal-exact-coordinates", false))
            impactMessage += " Impact: " + loc(event.objective) + ".";
        announce(ChatColor.GOLD + impactMessage);
    }

    private void startMeteor(Active event) {
        final Location landing = event.objective.clone();
        double skyY = Math.min(event.world.getMaxHeight() - 9, landing.getY() + 65);
        double altitude = Math.max(12, skyY - landing.getY());
        final int flightTicks = Math.max(60, Math.min(240,
                configuredInt("meteor-flight-seconds", 7) * 20));
        // Preload BEFORE creating entities so they cannot despawn during flight.
        keepMeteorChunksLoaded(event, landing);
        // Nine uniquely sized blocks make up the icy meteor core and fragments.
        final double[][] fragments = {
                {0, 0, 0, 5.0}, {2.4, 1.6, .8, 2.3}, {-2.2, .7, -.7, 2.2},
                {1.3, -2.2, 1.6, 1.8}, {-1.5, 2.5, 1.1, 1.8},
                {1.3, 1.2, -2.4, 1.8}, {-2.0, -1.8, -1.4, 1.7},
                {0, -3.0, 0, 1.5}, {0, 3.0, .4, 1.5}
        };
        final List<BlockDisplay> parts = new ArrayList<>();
        // Previously every fragment spawned at the same location until a later
        // teleport arrived. Build the meteor correctly at its initial position.
        Location initial = meteorFlightPosition(landing, altitude, 0.0);
        for (int i = 0; i < fragments.length; i++) {
            double[] f = fragments[i];
            Material material = i % 4 == 0 ? Material.BLUE_ICE
                    : i % 3 == 0 ? Material.ICE : Material.PACKED_ICE;
            parts.add(newMeteorDisplay(event, initial.clone().add(f[0], f[1], f[2]),
                    material, (float) f[3]));
        }
        String meteorStart = "A GIANT FROZEN METEOR IS FALLING in " + event.world.getName()
                + "! Search within the 2,500 x 2,500 region centered at X=0, Z=0!";
        if (plugin.getConfig().getBoolean("events.meteor-reveal-exact-coordinates", false))
            meteorStart += " Impact near X=" + landing.getBlockX() + ", Z=" + landing.getBlockZ() + ".";
        announce(ChatColor.BOLD + meteorStart);
        // Teleport every two server ticks with 0 teleport interpolation.
        // The visual is therefore at a *different real position* every frame,
        // and cannot get frozen waiting for an interpolation to complete.
        new BukkitRunnable() {
            int elapsedTicks = 0;
            @Override public void run() {
                if (active != event) { cancel(); return; }
                elapsedTicks += 2;
                double progress = Math.min(1.0, elapsedTicks / (double) flightTicks);
                Location point = meteorFlightPosition(landing, altitude, progress);
                WorldBorder border = event.world.getWorldBorder();
                if (!border.isInside(point) || !border.isInside(landing)) {
                    announce("The world border moved; Frozen Meteor was cancelled safely.");
                    closeEvent(); cancel(); return;
                }
                for (int i = 0; i < parts.size(); i++) {
                    BlockDisplay part = parts.get(i);
                    double[] f = fragments[i];
                    Location spot = point.clone().add(f[0], f[1], f[2]);
                    if (part != null && part.isValid()) {
                        part.teleport(spot);
                    }
                    // A missing or removed display must never prevent the
                    // remaining meteor animation or the eventual impact.
                }
                event.world.spawnParticle(Particle.SNOWFLAKE, point, 35,
                        2.0, 2.0, 2.0, .1);
                event.world.spawnParticle(Particle.CLOUD, point, 12,
                        1.0, 1.0, 1.0, .06);
                if (elapsedTicks % 8 == 0)
                    event.world.spawnParticle(Particle.END_ROD, point, 18,
                            1.5, 1.2, 1.5, .05);
                if (elapsedTicks % 20 == 0)
                    event.world.playSound(point, Sound.ENTITY_BLAZE_SHOOT, 1.5f, .55f);
                if (elapsedTicks >= flightTicks) {
                    finishMeteorLanding(event, landing, parts);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    // -------------------- Treasure Hunt: surface shrines + underground vaults --------------------
    private boolean treasureNatural(Material type) {
        return type.isAir() || type == Material.STONE || type == Material.DEEPSLATE
                || type == Material.TUFF || type == Material.ANDESITE
                || type == Material.DIORITE || type == Material.GRANITE
                || type == Material.DIRT || type == Material.GRASS_BLOCK
                || type == Material.COARSE_DIRT || type == Material.PODZOL
                || type == Material.MOSS_BLOCK || type == Material.GRAVEL
                || type == Material.CALCITE || type == Material.DRIPSTONE_BLOCK
                || type == Material.SNOW || type == Material.SNOW_BLOCK
                || type == Material.SHORT_GRASS || type == Material.TALL_GRASS
                || type == Material.FERN || type == Material.LARGE_FERN;
    }

    private boolean treasureWritable(Block block) {
        return !(block.getState() instanceof TileState) && treasureNatural(block.getType());
    }

    // Run all protection checks before editing a single block. Reject water, lava,
    // containers, player-built blocks, and any footprint touching the world border.
    private boolean safeSurfaceTreasure(Location surface, int radius) {
        World w = surface.getWorld();
        int x = surface.getBlockX(), z = surface.getBlockZ();
        int base = surface.getBlockY() - 1;
        if (base <= w.getMinHeight() + 4 || base >= w.getMaxHeight() - 8) return false;
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            Location p = new Location(w, x + dx + .5, base + 1, z + dz + .5);
            if (!w.getWorldBorder().isInside(p)) return false;
            Block floor = w.getHighestBlockAt(x + dx, z + dz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (Math.abs(floor.getY() - base) > 1 || !treasureWritable(floor)) return false;
            for (int dy = 1; dy <= 5; dy++) {
                Block b = w.getBlockAt(x + dx, base + dy, z + dz);
                if (!b.isEmpty() && !b.isPassable()) return false;
                if (!treasureWritable(b)) return false;
            }
        }
        return true;
    }

    private Location eventAreaSurfaceLocation(World world, int halfRange) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double half = Math.max(64, Math.min(1250, halfRange));
        double margin = 8.0;
        double minX = Math.max(-half + margin, center.getX() - border.getSize() / 2.0 + margin);
        double maxX = Math.min(half - margin, center.getX() + border.getSize() / 2.0 - margin);
        double minZ = Math.max(-half + margin, center.getZ() - border.getSize() / 2.0 + margin);
        double maxZ = Math.min(half - margin, center.getZ() + border.getSize() / 2.0 - margin);
        if (minX >= maxX || minZ >= maxZ) return null;
        for (int i = 0; i < 80; i++) {
            int x = (int) Math.floor(minX + random.nextDouble() * (maxX - minX));
            int z = (int) Math.floor(minZ + random.nextDouble() * (maxZ - minZ));
            Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (!ground.getType().isSolid() || ground.getY() < world.getMinHeight() + 8
                    || ground.getY() > world.getMaxHeight() - 10) continue;
            Location candidate = new Location(world, x + .5, ground.getY() + 1, z + .5);
            if (border.isInside(candidate)) return candidate;
        }
        return null;
    }

    private boolean startTreasure(Active event) {
        int range = Math.max(24, (int) (configuredDouble("meteor-area-side-blocks", 2500.0) / 2.0));
        double undergroundChance = Math.max(0.0, Math.min(100.0,
                configuredDouble("treasure-underground-chance-percent", 45.0)));
        // Alternate surface and underground trials; chance controls which we prefer.
        boolean wantUnderground = random.nextDouble() * 100.0 < undergroundChance;
        for (int attempt = 0; attempt < 35; attempt++) {
            Location surface = eventAreaSurfaceLocation(event.world, range);
            if (surface == null) continue;
            boolean underground = attempt < 23 ? wantUnderground : !wantUnderground;
            if (underground) {
                int groundY = surface.getBlockY() - 1;
                int depth = 12 + random.nextInt(19); // 12-30 blocks under the surface
                int chestY = groundY - depth;
                if (!safeUndergroundTreasure(surface, chestY)) continue;
                buildUndergroundTreasure(event, surface, chestY);
                event.treasureUnderground = true;
                event.treasureDepth = depth;
                return placeObjectiveChest(event);
            } else if (safeSurfaceTreasure(surface, 3)) {
                buildSurfaceTreasure(event, surface);
                return placeObjectiveChest(event);
            }
        }
        // Last-chance surface attempt, even if configured to prefer caves.
        for (int i = 0; i < 20; i++) {
            Location surface = eventAreaSurfaceLocation(event.world, range);
            if (surface != null && safeSurfaceTreasure(surface, 3)) {
                buildSurfaceTreasure(event, surface);
                return placeObjectiveChest(event);
            }
        }
        return false;
    }

    private void buildSurfaceTreasure(Active event, Location p) {
        World w = p.getWorld();
        int x = p.getBlockX(), y = p.getBlockY(), z = p.getBlockZ();
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                if (Math.abs(dx) + Math.abs(dz) < 6)
                    meteorEdit(event, w.getBlockAt(x + dx, y - 1, z + dz), Material.PACKED_ICE);
            } else {
                meteorEdit(event, w.getBlockAt(x + dx, y - 1, z + dz),
                        (Math.abs(dx) + Math.abs(dz)) % 4 == 0 ? Material.BLUE_ICE : Material.STONE_BRICKS);
            }
        }
        for (int dx : new int[]{-2, 2}) for (int dz : new int[]{-2, 2}) {
            for (int height = 0; height < 3; height++) {
                meteorEdit(event, w.getBlockAt(x + dx, y + height, z + dz),
                        height == 2 ? Material.SEA_LANTERN : Material.BLUE_ICE);
            }
        }
        for (int dx = -1; dx <= 1; dx++)
            meteorEdit(event, w.getBlockAt(x + dx, y + 3, z + 2), Material.PACKED_ICE);
        event.objective = new Location(w, x + .5, y, z + .5);
        event.treasureUnderground = false;
        w.spawnParticle(Particle.SNOWFLAKE, event.objective.clone().add(0, 1.5, 0), 60, 2, 2, 2, .04);
    }

    private boolean safeUndergroundTreasure(Location surface, int chestY) {
        World w = surface.getWorld();
        int x = surface.getBlockX(), z = surface.getBlockZ(), groundY = surface.getBlockY() - 1;
        if (chestY < w.getMinHeight() + 6 || chestY + 5 >= groundY) return false;
        // Check the chamber and ladder shaft before carving. Natural blocks only.
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (!w.getWorldBorder().isInside(new Location(w, x + dx + .5, chestY, z + dz + .5))) return false;
            for (int dy = -1; dy <= 4; dy++)
                if (!treasureWritable(w.getBlockAt(x + dx, chestY + dy, z + dz))) return false;
        }
        // Horizontal passage and the shaft's support walls must also be safe.
        for (int dx = 3; dx <= 5; dx++) for (int dy = 0; dy <= 2; dy++)
            if (!treasureWritable(w.getBlockAt(x + dx, chestY + dy, z))) return false;
        for (int y = chestY; y <= groundY + 2; y++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = w.getBlockAt(x + 5, y, z + dz);
                if (!treasureWritable(block)) return false;
                if (!w.getWorldBorder().isInside(block.getLocation().add(.5, .5, .5))) return false;
            }
        }
        // Leave enough free space above the visible entrance marker.
        return safeSurfaceTreasure(new Location(w, x + 5 + .5, groundY + 1, z + .5), 1);
    }

    private void buildUndergroundTreasure(Active event, Location surface, int y) {
        World w = surface.getWorld();
        int x = surface.getBlockX(), z = surface.getBlockZ();
        // Carve a 7x7 icy vault, two blocks of headroom around the central chest.
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            for (int dy = -1; dy <= 4; dy++) {
                Material material;
                if (dy == -1) material = Material.POLISHED_DEEPSLATE;
                else if (dy == 4) material = Material.PACKED_ICE;
                else if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                    material = (dy == 2 && (dx + dz) % 3 == 0)
                            ? Material.SEA_LANTERN : Material.DEEPSLATE_BRICKS;
                } else material = Material.AIR;
                meteorEdit(event, w.getBlockAt(x + dx, y + dy, z + dz), material);
            }
        }
        // Glowing central alcove and easy-to-recognize icy walls.
        for (int dx : new int[]{-2, 2}) for (int dz : new int[]{-2, 2}) {
            meteorEdit(event, w.getBlockAt(x + dx, y, z + dz), Material.BLUE_ICE);
            meteorEdit(event, w.getBlockAt(x + dx, y + 1, z + dz), Material.SEA_LANTERN);
        }
        // Surface-to-vault ladder shaft, plus a short horizontal doorway.
        int shaftX = x + 5;
        int topY = surface.getBlockY() + 1;
        for (int height = y; height <= topY; height++) {
            meteorEdit(event, w.getBlockAt(shaftX, height, z - 1), Material.DEEPSLATE_BRICKS);
            meteorEdit(event, w.getBlockAt(shaftX, height, z), Material.LADDER);
            Ladder ladder = (Ladder) Material.LADDER.createBlockData();
            ladder.setFacing(BlockFace.SOUTH); // North wall supports the ladder.
            Block ladderBlock = w.getBlockAt(shaftX, height, z);
            ladderBlock.setBlockData(ladder, false);
            event.impactPlaced.put(ladderBlock, ladder.clone());
        }
        for (int dx = 3; dx <= 4; dx++) for (int dy = 0; dy <= 2; dy++)
            meteorEdit(event, w.getBlockAt(x + dx, y + dy, z), Material.AIR);
        // Surface entrance: bright ice rim and two illuminated corner markers.
        int surfaceY = surface.getBlockY();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) continue;
            meteorEdit(event, w.getBlockAt(shaftX + dx, surfaceY - 1, z + dz),
                    (Math.abs(dx) == 1 && Math.abs(dz) == 1) ? Material.BLUE_ICE : Material.PACKED_ICE);
        }
        for (int dz : new int[]{-1, 1})
            meteorEdit(event, w.getBlockAt(shaftX - 1, surfaceY, z + dz), Material.SEA_LANTERN);
        event.treasureEntrance = new Location(w, shaftX + .5, surfaceY, z + .5);
        event.objective = new Location(w, x + .5, y, z + .5);
    }

    private boolean placeObjectiveChest(Active event) {
        Location location = event.objective;
        if (location == null || !location.getWorld().getWorldBorder().isInside(location)) return false;
        Block block = location.getBlock();
        if (!block.getType().isAir()) return false; // Never overwrite a player's structure or liquid.
        event.objectiveBlock = block;
        event.oldBlockData = block.getBlockData().clone();
        block.setType(Material.CHEST, false);
        event.world.spawnParticle(Particle.SNOWFLAKE, location.clone().add(0, .5, 0), 50, 1, 1, 1, .04);
        event.world.playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, .7f);
        return true;
    }

    private void startChosen(Active event, List<Player> players) {
        Player chosen = players.get(random.nextInt(players.size()));
        event.chosenHolder = chosen.getUniqueId();
        giveRelic(chosen, event);
        event.lastHolder = chosen.getUniqueId();
        updateChosenGlow(event, chosen);
        announce(ChatColor.GOLD + chosen.getName() + ChatColor.AQUA + " is the Chosen One!");
        announce("TAG the holder with a melee hit to steal the relic! The last holder wins.");
        announceChosenPosition(event, true);
    }
    private ItemStack createRelic(Active event) {
        ItemStack relic = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = relic.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Frost Chosen Relic");
        meta.setLore(List.of(ChatColor.GRAY + "Event " + event.token, ChatColor.GOLD + "Hold this when time expires to win!"));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(relicMarker, PersistentDataType.STRING, event.token.toString());
        relic.setItemMeta(meta);
        return relic;
    }
    private boolean isRelic(ItemStack item, Active event) {
        if (item == null || !item.hasItemMeta()) return false;
        String token = item.getItemMeta().getPersistentDataContainer().get(relicMarker, PersistentDataType.STRING);
        return token != null && token.equals(event.token.toString());
    }
    private void giveRelic(Player target, Active event) {
        Map<Integer, ItemStack> extra = target.getInventory().addItem(createRelic(event));
        if (!extra.isEmpty()) {
            // Tagging must succeed even if the new holder's inventory is full.
            ItemStack displaced = target.getInventory().getItem(8);
            if (displaced != null && !displaced.getType().isAir())
                target.getWorld().dropItemNaturally(target.getLocation(), displaced);
            target.getInventory().setItem(8, createRelic(event));
        }
    }

    // Track and restore the holder's original glowing state on handoff/cleanup.
    private void updateChosenGlow(Active event, Player holder) {
        UUID newId = holder == null ? null : holder.getUniqueId();
        if (java.util.Objects.equals(event.chosenGlowPlayer, newId)) return;
        restoreChosenGlow(event);
        if (holder != null) {
            event.chosenGlowPlayer = holder.getUniqueId();
            event.chosenOriginalGlowing = holder.isGlowing();
            holder.setGlowing(true);
        }
    }

    private void restoreChosenGlow(Active event) {
        if (event.chosenGlowPlayer == null) return;
        Player player = Bukkit.getPlayer(event.chosenGlowPlayer);
        if (player != null) player.setGlowing(event.chosenOriginalGlowing);
        event.chosenGlowPlayer = null;
    }

    private void announceChosenPosition(Active event, boolean broadcast) {
        Player holder = event.chosenHolder == null ? null : Bukkit.getPlayer(event.chosenHolder);
        if (holder == null || !holder.isOnline()) {
            event.chosenLastCoordinates = "Relic unclaimed";
            return;
        }
        Location p = holder.getLocation();
        event.chosenLastCoordinates = holder.getName() + " @ " + p.getWorld().getName() +
                " X=" + p.getBlockX() + " Y=" + p.getBlockY() + " Z=" + p.getBlockZ();
        if (broadcast) announce(ChatColor.GOLD + "Chosen One location: " + ChatColor.AQUA +
                event.chosenLastCoordinates);
    }

    private void tickChosenHeartDisplay() {
        Active event = active;
        if (event == null || !event.type.equals("chosen")) return;
        Player holder = event.chosenHolder == null ? null : Bukkit.getPlayer(event.chosenHolder);
        if (holder == null || !eligible(holder) || !holder.getUniqueId().equals(currentRelicHolder(event))) {
            if (event.chosenHeartDisplay != null && event.chosenHeartDisplay.isValid())
                event.chosenHeartDisplay.remove();
            event.chosenHeartDisplay = null;
            return;
        }
        double bob = Math.sin(System.currentTimeMillis() / 320.0) * .13;
        Location above = holder.getLocation().clone().add(0, holder.getHeight() + .72 + bob, 0);
        if (event.chosenHeartDisplay == null || !event.chosenHeartDisplay.isValid()) {
            ItemDisplay icon = holder.getWorld().spawn(above, ItemDisplay.class);
            icon.setItemStack(new ItemStack(Material.HEART_OF_THE_SEA));
            icon.setBillboard(Display.Billboard.CENTER);
            icon.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            icon.setTeleportDuration(1);
            icon.setViewRange(1.5f);
            event.visuals.add(icon);
            event.chosenHeartDisplay = icon;
        } else if (!event.chosenHeartDisplay.getWorld().equals(holder.getWorld())) {
            event.chosenHeartDisplay.remove();
            event.chosenHeartDisplay = null;
        } else event.chosenHeartDisplay.teleport(above);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChosenTag(EntityDamageByEntityEvent event) {
        Active current = active;
        if (current == null || !current.type.equals("chosen")) return;
        if (!(event.getDamager() instanceof Player tagger) ||
                !(event.getEntity() instanceof Player victim) ||
                !eligible(tagger) || !eligible(victim) || tagger.equals(victim)) return;
        UUID holder = currentRelicHolder(current);
        if (!victim.getUniqueId().equals(holder)) return;
        long now = System.currentTimeMillis();
        long immunity = Math.max(0, configuredInt("chosen-tag-protection-seconds", 2)) * 1000L;
        if (now - current.lastChosenTagMillis < immunity) {
            event.setCancelled(true); // Brief tag-back protection; no melee damage either.
            return;
        }
        // Transfer instead of forcing PvP death. This hit functions as a tag.
        event.setCancelled(true);
        removeAllRelics(current);
        giveRelic(tagger, current);
        current.chosenHolder = tagger.getUniqueId();
        current.lastHolder = current.chosenHolder;
        current.lastChosenTagMillis = now;
        updateChosenGlow(current, tagger);
        announce(ChatColor.GOLD + tagger.getName() + ChatColor.AQUA + " TAGGED " +
                victim.getName() + " and stole the Chosen Relic!");
        announceChosenPosition(current, false);
        tagger.playSound(tagger.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.25f);
    }

    private UUID currentRelicHolder(Active event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!eligible(player)) continue;
            for (ItemStack item : player.getInventory().getContents())
                if (isRelic(item, event)) return player.getUniqueId();
        }
        return null;
    }
    private void removeAllRelics(Active event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (isRelic(item, event)) player.getInventory().setItem(slot, null);
            }
        }
        for (World world : Bukkit.getWorlds()) {
            for (Item entity : world.getEntitiesByClass(Item.class)) {
                if (isRelic(entity.getItemStack(), event)) entity.remove();
            }
        }
    }

    // -------------------- Frost Titan: scripted Warden raid boss --------------------
    // Warden AI must remain OFF. A vanilla Warden has a dangerous sonic boom,
    // an extremely high natural melee hit, and its own autonomous target logic.
    // The event below fully owns movement, melee and exactly five special attacks.
    private void spawnTitan(Active event) {
        Location spot = event.objective.clone();
        Warden boss = event.world.spawn(spot, Warden.class);
        boss.setCustomName(ChatColor.AQUA + "FROST TITAN " + ChatColor.DARK_AQUA + "[RAID BOSS]");
        boss.setCustomNameVisible(true);
        boss.setAI(false);
        // Stop gravity fighting against our pathing teleports while descending.
        boss.setGravity(false);
        boss.setPersistent(true);
        boss.setRemoveWhenFarAway(false);
        boss.setCanPickupItems(false);
        // Vanilla's maximum max_health attribute value is 1,024. The raid
        // health below is independent and can safely be much higher.
        double nativeMaxHealth = 1000.0;
        if (boss.getAttribute(Attribute.MAX_HEALTH) != null)
            boss.getAttribute(Attribute.MAX_HEALTH).setBaseValue(nativeMaxHealth);
        boss.setHealth(nativeMaxHealth);
        event.titanMaxRaidHealth = Math.max(500.0, configuredDouble("titan-health", 6000.0));
        event.titanRaidHealth = event.titanMaxRaidHealth;
        event.titan = boss;
        event.bar.setColor(BarColor.PURPLE);
        announce("FROST TITAN spawned at " + loc(spot) + "! "
                + (int) event.titanMaxRaidHealth + " raid HP, five attacks, and a second phase!"
                + " Highest damage dealt wins!");
        event.world.playSound(spot, Sound.ENTITY_WITHER_SPAWN, 1.5f, .55f);
        event.world.spawnParticle(Particle.SNOWFLAKE, spot.clone().add(0, 2, 0),
                150, 3.0, 2.5, 3.0, .07);
    }

    // -------------------- Frost Wither: high-health scripted raid boss --------------------
    // The Wither has no vanilla AI or block-breaking. Its raid health is virtual,
    // allowing a configurable 10,000+ HP fight while preserving real damage scoring.
    private void spawnWither(Active event) {
        Location spot = event.objective.clone();
        Wither boss = event.world.spawn(spot.clone().add(0, 2, 0), Wither.class);
        boss.setCustomName(ChatColor.DARK_PURPLE + "FROST WITHER " + ChatColor.LIGHT_PURPLE + "[WORLD BOSS]");
        boss.setCustomNameVisible(true);
        boss.setAI(false);
        boss.setGravity(false);
        // Skip the vanilla 11-second invulnerable spawn charge/explosion.
        boss.setInvulnerableTicks(0);
        boss.setPersistent(true);
        boss.setRemoveWhenFarAway(false);
        boss.setCanPickupItems(false);
        double nativeMaxHealth = 1000.0;
        if (boss.getAttribute(Attribute.MAX_HEALTH) != null)
            boss.getAttribute(Attribute.MAX_HEALTH).setBaseValue(nativeMaxHealth);
        boss.setHealth(nativeMaxHealth);
        event.witherMaxRaidHealth = Math.max(1000.0, configuredDouble("wither-health", 15000.0));
        event.witherRaidHealth = event.witherMaxRaidHealth;
        event.wither = boss;
        event.bar.setColor(BarColor.PURPLE);
        announce(ChatColor.DARK_PURPLE + "THE FROST WITHER HAS AWAKENED" + ChatColor.AQUA + " at " + loc(spot)
                + "! " + (int) event.witherMaxRaidHealth + " RAID HP. Six unique abilities; highest damage dealer wins!");
        event.world.playSound(spot, Sound.ENTITY_WITHER_SPAWN, 2.0f, .65f);
        event.world.spawnParticle(Particle.SQUID_INK, spot.clone().add(0, 2, 0), 180, 3.5, 2.5, 3.5, .08);
        event.world.spawnParticle(Particle.SOUL, spot.clone().add(0, 2, 0), 120, 3.0, 2.0, 3.0, .06);
    }

    private boolean witherActive(Active event) {
        return active == event && !event.witherDefeated && event.wither != null
                && event.wither.isValid() && !event.wither.isDead();
    }

    private void tickWitherMovement() {
        Active event = active;
        if (event == null || !event.type.equals("wither") || !witherActive(event)) return;
        Wither boss = event.wither;
        List<Player> targets = titanPlayers(event, boss.getLocation(),
                Math.max(45.0, configuredDouble("wither-chase-range-blocks", 110.0)));
        if (targets.isEmpty()) {
            boss.getWorld().spawnParticle(Particle.SOUL, boss.getLocation(), 3, .7, .5, .7, .01);
            return;
        }
        Player target = targets.stream().min(Comparator.comparingDouble(p ->
                p.getLocation().distanceSquared(boss.getLocation()))).orElse(null);
        if (target == null) return;
        Location current = boss.getLocation();
        Vector toward = target.getLocation().add(0, 2.4, 0).toVector().subtract(current.toVector());
        double distance = toward.length();
        if (distance > 4.5) {
            Vector step = toward.normalize().multiply(Math.min(.42, distance * .025));
            Location next = current.clone().add(step);
            next.setDirection(toward);
            if (next.getBlock().isPassable() && next.clone().add(0, 1, 0).getBlock().isPassable()
                    && next.clone().add(0, 2, 0).getBlock().isPassable())
                boss.teleport(next);
        } else {
            current.setDirection(toward);
            boss.teleport(current);
        }
        if (event.seconds % 2 == 0) boss.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                boss.getLocation(), 5, .6, .5, .6, .01);
    }

    private void tickWitherRaid(Active event) {
        if (!witherActive(event)) return;
        Wither boss = event.wither;
        double fraction = event.witherRaidHealth / Math.max(1.0, event.witherMaxRaidHealth);
        if (!event.witherEnraged && fraction <= .25) {
            event.witherEnraged = true;
            event.witherNextAbility = Math.min(event.witherNextAbility, event.seconds + 2);
            announce(ChatColor.DARK_PURPLE + "THE FROST WITHER ENTERS ITS FINAL PHASE! Its attacks are faster and stronger!");
            event.world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 2.0f, .55f);
            event.world.spawnParticle(Particle.SOUL_FIRE_FLAME, boss.getLocation(), 100, 2.0, 1.5, 2.0, .05);
        }
        event.bar.setTitle(ChatColor.DARK_PURPLE + "Frost Wither " + (int) Math.ceil(event.witherRaidHealth)
                + "/" + (int) event.witherMaxRaidHealth + " RAID HP | "
                + (event.witherEnraged ? "FINAL PHASE | " : "") + Math.max(0, event.totalSeconds - event.seconds) + "s");
        event.bar.setProgress(Math.max(0, Math.min(1, fraction)));
        if (event.seconds >= event.witherNextAbility) {
            event.witherNextAbility = event.seconds + (event.witherEnraged ? 5 : 8);
            castWitherAbility(event, event.witherAttackIndex++ % 6);
        }
    }

    private List<Player> witherTargets(Active event, Location center, double radius) {
        return titanPlayers(event, center, radius).stream()
                .sorted(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(center))).toList();
    }

    private void witherWarning(Active event, String message) {
        if (!witherActive(event)) return;
        announce(ChatColor.DARK_PURPLE + "FROST WITHER: " + ChatColor.LIGHT_PURPLE + message);
        event.world.playSound(event.wither.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.5f, .8f);
        event.world.spawnParticle(Particle.SOUL, event.wither.getLocation(), 35, 1.2, 1.0, 1.2, .04);
    }

    private void shootWitherSkull(Active event, Player target, double speed) {
        if (!witherActive(event) || target == null || !target.isOnline()) return;
        Location launch = event.wither.getLocation().clone().add(0, .8, 0);
        Vector direction = target.getEyeLocation().toVector().subtract(launch.toVector());
        if (direction.lengthSquared() < .01) direction = new Vector(0, 0, 1);
        WitherSkull skull = event.world.spawn(launch, WitherSkull.class);
        skull.setShooter(event.wither);
        Vector velocity = direction.normalize().multiply(speed);
        skull.setDirection(velocity);
        skull.setVelocity(velocity);
        skull.setYield(0.0f);
        skull.setIsIncendiary(false);
        event.visuals.add(skull);
    }

    private void castWitherAbility(Active event, int ability) {
        if (!witherActive(event)) return;
        List<Player> targets = witherTargets(event, event.wither.getLocation(), 48.0);
        if (targets.isEmpty()) return;
        switch (ability) {
            case 0 -> {
                witherWarning(event, "SOULFIRE VOLLEY — dodge the seeking skulls!");
                int count = Math.min(event.witherEnraged ? 5 : 3, targets.size());
                for (int i = 0; i < count; i++) {
                    Player target = targets.get(i);
                    shootWitherSkull(event, target, event.witherEnraged ? 1.05 : .85);
                    target.sendActionBar(Component.text(ChatColor.DARK_PURPLE + "Soulfire Volley incoming! Keep moving."));
                }
            }
            case 1 -> {
                witherWarning(event, "FROST CATACLYSM — spread out before the pulse!");
                Location center = event.wither.getLocation().clone();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!witherActive(event)) return;
                    event.world.spawnParticle(Particle.EXPLOSION, center, 2, 1.2, 1.0, 1.2, .02);
                    event.world.spawnParticle(Particle.SNOWFLAKE, center, 180, 18, 2, 18, .08);
                    for (Player player : witherTargets(event, center, 20.0)) {
                        player.damage((event.witherEnraged ? 7.0 : 5.0), event.wither);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 70, 1, true, true, true));
                    }
                    event.world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, .65f);
                }, 30L);
            }
            case 2 -> {
                witherWarning(event, "SOUL DRAIN — the Wither is draining nearby players!");
                double drained = 0.0;
                for (Player player : witherTargets(event, event.wither.getLocation(), 18.0)) {
                    player.damage(event.witherEnraged ? 4.5 : 3.0, event.wither);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 50, 0, true, true, true));
                    drained += event.witherEnraged ? 3.5 : 2.0;
                    player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 18, .4, .6, .4, .03);
                }
                event.witherRaidHealth = Math.min(event.witherMaxRaidHealth, event.witherRaidHealth + drained);
                updateWitherNativeHealth(event);
            }
            case 3 -> {
                witherWarning(event, "WITHERING RAIN — watch the sky!");
                for (Player player : targets.subList(0, Math.min(targets.size(), event.witherEnraged ? 5 : 3))) {
                    Location launch = player.getLocation().clone().add(random.nextDouble() * 4 - 2, 12,
                            random.nextDouble() * 4 - 2);
                    WitherSkull skull = event.world.spawn(launch, WitherSkull.class);
                    skull.setShooter(event.wither);
                    Vector velocity = player.getLocation().toVector().subtract(launch.toVector()).normalize().multiply(.9);
                    skull.setDirection(velocity);
                    skull.setVelocity(velocity);
                    skull.setYield(0.0f);
                    skull.setIsIncendiary(false);
                    event.visuals.add(skull);
                    player.sendActionBar(Component.text(ChatColor.DARK_PURPLE + "Withering Rain above you! Move away."));
                }
            }
            case 4 -> {
                witherWarning(event, "SUMMON THE LOST — wither sentinels are joining the fight!");
                int minions = event.witherEnraged ? 5 : 3;
                for (int i = 0; i < minions; i++) {
                    double angle = Math.PI * 2 * i / minions;
                    Location spawn = event.wither.getLocation().clone().add(Math.cos(angle) * 3, -1, Math.sin(angle) * 3);
                    WitherSkeleton sentinel = event.world.spawn(spawn, WitherSkeleton.class);
                    sentinel.setCustomName(ChatColor.DARK_PURPLE + "Lost Sentinel");
                    sentinel.setCustomNameVisible(true);
                    sentinel.setRemoveWhenFarAway(false);
                    sentinel.setCanPickupItems(false);
                    if (sentinel.getEquipment() != null) {
                        sentinel.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
                        sentinel.getEquipment().setItemInMainHandDropChance(0.0f);
                        sentinel.getEquipment().setBootsDropChance(0.0f);
                        sentinel.getEquipment().setLeggingsDropChance(0.0f);
                        sentinel.getEquipment().setChestplateDropChance(0.0f);
                        sentinel.getEquipment().setHelmetDropChance(0.0f);
                    }
                    event.visuals.add(sentinel);
                }
                event.world.spawnParticle(Particle.SOUL, event.wither.getLocation(), 100, 3, 2, 3, .06);
            }
            default -> {
                witherWarning(event, "ABYSSAL NOVA — brace for the shockwave!");
                Location center = event.wither.getLocation().clone();
                for (Player player : witherTargets(event, center, 26.0)) {
                    Vector push = player.getLocation().toVector().subtract(center.toVector());
                    if (push.lengthSquared() < .01) push = new Vector(0, 0, 1);
                    push.normalize().multiply(event.witherEnraged ? 1.35 : .95).setY(.35);
                    player.setVelocity(push);
                    player.damage(event.witherEnraged ? 5.0 : 4.0, event.wither);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 35, 0, true, false, true));
                }
                event.world.spawnParticle(Particle.SONIC_BOOM, center, 1, 0, 0, 0, 0);
                event.world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, .65f);
            }
        }
    }

    private void updateWitherNativeHealth(Active event) {
        if (event.wither == null || !event.wither.isValid()) return;
        double fraction = event.witherRaidHealth / Math.max(1.0, event.witherMaxRaidHealth);
        event.wither.setHealth(Math.max(1.0, 1000.0 * fraction));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void protectWitherFromEnvironment(org.bukkit.event.entity.EntityDamageEvent event) {
        Active current = active;
        if (current == null || !current.type.equals("wither") || current.wither == null
                || !current.wither.equals(event.getEntity())) return;
        if (!(event instanceof EntityDamageByEntityEvent)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void suppressWitherBlockGrief(EntityExplodeEvent event) {
        Active current = active;
        if (current == null || !current.type.equals("wither") || current.wither == null
                || !current.wither.equals(event.getEntity())) return;
        event.blockList().clear();
        event.setYield(0.0f);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWitherDamage(EntityDamageByEntityEvent event) {
        Active current = active;
        if (current == null || !current.type.equals("wither") || current.wither == null
                || !current.wither.equals(event.getEntity()) || current.witherDefeated) return;
        Player player = null;
        if (event.getDamager() instanceof Player direct) player = direct;
        else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player owner)
            player = owner;
        else if (event.getDamageSource().getCausingEntity() instanceof Player source) player = source;
        double rawDamage = Math.max(0.0, event.getDamage());
        event.setCancelled(true);
        if (player == null || !eligible(player)) return;
        double maxHit = Math.max(1.0, configuredDouble("wither-max-damage-per-hit", 160.0));
        double scale = Math.max(.05, Math.min(2.0, configuredDouble("wither-damage-received-multiplier", 1.0)));
        double amount = Math.min(maxHit, rawDamage * scale);
        if (amount <= 0 || !Double.isFinite(amount)) return;
        current.bossDamage.merge(player.getUniqueId(), amount, Double::sum);
        current.witherRaidHealth = Math.max(0.0, current.witherRaidHealth - amount);
        if (current.witherRaidHealth > 0.0) {
            updateWitherNativeHealth(current);
            current.wither.playHurtAnimation(0.0f);
            current.world.spawnParticle(Particle.SOUL, current.wither.getLocation().add(0, 1.5, 0),
                    8, .5, .6, .5, .04);
        } else {
            current.witherDefeated = true;
            current.world.playSound(current.wither.getLocation(), Sound.ENTITY_WITHER_DEATH, 2.0f, .7f);
            current.world.spawnParticle(Particle.SOUL_FIRE_FLAME,
                    current.wither.getLocation().clone().add(0, 2, 0), 240, 4, 2, 4, .12);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (active == current) chooseWitherWinner(current);
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWitherDeath(EntityDeathEvent event) {
        Active current = active;
        if (current == null || !current.type.equals("wither") || !event.getEntity().equals(current.wither)) return;
        event.getDrops().clear();
        Bukkit.getScheduler().runTask(plugin, () -> chooseWitherWinner(current));
    }

    private boolean titanActive(Active event) {
        return active == event && !event.titanDefeated && event.titan != null
                && event.titan.isValid() && !event.titan.isDead();
    }

    private List<Player> titanPlayers(Active event, Location near, double radius) {
        List<Player> result = new ArrayList<>();
        for (Player player : participants()) {
            if (player.getWorld().equals(event.world)
                    && player.getLocation().distanceSquared(near) <= radius * radius)
                result.add(player);
        }
        return result;
    }

    // Each Titan attack can reach at least 20 blocks from the boss. Impact markers
    // for targeted attacks remain smaller so that skilled players can still dodge.
    private double titanAttackRadius() {
        return Math.max(20.0, Math.min(48.0,
                configuredDouble("titan-attack-radius-blocks", 24.0)));
    }

    private double titanChaseRadius() {
        return Math.max(35.0, Math.min(120.0,
                configuredDouble("titan-chase-range-blocks", 85.0)));
    }

    /** Checks a Warden-sized footprint and 3 blocks of head clearance. */
    private boolean titanBodyClear(Location feet) {
        if (!feet.getWorld().getWorldBorder().isInside(feet)) return false;
        final double halfWidth = .44;
        for (double ox : new double[]{-halfWidth, halfWidth}) {
            for (double oz : new double[]{-halfWidth, halfWidth}) {
                Location at = feet.clone().add(ox, 0, oz);
                for (int h = 0; h < 3; h++) {
                    Block space = at.clone().add(0, h, 0).getBlock();
                    if (!space.isPassable() || space.isLiquid()) return false;
                }
            }
        }
        return true;
    }

    private boolean titanStandable(Location feet) {
        if (!titanBodyClear(feet)) return false;
        final double halfWidth = .44;
        for (double ox : new double[]{-halfWidth, halfWidth}) {
            for (double oz : new double[]{-halfWidth, halfWidth}) {
                Block under = feet.clone().add(ox, -0.12, oz).getBlock();
                if (!under.getType().isSolid() || under.isLiquid()) return false;
            }
        }
        return true;
    }

    /** Crossing a ledge can leave half of a 0.9-block-wide Warden over each
     * height level. Require a safe foothold within 4 blocks below EVERY corner,
     * but allow a brief unsupported transition to move across that boundary. */
    private boolean titanSafeFootingWithin(Location feet, int maxDrop) {
        final double halfWidth = .44;
        for (double ox : new double[]{-halfWidth, halfWidth}) {
            for (double oz : new double[]{-halfWidth, halfWidth}) {
                boolean found = false;
                for (int depth = 1; depth <= maxDrop; depth++) {
                    Block under = feet.clone().add(ox, -depth + .05, oz).getBlock();
                    if (under.isLiquid()) return false;
                    if (under.getType().isSolid()) { found = true; break; }
                }
                if (!found) return false;
            }
        }
        return true;
    }

    /** Find a collision-free foot level at the destination. Tries to step up
     * one block or descend as much as four blocks, based on the player's height.
     * The old code always tried same-Y first, trapping the boss at ledges. */
    private Location titanTryStep(Location from, double dx, double dz, Location target) {
        int fromLevel = from.getBlockY();
        boolean seekHigher = target.getY() > from.getY() + .65;
        boolean seekLower = target.getY() < from.getY() - .65;
        int[] offsets = seekHigher ? new int[]{1, 0, -1, -2, -3, -4}
                : seekLower ? new int[]{-1, -2, 0, -3, -4, 1}
                : new int[]{0, 1, -1, -2, -3, -4};
        Location at = from.clone().add(dx, 0, dz);
        for (int offset : offsets) {
            Location next = at.clone();
            next.setY(fromLevel + offset);
            if (offset > 0) {
                // To get onto a one-block ledge, the space over the Warden's
                // current location must be clear while stepping upward.
                Location overHead = from.clone();
                overHead.setY(fromLevel + 1);
                if (!titanBodyClear(overHead)) continue;
            }
            if (!titanStandable(next)) continue;
            // Ensure the travel path doesn't cross a solid wall mid-step.
            Location middle = from.clone().add(dx * .5, 0, dz * .5);
            middle.setY(fromLevel + Math.max(0, offset));
            if (!titanBodyClear(middle)) continue;
            return next;
        }
        // When the ground changes level across the Warden's footprint, full
        // support at all four corners is briefly impossible. Move horizontally
        // over safe 1-4 block transitions instead of freezing at the ledge.
        for (int offset : new int[]{0, 1}) {
            Location transition = at.clone();
            transition.setY(fromLevel + offset);
            if (titanBodyClear(transition) && titanSafeFootingWithin(transition, 4))
                return transition;
        }
        return null;
    }

    /** Scripted raid chase and controlled melee; NO native Warden AI. */
    private void tickTitanMovement() {
        Active event = active;
        if (event == null || !event.type.equals("titan") || !titanActive(event)) return;
        Warden boss = event.titan;
        if (boss.hasAI()) boss.setAI(false);
        ++event.titanMovementStep;
        Location center = boss.getLocation();
        List<Player> targets = titanPlayers(event, center, titanChaseRadius());
        if (targets.isEmpty()) return;

        Player nearest = targets.stream().min(Comparator.comparingDouble(
                p -> p.getLocation().distanceSquared(center))).orElse(null);
        if (nearest == null) return;
        Vector toTarget = nearest.getLocation().toVector().subtract(center.toVector());
        double horizontal = Math.hypot(toTarget.getX(), toTarget.getZ());
        if (horizontal > 2.9) {
            double speed = Math.max(.20, Math.min(.85,
                    configuredDouble("titan-chase-speed-per-step", .48)))
                    * (event.enraged ? 1.22 : 1.0);
            double forwardX = toTarget.getX() / horizontal;
            double forwardZ = toTarget.getZ() / horizontal;
            // First try directly ahead, then diagonals and sideways to navigate
            // around trees, stone pillars and small obstacles without teleporting
            // through collision geometry. Alternate the preferred dodge direction
            // when the Titan becomes stuck.
            double[] offsets = {0, 35, -35, 70, -70, 100, -100, 140, -140};
            Location next = null;
            for (double offset : offsets) {
                double rad = Math.toRadians(offset * event.titanStrafeDirection);
                double nx = forwardX * Math.cos(rad) - forwardZ * Math.sin(rad);
                double nz = forwardX * Math.sin(rad) + forwardZ * Math.cos(rad);
                next = titanTryStep(center, nx * speed, nz * speed, nearest.getLocation());
                if (next != null) break;
            }
            if (next != null) {
                double yaw = (float) Math.toDegrees(Math.atan2(-toTarget.getX(), toTarget.getZ()));
                next.setYaw((float) yaw);
                next.setPitch(0.0f);
                if (boss.teleport(next)) {
                    event.titanLastSuccessfulStep = event.titanMovementStep;
                    if (event.titanMovementStep % 5 == 0)
                        event.world.spawnParticle(Particle.SNOWFLAKE,
                                next.clone().add(0, .25, 0), 4, .6, .08, .6, .01);
                }
            }
            if (event.titanMovementStep - event.titanLastSuccessfulStep > 15) {
                event.titanStrafeDirection *= -1;
                event.titanLastSuccessfulStep = event.titanMovementStep;
            }
        }
        if (toTarget.lengthSquared() <= 3.4 * 3.4
                && event.titanMovementStep >= event.titanNextMeleeStep) {
            // 2 ticks per movement step: one melee attack every 1.8s normally, 1.2s enraged.
            event.titanNextMeleeStep = event.titanMovementStep + (event.enraged ? 12 : 18);
            double damage = Math.max(1.0, Math.min(10.0,
                    configuredDouble("titan-melee-damage-health-points", 6.0)));
            nearest.damage(damage, boss);  // Armor and Protection apply.
            event.world.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.1f, .65f);
            Vector push = nearest.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() > .01) {
                push.setY(0).normalize().multiply(.55).setY(.22);
                nearest.setVelocity(push);
            }
        }
    }

    private void titanAttacks(Active event) {
        if (!titanActive(event)) return;
        Warden boss = event.titan;
        if (!event.enraged && event.titanRaidHealth <= event.titanMaxRaidHealth * .45) {
            event.enraged = true;
            announce(ChatColor.RED + "FROST TITAN IS ENRAGED! Phase II: faster attacks and more hazards!");
            event.world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.5f, 1.4f);
            event.world.spawnParticle(Particle.SNOWFLAKE,
                    boss.getLocation().add(0, 2, 0), 120, 3, 2, 3, .07);
        }
        if (event.seconds < event.titanNextAbility) return;
        event.titanNextAbility = event.seconds + (event.enraged ? 5 : 7);
        int ability = event.titanAttackIndex++ % 5;
        switch (ability) {
            case 0 -> castTitanFrostquake(event);
            case 1 -> castTitanIceLances(event);
            case 2 -> castTitanWhiteout(event);
            case 3 -> castTitanIcePillars(event);
            case 4 -> castTitanSummonGuards(event);
            default -> { }
        }
    }

    // Visual ground effects and an ambient chime telegraph attacks.
    // Never occupy the action bar: it belongs to event score / target tracking.
    private void titanWarning(Active event, String name) {
        if (!titanActive(event)) return;
        event.world.playSound(event.titan.getLocation(),
                Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.15f, .55f);
    }

    private void frostRing(Location center, double radius, Particle particle) {
        for (int step = 0; step < 36; step++) {
            double a = step * Math.PI / 18.0;
            center.getWorld().spawnParticle(particle, center.clone().add(
                    Math.cos(a) * radius, .15, Math.sin(a) * radius), 2, .10, .08, .10, .002);
        }
    }

    // Attack 1: expanding shockwave. A jump or moving beyond the ring avoids it.
    private void castTitanFrostquake(Active event) {
        titanWarning(event, "GLACIAL FROSTQUAKE — JUMP OR RETREAT!");
        Location center = event.titan.getLocation().clone();
        double radius = titanAttackRadius();
        frostRing(center, radius, Particle.SNOWFLAKE);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!titanActive(event)) return;
            event.world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, .75f);
            frostRing(center, radius, Particle.SNOWFLAKE);
            event.world.spawnParticle(Particle.BLOCK,
                    center.clone().add(0, .6, 0), 240, radius * .5, .4, radius * .5, .12,
                    Material.PACKED_ICE.createBlockData());
            for (Player target : titanPlayers(event, center, radius)) {
                if (Math.abs(target.getLocation().getY() - center.getY()) > 1.5) continue;
                target.damage(event.enraged ? 8.0 : 6.0, event.titan);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
                Vector away = target.getLocation().toVector().subtract(center.toVector());
                if (away.lengthSquared() > .01) target.setVelocity(away.setY(0).normalize().multiply(.6).setY(.34));
            }
        }, 26L);
    }

    // Attack 2: target markers, then crystal spears strike the old locations.
    private void castTitanIceLances(Active event) {
        titanWarning(event, "ICE LANCE VOLLEY — KEEP MOVING!");
        Location center = event.titan.getLocation();
        List<Player> targets = titanPlayers(event, center, titanAttackRadius() + 10.0);
        java.util.Collections.shuffle(targets, random);
        int count = Math.min(event.enraged ? 6 : 4, targets.size());
        List<Location> marks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Location mark = targets.get(i).getLocation().clone();
            marks.add(mark);
            event.world.spawnParticle(Particle.SNOWFLAKE, mark.clone().add(0, .3, 0), 45, .65, .15, .65, .015);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!titanActive(event)) return;
            Set<UUID> damaged = new LinkedHashSet<>();
            for (Location mark : marks) {
                event.world.playSound(mark, Sound.BLOCK_GLASS_BREAK, 1.0f, .8f);
                event.world.spawnParticle(Particle.BLOCK, mark.clone().add(0, 1.6, 0),
                        70, .7, 1.1, .7, .08, Material.BLUE_ICE.createBlockData());
                for (Player target : titanPlayers(event, mark, 3.2)) {
                    if (!damaged.add(target.getUniqueId())) continue;
                    target.damage(event.enraged ? 7.0 : 5.0, event.titan);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 45, 1));
                }
            }
        }, 28L);
    }

    // Attack 3: a blizzard that lasts five seconds. Leaving the area counters it.
    private void castTitanWhiteout(Active event) {
        titanWarning(event, "WHITEOUT BLIZZARD — LEAVE THE STORM!");
        Location center = event.titan.getLocation().clone();
        event.world.playSound(center, Sound.BLOCK_POWDER_SNOW_BREAK, 1.8f, .45f);
        for (int pulse = 0; pulse < 5; pulse++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!titanActive(event)) return;
                Location at = event.titan.getLocation().clone();
                double radius = titanAttackRadius() + (event.enraged ? 3.0 : 0.0);
                event.world.spawnParticle(Particle.SNOWFLAKE, at.clone().add(0, 2, 0),
                        320, radius * .68, 3, radius * .68, .06);
                for (Player target : titanPlayers(event, at, radius)) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                    target.damage(2.0, event.titan);
                }
            }, 20L * pulse);
        }
    }

    // Attack 4: ice pillars erupt from telegraphed ground spots. No block grief.
    private void castTitanIcePillars(Active event) {
        titanWarning(event, "CRYSTAL ERUPTION — DODGE THE ICE CIRCLES!");
        List<Location> marks = new ArrayList<>();
        Location center = event.titan.getLocation();
        int count = event.enraged ? 18 : 14;
        double range = titanAttackRadius();
        for (int i = 0; i < count; i++) {
            double angle = (2.0 * Math.PI * i / count) + (i % 3) * .31;
            // Inner / middle / outer rings all cover more than 20 blocks.
            double distance = range * (.38 + (i % 4) * .205);
            Location mark = center.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
            Block floor = event.world.getHighestBlockAt(mark, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            mark.setY(floor.getY() + 1);
            if (!event.world.getWorldBorder().isInside(mark)) continue;
            marks.add(mark);
            frostRing(mark, 2.8, Particle.SNOWFLAKE);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!titanActive(event)) return;
            Set<UUID> damaged = new LinkedHashSet<>();
            for (Location mark : marks) {
                event.world.spawnParticle(Particle.BLOCK, mark.clone().add(0, 1.2, 0),
                        50, .65, 1.0, .65, .04, Material.PACKED_ICE.createBlockData());
                event.world.playSound(mark, Sound.BLOCK_GLASS_BREAK, .85f, .7f);
                for (Player target : titanPlayers(event, mark, 3.0)) {
                    if (!damaged.add(target.getUniqueId())) continue;
                    target.damage(event.enraged ? 7.0 : 5.0, event.titan);
                    Vector lift = target.getVelocity();
                    target.setVelocity(new Vector(lift.getX(), .72, lift.getZ()));
                }
            }
        }, 32L);
    }

    // Attack 5: summons frosted sword-wielding Strays to split raid attention.
    private void castTitanSummonGuards(Active event) {
        titanWarning(event, "FROST GUARD — REINFORCEMENTS INCOMING!");
        Location center = event.titan.getLocation();
        event.world.playSound(center, Sound.ENTITY_WITHER_SPAWN, .75f, 1.5f);
        int present = 0;
        for (Entity entity : event.visuals) if (entity instanceof Stray && entity.isValid()) present++;
        int count = Math.min(event.enraged ? 7 : 5, Math.max(0, 16 - present));
        for (int i = 0; i < count; i++) {
            double angle = 2.0 * Math.PI * i / Math.max(1, count);
            double spawnRadius = 10.0 + (i % 4) * 4.2;
            Location spawn = center.clone().add(Math.cos(angle) * spawnRadius, 0, Math.sin(angle) * spawnRadius);
            Block ground = event.world.getHighestBlockAt(spawn, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            spawn.setY(ground.getY() + 1);
            if (!event.world.getWorldBorder().isInside(spawn)) continue;
            Stray guard = event.world.spawn(spawn, Stray.class);
            guard.setCustomName(ChatColor.AQUA + "Titan's Frost Guard");
            guard.setPersistent(true);
            guard.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            guard.getEquipment().setHelmetDropChance(0f);
            ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
            ItemMeta meta = sword.getItemMeta();
            meta.addEnchant(Enchantment.SHARPNESS, event.enraged ? 3 : 2, true);
            sword.setItemMeta(meta);
            guard.getEquipment().setItemInMainHand(sword);
            guard.getEquipment().setItemInMainHandDropChance(0f);
            guard.setFireTicks(0);
            event.visuals.add(guard);
            Player nearest = titanPlayers(event, spawn, titanAttackRadius() + 12.0).stream()
                    .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(spawn))).orElse(null);
            if (nearest != null) guard.setTarget(nearest);
            event.world.spawnParticle(Particle.SNOWFLAKE, spawn.clone().add(0, 1, 0),
                    30, .5, .7, .5, .03);
        }
    }

    private void assignTarget(Active event, Player hunter) {
        List<Player> options = participants().stream()
                .filter(player -> !player.getUniqueId().equals(hunter.getUniqueId())).toList();
        if (options.isEmpty()) { event.targets.remove(hunter.getUniqueId()); return; }
        UUID previous = event.targets.get(hunter.getUniqueId());
        List<Player> others = options.stream().filter(p -> !p.getUniqueId().equals(previous)).toList();
        if (!others.isEmpty()) options = others;
        Player target = options.get(random.nextInt(options.size()));
        event.targets.put(hunter.getUniqueId(), target.getUniqueId());
        hunter.sendMessage(prefix() + "Your next target is " + ChatColor.GOLD + target.getName() + ChatColor.RESET + "!");
    }
    private void tickAssassin(Active event) {
        for (Player hunter : participants()) {
            UUID id = hunter.getUniqueId();
            UUID targetId = event.targets.get(id);
            Player target = targetId == null ? null : Bukkit.getPlayer(targetId);
            if (target == null || !eligible(target) || target.equals(hunter)) {
                assignTarget(event, hunter);
                UUID assigned = event.targets.get(id);
                target = assigned == null ? null : Bukkit.getPlayer(assigned);
            }
            int kills = event.points.getOrDefault(id, 0);
            String distance = target == null ? "?" :
                    (target.getWorld().equals(hunter.getWorld()) ?
                            ((int) Math.round(target.getLocation().distance(hunter.getLocation()))) + " blocks" : "Other dimension");
            String name = target == null ? "Waiting for a player" : target.getName();
            hunter.sendActionBar(Component.text("❄ Target: " + name + " | Distance: " + distance
                    + " | Target kills: " + kills + " | " + (event.totalSeconds - event.seconds) + "s"));
        }
    }

    private void tick() {
        Active event = active;
        if (event == null) return;
        event.seconds++;
        event.bar.setProgress(Math.max(0, Math.min(1, (event.totalSeconds - event.seconds) / (double) event.totalSeconds)));
        for (Player player : Bukkit.getOnlinePlayers()) if (!event.bar.getPlayers().contains(player)) event.bar.addPlayer(player);
        if (event.type.equals("wither")) {
            if (event.wither == null || !event.wither.isValid() || event.wither.isDead()) {
                chooseWitherWinner(event); return;
            }
            tickWitherRaid(event);
        } else if (event.type.equals("titan")) {
            if (event.titan == null || !event.titan.isValid() || event.titan.isDead()) {
                chooseTitanWinner(event); return;
            }
            double max = Math.max(1.0, event.titanMaxRaidHealth);
            event.bar.setTitle(ChatColor.AQUA + "Frost Titan " + (int) Math.ceil(event.titanRaidHealth)
                    + "/" + (int) max + " RAID HP | " + Math.max(0, event.totalSeconds - event.seconds) + "s");
            event.bar.setProgress(Math.max(0, Math.min(1, event.titanRaidHealth / max)));
            titanAttacks(event);
        } else if (event.type.equals("chosen")) {
            event.bar.setTitle(ChatColor.AQUA + "Chosen One: " + ChatColor.GOLD +
                    event.chosenLastCoordinates + ChatColor.AQUA + " | " +
                    Math.max(0, event.totalSeconds - event.seconds) + "s");
        } else {
            event.bar.setTitle(ChatColor.AQUA + label(event.type) + " | " + Math.max(0, event.totalSeconds - event.seconds) + "s");
        }
        switch (event.type) {
            case "meteor" -> {
                if (event.meteorLanded && event.seconds % 5 == 0 && event.objective != null)
                    event.world.spawnParticle(Particle.SNOWFLAKE, event.objective.clone().add(0, 1, 0), 45, 1.4, 1.3, 1.4, .02);
            }
            case "treasure" -> {
                if (event.seconds % 50 == 0 && event.objective != null) {
                    Location l = event.objective;
                    int coarse = event.seconds < 150 ? 100 : 20;
                    int x = Math.floorDiv(l.getBlockX(), coarse) * coarse;
                    int z = Math.floorDiv(l.getBlockZ(), coarse) * coarse;
                    String clue = "Treasure clue: " + event.world.getName() + " | near X=" +
                            x + ", Z=" + z + " (within ~" + coarse + " blocks)";
                    if (event.treasureUnderground) {
                        int yPrecision = event.seconds < 150 ? 10 : 3;
                        int yHint = Math.floorDiv(l.getBlockY(), yPrecision) * yPrecision;
                        clue += " | UNDERGROUND near Y=" + yHint + " (+/-" + yPrecision +
                                "); search for a glowing ice ladder entrance at the surface!";
                    } else clue += " | ABOVE GROUND: look for the blue-ice shrine!";
                    announce(clue);
                }
            }
            case "koth" -> tickKoth(event);
            case "chosen" -> {
                UUID holder = currentRelicHolder(event);
                event.chosenHolder = holder;
                if (!java.util.Objects.equals(holder, event.lastHolder)) {
                    if (holder != null) {
                        Player player = Bukkit.getPlayer(holder);
                        if (player != null) announce(player.getName() + " now holds the Chosen Relic!");
                    }
                    event.lastHolder = holder;
                    announceChosenPosition(event, false);
                }
                Player player = holder == null ? null : Bukkit.getPlayer(holder);
                updateChosenGlow(event, player);
                if (player != null)
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            player.getLocation().add(0, 1, 0), 20, .4, .7, .4, .01);
                if (event.seconds % 60 == 0) announceChosenPosition(event, true);
            }
            case "assassin" -> tickAssassin(event);
            case "shatteredshrines" -> tickShatteredShrines(event);
            case "whiteout" -> tickWhiteout(event);
            case "crystalfallrush" -> tickCrystalfallRush(event);
            case "bingo" -> tickBingo(event);
            case "wither" -> { /* handled before the event-specific tick switch */ }
        }
        if (event.seconds >= event.totalSeconds && active == event) endDueToTime(event);
    }
    private void tickKoth(Active event) {
        int radius = Math.max(3, configuredInt("koth-radius-blocks", 10));
        int target = Math.max(20, configuredInt("koth-capture-seconds", 90));
        List<Player> inside = participants().stream().filter(p -> p.getWorld().equals(event.world))
                .filter(p -> p.getLocation().distanceSquared(event.objective) <= radius * radius).toList();
        if (event.seconds % 2 == 0) event.world.spawnParticle(Particle.SNOWFLAKE,
                event.objective.clone().add(0, .5, 0), 40, radius / 2.0, .3, radius / 2.0, 0.02);
        if (inside.size() != 1) return; // A contested or empty hill earns no points.
        Player leader = inside.get(0);
        int score = event.points.merge(leader.getUniqueId(), 1, Integer::sum);
        leader.sendActionBar(Component.text("❄ Capturing the hill: " + score + "/" + target + " seconds"));
        if (score >= target) win(event, leader.getUniqueId(), "Captured the Frost Hill");
    }

    private UUID leader(Map<UUID, ? extends Number> scores) {
        UUID result = null;
        double top = 0.0;
        for (var entry : scores.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !eligible(player)) continue;
            double amount = entry.getValue().doubleValue();
            if (amount > top) { result = entry.getKey(); top = amount; }
        }
        return result;
    }
    private void endDueToTime(Active event) {
        switch (event.type) {
            case "koth" -> win(event, leader(event.points), "Most hill-control seconds");
            case "assassin" -> win(event, leader(event.points), "Most assigned-target eliminations");
            case "chosen" -> win(event, currentRelicHolder(event), "Held the chosen relic at the final second");
            case "titan" -> chooseTitanWinner(event);
            case "wither" -> chooseWitherWinner(event);
            case "shatteredshrines" -> win(event, leader(event.points), "Most shrine control points");
            case "whiteout" -> win(event, leader(event.points), "Longest survival time inside the Whiteout safe zone");
            case "crystalfallrush" -> win(event, leader(event.points), "Most Frost Crystals banked");
            case "bingo" -> {
                announce("Frost Bingo ended: nobody completed a full row, column, or diagonal.");
                closeEvent();
            }
            case "meteor", "treasure" -> win(event, null, "No one claimed the objective before the timer expired");
        }
    }
    private void chooseTitanWinner(Active event) {
        if (active != event) return;
        win(event, leader(event.bossDamage), "Dealt the most damage to the Frost Titan");
    }

    private void chooseWitherWinner(Active event) {
        if (active != event) return;
        win(event, leader(event.bossDamage), "Dealt the most damage to the Frost Wither");
    }
    private void win(Active event, UUID winnerId, String reason) {
        if (active != event) return;
        Player winner = winnerId == null ? null : Bukkit.getPlayer(winnerId);
        if (winner == null || !winner.isOnline()) {
            announce("Event ended without an eligible winner. " + reason + ".");
            closeEvent(); return;
        }
        ItemStack prize = plugin.frostEventReward(event.reward);
        if (prize != null) {
            Map<Integer, ItemStack> leftovers = winner.getInventory().addItem(prize);
            for (ItemStack item : leftovers.values()) winner.getWorld().dropItemNaturally(winner.getLocation(), item);
        }
        announce(ChatColor.GOLD + winner.getName() + ChatColor.AQUA + " WON " + label(event.type)
                + "! Prize: " + ChatColor.GREEN + rewardName(event.reward) + ChatColor.AQUA + ". " + reason + ".");
        winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f);
        closeEvent();
    }
    void shutdown() { closeEvent(); }
    private void closeEvent() {
        Active event = active;
        if (event == null) return;
        active = null;
        generation++;
        event.bar.removeAll();
        for (Entity entity : event.visuals) if (entity.isValid()) entity.remove();
        if (event.type.equals("bingo")) {
            for (Player player : Bukkit.getOnlinePlayers()) removeBingoBooks(player);
            for (Item item : event.world.getEntitiesByClass(Item.class))
                if (isBingoBook(item.getItemStack())) item.remove();
        }
        if (event.type.equals("crystalfallrush")) {
            for (Item item : event.world.getEntitiesByClass(Item.class))
                if (isEventCrystal(item.getItemStack())) item.remove();
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack[] contents = player.getInventory().getContents();
                for (int slot = 0; slot < contents.length; slot++)
                    if (isEventCrystal(contents[slot])) player.getInventory().setItem(slot, null);
                if (isEventCrystal(player.getItemOnCursor())) player.setItemOnCursor(null);
                player.updateInventory();
            }
        }
        if (event.titan != null && event.titan.isValid()) event.titan.remove();
        if (event.wither != null && event.wither.isValid()) event.wither.remove();
        if (event.objectiveBlock != null && event.oldBlockData != null
                && event.objectiveBlock.getType() == Material.CHEST)
            event.objectiveBlock.setBlockData(event.oldBlockData, false);
        // Revert ONLY blocks that still match our exact changes. Other events/builds remain untouched.
        List<Map.Entry<Block, BlockData>> edits = new ArrayList<>(event.impactOriginal.entrySet());
        java.util.Collections.reverse(edits);
        for (Map.Entry<Block, BlockData> edit : edits) {
            BlockData expected = event.impactPlaced.get(edit.getKey());
            if (expected != null && edit.getKey().getBlockData().matches(expected))
                edit.getKey().setBlockData(edit.getValue(), false);
        }
        if (event.type.equals("chosen")) {
            restoreChosenGlow(event);
            removeAllRelics(event);
        }
        for (long chunkPos : event.meteorChunkTickets) {
            int cx = (int) (chunkPos >> 32);
            int cz = (int) chunkPos;
            event.world.removePluginChunkTicket(cx, cz, plugin);
        }
        event.meteorChunkTickets.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void protectCrystalsFromStorage(InventoryClickEvent event) {
        if (active == null || !active.type.equals("crystalfallrush")) return;
        if (event.getView().getTopInventory() instanceof org.bukkit.inventory.PlayerInventory) return;
        boolean cursorCrystal = isEventCrystal(event.getCursor());
        boolean topSlotCrystal = event.getRawSlot() >= 0
                && event.getRawSlot() < event.getView().getTopInventory().getSize()
                && isEventCrystal(event.getCurrentItem());
        boolean shiftMovingCrystal = event.isShiftClick() && isEventCrystal(event.getCurrentItem());
        boolean hotbarMovingCrystal = event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                && event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) != null
                && isEventCrystal(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
        if (cursorCrystal || topSlotCrystal || shiftMovingCrystal || hotbarMovingCrystal) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void protectCrystalsFromStorageDrag(InventoryDragEvent event) {
        if (active == null || !active.type.equals("crystalfallrush")) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)
                && event.getNewItems().values().stream().anyMatch(this::isEventCrystal)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrystalfallAltarClick(PlayerInteractEvent event) {
        Active current = active;
        if (current == null || !current.type.equals("crystalfallrush")
                || event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null || !event.getClickedBlock().equals(current.objectiveBlock)) return;
        event.setCancelled(true);
        if (eligible(event.getPlayer())) depositCrystals(current, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onObjectiveClick(PlayerInteractEvent event) {
        Active current = active;
        if (current == null || (!current.type.equals("meteor") && !current.type.equals("treasure"))) return;
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || !event.getClickedBlock().equals(current.objectiveBlock)) return;
        event.setCancelled(true);
        if (eligible(event.getPlayer())) win(current, event.getPlayer().getUniqueId(), "First player to claim the Frost Chest");
    }
    @EventHandler(ignoreCancelled = true)
    public void onObjectiveBreak(BlockBreakEvent event) {
        if (active != null && (event.getBlock().equals(active.objectiveBlock)
                || active.impactPlaced.containsKey(event.getBlock()))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(prefix() + "This structure belongs to the active Frost event and cannot be broken.");
        }
    }
    @EventHandler(ignoreCancelled = true)
    public void protectObjectiveFromExplosion(EntityExplodeEvent event) {
        if (active == null) return;
        event.blockList().removeIf(block -> (active.objectiveBlock != null && block.equals(active.objectiveBlock))
                || active.impactPlaced.containsKey(block));
    }
    @EventHandler(ignoreCancelled = true)
    public void protectObjectiveFromBlockExplosion(BlockExplodeEvent event) {
        if (active == null) return;
        event.blockList().removeIf(block -> (active.objectiveBlock != null && block.equals(active.objectiveBlock))
                || active.impactPlaced.containsKey(block));
    }
    // Prevent environmental attrition / accidental vanilla damage to the raid boss.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void protectTitanFromEnvironment(org.bukkit.event.entity.EntityDamageEvent event) {
        Active current = active;
        if (current == null || !current.type.equals("titan") || current.titan == null
                || !current.titan.equals(event.getEntity())) return;
        if (!(event instanceof EntityDamageByEntityEvent)) event.setCancelled(true);
    }

    // Virtual raid health: counts real final hit damage, then absorbs it into the
    // larger raid pool. Native Warden health is purely a visual progress proxy.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTitanDamage(EntityDamageByEntityEvent event) {
        Active current = active;
        if (current == null || !current.type.equals("titan") || current.titan == null
                || !current.titan.equals(event.getEntity()) || current.titanDefeated) return;
        Player player = null;
        if (event.getDamager() instanceof Player direct) player = direct;
        else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player owner)
            player = owner;
        else if (event.getDamageSource().getCausingEntity() instanceof Player source) player = source;
        // The raid boss's Warden armor reduces getFinalDamage() and, with the
        // old 24 * 0.30 cap, made almost every strong weapon hit count as ~7 HP.
        // getDamage() preserves raw critical-hit, enchantment and Frost ability
        // differences while virtual health absorbs native damage safely.
        double rawDamage = Math.max(0.0, event.getDamage());
        // No vanilla health/armor side-effects. Ownership is attributed below.
        event.setCancelled(true);
        if (player == null || !eligible(player)) return;
        double maxHit = Math.max(1.0, configuredDouble("titan-max-damage-per-hit", 96.0));
        double scale = Math.max(0.05, Math.min(2.0,
                configuredDouble("titan-damage-received-multiplier", 1.0)));
        double amount = Math.min(maxHit, rawDamage * scale);
        if (amount <= 0 || !Double.isFinite(amount)) return;
        if (event.isCritical()) {
            current.world.spawnParticle(Particle.CRIT, current.titan.getLocation().add(0, 1.8, 0),
                    14, .65, .8, .65, .14);
        }
        current.bossDamage.merge(player.getUniqueId(), amount, Double::sum);
        current.titanRaidHealth = Math.max(0.0, current.titanRaidHealth - amount);
        double fraction = current.titanRaidHealth / Math.max(1.0, current.titanMaxRaidHealth);
        if (current.titanRaidHealth > 0.0) {
            current.titan.setHealth(Math.max(1.0, 1000.0 * fraction));
            current.titan.playHurtAnimation(0.0f);
        } else {
            current.titanDefeated = true;
            current.world.playSound(current.titan.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.5f, .7f);
            current.world.spawnParticle(Particle.SNOWFLAKE,
                    current.titan.getLocation().clone().add(0, 2, 0), 240, 4, 2, 4, .12);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (active == current) chooseTitanWinner(current);
            });
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTitanDeath(EntityDeathEvent event) {
        Active current = active;
        if (current == null || !current.type.equals("titan") || !event.getEntity().equals(current.titan)) return;
        event.getDrops().clear();
        Bukkit.getScheduler().runTask(plugin, () -> chooseTitanWinner(current));
    }
    @EventHandler(ignoreCancelled = true)
    public void onAssassinOrChosenDeath(PlayerDeathEvent event) {
        Active current = active;
        if (current == null) return;
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null && event.getDamageSource().getCausingEntity() instanceof Player source)
            killer = source;
        if (current.type.equals("assassin")) {
            if (killer != null && !killer.equals(victim) && eligible(killer)
                    && victim.getUniqueId().equals(current.targets.get(killer.getUniqueId()))) {
                int kills = current.points.merge(killer.getUniqueId(), 1, Integer::sum);
                announce(killer.getName() + " eliminated their assigned target " + victim.getName() + "! Score: " + kills);
                assignTarget(current, killer);
            }
            current.targets.remove(victim.getUniqueId());
            return;
        }
        if (!current.type.equals("chosen")) return;
        boolean relicPresent = event.getDrops().removeIf(item -> isRelic(item, current));
        if (!relicPresent) {
            for (ItemStack item : victim.getInventory().getContents())
                if (isRelic(item, current)) { relicPresent = true; break; }
        }
        if (!relicPresent) return;
        if (event.getKeepInventory()) {
            for (int slot = 0; slot < victim.getInventory().getSize(); slot++)
                if (isRelic(victim.getInventory().getItem(slot), current))
                    victim.getInventory().setItem(slot, null);
        }
        // No special reward for killing the holder: tag is now the transfer mechanic.
        // On unrelated deaths the marked relic is dropped and can be picked up.
        event.getDrops().add(createRelic(current));
        current.chosenHolder = null;
        restoreChosenGlow(current);
        announce("The Chosen Relic dropped where " + victim.getName() + " died. Pick it up to claim it!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Active current = active;
        if (current == null) return;
        current.bar.addPlayer(event.getPlayer());
        if (current.type.equals("assassin"))
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (active == current && eligible(event.getPlayer())) assignTarget(current, event.getPlayer());
            }, 20L);
        if (current.type.equals("bingo"))
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (active == current && eligible(event.getPlayer())) assignBingoBoard(current, event.getPlayer());
            }, 20L);
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Active current = active;
        if (current == null) return;
        if (current.type.equals("assassin")) active.targets.remove(event.getPlayer().getUniqueId());
        if (!current.type.equals("chosen")) return;
        if (event.getPlayer().getUniqueId().equals(current.chosenGlowPlayer)) restoreChosenGlow(current);
        Player holder = event.getPlayer();
        boolean dropped = false;
        for (int slot = 0; slot < holder.getInventory().getSize(); slot++) {
            ItemStack item = holder.getInventory().getItem(slot);
            if (!isRelic(item, current)) continue;
            holder.getInventory().setItem(slot, null);
            holder.getWorld().dropItemNaturally(holder.getLocation(), item);
            dropped = true;
        }
        if (dropped) announce("The Chosen One disconnected; their relic dropped at their last position!");
    }
}

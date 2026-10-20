package de.joris.aplejuice.client;

import de.joris.aplejuice.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public final class AplejuiceClient implements ClientModInitializer {
    public static TrackConfig config;
    public static final CounterStore counters = new CounterStore();
    public static final Map<UUID, String> names = new LinkedHashMap<>();
    public static boolean authoritative;
    private static final UsageQueue<Placement> pending = new UsageQueue<>(128);
    private static final ZeroHealthGuard zeroHealth = new ZeroHealthGuard();
    private static final UsageQueue<PendingThrow> pendingThrows = new UsageQueue<>(128);
    private static long tick;
    private static ClientLevel lastLevel;
    private static final Map<UUID, Map<Item, Long>> recentHands = new HashMap<>();
    private static final Set<UUID> seenProjectiles = new LinkedHashSet<>();
    private static final Map<UUID, Deque<Pose>> recentPoses = new HashMap<>();
    private static final Map<UUID, Player> deadPlayers = new HashMap<>();
    private static final Set<UUID> observedZeroHealth = new HashSet<>();
    public static final int MAX_TRACKED_PLAYERS = 8;
    public static final int MAX_OBSERVERS = 9;
    public static final int MAX_PENDING = 128;
    public static final int MAX_SEEN_PROJECTILES = 2048;
    private static final Set<Item> watchedItems = new HashSet<>();
    private static final Set<UUID> watchedPlayers = new HashSet<>();
    private static List<Player> observers = List.of();
    private static final TargetSelection targets = new TargetSelection();
    private static Player selectedEntity;
    private static long nextRefresh;
    private static List<String> appliedItems = List.of();
    private static boolean appliedEnabled, appliedOpponentOnly;
    private static int appliedRange;
    private record Pose(Vec3 eye, Vec3 look, long tick) {}
    private record Actor(UUID id, double score, boolean heldAtReceipt, boolean recentAtReceipt) {}
    private record Placement(Item item, long tick, List<Actor> actors, UUID owner) {}
    private record PendingThrow(int ownerId, Item item, long tick) {}
    public void onInitializeClient() {
        config = TrackConfig.load();
        TrackConfig.onChanged(AplejuiceClient::refreshConfiguration);
        config.items.removeIf(id -> item(id) == Items.AIR);
        BadgeModels.registerReloadListener();
        refreshConfiguration();
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide() && player == Minecraft.getInstance().player && entity instanceof Player opponent)
                selectTarget(opponent);
            return InteractionResult.PASS;
        });
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("aplejuice", "main"));
        var open = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.aplejuice.menu", GLFW.GLFW_KEY_J, category));
        ClientPlayNetworking.registerGlobalReceiver(UsagePayload.TYPE, (packet, context) -> context.client().execute(() -> {
            if (!authoritative) { reset(); authoritative = true; }
            if (isWatched(packet.player()) && !deadPlayers.containsKey(packet.player()) && config.items.contains(packet.item()))
                counters.add(packet.player(), packet.item(), packet.amount());
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetConnection());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> resetConnection());
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Player player && player.getUUID().equals(targets.selected())) { selectedEntity = player; nextRefresh = 0; }
            if (entity instanceof Player player) loadedPlayer(player);
            if (entity instanceof Player player && deadPlayers.get(player.getUUID()) != player && player.isAlive()) {
                deadPlayers.remove(player.getUUID()); observedZeroHealth.remove(player.getUUID());
            }
            if (entity instanceof Player) resolveThrows();
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity == selectedEntity) { selectedEntity = null; nextRefresh = 0; }
            if (entity instanceof Player player) {
                recentHands.remove(player.getUUID()); recentPoses.remove(player.getUUID());
                observers = observers.stream().filter(p -> p != player).toList(); watchedPlayers.remove(player.getUUID()); nextRefresh = 0;
                BadgeModels.retain(watchedPlayers);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tick++;
            while (open.consumeClick()) if (client.screen == null) client.setScreen(new TrackScreen(null));
            if (client.level == null) return;
            ensureLevel(client);
            if (!config.enabled) { pending.clear(); pendingThrows.clear(); return; }
            if (tick >= nextRefresh) refreshObservers(client);
            resolveThrows();
            // Empty lobby state is O(1): no player scan, snapshots, model building or block inspection.
            if (watchedPlayers.isEmpty()) { pending.clear(); return; }
            for (Player player : observers) {
                if (player.isRemoved()) continue;
                if (zeroHealth.confirmsDeath(player.getUUID(), player.getHealth() <= 0, tick)) {
                    observeDeath(player);
                    if (player.getHealth() <= 0) observedZeroHealth.add(player.getUUID());
                } else if (player.getHealth() > 0 && deadPlayers.containsKey(player.getUUID())
                        && (deadPlayers.get(player.getUUID()) != player || observedZeroHealth.contains(player.getUUID()))) {
                    deadPlayers.remove(player.getUUID()); observedZeroHealth.remove(player.getUUID());
                }
                rememberPlayer(player);
            }
            pending.drain(placement -> tick - placement.tick >= 2, (placement, amount) -> attribute(client, placement, amount));
        });
    }
    public static Item item(String id) {
        Identifier key = Identifier.tryParse(id);
        return key == null ? Items.AIR : Objects.requireNonNullElse(BuiltInRegistries.ITEM.getValue(key), Items.AIR);
    }
    public static long clientTick() { return tick; }
    /** Retain preferences, but reset counters and selection for a new server connection. */
    public static void resetConnection() {
        config.save();
        reset(); names.clear(); seenProjectiles.clear(); recentHands.clear(); recentPoses.clear(); zeroHealth.clear();
        deadPlayers.clear(); observedZeroHealth.clear(); lastLevel = null; authoritative = false;
        clearTarget();
    }
    public static UUID targetId() { return targets.selected(); }
    public static int observerCount() { return observers.size(); }
    public static int watchedCount() { return watchedPlayers.size(); }
    public static int pendingCount() { return pending.size(); }
    public static boolean isObserving() { return config != null && config.enabled && !watchedPlayers.isEmpty(); }
    private static void ensureLevel(Minecraft client) {
        if (client.level == lastLevel) return;
        pending.clear(); pendingThrows.clear(); recentHands.clear(); recentPoses.clear();
        deadPlayers.clear(); observedZeroHealth.clear(); zeroHealth.clear();
        observers = List.of(); watchedPlayers.clear(); targets.clear(); selectedEntity = null; nextRefresh = 0; lastLevel = client.level;
        BadgeModels.clear();
    }
    /** Refresh invalidated selections before packet events, rather than missing them until the next tick. */
    public static boolean prepareObservation() {
        var mc = Minecraft.getInstance();
        if (!mc.isSameThread() || mc.level == null || config == null || !config.enabled || config.items.isEmpty()) return false;
        ensureLevel(mc);
        if (tick >= nextRefresh) refreshObservers(mc);
        return !config.opponentOnly || targets.selected() != null;
    }
    public static boolean isWatched(UUID id) {
        if (config == null || !targets.accepts(id, config.enabled, config.opponentOnly)) return false;
        if (watchedPlayers.contains(id)) return true;
        var local = Minecraft.getInstance().player;
        return !config.opponentOnly && local != null && local.getUUID().equals(id);
    }
    public static void refreshConfiguration() {
        if (config == null) return;
        // Saving language, zero visibility, or closing the menu must not discard in-flight uses.
        if (appliedItems.equals(config.items) && appliedEnabled == config.enabled
                && appliedOpponentOnly == config.opponentOnly && appliedRange == config.range) return;
        targets.configure(config.enabled, config.opponentOnly);
        if (targets.selected() == null) selectedEntity = null;
        appliedItems = List.copyOf(config.items); appliedEnabled = config.enabled;
        appliedOpponentOnly = config.opponentOnly; appliedRange = config.range;
        watchedItems.clear();
        for (String id : config.items) { Item value = item(id); if (value != Items.AIR) watchedItems.add(value); }
        pending.clear(); pendingThrows.clear(); observers = List.of(); watchedPlayers.clear(); recentHands.clear(); recentPoses.clear(); nextRefresh = 0;
        BadgeModels.clear();
    }
    public static void clearTarget() {
        targets.clear(); selectedEntity = null; pending.clear(); pendingThrows.clear();
        observers = List.of(); watchedPlayers.clear(); recentHands.clear(); recentPoses.clear(); nextRefresh = 0;
        BadgeModels.clear();
    }
    public static void selectTarget(Player target) {
        Minecraft mc = Minecraft.getInstance();
        if (config == null || !config.enabled || mc.level == null || target.level() != mc.level
                || target == mc.player || target.isSpectator() || !target.isAlive()) return;
        ensureLevel(mc);
        if (target.getUUID().equals(targets.selected()) && target == selectedEntity && watchedPlayers.contains(targets.selected())) return;
        if (config.opponentOnly && !target.getUUID().equals(targets.selected())) {
            pending.clear(); pendingThrows.clear(); recentHands.clear(); recentPoses.clear(); BadgeModels.clear();
        }
        targets.select(target.getUUID(), config.enabled); selectedEntity = target; rememberName(target);
        if (mc.level != null) refreshObservers(mc);
    }
    public static void rememberName(Player player) {
        names.remove(player.getUUID());
        names.put(player.getUUID(), player.getName().getString());
        while (names.size() > 64) {
            UUID oldest = names.keySet().iterator().next(); names.remove(oldest); counters.reset(oldest);
        }
    }
    private static void loadedPlayer(Player player) {
        var mc = Minecraft.getInstance();
        if (!config.enabled || mc.player == null || player == mc.player || player.isSpectator()) return;
        ensureLevel(mc);
        UUID id = player.getUUID();
        if (config.opponentOnly && !id.equals(targets.selected())) return;
        if (mc.player.distanceToSqr(player) > config.range * config.range) return;
        var updated = new ArrayList<Player>(MAX_OBSERVERS);
        for (Player observer : observers) if (!observer.getUUID().equals(id)) updated.add(observer);
        if (watchedPlayers.contains(id) || watchedPlayers.size() < (config.opponentOnly ? 1 : MAX_TRACKED_PLAYERS)) {
            watchedPlayers.add(id); updated.add(player);
            if (!updated.contains(mc.player)) updated.add(mc.player);
            observers = List.copyOf(updated); rememberName(player); nextRefresh = 0;
        }
    }
    public record PlayerOption(UUID id, String name) {}
    /** Lightweight name snapshots, independent of tracked histories and counter retention. */
    public static List<PlayerOption> playerOptions() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return List.of();
        return mc.level.players().stream().filter(p -> p != mc.player && p.isAlive() && !p.isSpectator())
                .sorted(Comparator.comparing((Player p) -> !p.getUUID().equals(targets.selected()))
                        .thenComparingDouble(mc.player::distanceToSqr))
                .map(p -> new PlayerOption(p.getUUID(), p.getName().getString())).toList();
    }
    public static List<UUID> playerChoices() {
        return playerOptions().stream().map(PlayerOption::id).toList();
    }
    private static void refreshObservers(Minecraft mc) {
        nextRefresh = tick + 5;
        watchedPlayers.clear();
        if (mc.player == null || mc.level == null || !config.enabled || config.items.isEmpty()) { observers = List.of(); return; }
        double range = Math.min(config.range, 64); range *= range;
        var picked = new LinkedHashMap<UUID, Player>();
        if (targets.selected() != null && (selectedEntity == null || selectedEntity.isRemoved() || selectedEntity.level() != mc.level))
            selectedEntity = mc.level.getPlayerByUUID(targets.selected());
        boolean selectedInRange = selectedEntity != null && !selectedEntity.isRemoved()
                && !selectedEntity.isSpectator() && mc.player.distanceToSqr(selectedEntity) <= range;
        if (config.opponentOnly) {
            if (targets.selected() == null) { observers = List.of(); return; }
            if (!selectedInRange) {
                observers = List.of(); return;
            }
            watchedPlayers.add(targets.selected()); picked.put(targets.selected(), selectedEntity);
            picked.put(mc.player.getUUID(), mc.player);
            final Player focus = selectedEntity;
            NearestSelection.within(mc.level.players(), MAX_OBSERVERS - picked.size(), 16 * 16,
                            p -> p != focus && p != mc.player && !p.isRemoved() && !p.isSpectator(), focus::distanceToSqr)
                    .forEach(p -> picked.put(p.getUUID(), p));
        } else {
            if (selectedInRange) { watchedPlayers.add(targets.selected()); picked.put(targets.selected(), selectedEntity); }
            NearestSelection.within(mc.level.players(), MAX_TRACKED_PLAYERS - picked.size(), range,
                            p -> p != mc.player && !p.getUUID().equals(targets.selected()) && !p.isRemoved() && !p.isSpectator(), mc.player::distanceToSqr)
                    .forEach(p -> { watchedPlayers.add(p.getUUID()); picked.put(p.getUUID(), p); });
            picked.put(mc.player.getUUID(), mc.player);
        }
        observers = List.copyOf(picked.values());
        for (Player player : observers) if (watchedPlayers.contains(player.getUUID())) rememberName(player);
        recentHands.keySet().retainAll(picked.keySet()); recentPoses.keySet().retainAll(picked.keySet());
        deadPlayers.keySet().retainAll(watchedPlayers); observedZeroHealth.retainAll(watchedPlayers);
        BadgeModels.retain(watchedPlayers);
        zeroHealth.retain(watchedPlayers);
    }
    public static void reset() { counters.clear(); pending.clear(); pendingThrows.clear(); }
    public static void observeBlock(BlockPos pos, BlockState state) {
        Minecraft mc = Minecraft.getInstance();
        if (authoritative || !prepareObservation() || !isObserving()) return;
        Item item = state.getBlock().asItem();
        if (!watchedItems.contains(item)) return;
        boolean nearby = false;
        for (Player player : observers) if (watchedPlayers.contains(player.getUUID()) && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 16 * 16) { nearby = true; break; }
        if (!nearby || !mc.level.hasChunkAt(pos)) return;
        var previous = mc.level.getBlockState(pos);
        if (item == Items.AIR || previous.is(state.getBlock()) || !previous.canBeReplaced()
                || !config.items.contains(BuiltInRegistries.ITEM.getKey(item).toString())) return;
        // Capture who was nearby NOW, not after two more ticks of movement or item switching.
        List<Actor> actors = captureActors(mc, pos, item);
        UUID owner = choose(actors, item, tick, false);
        // Equal-state updates were rejected above. A remove/place at the same position is a new use.
        queuePlacement(mc, item, actors, owner);
    }
    private static void queuePlacement(Minecraft mc, Item item, List<Actor> actors, UUID owner) {
        if (actors.isEmpty()) return;
        var placement = new Placement(item, tick, actors, owner);
        if (owner != null) attribute(mc, placement, 1);
        else pending.add(placement, (old, amount) -> attribute(mc, old, amount));
    }
    private static void attribute(Minecraft mc, Placement placement, int amount) {
        if (authoritative || !config.enabled || mc.level == null
                || !config.items.contains(BuiltInRegistries.ITEM.getKey(placement.item).toString())) return;
        UUID owner = placement.owner != null ? placement.owner : choose(placement.actors, placement.item, placement.tick, true);
        if (owner != null && isWatched(owner) && !deadPlayers.containsKey(owner))
            counters.add(owner, BuiltInRegistries.ITEM.getKey(placement.item).toString(), amount);
    }
    public static void rememberPlayer(Player player) {
        if (!isObserving() || !observers.contains(player) || deadPlayers.containsKey(player.getUUID())) return;
        var hands = recentHands.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        hands.entrySet().removeIf(entry -> tick - entry.getValue() > 8);
        if (!player.getMainHandItem().isEmpty()) hands.put(player.getMainHandItem().getItem(), tick);
        if (!player.getOffhandItem().isEmpty()) hands.put(player.getOffhandItem().getItem(), tick);
        var poses = recentPoses.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        while (!poses.isEmpty() && tick - poses.peekFirst().tick > 4) poses.removeFirst();
        if (!poses.isEmpty() && poses.peekLast().tick == tick) poses.removeLast();
        poses.addLast(new Pose(player.getEyePosition(), Vec3.directionFromRotation(player.getXRot(), player.getYHeadRot()), tick));
    }
    private static List<Actor> captureActors(Minecraft mc, BlockPos pos, Item item) {
        var actors = new ArrayList<Actor>();
        for (Player player : observers) {
            if (player.isSpectator() || player.isRemoved() || deadPlayers.containsKey(player.getUUID())) continue;
            boolean held = player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
            boolean recent = tick - recentHands.getOrDefault(player.getUUID(), Map.of()).getOrDefault(item, Long.MIN_VALUE / 2) <= 8;
            double score = poseScore(new Pose(player.getEyePosition(), Vec3.directionFromRotation(player.getXRot(), player.getYHeadRot()), tick), pos);
            for (Pose pose : recentPoses.getOrDefault(player.getUUID(), new ArrayDeque<>()))
                score = Math.min(score, poseScore(pose, pos));
            if (Double.isFinite(score)) actors.add(new Actor(player.getUUID(), score, held, recent));
        }
        return List.copyOf(actors);
    }
    private static double poseScore(Pose pose, BlockPos pos) {
        if (tick - pose.tick > 4) return Double.POSITIVE_INFINITY;
        // Reach is measured to the nearest point of the block, including edge/corner placements.
        Vec3 closest = new Vec3(Math.clamp(pose.eye.x, pos.getX(), pos.getX() + 1D),
                Math.clamp(pose.eye.y, pos.getY(), pos.getY() + 1D), Math.clamp(pose.eye.z, pos.getZ(), pos.getZ() + 1D));
        double distance = closest.distanceTo(pose.eye);
        if (distance > 6) return Double.POSITIVE_INFINITY;
        double alignment = pose.look.dot(Vec3.atCenterOf(pos).subtract(pose.eye).normalize());
        // View rotation is delayed independently of block packets, so it is a preference, not a veto.
        return distance + (1 - alignment) * 0.9 + (tick - pose.tick) * 0.1;
    }
    private static UUID choose(List<Actor> actors, Item item, long eventTick, boolean allowLaterEquipment) {
        UUID best = null; double first = Double.POSITIVE_INFINITY, second = Double.POSITIVE_INFINITY;
        for (Actor actor : actors) {
            if (deadPlayers.containsKey(actor.id)) continue;
            long lastHeld = recentHands.getOrDefault(actor.id, Map.of()).getOrDefault(item, Long.MIN_VALUE / 2);
            boolean later = allowLaterEquipment && lastHeld >= eventTick && lastHeld <= eventTick + 2;
            if (!actor.heldAtReceipt && !actor.recentAtReceipt && !later) continue;
            double score = actor.score + (actor.heldAtReceipt || later ? 0 : 0.5);
            if (score < first) { second = first; first = score; best = actor.id; }
            else second = Math.min(second, score);
        }
        return second - first >= 0.35 ? best : null;
    }
    public static void observeDeath(Player player) {
        if (!prepareObservation()) return;
        UUID id = player.getUUID();
        if (!isWatched(id)) { counters.reset(id); return; }
        if (deadPlayers.putIfAbsent(id, player) != null) return;
        counters.reset(id); recentHands.remove(id); recentPoses.remove(id);
        pending.removeIf(p -> id.equals(p.owner) || p.owner == null && p.actors.stream().anyMatch(a -> id.equals(a.id)));
    }
    public static void observeTotem(Player player) {
        if (!prepareObservation() || !isWatched(player.getUUID())) return;
        // Every status-35 packet is a pop, even several packets in the same tick.
        // It also proves survival despite a delayed zero-health update.
        UUID id = player.getUUID();
        zeroHealth.popped(id, tick); deadPlayers.remove(id); observedZeroHealth.remove(id);
        if (!authoritative && watchedItems.contains(Items.TOTEM_OF_UNDYING))
            counters.add(id, "minecraft:totem_of_undying", 1);
    }
    public static boolean observable(Item item) {
        return item instanceof BlockItem || item == Items.TOTEM_OF_UNDYING || item == Items.ENDER_PEARL
                || item == Items.SNOWBALL || item == Items.END_CRYSTAL
                || item == Items.EXPERIENCE_BOTTLE || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
    }
    public static void observeEntitySpawn(ClientboundAddEntityPacket packet) {
        Item item = spawnItem(packet.getType());
        if (item == Items.AIR || authoritative || !prepareObservation() || !watchedItems.contains(item)) return;
        if (!seenProjectiles.add(packet.getUUID())) return;
        if (seenProjectiles.size() > MAX_SEEN_PROJECTILES) seenProjectiles.remove(seenProjectiles.iterator().next());
        var mc = Minecraft.getInstance();
        if (item == Items.END_CRYSTAL) {
            BlockPos base = BlockPos.containing(packet.getX(), packet.getY() - 1, packet.getZ());
            if (!isObserving() || !mc.level.hasChunkAt(base)) return;
            var support = mc.level.getBlockState(base);
            if (!support.is(Blocks.OBSIDIAN) && !support.is(Blocks.BEDROCK)) return;
            var actors = captureActors(mc, base, item);
            queuePlacement(mc, item, actors, choose(actors, item, tick, false));
            return;
        }
        if (packet.getData() <= 0) return;
        // Read the owner ID directly before a fast projectile can disappear or lose its entity reference.
        var owner = mc.level.getEntity(packet.getData());
        if (owner instanceof Player player) creditThrow(player, item, 1);
        else if (owner == null) {
            pendingThrows.add(new PendingThrow(packet.getData(), item, tick), AplejuiceClient::resolveThrow);
        }
    }
    private static Item spawnItem(EntityType<?> type) {
        if (type == EntityType.ENDER_PEARL) return Items.ENDER_PEARL;
        if (type == EntityType.END_CRYSTAL) return Items.END_CRYSTAL;
        if (type == EntityType.SNOWBALL) return Items.SNOWBALL;
        if (type == EntityType.EXPERIENCE_BOTTLE) return Items.EXPERIENCE_BOTTLE;
        if (type == EntityType.SPLASH_POTION) return Items.SPLASH_POTION;
        if (type == EntityType.LINGERING_POTION) return Items.LINGERING_POTION;
        return Items.AIR;
    }
    private static void resolveThrows() {
        if (pendingThrows.size() == 0) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || authoritative || config == null || !config.enabled) { pendingThrows.clear(); return; }
        pendingThrows.drain(usage -> tick - usage.tick > 20 || mc.level.getEntity(usage.ownerId) != null,
                AplejuiceClient::resolveThrow);
    }
    private static void resolveThrow(PendingThrow usage, int amount) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && tick - usage.tick <= 20 && mc.level.getEntity(usage.ownerId) instanceof Player owner)
            creditThrow(owner, usage.item, amount);
    }
    private static void creditThrow(Player owner, Item item, int amount) {
        if (!isWatched(owner.getUUID()) || owner.isSpectator() || deadPlayers.containsKey(owner.getUUID())) return;
        counters.add(owner.getUUID(), BuiltInRegistries.ITEM.getKey(item).toString(), amount);
        rememberName(owner);
    }
}

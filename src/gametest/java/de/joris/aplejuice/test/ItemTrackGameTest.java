package de.joris.aplejuice.test;

import de.joris.aplejuice.client.*;
import de.joris.aplejuice.ZeroHealthGuard;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Items;
import net.minecraft.client.player.RemotePlayer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.EquipmentSlot;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

public final class ItemTrackGameTest implements FabricClientGameTest {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void click(ClientGameTestContext ctx, String label) {
        ctx.runOnClient(mc -> {
            var screen = (TrackScreen) mc.screen;
            var button = screen.children().stream().filter(c -> c instanceof TrackButton b && b.getMessage().getString().equals(label))
                    .map(c -> (TrackButton)c).findFirst().orElseThrow();
            float scale = Math.min(screen.width / (float) TrackScreen.W, screen.height / (float) TrackScreen.H);
            double ox = (screen.width - TrackScreen.W * scale) / 2, oy = (screen.height - TrackScreen.H * scale) / 2;
            screen.mouseClicked(new MouseButtonEvent(ox + (button.getX() + 8) * scale, oy + (button.getY() + 8) * scale, new MouseButtonInfo(0, 0)), false);
        });
        ctx.waitTick();
    }
    public void runTest(ClientGameTestContext ctx) {
        ctx.waitForScreen(TitleScreen.class);
        ctx.getInput().resizeWindow(1572, 742);
        ctx.runOnClient(mc -> {
            AplejuiceClient.config.items = new ArrayList<>(List.of("minecraft:cobweb"));
            AplejuiceClient.config.enabled = true;
            AplejuiceClient.config.language = "de_de";
            check(!new TrackConfig().opponentOnly && !new com.google.gson.Gson().fromJson("{}", TrackConfig.class).opponentOnly,
                    "Opponent-only setting defaults to off for new and older configs");
            check(new com.google.gson.Gson().fromJson("{\"opponentOnly\":true}", TrackConfig.class).opponentOnly,
                    "Explicit opponent-only setting is preserved");
            AplejuiceClient.config.opponentOnly = false;
            AplejuiceClient.refreshConfiguration();
        });
        ctx.setScreen(() -> new TrackScreen(null));
        ctx.waitTick();
        ctx.takeScreenshot("aplejuice-menu");
        ctx.runOnClient(mc -> {
            var screen = (TrackScreen) mc.screen;
            var edit = screen.children().stream().filter(c -> c instanceof EditBox).map(c -> (EditBox)c).findFirst().orElseThrow();
            edit.setValue("cobwebs");
            check(screen.children().stream().anyMatch(c -> c instanceof TrackButton b && b.getMessage().getString().equals("Entfernen")), "Cobwebs alias search");
            edit.setValue("ender_pearl");
            var button = screen.children().stream().filter(c -> c instanceof TrackButton b && b.getMessage().getString().equals("+ Track")).map(c -> (TrackButton)c).findFirst().orElseThrow();
            float scale = Math.min(screen.width / (float) TrackScreen.W, screen.height / (float) TrackScreen.H);
            double ox = (screen.width - TrackScreen.W * scale) / 2, oy = (screen.height - TrackScreen.H * scale) / 2;
            screen.mouseClicked(new MouseButtonEvent(ox + (button.getX() + 8) * scale, oy + (button.getY() + 8) * scale, new MouseButtonInfo(0, 0)), false);
            check(AplejuiceClient.config.items.contains("minecraft:ender_pearl"), "Scaled click adds item");
        });
        ctx.takeScreenshot("aplejuice-search");
        click(ctx, "Anzeige");
        ctx.takeScreenshot("aplejuice-settings");
        ctx.getInput().resizeWindow(854, 480);
        ctx.takeScreenshot("aplejuice-small-window");
        ctx.getInput().resizeWindow(1572, 742);
        click(ctx, "English");
        ctx.runOnClient(mc -> {
            check(AplejuiceClient.config.language.equals("en_us"), "Language switches immediately");
            check(TrackConfig.load().language.equals("en_us"), "Language is saved");
            check(mc.screen.children().stream().anyMatch(c -> c instanceof TrackButton b && b.getMessage().getString().equals("Display")), "Navigation translated");
        });
        ctx.takeScreenshot("aplejuice-settings-en");
        ctx.setScreen(() -> new TrackScreen(null));
        ctx.waitTick();
        ctx.runOnClient(mc -> check(mc.screen.children().stream().anyMatch(c -> c instanceof TrackButton b && b.getMessage().getString().equals("Display")), "Reopened menu retains language"));
        ctx.takeScreenshot("aplejuice-menu-en");
        click(ctx, "Deutsch");
        ctx.takeScreenshot("aplejuice-menu-de");
        if (Boolean.getBoolean("aplejuice.test.uiOnly")) {
            ctx.setScreen(TitleScreen::new);
            System.out.println("APLEJUICE_UI_TEST_PASSED: search, scaled clicks, language switching, persistence and reopening");
            return;
        }
        ctx.setScreen(TitleScreen::new);
        try (var world = ctx.worldBuilder().create()) {
            ctx.waitFor(mc -> mc.player != null && AplejuiceClient.authoritative);
            UUID id = ctx.computeOnClient(mc -> mc.player.getUUID());
            ctx.runOnClient(mc -> mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                player.awardStat(Stats.ITEM_USED.get(Items.COBWEB), 63);
            }));
            ctx.waitFor(mc -> AplejuiceClient.counters.get(id, "minecraft:cobweb") == 63);
            ctx.getInput().pressKey(74);
            ctx.waitForScreen(TrackScreen.class);
            click(ctx, "Spieler");
            ctx.takeScreenshot("aplejuice-server-counts");
            click(ctx, "Runde zurücksetzen");
            ctx.runOnClient(mc -> check(AplejuiceClient.counters.get(id, "minecraft:cobweb") == 0, "Session reset"));
            ctx.setScreen(() -> null);
            UUID opponent = UUID.randomUUID();
            ctx.getInput().resizeWindow(1572, 742);
            ctx.runOnClient(mc -> {
                var remote = new RemotePlayer(mc.level, new GameProfile(opponent, "Gegner"));
                remote.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ() - 4);
                remote.setId(20000); mc.level.addEntity(remote);
                AplejuiceClient.counters.add(opponent, "minecraft:cobweb", 63);
                mc.player.setYRot(180); mc.player.setXRot(0);
            });
            ctx.waitTicks(3);
            ctx.takeScreenshot("aplejuice-nametag");
            ctx.getInput().pressKey(74);
            ctx.waitForScreen(TrackScreen.class);
            ctx.takeScreenshot("aplejuice-world-menu-de");
            click(ctx, "English");
            ctx.runOnClient(mc -> {
                check(AplejuiceClient.config.language.equals("en_us"), "Language switches immediately");
                check(TrackConfig.load().language.equals("en_us"), "Language is saved");
                check(mc.screen.children().stream().anyMatch(c -> c instanceof TrackButton b && b.getMessage().getString().equals("Display")), "Navigation translated");
            });
            ctx.takeScreenshot("aplejuice-world-menu-en");
            click(ctx, "Display");
            ctx.takeScreenshot("aplejuice-world-settings-en");
            click(ctx, "Deutsch");
            ctx.runOnClient(mc -> check(mc.screen.children().stream().anyMatch(c -> c instanceof TrackButton b && b.getMessage().getString().equals("Anzeige")), "Switches back to German"));
            ctx.takeScreenshot("aplejuice-world-settings-de");
            ctx.setScreen(() -> null);
            ctx.runOnClient(mc -> {
                AplejuiceClient.authoritative = false;
                AplejuiceClient.reset();
                var remote = (RemotePlayer) mc.level.getPlayerByUUID(opponent);
                remote.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBWEB));
                remote.setYRot(0); remote.setXRot(40);
            });
            ctx.waitTick();
            ctx.runOnClient(mc -> {
                var remote = mc.level.getPlayerByUUID(opponent);
                var pos = BlockPos.containing(remote.position()).south();
                var packet = new ClientboundBlockUpdatePacket(pos, Blocks.COBWEB.defaultBlockState());
                mc.getConnection().handleBlockUpdate(packet);
                mc.getConnection().handleBlockUpdate(packet);
                // The final item of a stack can disappear before the placement is attributed.
                remote.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            });
            ctx.waitTicks(3);
            ctx.runOnClient(mc -> check(AplejuiceClient.counters.get(opponent, "minecraft:cobweb") == 1,
                    "Client placement attribution, final stack item and duplicate suppression"));
            ctx.takeScreenshot("aplejuice-observed-nametag");
            for (int i = 1; i < 64; i++) {
                ctx.runOnClient(mc -> {
                    var remote = mc.level.getPlayerByUUID(opponent);
                    remote.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBWEB));
                    var pos = BlockPos.containing(remote.position()).south();
                    mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState()));
                    mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.COBWEB.defaultBlockState()));
                });
                ctx.waitTicks(3);
            }
            ctx.runOnClient(mc -> {
                check(AplejuiceClient.counters.get(opponent, "minecraft:cobweb") == 64, "A full stack of observed cobweb placements counts as 64");
                var remote = mc.level.getPlayerByUUID(opponent);
                UUID pearlId = UUID.randomUUID();
                var packet = new ClientboundAddEntityPacket(20001, pearlId, remote.getX(), remote.getEyeY(), remote.getZ(),
                        0, 0, EntityType.ENDER_PEARL, remote.getId(), new Vec3(0, 0, 1), 0);
                mc.getConnection().handleAddEntity(packet);
                check(AplejuiceClient.counters.get(opponent, "minecraft:ender_pearl") == 1, "Vanilla pearl spawn is attributed to its owner without server sync");
                mc.level.removeEntity(20001, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
                mc.getConnection().handleAddEntity(packet);
                check(AplejuiceClient.counters.get(opponent, "minecraft:ender_pearl") == 1, "Reloaded pearl is not counted twice");
                var unknown = new ClientboundAddEntityPacket(20002, UUID.randomUUID(), remote.getX(), remote.getEyeY(), remote.getZ(),
                        0, 0, EntityType.ENDER_PEARL, 1234567, new Vec3(0, 0, 1), 0);
                mc.getConnection().handleAddEntity(unknown);
                check(AplejuiceClient.counters.get(opponent, "minecraft:ender_pearl") == 1, "Unknown pearl owner is not guessed");
                var distant = new ClientboundAddEntityPacket(20003, UUID.randomUUID(), remote.getX() + 40, remote.getEyeY(), remote.getZ(),
                        0, 0, EntityType.ENDER_PEARL, remote.getId(), new Vec3(0, 0, 1), 0);
                mc.getConnection().handleAddEntity(distant);
                check(AplejuiceClient.counters.get(opponent, "minecraft:ender_pearl") == 2,
                        "A first-seen pearl with an explicit owner is not discarded by the old eight-block cutoff");
            });
            ctx.takeScreenshot("aplejuice-observed-stack-and-pearl");
            ctx.runOnClient(mc -> {
                AplejuiceClient.config.items.add("minecraft:totem_of_undying");
                AplejuiceClient.config.items.add("minecraft:end_crystal");
                AplejuiceClient.refreshConfiguration();
                var remote = mc.level.getPlayerByUUID(opponent);
                // No tick deduplication, even after settings invalidate the observer cache.
                remote.setHealth(0);
                for (int i = 0; i < 100; i++)
                    mc.getConnection().handleEntityEvent(new ClientboundEntityEventPacket(remote, (byte) 35));
                remote.setHealth(20);
                check(AplejuiceClient.counters.get(opponent, "minecraft:totem_of_undying") == 100,
                        "Every received pop counts, including 100 in one tick and transient zero health");
                check(AplejuiceClient.counters.get(opponent, "minecraft:cobweb") == 64,
                        "Transient zero health during pops does not reset previous round counters");

                long pearls = AplejuiceClient.counters.get(opponent, "minecraft:ender_pearl");
                for (int i = 0; i < 256; i++) {
                    var packet = new ClientboundAddEntityPacket(40000 + i, UUID.randomUUID(), remote.getX(), remote.getEyeY(), remote.getZ(),
                            0, 0, EntityType.ENDER_PEARL, remote.getId(), Vec3.ZERO, 0);
                    mc.getConnection().handleAddEntity(packet);
                    mc.level.removeEntity(40000 + i, Entity.RemovalReason.DISCARDED);
                    mc.getConnection().handleAddEntity(packet);
                    mc.level.removeEntity(40000 + i, Entity.RemovalReason.DISCARDED);
                }
                check(AplejuiceClient.counters.get(opponent, "minecraft:ender_pearl") == pearls + 256,
                        "256 instant-disappearing pearls in one tick count once each despite replayed spawns");

                var delayedOwnerId = UUID.randomUUID();
                for (int i = 0; i < 256; i++) {
                    mc.getConnection().handleAddEntity(new ClientboundAddEntityPacket(42000 + i, UUID.randomUUID(), remote.getX(), remote.getEyeY(), remote.getZ(),
                            0, 0, EntityType.ENDER_PEARL, 40501, Vec3.ZERO, 0));
                    mc.level.removeEntity(42000 + i, Entity.RemovalReason.DISCARDED);
                }
                var delayedOwner = new RemotePlayer(mc.level, new GameProfile(delayedOwnerId, "SpaeterGeladen"));
                delayedOwner.setId(40501); delayedOwner.setPos(remote.getX() + 2, remote.getY(), remote.getZ());
                mc.level.addEntity(delayedOwner);
                check(AplejuiceClient.counters.get(delayedOwnerId, "minecraft:ender_pearl") == 256,
                        "A 256-pearl burst before its owner's entity loads is resolved without a per-projectile queue cap");
                mc.level.removeEntity(40501, Entity.RemovalReason.UNLOADED_WITH_PLAYER);

                remote.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.END_CRYSTAL));
                var base = BlockPos.containing(remote.position()).south();
                for (int i = 0; i < 64; i++) {
                    mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(base,
                            (i % 2 == 0 ? Blocks.OBSIDIAN : Blocks.BEDROCK).defaultBlockState()));
                    var packet = new ClientboundAddEntityPacket(41000 + i, UUID.randomUUID(), base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5,
                            0, 0, EntityType.END_CRYSTAL, 0, Vec3.ZERO, 0);
                    mc.getConnection().handleAddEntity(packet);
                    mc.level.removeEntity(41000 + i, Entity.RemovalReason.DISCARDED);
                    mc.getConnection().handleAddEntity(packet);
                    mc.level.removeEntity(41000 + i, Entity.RemovalReason.DISCARDED);
                }
                check(AplejuiceClient.counters.get(opponent, "minecraft:end_crystal") == 64,
                        "Crystals on obsidian and bedrock count even if instantly destroyed, without replay duplicates");
                mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(base, Blocks.STONE.defaultBlockState()));
                mc.getConnection().handleAddEntity(new ClientboundAddEntityPacket(41100, UUID.randomUUID(), base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5,
                        0, 0, EntityType.END_CRYSTAL, 0, Vec3.ZERO, 0));
                check(AplejuiceClient.counters.get(opponent, "minecraft:end_crystal") == 64,
                        "Unsupported decorative crystal spawns do not count as valid placements");
                mc.level.removeEntity(41100, Entity.RemovalReason.DISCARDED);
                mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(base, Blocks.AIR.defaultBlockState()));
            });
            ctx.runOnClient(mc -> {
                AplejuiceClient.counters.reset(opponent);
                var remote = mc.level.getPlayerByUUID(opponent);
                remote.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBWEB));
                // Equipment can be cleared before the block update, without a client tick in between.
                mc.getConnection().handleSetEquipment(new ClientboundSetEquipmentPacket(remote.getId(),
                        List.of(Pair.of(EquipmentSlot.MAINHAND, ItemStack.EMPTY))));
                var pos = BlockPos.containing(remote.position()).south();
                mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState()));
                mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.COBWEB.defaultBlockState()));
            });
            ctx.waitTicks(3);
            ctx.runOnClient(mc -> check(AplejuiceClient.counters.get(opponent, "minecraft:cobweb") == 1,
                    "Equipment packet before block packet preserves the last held cobweb within the same tick"));
            ctx.runOnClient(mc -> {
                AplejuiceClient.counters.reset(opponent);
                var remote = mc.level.getPlayerByUUID(opponent);
                remote.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBWEB));
                remote.setYRot(180); remote.setYHeadRot(180); remote.setXRot(-70);
                var pos = BlockPos.containing(remote.position()).south();
                // Separate remove/place transitions must survive even within the same two-tick window.
                for (int i = 0; i < 700; i++) {
                    mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState()));
                    mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.COBWEB.defaultBlockState()));
                }
                mc.getConnection().handleSetEquipment(new ClientboundSetEquipmentPacket(remote.getId(),
                        List.of(Pair.of(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD)))));
                // Packet-time ownership must survive movement before pending events are resolved.
                remote.setPos(remote.getX() + 15, remote.getY(), remote.getZ());
            });
            ctx.waitTicks(3);
            ctx.runOnClient(mc -> {
                check(AplejuiceClient.counters.get(opponent, "minecraft:cobweb") == 700,
                        "700 rapid placements exceed the old queue limit without losing uses, despite rotation and movement");
                var remote = mc.level.getPlayerByUUID(opponent);
                remote.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ() - 4);
            });
            ctx.waitTick();
            ctx.takeScreenshot("aplejuice-overhead-43");
            UUID unaffected = UUID.randomUUID();
            ctx.runOnClient(mc -> {
                AplejuiceClient.counters.add(unaffected, "minecraft:cobweb", 7);
                var remote = mc.level.getPlayerByUUID(opponent);
                remote.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBWEB));
                var pos = BlockPos.containing(remote.position()).south();
                mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState()));
                mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.COBWEB.defaultBlockState()));
                mc.getConnection().handleEntityEvent(new ClientboundEntityEventPacket(remote, (byte) 3));
                check(AplejuiceClient.counters.get(opponent, "minecraft:cobweb") == 0, "Observed death resets counters immediately");
            });
            ctx.waitTicks(3);
            ctx.runOnClient(mc -> {
                check(AplejuiceClient.counters.get(opponent, "minecraft:cobweb") == 0, "Pending pre-death placements cannot refill reset counters");
                check(AplejuiceClient.counters.get(unaffected, "minecraft:cobweb") == 7, "Death resets only the player who died");
                mc.level.removeEntity(20000, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
                var fresh = new RemotePlayer(mc.level, new GameProfile(opponent, "Gegner"));
                fresh.setId(20004); fresh.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ() - 4);
                fresh.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBWEB)); mc.level.addEntity(fresh);
                var pos = BlockPos.containing(fresh.position()).south();
                mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState()));
                mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.COBWEB.defaultBlockState()));
            });
            ctx.waitTicks(3);
            ctx.runOnClient(mc -> {
                check(AplejuiceClient.counters.get(opponent, "minecraft:cobweb") == 1, "A respawned player begins a fresh count");
                var fresh = mc.level.getPlayerByUUID(opponent);
                var other = new RemotePlayer(mc.level, new GameProfile(unaffected, "AndererSpieler"));
                other.setId(20005); other.setPos(fresh.getX(), fresh.getY(), fresh.getZ());
                other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBWEB)); mc.level.addEntity(other);
                var pos = BlockPos.containing(fresh.position()).south();
                mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState()));
                mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.COBWEB.defaultBlockState()));
            });
            ctx.waitTicks(3);
            ctx.runOnClient(mc -> {
                check(AplejuiceClient.counters.get(opponent, "minecraft:cobweb") == 1, "Equally likely players are not arbitrarily credited");
                check(AplejuiceClient.counters.get(unaffected, "minecraft:cobweb") == 7, "Ambiguous placement does not increase other player's counter");
                mc.level.removeEntity(20005, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
                check(AplejuiceClient.counters.get(unaffected, "minecraft:cobweb") == 7, "Chunk/entity unload alone is not death");
                mc.level.getPlayerByUUID(opponent).setHealth(0);
            });
            ctx.waitTicks(ZeroHealthGuard.GRACE_TICKS + 1);
            ctx.runOnClient(mc -> check(AplejuiceClient.counters.get(opponent, "minecraft:cobweb") == 0,
                    "Sustained zero health resets without a death event packet after a short grace period"));
            ctx.takeScreenshot("aplejuice-overhead-death-reset");
            List<UUID> crowd = new ArrayList<>();
            ctx.runOnClient(mc -> {
                AplejuiceClient.config.opponentOnly = true;
                AplejuiceClient.refreshConfiguration(); AplejuiceClient.clearTarget();
                for (int i = 0; i < 100; i++) {
                    UUID uuid = UUID.randomUUID(); crowd.add(uuid);
                    var npc = new RemotePlayer(mc.level, new GameProfile(uuid, "Lobby" + i));
                    npc.setId(30000 + i);
                    npc.setPos(mc.player.getX() + (i % 10 - 5) * 2, mc.player.getY(), mc.player.getZ() + (i / 10 - 5) * 2);
                    if (i == 0) { npc.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ() - 4); npc.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBWEB)); }
                    mc.level.addEntity(npc);
                }
            });
            ctx.waitTicks(6);
            ctx.runOnClient(mc -> {
                check(AplejuiceClient.watchedCount() == 0 && AplejuiceClient.observerCount() == 0,
                        "100 loaded players without a selected target create no tracking snapshots");
                long builds = BadgeModels.buildCount();
                for (UUID uuid : crowd) {
                    var npc = (RemotePlayer) mc.level.getPlayerByUUID(uuid);
                    @SuppressWarnings("unchecked") var renderer = (AvatarRenderer<RemotePlayer>) mc.getEntityRenderDispatcher().getRenderer(npc);
                    var state = renderer.createRenderState(npc, 0);
                    check(((BadgeState)state).aplejuice$getBadges().isEmpty(), "Unselected lobby player has no badges");
                }
                check(BadgeModels.buildCount() == builds, "Unselected players do not build any item models");
                var focus = mc.level.getPlayerByUUID(crowd.getFirst());
                AttackEntityCallback.EVENT.invoker().interact(mc.player, mc.level, InteractionHand.MAIN_HAND, focus, new EntityHitResult(focus));
                check(crowd.getFirst().equals(AplejuiceClient.targetId()), "Attacking a player selects that opponent");
            });
            ctx.waitTick();
            ctx.runOnClient(mc -> {
                check(AplejuiceClient.watchedCount() == 1, "Opponent mode tracks exactly one player among 100");
                check(AplejuiceClient.observerCount() <= AplejuiceClient.MAX_OBSERVERS, "Ambiguity observers stay bounded");
                var focus = (RemotePlayer) mc.level.getPlayerByUUID(crowd.getFirst());
                @SuppressWarnings("unchecked") var renderer = (AvatarRenderer<RemotePlayer>) mc.getEntityRenderDispatcher().getRenderer(focus);
                BadgeModels.clear(); long builds = BadgeModels.buildCount();
                for (int frame = 0; frame < 100; frame++) {
                    var state = renderer.createRenderState(focus, 0);
                    check(!((BadgeState)state).aplejuice$getBadges().isEmpty(), "Selected opponent has badges");
                }
                check(BadgeModels.buildCount() - builds == AplejuiceClient.config.items.size(),
                        "100 render extractions reuse the same models instead of rebuilding each frame");
                UUID otherId = crowd.get(1);
                var other = mc.level.getPlayerByUUID(otherId);
                mc.getConnection().handleAddEntity(new ClientboundAddEntityPacket(31000, UUID.randomUUID(), other.getX(), other.getEyeY(), other.getZ(),
                        0, 0, EntityType.ENDER_PEARL, other.getId(), new Vec3(0, 0, 1), 0));
                check(AplejuiceClient.counters.get(otherId, "minecraft:ender_pearl") == 0, "Other players' projectiles are ignored");
                var pos = BlockPos.containing(focus.position()).south();
                for (int i = 0; i < 700; i++) {
                    mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState()));
                    mc.getConnection().handleBlockUpdate(new ClientboundBlockUpdatePacket(pos, Blocks.COBWEB.defaultBlockState()));
                }
                check(AplejuiceClient.pendingCount() <= AplejuiceClient.MAX_PENDING, "A packet burst cannot grow the queue without bounds");
                AplejuiceClient.clearTarget();
                check(AplejuiceClient.pendingCount() == 0 && AplejuiceClient.watchedCount() == 0, "Clearing a target stops and drains its tracking");
                AplejuiceClient.selectTarget(focus);
            });
            ctx.getInput().pressKey(74);
            ctx.waitForScreen(TrackScreen.class);
            ctx.takeScreenshot("aplejuice-opponent-mode");
            click(ctx, "Anzeige");
            ctx.takeScreenshot("aplejuice-opponent-settings");
            click(ctx, "Spieler");
            ctx.takeScreenshot("aplejuice-player-picker");
            ctx.runOnClient(mc -> {
                var options = AplejuiceClient.playerOptions();
                check(options.size() >= 100 && options.stream().anyMatch(p -> p.id().equals(crowd.get(99))),
                        "Picker includes every loaded opponent, including players beyond the old 32-entry limit");
                long builds = BadgeModels.buildCount();
                var edit = mc.screen.children().stream().filter(c -> c instanceof EditBox).map(c -> (EditBox)c).findFirst().orElseThrow();
                edit.setValue("lObBy99");
                check(mc.screen.children().stream().filter(c -> c instanceof TrackButton b && b.getMessage().getString().equals("Wählen")).count() == 1,
                        "Case-insensitive name search finds a player beyond the first page");
                check(AplejuiceClient.watchedCount() == 1 && BadgeModels.buildCount() == builds,
                        "Listing and filtering all player names does not start extra tracking or build item models");
            });
            ctx.takeScreenshot("aplejuice-player-search");
            UUID pickedInMenu = crowd.get(99);
            click(ctx, "Wählen");
            ctx.runOnClient(mc -> check(pickedInMenu.equals(AplejuiceClient.targetId()) && AplejuiceClient.watchedCount() == 1,
                    "Player picker selects exactly the chosen opponent"));
            click(ctx, "Aktiv");
            ctx.runOnClient(mc -> check(AplejuiceClient.targetId() == null && AplejuiceClient.watchedCount() == 0,
                    "Clicking Active deselects the opponent and stops tracking"));
            click(ctx, "Wählen");
            ctx.runOnClient(mc -> check(pickedInMenu.equals(AplejuiceClient.targetId()),
                    "Deselected player can be selected again from the filtered list"));
            click(ctx, "Item-Track");
            click(ctx, "Ziel abwählen");
            ctx.runOnClient(mc -> check(AplejuiceClient.targetId() == null && AplejuiceClient.watchedCount() == 0,
                    "Clear target button stops opponent tracking"));
            ctx.setScreen(() -> null);
            ctx.runOnClient(mc -> { AplejuiceClient.config.opponentOnly = false; AplejuiceClient.refreshConfiguration(); });
            ctx.waitTicks(6);
            ctx.runOnClient(mc -> {
                check(AplejuiceClient.watchedCount() <= AplejuiceClient.MAX_TRACKED_PLAYERS, "Opt-in nearby mode is capped at 8 players");
                check(AplejuiceClient.observerCount() <= AplejuiceClient.MAX_OBSERVERS, "Nearby mode keeps a bounded observer list");
            });
            // Manual selection does not change the nearby preference, and gets a slot among the eight targets.
            ctx.setScreen(() -> new TrackScreen(null)); click(ctx, "Spieler");
            ctx.runOnClient(mc -> {
                var edit = mc.screen.children().stream().filter(c -> c instanceof EditBox).map(c -> (EditBox)c).findFirst().orElseThrow();
                edit.setValue("Lobby99");
            });
            click(ctx, "Wählen");
            ctx.runOnClient(mc -> {
                check(!AplejuiceClient.config.opponentOnly, "Manual selection never flips the saved opponent-only setting");
                var focus = (RemotePlayer) mc.level.getPlayerByUUID(pickedInMenu);
                check(AplejuiceClient.isWatched(pickedInMenu), "Explicitly selected player is prioritized over nearer lobby players");
                AplejuiceClient.counters.reset(pickedInMenu); AplejuiceClient.config.showZero = true; AplejuiceClient.config.save();
                var team = mc.level.getScoreboard().addPlayerTeam("aple_hidden_names");
                team.setNameTagVisibility(net.minecraft.world.scores.Team.Visibility.NEVER);
                mc.level.getScoreboard().addPlayerToTeam(focus.getScoreboardName(), team);
                @SuppressWarnings("unchecked") var renderer = (AvatarRenderer<RemotePlayer>) mc.getEntityRenderDispatcher().getRenderer(focus);
                var state = renderer.createRenderState(focus, 0);
                check(state.nameTag == null, "Regression setup hides vanilla server name tags");
                check(!((BadgeState)state).aplejuice$getBadges().isEmpty() && ((BadgeState)state).aplejuice$getAnchor() != null,
                        "Item badges and their independent anchor exist even without a vanilla name tag");
                check(((BadgeState)state).aplejuice$getBadges().stream().allMatch(b -> b.count().equals("0")),
                        "Show-zero displays every configured item before the first use");
                AplejuiceClient.config.showZero = false; AplejuiceClient.config.save();
                check(((BadgeState)renderer.createRenderState(focus, 0)).aplejuice$getBadges().isEmpty(), "Show-zero off hides unused items");
                AplejuiceClient.counters.add(pickedInMenu, "minecraft:cobweb", 1);
                check(((BadgeState)renderer.createRenderState(focus, 0)).aplejuice$getBadges().size() == 1,
                        "The first nonzero count appears immediately without waiting for the model cache");
                AplejuiceClient.config.enabled = false; AplejuiceClient.config.save();
                AplejuiceClient.selectTarget(focus);
                check(AplejuiceClient.targetId() == null && ((BadgeState)renderer.createRenderState(focus, 0)).aplejuice$getBadges().isEmpty(),
                        "Global off hides badges and cannot preselect a target through an attack");
                AplejuiceClient.config.enabled = true; AplejuiceClient.config.opponentOnly = true;
                AplejuiceClient.config.showZero = true; AplejuiceClient.config.save();
                check(AplejuiceClient.targetId() == null, "Enabling opponent mode waits for a fresh attack or selection");
                var spawnedBot = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "UngewaehlterBot"));
                spawnedBot.setId(50000); spawnedBot.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ() + 2); mc.level.addEntity(spawnedBot);
                check(AplejuiceClient.watchedCount() == 0, "Spawning a bot does not activate opponent tracking");
                AttackEntityCallback.EVENT.invoker().interact(mc.player, mc.level, InteractionHand.MAIN_HAND, focus, new EntityHitResult(focus));
                check(AplejuiceClient.isWatched(pickedInMenu), "An attack activates the opponent after changing modes");
                AplejuiceClient.config.language = "en_us"; AplejuiceClient.config.range = 64; AplejuiceClient.config.save();
                var samePreferences = AplejuiceClient.config;
                AplejuiceClient.resetConnection();
                check(AplejuiceClient.config == samePreferences && AplejuiceClient.config.enabled && AplejuiceClient.config.showZero
                                && AplejuiceClient.config.opponentOnly && AplejuiceClient.config.range == 64 && AplejuiceClient.config.language.equals("en_us"),
                        "Connection reset keeps every user preference instead of replacing it with defaults");
                var saved = TrackConfig.load();
                check(saved.enabled && saved.showZero && saved.opponentOnly && saved.range == 64 && saved.language.equals("en_us"),
                        "Preferences also survive reloading the saved file after a server switch");
                check(AplejuiceClient.targetId() == null && AplejuiceClient.watchedCount() == 0, "Connection reset clears only session targets");
                AplejuiceClient.selectTarget(focus);
                check(AplejuiceClient.isWatched(pickedInMenu), "Manual selection works immediately after rebuilding a connection's tracking state");
                mc.level.getScoreboard().removePlayerTeam(team);
            });
        }
        System.out.println("APLEJUICE_FULL_TEST_PASSED: core tracking, death/respawn, 100 lobby players, hit selection, no-target zero model work, cached 100 render extractions, queue cap and nearby player cap");
    }
}

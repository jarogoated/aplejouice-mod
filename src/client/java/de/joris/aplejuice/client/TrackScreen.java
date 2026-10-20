package de.joris.aplejuice.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;

public final class TrackScreen extends Screen {
    public static final int W = 780, H = 410;
    private static final int HEADER = 40;
    private static final int TEXT = 0xFFFFEDDA, MUTED = 0xFFB7A394;
    private static final int PAGE = 3, ROW_Y = 122, ROW_STEP = 36;
    private final Screen parent;
    private int tab, offset;
    private EditBox search;
    private String query = "", notice = "";
    private String playerQuery = "";
    private List<AplejuiceClient.PlayerOption> playerOptions = List.of();
    private final Map<UUID, String> optionNames = new HashMap<>();
    private long lastPlayerRefresh;
    private List<Item> rows = List.of();
    private List<UUID> players = List.of();
    private final List<TrackButton> rowButtons = new ArrayList<>();
    private ClientLanguage language;
    private float scale, ox, oy;
    public TrackScreen(Screen parent) { super(Component.literal("Aplejouice Mod")); this.parent = parent; }
    private TrackConfig cfg() { return AplejuiceClient.config; }
    private String t(String key, Object... args) {
        String value = language.getOrDefault("ui.aplejuice." + key);
        return args.length == 0 ? value : String.format(Locale.ROOT, value, args);
    }
    private String itemName(Item item) { return language.getOrDefault(item.getDescriptionId()); }
    protected void init() {
        scale = Math.min(width / (float) W, height / (float) H);
        ox = (width - W * scale) / 2; oy = (height - H * scale) / 2;
        language = ClientLanguage.loadFrom(minecraft.getResourceManager(), List.of("en_us", cfg().language), false);
        search = null; rowButtons.clear();
        button(20, 66, 198, 27, t("track"), tab == 0, () -> select(0)).style(TrackButton.Kind.NAV, "eye_tab");
        button(20, 94, 198, 27, t("display"), tab == 1, () -> select(1)).style(TrackButton.Kind.NAV, "display");
        button(20, 122, 198, 27, t("players"), tab == 2, () -> select(2)).style(TrackButton.Kind.NAV, "players");
        button(25, 291, 90, 24, "Deutsch", cfg().language.equals("de_de"), () -> setLanguage("de_de")).style(TrackButton.Kind.FLAG, "flag_de");
        button(123, 291, 90, 24, "English", cfg().language.equals("en_us"), () -> setLanguage("en_us")).style(TrackButton.Kind.FLAG, "flag_en");
        button(16, 323, 208, 33, t("resetRound"), false, () -> { AplejuiceClient.reset(); notice = t("resetDone"); }).style(TrackButton.Kind.RESET, null);
        if (tab == 0) initItems();
        if (tab == 0 && AplejuiceClient.targetId() != null) {
            button(602, 246, 114, 20, t("clearTarget"), false, () -> { AplejuiceClient.clearTarget(); rebuildWidgets(); });
        }
        if (tab == 1) {
            toggle(77, cfg().enabled, () -> cfg().enabled = !cfg().enabled);
            toggle(132, cfg().showZero, () -> cfg().showZero = !cfg().showZero);
            button(658, 188, 58, 20, t("blocks", cfg().range), false, () -> {
                cfg().range = cfg().range == 24 ? 48 : cfg().range == 48 ? 64 : 24; saveRebuild();
            });
            toggle(242, cfg().opponentOnly, () -> cfg().opponentOnly = !cfg().opponentOnly);
        }
        if (tab == 2) {
            refreshPlayerSnapshot();
            search = new EditBox(font, 254, 78 + HEADER, 462, 20, Component.literal(t("playerSearch")));
            search.setMaxLength(64); search.setHint(Component.literal(t("playerSearchHint"))); search.setValue(playerQuery);
            search.setResponder(value -> { playerQuery = value; offset = 0; refreshPlayerRows(); });
            addRenderableWidget(search); refreshPlayerRows();
        }
    }
    private void refreshPlayerSnapshot() {
        playerOptions = AplejuiceClient.playerOptions(); optionNames.clear();
        for (var option : playerOptions) optionNames.put(option.id(), option.name());
        lastPlayerRefresh = AplejuiceClient.clientTick();
    }
    private void refreshPlayerRows() {
        rowButtons.forEach(this::removeWidget); rowButtons.clear();
        String filter = playerQuery.strip().toLowerCase(Locale.ROOT);
        players = playerOptions.stream().filter(p -> p.name().toLowerCase(Locale.ROOT).contains(filter)).map(AplejuiceClient.PlayerOption::id).toList();
        offset = Math.clamp(offset, 0, Math.max(0, players.size() - PAGE));
        for (int i = offset; i < Math.min(players.size(), offset + PAGE); i++) {
            UUID id = players.get(i); int y = ROW_Y + (i - offset) * ROW_STEP;
            rowButtons.add(button(663, y + 6, 53, 19, t("reset"), false, () -> { AplejuiceClient.counters.reset(id); notice = t("playerReset"); }));
            rowButtons.add(button(602, y + 6, 55, 19, t(id.equals(AplejuiceClient.targetId()) ? "selected" : "select"), id.equals(AplejuiceClient.targetId()), () -> {
                if (id.equals(AplejuiceClient.targetId())) {
                    AplejuiceClient.clearTarget(); offset = 0; rebuildWidgets(); return;
                }
                var player = minecraft.level == null ? null : minecraft.level.getPlayerByUUID(id);
                if (player != null) {
                    if (!cfg().enabled) { notice = t("trackingDisabled"); return; }
                    AplejuiceClient.selectTarget(player); offset = 0; rebuildWidgets();
                } else { notice = t("playerGone"); refreshPlayerSnapshot(); refreshPlayerRows(); }
            }));
        }
    }
    public void tick() {
        super.tick();
        if (tab == 2 && AplejuiceClient.clientTick() - lastPlayerRefresh >= 20) {
            refreshPlayerSnapshot(); refreshPlayerRows();
        }
    }
    private void setLanguage(String code) { cfg().language = code; notice = ""; saveRebuild(); }
    private void initItems() {
        search = new EditBox(font, 254, 78 + HEADER, 462, 20, Component.literal(t("search")));
        search.setMaxLength(128); search.setHint(Component.literal(t("searchHint")));
        search.setValue(query);
        search.setResponder(value -> { query = value; offset = 0; refreshRows(); });
        addRenderableWidget(search); refreshRows();
    }
    private void refreshRows() {
        rowButtons.forEach(this::removeWidget); rowButtons.clear();
        String q = query.toLowerCase(Locale.ROOT).trim();
        if (q.equals("cobwebs") || q.equals("webs") || q.equals("web")) q = "cobweb";
        final String filter = q;
        rows = q.isEmpty() ? cfg().items.stream().map(AplejuiceClient::item).filter(i -> i != Items.AIR).toList()
                : BuiltInRegistries.ITEM.stream().filter(i -> i != Items.AIR)
                .filter(i -> BuiltInRegistries.ITEM.getKey(i).toString().contains(filter)
                        || itemName(i).toLowerCase(Locale.ROOT).contains(filter)
                        || (i == Items.COBWEB && "spinnennetz".contains(filter)))
                .sorted(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString())).toList();
        offset = Math.clamp(offset, 0, Math.max(0, rows.size() - PAGE));
        for (int i = offset; i < Math.min(rows.size(), offset + PAGE); i++) {
            String id = BuiltInRegistries.ITEM.getKey(rows.get(i)).toString();
            boolean tracked = cfg().items.contains(id);
            rowButtons.add(button(661, ROW_Y + (i - offset) * ROW_STEP + 3, 55, 20, t(tracked ? "remove" : "add"), false, () -> {
                if (cfg().items.contains(id)) { cfg().items.remove(id); AplejuiceClient.counters.resetItem(id); }
                else if (cfg().items.size() < 8) { cfg().items.add(id); AplejuiceClient.counters.resetItem(id); }
                else notice = t("limit");
                if (!cfg().save()) notice = t("saveFailed"); refreshRows();
            }));
        }
    }
    private void toggle(int y, boolean value, Runnable action) {
        button(650, y, 66, 21, t(value ? "on" : "off"), value, () -> { action.run(); saveRebuild(); }).style(TrackButton.Kind.SWITCH, null);
    }
    private void saveRebuild() { if (!cfg().save()) notice = t("saveFailed"); rebuildWidgets(); }
    private void select(int value) { tab = value; offset = 0; notice = ""; rebuildWidgets(); }
    private TrackButton button(int x, int y, int w, int h, String text, boolean accent, Runnable action) {
        return addRenderableWidget(new TrackButton(x, y + HEADER, w, h, text, accent, action));
    }
    public void render(GuiGraphics g, int mx, int my, float delta) {
        int x = (int) ((mx - ox) / scale), y = (int) ((my - oy) / scale);
        g.pose().pushMatrix(); g.pose().translate(ox, oy); g.pose().scale(scale, scale);
        String brand = "Aplejouice Mod";
        int brandWidth = Math.round(font.width(Component.literal(brand).withStyle(ChatFormatting.BOLD)) * 2F);
        int brandX = (W - brandWidth - 38) / 2;
        GuiAssets.logo(g, brandX, 5, 32);
        scaled(g, Component.literal(brand).withStyle(ChatFormatting.BOLD), brandX + 38, 13, 0xFFFFC05A, 2F);
        g.pose().pushMatrix(); g.pose().translate(0, HEADER);
        panel(g, 14, 12, 212, 346);
        panel(g, 242, 12, 488, 282);
        g.fillGradient(16, 14, 224, 62, 0xFF593424, 0xFF352A25);
        g.fill(16, 62, 224, 64, 0xFF8B5634);
        scaled(g, Component.literal(t("categories")).withStyle(ChatFormatting.BOLD), 27, 30, TEXT, 1.4F);
        g.fillGradient(244, 14, 728, 62, 0xFF593424, 0xFF352A25);
        g.fill(244, 62, 728, 64, 0xFF8B5634);
        GuiAssets.draw(g, tab == 0 ? "eye_header" : tab == 1 ? "display" : "players", 255, 22, 32, 32);
        bold(g, t(tab == 0 ? "track" : tab == 1 ? "display" : "players"), 299, 25, TEXT);
        String subtitle = t(tab == 0 ? "trackSub" : tab == 1 ? "displaySub" : "playersSub");
        if (tab == 0) subtitle = cfg().opponentOnly
                ? AplejuiceClient.targetId() == null ? t("targetNone") : t("targetName", AplejuiceClient.names.getOrDefault(AplejuiceClient.targetId(), t("player")))
                : t("nearbyMode");
        if (tab == 0 && !cfg().enabled) subtitle = t("trackingDisabled");
        scaled(g, Component.literal(font.plainSubstrByWidth(subtitle, 275)), 299, 40, MUTED, 1.5F);
        g.fill(16, 322, 224, 323, 0xFF6D4934);
        if (tab == 0) renderItems(g);
        if (tab == 1) renderSettings(g);
        if (tab == 2) renderPlayers(g);
        if (!notice.isEmpty()) label(g, font.plainSubstrByWidth(notice, 380), 254, 276, 0xFFFFCA73);
        g.pose().popMatrix();
        super.render(g, x, y, delta);
        g.pose().popMatrix();
    }
    private void renderItems(GuiGraphics g) {
        label(g, t(query.isBlank() ? "watchlist" : "results"), 254, 109, MUTED);
        label(g, cfg().items.size() + " / 8", 685, 109, MUTED);
        for (int i = offset; i < Math.min(rows.size(), offset + PAGE); i++) {
            Item item = rows.get(i); int y = ROW_Y + (i - offset) * ROW_STEP;
            g.renderItem(new ItemStack(item), 254, y + 4);
            bold(g, font.plainSubstrByWidth(itemName(item), 245), 274, y + 2, TEXT);
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            boolean detectable = AplejuiceClient.observable(item);
            label(g, font.plainSubstrByWidth(id + (!AplejuiceClient.authoritative && !detectable ? " · " + t("serverRequired") : ""), 377), 274, y + 19, MUTED);
            g.fill(254, y + 34, 716, y + 35, 0xFF493C32);
        }
        if (rows.isEmpty()) label(g, t(query.isBlank() ? "empty" : "notFound"), 254, 132, MUTED);
        if (rows.size() > PAGE) label(g, t("scroll", offset + 1, Math.min(offset + PAGE, rows.size()), rows.size()), 254, 238, MUTED);
    }
    private void renderSettings(GuiGraphics g) {
        setting(g, 77, "setting_tracking", t("tracking"), t("trackingDesc"));
        setting(g, 132, "setting_zeros", t("zeros"), t("zerosDesc"));
        setting(g, 187, "setting_range", t("range"), t("rangeDesc"));
        setting(g, 242, "setting_opponent", t("opponentOnly"), t("opponentDesc"));
    }
    private void setting(GuiGraphics g, int y, String icon, String title, String subtitle) {
        GuiAssets.draw(g, icon, 254, y, 16, 16);
        bold(g, title, 278, y + 2, TEXT);
        wrap(g, subtitle, 278, y + 17, 362);
        g.fill(254, y + 46, 716, y + 47, 0xFF493C32);
    }
    private void renderPlayers(GuiGraphics g) {
        label(g, t("loadedPlayers", players.size(), playerOptions.size()), 254, 109, MUTED);
        for (int i = offset; i < Math.min(players.size(), offset + PAGE); i++) {
            UUID id = players.get(i); int y = ROW_Y + (i - offset) * ROW_STEP;
            bold(g, font.plainSubstrByWidth(optionNames.getOrDefault(id, t("player")), 245), 254, y + 2, TEXT);
            int x = 254;
            if (!AplejuiceClient.isWatched(id)) label(g, t("notTracked"), 254, y + 21, MUTED);
            else for (String item : cfg().items) {
                String count = Long.toString(AplejuiceClient.counters.get(id, item));
                int next = x + 30 + font.width(count);
                if (next > 595) break;
                g.renderItem(new ItemStack(AplejuiceClient.item(item)), x, y + 17);
                label(g, count, x + 20, y + 22, 0xFFFFC05A); x = next;
            }
            g.fill(254, y + 35, 716, y + 36, 0xFF493C32);
        }
        if (players.isEmpty()) label(g, t(playerQuery.isBlank() ? "noPlayers" : "noPlayerMatch"), 254, 134, MUTED);
        if (players.size() > PAGE) label(g, t("scroll", offset + 1, Math.min(offset + PAGE, players.size()), players.size()), 254, 248, MUTED);
    }
    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        GuiAssets.rounded(g, x + 2, y + 2, w, h, 4, 0xFF181612, 0xFF181612);
        GuiAssets.rounded(g, x, y, w, h, 4, 0xFF76543C, 0xFF2C2824);
    }
    private void label(GuiGraphics g, String text, int x, int y, int color) { g.drawString(font, text, x, y, color, false); }
    private void bold(GuiGraphics g, String text, int x, int y, int color) { scaled(g, Component.literal(text).withStyle(ChatFormatting.BOLD), x, y, color, 1.5F); }
    private void scaled(GuiGraphics g, Component text, int x, int y, int color, float size) {
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(size, size);
        g.drawString(font, text, 0, 0, color, false); g.pose().popMatrix();
    }
    private void wrap(GuiGraphics g, String text, int x, int y, int width) {
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(1.5F, 1.5F);
        g.drawWordWrap(font, Component.literal(text), 0, 0, (int) (width / 1.5F), MUTED, false); g.pose().popMatrix();
    }
    private MouseButtonEvent local(MouseButtonEvent e) { return new MouseButtonEvent((e.x() - ox) / scale, (e.y() - oy) / scale, e.buttonInfo()); }
    public boolean mouseClicked(MouseButtonEvent e, boolean twice) { return super.mouseClicked(local(e), twice); }
    public boolean mouseReleased(MouseButtonEvent e) { return super.mouseReleased(local(e)); }
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { return super.mouseDragged(local(e), dx / scale, dy / scale); }
    public void mouseMoved(double x, double y) { super.mouseMoved((x - ox) / scale, (y - oy) / scale); }
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if ((x - ox) / scale >= 242 && dy != 0) {
            int size = tab == 0 ? rows.size() : tab == 2 ? players.size() : 0;
            offset = Math.clamp(offset - (int) Math.signum(dy), 0, Math.max(0, size - PAGE));
            if (tab == 0) refreshRows(); else if (tab == 2) refreshPlayerRows();
            return true;
        }
        return super.mouseScrolled((x - ox) / scale, (y - oy) / scale, dx, dy);
    }
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 74 && (search == null || !search.isFocused())) { onClose(); return true; }
        return super.keyPressed(event);
    }
    public void onClose() {
        if (!cfg().save()) { notice = t("saveFailed"); return; }
        minecraft.setScreen(parent);
    }
    public boolean isPauseScreen() { return false; }
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
        if (minecraft.level == null) renderPanorama(g, delta);
        // Native Minecraft blur keeps the current world visible beneath the settings window.
        g.blurBeforeThisStratum();
        g.fill(0, 0, width, height, 0x66271E16);
    }
}

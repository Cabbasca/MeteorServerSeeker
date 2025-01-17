package de.damcraft.serverseeker.gui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.damcraft.serverseeker.ServerSeekerSystem;
import de.damcraft.serverseeker.SmallHttp;
import de.damcraft.serverseeker.mixin.MultiplayerScreenAccessor;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerInfo;

public class FindNewServersScreen extends WindowScreen {
    private int timer;
    public WButton findButton;

    public enum cracked {
        Any,
        Yes,
        No
    }

    public enum version {
        Current,
        Any,
        Custom
    }

    public enum software {
        Any,
        Vanilla,
        Paper,
        Spigot,
        Bukkit
    }

    public enum NumRangeType {
        Any,
        Equals,
        At_Least,
        At_Most,
        Between
    }

    private final Settings settings = new Settings();
    private final SettingGroup sg = settings.getDefaultGroup();
    WContainer settingsContainer;

    private final Setting<cracked> crackedSetting = sg.add(new EnumSetting.Builder<cracked>()
        .name("cracked")
        .description("Whether the server should be cracked or not")
        .defaultValue(cracked.Any)
        .build()
    );

    private final Setting<NumRangeType> onlinePlayersNumTypeSetting = sg.add(new EnumSetting.Builder<NumRangeType>()
        .name("online-players")
        .description("The type of number range for the online players")
        .defaultValue(NumRangeType.Any)
        .build()
    );

    private final Setting<Integer> equalsOnlinePlayersSetting = sg.add(new IntSetting.Builder()
        .name("online-player-equals")
        .description("The amount of online players the server should have")
        .defaultValue(2)
        .min(0)
        .visible(() -> onlinePlayersNumTypeSetting.get().equals(NumRangeType.Equals))
        .noSlider()
        .build()
    );

    private final Setting<Integer> atLeastOnlinePlayersSetting = sg.add(new IntSetting.Builder()
        .name("online-player-at-least")
        .description("The minimum amount of online players the server should have")
        .defaultValue(1)
        .min(0)
        .visible(() -> onlinePlayersNumTypeSetting.get().equals(NumRangeType.At_Least) || onlinePlayersNumTypeSetting.get().equals(NumRangeType.Between))
        .noSlider()
        .build()
    );

    private final Setting<Integer> atMostOnlinePlayersSetting = sg.add(new IntSetting.Builder()
        .name("online-player-at-most")
        .description("The maximum amount of online players the server should have")
        .defaultValue(20)
        .min(0)
        .visible(() -> onlinePlayersNumTypeSetting.get().equals(NumRangeType.At_Most) || onlinePlayersNumTypeSetting.get().equals(NumRangeType.Between))
        .noSlider()
        .build()
    );


    private final Setting<NumRangeType> maxPlayersNumTypeSetting = sg.add(new EnumSetting.Builder<NumRangeType>()
        .name("max-players")
        .description("The type of number range for the max players")
        .defaultValue(NumRangeType.Any)
        .build()
    );

    private final Setting<Integer> equalsMaxPlayersSetting = sg.add(new IntSetting.Builder()
        .name("max-players-equals")
        .description("The amount of max players the server should have")
        .defaultValue(2)
        .min(0)
        .visible(() -> maxPlayersNumTypeSetting.get().equals(NumRangeType.Equals))
        .noSlider()
        .build()
    );

    private final Setting<Integer> atLeastMaxPlayersSetting = sg.add(new IntSetting.Builder()
        .name("max-players-at-least")
        .description("The minimum amount of max players the server should have")
        .defaultValue(1)
        .min(0)
        .visible(() -> maxPlayersNumTypeSetting.get().equals(NumRangeType.At_Least) || maxPlayersNumTypeSetting.get().equals(NumRangeType.Between))
        .noSlider()
        .build()
    );

    private final Setting<Integer> atMostMaxPlayersSetting = sg.add(new IntSetting.Builder()
        .name("max-players-at-most")
        .description("The maximum amount of max players the server should have")
        .defaultValue(20)
        .min(0)
        .visible(() -> maxPlayersNumTypeSetting.get().equals(NumRangeType.At_Most) || maxPlayersNumTypeSetting.get().equals(NumRangeType.Between))
        .noSlider()
        .build()
    );

    private final Setting<String> descriptionSetting = sg.add(new StringSetting.Builder()
        .name("description")
        .description("The description (aka motd) the servers should have (empty for any)")
        .defaultValue("")
        .build()
    );

    private final Setting<software> softwareSetting = sg.add(new EnumSetting.Builder<software>()
        .name("software")
        .description("The software the servers should have")
        .defaultValue(software.Any)
        .build()
    );

    private final Setting<version> versionSetting = sg.add(new EnumSetting.Builder<version>()
        .name("version")
        .description("The protocol version the servers should have")
        .defaultValue(version.Current)
        .build()
    );

    private final Setting<Integer> customProtocolSetting = sg.add(new IntSetting.Builder()
        .name("protocol")
        .description("The protocol version the servers should have")
        .defaultValue(SharedConstants.getProtocolVersion())
        .visible(() -> versionSetting.get() == version.Custom)
        .min(0)
        .noSlider()
        .build()
    );

    private final Setting<Boolean> onlineOnly = sg.add(new BoolSetting.Builder()
        .name("online-only")
        .description("Whether to only show servers that are online")
        .defaultValue(true)
        .build()
    );


    MultiplayerScreen multiplayerScreen;


    public FindNewServersScreen(MultiplayerScreen multiplayerScreen) {
        super(GuiThemes.get(), "Find new servers");
        this.multiplayerScreen = multiplayerScreen;
    }

    @Override
    public void initWidgets() {

        settingsContainer = add(theme.verticalList()).widget();
        settingsContainer.add(theme.settings(settings));
        findButton = add(theme.button("Find")).expandX().widget();
        findButton.action = () -> {

            String apiKey = Systems.get(ServerSeekerSystem.class).apiKey;

            // Create a new JSON object
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("api_key", apiKey);
            if (!onlinePlayersNumTypeSetting.get().equals(NumRangeType.Any)) {
                if (onlinePlayersNumTypeSetting.get().equals(NumRangeType.Equals)) {
                    jsonObject.addProperty("online_players", equalsOnlinePlayersSetting.get());
                } else if (onlinePlayersNumTypeSetting.get().equals(NumRangeType.At_Least)) {
                    // [n, "inf"]
                    JsonArray jsonArray = new JsonArray();
                    jsonArray.add(atLeastOnlinePlayersSetting.get());
                    jsonArray.add("inf");
                    jsonObject.add("online_players", jsonArray);
                } else if (onlinePlayersNumTypeSetting.get().equals(NumRangeType.At_Most)) {
                    // [0, n]
                    JsonArray jsonArray = new JsonArray();
                    jsonArray.add(0);
                    jsonArray.add(atMostOnlinePlayersSetting.get());
                    jsonObject.add("online_players", jsonArray);
                } else if (onlinePlayersNumTypeSetting.get().equals(NumRangeType.Between)) {
                    // [min, max]
                    JsonArray jsonArray = new JsonArray();
                    jsonArray.add(atLeastOnlinePlayersSetting.get());
                    jsonArray.add(atMostOnlinePlayersSetting.get());
                    jsonObject.add("online_players", jsonArray);
                }
            }
            if (!maxPlayersNumTypeSetting.get().equals(NumRangeType.Any)) {
                if (maxPlayersNumTypeSetting.get().equals(NumRangeType.Equals)) {
                    jsonObject.addProperty("max_players", equalsMaxPlayersSetting.get());
                } else if (maxPlayersNumTypeSetting.get().equals(NumRangeType.At_Least)) {
                    // [n, "inf"]
                    JsonArray jsonArray = new JsonArray();
                    jsonArray.add(atLeastMaxPlayersSetting.get());
                    jsonArray.add("inf");
                    jsonObject.add("max_players", jsonArray);
                } else if (maxPlayersNumTypeSetting.get().equals(NumRangeType.At_Most)) {
                    // [0, n]
                    JsonArray jsonArray = new JsonArray();
                    jsonArray.add(0);
                    jsonArray.add(atMostMaxPlayersSetting.get());
                    jsonObject.add("max_players", jsonArray);
                } else if (maxPlayersNumTypeSetting.get().equals(NumRangeType.Between)) {
                    // [min, max]
                    JsonArray jsonArray = new JsonArray();
                    jsonArray.add(atLeastMaxPlayersSetting.get());
                    jsonArray.add(atMostMaxPlayersSetting.get());
                    jsonObject.add("max_players", jsonArray);
                }
            }
            if (crackedSetting.get() != cracked.Any) {
                jsonObject.addProperty("cracked", crackedSetting.get() == cracked.Yes);
            }
            if (!descriptionSetting.get().isEmpty()) {
                jsonObject.addProperty("description", descriptionSetting.get());
            }
            if (softwareSetting.get() != software.Any) {
                jsonObject.addProperty("software", softwareSetting.get().toString().toLowerCase());
            }
            if (versionSetting.get() == version.Custom) {
                jsonObject.addProperty("protocol", customProtocolSetting.get());
            } else if (versionSetting.get() == version.Current) {
                jsonObject.addProperty("protocol", SharedConstants.getProtocolVersion());
            }
            if (!onlineOnly.get()) {
                jsonObject.addProperty("online_after", 0);
            }
            this.locked = true;
            MeteorExecutor.execute(() -> {
                String json = jsonObject.toString();

                String jsonResp = SmallHttp.post("https://api.serverseeker.net/servers", json);

                Gson gson = new Gson();
                JsonObject resp = gson.fromJson(jsonResp, JsonObject.class);

                // Set error message if there is one
                String error = resp.has("error") ? resp.get("error").getAsString() : null;
                if (error != null) {
                    clear();
                    add(theme.label(error)).expandX();
                    WButton backButton = add(theme.button("Back")).expandX().widget();
                    backButton.action = this::reload;
                    this.locked = false;
                    return;
                }
                clear();
                JsonArray servers = resp.getAsJsonArray("data");
                if (servers.isEmpty()) {
                    add(theme.label("No servers found")).expandX();
                    WButton backButton = add(theme.button("Back")).expandX().widget();
                    backButton.action = this::reload;
                    this.locked = false;
                    return;
                }
                add(theme.label("Found " + servers.size() + " servers")).expandX();
                WButton addAllButton = add(theme.button("Add all")).expandX().widget();
                addAllButton.action = () -> {
                    for (JsonElement server : servers) {
                        String ip = server.getAsJsonObject().get("server").getAsString();

                        ServerInfo info = new ServerInfo("ServerSeeker " + ip, ip, false);

                        // Add server to list
                        this.multiplayerScreen.getServerList().add(info, false);
                    }
                    this.multiplayerScreen.getServerList().saveFile();

                    // Reload widget
                    ((MultiplayerScreenAccessor) this.multiplayerScreen).getServerListWidget().setServers(this.multiplayerScreen.getServerList());

                    // Close screen
                    if (this.client == null) return;
                    client.setScreen(this.multiplayerScreen);
                };
                this.locked = false;
            });
        };
    }

    @Override
    public void tick() {
        super.tick();
        settings.tick(settingsContainer, theme);

        if (locked) {
            if (timer > 2) {
                findButton.set(getNext(findButton));
                timer = 0;
            }
            else {
                timer++;
            }
        }

        else if (!findButton.getText().equals("Find")) {
            findButton.set("Find");
        }
    }

    private String getNext(WButton add) {
        return switch (add.getText()) {
            case "Find", "oo0" -> "ooo";
            case "ooo" -> "0oo";
            case "0oo" -> "o0o";
            case "o0o" -> "oo0";
            default -> "Find";
        };
    }
}

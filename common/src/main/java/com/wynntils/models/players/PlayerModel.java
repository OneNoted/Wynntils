/*
 * Copyright © Wynntils 2022-2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.players;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.net.ApiResponse;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.PlayerJoinedWorldEvent;
import com.wynntils.mc.event.PlayerTeamEvent;
import com.wynntils.models.players.type.wynnplayer.CharacterData;
import com.wynntils.models.players.type.wynnplayer.WynnPlayerInfo;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.services.secrets.type.WynntilsSecret;
import com.wynntils.utils.mc.McUtils;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;

public final class PlayerModel extends Model {
    public static final Gson PLAYER_GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(WynnPlayerInfo.class, new WynnPlayerInfo.WynnPlayerInfoDeserializer())
            .registerTypeHierarchyAdapter(CharacterData.class, new CharacterData.CharacterDataDeserializer())
            .create();
    private static final Pattern GHOST_WORLD_PATTERN = Pattern.compile("^_([A-Z]+)(\\d+)$");

    private final Map<UUID, Integer> ghosts = new ConcurrentHashMap<>();
    private final Map<UUID, String> nameMap = new ConcurrentHashMap<>();

    public PlayerModel() {
        super(List.of());
    }

    // Returns true if the player is on the same server and is not a npc
    public boolean isLocalPlayer(Player player) {
        // All local players have UUID version 4, which is a proper Minecraft UUID
        return player.getUUID().version() == 4;
    }

    public boolean isLocalPlayer(String name) {
        // Wynn uses TeamNames for player names that are online
        return !isNpcName(StyledText.fromString(name))
                && McUtils.mc().level.getScoreboard().getTeamNames().contains(name);
    }

    public boolean isNpc(Player player) {
        StyledText scoreboardName = StyledText.fromString(player.getScoreboardName());
        return isNpcName(scoreboardName);
    }

    public boolean isDisplayPlayer(Player player) {
        // This is a "fake" player, like an NPC but that you can't interact with,
        // like the player at the character selection screen
        var uuid = player.getUUID();
        if (uuid.version() == 4) return false;
        if (isNpc(player)) return false;
        if (isPlayerGhost(player)) return false;

        return true;
    }

    public boolean isPlayerGhost(Player player) {
        return ghosts.containsKey(player.getUUID());
    }

    public UUID getUserUUID(Player player) {
        var uuid = player.getUUID();
        if (uuid == null) return null;

        if (uuid.version() == 4) {
            // This is a local player
            return uuid;
        } else {
            // Ghost players have their UUIDs converted to version 2; convert it back
            return new UUID((uuid.getMostSignificantBits() & ~0xF000L) | 0x4000L, uuid.getLeastSignificantBits());
        }
    }

    public WynntilsUser getWynntilsUser(Player player) {
        return null;
    }

    public Stream<String> getAllPlayerNames() {
        return nameMap.values().stream();
    }

    public void reset() {
        clearNameMap();
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldState.NOT_CONNECTED) {
            clearNameMap();
            reset();
        }
        if (event.getNewState() == WorldState.WORLD) {
            clearGhostCache();

            // Keep our own name indexed even though Athena-backed metadata is disabled.
            loadSelf();
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerJoinedWorldEvent event) {
        if (!Models.WorldState.onWorld()) return;

        Player player = event.getPlayer();
        if (player == null || player.getUUID() == null) return;
        StyledText name = StyledText.fromString(player.getGameProfile().name());
        if (isNpc(player) || isDisplayPlayer(player)) return;

        loadUser(Models.Player.getUserUUID(player), name.getString());
    }

    @SubscribeEvent
    public void onAddPlayerToTeam(PlayerTeamEvent.Added event) {
        PlayerInfo playerInfo = McUtils.mc().getConnection().getPlayerInfo(event.getUsername());
        if (playerInfo == null) return;

        UUID uuid = playerInfo.getProfile().id();
        if (uuid == null) return;

        PlayerTeam playerTeam = event.getPlayerTeam();

        Matcher matcher = GHOST_WORLD_PATTERN.matcher(playerTeam.getName());
        if (!matcher.matches()) {
            // Maybe it should not be a ghost anymore
            ghosts.remove(uuid);
            return;
        }

        int world = Integer.parseInt(matcher.group(2));
        ghosts.put(uuid, world);
    }

    public void loadSelf() {
        Player player = McUtils.player();
        if (player == null) return;

        UUID uuid = player.getUUID();
        if (uuid == null) return;

        loadUser(uuid, player.getScoreboardName());
    }

    private void loadUser(UUID uuid, String userName) {
        nameMap.put(uuid, userName);
    }

    public CompletableFuture<WynnPlayerInfo> getPlayer(String username) {
        CompletableFuture<WynnPlayerInfo> future = new CompletableFuture<>();

        Map<String, String> authHeader = new HashMap<>();
        String apiToken = Services.Secrets.getSecret(WynntilsSecret.WYNNCRAFT_API_TOKEN);
        if (!apiToken.isEmpty()) {
            authHeader.put("Authorization", "Bearer " + apiToken);
        }

        ApiResponse apiResponse =
                Managers.Net.callApi(UrlId.DATA_WYNNCRAFT_PLAYER, Map.of("username", username), authHeader);
        apiResponse.handleJsonObject(
                json -> {
                    Type type = new TypeToken<WynnPlayerInfo>() {}.getType();

                    future.complete(PLAYER_GSON.fromJson(json, type));
                },
                onError -> future.complete(null));

        return future;
    }

    public CompletableFuture<WynnPlayerInfo> getPlayerFullInfo(String identifier) {
        CompletableFuture<WynnPlayerInfo> future = new CompletableFuture<>();

        Map<String, String> authHeader = new HashMap<>();
        String apiToken = Services.Secrets.getSecret(WynntilsSecret.WYNNCRAFT_API_TOKEN);
        if (!apiToken.isEmpty()) {
            authHeader.put("Authorization", "Bearer " + apiToken);
        }

        ApiResponse apiResponse = Managers.Net.callApi(
                UrlId.DATA_WYNNCRAFT_PLAYER_FULL_RESULTS, Map.of("identifier", identifier), authHeader);
        apiResponse.handleJsonObject(
                json -> {
                    Type type = new TypeToken<WynnPlayerInfo>() {}.getType();

                    future.complete(PLAYER_GSON.fromJson(json, type));
                },
                onError -> future.complete(null));

        return future;
    }

    private void clearNameMap() {
        nameMap.clear();
    }

    private void clearGhostCache() {
        ghosts.clear();
    }

    private boolean isNpcName(StyledText name) {
        return name.equals("\uE000");
    }

    private boolean isNpcUuid(UUID uuid) {
        // All players have UUID version 4,
        // while most NPCs have UUID version 2
        // Starting Wynncraft 2.1, all NPCs will have UUID version 2
        return uuid.version() == 2;
    }
}

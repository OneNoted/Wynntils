/*
 * Copyright © Wynntils 2022-2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.playerviewer;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.inventory.type.InventoryAccessory;
import com.wynntils.models.inventory.type.InventoryArmor;
import com.wynntils.screens.base.WynntilsContainerScreen;
import com.wynntils.screens.playerviewer.widgets.FriendButton;
import com.wynntils.screens.playerviewer.widgets.PartyButton;
import com.wynntils.screens.playerviewer.widgets.PlayerInteractionButton;
import com.wynntils.screens.playerviewer.widgets.SimplePlayerInteractionButton;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team.Visibility;

public final class PlayerViewerScreen extends WynntilsContainerScreen<PlayerViewerMenu> {
    private static final String TEAM_NAME = "PlayerViewerTeam";

    private final Player player;
    private final Scoreboard scoreboard;
    private final PlayerTeam playerViewerTeam;
    private final PlayerTeam oldTeam;
    private final List<PlayerInteractionButton> interactionButtons = new ArrayList<>();

    private FriendButton friendButton;
    private PartyButton partyButton;

    private PlayerViewerScreen(Player player, PlayerViewerMenu menu) {
        super(menu, player.getInventory(), Component.empty());

        this.player = player;
        this.scoreboard = player.level().getScoreboard();

        if (scoreboard.getTeamNames().contains(TEAM_NAME)) {
            playerViewerTeam = scoreboard.getPlayerTeam(TEAM_NAME);
        } else {
            playerViewerTeam = scoreboard.addPlayerTeam(TEAM_NAME);
            playerViewerTeam.setNameTagVisibility(Visibility.NEVER);
        }

        // this is done to prevent the player's nametag from rendering in the GUI
        oldTeam = scoreboard.getPlayersTeam(player.getScoreboardName());
        scoreboard.addPlayerToTeam(player.getScoreboardName(), playerViewerTeam);
    }

    public static Screen create(Player player) {
        List<ItemStack> armorItems = new ArrayList<>();
        List<ItemStack> accessoryItems = new ArrayList<>();
        for (InventoryAccessory accessory : InventoryAccessory.values()) {
            accessoryItems.add(ItemStack.EMPTY);
        }
        for (InventoryArmor armor : InventoryArmor.values()) {
            armorItems.add(ItemStack.EMPTY);
        }

        return new PlayerViewerScreen(player, PlayerViewerMenu.create(ItemStack.EMPTY, armorItems, accessoryItems));
    }

    @Override
    protected void doInit() {
        interactionButtons.clear();
        this.leftPos = (this.width - Texture.PLAYER_VIEWER_BACKGROUND.width()) / 2;
        this.topPos = (this.height - Texture.PLAYER_VIEWER_BACKGROUND.height()) / 2;

        String playerName = StyledText.fromComponent(player.getName()).getStringWithoutFormatting();

        // left
        // view player stats button
        interactionButtons.add(new SimplePlayerInteractionButton(
                leftPos - 21,
                topPos + (Texture.PLAYER_VIEWER_BACKGROUND.height() / 5 - 2),
                Component.translatable("screens.wynntils.playerViewer.viewStats"),
                Texture.STATS_ICON,
                () -> Managers.Net.openLink(UrlId.LINK_WYNNCRAFT_PLAYER_STATS, Map.of("username", playerName))));

        // add friend button
        friendButton = new FriendButton(
                leftPos - 21, topPos + (Texture.PLAYER_VIEWER_BACKGROUND.height() / 5) + 18, playerName);
        interactionButtons.add(friendButton);

        // invite party button
        partyButton = new PartyButton(
                leftPos - 21, topPos + (Texture.PLAYER_VIEWER_BACKGROUND.height() / 5) + 38, playerName);
        interactionButtons.add(partyButton);

        // right
        // duel button
        interactionButtons.add(new SimplePlayerInteractionButton(
                leftPos + Texture.PLAYER_VIEWER_BACKGROUND.width() + 1,
                topPos + (Texture.PLAYER_VIEWER_BACKGROUND.height() / 5) - 2,
                Component.translatable("screens.wynntils.playerViewer.duel"),
                Texture.DUEL_ICON,
                () -> Handlers.Command.queueCommand("duel " + playerName)));

        // trade button
        interactionButtons.add(new SimplePlayerInteractionButton(
                leftPos + Texture.PLAYER_VIEWER_BACKGROUND.width() + 1,
                topPos + (Texture.PLAYER_VIEWER_BACKGROUND.height() / 5) + 18,
                Component.translatable("screens.wynntils.playerViewer.trade"),
                Texture.TRADE_ICON,
                () -> Handlers.Command.queueCommand("trade " + playerName)));

        // msg button
        interactionButtons.add(new SimplePlayerInteractionButton(
                leftPos + Texture.PLAYER_VIEWER_BACKGROUND.width() + 1,
                topPos + (Texture.PLAYER_VIEWER_BACKGROUND.height() / 5) + 38,
                Component.translatable("screens.wynntils.playerViewer.message"),
                Texture.MESSAGE_ICON,
                () -> {
                    this.onClose(); // Required so that nametags render properly
                    McUtils.openChatScreen("/msg " + playerName + " ");
                }));
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.doRender(guiGraphics, mouseX, mouseY, partialTick);

        renderPlayerModel(guiGraphics, mouseX, mouseY);

        this.renderTooltip(guiGraphics, mouseX, mouseY);

        interactionButtons.forEach(button -> button.render(guiGraphics, mouseX, mouseY, partialTick));
    }

    private void renderPlayerModel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int renderX = (this.width - Texture.PLAYER_VIEWER_BACKGROUND.width()) / 2 + 13;
        int renderY = (this.height - Texture.PLAYER_VIEWER_BACKGROUND.height()) / 2 - 4;

        int renderWidth = Texture.PLAYER_VIEWER_BACKGROUND.width();
        int renderHeight = Texture.PLAYER_VIEWER_BACKGROUND.height();

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                renderX,
                renderY,
                renderX + renderWidth,
                renderY + renderHeight,
                30,
                0.2f,
                mouseX,
                mouseY,
                player);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderUtils.drawTexturedRect(guiGraphics, Texture.PLAYER_VIEWER_BACKGROUND, this.leftPos, this.topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // we don't want to draw any labels
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        for (PlayerInteractionButton interactionButton : interactionButtons) {
            if (interactionButton.mouseClicked(event, isDoubleClick)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        // do nothing here, because we don't want the user interacting with slots at all
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == McUtils.options().keyInventory.key.getValue()) {
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        // restore previous scoreboard team setup
        scoreboard.removePlayerFromTeam(player.getScoreboardName(), playerViewerTeam);
        if (oldTeam != null) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), oldTeam);
        }

        super.onClose();
    }

    public Player getPlayer() {
        return player;
    }

    public void updateButtonIcons() {
        friendButton.updateIcon();
        partyButton.updateIcon();
    }
}

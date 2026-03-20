/*
 * Copyright © Wynntils 2023-2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.cosmetics;

import com.wynntils.core.components.Service;
import com.wynntils.models.players.WynntilsUser;
import com.wynntils.services.cosmetics.type.WynntilsLayer;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class CosmeticsService extends Service {
    public CosmeticsService() {
        super(List.of());
    }

    // Keep the mixin entrypoint, but stop registering Athena-backed render layers.
    public static List<
                    BiFunction<
                            LivingEntityRenderer<AbstractClientPlayer, AvatarRenderState, PlayerModel>,
                            EntityRendererProvider.Context,
                            WynntilsLayer>>
            getRegisteredLayers() {
        return List.of();
    }

    public boolean shouldRenderCape(Player player, boolean elytra) {
        return false;
    }

    public Identifier getCapeTexture(Player player) {
        return null;
    }

    public void loadCosmeticTextures(UUID uuid, WynntilsUser user) {}
}

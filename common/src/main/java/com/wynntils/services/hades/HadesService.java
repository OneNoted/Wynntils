/*
 * Copyright © Wynntils 2022-2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.hades;

import com.wynntils.core.components.Service;
import com.wynntils.hades.protocol.enums.SocialType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class HadesService extends Service {
    public HadesService() {
        super(List.of());
    }

    public Stream<HadesUser> getHadesUsers() {
        return Stream.empty();
    }

    public Optional<HadesUser> getHadesUser(UUID uuid) {
        return Optional.empty();
    }

    public void tryResendWorldData() {}

    public void resetSocialType(SocialType socialType) {}

    public void resetHadesUsers() {}
}

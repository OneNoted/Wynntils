/*
 * Copyright © Wynntils 2022-2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.athena;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Service;
import com.wynntils.core.net.ApiResponse;
import com.wynntils.core.net.UrlId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WynntilsAccountService extends Service {
    private static final String NO_TOKEN = "";

    private final HashMap<String, String> encodedConfigs = new HashMap<>();

    public WynntilsAccountService() {
        super(List.of());
    }

    @Override
    public void reloadData() {}

    // Keep the service surface, but stop performing Athena auth or user-specific requests.
    public ApiResponse callApi(UrlId urlId, Map<String, String> arguments) {
        return Managers.Net.callApi(urlId, arguments, Map.of("authToken", NO_TOKEN));
    }

    public ApiResponse callApi(UrlId urlId) {
        return callApi(urlId, Map.of());
    }

    public String getToken() {
        return NO_TOKEN;
    }

    public boolean isLoggedIn() {
        return false;
    }

    public Map<String, String> getEncodedConfigs() {
        return encodedConfigs;
    }

    public void dumpEncodedConfig(String name) {
        encodedConfigs.remove(name);
    }
}

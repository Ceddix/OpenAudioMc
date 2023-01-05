package com.craftmend.openaudiomc.generic.authentication.driver;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.generic.authentication.AuthenticationService;
import com.craftmend.openaudiomc.generic.authentication.requests.ClientTokenRequestBody;
import com.craftmend.openaudiomc.generic.authentication.requests.SimpleTokenResponse;
import com.craftmend.openaudiomc.generic.networking.interfaces.Authenticatable;
import com.craftmend.openaudiomc.generic.networking.rest.RestRequest;
import com.craftmend.openaudiomc.generic.networking.rest.endpoints.RestEndpoint;
import com.craftmend.openaudiomc.generic.networking.rest.interfaces.ApiResponse;
import com.craftmend.openaudiomc.generic.platform.interfaces.TaskService;
import com.craftmend.openaudiomc.generic.utils.data.ConcurrentHeatMap;
import com.craftmend.openaudiomc.generic.networking.rest.Task;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class AuthenticationDriver {

    private final AuthenticationService service;
    @Getter private ConcurrentHeatMap<UUID, String> sessionCacheMap = null;

    public void removePlayerFromCache(UUID uuid) {
        if (sessionCacheMap == null) {
            initCache();
        }
        sessionCacheMap.delete(uuid);
    }

    public void initCache() {
        sessionCacheMap = new ConcurrentHeatMap<>(60, 100, () -> {
            return "";
        });
    }

    public Task<String> createPlayerSession(Authenticatable authenticatable) {
        if (sessionCacheMap == null) {
            initCache();
        }

        Task<String> task = new Task<>();

        OpenAudioMc.resolveDependency(TaskService.class).runAsync(() -> {
            // check ache, since there might be a value
            sessionCacheMap.clean();
            ConcurrentHeatMap<UUID, String>.Value entry = sessionCacheMap.get(authenticatable.getOwner().getUniqueId());
            if (!entry.getContext().isEmpty()) {
                task.finish(entry.getContext());
                return;
            }

            // create request
            ClientTokenRequestBody requestBody = new ClientTokenRequestBody(
                    "ACCOUNT",
                    authenticatable.getOwner().getName(),
                    authenticatable.getOwner().getUniqueId().toString(),
                    authenticatable.getAuth().getWebSessionKey(),
                    service.getServerKeySet().getPublicKey().getValue(),
                    service.getIdentity()
            );

            ApiResponse request = new RestRequest(RestEndpoint.CREATE_SESSION_TOKEN)
                    .setBody(requestBody)
                    .executeInThread();

            if (request.getStatusCode() != 200) {
                task.fail("Status code " + request.getStatusCode() + " is not 200");
                return;
            }

            String token = request.getResponse(SimpleTokenResponse.class).getToken();
            task.finish(token);

            // push to cache
            entry.setContext(token);
            entry.bump();

            sessionCacheMap.clean();
        });
        return task;
    }
}

package com.craftmend.openaudiomc.generic.authentication.driver;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.generic.authentication.AuthenticationService;
import com.craftmend.openaudiomc.generic.authentication.requests.ClientTokenRequestBody;
import com.craftmend.openaudiomc.generic.authentication.requests.SimpleTokenResponse;
import com.craftmend.openaudiomc.generic.networking.interfaces.Authenticatable;
import com.craftmend.openaudiomc.generic.platform.interfaces.TaskService;
import com.craftmend.openaudiomc.generic.rest.RestRequest;
import com.craftmend.openaudiomc.generic.rest.target.Endpoint;
import com.craftmend.openaudiomc.generic.utils.data.ConcurrentHeatMap;
import com.craftmend.openaudiomc.generic.rest.Task;
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
                    service.getServerKeySet().getPublicKey().getValue()
            );

            RestRequest<SimpleTokenResponse> request = new RestRequest(SimpleTokenResponse.class, Endpoint.CREATE_SESSION_TOKEN);
            request.run();


            if (request.hasError()) {
                task.fail(request.getError());
                return;
            }

            String token = request.getResponse().getToken();
            task.finish(token);

            // push to cache
            entry.setContext(token);
            entry.bump();

            sessionCacheMap.clean();
        });
        return task;
    }
}

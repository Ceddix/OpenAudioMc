package com.craftmend.openaudiomc.generic.networking.rest.endpoints;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.generic.authentication.AuthenticationService;
import com.craftmend.openaudiomc.generic.authentication.objects.ServerKeySet;

public enum RestEndpoint {

    // modern cloud API's
    GET_HOST_DETAILS("https://cloud.openaudiomc.net/api/v2/ip", false),
    CREATE_SESSION_TOKEN("https://cloud.openaudiomc.net/session", false),
    DIRECT_REST("https://cloud.openaudiomc.net/direct-rest", false),

    // external calls
    GITHUB_VERSION_CHECK("https://raw.githubusercontent.com/Mindgamesnl/OpenAudioMc/master/plugin/protocol/static-resources/project_status.json", false),

    // account
    ACCOUNT_CLAIM_SERVER("https://account.craftmend.com/api/v1/public/openaudiomc/claim/_public_key_/_private_key_", true),

    GET_ACCOUNT_SETTINGS("https://account.openaudiomc.net/api/v1/installation/settings/_public_key_", true),

    // login requests a relay and registers it
    START_SESSION("https://cloud.openaudiomc.net/api/v2/plugin/login/_private_key_", true),
    END_SESSION("https://account.craftmend.com/api/v1/public/openaudiomc/logout/_private_key_", true),
    START_VOICE_SESSION("https://account.craftmend.com/api/v1/closed/openaudiomc/voice/start-session/_private_key_/_public_key_", true),
    END_VOICE_SESSION("https://account.craftmend.com/api/v1/closed/openaudiomc/voice/end-session/_private_key_/_public_key_", true),

    // plus
    REGISTER("https://account.openaudiomc.net/api/v1/installation/register", false),

    // voice
    VOICE_EVENT_BUS_PREAUTH("/eb/check", false),
    VOICE_EVENT_BUS("/eb/ws", false),

    // cdn
    CDN_CHECK("/api/validate", false),

    TEST_ENDPOINT_GOOGLE("https://google.com/", false),
    ;

    private String url;
    private final boolean translate;

    RestEndpoint(String url, boolean translate) {
        this.url = url;
        this.translate = translate;
    }

    public String getURL() {
        if (this.translate) {
            ServerKeySet keySet = OpenAudioMc.getService(AuthenticationService.class).getServerKeySet();
            this.url = this.url.replace("_private_key_", keySet.getPrivateKey().getValue());
            this.url = this.url.replace("_public_key_", keySet.getPublicKey().getValue());
        }
        return this.url;
    }
}

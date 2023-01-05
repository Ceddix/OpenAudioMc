package com.craftmend.openaudiomc.generic.craftmend;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.api.impl.event.events.AccountAddTagEvent;
import com.craftmend.openaudiomc.api.impl.event.events.AccountRemoveTagEvent;
import com.craftmend.openaudiomc.api.interfaces.AudioApi;
import com.craftmend.openaudiomc.generic.authentication.AuthenticationService;
import com.craftmend.openaudiomc.generic.craftmend.enums.AddonCategory;
import com.craftmend.openaudiomc.generic.craftmend.enums.CraftmendTag;
import com.craftmend.openaudiomc.generic.craftmend.response.OpenaudioSettingsResponse;
import com.craftmend.openaudiomc.generic.craftmend.response.VoiceSessionRequestResponse;
import com.craftmend.openaudiomc.generic.logging.OpenAudioLogger;
import com.craftmend.openaudiomc.generic.networking.interfaces.NetworkingService;
import com.craftmend.openaudiomc.generic.rest.RestRequest;
import com.craftmend.openaudiomc.generic.rest.ServerEnvironment;
import com.craftmend.openaudiomc.generic.platform.interfaces.TaskService;
import com.craftmend.openaudiomc.generic.proxy.interfaces.UserHooks;
import com.craftmend.openaudiomc.generic.rest.target.Endpoint;
import com.craftmend.openaudiomc.generic.service.Inject;
import com.craftmend.openaudiomc.generic.service.Service;
import com.craftmend.openaudiomc.generic.voicechat.bus.VoiceApiConnection;
import com.craftmend.openaudiomc.generic.voicechat.enums.VoiceApiStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
public class CraftmendService extends Service {

    @Inject
    private OpenAudioMc openAudioMc;
    @Getter
    private final VoiceApiConnection voiceApiConnection = new VoiceApiConnection();

    @Getter
    private OpenaudioSettingsResponse accountResponse = new OpenaudioSettingsResponse();
    @Getter
    private Set<CraftmendTag> tags = new HashSet<>();
    private boolean isVcLocked = false;

    // ugly state management, I should _really_ change this at some point, just like the state service
    @Getter
    private boolean isAttemptingVcConnect = false;
    @Getter
    private boolean lockVcAttempt = false;
    @Getter
    private boolean initialized = false;
    private boolean delayedInit = false;

    @Override
    public void onEnable() {
        // wait after buut if its a new account
        if (OpenAudioMc.getService(AuthenticationService.class).isNewAccount()) {
            delayedInit = true;
            OpenAudioLogger.toConsole("Delaying account init because we're a fresh installation");
            OpenAudioMc.resolveDependency(TaskService.class).schduleSyncDelayedTask(this::initialize, 20 * 3);
        } else {
            delayedInit = false;
            initialize();
        }
    }

    public void postBoot() {
        // only allow registration after initialization
        if (!initialized) return;
    }

    private void initialize() {
        OpenAudioLogger.toConsole("Initializing account details");
        syncAccount();
        initialized = true;

        if (delayedInit) {
            // we skipped the original boot, so run it now
            postBoot();
            delayedInit = false;
        }
    }

    public void syncAccount() {
        if (OpenAudioMc.getInstance().getInvoker().isNodeServer()) return;
        // stop the voice service
        if (this.voiceApiConnection != null) {
            this.voiceApiConnection.stop();
        }
        RestRequest<OpenaudioSettingsResponse> settingsRequest = new RestRequest<>(OpenaudioSettingsResponse.class, Endpoint.class.GET_ACCOUNT_SETTINGS);
        settingsRequest.run();

        for (CraftmendTag tag : tags) {
            AudioApi.getInstance().getEventDriver().fire(new AccountRemoveTagEvent(tag));
        }

        tags.clear();

        if (settingsRequest.getResponse().getSettings().isBanned()) addTag(CraftmendTag.BANNED);
        if (settingsRequest.getResponse().isClaimed()) addTag(CraftmendTag.CLAIMED);
        accountResponse = settingsRequest.getResponse();

        // is voice enabled with the new system?
        if (accountResponse.hasAddon(AddonCategory.VOICE)) {
            addTag(CraftmendTag.VOICECHAT);
            startVoiceHandshake(true);
        }
    }

    public void shutdown() {
        if (OpenAudioMc.getInstance().getInvoker().isNodeServer()) return;
        this.voiceApiConnection.stop();
    }

    public boolean is(CraftmendTag tag) {
        return tags.contains(tag);
    }

    public void addTag(CraftmendTag tag) {
        tags.add(tag);
        AudioApi.getInstance().getEventDriver().fire(new AccountAddTagEvent(tag));
    }

    private void removeTag(CraftmendTag tag) {
        tags.remove(tag);
        AudioApi.getInstance().getEventDriver().fire(new AccountRemoveTagEvent(tag));
    }

    public void startVoiceHandshake() {
        if (voiceApiConnection.getStatus() == VoiceApiStatus.IDLE && !isAttemptingVcConnect) {
            startVoiceHandshake(false);
        }
    }

    public void startVoiceHandshake(boolean ignoreLocal) {
        if (isVcLocked) return;
        if (voiceApiConnection.getStatus() != VoiceApiStatus.IDLE) {
            return;
        }
        if (!ignoreLocal && !is(CraftmendTag.VOICECHAT)) return;

        if (!ignoreLocal) {
            // check anyway
            if (OpenAudioMc.getService(NetworkingService.class).getClients().isEmpty()) return;
        }

        if (OpenAudioMc.resolveDependency(UserHooks.class).getOnlineUsers().isEmpty()) {
            OpenAudioLogger.toConsole("The server is empty! ignoring voice chat.");
            return;
        }

        if (OpenAudioMc.SERVER_ENVIRONMENT == ServerEnvironment.PRODUCTION) {
            OpenAudioLogger.toConsole("VoiceChat seems to be enabled for this account! Requesting RTC and Password...");
        }
        // do magic, somehow fail, or login to the voice server
        isAttemptingVcConnect = true;
        RestRequest request = new RestRequest(RestEndpoint.START_VOICE_SESSION);

        isVcLocked = true;
        request.executeAsync()
                .thenAccept(response -> {
                    if (response.getStatusCode() != 200) {
                        ErrorCode errorCode = response.getErrors().get(0).getCode();

                        if (errorCode == ErrorCode.NO_RTC) {
                            new RestRequest(RestEndpoint.END_VOICE_SESSION).executeInThread();
                            if (OpenAudioMc.SERVER_ENVIRONMENT == ServerEnvironment.PRODUCTION) {
                                OpenAudioLogger.toConsole("Failed to initialize voice chat. There aren't any servers that can handle your request. Trying again in 20 seconds.");
                                for (RestErrorResponse error : response.getErrors()) {
                                    OpenAudioLogger.toConsole(" - " + error.getMessage());
                                }
                            }
                            voiceApiConnection.setStatus(VoiceApiStatus.IDLE);
                            OpenAudioMc.resolveDependency(TaskService.class).schduleSyncDelayedTask(() -> {
                                startVoiceHandshake(true);
                            }, 20 * 20);
                            isVcLocked = false;
                            return;
                        }

                        if (errorCode == ErrorCode.NO_PERMISSIONS) {
                            OpenAudioLogger.toConsole("Your account doesn't actually have permissions for voicechat, shutting down.");
                            removeTag(CraftmendTag.VOICECHAT);
                            isAttemptingVcConnect = false;
                            lockVcAttempt = false;
                            isVcLocked = false;
                            return;
                        }

                        if (errorCode == ErrorCode.ALREADY_ACTIVE) {
                            new RestRequest(RestEndpoint.END_VOICE_SESSION).executeInThread();
                            OpenAudioLogger.toConsole("This server still has a session running with voice chat, terminating and trying again in 20 seconds.");
                            OpenAudioMc.resolveDependency(TaskService.class).schduleSyncDelayedTask(() -> {
                                startVoiceHandshake(true);
                            }, 20 * 20);
                            isVcLocked = false;
                            return;
                        }

                        if (response.getErrors().get(0).getMessage().toLowerCase().contains("path $")) {
                            new RestRequest(RestEndpoint.END_VOICE_SESSION).executeInThread();
                            OpenAudioLogger.toConsole("Failed to claim a voicechat session, terminating and trying again in 20 seconds.");
                            OpenAudioMc.resolveDependency(TaskService.class).schduleSyncDelayedTask(() -> {
                                startVoiceHandshake(true);
                            }, 20 * 20);
                            isVcLocked = false;
                            return;
                        }

                        OpenAudioLogger.toConsole("Failed to initialize voice chat. Error: " + response.getErrors().get(0).getMessage());
                        voiceApiConnection.setStatus(VoiceApiStatus.IDLE);
                        isAttemptingVcConnect = false;
                        lockVcAttempt = false;
                        isVcLocked = false;
                        return;
                    }

                    VoiceSessionRequestResponse voiceResponse = response.getResponse(VoiceSessionRequestResponse.class);
                    int highestPossibleLimit = accountResponse.getAddon(AddonCategory.VOICE).getLimit();
                    this.voiceApiConnection.start(voiceResponse.getServer(), voiceResponse.getPassword(), highestPossibleLimit);
                    isAttemptingVcConnect = false;
                    lockVcAttempt = false;
                    addTag(CraftmendTag.VOICECHAT);
                    isVcLocked = false;
                });
    }
}

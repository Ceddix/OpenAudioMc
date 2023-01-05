package com.craftmend.openaudiomc.generic.environment.models;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.generic.rest.response.AbstractRestResponse;
import lombok.Getter;

@Getter
public class ProjectStatus extends AbstractRestResponse {

    private VersionDetails versioning;
    private Announcement announcement;

    public boolean isLocalLatest() {
        return OpenAudioMc.BUILD.getBuildNumber() >= versioning.getBuildNumber();
    }

    public int getLatestBuildNumber() {
        return versioning.getBuildNumber();
    }

    public boolean isAnnouncementAvailable() {
        return announcement.getHasAnnouncement();
    }

}

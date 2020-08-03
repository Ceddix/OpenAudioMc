package com.craftmend.openaudiomc.generic.networking.packets.client.media;

import com.craftmend.openaudiomc.generic.networking.abstracts.AbstractPacket;
import com.craftmend.openaudiomc.generic.networking.enums.PacketChannel;
import com.craftmend.openaudiomc.generic.networking.payloads.client.media.ClientDestroyMediaPayload;

public class PacketClientDestroyMedia extends AbstractPacket {

    public PacketClientDestroyMedia(String soundId, boolean deleteSpecial, int fadeTime) {
        super(new ClientDestroyMediaPayload(soundId, deleteSpecial, fadeTime), PacketChannel.CLIENT_OUT_DESTROY_MEDIA, null);
    }

    public PacketClientDestroyMedia(String soundId, int fadeTime) {
        super(new ClientDestroyMediaPayload(soundId, false, fadeTime), PacketChannel.CLIENT_OUT_DESTROY_MEDIA, null);
    }

    public PacketClientDestroyMedia(String soundId) {
        super(new ClientDestroyMediaPayload(soundId, false, 250), PacketChannel.CLIENT_OUT_DESTROY_MEDIA, null);
    }

}

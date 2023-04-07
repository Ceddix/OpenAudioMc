package com.craftmend.openaudiomc.api.impl.event.events;

import com.craftmend.openaudiomc.api.impl.event.AudioEvent;
import com.craftmend.openaudiomc.api.impl.event.enums.EventSupport;
import com.craftmend.openaudiomc.api.interfaces.EventSupportFlag;
import com.craftmend.openaudiomc.generic.oac.enums.CraftmendTag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * This event gets called whenever a new {@link CraftmendTag} gets activated for this
 * OpenAudioMc installation. CraftmendTags are flags which tell the plugin
 * if it can disable specific features (like voicechat)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EventSupportFlag(support = EventSupport.EVERYWHERE)
public class AccountRemoveTagEvent extends AudioEvent {

    private CraftmendTag removedTag;

}

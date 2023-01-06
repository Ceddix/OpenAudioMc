import {oalog} from "../../helpers/log";

export class StreamerLink {

    constructor(openAudioMc) {
        this.openAudioMc = openAudioMc;
        this.isEnabled = false;
        this.isOpen = false;

        setInterval(() => {
            this._pushWs("ping", "ping")
        }, 1000)
    }

    enable() {
        if (this.isEnabled) return;
        this.isEnabled = true;

        // start websocket
        this.ws = new WebSocket("ws://localhost:4783/ws")
        this.ws.onclose = () => {
            this.isOpen = false;
            if (!this.isEnabled) return;
            this.isEnabled = false;
            oalog("Streamer socket died. Reconnectin in 500 ms")
            setTimeout(() => {
                this.enable();
            }, 500)
        }

        this.ws.onopen = () => {
            this.isOpen = true;
            oalog("Connected to streamer socket!")

            if (this.openAudioMc.voiceModule != null) {
                this.openAudioMc.voiceModule.peerMap.forEach((peer, key) => {
                    this.announceVoicePeerJoin(peer.asSimpleJson());
                })
            }

            this._pushWs("connect", "connect")
        }
    }

    announceVoicePeerJoin(peer) {
        this._pushWs("peer_join", peer.asSimpleJson())
    }

    announceVoicePeerTalkingStart(peer) {
        this._pushWs("peer_start_talking", peer.asSimpleJson())
    }

    announceVoicePeerTalkingStop(peer) {
        this._pushWs("peer_stop_talking", peer.asSimpleJson())
    }

    announceVoicePeerLeave(peer) {
        this._pushWs("peer_leave", peer.asSimpleJson())
    }

    _pushWs(type, data) {
        if (this.isOpen) {
            oalog("Writing streamer event:" + type)
            this.ws.send(JSON.stringify({
                "openaudiomc": {
                    "type": type,
                    "data": data
                }
            }))
        }
    }

}
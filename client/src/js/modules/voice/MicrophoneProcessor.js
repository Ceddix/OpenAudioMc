import {Hark} from "../../helpers/libs/hark.bundle";
import {VoiceStatusChangeEvent} from "./VoiceModule";
import {oalog} from "../../helpers/log";

/*
 * This class measures the average volume from an incoming media stream
 * It does this by trying to detect when someone is talking, and keeping track
 * of the average volume during silence (mic self noise, etc) and the average volume of speech
 *
 * it then uses those values to detect differences in speech (whispering, normal and shouting)
 *
 * the delta value is completely random and will need some fine tuning, but this design means that you
 * can only stay in a SHOUTING or WHISPERING state for a few seconds before it'll reset become the new
 * default/average volume. This can be tuned by changing the number of required samples.
 */
export class MicrophoneProcessor {

    constructor(openAudioMc, voiceModule, stream) {
        this.openAudioMc = openAudioMc

        this.harkEvents = Hark(stream, {})
        this.loudnessHistory = []
        this.isSpeaking = false;
        this.filledHistory = false;
        this.state = VoiceStatusChangeEvent.LEVEL_NORMAL

        // temp config, loudness delta to trigger shouting and whispering when
        // finding a difference from the average
        this.delta = 30

        this.harkEvents.on('speaking', () => {
            this.isSpeaking = true;
        });

        this.harkEvents.on('volume_change', measurement => {
            // only process data when the user is actively speaking
            if (this.isSpeaking) {
                // normalize DB
                let level = measurement + 100

                // map the last 200 voice events while the user is speaking
                if (this.loudnessHistory.length > 200) {
                    // drop first entry
                    this.loudnessHistory.shift()

                    if (!this.filledHistory) {
                        this.filledHistory = true;
                        this.onStart();
                    }
                    this.onUpdate(measurement)
                }
                this.loudnessHistory.push(level)
            }
        })

        this.harkEvents.on('stopped_speaking', () => {
            this.isSpeaking = false;
        });
    }

    // only enable this feature when there is enough data to calculate an average
    isReady() {
        return this.loudnessHistory.length > 180
    }

    // calculate and return the average loudness when speaking
    findAverageLoudness() {
        return arr => this.loudnessHistory.reduce((a,b) => a + b, 0) / this.loudnessHistory.length
    }

    // fired when we have enough data to start our funky buisness
    onStart() {

    }

    onUpdate(lastMeasurement) {
        // do we have enough data for a reliable result?
        if (!this.isReady()) return // no, lets try again in a bit

        // find a difference
        let diff = Math.abs(this.findAverageLoudness() - lastMeasurement)

        // enough to trigger
        if (diff > this.delta) {
            if (lastMeasurement > this.findAverageLoudness()) {
                // shouting
                this.onChange(VoiceStatusChangeEvent.LEVEL_SHOUTING)
            } else {
                // whispering
                this.onChange(VoiceStatusChangeEvent.LEVEL_WHISPERING)
            }
        } else {
            // nothing crazy
            this.onChange(VoiceStatusChangeEvent.LEVEL_NORMAL)
        }
    }

    onChange(updatedState) {
        if (updatedState != this.state) {
            this.state = updatedState
            oalog("Changing special voice flair to " + this.state)
            this.openAudioMc.voiceModule.pushSocketEvent(this.state);
        }
    }

    stop() {
        this.harkEvents.stop()
    }

}
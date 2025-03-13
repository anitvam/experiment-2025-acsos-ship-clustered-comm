package it.unibo.util.ais

import dk.dma.ais.message.AisMessage
import dk.dma.ais.sentence.SentenceException
import dk.dma.ais.sentence.Vdm

/**
 * This additional builder implementation to dk.dma.ais.* library is required due to the particular type of messages
 * managed.
 * The message files contains an additional, non-standard AIS NMEA 0183, !DATE-TIME to
 * register the timestamp of the message.
 * For this reason the library builder fails.
 * The !DATE-TIME seems to refer to the previous sent message, and is saved for doing so.
 * This builder is more flexible than the library one because it allows to parse messages with any subdivisions,
 * without considering the whole stream of messages incoming.
 */
class AisCustomMessageParser() {
    var vdm = Vdm()
    fun reset() {
        vdm = Vdm()
    }
    fun parseLine(message: String) {
        try {
            vdm.parse(message)
        } catch (e: SentenceException) {
            if (e.message != null && e.message!!.contains("Out of sequence sentence:")) {
                // Failure due to previously interrupted message
                // The sequence is dropped. Not the optimal solution but a easy workaround.
                //println("Out of sequence sentence for $message: IGNORED")
                reset()
                //vdm.parse(message)
            } else if (e.message != null && e.message!!.contains("Invalid checksum")){
                println("Invalid checksum for $message: IGNORED")
                reset()
            } else {
                println("Failure due to parsing message $message")
                throw (e)
            }
        }
    }
    fun isComplete() = vdm.isCompletePacket
    fun build(): AisMessage? {
        try {
            return AisMessage.getInstance(vdm)
        } catch (e: Exception) {
            if (e.message == "Unknown AIS message id 0"){
                // Empty payload: Ignoring message.
                // println("Non valid NMEA AIS message on line ${vdm.encoded}: IGNORING THE MESSAGE")
                return null
            } else {
                println("Message not valid ${vdm.rawSentencesJoined}: IGNORED")
                //throw(e)
                return null
            }

        } finally {
            reset()
        }
    }
}

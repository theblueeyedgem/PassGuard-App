package com.example.security

import org.apache.commons.codec.binary.Base32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.nio.ByteBuffer
import kotlin.math.pow

object TOTPGenerator {

    fun generateTOTP(secretBase32: String, timeMillis: Long = System.currentTimeMillis()): String {
        if (secretBase32.isBlank()) return ""
        try {
            val base32 = Base32()
            val decodedKey = base32.decode(secretBase32.replace(" ", "").uppercase())
            
            // X interval is 30 seconds
            val timeStep = timeMillis / 30000L
            
            val msg = ByteBuffer.allocate(8).putLong(timeStep).array()
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(decodedKey, "RAW"))
            val hash = mac.doFinal(msg)
            
            val offset = hash[hash.size - 1].toInt() and 0xf
            var binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                    ((hash[offset + 1].toInt() and 0xff) shl 16) or
                    ((hash[offset + 2].toInt() and 0xff) shl 8) or
                    (hash[offset + 3].toInt() and 0xff)
            
            val otp = binary % 10.0.pow(6.0).toInt()
            return String.format("%06d", otp)
        } catch (e: Exception) {
            return "ERROR"
        }
    }
    
    fun getProgressPercentage(timeMillis: Long = System.currentTimeMillis()): Float {
        val currentSecond = (timeMillis / 1000) % 30
        return (30 - currentSecond).toFloat() / 30f
    }
    
    fun getSecondsRemaining(timeMillis: Long = System.currentTimeMillis()): Int {
        return (30 - ((timeMillis / 1000) % 30)).toInt()
    }
}

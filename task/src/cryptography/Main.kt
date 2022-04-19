package cryptography

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.random.Random

const val FORMAT_PNG = "png"

const val FINAL_BYTE_1: Byte = 0
const val FINAL_BYTE_2: Byte = 0
const val FINAL_BYTE_3: Byte = 3

const val MASK = 0xFE

enum class Task {
    HIDE,
    SHOW,
    EXIT,
    ;

    companion object {
        fun valueOfOrNull(string: String): Task? =
            try {
                Task.valueOf(string.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
    }
}

fun main() {
    while (true) {
        println("Task (${Task.values().map { it.name.lowercase() }.joinToString(", ")}):")
        val input = readLine()!!
        when (Task.valueOfOrNull(input)) {
            Task.EXIT -> {
                println("Bye!")
                return
            }
            Task.HIDE -> {
                println("Input image file:")
                val inputFileName = readLine()!!
                println("Output image file:")
                val outputFileName = readLine()!!

                println("Message to hide:")
                val message = readLine()!!

                println("Password:")
                val password = readLine()!!

                val bufferedImage: BufferedImage
                try {
                    bufferedImage = ImageIO.read(File(inputFileName))
                } catch (e: IOException) {
                    println("Can't read input file!")
                    continue
                }

                setMessage(bufferedImage, encrypt(message, password))

                try {
                    ImageIO.write(bufferedImage, FORMAT_PNG, File(outputFileName))
                    println("Message saved in $outputFileName image.")
                } catch (e: IOException) {
                    continue
                }
            }
            Task.SHOW -> {
                println("Input image file:")
                val inputFileName = readLine()!!

                println("Password:")
                val password = readLine()!!

                val bufferedImage: BufferedImage
                try {
                    bufferedImage = ImageIO.read(File(inputFileName))
                } catch (e: IOException) {
                    println("Can't read input file!")
                    continue
                }

                val encryptedMessage = getMessage(bufferedImage)
                val message = decrypt(encryptedMessage, password).toString(Charsets.UTF_8)
                println("Message:")
                println(message)
            }
            null -> {
                println("Wrong task: $input")
            }
        }
    }
}

fun encrypt(message: String, password: String): ByteArray {
    val messageByteArray = message.toByteArray(Charsets.UTF_8)
    val passwordByteArray = password.toByteArray(Charsets.UTF_8)
    val passwordLength = passwordByteArray.size

    return ByteArray(messageByteArray.size) {
        (messageByteArray[it].toInt() xor passwordByteArray[it % passwordLength].toInt()).toByte()
    }
}

fun decrypt(encryptedMessage: ByteArray, password: String): ByteArray {
    val passwordByteArray = password.toByteArray(Charsets.UTF_8)
    val passwordLength = passwordByteArray.size

    return ByteArray(encryptedMessage.size) {
        (encryptedMessage[it].toInt() xor passwordByteArray[it % passwordLength].toInt()).toByte()
    }
}

fun setMessage(bufferedImage: BufferedImage, message: ByteArray) {
    val byteArray = message + ByteArray(3) {
        when (it) {
            0 -> FINAL_BYTE_1
            1 -> FINAL_BYTE_2
            2 -> FINAL_BYTE_3
            else -> 0
        }
    }
    val bitArray = byteArray.toList().map {
        getBits(it.toInt())
    }.flatten()

    val bitCount = bitArray.size
    if (bitCount > bufferedImage.width * bufferedImage.height) {
        println("The input image is not large enough to hold this message.")
        return
    }

    var i = 0
    for (y in 0 until bufferedImage.height) {
        for (x in 0 until bufferedImage.width) {
            val inputColor = Color(bufferedImage.getRGB(x, y))
            val outputColor = if (i < bitCount) {
                Color(
                    inputColor.red,
                    inputColor.green,
                    (inputColor.blue and MASK) or bitArray[i]
                )
            } else
                inputColor
            bufferedImage.setRGB(x, y, outputColor.rgb)
            i++
        }
    }
}

fun getMessage(bufferedImage: BufferedImage): ByteArray {
    val bytes = mutableListOf<Int>()
    val last3Bytes = mutableListOf<Int>()
    var byte = 0
    var i = 0
    loop@for (y in 0 until bufferedImage.height) {
        for (x in 0 until bufferedImage.width) {
            val inputColor = Color(bufferedImage.getRGB(x, y))
            val blue = inputColor.blue
            val bit = blue and 1

            val bitIndex = i % 8
            byte += when (bitIndex) {
                7 -> bit * 1
                6 -> bit * 2
                5 -> bit * 4
                4 -> bit * 8
                3 -> bit * 16
                2 -> bit * 32
                1 -> bit * 64
                0 -> bit * 128
                else -> 0
            }
            i++
            if (bitIndex == 7) {
                last3Bytes.add(byte)

                if (last3Bytes.size >= 3) {
                    if ((last3Bytes[0] == 0)
                        && (last3Bytes[1] == 0)
                        && (last3Bytes[2] == 3)) {
                        break@loop
                    } else {
                        bytes.add(last3Bytes.removeAt(0))
                    }
                }
                byte = 0
            }
        }
    }

    return ByteArray(bytes.size) {
        bytes[it].toByte()
    }
}

fun getBits(byte: Int) = listOf(getBit(byte, 7),
    getBit(byte, 6),
    getBit(byte, 5),
    getBit(byte, 4),
    getBit(byte, 3),
    getBit(byte, 2),
    getBit(byte, 1),
    getBit(byte, 0))

fun getBit(byte: Int, bitIndex: Int) = when (bitIndex) {
    0 -> byte and 1
    1 -> byte and 2 shr 1
    2 -> byte and 4 shr 2
    3 -> byte and 8 shr 3
    4 -> byte and 16 shr 4
    5 -> byte and 32 shr 5
    6 -> byte and 64 shr 6
    7 -> byte and 128 shr 7
    else -> 0
}

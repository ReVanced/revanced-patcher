package app.revanced.java.io

import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset

internal expect fun File.kmpResolve(child: String): File

internal expect fun File.kmpDeleteRecursively(): Boolean

internal expect fun File.kmpInputStream(): FileInputStream

internal expect fun File.kmpBufferedWriter(charset: Charset): BufferedWriter

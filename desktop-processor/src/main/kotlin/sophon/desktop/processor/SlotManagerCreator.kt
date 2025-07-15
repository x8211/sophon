package sophon.desktop.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec

private const val PACKAGE_NAME = "com.processor"
private const val FILE_NAME = "SlotManger"

/**
 * 生成文件
 * ```
 * class SlotManger {
 *     private val list:  List<Pair<String, String>> = listOf(Pair("AA", "BB"),Pair("CC", "DD"))
 * }
 * ```
 */
fun createFile(meta: MetaInfo, codeGenerator: CodeGenerator) {
    val file = FileSpec.builder(PACKAGE_NAME, FILE_NAME)
        .addType(
            TypeSpec.objectBuilder(FILE_NAME)
                .addProperty(meta.toProperty())
                .build()
        )
        .build()
    codeGenerator.createNewFile(
        Dependencies.ALL_FILES,
        PACKAGE_NAME,
        FILE_NAME, "kt"
    ).bufferedWriter().use { file.writeTo(it) }
}

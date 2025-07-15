package sophon.desktop.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import sophon.desktop.processor.annotation.Slot
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.asTypeName

class SlotProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private var invoked = false
    private val codeGenerator = environment.codeGenerator

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()
        invoked = true
        val meta = MetaInfo()
        //解析注解
        resolver.getSymbolsWithAnnotation(Slot::class.java.name)
            .filterIsInstance<KSClassDeclaration>()
            .onEach { cls -> parseEntranceAnnotation(cls)?.also { meta.list.add(it) } }
            .count()
        //生成文件
        createFile(meta, codeGenerator)
        return emptyList()
    }

    /**
     * 从[cls]中解析[Slot]注解信息
     */
    private fun parseEntranceAnnotation(cls: KSClassDeclaration): Pair<String, String>? {
        val target = cls.annotations
            .find { it.shortName.getShortName() == Slot::class.java.simpleName }
            ?: return null
        val titleValue = target.getValue(Slot::title.name, "")
        return Pair(titleValue, cls.qualifiedName?.asString() ?: "")
    }

    private inline fun <reified T> KSAnnotation.getValue(key: String, defaultValue: T): T {
        return arguments.find { it.name?.asString() == key }?.value as? T ?: defaultValue
    }

}

/**
 * 元信息，数据格式：Map<所属Group，List<Pair<标题，类名>>>
 */
data class MetaInfo(val list: MutableList<Pair<String, String>> = mutableListOf()) {
    /**
     * 初始化语句：
     * ```
     *  listOf(Pair("2","3"),Pair("4","5"))
     * ```
     */
    private fun toInitializerCmd(): String {
        val listCmd = StringBuilder("listOf(")
        list.forEach { pair -> listCmd.append("Pair(\"${pair.first}\",\"${pair.second}\"),") }
        listCmd.append(")")
        return listCmd.toString()
    }

    fun toProperty(): PropertySpec {
        val pairType = Pair::class.asTypeName().parameterizedBy(STRING, STRING)
        val listType = LIST.parameterizedBy(pairType)
        return PropertySpec.builder("list", listType, KModifier.PUBLIC)
            .initializer(toInitializerCmd())
            .build()
    }
}

class SlotProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = SlotProcessor(environment)
}
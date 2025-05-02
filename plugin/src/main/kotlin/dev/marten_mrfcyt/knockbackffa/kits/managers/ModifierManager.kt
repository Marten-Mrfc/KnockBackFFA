package dev.marten_mrfcyt.knockbackffa.kits.managers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.guis.editor.kit.ItemModifierGUI
import dev.marten_mrfcyt.knockbackffa.kits.models.Kit
import dev.marten_mrfcyt.knockbackffa.kits.models.KitModifier
import dev.marten_mrfcyt.knockbackffa.kits.models.ModifyObject
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import mlib.api.forms.Form
import mlib.api.forms.FormType
import mlib.api.utilities.*
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.jar.JarFile
import java.util.logging.Level

class ModifierManager(private val plugin: KnockBackFFA) {
    private val logger = plugin.logger
    private val registry = ModifierRegistry()

    init {
        registerAllModifiers()
    }
    
    fun reloadModifiers() {
        plugin.logger.info(TranslationManager.translate("modifiers.reload.start"))

        val previousCount = registry.getAllModifiers().size

        try {
            var unregistered = 0
            registry.getAllModifiers().forEach { modifier ->
                if (modifier is Listener) {
                    HandlerList.getRegisteredListeners(plugin).forEach { listener ->
                        if (listener.listener == modifier) {
                            HandlerList.unregisterAll(modifier)
                            unregistered++
                        }
                    }
                }
            }
            plugin.logger.info(TranslationManager.translate("modifiers.reload.unregistered", "count" to unregistered))

            registry.clearModifiers()

            registerAllModifiers()

            registerEvents(KnockBackFFA.instance)

            val newCount = registry.getAllModifiers().size
            val change = newCount - previousCount
            val changeText = if (change > 0) "+$change" else "$change"
            plugin.logger.info(TranslationManager.translate("modifiers.reload.complete", "count" to newCount, "change" to changeText))
        } catch (e: Exception) {
            plugin.logger.severe(TranslationManager.translate("modifiers.reload.error", "error" to e.message.toString()))
            e.printStackTrace()

            if (registry.getAllModifiers().isEmpty()) {
                plugin.logger.info(TranslationManager.translate("modifiers.reload.recovery"))
                registerAllModifiers()
                registerEvents(plugin)
            }
        }
    }

    private fun registerAllModifiers() {
        logger.info(TranslationManager.translate("modifiers.register.start"))

        val modifiersPackage = "dev.marten_mrfcyt.knockbackffa.kits.modifiers"
        val classLoader = plugin.javaClass.classLoader
        val registeredModifiers = mutableListOf<String>()

        try {
            val path = modifiersPackage.replace('.', '/')
            val resources = classLoader.getResources(path)

            var count = 0
            
            while (resources.hasMoreElements()) {
                val resource = resources.nextElement()
                val urls = resource.toString()

                if (urls.startsWith("jar:")) {
                    try {
                        val urlStr = resource.toString()
                        val jarPath = urlStr.substringAfter("jar:file:").substringBefore("!")
                        // Safely decode URL-encoded paths
                        val decodedJarPath = try {
                            URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name())
                        } catch (e: Exception) {
                            jarPath // Return original if decoding fails
                        }
                        
                        val jarFile = JarFile(File(decodedJarPath))
                        
                        val entries = jarFile.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            val entryName = entry.name

                            if (entryName.startsWith(path) && entryName.endsWith(".class") && !entryName.contains('$')) {
                                val className = entryName.replace('/', '.').removeSuffix(".class")
                                val modifierName = processModifierClass(className)
                                if (modifierName != null) {
                                    registeredModifiers.add(modifierName)
                                    count++
                                }
                            }
                        }
                        jarFile.close()
                    } catch (e: Exception) {
                        plugin.logger.severe("Error processing JAR for modifiers: ${e.message}")
                        // Continue execution without failing
                    }
                } else {
                    val directory = File(resource.toURI())
                    if (directory.exists()) {
                        directory.listFiles()?.forEach { file ->
                            if (file.isFile && file.name.endsWith(".class") && !file.name.contains('$')) {
                                val className = "$modifiersPackage.${file.name.removeSuffix(".class")}"
                                val modifierName = processModifierClass(className)
                                if (modifierName != null) {
                                    registeredModifiers.add(modifierName)
                                    count++
                                }
                            }
                        }
                    }
                }
            }

            val modifierNames = registeredModifiers.joinToString(", ")
            logger.info(TranslationManager.translate("modifiers.register.success", "count" to registeredModifiers.size, "names" to modifierNames))
        } catch (e: Exception) {
            logger.severe(TranslationManager.translate("modifiers.register.error", "error" to e.message.toString()))
            e.printStackTrace()
        }
    }
    
    private fun processModifierClass(className: String): String? {
        try {
            debug("Processing class: $className")
            val clazz = Class.forName(className)

            if (!ModifyObject::class.java.isAssignableFrom(clazz) ||
                java.lang.reflect.Modifier.isAbstract(clazz.modifiers)) {
                debug("Class $className is not a valid modifier class (not a ModifyObject or is abstract)")
                return null
            }

            val annotation = clazz.getAnnotation(KitModifier::class.java)
            if (annotation != null) {
                debug("Found KitModifier annotation with ID: ${annotation.id} on class $className")
                try {
                    val field = clazz.getDeclaredField("INSTANCE")
                    field.isAccessible = true
                    val modifier = field.get(null) as ModifyObject
                    
                    debug("Successfully obtained modifier instance: ${modifier.id}")
                    registry.register(modifier)
                    return modifier.id
                } catch (e: Exception) {
                    logger.severe("Failed to access INSTANCE field on $className: ${e.message}")
                    e.printStackTrace()
                    return null
                }
            } else {
                logger.info("Class $className does not have the KitModifier annotation")
            }
        } catch (e: Exception) {
            logger.severe("Exception processing class $className: ${e.message}")
            e.printStackTrace()
        }
        return null
    }

    fun getModifyObjects() = registry.getAllModifiers()

    fun handleModifier(player: Player, kitName: String, slot: Int, modifierId: String) {
        val kit = KnockBackFFA.kitManager.getKit(kitName)
        val modifyObject = registry.getModifier(modifierId) ?: return

        val item = kit.getItem(slot) ?: return
        val currentValue = item.modifiers[modifierId] as? Boolean == true

        if (modifyObject.args.isNotEmpty() && !currentValue) {
            handleModifierArgs(player, kit, slot, modifyObject)
            return
        }

        if (modifyObject.args.isNotEmpty() && currentValue) {
            kit.setModifier(slot, modifierId, false)
        } else {
            kit.setModifier(slot, modifierId, !currentValue)
        }

        ItemModifierGUI(KnockBackFFA.instance, player, kitName, slot)
    }

    private fun handleModifierArgs(player: Player, kit: Kit, slot: Int, modifyObject: ModifyObject) {
        var currentArgIndex = 0
        val args = mutableMapOf<String, Any>()

        fun processNextArg() {
            if (currentArgIndex >= modifyObject.args.size) {
                kit.setModifier(slot, modifyObject.id, true, args)

                ItemModifierGUI(plugin, player, kit.name, slot)
                return
            }

            val (argName, argType) = modifyObject.args[currentArgIndex]
            val formType = when (argType) {
                Int::class.java -> FormType.INTEGER
                Boolean::class.java -> FormType.BOOLEAN
                else -> FormType.STRING
            }

            val form = Form(TranslationManager.translate("modifiers.args.prompt", "name" to argName), formType, 30) { _, response ->
                args[argName] = response
                currentArgIndex++
                processNextArg()
            }
            player.closeInventory()
            form.show(player)
        }

        processNextArg()
    }

    fun handleEvent(player: Player, item: ItemStack?, args: Map<String, Any>, id: String) {
        item?.itemMeta?.let { meta ->
            if (checkCustomValue(meta, plugin, "modify", id)) {
                val originalSlot = getCustomValue(meta, plugin, "slot") as? Int
                val kitName = getCustomValue(meta, plugin, "kit_name") as? String
                val updatedArgs = args.toMutableMap()
                if (originalSlot != null && kitName != null && !args.containsKey("slot")) {
                    updatedArgs["slot"] = originalSlot
                    updatedArgs["kit_name"] = kitName
                }

                registry.getModifier(id)?.handle(player, item, updatedArgs)
            }
        }
    }

    fun registerEvents(plugin: Plugin) {
        registry.getAllModifiers().forEach { entry ->
            if (entry is Listener) {
                plugin.server.pluginManager.registerEvents(entry, plugin)
            }
        }
        val listenerCount = registry.getAllModifiers().count { it is Listener }
        val staticCount = registry.getAllModifiers().count { it !is Listener }
        plugin.logger.info(TranslationManager.translate("modifiers.events.registered", "listeners" to listenerCount, "static" to staticCount))
    }
}
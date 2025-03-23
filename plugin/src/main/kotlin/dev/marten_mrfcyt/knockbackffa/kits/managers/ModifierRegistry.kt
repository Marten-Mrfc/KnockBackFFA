package dev.marten_mrfcyt.knockbackffa.kits.managers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.models.ModifyObject
import kotlin.text.clear

class ModifierRegistry(private val plugin: KnockBackFFA) {
    private val modifiers = mutableMapOf<String, ModifyObject>()

    fun register(modifier: ModifyObject) {
        modifiers[modifier.id] = modifier
    }

    fun getModifier(id: String): ModifyObject? = modifiers[id]

    fun getAllModifiers(): Collection<ModifyObject> = modifiers.values

    fun clearModifiers() {
        modifiers.clear()
    }
}
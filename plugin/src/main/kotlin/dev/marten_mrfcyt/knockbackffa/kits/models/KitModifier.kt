package dev.marten_mrfcyt.knockbackffa.kits.models

/**
 * Annotation to define a modifier for an item.
 *
 * @property id The unique identifier for the modifier.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class KitModifier(
    val id: String,
)
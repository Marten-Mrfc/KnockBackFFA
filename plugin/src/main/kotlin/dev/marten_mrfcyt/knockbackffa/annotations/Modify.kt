package dev.marten_mrfcyt.knockbackffa.annotations

/**
 * Annotation to define a modifier for an item.
 *
 * @property id The unique identifier for the modifier.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Modify(
    val id: String,
)
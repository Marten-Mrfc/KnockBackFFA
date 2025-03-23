package dev.marten_mrfcyt.knockbackffa.kits.models

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget

/**
 * Annotation to define a modifier for an item.
 *
 * @property id The unique identifier for the modifier.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KitModifier(val id: String)
package dev.marten_mrfcyt.knockbackffa.boosts.models

/**
 * Annotation to mark a class as a boost type.
 * Used for automatic registration through reflection.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class BoostType(val id: String)
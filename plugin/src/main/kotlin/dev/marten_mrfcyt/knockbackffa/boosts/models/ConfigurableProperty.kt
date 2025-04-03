package dev.marten_mrfcyt.knockbackffa.boosts.models

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigurableProperty(
    val configKey: String,
    val defaultValue: String = ""
)
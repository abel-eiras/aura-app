# kotlinx.serialization: keep generated serializers of the domain module.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class io.github.abeleiras.aura.domain.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.abeleiras.aura.domain.** {
    kotlinx.serialization.KSerializer serializer(...);
}

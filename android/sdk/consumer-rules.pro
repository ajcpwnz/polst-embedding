# PolstSDK consumer R8/ProGuard rules.
# These rules are applied to any app that consumes the :sdk artifact.

# --- Public API surface -------------------------------------------------------
# Keep the entire public API of the SDK so host apps can link against it after
# R8 shrinking.
-keep public class com.polst.sdk.** { public *; }

# --- Kotlinx.serialization ----------------------------------------------------
# Preserve annotated fields and constructors used by the serialization runtime.
-keepclassmembers,allowobfuscation class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.Serializable <init>(...);
}

# Keep annotations, generic signatures, inner classes, and enclosing method
# metadata required by the kotlinx.serialization runtime reflection.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature,*Annotation*,InnerClasses,EnclosingMethod

# Keep Companion objects of @Serializable classes (holds the generated serializer).
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Standard kotlinx.serialization keep rules — generated $serializer companions
# and their serializer() accessors.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keep,includedescriptorclasses class <1>$$serializer { *; }

-keepclasseswithmembers class **$$serializer {
    public static ** INSTANCE;
}

# --- Network DTOs -------------------------------------------------------------
# DTOs are reflectively serialized; keep all members to be safe.
-keep class com.polst.sdk.network.dto.** { *; }

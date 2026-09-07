# Liquid Prism Clock — R8/ProGuard rules.
# The app uses no reflection-based serialization and Compose ships its own
# consumer rules, so a minimal set is sufficient.

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Keep DataStore generated preferences flow accessors (they are ordinary
# Kotlin functions, but keep the model/data classes for clarity).
-keep class com.example.liquidprismclock.model.** { *; }
-keep class com.example.liquidprismclock.data.** { *; }

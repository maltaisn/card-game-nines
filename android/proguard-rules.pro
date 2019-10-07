-verbose

-dontwarn android.support.**
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.jnigen.BuildTarget*

# Keep android input classes created by reflection
-keep class com.badlogic.gdx.backends.android.AndroidInput* { *; }
-keep class com.badlogic.gdx.backends.android.AndroidInputThreePlus* { *; }

# Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

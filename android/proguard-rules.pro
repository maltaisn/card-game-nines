-verbose

-dontwarn android.support.**
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.jnigen.BuildTarget*

# Keep android input classes created by reflection
-keep class com.badlogic.gdx.backends.android.AndroidInput* { *; }
-keep class com.badlogic.gdx.backends.android.AndroidInputThreePlus* { *; }

# Keep classes used in skin files
-keep class com.maltaisn.**.*Style { *; }
-keep class com.badlogic.gdx.**.*Style { *; }
-keep class com.badlogic.gdx.graphics.Color { *; }
-keep class com.badlogic.gdx.math.Vector2 { *; }

# Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

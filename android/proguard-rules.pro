-verbose

-dontwarn android.support.**
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.jnigen.BuildTarget*
-dontwarn com.badlogic.gdx.graphics.g2d.freetype.FreetypeBuild

# Keep android input classes created by reflection
-keep class com.badlogic.gdx.backends.android.AndroidInput* { *; }
-keep class com.badlogic.gdx.backends.android.AndroidInputThreePlus* { *; }

# Keep classes used in skin files
-keep class com.maltaisn.**.*Style { *; }
-keep class com.badlogic.gdx.**.*Style { *; }
-keep class com.badlogic.gdx.graphics.Color { *; }
-keep class com.badlogic.gdx.math.Vector2 { *; }

# Keep classes used in preferences and stats files
-keep class * extends com.maltaisn.cardgame.prefs.PrefEntry { *; }
-keep class * extends com.maltaisn.cardgame.stats.Statistic { *; }

# Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

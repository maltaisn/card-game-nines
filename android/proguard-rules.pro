-verbose

-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.jnigen.BuildTarget*

# Keep android input classes created by reflection
-keep class com.badlogic.gdx.backends.android.AndroidInput* { *; }
-keep class com.badlogic.gdx.backends.android.AndroidInputThreePlus* { *; }

# Note: used to work fine without these, not sure why they are now needed (since updating to 1.12.1 at least)
-keep public class com.badlogic.gdx.scenes.scene2d.** { *; }
-keep public class com.badlogic.gdx.graphics.g2d.GlyphLayout$GlyphRun { *; }
-keep public class com.badlogic.gdx.graphics.Color { *; }
-keep public class com.badlogic.gdx.math.Rectangle { *; }

# For deobfuscating crash reports
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

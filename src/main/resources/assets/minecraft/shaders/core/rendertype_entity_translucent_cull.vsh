#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform float GameTime;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec4 lightMapColor;
flat out float finish;
out vec4 normal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance(Position, FogShape);
    lightMapColor  = texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor   = texelFetch(Sampler1, UV1, 0);
    texCoord0      = UV0;
    normal         = ProjMat * ModelViewMat * vec4(Normal, 0.0);

    // ---------- Finish detection ----------
    // When the item/block colour provider returns the magic ARGB (R=1, B=1, G<1)
    // Minecraft passes that as the vertex Color here.
    // finish = floor(Color.y * 255 / 2) + 1  (IDs 1-14)
    if (Color.xz == vec2(1.0) && Color.y < 1.0) {
        float y = floor(Color.y * 255.0);
        vertexColor = (mod(y, 2.0) == 1.0)
            ? vec4(1.0)
            : minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, vec4(1.0));
        lightMapColor = (mod(y, 2.0) == 1.0) ? vec4(1.0) : lightMapColor;
        finish = floor(y / 2.0) + 1.0;
    } else {
        vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
        finish = 0.0;
    }
}

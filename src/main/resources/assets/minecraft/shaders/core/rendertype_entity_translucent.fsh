#version 150

#moj_import <fog.glsl>

// Sampler0 = entity / furniture model texture (auto-bound by Minecraft)
// Sampler1 = overlay texture (auto-bound)
// Sampler2 = lightmap texture (auto-bound)
// Sampler3 = finish atlas texture (bound manually via RenderSystem.setShaderTexture(3, ...))
uniform sampler2D Sampler0;
uniform sampler2D Sampler3;
#define FinishSampler Sampler3

uniform mat4 ModelViewMat;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float GameTime;

in float vertexDistance;
in vec4 vertexColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec4 lightMapColor;
flat in float finish;
in vec4 normal;

out vec4 fragColor;

#moj_import <utils.glsl>
#moj_import <finish_atlas.glsl>
#moj_import <finish_phantom.glsl>
#moj_import <finishes.glsl>

void main() {
    vec4 color = texture(Sampler0, texCoord0);

    if (color.a < 0.1) {
        discard;
    }

    if (finish > 0.0) {
        color = finishGet(color, texCoord0, finish);
    }

    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;

    if (color.a < 0.01) {
        discard;
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

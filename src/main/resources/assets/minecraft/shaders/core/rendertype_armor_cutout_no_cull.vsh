#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

#define N (1. / 8.)

in vec3 Position;
in vec4 Color;
in vec2 UV0, UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform mat4 ModelViewMat, ProjMat;
uniform int FogShape;
uniform vec3 Light0_Direction, Light1_Direction;

out float vertexDistance, ov;
out vec4 vertexColor, diffuseColor, normal;
out vec2 texCoord0, overlayCoord;

void main() {
  gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.);
  vertexDistance = fog_distance(Position, FogShape);
  vec4 v = Color;
  vec2 r = UV0;
  vec2 o = UV0;
  ov = 0.;
  if (Color.x < 1. && textureSize(Sampler0, 0).x > 64) {
    ov = 1.;
    r.x *= .5;
    v = vec4(1.);
    ivec3 key = ivec3(floor(Color.xyz * 255.));
    if      (key == ivec3(122, 150, 159)) r.y = r.y * N + 1. * N;
    else if (key == ivec3(136, 112, 192)) r.y = r.y * N + 2. * N;
    else if (key == ivec3(184,  94, 204)) r.y = r.y * N + 3. * N;
    else if (key == ivec3(230,  54,  41)) r.y = r.y * N + 4. * N;
    else if (key == ivec3(162,  99,  83)) r.y = r.y * N + 5. * N;
    else if (key == ivec3(161, 112, 179)) r.y = r.y * N + 6. * N;
    else if (key == ivec3(229, 124, 221)) r.y = r.y * N + 7. * N;
    else v = Color, r.y = r.y * N;
    o = r + vec2(.5, 0.);
  }
  vec4 l = texelFetch(Sampler2, UV2 / 16, 0);
  vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, v) * l;
  diffuseColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, vec4(1.)) * l;
  texCoord0 = r;
  overlayCoord = o;
  normal = ProjMat * ModelViewMat * vec4(Normal, 0.);
}
#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV2;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out float vertexDistance;
out vec2 texCoord0;
out vec4 vertexColor;
flat out float custom;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    custom = 0.;
    vertexDistance = fog_distance(Position, FogShape);
    texCoord0 = UV0;
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
    vec3 x = Color.xyz;
    if (x == vec3(219.,211.,160.)/255. ||
        x == vec3(169., 88., 33.)/255. ||
        x == vec3(128.,124.,123.)/255.) {
        custom = 1.;
    } else if (x == vec3(1.,0.,0.)) {
        custom = 2.;
        vertexColor = vec4(1.);
    } else if (x == vec3(0.,217.,58.)/255.) {
        custom = 3.;
        vertexColor = texelFetch(Sampler2, UV2 / 16, 0);
    } else if (x == vec3(250.,238.,77.)/255.) {
        custom = 4.;
        vertexColor = texelFetch(Sampler2, UV2 / 16, 0);
    } else if (x == vec3(127.,204.,25.)/255.) {
        custom = 5.;
        vertexColor = vec4(1.);
    } else if (x == vec3(102.,76.,51.)/255.) {
        custom = 6.;
        vertexColor = texelFetch(Sampler2, UV2 / 16, 0);
    } else if (x == vec3(127.,167.,150.)/255.) {
        custom = 2.;
        vertexColor = vec4(vec3(1.), 1. - texelFetch(Sampler2, ivec2(0, 15), 0).z);
    } else if (x == vec3(51.,76.,178.)/255.) {
        custom = 7.;
        vertexColor = texelFetch(Sampler2, UV2 / 16, 0);
    } else if (x == vec3(103.,117.,53.)/255.) {
        custom = 8.;
        vertexColor = vec4(1.);
    } else if (x == vec3(112.,2.,0.)/255.) {
        custom = 9.;
        vertexColor = texelFetch(Sampler2, UV2 / 16, 0);
    } else if (x == vec3(255.,252.,245.)/255.) {
        custom = 10.;
        vertexColor = vec4(1.);
    } else if (x == vec3(22.,126.,134.)/255.) {
        custom = 11.;
        vertexColor = vec4(1.);
    } else if (x == vec3(76.,82.,42.)/255.) {
        custom = 12.;
        vertexColor = texelFetch(Sampler2, UV2 / 16, 0);
    } else if (x == vec3(0.,124.,0.)/255.) {
        custom = 13.;
        vertexColor = texelFetch(Sampler2, UV2 / 16, 0);
    } else if (x == vec3(129.,86.,49.)/255.) {
        custom = 14.;
        vertexColor = vec4(1.);
    } else if (x == vec3(229.,229.,51.)/255.) {
        custom = 15.;
        vertexColor = vec4(1.);
    } else if (x == vec3(186.,133.,36.)/255.) {
        custom = 16.;
        vertexColor = vec4(1.);
    } else if (x == vec3(135.,107.,98.)/255.) {
        custom = 17.;
        vertexColor = vec4(1.);
    } else if (x == vec3(160.,77.,78.)/255.) {
        custom = 18.;
        vertexColor = vec4(1.);
    } else if (x == vec3(20.,180.,133.)/255.) {
        custom = 19.;
        vertexColor = vec4(1.);
    } else if (x == vec3(57.,41.,35.)/255.) {
        custom = 20.;
        vertexColor = vec4(1.);
    } else if (x == vec3(76.,62.,92.)/255.) {
        custom = 21.;
        vertexColor = vec4(1.);
    }
}

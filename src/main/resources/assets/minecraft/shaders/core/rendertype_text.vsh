#version 150

#moj_import <fog.glsl>
#moj_import <h.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler0, Sampler2;
uniform mat4 ModelViewMat, ProjMat;
uniform int FogShape;
uniform float GameTime;
uniform vec2 ScreenSize;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

out float barProgress;
out vec3 barColor;

out float effect;
out vec2 pos;

#moj_import <a.glsl>

void main() {
  pos = Position.xy;
  gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.);
  vertexDistance = fog_distance(Position, FogShape);
  texCoord0 = UV0;
  barProgress = -1.;
  barColor = vec3(1.);
  effect = 0.;
  if (Color.y == 254. / 255.) {
    vertexColor = vec4(1.);
    barProgress = Color.z;
    float c = floor(Color.x * 255.);
    if (c == 255.) {
      barColor = vec3( 251.0, 173.0, 56.0 )/255.; // event bar
    } else if (c == 254.) {
      barColor = vec3(238.,106.,99.)/255.;
    } else if (c == 253.) {
      barColor = vec3(212.,30.,50.)/255.;
    } else if (c == 252.) {
      barColor = vec3(182.,127.,225.)/255.;
    } else if (c == 251.) {
      barProgress = Color.z + 2.;
    }
  } else if (Color.xyz == vec3(250./255., 5./255., 251./255.)) {
    vertexColor = vec4(vec3(1.), 0.5);
    gl_Position = ProjMat * ModelViewMat * vec4(Position + vec3(0., 0., 350.), 1.);
  } else if (Color.xyz == vec3(1., 84./255., 1./3.)) {
    vertexColor = vec4(vec3(1., 1./3., 1./3.), abs(mod(GameTime * 16000., 20.) - 10.) / 10.);
  } else if (Color.xyz == vec3(254. / 255., 1., 1.)) {
    vertexColor = vec4(1.);
  } else if (Color.xyz == vec3(0., 0., 170.) / 255.) {
    vertexColor = vec4(int(floor(max(mod(GameTime * 12000., 50.) - 46., 0.))) == int(mod(floor(texture(Sampler0, UV0).z * 255.), 4.)));
  } else if (Color.xyz == vec3(1., 253./255., 1.)) {
    vertexColor = vec4(1.);
    effect = 1.;
  } else if (Color.xyz == vec3(1., 252./255., 1.)) {
    vertexColor = vec4(fract(GameTime * 800.) < .5 ? 1. : 0.);
  } else if (false && (Color.xyz == vec3(0., 170./255., 170./255.) || Color.xyz == vec3(0., 169./255., 170./255.))) {
    vertexColor = vec4(1.);
    effect = Color.xyz == vec3(0., 169./255., 170./255.) ? 6. : 5.;
    int id = gl_VertexID % 4;
    float x0 = (id == 2 || id == 3) ? 1. : -1.;
    float y0 = (id == 0 || id == 3) ? 1. : -1.;
    gl_Position = vec4(x0, y0, 0.001, 1.);
    texCoord0 = vec2(x0, y0) * .5 + .5;
  } else if (Color.xz == vec2(254.,4.)/255. || Color.xz == vec2(253.,4.)/255.) {
    if (Color.y <= 240./255.) {
      vertexColor = vec4(1.);
      effect = (Color.xz == vec2(253.,4.)/255. ? 3. : 2.) + (240./255. - Color.y);
      vec4 color = texture(Sampler0, UV0);
      gl_Position = vec4(color.x * 2. - 1., color.y * 2. - 1., gl_Position.z, 1.);
      texCoord0 = color.xy;
      ivec2 size = textureSize(Sampler0, 0);
      pos = UV0 - vec2(color.x, 1. - color.y) * (vec2(160., 64.) / size);
    } else {
      vertexColor = vec4(vec3(1.), Color.w);
      gl_Position = ProjMat * ModelViewMat * vec4(Position + vec3(0., 0., -1.), 1.);
    }
  } else if (Color.xz == vec2(63./255.,1./255.)) {
    vertexColor = vec4(0.);
  } else if (Color.xyz == vec3(1., 250./255., 1.)) {
    vertexColor = vec4(1.);
    gl_Position = ProjMat * ModelViewMat * vec4(Position + vec3(0., 0., 300.), 1.);
  } else if (Color.xyz == vec3(1., 251./255., 1.)) {
    gl_Position = ProjMat * ModelViewMat * vec4(Position + vec3(6., 0., 0.), 1.);
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
  } else if (Color.xyz == vec3(0., 239./255., 0.)) {
    effect = 7.;

    float guiScale = round(ScreenSize.x * ProjMat[0][0] / 2.);
    vec2 pixel = vec2(ProjMat[0][0], ProjMat[1][1]);

    ivec2 corner = ivec2(UV0 * textureSize(Sampler0, 0));

    vec2 pp = floor(texelFetch(Sampler0, corner, 0).zy * 255.);

    float rows = 6.;
    float imageWidth = 176.;
    float imageHeight = 114. + rows * 18.;
    vec2 guiSize = ceil(ScreenSize / guiScale);

    float left = floor((guiSize.x - imageWidth) / 2.);
    float top = floor((guiSize.y - imageHeight) / 2.);
    top -= 6.; // title offset

    gl_Position = vec4((pp + vec2(left, top)) * pixel + vec2(-1., 1.), 0., 1.); // z = -1 on top of items

    vertexDistance = 0.;
    vertexColor = vec4(1.);
    texCoord0 = UV0;

  } else if (Color.xyz == vec3(0., 59./255., 0.)) {
    gl_Position = vec4(0.);
    vertexColor = vec4(0.);
  } else if (A_test()) {
    effect = 4.;
    vertexColor = vec4(1.);
    gl_Position = A_transform();
    vertexDistance = 0.;
  } else {
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
  }
}
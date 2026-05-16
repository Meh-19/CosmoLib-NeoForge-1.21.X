#version 150

#moj_import <fog.glsl>
#moj_import <h.glsl>
#moj_import <m.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart, FogEnd;
uniform vec4 FogColor;
uniform float GameTime;
uniform vec2 ScreenSize;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in float barProgress;
in vec3 barColor;
in float effect;
in vec2 pos;

out vec4 fragColor;

vec2 rotate(vec2 v, float a) {
	float s = sin(a);
	float c = cos(a);
	mat2 m = mat2(c, -s, s, c);
	return m * v;
}

float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5*(a-b)/k, 0.0, 1.0);
    return mix(a, b, h) - k*h*(1.0-h);
}

float crawl(float t) {
  return (.23 * smoothstep(-1., 1., 4.2 * sin(6.2831 * (240. * GameTime + t)))) +
    3. * smin(0, .1 + .15 * sin(6.2831 * (100. * GameTime + t)), 0.02);
}

vec4 scarab(vec2 uv, float amt, float offset, float scale, ivec2 size) {
  float n = 5.;
  float an = 6.2831 / n;

  uv = rotate(uv, 6.2831 * GameTime * 150. + offset);
  
  float fa = (atan(uv.y, uv.x) + an * .5) / an;
  float ia = floor(fa);
  float sym = an * ia;
  
  float index = 4. * (ia + floor(n / 2.));
  if (index > amt) {
    return vec4(0.);
  }
  scale *= min(1., (amt - index) + 1./12.);

  vec2 p = rotate(uv, sym);
  p.x -= .6;

  p.x += scale * sin(6.2831 * (750. * GameTime + ia / n)) * 0.025;

  float mt = ia / n + offset * .5;
  float c = crawl(mt);
  p.x += c;

  p = rotate(p, 300. * (crawl(mt + 0.001) - c));

  float s = scale * .25 / 2.;
  if (abs(p.x) < s && abs(p.y) < s) {
    p *= .998;
    vec2 tex = (p + s) / (2. * s);
    float frame = mod(floor(fract(GameTime * 2400. + ia / n) * 4.) + ia, 4.);
    return texture(Sampler0, pos + vec2(8., frame * 16.) / size + tex * (16. / size));
  }
  return vec4(0.);
}

vec4 scarabs(vec2 uv, float amt, ivec2 size) {
    vec4 col = scarab(uv.yx * vec2(-1., -1.) + vec2(0., .33), amt - 3., .9, 1., size);

    vec4 layer = scarab(uv + vec2(.33, 0.), amt - 1., 0., 0.98, size);
    layer.xyz *= .8;
    col = mix(layer, col, col.a);

    layer = scarab(uv * vec2(.9, -.9) + vec2(1., 0.), amt - 2., -1.5, .66, size);
    layer.xyz *= .75;
    col = mix(layer, col, col.a);

    layer = scarab(uv.yx * vec2(-.9, .9) - vec2(0., 1.), amt, 0., .66, size);
    layer.xyz *= .7;
    col = mix(layer, col, col.a);

    return col;
}

float noise21(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 4.1414))) * 43758.5453);
}

float crateTile(vec2 uv, float time) {
    vec2 muv = (uv + vec2(0., mod(time * 5000., 10000.))) / 8.;
    vec2 center = fract(muv) - vec2(.5);
    float k = min(10, 4. * length(max(abs(center.x) / 7., abs(center.y)))) < .5 ? 1. : 0.;
    return k * abs(sin(time * 2000. + noise21(floor(muv) * 10.)));
}

vec4 finishCrate(vec4 rgb, vec2 dim, vec2 uv) {
    float x = crateTile(uv, GameTime) * .9;
    rgb.rgb += vec3(x) * vec3(.42, .4, .29) * .82 + .1;
    return rgb;
}

void main() {
  vec4 col;
  if (effect == 7.) {
    col = texture(Sampler0, texCoord0);
    if (col.x < .1) {
      discard;
    }
    col = vec4(vec3(1.), 96./255.);
  } else if (effect == 4.) {
    ivec2 coord = ivec2(texCoord0 * 128.0);
    if (coord.y < 4) {
        discard;
    }
    col = M_read(Sampler0, coord);
    if (col.w < .1) {
      discard;
    }
  } else if (effect == 5.) {
    col = vec4(vec3(16.), mix(208., 192., texCoord0.y))/255.;
  } else if (effect == 6.) {
    col = vec4(vec3(1./255.), 1.);
  } else if (effect == 7.) {
    col = texture(Sampler0, texCoord0);
    vec2 dim = vec2(textureSize(Sampler0, 0));
    vec2 uv = texCoord0 * dim;
    col = finishCrate(col, dim, uv);
  } else {
    col = texture(Sampler0, texCoord0);
    if (col == vec4(0., 1., 1., 1.)) {
      discard;
    }    
  }
  if (barProgress >= 0.) {
    float p = barProgress - 2.;
    if (p >= 0.) {
      if (col.b < p) {
        col.rgb = vec3(139./255.);
      } else {
        col.rgb = vec3(183./255., 72./255., 72./255.);
      }
    } else {
      float t = col.r * .8 + .2;
      vec3 bias;
      if (barColor.g == max(max(barColor.r, barColor.g), barColor.b)) {
        bias = vec3(t, t, mix(t*t*t, t, barColor.b));
      } else {
        bias = vec3(t, mix(t*t*t, t, barColor.g), t);
      }
      float b = col.b < barProgress ? 1. : .4;
      col.rgb = min(bias * barColor * 1.2 * b, vec3(1.));
    }
  }
  if (effect == 1.) {
    vec2 m = mod(floor(pos) / 2., 32.);
    float p = floor(fract(floor(m.x + m.y - floor(GameTime * 24000.) - col.x * 4.) / 32.) * 6.);
    vec3 c = vec3(1.);
    if(p==0.)c=vec3(236.,79.,79.);
    else if(p==1.)c=vec3(255.,169.,56.);
    else if(p==2.)c=vec3(255.,226.,50.);
    else if(p==3.)c=vec3(101.,236.,79.);
    else if(p==4.)c=vec3(79.,202.,236.);
    else if(p==5.)c=vec3(193.,113.,239.);
    col.xyz = min(col.xyz + 0.15, 1.)*c/255.;
  } else if (floor(effect) == 2. || floor(effect) == 3.) {
    float amt = floor(fract(effect) * 255.) / 12.;

    col = vec4(vec3(0.), 1.);
    ivec2 size = textureSize(Sampler0, 0);
    vec2 uv = texCoord0;
    uv.y = 1. - uv.y;
    uv.y -= .5;
    uv.x -= .5;
    float aspect = ScreenSize.x / ScreenSize.y;
    uv.x *= aspect;

    col = scarabs(uv, amt, size);
    vec4 shadow = scarabs(uv - vec2(.011), amt, size);
    shadow.rgb = vec3(0.);
    shadow.a *= .25;
    col.rgb = mix(shadow.rgb, col.rgb, col.a);
    col.a = min(1., shadow.a + col.a);

    if (floor(effect) == 3.) {
      col.rgb = mix(col.rgb, vec3(1., 0., 0.), .15);
    }
  }
  vec4 v = col * vertexColor * ColorModulator;
  if (v.w < .01) {
    discard;
  }
  fragColor = linear_fog(v, vertexDistance, FogStart, FogEnd, FogColor);
}
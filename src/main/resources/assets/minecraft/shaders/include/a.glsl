#version 150

bool A_test() {
  if ((UV0.x==0.||UV0.x==1.||UV0.y==0.||UV0.y==1.) && textureSize(Sampler0, 0) == ivec2(128)) {
    float k0 = texelFetch(Sampler0, ivec2(0, 1), 0).a;
    float k1 = texelFetch(Sampler0, ivec2(1, 1), 0).a;
    float k2 = texelFetch(Sampler0, ivec2(2, 1), 0).a;
    return k0 != 0. && k1 == 0. && k2 == 0.;
  }
  return false;
}

vec4 A_transform() {
  float x  = float(H_get(texelFetch(Sampler0, ivec2(0, 0), 0))) - 120.0;
  float xf = float(H_get(texelFetch(Sampler0, ivec2(1, 0), 0)));
  float y  = float(H_get(texelFetch(Sampler0, ivec2(2, 0), 0))) - 120.0;
  float yf = float(H_get(texelFetch(Sampler0, ivec2(3, 0), 0)));
  float a  = float(H_get(texelFetch(Sampler0, ivec2(4, 0), 0)));
  float scale = mod(a, 8.0);
  a = floor(a / 8.);
  return vec4((UV0 * vec2(128.0, -128.0) + vec2(x * 128.0 + xf, y * -124.0 + yf + 4.0)) *
    vec2(ScreenSize.y / ScreenSize.x, 1.0) * ((1.0 + scale) / (ScreenSize.y / 2.)) * ceil(ScreenSize.y / 1080.0), a / -200.0, 1.0);
}
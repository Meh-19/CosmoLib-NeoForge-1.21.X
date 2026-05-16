#version 150

uint[15] M_b(uint[16] g) {
  uint[15] r;
  for (uint i = 0u; i < 16u; i++) {
      uint a = g[i];
      for (uint j = 0u; j < i; j++) {
          uint t = r[14u - j] * 240u + a;
          r[14u - j] = t & 0xFFu;
          a = t >> 8;
      }
      if (i < 15u) {
          r[14u - i] = a & 0xFFu;
      }
  }
  return r;
}

uint M_q(uint v, uint p, uint b) {
    v = (v << 1u) | p;
    v <<= (8u - (b + 1u));
    v |= (v >> (b + 1u));
    return v;
}

vec4 M_read(sampler2D data, ivec2 coord) {
  ivec2 c = coord >> 2 << 2;
  vec4[16] a;
  for (int i = 0; i < 16; i += 4) {
    a[i + 0] = texelFetch(data, c + ivec2(0, i >> 2), 0);
    a[i + 1] = texelFetch(data, c + ivec2(1, i >> 2), 0);
    a[i + 2] = texelFetch(data, c + ivec2(2, i >> 2), 0);
    a[i + 3] = texelFetch(data, c + ivec2(3, i >> 2), 0);
  }
  uint[16] g;
  for (int i = 0; i < 16; i += 4) {
    g[i + 0] = H_get(a[i + 0]);
    g[i + 1] = H_get(a[i + 1]);
    g[i + 2] = H_get(a[i + 2]);
    g[i + 3] = H_get(a[i + 3]);
  }
  uint[15] b = M_b(g);
  // Mode 6
  uint h = ((b[1] & 0x3Fu) << 2) | ((b[0] & 0x80u) >> 6) | 1u;
  uint k = ((b[2] & 0x1Fu) << 3) | ((b[1] & 0xE0u) >> 5) | 1u;
  uint l = ((b[3] & 0x0Fu) << 4) | ((b[2] & 0xF0u) >> 4) | 1u;
  uint m = ((b[4] & 0x07u) << 5) | ((b[3] & 0xF8u) >> 3) | 1u;
  uint o = ((b[5] & 0x03u) << 6) | ((b[4] & 0xFCu) >> 2) | 1u;
  uint p = ((b[6] & 0x01u) << 7) | ((b[5] & 0xFEu) >> 1) | 1u;
  uint q = (b[6] & 0x1Eu) >> 1;
  uint r = ((b[7] & 0x01u) << 3) | ((b[6] & 0xE0u) >> 5);
  q = M_q(q, 1u, 4u);
  r = M_q(r, 1u, 4u);
  uint x =                 (b[14] << 23) | (b[13] << 15) | (b[12] << 7) | ((b[11] & 0xFEu) >> 1);
  uint y = (b[11] << 31) | (b[10] << 23) | (b[ 9] << 15) | (b[ 8] << 7) | ((b[ 7] & 0xFEu) >> 1);
  uint z = uint(coord.y & 3) * 4u + uint(coord.x & 3);
  int d = z == 0u ? 0 : int(z - 1u) * 4 + 3;
  int e = 32 - d;
  uint w = ((((d < 32 ? (y >> d) : 0u) | (e == 32 ? 0u : (e < 0 ? (x >> (-e)) : (x << e)))) & (z == 0u ? 7u : 15u)) * 64u + 7u) / 15u;
  uint f = 64u - w;
  return vec4(
    float((h * f + k * w + 32u) >> 6) / 255.0,
    float((l * f + m * w + 32u) >> 6) / 255.0,
    float((o * f + p * w + 32u) >> 6) / 255.0,
    float((q * f + r * w + 32u) >> 6) / 255.0
  );
}
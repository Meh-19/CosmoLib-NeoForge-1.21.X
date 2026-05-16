#version 150

const int PORTRAIT_BUCKETS = 32;

float portraitSkipColor(vec3 c) {
    float maxc = max(max(c.r, c.g), c.b);
    float minc = min(min(c.r, c.g), c.b);
    float diff = maxc - minc;
    float l = (maxc + minc) * .5;
    float s = (maxc - minc) / (maxc + .0001);
    if (s < (1. - min(1., l / .2))) {
        return 1.;
    }
    if (s < .6 && l < .6 || s > .05 && l >= .6 && s < .55) {
        float hue = 0.;
        if (diff > 0.) {
            if (maxc == c.r) hue = mod((c.g - c.b) / diff, 6.);
            else if (maxc == c.g) hue = (c.b - c.r) / diff + 2.;
            else hue = (c.r - c.g) / diff + 4.;
            hue /= 6.;
        }
        if (hue < .1) {
            return 1.;
        }
    }
    return 0.;
}

vec3 portraitExtractColor() {
    vec3 bucketColors[PORTRAIT_BUCKETS];
    float bucketCounts[PORTRAIT_BUCKETS];

    for (int i = 0; i < PORTRAIT_BUCKETS; i++) {
        bucketColors[i] = vec3(0);
        bucketCounts[i] = 0.;
    }

    for (int y = 4; y < 24; y++) {
        for (int x = 0; x < 32; x++) {
            if (!((y < 8) || (y >= 8 && x >= 8 && x < 20))) continue;
            vec2 uv = (vec2(x, y) + .5) / 32.;
            vec4 c = texture(Sampler0, uv);
            if (c.a < .5) continue;
            float maxc = max(max(c.r, c.g), c.b);
            float minc = min(min(c.r, c.g), c.b);
            float s = (maxc - minc) / (maxc + .0001);
            int br = int(min(3, floor(c.r * 4.)));
            int bg = int(min(1, floor(c.g * 2.)));
            int bb = int(min(3, floor(c.b * 4.)));
            int bucket = ((bb * 2 + bg) * 4 + br) % PORTRAIT_BUCKETS;
            bucketColors[bucket] += c.rgb * (c.a + (1 + s));
            bucketCounts[bucket] += c.a + (1 + s);
        }
    }

    vec3 bestColor = vec3(.35, .17, .49);
    float bestScore = 0.;

    for (int i = 0; i < PORTRAIT_BUCKETS; i++) {
        if (bucketCounts[i] < 1.) continue;
        vec3 avg = bucketColors[i] / bucketCounts[i];

        float maxc = max(max(avg.r, avg.g), avg.b);
        float minc = min(min(avg.r, avg.g), avg.b);
        float l = (maxc + minc) * .5;
        float s = (maxc - minc) / (maxc + .0001);

        float freq = bucketCounts[i] / 320.;

        float brightnessScore = 1. - abs(min(l, .5) - .5) * 2.;
        float penality = portraitSkipColor(avg);
        float score = freq * 1.1 + s * 1.4 + brightnessScore * .1 - penality * 1.5;

        if (score > bestScore) {
            bestScore = score;
            bestColor = avg;

            if (s > 0. && s < .7) {
                bestColor = (bestColor - minc) * (min(.5, maxc + .1)) / (maxc - minc) + minc;
            }
        }
    }

    return bestColor;
}

struct intersection {
    float t;
    vec3 position;
    vec3 normal;
    vec2 uv;
    vec4 albedo;
};

const float PORTRAIT_FAR = 1024.;

void boxTexCoord(inout intersection it, vec3 origin, vec3 size, const vec4 uvs[12], int offset) {
    vec3 t = it.position - origin;
    vec3 mask = abs(it.normal);
    vec2 uv = mask.x * t.zy + mask.y * t.xz + mask.z * t.xy;
    vec2 dim = mask.x * size.zy + mask.y * size.xz + mask.z * size.xy;
    uv = mod(uv / (dim * 2.) + .5, 1.);

    vec4 uvmap;
    if (it.normal.x == 1.) uvmap = uvs[offset];
    else if (it.normal.x == -1.) uvmap = uvs[offset + 1];
    else if (it.normal.y == 1.) uvmap = uvs[offset + 2];
    else if (it.normal.y == -1.) uvmap = uvs[offset + 3];
    else if (it.normal.z == 1.) uvmap = uvs[offset + 4];
    else if (it.normal.z == -1.) uvmap = uvs[offset + 5];

    it.uv = floor(mix(uvmap.xy, uvmap.zw, uv));
}

bool boxIntersect(inout intersection it, vec3 origin, vec3 direction, vec3 position, vec3 size) {
    vec3 invDir = 1. / direction;
    vec3 ext = abs(invDir) * size;
    vec3 tMin = -invDir * (origin - position) - ext;
    vec3 tMax = tMin + ext * 2.;
    float near = max(max(tMin.x, tMin.y), tMin.z);
    float far = min(min(tMax.x, tMax.y), tMax.z);
    if (near > far || far < 0. || near > it.t) return false;
    it.t = near > 0. ? near : far;
    it.normal = near > 0. ? 
        step(vec3(near), tMin) * -sign(direction) :
        step(tMax, vec3(far)) * -sign(direction);
    it.position = direction * it.t + origin;
    return true;
}

void box(inout intersection it, vec3 origin, vec3 direction, mat3 transform, vec3 position, vec3 size, const vec4 uvs[12], int layer) {
    intersection temp = it;
    vec3 originT = transform * origin;
    vec3 directionT = transform * direction;
    if (!boxIntersect(temp, originT, directionT, position, size)) return;
    boxTexCoord(temp, position, size, uvs, layer * 6);
    temp.albedo = texelFetch(Sampler0, ivec2(temp.uv), 0);
    if (temp.albedo.a < .1) return;
    
    temp.normal = temp.normal * transform;
    temp.position = direction * temp.t + origin;
    it = temp;
}

intersection rayTrace(vec3 origin, vec3 direction, float inflate) {
    const vec4 headUV[12] = vec4[](
        vec4(0, 16, 8, 8), vec4(24, 16, 16, 8), vec4(16, 0, 8, 8),
        vec4(24, 0, 16, 8), vec4(16, 16, 8, 8), vec4(24, 16, 32, 8),
        vec4(24, 16, 16, 8) + vec4(32, 0, 32, 0), vec4(0, 16, 8, 8) + vec4(32, 0, 32, 0),
        vec4(16, 0, 8, 8) + vec4(32, 0, 32, 0), vec4(24, 0, 16, 8) + vec4(32, 0, 32, 0),
        vec4(16, 16, 8, 8) + vec4(32, 0, 32, 0), vec4(24, 16, 32, 8) + vec4(32, 0, 32, 0)
    );

    const vec4 bodyUV[12] = vec4[](
        vec4(28, 32, 32, 20), vec4(20, 32, 16, 20), vec4(28, 16, 20, 20),
        vec4(36, 16, 28, 20), vec4(28, 32, 20, 20), vec4(32, 32, 40, 20),
        vec4(28, 32, 32, 20) + vec4(0, 16, 0, 16), vec4(20, 32, 16, 20) + vec4(0, 16, 0, 16),
        vec4(28, 16, 20, 20) + vec4(0, 16, 0, 16), vec4(36, 16, 28, 20) + vec4(0, 16, 0, 16),
        vec4(28, 32, 20, 20) + vec4(0, 16, 0, 16), vec4(32, 32, 40, 20) + vec4(0, 16, 0, 16)
    );

    const vec4 rightArmUV[12] = vec4[](
        vec4(40, 32, 44, 20), vec4(51, 32, 47, 20), vec4(47, 16, 44, 20),
        vec4(50, 16, 47, 20), vec4(47, 32, 44, 20), vec4(51, 32, 54, 20),
        vec4(40, 32, 44, 20) + vec4(0, 16, 0, 16), vec4(51, 32, 47, 20) + vec4(0, 16, 0, 16),
        vec4(47, 16, 44, 20) + vec4(0, 16, 0, 16), vec4(50, 16, 47, 20) + vec4(0, 16, 0, 16),
        vec4(47, 32, 44, 20) + vec4(0, 16, 0, 16), vec4(51, 32, 54, 20) + vec4(0, 16, 0, 16)
    );

    const vec4 leftArmUV[12] = vec4[](
        vec4(32, 64, 36, 52), vec4(43, 64, 39, 52), vec4(39, 48, 36, 52),
        vec4(42, 48, 39, 52), vec4(39, 64, 36, 52), vec4(43, 64, 46, 52),
        vec4(32, 64, 36, 52) + vec4(16, 0, 16, 0), vec4(43, 64, 39, 52) + vec4(16, 0, 16, 0),
        vec4(39, 48, 36, 52) + vec4(16, 0, 16, 0), vec4(42, 48, 39, 52) + vec4(16, 0, 16, 0),
        vec4(39, 64, 36, 52) + vec4(16, 0, 16, 0), vec4(43, 64, 46, 52) + vec4(16, 0, 16, 0)
    );

    const float rightArmR = radians(6.);
    const float leftArmR = radians(-6.);
    const mat3 rightArmT = mat3(cos(rightArmR), -sin(rightArmR), 0, sin(rightArmR), cos(rightArmR), 0, 0, 0, 1);
    const mat3 leftArmT = mat3(cos(leftArmR), -sin(leftArmR), 0, sin(leftArmR), cos(leftArmR), 0, 0, 0, 1);

    intersection it = intersection(PORTRAIT_FAR, vec3(0), vec3(0), vec2(0), vec4(1));
    
    box(it, origin, direction, mat3(1), vec3(0, 6, 0), vec3(4, 6, 2) + .25 + inflate, bodyUV, 1);
    box(it, origin, direction, leftArmT, vec3(-6.725, 5.5, 0), vec3(1.5, 6, 2) + .25 + inflate, leftArmUV, 1);
    box(it, origin, direction, rightArmT, vec3(6.725, 5.5, 0), vec3(1.5, 6, 2) + .25 + inflate, rightArmUV, 1);
    box(it, origin, direction, mat3(1), vec3(0, 16, 0), vec3(4, 4, 4) + .5 + inflate, headUV, 1);
    
    box(it, origin, direction, mat3(1), vec3(0, 6, 0), vec3(4, 6, 2) + inflate, bodyUV, 0);
    box(it, origin, direction, leftArmT, vec3(-6.725, 5.5, 0), vec3(1.5, 6, 2) + inflate, leftArmUV, 0);
    box(it, origin, direction, rightArmT, vec3(6.725, 5.5, 0), vec3(1.5, 6, 2) + inflate, rightArmUV, 0);
    box(it, origin, direction, mat3(1), vec3(0, 16, 0), vec3(4, 4, 4) + inflate, headUV, 0);

    return it;
}

vec3 portraitCalculateLighting(vec3 normal, vec3 lightDir) {
    float NdotL = max(dot(normal, lightDir), 0.);
    return sqrt(vec3(.3 + .7 * NdotL));
}

float portraitSoftLight(float b, float f) {
    return (f < .5) ?
        (2. * b * f + b * b * (1. - 2. * f)) :
        (sqrt(b) * (2. * f - 1.) + (2. * b * (1. - f)));
}

vec3 portraitSoftLightSimple(vec3 base, vec3 blend, float alpha) {
    return mix(base, vec3(
        portraitSoftLight(base.r, blend.r),
        portraitSoftLight(base.g, blend.g),
        portraitSoftLight(base.b, blend.b)
    ), alpha);
}

vec4 portraitRender(vec2 uv, float aspectRatio, vec3 color) {
  vec3 start = color * .5;
  vec3 end = color * .25;
  vec3 result = mix(start, end, uv.y);

  vec3 direction = normalize(vec3(-1));
  vec3 side = normalize(cross(vec3(0, 1, 0), direction));
  vec3 up = cross(direction, side);

  vec2 uv1 = uv * 2 - 1;
  vec3 origin = vec3(1, 1.75, 1) * 20;

  origin += 10 * (side * uv1.x * aspectRatio - up * uv1.y);

  intersection it = rayTrace(origin, direction, 0);
  
  if (it.t >= PORTRAIT_FAR) {
    it = rayTrace(origin, direction, .25);
    if (it.t < PORTRAIT_FAR) {
      result = mix(result, vec3(0), it.albedo.a);
    }
  } else {
    vec3 lightDir = normalize(vec3(0, 1, .5));
    vec3 lighting = portraitCalculateLighting(it.normal, lightDir);
    
    vec4 c = it.albedo;

    if (false&&c.a > .5) {
        if (portraitSkipColor(c.rgb) == 1.) {
            c.rgb = vec3(1, 0, 1);
        }
    }

    c.rgb *= lighting;
    
    result = mix(result, c.rgb, c.a);
  }

  result = portraitSoftLightSimple(result, mix(vec3(.20, .18, .14), vec3(.97, .90, .74), sqrt(1 - uv.y)), 1);

  return vec4(result, 1);
}

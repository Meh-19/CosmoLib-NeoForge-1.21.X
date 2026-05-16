#version 150

vec4 avatarRenderHead(ivec2 pixel) {
    vec4 fragColor = vec4(0.0);

    fragColor = texelFetch(Sampler0, pixel + ivec2(40, 8), 0);

    if (fragColor.a < 1.0) {
        vec4 tex = texelFetch(Sampler0, pixel + 8, 0);
        if (tex.a > 0.1) {
            fragColor.rgb = mix(tex.rgb, fragColor.rgb, fragColor.a);
            fragColor.a = 1.0;
        }
    }

    if (fragColor.a > 0.1 && fragColor.a < 1.0) {
        fragColor.rgb = mix(vec3(0.0), fragColor.rgb, fragColor.a);
        fragColor.a = 1.0;
    }

    return fragColor;
}

vec3 avatarRgb2hsl(vec3 c) {
    float maxC = max(c.r, max(c.g, c.b));
    float minC = min(c.r, min(c.g, c.b));
    float delta = maxC - minC;
    float h = 0.;
    float s = 0.;
    float l = (maxC + minC) * .5;
    if (delta > 0.) {
        s = (l < .5) 
            ? (delta / (maxC + minC)) 
            : (delta / (2. - maxC - minC));
        float deltaR = (((maxC - c.r) / 6.) + (delta / 2.)) / delta;
        float deltaG = (((maxC - c.g) / 6.) + (delta / 2.)) / delta;
        float deltaB = (((maxC - c.b) / 6.) + (delta / 2.)) / delta;
        if      (c.r == maxC) h = deltaB - deltaG;
        else if (c.g == maxC) h = (1. / 3.) + deltaR - deltaB;
        else if (c.b == maxC) h = (2. / 3.) + deltaG - deltaR;
        h = fract(h);
    }
    return vec3(h, s, l);
}

vec3 avatarHue2rgb(float h) {
    h = fract(h);
    return clamp(vec3(
        abs(h * 6. - 3.) - 1.,
        2. - abs(h * 6. - 2.),
        2. - abs(h * 6. - 4.)
    ), 0., 1.);
}

vec3 avatarHsl2rgb(vec3 hsl) {
    vec3 rgb = avatarHue2rgb(hsl.x);
    float c = (1. - abs(2. * hsl.z - 1.)) * hsl.y;
    return (rgb - .5) * c + hsl.z;
}

vec3 avatarAdjustHueSatLight(vec3 col, float h, float s, float l, float alpha) {
    float minC = min(min(col.r, col.g), col.b);
    float maxC = max(max(col.r, col.g), col.b);
    float delta = maxC - minC;
    float value = maxC + minC;
    float deltaR = (((maxC - col.r) / 6.) + (delta / 2.)) / delta;
    float deltaG = (((maxC - col.g) / 6.) + (delta / 2.)) / delta;
    float deltaB = (((maxC - col.b) / 6.) + (delta / 2.)) / delta;
    float hue = 0.;
    float saturation = 0.;
    float lightness = value / 2.;
    if (delta > 0.) {         
        if (maxC == col.r) {
            hue = deltaB - deltaG;
        } else if (maxC == col.g) {
            hue = (1. / 3.) + deltaR - deltaB;
        } else { 
            hue = (2. / 3.) + deltaG - deltaR;
        }
        hue = fract(hue);
        if (lightness < .5) {
            saturation = delta / value;
        } else {
            saturation = delta / (2. - value);
        }
    }
    hue = fract(hue + h);
    col = avatarHsl2rgb(vec3(hue, saturation, lightness));
    if (delta > 0.) {
        float a = 0.;
        if (s >= 0.) {
            if ((s + saturation) >= 1.) {
                a = saturation;
            } else {
                a = 1. - s;
            }
            a = 1. / a - 1.;
            col = col + (col - lightness) * a;
        } else {
            a = s;
            col = lightness + (col - lightness) * (1. + a);
        }
    }
    if (l > 0.) {
        col = col * (1. - l) + l;
    } else if (l < 0.) {
        col = col + col * l;
    }
    return mix(col, col, alpha);
}

float avatarSoftLight(float b, float f) {
    return (f < .5) ?
        (2. * b * f + b * b * (1. - 2. * f)) :
        (sqrt(b) * (2. * f - 1.) + (2. * b * (1. - f)));
}

vec3 avatarSoftLightSimple(vec3 base, vec3 blend, float alpha) {
    return mix(base, vec3(
        avatarSoftLight(base.r, blend.r),
        avatarSoftLight(base.g, blend.g),
        avatarSoftLight(base.b, blend.b)
    ), alpha);
}

float avatarCubicSpline(float x, vec3 cur, vec3 next) {
    float h = next.x - cur.x, t = (x - cur.x) / h, a = 1. - t;
    return mix(cur.y, next.y, t) + (h*h / 6.) * ((a*a*a - a) * cur.z + (t*t*t - t) * next.z);
}

float avatarCurve(float x) {
    const vec3 a = vec3 AVATAR_DISABLED_CURVE_A;
    const vec3 b = vec3 AVATAR_DISABLED_CURVE_B;
    if (x < a.x) {
        return avatarCubicSpline(x, vec3(vec2(0.), 0.), a);
    }
    if (x < b.x) {
        return avatarCubicSpline(x, a, b);
    }
    return avatarCubicSpline(x, b, vec3(vec2(1.), 0.));
}

vec3 avatarCurveAdjustment(vec3 col) {
    return vec3(
        avatarCurve(col.r),
        avatarCurve(col.g),
        avatarCurve(col.b)
    );
}

vec3 avatarSRGBtoLinear(vec3 srgb) {
    return pow(srgb, vec3(2.2));
}

vec3 avatarLinearTosRGB(vec3 linear) {
    return pow(linear, vec3(1./2.2));
}

vec4 avatarRender(ivec2 headPixel, ivec2 truePixel, bool disabled) {
    vec4 fragColor = avatarRenderHead(headPixel);
    
    fragColor.rgb = avatarAdjustHueSatLight(fragColor.rgb,
        AVATAR_BASE_HUE/100.,
        AVATAR_BASE_SAT_ADJ/100.,
        AVATAR_BASE_LIGHT_ADJ/100.,
        AVATAR_BASE_OPACITY/100.
    );
    
    fragColor.rgb = avatarSoftLightSimple(fragColor.rgb,
        vec3 AVATAR_OVERLAY_COLOR /255.,
        AVATAR_OVERLAY_OPACITY/100.
    );
    
    if (disabled&&false) {
        if (truePixel.x == 0 ||
               truePixel.y == 0 ||
               truePixel.x == (AVATAR_SIZE - 1) ||
               truePixel.y == (AVATAR_SIZE - 1)) {
            fragColor.rgb = avatarSoftLightSimple(fragColor.rgb,
                vec3 AVATAR_OUTLINE_COLOR /255.,
                AVATAR_OUTLINE_OPACITY/100.
            );
        }

        fragColor.rgb = avatarAdjustHueSatLight(fragColor.rgb,
            AVATAR_DISABLED_HUE/100.,
            AVATAR_DISABLED_SAT_ADJ/100.,
            AVATAR_DISABLED_LIGHT_ADJ/100.,
            AVATAR_DISABLED_OPACITY/100.
        );

        fragColor.rgb = avatarSRGBtoLinear(avatarCurveAdjustment(avatarLinearTosRGB(fragColor.rgb)));
    } else if (truePixel.y == (AVATAR_SIZE - 1)) {
        fragColor.rgb = avatarAdjustHueSatLight(fragColor.rgb,
            AVATAR_SHADOW_HUE/100.,
            AVATAR_SHADOW_SAT_ADJ/100.,
            AVATAR_SHADOW_LIGHT_ADJ/100.,
            AVATAR_SHADOW_OPACITY/100.
        );
    } else if (truePixel.x == 0 ||
               truePixel.y == 0 ||
               truePixel.x == (AVATAR_SIZE - 1) ||
               truePixel.y == (AVATAR_SIZE - 2)) {
        fragColor.rgb = avatarSoftLightSimple(fragColor.rgb,
            vec3 AVATAR_OUTLINE_COLOR /255.,
            AVATAR_OUTLINE_OPACITY/100.
        );
    }
    
    return fragColor;
}

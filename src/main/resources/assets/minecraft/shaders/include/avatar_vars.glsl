#version 150

#define AVATAR_SIZE 14

// Base hue, saturation, and light adjustments
#define AVATAR_BASE_HUE           0
#define AVATAR_BASE_SAT_ADJ      -8
#define AVATAR_BASE_LIGHT_ADJ   -12
#define AVATAR_BASE_OPACITY     100

// Overlay color
#define AVATAR_OVERLAY_COLOR     (227, 199, 140)
#define AVATAR_OVERLAY_OPACITY   30

// Bottom "shadow" row pixel adjustments
#define AVATAR_SHADOW_HUE           0
#define AVATAR_SHADOW_SAT_ADJ     -12
#define AVATAR_SHADOW_LIGHT_ADJ   -36
#define AVATAR_SHADOW_OPACITY     100

// Outline overlay
#define AVATAR_OUTLINE_COLOR     (255, 255, 255)
#define AVATAR_OUTLINE_OPACITY   100

// Disabled hue, saturation, and light adjustments
#define AVATAR_DISABLED_HUE           0
#define AVATAR_DISABLED_SAT_ADJ     -70
#define AVATAR_DISABLED_LIGHT_ADJ   -40
#define AVATAR_DISABLED_OPACITY     100

// Disabled color curve adjustment
#define AVATAR_DISABLED_CURVE_A   (0.3225, 0.13, 5.73697)
#define AVATAR_DISABLED_CURVE_B   (0.795, 0.8425, -5.27571)
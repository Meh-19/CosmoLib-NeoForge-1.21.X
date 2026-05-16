// FinishSampler is declared in the FSH as:
//   uniform sampler2D Sampler3;
//   #define FinishSampler Sampler3
//
// Atlas pixel origin within the atlas texture (standalone 512x512 atlas).
const vec2 FINISH_ATLAS = vec2(0.0);

// Use this macro instead of /dim for all atlas texture lookups.
// It uses FinishSampler's own dimensions, not the entity texture's dimensions.
#define ATLAS_DIM vec2(textureSize(FinishSampler, 0))

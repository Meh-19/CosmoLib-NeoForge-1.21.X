#version 150

uint H_A[80] = uint[80](
    0x00c00000u, 0x69d97f0au, 0x00da0005u, 0x00fe00cau, 
    0x3e3a6b00u, 0xb6ce2e0eu, 0xcade1037u, 0x0029bbc1u, 
    0xbac3f500u, 0x4526154eu, 0x99f85900u, 0x1367004fu, 
    0x65003000u, 0xe0df94bbu, 0xef950000u, 0x8dfe0723u, 
    0xd7c90000u, 0xa362002bu, 0xba000030u, 0xa2f7002bu, 
    0xd71d83fcu, 0x00cb0ff0u, 0xf61c9200u, 0x2b003b37u, 
    0x4e2a9d00u, 0x0025f2c9u, 0x00e06095u, 0x014fa90bu, 
    0xe18f0015u, 0xb11106d2u, 0x240000bfu, 0x005e72a4u, 
    0x0097c421u, 0x347d1799u, 0xaf9b000bu, 0x7700002fu, 
    0x31051700u, 0x003a00dau, 0x95030000u, 0x00000bd4u, 
    0x57a10074u, 0x26396a0cu, 0x9f67f400u, 0xb2257fa7u, 
    0xa3b30012u, 0x7b002349u, 0x6a3c0090u, 0x42285e94u, 
    0x662695deu, 0x003a00f3u, 0x5c85f200u, 0xc8b20079u, 
    0x23171600u, 0x2cf40773u, 0x2600d1f0u, 0x0003dc00u, 
    0x4e00dbd0u, 0x000bd200u, 0x2200fccfu, 0x33b450bau, 
    0x5f960c00u, 0x00afc200u, 0x334d5b85u, 0x0d373224u, 
    0x00c68600u, 0x90000000u, 0x04a5bdc0u, 0x978119e6u, 
    0x549a0f26u, 0x5918e885u, 0x00000004u, 0xfd005a3fu, 
    0xa3542960u, 0x06006d2eu, 0xfb005fc4u, 0x00006384u, 
    0x2a1dec10u, 0xb8a50aceu, 0xd6f2012du, 0x1f002a30u
);

uint H_h(int x) {
    x = max(0, x - 1);
    uint y = H_A[x >> 2];
    uint z;
    switch (x >> 5) {
        case 0: z = 0x43280110u; break;
        case 1: z = 0x30000060u; break;
        case 2: z = 0x80041000u; break;
        case 3: z = 0x08008000u; break;
        case 4: z = 0x20040320u; break;
        case 5: z = 0x00210090u; break;
        case 6: z = 0x000e0000u; break;
        case 7: z = 0x10008000u; break;
        case 8: z = 0x01030000u; break;
        case 9: z = 0xa2010090u; break;
        default: z = 0u;
    }
    return (((z >> (x & 31)) & 1u) << 8) | ((y >> ((x & 3) << 3)) & 0xffu);
}

uint H_get(vec4 c) {
    ivec3 H_1 = ivec3(103, 49, 313);
    ivec3 H_2 = ivec3(103, 112, 119);
    ivec3 s = ivec3(c.xyz * 255.0);
    ivec3 s1 = s * H_1 + H_1;
    ivec3 s2 = s * H_2 + H_2;
    int f1 = (s1.x + s1.y + s1.z) % 321;
    int f2 = (s2.x + s2.y + s2.z) % 321;
    return (H_h(f1) + H_h(f2)) % 321u;
}

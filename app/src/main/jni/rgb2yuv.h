//
// Created by changjianhui on 2018/8/26.
//

#ifndef FACEEXPRESSION_RGB2YUV_H
#define FACEEXPRESSION_RGB2YUV_H
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

void ConvertARGB8888ToYUV420SP(const uint32_t* const input,
                               uint8_t* const output, int width, int height);

void ConvertRGB565ToYUV420SP(const uint16_t* const input, uint8_t* const output,
                             const int width, const int height);

#ifdef __cplusplus
}
#endif

#endif //FACEEXPRESSION_RGB2YUV_H

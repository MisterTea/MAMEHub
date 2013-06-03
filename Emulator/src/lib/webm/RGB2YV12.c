#define rgbtoy(b, g, r, y) \
    y=(unsigned char)(((int)(30*r) + (int)(59*g) + (int)(11*b))/100)

#define rgbtoyuv(b, g, r, y, u, v) \
    rgbtoy(b, g, r, y); \
    u=(unsigned char)(((int)(-17*r) - (int)(33*g) + (int)(50*b)+12800)/100); \
    v=(unsigned char)(((int)(50*r) - (int)(42*g) - (int)(8*b)+12800)/100)

void RGBtoYUV420PSameSize (const unsigned char * rgb,
    unsigned char * yuv,
    unsigned rgbIncrement,
    unsigned char flip,
    int srcFrameWidth, int srcFrameHeight)
{
    unsigned int planeSize;
    unsigned int halfWidth;

    unsigned char * yplane;
    unsigned char * uplane;
    unsigned char * vplane;
    const unsigned char * rgbIndex;

    int x, y;
    unsigned char * yline;
    unsigned char * uline;
    unsigned char * vline;

    planeSize = srcFrameWidth * srcFrameHeight;
    halfWidth = srcFrameWidth >> 1;

    // get pointers to the data
    yplane = yuv;
    uplane = yuv + planeSize;
    vplane = yuv + planeSize + (planeSize >> 2);
    rgbIndex = rgb;

    for (y = 0; y < srcFrameHeight; y++)
    {
        yline = yplane + (y * srcFrameWidth);
        uline = uplane + ((y >> 1) * halfWidth);
        vline = vplane + ((y >> 1) * halfWidth);

        if (flip)
            rgbIndex = rgb + (srcFrameWidth*(srcFrameHeight-1-y)*rgbIncrement);

        for (x = 0; x < (int) srcFrameWidth; x+=2)
        {
            rgbtoyuv(rgbIndex[0], rgbIndex[1], rgbIndex[2], *yline, *uline, *vline);
            rgbIndex += rgbIncrement;
            yline++;
            rgbtoyuv(rgbIndex[0], rgbIndex[1], rgbIndex[2], *yline, *uline, *vline);
            rgbIndex += rgbIncrement;
            yline++;
            uline++;
            vline++;
        }
    }
}


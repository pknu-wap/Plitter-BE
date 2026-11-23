package com.playlist.plitter.character.infrastructure.s3;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImageStrokeNormalizationTest {
    @Test void preservesFaintBlackEdgeAlpha() {
        var image=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,0x20000000);
        S3ImageStorageClient.normalizeForegroundStrokes(image);
        assertEquals(32,image.getRGB(0,0)>>>24);
    }
    @Test void convertsOpaqueGrayToEquivalentBlackAlpha() {
        var image=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,0xff808080);
        S3ImageStorageClient.normalizeForegroundStrokes(image);
        assertEquals(127,image.getRGB(0,0)>>>24);
        assertEquals(0,image.getRGB(0,0)&0x00ffffff);
    }
    @Test void clearsWhiteAndRetainsTransparentAndOpaqueBlack() {
        var image=new BufferedImage(3,1,BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,0xffffffff);image.setRGB(1,0,0);image.setRGB(2,0,0xff000000);
        S3ImageStorageClient.normalizeForegroundStrokes(image);
        assertEquals(0,image.getRGB(0,0));assertEquals(0,image.getRGB(1,0));assertEquals(0xff000000,image.getRGB(2,0));
    }
}

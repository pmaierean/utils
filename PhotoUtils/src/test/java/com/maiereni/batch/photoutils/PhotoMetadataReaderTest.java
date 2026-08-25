package com.maiereni.batch.photoutils;

import com.maiereni.photo.photoutils.PhotoMetadataReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author pmaierean on 2026-08-17
 *
 **/
public class PhotoMetadataReaderTest {
    private final PhotoMetadataReader photoMetadataReader = new PhotoMetadataReader();

    @Test
    public void testMetadataReader() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/IMG_0005.JPEG")) {
            Assert.notNull(is, "The resource IMG_0005.JPEG is null");
            byte[] bytes = IOUtils.toByteArray(is);
            File f = File.createTempFile("PhotoUtilsTest", ".jpg");
            f.deleteOnExit();
            FileUtils.writeByteArrayToFile(f, bytes);
            Properties props = photoMetadataReader.getMetadata(f);
            String[] s = props.getProperty("DateTime")
                    .replaceAll("'", "")
                    .replaceAll(":", "\\\\").split(" ");
            System.out.println("Creation date is " + s[0]);
        }
    }
}

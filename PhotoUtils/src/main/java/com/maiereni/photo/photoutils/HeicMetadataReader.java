package com.maiereni.photo.photoutils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Properties;

/**
 *
 * @author pmaierean on 2026-06-17
 *
 **/
@Slf4j
public class HeicMetadataReader {

    /**
     * Read properties of a HEIC file
     * @param file the file
     * @return the properties
     * @throws Exception failed to read
     */
    public Properties getMetadata(final File file) throws Exception {
        Assert.notNull(file, "The argument cannot be null");
        Properties properties = new Properties();
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                if (tag.getTagName().equalsIgnoreCase("GPS Date Stamp")) {
                    properties.put("takenDate", tag.getDescription());
                }
            }
        }
        return properties;
    }
}

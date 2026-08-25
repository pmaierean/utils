/*
 * =========================================================================================
 * Copyright (c) 2024 - 2025 to Maiereni Software and Consulting Inc
 * =========================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations
 *  under the License.
 *
 */
package com.maiereni.photo.photoutils;

import com.maiereni.photo.photoutils.bo.FileCopierConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * @author Petre Maierean
 * @date 10/27/2024 5:13 PM
 **/
@Component
@Slf4j
public class FileCopier implements CommandLineRunner  {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy\\MM\\dd");
    private static final PhotoMetadataReader reader = new PhotoMetadataReader();

    /**
     * Copy files
     * @param configuration
     * @throws Exception
     */
    public void copyFiles(FileCopierConfig configuration) throws Exception {
        Assert.notNull(configuration, "The configuration cannot be null");
        Assert.notNull(configuration.getDestDir(), "The destination dir cannot be null");
        Assert.notNull(configuration.getSourceDir(), "The source dir cannot be null");
        File fSource = new File(configuration.getSourceDir());
        Assert.isTrue(fSource.isDirectory(), "The source dir must exist");
        File fDest = new File(configuration.getDestDir());
        if (!fDest.isDirectory()) {
            Assert.isTrue(fDest.mkdirs(),"The destination dir must be created");
        }
        copyFromSubdir(fSource, fDest, configuration);
    }

    private void copyFromSubdir(File fSource, File fDest, FileCopierConfig configuration) {
        File[] childrenSrc = fSource.listFiles();
        if (childrenSrc != null) {
            StringBuilder sb = new StringBuilder();
            StringBuilder sbNotCopied = new StringBuilder();
            for (File child : childrenSrc) {
                if (child.isFile()) {
                    String fileName = child.getName().toLowerCase();
                    if (configuration.isHeic() && !fileName.endsWith("heic")) {
                        continue;
                    }
                    if (fileName.endsWith("mov") || fileName.endsWith("mp4")) {
                        continue;
                    }
                    try {
                        Properties props = reader.getMetadata(child);
                        Date date = new Date(child.lastModified());
                        String sDate = SDF.format(date);
                        if (props.getProperty("takenDate") != null) {
                            sDate = props.getProperty("takenDate").replaceAll(":", "\\\\");
                        }
                        if (props.getProperty("DateTime") != null) {
                            String[] s = props.getProperty("DateTime")
                                    .replaceAll("'", "")
                                    .replaceAll(":", "\\\\").split(" ");
                            sDate = s[0];
                        }
                        File destChildDir = new File(fDest, sDate);
                        if (!destChildDir.isDirectory()) {
                            log.debug("Make a destination dir: {}", destChildDir.getPath());
                            Assert.isTrue(destChildDir.mkdirs(), "Could not create destination dir");
                        }
                        File destChild = new File(destChildDir, child.getName());
                        if (!destChild.exists()) {
                            FileUtils.copyFile(child, destChild);
                            sb.append(child.getName()).append("\n");
                        } else {
                            sbNotCopied.append(child.getName()).append("\n");
                        }
                        if (configuration.isMove()) {
                            child.deleteOnExit();
                        }
                    } catch (Exception e) {
                        log.error("Cannot load properties of the file {}", child.getAbsolutePath(), e);
                    }
                } else {
                    copyFromSubdir(child, fDest, configuration);
                }
            }
        }
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("Run command " + args);
        FileCopierConfig configuration = new FileCopierConfig();
        String jpg = null;
        for(String arg: args) {
            if (arg.startsWith("srcDir=")) {
                configuration.setSourceDir(arg.substring("srcDir=".length()));
            }
            else if (arg.startsWith("destDir=")) {
                configuration.setDestDir(arg.substring("destDir=".length()));
            }
            else if (arg.startsWith("move=")) {
                boolean move = arg.substring("move=".length()).equalsIgnoreCase("true");
                configuration.setMove(move);
            }
            else if (arg.startsWith("heic=")) {
                boolean heic = arg.substring("heic=".length()).equalsIgnoreCase("true");
                configuration.setHeic(heic);
            }
            else if (arg.startsWith("sourceFile=")) {
                jpg = arg.substring("sourceFile=".length());
            }
        }
        if (configuration.isMove() && StringUtils.isNoneBlank(configuration.getSourceDir(), configuration.getDestDir())) {
            copyFiles(configuration);
        }
        else if (StringUtils.isNotBlank(jpg)) {
            reader.metadataExtractor(jpg);
        }
        else {
            log.error("File copier will not process. Expected arguments: srcDir=<srcDir> destDir=<destDir> [move]");
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            new FileCopier().run(args);
        }
        catch (Exception e) {
            log.error("Failed to process", e);
        }
    }
}

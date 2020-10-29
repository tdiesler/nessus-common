package io.nessus.common.utils;

/*-
 * #%L
 * Nessus :: API
 * %%
 * Copyright (C) 2018 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import io.nessus.common.AssertArg;
import io.nessus.common.AssertState;

public final class FileUtils {

    // Hide ctor
    private FileUtils() {};
    
    public static boolean recursiveDelete(Path path) throws IOException {
        AssertArg.notNull(path, "Null path");
        
        if (path.toFile().exists()) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        
        return !path.toFile().exists();
    }
    
    public static boolean recursiveCopy(Path srcPath, Path dstPath) throws IOException {
    	AssertArg.notNull(srcPath, "Null srcPath");
    	AssertArg.notNull(dstPath, "Null destPath");
    	
    	AssertState.isTrue(srcPath.toFile().exists(), "Does not exist: " + srcPath);
        
        Files.walkFileTree(srcPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relPath = srcPath.relativize(file);
                Path outPath = dstPath.resolve(relPath);
                outPath.getParent().toFile().mkdirs();
                Files.copy(file, outPath);
                return FileVisitResult.CONTINUE;
            }
        });
        
        return !dstPath.toFile().exists();
    }

	public static void atomicMove(Path srcPath, Path dstPath) throws IOException {
        recursiveDelete(dstPath);
        Files.move(srcPath, dstPath, StandardCopyOption.ATOMIC_MOVE);
	}
}


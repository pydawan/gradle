/*
 * Copyright 2018 the original author or authors.
 *
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
 */

package org.gradle.swiftpm.internal;

import com.google.common.collect.ImmutableSet;
import org.gradle.api.file.FileCollection;
import org.gradle.swiftpm.Product;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;

public abstract class AbstractProduct implements Product, Serializable {
    private final String name;
    private final String targetName;
    private final File path;
    private final Collection<File> sourceFiles;
    private final Collection<String> requiredTargets;
    private final Collection<String> requiredProducts;

    AbstractProduct(String name, String targetName, File path, FileCollection sourceFiles, Collection<String> requiredTargets, Collection<String> requiredProducts) {
        this.name = name;
        this.targetName = targetName;
        this.path = path;
        this.sourceFiles = ImmutableSet.copyOf(sourceFiles);
        this.requiredTargets = requiredTargets;
        this.requiredProducts = requiredProducts;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getTargetName() {
        return targetName;
    }

    public File getPath() {
        return path;
    }

    public Collection<File> getSourceFiles() {
        return sourceFiles;
    }

    public Collection<String> getRequiredTargets() {
        return requiredTargets;
    }

    public Collection<String> getRequiredProducts() {
        return requiredProducts;
    }

    public abstract boolean isExecutable();
}

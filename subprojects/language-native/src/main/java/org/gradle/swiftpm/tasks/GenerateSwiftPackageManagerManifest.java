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

package org.gradle.swiftpm.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.swiftpm.Product;
import org.gradle.swiftpm.internal.AbstractProduct;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

public class GenerateSwiftPackageManagerManifest extends DefaultTask {
    private final RegularFileProperty manifestFile = newOutputFile();
    private final SetProperty<Product> products = getProject().getObjects().setProperty(Product.class);

    @Input
    public SetProperty<Product> getProducts() {
        return products;
    }

    @OutputFile
    public RegularFileProperty getManifestFile() {
        return manifestFile;
    }

    @TaskAction
    public void generate() {
        Path manifest = manifestFile.get().getAsFile().toPath();
        try {
            Path baseDir = manifest.getParent();
            Files.createDirectories(baseDir);
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(manifest, Charset.forName("utf-8")));
            try {
                writer.println("// swift-tools-version:4.0");
                writer.println("//");
                writer.println("// GENERATED FILE - do not edit");
                writer.println("//");
                writer.println("import PackageDescription");
                writer.println();
                writer.println("let package = Package(");
                writer.println("    name: \"" + getProject().getName() + "\",");
                writer.println("    products: [");
                for (Product p : products.get()) {
                    AbstractProduct product = (AbstractProduct) p;
                    if (product.isExecutable()) {
                        writer.print("        .executable(");
                    } else {
                        writer.print("        .library(");
                    }
                    writer.print("name: \"");
                    writer.print(product.getName());
                    writer.print("\", targets: [\"");
                    writer.print(product.getName());
                    writer.println("\"]),");
                }
                writer.println("    ],");
                writer.println("    targets: [");
                for (Product p : products.get()) {
                    AbstractProduct product = (AbstractProduct) p;
                    writer.println("        .target(");
                    writer.print("            name: \"");
                    writer.print(product.getName());
                    writer.println("\",");
                    if (!product.getDependencies().isEmpty()) {
                        writer.println("            dependencies: [");
                        for (String dep : product.getDependencies()) {
                            writer.print("                .target(name: \"");
                            writer.print(dep);
                            writer.println("\"),");
                        }
                        writer.println("            ],");
                    }
                    writer.print("            path: \"");
                    Path productPath = product.getPath().toPath();
                    String relPath = baseDir.relativize(productPath).toString();
                    writer.print(relPath.isEmpty() ? ".": relPath);
                    writer.println("\",");
                    writer.println("            sources: [");
                    Set<String> sorted = new TreeSet<String>();
                    for (File sourceFile : product.getSourceFiles()) {
                        sorted.add(productPath.relativize(sourceFile.toPath()).toString());
                    }
                    for (String sourcePath : sorted) {
                        writer.print("                \"");
                        writer.print(sourcePath);
                        writer.println("\",");
                    }
                    writer.println("            ]");
                    writer.println("        ),");
                }
                writer.println("    ]");
                writer.println(")");
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new GradleException(String.format("Could not write manifest file %s.", manifest), e);
        }
    }
}

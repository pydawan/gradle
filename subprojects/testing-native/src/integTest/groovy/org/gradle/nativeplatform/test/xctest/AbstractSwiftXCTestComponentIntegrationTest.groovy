/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.nativeplatform.test.xctest

import org.gradle.internal.os.OperatingSystem
import org.gradle.language.swift.AbstractSwiftComponentIntegrationTest
import org.gradle.nativeplatform.fixtures.app.Swift3XCTest
import org.gradle.nativeplatform.fixtures.app.Swift4XCTest
import org.gradle.nativeplatform.fixtures.app.XCTestSourceElement
import org.junit.Assume

abstract class AbstractSwiftXCTestComponentIntegrationTest extends AbstractSwiftComponentIntegrationTest {

    def setup() {
        // TODO: Temporarily disable XCTests with Swift3 on macOS
        Assume.assumeFalse(OperatingSystem.current().isMacOsX() && toolChain.version.major == 3)
    }

    @Override
    protected String getComponentUnderTestDsl() {
        return "xctest"
    }

    @Override
    String getTaskNameToAssembleDevelopmentBinary() {
        return "test"
    }

    @Override
    String getDevelopmentBinaryCompileTask() {
        return ":compileTestSwift"
    }

    @Override
    XCTestSourceElement getSwift3Component() {
        return new Swift3XCTest('project')
    }

    @Override
    XCTestSourceElement getSwift4Component() {
        return new Swift4XCTest('project')
    }

    @Override
    List<String> getTasksToAssembleDevelopmentBinaryOfComponentUnderTest() {
        return [":compileTestSwift", ":linkTest", ":installTest", ":xcTest"]
    }
}

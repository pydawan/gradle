/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.nativeplatform.fixtures;

import org.gradle.integtests.fixtures.AbstractMultiTestRunner;

import java.util.List;

public class SingleToolChainTestRunner extends AbstractMultiTestRunner {
    private static final String TOOLCHAINS_SYSPROP_NAME = "org.gradle.integtest.native.toolChains";

    public SingleToolChainTestRunner(Class<? extends AbstractInstalledToolChainIntegrationSpec> target) {
        super(target, "all".equals(System.getProperty(TOOLCHAINS_SYSPROP_NAME, "default")));
    }

    @Override
    protected void createExecutions() {
        List<AvailableToolChains.ToolChainCandidate> toolChains = AvailableToolChains.getToolChains();

        for (AvailableToolChains.ToolChainCandidate toolChain : toolChains) {
            if (!toolChain.isAvailable()) {
                throw new RuntimeException(String.format("Tool chain %s is not available.", toolChain.getDisplayName()));
            }
            if (canUseToolChain(toolChain)) {
                add(new ToolChainExecution(toolChain));
            }
        }
    }

    // TODO: This exists because we detect all available native tool chains on a system (clang, gcc, swiftc, msvc)
    // Many of our old tests assume that available tool chains can compile many/most languages, so they do not try to
    // restrict the required set of tool chains.
    // The swiftc tool chain can build _only_ Swift, so tests must opt-in to see the swiftc tool chain.
    // Our multi-test runner is smart enough to disable tests that do not meet our requirements, but since many
    // of the old tests do not have requirements, we need to remove tool chains that do not meet the test classes
    // requirements (if there are any) early.
    // In the future... we want to either split apart the swiftc tool chain into a separate test runner/tool chain
    // detector or go back to old tests and annotate them with tool chains requirements (like NOT_SWIFTC) or
    // something more involved like declaring which features are needed for the test (I need a tool chain for
    // C and a runtime like such-and-such).
    private boolean canUseToolChain(AvailableToolChains.ToolChainCandidate toolChain) {
        if (toolChain.meets(ToolChainRequirement.SWIFTC)) {
            RequiresInstalledToolChain toolChainRequirement = target.getAnnotation(RequiresInstalledToolChain.class);
            return toolChainRequirement!=null && toolChain.meets(toolChainRequirement.value());
        }
        return true;
    }

    private static class ToolChainExecution extends Execution {
        private final AvailableToolChains.ToolChainCandidate toolChain;

        public ToolChainExecution(AvailableToolChains.ToolChainCandidate toolChain) {
            this.toolChain = toolChain;
        }

        @Override
        protected String getDisplayName() {
            return toolChain.getDisplayName();
        }

        @Override
        protected boolean isTestEnabled(TestDetails testDetails) {
            RequiresInstalledToolChain toolChainRestriction = testDetails.getAnnotation(RequiresInstalledToolChain.class);
            return toolChainRestriction == null || toolChain.meets(toolChainRestriction.value());
        }

        @Override
        protected void assertCanExecute() {
            assert toolChain.isAvailable() : String.format("Tool chain %s not available", toolChain.getDisplayName());
        }

        @Override
        protected void before() {
            System.out.println(String.format("Using tool chain %s", toolChain.getDisplayName()));
            AbstractInstalledToolChainIntegrationSpec.setToolChain((AvailableToolChains.InstalledToolChain) toolChain);
        }
    }
}

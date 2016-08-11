/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.composite.internal;

import org.gradle.StartParameter;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.initialization.BuildRequestContext;
import org.gradle.initialization.GradleLauncher;
import org.gradle.initialization.GradleLauncherFactory;
import org.gradle.initialization.IncludedBuildFactory;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;

import java.io.File;

public class DefaultIncludedBuildFactory implements IncludedBuildFactory {
    private final Instantiator instantiator;
    private final StartParameter startParameter;
    private final GradleLauncherFactory gradleLauncherFactory;
    private final ServiceRegistry sharedServices;

    public DefaultIncludedBuildFactory(Instantiator instantiator, StartParameter startParameter,
                                       GradleLauncherFactory gradleLauncherFactory, ServiceRegistry sharedServices) {
        this.instantiator = instantiator;
        this.startParameter = startParameter;
        this.gradleLauncherFactory = gradleLauncherFactory;
        this.sharedServices = sharedServices;
    }

    @Override
    public IncludedBuild createBuild(File buildDirectory, BuildRequestContext requestContext) {
        GradleLauncher launcher = createGradleLauncher(buildDirectory, requestContext);
        return instantiator.newInstance(DefaultIncludedBuild.class, buildDirectory, launcher);
    }

    @Override
    public IncludedBuild createBuild(File buildDirectory) {
        return createBuild(buildDirectory, null);
    }

    private StartParameter createStartParameter(File buildDirectory) {
        StartParameter includedBuildStartParam = startParameter.newBuild();
        includedBuildStartParam.setProjectDir(buildDirectory);
        includedBuildStartParam.setSearchUpwards(false);
        includedBuildStartParam.setConfigureOnDemand(false);
        return includedBuildStartParam;
    }

    private GradleLauncher createGradleLauncher(File buildDirectory, BuildRequestContext requestContext) {
        StartParameter participantStartParam = createStartParameter(buildDirectory);
        if (requestContext == null) {
            return gradleLauncherFactory.nestedInstance(participantStartParam, sharedServices);
        }

        GradleLauncher gradleLauncher = gradleLauncherFactory.newInstance(participantStartParam, requestContext, sharedServices);
        gradleLauncher.addStandardOutputListener(requestContext.getOutputListener());
        gradleLauncher.addStandardErrorListener(requestContext.getErrorListener());
        return gradleLauncher;
    }
}

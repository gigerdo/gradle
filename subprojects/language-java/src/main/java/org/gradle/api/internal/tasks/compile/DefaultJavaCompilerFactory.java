/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.api.internal.tasks.compile;

import org.gradle.api.internal.ClassPathRegistry;
import org.gradle.api.internal.tasks.compile.processing.AnnotationProcessorDetector;
import org.gradle.internal.Factory;
import org.gradle.language.base.internal.compile.CompileSpec;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.process.internal.ExecHandleFactory;
import org.gradle.process.internal.JavaForkOptionsFactory;
import org.gradle.process.internal.worker.child.WorkerDirectoryProvider;
import org.gradle.workers.internal.WorkerDaemonFactory;

import javax.tools.JavaCompiler;

public class DefaultJavaCompilerFactory implements JavaCompilerFactory {
    private final WorkerDirectoryProvider workingDirProvider;
    private final WorkerDaemonFactory workerDaemonFactory;
    private final Factory<JavaCompiler> javaHomeBasedJavaCompilerFactory;
    private final JavaForkOptionsFactory forkOptionsFactory;
    private final ExecHandleFactory execHandleFactory;
    private final AnnotationProcessorDetector processorDetector;
    private final ClassPathRegistry classPathRegistry;

    public DefaultJavaCompilerFactory(WorkerDirectoryProvider workingDirProvider, WorkerDaemonFactory workerDaemonFactory, Factory<JavaCompiler> javaHomeBasedJavaCompilerFactory, JavaForkOptionsFactory forkOptionsFactory, ExecHandleFactory execHandleFactory, AnnotationProcessorDetector processorDetector, ClassPathRegistry classPathRegistry) {
        this.workingDirProvider = workingDirProvider;
        this.workerDaemonFactory = workerDaemonFactory;
        this.javaHomeBasedJavaCompilerFactory = javaHomeBasedJavaCompilerFactory;
        this.forkOptionsFactory = forkOptionsFactory;
        this.execHandleFactory = execHandleFactory;
        this.processorDetector = processorDetector;
        this.classPathRegistry = classPathRegistry;
    }

    @Override
    public Compiler<JavaCompileSpec> createForJointCompilation(Class<? extends CompileSpec> type) {
        return createTargetCompiler(type, true);
    }

    @Override
    public Compiler<JavaCompileSpec> create(Class<? extends CompileSpec> type) {
        Compiler<JavaCompileSpec> result = createTargetCompiler(type, false);
        return new AnnotationProcessorDiscoveringCompiler<JavaCompileSpec>(new NormalizingJavaCompiler(result), processorDetector);
    }

    private Compiler<JavaCompileSpec> createTargetCompiler(Class<? extends CompileSpec> type, boolean jointCompilation) {
        if (!JavaCompileSpec.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(String.format("Cannot create a compiler for a spec with type %s", type.getSimpleName()));
        }

        if (CommandLineJavaCompileSpec.class.isAssignableFrom(type)) {
            return new CommandLineJavaCompiler(execHandleFactory);
        }

        if (ForkingJavaCompileSpec.class.isAssignableFrom(type) && !jointCompilation) {
            return new DaemonJavaCompiler(workingDirProvider.getWorkingDirectory(), JdkJavaCompiler.class, new Object[] {javaHomeBasedJavaCompilerFactory}, workerDaemonFactory, forkOptionsFactory, classPathRegistry);
        } else {
            return new JdkJavaCompiler(javaHomeBasedJavaCompilerFactory);
        }
    }
}

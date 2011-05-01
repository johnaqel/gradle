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

package org.gradle.plugins.ide.idea.model

import org.gradle.api.artifacts.Configuration
import org.gradle.plugins.ide.idea.model.internal.IdeaDependenciesProvider
import org.gradle.util.ConfigureUtil
import org.gradle.api.dsl.ConventionProperty

/**
 * Enables fine-tuning module details (*.iml file) of the Idea plugin
 * <p>
 * Example of use with a blend of all possible properties.
 * Bear in mind that usually you don't have configure this model directly because Gradle configures it for free!
 *
 * <pre autoTested=''>
 * apply plugin: 'java'
 * apply plugin: 'idea'
 *
 * //for the sake of the example lets have a 'provided' dependency configuration
 * configurations {
 *   provided
 *   provided.extendsFrom(compile)
 * }
 *
 * dependencies {
 *   //provided "some.interesting:dependency:1.0"
 * }
 *
 * idea {
 *   module {
 *     //if for some reason you want to add an extra sourceDirs
 *     sourceDirs += file('some-extra-source-folder')
 *
 *     //and some extra test source dirs
 *     testSourceDirs += file('some-extra-test-dir')
 *
 *     //and some extra dirs that should be excluded by IDEA
 *     excludeDirs += file('some-extra-exclude-dir')
 *
 *     //if you don't like the name Gradle have chosen
 *     name = 'some-better-name'
 *
 *     //if you prefer different output folders
 *     inheritOutputDirs = false
 *     outputDir = file('muchBetterOutputDir')
 *     testOutputDir = file('muchBetterTestOutputDir')
 *
 *     //if you prefer different java version than inherited from IDEA project
 *     javaVersion = '1.6'
 *
 *     //if you need to put provided dependencies on the classpath
 *     scopes.PROVIDED.plus += configurations.provided
 *
 *     //if 'content root' (as IDEA calls it) of the module is different
 *     contentRoot = file('my-module-content-root')
 *
 *     //if you love browsing javadocs
 *     downloadJavadoc = true
 *
 *     //and hate reading sources :)
 *     downloadSources = false
 *
 *     //if you want parts of paths in resulting *.iml to be replaced by variables (Files)
 *     pathVariables = [GRADLE_HOME: file('~/cool-software/gradle')]
 *   }
 * }
 * </pre>
 *
 * For tackling edge cases users can perform advanced configuration on resulting xml file.
 * It is also possible to affect the way idea plugin merges the existing configuration
 * via beforeMerged and whenMerged closures.
 * <p>
 * beforeMerged and whenMerged closures receive {@link Module} object
 * <p>
 * Examples of advanced configuration:
 *
 * <pre autoTested=''>
 * apply plugin: 'java'
 * apply plugin: 'idea'
 *
 * idea {
 *   module {
 *     iml {
 *       //if you like to keep *.iml in a secret folder
 *       generateTo = file('secret-modules-folder')
 *
 *       //if you want to mess with the resulting xml in whatever way you fancy
 *       withXml {
 *         def node = it.asNode()
 *         node.appendNode('iLoveGradle', 'true')
 *         node.appendNode('butAlso', 'I find increasing pleasure tinkering with output *.iml contents. Yeah!!!')
 *       }
 *
 *       //closure executed after *.iml content is loaded from existing file
 *       //but before gradle build information is merged
 *       beforeMerged { module ->
 *         //if you want skip merging exclude dirs
 *         module.excludeFolders.clear()
 *       }
 *
 *       //closure executed after *.iml content is loaded from existing file
 *       //and after gradle build information is merged
 *       whenMerged { module ->
 *         //you can tinker with {@link Module}
 *       }
 *     }
 *   }
 * }
 *
 * </pre>
 *
 * @author Szczepan Faber, created at: 3/31/11
 */
class IdeaModule {

   /**
     * Configures module name, that is the name of the *.iml file.
     * <p>
     * It's <b>optional</b> because the task should configure it correctly for you.
     * By default it will try to use the <b>project.name</b> or prefix it with a part of a <b>project.path</b>
     * to make sure the module name is unique in the scope of a multi-module build.
     * The 'uniqeness' of a module name is required for correct import
     * into IntelliJ IDEA and the task will make sure the name is unique.
     * <p>
     * <b>since</b> 1.0-milestone-2
     * <p>
     * If your project has problems with unique names it is recommended to always run gradle idea from the root, e.g. for all subprojects.
     * If you run the generation of the idea module only for a single subproject then you may have different results
     * because the unique names are calculated based on idea modules that are involved in the specific build run.
     * <p>
     * If you update the module names then make sure you run gradle idea from the root, e.g. for all subprojects, including generation of idea project.
     * The reason is that there may be subprojects that depend on the subproject with amended module name.
     * So you want them to be generated as well because the module dependencies need to refer to the amended project name.
     * Basically, for non-trivial projects it is recommended to always run gradle idea from the root.
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    String name

    /**
     * The directories containing the production sources.
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    Set<File> sourceDirs

    /**
     * The keys of this map are the IDEA scopes. Each key points to another map that has two keys, plus and minus.
     * The values of those keys are collections of {@link org.gradle.api.artifacts.Configuration} objects. The files of the
     * plus configurations are added minus the files from the minus configurations. See example below...
     * <p>
     * Example how to use scopes property to enable 'provided' dependencies in the output *.iml file:
     * <pre autoTested=''>
     * apply plugin: 'java'
     * apply plugin: 'idea'
     *
     * configurations {
     *   provided
     *   provided.extendsFrom(compile)
     * }
     *
     * dependencies {
     *   //provided "some.interesting:dependency:1.0"
     * }
     *
     * idea {
     *   module {
     *     scopes.PROVIDED.plus += configurations.provided
     *   }
     * }
     * </pre>
     */
    Map<String, Map<String, Collection<Configuration>>> scopes = [:]

    /**
     * Whether to download and add sources associated with the dependency jars.
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    boolean downloadSources = true

    /**
     * Whether to download and add javadoc associated with the dependency jars.
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    boolean downloadJavadoc = false

    /**
     * The content root directory of the module.
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    File contentRoot

    /**
     * The directories containing the test sources.
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    Set<File> testSourceDirs

    /**
     * {@link ConventionProperty} for the directories to be excluded.
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    Set<File> excludeDirs

    /**
     * If true, output directories for this module will be located below the output directory for the project;
     * otherwise, they will be set to the directories specified by {@link #outputDir} and {@link #testOutputDir}.
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    Boolean inheritOutputDirs

    /**
     * The output directory for production classes. If {@code null}, no entry will be created.
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    File outputDir

    /**
     * The output directory for test classes. If {@code null}, no entry will be created.
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    File testOutputDir

    /**
     * The variables to be used for replacing absolute paths in the iml entries. For example, you might add a
     * {@code GRADLE_USER_HOME} variable to point to the Gradle user home dir.
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    Map<String, File> pathVariables = [:]

    /**
     * The JDK to use for this module. If {@code null}, the value of the existing or default ipr XML (inherited)
     * is used. If it is set to <code>inherited</code>, the project SDK is used. Otherwise the SDK for the corresponding
     * value of java version is used for this module
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    String javaVersion = Module.INHERITED

    /**
     * Enables advanced configuration like tinkering with the output xml
     * or affecting the way existing *.iml content is merged with gradle build information
     * <p>
     * For example see docs for {@link IdeaModule}
     */
    void iml(Closure closure) {
        ConfigureUtil.configure(closure, getIml())
    }

    //TODO SF: most likely what's above should be a part of an interface and what's below should not be exposed.
    //For now, below methods are protected - same applies to new model

    org.gradle.api.Project project
    PathFactory pathFactory
    IdeaModuleIml iml

    File getOutputFile() {
        new File((File) iml.getGenerateTo(), getName() + ".iml")
    }

    void setOutputFile(File newOutputFile) {
        name = newOutputFile.name.replaceFirst(/\.iml$/,"");
        iml.generateTo = newOutputFile.parentFile
    }

    Set<Path> getSourcePaths() {
        getSourceDirs().findAll { it.exists() }.collect { path(it) }
    }

    Set<Dependency> getDependencies() {
        new IdeaDependenciesProvider().provide(this, getPathFactory());
    }

    Set<Path> getTestSourcePaths() {
        getTestSourceDirs().findAll { it.exists() }.collect { getPathFactory().path(it) }
    }

    Set<Path> getExcludePaths() {
        getExcludeDirs().collect { path(it) }
    }

    Path getOutputPath() {
        getOutputDir() ? path(getOutputDir()) : null
    }

    Path getTestOutputPath() {
        getTestOutputDir() ? path(getTestOutputDir()) : null
    }

    void mergeXmlModule(Module xmlModule) {
        iml.beforeMerged.execute(xmlModule)
        xmlModule.configure(getContentPath(), getSourcePaths(), getTestSourcePaths(), getExcludePaths(),
                getInheritOutputDirs(), getOutputPath(), getTestOutputPath(), getDependencies(), getJavaVersion())
        iml.whenMerged.execute(xmlModule)
    }

    Path getContentPath() {
        path(getContentRoot())
    }

    Path path(File dir) {
        getPathFactory().path(dir)
    }
}

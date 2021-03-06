package net.klakegg.plugin;

import no.difi.commons.schematron.SchematronCompiler;
import no.difi.commons.schematron.SchematronException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author erlend
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class CompileMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/schematron", property = "sourceDir", readonly = true)
    private File sourceDirectory;

    @Parameter(defaultValue = "${project.build.directory}/classes", property = "outputDir", required = true)
    private File outputDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!sourceDirectory.exists()) {
            getLog().info(String.format(
                    "Unable to find folder '%s', skips compilation of Schematron files.", sourceDirectory));
            return;
        }

        try {
            final SchematronCompiler schematronCompiler = new SchematronCompiler();

            Files.walkFileTree(sourceDirectory.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        getLog().info(String.format("Compiling '%s'.", file));

                        String filename = file.toString()
                                .substring(sourceDirectory.toString().length() + 1)
                                .replace(".sch", ".xsl");

                        Path target = outputDirectory.toPath().resolve(filename);
                        Files.createDirectories(target.getParent());

                        schematronCompiler.compile(file, target);

                        return FileVisitResult.CONTINUE;
                    } catch (SchematronException e) {
                        throw new IOException(e.getMessage(), e);
                    }
                }
            });
        } catch (SchematronException | IOException e) {
            getLog().error(e.getMessage(), e);
            throw new MojoFailureException(e.getMessage());
        }
    }
}
package io.mdsl.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.generator.GeneratorContext;
import org.eclipse.xtext.generator.JavaIoFileSystemAccess;
import org.junit.jupiter.api.Test;

import io.mdsl.tests.AbstractMDSLInputIntegrationTest;

/**
 * 
 * Test for the OpenAPI Generator SLA Extension
 * 
 * @author Pascal Schur
 *
 */
public class OpenAPIGeneratorSLAExtensionTest extends AbstractMDSLInputIntegrationTest {

	
	@Test
	public void canGenerateSLATemplateFileValuePair() throws IOException {
		assertThatInputFileGeneratesExpectedOutput("sla-simple-template");
	}
	
	@Test
	public void canGenerateEndpointSLAWithHTTPBinding() throws IOException {
		assertThatInputFileGeneratesExpectedOutput("endpoint-sla-http-binding");
	}
	
	@Test
	public void canGenerateEndpointSLAWithOtherBinding() throws IOException {
		assertThatInputFileGeneratesExpectedOutput("endpoint-sla-other-binding");
	}
	
	@Test
	public void canGenerateProviderSLAWithOtherBinding() throws IOException {
		assertThatInputFileGeneratesExpectedOutput("provider-sla-http-binding");
	}
	
	@Test
	public void canGenerateProviderSLAWithHTTPBinding() throws IOException {
		assertThatInputFileGeneratesExpectedOutput("provider-sla-other-binding");
	}
	
	@Test
	public void canGenerateSLATemplateFile() throws IOException {
		assertThatInputFileGeneratesExpectedOutputSLA("sla-extension-file");
	}
	
	@Test
	public void canGenerateInternalProivderSLA() throws IOException {
		assertThatInputFileGeneratesExpectedOutput("provider-sla-internal-http-binding");
	}
	
	@Test
	public void canGenerateInternalEndpointSLA() throws IOException {
		assertThatInputFileGeneratesExpectedOutput("endpoint.sla-internal-other-binding");
	}
	
	@Test
	public void canGenerateProviderAndEndpointSLAHTTPBinding () throws IOException {
		assertThatInputFileGeneratesExpectedOutput("provider-endpoint-sla-http-binding");
	}
	
	@Test
	public void canGenerateProviderAndEndpointSLAOtherBinding () throws IOException {
		assertThatInputFileGeneratesExpectedOutput("provider-endpoint-sla-other-binding");
	}
	
	
	/**
	 * Allows testing whether a test input file ({baseFilename}.mdsl) leads to the
	 * expected output ({baseFilename}.yaml).
	 */
	protected void assertThatInputFileGeneratesExpectedOutput(String baseFilename) throws IOException {

		// given
		Resource inputModel = getTestResource(baseFilename + ".mdsl");
		OpenAPIGenerator generator = new OpenAPIGenerator();

		// when
		JavaIoFileSystemAccess javaIoFileSystemAccess = getFileSystemAccess();
		javaIoFileSystemAccess.setOutputPath(getGenerationDirectory().getAbsolutePath());
		generator.doGenerate(inputModel, javaIoFileSystemAccess, new GeneratorContext());

		// then
		assertTrue(generator.getValidationMessages().isEmpty(), "OAS validation reports errors");
		assertEquals(getExpectedTestResult(baseFilename + ".yaml"), getGeneratedFileContent(baseFilename + ".yaml"));

	}
	
	/**
	 * Allows testing whether a test input file ({baseFilename}.mdsl) leads to the
	 * expected output ({baseFilename}-sla.yaml).
	 */
	protected void assertThatInputFileGeneratesExpectedOutputSLA(String baseFilename) throws IOException {

		// given
		Resource inputModel = getTestResource(baseFilename + ".mdsl");
		OpenAPIGenerator generator = new OpenAPIGenerator();

		// when
		JavaIoFileSystemAccess javaIoFileSystemAccess = getFileSystemAccess();
		javaIoFileSystemAccess.setOutputPath(getGenerationDirectory().getAbsolutePath());
		generator.doGenerate(inputModel, javaIoFileSystemAccess, new GeneratorContext());

		// then
		assertTrue(generator.getValidationMessages().isEmpty(), "OAS validation reports errors");
		assertEquals(getExpectedTestResult(baseFilename + "-sla.yaml"), getGeneratedFileContent(baseFilename + "-sla.yaml"));

	}

	protected String getExpectedTestResult(String fileName) throws IOException {
		return FileUtils.readFileToString(getTestInputFile(fileName), "UTF-8");
	}

	@Override
	protected String getGeneratedFileContent(String fileName) throws IOException {
		String generatedFileContent = FileUtils.readFileToString(new File(getGenerationDirectory(), fileName), "UTF-8");
		// remove the x-generated-on and its timestamp, along with the newline
		return generatedFileContent.replaceFirst("  x-generated-on: .*?(\\r?\\n|\\r)", "");
	}

	@Override
	protected String testDirectory() {
		return "/test-data/openapi-sla-extension-generation/";
	}

}

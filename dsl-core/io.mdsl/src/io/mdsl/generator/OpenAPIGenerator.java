package io.mdsl.generator;

import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.generator.IFileSystemAccess2;

import com.google.common.collect.Sets;

import io.mdsl.apiDescription.ServiceSpecification;
import io.mdsl.generator.openapi.converter.MDSL2OpenAPIConverter;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

/**
 * Generates an OpenAPI specification with an MDSL model as input.
 * 
 * @author ska, socadk
 *
 */
public class OpenAPIGenerator extends AbstractMDSLGenerator {

	private Set<String> validationMessages;

	@Override
	protected void generateFromServiceSpecification(ServiceSpecification mdslSpecification, IFileSystemAccess2 fsa,
			URI inputFileURI) {
		String fileName = inputFileURI.trimFileExtension().lastSegment() + ".yaml";
		validationMessages = Sets.newHashSet();

		// convert MDSL to OpenAPI model:
		OpenAPI oas = new MDSL2OpenAPIConverter(mdslSpecification).convert();
		
		 //check of specification contains sla and generate extension file 
		if(!mdslSpecification.getSlas().isEmpty()) {
			OpenAPISLAGenerator slaGen = new OpenAPISLAGenerator(mdslSpecification.getSlas(),inputFileURI); 
			
			String slaYaml = slaGen.generate();
			
			// generate output file
			fsa.generateFile(slaGen.getSlaOutputFileName(), slaYaml);
			
			// set ref to generate sla file in oas file 
			oas.setInfo(slaGen.setSLAPath(oas.getInfo()));
			
			
		}

		// serialize model as YAML file:
		String yaml = Yaml.pretty(oas);

		// validate the generated model:
		SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(yaml);
		validationMessages.addAll(parseResult.getMessages());

		// generate output file
		fsa.generateFile(fileName, yaml);
	}

	/**
	 * Allows clients to get the validation/error messages of the generated OpenAPI
	 * contract. Should ideally be empty.
	 */
	public Set<String> getValidationMessages() {
		return validationMessages;
	}

}

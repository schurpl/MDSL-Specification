package io.mdsl.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;

import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;

import io.mdsl.apiDescription.InternalSLA;
import io.mdsl.apiDescription.LandingZone;
import io.mdsl.apiDescription.RateLimit;
import io.mdsl.apiDescription.SLATemplate;
import io.mdsl.apiDescription.SLO;
import io.mdsl.apiDescription.SimpleMeasurement;
import io.swagger.v3.oas.models.info.Info;

import io.swagger.v3.core.util.Yaml;

public class OpenAPISLAGenerator {

	private final String SLA_EXTIONSION = "x-external-sla-file";
	private final String REF_KEYWORD = "$ref";
	private final String SLA_FILENAME_EXTENSION = "-sla.yaml";
	private final String DOC_VERSION_NAME = "sla-doc-version";
	private final String SLA_DOC_Version = "1.0";
	private final String SLA_NODE_NAME = "sla-templates";

	private final EList<SLATemplate> sla;
	private final String slaOutputFileName;
	private final SLAConverter slaConverter;

	/**
	 * This class is used to generate an Extension FIle for OpenAPI regarding SLAs
	 * 
	 * @param sla          contains the SLA Specification for which the SLA YAAML
	 *                     file is generated
	 * @param inputFileURI URI Object of the input File used to generate
	 *                     outputFileName
	 */
	public OpenAPISLAGenerator(EList<SLATemplate> sla, URI inputFileURI) {
		this.sla = sla;
		this.slaOutputFileName = inputFileURI.trimFileExtension().lastSegment() + SLA_FILENAME_EXTENSION;
		this.slaConverter = new SLAConverter();
	}

	public String getSlaOutputFileName() {
		return slaOutputFileName;
	}

	/**
	 * This method is used to set the sla extension parameter in the Info Object of
	 * the OpenAPI Specification
	 * 
	 * @param info the info Object of the OpenAPI Specification generated
	 * @return the extended info object
	 */
	public Info setSLAPath(Info info) {
		Map<String, Object> extensions = new LinkedHashMap<String, Object>(info.getExtensions());
		String relatveFileName = "." + File.separator + slaOutputFileName;
		extensions.put(SLA_EXTIONSION, Map.of(REF_KEYWORD, relatveFileName));
		info.setExtensions(extensions);
		return info;

	}

	/**
	 * Generates an YAML document containing the SLA description
	 * 
	 * @return serialized YAML String
	 */
	public String generate() {
		Map<String, Object> slaMap = new LinkedHashMap<>();
		slaMap.put(DOC_VERSION_NAME, SLA_DOC_Version);
		ArrayList<Map<String, Object>> slaTemplates = new ArrayList();
		for (SLATemplate slaTemplate : sla) {
			slaTemplates.add(slaConverter.generateSLATemplate(slaTemplate));
		}
		slaMap.put(SLA_NODE_NAME, slaTemplates);

		return Yaml.pretty(slaMap);

	}

	

}

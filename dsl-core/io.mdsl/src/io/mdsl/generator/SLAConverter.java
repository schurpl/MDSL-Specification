package io.mdsl.generator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;

import io.mdsl.apiDescription.InternalSLA;
import io.mdsl.apiDescription.LandingZone;
import io.mdsl.apiDescription.RateLimit;
import io.mdsl.apiDescription.SLATemplate;
import io.mdsl.apiDescription.SLO;
import io.mdsl.apiDescription.SimpleMeasurement;

/**
 * 
 * This class provides a Converter for the SLA Template Object and all nested Objects provided by MDSL 
 * 
 * Note Optional Attributes will be omitted if they do not provide a standard value. If they provide
 * a standard value this value will be displayed  
 * 
 * @author Pascal Schur
 *
 */
public class SLAConverter {
	
	
	/**
	 * Convert a SLATemplate Object to a Map 
	 * 
	 * @param slaTemplate Object
	 * @return Map consisting of key value pairs each pair represents one attribute 
	 */
	public Map<String, Object> convertSLATemplate(SLATemplate slaTemplate) {
		Map<String, Object> slaTemplateMap = new LinkedHashMap<>();
		slaTemplateMap.put("name", slaTemplate.getName());
		slaTemplateMap.put("slas", convertInternalSlas(slaTemplate.getSlas()));
		return slaTemplateMap;
	}

	/**
	 * Convert internalSLA Object to List of Maps of Key Value pairs
	 * 
	 * @param internalSlas List which contains all internalSLA Objects
	 * @return List of Maps which contains Maps each Key Value pair represents one Attribute optional Attributes are omitted
	 */
	private List<Map<String, Object>> convertInternalSlas(EList<InternalSLA> internalSlas) {
		List<Map<String, Object>> internalSlasList = new ArrayList<>();
		for (InternalSLA internalSla : internalSlas) {
			internalSlasList.add(convertInternalSla(internalSla));
		}
		return internalSlasList;
	}

	/**
	 * 
	 * Operations takes one InternalSLA Object and converts it to a Map of Key Value pairs representing the Object
	 * 
	 * @param internalSla Object  
	 * @return Map of String Object pairs each pair represents one Attribute
	 */
	public Map<String, Object> convertInternalSla(InternalSLA internalSla) {
		Map<String, Object> internalSlaMap = new LinkedHashMap<>();

		internalSlaMap.put("type", internalSla.getType());

		internalSlaMap.put("slos", convertSlos(internalSla.getSlos()));

		if (internalSla.getPenalties() != null) {
			internalSlaMap.put("penalty", internalSla.getPenalties());
		}
		if (internalSla.getReporting() != null) {
			internalSlaMap.put("notification", internalSla.getReporting());
		}
		// generate Rate plan
		if (internalSla.getRp() != null) {
			internalSlaMap.put("ratePlan", internalSla.getRp().replace("rate plan", "").trim());
		}
		// generate Rate limit
		if (!internalSla.getRl().isEmpty()) {
			internalSlaMap.put("rate limits", convertRateLimits(internalSla.getRl()));
		}
		return internalSlaMap;
	}

	/**
	 * 
	 * Operations takes a List of Rate Limit Objects and returns a list of Maps each Map represents the coressponding Rate Limit Object 
	 * 
	 * @param rl EList which contains 
	 * @return Map of String Object pairs each pair represents one Attribute
	 */
	private List<Map<String, Object>> convertRateLimits(EList<RateLimit> rl) {
		List<Map<String, Object>> rateLimitsList = new ArrayList<>();
		for (RateLimit rateLimit : rl) {
			rateLimitsList.add(convertRateLimit(rateLimit));
		}
		return rateLimitsList;
	}

	/**
	 * Converts a single Rate Limit Object into a Map of Key Value pairs each represents a Attribute of the Rate Limit Object 
	 * 
	 * @param rateLimit Object
	 * @return Map consisting of Key Value pairs each pair represents one Attribute
	 */
	private Map<String, Object> convertRateLimit(RateLimit rateLimit) {
		Map<String, Object> rateLimitMap = new LinkedHashMap<>();
		if (rateLimit.getCallRate() != null) {
			rateLimitMap.put("rateLimit", "MAX_CALLS");
			rateLimitMap.put("measurement", convertSimpleMeasurement(rateLimit.getCallRate()));
		} else if (rateLimit.getDataRate() != null) {
			rateLimitMap.put("rateLimit", "DATA_QUOTA");
			rateLimitMap.put("measurement", convertSimpleMeasurement(rateLimit.getDataRate()));
		} else {
			rateLimitMap.put("type", "NONE");
		}
		rateLimitMap.put("interval", convertSimpleMeasurement(rateLimit.getInterval()));
		return rateLimitMap;
	}
	
	/**
	 * Converts a List of SLO Objects into a List of Maps 
	 * 
	 * @param slos List containing SLO Objects
	 * @return List of Maps each Map represents one SLO element in the List
	 */
	private List<Map<String, Object>> convertSlos(EList<SLO> slos) {
		List<Map<String, Object>> sloList = new ArrayList<>();
		for (SLO slo : slos) {
			sloList.add(convertSlo(slo));
		}
		return sloList;
	}
	
	/**
	 * Converts a single SLO object into a Map
	 * 
	 * @param slo Object
	 * @return Map of Key Value pairs each representing an attribute of the SLO object
	 */
	private Map<String, Object> convertSlo(SLO slo) {
		Map<String, Object> sloMap = new LinkedHashMap<>();
		sloMap.put("name", slo.getName());
		sloMap.put("qualityGoal", slo.getQualityGoal());
		if (slo.getMeasurement().getLz() != null) {
			sloMap.put("measurement", convertLandingZone(slo.getMeasurement().getLz()));
		} else {
			sloMap.put("measurement", convertSimpleMeasurement(slo.getMeasurement().getSm()));
		}
		return sloMap;
	}
	/**
	 * Converts a simple measurement Object into a Map
	 * 
	 * @param sm Object
	 * @return Map <String, Object> 
	 */
	private Map<String, Object> convertSimpleMeasurement(SimpleMeasurement sm) {
		Map<String, Object> simpleMeasurementMap = new LinkedHashMap<>();
		simpleMeasurementMap.put("value", sm.getValue());
		if(sm.getUnitOfMeasure() != null) {
			simpleMeasurementMap.put("unit", sm.getUnitOfMeasure());
		} 
		return simpleMeasurementMap;
	}
	
	/**
	 * Converts a Landing Zone Object into a Map of Key Value pairs
	 * 
	 * @param lz Object
	 * @return A Map representing the Landing Zone Object
	 */
	private Map<String, Object> convertLandingZone(LandingZone lz) {
		Map<String, Object> landingZoneMap = new LinkedHashMap<>();
		landingZoneMap.put("minimal", convertSimpleMeasurement(lz.getS()));
		landingZoneMap.put("target", convertSimpleMeasurement(lz.getT()));
		if (lz.getO() != null) {
			landingZoneMap.put("optional", convertSimpleMeasurement(lz.getO()));
		}
		return landingZoneMap;
	}
}

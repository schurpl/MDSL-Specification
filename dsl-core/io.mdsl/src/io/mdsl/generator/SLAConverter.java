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

public class SLAConverter {
	Map<String, Object> generateSLATemplate(SLATemplate slaTemplate) {
		Map<String, Object> slaTemplateMap = new LinkedHashMap<>();
		slaTemplateMap.put("name", slaTemplate.getName());
		slaTemplateMap.put("slas", generateInternalSlas(slaTemplate.getSlas()));
		return slaTemplateMap;
	}

	private List<Map<String, Object>> generateInternalSlas(EList<InternalSLA> internalSlas) {
		List<Map<String, Object>> internalSlasList = new ArrayList<>();
		for (InternalSLA internalSla : internalSlas) {
			internalSlasList.add(generateInternalSla(internalSla));
		}
		return internalSlasList;
	}

	public Map<String, Object> generateInternalSla(InternalSLA internalSla) {
		Map<String, Object> internalSlaMap = new LinkedHashMap<>();

		internalSlaMap.put("type", internalSla.getType());

		internalSlaMap.put("slos", generateSlos(internalSla.getSlos()));

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
			internalSlaMap.put("rate limits", generateRateLimits(internalSla.getRl()));
		}
		return internalSlaMap;
	}

	private List<Map<String, Object>> generateRateLimits(EList<RateLimit> rl) {
		List<Map<String, Object>> rateLimitsList = new ArrayList<>();
		for (RateLimit rateLimit : rl) {
			rateLimitsList.add(generateRateLimit(rateLimit));
		}
		return rateLimitsList;
	}

	private Map<String, Object> generateRateLimit(RateLimit rateLimit) {
		Map<String, Object> rateLimitMap = new LinkedHashMap<>();
		if (rateLimit.getCallRate() != null) {
			rateLimitMap.put("rateLimit", "MAX_CALLS");
			rateLimitMap.put("measurement", generateSimpleMeasurement(rateLimit.getCallRate()));
		} else if (rateLimit.getDataRate() != null) {
			rateLimitMap.put("rateLimit", "DATA_QUOTA");
			rateLimitMap.put("measurement", generateSimpleMeasurement(rateLimit.getDataRate()));
		} else {
			rateLimitMap.put("type", "NONE");
		}
		rateLimitMap.put("interval", generateSimpleMeasurement(rateLimit.getInterval()));
		return rateLimitMap;
	}

	private List<Map<String, Object>> generateSlos(EList<SLO> slos) {
		List<Map<String, Object>> sloList = new ArrayList<>();
		for (SLO slo : slos) {
			sloList.add(generateSLo(slo));
		}
		return sloList;
	}

	private Map<String, Object> generateSLo(SLO slo) {
		Map<String, Object> sloMap = new LinkedHashMap<>();
		sloMap.put("name", slo.getName());
		sloMap.put("qualityGoal", slo.getQualityGoal());
		if (slo.getMeasurement().getLz() != null) {
			sloMap.put("measurement", generateLandingZone(slo.getMeasurement().getLz()));
		} else {
			sloMap.put("measurement", generateSimpleMeasurement(slo.getMeasurement().getSm()));
		}
		return sloMap;
	}

	private Map<String, Object> generateSimpleMeasurement(SimpleMeasurement sm) {
		Map<String, Object> simpleMeasurementMap = new LinkedHashMap<>();
		simpleMeasurementMap.put("value", sm.getValue());
		if(sm.getUnitOfMeasure() != null) {
			simpleMeasurementMap.put("unit", sm.getUnitOfMeasure());
		} 
		return simpleMeasurementMap;
	}

	private Map<String, Object> generateLandingZone(LandingZone lz) {
		Map<String, Object> landingZoneMap = new LinkedHashMap<>();
		landingZoneMap.put("minimal", generateSimpleMeasurement(lz.getS()));
		landingZoneMap.put("target", generateSimpleMeasurement(lz.getT()));
		if (lz.getO() != null) {
			landingZoneMap.put("optional", generateSimpleMeasurement(lz.getO()));
		}
		return landingZoneMap;
	}
}

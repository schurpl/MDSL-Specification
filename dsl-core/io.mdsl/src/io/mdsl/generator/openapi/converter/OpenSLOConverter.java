package io.mdsl.generator.openapi.converter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;

import io.mdsl.apiDescription.AlertCondition;
import io.mdsl.apiDescription.AlertNotificationTarget;
import io.mdsl.apiDescription.AlertPolicy;
import io.mdsl.apiDescription.Condition;
import io.mdsl.apiDescription.DataSource;
import io.mdsl.apiDescription.Duration;
import io.mdsl.apiDescription.MetricSource;
import io.mdsl.apiDescription.OSLOTemplate;
import io.mdsl.apiDescription.Objective;
import io.mdsl.apiDescription.RatioMetric;
import io.mdsl.apiDescription.SLI;
import io.mdsl.apiDescription.Service;
import io.mdsl.apiDescription.ServiceSpecification;
import io.mdsl.apiDescription.ThresholdMetric;
import io.mdsl.apiDescription.TimeWindow;
import io.swagger.v3.core.util.Yaml;

/**
 * Class converts the OpenSLO template in a YAML formatted Sting
 * 
 * @author Pascal Schur
 *
 */
public class OpenSLOConverter {

	private ServiceSpecification mdslSpecification;
	private URI inputFileURI;

	public OpenSLOConverter(ServiceSpecification mdslSpecification, URI inputFileURI) {
		this.mdslSpecification = mdslSpecification;
		this.inputFileURI = inputFileURI;
	}

	/**
	 * Operation converts all OSLO Templates in the provided Service Specification
	 * into YAML Formatted Strings in YAML Format and returns them in a Map. The Map
	 * has Filenames as keys and the coresponding contents as values. This allows to
	 * get the map iterate over it and write all Open SLO Templates in the according
	 * files.
	 *
	 * @return Map<String, String> Map represents a mapping between filename
	 *         contents for the Open SLO YAML Files
	 */
	public Map<String, String> convert() {
		Map<String, String> docummentMap = new LinkedHashMap<String, String>();
		if (this.mdslSpecification.getOslos().isEmpty()) {
			throw new NullPointerException("No Open SLO template defined");
		}

		for (OSLOTemplate osloTemplate : this.mdslSpecification.getOslos()) {
			String osloString = new String();
			String sloString = toYAML(convertOSLOTemplate(osloTemplate), true);
			osloString = osloString.concat(sloString);
			// SLIs
			/// check if oslo has external sli then include external sli here
			if (osloTemplate.getExternalIndicator() != null) {
				Map<String, Object> sliMap = createBuiltInSLIMap(osloTemplate.getExternalIndicator());
				Map<String, Object> sliHeadMap = new LinkedHashMap<String, Object>();
				sliHeadMap.put("apiVersion", "openslo/v1");
				sliHeadMap.put("kind", "SLI");
				sliHeadMap.putAll(sliMap);
				String sliString = toYAML(sliHeadMap, false);
				osloString = osloString.concat(sliString);
			}

			// AlertPpolicies
			Set<AlertNotificationTarget> notificationTargets = new HashSet<AlertNotificationTarget>();
			if (!osloTemplate.getExternalAlertPol().isEmpty()) {
				for (AlertPolicy alertPolicy : osloTemplate.getExternalAlertPol()) {
					String alertPolString = toYAML(createAlertPolyMap(alertPolicy), false);
					osloString = osloString.concat(alertPolString);
					notificationTargets.addAll(alertPolicy.getNotificationTarget());
				}

			} else if (!osloTemplate.getBuiltInalertPol().isEmpty()) {
				for (AlertPolicy alertPolicy : osloTemplate.getBuiltInalertPol()) {
					String alertPolString = toYAML(createAlertPolyMap(alertPolicy), false);
					osloString = osloString.concat(alertPolString);
					notificationTargets.addAll(alertPolicy.getNotificationTarget());
				}

			}
			// Alert Notification Targets
			for (AlertNotificationTarget alertNotificationTarget : notificationTargets) {
				String notificationTargetString = toYAML(createNotificationTargetMap(alertNotificationTarget), false);
				osloString = osloString.concat(notificationTargetString);
			}
			// Services
			String serviceString = toYAML(createServiceMap(osloTemplate.getService()), false);
			osloString = osloString.concat(serviceString);

			// DataSources
			Set<DataSource> dataSources = getDataSources(osloTemplate);
			for (DataSource dataSource : dataSources) {
				String dataSourceString = toYAML(createDataSourceMap(dataSource), false);
				osloString = osloString.concat(dataSourceString);
			}
			docummentMap.put(createFIleName(osloTemplate), osloString);
		}

		return docummentMap;

	}

	/**
	 * Operation takes the OpenSLO Template and generates a filename based on the
	 * Input MDSL file.
	 * 
	 * Example: Input esample.mdsl Result : example-openslo-$oslotemplateName.yaml
	 * 
	 * @param osloTemplate
	 * @return Filename
	 */
	private String createFIleName(OSLOTemplate osloTemplate) {
		String osloFilmeName = new String(
				inputFileURI.trimFileExtension().lastSegment() + "-openslo-" + osloTemplate.getName() + ".yaml");
		return osloFilmeName;
	}

	/**
	 * 
	 * Helper Operation returns all Used DataSources in one OSLOTemplate object
	 * 
	 * @param osloTemplate
	 * @return Set<DataSources> set contains all DataSources used in the given
	 *         OSLOTemplate
	 */
	private Set<DataSource> getDataSources(OSLOTemplate osloTemplate) {
		Set<DataSource> dataSources = new HashSet<DataSource>();
		SLI indicator;
		if (osloTemplate.getBuiltInIndicator() != null) {
			indicator = osloTemplate.getBuiltInIndicator();
		} else {
			indicator = osloTemplate.getExternalIndicator();
		}

		if (indicator.getTMetric() != null) {
			if (indicator.getTMetric().getMetricSource().getDataSource() != null) {
				dataSources.add(indicator.getTMetric().getMetricSource().getDataSource());
			}
		} else {
			dataSources.addAll(getDataSourcesFromRatioMetric(indicator.getRMetric()));
		}
		return dataSources;
	}

	/**
	 * Helper Operation returns all DataSources from a RaioMetric Object
	 * 
	 * @param ratioMetric
	 * @return List<DataSource> List of all DataSources used in the given
	 *         RataioMetric Object
	 */
	private List<DataSource> getDataSourcesFromRatioMetric(RatioMetric ratioMetric) {
		List<DataSource> dataSources = new ArrayList<DataSource>();
		if (ratioMetric.getBad() != null) {
			if (ratioMetric.getBad().getDataSource() != null) {
				dataSources.add(ratioMetric.getBad().getDataSource());
			}
		}
		if (ratioMetric.getGood() != null) {
			if (ratioMetric.getGood().getDataSource() != null) {
				dataSources.add(ratioMetric.getGood().getDataSource());
			}
		}
		if (ratioMetric.getTotal() != null) {
			if (ratioMetric.getTotal().getDataSource() != null) {
				dataSources.add(ratioMetric.getTotal().getDataSource());
			}
		}
		if (ratioMetric.getRaw() != null) {
			if (ratioMetric.getRaw().getDataSource() != null) {
				dataSources.add(ratioMetric.getRaw().getDataSource());
			}
		}

		return dataSources;

	}

	/**
	 * Operation takes a Service Object and converts it to Map according to Open SLO
	 * specification
	 * 
	 * @param service
	 * @return Map<String, Object> representation of given Service Object
	 */
	private Map<String, Object> createServiceMap(Service service) {
		Map<String, Object> serviceMap = new LinkedHashMap<String, Object>();
		serviceMap.put("apiVersion", "openslo/v1");
		serviceMap.put("kind", "Service");
		serviceMap.put("metadata", createMetadataMap(service.getName(), Optional.of(service.getDisplayName())));
		Map<String, Object> serviceSpecMap = new LinkedHashMap<String, Object>();
		if (service.getPurpose() != null) {
			serviceSpecMap.put("description", service.getPurpose());
		} else {
			serviceSpecMap.put("description", "");
		}
		serviceMap.put("spec", serviceSpecMap);
		return serviceMap;
	}

	/**
	 * Operation takes a AlertNotificationTarget Object and returns a Map according
	 * to Open SLO Specification
	 * 
	 * @param alertNotificationTarget
	 * @return Map<String, Object> representation of given AlertNotificationTarget
	 *         Object
	 */
	private Map<String, Object> createNotificationTargetMap(AlertNotificationTarget alertNotificationTarget) {
		Map<String, Object> alertNotificationTargetMap = new LinkedHashMap<String, Object>();
		alertNotificationTargetMap.put("apiVersion", "openslo/v1");
		alertNotificationTargetMap.put("kind", "AlertNotificationTarget");
		alertNotificationTargetMap.put("metadata", createMetadataMap(alertNotificationTarget.getName(),
				Optional.of(alertNotificationTarget.getDisplayName())));
		alertNotificationTargetMap.put("spec", createNotificationTargetSpecmMap(alertNotificationTarget));
		return alertNotificationTargetMap;
	}

	/**
	 * Operation takes a alertNotificationTarget Object and returns a Map
	 * representing the spec Attribute of the AlertNotificationTarget according to
	 * OpenSLO specification
	 * 
	 * @param alertNotificationTarget
	 * @return Map<String, Object> of the spec Attribute of AltertNotificationTarget
	 */
	private Map<String, Object> createNotificationTargetSpecmMap(AlertNotificationTarget alertNotificationTarget) {
		Map<String, Object> alertNotificationTargetSpecMap = new LinkedHashMap<String, Object>();
		alertNotificationTargetSpecMap.put("target", alertNotificationTarget.getTarget());
		if (alertNotificationTarget.getDescription() != null) {
			alertNotificationTargetSpecMap.put("description", alertNotificationTarget.getDescription());
		}
		return alertNotificationTargetSpecMap;
	}

	/**
	 * Operation takes a AlertPolicy Object and returns a Map according to Open SLO
	 * Specification
	 * 
	 * @param alertPolicy
	 * @return Map<String, Object> representation of given AlertPolicy Object
	 */
	private Map<String, Object> createAlertPolyMap(AlertPolicy alertPolicy) {
		Map<String, Object> alertPolMap = new LinkedHashMap<String, Object>();
		alertPolMap.put("apiVersion", "openslo/v1");
		alertPolMap.put("kind", "AlertPolicy");
		alertPolMap.put("metadata",
				createMetadataMap(alertPolicy.getName(), Optional.ofNullable(alertPolicy.getDisplayName())));
		alertPolMap.put("spec", createAlertPolicySpecMap(alertPolicy));
		return alertPolMap;
	}

	/**
	 * Operation takes a Alert Policy Object and returns a Map representing the spec
	 * Attribute of the Alert Policy according to OpenSLO specification
	 * 
	 * @param alertNotificationTarget
	 * @return Map<String, Object> of the spec Attribute of AlertPolicy
	 */
	private Map<String, Object> createAlertPolicySpecMap(AlertPolicy alertPolicy) {
		Map<String, Object> alertPolSpecMap = new LinkedHashMap<String, Object>();
		if (alertPolicy.getDescription() != null) {
			alertPolSpecMap.put("description", alertPolicy.getDescription());
		}
		alertPolSpecMap.put("alertWhenNoData", alertPolicy.isNodata());
		alertPolSpecMap.put("alertWhenResolved", alertPolicy.isResolved());
		alertPolSpecMap.put("alertWhenBreaching", alertPolicy.isBreaching());
		alertPolSpecMap.put("conditions", createConditionList(alertPolicy.getConditions()));
		alertPolSpecMap.put("notificationTargets",
				createAlertNotificationTargetsList(alertPolicy.getNotificationTarget()));
		return alertPolSpecMap;
	}

	/**
	 * 
	 * Operations creates a List of Alert Notifications Targets used in the provided
	 * Service Specification Each entry in the List is a Key Value Map
	 * representation of one AlertNotificationTarget used in the Service
	 * Specification
	 * 
	 * @param notificationTargets
	 * @return List<Map<String, Object>> of AlertNotificationTarget Objects
	 */
	private List<Map<String, Object>> createAlertNotificationTargetsList(
			EList<AlertNotificationTarget> notificationTargets) {
		List<Map<String, Object>> alertNotificationTargetList = new ArrayList<Map<String, Object>>();
		for (AlertNotificationTarget alertTarget : notificationTargets) {
			Map<String, Object> targetMap = new LinkedHashMap<String, Object>();
			targetMap.put("targetRef", alertTarget.getName());
			alertNotificationTargetList.add(targetMap);
		}
		return alertNotificationTargetList;
	}

	/**
	 * 
	 * Operations creates a List of Conditions Each entry in the List is a Key Value
	 * Map represents one Condition
	 * 
	 * @param notificationTargets
	 * @return List<Map<String, Object>> of AlertNotificationTarget Objects
	 */
	private List<Map<String, Object>> createConditionList(EList<AlertCondition> conditions) {
		List<Map<String, Object>> alertConditionList = new ArrayList<Map<String, Object>>();
		for (AlertCondition alertCondition : conditions) {
			alertConditionList.add(createAlterConditionMap(alertCondition));
		}
		return alertConditionList;
	}

	/**
	 * Operations converts a Alert Condition Object into a Map representation of
	 * itself according to Open SLO specification.
	 * 
	 * @param alertCondition
	 * @return Map<String, Object> representation of the given Alert Condition
	 *         Object
	 */
	private Map<String, Object> createAlterConditionMap(AlertCondition alertCondition) {
		Map<String, Object> alertConditionMap = new LinkedHashMap<String, Object>();
		alertConditionMap.put("kind", "AlertCondition");
		alertConditionMap.put("metadata",
				createMetadataMap(alertCondition.getName(), Optional.ofNullable(alertCondition.getDisplayName())));
		alertConditionMap.put("spec", createAlertConditionSpecMap(alertCondition));
		return alertConditionMap;
	}

	/**
	 * Operation takes a Alert Condition Object. Returns a Map representing the spec
	 * Attribute of the Alert Condition according to OpenSLO specification
	 * 
	 * @param alertCondition
	 * @return Map<String, Object> of the spec Attribute of AlertCondition
	 */
	private Map<String, Object> createAlertConditionSpecMap(AlertCondition alertCondition) {
		Map<String, Object> alertConditionSpecMap = new LinkedHashMap<String, Object>();
		if (alertCondition.getDescription() != null) {
			alertConditionSpecMap.put("description", alertCondition.getDescription());
		}
		alertConditionSpecMap.put("severity", alertCondition.getSeverity());
		alertConditionSpecMap.put("condition", createConditionMap(alertCondition.getCondition()));
		return alertConditionSpecMap;
	}

	/**
	 * Operations converts a Condition Object into a Map representation of itself
	 * according to Open SLO specification.
	 * 
	 * @param condition
	 * @return Map<String, Object> representation of the given Condition Object
	 */
	private Map<String, Object> createConditionMap(Condition condition) {
		Map<String, Object> conditionMap = new LinkedHashMap<String, Object>();
		conditionMap.put("kind", condition.getKind());
		conditionMap.put("op", condition.getOperator());
		conditionMap.put("threshold", condition.getThreshold());
		conditionMap.put("lookbackWindow", createDurationShorthand(condition.getLookBack()));
		conditionMap.put("alertAfter", createDurationShorthand(condition.getAlertAfter()));
		return conditionMap;
	}

	/**
	 * Takes a duration Object and returns the Shorthand as a String Example:
	 * duration.value 24 duration.UnitOfMeasure h returns 24h
	 * 
	 * @param duration
	 * @return duration shorthand as String
	 */
	private String createDurationShorthand(Duration duration) {
		return new String(duration.getValue() + duration.getUnitOfMeasure());
	}

	/**
	 * Helper Operation which takes a Key Value Map and converts it to its String
	 * representation in YAML Format
	 * 
	 * @param objectMap
	 * @return String in YAML Format
	 */
	private String toYAML(Map<String, Object> objectMap, Boolean isFirst) {
		String yamlString = new String();
		yamlString = yamlString.concat("---" + System.lineSeparator());
		yamlString = yamlString.concat(Yaml.pretty(objectMap));

		return yamlString;
	}

	/**
	 * Operation creates the metadata object of the OpenSlo specification
	 * 
	 * @param name        of the Object
	 * @param displayName
	 * @return Key Value Map which represents the metadata Object
	 */
	private Map<String, Object> createMetadataMap(String name, Optional<String> displayName) {
		Map<String, Object> metadataMap = new LinkedHashMap<String, Object>();
		metadataMap.put("name", name);
		if (displayName.isPresent()) {
			metadataMap.put("displayName", displayName.get());
		}
		return metadataMap;
	}

	/**
	 * Operations returns a Map representation of the OpenSLO Template Object
	 * 
	 * @param osloTemplate
	 * @return Map<String, Object> representation of the given OpenSLO Template
	 */
	private Map<String, Object> convertOSLOTemplate(OSLOTemplate osloTemplate) {
		Map<String, Object> osloMap = new LinkedHashMap<>();
		osloMap.put("apiVersion", "openslo/v1");
		osloMap.put("kind", "SLO");
		osloMap.put("metadata",
				createMetadataMap(osloTemplate.getName(), Optional.ofNullable(osloTemplate.getDisplayName())));
		osloMap.put("spec", createOsloSpecMap(osloTemplate));
		return osloMap;
	}

	/**
	 * Operation takes a OSLOTemplate Object. Returns a Map representing the spec
	 * Attribute of the OSLOTemplate according to OpenSLO specification
	 * 
	 * @param osloTemplate
	 * @return Map<String, Object> of the spec Attribute of OSLOTemplate
	 */
	private Map<String, Object> createOsloSpecMap(OSLOTemplate osloTemplate) {
		Map<String, Object> specMap = new LinkedHashMap<>();
		if (osloTemplate.getDescription() != null) {
			specMap.put("description", osloTemplate.getDescription());
		}
		specMap.put("service", osloTemplate.getService().getName());

		if (osloTemplate.getBuiltInIndicator() != null) {
			specMap.put("indicator", createBuiltInSLIMap(osloTemplate.getBuiltInIndicator()));
		} else {
			specMap.put("indicatorRef", osloTemplate.getExternalIndicator().getName());
		}

		specMap.put("timeWindow", createTimeWindowMap(osloTemplate.getTimeWindow()));
		specMap.put("budgetingMethod", osloTemplate.getBudgetingMethod());

		specMap.put("objectives", createObjectivesList(osloTemplate));

		List<String> alertPolicyList = new ArrayList<String>();
		if (!osloTemplate.getExternalAlertPol().isEmpty()) {
			for (AlertPolicy alertPolicy : osloTemplate.getExternalAlertPol()) {
				alertPolicyList.add(alertPolicy.getName());
			}
		}
		if (!osloTemplate.getBuiltInalertPol().isEmpty()) {
			for (AlertPolicy alertPolicy : osloTemplate.getBuiltInalertPol()) {
				alertPolicyList.add(alertPolicy.getName());
			}
		}
		specMap.put("alertPolicies", alertPolicyList);
		return specMap;

	}

	/**
	 * Operations returns a List of Maps each represents one Objective Object
	 * declared in the Service Specification Object and used in the given OpenSLO
	 * Template
	 * 
	 * @param osloTemplate
	 * @return List<Map<String, Object>> List of Maps representation Objectives
	 *         Objects
	 */
	private List<Map<String, Object>> createObjectivesList(OSLOTemplate osloTemplate) {
		List<Map<String, Object>> objectivesList = new ArrayList<Map<String, Object>>();
		if (checkIfThresholdMetric(osloTemplate)) {
			Map<String, Object> objective = new LinkedHashMap<String, Object>();
			objectivesList.add(objective);
		} else {
			for (Objective objective : osloTemplate.getObjectives()) {
				objectivesList.add(createObjectiveMap(objective, osloTemplate));
			}
		}
		return objectivesList;

	}

	/**
	 * Operations returns a Map representation of the given Objective according to
	 * OpenSlO specification
	 * 
	 * @param objective
	 * @param osloTemplate osloTemplate to check which attributes are needed
	 * @return Map<String, Object> representation of the given Objective Object
	 */
	private Map<String, Object> createObjectiveMap(Objective objective, OSLOTemplate osloTemplate) {
		Map<String, Object> objectiveMap = new LinkedHashMap<String, Object>();
		if (objective.getDisplayName() != null) {
			objectiveMap.put("displayName", objective.getDisplayName());
		}
		if (checkIfThresholdMetric(osloTemplate)) {
			objectiveMap.put("op", objective.getOperator());
			objectiveMap.put("value", objective.getCompValue());
		}
		if (objective.getTarget() != null) {
			objectiveMap.put("target", objective.getTarget().getTarget());
		} else {
			objectiveMap.put("targetPercent", objective.getTargetPer().getTargetPer());
		}
		if (osloTemplate.getBudgetingMethod().equals("Timeslices")) {
			objectiveMap.put("timeSliceTarget", objective.getSliceTarget());
			objectiveMap.put("timeSliceWindow", getSliceWindow(objective));
		} else if (osloTemplate.getBudgetingMethod().equals("RatioTimeslices")) {
			objectiveMap.put("timeSliceWindow", getSliceWindow(objective));
		}
		return objectiveMap;

	}

	/**
	 * Operation returns the Vale for the SliceWindow Attribute
	 * 
	 * @param objective
	 * @return value
	 */
	private Object getSliceWindow(Objective objective) {
		if (objective.getDurationBased() != null) {
			return createDurationShorthand(objective.getDurationBased());
		} else {
			return objective.getNumeric();
		}
	}

	/**
	 * Operation returns true when in the given OpenSLo Template Object a Threshold
	 * Metric is used by the Service Level Indicator
	 * 
	 * @param osloTemplate
	 * @return true when ThresholdMetric is used in the Service Level Indicator
	 */
	private Boolean checkIfThresholdMetric(OSLOTemplate osloTemplate) {
		if (osloTemplate.getBuiltInIndicator() != null) {
			return osloTemplate.getBuiltInIndicator().getTMetric() != null;

		} else {
			return osloTemplate.getExternalIndicator().getTMetric() != null;
		}
	}

	/**
	 * Operation creates a Key Value Map of the Time Window Object
	 * 
	 * @param timeWindow Object
	 * @return Map of Key Value pairs representing the TimeWindow Object
	 */
	private Map<String, Object> createTimeWindowMap(TimeWindow timeWindow) {
		Map<String, Object> timeWindowMap = new LinkedHashMap<String, Object>();
		String durationShorthand = createDurationShorthand(timeWindow.getDuration());
		timeWindowMap.put("duration", durationShorthand);
		if (timeWindow.getCalendarBased() != null) {
			Map<String, Object> calendarMap = new LinkedHashMap<String, Object>();
			calendarMap.put("startTime", timeWindow.getCalendarBased().getStartDate());
			calendarMap.put("timeZone", timeWindow.getCalendarBased().getTimeZone());
			timeWindowMap.put("calendar", calendarMap);
			timeWindowMap.put("isRolling", "false");
		} else {
			timeWindowMap.put("isRolling", "true");
		}
		return timeWindowMap;
	}

	/**
	 * Operations takes a Service Level Indicator Object (SLI) and returns a Key
	 * Value Map representation of the Object
	 * 
	 * @param sli
	 * @return Map<String, Object> representing the given SLI Object
	 */
	private Map<String, Object> createBuiltInSLIMap(SLI sli) {
		Map<String, Object> sliMap = new LinkedHashMap<String, Object>();
		sliMap.put("metadata", createMetadataMap(sli.getName(), Optional.ofNullable(sli.getDisplayName())));
		sliMap.put("metadata", createMetadataMap(sli.getName(), Optional.empty()));
		sliMap.put("spec", createSLISpecMap(sli));
		return sliMap;
	}

	/**
	 * Operation takes a SLI Object. Returns a Map representing the spec Attribute
	 * of the SLI according to OpenSLO specification
	 * 
	 * @param sli
	 * @return Map<String, Object> of the spec Attribute of SLI
	 */
	private Map<String, Object> createSLISpecMap(SLI sli) {
		Map<String, Object> sliSpecMap = new LinkedHashMap<String, Object>();
		sliSpecMap.put("description", sli.getDescription());
		if (sli.getTMetric() != null) {
			sliSpecMap.put("thresholdMetric", createThresholdMetricMap(sli.getTMetric()));
		} else {
			sliSpecMap.put("ratioMetric", createRatioMetricMap(sli.getRMetric()));
		}
		return sliSpecMap;
	}

	/**
	 * Operation takes a Ratio Metric Object and converts it to Map according to
	 * Open SLO specification
	 * 
	 * @param rMetric
	 * @return Map<String, Object> representation of given Ratio Metric Object
	 */
	private Map<String, Object> createRatioMetricMap(RatioMetric rMetric) {
		Map<String, Object> ratioMetricMap = new LinkedHashMap<String, Object>();
		ratioMetricMap.put("counter", rMetric.isCounter());
		if (rMetric.getTotal() != null) {
			if (rMetric.getGood() != null) {
				Map<String, Object> goodMetricSourceWrapperMap = new LinkedHashMap<String, Object>();
				goodMetricSourceWrapperMap.put("metricSource", createMetricSourceMap(rMetric.getGood()));
				ratioMetricMap.put("good", goodMetricSourceWrapperMap);
			} else {
				Map<String, Object> badMetricSourceWrapperMap = new LinkedHashMap<String, Object>();
				badMetricSourceWrapperMap.put("metricSource", createMetricSourceMap(rMetric.getBad()));
				ratioMetricMap.put("good", badMetricSourceWrapperMap);
			}
			Map<String, Object> totalMetricSourceWrapperMap = new LinkedHashMap<String, Object>();
			totalMetricSourceWrapperMap.put("metricSource", createMetricSourceMap(rMetric.getTotal()));
			ratioMetricMap.put("total", totalMetricSourceWrapperMap);

		} else {
			Map<String, Object> metricSourceWrapperMap = new LinkedHashMap<String, Object>();
			ratioMetricMap.put("rawType", rMetric.getRType());
			metricSourceWrapperMap.put("metricSource", createMetricSourceMap(rMetric.getRaw()));
			ratioMetricMap.put("raw", metricSourceWrapperMap);
		}
		return ratioMetricMap;
	}

	/**
	 * Operation takes a Threshold Metric Object and converts it to Map according to
	 * Open SLO specification
	 * 
	 * @param tresholdMetric
	 * @return Map<String, Object> representation of given Threshold Metric Object
	 */
	private Map<String, Object> createThresholdMetricMap(ThresholdMetric thresholdMetric) {
		Map<String, Object> thresholdMetricMap = new LinkedHashMap<String, Object>();
		thresholdMetricMap.put("metricSource", createMetricSourceMap(thresholdMetric.getMetricSource()));
		return thresholdMetricMap;
	}

	/**
	 * Operation takes a Metric Source Object and converts it to Map according to
	 * Open SLO specification
	 * 
	 * @param metricSource
	 * @return Map<String, Object> representation of given Metric Source Object
	 */
	private Map<String, Object> createMetricSourceMap(MetricSource metricSource) {
		Map<String, Object> metricSourceMap = new LinkedHashMap<String, Object>();
		if (metricSource.getDataSource() != null) {
			metricSourceMap.put("metricSourceRef", metricSource.getDataSource().getName());
		}
		if (metricSource.getDataSource().getType() != null) {
			metricSourceMap.put("type", metricSource.getType());
		}
		metricSourceMap.put("spec", metricSource.getSpec());

		return metricSourceMap;
	}

	/**
	 * Operation converts a DataSource object into a Map representation according to
	 * Open SLO specification
	 * 
	 * @param dataSource
	 * @return Map<String, Object> of the given DataSource Object
	 */
	private Map<String, Object> createDataSourceMap(DataSource dataSource) {
		Map<String, Object> dataSourceMap = new LinkedHashMap<String, Object>();
		dataSourceMap.put("apiVersion", "openslo/v1");
		dataSourceMap.put("kind", "DataSource");
		dataSourceMap.put("metadata",
				createMetadataMap(dataSource.getName(), Optional.ofNullable(dataSource.getDisplayName())));
		dataSourceMap.put("spec", createDataSourceSpecMap(dataSource));
		return dataSourceMap;
	}

	/**
	 * Operation takes a DataSource Object. Returns a Map representing the spec
	 * Attribute of the DataSource according to OpenSLO specification
	 * 
	 * @param dataSource
	 * @return Map<String, Object> of the spec Attribute of DataSource
	 */
	private Map<String, Object> createDataSourceSpecMap(DataSource dataSource) {
		Map<String, Object> dataSourceSpecMap = new LinkedHashMap<String, Object>();
		if (dataSource.getDescription() != null) {
			dataSourceSpecMap.put("description", dataSource.getDescription());
		}
		dataSourceSpecMap.put("type", dataSource.getType());
		Map<String, Object> dataSourceConenctionDetailsMap = new LinkedHashMap<String, Object>();
		dataSourceConenctionDetailsMap.put("url", dataSource.getUrl());
		dataSourceSpecMap.put("connectionDetails", dataSourceConenctionDetailsMap);
		return dataSourceSpecMap;
	}

}

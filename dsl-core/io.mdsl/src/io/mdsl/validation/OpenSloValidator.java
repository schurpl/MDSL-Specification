package io.mdsl.validation;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.CheckType;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import io.mdsl.apiDescription.AlertCondition;
import io.mdsl.apiDescription.AlertNotificationTarget;
import io.mdsl.apiDescription.AlertPolicy;
import io.mdsl.apiDescription.ApiDescriptionPackage;
import io.mdsl.apiDescription.DataSource;
import io.mdsl.apiDescription.OSLOTemplate;
import io.mdsl.apiDescription.Objective;
import io.mdsl.apiDescription.RatioMetric;
import io.mdsl.apiDescription.SLI;
import io.mdsl.apiDescription.TimeWindow;
import io.mdsl.services.APIDescriptionGrammarAccess.BudgetingMethodElements;

/**
 * Class contains Validation Operations which are performed automatically in the
 * eclipse editor, to check the Open SLO Object of the Specification
 * specifically
 * 
 * @author Pascal Schur
 *
 */
public class OpenSloValidator extends AbstractMDSLValidator {

	private static final String OPERATOR_NOT_NECESSARY_MESSAGE = "The SLI has no Threshold Metric defined therefore Operator is not necessary";
	private static final String NOT_NECESSARY = "NOT NECESSARY";
	private static final String NOT_SET_CORRECTLY = "NOT SET CORRECTLY";
	private static final String CALENDAR_BASED_IS_ROLLING_MESSAGE = "Rolling duration is selected isRolling Attribute should be true";
	private static final Integer MAX_NAME_LENGTH = 255;
	private final Integer MAX_DESCRIPTION_LENGTH = 1050;
	private final static String INVALID_URL = "INVALID URL";
	private final static String HTTP = "http";
	private final static String HTTPS = "https";
	private final static String FIELD_TOO_LONG = "FIELD IS TO LONG";
	private final String FIELD_LENGTH_ERROR_MESSGAGE = "Max allowd characters are %d you have ";
	private final String INVALID_URL_MESSAGE = "Invalid URL";
	private final String WORNG_PROTOCOLL_MESSAGE = "Wrong Protocoll should be http or https";

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}

	@Check(CheckType.FAST)
	public void checkConnectionURL(DataSource source) {
		try {
			URL testURL = new URL(source.getUrl());
			testURL.toURI();
			if (!testURL.getProtocol().equals(HTTP) && !testURL.getProtocol().equals(HTTPS)) {
				error(WORNG_PROTOCOLL_MESSAGE, source, ApiDescriptionPackage.eINSTANCE.getDataSource_Url());
			}
		} catch (MalformedURLException | URISyntaxException exception) {
			error(INVALID_URL_MESSAGE, source, ApiDescriptionPackage.eINSTANCE.getDataSource_Url(), INVALID_URL);
		}

	}

	@Check
	public void checkOSLOTemplateName(OSLOTemplate oslo) {
		String name = oslo.getName();
		if (checkFieldLengthTooLong(name, MAX_NAME_LENGTH)) {
			error(String.format(FIELD_LENGTH_ERROR_MESSGAGE, MAX_NAME_LENGTH) + name.length(), oslo,
					ApiDescriptionPackage.eINSTANCE.getOSLOTemplate_Name(), FIELD_TOO_LONG);
		}
	}

	@Check
	public void checkDataSourceName(DataSource dataSource) {
		String name = dataSource.getName();
		if (checkFieldLengthTooLong(name, MAX_NAME_LENGTH)) {
			error(String.format(FIELD_LENGTH_ERROR_MESSGAGE, MAX_NAME_LENGTH) + name.length(), dataSource,
					ApiDescriptionPackage.eINSTANCE.getDataSource_Name(), FIELD_TOO_LONG);
		}
	}

	@Check
	public void checkSLIName(SLI sli) {
		String name = sli.getName();
		if (checkFieldLengthTooLong(name, MAX_NAME_LENGTH)) {
			error(String.format(FIELD_LENGTH_ERROR_MESSGAGE, MAX_NAME_LENGTH) + name.length(), sli,
					ApiDescriptionPackage.eINSTANCE.getSLI_Name(), FIELD_TOO_LONG);
		}
	}

	@Check
	public void checkAlertPolicyName(AlertPolicy alertPolicy) {
		String name = alertPolicy.getName();
		if (checkFieldLengthTooLong(name, MAX_NAME_LENGTH)) {
			error(String.format(FIELD_LENGTH_ERROR_MESSGAGE, MAX_NAME_LENGTH) + name.length(), alertPolicy,
					ApiDescriptionPackage.eINSTANCE.getAlertPolicy_Name(), FIELD_TOO_LONG);
		}
	}

	@Check
	public void checkAlertConditionName(AlertCondition alertCondition) {
		String name = alertCondition.getName();
		if (checkFieldLengthTooLong(name, MAX_NAME_LENGTH)) {
			error(String.format(FIELD_LENGTH_ERROR_MESSGAGE, MAX_NAME_LENGTH) + name.length(), alertCondition,
					ApiDescriptionPackage.eINSTANCE.getAlertCondition_Name(), FIELD_TOO_LONG);
		}
	}

	@Check
	public void checkAlertNotificationTargetName(AlertNotificationTarget alertNotificationTarget) {
		String name = alertNotificationTarget.getName();
		if (checkFieldLengthTooLong(name, MAX_NAME_LENGTH)) {
			error(String.format(String.format(FIELD_LENGTH_ERROR_MESSGAGE, MAX_NAME_LENGTH), MAX_NAME_LENGTH)
					+ name.length(), alertNotificationTarget,
					ApiDescriptionPackage.eINSTANCE.getAlertNotificationTarget_Name(), FIELD_TOO_LONG);
		}
	}

	@Check
	public void checkDataSourceDescriptionLength(DataSource dataSource) {
		String description = dataSource.getDescription();
		if (checkFieldLengthTooLong(description, MAX_DESCRIPTION_LENGTH)) {
			error(String.format(FIELD_LENGTH_ERROR_MESSGAGE, MAX_DESCRIPTION_LENGTH) + description.length(), dataSource,
					ApiDescriptionPackage.eINSTANCE.getDataSource_Description(), FIELD_TOO_LONG);
		}

	}

	@Check
	public void checkOSLOTemplateDescriptionLength(OSLOTemplate osloTemplate) {
		String description = osloTemplate.getDescription();
		if (checkFieldLengthTooLong(description, MAX_DESCRIPTION_LENGTH)) {
			error(String.format(FIELD_LENGTH_ERROR_MESSGAGE, MAX_DESCRIPTION_LENGTH) + description.length(),
					osloTemplate, ApiDescriptionPackage.eINSTANCE.getOSLOTemplate_Description(), FIELD_TOO_LONG);
		}

	}

	@Check
	public void checkAlertPolicyDescriptionLength(AlertPolicy alertPolicy) {
		String description = alertPolicy.getDescription();
		if (checkFieldLengthTooLong(description, MAX_DESCRIPTION_LENGTH)) {
			error(String.format(FIELD_LENGTH_ERROR_MESSGAGE, MAX_DESCRIPTION_LENGTH) + description.length(),
					alertPolicy, ApiDescriptionPackage.eINSTANCE.getAlertPolicy_Description(), FIELD_TOO_LONG);
		}

	}

	@Check
	public void checkAlertConditionDescriptionLength(AlertCondition alertCondition) {
		String description = alertCondition.getDescription();
		if (checkFieldLengthTooLong(description, MAX_DESCRIPTION_LENGTH)) {
			error(String.format(FIELD_LENGTH_ERROR_MESSGAGE, MAX_DESCRIPTION_LENGTH) + description.length(),
					alertCondition, ApiDescriptionPackage.eINSTANCE.getAlertCondition_Description(), FIELD_TOO_LONG);
		}

	}

	@Check
	public void checkAlertNotificationTargetDescriptionLength(AlertNotificationTarget alertNotificationTarget) {
		String description = alertNotificationTarget.getDescription();
		if (checkFieldLengthTooLong(description, MAX_DESCRIPTION_LENGTH)) {
			error(String.format(FIELD_LENGTH_ERROR_MESSGAGE, MAX_DESCRIPTION_LENGTH) + description.length(),
					alertNotificationTarget, ApiDescriptionPackage.eINSTANCE.getAlertNotificationTarget_Description(),
					FIELD_TOO_LONG);
		}

	}

	@Check
	public void checkTimeWindowIfIsRollingSetCorrectly(TimeWindow timeWindow) {
		if (timeWindow.getCalendarBased() == null && !timeWindow.isRolling()) {
			error(CALENDAR_BASED_IS_ROLLING_MESSAGE, timeWindow,
					ApiDescriptionPackage.eINSTANCE.getTimeWindow_Rolling(), NOT_SET_CORRECTLY);
		}
	}

	@Check
	public void checkIfOperatorIsSetCorrectly(OSLOTemplate osloTemplate) {
		Boolean isNotThresholdMetric = false;
		if (osloTemplate.getBuiltInIndicator() != null) {
			isNotThresholdMetric = osloTemplate.getBuiltInIndicator().getTMetric() == null;
		} else {
			isNotThresholdMetric = osloTemplate.getExternalIndicator().getTMetric() == null;
		}
		Boolean isOperatorSet = false;
		Objective causingObjective = null;
		for (Objective objective : osloTemplate.getObjectives()) {
			if (objective.getOperator() != null) {
				isOperatorSet = true;
				causingObjective = objective;
			}

		}
		if (isNotThresholdMetric && isOperatorSet) {
			error(OPERATOR_NOT_NECESSARY_MESSAGE, causingObjective,
					ApiDescriptionPackage.eINSTANCE.getObjective_Operator(), NOT_NECESSARY);
		}
	}

	@Check
	public void ObjectiveTargetValueSetInRange(Objective objective) {
		if (!isInRange(objective.getTarget().getTarget(), 0.0, 1.0, true, false)) {
			error("Target value not in range [0.0,1.0)" + objective.getTarget(), objective,
					ApiDescriptionPackage.eINSTANCE.getObjective_Target(), NOT_SET_CORRECTLY);
		}
	}

	@Check
	public void ObjectiveTargetPercentValueSetInRange(Objective objective) {
		if (!isInRange(objective.getTargetPer().getTargetPer(), 0.0, 100, true, false)) {
			error("Target value not in range [0.0,100)" + objective.getTargetPer(), objective,
					ApiDescriptionPackage.eINSTANCE.getObjective_TargetPer(), NOT_SET_CORRECTLY);
		}
	}

	@Check
	public void checkIfRawTypeIsSet(RatioMetric ratioMetric) {
		if (ratioMetric.getRaw() != null && ratioMetric.getRType() == null) {
			error("Raw Type Missing must be set", ratioMetric, ApiDescriptionPackage.eINSTANCE.getRatioMetric_RType(),
					"MISSING ATTRIBUTE");
		}
	}

	/**
	 * 
	 * Helper Method since Java does not provide a range Method for double values.
	 * 
	 * Method Checks if a given number is in the specified range
	 * 
	 * @param value
	 * @param min    Minimum value of the range
	 * @param max    Maximum value of the rang
	 * @param minInc
	 * @param maxInc
	 * @return true if value is range
	 */
	private Boolean isInRange(double value, double min, double max, Boolean minInc, Boolean maxInc) {
		if (minInc && maxInc) {
			return min <= value && value <= max;
		} else if (minInc && !maxInc) {
			return min <= value && value < max;
		} else if (!minInc && maxInc) {
			return min < value && value <= max;
		} else {
			return min < value && value < max;
		}
	}

	/**
	 * 
	 * Checks if a given String is longer than the defined maximum Length
	 * 
	 * @param fieldContent
	 * @param maxLength
	 * @return true if fieldContent is longer than given maxLength
	 */
	private Boolean checkFieldLengthTooLong(String fieldContent, Integer maxLength) {
		Boolean tooLong = false;
		if (fieldContent != null) {
			tooLong = fieldContent.length() > maxLength;
		}
		return tooLong;
	}
}

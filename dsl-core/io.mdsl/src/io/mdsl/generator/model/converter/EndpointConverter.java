/*
 * Copyright 2020 The MDSL Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mdsl.generator.model.converter;

import java.util.Optional;

import io.mdsl.apiDescription.Cardinality;
import io.mdsl.apiDescription.ElementStructure;
import io.mdsl.apiDescription.TypeReference;
import io.mdsl.generator.CardinalityHelper;
import io.mdsl.generator.model.DataType;
import io.mdsl.generator.model.DataTypeField;
import io.mdsl.generator.model.EndpointContract;
import io.mdsl.generator.model.MDSLGeneratorModel;
import io.mdsl.generator.model.Operation;
import io.mdsl.generator.model.OperationParameter;

/**
 * Converts MDSL endpoints (AST model) into endpoints of our generator model.
 *
 */
public class EndpointConverter {

	private MDSLGeneratorModel model;
	private DataTypeConverter dataTypeConverter;

	public EndpointConverter(MDSLGeneratorModel model, DataTypeConverter dataTypeConverter) {
		this.model = model;
		this.dataTypeConverter = dataTypeConverter;
	}

	public EndpointContract convert(io.mdsl.apiDescription.EndpointContract mdslEndpoint) {
		EndpointContract endpoint = new EndpointContract(mdslEndpoint.getName());
		for (io.mdsl.apiDescription.Operation operation : mdslEndpoint.getOps()) {
			endpoint.addOperation(convertOperation(operation));
		}
		return endpoint;
	}

	private Operation convertOperation(io.mdsl.apiDescription.Operation operation) {
		DataType input = null;
		DataType output = null;
		if (operation.getRequestMessage() != null) {
			// handle references specially: in this case we can assume the message as
			// already been created
			if (operation.getRequestMessage().getPayload().getNp() != null
					&& operation.getRequestMessage().getPayload().getNp().getTr() != null) {
				TypeReference ref = operation.getRequestMessage().getPayload().getNp().getTr();
				input = wrapDataTypeIntoListTypeIfNecessary(getExistingDataTypeOrCreateEmpty(ref.getDcref().getName()),
						ref.getCard());
			} else {
				ElementStructure payload = operation.getRequestMessage().getPayload();
				input = wrapDataTypeIntoListTypeIfNecessary(
						createNewDataType(operation.getName() + "RequestDataType", payload),
						getCardinality4ElementStructure(payload));
			}
		}
		if (operation.getResponseMessage() != null) {
			// handle references specially: in this case we can assume the message as
			// already been created
			if (operation.getResponseMessage().getPayload().getNp() != null
					&& operation.getResponseMessage().getPayload().getNp().getTr() != null) {
				TypeReference ref = operation.getResponseMessage().getPayload().getNp().getTr();
				output = wrapDataTypeIntoListTypeIfNecessary(getExistingDataTypeOrCreateEmpty(ref.getDcref().getName()),
						ref.getCard());
			} else {
				ElementStructure payload = operation.getResponseMessage().getPayload();
				output = wrapDataTypeIntoListTypeIfNecessary(
						createNewDataType(operation.getName().substring(0, 1).toUpperCase()
								+ operation.getName().substring(1) + "ResponseDataType", payload),
						getCardinality4ElementStructure(payload));
			}
		}

		if (input == null)
			input = getExistingDataTypeOrCreateEmpty(operation.getName().substring(0, 1).toUpperCase()
					+ operation.getName().substring(1) + "RequestMessage");
		if (output == null) {
			output = getExistingDataTypeOrCreateEmpty("VoidResponse");
		}

		Operation genModelOperation = new Operation(operation.getName());
		genModelOperation.setResponse(output);
		OperationParameter parameter = new OperationParameter("input", input);
		genModelOperation.addParameter(parameter);

		return genModelOperation;
	}

	private DataType getExistingDataTypeOrCreateEmpty(String name) {
		Optional<DataType> optDataType = this.model.getDataTypes().stream().filter(d -> d.getName().equals(name))
				.findFirst();
		if (optDataType.isPresent()) {
			return optDataType.get();
		} else {
			DataType dataType = new DataType(name);
			this.model.addDataType(dataType);
			return dataType;
		}
	}

	private DataType createNewDataType(String name, ElementStructure elementStructure) {
		DataType dataType = new DataType(name);
		this.dataTypeConverter.mapElementStructure(elementStructure, dataType);
		this.model.addDataType(dataType);
		return dataType;
	}

	private DataType wrapDataTypeIntoListTypeIfNecessary(DataType dataType, Cardinality card) {
		if (CardinalityHelper.isList(card)) {
			Optional<DataType> alreadyExistingList = getDataTypeIfAlreadyExists(dataType.getName() + "List");
			if (alreadyExistingList.isPresent())
				return alreadyExistingList.get();

			DataType wrapper = new DataType(dataType.getName() + "List");
			DataTypeField field = new DataTypeField("entries");
			field.setType(dataType);
			field.isList(true);
			wrapper.addField(field);

			model.addDataType(wrapper);
			return wrapper;
		} else if (CardinalityHelper.isOptional(card)) {
			Optional<DataType> alreadyExistingOptionalType = getDataTypeIfAlreadyExists(
					dataType.getName() + "Optional");
			if (alreadyExistingOptionalType.isPresent())
				return alreadyExistingOptionalType.get();

			DataType wrapper = new DataType(dataType.getName() + "Optional");
			DataTypeField field = new DataTypeField("value");
			field.setType(dataType);
			field.isList(false);
			field.isNullable(true);
			wrapper.addField(field);

			model.addDataType(wrapper);
			return wrapper;
		}

		return dataType;
	}

	private Optional<DataType> getDataTypeIfAlreadyExists(String name) {
		return model.getDataTypes().stream().filter(d -> d.getName().equals(name)).findFirst();
	}

	private Cardinality getCardinality4ElementStructure(ElementStructure elementStructure) {
		if (elementStructure.getPt() != null) {
			return elementStructure.getPt().getCard();
		} else if (elementStructure.getApl() != null) {
			return elementStructure.getApl().getCard();
		} else if (elementStructure.getNp() != null) {
			if (elementStructure.getNp().getAtomP() != null) {
				return elementStructure.getNp().getAtomP().getCard();
			} else if (elementStructure.getNp().getTr() != null) {
				return elementStructure.getNp().getTr().getCard();
			}
		}
		return null;
	}

}
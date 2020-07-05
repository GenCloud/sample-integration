package org.genfork.integration.model.components.transformer;

import groovy.lang.Script;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.stream.annotation.SettingClass;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.annotation.StreamComponent;
import org.genfork.integration.stream.converters.impl.GroovyConverter;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.MethodInvokingTransformer;

import static org.genfork.integration.stream.annotation.StreamComponent.ComponentType.processor;

/**
 * Model storage properties for populate the {@link MessageTransformingHandler} for the {@link MethodInvokingTransformer}
 * to invoke the service method at runtime or populate the {@link MessageTransformingHandler} instance for the provided
 * {@link GenericTransformer} for the specific payload type to convert at runtime.
 *
 * @author: GenCloud
 * @date: 2020/07
 */
@StreamComponent(componentName = "transformer", componentType = processor)
@SettingClass
@Data
@EqualsAndHashCode(callSuper = false)
public class TransformerDefinition extends AbstractDefinition<TransformerDefinition> {
	@SettingValue(originName = "groovy-script", converter = GroovyConverter.class)
	private Script groovyScript;

	@Override
	public boolean isTransformerDefinition() {
		return true;
	}
}

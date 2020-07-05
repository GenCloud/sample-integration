package org.genfork.integration.service;

import groovy.lang.Script;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.genfork.integration.exceptions.StreamException;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.model.components.AbstractHttpHandlerDefinition;
import org.genfork.integration.model.components.bean.BeanDefinition;
import org.genfork.integration.model.components.handler.HandlerDefinition;
import org.genfork.integration.model.components.inbound.http.HttpInGatewayAdapterDefinition;
import org.genfork.integration.model.components.messaging.ChannelDefinition;
import org.genfork.integration.model.components.outbound.jdbc.JdbcOutboundAdapterDefinition;
import org.genfork.integration.model.components.result.ResultDefinition;
import org.genfork.integration.model.components.transformer.TransformerDefinition;
import org.genfork.integration.model.enums.HttpAdapterType;
import org.genfork.integration.stream.registry.HttpRegistry;
import org.genfork.integration.stream.registry.JdbcRegistry;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.http.dsl.BaseHttpInboundEndpointSpec;
import org.springframework.integration.jdbc.JdbcOutboundGateway;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.genfork.integration.model.enums.HttpAdapterType.INBOUND_GATEWAY;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class IntegrationFlowBuilderRegistry {
	private final GenericApplicationContext applicationContext;

	private final HttpRegistry httpRegistry = new HttpRegistry();
	private final JdbcRegistry jdbcRegistry = new JdbcRegistry();

	@SuppressWarnings("rawtypes")
	private GenericTransformer<Message, Object> getMessageObjectGenericTransformer(Script script, Map<String, Object> scriptVariables) {
		return o -> {
			script.getBinding().setVariable("message", o);
			script.getBinding().setVariable("payload", o.getPayload());
			script.getBinding().setVariable("headers", o.getHeaders());

			scriptVariables.forEach((k, v) ->
					script.getBinding().setVariable(k, v));

			final Object result = script.run();
			if (log.isDebugEnabled()) {
				log.debug("Groovy Evaluation: Payload [{}], Evaluated [{}]", o, result);
			}

			return result;
		};
	}

	private GenericHandler<?> getObjectGenericHandler(Script script, Map<String, Object> scriptVariables) {
		return (payload, headers) -> {
			script.getBinding().setVariable("payload", payload);
			script.getBinding().setVariable("headers", headers);

			scriptVariables.forEach((k, v) ->
					script.getBinding().setVariable(k, v));

			final Object result = script.run();

			if (log.isDebugEnabled()) {
				log.debug("Groovy Evaluation: Payload [{}], Evaluated [{}]", payload, result);
			}

			return result;
		};
	}

	/**
	 * Populate {@link WireTap} for currentMessageChannel with {@link LoggingHandler} subscriber for provided {@link Level}
	 * logging level and {@code org.springframework.integration.handler.LoggingHandler} as default logging category.
	 *
	 * @param streamBuilder - stream data builder
	 * @param def           - definition for logging
	 */
	private void setLogHandler(IntegrationFlowDefinition<?> streamBuilder, AbstractDefinition<?> def) {
		if (def.isLogStages()) {
			streamBuilder.log(def.getLogLevel());
		}
	}

	/**
	 * Function of processing registered stream handlers and
	 * their initialization in the context of an application by typing components.
	 *
	 * @param flowBuilder        - stream data builder
	 * @param definitions        - collection of flo definition settings
	 * @param enableReplyChannel - boolean flag enable reply channel or not
	 * @return completed flo definition
	 */
	private IntegrationFlow buildFlow(String streamId, IntegrationFlowDefinition<?> flowBuilder, List<AbstractDefinition<?>> definitions, boolean enableReplyChannel) {
		definitions.forEach(def -> {
			if (def.isTransformerDefinition()) {
				defineTransformerEvaluator(streamId, applicationContext, definitions, (TransformerDefinition) def, flowBuilder);
			} else if (def.isHandlerDefinition()) {
				buildHandler(streamId, flowBuilder, definitions, (HandlerDefinition) def);
			} else if (def.isJdbcDefinition() && def.isOutbound()) {
				buildJdbcOutAdapter(streamId, flowBuilder, (JdbcOutboundAdapterDefinition) def);
			}
		});

		final AbstractDefinition<?> resultDef = definitions
				.stream()
				.filter(AbstractDefinition::isResultDefinition)
				.findFirst()
				.orElse(null);

		if (resultDef != null) {
			final boolean result = buildResult(streamId, flowBuilder, enableReplyChannel, (ResultDefinition) resultDef);
			if (result) {
				return flowBuilder.nullChannel();
			}
		}

		return ((IntegrationFlowBuilder) flowBuilder).get();
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the selected protocol specific {@link MessageHandler}.
	 *
	 * @param flowBuilder - integration stream builder
	 * @param def         - jdbc outbound adapter definition
	 */
	private void buildJdbcOutAdapter(String streamId, IntegrationFlowDefinition<?> flowBuilder, JdbcOutboundAdapterDefinition def) {
		final JdbcOutboundGateway gateway = jdbcRegistry.defineOutboundAdapter(streamId, applicationContext, def);
		flowBuilder.handle(gateway);
		setLogHandler(flowBuilder, def);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the {@code method} for provided {@code bean} or invoke the provided {@link GenericHandler} in runtime.
	 *
	 * @param flowBuilder - integration stream builder
	 * @param def         - handler definition
	 */
	private void buildHandler(String streamId, IntegrationFlowDefinition<?> flowBuilder, List<AbstractDefinition<?>> definitions, HandlerDefinition def) {
		final AbstractDefinition<?> definition = def.getHandlerDefinition();
		if (definition == null) {
			return;
		}

		if (definition.isTransformerDefinition()) {
			final TransformerDefinition transformDefinition = (TransformerDefinition) definition;
			final Script groovyScript = transformDefinition.getGroovyScript();
			if (groovyScript != null) {
				defineGenericHandlerEvaluator(streamId, applicationContext, definitions, transformDefinition, flowBuilder);
			}
		} else if (definition.isOutbound()) {
			if (definition.isJdbcDefinition()) {
				buildJdbcOutAdapter(streamId, flowBuilder, (JdbcOutboundAdapterDefinition) definition);
			}
		}
	}

	/**
	 * Init integration stream builder by message channel.
	 *
	 * @param cancel  - cancel result or no
	 * @param collect - collection of registered definitions
	 * @return integration stream builder
	 */
	public IntegrationFlow initFlowByChannel(String streamId, boolean cancel, List<AbstractDefinition<?>> channelDefinitions, List<AbstractDefinition<?>> collect) {
		final ChannelDefinition def = channelDefinitions
				.stream()
				.filter(AbstractDefinition::isChannelDefinition)
				.map(d -> (ChannelDefinition) d)
				.findFirst().orElse(null);

		if (def != null) {
			final String channelId = def.getId();

			final String requestChannel = String.format("%s_%s", streamId, channelId);

			if (applicationContext.containsBean(requestChannel)) {
				final MessageChannel channel = applicationContext.getBean(requestChannel, MessageChannel.class);
				final IntegrationFlowBuilder flowBuilder = IntegrationFlows.from(channel);
				setLogHandler(flowBuilder, def);
				return buildFlow(streamId, flowBuilder, collect, cancel);
			} else {
				throw new StreamException("Request channel does not contains bean in application context, check your configuration!");
			}
		} else {
			throw new StreamException("Can't find request channel in your configuration, check this!");
		}
	}

	/**
	 * Init integration stream builder by http inbound gateway adapter.
	 *
	 * @param cancel  - cancel result or no
	 * @param collect - collection of registered definitions
	 * @param def     - http inbound gateway definition
	 * @return integration stream builder
	 */
	public IntegrationFlow initFlowByHttpDefinition(String streamId, String uuid, boolean cancel, List<AbstractDefinition<?>> collect, AbstractDefinition<? extends AbstractHttpHandlerDefinition<?>> def) {
		final AbstractHttpHandlerDefinition<?> fromDef = (AbstractHttpHandlerDefinition<?>) def;

		final HttpAdapterType httpAdapterType = fromDef.getHttpAdapterType();
		if (httpAdapterType == INBOUND_GATEWAY) {
			final BaseHttpInboundEndpointSpec<?, ?> spec = httpRegistry.defineHttpInGatewaySpec(streamId, applicationContext, uuid, (HttpInGatewayAdapterDefinition) def);
			final IntegrationFlowBuilder flowBuilder = IntegrationFlows.from(spec);
			setLogHandler(flowBuilder, def);
			collect.remove(def);
			return buildFlow(streamId, flowBuilder, collect, cancel);
		}

		return null;
	}

	/**
	 * Populate the provided {@link MessageChannel} instance at the current {@link IntegrationFlow} chain position.
	 *
	 * @param flowBuilder        - integration stream builder
	 * @param enableReplyChannel - boolean flag of enabling result or not
	 * @param def                - result definition
	 */
	private boolean buildResult(String streamId, IntegrationFlowDefinition<?> flowBuilder, boolean enableReplyChannel, ResultDefinition def) {
		if (!enableReplyChannel) {
			final boolean resultToNull = def.isResultToNull();
			final String replyChannel = def.getReplyChannel();
			if (resultToNull) {
				return true;
			} else if (replyChannel != null) {
				final String repChannelName = String.format("%s_%s", streamId, replyChannel);
				if (applicationContext.containsBean(repChannelName)) {
					final MessageChannel messageChannel = applicationContext.getBean(repChannelName, MessageChannel.class);
					flowBuilder.channel(messageChannel);
					setLogHandler(flowBuilder, def);
				} else {
					throw new StreamException("Reply channel does not contains bean in application context, check your configuration!!");
				}
			}
		}

		return false;
	}

	/**
	 * Defines data converters depending on the passed parameters.
	 * Initializes either embedded data transformers or groovy scripts in the stream.
	 *
	 * @param applicationContext - spring context factory
	 * @param definition         - object meta information adapter
	 * @param builder            - stream builder
	 */
	public void defineTransformerEvaluator(String streamId, GenericApplicationContext applicationContext, List<AbstractDefinition<?>> definitions, TransformerDefinition definition, IntegrationFlowDefinition<?> builder) {
		final boolean logging = definition.isLogStages();
		final Level level = definition.getLogLevel();

		final Script groovyScript = definition.getGroovyScript();

		try {
			final Map<String, Object> scriptVariables = new HashMap<>();

			scriptVariables.put("applicationContext", applicationContext);

			defineBeanObjects(streamId, applicationContext, definitions, scriptVariables);

			builder.transform(Message.class, getMessageObjectGenericTransformer(groovyScript, scriptVariables));

			if (logging) {
				builder.log(level);
			}
		} catch (Exception ex) {
			throw new StreamException(String.format("Cant evaluate transformer definition -> %s", ex.getMessage()));
		}
	}

	/**
	 * Defines data converters depending on the passed parameters.
	 * Initializes either embedded data handler or groovy GenericHandler scripts in the stream.
	 *
	 * @param applicationContext - spring context factory
	 * @param definition         - object meta information adapter
	 * @param builder            - stream builder
	 */
	private void defineGenericHandlerEvaluator(String streamId, GenericApplicationContext applicationContext, List<AbstractDefinition<?>> definitions, TransformerDefinition definition, IntegrationFlowDefinition<?> builder) {
		try {
			final Script groovyScript = definition.getGroovyScript();

			try {
				final Map<String, Object> scriptVariables = new HashMap<>();

				scriptVariables.put("applicationContext", applicationContext);

				defineBeanObjects(streamId, applicationContext, definitions, scriptVariables);

				builder.handle(getObjectGenericHandler(groovyScript, scriptVariables));

			} catch (Exception ex) {
				throw new StreamException("Cant evaluate handler definition -> class type is not math! Use GenericHandler<?> type.");
			}
		} catch (Exception e) {
			throw new StreamException(e.getLocalizedMessage());
		}
	}

	private void defineBeanObjects(String streamId, GenericApplicationContext applicationContext, List<AbstractDefinition<?>> definitions, Map<String, Object> scriptVariables) {
		final List<BeanDefinition> beans = definitions
				.stream()
				.filter(AbstractDefinition::isBeanDefinition)
				.map(d -> (BeanDefinition) d)
				.collect(Collectors.toList());

		beans
				.stream()
				.map(def -> String.format("%s_%s", streamId, def.getId()))
				.filter(applicationContext::containsBean)
				.forEach(def -> {
					final Object bean = applicationContext.getBean(def);
					scriptVariables.putIfAbsent(def.substring(def.lastIndexOf("_") + 1), bean);
				});
	}
}

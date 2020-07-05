package org.genfork.integration.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.genfork.integration.exceptions.StreamException;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.model.components.AbstractHttpHandlerDefinition;
import org.genfork.integration.model.components.bean.BeanDefinition;
import org.genfork.integration.model.components.bean.PropertyDefinition;
import org.genfork.integration.model.components.messaging.ChannelDefinition;
import org.genfork.integration.model.components.result.ResultDefinition;
import org.genfork.integration.model.enums.ChannelType;
import org.genfork.integration.model.request.definition.DefinitionDeployModel;
import org.genfork.integration.model.response.StatusRegistration;
import org.genfork.integration.model.response.StreamInfo;
import org.genfork.integration.service.monitoring.MonitoringService;
import org.genfork.integration.stream.StreamComponentRegistrar;
import org.genfork.integration.utils.TypeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.genfork.integration.model.enums.ChannelType.direct;
import static org.genfork.integration.model.response.StatusRegistration.Status.ERROR;
import static org.genfork.integration.model.response.StatusRegistration.Status.SUCCESS;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreamDeployingService {
	private final GenericApplicationContext applicationContext;
	private final IntegrationFlowContext integrationFlowContext;
	private final MonitoringService monitoringService;
	private final IntegrationFlowBuilderRegistry integrationFlowBuilderRegistry;

	private final ConcurrentMap<Pair<String, StreamInfo>, IntegrationFlow> deployedStreams = new ConcurrentHashMap<>();

	@Getter
	private final ConcurrentMap<Pair<String, StreamInfo>, LinkedList<AbstractDefinition<?>>> deployedStreamsMetaInfo = new ConcurrentHashMap<>();

	@Value("${monitoring.injectction.default}")
	private boolean metricsEnabled;

	/**
	 * Check stream is running.
	 *
	 * @param streamId - stream identifier
	 * @return bool value {#true} if stream is running
	 */
	public boolean isStreamStarted(String streamId) {
		final IntegrationFlowRegistration registered = integrationFlowContext.getRegistrationById(streamId);
		return registered != null;
	}

	/**
	 * Check stream is registered in application context.
	 *
	 * @param streamId - stream identifier
	 * @return bool value {#true} if stream is registered
	 */
	public boolean isStreamDeployed(String streamId) {
		final Optional<String> key = deployedStreams.keySet()
				.stream()
				.filter(id -> id.getFirst().contains(streamId))
				.map(Pair::getFirst)
				.findFirst();
		return key.isPresent();
	}

	/**
	 * Deploy stream definition components into integration stream context
	 *
	 * @param uuid                  - stream identifier
	 * @param definitionDeployModel - stream deploy model
	 * @return http entry of status registration
	 */
	public ResponseEntity<StatusRegistration> deployStream(String uuid, DefinitionDeployModel definitionDeployModel) {
		try {
			final LinkedList<AbstractDefinition<?>> components = definitionDeployModel.getComponents()
					.stream()
					.map(c -> {
						final String componentName = c.getComponentName();
						final Class<AbstractDefinition<?>> streamComponentType = StreamComponentRegistrar.getStreamComponent(componentName);
						if (streamComponentType == null) {
							throw new StreamException("Can't find stream component with the same name: " + componentName);
						}

						final AbstractDefinition<?> abstractDefinition = TypeUtils.instantiateObject(streamComponentType);
						abstractDefinition.pushProps(c.getComponentParameters());
						return abstractDefinition;
					})
					.sorted(Comparator.comparingInt(AbstractDefinition::getOrder))
					.collect(Collectors.toCollection(LinkedList::new));
			return registerStreamContext(definitionDeployModel, uuid, components);
		} catch (Exception ex) {
			return new ResponseEntity<>(new StatusRegistration(ERROR, String.format("Can't deploy [%s] stream. Trace: [%s]", definitionDeployModel.getFlowName(), ex.getMessage())), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Register new integration stream in application context.
	 *
	 * @param definitionDeployModel - stream deploy model
	 * @param definitions           - meta information of stream adapters
	 * @return http entry of status registration
	 */
	@SuppressWarnings("unchecked")
	public ResponseEntity<StatusRegistration> registerStreamContext(DefinitionDeployModel definitionDeployModel, String uuid, LinkedList<AbstractDefinition<?>> definitions) {
		final String streamId = String.format("%s_%s", definitionDeployModel.getFlowName(), uuid);

		try {
			if (CollectionUtils.isEmpty(definitions)) {
				throw new StreamException("Does not have type stream with the same name!");
			}

			if (isStreamDeployed(uuid)) {
				throw new StreamException("Already deployed with the same name!");
			}

			initStreamChannels(streamId, definitions);
			initStreamBeans(streamId, definitions);

			IntegrationFlow flow = null;

			final boolean cancel = isCancel(definitions);

			final List<AbstractDefinition<?>> collect = definitions
					.stream()
					.filter(this::excludeDefinition)
					.collect(Collectors.toList());

			final List<AbstractDefinition<?>> inboundCollection = definitions
					.stream()
					.filter(AbstractDefinition::isInbound)
					.collect(Collectors.toList());

			final List<AbstractDefinition<?>> channelDefinitions = definitions
					.stream()
					.filter(AbstractDefinition::isChannelDefinition)
					.collect(Collectors.toList());

			if (!inboundCollection.isEmpty()) {
				if (inboundCollection.size() > 1) {
					throw new StreamException("Wrong configuration - inbound adapter must be 1!");
				}

				final AbstractDefinition<?> def = inboundCollection.get(0);
				if (def.isHttpDefinition()) {
					flow = integrationFlowBuilderRegistry.initFlowByHttpDefinition(streamId, uuid, cancel, collect, (AbstractDefinition<? extends AbstractHttpHandlerDefinition<?>>) def);
				}
			} else {
				flow = integrationFlowBuilderRegistry.initFlowByChannel(streamId, cancel, channelDefinitions, collect);
			}

			final StreamInfo streamInfo = new StreamInfo();
			streamInfo.setStreamName(definitionDeployModel.getFlowName());
			streamInfo.setDescription(definitionDeployModel.getDescription());
			final Pair<String, StreamInfo> pair = Pair.of(streamId, streamInfo);
			if (!deployedStreams.containsKey(pair)) {
				deployedStreamsMetaInfo.put(pair, definitions);
				deployedStreams.putIfAbsent(pair, flow);
			}

			if (log.isDebugEnabled()) {
				log.debug("Deployed stream with name {} and guid {}", pair, uuid);
			}

			return new ResponseEntity<>(new StatusRegistration(SUCCESS, uuid, null), HttpStatus.OK);
		} catch (Exception ex) {
			if (!CollectionUtils.isEmpty(definitions)) {
				removeBeansDefinition(streamId, definitions);
				removeChannelsDefinition(streamId, definitions);
			}

			log.error("Can't deploy stream [{}]. Trace: {}", definitionDeployModel, ex.getMessage());
			return new ResponseEntity<>(new StatusRegistration(ERROR, String.format("Can't deploy stream [%s]. Trace: %s", definitionDeployModel.getFlowName(), ex.getMessage())), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Exclude already initialized definitions.
	 *
	 * @param def - verifiable def
	 * @return boolean value
	 */
	private boolean excludeDefinition(AbstractDefinition<?> def) {
		return !def.isChannelDefinition();
	}

	/**
	 * Check stream is returning result or not.
	 *
	 * @param definitions - collection of registered definitions
	 * @return boolean value
	 */
	private boolean isCancel(List<AbstractDefinition<?>> definitions) {
		final ResultDefinition resultDef = (ResultDefinition) definitions
				.stream()
				.filter(AbstractDefinition::isResultDefinition)
				.findFirst()
				.orElse(null);

		boolean cancel = true;
		if (resultDef != null) {
			cancel = resultDef.isCancel();
		}
		return cancel;
	}

	/**
	 * Initialize all defined in stream channels in application context.
	 *
	 * @param definitions - collection of registered definitions
	 */
	private void initStreamChannels(String streamId, List<AbstractDefinition<?>> definitions) {
		definitions
				.stream()
				.filter(AbstractDefinition::isChannelDefinition)
				.map(d -> (ChannelDefinition) d)
				.forEach(def -> {
					final ChannelType channelType = def.getChannelType();
					final int maxSubscribers = def.getMaxSubscribers();
					final boolean failover = def.isFailover();

					final String format = String.format("%s_%s", streamId, def.getId());
					if (channelType == direct) {
						final DirectChannel channel = new DirectChannel();
						channel.setFailover(failover);

						if (maxSubscribers > 0) {
							channel.setMaxSubscribers(maxSubscribers);
						}

						applicationContext.registerBean(format, AbstractMessageChannel.class, () -> channel);
					}
				});
	}

	/**
	 * Initialize all defined in stream beans in application context.
	 *
	 * @param streamId    - stream id
	 * @param definitions - collection of registered definitions
	 */
	private void initStreamBeans(String streamId, List<AbstractDefinition<?>> definitions) {
		definitions
				.stream()
				.filter(AbstractDefinition::isBeanDefinition)
				.map(d -> (BeanDefinition) d)
				.forEach(def -> {
					final String id = String.format("%s_%s", streamId, def.getId());

					final String initMethodName = def.getInitMethod();
					final Class<?> type = def.getBeanType();

					final List<PropertyDefinition> constructorArgs = def.getConstructorArgs();
					final List<PropertyDefinition> propertyArgs = def.getPropertyArgs();

					final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(type);

					if (!CollectionUtils.isEmpty(constructorArgs)) {
						constructorArgs.forEach(pair -> {
							final Object defaultValue = pair.getValue();
							if (pair instanceof Map) {
								builder.addConstructorArgValue(pair);
							} else if (pair instanceof List) {
								builder.addConstructorArgValue(pair);
							} else {
								if (pair.isNamedBean()) {
									final String beanName = String.format("%s_%s", streamId, defaultValue);
									if (applicationContext.containsBean(beanName)) {
										final Object bean = applicationContext.getBean(beanName);
										builder.addConstructorArgValue(bean);
									} else {
										throw new StreamException("No such bean found in context: " + beanName);
									}
								} else {
									builder.addConstructorArgValue(defaultValue);
								}
							}
						});
					}

					if (!CollectionUtils.isEmpty(propertyArgs)) {
						propertyArgs.forEach(pair -> {
							final Object defaultValue = pair.getValue();
							final String propertyName = pair.getPropertyName();
							if (pair.isNamedBean()) {
								final String beanName = String.format("%s_%s", streamId, defaultValue);
								if (applicationContext.containsBean(beanName)) {
									final Object bean = applicationContext.getBean(beanName);
									builder.addPropertyValue(propertyName, bean);
								}
							} else {
								builder.addPropertyValue(propertyName, defaultValue);
							}
						});
					}

					final AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
					beanDefinition.setScope(SCOPE_SINGLETON);

					if (!StringUtils.isEmpty(initMethodName)) {
						beanDefinition.setInitMethodName(initMethodName);
					}

					applicationContext.registerBeanDefinition(id, beanDefinition);
				});
	}

	/**
	 * Register {@link IntegrationFlow} and all the dependant and support components in the application context and
	 * return associated {@link IntegrationFlowRegistration} control object.
	 *
	 * @param streamId - stream identifier
	 * @return http entry status of running
	 */
	public ResponseEntity<StatusRegistration> startStream(String streamId) {
		try {
			final Pair<String, StreamInfo> key = deployedStreams.keySet()
					.stream()
					.filter(id -> id.getFirst().contains(streamId))
					.findFirst()
					.orElseThrow(() -> new StreamException(String.format("No math streams by streamId %s", streamId)));

			final IntegrationFlow flow = deployedStreams.get(key);
			if (flow == null) {
				throw new StreamException(String.format("No math streams by streamId %s", streamId));
			}

			if (integrationFlowContext.getRegistrationById(streamId) != null) {
				throw new StreamException(String.format("Streams with the same streamId [%s] already started!", streamId));
			}

			final IntegrationFlowRegistration register = integrationFlowContext.registration(flow).id(streamId).register();
			final IntegrationFlow integrationFlow = register.getIntegrationFlow();
			if (integrationFlow instanceof StandardIntegrationFlow) {
				final StandardIntegrationFlow standardIntegrationFlow = (StandardIntegrationFlow) integrationFlow;
				final Map<Object, String> integrationComponents = standardIntegrationFlow.getIntegrationComponents();

				final List<AbstractMessageChannel> channels = new LinkedList<>();
				integrationComponents.keySet().forEach(components -> {
					if (components instanceof AbstractMessageChannel) {
						final AbstractMessageChannel messageChannel = (AbstractMessageChannel) components;
						messageChannel.setCountsEnabled(true);
						messageChannel.setStatsEnabled(true);
						channels.add(messageChannel);
					}
				});

				if (!channels.isEmpty() && metricsEnabled) {
					monitoringService.add(streamId, channels);
				}
			}

			if (log.isDebugEnabled()) {
				log.debug("Started stream with guid {}", streamId);
			}

			return new ResponseEntity<>(new StatusRegistration(SUCCESS), HttpStatus.OK);
		} catch (Exception ex) {
			return new ResponseEntity<>(new StatusRegistration(ERROR, String.format("Can't start stream [%s]. Trace: %s", streamId, ex.getMessage())), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Stop stream in integration stream context and destroy it.
	 *
	 * @param streamId - stream identifier
	 * @return http entry status of stop operation
	 */
	public ResponseEntity<StatusRegistration> stopStream(String streamId) {
		try {
			final IntegrationFlowRegistration registered = integrationFlowContext.getRegistrationById(streamId);
			if (registered != null) {
				registered.stop();
				registered.destroy();

				if (log.isDebugEnabled()) {
					log.debug("Stopped stream with guid {}", streamId);
				}

				return new ResponseEntity<>(new StatusRegistration(SUCCESS), HttpStatus.OK);
			} else {
				return new ResponseEntity<>(new StatusRegistration(ERROR, String.format("Stream with name [%s] not found!", streamId)), HttpStatus.NOT_FOUND);
			}
		} catch (Exception ex) {
			log.error("Can't stop stream [{}] Trace: [{}]", streamId, ex.getMessage());
			return new ResponseEntity<>(new StatusRegistration(ERROR, String.format("Can't stop stream [%s] Trace: [%s]", streamId, ex.getMessage())), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Unregister integration stream in application context.
	 * Clean all initialized bean's in application context by stream.
	 *
	 * @param streamId    - flo name
	 * @param definitions - stream adapters/beans/components definitions
	 * @return http entry status of unregister operation
	 */
	public ResponseEntity<StatusRegistration> undeployStream(String streamId, List<AbstractDefinition<?>> definitions) {
		if (isStreamStarted(streamId)) {
			return new ResponseEntity<>(new StatusRegistration(ERROR, String.format("Stream with name [%s] is not stopped!", streamId)), HttpStatus.BAD_REQUEST);
		}

		final Optional<Pair<String, StreamInfo>> key = deployedStreams.keySet()
				.stream()
				.filter(id -> id.getFirst().contains(streamId))
				.findFirst();

		if (key.isPresent()) {
			final Pair<String, StreamInfo> foundKey = key.get();
			final IntegrationFlow flow = deployedStreams.get(foundKey);
			if (flow != null) {
				removeBeansDefinition(foundKey.getFirst(), definitions);
				removeChannelsDefinition(foundKey.getFirst(), definitions);

				deployedStreams.remove(foundKey);
				deployedStreamsMetaInfo.remove(foundKey);

				if (log.isDebugEnabled()) {
					log.debug("Un deployed stream with guid {}", streamId);
				}

				return new ResponseEntity<>(new StatusRegistration(SUCCESS), HttpStatus.OK);
			} else {
				return new ResponseEntity<>(new StatusRegistration(ERROR, String.format("Stream with name [%s] not found!", streamId)), HttpStatus.NOT_FOUND);
			}
		} else {
			return new ResponseEntity<>(new StatusRegistration(ERROR, String.format("Stream with name [%s] not found!", streamId)), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Remove all registered bean's in application context by stream definitions.
	 *
	 * @param definitions - stream definitions
	 */
	private void removeBeansDefinition(String streamId, List<AbstractDefinition<?>> definitions) {
		definitions
				.stream()
				.filter(AbstractDefinition::isBeanDefinition)
				.map(d -> (BeanDefinition) d)
				.forEach(d -> {
					final String id = String.format("%s_%s", streamId, d.getId());
					if (applicationContext.containsBeanDefinition(id)) {
						applicationContext.removeBeanDefinition(id);

						if (log.isDebugEnabled()) {
							log.debug("Remove bean definition by id {}", id);
						}
					}
				});
	}

	/**
	 * Remove all registered bean's in application context by stream definitions.
	 *
	 * @param definitions - stream definitions
	 */
	private void removeChannelsDefinition(String streamId, List<AbstractDefinition<?>> definitions) {
		definitions
				.stream()
				.filter(AbstractDefinition::isChannelDefinition)
				.map(d -> (ChannelDefinition) d)
				.forEach(d -> {
					final String id = String.format("%s_%s", streamId, d.getId());
					if (applicationContext.containsBeanDefinition(id)) {
						applicationContext.removeBeanDefinition(id);

						if (log.isDebugEnabled()) {
							log.debug("Remove bean definition by id {}", id);
						}
					}
				});
	}
}

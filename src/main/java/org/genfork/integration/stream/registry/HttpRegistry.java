package org.genfork.integration.stream.registry;

import org.genfork.integration.exceptions.StreamException;
import org.genfork.integration.model.components.inbound.http.AbstractHttpInboundDefinition;
import org.genfork.integration.model.components.inbound.http.CrossOrigin;
import org.genfork.integration.model.components.inbound.http.HttpInGatewayAdapterDefinition;
import org.genfork.integration.model.components.inbound.http.RequestMapping;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.http.dsl.BaseHttpInboundEndpointSpec;
import org.springframework.integration.http.dsl.Http;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
public class HttpRegistry {
	/**
	 * Build inbound messaging gateway specification by http adapter definition.
	 *
	 * @param uuid       - UUID stream
	 * @param definition - inbound http definition
	 * @return inbound specification
	 */
	public BaseHttpInboundEndpointSpec<?, ?> defineHttpInGatewaySpec(String streamId, GenericApplicationContext applicationContext, String uuid, HttpInGatewayAdapterDefinition definition) {
		final String localPath = definition.getLocalPath();

		final BaseHttpInboundEndpointSpec<?, ?> spec = Http.inboundGateway(localPath != null ? localPath : String.format("/stream/%s/call", uuid));
		defineInboundCommonInfo(streamId, applicationContext, spec, definition);
		return spec;
	}

	private void defineInboundCommonInfo(String streamId, GenericApplicationContext applicationContext, BaseHttpInboundEndpointSpec<?, ?> spec, AbstractHttpInboundDefinition<?> definition) {
		final List<String> supportedHttpMethods = definition.getSupportedHttpMethods();
		final Class<?> payloadType = definition.getPayloadType();
		final String payloadExpression = definition.getPayloadExpression();
		final List<String> mappedRequestHeaders = definition.getMappedRequestHeaders();
		final List<String> messageConverters = definition.getMessageConverters();
		final boolean mergeWithDefaultConverters = definition.isMergeWithDefaultConverters();
		final long requestTimeout = definition.getRequestTimeout();
		final long responseTimeout = definition.getResponseTimeout();

		spec.requestTimeout(requestTimeout);
		spec.replyTimeout(responseTimeout);

		final String requestChannel = definition.getRequestChannel();
		final String replyChannel = definition.getReplyChannel();

		final CrossOrigin crossOrigin = definition.getCrossOrigin();
		final RequestMapping requestMapping = definition.getRequestMapping();

		if (!CollectionUtils.isEmpty(messageConverters)) {
			final HttpMessageConverter<?>[] objects = messageConverters
					.stream()
					.map(c -> String.format("%s_%s", streamId, c))
					.filter(applicationContext::containsBean)
					.map(m -> applicationContext.getBean(m, HttpMessageConverter.class))
					.toArray(HttpMessageConverter[]::new);

			spec.messageConverters(objects);
		}

		if (mergeWithDefaultConverters) {
			spec.mergeWithDefaultConverters(true);
		}

		if (applicationContext != null) {
			if (requestChannel != null) {
				final String reqChannelName = String.format("%s_%s", streamId, requestChannel);
				if (applicationContext.containsBean(reqChannelName)) {
					spec.requestChannel(applicationContext.getBean(reqChannelName, MessageChannel.class));
				} else {
					throw new StreamException("Request channel not found in context. Check your configuration!");
				}
			}

			if (replyChannel != null) {
				final String repChannelName = String.format("%s_%s", streamId, replyChannel);

				if (applicationContext.containsBean(repChannelName)) {
					spec.replyChannel(applicationContext.getBean(repChannelName, MessageChannel.class));
				} else {
					throw new StreamException("Reply channel not found in context. Check your configuration!");
				}
			}
		}

		spec.requestPayloadType(payloadType);

		spec.requestMapping(rms ->
				rms.methods(supportedHttpMethods.stream().map(HttpMethod::valueOf).toArray(HttpMethod[]::new)));

		if (payloadExpression != null) {
			spec.payloadExpression(payloadExpression);
		}

		if (!CollectionUtils.isEmpty(mappedRequestHeaders)) {
			spec.mappedRequestHeaders(mappedRequestHeaders.toArray(new String[0]));
		}

		if (crossOrigin != null) {
			spec.crossOrigin(cross -> {
				final List<String> allowedHeaders = crossOrigin.getAllowedHeaders();
				final List<String> exposedHeaders = crossOrigin.getExposedHeaders();
				final List<RequestMethod> httpMethod = crossOrigin.getHttpMethods();
				final int maxAge = crossOrigin.getMaxAge();
				final List<String> origin = crossOrigin.getOrigin();
				final boolean allowCredentials = crossOrigin.isAllowCredentials();
				final String[] headers = allowedHeaders.toArray(new String[0]);
				cross.allowedHeaders(headers);
				cross.method(httpMethod.toArray(new RequestMethod[0]));
				cross.origin(origin.toArray(new String[0]));
				cross.maxAge(maxAge);
				cross.allowCredentials(allowCredentials);

				if (!CollectionUtils.isEmpty(exposedHeaders)) {
					cross.exposedHeaders(exposedHeaders.toArray(new String[0]));
				}
			});
		}

		if (requestMapping != null) {
			final List<String> params = requestMapping.getRequestMappingParams();
			final List<String> consumes = requestMapping.getRequestMappingConsumes();
			final List<String> produces = requestMapping.getRequestMappingProduces();
			if (!CollectionUtils.isEmpty(params)) {
				spec.requestMapping(rms ->
						rms.params(params.toArray(new String[0])));
			}

			if (consumes != null) {
				spec.requestMapping(rms ->
						rms.consumes(consumes.toArray(new String[0])));
			}

			if (produces != null) {
				spec.requestMapping(rms ->
						rms.produces(produces.toArray(new String[0])));
			}
		}
	}
}

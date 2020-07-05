package org.genfork.integration.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.model.request.definition.DefinitionDeployModel;
import org.genfork.integration.model.response.StatusRegistration;
import org.genfork.integration.model.response.StreamComponentInfo;
import org.genfork.integration.model.response.StreamDefinition;
import org.genfork.integration.model.response.StreamInfo;
import org.genfork.integration.model.response.metrics.ChannelStats;
import org.genfork.integration.service.StreamDeployingService;
import org.genfork.integration.service.monitoring.MonitoringService;
import org.genfork.integration.stream.StreamComponentOptionsParser;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static org.genfork.integration.model.response.StatusRegistration.Status.ERROR;
import static org.genfork.integration.model.response.StreamDefinition.StreamStatus.*;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@RestController
@RequestMapping("/stream")
@Slf4j
@RequiredArgsConstructor
public class StreamController {
	private final MonitoringService monitoringService;
	private final StreamDeployingService streamDeployingService;

	@GetMapping("/list")
	public ResponseEntity<List<StreamDefinition>> getAllStatusesFlows() {
		final List<StreamDefinition> collect = new ArrayList<>();

		streamDeployingService.getDeployedStreamsMetaInfo().forEach((k, v) -> {
			final String streamName = k.getFirst().substring(0, k.getFirst().indexOf("_"));
			final String streamId = k.getFirst().substring(k.getFirst().indexOf("_") + 1);

			final StreamDefinition data = new StreamDefinition();
			data.setStreamName(streamName);
			data.setStreamId(streamId);

			final boolean flowStarted = streamDeployingService.isStreamStarted(streamId);
			final boolean flowDeployed = streamDeployingService.isStreamDeployed(k.getFirst());
			if (flowStarted) {
				data.setStreamStatus(STARTED);
			} else if (flowDeployed) {
				data.setStreamStatus(DEPLOYED);
			} else {
				data.setStreamStatus(UNDEPLOYED);
			}

			final LinkedList<StreamComponentInfo> components = StreamComponentOptionsParser.getComponentsFromInstances(v);
			data.setComponents(components);
			data.setDefinitions(v);
			collect.add(data);
		});

		return new ResponseEntity<>(collect, HttpStatus.OK);
	}

	@PostMapping("/deploy")
	public ResponseEntity<StatusRegistration> deployStream(@RequestBody DefinitionDeployModel definitionDeployModel) {
		final String uuid = UUID.randomUUID().toString();
		return streamDeployingService.deployStream(uuid, definitionDeployModel);
	}

	@GetMapping("/{streamId}/start")
	public ResponseEntity<StatusRegistration> startStream(@PathVariable String streamId) {
		return streamDeployingService.startStream(streamId);
	}

	@GetMapping("/{streamId}/stop")
	public ResponseEntity<StatusRegistration> stopStream(@PathVariable String streamId) {
		return streamDeployingService.stopStream(streamId);
	}

	@GetMapping("/{streamId}/undeploy")
	public ResponseEntity<StatusRegistration> undeployStream(@PathVariable String streamId) {
		final ConcurrentMap<Pair<String, StreamInfo>, LinkedList<AbstractDefinition<?>>> flowDefinitions = streamDeployingService.getDeployedStreamsMetaInfo();
		final Optional<Pair<String, StreamInfo>> key = flowDefinitions.keySet().stream().filter(id -> id.getFirst().contains(streamId)).findFirst();
		if (!key.isPresent()) {
			return new ResponseEntity<>(new StatusRegistration(ERROR, String.format("Stream definition with name [%s] not found!", streamId)), HttpStatus.NOT_FOUND);
		}

		return streamDeployingService.undeployStream(streamId, flowDefinitions.get(key.get()));
	}

	@GetMapping("/{streamId}/metrics")
	public ResponseEntity<?> getMetrics(@PathVariable String streamId) {
		final List<ChannelStats> metrcisData = monitoringService.getMetrcisData(streamId);
		if (CollectionUtils.isEmpty(metrcisData)) {
			return new ResponseEntity<>(Collections.emptyList(), HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(metrcisData, HttpStatus.OK);
	}
}

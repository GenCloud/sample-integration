package org.genfork.integration.controller;

import org.genfork.integration.model.response.StreamComponentInfo;
import org.genfork.integration.model.response.paged.DataModel;
import org.genfork.integration.stream.StreamComponentRegistrar;
import org.genfork.integration.stream.annotation.StreamComponent;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@RestController
@RequestMapping("/components")
public class ComponentsController {
	@GetMapping
	public DataModel<Collection<StreamComponentInfo>> list(Pageable pageable,
														   @RequestParam(value = "type", required = false) StreamComponent.ComponentType type,
														   @RequestParam(required = false) String search) {
		final Collection<StreamComponentInfo> values = StreamComponentRegistrar.getStreamComponentInfos().values()
				.stream()
				.flatMap(m ->
						m.values().stream())
				.filter(m ->
						!m.isSubComponent())
				.collect(Collectors.toList());

		final List<StreamComponentInfo> items = values
				.stream()
				.filter(ar -> type == null || ar.getComponentType() == type)
				.filter(ar -> !StringUtils.hasText(search) || ar.getComponentName().contains(search))
				.sorted(Comparator.comparing(StreamComponentInfo::getComponentType))
				.collect(Collectors.toList());

		return DataModel.of(pageable, items);
	}

	@GetMapping("/{type}/{name}")
	public ResponseEntity<StreamComponentInfo> info(@PathVariable("type") StreamComponent.ComponentType type,
													@PathVariable("name") String name) {
		final StreamComponentInfo info = StreamComponentRegistrar.getStreamComponentInfo(name, type);
		if (info != null) {
			return ResponseEntity.ok(info);
		}

		return ResponseEntity.notFound().build();
	}
}

package org.genfork.integration.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComponentOption implements Serializable {
	private String id;
	private String name;
	private String type;
	private String description;
	private String shortDescription;
	private Object defaultValue;
	private Hints hints;
	private String deprecation;
	private String sourceType;
	private String sourceMethod;
	private boolean deprecated;

	private boolean inner;

	private StreamComponentInfo innerObject;

	@Data
	public static class Hints implements Serializable {
		private List<String> keyHints;
		private List<String> keyProviders;
		private List<String> valueHints;
		private List<String> valueProviders;
	}
}

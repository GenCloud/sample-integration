package org.genfork.integration.model.response.paged;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
@RequiredArgsConstructor
public class Page implements Serializable {
	private final int totalPages;
	private final int totalElements;
	private final int number;
	private final int size;
}

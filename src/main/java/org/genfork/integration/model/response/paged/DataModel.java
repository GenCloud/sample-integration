package org.genfork.integration.model.response.paged;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
@RequiredArgsConstructor
public class DataModel<O> implements Serializable {
	private final O items;
	private final Page page;

	public static <I> DataModel<Collection<I>> of(Pageable pageable, List<I> collection) {
		final long count = collection.size();
		final int pageSize = pageable.getPageSize();
		final long to = Math.min(count, pageable.getOffset() + pageSize);

		int offset = 0;
		int page = 0;
		if (pageable.getOffset() <= to) {
			offset = (int) pageable.getOffset();
			page = pageable.getPageNumber();
		} else if (pageable.getOffset() + pageSize <= to) {
			offset = (int) pageable.getOffset();
		}

		final int allElementsSize = collection.size();
		return new DataModel<>(collection.subList(offset, (int) to), new Page(allElementsSize / pageSize, allElementsSize, page, pageSize));
	}
}

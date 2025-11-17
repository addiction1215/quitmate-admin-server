package com.quitmate.global.page.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Getter
@NoArgsConstructor
@SuperBuilder
public class PageInfoServiceRequest {

	private int page;
	private int size;

	public Pageable toPageable() {
		return PageRequest.of(page - 1, size);
	}
}

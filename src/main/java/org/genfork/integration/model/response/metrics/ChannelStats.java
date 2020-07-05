package org.genfork.integration.model.response.metrics;

import lombok.Data;
import org.springframework.integration.support.management.Statistics;

import java.io.Serializable;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
public class ChannelStats implements Serializable {
	private String streamId;
	private String channelName;

	private Statistics sendDuration;

	private double maxSendDuration;
	private double minSendDuration;

	private double meanSendDuration;
	private double meanSendRate;

	private long sendCount;
	private long sendErrorCount;

	private Statistics errorRate;
	private double meanErrorRate;
	private double meanErrorRatio;
}

package org.genfork.integration.service.monitoring;

import org.genfork.integration.model.response.metrics.ChannelStats;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.support.management.Statistics;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Component
public class MonitoringService {
	private final ConcurrentMap<String, List<AbstractMessageChannel>> metrics = new ConcurrentHashMap<>();

	public Map<String, List<ChannelStats>> getAllMetricsData() {
		final Map<String, List<ChannelStats>> stats = new HashMap<>();

		metrics.forEach((streamId, list) -> stats.put(streamId, list
				.stream()
				.map(this::map)
				.peek(ch -> ch.setStreamId(streamId))
				.collect(Collectors.toList())));

		return stats;
	}

	public List<ChannelStats> getMetrcisData(String streamId) {
		final List<AbstractMessageChannel> channels = metrics.get(streamId);
		if (channels != null) {
			return channels
					.stream()
					.map(this::map)
					.peek(ch -> ch.setStreamId(streamId))
					.collect(Collectors.toList());
		}

		return Collections.emptyList();
	}

	private ChannelStats map(AbstractMessageChannel ch) {
		final Statistics sendDuration = ch.getSendDuration();

		final double maxSendDuration = ch.getMaxSendDuration();
		final double minSendDuration = ch.getMinSendDuration();

		final double meanSendDuration = ch.getMeanSendDuration();
		final double meanSendRate = ch.getMeanSendRate();

		final long sendCount = ch.getSendCountLong();

		final Statistics errorRate = ch.getErrorRate();

		final long sendErrorCount = ch.getSendErrorCountLong();

		final double meanErrorRate = ch.getMeanErrorRate();
		final double meanErrorRatio = ch.getMeanErrorRatio();

		final String fullChannelName = ch.getFullChannelName();

		final ChannelStats stats = new ChannelStats();
		stats.setChannelName(fullChannelName);

		stats.setSendDuration(sendDuration);
		stats.setMaxSendDuration(maxSendDuration);
		stats.setMinSendDuration(minSendDuration);

		stats.setMeanSendDuration(meanSendDuration);
		stats.setMeanSendRate(meanSendRate);

		stats.setSendCount(sendCount);

		stats.setErrorRate(errorRate);

		stats.setSendErrorCount(sendErrorCount);

		stats.setMeanErrorRate(meanErrorRate);
		stats.setMeanErrorRatio(meanErrorRatio);

		return stats;
	}

	public void add(String streamId, List<AbstractMessageChannel> channelDefinitions) {
		if (!metrics.containsKey(streamId)) {
			metrics.put(streamId, channelDefinitions);
		}
	}

	public void remove(String streamId) {
		metrics.remove(streamId);
	}
}

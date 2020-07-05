package org.genfork.integration.stream.converters.impl;

import org.genfork.integration.stream.converters.AbstractConverter;

import java.net.InetSocketAddress;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
public class InetSocketAddressConverter extends AbstractConverter<String, InetSocketAddress> {
	@Override
	public InetSocketAddress convert(Class<InetSocketAddress> resultType, String value) {
		try {
			final String[] params = value.split(":");
			return InetSocketAddress.createUnresolved(params[0], Integer.parseInt(params[1]));
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	@Override
	public String reverseConvert(Class<String> resultType, InetSocketAddress value) {
		return value.getHostString();
	}
}

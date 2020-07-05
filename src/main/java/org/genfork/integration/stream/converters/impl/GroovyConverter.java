package org.genfork.integration.stream.converters.impl;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.genfork.integration.stream.converters.AbstractConverter;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.UUID;

import static groovy.lang.GroovyShell.DEFAULT_CODE_BASE;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
public class GroovyConverter extends AbstractConverter<String, Script> {
	private static GroovyClassLoader GROOVY_CLASS_LOADER;

	@Override
	public Script convert(Class<Script> resultType, String value) {
		if (GROOVY_CLASS_LOADER == null) {
			GROOVY_CLASS_LOADER = AccessController.doPrivileged((PrivilegedAction<GroovyClassLoader>) () ->
					new GroovyClassLoader(getClass().getClassLoader(), CompilerConfiguration.DEFAULT));
		}

		final String scriptName = String.format("%s%s", "GroovyScript", UUID.randomUUID().toString().replaceAll("-", ""));

		final GroovyCodeSource gcs = AccessController.doPrivileged((PrivilegedAction<GroovyCodeSource>) () ->
				new GroovyCodeSource(value, scriptName, DEFAULT_CODE_BASE));

		final Class<?> groovyClass = GROOVY_CLASS_LOADER.parseClass(gcs, false);
		return InvokerHelper.createScript(groovyClass, new Binding());
	}

	@Override
	public String reverseConvert(Class<String> resultType, Script value) {
		return value.toString();
	}
}

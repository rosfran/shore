package com.codahale.shore;

import static com.google.common.base.Preconditions.*;

import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.Logger;

import net.jcip.annotations.Immutable;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.codahale.shore.modules.HibernateModule;
import com.codahale.shore.util.PerformanceRequestLog;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.wideplay.warp.persist.SessionFilter;

/**
 * Starts a new Jetty server and runs a Shore application.
 * 
 * @author coda
 *
 */
@Immutable
public class ServerCommand implements Runnable {
	private static final int GRACEFUL_SHUTDOWN_PERIOD = 5000; //ms
	private static final Logger LOGGER = Logger.getLogger(ServerCommand.class.getCanonicalName());
	private final AbstractConfiguration configuration;
	private final int port;
	private final boolean gracefulShutdown;
	private final Properties properties;
	
	/**
	 * Creates a new {@link ServerCommand}.
	 * 
	 * @param configuration
	 *            the application's configuration
	 * @param port
	 *            the port to listen on
	 * @param properties
	 *            the connection properties
	 */
	public ServerCommand(AbstractConfiguration configuration, int port, boolean gracefulShutdown, Properties properties) {
		this.configuration = checkNotNull(configuration);
		this.port = port;
		this.gracefulShutdown = gracefulShutdown;
		this.properties = checkNotNull(properties);
	}
	
	public AbstractConfiguration getConfiguration() {
		return configuration;
	}
	
	public int getPort() {
		return port;
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	public boolean getGracefulShutdown() {
		return gracefulShutdown;
	}

	@Override
	public void run() {
		final Server server = new Server();
		configuration.configure();
		server.addConnector(buildConnector());
		server.setHandler(buildContext(buildServletHolder()));
		server.setSendServerVersion(false);
		server.setGracefulShutdown(GRACEFUL_SHUTDOWN_PERIOD);
		server.setStopAtShutdown(gracefulShutdown);
		
		server.addBean(buildRequestLog());
		
		
		
		configuration.configureServer(server);
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private RequestLogHandler buildRequestLog() {
		final RequestLogHandler handler = new RequestLogHandler();
		handler.setRequestLog(new PerformanceRequestLog());
		return handler;
	}

	private Connector buildConnector() {
		final Connector connector = configuration.getConnector();
		connector.setPort(port);
		return connector;
	}

	private ServletContextHandler buildContext(ServletHolder servletHolder) {
		final ServletContextHandler root = new ServletContextHandler();
		root.setContextPath("/");
		root.addServlet(servletHolder, "/*");
		for (Entry<FilterHolder, String> filter : configuration.getServletFilters().entrySet()) {
			root.addFilter(filter.getKey(), filter.getValue(), FilterMapping.DEFAULT);
		}
		root.addFilter(SessionFilter.class, "/*", FilterMapping.DEFAULT);
		configuration.configureContext(root);
		return root;
	}

	private ServletHolder buildServletHolder() {
		final ServletHolder servletHolder = new ServletHolder(new GuiceContainer(buildInjector()));
		servletHolder.setInitParameter("com.sun.jersey.config.property.packages", getResourcePackages());
		LOGGER.info("Configured resource packages: " + configuration.getResourcePackages());
		return servletHolder;
	}

	private String getResourcePackages() {
		return Joiner.on(";").join(Iterables.transform(
			configuration.getResourcePackages(),
			Functions.toStringFunction()
		));
	}

	private Injector buildInjector() {
		return Guice.createInjector(
			configuration.getStage(),
			Iterables.concat(
				configuration.getModules(),
				ImmutableList.of(buildHibernateModule())
			)
		);
	}

	private Module buildHibernateModule() {
		return new HibernateModule(LOGGER, properties, configuration.getEntityPackages());
	}
}

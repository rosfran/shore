package com.codahale.shore.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.servlets.QoSFilter;
import org.eclipse.jetty.servlets.WelcomeFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import com.codahale.shore.AbstractConfiguration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Stage;

@RunWith(Enclosed.class)
public class AbstractConfigurationTest {
	private static class MockModule extends AbstractModule {
		@Override
		protected void configure() {
			
		}
	}
	private static Module MOCK_MODULE = new MockModule();
	
	private static class MockConfiguration extends AbstractConfiguration {
		@Override
		public String getExecutableName() {
			return "dingo";
		}
		
		@Override
		protected void configure() {
			addEntityPackage("com.codahale.shore.test");
			addResourcePackage("com.codahale.shore.test");
			addModule(MOCK_MODULE);
			setStage(Stage.PRODUCTION);
			addServletFilter(GzipFilter.class, "/*");
			addServletFilter(QoSFilter.class, "/hard-to-serve/*");
			addServletFilter(new FilterHolder(WelcomeFilter.class), "/welcome");
		}
		
		public void doConfig() {
			configure();
		}
		
		@Override
		protected void configureContext(ServletContextHandler context) {
			context.setAllowNullPathInfo(true);
		}
		
		@Override
		protected void configureServer(Server server) {
			server.setGracefulShutdown(20);
		}
	}
	
	public static class A_Configuration {
		private MockConfiguration config;
		
		@Before
		public void setup() throws Exception {
			this.config = new MockConfiguration();
			config.doConfig();
		}
		
		@Test
		public void itHasAListOfEntityPackages() throws Exception {
			assertThat(
				config.getEntityPackages(),
				hasItem("com.codahale.shore.test")
			);
		}
		
		@Test
		public void itHasAListOfResourcePackages() throws Exception {
			assertThat(
				config.getResourcePackages(),
				hasItem("com.codahale.shore.test")
			);
		}
		
		@Test
		public void itHasGuiceModules() throws Exception {
			assertThat(
				config.getModules(),
				hasItem(MOCK_MODULE)
			);
		}
		
		@Test
		public void itHasAGuiceStage() throws Exception {
			assertThat(config.getStage(), is(Stage.PRODUCTION));
		}
		
		@Test
		public void itHasAConnector() throws Exception {
			assertThat(config.getConnector(), is(SocketConnector.class));
		}
		
		@Test
		public void itHasServletFiltersInOrderOfAddition() throws Exception {
			final List<String> filters = Lists.newLinkedList();
			final List<String> patterns = Lists.newLinkedList();
			for (Entry<FilterHolder, String> entry : config.getServletFilters().entrySet()) {
				filters.add(entry.getKey().getHeldClass().getName());
				patterns.add(entry.getValue());
			}
			
			final List<String> expectedFilterClasses = ImmutableList.of("org.eclipse.jetty.servlets.GzipFilter", "org.eclipse.jetty.servlets.QoSFilter", "org.eclipse.jetty.servlets.WelcomeFilter");
			assertThat(filters, is(expectedFilterClasses));
			
			final List<String> expectedUrlPatterns = ImmutableList.of("/*", "/hard-to-serve/*", "/welcome");
			assertThat(patterns, is(expectedUrlPatterns));
		}
		
		@Test
		public void itConfiguresAJettyServer() throws Exception {
			final Server server = mock(Server.class);
			
			config.configureServer(server);
			
			verify(server).setGracefulShutdown(20);
		}
		
		@Test
		public void itConfiguresAJettyContext() throws Exception {
			final ServletContextHandler context = mock(ServletContextHandler.class);
			
			config.configureContext(context);
			
			verify(context).setAllowNullPathInfo(true);
		}
	}
}

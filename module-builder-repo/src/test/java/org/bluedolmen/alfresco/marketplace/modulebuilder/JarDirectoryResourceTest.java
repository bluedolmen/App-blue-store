package org.bluedolmen.alfresco.marketplace.modulebuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class JarDirectoryResourceTest {
	private static final String JAR_FILENAME = "module.jar";
	private static final String MODULE_YML_PATH = "module/META-INF/module.yml.ftl";
	private static final ClassLoader classLoader = new URLClassLoader(
			new URL[]{
					JarDirectoryResourceTest.class.getClassLoader().getResource(JAR_FILENAME)	
			}, 
			JarDirectoryResourceTest.class.getClassLoader()
	);
	private static final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);

	@Test
	public void testClassLoaderResourceLoading() throws IOException {
		
		final URL moduleURL = classLoader.getResource(MODULE_YML_PATH);		
		Assert.assertNotNull("Cannot get a resource with the only class-loader", moduleURL);
		
	}
	
	@Test
	public void testResolverLoading() throws IOException {
		
		final Resource moduleResource = resolver.getResource(MODULE_YML_PATH);		
		Assert.assertNotNull("Cannot get a resource with the resource resolver", moduleResource);
	
	}
	
	@Test
	public void testResolverStarLoading() throws IOException {
		
		final Resource[] resources = resolver.getResources("classpath:" + "module/**/*.ftl");
		Assert.assertFalse("Cannot retrieve any ftl resource using a resolver", resources.length == 0);
		
		for (Resource resource : resources) {
			System.out.println(resource.getURL());
		}
			
	}
	
	@Test
	public void testJarEntryIsDirectory() throws IOException {
		
		final Resource dirResource = resolver.getResource("module/META-INF/");
		final URI resourceURI = dirResource.getURI(); 
		Assert.assertTrue(resourceURI.toString().endsWith("/"));
		
		final Resource[] resources = resolver.getResources("classpath:" + "module/**/*");
		for (final Resource resource : resources) {
			if (!resource.getFilename().contains(".")) {
				Assert.assertTrue(resourceURI.toString().endsWith("/"));
			}
		}
		
	}
	

}
package org.bluedolmen.alfresco.marketplace.modulebuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.repo.template.FreeMarkerProcessor;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class ModuleGenerator {
	
	private static final Log logger = LogFactory.getLog(ModuleGenerator.class);
	public static final String MODULE_TEMPLATE_BASE_DIR = "alfresco/templates/bluedolmen/marketplace/module";
	private static final String FTL_EXTENSION = ".ftl";
	
	public static class ModuleGeneratorException extends Exception {

		private static final long serialVersionUID = 1L;
		
		public ModuleGeneratorException(Throwable t) {
			super(t);
		}
		
		public ModuleGeneratorException(String message, Throwable t) {
			super(message, t);
		}
		
		public ModuleGeneratorException(String message) {
			super(message);
		}
		
	}
	
	public static final class ModelParsingException extends ModuleGeneratorException {
		
		private static final long serialVersionUID = 1L;

		public ModelParsingException(String message) {
			super(message);
		}
		
	}
	
	public static interface ModuleGeneratorEvent {
		
		String getMessage();
		
	};
	
	public static final class GeneratingFileEvent implements ModuleGeneratorEvent {
		
		public static enum State {
			PREPARING,
			STARTED,
			ENDED
		}
		
		private final String filePath;
		private State state;
		private long durationInMs = -1;
		private long startedTime;
		
		public GeneratingFileEvent(final String filePath) {
			this.filePath = filePath;
			this.state = State.PREPARING;
		}
		
		public synchronized GeneratingFileEvent start() {
			state = State.STARTED;
			startedTime = System.currentTimeMillis();
			return this;
		}
		
		public synchronized GeneratingFileEvent end() {
			state = State.ENDED;
			durationInMs = System.currentTimeMillis() - startedTime;
			return this;
		}
		
		@Override
		public String getMessage() {
			return "Generating file '" + filePath + "' [" + state.name() + "]";
		}
		
		public State getState() {
			return state;
		}
		
		public long getDurationInMs() {
			return durationInMs;
		}
		
	}
	
	public File generate(Map<String, Object> model, File outputFile) throws ModuleGeneratorException {
		
		final Worker worker = new Worker(model);
		worker.setOutputFile(outputFile);
		
		return worker.generate();
		
	}
	
	private FreeMarkerProcessor freeMarkerProcessor;
	private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
	private final Pattern VARIABLE_PATTERN = Pattern.compile("%(\\p{Alnum}+)%"); 
	
	
	protected class Worker extends Observable {
		
		private final Map<String, Object> model;
		private final String typeName;
		
		private File workingDir;
		private Path workingDirPath;
		private File outputFile;
		private JarOutputStream jarStream;
		
		protected Worker(final Map<String, Object> model) throws ModelParsingException {
			this.model = model;
			this.typeName = (String) model.get("typeName");
			validateJSONModel(model);
		}
		
		/**
		 * Set the output-file to be generated.
		 * If not provided, a temporary file will be generated instead.
		 * 
		 * @param outputFile
		 */
		protected final void setOutputFile(File outputFile) {
			this.outputFile = outputFile;
		}
		
		public File generate() throws ModuleGeneratorException {
			
			try {
				return generateImpl();
			}
			catch (ModuleGeneratorException e) {
				throw e;
			}
			catch (Exception e) {
				throw new ModuleGeneratorException(e);
			}
			
		}
		
		protected File generateImpl() throws ModuleGeneratorException, IOException {
			
			createWorkingDirectory();
			generateModuleStructure();
			return generateJarFile();
			
		}
		
		protected final void createWorkingDirectory() throws ModuleGeneratorException, IOException {
			
			workingDirPath = Files.createTempDirectory(TempFileProvider.getTempDir().toPath(), typeName, new FileAttribute<?>[]{});
			workingDir = workingDirPath.toFile();
			if (null == workingDir || !workingDir.exists()) {
				throw new ModuleGeneratorException("Cannot create a valid working directory to build the module.");
			}
			
		}
		
		protected void generateModuleStructure() throws IOException, ModuleGeneratorException {
			
			generateTemplateFiles();
			copyOtherResources();
			copyLogo();
			
		}
		
		protected void generateTemplateFiles() throws IOException, ModuleGeneratorException {

			final Resource[] resources = resourceResolver.getResources("classpath:" + MODULE_TEMPLATE_BASE_DIR + "/**/*" + FTL_EXTENSION);
			if (null == resources || 0 == resources.length) {
				throw new ModuleGeneratorException("Cannot find the module-template base directory '" + MODULE_TEMPLATE_BASE_DIR + "' as a classpath resource");
			}
			
			for (final Resource resource : resources) {
			
				final String relativePath = resolveToRelativePath(resource);
				
				final GeneratingFileEvent event = new GeneratingFileEvent(relativePath);
				notifyObservers(event.start());
				
				final Path fileOutputPath = workingDir.toPath().resolve(
						resolveVariables(
								relativePath.replace(FTL_EXTENSION, "")
						)
				);
				
				// Ensure the parent directory exists
				final Path parentDirPath = fileOutputPath.getParent();
				parentDirPath.toFile().mkdirs();
				
				final File outputFile = fileOutputPath.toFile();
				final FileWriter writer = new FileWriter(outputFile);
				
				final String classpathResource = MODULE_TEMPLATE_BASE_DIR + "/" + relativePath;
				generateTemplate(classpathResource, writer);
				
				notifyObservers(event.end());
				
			}			
			
		}
		
		protected void copyLogo() throws IOException {
			
			final File logo = (File) model.get("logo");
			if (null == logo) return;
			
			final String fileName = logo.getName();
			if (!fileName.endsWith(".png")) {
				logger.warn("The logo file is not a png file, ignoring");
				return;
			}
			
			final File metaInfDir = getMetaInfDirectory();
			final File targetFile = new File(metaInfDir, "logo.png"); 
			
			FileOutputStream fos = null;
			FileInputStream fis = null;
			
			try {
				
				fis = new FileInputStream(logo);
				fos = new FileOutputStream(targetFile); // throws FileNotFoundException
				
				IOUtils.copy(fis, fos);
				
			}
			finally {
				IOUtils.closeQuietly(fis);
				IOUtils.closeQuietly(fos);
			}
			
		}
		
		protected File getMetaInfDirectory() {
			
			final File metaInfDir = new File(workingDirPath + "/META-INF");
			if (!metaInfDir.exists()) {
				metaInfDir.mkdirs();
			}
			
			return metaInfDir;
			
		}
		
		protected void copyOtherResources() throws IOException, ModuleGeneratorException {
			
			final Resource[] resources = resourceResolver.getResources("classpath:" + MODULE_TEMPLATE_BASE_DIR + "/**/*");
			for (final Resource resource : resources) {
				
				final String fileName = resource.getFilename();
				if (null == fileName || fileName.isEmpty() || fileName.endsWith(FTL_EXTENSION)) continue;
				
				final File inputFile = resource.getFile();
				if (null == inputFile) {
					logger.warn(
						String.format("Resource '%s' is not deserved through the file-system. The generation may fail due to incorrect recognition of directory paths.", resource.toString())
					);
					if (resource.getURI().toString().endsWith("/")) continue;
				}
				else {
					if (inputFile.isDirectory()) continue;
				}
				
				final String relativePath = resolveToRelativePath(resource);
				final Path fileOutputPath = workingDir.toPath().resolve(
					resolveVariables(relativePath) // we suppose we may have variables here but this is unlikely
				);
				
				// Ensure the parent directory exists
				final Path parentDirPath = fileOutputPath.getParent();
				parentDirPath.toFile().mkdirs();
								
				final File outputFile = fileOutputPath.toFile();
				
				InputStream fileInput = null;
				final OutputStream fileOutput = new FileOutputStream(outputFile);
				try {
					fileInput = resource.getInputStream();
					IOUtils.copy(fileInput, fileOutput);
				}
				finally {
					IOUtils.closeQuietly(fileInput);
					IOUtils.closeQuietly(fileOutput);
				}
				
			}
			
			@SuppressWarnings("unchecked")
			final List<Map<String, Object>> extraResources = (List<Map<String, Object>>) model.get("resources");
			if (null == extraResources || extraResources.isEmpty()) return;
			
			for (Map<String, Object> extraResource : extraResources) {
				
				final String fileName = (String) extraResource.get("name");
				final String mapping = (String) extraResource.get("mapping");
				final File srcFile = (File) extraResource.get("file");
				
				if (null == srcFile || !srcFile.exists()) {
					throw new ModuleGeneratorException(
						String.format("The file '%s' is not mapped with a valid file. A problem may occur with resources uploaded early.", fileName)
					);
				}
				
				final String relativePath = (StringUtils.isBlank(mapping) ? "META-INF/" : mapping + "/" ) + fileName;
				final Path fileOutputPath = workingDir.toPath().resolve(relativePath);
				
				extraResource.put("path", relativePath);
				
				// Ensure the parent directory exists
				final Path parentDirPath = fileOutputPath.getParent();
				parentDirPath.toFile().mkdirs();
								
				final File destFile = fileOutputPath.toFile();
				
				FileUtils.copyFile(srcFile, destFile);
				
			}
			
		}
		
		protected String resolveToRelativePath(Resource resource) throws IOException {
			
			String path = resource.getURI().getPath(); // We do not want '%' signs to be escaped, that why we use getURI()
			if (null == path) {
				throw new UnsupportedOperationException("Cannot generate the module for resources non deserved through the file-system.");
			}
			
			final int index = path.indexOf(MODULE_TEMPLATE_BASE_DIR);
			if (-1 != index) {
				path = path.substring(index + MODULE_TEMPLATE_BASE_DIR.length());
				if (path.startsWith("/")) {
					path = path.substring(1);
				}
			}
			
			return path;
						
		}
		
		/**
		 * Resolve variables matching the following pattern:
		 * <code>%variable%</code> using the internal model
		 * 
		 * @param value
		 * @return
		 * @throws ModuleGeneratorException
		 */
		protected String resolveVariables(String value) throws ModuleGeneratorException {
			
			// Resolve potential variable-names towards the given model
			
			final Matcher matcher = VARIABLE_PATTERN.matcher(value);
			final StringBuffer buffer = new StringBuffer();
			while (matcher.find()) {
				final String variableName = matcher.group(1);
				final String resolvedVariable = (String) model.get(variableName);
				if (null == resolvedVariable) {
					throw new ModuleGeneratorException(String.format("Cannot resolve variable '%s' on expression '%s'", variableName, value));
				}
				matcher.appendReplacement(buffer, resolvedVariable);
			}
			matcher.appendTail(buffer);
			
			return buffer.toString();

		}
		
		private void generateTemplate(final String classpathResource, Writer writer) throws IOException {
			
			freeMarkerProcessor.process(classpathResource, model, writer);
			
		}
		
		protected File generateJarFile() throws IOException {
			
			if (null == outputFile) {
				outputFile = TempFileProvider.createTempFile(typeName, ".jar");
			}
			
			final FileOutputStream fos = new FileOutputStream(outputFile);
			
			try {
				
				final Manifest manifest = new Manifest();
				jarStream = new JarOutputStream(fos, manifest);
				
				/*
				 * Here we add entries by prioritizing the ones in the META-INF
				 * directory. This is a current pre-requisite for the modules.
				 */
				final File metaInfDir = getMETAINFEntry();
				if (null != metaInfDir) {
					addEntry(metaInfDir);
				}
				for (final File entry : getNonMETAINFEntries()) {
					addEntry(entry);
				}
				
			}
			finally {
				
				IOUtils.closeQuietly(jarStream);
				
			}
			
			return outputFile;
			
		}
		
		private File getMETAINFEntry() {
			
			final File[] matchingFiles = workingDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return "META-INF".equals(name);
				}
			});
			
			if (0 == matchingFiles.length) {
				return null;
			}
			
			final File metaInfDir = matchingFiles[0];
			if (!metaInfDir.isDirectory()) {
				throw new IllegalStateException("META-INF entry is not a directory as expected");
			}
			
			return metaInfDir;
			
		}
		
		private File[] getNonMETAINFEntries() {
			
			return workingDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return !"META-INF".equals(name);
				}
			});
			
		}
		
		// TODO: Rewrite as a terminal recursive method
		private void addEntry(File entry) throws IOException {
			
			if (entry.isDirectory()) {
				
				for (final File file : entry.listFiles()) {
					addEntry(file);
				}
				
			}
			else {
				addFileEntry(entry);
			}
			
		}
		
		private void addFileEntry(File entry) throws IOException {
			
			final Path entryPath = entry.toPath();
			final Path relativePath = workingDirPath.relativize(entryPath);
			final JarEntry jarEntry = new JarEntry(relativePath.toString());
			jarStream.putNextEntry(jarEntry);
			
			FileInputStream fileIS = null;			
			try {
				fileIS = new FileInputStream(entry);
				IOUtils.copy(fileIS, jarStream);
			}
			finally {
				IOUtils.closeQuietly(fileIS);
			}
			
		}
		
		
	}
	
	@SuppressWarnings("unchecked")
	protected static Map<String, Object> validateJSONModel(Map<?,?> json) throws ModelParsingException {
		
		final String typeName = (String) json.get("typeName");
		validateTypeName(typeName);
		
		validateFields(json);
		
		return (Map<String, Object>) json;
		
	}
	
	private static final Pattern TYPE_NAME_PATTERN = Pattern.compile("[A-Za-z]\\p{Alnum}*");
	
	protected static final void validateTypeName(String typeName) throws ModelParsingException {
		
		if (null == typeName || typeName.isEmpty()) {
			throw new ModelParsingException("The typeName is a mandatory and non-empty string.");
		}
		
		if (!TYPE_NAME_PATTERN.matcher(typeName).matches()) {
			throw new ModelParsingException("The typeName has to be an alpha-numeric string starting with a letter.");
		}
		
	}
	
	protected static final void validateFields(Map<?,?> json) throws ModelParsingException {
		
		final Object fields_ = json.get("fields");
		if (null == fields_) return; // no fields defined
		
		if (!(fields_ instanceof List<?>)) {
			throw new ModelParsingException("The 'fields' property has to be a valid JSON Array.");
		}
		final List<?> fields = (List<?>) fields_;
		
		final Iterator<?> iterator = fields.iterator();
		while (iterator.hasNext()) {
			
			final Object field_ = iterator.next();
			if (!(field_ instanceof Map<?,?>)) {
				throw new ModelParsingException("The 'fields' element have to be valid JSON Objects.");
			}
			
			final Map<?,?> field = (Map<?,?>) field_;
			validateField(field);
			
		}
		
	}
	
	private static final Pattern PROPERTY_NAME_PATTERN = Pattern.compile("[a-z]\\p{Alnum}*");
	
	protected static final void validateField(Map<?,?> field) throws ModelParsingException {
		
		final String fieldName = (String) field.get("name");
		if (null == fieldName || fieldName.isEmpty()) {
			throw new ModelParsingException("The 'fieldName' property is a mandatory property and has to be a non-empty string.");
		}
		
		if (!PROPERTY_NAME_PATTERN.matcher(fieldName).matches()) {
			throw new ModelParsingException("The 'fieldName' property has to be an alpha-numeric string starting with a lowercase letter.");
		}
	
		// Do not go further for the moment, the template will take care of
		// defaulting missing or incorrect values
		
	}
	
	public void setFreeMarkerProcessor(FreeMarkerProcessor freeMarkerProcessor) {
		this.freeMarkerProcessor = freeMarkerProcessor;
	}
	
}

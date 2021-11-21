package com.project.BatchConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.deployer.resource.docker.DockerResourceLoader;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.partition.DeployerPartitionHandler;
import org.springframework.cloud.task.batch.partition.DeployerStepExecutionHandler;
import org.springframework.cloud.task.batch.partition.PassThroughCommandLineArgsProvider;
import org.springframework.cloud.task.batch.partition.SimpleEnvironmentVariablesProvider;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;

import com.project.Constants.Constants;
import com.project.Customs.CustomProcessor;
import com.project.Customs.CustomReader;
import com.project.Customs.CustomWriter;
import com.project.Customs.JobExecutionListnerClass;
import com.project.Entity.User;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableTask
@EnableBatchProcessing
@Slf4j
public class BatchConfig {
	
	@Autowired
	private JobExecutionListnerClass jobExecutionListnerClass;
	
	@Autowired
	private JobRegistry jobRegistry;
	
	@Autowired
	private JobRepository jobRepository;
	
	@Autowired
	private ConfigurableApplicationContext context;
	
	@Autowired
	private TaskRepository taskRepository;
	
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	public StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	public DelegatingResourceLoader delegatingResourceLoader;
	
	@Autowired
	private CustomWriter customWriter;
	
	@Autowired
	private CustomProcessor customProcessor;
	
	@Value("${jarLocation}")
	public String jarLocation;
	
	@Value("${batch.job.jobname}")
	public String jobName;
	
	@Autowired
	@Qualifier("transactionManager")
	private PlatformTransactionManager platform;
	
	@Bean
	@Primary
	PlatformTransactionManager getTransactionManager() {
		return platform;
	}
	
	
	@Bean
	public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor() {
		JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
		postProcessor.setJobRegistry(jobRegistry);
		return postProcessor;
	}
	
	@Bean
	@Profile("worker")
	public DeployerStepExecutionHandler stepExecutionHandler(JobExplorer jobExplorer) {
		return new DeployerStepExecutionHandler(this.context, jobExplorer, this.jobRepository);
	}
	
	@Bean
	public DelegatingResourceLoader delegatingResourceLoader(MavenProperties mavenProperties) {
		if (Constants.MAVEN_PATH != null)
			mavenProperties.setLocalRepository(Constants.MAVEN_PATH);
		
		DockerResourceLoader dockerloader = new DockerResourceLoader();
		MavenResourceLoader mavenResourceLoader = new MavenResourceLoader(mavenProperties);
		Map<String, ResourceLoader> loaders = new HashMap<>();
		
		loaders.put("docker", dockerloader);
		loaders.put("maven", mavenResourceLoader);
		return new DelegatingResourceLoader(loaders);
		
	}
	
	
	public String[] fetchAllPrimaryKeys(String fileName) {
		String[] contents = null;
		
		try {
			File directoryPath = new File(fileName);
			contents = directoryPath.list();
			for(int i=0; i< contents.length ; i++) {
				if(contents[i].endsWith(".csv"))
					contents[i] = fileName + "/" + contents[i]; 
			}
		}catch(Exception e) {
			log.error(e.getMessage());
		}
		System.out.println("Content " + contents.length);
		return contents;
	}
	
	public List<List<String>> splitPayLoad(String[] array, int partitionSize){
		if(partitionSize <= 0)
			return null;
		
		int rest = array.length % partitionSize;
		int chunks = array.length / partitionSize + (rest > 0 ? 1 : 0);
		String[][] arrays = new String[chunks][];
		
		for(int i = 0; i < (rest > 0 ? chunks - 1 : chunks) ; i++) {
			arrays[i] = Arrays.copyOfRange(array, i * partitionSize, i * partitionSize + partitionSize);
		}
		
		
		if(rest > 0) {
			arrays[chunks -1] = Arrays.copyOfRange(array, (chunks -1) * partitionSize, (chunks -1) * partitionSize + rest);
		}
		
		
		List<List<String>> list = new ArrayList<>();
		for(String[] arr : arrays) {
			List<String> list2 = new ArrayList<>();
			for(String arr1 : arr) {
				list2.add(arr1);
			}
			list.add(list2);
		}
		return list;
	}
	
	@Bean
	public PartitionHandler partitionHandler(TaskLauncher taskLauncher, JobExplorer jobExplorer, Environment environment) {
		Resource resource = this.delegatingResourceLoader.getResource(jarLocation);
		
		DeployerPartitionHandler partitionHandler = new DeployerPartitionHandler(taskLauncher, jobExplorer, resource, "workerStep", taskRepository);
		List<String> commandLineArguments = new ArrayList<>(5);
		commandLineArguments.add("--spring.profiles.active=worker");
		commandLineArguments.add("--spring.cloud.task.initialize.enable=false");
		commandLineArguments.add("--spring.batch.initializer.enabled=false");
		commandLineArguments.add("--spring.cloud.task.closecontext_enabled=true");
		commandLineArguments.add("--logging.level=DEBUG");
		
		partitionHandler.setCommandLineArgsProvider(new PassThroughCommandLineArgsProvider(commandLineArguments));
		partitionHandler.setEnvironmentVariablesProvider(new SimpleEnvironmentVariablesProvider(environment));
		partitionHandler.setMaxWorkers(5);
		partitionHandler.setApplicationName("BatchApplicationWorker");
		return partitionHandler;
	}
	
	@Bean
	@StepScope
	public Partitioner partitioner(@Value("#{jobParameters['inputFiles']}") String file, @Value("#{jobParameters['partitionSize']}") String partitionSize1){
		int partitionSize = Integer.parseInt(partitionSize1);
		System.out.println("file : " + file);
		System.out.println("partitionSize : " + partitionSize);
		return new Partitioner() {
		
			public Map<String, ExecutionContext> partition(int gridSize) {
				Map<String, ExecutionContext> partitions = new HashMap<>();
				String[] ids = fetchAllPrimaryKeys(file);
				List<List<String>> partitionPayloads = splitPayLoad(ids, partitionSize);
				int size = partitionPayloads.size();
				for(int i = 0 ; i < size ; i++) {
					ExecutionContext executionContext = new ExecutionContext();
					executionContext.put("partitionNumber", i);
					executionContext.put("partitionPayLoad", new ArrayList<>(partitionPayloads.get(i)));
					partitions.put("partition" + i, executionContext);
				}
				return partitions;
			}
		};
	}
	
	
	@Bean
	public Step masterStep(Step workerStep, PartitionHandler partitionHandler) {
		return this.stepBuilderFactory.get("masterStep")
				.partitioner(workerStep.getName(), partitioner(null, null))
				.step(workerStep)
				.partitionHandler(partitionHandler)
				.build();
	}
	
	
	@Bean
	public Step workerStep() {
		return this.stepBuilderFactory.get("workerStep")
				.<User,User>chunk(10000)
				.reader(reader(null))
				.processor(customProcessor)
				.writer(customWriter)
				.build();
	}
	
	@Bean
	public Job batchJob(Step masterStep) {
		return this.jobBuilderFactory.get("batchJob").start(masterStep)
				.listener(jobExecutionListnerClass)
				.build();
	}
	
	
	@Bean
	@StepScope
	public CustomReader reader(@Value("#{stepExecutionContext['partitionPayLoad']}") List<String> payload){
		return new CustomReader(payload);
	}
	


}

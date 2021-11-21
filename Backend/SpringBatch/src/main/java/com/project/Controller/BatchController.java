package com.project.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.Entity.BatchJob;
import com.project.Entity.JobParams;
import com.project.Entity.JobScheduleDetails;
import com.project.Entity.WorkerNode;
import com.project.Service.BatchService;

@RestController
@RequestMapping("/batch/")
@CrossOrigin(origins = "http://localhost:4200")
public class BatchController {
	
	
	
	@Autowired
	private BatchService batchService;
	
	
	@PostMapping("/run")
	public ResponseEntity<Long> run(@RequestBody JobParams body) throws Exception{
		Long id = batchService.runJob(body);
		return new ResponseEntity<>(id,HttpStatus.OK);
	}
	
	@GetMapping("/restart")
	public ResponseEntity<Long> restart(@RequestParam Long jobId) throws Exception{
			Long restartId = batchService.restartJob(jobId);
			return new ResponseEntity<>(restartId, HttpStatus.OK);
	}
	
	@GetMapping("/stop")
	public ResponseEntity<Boolean> stopJob(@RequestParam Long ID) throws Exception {
		Boolean status = batchService.stopJob(ID);
		return new ResponseEntity<>(status, HttpStatus.OK);
	}
	
	
	@GetMapping("/getAllJobs")
	public ResponseEntity<List<BatchJob>> getAllJobs() throws Exception{
		List<BatchJob> allJobs = batchService.getAllJobs();
		return new ResponseEntity<>(allJobs, HttpStatus.OK);
	}
	
	
	@GetMapping("/getAllJobExecutions")
	public ResponseEntity<List<BatchJob>> getAllJobExecutions(@RequestParam Long jobInstanceID) throws Exception{
		List<BatchJob> allJobExecutions = batchService.getAllJobExecutions(jobInstanceID);
		return new ResponseEntity<>(allJobExecutions, HttpStatus.OK);
	}
	
	
	@GetMapping("/getAllWorkerNodes")
	public ResponseEntity<List<WorkerNode>> getAllWorkerNodes(@RequestParam Long jobExecutionID) throws Exception{
		List<WorkerNode> allWorkers = batchService.getAllWorkerNodes(jobExecutionID);
		return new ResponseEntity<>(allWorkers, HttpStatus.OK);
	}
	
	
	@GetMapping("/getWorkerLogs")
	public ResponseEntity<List<String>> getWorkerLogs(@RequestParam Long jobExecutionID,@RequestParam String partitionName) throws Exception{
		List<String> logs = batchService.getWorkerLogs(jobExecutionID, partitionName);
		return new ResponseEntity<>(logs, HttpStatus.OK);
	}
	
	
	@PostMapping("/schedule")
	public ResponseEntity<Boolean> setSchedule(@RequestBody JobScheduleDetails scheduleDetails) throws Exception {
		Boolean scheduleStatus = batchService.scheduleJob(scheduleDetails);
		return new ResponseEntity<>(scheduleStatus, HttpStatus.OK);
	}
	
	
	@GetMapping("/scheduledJobs")
	public ResponseEntity<List<JobScheduleDetails>> getScheduledJobs() throws Exception{
		List<JobScheduleDetails> listOfScheduledJobs = batchService.getScheduledJobs();
		return new ResponseEntity<>(listOfScheduledJobs, HttpStatus.OK);
	}
	
	
	@GetMapping("/unschedule")
	public ResponseEntity<Boolean> unscheduleJob(@RequestParam String jobName,@RequestParam String jobGroup) throws Exception {
		Boolean status = batchService.unscheduleJob(jobName, jobGroup);
		return new ResponseEntity<>(status, HttpStatus.OK);
	}
	
}

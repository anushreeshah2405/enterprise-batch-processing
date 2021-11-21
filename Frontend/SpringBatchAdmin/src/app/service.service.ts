import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable} from 'rxjs';
import { Job } from './Entities/Job';
import { map } from 'rxjs/operators';
import { JobExecution } from './Entities/JobExecution';
import { Steps } from './Entities/Steps';
import { jobParameters } from './Entities/jobParameters';
import { jobScheduler } from './Entities/jobScheduler';


@Injectable({
  providedIn: 'root'
})
export class ServiceService {

  constructor(private http: HttpClient) { }

  allJobsUrl = "http://localhost:8080/batch/getAllJobs";
  getAllJobs():Observable<Job[]>{
      return this.http.get<Job[]>(this.allJobsUrl);
  }

  allExecutions = "http://localhost:8080/batch/getAllJobExecutions?jobInstanceID=";
  getAllExecutions(jobInstanceID):Observable<JobExecution[]>{
    return this.http.get<JobExecution[]>(this.allExecutions+jobInstanceID);
  }

  allWorkers = "http://localhost:8080/batch/getAllWorkerNodes?jobExecutionID=";
  getAllWorkers(jobExecutionID):Observable<Steps[]>{
    return this.http.get<Steps[]>(this.allWorkers+jobExecutionID);
  }

  stopJobUrl = "http://localhost:8080/batch/stop?ID=";
  stopJob(jobId):Observable<boolean>{
    return this.http.get<boolean>(this.stopJobUrl+jobId);
  }

  restartJobUrl = "http://localhost:8080/batch/restart?jobId=";
  restartJob(jobId):Observable<boolean>{
    return this.http.get<boolean>(this.restartJobUrl+jobId);
  }

  startJobUrl = "http://localhost:8080/batch/run";
  startJob(jobParams:jobParameters):Observable<number>{
    return this.http.post<number>(this.startJobUrl, jobParams);
  }

  scheduleJobUrl = "http://localhost:8080/batch/schedule";
  scheduleJob(jobParams:jobScheduler):Observable<boolean>{
    return this.http.post<boolean>(this.scheduleJobUrl, jobParams);
  }

  scheduledJobsUrl = "http://localhost:8080/batch/scheduledJobs";
  getScheduled():Observable<[]>{
    return this.http.get<[]>(this.scheduledJobsUrl);
  }

  unscheduleJobUrl = "http://localhost:8080/batch/unschedule?";
  unscheduleJob(jobName,jobGroup):Observable<boolean>{
    let url = this.unscheduleJobUrl + "jobName=" + jobName + "&jobGroup=" + jobGroup;
    console.log(url);
    return this.http.get<boolean>(url);
  }


  getLogUrl = "http://localhost:8080/batch/getWorkerLogs?";
  getWorkerLog(jobExecutionID,partitionName):Observable<[]>{
    return this.http.get<[]>(this.getLogUrl + "jobExecutionID=" + jobExecutionID + "&partitionName=" + partitionName);
  }
}

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CreateJobComponent } from './create-job/create-job.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { JobDetailsComponent } from './job-details/job-details.component';
import { JobExecutionDetailsComponent } from './job-execution-details/job-execution-details.component';
import { ScheduledJobsComponent } from './scheduled-jobs/scheduled-jobs.component';

const routes: Routes = [
  {path:'', component:DashboardComponent, pathMatch:'full'},
  {path:'dashboard', redirectTo:''},
  {path:'schedule', component:ScheduledJobsComponent, pathMatch:'full'},
  {path:'create', component:CreateJobComponent, pathMatch:'full'},
  {path:'jobDetails/:jobID', component:JobDetailsComponent, pathMatch:'full'},
  {path:'jobDetails', redirectTo:''},
  {path:'jobExecution/:jobExecutionId', component:JobExecutionDetailsComponent, pathMatch:'full'},
  {path:'jobExecution', redirectTo:''},
  {path:'scheduledJobs', component:ScheduledJobsComponent, pathMatch:'full'},

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

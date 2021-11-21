import { Component, OnInit, ViewChild  } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ServiceService } from '../service.service';
import {MatTableDataSource} from '@angular/material/table';
import { map } from "rxjs/operators"; 
import {MatPaginator} from '@angular/material/paginator';
import { JobExecution } from '../Entities/JobExecution';
import { StopDialogComponent } from '../stop-dialog/stop-dialog.component';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'app-job-details',
  templateUrl: './job-details.component.html',
  styleUrls: ['./job-details.component.css']
})
export class JobDetailsComponent implements OnInit {
  jobInstanceId;
  listOfJobExecutions = [];
  dataSource;
  displayedColumns;
  emails =[];
  loading = true;

  @ViewChild(MatPaginator) paginator: MatPaginator;

  constructor(private route:ActivatedRoute, private _service:ServiceService, public dialog: MatDialog) { }

  ngOnInit(): void {
    let id = parseInt(this.route.snapshot.paramMap.get('jobID'));
    this.jobInstanceId = id;
    this.getJobExecutions(this.jobInstanceId).subscribe(_ => {this.dataSource = new MatTableDataSource<JobExecution>(this.listOfJobExecutions);
      this.loading = false;
      this.emails = this.listOfJobExecutions[0].jobParams.mailRecipients.split(",");
      if(this.emails.length == 1 && this.emails[0] == ''){
        this.emails = [];
      }    
      this.displayedColumns = ['jobID', 'createTime','endTime','duration', 'status','actions'];
      this.dataSource.paginator = this.paginator;  },
      (error) => {                            
        this.loading = false;
      }); 
  }

  getJobExecutions(id) {
    return this._service
    .getAllExecutions(id)
    .pipe(map(
      (users) => {
        this.listOfJobExecutions = users;
      }));
  }

  calculateDuration(startTime:any,endTime:any){
    startTime = new Date(startTime);
    endTime = new Date(endTime);
    let duration = new Date(0,0,0);
    var seconds = endTime.getTime() - startTime.getTime();
    duration.setMilliseconds(seconds);
    return duration;
    }

    stopJob(){
      let dialogRef =  this.dialog.open(StopDialogComponent);
      dialogRef.afterClosed().subscribe(result => {
        if(result == "true"){
          this._service.stopJob(this.jobInstanceId).subscribe(data => data);
          setTimeout(() => {
            this.ngOnInit();
          },1000)
        }
      })
  }
  

    restartJob(){
      this._service.restartJob(this.jobInstanceId).subscribe(data => data);
      setTimeout(() => {
        this.ngOnInit();
      },1000)
    }

}

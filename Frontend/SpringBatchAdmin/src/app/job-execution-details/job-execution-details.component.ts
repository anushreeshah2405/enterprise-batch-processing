import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ServiceService } from '../service.service';
import {MatTableDataSource} from '@angular/material/table';
import { map } from "rxjs/operators"; 
import {MatPaginator} from '@angular/material/paginator';
import { Steps } from '../Entities/Steps';
import {MatDialog} from '@angular/material/dialog';
import { SteplogsComponent } from './steplogs/steplogs.component';
import {Subscription} from "rxjs";
import { Message } from '@stomp/stompjs';
import {RxStompService} from "@stomp/ng2-stompjs";
import { Globals } from '../globals';
import { Status } from '../Entities/status';
import { Partition } from '../Entities/partition';



@Component({
  selector: 'app-job-execution-details',
  templateUrl: './job-execution-details.component.html',
  styleUrls: ['./job-execution-details.component.css']
})
export class JobExecutionDetailsComponent implements OnInit {
  jobExecutionId;
  listOfWorkers = []
  dataSource;
  class = [];
  displayedColumns = ['stepName', 'createTime','endTime','duration', 'readCount', 'writeCount', 'status','logs'];;
  topicSubscription: Subscription;
  constructor(private route:ActivatedRoute, private _service:ServiceService,public dialog: MatDialog, private rxStompService: RxStompService,private globals: Globals) { }
  @ViewChild(MatPaginator) paginator: MatPaginator;
  ngOnInit(): void {
    let id = parseInt(this.route.snapshot.paramMap.get('jobExecutionId'));
    this.jobExecutionId = id;
    this.getAllWorkers(this.jobExecutionId).subscribe(_ => {this.dataSource = new MatTableDataSource<Steps>(this.listOfWorkers);
      this.dataSource.paginator = this.paginator;  });  
      this.topicSubscription = this.rxStompService.watch("/topic/public").subscribe((message:Message) =>{
        let jsonMessage = JSON.parse(message.body)
        if(jsonMessage.message == 'started' && jsonMessage.partition == null){
          let ssta = new Status();
          ssta.jobExecutionId = jsonMessage.jobExecutionId;
          ssta.status = [];
          this.globals.jobExecutions.push(ssta);
        }else if(jsonMessage.message == 'completed' && jsonMessage.partition == null){
          this.globals.jobExecutions.pop();
        }else{
          let sss:[boolean, number];
          sss = this.isExists(jsonMessage.partition);
          if(jsonMessage.message == 'running' && !sss[0]){
            let p = new Partition();
            p.partitionName = jsonMessage.partition;
            p.time = new Date();
            this.globals.jobExecutions[0].status.push(p); 
          }else if(jsonMessage.message == 'running' && sss[0]){
            let l : number = sss[1];
            this.globals.jobExecutions[0].status[sss[1]].time = new Date();
          }
        } 
        this.getAllWorkers(this.jobExecutionId).subscribe(_ => {this.dataSource = new MatTableDataSource<Steps>(this.listOfWorkers);
          this.dataSource.paginator = this.paginator; 
         });
      })
  }

  isExists(name):[boolean, number]{    
    for(let i =0; i < this.globals.jobExecutions[0].status.length; i++){
      if(this.globals.jobExecutions[0].status[i].partitionName == name){
        return [true,i];
      }
    }
    return [false,-1];
  }
  

  getAllWorkers(id) {
    return this._service
    .getAllWorkers(id)
    .pipe(map(
      (users) => {
        this.listOfWorkers = users;
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

    openDialog(name) {
      let dialogRef =  this.dialog.open(SteplogsComponent, {
        data: {
          Id : this.jobExecutionId,
          Name : name
        }
      });
      dialogRef.afterClosed().subscribe(result => {
       console.log(result);
      })
      }


}

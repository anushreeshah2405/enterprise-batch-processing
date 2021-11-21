import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import {MatDialog} from '@angular/material/dialog';
import { jobParameters } from '../Entities/jobParameters';
import { jobScheduler } from '../Entities/jobScheduler';
import { ServiceService } from '../service.service';
import { EmalDialogComponent } from './emal-dialog/emal-dialog.component';
import {MatSnackBar} from '@angular/material/snack-bar';
import { Router } from '@angular/router';

@Component({
  selector: 'app-create-job',
  templateUrl: './create-job.component.html',
  styleUrls: ['./create-job.component.css']
})
export class CreateJobComponent implements OnInit {
  srcResult;
  mails : string[]= [];
  email = false;
  timeValue = '0';
  mailsList = "";
  displayEmail = false;
  constructor(public dialog: MatDialog, private _service:ServiceService, private _snackBar: MatSnackBar, private router:Router) { }
  jobParams:jobParameters;
  jobSchedule:jobScheduler;
  validate;
  

  createForm = new FormGroup({
    jobName : new FormControl('', Validators.required),
    jobDescription : new FormControl('', Validators.required),
    input: new FormControl('', Validators.required),
    size: new FormControl('', Validators.required),
    time: new FormControl("0"),
    jobGroup: new FormControl(' ', Validators.required),
    cron: new FormControl(' ', Validators.required)
  });

  emailForm = new FormGroup({
    email : new FormControl('', Validators.email)
  });

  ngOnInit(): void {
  }

  openDialog() {
  let dialogRef =  this.dialog.open(EmalDialogComponent, { disableClose: true });
  dialogRef.afterClosed().subscribe(result => {
   this.mails = result;
   console.log(result);
   
  })
  }

  input(){
    console.log(this.timeValue);
    if(this.timeValue == "1"){
      this.createForm.get('jobGroup').setValue('');
      this.createForm.get('cron').setValue('');
      }
      else if(this.timeValue == "0"){
        this.createForm.get('jobGroup').setValue(' ');
        this.createForm.get('cron').setValue(' ');
    }
  }

  submitJob(){
  if(this.mails.length > 0){
    for(let i = 0;i<this.mails.length;i++){
      if(i==0){
        this.mailsList = this.mails[0];
      }
      else{
        this.mailsList+= "," + this.mails[i];
      }
      
    }
    
  }

  if(this.timeValue == "0"){
    this.jobParams = new jobParameters();
    this.jobParams.inputSource = this.createForm.get('input').value.trim();
    this.jobParams.jobName = this.createForm.get('jobName').value.trim();
    this.jobParams.jobDesciption = this.createForm.get('jobDescription').value.trim();
    this.jobParams.partitionSize = this.createForm.get('size').value.trim();
    this.jobParams.mailRecipients = this.mailsList;
    this._service.startJob(this.jobParams).subscribe(
    );

    let snakbarRef = this._snackBar.open('Job Execution Started','View DashBoard', {
      duration: 5000
    });

    snakbarRef.onAction().subscribe(() =>{
        this.router.navigate(['/']);
    });
    console.log(this.jobParams);
    }else if (this.timeValue == "1") {
      this.jobSchedule = new jobScheduler();
      this.jobSchedule.inputSource = this.createForm.get('input').value.trim();
      this.jobSchedule.jobName = this.createForm.get('jobName').value.trim();
      this.jobSchedule.jobGroup = this.createForm.get('jobGroup').value.trim();
      this.jobSchedule.jobDesciption = this.createForm.get('jobDescription').value.trim();
      this.jobSchedule.partitionSize = this.createForm.get('size').value.trim();
      this.jobSchedule.cronExpression = this.createForm.get('cron').value.trim();
      this.jobSchedule.mailRecipients = this.mailsList;
      this._service.scheduleJob(this.jobSchedule).subscribe(data => {
        if(data == false){
          this.createForm.get('cron').setErrors(
            {'incorrect': true})
        }else if(data == true){
          let snakbarRef = this._snackBar.open('Job Scheduled Successfully!','View DashBoard', {
            duration: 5000
          });
      
          snakbarRef.onAction().subscribe(() =>{
              this.router.navigate(['/scheduledJobs']);
          });
        }
      });
    }
  }
}

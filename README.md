WorkManager 
===================================
WorkManager Basics

Worker: This is where you put the code for the actual work you want to perform in the background. You'll extend this class and override the doWork() method.
WorkRequest: This represents a request to do some work. You'll pass in your Worker as part of creating your WorkRequest. When making the WorkRequest you can also specify things like Constraints on when the Worker should run.
WorkManager: This class actually schedules your WorkRequest and makes it run. It schedules WorkRequests in a way that spreads out the load on system resources, while honoring the constraints you specify.




# factory-in-a-box
Code and Models for developing Factory in a Box LIT Artifact Call

The instruction manual consists of three parts: 1) Startup, 2) Operation, and 3) Shutdown.

## 1.  Starting up FIAB 
total required time around 5-10min
see also following video: https://youtu.be/iMeB6S-nrNo
1.  Open the box. 
1.	Fold out the input and output station.
1.	Connect the power cord, flip the powerswitch, and power on the two turntables (2x BrickPi), input station, output station, four plotters (6x EV3), MES (RaspberryPi 4), the laptop for configuring the processes, and the tablet for displaying process progress.
1.	Remove the transport safety pins on the four plotters (8 pins in total).
1.	Remove the pen caps (one per plotter).
1.	If the automatic booting of the software does not work:
    1.  Manually start the software on the input/output/plotters by using the buttons on the lego EV3.
    1.  Manually start the turntables by connecting from the laptop via SSH.
    1.  Manually start the MES on the RaspberryPi.
1.	Startup the browser on the RaspberryPi to display the machine and order status on the FIAB widescreen.
1.	Startup the Editor on the laptop.
1.	From the browser (or via the UAExpert OPC UA browser) signal each machine to reset (and thus become available).
 
## 2)	Operation 
see following video for multiple, parallel orders/processes: https://youtu.be/k5Hi9-SmJ58 
1.	In the editor, select the process template (see also following videos for a template based process https://youtu.be/HImjfzYdLLk or a process from scratch https://youtu.be/XpVqB53-rHE )
1.	In the template set the image to be printed in the desired colour.
1.	Choose a process name.
1.	Deploy the process to the shopfloor.
1.	Watch on the web interface how the order/process is handled and moved from machine to machine.
1.	The web front end displays the current status as well as the history of a process.

## 3)	Shutdown 
total required time around 5min
1.	Manually stop the software on the input/output/plotters by using the buttons on the lego EV3.
1.	Shutdown the BrickPis (Turntables) and MES via SSH from the laptop
1.	Shutdown the laptop and tablet.
1.	Insert safety pins to secure the plotter head (2 per Station).
1.	Put the pen caps on to avoid the pens drying out.
1.	Fold in Input station and output station (including EV3 device).
1.	Store away any power cable and network cable in the double bottom of the box.
1.	Close the box.

There is an additional video for showing how the wiring of machines (here turntables) works: https://youtu.be/F8t_qSALybs 

## 4)  UML
requires Papyrus standalone version equal or later 2020-06 to load the diagrams correctly (at all)

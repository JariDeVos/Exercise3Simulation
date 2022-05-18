import java.io.*;
import java.util.*;

public class Simulation {
    //information given in the case
    String inputFileName = "/Users/jaridevos/Documents/Simulation/Exercise3/strategies/input-S3-18.txt";
    int D = 6;                         // number of days per week (NOTE: Sunday not included! so do NOT use to calculate appointment waiting time)
    int amountOTSlotsPerDay = 10;      // number of overtime slots per day
    int S = 32 + amountOTSlotsPerDay;  // number of slots per day
    double slotLength = 15.0 / 60.0;             // duration of a slot (in hours)
    double lambdaElective = 28.345;
    double meanTardiness = 0;
    double stdevTardiness = 2.5;
    double probNoShow = 0.02;
    double meanElectiveDuration = 15;
    double stdevElectiveDuration = 3;
    double[] lambdaUrgent = {2.5, 1.25};
    double[] probUrgentType = {0.7, 0.1, 0.1, 0.05, 0.05};
    double[] cumulativeProbUrgentType = {0.7, 0.8, 0.9, 0.95, 1.0};
    double[] meanUrgentDuration = {15, 17.5, 22.5, 30, 30};
    double[] stdevUrgentDuration = {2.5, 1, 2.5, 1, 4.5};
    double weightEl = 1.0 / 168.0;      // objective weight elective appointment wait time
    double weightUr = 1.0 / 9.0;        // objective weight urgent scan wait time

    // Variables we need to set ourselves
    int W = 104;                        // number of weeks (= runs length)
    int R = 28;                      // number of replications (set their values yourself in the initialization method!)
    int d,s,w,r;
    int rule = 2;                      // the appointment scheduling rule
    //not sure about this one
    Slot[][] weekSchedule = new Slot[S][D];       // array of the cyclic slot schedule (days-slots)


    //Variables specific to one simulation run
    ArrayList<Patient> patients = new ArrayList<>();            // patient list
    double[] movingAvgElectiveAppWT = new double[W];    // moving average elective appointment waiting time
    double[] movingAvgElectiveScanWT = new double[W];   // moving average elective scan waiting time
    double[] movingAvgUrgentScanWT = new double[W];     // moving average urgent scan waiting time
    double[] movingAvgOT = new double[W];               // moving average overtime
    double avgElectiveAppWT = 0;           // average elective appointment waiting time
    double avgElectiveScanWT = 0;          // average elective scan waiting time
    double avgUrgentScanWT = 0;            // average urgent scan waiting time
    double avgOT = 0;                      // average overtime
    int numberOfElectivePatientsPlanned = 0;
    int numberOfUrgentPatientsPlanned = 0;

    ArrayList<Integer> count = new ArrayList<>();
    //set true to generateRandomNumbers and set false to use the same random numbers (method of common random numbers)
    // +!! if you set false you also have to change the text file on line 589 to a file that you haven't used yet
    boolean generateRandomNumbers = true;
    File f1 = new File("/Users/jaridevos/Documents/Simulation/Exercise3/randomnumbersElective.txt");
    ArrayList<String> read = new ArrayList<>();

    public void fillArray() throws FileNotFoundException{
        Scanner sc = new Scanner(f1);
        while(sc.hasNextLine()){ read.add(sc.nextLine()); }
        sc.close();
    }

    public void resetSystem(){
        patients.clear();
        avgElectiveAppWT = 0;
        avgElectiveScanWT = 0;
        avgUrgentScanWT = 0;
        avgOT = 0;
        numberOfElectivePatientsPlanned = 0;
        numberOfUrgentPatientsPlanned = 0;
        for(int i = 0; i<W; i++){
            movingAvgElectiveAppWT[i] = 0;
            movingAvgElectiveScanWT[i] = 0;
            movingAvgUrgentScanWT[i] = 0;
            movingAvgOT[i] = 0;
        }
    }

    public void setWeekSchedule() throws FileNotFoundException{
        File file = new File(inputFileName);
        Scanner scanner = new Scanner(file);
        for(int s = 0; scanner.hasNextLine() && s<32; s++){
            for(int d = 0; scanner.hasNext() && d<D; d++){
                int elementInt = Integer.parseInt(scanner.next());
                weekSchedule[s][d] = new Slot(elementInt, elementInt);
            }
        }
        scanner.close();

        for(int d = 0; d < D; d++){
            for(int s = 32; s < S; s++){
                weekSchedule[s][d] = new Slot(3,2);
            }
        }

        double time;
        for(int d = 0; d<D; d++){
            time = 8;
            for(int s = 0; s<S; s++){
                weekSchedule[s][d].startTime = time;
                if(weekSchedule[s][d].slotType != 1){    // all slot types not elective : appointment time = slot start time
                    weekSchedule[s][d].appTime = time;
                }else{                                      // elective slots: appointment time is set according to rule !
                    if(rule == 1){ // FIFO
                        weekSchedule[s][d].appTime = time;
                        }else if(rule == 2){ // Bailey-Welch rule
                            if (s == 0) {
                                weekSchedule[s][d].appTime = time;
                            } else weekSchedule[s][d].appTime = time - slotLength;
                        }else if(rule == 3){ // Blocking rule
                            if (s % 2 == 0) {
                                weekSchedule[s][d].appTime = time;
                            } else weekSchedule[s][d].appTime = time - slotLength;
                        }else if(rule == 4){ // Benchmark rule
                            weekSchedule[s][d].appTime = time - (0.5 * stdevElectiveDuration);
                    }
                }
                time += slotLength;
                if(time == 12){
                    time = 13; // skip to the end of the luchbreak
                }
            }
        }
    }

    public int getRandomScanType(Random rand){

        double r = (float) rand.nextInt(1000)/1000;
        int type = -1;
        for(int i = 0; i<5 && type == -1; i++){
            if(r<cumulativeProbUrgentType[i]){
                type = i;
            }
            if(r == 1.0){
                type = 4;
            }
        }
        return type;
    }

    public void generatePatients(Random r, PrintWriter pw) throws IOException {
        double arrivalTimeNext;
        int counter = 0; // total number of patients so far
        int patientType, scanType, endTime;
        double callTime, tardiness, duration, lambda;
        boolean noShow;
        if(generateRandomNumbers){
            //PrintWriter pw = new PrintWriter(new FileWriter("/Users/jaridevos/Documents/Simulation/Exercise3/randomnumbersElective.txt"));
            //PrintWriter prwr = new PrintWriter(new FileWriter("/Users/jaridevos/Documents/Simulation/Exercise3/randomnumbersUrgent.txt"));
            for(w=0; w < W; w++){
                for(d = 0; d < D; d++){ // not on Sunday
                    // generate ELECTIVE patients for this day
                    if(d < D-1){  // not on Saturday either
                        arrivalTimeNext = 8 + Distributions.Exponential_distribution(lambdaElective, r) * (17-8);
                        while(arrivalTimeNext < 17){ // desk open from 8h until 17h
                            patientType = 1;                // elective
                            scanType = 0;                   // no scan type
                            callTime = arrivalTimeNext;     // set call time, i.e. arrival event time
                            tardiness = Distributions.Normal_distribution(meanTardiness, stdevTardiness, r) / 60.0;       // in practice this is not known yet at time of call
                            int num = Distributions.Bernouilli_distribution(probNoShow, r); // in practice this is not known yet at time of call
                            if(num == 1){
                                noShow = true;
                            } else{
                                noShow = false;
                            }
                            duration = Distributions.Normal_distribution(meanElectiveDuration, stdevElectiveDuration, r) / 60.0; // in practice this is not known yet at time of call
                            pw.write(arrivalTimeNext + ";" + tardiness + ";" + num + ";" + duration + "\n");
                            Patient pat = new Patient(counter, patientType, scanType, w, d, callTime, tardiness, noShow, duration);
                            patients.add(pat);
                            count.add(counter);
                            counter++;
                            arrivalTimeNext = arrivalTimeNext + Distributions.Exponential_distribution(lambdaElective, r) * (17-8); // arrival time of next patient (if < 17h)
                        }
                        pw.write(arrivalTimeNext + ";" + "\n");
                    }
                    // generate URGENT patients for this day
                    if(d == 3 || d == 5){
                        lambda = lambdaUrgent[1]; // on Wed and Sat, only half a day!
                        endTime = 12;
                    }else{
                        lambda = lambdaUrgent[0];
                        endTime = 17;
                    }
                    arrivalTimeNext = 8 + Distributions.Exponential_distribution(lambda, r) * (endTime-8);
                    while(arrivalTimeNext < endTime){ // desk open from 8h until 17h
                        patientType = 2;                // urgent
                        scanType = getRandomScanType(r); // set scan type
                        callTime = arrivalTimeNext;     // set arrival time, i.e. arrival event time
                        tardiness = 0;                  // urgent patients have an arrival time = arrival event time
                        noShow = false;                 // urgent patients are never no-show
                        duration = Distributions.Normal_distribution(meanUrgentDuration[scanType], stdevUrgentDuration[scanType], r) / 60.0; // in practice this is not known yet at time of arrival
                        pw.write(arrivalTimeNext + ";" + scanType + ";" + duration + "\n");
                        Patient patient = new Patient(counter, patientType, scanType, w, d, callTime, tardiness, noShow, duration);
                        patients.add(patient);
                        count.add(counter);
                        counter++;
                        arrivalTimeNext = arrivalTimeNext + Distributions.Exponential_distribution(lambda, r) * (endTime-8); // arrival time of next patient (if < 17h)
                    }
                    pw.write(arrivalTimeNext + ";" + "\n");
                }
            }
        }else{
            /*File f2 = new File("/Users/jaridevos/Documents/Simulation/Exercise3/randomnumbersUrgent.txt");
            ArrayList<String> read2 = new ArrayList<>();
            Scanner sc2 = new Scanner(f2);
            while(sc2.hasNextLine()){ read2.add(sc2.nextLine()); }*/

            for(w=0; w < W; w++){
                for(d = 0; d < D; d++){ // not on Sunday
                    // generate ELECTIVE patients for this day
                    if(d < D-1){  // not on Saturday either
                        arrivalTimeNext = Double.parseDouble(read.get(0).substring(0, read.get(0).indexOf(";")));
                        while(arrivalTimeNext < 17){ // desk open from 8h until 17h
                            String line = read.get(0);
                            patientType = 1;                // elective
                            scanType = 0;                   // no scan type
                            callTime = arrivalTimeNext;     // set call time, i.e. arrival event time
                            line = line.substring(line.indexOf(";")+1);
                            tardiness = Double.parseDouble(line.substring(0, line.indexOf(";")));       // in practice this is not known yet at time of call
                            line = line.substring(line.indexOf(";")+1);
                            int num = Integer.parseInt(line.substring(0, line.indexOf(";"))); // in practice this is not known yet at time of call
                            if(num == 1){
                                noShow = true;
                            } else{
                                noShow = false;
                            }
                            duration = Double.parseDouble(line.substring(line.indexOf(";")+1)); // in practice this is not known yet at time of call
                            Patient pat = new Patient(counter, patientType, scanType, w, d, callTime, tardiness, noShow, duration);
                            patients.add(pat);
                            count.add(counter);
                            counter++;
                            read.remove(0);
                            arrivalTimeNext = Double.parseDouble(read.get(0).substring(0, read.get(0).indexOf(";"))); // arrival time of next patient (if < 17h)
                        }
                    }
                    // generate URGENT patients for this day
                    if(d == 3 || d == 5){
                        lambda = lambdaUrgent[1]; // on Wed and Sat, only half a day!
                        endTime = 12;
                    }else{
                        lambda = lambdaUrgent[0];
                        endTime = 17;
                    }
                    if((d != D-1)){
                        read.remove(0);
                    }
                    arrivalTimeNext = Double.parseDouble(read.get(0).substring(0, read.get(0).indexOf(";")));
                    while(arrivalTimeNext < endTime){ // desk open from 8h until 17h
                        String line2 = read.get(0);
                        patientType = 2;              // urgent
                        line2 = line2.substring(line2.indexOf(";")+1);
                        scanType = Integer.parseInt(line2.substring(0,line2.indexOf(";"))); // set scan type
                        callTime = arrivalTimeNext;     // set arrival time, i.e. arrival event time
                        tardiness = 0;                  // urgent patients have an arrival time = arrival event time
                        noShow = false;                 // urgent patients are never no-show
                        line2 = line2.substring(line2.indexOf(";")+1);
                        duration = Double.parseDouble(line2); // in practice this is not known yet at time of arrival
                        Patient patient = new Patient(counter, patientType, scanType, w, d, callTime, tardiness, noShow, duration);
                        patients.add(patient);
                        count.add(counter);
                        counter++;
                        read.remove(0);
                        arrivalTimeNext = Double.parseDouble(read.get(0).substring(0, read.get(0).indexOf(";"))); // arrival time of next patient (if < 17h)
                    }
                    read.remove(0);
                }
            }
        }
    }



    public int getNextSlotNrFromTime(int day, int patientType, double time) throws NumberFormatException{
        boolean found = false;
        int slotNr = -1;
        for(s = 0; !found && s < S; s++){
            if(weekSchedule[s][day].appTime > time && patientType == weekSchedule[s][day].patientType){
                found = true;
                slotNr = s;
            }
        }
        if(!found){
            System.out.println("NO SLOT EXISTS DURING TIME " + time);
            throw new NumberFormatException();
        }
        return slotNr;
    }

    public void schedulePatients(){
        //sort arrival events (= patient list) on arrival time (call time for elective patients, arrival time for urgent)
        Collections.sort(patients, new Comparator<Patient>() {
            @Override
            public int compare(Patient o1, Patient o2) {
                if(o1.callWeek != o2.callWeek){ return o1.callWeek - o2.callWeek; }
                if (o1.callDay != o2.callDay){ return o1.callDay - o2.callDay; }
                if(o1.callTime != o2.callTime){
                    if((o1.callTime - o2.callTime) > 0){ return 1;
                    }else if((o1.callTime - o2.callTime) == 0){ return 0;
                    }else{ return -1; } }
                if(o1.patientType == 2){ return 1; }
                if(o2.patientType == 2){ return -1; }
                return 1;
            }
        });

        int[] week = {0,0}; // week of the next available slot {elective,urgent}
        int[] day = {0,0}; // day of the next available slot {elective,urgent}
        int[] slot = {0,0}; // slotNr of the next available slot {elective,urgent}

        //find first slot of each patient type (note, we assume each day (i.e. also day 0) has at least one slot of each patient type!)
        //elective
        d = 0;
        boolean found = false;
        for(s = 0; s < S && !found; s++){
            if(weekSchedule[s][d].patientType == 1){
                day[0] = d;
                slot[0] = s;
                found = true;
            }
        }
        //urgent
        found = false;
        for(s = 0; s < S && !found; s++){
            if(weekSchedule[s][d].patientType == 2){
                day[1] = d;
                slot[1] = s;
                found = true;
            }
        }

        // go over SORTED patient list and assign slots
        int previousWeek = 0;
        int numberOfElective = 0;
        int numberOfElectivePerWeek = 0;   // keep track of week to know when to update moving average elective appointment waiting time
        double wt;
        int slotNr;
        for(Patient pat : patients){
            //set index i dependant on patient type
            int i = pat.patientType - 1;

            // if still within the planning horizon:
            if(week[i] < W){
                // determine week where we start searching for a slot
                if(pat.callWeek > week[i]){
                    week[i] = pat.callWeek;
                    day[i] = 0;
                    slot[i] = getNextSlotNrFromTime(day[i], pat.patientType, 0);           // note we assume there is at least one slot of each patient type per day => this line will find first slot of this type
                }
                // determine day where we start searching for a slot
                if(pat.callWeek == week[i] && pat.callDay > day[i]){
                    day[i] = pat.callDay;
                    slot[i] = getNextSlotNrFromTime(day[i], pat.patientType, 0);           // note we assume there is at least one slot of each patient type per day => this line will find first slot of this type
                }

                // determine slot
                if(pat.callWeek == week[i] && pat.callDay == day[i] && pat.callTime >= weekSchedule[slot[i]][day[i]].appTime){
                    // find last slot on day "day[i]"
                    found = false; slotNr = -1;
                    for(s = S - 1; s >= 0 && !found; s--){
                        if(weekSchedule[s][day[i]].patientType == pat.patientType){
                            found = true;
                            slotNr = s;
                        }
                    }
                    // urgent patients have to be treated on the same day either in normal hours or in overtime (!! make sure there are enough overtime slots)
                    // for elective patients: check if the patient call time is before the last slot, i.e. if the patient can be planned on day "day[i]"
                    if(pat.patientType == 2 || pat.callTime < weekSchedule[slotNr][day[i]].appTime){
                        slot[i] = getNextSlotNrFromTime(day[i], pat.patientType, pat.callTime);   // find the first elective slot after the call time on day "day[i]"
                    }else{
                        // determine the next day
                        if(day[i] < D - 1){
                            day[i] = day[i] + 1;
                        }else{
                            day[i] = 0;
                            week[i] = week[i] + 1;
                        }
                        if(week[i] < W){   // find the first slot on the next day (if within the planning horizon)
                            slot[i] = getNextSlotNrFromTime(day[i], pat.patientType, 0);
                        }
                    }
                }

                // schedule the patient
                pat.scanWeek = week[i];
                pat.scanDay = day[i];
                pat.slotNr = slot[i];
                pat.appTime = weekSchedule[slot[i]][day[i]].appTime;

                // update moving average elective appointment waiting time
                if(pat.patientType == 1){
                    if(previousWeek < week[i]){
                        movingAvgElectiveAppWT[previousWeek] = movingAvgElectiveAppWT[previousWeek] / numberOfElectivePerWeek;
                        numberOfElectivePerWeek = 0;
                        previousWeek = week[i];
                    }
                    wt = pat.getAppWT();
                    movingAvgElectiveAppWT[week[i]] += wt;
                    numberOfElectivePerWeek++;
                    avgElectiveAppWT += wt;
                    numberOfElective++;
                }

                // set next slot of the current patient type
                found = false; int startD = day[i]; int startS = slot[i] + 1;
                for(w = week[i]; w < W && !found; w++){
                    for(d = startD; d < D && !found; d++){
                        for(s = startS; s < S && !found; s++){
                            if(weekSchedule[s][d].patientType == pat.patientType){
                                week[i] = w;
                                day[i] = d;
                                slot[i] = s;
                                found = true;
                            }
                        }
                        startS = 0;
                    }
                    startD = 0;
                }
                if(!found) week[i] = W;
            }
        }
        // update moving average elective appointment waiting time in last week
        movingAvgElectiveAppWT[W-1] = movingAvgElectiveAppWT[W-1] / numberOfElectivePerWeek;

        // calculate objective value
        avgElectiveAppWT = avgElectiveAppWT / numberOfElective;

    }

    public void sortPatientsOnAppTime(){
        Collections.sort(patients, new Comparator<Patient>() {
            @Override
            public int compare(Patient patient1, Patient patient2) {
                if(patient1.scanWeek == -1 && patient2.scanWeek == -1){
                    if (patient1.callWeek != patient2.callWeek)
                        return patient1.callWeek - patient2.callWeek;
                    if (patient1.callDay != patient2.callDay)
                        return patient1.callDay - patient2.callDay;
                    if (patient1.callTime != patient2.callTime){
                        if((patient1.callTime - patient2.callTime) > 0){ return 1; }
                        else if((patient1.callTime - patient2.callTime) == 0){ return 0; }
                        else{ return -1; }
                    }
                    if (patient1.scanType == 2)          // if arrival time same, urgent patient before elective patient
                        return 1;
                    if (patient2.scanType == 2)
                        return -1;
                    return 1;
                }
                if(patient1.scanWeek == -1){
                    return 1;
                }
                if(patient2.scanWeek == -1){
                    return -1;
                }
                if (patient1.scanWeek != patient2.scanWeek)
                    return patient1.scanWeek - patient2.scanWeek;
                if (patient1.scanDay != patient2.scanDay)
                    return patient1.scanDay - patient2.scanDay;
                if (patient1.appTime != patient2.appTime){
                    if((patient1.appTime - patient2.appTime)>0){ return 1;}
                    else if((patient1.appTime - patient2.appTime)==0){ return 0;}
                    else{ return -1;}
                }
                if (patient1.scanType == 2)                             // if arrival time same, urgent patient before elective patient
                    return 1;
                if (patient2.scanType == 2)
                    return -1;
                if(patient1.nr < patient2.nr){
                    return 1;
                }
                if(patient1.nr > patient2.nr){
                    return -1;
                }
                return 1;
            }
        });
    }

    public void runOneSimulation(Random r, PrintWriter pw) throws IOException{
        try{
            generatePatients(r, pw);// create patient arrival events (elective patients call, urgent patient arrive at the hospital)
        }catch(Exception e){
            System.out.println("smt wrong with writing random numbers");
        }
        schedulePatients();     // schedule urgent and elective patients in slots based on their arrival events => determine the appointment wait time
        sortPatientsOnAppTime();   // sort patients on their appointment time (unscheduled patients are grouped at the end of the list)

        // determine scan wait time per patient and overtime per day
        int prevWeek = 0; int prevDay = -1;
        int[] numberOfPatientsWeek = {0,0};
        int[] numberOfPatients = {0,0};
        double arrivalTime, wt;
        double prevScanEndTime = 0;
        boolean prevIsNoShow = false;
        // go over arrival events (i.e. the moment the patient arrives at the hospital)
        for(Patient pat : patients){
            if(pat.scanWeek == -1){ // stop at the first unplanned patient
                break;
            }

            arrivalTime = (double) pat.appTime + pat.tardiness;
            // SCAN WT
            if(!pat.isNoShow){
                if(pat.scanWeek != prevWeek || pat.scanDay != prevDay){
                    pat.scanTime = arrivalTime;
                } else{
                    if(prevIsNoShow){
                        pat.scanTime = Math.max(weekSchedule[pat.slotNr][pat.scanDay].startTime, Math.max(prevScanEndTime,arrivalTime)); // note we assume we wait at least 15minutes on a no-show patient to see whether he shows or is just late
                    }else{
                        pat.scanTime = Math.max(prevScanEndTime,arrivalTime);
                    }
                }
                wt = pat.getScanWT();
                if(pat.patientType == 1){
                    movingAvgElectiveScanWT[pat.scanWeek] += wt;
                }else{
                    movingAvgUrgentScanWT[pat.scanWeek] += wt;
                }
                numberOfPatientsWeek[pat.patientType - 1]++;
                if(pat.patientType == 1){
                    avgElectiveScanWT += wt;
                }else{
                    avgUrgentScanWT += wt;
                }
                numberOfPatients[pat.patientType - 1]++;
            }

            // OVERTIME
            if(prevDay > -1 && prevDay != pat.scanDay){
                if(prevDay == 3 || prevDay == 5){
                    movingAvgOT[prevWeek] += Math.max(0.0, prevScanEndTime - 13);
                }else{
                    movingAvgOT[prevWeek] += Math.max(0.0, prevScanEndTime - 17);
                }
                if(prevDay == 3 || prevDay == 5){
                    avgOT += Math.max(0.0, prevScanEndTime - 13);
                }else{
                    avgOT += Math.max(0.0, prevScanEndTime - 17);
                }
            }

            // update moving averages if week ends
            if(prevWeek != pat.scanWeek){
                movingAvgElectiveScanWT[prevWeek] = movingAvgElectiveScanWT[prevWeek] / numberOfPatientsWeek[0];
                movingAvgUrgentScanWT[prevWeek] = movingAvgUrgentScanWT[prevWeek] / numberOfPatientsWeek[1];
                movingAvgOT[prevWeek] = movingAvgOT[prevWeek] / D;
                numberOfPatientsWeek[0] = 0;
                numberOfPatientsWeek[1] = 0;
            }

            //set prev patient
            if(pat.isNoShow){
                //prevScanEndTime stays the same, it is the end time of the patient before the no-show patient
                prevIsNoShow = true;
                if(pat.scanWeek != prevWeek || pat.scanDay != prevDay){
                    prevScanEndTime = weekSchedule[pat.slotNr][pat.scanDay].startTime;
                }
            }else{
                prevScanEndTime = pat.scanTime + pat.duration;
                prevIsNoShow = false;
            }
            prevWeek = pat.scanWeek;
            prevDay = pat.scanDay;
        }
        // update moving averages of the last week
        movingAvgElectiveScanWT[W-1] = movingAvgElectiveScanWT[W-1] / numberOfPatientsWeek[0];
        movingAvgUrgentScanWT[W-1] = movingAvgUrgentScanWT[W-1] / numberOfPatientsWeek[1];
        movingAvgOT[W-1] = movingAvgOT[W-1] / D;

        // calculate objective values
        avgElectiveScanWT = avgElectiveScanWT / numberOfPatients[0];
        avgUrgentScanWT = avgUrgentScanWT / numberOfPatients[1];
        avgOT = avgOT / (D * W);
    }

    public void runSimulations() throws IOException {
        double electiveAppWT = 0;
        double electiveScanWT = 0;
        double urgentScanWT = 0;
        double OT = 0;
        double OV = 0;
        try{  // set cyclic slot schedule based on given input file
            setWeekSchedule();
        }catch (FileNotFoundException e){
            System.out.println("File not found");
        }
        System.out.println("R \t elAppWT \t elScanWT \t urScanWT \t OT \t OV \n");
        // run R replications
        PrintWriter pw1 = new PrintWriter(new FileWriter("/Users/jaridevos/Documents/Simulation/Output.txt"));
        // if you want to use the same random numbers, you need to fill in a text file that you haven't used yet here instead of the file randomnumbersElective
        PrintWriter pw = new PrintWriter(new FileWriter("/Users/jaridevos/Documents/Simulation/Exercise3/randomnumbersElective.txt"));
        try{
            fillArray();
        }catch(FileNotFoundException e){
            System.out.println("File with randomnumbers not found");
        }
        for(r = 0; r < R; r++){
            resetSystem(); // reset all variables related to 1 replication
            Random rand = new Random();
            rand.setSeed(r);               // set seed value for random value generator
            runOneSimulation(rand, pw);     // run 1 simulation / replication
            electiveAppWT += avgElectiveAppWT;
            electiveScanWT += avgElectiveScanWT;
            urgentScanWT += avgUrgentScanWT;
            OT += avgOT;
            OV += (avgElectiveAppWT * weightEl) + (avgUrgentScanWT * weightUr);
            //System.out.println(r+1 + " " + String.format("\t\t%.4f  \t", avgElectiveAppWT) + String.format(" %.4f \t ", avgElectiveScanWT) + String.format("\t %.4f \t ", avgUrgentScanWT) + String.format("\t %.4f \t ", avgOT) + String.format("%.4f \t ", ((avgElectiveAppWT * weightEl) + (avgUrgentScanWT * weightUr))));
            System.out.println(String.format("%.4f",((avgElectiveAppWT * weightEl) + (avgUrgentScanWT * weightUr))));
            pw1.write(String.format("%.4f",((avgElectiveAppWT * weightEl) + (avgUrgentScanWT * weightUr))) + "\n");
        }
        pw1.close();
        pw.close();
        //System.out.println(count.size());
        electiveAppWT = electiveAppWT / R;
        electiveScanWT = electiveScanWT / R;
        urgentScanWT = urgentScanWT / R;
        OT = OT / R;
        OV = OV / R;
        double objectiveValue = (electiveAppWT * weightEl) + (urgentScanWT * weightUr);
        //System.out.println("Avg. " + String.format("\t%.4f",electiveAppWT) + " " + String.format("\t %.4f \t",electiveScanWT) + " " + String.format("\t %.4f\t",urgentScanWT) + " " + String.format("\t %.4f\t",OT) + " " + String.format("%.4f",objectiveValue));

        //System.out.println("AVG: " + String.format("%.4f",objectiveValue));
        // print results
        //PrintWriter pw = new PrintWriter(new FileWriter("C:/Users/casco/OneDrive/Documents/GitHub/ProjectSimulation/output.txt")); // TODO: use your own directory
        //pw.write("week \t elAppWT \t elScanWT \t urScanWT \t OT \n");

        //for(w = 0; w < W; w++){
        //    pw.write("");
        //}
        //pw.close();
        // TODO: print the output you need to a .txt file
    }
}

public class Patient {
    int nr;
    int patientType;    // (0=none, 1=elective, 2=urgent)
    int scanType;       // elective: (0=none), urgent: (0=brain, 1=lumbar, 2=cervival, 3=abdomen, 4=others)
    int callWeek;       // week of arrival (elective: call, urgent: actual arrival)
    int callDay;        // day of arrival (elective: call, urgent: actual arrival)
    double callTime;    // time of arrival (elective: call, urgent: actual arrival) (in hours)
    int scanWeek;       // week of appointment
    int scanDay;        // day of appointment
    int slotNr;         // slot number of appointment
    double appTime;     // time of appointment (elective: according to rule, urgent: slot start time) (in hours)
    double tardiness;   // (in hours)
    boolean isNoShow;
    double scanTime;    // actual start time of the scan
    double duration;    // actual duration of the scan

    public Patient(int nr_, int patientType_, int scanType_, int callWeek_, int callDay_, double callTime_,
                   double tardiness_, boolean isNoShow_, double duration_){
        nr = nr_;
        patientType = patientType_;
        scanType = scanType_;
        callWeek = callWeek_;
        callDay = callDay_;
        callTime = callTime_;
        tardiness = tardiness_;
        isNoShow = isNoShow_;
        duration = duration_;

        scanWeek = -1;
        scanDay = -1;
        slotNr = -1;
        appTime = -1;
        scanTime = -1.0;
    }

    public double getAppWT() throws NumberFormatException{
        if(slotNr != -1){
            double appWT = (((scanWeek-callWeek)*7 + scanDay - callDay)*24 + appTime - callTime); // in hours;
            return appWT;
        }else{
            System.out.println("CAN NOT CALCULATE APPOINTMENT WT OF PATIENT " + this.nr);
            throw new NumberFormatException();
        }
    }

    public double getScanWT() throws NumberFormatException{
        if(scanTime != 0){
            double scanWt;
            if(patientType == 1){ // elective
                scanWt = scanTime - (appTime + tardiness);
            }else{ // urgent
                scanWt = scanTime - callTime;
            }
            return Math.max(0.0, scanWt);
        }else{
            System.out.println("CAN NOT CALCULATE SCAN WT OF PATIENT " + this.nr);
            throw new NumberFormatException();
        }
    }
}


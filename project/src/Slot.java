public class Slot {
    double startTime;  // start time of the slot (in hours)
    double appTime;    // appointment time of the slot, dependant on type and rule (in hours)
    int slotType;       // type of slot (0=none, 1=elective, 2=urgent within normal working hours, 3=urgent in overtime)
    int patientType;    // (0=none, 1=elective, 2=urgent)

    public Slot(int slotType_, int patientType_){
        this.slotType = slotType_;
        this.patientType = patientType_;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getAppTime() {
        return appTime;
    }

    public int getSlotType() {
        return slotType;
    }

    public int getPatientType() {
        return patientType;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public void setAppTime(double appTime) {
        this.appTime = appTime;
    }

    public void setSlotType(int slotType) {
        this.slotType = slotType;
    }

    public void setPatientType(int patientType) {
        this.patientType = patientType;
    }
}
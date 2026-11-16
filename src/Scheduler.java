import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

class Process {
    int pid;
    int arrivalTime;
    int burstTime;
    int remainingTime;
    int priority;
    int startTime = -1;
    int completionTime = 0;

    Process(int pid, int arrivalTime, int burstTime) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.priority = 1;
    }

    // Constructor for the extra credit when the priority is provided
    Process(int pid, int arrivalTime, int burstTime, int priority) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.priority = priority;
    }

    int getTurnaroundTime() {
        return completionTime - arrivalTime;
    }

    int getWaitingTime() {
        return getTurnaroundTime() - burstTime;
    }

    int getResponseTime() {
        if (startTime == -1) {
            return 0;
        }
        return startTime - arrivalTime;
    }

    public String toString() {
        return "Process: " + pid + " (Arrival: " + arrivalTime + ", Burst: " + burstTime + ", Priority: " + priority + ")";
    }

    // A simple trim method to remove leading and trailing spaces.
    public static String trim(String s) {
        int start = 0;
        int end = s.length() - 1;
        while(start <= end && s.charAt(start) == ' ') {
            start++;
        }
        while(end >= start && s.charAt(end) == ' ') {
            end--;
        }
        if(start > end) {
            return "";
        }
        return s.substring(start, end + 1);
    }

    // Custom method to convert a string of digits into an integer.
    public static int myAtoi(String s) {
        int num = 0;
        s = trim(s);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if(c < '0' || c > '9'){
                // if not a digit, simply stop processing the string.
                break;
            }
            num = num * 10 + (c - '0');
        }
        return num;
    }

    // Reads processes from a file.
    // Assumes that the file has a header row and then each subsequent line contains:
    // pid, arrivalTime, burstTime   OR   pid, arrivalTime, burstTime, priority
    public static ArrayList<Process> readProcesses(String filename) {
        ArrayList<Process> list = new ArrayList<Process>();
        try {
            File file = new File(filename);
            Scanner fileScanner = new Scanner(file);
            // Skip the header line.
            if(fileScanner.hasNextLine()){
                fileScanner.nextLine();
            }
            while(fileScanner.hasNextLine()){
                String line = fileScanner.nextLine();
                if(line.length() == 0) {
                    continue;
                }
                // Replace commas with spaces (in case the file uses comma delimiters)
                line = line.replace(',', ' ');
                Scanner lineSc = new Scanner(line);
                if(lineSc.hasNext()){
                    String pidStr = lineSc.next();
                    String arrivalStr = lineSc.next();
                    String burstStr = lineSc.next();
                    int pid = myAtoi(pidStr);
                    int arrival = myAtoi(arrivalStr);
                    int burst = myAtoi(burstStr);
                    if(lineSc.hasNext()){
                        String prioStr = lineSc.next();
                        int priority = myAtoi(prioStr);
                        list.add(new Process(pid, arrival, burst, priority));
                    } else {
                        list.add(new Process(pid, arrival, burst));
                    }
                }
                lineSc.close();
            }
            fileScanner.close();
        } catch(Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return list;
    }

    // Bubble sort to order processes by arrivalTime.
    public static void sortByArrival(ArrayList<Process> processes) {
        for (int i = 0; i < processes.size() - 1; i++) {
            for (int j = i + 1; j < processes.size(); j++) {
                if(processes.get(i).arrivalTime > processes.get(j).arrivalTime) {
                    Process temp = processes.get(i);
                    processes.set(i, processes.get(j));
                    processes.set(j, temp);
                }
            }
        }
    }

    // Standard Round Robin simulation.
    public static void runRoundRobin(ArrayList<Process> processes, int timeQuantum) {
        System.out.println("Round Robin Scheduling (Time Quantum = " + timeQuantum + ")");
        sortByArrival(processes);
        int currentTime = 0;
        int completed = 0;
        int contextSwitches = 0;
        int idleTime = 0;
        Queue<Process> readyQueue = new LinkedList<Process>();
        int index = 0;  // pointer for new arrivals

        while(completed < processes.size()){
            // Add processes that have arrived to the queue.
            while(index < processes.size() && processes.get(index).arrivalTime <= currentTime){
                Process p = processes.get(index);
                readyQueue.add(p);
                System.out.println("Time " + currentTime + ": Process " + p.pid + " added to queue.");
                index++;
            }
            if(readyQueue.isEmpty()){
                System.out.println("Time " + currentTime + ": CPU is idle.");
                currentTime++;
                idleTime++;
                continue;
            }
            Process currentProcess = readyQueue.poll();
            if(currentProcess.startTime == -1) {
                currentProcess.startTime = currentTime;
                System.out.println("Time " + currentTime + ": Process " + currentProcess.pid + " starts running.");
            } else {
                System.out.println("Time " + currentTime + ": Process " + currentProcess.pid + " resumes running.");
            }
            int execTime = (currentProcess.remainingTime < timeQuantum) ? currentProcess.remainingTime : timeQuantum;
            System.out.println("Time " + currentTime + " to " + (currentTime + execTime) + ": Process " + currentProcess.pid + " runs for " + execTime + " unit(s).");
            currentProcess.remainingTime -= execTime;
            currentTime += execTime;
            contextSwitches++;

            // Check for new arrivals during execution.
            while(index < processes.size() && processes.get(index).arrivalTime <= currentTime){
                Process p = processes.get(index);
                readyQueue.add(p);
                System.out.println("Time " + currentTime + ": Process " + p.pid + " added to queue.");
                index++;
            }

            if(currentProcess.remainingTime > 0){
                readyQueue.add(currentProcess);
                System.out.println("Time " + currentTime + ": Process " + currentProcess.pid + " is preempted.");
            } else {
                currentProcess.completionTime = currentTime;
                completed++;
                System.out.println("Time " + currentTime + ": Process " + currentProcess.pid + " finished.");
            }
        }

        // Calculate and output performance metrics.
        int totalTurnaround = 0;
        int totalWaiting = 0;
        for(Process p : processes) {
            totalTurnaround += p.getTurnaroundTime();
            totalWaiting += p.getWaitingTime();
        }
        double avgTurnaround = (double)totalTurnaround / processes.size();
        double avgWaiting = (double)totalWaiting / processes.size();
        double cpuUtil = 100 * (1 - (idleTime + contextSwitches * 0.1 / (double) currentTime));
        double throughput = processes.size() / (double)currentTime;

        System.out.println("\n--- Performance Metrics ---");
        System.out.println("Total execution time: " + currentTime + " time unit(s).");
        System.out.println("Idle time: " + idleTime + " time unit(s).");
        System.out.println("Context switches: " + contextSwitches);
        System.out.println("Average Turnaround Time: " + avgTurnaround);
        System.out.println("Average Waiting Time: " + avgWaiting);
        System.out.println("CPU Utilization: " + cpuUtil + "%");
        System.out.println("Throughput: " + throughput + " processes per time unit.");
    }

    // Extra Credit: Priority-based Round Robin simulation.
    // Here, among ready processes the one with the highest priority (lowest number) is selected.
    public static void runPriorityRoundRobin(ArrayList<Process> processes1, int timeQuantum) {
        System.out.println("Priority-based Round Robin Scheduling (Time Quantum = " + timeQuantum + ")");
        sortByArrival(processes1);
        int currentTime = 0;
        int completed = 0;
        int contextSwitches = 0;
        int idleTime = 0;
        ArrayList<Process> readyQueue = new ArrayList<Process>();
        int index = 0;

        while(completed < processes1.size()){
            while(index < processes1.size() && processes1.get(index).arrivalTime <= currentTime){
                Process p = processes1.get(index);
                readyQueue.add(p);
                System.out.println("Time " + currentTime + ": Process " + p.pid + " (Priority " + p.priority + ") added to queue.");
                index++;
            }
            if(readyQueue.size() == 0){
                System.out.println("Time " + currentTime + ": CPU is idle.");
                currentTime++;
                idleTime++;
                continue;
            }
            // Find the process with the highest priority (lowest numeric value)
            int chosenIndex = 0;
            for(int i = 1; i < readyQueue.size(); i++){
                if(readyQueue.get(i).priority < readyQueue.get(chosenIndex).priority){
                    chosenIndex = i;
                }
            }
            Process currentProcess = readyQueue.remove(chosenIndex);
            if(currentProcess.startTime == -1) {
                currentProcess.startTime = currentTime;
                System.out.println("Time " + currentTime + ": Process " + currentProcess.pid + " starts running.");
            } else {
                System.out.println("Time " + currentTime + ": Process " + currentProcess.pid + " resumes running.");
            }
            int execTime = (currentProcess.remainingTime < timeQuantum) ? currentProcess.remainingTime : timeQuantum;
            System.out.println("Time " + currentTime + " to " + (currentTime + execTime) + ": Process " + currentProcess.pid +
                    " runs for " + execTime + " unit(s).");
            currentProcess.remainingTime -= execTime;
            currentTime += execTime;
            contextSwitches++;

            while(index < processes1.size() && processes1.get(index).arrivalTime <= currentTime){
                Process p = processes1.get(index);
                readyQueue.add(p);
                System.out.println("Time " + currentTime + ": Process " + p.pid + " (Priority " + p.priority + ") added to queue.");
                index++;
            }

            if(currentProcess.remainingTime > 0){
                readyQueue.add(currentProcess);
                System.out.println("Time " + currentTime + ": Process " + currentProcess.pid + " is preempted.");
            } else {
                currentProcess.completionTime = currentTime;
                completed++;
                System.out.println("Time " + currentTime + ": Process " + currentProcess.pid + " finished.");
            }
        }
        int totalTurnaround = 0;
        int totalWaiting = 0;
        for(Process p : processes1) {
            totalTurnaround += p.getTurnaroundTime();
            totalWaiting += p.getWaitingTime();
        }
        double avgTurnaround = (double)totalTurnaround / processes1.size();
        double avgWaiting = (double)totalWaiting / processes1.size();
        double cpuUtil = 100 * (1 - ((idleTime + contextSwitches * 0.1) / (double)currentTime));
        double throughput = processes1.size() / (double)currentTime;

        System.out.println("\n--- Performance Metrics ---");
        System.out.println("Total execution time: " + currentTime + " time unit(s).");
        System.out.println("Idle time: " + idleTime + " time unit(s).");
        System.out.println("Context switches: " + contextSwitches);
        System.out.println("Average Turnaround Time: " + avgTurnaround);
        System.out.println("Average Waiting Time: " + avgWaiting);
        System.out.println("CPU Utilization: " + cpuUtil + "%");
        System.out.println("Throughput: " + throughput + " processes per time unit.");
    }

    public static void main(String args[]){
        if(args.length < 2){
            System.out.println("Usage: java CPU_Scheduler <process_file> <time_quantum>");
            return;
        }

        String filePath = args[0];
        int timeQuantum = myAtoi(args[1]);
        ArrayList<Process> processes = readProcesses(filePath);

        if(processes.size() == 0){
            System.out.println("No valid process data found.");
            return;
        }

        // Check if any process has a priority different from the default value of 1.
        boolean extraCreditMode = false;
        for(Process p : processes){
            if(p.priority != 1){
                extraCreditMode = true;
                break;
            }
        }

        if(extraCreditMode){
            runPriorityRoundRobin(processes, timeQuantum);
        } else {
            runRoundRobin(processes, timeQuantum);
        }
    }
}
package com.example.nwipe_android;

public class WipeJob {
    public static int MAX_NUMBER_PASSES = 10;
    public static int DEFAULT_NUMBER_PASSES = 3;
    public static boolean DEFAULT_VERIFY = false;
    public static boolean DEFAULT_BLANK = true;

    /**
     * Parameters to the job.
     */
    public int number_passes;
    public boolean verify;
    public boolean blank;

    /**
     * Completion status.
     */
    public boolean completed = false;
    public boolean succeeded = false;
    public int passes_completed = 0;

    /**
     * Information on the current pass.
     */
    public long totalBytes;
    public long wipedBytes = 0;
    public boolean verifying = false;

    public String toString() {
        if (this.completed && this.succeeded) {
            return "Succeeded";
        } else if (this.completed) {
            return "Wiping finished with errors";
        }

        if (this.passes_completed >= this.number_passes && this.blank) {
            if (this.verifying) {
                return "Verifying Blanking pass";
            } else {
                return "Blanking pass";
            }
        }

        if (this.verifying) {
            return String.format("Verifying Pass %d/%d", this.passes_completed + 1, this.number_passes);
        }
        return String.format("Pass %d/%d", this.passes_completed + 1, this.number_passes);
    }

    public int getCurrentPassPercentageCompletion() {
        return (int)(((double)this.wipedBytes / (double)this.totalBytes) * 100);
    }
}

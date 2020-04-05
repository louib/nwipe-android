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
    public int passes_completed = 0;
    public String errorMessage = "";

    /**
     * Information on the current pass.
     */
    public long totalBytes;
    public long wipedBytes = 0;
    public boolean verifying = false;

    public String toString() {
        String completionText = String.format(" (%d%%)", this.getCurrentPassPercentageCompletion());

        if (this.failed()) {
            return "Failed" + completionText;
        }

        if (this.isCompleted()) {
            return "Succeeded";
        }

        if (this.isBlankingPass() && this.blank) {
            if (this.verifying) {
                return "Verifying Blanking pass" + completionText;
            } else {
                return "Blanking pass" + completionText;
            }
        }

        if (this.verifying) {
            return String.format(
                    "Verifying Pass %d/%d%s",
                    this.passes_completed + 1,
                    this.number_passes,
                    completionText
            );
        }
        return String.format("Pass %d/%d%s", this.passes_completed + 1, this.number_passes, completionText);
    }

    public int getCurrentPassPercentageCompletion() {
        return (int)(((double)this.wipedBytes / (double)this.totalBytes) * 100);
    }

    public boolean isBlankingPass() {
        return this.passes_completed == this.number_passes;
    }

    public boolean isCompleted() {
        if (this.blank) {
            return this.passes_completed > this.number_passes;
        }
        return this.passes_completed == this.number_passes;
    }

    public boolean failed() {
        return !this.errorMessage.isEmpty();
    }
}

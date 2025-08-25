# Generic Gnuplot script to plot node throughput as a multi-line graph
# --------------------------------------------------------------------
# This script automatically detects the number of columns and plots each
# data series as a separate line.
#
# Usage: gnuplot -c plot_script.gp "input.csv" ["output.png"]

# --- Argument Handling ---
# Check for the input file argument.
if (!exists("ARG1")) {
    print "Error: No data file specified."
    print "Usage: gnuplot -c plot_script.gp \"<input.csv>\" \"[<output.png>]\""
    exit
}
datafile = ARG1

# Check for the optional output file argument.
if (exists("ARG2")) {
    outputfile = ARG2
} else {
    outputfile = 'throughput_multiline_chart.png'
}


# --- Output Configuration ---
# Set the output format and file name from variables.
set terminal pngcairo size 1920,1080 enhanced font 'Verdana,10'
set output outputfile

# --- General Plot Styling ---
set title "Node Throughput Over Time (Individual)"
set xlabel "Time (seconds)"
set ylabel "Throughput (Operations/sec)"
set grid
set key outside top center horizontal
set datafile separator ","
# --- NEW: Use a color swatch (box) in the legend instead of a line sample ---
set key box

# --- Dynamic Plot Command Generation ---
# Use 'stats' to get the number of columns in the data file.
stats datafile every ::1 nooutput
num_cols = STATS_columns

# Start the plot command.
# The 'title columnhead(2)' uses the header from the CSV for the legend.
plot_cmd = "plot datafile using 1:2 with lines title columnhead(2)"

# Loop from the third column to the last, adding a new line for each.
do for [i=3:num_cols] {
    # Append the next plot command, separated by a comma.
    # Each 'using 1:i' plots an independent line for that column.
    plot_cmd = plot_cmd . ", '' using 1:" . i . " with lines title columnhead(" . i . ")"
}

# --- Execute the Plot Command ---
# Use 'eval' to execute the dynamically generated command string.
eval(plot_cmd)

# Unset the output to finalize the file.
print "Chart generated: " . outputfile
unset output

# Generic Gnuplot script to plot node throughput from a CSV file
# ----------------------------------------------------------------
# This script automatically detects the number of columns and plots them
# as a stacked area chart.
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
    outputfile = 'throughput_chart.png'
}

# --- Output Configuration ---
# Set the output format and file name from variables.
set terminal pngcairo size 1920,1080 enhanced font 'Verdana,10'
set output outputfile

# --- General Plot Styling ---
set title "Node Throughput Over Time"
set xlabel "Time (seconds)"
set ylabel "Throughput (Operations/sec)"
set grid
set key outside top center horizontal
set datafile separator ","
set style fill solid 0.6 border -1

# --- Dynamic Plot Command Generation ---
# Use 'stats' to get the number of columns in the data file.
# This avoids hardcoding the plot command.
stats datafile every ::1 nooutput
num_cols = STATS_columns

# Initialize the plot command string for the first data column (column 2).
# The 'title columnhead(2)' uses the header from the CSV for the legend.
plot_cmd = "plot datafile using 1:2 with lines title columnhead(2)"

# Initialize the string that will hold the sum of the columns.
sum_expr = "($2)"

# Loop from the third column to the last column, building the plot command.
do for [i=3:num_cols] {
    # Append the next column to the sum expression.
    sum_expr = sum_expr . "+column(" . i . ")"
    # Append the next plot segment to the command string.
    # Each new area is stacked on the sum of the previous ones.
    plot_cmd = plot_cmd . ", '' using 1:(" . sum_expr . ") with lines title columnhead(" . i . ")"
}

# --- Execute the Plot Command ---
# Use 'eval' to execute the dynamically generated command string.
eval(plot_cmd)

# Unset the output to finalize the file.
print "Chart generated: " . outputfile
unset output
